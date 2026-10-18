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
import com.airwaves.logic.BoardAnalysis;
import com.airwaves.logic.BoardDesign;
import com.airwaves.logic.BoardLayout;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;

public class BoardItem extends Item {
   public final BoardDesign design;

   public BoardItem(BoardDesign design, Properties p) {
      super(p);
      this.design = design;
   }

   public static BoardLayout layout(ItemStack st) {
      BoardLayout l = (BoardLayout)st.get((DataComponentType)ModRegistry.BOARD_LAYOUT.get());
      if (l != null) {
         return l;
      } else {
         return st.getItem() instanceof BoardItem b ? b.design.exampleLayout() : BoardDesign.LOGIC.exampleLayout();
      }
   }

   public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
      BoardAnalysis.Result r = layout(st).analyse();
      if (r.works()) {
         MutableComponent stages = Component.empty();
         boolean firstOne = true;

         for (Entry<BoardAnalysis.Stage, Integer> e : r.counts().entrySet()) {
            if (!firstOne) {
               stages.append(Component.literal(", "));
            }

            stages.append(Component.literal(e.getValue() + " ")).append(Component.translatable(e.getKey().key()));
            firstOne = false;
         }

         if (!firstOne) {
            tip.add(stages.withStyle(ChatFormatting.GRAY));
         }

         tip.add(
            Component.translatable("tooltip.airwaves.board.program", new Object[]{BoardAnalysis.describe(BoardAnalysis.program(r))})
               .withStyle(ChatFormatting.DARK_AQUA)
         );
         tip.add(Component.translatable("tooltip.airwaves.board." + this.design.id).withStyle(ChatFormatting.DARK_GRAY));
      }
   }
}
