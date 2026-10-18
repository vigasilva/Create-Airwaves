/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Minecraft block interaction, state, shape, and block-entity hookup.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.block;

import com.airwaves.be.InsulatorBE;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class InsulatorBlock extends DeviceBlock {
   public static final MapCodec<InsulatorBlock> CODEC = simpleCodec(InsulatorBlock::new);

   public InsulatorBlock(Properties p) {
      super(p, 2.5, 0.0, 4.5, 13.5, 11.0, 11.5);
   }

   protected MapCodec<InsulatorBlock> codec() {
      return CODEC;
   }

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new InsulatorBE(p, s);
   }
}
