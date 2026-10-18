/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Item behavior and item-backed state.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.item;

import com.airwaves.ModSounds;
import com.airwaves.client.ManualScreen;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class FieldManualItem extends Item {
   public FieldManualItem(Properties p) {
      super(p);
   }

   public InteractionResultHolder<ItemStack> use(Level lv, Player pl, InteractionHand hand) {
      if (lv.isClientSide) {
         ManualScreen.open();
      } else {
         lv.playSound(null, pl.blockPosition(), (SoundEvent)ModSounds.BOOK.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
      }

      return InteractionResultHolder.sidedSuccess(pl.getItemInHand(hand), lv.isClientSide);
   }
}
