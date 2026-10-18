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
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record RadioAudioPayload(BlockPos sink, UUID talker, byte[] data, float quality, boolean fm, float gain) implements CustomPacketPayload {
   public static final Type<RadioAudioPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("airwaves", "radio_audio"));
   public static final StreamCodec<ByteBuf, RadioAudioPayload> CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      RadioAudioPayload::sink,
      UUIDUtil.STREAM_CODEC,
      RadioAudioPayload::talker,
      ByteBufCodecs.byteArray(1024),
      RadioAudioPayload::data,
      ByteBufCodecs.FLOAT,
      RadioAudioPayload::quality,
      ByteBufCodecs.BOOL,
      RadioAudioPayload::fm,
      ByteBufCodecs.FLOAT,
      RadioAudioPayload::gain,
      RadioAudioPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
