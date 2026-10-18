/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Multiblock/tower validation and structural logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.structure;

public final class StructureSizes {
   public static final int RELAY_TOWER_SMALL = 28;
   public static final int RELAY_TOWER_BIG = 201;
   public static final int RADIO_TOWER = 211;

   private StructureSizes() {
   }
}
