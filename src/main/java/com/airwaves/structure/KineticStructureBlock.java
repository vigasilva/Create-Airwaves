/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Multiblock/tower validation and structural logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.structure;

import com.airwaves.ModRegistry;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class KineticStructureBlock extends StructureBlock implements IRotate {
   private final Set<Integer> shaftParts = new HashSet<>();

   public KineticStructureBlock(StructureDef def, Properties p, BiFunction<BlockPos, BlockState, BlockEntity> be) {
      super(def, p, be);
      int[] c = def.parts[0];

      for (int dir : new int[]{-1, 1}) {
         int k = 1;

         while (true) {
            int found = -1;

            for (int i = 1; i < def.parts.length; i++) {
               if (def.parts[i][0] == c[0] && def.parts[i][1] == c[1] && def.parts[i][2] == c[2] + dir * k) {
                  found = i;
                  break;
               }
            }

            if (found < 0) {
               break;
            }

            this.shaftParts.add(found);
            k++;
         }
      }
   }

   public boolean onDriveLine(BlockState s) {
      int p = this.partOf(s);
      return p == 0 || this.shaftParts.contains(p);
   }

   public Axis getRotationAxis(BlockState s) {
      return s.getValue(AXIS) == Axis.X ? Axis.Z : Axis.X;
   }

   public boolean hasShaftTowards(LevelReader w, BlockPos p, BlockState s, Direction face) {
      return this.onDriveLine(s) && face.getAxis() == this.getRotationAxis(s);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return (BlockEntity)(this.shaftParts.contains(this.partOf(s)) ? new KineticStructureBlock.TowerShaftBE(p, s) : super.newBlockEntity(p, s));
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level lv, BlockState s, BlockEntityType<T> t) {
      return (BlockEntityTicker<T>)(!this.onDriveLine(s) ? super.getTicker(lv, s, t) : new SmartBlockEntityTicker());
   }

   protected void onPlace(BlockState state, Level lv, BlockPos pos, BlockState old, boolean moving) {
      super.onPlace(state, lv, pos, old, moving);
      if (lv.getBlockEntity(pos) instanceof KineticBlockEntity kbe) {
         kbe.preventSpeedUpdate = 0;
         if (old.getBlock() != state.getBlock() || state.hasBlockEntity() != old.hasBlockEntity()) {
            return;
         }

         kbe.preventSpeedUpdate = 2;
      }
   }

   public void updateIndirectNeighbourShapes(BlockState st, LevelAccessor lv, BlockPos pos, int flags, int count) {
      if (!lv.isClientSide() && lv.getBlockEntity(pos) instanceof KineticBlockEntity kbe && kbe.preventSpeedUpdate <= 0) {
         kbe.warnOfMovement();
         kbe.clearKineticInformation();
         kbe.updateSpeed = true;
      }
   }

   public static class TowerShaftBE extends KineticBlockEntity {
      public TowerShaftBE(BlockPos p, BlockState s) {
         super(ModRegistry.TOWER_SHAFT_BE.get(), p, s);
      }

      public float calculateStressApplied() {
         this.lastStressApplied = 0.0F;
         return 0.0F;
      }
   }
}
