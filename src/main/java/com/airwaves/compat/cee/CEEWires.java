/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Optional compatibility bridge for another mod or API.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.compat.cee;

import com.airwaves.cable.CableType;
import com.airwaves.power.Terminals;
import com.george_vi.electroenergetics.CEEDataComponents;
import com.george_vi.electroenergetics.CEEWireTypes;
import com.george_vi.electroenergetics.foundation.device.ElectricalDeviceBlock;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.simulation.WireType;
import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CEEWires {
   private CEEWires() {
   }

   private static WireType wireFor(CableType t) {
      return (WireType)CEEWireTypes.COPPER.get();
   }

   public static InteractionResult use(UseOnContext ctx, CableType type) {
      Level level = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      Player player = ctx.getPlayer();
      ItemStack held = ctx.getItemInHand();
      BlockState state = level.getBlockState(pos);
      if (!(state.getBlock() instanceof ElectricalDeviceBlock<?> db)) {
         return InteractionResult.PASS;
      } else {
         boolean var18 = held.has(CEEDataComponents.SELECTED_NODE);
         if (state.getBlock() instanceof CEEMachine && !var18) {
            Vec3 local = ctx.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            if (!Terminals.has(state) || Terminals.at(state, local) < 0) {
               return InteractionResult.PASS;
            }
         }

         if (player == null || !player.isShiftKeyDown()) {
            // Electro Energetics 1.1.1 supplies the state-aware overload.
            // The four-argument call in the released binary targeted an older API.
            InWorldNode hovered = InWorldNode.closestNode(level, pos, state, 1.5F, ctx.getClickLocation());

            if (!var18) {
               if (hovered == null) {
                  return InteractionResult.PASS;
               }

               held.set(CEEDataComponents.SELECTED_NODE, hovered);
               level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
               return InteractionResult.sidedSuccess(level.isClientSide);
            } else if (!(level instanceof ServerLevel sl)) {
               return InteractionResult.SUCCESS;
            } else {
               InWorldNode original = (InWorldNode)held.get(CEEDataComponents.SELECTED_NODE);
               WireType wire = wireFor(type);
               InfrastructureSavedData sd = InfrastructureSavedData.load(sl);
               if (hovered != null
                  && original != null
                  && !hovered.equals(original)
                  && (!hovered.sourcePos().equals(original.sourcePos()) || db.canSelfConnect(level, pos, state, hovered.id(), original.id()))
                  && !sd.isConnected(hovered, original)
                  && !(Math.sqrt(original.sableSourcePos(level).distSqr(hovered.sableSourcePos(level))) > Math.max(wire.getMaxLength(), type.maxLen))) {
                  BlockState os = level.getBlockState(original.sourcePos());
                  BlockState hs = level.getBlockState(hovered.sourcePos());
                  if (os.getBlock() instanceof ElectricalDeviceBlock<?> e1) {
                     e1.ensureNodesExist(sl, original.sourcePos(), os);
                  }

                  if (hs.getBlock() instanceof ElectricalDeviceBlock<?> e2) {
                     e2.ensureNodesExist(sl, hovered.sourcePos(), hs);
                  }

                  if (sd.getNodes().contains(hovered) && sd.getNodes().contains(original)) {
                     sd.connect(original, hovered, wire);
                     level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
                     if (player == null || !player.isCreative()) {
                        held.shrink(1);
                     }

                     held.remove(CEEDataComponents.SELECTED_NODE);
                     return InteractionResult.SUCCESS;
                  } else {
                     return InteractionResult.FAIL;
                  }
               } else {
                  level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.4F, 1.6F);
                  return InteractionResult.FAIL;
               }
            }
         } else {
            if (!var18) {
               return InteractionResult.PASS;
            }

            held.remove(CEEDataComponents.SELECTED_NODE);
            return InteractionResult.sidedSuccess(level.isClientSide);
         }
      }
   }
}
