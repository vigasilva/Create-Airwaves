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
import com.airwaves.be.JammerBE;
import com.airwaves.kinetic.KineticDeviceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class JammerBlock extends KineticDeviceBlock<JammerBE> {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final double[][] TERMINALS = new double[][]{{11.0, 1.0, 2.0, 13.0, 3.0, 3.0}, {3.0, 1.0, 2.0, 5.0, 3.0, 3.0}};

   @Override
   public double[][] terminals() {
      return TERMINALS;
   }

   public JammerBlock(Properties p) {
      super(p, new double[]{0.0, 0.0, 2.0, 16.0, 13.0, 14.0});
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POWERED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      super.createBlockStateDefinition(b);
      b.add(new Property[]{POWERED});
   }

   public Class<JammerBE> getBlockEntityClass() {
      return JammerBE.class;
   }

   public BlockEntityType<? extends JammerBE> getBlockEntityType() {
      return ModRegistry.JAMMER_BE.get();
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)super.getStateForPlacement(c).setValue(POWERED, c.getLevel().hasNeighborSignal(c.getClickedPos()));
   }

   protected void neighborChanged(BlockState s, Level l, BlockPos p, Block b, BlockPos from, boolean moved) {
      if (!l.isClientSide) {
         boolean pw = l.hasNeighborSignal(p);
         if (pw != (Boolean)s.getValue(POWERED)) {
            l.setBlock(p, (BlockState)s.setValue(POWERED, pw), 3);
         }
      }
   }
}
