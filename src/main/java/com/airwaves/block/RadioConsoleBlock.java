/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Minecraft block interaction, state, shape, and block-entity hookup.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.block;

import com.airwaves.ModSounds;
import com.airwaves.be.RadioConsoleBE;
import com.airwaves.logic.Emitter;
import com.airwaves.logic.RadioNetwork;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class RadioConsoleBlock extends DeviceBlock {
   public static final MapCodec<RadioConsoleBlock> CODEC = simpleCodec(RadioConsoleBlock::new);
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

   public RadioConsoleBlock(Properties p) {
      super(p, 0.0, 0.0, 1.0, 16.0, 16.0, 15.0);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(ACTIVE, false));
   }

   protected MapCodec<RadioConsoleBlock> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      super.createBlockStateDefinition(b);
      b.add(new Property[]{ACTIVE});
   }

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new RadioConsoleBE(p, s);
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState st, Level lv, BlockPos pos, Player pl, InteractionHand h, BlockHitResult hit) {
      ItemInteractionResult r = super.useItemOn(stack, st, lv, pos, pl, h, hit);
      if (r.consumesAction() && !lv.isClientSide && lv.getBlockEntity(pos) instanceof RadioConsoleBE be) {
         be.broadcastTuning();
      }

      return r;
   }

   protected InteractionResult useWithoutItem(BlockState s, Level lv, BlockPos pos, Player pl, BlockHitResult hit) {
      if (!handEmpty(pl)) {
         return InteractionResult.PASS;
      } else if (lv.isClientSide) {
         return InteractionResult.SUCCESS;
      } else if (!(lv.getBlockEntity(pos) instanceof RadioConsoleBE be && lv instanceof ServerLevel sl)) {
         return InteractionResult.CONSUME;
      } else {
         if (pl.isShiftKeyDown()) {
            be.refresh();
            pl.displayClientMessage(
               Component.literal(
                     be.tuning.display()
                        + "  |  TX "
                        + be.txOn
                        + "/"
                        + be.tx
                        + "  RX "
                        + be.rxOn
                        + "/"
                        + be.rx
                        + (be.relays > 0 ? "  relays " + be.relays : "")
                        + (be.radios > 0 ? "  radios " + be.radios : "")
                  )
                  .withStyle(ChatFormatting.AQUA),
               true
            );
            return InteractionResult.CONSUME;
         }

         Emitter e = RadioNetwork.seek(sl, pos, be.antennaTop(), be.tuning, be.rxGain());
         if (e != null) {
            be.tuning = e.tuning();
            be.sync();
            be.broadcastTuning();
         }

         lv.playSound(null, pos, (SoundEvent)ModSounds.SEEK.get(), SoundSource.BLOCKS, 0.4F, e != null ? 1.6F : 0.7F);
         pl.displayClientMessage(
            Component.literal(be.tuning.display() + (e != null && !e.name().isEmpty() ? "  " + e.name() : (e == null ? "  (no other station)" : ""))), true
         );
         return InteractionResult.CONSUME;
      }
   }

   protected boolean hasAnalogOutputSignal(BlockState s) {
      return true;
   }

   protected int getAnalogOutputSignal(BlockState s, Level lv, BlockPos pos) {
      return lv.getBlockEntity(pos) instanceof RadioConsoleBE be ? be.comparator() : 0;
   }
}
