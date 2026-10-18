/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Core radio simulation and shared gameplay logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.logic;

import com.mojang.logging.LogUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public final class TapeAudio {
   private static final Logger LOG = LogUtils.getLogger();
   public static final int MAX_FRAMES = 15000;
   private static final Map<String, List<TapeAudio.Frame>> CACHE = Collections.synchronizedMap(
      new LinkedHashMap<String, List<TapeAudio.Frame>>(16, 0.75F, true) {
         @Override
         protected boolean removeEldestEntry(Entry<String, List<TapeAudio.Frame>> e) {
            return this.size() > 12;
         }
      }
   );

   private TapeAudio() {
   }

   private static Path file(MinecraftServer s, String id) {
      return s.getWorldPath(LevelResource.ROOT).resolve("airwaves_tapes").resolve(id.replaceAll("[^a-zA-Z0-9-]", "") + ".bin");
   }

   public static void save(MinecraftServer s, String id, List<TapeAudio.Frame> frames) {
      List<TapeAudio.Frame> copy = List.copyOf(frames);
      CACHE.put(id, copy);
      Path f = file(s, id);
      Util.ioPool().execute(() -> {
         try {
            Files.createDirectories(f.getParent());

            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(f))))) {
               out.writeInt(1);
               out.writeInt(copy.size());

               for (TapeAudio.Frame fr : copy) {
                  out.writeInt(fr.ms());
                  out.writeShort(fr.data().length);
                  out.write(fr.data());
               }
            }
         } catch (IOException e) {
            LOG.warn("Could not save tape {}", id, e);
         }
      });
   }

   public static List<TapeAudio.Frame> load(MinecraftServer s, String id) {
      if (id != null && !id.isEmpty()) {
         List<TapeAudio.Frame> hit = CACHE.get(id);
         if (hit != null) {
            return hit;
         }

         Path f = file(s, id);
         if (!Files.exists(f)) {
            return List.of();
         }

         try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(f))))) {
            in.readInt();
            int n = Math.min(in.readInt(), 15000);
            List<TapeAudio.Frame> out = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
               int ms = in.readInt();
               byte[] d = new byte[Math.min(in.readUnsignedShort(), 1024)];
               in.readFully(d);
               out.add(new TapeAudio.Frame(ms, d));
            }

            List<TapeAudio.Frame> r = List.copyOf(out);
            CACHE.put(id, r);
            return r;
         } catch (IOException e) {
            LOG.warn("Could not read tape {}", id, e);
            return List.of();
         }
      } else {
         return List.of();
      }
   }

   public static void clear() {
      CACHE.clear();
   }

   public record Frame(int ms, byte[] data) {
   }
}
