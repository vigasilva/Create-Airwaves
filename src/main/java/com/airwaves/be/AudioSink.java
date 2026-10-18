/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Block-entity state and server-side behavior for a placed device.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.be;

import net.minecraft.core.BlockPos;

public interface AudioSink {
   BlockPos sinkPos();

   boolean sinkOn();

   boolean carrier();

   float quality();

   boolean fm();

   default boolean ambientOn() {
      return this.sinkOn();
   }
}
