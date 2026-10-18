/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * General mod bootstrap, registry, configuration, or shared type.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves;

public enum Tier {
   SMALL(1, "small"),
   MEDIUM(2, "medium"),
   LARGE(3, "large"),
   GIANT(4, "giant");

   public final int level;
   public final String id;

   Tier(int level, String id) {
      this.level = level;
      this.id = id;
   }

   public static Tier of(int level) {
      return values()[Math.max(1, Math.min(4, level)) - 1];
   }
}
