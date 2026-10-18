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
import com.airwaves.be.TapePlayerBE;
import com.airwaves.item.TapeItem;
import com.airwaves.menu.TapePlayerMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TapePlayerBlock extends DeviceBlock {
   public static final MapCodec<TapePlayerBlock> CODEC = simpleCodec(TapePlayerBlock::new);

   public TapePlayerBlock(Properties p) {
      super(p, 1.0, 0.0, 3.0, 15.0, 16.0, 15.0);
   }

   protected MapCodec<TapePlayerBlock> codec() {
      return CODEC;
   }

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new TapePlayerBE(p, s);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level lv, BlockState s, BlockEntityType<T> t) {
      return lv.isClientSide ? null : createTickerHelper(t, ModRegistry.TAPE_PLAYER_BE.get(), (l, p, st, be) -> be.serverTick());
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState st, Level lv, BlockPos pos, Player pl, InteractionHand h, BlockHitResult hit) {
      if (stack.getItem() instanceof TapeItem && lv.getBlockEntity(pos) instanceof TapePlayerBE be && be.stack().isEmpty()) {
         if (!lv.isClientSide) {
            be.inv.setItem(0, stack.split(1));
            lv.playSound(null, pos, (SoundEvent)SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.6F, 1.6F);
         }

         return ItemInteractionResult.sidedSuccess(lv.isClientSide);
      } else {
         return super.useItemOn(stack, st, lv, pos, pl, h, hit);
      }
   }

   protected InteractionResult useWithoutItem(BlockState st, Level lv, BlockPos pos, Player pl, BlockHitResult hit) {
      if (!lv.isClientSide) {
         pl.openMenu(st.getMenuProvider(lv, pos));
      }

      return InteractionResult.sidedSuccess(lv.isClientSide);
   }

   protected MenuProvider getMenuProvider(BlockState st, Level lv, BlockPos pos) {
      return lv.getBlockEntity(pos) instanceof TapePlayerBE be
         ? new SimpleMenuProvider(
            (id, inv, p) -> new TapePlayerMenu(id, inv, be.inv, be.data, ContainerLevelAccess.create(lv, pos), be),
            Component.translatable("container.airwaves.tape_player")
         )
         : null;
   }

   protected void neighborChanged(BlockState s, Level lv, BlockPos p, Block b, BlockPos from, boolean moved) {
      super.neighborChanged(s, lv, p, b, from, moved);
      if (!lv.isClientSide && lv.getBlockEntity(p) instanceof TapePlayerBE be) {
         be.onRedstone(lv.hasNeighborSignal(p));
      }
   }

   @Override
   protected void onRemove(BlockState s, Level lv, BlockPos p, BlockState ns, boolean moved) {
      if (!s.is(ns.getBlock()) && lv.getBlockEntity(p) instanceof TapePlayerBE be) {
         Containers.dropContents(lv, p, be.inv);
      }

      super.onRemove(s, lv, p, ns, moved);
   }

   protected boolean hasAnalogOutputSignal(BlockState s) {
      return true;
   }

   protected int getAnalogOutputSignal(BlockState s, Level lv, BlockPos p) {
      return lv.getBlockEntity(p) instanceof TapePlayerBE be && be.mode == 1 ? 15 : 0;
   }
}
