/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Public API types and extension events used by other mods.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.api.event;

import com.airwaves.logic.BoardAnalysis;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class CircuitBoxOutputEvent extends Event {
   private final ServerLevel level;
   private final BlockPos pos;
   private final ItemStack board;
   private final BoardAnalysis.Program program;
   private int output;

   public CircuitBoxOutputEvent(ServerLevel level, BlockPos pos, ItemStack board, BoardAnalysis.Program program, int output) {
      this.level = level;
      this.pos = pos;
      this.board = board;
      this.program = program;
      this.output = output;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public ItemStack getBoard() {
      return this.board;
   }

   public BoardAnalysis.Program getProgram() {
      return this.program;
   }

   public int getOutput() {
      return this.output;
   }

   public void setOutput(int output) {
      this.output = Math.max(0, Math.min(15, output));
   }
}
