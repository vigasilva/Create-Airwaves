/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Custom network payloads and packet registration.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StationInfo(boolean fm, double freq, float quality, String name) {
   public static final StreamCodec<ByteBuf, StationInfo> STREAM = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      StationInfo::fm,
      ByteBufCodecs.DOUBLE,
      StationInfo::freq,
      ByteBufCodecs.FLOAT,
      StationInfo::quality,
      ByteBufCodecs.stringUtf8(64),
      StationInfo::name,
      StationInfo::new
   );
}
