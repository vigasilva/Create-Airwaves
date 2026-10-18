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
import com.airwaves.net.RadioSoundPayload;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class RadioSounds {
   private static final RandomSource RNG = RandomSource.create();
   private static final long TIMEOUT_MS = 3500L;
   private static final double MUSIC_RANGE = 72.0;
   private static final Map<String, RadioSounds.Track> MUSIC = new HashMap<>();

   private RadioSounds() {
   }

   public static void play(RadioSoundPayload p) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         String key = (p.handheld() ? "h" : Long.toString(p.sink().asLong())) + ":" + p.key() + ":" + p.sound();
         if (p.kind() == 2) {
            stop(key);
         } else if (VoiceConfig.deafened) {
            stop(key);
         } else {
            float q = p.quality();
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            Vec3 at = null;
            if (!p.handheld()) {
               at = SableCompat.rendered(Vec3.atCenterOf(p.sink()), 1.0F);
               x = at.x;
               y = at.y;
               z = at.z;
            }

            if (p.kind() == 1) {
               music(mc, p, key, at, q);
            } else if (!(q < 0.04F)) {
               if (at == null || !(mc.player.getEyePosition().distanceTo(at) > 32.0)) {
                  if (!(RNG.nextFloat() > 0.3F + q)) {
                     SimpleSoundInstance inst = new SimpleSoundInstance(
                        p.sound(),
                        SoundSource.BLOCKS,
                        volume(p, q),
                        pitch(p, q),
                        RNG,
                        false,
                        0,
                        p.handheld() ? Attenuation.NONE : Attenuation.LINEAR,
                        x,
                        y,
                        z,
                        p.handheld()
                     );
                     mc.getSoundManager().play(inst);
                     if (q < 0.55F) {
                        AudioPlayback.burst(0.12F * (1.0F - q));
                     }
                  }
               }
            }
         }
      }
   }

   private static void music(Minecraft mc, RadioSoundPayload p, String key, Vec3 at, float q) {
      RadioSounds.Track t = MUSIC.get(key);
      boolean inRange = q >= 0.04F && (at == null || mc.player.getEyePosition().distanceTo(at) <= 72.0);
      if (!inRange) {
         stop(key);
      } else if (t != null) {
         t.seen = System.currentTimeMillis();
      } else {
         t = new RadioSounds.Track();
         t.seen = System.currentTimeMillis();
         t.at = at;
         t.inst = new RadioSounds.SeekingSound(p.sound(), volume(p, q), pitch(p, q), at, p.handheld(), p.offsetMs());
         MUSIC.put(key, t);
         mc.getSoundManager().play(t.inst);
         if (q < 0.55F) {
            AudioPlayback.burst(0.12F * (1.0F - q));
         }
      }
   }

   private static float volume(RadioSoundPayload p, float q) {
      return Math.max(0.02F, p.volume() * VoiceConfig.radio * (0.35F + 0.65F * q));
   }

   private static float pitch(RadioSoundPayload p, float q) {
      return p.pitch() * (p.fm() ? 1.0F : 1.0F + (RNG.nextFloat() - 0.5F) * 0.05F * (1.0F - q));
   }

   private static void stop(String key) {
      RadioSounds.Track old = MUSIC.remove(key);
      if (old != null) {
         Minecraft.getInstance().getSoundManager().stop(old.inst);
      }
   }

   public static void tick(Minecraft mc) {
      if (mc.level != null && mc.player != null) {
         long now = System.currentTimeMillis();
         Vec3 eye = mc.player.getEyePosition();
         Iterator<Entry<String, RadioSounds.Track>> it = MUSIC.entrySet().iterator();

         while (it.hasNext()) {
            RadioSounds.Track t = it.next().getValue();
            if (now - t.seen > 3500L || t.at != null && eye.distanceTo(t.at) > 80.0) {
               mc.getSoundManager().stop(t.inst);
               it.remove();
            }
         }
      } else {
         stopAll();
      }
   }

   public static void stopAt(BlockPos pos) {
      String prefix = pos.asLong() + ":";
      Minecraft mc = Minecraft.getInstance();
      Iterator<Entry<String, RadioSounds.Track>> it = MUSIC.entrySet().iterator();

      while (it.hasNext()) {
         Entry<String, RadioSounds.Track> e = it.next();
         if (e.getKey().startsWith(prefix)) {
            mc.getSoundManager().stop(e.getValue().inst);
            it.remove();
         }
      }

      AudioPlayback.stopSink(pos);
   }

   public static void stopAll() {
      Minecraft mc = Minecraft.getInstance();

      for (RadioSounds.Track t : MUSIC.values()) {
         mc.getSoundManager().stop(t.inst);
      }

      MUSIC.clear();
   }

   private static final class SeekingSound extends SimpleSoundInstance {
      private final int offsetMs;

      SeekingSound(ResourceLocation id, float vol, float pitch, Vec3 at, boolean handheld, int offsetMs) {
         super(
            id,
            SoundSource.RECORDS,
            vol,
            pitch,
            RadioSounds.RNG,
            false,
            0,
            handheld ? Attenuation.NONE : Attenuation.LINEAR,
            at == null ? 0.0 : at.x,
            at == null ? 0.0 : at.y,
            at == null ? 0.0 : at.z,
            handheld
         );
         this.offsetMs = offsetMs;
      }

      public CompletableFuture<AudioStream> getStream(SoundBufferLibrary lib, Sound sound, boolean looping) {
         CompletableFuture<AudioStream> f = lib.getStream(sound.getPath(), looping);
         return this.offsetMs < 250 ? f : f.thenApply(s -> {
            skip(s, this.offsetMs);
            return (AudioStream)s;
         });
      }

      private static void skip(AudioStream s, int ms) {
         try {
            AudioFormat fmt = s.getFormat();
            long bytes = (long)(ms / 1000.0 * fmt.getSampleRate()) * fmt.getChannels() * Math.max(1, fmt.getSampleSizeInBits() / 8);

            while (bytes > 0L) {
               ByteBuffer b = s.read((int)Math.min(bytes, 32768L));
               if (b == null || b.remaining() == 0) {
                  break;
               }

               bytes -= b.remaining();
            }
         } catch (IOException | RuntimeException var6) {
         }
      }
   }

   private static final class Track {
      SoundInstance inst;
      long seen;
      Vec3 at;
   }
}
