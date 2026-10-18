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
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class ElectricLoad {
   public static final double OPEN = 100000.0;
   private double resistance = 100000.0;
   public float volts;
   public float watts;
   public ElectricLoad.Status status = ElectricLoad.Status.IDLE;

   public double resistance() {
      return this.resistance;
   }

   public void update(PowerSupply s, double v, double joules, long gameTime) {
      this.volts = (float)Math.abs(v);
      this.watts = (float)(joules * 20.0);
      double r = 100000.0;
      if (!Config.electricEnabled()) {
         this.status = ElectricLoad.Status.DISABLED;
      } else if (s == null) {
         this.status = ElectricLoad.Status.NO_MACHINE;
      } else {
         if (joules > 0.0) {
            s.feed(joules, gameTime);
         } else {
            s.feed(0.0, gameTime);
         }

         double want = s.wantWatts();
         int rated = s.ratedVolts();
         if (this.volts > rated * 2.2) {
            this.status = ElectricLoad.Status.OVERVOLTAGE;
         } else if (want <= 0.0) {
            this.status = ElectricLoad.Status.IDLE;
         } else {
            double vv = Math.max(this.volts, rated);
            r = Mth.clamp(vv * vv / want, 0.05, 100000.0);
            this.status = ElectricLoad.Status.DRAWING;
         }
      }

      this.resistance = r;
   }

   public void tooltip(List<Component> tip, PowerSupply s) {
      if (!(this.volts < 0.05F) || this.status == ElectricLoad.Status.OVERVOLTAGE) {
         tip.add(
            Component.literal("    ⚡ " + String.format("%.1f", this.volts) + " V, " + String.format("%.1f", this.watts) + " W").withStyle(ChatFormatting.AQUA)
         );
         if (this.status == ElectricLoad.Status.OVERVOLTAGE && s != null) {
            tip.add(Component.literal("    too much voltage: max " + Math.round(s.ratedVolts() * 2.2) + " V").withStyle(ChatFormatting.RED));
         }
      }
   }

   public enum Status {
      DRAWING,
      NO_MACHINE,
      OVERVOLTAGE,
      IDLE,
      DISABLED;
   }
}
