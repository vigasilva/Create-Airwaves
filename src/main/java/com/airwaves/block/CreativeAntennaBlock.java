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
import com.airwaves.be.CreativeAntennaBE;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CreativeAntennaBlock extends Block implements IBE<CreativeAntennaBE> {
   private static final VoxelShape SHAPE = Shapes.or(Block.box(3.0, 0.0, 3.0, 13.0, 4.0, 13.0), Block.box(6.0, 4.0, 6.0, 10.0, 16.0, 10.0));

   public CreativeAntennaBlock(Properties p) {
      super(p);
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return SHAPE;
   }

   public Class<CreativeAntennaBE> getBlockEntityClass() {
      return CreativeAntennaBE.class;
   }

   public BlockEntityType<? extends CreativeAntennaBE> getBlockEntityType() {
      return ModRegistry.CREATIVE_ANTENNA_BE.get();
   }
}
