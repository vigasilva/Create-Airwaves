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
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record VoiceRelayPayload(UUID talker, byte[] data) implements CustomPacketPayload {
   public static final Type<VoiceRelayPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("airwaves", "voice_relay"));
   public static final StreamCodec<ByteBuf, VoiceRelayPayload> CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC, VoiceRelayPayload::talker, ByteBufCodecs.byteArray(1024), VoiceRelayPayload::data, VoiceRelayPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
