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
import com.airwaves.be.ReceiverBE;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class ReceiverBlock extends DeviceBlock {
   public static final MapCodec<ReceiverBlock> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.INT.fieldOf("tier").forGetter(b -> b.tier.level), propertiesCodec()).apply(i, (t, p) -> new ReceiverBlock(Tier.of(t), p))
   );
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
   public static final IntegerProperty CRYSTAL = TransmitterBlock.CRYSTAL;
   private static final double[][] BOX = new double[][]{
      {3.0, 0.0, 4.0, 13.0, 16.0, 13.0}, {1.0, 0.0, 3.0, 15.0, 16.0, 13.0}, {0.0, 0.0, 1.0, 16.0, 16.0, 15.0}, {0.0, 0.0, 0.0, 16.0, 16.0, 16.0}
   };
   public final Tier tier;

   public ReceiverBlock(Tier tier, Properties p) {
      super(p, BOX[tier.level - 1]);
      this.tier = tier;
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(ACTIVE, false)).setValue(CRYSTAL, 0));
   }

   protected MapCodec<ReceiverBlock> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      super.createBlockStateDefinition(b);
      b.add(new Property[]{ACTIVE, CRYSTAL});
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState st, Level lv, BlockPos pos, Player pl, InteractionHand h, BlockHitResult hit) {
      ItemInteractionResult r = this.crystalInsert(stack, st, lv, pos, pl, CRYSTAL);
      return r != null ? r : super.useItemOn(stack, st, lv, pos, pl, h, hit);
   }

   protected InteractionResult useWithoutItem(BlockState st, Level lv, BlockPos pos, Player pl, BlockHitResult hit) {
      if (handEmpty(pl) && pl.isShiftKeyDown() && (Integer)st.getValue(CRYSTAL) != 0) {
         if (!lv.isClientSide) {
            this.crystalRemove((BlockState)st.setValue(ACTIVE, false), lv, pos, CRYSTAL);
         }

         return InteractionResult.sidedSuccess(lv.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new ReceiverBE(p, s);
   }

   protected boolean isSignalSource(BlockState s) {
      return true;
   }

   protected int getSignal(BlockState s, BlockGetter l, BlockPos p, Direction d) {
      return l.getBlockEntity(p) instanceof ReceiverBE r ? r.signal() : 0;
   }
}
