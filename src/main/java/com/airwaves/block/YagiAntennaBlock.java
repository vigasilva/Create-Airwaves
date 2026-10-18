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
import com.airwaves.be.YagiAntennaBE;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class YagiAntennaBlock extends Block implements IBE<YagiAntennaBE> {
   public static final TagKey<Block> MOUNTS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("airwaves", "yagi_mounts"));
   private static final VoxelShape SHAPE = Shapes.or(box(7.0, 0.0, 7.5, 9.0, 11.0, 9.5), box(0.0, 7.0, 0.0, 16.0, 12.0, 16.0));

   public YagiAntennaBlock(Properties p) {
      super(p);
   }

   public static boolean isMount(BlockState s) {
      return s.is(MOUNTS);
   }

   protected boolean canSurvive(BlockState s, LevelReader lv, BlockPos pos) {
      return isMount(lv.getBlockState(pos.below())) && lv.getBlockState(pos.below(2)).getBlock() instanceof AntennaBlock;
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext c) {
      BlockState s = this.defaultBlockState();
      return this.canSurvive(s, c.getLevel(), c.getClickedPos()) ? s : null;
   }

   protected BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) {
      return d == Direction.DOWN && !this.canSurvive(s, l, p) ? Blocks.AIR.defaultBlockState() : super.updateShape(s, d, n, l, p, np);
   }

   public void setPlacedBy(Level lv, BlockPos pos, BlockState s, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(lv, pos, s, placer, stack);
      if (placer != null) {
         this.withBlockEntityDo(lv, pos, be -> be.setBearing(Math.round(placer.getYRot() + 180.0F) % 360));
      }
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return SHAPE;
   }

   public Class<YagiAntennaBE> getBlockEntityClass() {
      return YagiAntennaBE.class;
   }

   public BlockEntityType<? extends YagiAntennaBE> getBlockEntityType() {
      return ModRegistry.YAGI_BE.get();
   }
}
