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
import com.airwaves.ModSounds;
import com.airwaves.Tuning;
import com.airwaves.be.CircuitBoxBE;
import com.airwaves.item.BoardItem;
import com.airwaves.item.Tunable;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CircuitBoxBlock extends BaseEntityBlock {
   public static final MapCodec<CircuitBoxBlock> CODEC = simpleCodec(CircuitBoxBlock::new);
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   public static final BooleanProperty BOARD = BooleanProperty.create("board");
   private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 8.0, 15.0);

   public CircuitBoxBlock(Properties p) {
      super(p);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(LIT, false))
            .setValue(BOARD, false)
      );
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{FACING, LIT, BOARD});
   }

   protected RenderShape getRenderShape(BlockState s) {
      return RenderShape.MODEL;
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return SHAPE;
   }

   public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new CircuitBoxBE(p, s);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level lv, BlockState s, BlockEntityType<T> t) {
      return lv.isClientSide ? null : createTickerHelper(t, ModRegistry.CIRCUIT_BOX_BE.get(), (l, p, st, be) -> be.serverTick());
   }

   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState st, Level lv, BlockPos pos, Player pl, InteractionHand h, BlockHitResult hit) {
      if (lv.getBlockEntity(pos) instanceof CircuitBoxBE be) {
         if (stack.getItem() instanceof BoardItem && be.board().isEmpty()) {
            if (!lv.isClientSide) {
               be.setBoard(stack.copyWithCount(1));
               if (!pl.getAbilities().instabuild) {
                  stack.shrink(1);
               }

               lv.playSound(null, pos, (SoundEvent)ModSounds.SWITCH.get(), SoundSource.BLOCKS, 0.6F, 1.4F);
            }

            return ItemInteractionResult.sidedSuccess(lv.isClientSide);
         } else {
            if (stack.getItem() instanceof Tunable tn && tn.canTune(stack) && !pl.isSecondaryUseActive()) {
               Tuning t = (Tuning)stack.getOrDefault((DataComponentType)ModRegistry.TUNING.get(), Tuning.DEFAULT);
               if (!t.matches(be.getTuning())) {
                  if (!lv.isClientSide) {
                     be.setTuning(t);
                     be.sync();
                     lv.playSound(null, pos, (SoundEvent)ModSounds.TUNE.get(), SoundSource.BLOCKS, 0.4F, 1.8F);
                  }

                  return ItemInteractionResult.sidedSuccess(lv.isClientSide);
               }
            }

            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   protected InteractionResult useWithoutItem(BlockState st, Level lv, BlockPos pos, Player pl, BlockHitResult hit) {
      if (pl.isSecondaryUseActive() && lv.getBlockEntity(pos) instanceof CircuitBoxBE be && !be.board().isEmpty()) {
         if (!lv.isClientSide) {
            ItemStack b = be.board();
            be.setBoard(ItemStack.EMPTY);
            if (!pl.getInventory().add(b)) {
               Block.popResource(lv, pos.above(), b);
            }

            lv.playSound(null, pos, (SoundEvent)ModSounds.SWITCH.get(), SoundSource.BLOCKS, 0.6F, 0.9F);
         }

         return InteractionResult.sidedSuccess(lv.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   protected boolean isSignalSource(BlockState s) {
      return true;
   }

   protected int getSignal(BlockState s, BlockGetter l, BlockPos p, Direction side) {
      return side == s.getValue(FACING) && l.getBlockEntity(p) instanceof CircuitBoxBE be ? be.output() : 0;
   }

   protected int getDirectSignal(BlockState s, BlockGetter l, BlockPos p, Direction side) {
      return this.getSignal(s, l, p, side);
   }

   public boolean canConnectRedstone(BlockState s, BlockGetter l, BlockPos p, @Nullable Direction side) {
      return side != null && side.getAxis() == ((Direction)s.getValue(FACING)).getAxis();
   }

   public static void updateFront(Level lv, BlockPos pos, BlockState s) {
      Direction out = ((Direction)s.getValue(FACING)).getOpposite();
      BlockPos front = pos.relative(out);
      lv.neighborChanged(front, s.getBlock(), pos);
      lv.updateNeighborsAtExceptFromFacing(front, s.getBlock(), out.getOpposite());
   }

   protected void onRemove(BlockState s, Level lv, BlockPos pos, BlockState ns, boolean moved) {
      if (!s.is(ns.getBlock()) && lv.getBlockEntity(pos) instanceof CircuitBoxBE be && !be.board().isEmpty()) {
         Block.popResource(lv, pos, be.board());
      }

      super.onRemove(s, lv, pos, ns, moved);
      if (!s.is(ns.getBlock()) && !moved) {
         updateFront(lv, pos, s);
      }
   }
}
