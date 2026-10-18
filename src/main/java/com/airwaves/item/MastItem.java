/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Item behavior and item-backed state.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.item;

import com.airwaves.block.AntennaBlock;
import com.airwaves.block.MastBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MastItem extends BlockItem {
   protected final MastBlock mb;

   public MastItem(MastBlock b, Properties p) {
      super(b, p);
      this.mb = b;
   }

   public InteractionResult place(BlockPlaceContext ctx) {
      Level lv = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      Player pl = ctx.getPlayer();
      MastBlock target = this.mb;
      BlockPos below = pos.below();
      BlockState belowSt = lv.getBlockState(below);
      int seg = 0;
      boolean narrowing = false;
      BlockPos origin;
      if (belowSt.getBlock() instanceof AntennaBlock under && this.mb instanceof AntennaBlock) {
         Object[] b = AntennaBlock.base(lv, below);
         if (b == null || b[0] != this.mb) {
            return InteractionResult.FAIL;
         }

         int w = ((AntennaBlock)this.mb).widthAt((Integer)b[1]);
         if (w == 0) {
            return InteractionResult.FAIL;
         }

         target = AntennaBlock.forWidth(w);
         BlockPos col = under.column(below, belowSt);
         origin = col.offset(-(w / 2), 1, -(w / 2));
         seg = target.nextSeg(lv.getBlockState(col));
         narrowing = w < under.size();
      } else if (belowSt.getBlock() instanceof MastBlock other && other == this.mb) {
         origin = this.mb.origin(below, belowSt).above();
         seg = this.mb.nextSeg(belowSt);
      } else {
         int n = this.mb.size();
         origin = pos.offset(-((n - 1) / 2), 0, -((n - 1) / 2));
      }

      int n = target.size();

      for (int dx = 0; dx < n; dx++) {
         for (int dz = 0; dz < n; dz++) {
            BlockPos q = origin.offset(dx, 0, dz);
            if (!lv.isInWorldBounds(q) || !lv.getBlockState(q).canBeReplaced()) {
               return InteractionResult.FAIL;
            }
         }
      }

      if (!lv.isClientSide) {
         for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
               BlockPos q = origin.offset(dx, 0, dz);
               BlockState s = (BlockState)target.defaultBlockState().setValue(MastBlock.TOP, !(lv.getBlockState(q.above()).getBlock() instanceof MastBlock));
               if (n > 1) {
                  s = (BlockState)((BlockState)s.setValue(target.px(), dx)).setValue(target.pz(), dz);
               }

               if (s.hasProperty(MastBlock.SEG)) {
                  s = (BlockState)s.setValue(MastBlock.SEG, seg);
               }

               lv.setBlock(q, s, 3);
            }
         }

         if (narrowing) {
            MastBlock.setNarrow(lv, belowSt.getBlock() instanceof MastBlock wb ? wb.origin(below, belowSt) : below, true);
         }

         lv.playSound(null, pos, target.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
         if (pl == null || !pl.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
         }
      }

      return InteractionResult.sidedSuccess(lv.isClientSide);
   }
}
