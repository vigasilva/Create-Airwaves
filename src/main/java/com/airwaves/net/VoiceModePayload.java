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

public record VoiceModePayload(boolean simpleVoiceChat) implements CustomPacketPayload {
   public static final Type<VoiceModePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("airwaves", "voice_mode"));
   public static final StreamCodec<ByteBuf, VoiceModePayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, VoiceModePayload::simpleVoiceChat, VoiceModePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
