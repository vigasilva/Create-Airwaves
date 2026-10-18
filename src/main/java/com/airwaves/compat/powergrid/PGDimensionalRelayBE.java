/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Optional compatibility bridge for another mod or API.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.compat.powergrid;

import com.airwaves.be.DimensionalRelayBE;
import com.airwaves.power.ElectricLoad;
import com.airwaves.power.PowerSupply;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.IElectricEntity.CircuitBuilder;

public class PGDimensionalRelayBE extends DimensionalRelayBE implements IElectricEntity, IElectric {
   private ElectricBehaviour electric;
   private PGMachine.Load load;

   public PGDimensionalRelayBE(BlockPos p, BlockState s) {
      super(p, s);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.electric = new ElectricBehaviour(this);
      behaviours.add(this.electric);
   }

   private PGMachine.Load load() {
      if (this.load == null) {
         this.load = new PGMachine.Load();
      }

      return this.load;
   }

   public void buildCircuit(CircuitBuilder builder) {
      this.load().build(builder);
   }

   public int terminalCount() {
      return 2;
   }

   public ITerminalPlacement terminal(BlockState state, int index) {
      return PGMachine.terminal(state, index);
   }

   public void tick() {
      super.tick();
      if (this.level != null && !this.level.isClientSide) {
         this.load().tick(this);
      }
   }

   public void remove() {
      super.remove();
      if (this.electric != null) {
         this.electric.remove();
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider r, boolean clientPacket) {
      super.write(tag, r, clientPacket);
      if (clientPacket) {
         tag.putFloat("ev", this.load().logic.volts);
         tag.putFloat("ew", this.load().logic.watts);
         tag.putInt("es", this.load().logic.status.ordinal());
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider r, boolean clientPacket) {
      super.read(tag, r, clientPacket);
      if (clientPacket) {
         this.load().logic.volts = tag.getFloat("ev");
         this.load().logic.watts = tag.getFloat("ew");
         int st = tag.getInt("es");
         if (st >= 0 && st < ElectricLoad.Status.values().length) {
            this.load().logic.status = ElectricLoad.Status.values()[st];
         }
      }
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tip, boolean sneaking) {
      boolean r = super.addToGoggleTooltip(tip, sneaking);
      if (this.level != null) {
         this.load().logic.tooltip(tip, PowerSupply.at(this.level, this.worldPosition));
      }

      return r;
   }
}
