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
import com.airwaves.be.DimensionalRelayBE;
import com.airwaves.kinetic.KineticDeviceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class DimensionalRelayBlock extends KineticDeviceBlock<DimensionalRelayBE> {
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
   private static final double[][] TERMINALS = new double[][]{{13.0, 2.0, 0.0, 15.0, 4.0, 1.0}, {1.0, 2.0, 0.0, 3.0, 4.0, 1.0}};

   @Override
   public double[][] terminals() {
      return TERMINALS;
   }

   public DimensionalRelayBlock(Properties p) {
      super(p, new double[]{0.0, 0.0, 0.0, 16.0, 16.0, 16.0});
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(ACTIVE, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      super.createBlockStateDefinition(b);
      b.add(new Property[]{ACTIVE});
   }

   public Class<DimensionalRelayBE> getBlockEntityClass() {
      return DimensionalRelayBE.class;
   }

   public BlockEntityType<? extends DimensionalRelayBE> getBlockEntityType() {
      return ModRegistry.DIM_RELAY_BE.get();
   }

   public void animateTick(BlockState s, Level lv, BlockPos p, RandomSource r) {
      if ((Boolean)s.getValue(ACTIVE)) {
         for (int i = 0; i < 2; i++) {
            lv.addParticle(
               ParticleTypes.PORTAL,
               p.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.4,
               p.getY() + 0.8 + r.nextDouble() * 0.3,
               p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.4,
               (r.nextDouble() - 0.5) * 0.6,
               r.nextDouble() * 0.4,
               (r.nextDouble() - 0.5) * 0.6
            );
         }

         if (r.nextInt(30) == 0) {
            lv.playLocalSound(p, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.08F, 1.6F + r.nextFloat() * 0.2F, false);
         }
      }
   }

   @Override
   public void onRemove(BlockState s, Level lv, BlockPos p, BlockState ns, boolean moved) {
      if (!s.is(ns.getBlock()) && !lv.isClientSide && lv.getBlockEntity(p) instanceof DimensionalRelayBE be) {
         be.keepLoaded(false);
      }

      super.onRemove(s, lv, p, ns, moved);
   }
}
