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
import com.airwaves.be.CableNode;
import com.airwaves.be.TunableBE;
import com.airwaves.item.CableItem;
import com.airwaves.item.Tunable;
import com.airwaves.logic.Cables;
import com.airwaves.logic.ShapeUtil;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class DeviceBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private final Map<Direction, VoxelShape> shapes;

   protected DeviceBlock(Properties p, double x1, double y1, double z1, double x2, double y2, double z2) {
      super(p);
      this.shapes = ShapeUtil.make(x1, y1, z1, x2, y2, z2);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected DeviceBlock(Properties p, double[] b) {
      this(p, b[0], b[1], b[2], b[3], b[4], b[5]);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return this.shapes.get(s.getValue(FACING));
   }

   protected RenderShape getRenderShape(BlockState s) {
      return RenderShape.MODEL;
   }

   protected BlockState rotate(BlockState s, Rotation r) {
      return (BlockState)s.setValue(FACING, r.rotate((Direction)s.getValue(FACING)));
   }

   protected BlockState mirror(BlockState s, Mirror m) {
      return s.rotate(m.getRotation((Direction)s.getValue(FACING)));
   }

   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState st, Level lv, BlockPos pos, Player pl, InteractionHand h, BlockHitResult hit) {
      if (stack.getItem() instanceof CableItem) {
         return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      }

      if (stack.getItem() instanceof Tunable tn && tn.canTune(stack) && lv.getBlockEntity(pos) instanceof TunableBE be && !pl.isSecondaryUseActive()) {
         Tuning t = (Tuning)stack.getOrDefault((DataComponentType)ModRegistry.TUNING.get(), Tuning.DEFAULT);
         if (!t.matches(be.tuning)) {
            if (!lv.isClientSide) {
               be.tuning = t;
               be.sync();
               lv.playSound(null, pos, (SoundEvent)ModSounds.TUNE.get(), SoundSource.BLOCKS, 0.4F, 1.8F);
            }

            return ItemInteractionResult.sidedSuccess(lv.isClientSide);
         }
      }

      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   protected void onRemove(BlockState s, Level lv, BlockPos p, BlockState ns, boolean moved) {
      if (!s.is(ns.getBlock()) && !lv.isClientSide && lv.getBlockEntity(p) instanceof CableNode n) {
         Cables.broken(lv, p, new ArrayList<>(n.links().list()));
      }

      super.onRemove(s, lv, p, ns, moved);
   }

   protected static boolean handEmpty(Player p) {
      return p.getMainHandItem().isEmpty();
   }

   protected ItemInteractionResult crystalInsert(ItemStack stack, BlockState st, Level lv, BlockPos pos, Player pl, IntegerProperty prop) {
      int v = stack.is(Items.AMETHYST_SHARD) ? 1 : (stack.is(Items.AMETHYST_BLOCK) ? 2 : 0);
      if (v == 0) {
         return null;
      }

      if ((Integer)st.getValue(prop) != 0) {
         return ItemInteractionResult.CONSUME;
      }

      if (!lv.isClientSide) {
         lv.setBlock(pos, (BlockState)st.setValue(prop, v), 3);
         if (lv.getBlockEntity(pos) instanceof TunableBE be) {
            be.refresh();
            be.sync();
         }

         if (!pl.getAbilities().instabuild) {
            stack.shrink(1);
         }

         lv.playSound(null, pos, (SoundEvent)ModSounds.CRYSTAL_IN.get(), SoundSource.BLOCKS, 1.0F, v == 2 ? 0.7F : 1.0F);
      }

      return ItemInteractionResult.sidedSuccess(lv.isClientSide);
   }

   protected boolean crystalRemove(BlockState st, Level lv, BlockPos pos, IntegerProperty prop) {
      int v = (Integer)st.getValue(prop);
      if (v == 0) {
         return false;
      }

      lv.setBlock(pos, (BlockState)st.setValue(prop, 0), 3);
      if (lv.getBlockEntity(pos) instanceof TunableBE be) {
         be.refresh();
         be.sync();
      }

      lv.addFreshEntity(
         new ItemEntity(lv, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, new ItemStack(v == 2 ? Items.AMETHYST_BLOCK : Items.AMETHYST_SHARD))
      );
      lv.playSound(null, pos, (SoundEvent)ModSounds.CRYSTAL_OUT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
      return true;
   }
}
