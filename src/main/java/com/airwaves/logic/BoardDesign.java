/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Core radio simulation and shared gameplay logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.logic;

import com.airwaves.ModRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum BoardDesign {
   RADIO("radio_board", -10444560, "........", "-kc-xc-.", "..l..t--", "..r--r..", "........"),
   RF("rf_board", -2076592, "........", "-xc--u--", "..k..r..", "..c.....", "........"),
   TUNER("tuner_board", -11220768, "........", "-kc--xc-", "........", "........", "........"),
   AMPLIFIER("amplifier_board", -1533376, "........", "..u--u..", "-ru--ur-", "..c.....", "........"),
   AUDIO("audio_board", -9383840, "........", "-rc--rt-", ".....t..", "........", "........"),
   DISPLAY("display_board", -999360, "........", ".l..l..l", "-r--r--r", "....t...", "........"),
   CONTROL("control_board", -5213968, "........", "-tt--rl-", "........", "........", "........"),
   COMPUTER("computer_board", -12558112, "........", "-rc-m-n-", "........", "........", "........"),
   SENSOR("sensor_board", -12533600, "........", "-d--q-rl", "........", "........", "........"),
   LOGIC("logic_board", -6642512, "........", "-kc-tt-.", "......rc", "........", "........");

   public static final int W = 8;
   public static final int H = 5;
   public static final int CELLS = 40;
   public static final int EMPTY = -1;
   public static final int TRACE = -2;
   public final String id;
   public final int accent;
   private final String[] rows;

   BoardDesign(String id, int accent, String... rows) {
      this.id = id;
      this.accent = accent;
      this.rows = rows;
   }

   public Item item() {
      return ModRegistry.board(this);
   }

   public static BoardDesign of(Item item) {
      for (BoardDesign d : values()) {
         if (d.item() == item) {
            return d;
         }
      }

      return null;
   }

   public int[] example() {
      int[] out = new int[40];

      for (int r = 0; r < 5; r++) {
         for (int c = 0; c < 8; c++) {
            char ch = this.rows[r].charAt(c);
            out[r * 8 + c] = ch == '-' ? -2 : (ch == '.' ? -1 : BoardDesign.Part.byLetter(ch).ordinal());
         }
      }

      return out;
   }

   public BoardLayout exampleLayout() {
      return BoardLayout.of(this.example());
   }

   public enum Part {
      RESISTOR('r'),
      CAPACITOR('c'),
      LED('l'),
      TRANSISTOR('t'),
      TUBE('u'),
      COIL('k'),
      CRYSTAL('x'),
      MEMORY_CHIP('m'),
      COUNTER_CHIP('n'),
      PULSE_CHIP('q'),
      LIGHT_SENSOR('d'),
      RAIN_SENSOR('w'),
      MOTION_SENSOR('o');

      public final char letter;

      Part(char letter) {
         this.letter = letter;
      }

      public Item item() {
         return switch (this) {
            case RESISTOR -> (Item)ModRegistry.RESISTOR.get();
            case CAPACITOR -> (Item)ModRegistry.CAPACITOR.get();
            case LED -> (Item)ModRegistry.LED.get();
            case TRANSISTOR -> (Item)ModRegistry.TRANSISTOR.get();
            case TUBE -> ModRegistry.electronTube();
            case COIL -> (Item)ModRegistry.COPPER_COIL.get();
            case CRYSTAL -> (Item)ModRegistry.CRYSTAL_OSCILLATOR.get();
            case MEMORY_CHIP -> (Item)ModRegistry.MEMORY_CHIP.get();
            case COUNTER_CHIP -> (Item)ModRegistry.COUNTER_CHIP.get();
            case PULSE_CHIP -> (Item)ModRegistry.PULSE_CHIP.get();
            case LIGHT_SENSOR -> (Item)ModRegistry.LIGHT_SENSOR.get();
            case RAIN_SENSOR -> (Item)ModRegistry.RAIN_SENSOR.get();
            case MOTION_SENSOR -> (Item)ModRegistry.MOTION_SENSOR.get();
         };
      }

      public static BoardDesign.Part of(ItemStack s) {
         if (s.isEmpty()) {
            return null;
         }

         for (BoardDesign.Part p : values()) {
            if (s.is(p.item())) {
               return p;
            }
         }

         return null;
      }

      public boolean standalone() {
         return this.ordinal() >= MEMORY_CHIP.ordinal();
      }

      public static BoardDesign.Part byLetter(char c) {
         for (BoardDesign.Part p : values()) {
            if (p.letter == c) {
               return p;
            }
         }

         throw new IllegalArgumentException("no part '" + c + "'");
      }
   }
}
