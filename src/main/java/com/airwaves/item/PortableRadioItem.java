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
import com.airwaves.logic.Emitter;
import com.airwaves.logic.RadioNetwork;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

public class PortableRadioItem extends Item implements Tunable {
   public PortableRadioItem(Properties p) {
      super(p);
   }

   public static boolean isOn(ItemStack s) {
      return (Boolean)s.getOrDefault((DataComponentType)ModRegistry.POWERED.get(), false);
   }

   public InteractionResultHolder<ItemStack> use(Level lv, Player pl, InteractionHand h) {
      ItemStack st = pl.getItemInHand(h);
      if (lv instanceof ServerLevel sl && pl instanceof ServerPlayer sp) {
         Tuning cur = (Tuning)st.getOrDefault((DataComponentType)ModRegistry.TUNING.get(), Tuning.DEFAULT);
         if (pl.isShiftKeyDown()) {
            Emitter e = RadioNetwork.seek(sl, pl.blockPosition(), pl.getEyeY(), cur, 0.9);
            st.set((DataComponentType)ModRegistry.POWERED.get(), true);
            if (e != null) {
               st.set((DataComponentType)ModRegistry.TUNING.get(), e.tuning());
               lv.playSound(null, pl.blockPosition(), (SoundEvent)ModSounds.SEEK.get(), SoundSource.PLAYERS, 0.5F, 1.6F);
            } else {
               lv.playSound(null, pl.blockPosition(), (SoundEvent)ModSounds.SEEK.get(), SoundSource.PLAYERS, 0.5F, 0.7F);
            }

            float q = RadioNetwork.refreshHandheld(sp);
            Tuning t = (Tuning)st.getOrDefault((DataComponentType)ModRegistry.TUNING.get(), Tuning.DEFAULT);
            pl.displayClientMessage(
               e == null
                  ? Component.literal("No stations in range on " + (cur.fm() ? "FM" : "AM")).withStyle(ChatFormatting.GRAY)
                  : Component.literal(t.display() + (e.name() != null && !e.name().isEmpty() ? "  " + e.name() : "")).withStyle(ChatFormatting.AQUA),
               true
            );
         } else {
            boolean on = !isOn(st);
            st.set((DataComponentType)ModRegistry.POWERED.get(), on);
            lv.playSound(null, pl.blockPosition(), (SoundEvent)ModSounds.SWITCH.get(), SoundSource.PLAYERS, 0.6F, on ? 1.3F : 0.9F);
            float q = RadioNetwork.refreshHandheld(sp);
            if (on) {
               pl.displayClientMessage(
                  Component.literal(cur.display() + (q > 0.04F ? "" : "  (no signal - sneak + right-click to seek)"))
                     .withStyle(q > 0.04F ? ChatFormatting.AQUA : ChatFormatting.GRAY),
                  true
               );
            }
         }
      }

      return InteractionResultHolder.sidedSuccess(st, lv.isClientSide);
   }

   public boolean isFoil(ItemStack s) {
      return isOn(s);
   }

   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
      tip.add(
         Component.literal(((Tuning)stack.getOrDefault((DataComponentType)ModRegistry.TUNING.get(), Tuning.DEFAULT)).display()).withStyle(ChatFormatting.AQUA)
      );
      tip.add(Component.translatable("tooltip.airwaves.portable_radio.use").withStyle(ChatFormatting.GRAY));
   }
}
