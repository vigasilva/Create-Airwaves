/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Minecraft block interaction, state, shape, and block-entity hookup.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class ConnectedBlock extends Block {
   public ConnectedBlock(Properties p) {
      super(p);
      BlockState s = (BlockState)this.stateDefinition.any();

      for (BooleanProperty b : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
         s = (BlockState)s.setValue(b, false);
      }

      this.registerDefaultState(s);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN});
   }

   private BlockState connect(BlockState s, BlockGetter lv, BlockPos pos) {
      for (Direction d : Direction.values()) {
         s = (BlockState)s.setValue((Property)PipeBlock.PROPERTY_BY_DIRECTION.get(d), lv.getBlockState(pos.relative(d)).is(this));
      }

      return s;
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return this.connect(this.defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
   }

   protected BlockState updateShape(BlockState s, Direction d, BlockState other, LevelAccessor lv, BlockPos pos, BlockPos otherPos) {
      return (BlockState)s.setValue((Property)PipeBlock.PROPERTY_BY_DIRECTION.get(d), other.is(this));
   }
}
