/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Block-entity state and server-side behavior for a placed device.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.be;

import com.airwaves.ModRegistry;
import com.airwaves.api.event.CircuitBoxOutputEvent;
import com.airwaves.block.CircuitBoxBlock;
import com.airwaves.item.BoardItem;
import com.airwaves.logic.BoardAnalysis;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

public class CircuitBoxBE extends TunableBE {
   private ItemStack board = ItemStack.EMPTY;
   private BoardAnalysis.Program program;
   private int output;
   private long[] until = new long[0];
   private int[] held = new int[0];
   private int[] mem = new int[0];
   private int[] prev = new int[0];
   public static final double MOTION_RADIUS = 8.0;

   public CircuitBoxBE(BlockPos p, BlockState s) {
      super(ModRegistry.CIRCUIT_BOX_BE.get(), p, s);
   }

   public ItemStack board() {
      return this.board;
   }

   public int output() {
      return this.output;
   }

   @Override
   public double rxGain() {
      return 0.6;
   }

   public void setBoard(ItemStack b) {
      this.board = b;
      this.program = null;
      if (this.level != null && !this.level.isClientSide) {
         BlockState st = this.getBlockState();
         if ((Boolean)st.getValue(CircuitBoxBlock.BOARD) != !b.isEmpty()) {
            this.level.setBlock(this.worldPosition, (BlockState)st.setValue(CircuitBoxBlock.BOARD, !b.isEmpty()), 3);
         }
      }

      this.sync();
   }

   private BoardAnalysis.Program program() {
      if (this.program == null) {
         this.program = this.board.isEmpty() ? null : BoardAnalysis.program(BoardItem.layout(this.board).analyse());
         int n = this.program == null ? 0 : this.program.steps().size();
         if (this.until.length != n) {
            this.until = new long[n];
            this.held = new int[n];
            this.prev = new int[n];
            if (this.mem.length != n) {
               this.mem = new int[n];
            }
         }
      }

      return this.program;
   }

   public void serverTick() {
      if (this.level != null) {
         BoardAnalysis.Program p = this.board.isEmpty() ? null : this.program();
         long now = this.level.getGameTime();
         int s = 0;
         if (p != null) {
            Direction back = (Direction)this.getBlockState().getValue(CircuitBoxBlock.FACING);

            s = switch (p.source()) {
               case RADIO -> this.carrier ? Math.max(1, Math.round(this.quality * 15.0F)) : 0;
               case CLOCK -> now % p.clockTicks() < p.clockTicks() / 2 ? 15 : 0;
               case REDSTONE -> this.level.getSignal(this.worldPosition.relative(back), back);
               case DAYLIGHT -> this.daylight();
               case RAIN -> this.rain();
               case MOTION -> this.motion();
            };

            for (int k = 0; k < p.steps().size() && k < this.until.length; k++) {
               boolean rising = s > 0 && this.prev[k] == 0;
               int in = s;
               switch ((BoardAnalysis.Step)p.steps().get(k)) {
                  case INVERT:
                     s = s > 0 ? 0 : 15;
                     break;
                  case AMPLIFY:
                     s = s > 0 ? 15 : 0;
                     break;
                  case BLINK:
                     s = now / 5L % 2L == 0L ? s : 0;
                     break;
                  case ONLY_ON_AIR:
                     s = this.carrier ? s : 0;
                     break;
                  case HOLD:
                     if (s > 0) {
                        this.held[k] = s;
                        this.until[k] = now + 20L;
                     } else if (now < this.until[k]) {
                        s = this.held[k];
                     }
                     break;
                  case TOGGLE:
                     if (rising) {
                        this.mem[k] = this.mem[k] ^ 1;
                     }

                     s = this.mem[k] == 1 ? 15 : 0;
                     break;
                  case COUNT:
                     if (rising) {
                        this.mem[k] = (this.mem[k] + 1) % 16;
                     }

                     s = this.mem[k];
                     break;
                  case PULSE:
                     if (rising) {
                        this.until[k] = now + 4L;
                     }

                     s = now < this.until[k] ? 15 : 0;
                     break;
                  case ONLY_DAY:
                     s = this.daylight() >= 8 ? s : 0;
                     break;
                  case ONLY_RAIN:
                     s = this.rain() > 0 ? s : 0;
                     break;
                  case ONLY_PLAYER:
                     s = this.motion() > 0 ? s : 0;
               }

               this.prev[k] = in;
            }
         }

         if (!this.board.isEmpty() && this.level instanceof ServerLevel sl) {
            s = ((CircuitBoxOutputEvent)NeoForge.EVENT_BUS.post(new CircuitBoxOutputEvent(sl, this.worldPosition, this.board, p, s))).getOutput();
         }

         boolean lit = p != null && p.lamp() && s > 0;
         if (s != this.output) {
            this.output = s;
            this.setChanged();
            CircuitBoxBlock.updateFront(this.level, this.worldPosition, this.getBlockState());
         }

         BlockState st = this.getBlockState();
         if ((Boolean)st.getValue(CircuitBoxBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, (BlockState)st.setValue(CircuitBoxBlock.LIT, lit), 3);
         }
      }
   }

   private int daylight() {
      int v = this.level.getBrightness(LightLayer.SKY, this.worldPosition.above()) - this.level.getSkyDarken();
      return Math.max(0, Math.min(15, v));
   }

   private int rain() {
      if (!this.level.isRainingAt(this.worldPosition.above())) {
         return 0;
      } else {
         return this.level.isThundering() ? 15 : 8;
      }
   }

   private int motion() {
      Player pl = this.level
         .getNearestPlayer(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, 8.0, EntitySelector.NO_SPECTATORS);
      return pl == null
         ? 0
         : Math.max(
            1, 15 - (int)Math.floor(pl.distanceToSqr(this.worldPosition.getCenter()) < 1.0 ? 0.0 : Math.sqrt(pl.distanceToSqr(this.worldPosition.getCenter())))
         );
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tip, boolean sneaking) {
      tip.add(Component.literal("    Circuit Box").withStyle(ChatFormatting.WHITE));
      if (this.board.isEmpty()) {
         tip.add(Component.literal("    no board: right-click one in").withStyle(ChatFormatting.GOLD));
         return true;
      }

      BoardAnalysis.Program p = this.program();
      tip.add(Component.literal("    ").append(this.board.getHoverName()).withStyle(ChatFormatting.GRAY));
      tip.add(Component.literal("    " + BoardAnalysis.describe(p)).withStyle(ChatFormatting.DARK_AQUA));
      if (p.source() == BoardAnalysis.Source.RADIO || p.steps().contains(BoardAnalysis.Step.ONLY_ON_AIR)) {
         tip.add(Component.literal("    " + this.tuning.display()).withStyle(ChatFormatting.AQUA));
         tip.add(
            Component.literal(this.carrier ? "    station heard: " + Math.round(this.quality * 100.0F) + "%" : "    no station")
               .withStyle(this.carrier ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
         );
      }

      tip.add(Component.literal("    output " + this.output).withStyle(this.output > 0 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
      return true;
   }

   @Override
   protected void saveAdditional(CompoundTag tag, Provider r) {
      super.saveAdditional(tag, r);
      if (!this.board.isEmpty()) {
         tag.put("board", this.board.save(r));
      }

      tag.putInt("out", this.output);
      tag.putIntArray("mem", this.mem);
   }

   @Override
   protected void loadAdditional(CompoundTag tag, Provider r) {
      super.loadAdditional(tag, r);
      ItemStack b = tag.contains("board") ? ItemStack.parseOptional(r, tag.getCompound("board")) : ItemStack.EMPTY;
      if (!ItemStack.matches(b, this.board)) {
         this.board = b;
         this.program = null;
      }

      this.output = tag.getInt("out");
      this.mem = tag.getIntArray("mem");
   }
}
