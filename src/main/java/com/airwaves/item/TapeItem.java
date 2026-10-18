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
import com.airwaves.logic.TapeData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;

public class TapeItem extends Item {
   public TapeItem(Properties p) {
      super(p);
   }

   public static TapeData data(ItemStack s) {
      return (TapeData)s.getOrDefault((DataComponentType)ModRegistry.TAPE.get(), TapeData.EMPTY);
   }

   public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
      TapeData d = data(s);
      if (d.blank()) {
         tip.add(Component.literal("Blank").withStyle(ChatFormatting.GRAY));
      } else {
         tip.add(Component.literal("Length " + TapeData.time(d.length())).withStyle(ChatFormatting.GOLD));
         tip.add(Component.literal(d.cues().size() + " sounds recorded").withStyle(ChatFormatting.DARK_GRAY));
         if (d.hasVoice()) {
            tip.add(Component.literal("Voice: " + TapeData.time(d.voiceSeconds() * 20)).withStyle(ChatFormatting.DARK_GRAY));
         }
      }
   }

   public boolean isFoil(ItemStack s) {
      return false;
   }
}
