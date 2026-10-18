/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Power-system abstractions and electrical integration.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.power;

import com.airwaves.Config;
import com.airwaves.api.MachinePower;
import com.airwaves.be.CreativeTransmitterBE;
import com.airwaves.be.DimensionalRelayBE;
import com.airwaves.be.JammerBE;
import com.airwaves.be.KineticTunableBE;
import com.airwaves.structure.StructureBlock;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PowerSupply implements MachinePower {
   private static final float MIN_FRACTION = 0.25F;
   private static final double BANK_SECONDS = 2.0;
   private final DoubleSupplier stress;
   private final IntSupplier volts;
   private double stored;
   private double lastUsed;
   private PowerSupply.Source source = PowerSupply.Source.NONE;
   private float fraction;
   private long lastFed = -1000L;
   private boolean wired;

   public PowerSupply(DoubleSupplier stress, IntSupplier volts) {
      this.stress = stress;
      this.volts = volts;
   }

   public static int watts(double stress) {
      return Math.max(1, (int)Math.round(stress * (Double)Config.WATTS_PER_STRESS.get()));
   }

   @Override
   public int watts() {
      return watts(this.stress.getAsDouble());
   }

   @Override
   public int ratedVolts() {
      return this.volts.getAsInt();
   }

   private double bank() {
      return this.watts() * 2.0;
   }

   public PowerSupply.Source source() {
      return this.source;
   }

   @Override
   public boolean running() {
      return this.source != PowerSupply.Source.NONE;
   }

   @Override
   public boolean onRotation() {
      return this.source == PowerSupply.Source.ROTATION;
   }

   @Override
   public boolean onElectricity() {
      return this.source == PowerSupply.Source.ELECTRIC;
   }

   @Override
   public float fraction() {
      return this.fraction;
   }

   public boolean wired() {
      return this.wired;
   }

   @Override
   public double storedJoules() {
      return this.stored;
   }

   @Override
   public double wantWatts() {
      double room = this.bank() - this.stored;
      return room <= 1.0E-6 ? 0.0 : Math.min(this.watts() * 1.5, room * 20.0);
   }

   @Override
   public void feed(double joules, long gameTime) {
      if (joules > 0.0) {
         this.stored = Math.min(this.bank(), this.stored + joules);
      }

      this.lastFed = gameTime;
   }

   public boolean update(boolean rotationOk, long gameTime) {
      PowerSupply.Source was = this.source;
      float wasF = this.fraction;
      boolean wasW = this.wired;
      this.wired = gameTime - this.lastFed < 40L;
      this.lastUsed = 0.0;
      if (rotationOk) {
         this.source = PowerSupply.Source.ROTATION;
         this.fraction = 1.0F;
      } else if (Config.electricEnabled() && this.stored > 0.0) {
         double need = this.watts();
         double use = Math.min(this.stored, need);
         float f = (float)(use / need);
         if (f >= 0.25F) {
            this.stored -= use;
            this.lastUsed = use;
            this.source = PowerSupply.Source.ELECTRIC;
            this.fraction = f;
         } else {
            this.source = PowerSupply.Source.NONE;
            this.fraction = f;
         }
      } else {
         this.source = PowerSupply.Source.NONE;
         this.fraction = 0.0F;
      }

      return was != this.source || wasW != this.wired || Math.abs(wasF - this.fraction) > 0.04F;
   }

   public void refund() {
      if (this.lastUsed > 0.0) {
         this.stored = Math.min(this.bank(), this.stored + this.lastUsed);
         this.lastUsed = 0.0;
      }
   }

   public double electricFactor() {
      return 0.5 + 0.5 * Math.min(1.0F, this.fraction);
   }

   public void tooltip(List<Component> tip, boolean wantsPower, boolean overstressed, float rpm) {
      boolean elec = Config.electricEnabled();
      switch (this.source) {
         case NONE:
            if (!wantsPower) {
               return;
            }

            if (overstressed) {
               tip.add(Component.literal("    overstressed").withStyle(ChatFormatting.RED));
            } else {
               tip.add(
                  Component.literal(
                        elec
                           ? "    needs " + Config.MIN_RPM.get() + " RPM or " + this.watts() + " W at " + this.ratedVolts() + " V"
                           : "    needs " + Config.MIN_RPM.get() + " RPM"
                     )
                     .withStyle(ChatFormatting.GOLD)
               );
            }

            if (elec && this.wired && this.fraction > 0.0F) {
               tip.add(Component.literal("    grid too weak: needs " + this.ratedVolts() + " V").withStyle(ChatFormatting.GRAY));
            }
            break;
         case ROTATION:
            tip.add(Component.literal("    ⟳ rotation, " + Math.round(rpm) + " RPM").withStyle(ChatFormatting.YELLOW));
            break;
         case ELECTRIC:
            boolean full = this.fraction >= 0.99F;
            tip.add(
               Component.literal("    ⚡ electric, " + this.watts() + " W" + (full ? "" : " (weak: " + Math.round(this.fraction * 100.0F) + "%)"))
                  .withStyle(full ? ChatFormatting.AQUA : ChatFormatting.GOLD)
            );
            if (!full) {
               tip.add(Component.literal("    the grid needs " + this.ratedVolts() + " V").withStyle(ChatFormatting.GRAY));
            }
      }
   }

   public String summary() {
      return switch (this.source) {
         case NONE -> "unpowered";
         case ROTATION -> "rotation";
         case ELECTRIC -> "electric " + this.watts() + " W" + (this.fraction < 0.99F ? " (" + Math.round(this.fraction * 100.0F) + "%)" : "");
      };
   }

   public void save(CompoundTag tag) {
      tag.putDouble("bankJ", this.stored);
      tag.putByte("psrc", (byte)this.source.ordinal());
      tag.putFloat("pfrac", this.fraction);
      tag.putBoolean("pwired", this.wired);
   }

   public void load(CompoundTag tag) {
      this.stored = tag.getDouble("bankJ");
      int s = tag.getByte("psrc");
      this.source = s >= 0 && s < PowerSupply.Source.values().length ? PowerSupply.Source.values()[s] : PowerSupply.Source.NONE;
      this.fraction = tag.getFloat("pfrac");
      this.wired = tag.getBoolean("pwired");
   }

   public static PowerSupply at(Level lv, BlockPos pos) {
      BlockState st = lv.getBlockState(pos);
      BlockPos p = st.getBlock() instanceof StructureBlock sb ? sb.corePos(st, pos) : pos;
      BlockEntity be = lv.getBlockEntity(p);
      if (be instanceof KineticTunableBE k && !(be instanceof CreativeTransmitterBE)) {
         return k.power;
      } else if (be instanceof JammerBE j) {
         return j.power;
      } else {
         return be instanceof DimensionalRelayBE d ? d.power : null;
      }
   }

   public enum Source {
      NONE,
      ROTATION,
      ELECTRIC;
   }
}
