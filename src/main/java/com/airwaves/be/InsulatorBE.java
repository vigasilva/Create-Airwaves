/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Block-entity state and server-side behavior for a placed device.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.be;

import com.airwaves.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class InsulatorBE extends NodeBE {
   public InsulatorBE(BlockPos p, BlockState s) {
      super(ModRegistry.INSULATOR_BE.get(), p, s);
   }

   @Override
   public int maxLinks(int slot) {
      return 4;
   }

   @Override
   public boolean junction(int slot) {
      return true;
   }

   @Override
   public Vec3 anchor(int slot) {
      return CableNode.facing(this, 8.0, 9.6, 8.0);
   }
}
