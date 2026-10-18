/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * General mod bootstrap, registry, configuration, or shared type.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class Config {
   public static final ModConfigSpec SPEC;
   public static final BooleanValue REQUIRE_ROTATION;
   public static final IntValue MIN_RPM;
   public static final IntValue FULL_RPM;
   public static final DoubleValue ALTITUDE_BONUS;
   public static final BooleanValue ELECTRIC;
   public static final DoubleValue WATTS_PER_STRESS;
   public static final BooleanValue DIM_RELAY_CHUNK;
   private static Boolean powerGrid;
   private static Boolean cee;

   public static boolean rotation() {
      return (Boolean)REQUIRE_ROTATION.get();
   }

   public static boolean powerGridLoaded() {
      if (powerGrid == null) {
         powerGrid = ModList.get() != null && ModList.get().isLoaded("powergrid");
      }

      return powerGrid;
   }

   public static boolean ceeLoaded() {
      if (cee == null) {
         cee = ModList.get() != null && ModList.get().isLoaded("electroenergetics");
      }

      return cee;
   }

   public static boolean electricEnabled() {
      return !powerGridLoaded() && !ceeLoaded() ? false : !SPEC.isLoaded() || (Boolean)ELECTRIC.get();
   }

   private Config() {
   }

   static {
      Builder b = new Builder();
      REQUIRE_ROTATION = b.comment("Transmitters, relay towers, radio towers and jammers need power (rotation, or electricity, see below) to run")
         .define("requireRotation", true);
      MIN_RPM = b.comment("Slowest shaft speed that still runs a powered device").defineInRange("minRpm", 16, 1, 256);
      FULL_RPM = b.comment("Shaft speed for full transmit power; faster shafts give more range, up to twice this speed").defineInRange("fullRpm", 64, 1, 256);
      ALTITUDE_BONUS = b.comment("Range bonus per 100 blocks a transmitter or relay flies above the ground (Create: Aeronautics airships)")
         .defineInRange("altitudeBonus", 0.5, 0.0, 4.0);
      b.push("electricity");
      ELECTRIC = b.comment(
            new String[]{
               "With Create: Power Grid or Create: Electro Energetics installed, powered machines can also run on electricity through their built-in terminals.",
               "Rotation is always used first."
            }
         )
         .define("electricPower", true);
      WATTS_PER_STRESS = b.comment("Watts a machine draws on electricity per stress unit (SU per RPM) it takes on rotation")
         .defineInRange("wattsPerStress", 2.5, 0.01, 1000.0);
      b.pop();
      DIM_RELAY_CHUNK = b.comment("A running Dimensional Relay keeps its own chunk loaded, so the link to the other dimension never drops")
         .define("dimensionalRelayLoadsChunk", true);
      SPEC = b.build();
   }
}
