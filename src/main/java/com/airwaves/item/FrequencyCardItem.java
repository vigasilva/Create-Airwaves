/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Item behavior and item-backed state.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.item;

import com.airwaves.ModRegistry;
import com.airwaves.ModSounds;
import com.airwaves.Tuning;
import com.airwaves.be.Tuned;
import com.airwaves.structure.StructureBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FrequencyCardItem extends Item implements Tunable {
   public FrequencyCardItem(Properties p) {
      super(p);
   }

   @Override
   public boolean canTune(ItemStack stack) {
      return stack.has((DataComponentType)ModRegistry.TUNING.get());
   }

   public boolean isFoil(ItemStack stack) {
      return stack.has((DataComponentType)ModRegistry.TUNING.get());
   }

   public InteractionResult useOn(UseOnContext ctx) {
      Player pl = ctx.getPlayer();
      if (pl != null && pl.isSecondaryUseActive()) {
         Level lv = ctx.getLevel();
         BlockPos pos = ctx.getClickedPos();
         BlockState st = lv.getBlockState(pos);
         if (lv.getBlockEntity(st.getBlock() instanceof StructureBlock sb ? sb.corePos(st, pos) : pos) instanceof Tuned t) {
            if (!lv.isClientSide) {
               ItemStack card = ctx.getItemInHand();
               card.set((DataComponentType)ModRegistry.TUNING.get(), t.getTuning());
               if (t.getStationName() != null) {
                  card.set(DataComponents.CUSTOM_NAME, t.getStationName());
               }

               lv.playSound(null, pos, (SoundEvent)ModSounds.TUNE.get(), SoundSource.PLAYERS, 0.5F, 1.4F);
               pl.displayClientMessage(Component.literal("Copied " + t.getTuning().display()).withStyle(ChatFormatting.AQUA), true);
            }

            return InteractionResult.sidedSuccess(lv.isClientSide);
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
      Tuning t = (Tuning)stack.get((DataComponentType)ModRegistry.TUNING.get());
      if (t != null) {
         tip.add(Component.literal(t.display()).withStyle(ChatFormatting.AQUA));
      } else {
         tip.add(Component.literal("Blank").withStyle(ChatFormatting.DARK_GRAY));
      }
   }
}
