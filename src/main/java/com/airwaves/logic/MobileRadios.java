/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Core radio simulation and shared gameplay logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.logic;

import com.airwaves.Tuning;
import com.airwaves.net.HandheldAudioPayload;
import com.airwaves.net.RadioSoundPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MobileRadios {
   public static final int RADIO = 1;
   public static final int RECEIVER = 2;
   public static final int CONSOLE = 3;
   public static final int SPEAKER = 4;
   public static final int TRANSMITTER = 5;
   public static final int MICROPHONE = 6;
   private static final Map<UUID, MobileRadios.Mobile> ALL = new HashMap<>();
   public static final double HEAR = 20.0;
   public static final double MIC_RANGE = 3.5;
   public static final double DESK_RANGE = 2.5;
   public static final double CONSOLE_FM = 320.0;
   public static final double CONSOLE_AM = 480.0;

   private MobileRadios() {
   }

   public static MobileRadios.Mobile update(UUID id, int kind, ServerLevel sl, Vec3 pos, int contraption) {
      MobileRadios.Mobile m = ALL.computeIfAbsent(id, k -> new MobileRadios.Mobile(k, kind));
      m.dim = sl.dimension();
      m.pos = pos;
      m.contraption = contraption;
      m.seen = sl.getGameTime();
      return m;
   }

   public static void remove(UUID id) {
      ALL.remove(id);
   }

   public static void clear() {
      ALL.clear();
   }

   private static List<MobileRadios.Mobile> in(ServerLevel sl) {
      long now = sl.getGameTime();
      ALL.values().removeIf(mx -> mx.dim == sl.dimension() && now - mx.seen > 60L);
      List<MobileRadios.Mobile> out = new ArrayList<>();

      for (MobileRadios.Mobile m : ALL.values()) {
         if (m.dim == sl.dimension()) {
            out.add(m);
         }
      }

      return out;
   }

   public static void addEmitters(ServerLevel sl, List<Emitter> em) {
      for (MobileRadios.Mobile m : in(sl)) {
         if (m.on && m.station()) {
            double r = m.kind == 3 ? (m.tuning.fm() ? 320.0 : 480.0) : m.range;
            if (!(r <= 0.0)) {
               BlockPos at = BlockPos.containing(m.pos);
               em.add(new Emitter(m.root, at, m.pos.y + 2.0, m.tuning, r, r >= 1.0E8 ? 1.0F : 0.95F, m.kind == 3 ? "Radio Console (moving)" : "Moving station"));
            }
         }
      }
   }

   public static void tune(ServerLevel sl, List<Emitter> em, List<BlockPos> jam) {
      for (MobileRadios.Mobile m : in(sl)) {
         if (m.listens() && m.on) {
            List<Emitter> others = new ArrayList<>();

            for (Emitter e : em) {
               if (!ownStation(m, e.root())) {
                  others.add(e);
               }
            }

            RadioNetwork.Best b = RadioNetwork.best(sl, BlockPos.containing(m.pos), m.pos.y + 1.0, m.tuning, m.rxGain, others, jam);
            m.root = b.root();
            m.q = b.q();
         } else {
            m.q = 0.0F;
         }
      }
   }

   private static boolean ownStation(MobileRadios.Mobile listener, long root) {
      for (MobileRadios.Mobile o : ALL.values()) {
         if (o.station() && o.contraption == listener.contraption && o.dim == listener.dim && o.root == root) {
            return true;
         }
      }

      return false;
   }

   public static long voice(ServerLevel sl, Vec3 at) {
      MobileRadios.Mobile best = null;
      double bd = Double.MAX_VALUE;

      for (MobileRadios.Mobile m : in(sl)) {
         double d = m.pos.distanceTo(at);
         MobileRadios.Mobile station = null;
         if (m.kind == 6 && d <= 3.5) {
            station = stationOf(m);
         } else if (m.kind == 5 && d <= 2.5) {
            station = m;
         } else if (m.kind == 3 && d <= 3.5) {
            station = m;
         }

         if (station != null && station.on && d < bd) {
            bd = d;
            best = station;
         }
      }

      return best == null ? 0L : best.root;
   }

   private static MobileRadios.Mobile stationOf(MobileRadios.Mobile mic) {
      MobileRadios.Mobile best = null;
      double bd = Double.MAX_VALUE;

      for (MobileRadios.Mobile o : ALL.values()) {
         if (o.kind == 5 && o.on && o.contraption == mic.contraption && o.dim == mic.dim) {
            double d = o.pos.distanceTo(mic.pos);
            if (d < bd) {
               bd = d;
               best = o;
            }
         }
      }

      return best;
   }

   public static MobileRadios.Mobile byRoot(ServerLevel sl, long root) {
      for (MobileRadios.Mobile m : in(sl)) {
         if (m.station() && m.root == root) {
            return m;
         }
      }

      return null;
   }

   private static void listeners(ServerLevel sl, long root, UUID talker, MobileRadios.Out out) {
      Map<ServerPlayer, float[]> best = new HashMap<>();
      Map<ServerPlayer, Boolean> fmOf = new HashMap<>();

      for (MobileRadios.Mobile m : in(sl)) {
         if (m.listens() && m.on && !(m.q < 0.04F) && m.root == root) {
            List<Vec3> mouths = new ArrayList<>();
            mouths.add(m.pos);

            for (MobileRadios.Mobile s : ALL.values()) {
               if (s.kind == 4 && s.contraption == m.contraption && s.dim == m.dim) {
                  mouths.add(s.pos);
               }
            }

            for (ServerPlayer p : sl.players()) {
               if (talker == null || !p.getUUID().equals(talker)) {
                  double d = Double.MAX_VALUE;

                  for (Vec3 v : mouths) {
                     d = Math.min(d, v.distanceTo(p.getEyePosition()));
                  }

                  if (!(d > 20.0)) {
                     float near = (float)Math.pow(1.0 - d / 20.0, 1.4);
                     float[] cur = best.get(p);
                     if (cur == null || near * m.q > cur[0] * cur[1]) {
                        best.put(p, new float[]{near, m.q});
                        fmOf.put(p, m.tuning.fm());
                     }
                  }
               }
            }
         }
      }

      for (Entry<ServerPlayer, float[]> e : best.entrySet()) {
         out.to(e.getKey(), e.getValue()[1], fmOf.get(e.getKey()), e.getValue()[0]);
      }
   }

   public static void deliverVoice(ServerLevel sl, long root, UUID talker, byte[] data, float gain) {
      listeners(
         sl,
         root,
         talker,
         (p, q, fm, near) -> PacketDistributor.sendToPlayer(p, new HandheldAudioPayload(talker, data, q, fm, gain * near * 1.1F), new CustomPacketPayload[0])
      );
   }

   public static void deliverSound(ServerLevel sl, long root, ResourceLocation id, float vol, float pitch, byte kind, long key, int offsetMs) {
      listeners(
         sl,
         root,
         null,
         (p, q, fm, near) -> PacketDistributor.sendToPlayer(
            p, new RadioSoundPayload(BlockPos.ZERO, true, id, vol * near, pitch, q, fm, kind, key ^ 2135587861L, offsetMs), new CustomPacketPayload[0]
         )
      );
   }

   public static final class Mobile {
      public final UUID id;
      public final int kind;
      public ResourceKey<Level> dim;
      public Vec3 pos = Vec3.ZERO;
      public Tuning tuning = Tuning.DEFAULT;
      public int contraption;
      public boolean on;
      public double range;
      public double rxGain = 1.0;
      public long seen;
      public long root;
      public float q;

      Mobile(UUID id, int kind) {
         this.id = id;
         this.kind = kind;
         this.root = id.getMostSignificantBits() ^ id.getLeastSignificantBits() ^ 1292571109L;
      }

      public boolean listens() {
         return this.kind == 1 || this.kind == 2 || this.kind == 3;
      }

      public boolean station() {
         return this.kind == 5 || this.kind == 3;
      }
   }

   private interface Out {
      void to(ServerPlayer var1, float var2, boolean var3, float var4);
   }
}
