/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Minecraft block interaction, state, shape, and block-entity hookup.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.block;

import com.airwaves.be.CircuitWorkbenchBE;
import com.airwaves.menu.CircuitWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CircuitWorkbenchBlock extends Block implements EntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

   public CircuitWorkbenchBlock(Properties p) {
      super(p);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return SHAPE;
   }

   protected InteractionResult useWithoutItem(BlockState st, Level lv, BlockPos pos, Player pl, BlockHitResult hit) {
      if (!lv.isClientSide) {
         pl.openMenu(st.getMenuProvider(lv, pos));
      }

      return InteractionResult.sidedSuccess(lv.isClientSide);
   }

   protected MenuProvider getMenuProvider(BlockState st, Level lv, BlockPos pos) {
      CircuitWorkbenchBE be = lv.getBlockEntity(pos) instanceof CircuitWorkbenchBE b ? b : null;
      return new SimpleMenuProvider(
         (id, inv, p) -> new CircuitWorkbenchMenu(id, inv, ContainerLevelAccess.create(lv, pos), be),
         Component.translatable("container.airwaves.circuit_workbench")
      );
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
      return new CircuitWorkbenchBE(pos, st);
   }

   protected void onRemove(BlockState st, Level lv, BlockPos pos, BlockState now, boolean moved) {
      if (!st.is(now.getBlock()) && lv.getBlockEntity(pos) instanceof CircuitWorkbenchBE be) {
         Containers.dropContents(lv, pos, be.items);
      }

      super.onRemove(st, lv, pos, now, moved);
   }
}
