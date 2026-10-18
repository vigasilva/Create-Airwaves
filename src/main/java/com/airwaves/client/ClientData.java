/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Client-only rendering, audio, UI, Ponder, or JEI behavior.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.client;

import com.airwaves.net.StationInfo;
import java.util.List;

public final class ClientData {
   public static volatile List<StationInfo> spectrum = List.of();
   public static volatile long spectrumTime;
   public static volatile boolean hhOn;
   public static volatile boolean hhCarrier;
   public static volatile boolean hhFm;
   public static volatile float hhQuality;
   public static volatile String hhLabel = "";

   private ClientData() {
   }
}
