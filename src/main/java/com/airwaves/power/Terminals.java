/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Power-system abstractions and electrical integration.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.power;

import com.airwaves.kinetic.KineticDeviceBlock;
import com.airwaves.structure.StructureBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Terminals {
   public static final int POSITIVE = 0;
   public static final int NEGATIVE = 1;
   public static final double[][] DEFAULT = new double[][]{{11.0, 1.0, 0.0, 14.0, 4.0, 1.0}, {2.0, 1.0, 0.0, 5.0, 4.0, 1.0}};

   private Terminals() {
   }

   public static double[][] spec(BlockState st) {
      return st.getBlock() instanceof KineticDeviceBlock<?> k ? k.terminals() : DEFAULT;
   }

   public static Direction facing(BlockState st) {
      return st.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING) ? (Direction)st.getValue(HorizontalKineticBlock.HORIZONTAL_FACING) : Direction.NORTH;
   }

   public static boolean has(BlockState st) {
      return TowerTerminals.isTerminal(st) || st.getBlock() instanceof KineticDeviceBlock && st.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING);
   }

   public static AABB box(BlockState st, int i) {
      if (st.getBlock() instanceof StructureBlock) {
         return TowerTerminals.box(st, i);
      }

      double[] b = spec(st)[i];
      Direction f = facing(st);
      return new AABB(rotate(new Vec3(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0), f), rotate(new Vec3(b[3] / 16.0, b[4] / 16.0, b[5] / 16.0), f));
   }

   public static Vec3 point(BlockState st, int i) {
      if (st.getBlock() instanceof StructureBlock) {
         return TowerTerminals.point(st, i);
      }

      double[] b = spec(st)[i];
      return rotate(new Vec3((b[0] + b[3]) / 32.0, (b[1] + b[4]) / 32.0, (b[2] - 1.0) / 16.0), facing(st));
   }

   public static int at(BlockState st, Vec3 local) {
      for (int i = 0; i < 2; i++) {
         if (box(st, i).inflate(0.03).contains(local)) {
            return i;
         }
      }

      return -1;
   }

   private static Vec3 rotate(Vec3 v, Direction f) {
      return switch (f) {
         case EAST -> new Vec3(1.0 - v.z, v.y, v.x);
         case SOUTH -> new Vec3(1.0 - v.x, v.y, 1.0 - v.z);
         case WEST -> new Vec3(v.z, v.y, 1.0 - v.x);
         default -> v;
      };
   }
}
