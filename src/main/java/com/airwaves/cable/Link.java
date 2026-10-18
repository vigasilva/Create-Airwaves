/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Cable nodes, link references, and wire-system integration.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.cable;

import net.minecraft.core.BlockPos;

public record Link(int slot, BlockPos other, int otherSlot, CableType type) {
   public NodeRef far() {
      return new NodeRef(this.other, this.otherSlot);
   }
}
