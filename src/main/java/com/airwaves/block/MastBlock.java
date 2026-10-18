/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Minecraft block interaction, state, shape, and block-entity hookup.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.block;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class MastBlock extends Block {
   public static final BooleanProperty TOP = BooleanProperty.create("top");
   public static final BooleanProperty NARROW = BooleanProperty.create("narrow");
   public static final IntegerProperty SEG = IntegerProperty.create("seg", 0, 3);
   public static final IntegerProperty CX2 = IntegerProperty.create("cx", 0, 1);
   public static final IntegerProperty CZ2 = IntegerProperty.create("cz", 0, 1);
   public static final IntegerProperty CX4 = IntegerProperty.create("cx", 0, 3);
   public static final IntegerProperty CZ4 = IntegerProperty.create("cz", 0, 3);
   public static final IntegerProperty CX8 = IntegerProperty.create("cx", 0, 7);
   public static final IntegerProperty CZ8 = IntegerProperty.create("cz", 0, 7);
   private static final Map<Long, VoxelShape> SHAPES = new HashMap<>();

   protected MastBlock(Properties p) {
      super(p);
      BlockState d = (BlockState)((BlockState)this.stateDefinition.any()).setValue(TOP, true);
      if (d.hasProperty(NARROW)) {
         d = (BlockState)d.setValue(NARROW, false);
      }

      this.registerDefaultState(d);
   }

   public abstract int size();

   public abstract int tier();

   public abstract int maxHeight();

   public IntegerProperty px() {
      return switch (this.size()) {
         case 2 -> CX2;
         case 4 -> CX4;
         case 8 -> CX8;
         default -> null;
      };
   }

   public IntegerProperty pz() {
      return switch (this.size()) {
         case 2 -> CZ2;
         case 4 -> CZ4;
         case 8 -> CZ8;
         default -> null;
      };
   }

   protected boolean segAlways() {
      return false;
   }

   public int cx(BlockState s) {
      return this.size() > 1 ? (Integer)s.getValue(this.px()) : 0;
   }

   public int cz(BlockState s) {
      return this.size() > 1 ? (Integer)s.getValue(this.pz()) : 0;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{TOP});
      if (this.size() >= 4) {
         b.add(new Property[]{NARROW});
      }

      if (this.px() != null) {
         b.add(new Property[]{this.px(), this.pz()});
      }

      if (this.px() != null || this.segAlways()) {
         b.add(new Property[]{SEG});
      }
   }

   public int mask(BlockState s) {
      int n = this.size();
      if (n == 1) {
         return 15;
      }

      int x = this.cx(s);
      int z = this.cz(s);
      int last = n - 1;
      return (z == 0 ? 1 : 0) | (x == last ? 2 : 0) | (z == last ? 4 : 0) | (x == 0 ? 8 : 0);
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      boolean top = (Boolean)s.getValue(TOP) && (!s.hasProperty(NARROW) || !(Boolean)s.getValue(NARROW));
      int m = this.mask(s);
      long key = (long)this.size() << 8 | (long)m << 1 | (top ? 1 : 0);
      return SHAPES.computeIfAbsent(key, k -> this.build(m, top));
   }

   private VoxelShape build(int m, boolean top) {
      if (this.size() == 1) {
         VoxelShape v = Block.box(3.5, 0.0, 3.5, 12.5, top ? 9.0 : 16.0, 12.5);
         return top ? Shapes.or(v, Block.box(5.0, 9.0, 5.0, 11.0, 15.0, 11.0)) : v;
      }

      double h = top ? 9.0 : 16.0;
      VoxelShape v = Shapes.empty();
      if ((m & 1) != 0) {
         v = Shapes.or(v, Block.box(0.0, 0.0, 0.0, 16.0, h, top ? 5.0 : 1.5));
      }

      if ((m & 2) != 0) {
         v = Shapes.or(v, Block.box(top ? 11.0 : 14.5, 0.0, 0.0, 16.0, h, 16.0));
      }

      if ((m & 4) != 0) {
         v = Shapes.or(v, Block.box(0.0, 0.0, top ? 11.0 : 14.5, 16.0, h, 16.0));
      }

      if ((m & 8) != 0) {
         v = Shapes.or(v, Block.box(0.0, 0.0, 0.0, top ? 5.0 : 1.5, h, 16.0));
      }

      return v;
   }

   public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)this.defaultBlockState().setValue(TOP, !continues(c.getLevel().getBlockState(c.getClickedPos().above())));
   }

   protected BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) {
      return d == Direction.UP ? (BlockState)s.setValue(TOP, !continues(n)) : s;
   }

   public static boolean continues(BlockState above) {
      return above.getBlock() instanceof MastBlock || above.getBlock() instanceof YagiAntennaBlock || YagiAntennaBlock.isMount(above);
   }

   protected boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) {
      return true;
   }

   public int nextSeg(BlockState below) {
      return below.getBlock() instanceof MastBlock && below.hasProperty(SEG) ? ((Integer)below.getValue(SEG) + 1) % 4 : 0;
   }

   public static void setNarrow(Level lv, BlockPos origin, boolean v) {
      BlockState o = lv.getBlockState(origin);
      if (o.getBlock() instanceof MastBlock mb && o.hasProperty(NARROW)) {
         for (int dx = 0; dx < mb.size(); dx++) {
            for (int dz = 0; dz < mb.size(); dz++) {
               BlockPos q = origin.offset(dx, 0, dz);
               BlockState s = lv.getBlockState(q);
               if (s.is(mb) && (Boolean)s.getValue(NARROW) != v) {
                  lv.setBlock(q, (BlockState)s.setValue(NARROW, v), 2);
               }
            }
         }
      }
   }

   protected void onRemove(BlockState s, Level lv, BlockPos p, BlockState ns, boolean moved) {
      if (!ns.is(this) && !lv.isClientSide) {
         BlockState below = lv.getBlockState(p.below());
         if (below.getBlock() instanceof MastBlock wide && wide.size() > this.size()) {
            setNarrow(lv, wide.origin(p.below(), below), false);
         }
      }

      super.onRemove(s, lv, p, ns, moved);
   }

   public BlockPos origin(BlockPos pos, BlockState st) {
      return pos.offset(-this.cx(st), 0, -this.cz(st));
   }

   protected ItemStack layerDrop(Level lv, BlockPos origin, BlockState st) {
      return new ItemStack(this.asItem());
   }

   protected boolean customDrop() {
      return this.size() > 1;
   }

   public BlockState playerWillDestroy(Level lv, BlockPos pos, BlockState st, Player pl) {
      if (!lv.isClientSide && this.customDrop()) {
         int n = this.size();
         BlockPos o = this.origin(pos, st);
         ItemStack drop = this.layerDrop(lv, o, lv.getBlockState(o).is(this) ? lv.getBlockState(o) : st);

         for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
               BlockPos q = o.offset(dx, 0, dz);
               if (!q.equals(pos) && lv.getBlockState(q).is(this)) {
                  lv.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
               }
            }
         }

         if (!pl.isCreative()) {
            Block.popResource(lv, pos, drop);
         }
      }

      return super.playerWillDestroy(lv, pos, st, pl);
   }
}
