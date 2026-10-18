/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Client-only rendering, audio, UI, Ponder, or JEI behavior.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.client;

import com.airwaves.compat.SableCompat;
import com.airwaves.net.HandheldAudioPayload;
import com.airwaves.net.RadioAudioPayload;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AudioPlayback {
   static final float SRC_RATE = 16000.0F;
   static final int FRAME = 320;
   static final int PREBUFFER = 3;
   static final int MAX_QUEUE = 12;
   private static final float[] OUT_RATES = new float[]{48000.0F, 44100.0F, 16000.0F, 32000.0F};
   static final Map<String, AudioPlayback.Source> SOURCES = new ConcurrentHashMap<>();
   static volatile float rainLevel;
   static volatile float thunderLevel;
   static volatile float burstAmp;
   static volatile long burstStart;
   static volatile long burstUntil;
   private static volatile AudioPlayback.Output output;

   private AudioPlayback() {
   }

   static float pan(Player me, double x, double z) {
      double dx = x - me.getX();
      double dz = z - me.getZ();
      double len = Math.hypot(dx, dz);
      if (len < 0.5) {
         return 0.0F;
      }

      double yaw = Math.toRadians(me.getYRot());
      double rx = -Math.cos(yaw);
      double rz = -Math.sin(yaw);
      return (float)Math.max(-1.0, Math.min(1.0, (dx * rx + dz * rz) / len)) * 0.85F;
   }

   private static AudioPlayback.Source source(String key) {
      AudioPlayback.Output o = output;
      if (o == null || !o.alive) {
         synchronized (AudioPlayback.class) {
            if (output == null || !output.alive) {
               if (output != null && System.currentTimeMillis() - output.failedAt < 5000L) {
                  return null;
               }

               output = new AudioPlayback.Output();
            }

            o = output;
         }

         if (!o.alive) {
            return null;
         }
      }

      return SOURCES.computeIfAbsent(key, k -> new AudioPlayback.Source());
   }

   static float burstLevel() {
      long now = System.currentTimeMillis();
      return now >= burstUntil ? 0.0F : burstAmp * (float)(burstUntil - now) / (float)Math.max(1L, burstUntil - burstStart);
   }

   public static void burst(float strength) {
      long now = System.currentTimeMillis();
      burstStart = now;
      burstUntil = now + (long)(300.0F + strength * 600.0F);
      burstAmp = strength;
   }

   public static void playProximity(UUID id, byte[] data) {
      if (!VoiceConfig.deafened) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.player != null) {
            Player p = mc.level.getPlayerByUUID(id);
            if (p != null && p != mc.player) {
               double d = p.getEyePosition().distanceTo(mc.player.getEyePosition());
               if (!(d > 48.0)) {
                  float vol = (float)Math.pow(1.0 - d / 48.0, 1.6) * VoiceConfig.voice * 1.3F;
                  AudioPlayback.Source s = source("v:" + id);
                  if (s != null) {
                     s.push(data, vol, pan(mc.player, p.getX(), p.getZ()), 1.0F, 0);
                  }
               }
            }
         }
      }
   }

   public static void playRadio(RadioAudioPayload pl) {
      if (!VoiceConfig.deafened) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.player != null) {
            Vec3 c = SableCompat.rendered(Vec3.atCenterOf(pl.sink()), 1.0F);
            double d = mc.player.getEyePosition().distanceTo(c);
            if (!(d > 24.0)) {
               float vol = (float)Math.pow(1.0 - d / 24.0, 1.6) * pl.gain() * 1.1F * VoiceConfig.radio;
               AudioPlayback.Source s = source("r:" + pl.sink().asLong() + ":" + pl.talker());
               if (s != null) {
                  s.push(pl.data(), vol, pan(mc.player, c.x, c.z), pl.quality(), pl.fm() ? 2 : 1);
               }
            }
         }
      }
   }

   public static void playHandheld(HandheldAudioPayload p) {
      if (!VoiceConfig.deafened) {
         AudioPlayback.Source s = source("h:" + p.talker());
         if (s != null) {
            s.push(p.data(), 0.9F * p.gain() * VoiceConfig.radio, 0.0F, p.quality(), p.fm() ? 2 : 1);
         }
      }
   }

   public static void playSelf(byte[] enc) {
      AudioPlayback.Source s = source("self");
      if (s != null) {
         s.push(enc, 0.8F * VoiceConfig.voice, 0.0F, 1.0F, 0);
      }
   }

   public static void tick(Minecraft mc) {
      if (mc.level != null && mc.player != null) {
         rainLevel = mc.level.getRainLevel(1.0F);
         thunderLevel = mc.level.getThunderLevel(1.0F);
         long now = System.currentTimeMillis();
         SOURCES.entrySet().removeIf(e -> now - e.getValue().last > 4000L);
      } else {
         stopAll();
      }
   }

   public static void stopSink(BlockPos pos) {
      String prefix = "r:" + pos.asLong() + ":";
      SOURCES.keySet().removeIf(k -> k.startsWith(prefix));
   }

   public static void stopAll() {
      SOURCES.clear();
      AudioPlayback.Output o = output;
      output = null;
      if (o != null) {
         o.close();
      }
   }

   static final class Output implements Runnable {
      SourceDataLine line;
      float outRate = 16000.0F;
      volatile boolean alive = true;
      long failedAt;
      private Thread thread;
      private float prevL;
      private float prevR;

      Output() {
         for (float r : AudioPlayback.OUT_RATES) {
            try {
               AudioFormat f = new AudioFormat(r, 16, 2, true, false);
               this.line = AudioSystem.getSourceDataLine(f);
               this.line.open(f, (int)(r * 4.0F * 0.1));
               this.outRate = r;
               break;
            } catch (Throwable t) {
               this.line = null;
            }
         }

         if (this.line == null) {
            this.alive = false;
            this.failedAt = System.currentTimeMillis();
         } else {
            this.thread = new Thread(this, "Airwaves-Audio");
            this.thread.setDaemon(true);
            this.thread.setPriority(9);
            this.thread.start();
         }
      }

      @Override
      public void run() {
         try {
            this.line.start();
            double step = 16000.0F / this.outRate;
            int outFrames = (int)Math.round(320.0 / step);
            float[] mix = new float[640];
            byte[] b = new byte[outFrames * 4];

            while (this.alive) {
               Arrays.fill(mix, 0.0F);

               for (AudioPlayback.Source s : AudioPlayback.SOURCES.values()) {
                  s.mixInto(mix);
               }

               for (int o = 0; o < outFrames; o++) {
                  double pos = o * step;
                  int i0 = (int)pos;
                  double t = pos - i0;
                  float l0 = i0 == 0 ? this.prevL : mix[2 * (i0 - 1)];
                  float r0 = i0 == 0 ? this.prevR : mix[2 * (i0 - 1) + 1];
                  float l1 = mix[2 * Math.min(i0, 319)];
                  float r1 = mix[2 * Math.min(i0, 319) + 1];
                  float L = (float)(l0 + (l1 - l0) * t);
                  float R = (float)(r0 + (r1 - r0) * t);
                  L /= 1.0F + Math.abs(L) * 0.6F;
                  R /= 1.0F + Math.abs(R) * 0.6F;
                  short sl = (short)(L * 30000.0F);
                  short sr = (short)(R * 30000.0F);
                  b[4 * o] = (byte)sl;
                  b[4 * o + 1] = (byte)(sl >> 8);
                  b[4 * o + 2] = (byte)sr;
                  b[4 * o + 3] = (byte)(sr >> 8);
               }

               this.prevL = mix[638];
               this.prevR = mix[639];
               this.line.write(b, 0, b.length);
            }
         } catch (Throwable var28) {
         } finally {
            try {
               this.line.stop();
               this.line.close();
            } catch (Throwable var27) {
            }

            this.alive = false;
            this.failedAt = System.currentTimeMillis();
         }
      }

      void close() {
         this.alive = false;
         if (this.thread != null) {
            this.thread.interrupt();
         }
      }
   }

   static final class Source {
      private final ArrayDeque<float[]> q = new ArrayDeque<>();
      private float[] pending = new float[640];
      private int pendingN = 0;
      volatile long last = System.currentTimeMillis();
      private boolean primed;
      private float lastL;
      private float lastR;
      private float hpX;
      private float hpY;
      private float lp;
      private float lp2;
      private float crackle;
      private final Random rnd = new Random();

      private float weatherNoise(int mode) {
         float am = mode == 1 ? 1.0F : 0.3F;
         float bl = AudioPlayback.burstLevel() * am;
         float y = (this.rnd.nextFloat() * 2.0F - 1.0F) * (AudioPlayback.rainLevel * 0.01F + AudioPlayback.thunderLevel * 0.025F) * am;
         if (this.rnd.nextFloat() < AudioPlayback.rainLevel * 0.003F) {
            this.crackle = (this.rnd.nextFloat() - 0.5F) * 0.35F * am;
         }

         if (bl > 0.0F && this.rnd.nextFloat() < 0.012F) {
            this.crackle = (this.rnd.nextFloat() - 0.5F) * 0.9F * bl;
         }

         y += this.crackle + (bl > 0.0F ? (this.rnd.nextFloat() * 2.0F - 1.0F) * bl * 0.25F : 0.0F);
         this.crackle *= 0.86F;
         return y;
      }

      void push(byte[] mu, float vol, float pan, float qual, int mode) {
         this.last = System.currentTimeMillis();
         float lg = pan <= 0.0F ? 1.0F : 1.0F - pan;
         float rg = pan >= 0.0F ? 1.0F : 1.0F + pan;
         float inv = 1.0F - qual;
         boolean drop = mode == 2 && qual < 0.3F && this.rnd.nextFloat() < (0.3F - qual) * 1.4F;

         for (byte b : mu) {
            float x = MuLaw.dec(b) / 32768.0F;
            if (mode == 1) {
               this.hpY = 0.96F * (this.hpY + x - this.hpX);
               this.hpX = x;
               x = this.hpY;
               this.lp = this.lp + 0.6F * (x - this.lp);
               this.lp2 = this.lp2 + 0.6F * (this.lp - this.lp2);
               x = this.lp2;
               x = (float)Math.tanh(x * 1.7F) * 0.8F;
               x = x * (0.45F + 0.55F * qual) + (this.rnd.nextFloat() * 2.0F - 1.0F) * (0.004F + inv * inv * 0.07F);
               x += this.weatherNoise(1);
            } else if (mode == 2) {
               this.lp = this.lp + 0.88F * (x - this.lp);
               x = drop ? this.lp * 0.1F : this.lp;
               x = x * (0.6F + 0.4F * qual) + (this.rnd.nextFloat() * 2.0F - 1.0F) * (inv * inv * inv * 0.06F);
               x += this.weatherNoise(2);
            }

            float y = x * vol;
            this.pending[2 * this.pendingN] = y * lg;
            this.pending[2 * this.pendingN + 1] = y * rg;
            if (++this.pendingN == 320) {
               synchronized (this.q) {
                  this.q.addLast(this.pending);

                  while (this.q.size() > 12) {
                     this.q.pollFirst();
                  }
               }

               this.pending = new float[640];
               this.pendingN = 0;
            }
         }
      }

      void mixInto(float[] mix) {
         float[] f;
         synchronized (this.q) {
            if (!this.primed && this.q.size() >= 3) {
               this.primed = true;
            }

            f = this.primed ? this.q.pollFirst() : null;
            if (f == null) {
               this.primed = false;
            }
         }

         if (f == null) {
            if (this.lastL != 0.0F || this.lastR != 0.0F) {
               for (int i = 0; i < 80; i++) {
                  float k = 1.0F - i / 80.0F;
                  mix[2 * i] = mix[2 * i] + this.lastL * k;
                  mix[2 * i + 1] = mix[2 * i + 1] + this.lastR * k;
               }

               this.lastL = this.lastR = 0.0F;
            }
         } else {
            boolean fadeIn = this.lastL == 0.0F && this.lastR == 0.0F;

            for (int i = 0; i < 320; i++) {
               float k = fadeIn && i < 80 ? i / 80.0F : 1.0F;
               mix[2 * i] = mix[2 * i] + f[2 * i] * k;
               mix[2 * i + 1] = mix[2 * i + 1] + f[2 * i + 1] * k;
            }

            this.lastL = f[638];
            this.lastR = f[639];
            if (this.lastL == 0.0F && this.lastR == 0.0F) {
               this.lastL = 1.0E-9F;
            }
         }
      }
   }
}
