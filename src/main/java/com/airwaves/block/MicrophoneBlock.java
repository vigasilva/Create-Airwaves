/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Minecraft block interaction, state, shape, and block-entity hookup.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.block;

import com.airwaves.be.MicrophoneBE;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class MicrophoneBlock extends DeviceBlock {
   public static final MapCodec<MicrophoneBlock> CODEC = simpleCodec(MicrophoneBlock::new);

   public MicrophoneBlock(Properties p) {
      super(p, 4.0, 0.0, 3.0, 12.0, 12.0, 13.0);
   }

   protected MapCodec<MicrophoneBlock> codec() {
      return CODEC;
   }

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new MicrophoneBE(p, s);
   }
}
