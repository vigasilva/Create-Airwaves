/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Block-entity state and server-side behavior for a placed device.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.be;

import com.airwaves.Config;
import com.airwaves.ModRegistry;
import com.airwaves.Tuning;
import com.airwaves.compat.SableCompat;
import com.airwaves.logic.RadioNetwork;
import com.airwaves.power.PowerSupply;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class KineticTunableBE extends KineticBlockEntity implements Tuned {
   public Tuning tuning = Tuning.DEFAULT;
   public float quality;
   public boolean carrier;
   public long bestRoot;
   public Component stationName;
   public final PowerSupply power = new PowerSupply(this::stressImpact, this::ratedVolts);

   protected KineticTunableBE(BlockEntityType<?> t, BlockPos p, BlockState s) {
      super(t, p, s);
   }

   @Override
   public Tuning getTuning() {
      return this.tuning;
   }

   @Override
   public void setTuning(Tuning t) {
      this.tuning = t;
   }

   @Override
   public Component getStationName() {
      return this.stationName;
   }

   public String stationLabel() {
      return this.stationName == null ? "" : this.stationName.getString();
   }

   @Override
   public void sync() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.sendData();
      }
   }

   public void initialize() {
      super.initialize();
      if (this.level != null && !this.level.isClientSide) {
         RadioNetwork.register(this);
      }
   }

   public void invalidate() {
      super.invalidate();
      if (this.level != null && !this.level.isClientSide) {
         RadioNetwork.unregister(this);
      }
   }

   public void refresh() {
   }

   public void networkRefresh() {
      this.power.update(this.rotationOk(), this.level == null ? 0L : this.level.getGameTime());
      this.refresh();
      if (!this.usesPower()) {
         this.power.refund();
      }
   }

   public double stressImpact() {
      return 0.0;
   }

   public int ratedVolts() {
      return 120;
   }

   protected boolean usesPower() {
      return true;
   }

   public double rxGain() {
      return 1.0;
   }

   public double antennaTop() {
      return SableCompat.global(this.level, this.worldPosition).y + 0.5;
   }

   public BlockPos radioPos() {
      return SableCompat.globalPos(this.level, this.worldPosition);
   }

   public float rpm() {
      return Math.abs(this.getSpeed());
   }

   public boolean rotationOk() {
      return this.rpm() >= ((Integer)Config.MIN_RPM.get()).intValue() && !this.isOverStressed();
   }

   public boolean turning() {
      return !Config.rotation() ? true : this.power.running();
   }

   public double powerFactor() {
      if (!Config.rotation()) {
         return 1.0;
      }

      if (this.power.source() == PowerSupply.Source.ELECTRIC) {
         return this.power.electricFactor();
      }

      double f = Mth.clamp(this.rpm() / ((Integer)Config.FULL_RPM.get()).intValue(), 0.0F, 2.0F);
      return 0.5 + 0.5 * f;
   }

   public double altitudeFactor() {
      return 1.0 + (Double)Config.ALTITUDE_BONUS.get() * SableCompat.altitude(this.level, this.worldPosition) / 100.0;
   }

   protected void radioTooltip(List<Component> tip, boolean running, double range) {
      tip.add(
         Component.literal("    " + this.tuning.display() + (this.stationName != null ? "  " + this.stationName.getString() : ""))
            .withStyle(ChatFormatting.AQUA)
      );
      if (running) {
         tip.add(Component.literal("    ▲ " + (int)range + " m").withStyle(ChatFormatting.GREEN));
      }

      if (Config.rotation()) {
         this.power.tooltip(tip, true, this.isOverStressed(), this.rpm());
      }

      if (SableCompat.altitude(this.level, this.worldPosition) > 8.0) {
         tip.add(Component.literal("    airborne: ×" + String.format("%.2f", this.altitudeFactor()) + " range").withStyle(ChatFormatting.GRAY));
      }
   }

   protected void write(CompoundTag tag, Provider r, boolean clientPacket) {
      super.write(tag, r, clientPacket);
      tag.putBoolean("fm", this.tuning.fm());
      tag.putDouble("freq", this.tuning.freq());
      tag.putFloat("q", this.quality);
      tag.putBoolean("carrier", this.carrier);
      if (this.stationName != null) {
         tag.putString("station", Serializer.toJson(this.stationName, r));
      }

      this.power.save(tag);
   }

   protected void read(CompoundTag tag, Provider r, boolean clientPacket) {
      super.read(tag, r, clientPacket);
      if (tag.contains("freq")) {
         this.tuning = new Tuning(tag.getBoolean("fm"), tag.getDouble("freq")).clamped();
      }

      this.quality = tag.getFloat("q");
      this.carrier = tag.getBoolean("carrier");
      this.stationName = tag.contains("station") ? Serializer.fromJson(tag.getString("station"), r) : null;
      this.power.load(tag);
   }

   protected void applyImplicitComponents(DataComponentInput in) {
      super.applyImplicitComponents(in);
      Tuning t = (Tuning)in.get((DataComponentType)ModRegistry.TUNING.get());
      if (t != null) {
         this.tuning = t.clamped();
      }

      Component n = (Component)in.get(DataComponents.CUSTOM_NAME);
      if (n != null) {
         this.stationName = n;
      }
   }

   protected void collectImplicitComponents(Builder b) {
      super.collectImplicitComponents(b);
      b.set((DataComponentType)ModRegistry.TUNING.get(), this.tuning);
      if (this.stationName != null) {
         b.set(DataComponents.CUSTOM_NAME, this.stationName);
      }
   }

   public void removeComponentsFromTag(CompoundTag tag) {
      super.removeComponentsFromTag(tag);
      tag.remove("fm");
      tag.remove("freq");
      tag.remove("station");
   }
}
