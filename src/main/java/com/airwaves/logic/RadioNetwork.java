/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Core radio simulation and shared gameplay logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.logic;

import com.airwaves.ModRegistry;
import com.airwaves.Tuning;
import com.airwaves.api.RadioStation;
import com.airwaves.api.event.CollectStationsEvent;
import com.airwaves.api.event.RadioNetworkTickEvent;
import com.airwaves.api.event.VoiceBroadcastEvent;
import com.airwaves.be.CableNode;
import com.airwaves.be.CircuitBoxBE;
import com.airwaves.be.DimensionalRelayBE;
import com.airwaves.be.JammerBE;
import com.airwaves.be.MicrophoneBE;
import com.airwaves.be.RadioBE;
import com.airwaves.be.RadioConsoleBE;
import com.airwaves.be.ReceiverBE;
import com.airwaves.be.RelayBE;
import com.airwaves.be.SpeakerBE;
import com.airwaves.be.TapePlayerBE;
import com.airwaves.be.TransmitterBE;
import com.airwaves.be.TunableBE;
import com.airwaves.block.TransmitterBlock;
import com.airwaves.compat.SableCompat;
import com.airwaves.item.PortableRadioItem;
import com.airwaves.net.CablePulsePayload;
import com.airwaves.net.HandheldAudioPayload;
import com.airwaves.net.HandheldStatePayload;
import com.airwaves.net.RadioAudioPayload;
import com.airwaves.net.RadioSoundPayload;
import com.airwaves.net.StaticBurstPayload;
import com.airwaves.net.StationInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RadioNetwork {
   /** Per-dimension live radio-device indexes. */
   private static final Map<ResourceKey<Level>, RadioNetwork.Lvl> LEVELS = new HashMap<>();
   /** State for players holding a portable radio. */
   private static final Map<UUID, RadioNetwork.HH> HANDHELD = new HashMap<>();
   public static final double MIC_RANGE = 3.5;
   public static final double DESK_RANGE = 2.5;
   public static final double JAM_RADIUS = 48.0;
   private static final Map<Long, long[]> TERRAIN = new ConcurrentHashMap<>();
   public static final double TAPE_VOICE_RANGE = 6.0;
   public static final double SOUND_RANGE = 8.0;
   private static final Map<Long, long[]> SOUND_RATE = new HashMap<>();
   public static final double WALKIE_FM = 320.0;
   public static final double WALKIE_AM = 480.0;
   private static final Map<UUID, Long> WALKIE_SHOWN = new HashMap<>();
   private static final Map<Long, RadioNetwork.Talk> TALKING = new HashMap<>();
   public static final long TALK_WINDOW_MS = 150L;

   private RadioNetwork() {
   }

   /** Returns (or lazily creates) the device index for a world dimension. */
   private static RadioNetwork.Lvl lvl(Level level) {
      return LEVELS.computeIfAbsent(level.dimension(), dimension -> new RadioNetwork.Lvl());
   }

   public static long crossRoot(long root, ResourceKey<Level> from) {
      return root ^ from.location().hashCode() * -7046029254386353131L ^ 25214903917L;
   }

   public static String dimName(ResourceKey<Level> k) {
      if (k == Level.OVERWORLD) {
         return "the Overworld";
      }

      if (k == Level.NETHER) {
         return "the Nether";
      }

      if (k == Level.END) {
         return "the End";
      }

      String p = k.location().getPath().replace('_', ' ');
      return p.isEmpty() ? k.location().toString() : Character.toUpperCase(p.charAt(0)) + p.substring(1);
   }

   public static void clear() {
      LEVELS.clear();
      HANDHELD.clear();
      TALKING.clear();
      MobileRadios.clear();
   }

   /** Adds a loaded radio-related block entity to its dimension's indexes. */
   public static void register(BlockEntity blockEntity) {
      RadioNetwork.Lvl dimensionNetwork = lvl(blockEntity.getLevel());
      if (blockEntity instanceof TransmitterBE transmitter) {
         dimensionNetwork.tx.add(transmitter);
      } else if (blockEntity instanceof RelayBE relay) {
         dimensionNetwork.relays.add(relay);
      } else if (blockEntity instanceof RadioBE || blockEntity instanceof ReceiverBE || blockEntity instanceof RadioConsoleBE || blockEntity instanceof CircuitBoxBE) {
         dimensionNetwork.sinks.add((TunableBE)blockEntity);
      } else if (blockEntity instanceof MicrophoneBE microphone) {
         dimensionNetwork.mics.add(microphone);
      } else if (blockEntity instanceof JammerBE jammer) {
         dimensionNetwork.jammers.add(jammer);
      } else if (blockEntity instanceof SpeakerBE speaker) {
         dimensionNetwork.speakers.add(speaker);
      } else if (blockEntity instanceof DimensionalRelayBE dimensionalRelay) {
         dimensionNetwork.links.add(dimensionalRelay);
      }
   }

   /** Removes an unloading radio-related block entity from its indexes. */
   public static void unregister(BlockEntity blockEntity) {
      if (blockEntity.getLevel() != null) {
         RadioNetwork.Lvl dimensionNetwork = lvl(blockEntity.getLevel());
         if (blockEntity instanceof TransmitterBE transmitter) {
            dimensionNetwork.tx.remove(transmitter);
         } else if (blockEntity instanceof RelayBE relay) {
            dimensionNetwork.relays.remove(relay);
         } else if (blockEntity instanceof TunableBE tunableSink) {
            dimensionNetwork.sinks.remove(tunableSink);
         } else if (blockEntity instanceof MicrophoneBE microphone) {
            dimensionNetwork.mics.remove(microphone);
         } else if (blockEntity instanceof JammerBE jammer) {
            dimensionNetwork.jammers.remove(jammer);
         } else if (blockEntity instanceof SpeakerBE speaker) {
            dimensionNetwork.speakers.remove(speaker);
         } else if (blockEntity instanceof DimensionalRelayBE dimensionalRelay) {
            dimensionNetwork.links.remove(dimensionalRelay);
         }
      }
   }

   /**
    * Computes signal quality after range and receiver gain, before terrain
    * attenuation. FM falls off more gently near its useful range than AM.
    */
   public static float quality(double distance, double nominalRange, boolean frequencyModulation, float baseQuality, double receiverGain) {
      double effectiveRange = nominalRange * receiverGain;
      if (effectiveRange <= 0.0) {
         return 0.0F;
      }

      double rangeFraction = distance / effectiveRange;
      if (rangeFraction >= 1.0) {
         return 0.0F;
      }

      double noise = frequencyModulation ? Math.pow(rangeFraction, 3.2) : Math.pow(rangeFraction, 1.35);
      double quality = (1.0 - noise) * baseQuality * (frequencyModulation ? 1.0 : 0.88);
      return (float)Mth.clamp(quality, 0.0, 1.0);
   }

   public static float terrain(Level lv, BlockPos a, double ay, BlockPos b, double by, boolean fm) {
      double dx = b.getX() - a.getX();
      double dz = b.getZ() - a.getZ();
      double len = Math.sqrt(dx * dx + dz * dz);
      if (len < 48.0) {
         return 1.0F;
      }

      long key = a.asLong() * 1000003L
         ^ b.asLong()
         ^ (fm ? 25214903917L : 0L)
         ^ (long)(ay * 4.0) << 42
         ^ (long)(by * 4.0) << 21
         ^ lv.dimension().location().hashCode();
      long now = lv.getGameTime();
      long[] c = TERRAIN.get(key);
      if (c != null && now - c[1] < 200L && now >= c[1]) {
         return Float.intBitsToFloat((int)c[0]);
      }

      float v = terrainUncached(lv, a, ay, b, by, fm, dx, dz, len);
      if (TERRAIN.size() > 20000) {
         TERRAIN.clear();
      }

      TERRAIN.put(key, new long[]{Float.floatToIntBits(v), now});
      return v;
   }

   private static float terrainUncached(Level lv, BlockPos a, double ay, BlockPos b, double by, boolean fm, double dx, double dz, double len) {
      int n = (int)Math.min(24.0, Math.max(6.0, len / 64.0));
      double worst = 0.0;
      MutableBlockPos m = new MutableBlockPos();

      for (int i = 1; i < n; i++) {
         double u = (double)i / n;
         int x = Mth.floor(a.getX() + dx * u);
         int z = Mth.floor(a.getZ() + dz * u);
         m.set(x, 64, z);
         if (lv.isLoaded(m)) {
            int h = lv.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            worst = Math.max(worst, h - (ay + (by - ay) * u));
         }
      }

      return worst <= 0.0 ? 1.0F : (float)(fm ? Math.max(0.3, 1.0 - worst / 45.0) : Math.max(0.75, 1.0 - worst / 140.0));
   }

   public static RadioNetwork.Best best(Level lv, BlockPos pos, double rxTop, Tuning t, double gain, List<Emitter> em, List<BlockPos> jam) {
      return best(lv, pos, rxTop, t, gain, em, jam, null);
   }

   public static RadioNetwork.Best best(Level lv, BlockPos pos, double rxTop, Tuning t, double gain, List<Emitter> em, List<BlockPos> jam, Float rxBeam) {
      float best = 0.0F;
      long root = 0L;
      double night = t.fm() ? 1.0 : WeatherUtil.amNightBoost(lv);

      for (Emitter e : em) {
         if (e.tuning().matches(t)) {
            if (e.creative()) {
               if (e.baseQ() > best) {
                  best = e.baseQ();
                  root = e.root();
               }
            } else {
               double g = rxBeam == null ? gain : gain * AntennaUtil.pattern(rxBeam, e.pos().getX() - pos.getX(), e.pos().getZ() - pos.getZ());
               float q = quality(Math.sqrt(pos.distSqr(e.pos())), e.rangeTo(pos) * night, t.fm(), e.baseQ(), g);
               if (!(q <= best)) {
                  q *= jamming(e.pos(), pos, jam);
                  if (!(q <= best)) {
                     q *= terrain(lv, e.pos(), e.topY(), pos, rxTop, t.fm());
                     if (q > best) {
                        best = q;
                        root = e.root();
                     }
                  }
               }
            }
         }
      }

      if (best > 0.0F) {
         best *= WeatherUtil.factor(lv, pos, t.fm());
      }

      return new RadioNetwork.Best(best, root);
   }

   public static float jamming(BlockPos from, BlockPos to, List<BlockPos> jam) {
      if (jam.isEmpty()) {
         return 1.0F;
      }

      float f = 1.0F;

      for (BlockPos j : jam) {
         double d = segmentDistance(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), j.getX(), j.getY(), j.getZ());
         if (d < 48.0) {
            f *= (float)(0.08 + 0.92 * Math.pow(d / 48.0, 1.5));
         }
      }

      return f;
   }

   static double segmentDistance(double ax, double ay, double az, double bx, double by, double bz, double px, double py, double pz) {
      double dx = bx - ax;
      double dy = by - ay;
      double dz = bz - az;
      double l2 = dx * dx + dy * dy + dz * dz;
      double u = l2 < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((px - ax) * dx + (py - ay) * dy + (pz - az) * dz) / l2));
      double cx = ax + dx * u - px;
      double cy = ay + dy * u - py;
      double cz = az + dz * u - pz;
      return Math.sqrt(cx * cx + cy * cy + cz * cz);
   }

   public static Emitter seek(ServerLevel lv, BlockPos pos, double rxTop, Tuning cur, double gain) {
      RadioNetwork.Lvl l = LEVELS.get(lv.dimension());
      if (l == null) {
         return null;
      }

      Emitter first = null;
      Emitter next = null;

      for (Emitter e : l.emitters) {
         if (e.tuning().fm() == cur.fm() && !(best(lv, pos, rxTop, e.tuning(), gain, List.of(e), l.jam).q() < 0.05F)) {
            double f = e.tuning().freq();
            if (first == null || f < first.tuning().freq()) {
               first = e;
            }

            if (f > cur.freq() + 0.005 && (next == null || f < next.tuning().freq())) {
               next = e;
            }
         }
      }

      return next != null ? next : first;
   }

   public static void tick(ServerLevel level) {
      RadioNetwork.Lvl l = lvl(level);
      List<BlockEntity> nodes = new ArrayList<>(l.tx);
      nodes.addAll(l.relays);
      nodes.addAll(l.sinks);
      nodes.addAll(l.mics);
      nodes.addAll(l.speakers);
      nodes.addAll(l.jammers);

      for (BlockEntity be : nodes) {
         if (be instanceof CableNode n && !n.links().isEmpty() && SableCompat.onShip(level, be.getBlockPos())) {
            Cables.checkStretch(level, be.getBlockPos(), n);
         }
      }

      for (TransmitterBE t : l.tx) {
         t.networkRefresh();
      }

      for (RelayBE r : l.relays) {
         r.networkRefresh();
      }

      for (TunableBE s : l.sinks) {
         s.refresh();
      }

      Set<TransmitterBE> wired = new HashSet<>();

      for (MicrophoneBE m : l.mics) {
         List<Cables.Hit<TransmitterBE>> t = Cables.reach(level, m.getBlockPos(), TransmitterBE.class);
         m.feeds = t.isEmpty() ? null : t.get(0).target();
         m.path = t.isEmpty() ? List.of() : t.get(0).path();
         m.line = t.isEmpty() ? 1.0F : t.get(0).lineQuality(level);
         if (m.feeds != null) {
            wired.add(m.feeds);
         }
      }

      for (TransmitterBE t : l.tx) {
         t.wired = wired.contains(t);
      }

      SoundCapture.pollJukeboxes(level, l.mics);
      Set<SpeakerBE> driven = new HashSet<>();

      for (TunableBE s : l.sinks) {
         if (s instanceof ReceiverBE rx) {
            List<Cables.Hit<SpeakerBE>> h = Cables.reach(level, rx.getBlockPos(), SpeakerBE.class);
            rx.speakers = Cables.targets(h);
            rx.lineQ.clear();

            for (Cables.Hit<SpeakerBE> x : h) {
               rx.lineQ.put(x.target(), x.lineQuality(level));
            }

            List<Cables.Seg> segs = new ArrayList<>();

            for (Cables.Hit<SpeakerBE> x : h) {
               for (Cables.Seg sg : x.path()) {
                  if (!segs.contains(sg)) {
                     segs.add(sg);
                  }
               }
            }

            rx.paths = segs;
            driven.addAll(rx.speakers);
         }
      }

      for (SpeakerBE sp : l.speakers) {
         if (!driven.contains(sp)) {
            sp.setState(false, false, 0.0F, false);
         }
      }

      List<Emitter> em = new ArrayList<>();

      for (TransmitterBE t : l.tx) {
         if (t.active) {
            em.add(new Emitter(t.getBlockPos().asLong(), t.radioPos(), t.antennaTop(), t.tuning, t.range, t.baseQ, t.stationLabel(), t.beam));
         }
      }

      for (RadioStation st : ((CollectStationsEvent)NeoForge.EVENT_BUS.post(new CollectStationsEvent(level))).stations()) {
         em.add(st.toEmitter());
      }

      MobileRadios.addEmitters(level, em);
      List<BlockPos> jam = new ArrayList<>();

      for (JammerBE j : l.jammers) {
         if (j.refresh()) {
            jam.add(SableCompat.globalPos(level, j.getBlockPos()));
         }
      }

      l.jam = jam;

      for (DimensionalRelayBE d : l.links) {
         d.refresh();
      }

      List<DimensionalRelayBE> links = new ArrayList<>();

      for (DimensionalRelayBE d : l.links) {
         if (d.active) {
            links.add(d);
         }
      }

      l.linkActive = !links.isEmpty();
      Set<Long> imported = new HashSet<>();
      Map<DimensionalRelayBE, List<String>> partners = new HashMap<>();
      int bring = 0;
      if (!links.isEmpty()) {
         for (Entry<ResourceKey<Level>, RadioNetwork.Lvl> o : LEVELS.entrySet()) {
            if (o.getKey() != level.dimension() && o.getValue().linkActive) {
               for (DimensionalRelayBE d : links) {
                  partners.computeIfAbsent(d, k -> new ArrayList<>()).add(dimName(o.getKey()));
               }

               for (RadioNetwork.Export x : o.getValue().exports) {
                  long r = crossRoot(x.root(), o.getKey());
                  imported.add(r);
                  bring++;

                  for (DimensionalRelayBE d : links) {
                     em.add(
                        new Emitter(
                           r, d.radioPos(), d.antennaTop(), x.tuning(), d.range(x.tuning().fm()), Math.min(1.0F, (0.5F + 0.5F * x.q()) * 0.95F), x.name()
                        )
                     );
                  }
               }
            }
         }
      }

      Set<RelayBE> done = new HashSet<>();

      for (int pass = 0; pass < 4; pass++) {
         List<Emitter> add = new ArrayList<>();

         for (RelayBE r : l.relays) {
            if (r.active && !done.contains(r)) {
               float best = 0.0F;
               Emitter src = null;

               for (Emitter e : em) {
                  if (e.tuning().matches(r.tuning)) {
                     BlockPos rp = r.radioPos();
                     float q = quality(Math.sqrt(rp.distSqr(e.pos())), e.rangeTo(rp), r.tuning.fm(), e.baseQ(), r.rxGain())
                        * terrain(level, e.pos(), e.topY(), rp, r.antennaTop(), r.tuning.fm())
                        * jamming(e.pos(), rp, jam);
                     if (q > best) {
                        best = q;
                        src = e;
                     }
                  }
               }

               if (src != null && best >= 0.2F) {
                  done.add(r);
                  r.relaying = true;
                  add.add(new Emitter(src.root(), r.radioPos(), r.antennaTop(), r.tuning, r.range, Math.min(1.0F, (0.5F + 0.5F * best) * r.antQ), src.name()));
               }
            }
         }

         if (add.isEmpty()) {
            break;
         }

         em.addAll(add);
      }

      for (RelayBE r : l.relays) {
         if (!done.contains(r)) {
            r.relaying = false;
         }
      }

      Map<Long, RadioNetwork.Export> out = new HashMap<>();
      Map<DimensionalRelayBE, Integer> sent = new HashMap<>();

      for (DimensionalRelayBE d : links) {
         BlockPos dp = d.radioPos();

         for (Emitter e : em) {
            if (!imported.contains(e.root())) {
               float q = e.creative()
                  ? e.baseQ()
                  : quality(Math.sqrt(dp.distSqr(e.pos())), e.rangeTo(dp), e.tuning().fm(), e.baseQ(), d.rxGain())
                     * terrain(level, e.pos(), e.topY(), dp, d.antennaTop(), e.tuning().fm())
                     * jamming(e.pos(), dp, jam);
               if (!(q < 0.2F)) {
                  sent.merge(d, 1, Integer::sum);
                  RadioNetwork.Export old = out.get(e.root());
                  if (old == null || old.q() < q) {
                     out.put(e.root(), new RadioNetwork.Export(e.root(), e.tuning(), q, e.name()));
                  }
               }
            }
         }
      }

      l.exports = new ArrayList<>(out.values());

      for (DimensionalRelayBE d : l.links) {
         List<String> p = d.active ? partners.getOrDefault(d, List.of()) : List.of();
         int s1 = d.active ? sent.getOrDefault(d, 0) : 0;
         int b1 = d.active ? bring : 0;
         if (!p.equals(d.linked) || s1 != d.sending || b1 != d.bringing) {
            d.linked = new ArrayList<>(p);
            d.sending = s1;
            d.bringing = b1;
            d.sendData();
         }
      }

      l.emitters = new ArrayList<>(em);

      for (TunableBE s : l.sinks) {
         s.updateSignal(em, jam);
      }

      for (ServerPlayer p : level.players()) {
         updateHandheld(p, level, em, jam, false);
      }

      MobileRadios.tune(level, em, jam);
      NeoForge.EVENT_BUS.post(new RadioNetworkTickEvent(level, l.emitters));
   }

   public static void updateHandheld(ServerPlayer p, ServerLevel level, List<Emitter> em, List<BlockPos> jam, boolean force) {
      ItemStack st = findRadio(p);
      RadioNetwork.HH old = HANDHELD.get(p.getUUID());
      if (st == null) {
         if (old != null || force) {
            PacketDistributor.sendToPlayer(p, new HandheldStatePayload(false, false, 0.0F, false, ""), new CustomPacketPayload[0]);
         }

         HANDHELD.remove(p.getUUID());
      } else {
         Tuning t = (Tuning)st.getOrDefault((DataComponentType)ModRegistry.TUNING.get(), Tuning.DEFAULT);
         RadioNetwork.Best b = best(level, p.blockPosition(), p.getEyeY(), t, 0.9, em, jam);
         RadioNetwork.HH nw = new RadioNetwork.HH(b.root(), b.q(), t.fm(), b.q() > 0.04F);
         if (force || old == null || old.carrier() != nw.carrier() || old.fm() != nw.fm() || Math.round(old.q() * 50.0F) != Math.round(nw.q() * 50.0F)) {
            PacketDistributor.sendToPlayer(p, new HandheldStatePayload(true, nw.carrier(), nw.q(), nw.fm(), t.display()), new CustomPacketPayload[0]);
         }

         HANDHELD.put(p.getUUID(), nw);
      }
   }

   public static float refreshHandheld(ServerPlayer p) {
      if (p.level() instanceof ServerLevel sl) {
         RadioNetwork.Lvl var4 = LEVELS.get(sl.dimension());
         updateHandheld(p, sl, var4 == null ? List.of() : var4.emitters, var4 == null ? List.of() : var4.jam, true);
         RadioNetwork.HH h = HANDHELD.get(p.getUUID());
         return h == null ? 0.0F : h.q();
      } else {
         return 0.0F;
      }
   }

   private static ItemStack findRadio(ServerPlayer p) {
      ItemStack off = p.getOffhandItem();
      if (off.getItem() instanceof PortableRadioItem && PortableRadioItem.isOn(off)) {
         return off;
      }

      for (int i = 0; i < 9; i++) {
         ItemStack s = p.getInventory().getItem(i);
         if (s.getItem() instanceof PortableRadioItem && PortableRadioItem.isOn(s)) {
            return s;
         }
      }

      return null;
   }

   public static void onVoice(ServerPlayer p, byte[] data) {
      if (p.level() instanceof ServerLevel sl) {
         RadioNetwork.Lvl l = lvl(sl);

         for (MicrophoneBE m : l.mics) {
            if (m instanceof TapePlayerBE tp
               && tp.isRecording()
               && !tp.isRemoved()
               && SableCompat.distance(sl, m.getBlockPos().getCenter(), p.getEyePosition()) <= 6.0) {
               tp.captureVoice(data);
            }
         }

         if (!walkieTalkie(p, sl, l, data)) {
            voiceFromPoint(sl, p.getEyePosition(), p.getUUID(), data);
         }
      }
   }

   public static void voiceFromPoint(ServerLevel sl, Vec3 at, UUID talker, byte[] data) {
      RadioNetwork.Lvl l = lvl(sl);
      TransmitterBE src = null;
      float gain = 0.0F;
      double bd = 3.5;

      for (MicrophoneBE m : l.mics) {
         if (!(m instanceof TapePlayerBE) && m.feeds != null && m.feeds.active && !m.feeds.isRemoved()) {
            double d = SableCompat.distance(sl, m.getBlockPos().getCenter(), at);
            if (d <= bd) {
               bd = d;
               src = m.feeds;
               gain = (float)(1.0 - 0.7 * (d / 3.5)) * m.line;
            }
         }
      }

      if (src != null) {
         for (MicrophoneBE m : l.mics) {
            if (m.feeds == src && !(m instanceof TapePlayerBE) && SableCompat.distance(sl, m.getBlockPos().getCenter(), at) <= 3.5) {
               long now = sl.getGameTime();
               if (now - m.lastPulse >= 6L) {
                  m.lastPulse = now;
                  pulse(sl, m.path);
               }
               break;
            }
         }
      }

      if (src == null) {
         double bb = 2.5;

         for (TransmitterBE t : l.tx) {
            if (t.active && !t.wired) {
               double d = SableCompat.distance(sl, t.getBlockPos().getCenter(), at);
               if (d <= bb) {
                  bb = d;
                  src = t;
                  gain = (float)(0.6 * (1.0 - 0.5 * d / 2.5));
               }
            }
         }
      }

      if (src == null) {
         long mr = MobileRadios.voice(sl, at);
         if (mr != 0L) {
            broadcast(sl, mr, talker, data, 0.9F);
         }
      } else {
         src.onAir(sl);
         broadcast(sl, src.getBlockPos().asLong(), talker, data, gain);
      }
   }

   public static boolean broadcast(ServerLevel sl, long root, UUID talker, byte[] data, float gain) {
      RadioNetwork.Lvl l = LEVELS.get(sl.dimension());
      if (l == null) {
         return false;
      }

      if (((VoiceBroadcastEvent)NeoForge.EVENT_BUS.post(new VoiceBroadcastEvent(sl, root, talker, data))).isCanceled()) {
         return false;
      }

      deliverAll(sl, l, root, talker, data, gain);
      if (l.linkActive) {
         long cross = crossRoot(root, sl.dimension());

         for (Entry<ResourceKey<Level>, RadioNetwork.Lvl> o : LEVELS.entrySet()) {
            if (o.getKey() != sl.dimension() && o.getValue().linkActive) {
               ServerLevel ol = sl.getServer().getLevel(o.getKey());
               if (ol != null) {
                  deliverAll(ol, o.getValue(), cross, talker, data, gain);
               }
            }
         }
      }

      for (Entry<UUID, RadioNetwork.HH> e : HANDHELD.entrySet()) {
         RadioNetwork.HH h = e.getValue();
         if (h.carrier() && !e.getKey().equals(talker) && (h.root() == root || l.linkActive && h.root() == crossRoot(root, sl.dimension()))) {
            ServerPlayer lp = sl.getServer().getPlayerList().getPlayer(e.getKey());
            if (lp != null) {
               PacketDistributor.sendToPlayer(lp, new HandheldAudioPayload(talker, data, h.q(), h.fm(), gain), new CustomPacketPayload[0]);
            }
         }
      }

      MobileRadios.deliverVoice(sl, root, talker, data, gain);
      return true;
   }

   public static void onWorldSound(ServerLevel sl, Vec3 at, ResourceLocation id, float vol, float pitch) {
      RadioNetwork.Lvl l = LEVELS.get(sl.dimension());
      if (l != null) {
         long moving = MobileRadios.voice(sl, at);
         if (moving != 0L) {
            long[] rate = SOUND_RATE.computeIfAbsent(moving, k -> new long[2]);
            long t0 = sl.getGameTime();
            if (rate[0] != t0 / 20L) {
               rate[0] = t0 / 20L;
               rate[1] = 0L;
            }

            if (++rate[1] <= 24L) {
               broadcastSound(sl, moving, id, vol * 0.8F, pitch, (byte)0, 0L);
            }
         }

         if (!l.mics.isEmpty()) {
            Map<TransmitterBE, Double> best = new HashMap<>();

            for (MicrophoneBE m : l.mics) {
               if (!m.isRemoved()) {
                  double d = SableCompat.distance(sl, m.getBlockPos().getCenter(), at);
                  if (!(d > 8.0)) {
                     if (m instanceof TapePlayerBE tp) {
                        tp.capture(id, vol, pitch, (byte)0);
                     } else if (m.feeds != null && m.feeds.active && !m.feeds.isRemoved()) {
                        Double o = best.get(m.feeds);
                        if (o == null || d < o) {
                           best.put(m.feeds, d);
                        }
                     }
                  }
               }
            }

            long now = sl.getGameTime();

            for (Entry<TransmitterBE, Double> e : best.entrySet()) {
               TransmitterBE t = e.getKey();
               long[] rate = SOUND_RATE.computeIfAbsent(t.getBlockPos().asLong(), k -> new long[2]);
               if (rate[0] != now / 20L) {
                  rate[0] = now / 20L;
                  rate[1] = 0L;
               }

               if (++rate[1] <= 24L) {
                  float gain = (float)(1.0 - 0.6 * (e.getValue() / 8.0));
                  t.onAir(sl);
                  broadcastSound(sl, t.getBlockPos().asLong(), id, vol * gain, pitch, (byte)0, 0L);
               }
            }
         }
      }
   }

   public static void broadcastSound(ServerLevel sl, long root, ResourceLocation id, float vol, float pitch, byte kind, long key) {
      broadcastSound(sl, root, id, vol, pitch, kind, key, 0);
   }

   public static void broadcastSound(ServerLevel sl, long root, ResourceLocation id, float vol, float pitch, byte kind, long key, int offsetMs) {
      RadioNetwork.Lvl l = LEVELS.get(sl.dimension());
      if (l != null) {
         deliverSound(sl, l, root, id, vol, pitch, kind, key, offsetMs);
         if (l.linkActive) {
            long cross = crossRoot(root, sl.dimension());

            for (Entry<ResourceKey<Level>, RadioNetwork.Lvl> o : LEVELS.entrySet()) {
               if (o.getKey() != sl.dimension() && o.getValue().linkActive) {
                  ServerLevel ol = sl.getServer().getLevel(o.getKey());
                  if (ol != null) {
                     deliverSound(ol, o.getValue(), cross, id, vol, pitch, kind, key, offsetMs);
                  }
               }
            }
         }

         for (Entry<UUID, RadioNetwork.HH> e : HANDHELD.entrySet()) {
            RadioNetwork.HH h = e.getValue();
            if (h.carrier() && (h.root() == root || l.linkActive && h.root() == crossRoot(root, sl.dimension()))) {
               ServerPlayer lp = sl.getServer().getPlayerList().getPlayer(e.getKey());
               if (lp != null) {
                  PacketDistributor.sendToPlayer(
                     lp, new RadioSoundPayload(BlockPos.ZERO, true, id, vol * 0.8F, pitch, h.q(), h.fm(), kind, key, offsetMs), new CustomPacketPayload[0]
                  );
               }
            }
         }

         MobileRadios.deliverSound(sl, root, id, vol, pitch, kind, key, offsetMs);
      }
   }

   private static void deliverSound(ServerLevel sl, RadioNetwork.Lvl l, long root, ResourceLocation id, float vol, float pitch, byte kind, long key, int off) {
      for (TunableBE s : l.sinks) {
         if (s.carrier && s.bestRoot == root && !s.isRemoved()) {
            if (s instanceof RadioBE r && r.sinkOn()) {
               sendSound(sl, r.getBlockPos(), id, vol, pitch, r.quality, r.tuning.fm(), kind, key, off);
            } else if (s instanceof ReceiverBE rx && rx.sinkOn()) {
               if (rx.speakers.isEmpty()) {
                  sendSound(sl, rx.getBlockPos(), id, vol * 0.6F, pitch, rx.quality * 0.92F, rx.tuning.fm(), kind, key, off);
               } else {
                  for (SpeakerBE sp : rx.speakers) {
                     if (!sp.isRemoved()) {
                        sendSound(sl, sp.getBlockPos(), id, vol, pitch, rx.quality * rx.lineQ.getOrDefault(sp, 1.0F), rx.tuning.fm(), kind, key, off);
                     }
                  }
               }
            }
         }
      }
   }

   public static void sendSound(ServerLevel lv, BlockPos pos, ResourceLocation id, float vol, float pitch, float q, boolean fm, byte kind, long key, int off) {
      Vec3 g = SableCompat.global(lv, pos);
      PacketDistributor.sendToPlayersNear(
         lv, null, g.x, g.y, g.z, kind == 0 ? 32.0 : 80.0, new RadioSoundPayload(pos, false, id, vol, pitch, q, fm, kind, key, off), new CustomPacketPayload[0]
      );
   }

   public static List<Emitter> emitters(Level lv) {
      RadioNetwork.Lvl l = LEVELS.get(lv.dimension());
      return l == null ? List.of() : Collections.unmodifiableList(l.emitters);
   }

   public static List<BlockEntity> tunedNear(Level lv, BlockPos pos, int radius) {
      RadioNetwork.Lvl l = LEVELS.get(lv.dimension());
      List<BlockEntity> out = new ArrayList<>();
      if (l == null) {
         return out;
      }

      double r2 = (double)radius * radius;

      for (BlockEntity be : l.tx) {
         if (!be.isRemoved() && be.getBlockPos().distSqr(pos) <= r2) {
            out.add(be);
         }
      }

      for (BlockEntity be : l.relays) {
         if (!be.isRemoved() && be.getBlockPos().distSqr(pos) <= r2) {
            out.add(be);
         }
      }

      for (BlockEntity be : l.sinks) {
         if (!be.isRemoved() && be.getBlockPos().distSqr(pos) <= r2) {
            out.add(be);
         }
      }

      return out;
   }

   public static boolean onAirNear(Level lv, BlockPos pos, int radius) {
      RadioNetwork.Lvl l = LEVELS.get(lv.dimension());
      if (l == null) {
         return false;
      }

      double r2 = (double)radius * radius;

      for (TransmitterBE t : l.tx) {
         if (!t.isRemoved() && !(t.getBlockPos().distSqr(pos) > r2)) {
            BlockState st = t.getBlockState();
            if (st.hasProperty(TransmitterBlock.ON_AIR) && (Boolean)st.getValue(TransmitterBlock.ON_AIR)) {
               return true;
            }
         }
      }

      return false;
   }

   public static List<BlockPos> jammers(Level lv) {
      RadioNetwork.Lvl l = LEVELS.get(lv.dimension());
      return l == null ? List.of() : Collections.unmodifiableList(l.jam);
   }

   public static ItemStack heldRadio(Player p) {
      for (InteractionHand h : InteractionHand.values()) {
         ItemStack s = p.getItemInHand(h);
         if (s.getItem() instanceof PortableRadioItem && PortableRadioItem.isOn(s)) {
            return s;
         }
      }

      return null;
   }

   private static boolean walkieTalkie(ServerPlayer p, ServerLevel sl, RadioNetwork.Lvl l, byte[] data) {
      ItemStack held = heldRadio(p);
      if (held == null) {
         return false;
      }

      Tuning t = (Tuning)held.getOrDefault((DataComponentType)ModRegistry.TUNING.get(), Tuning.DEFAULT);
      double reach = t.fm() ? 320.0 : 480.0;
      Vec3 from = p.getEyePosition();
      float gain = 0.75F;

      for (ServerPlayer o : sl.players()) {
         if (o != p) {
            ItemStack r = findRadio(o);
            if (r != null && ((Tuning)r.getOrDefault((DataComponentType)ModRegistry.TUNING.get(), Tuning.DEFAULT)).matches(t)) {
               float q = walkieQuality(sl, l, from, o.getEyePosition(), reach, t.fm(), 0.9);
               if (q > 0.04F) {
                  PacketDistributor.sendToPlayer(o, new HandheldAudioPayload(p.getUUID(), data, q, t.fm(), gain), new CustomPacketPayload[0]);
               }
            }
         }
      }

      for (TunableBE s : l.sinks) {
         if (s.tuning.matches(t)) {
            Vec3 at = SableCompat.global(sl, s.getBlockPos());
            float q = walkieQuality(sl, l, from, at, reach, t.fm(), s.rxGain());
            if (!(q <= 0.04F)) {
               if (s instanceof RadioBE r && r.sinkOn()) {
                  send(sl, r.getBlockPos(), p.getUUID(), data, q, t.fm(), gain);
               } else if (s instanceof ReceiverBE rx && rx.sinkOn()) {
                  if (rx.speakers.isEmpty()) {
                     send(sl, rx.getBlockPos(), p.getUUID(), data, q * 0.92F, t.fm(), gain * 0.6F);
                  } else {
                     for (SpeakerBE sp : rx.speakers) {
                        send(sl, sp.getBlockPos(), p.getUUID(), data, q * rx.lineQ.getOrDefault(sp, 1.0F), t.fm(), gain);
                     }
                  }
               }
            }
         }
      }

      long now = sl.getGameTime();
      Long last = WALKIE_SHOWN.get(p.getUUID());
      if (last == null || now - last > 40L) {
         WALKIE_SHOWN.put(p.getUUID(), now);
         p.displayClientMessage(Component.literal("● Talking on " + t.display()).withStyle(ChatFormatting.RED), true);
      }

      return true;
   }

   private static float walkieQuality(ServerLevel sl, RadioNetwork.Lvl l, Vec3 a, Vec3 b, double reach, boolean fm, double gain) {
      float q = quality(a.distanceTo(b), reach, fm, 0.92F, gain);

      for (BlockPos j : l.jam) {
         double d = segmentDistance(a.x, a.y, a.z, b.x, b.y, b.z, j.getX() + 0.5, j.getY() + 0.5, j.getZ() + 0.5);
         if (d < 48.0) {
            q *= (float)(0.08 + 0.92 * Math.pow(d / 48.0, 1.5));
         }
      }

      return q;
   }

   private static void deliverAll(ServerLevel sl, RadioNetwork.Lvl l, long root, UUID talker, byte[] data, float gain) {
      long now = System.currentTimeMillis();
      TALKING.put(root, new RadioNetwork.Talk(talker, now));

      for (TunableBE s : l.sinks) {
         if (s.carrier && s.bestRoot == root) {
            float clash = clash(sl, l, s, root, talker, now);
            if (s instanceof RadioBE r && r.sinkOn()) {
               send(sl, r.getBlockPos(), talker, data, r.quality * clash, r.tuning.fm(), gain);
            } else if (s instanceof ReceiverBE rx && rx.sinkOn()) {
               if (rx.speakers.isEmpty()) {
                  send(sl, rx.getBlockPos(), talker, data, rx.quality * clash * 0.92F, rx.tuning.fm(), gain * 0.6F);
               } else {
                  for (SpeakerBE sp : rx.speakers) {
                     send(sl, sp.getBlockPos(), talker, data, rx.quality * clash * rx.lineQ.getOrDefault(sp, 1.0F), rx.tuning.fm(), gain);
                  }
               }

               if (sl.getGameTime() - rx.lastPulse >= 6L && !rx.paths.isEmpty()) {
                  rx.lastPulse = sl.getGameTime();
                  pulse(sl, rx.paths);
               }
            }
         }
      }
   }

   private static float clash(ServerLevel sl, RadioNetwork.Lvl l, TunableBE s, long root, UUID talker, long now) {
      float own = s.quality;
      if (own <= 0.0F) {
         return 1.0F;
      }

      float worst = 0.0F;
      BlockPos at = SableCompat.globalPos(sl, s.getBlockPos());

      for (Emitter e : l.emitters) {
         if (e.root() != root && e.tuning().matches(s.tuning)) {
            RadioNetwork.Talk t = TALKING.get(e.root());
            if (t != null && now - t.at() <= 150L && !t.talker().equals(talker)) {
               float q = e.creative() ? e.baseQ() : quality(Math.sqrt(at.distSqr(e.pos())), e.rangeTo(at), s.tuning.fm(), e.baseQ(), s.rxGain());
               worst = Math.max(worst, q);
            }
         }
      }

      if (worst <= 0.02F) {
         return 1.0F;
      }

      float ratio = worst / own;
      return ratio >= 1.0F ? 0.08F : Math.max(0.08F, 1.0F - 0.9F * ratio);
   }

   public static void pulse(ServerLevel lv, List<Cables.Seg> segs) {
      if (!segs.isEmpty()) {
         int n = Math.min(512, segs.size());
         long[] f = new long[n];
         long[] t = new long[n];
         int[] fs = new int[n];
         int[] ts = new int[n];

         for (int i = 0; i < n; i++) {
            Cables.Seg sg = segs.get(i);
            f[i] = sg.from().pos().asLong();
            fs[i] = sg.from().slot();
            t[i] = sg.to().pos().asLong();
            ts[i] = sg.to().slot();
         }

         Vec3 at = SableCompat.global(lv, segs.get(0).from().pos());
         PacketDistributor.sendToPlayersNear(lv, null, at.x, at.y, at.z, 160.0, new CablePulsePayload(f, fs, t, ts), new CustomPacketPayload[0]);
      }
   }

   public static void send(ServerLevel lv, BlockPos pos, UUID talker, byte[] data, float q, boolean fm, float gain) {
      Vec3 g = SableCompat.global(lv, pos);
      PacketDistributor.sendToPlayersNear(lv, null, g.x, g.y, g.z, 26.0, new RadioAudioPayload(pos, talker, data, q, fm, gain), new CustomPacketPayload[0]);
   }

   public static List<StationInfo> spectrum(ServerPlayer p) {
      RadioNetwork.Lvl l = LEVELS.get(p.level().dimension());
      if (l == null) {
         return List.of();
      }

      Map<String, StationInfo> m = new HashMap<>();

      for (Emitter e : l.emitters) {
         float q = best(p.level(), p.blockPosition(), p.getEyeY() + 2.0, e.tuning(), 1.0, List.of(e), l.jam).q();
         if (!(q < 0.02F)) {
            String key = e.tuning().fm() + ":" + Math.round(e.tuning().freq() * 100.0);
            StationInfo old = m.get(key);
            if (old == null || old.quality() < q) {
               m.put(key, new StationInfo(e.tuning().fm(), e.tuning().freq(), q, e.name()));
            }
         }
      }

      List<StationInfo> out = new ArrayList<>(m.values());
      out.sort(Comparator.comparingDouble(StationInfo::freq));
      return out.size() > 60 ? out.subList(0, 60) : out;
   }

   public static void lightning(ServerLevel sl, Vec3 at) {
      for (ServerPlayer p : sl.players()) {
         double d = p.position().distanceTo(at);
         float strength = (float)Mth.clamp(1.0 - d / 1200.0, 0.12, 1.0);
         PacketDistributor.sendToPlayer(p, new StaticBurstPayload(strength), new CustomPacketPayload[0]);
      }
   }

   public record Best(float q, long root) {
   }

   private record Export(long root, Tuning tuning, float q, String name) {
   }

   private record HH(long root, float q, boolean fm, boolean carrier) {
   }

   private static final class Lvl {
      final Set<TransmitterBE> tx = new LinkedHashSet<>();
      final Set<RelayBE> relays = new LinkedHashSet<>();
      final Set<TunableBE> sinks = new LinkedHashSet<>();
      final Set<MicrophoneBE> mics = new LinkedHashSet<>();
      final Set<JammerBE> jammers = new LinkedHashSet<>();
      final Set<SpeakerBE> speakers = new LinkedHashSet<>();
      final Set<DimensionalRelayBE> links = new LinkedHashSet<>();
      List<Emitter> emitters = new ArrayList<>();
      List<BlockPos> jam = new ArrayList<>();
      List<RadioNetwork.Export> exports = new ArrayList<>();
      boolean linkActive;
   }

   private record Talk(UUID talker, long at) {
   }
}
