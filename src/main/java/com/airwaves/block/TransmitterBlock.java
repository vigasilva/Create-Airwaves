/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Minecraft block interaction, state, shape, and block-entity hookup.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.block;

import com.airwaves.ModRegistry;
import com.airwaves.Tier;
import com.airwaves.be.TransmitterBE;
import com.airwaves.kinetic.KineticDeviceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class TransmitterBlock extends KineticDeviceBlock<TransmitterBE> {
   public static final IntegerProperty CRYSTAL = IntegerProperty.create("crystal", 0, 2);
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
   public static final BooleanProperty ON_AIR = BooleanProperty.create("on_air");
   private static final double[][] BOX = new double[][]{
      {0.0, 0.0, 3.0, 16.0, 16.0, 14.0}, {0.0, 0.0, 1.0, 16.0, 16.0, 15.0}, {0.0, 0.0, 0.0, 16.0, 16.0, 16.0}, {0.0, 0.0, 0.0, 16.0, 16.0, 16.0}
   };
   public final Tier tier;
   private static final double[][][] TERMINALS = new double[][][]{
      {{10.0, 10.0, 3.0, 13.0, 12.0, 4.0}, {3.0, 10.0, 3.0, 6.0, 12.0, 4.0}},
      {{13.0, 1.0, 1.0, 15.0, 3.0, 3.0}, {1.0, 1.0, 1.0, 3.0, 3.0, 3.0}},
      {{11.0, 1.0, 0.0, 13.0, 3.0, 1.0}, {5.0, 1.0, 0.0, 7.0, 3.0, 1.0}},
      {{12.0, 2.0, 0.0, 14.0, 4.0, 1.0}, {7.0, 2.0, 0.0, 9.0, 4.0, 1.0}}
   };

   @Override
   public double[][] terminals() {
      return TERMINALS[this.tier.level - 1];
   }

   public TransmitterBlock(Tier tier, Properties p) {
      super(p, BOX[tier.level - 1]);
      this.tier = tier;
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(CRYSTAL, 0)).setValue(ACTIVE, false)).setValue(ON_AIR, false)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      super.createBlockStateDefinition(b);
      b.add(new Property[]{CRYSTAL, ACTIVE, ON_AIR});
   }

   public Class<TransmitterBE> getBlockEntityClass() {
      return TransmitterBE.class;
   }

   public BlockEntityType<? extends TransmitterBE> getBlockEntityType() {
      return ModRegistry.TRANSMITTER_BE.get();
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState st, Level lv, BlockPos pos, Player pl, InteractionHand h, BlockHitResult hit) {
      ItemInteractionResult r = this.crystalInsert(stack, st, lv, pos, pl, CRYSTAL);
      return r != null ? r : super.useItemOn(stack, st, lv, pos, pl, h, hit);
   }

   protected InteractionResult useWithoutItem(BlockState st, Level lv, BlockPos pos, Player pl, BlockHitResult hit) {
      if (handEmpty(pl) && pl.isShiftKeyDown() && (Integer)st.getValue(CRYSTAL) != 0) {
         if (!lv.isClientSide) {
            this.crystalRemove((BlockState)((BlockState)st.setValue(ACTIVE, false)).setValue(ON_AIR, false), lv, pos, CRYSTAL);
         }

         return InteractionResult.sidedSuccess(lv.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }
}
