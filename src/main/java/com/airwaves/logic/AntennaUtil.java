/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Core radio simulation and shared gameplay logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.logic;

import com.airwaves.be.CreativeAntennaBE;
import com.airwaves.be.YagiAntennaBE;
import com.airwaves.block.AntennaBlock;
import com.airwaves.block.CreativeAntennaBlock;
import com.airwaves.block.YagiAntennaBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;

public final class AntennaUtil {
   public static final int MIN_TX_HEIGHT = 4;
   private static final int[] CAPS = new int[]{20, 40, 64, 96};
   private static final double[] RANGE_MULT = new double[]{1.0, 1.3, 1.7, 2.3};
   private static final float[] QUALITY_BONUS = new float[]{0.0F, 0.08F, 0.15F, 0.22F};
   public static final String[] TX_NAMES = new String[]{"Low-Power", "Mid-Power", "High-Power", "Max-Power"};
   public static final String[] RX_NAMES = new String[]{"Basic", "Sensitive", "High-Gain", "Max-Gain"};
   public static final String[] TIER_NAMES = TX_NAMES;
   public static final double FORWARD = 1.9;
   public static final double SIDE = 0.0;
   public static final double BACK = 0.0;
   public static final double BEAM_EDGE = 0.5;

   private AntennaUtil() {
   }

   public static int cap(int tier) {
      return CAPS[Math.max(1, Math.min(4, tier)) - 1];
   }

   public static AntennaUtil.Info scan(Level l, BlockPos base, int wantTier) {
      if (l.isLoaded(base) && l.getBlockState(base).getBlock() instanceof CreativeAntennaBlock) {
         return new AntennaUtil.Info(
            cap(wantTier), Math.max(1, Math.min(4, wantTier)), l.getBlockEntity(base) instanceof CreativeAntennaBE ca ? ca.range() : 1000.0
         );
      }

      AntennaUtil.Info i = scan(l, base);
      if (i.height() <= 0) {
         return AntennaUtil.Info.NONE;
      }

      int t = Math.max(1, Math.min(4, wantTier));
      return new AntennaUtil.Info(Math.min(i.height(), cap(t)), t, -1.0, i.beam());
   }

   public static AntennaUtil.Info scan(Level l, BlockPos base) {
      if (l.isLoaded(base) && l.getBlockState(base).getBlock() instanceof AntennaBlock ab) {
         int var6 = ab.maxHeight();
         int h = 0;
         MutableBlockPos p = ab.column(base, l.getBlockState(base)).mutable();

         while (h < var6 && l.isLoaded(p) && l.getBlockState(p).getBlock() instanceof AntennaBlock) {
            h++;
            p.move(Direction.UP);
         }

         return new AntennaUtil.Info(h, ab.tier(), -1.0, yagiAbove(l, p, ab.size()));
      } else {
         return AntennaUtil.Info.NONE;
      }
   }

   public static Float yagiAbove(Level l, BlockPos at, int size) {
      return l.isLoaded(at) && YagiAntennaBlock.isMount(l.getBlockState(at)) ? yagi(l, at.above()) : null;
   }

   public static double range(int h, int tier, boolean fm) {
      if (h > 0 && tier > 0) {
         double r = h <= 20 ? 600.0 * Math.pow(h / 4.0, 1.13) : 600.0 * Math.pow(5.0, 1.13) * Math.pow(h / 20.0, 0.75);
         r *= RANGE_MULT[tier - 1];
         return fm ? r : r * 1.3;
      } else {
         return 0.0;
      }
   }

   public static double range(AntennaUtil.Info a, boolean fm) {
      return range(a.height(), a.tier(), fm);
   }

   public static double range(int h, boolean fm) {
      return range(h, 1, fm);
   }

   public static float quality(AntennaUtil.Info a) {
      return a.height() <= 0 ? 0.0F : Math.min(1.0F, 0.5F + 0.5F * Math.min(1.0F, a.height() / 20.0F) + QUALITY_BONUS[a.tier() - 1]);
   }

   public static float quality(int h) {
      return quality(new AntennaUtil.Info(h, 1));
   }

   public static double rxGain(AntennaUtil.Info a) {
      return a.height() <= 0 ? 1.0 : (1.0 + 0.12 * Math.min(a.height(), 30)) * (1.0 + 0.2 * (a.tier() - 1));
   }

   public static Float yagi(Level l, BlockPos at) {
      if (!l.isLoaded(at)) {
         return null;
      } else {
         return l.getBlockEntity(at) instanceof YagiAntennaBE y ? (float)y.bearing() : null;
      }
   }

   public static double bx(float deg) {
      return Math.sin(Math.toRadians(deg));
   }

   public static double bz(float deg) {
      return -Math.cos(Math.toRadians(deg));
   }

   public static String compass(float deg) {
      String[] n = new String[]{"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
      return n[Math.floorMod(Math.round(deg / 45.0F), 8)];
   }

   public static double pattern(Float beam, double dx, double dz) {
      double d = Math.sqrt(dx * dx + dz * dz);
      if (d < 1.0E-6) {
         return 1.0;
      }

      double cos = (dx * bx(beam) + dz * bz(beam)) / d;
      if (cos <= 0.5) {
         return 0.0;
      }

      double k = (cos - 0.5) / 0.5;
      return 1.9 * k * Math.sqrt(k);
   }

   public static String tierName(int tier) {
      return tier <= 0 ? "none" : TIER_NAMES[tier - 1];
   }

   public record Info(int height, int tier, double fixedRange, Float beam) {
      public static final AntennaUtil.Info NONE = new AntennaUtil.Info(0, 0);

      public Info(int height, int tier) {
         this(height, tier, -1.0, null);
      }

      public Info(int height, int tier, double fixedRange) {
         this(height, tier, fixedRange, null);
      }

      public boolean creative() {
         return this.fixedRange >= 0.0;
      }
   }
}
