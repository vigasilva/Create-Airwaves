/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Minecraft block interaction, state, shape, and block-entity hookup.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.block;

import com.airwaves.Tier;
import com.airwaves.be.CreativeReceiverBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class CreativeReceiverBlock extends ReceiverBlock {
   public CreativeReceiverBlock(Properties p) {
      super(Tier.GIANT, p);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(CRYSTAL, 2)).setValue(ACTIVE, true));
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new CreativeReceiverBE(p, s);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState st, Level lv, BlockPos pos, Player pl, BlockHitResult hit) {
      return InteractionResult.PASS;
   }
}
