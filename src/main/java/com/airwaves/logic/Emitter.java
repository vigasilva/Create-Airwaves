/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Core radio simulation and shared gameplay logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.logic;

import com.airwaves.Tuning;
import net.minecraft.core.BlockPos;

public record Emitter(long root, BlockPos pos, double topY, Tuning tuning, double range, float baseQ, String name, Float beam) {
   public static final double CREATIVE = 1.0E8;

   public Emitter(long root, BlockPos pos, double topY, Tuning tuning, double range, float baseQ, String name) {
      this(root, pos, topY, tuning, range, baseQ, name, null);
   }

   public double rangeTo(BlockPos at) {
      return this.beam == null ? this.range : this.range * AntennaUtil.pattern(this.beam, at.getX() - this.pos.getX(), at.getZ() - this.pos.getZ());
   }

   public boolean creative() {
      return this.range >= 1.0E8;
   }
}
