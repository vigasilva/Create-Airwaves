/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Block-entity state and server-side behavior for a placed device.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.be;

import com.airwaves.logic.AntennaUtil;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class YagiAntennaBE extends SmartBlockEntity {
   public ScrollValueBehaviour bearing;
   public float shown = Float.NaN;

   public YagiAntennaBE(BlockEntityType<?> t, BlockPos p, BlockState s) {
      super(t, p, s);
   }

   public void addBehaviours(List<BlockEntityBehaviour> list) {
      this.bearing = new ScrollValueBehaviour(Component.literal("Bearing"), this, new YagiAntennaBE.TopBox())
         .between(0, 359)
         .withFormatter(v -> v + "° " + AntennaUtil.compass(v.intValue()));
      list.add(this.bearing);
   }

   public int bearing() {
      return this.bearing == null ? 0 : this.bearing.getValue();
   }

   public void setBearing(int deg) {
      if (this.bearing != null) {
         this.bearing.setValue(Math.floorMod(deg, 360));
      }
   }

   private static class TopBox extends CenteredSideValueBoxTransform {
      static final float TOP = 12.0F;

      TopBox() {
         super((state, d) -> d == Direction.UP);
      }

      public Vec3 getLocalOffset(LevelAccessor l, BlockPos p, BlockState s) {
         return new Vec3(0.5, 0.753125, 0.5);
      }
   }
}
