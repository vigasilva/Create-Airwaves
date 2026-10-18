/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Core radio simulation and shared gameplay logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.logic;

import com.airwaves.be.MicrophoneBE;
import com.airwaves.be.TapePlayerBE;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity;
import net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition;

public final class SoundCapture {
   public static boolean suppress;
   private static final Map<ResourceKey<Level>, Map<Long, Map<Long, SoundCapture.Heard>>> HEARD = new HashMap<>();
   public static final int JUKEBOX_RANGE = 6;

   private SoundCapture() {
   }

   @SubscribeEvent
   public static void atPosition(AtPosition e) {
      handle(e.getLevel(), e.getPosition(), e);
   }

   @SubscribeEvent
   public static void atEntity(AtEntity e) {
      handle(e.getEntity().level(), e.getEntity().position(), e);
   }

   private static void handle(Level lv, Vec3 at, PlayLevelSoundEvent e) {
      if (!suppress && !e.isCanceled() && lv instanceof ServerLevel sl) {
         Holder<SoundEvent> h = e.getSound();
         if (h != null) {
            SoundSource src = e.getSource();
            if (src != SoundSource.MUSIC && src != SoundSource.AMBIENT && src != SoundSource.VOICE) {
               RadioNetwork.onWorldSound(sl, at, ((SoundEvent)h.value()).getLocation(), e.getNewVolume(), e.getNewPitch());
            }
         }
      }
   }

   public static void pollJukeboxes(ServerLevel sl, Collection<MicrophoneBE> mics) {
      Map<Long, Map<Long, SoundCapture.Heard>> dim = HEARD.computeIfAbsent(sl.dimension(), k -> new HashMap<>());
      Set<Long> alive = new HashSet<>();

      for (MicrophoneBE m : mics) {
         if (!m.isRemoved()) {
            long mk = m.getBlockPos().asLong();
            alive.add(mk);
            TapePlayerBE tape = m instanceof TapePlayerBE tp && tp.isRecording() ? tp : null;
            boolean live = !(m instanceof TapePlayerBE) && m.feeds != null && m.feeds.active && !m.feeds.isRemoved();
            long root = live ? m.feeds.getBlockPos().asLong() : 0L;
            Map<Long, SoundCapture.Heard> now = new HashMap<>();
            if (tape != null || live) {
               BlockPos c = m.getBlockPos();

               for (BlockPos p : BlockPos.betweenClosed(c.offset(-6, -6, -6), c.offset(6, 6, 6))) {
                  if (sl.isLoaded(p) && sl.getBlockEntity(p) instanceof JukeboxBlockEntity jb && jb.getSongPlayer().isPlaying()) {
                     JukeboxSong song = jb.getSongPlayer().getSong();
                     if (song != null) {
                        ResourceLocation sid = ((SoundEvent)song.soundEvent().value()).getLocation();
                        SoundCapture.Heard was = dim.getOrDefault(mk, Map.of()).get(p.asLong());
                        now.put(
                           p.asLong(),
                           new SoundCapture.Heard(sid, root, was != null && was.sound().equals(sid) && was.root() == root ? was.since() : sl.getGameTime())
                        );
                     }
                  }
               }
            }

            Map<Long, SoundCapture.Heard> before = dim.computeIfAbsent(mk, k -> new HashMap<>());

            for (Entry<Long, SoundCapture.Heard> e : before.entrySet()) {
               SoundCapture.Heard n = now.get(e.getKey());
               if (n == null || !n.sound().equals(e.getValue().sound())) {
                  send(sl, m, tape, e.getValue(), e.getKey(), (byte)2);
               }
            }

            for (Entry<Long, SoundCapture.Heard> e : now.entrySet()) {
               SoundCapture.Heard b = before.get(e.getKey());
               boolean fresh = b == null || !b.sound().equals(e.getValue().sound());
               if (tape != null) {
                  if (fresh) {
                     tape.capture(e.getValue().sound(), 1.0F, 1.0F, (byte)1);
                  }
               } else if (e.getValue().root() != 0L) {
                  RadioNetwork.broadcastSound(
                     sl,
                     e.getValue().root(),
                     e.getValue().sound(),
                     1.0F,
                     1.0F,
                     (byte)1,
                     e.getKey(),
                     (int)Math.min(2147483647L, (sl.getGameTime() - e.getValue().since()) * 50L)
                  );
               }
            }

            before.clear();
            before.putAll(now);
         }
      }

      Iterator<Entry<Long, Map<Long, SoundCapture.Heard>>> it = dim.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Long, Map<Long, SoundCapture.Heard>> e = it.next();
         if (!alive.contains(e.getKey())) {
            for (Entry<Long, SoundCapture.Heard> h : e.getValue().entrySet()) {
               if (h.getValue().root() != 0L) {
                  RadioNetwork.broadcastSound(sl, h.getValue().root(), h.getValue().sound(), 1.0F, 1.0F, (byte)2, h.getKey());
               }
            }

            it.remove();
         }
      }
   }

   private static void send(ServerLevel sl, MicrophoneBE m, TapePlayerBE tape, SoundCapture.Heard h, long jukebox, byte kind) {
      if (tape != null) {
         tape.capture(h.sound(), 1.0F, 1.0F, kind);
      } else {
         if (h.root() != 0L) {
            RadioNetwork.broadcastSound(sl, h.root(), h.sound(), 1.0F, 1.0F, kind, jukebox);
         }
      }
   }

   public static void clear() {
      HEARD.clear();
   }

   private record Heard(ResourceLocation sound, long root, long since) {
   }
}
