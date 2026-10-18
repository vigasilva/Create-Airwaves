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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record HandheldStatePayload(boolean on, boolean carrier, float quality, boolean fm, String label) implements CustomPacketPayload {
   public static final Type<HandheldStatePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("airwaves", "handheld_state"));
   public static final StreamCodec<ByteBuf, HandheldStatePayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      HandheldStatePayload::on,
      ByteBufCodecs.BOOL,
      HandheldStatePayload::carrier,
      ByteBufCodecs.FLOAT,
      HandheldStatePayload::quality,
      ByteBufCodecs.BOOL,
      HandheldStatePayload::fm,
      ByteBufCodecs.stringUtf8(64),
      HandheldStatePayload::label,
      HandheldStatePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
