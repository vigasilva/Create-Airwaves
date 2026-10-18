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
import com.airwaves.block.RadioBlock;
import com.airwaves.logic.ClientSinks;
import com.airwaves.logic.RadioNetwork;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class RadioBE extends TunableBE implements AudioSink {
   public RadioBE(BlockPos p, BlockState s) {
      super(ModRegistry.RADIO_BE.get(), p, s);
   }

   @Override
   public void onLoad() {
      super.onLoad();
      if (this.level != null && this.level.isClientSide) {
         ClientSinks.SINKS.add(this);
      }
   }

   @Override
   public void setRemoved() {
      super.setRemoved();
      ClientSinks.removed(this, this);
   }

   @Override
   public double rxGain() {
      return 0.8;
   }

   @Override
   public double antennaTop() {
      return super.antennaTop() + 1.3;
   }

   @Override
   public boolean sinkOn() {
      return (Boolean)this.getBlockState().getValue(RadioBlock.POWERED);
   }

   @Override
   public BlockPos sinkPos() {
      return this.worldPosition;
   }

   @Override
   public boolean carrier() {
      return this.carrier;
   }

   @Override
   public float quality() {
      return this.quality;
   }

   @Override
   public boolean fm() {
      return this.tuning.fm();
   }

   public void deliver(ServerLevel lv, UUID talker, byte[] data, float gain) {
      RadioNetwork.send(lv, this.worldPosition, talker, data, this.quality, this.tuning.fm(), gain);
   }
}
