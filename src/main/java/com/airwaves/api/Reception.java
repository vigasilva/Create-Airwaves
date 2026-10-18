/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Public API types and extension events used by other mods.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.api;

public record Reception(float quality, long stationId, String name) {
   public static final Reception NOTHING = new Reception(0.0F, 0L, "");
   public static final float THRESHOLD = 0.02F;

   public boolean heard() {
      return this.quality >= 0.02F && this.stationId != 0L;
   }
}
