/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Block-entity state and server-side behavior for a placed device.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.be;

import com.airwaves.cable.CableType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public interface CableNode {
   LinkSet links();

   BlockPos nodePos();

   default int connectors() {
      return 1;
   }

   int maxLinks(int var1);

   default boolean accepts(int slot, CableType t) {
      return t.light;
   }

   default boolean junction(int slot) {
      return false;
   }

   Vec3 anchor(int var1);

   void cablesChanged();

   default int slotAt(Vec3 hit) {
      int best = 0;
      double bd = Double.MAX_VALUE;

      for (int i = 0; i < this.connectors(); i++) {
         double d = this.anchor(i).distanceToSqr(hit);
         if (d < bd) {
            bd = d;
            best = i;
         }
      }

      return best;
   }

   static Vec3 facing(BlockEntity be, double px, double py, double pz) {
      double x = px / 16.0;
      double y = py / 16.0;
      double z = pz / 16.0;
      BlockState st = be.getBlockState();
      if (st.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
         double ox = x;
         double oz = z;
         Direction f = (Direction)st.getValue(BlockStateProperties.HORIZONTAL_FACING);
         switch (f) {
            case SOUTH:
               x = 1.0 - ox;
               z = 1.0 - oz;
               break;
            case EAST:
               x = 1.0 - oz;
               z = ox;
               break;
            case WEST:
               x = oz;
               z = 1.0 - ox;
         }
      }

      BlockPos p = be.getBlockPos();
      return new Vec3(p.getX() + x, p.getY() + y, p.getZ() + z);
   }
}
