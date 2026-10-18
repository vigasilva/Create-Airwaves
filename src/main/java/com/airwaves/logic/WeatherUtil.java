/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Core radio simulation and shared gameplay logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class WeatherUtil {
   private WeatherUtil() {
   }

   public static float rain(Level l) {
      return l.dimensionType().hasSkyLight() ? l.getRainLevel(1.0F) : 0.0F;
   }

   public static float thunder(Level l) {
      return l.dimensionType().hasSkyLight() ? l.getThunderLevel(1.0F) : 0.0F;
   }

   public static float penalty(float rain, float thunder, boolean fm) {
      return fm ? rain * 0.1F + thunder * 0.22F : rain * 0.22F + thunder * 0.45F;
   }

   public static float factor(Level l, BlockPos pos, boolean fm) {
      float r = rain(l);
      float t = thunder(l);
      if (r <= 0.0F && t <= 0.0F) {
         return 1.0F;
      }

      float k = l.canSeeSky(pos.above()) ? 1.0F : 0.5F;
      return Math.max(0.2F, 1.0F - penalty(r, t, fm) * k);
   }

   public static double amNightBoost(Level l) {
      if (!l.dimensionType().hasSkyLight()) {
         return 1.0;
      } else {
         long t = l.getDayTime() % 24000L;
         if (t > 13000L && t < 23000L) {
            return 1.6;
         } else {
            return t <= 12000L && t < 23000L ? 1.0 : 1.3;
         }
      }
   }

   public static String describe(Level l) {
      float t = thunder(l);
      float r = rain(l);
      return t > 0.5F ? "Thunderstorm" : (r > 0.5F ? "Rain" : (r > 0.05F ? "Drizzle" : "Clear"));
   }
}
