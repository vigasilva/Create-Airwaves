/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Block-entity state and server-side behavior for a placed device.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.be;

import com.airwaves.ModRegistry;
import com.airwaves.logic.Cables;
import com.airwaves.logic.RadioNetwork;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MicrophoneBE extends NodeBE {
   public TransmitterBE feeds;
   public List<Cables.Seg> path = List.of();
   public long lastPulse;
   public float line = 1.0F;

   public MicrophoneBE(BlockPos p, BlockState s) {
      super(ModRegistry.MIC_BE.get(), p, s);
   }

   protected MicrophoneBE(BlockEntityType<?> t, BlockPos p, BlockState s) {
      super(t, p, s);
   }

   @Override
   public int maxLinks(int slot) {
      return 1;
   }

   @Override
   public Vec3 anchor(int slot) {
      return CableNode.facing(this, 8.0, 0.5, 11.0);
   }

   public void onLoad() {
      super.onLoad();
      if (this.level != null && !this.level.isClientSide) {
         RadioNetwork.register(this);
      }
   }

   public void setRemoved() {
      super.setRemoved();
      if (this.level != null && !this.level.isClientSide) {
         RadioNetwork.unregister(this);
      }
   }
}
