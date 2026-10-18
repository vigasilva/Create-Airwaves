/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Minecraft block interaction, state, shape, and block-entity hookup.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.block;

import com.airwaves.be.SpeakerBE;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class SpeakerBlock extends DeviceBlock {
   public static final MapCodec<SpeakerBlock> CODEC = simpleCodec(SpeakerBlock::new);

   public SpeakerBlock(Properties p) {
      super(p, 2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
   }

   protected SpeakerBlock(Properties p, double[] box) {
      super(p, box);
   }

   protected MapCodec<SpeakerBlock> codec() {
      return CODEC;
   }

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new SpeakerBE(p, s);
   }
}
