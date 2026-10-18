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
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.placement.PoleHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AntennaBlock extends MastBlock implements Fallable {
   public static final int PLACEMENT_HELPER = PlacementHelpers.register(new AntennaBlock.ExtendHelper());
   private static final int FALL_DELAY = 2;

   protected AntennaBlock(Properties p) {
      super(p);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
   ) {
      if (!player.isShiftKeyDown() && player.mayBuild()) {
         IPlacementHelper helper = PlacementHelpers.get(PLACEMENT_HELPER);
         return helper.matchesItem(stack) && stack.getItem() instanceof BlockItem bi
            ? helper.getOffset(player, level, state, pos, hit).placeInWorld(level, bi, player, hand, hit)
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   private static boolean supported(LevelAccessor lv, BlockPos pos) {
      BlockState below = lv.getBlockState(pos.below());
      return below.getBlock() instanceof AntennaBlock || !below.canBeReplaced();
   }

   @Override
   protected BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) {
      BlockState out = super.updateShape(s, d, n, l, p, np);
      if (d == Direction.DOWN && !supported(l, p)) {
         l.scheduleTick(p, this, 2);
      }

      return out;
   }

   protected void tick(BlockState s, ServerLevel lv, BlockPos pos, RandomSource r) {
      if (!supported(lv, pos)) {
         List<BlockPos> column = new ArrayList<>();
         MutableBlockPos m = pos.mutable();

         while (column.size() < 512 && lv.getBlockState(m).getBlock() instanceof AntennaBlock) {
            column.add(m.immutable());
            m.move(Direction.UP);
         }

         for (BlockPos p : column) {
            BlockState st = lv.getBlockState(p);
            if (st.getBlock() instanceof AntennaBlock) {
               FallingBlockEntity f = FallingBlockEntity.fall(lv, p, st);
               f.setHurtsEntities(1.0F, 6);
            }
         }
      }
   }

   public void onLand(Level lv, BlockPos pos, BlockState state, BlockState replaced, FallingBlockEntity e) {
      lv.destroyBlock(pos, true);
   }

   public abstract int[][] profile();

   @Override
   public int maxHeight() {
      int h = 0;

      for (int[] s : this.profile()) {
         h += s[1];
      }

      return h;
   }

   public int widthAt(int h) {
      for (int[] s : this.profile()) {
         if (h < s[1]) {
            return s[0];
         }

         h -= s[1];
      }

      return 0;
   }

   public static AntennaBlock forWidth(int w) {
      return (AntennaBlock)ModRegistry.ANTENNA.get();
   }

   public BlockPos column(BlockPos pos, BlockState st) {
      return this.origin(pos, st).offset(this.size() / 2, 0, this.size() / 2);
   }

   public static Object[] base(BlockGetter lv, BlockPos pos) {
      BlockState st = lv.getBlockState(pos);
      if (!(st.getBlock() instanceof AntennaBlock a)) {
         return null;
      } else {
         MutableBlockPos var9 = a.column(pos, st).mutable();
         AntennaBlock base = a;
         int n = 0;

         while (n < 512) {
            if (!(lv.getBlockState(var9).getBlock() instanceof AntennaBlock b)) {
               break;
            }

            base = b;
            n++;
            var9.move(Direction.DOWN);
         }

         return new Object[]{base, n};
      }
   }

   @Override
   protected ItemStack layerDrop(Level lv, BlockPos origin, BlockState st) {
      Object[] b = base(lv, origin.offset(this.size() / 2, 0, this.size() / 2));
      return new ItemStack(b == null ? this.asItem() : ((AntennaBlock)b[0]).asItem());
   }

   private static class ExtendHelper extends PoleHelper<Boolean> {
      ExtendHelper() {
         super(s -> s.getBlock() instanceof AntennaBlock, s -> Axis.Y, MastBlock.TOP);
      }

      public Predicate<ItemStack> getItemPredicate() {
         return i -> i.getItem() instanceof BlockItem b && b.getBlock() instanceof AntennaBlock;
      }

      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         if (state.getBlock() instanceof AntennaBlock a) {
            int var10 = this.attachedPoles(world, pos, Direction.UP);
            int below = this.attachedPoles(world, pos, Direction.DOWN);
            if (var10 + below + 1 >= a.maxHeight()) {
               return PlacementOffset.fail();
            }

            BlockPos np = pos.above(var10 + 1);
            return world.isInWorldBounds(np) && world.getBlockState(np).canBeReplaced()
               ? PlacementOffset.success(np, s -> (BlockState)s.setValue(MastBlock.TOP, !MastBlock.continues(world.getBlockState(np.above()))))
               : PlacementOffset.fail();
         } else {
            return PlacementOffset.fail();
         }
      }
   }

   public static class Small extends AntennaBlock {
      public static final MapCodec<AntennaBlock.Small> CODEC = simpleCodec(AntennaBlock.Small::new);

      public Small(Properties p) {
         super(p);
      }

      protected MapCodec<AntennaBlock.Small> codec() {
         return CODEC;
      }

      @Override
      public int size() {
         return 1;
      }

      @Override
      public int tier() {
         return 1;
      }

      @Override
      public int[][] profile() {
         return new int[][]{{1, 96}};
      }
   }
}
