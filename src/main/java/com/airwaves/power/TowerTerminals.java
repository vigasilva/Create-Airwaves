/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Power-system abstractions and electrical integration.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.power;

import com.airwaves.structure.StructureBlock;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TowerTerminals {
   private static final double[][] RELAY = new double[][]{{4.0, 6.0, 14.0, 6.0, 9.0, 16.0}, {10.0, 6.0, 14.0, 12.0, 9.0, 16.0}};
   private static final double[][] RADIO = new double[][]{{4.0, 8.0, 7.0, 6.0, 11.0, 9.0}, {10.0, 8.0, 7.0, 12.0, 11.0, 9.0}};

   private TowerTerminals() {
   }

   public static int part(StructureBlock b) {
      return switch (b.def.name) {
         case "relay_tower_small", "radio_tower" -> 7;
         case "relay_tower_big" -> 13;
         default -> -1;
      };
   }

   public static boolean isTerminal(StructureBlock b, BlockState st) {
      int p = part(b);
      return p >= 0 && b.partOf(st) == p;
   }

   public static boolean isTerminal(BlockState st) {
      return st.getBlock() instanceof StructureBlock b && isTerminal(b, st);
   }

   private static double[][] spec(StructureBlock b) {
      return b.def.name.equals("radio_tower") ? RADIO : RELAY;
   }

   private static boolean up(StructureBlock b) {
      return b.def.name.equals("radio_tower");
   }

   public static AABB box(BlockState st, int i) {
      StructureBlock b = (StructureBlock)st.getBlock();
      double[] s = spec(b)[i];
      Vec3 a = turn(new Vec3(s[0] / 16.0, s[1] / 16.0, s[2] / 16.0), st);
      Vec3 c = turn(new Vec3(s[3] / 16.0, s[4] / 16.0, s[5] / 16.0), st);
      return new AABB(Math.min(a.x, c.x), Math.min(a.y, c.y), Math.min(a.z, c.z), Math.max(a.x, c.x), Math.max(a.y, c.y), Math.max(a.z, c.z));
   }

   public static Vec3 point(BlockState st, int i) {
      StructureBlock b = (StructureBlock)st.getBlock();
      double[] s = spec(b)[i];
      Vec3 p = up(b) ? new Vec3((s[0] + s[3]) / 32.0, s[4] / 16.0, (s[2] + s[5]) / 32.0) : new Vec3((s[0] + s[3]) / 32.0, (s[1] + s[4]) / 32.0, s[5] / 16.0);
      return turn(p, st);
   }

   private static Vec3 turn(Vec3 v, BlockState st) {
      return st.getValue(StructureBlock.AXIS) == Axis.Z ? new Vec3(1.0 - v.z, v.y, v.x) : v;
   }
}
