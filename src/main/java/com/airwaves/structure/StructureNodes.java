/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Multiblock/tower validation and structural logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.structure;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

final class StructureNodes {
   private StructureNodes() {
   }

   static Vec3 anchor(BlockEntity be, int slot) {
      return be.getBlockState().getBlock() instanceof StructureBlock b && b.def.connectors.length != 0
         ? b.def.connector(be.getBlockPos(), Math.min(slot, b.def.connectors.length - 1), (Axis)be.getBlockState().getValue(StructureBlock.AXIS))
         : Vec3.atCenterOf(be.getBlockPos());
   }
}
