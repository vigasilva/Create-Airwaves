/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Public API types and extension events used by other mods.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.api;

public interface MachinePower {
   int watts();

   int ratedVolts();

   double wantWatts();

   void feed(double var1, long var3);

   double storedJoules();

   boolean running();

   boolean onRotation();

   boolean onElectricity();

   float fraction();
}
