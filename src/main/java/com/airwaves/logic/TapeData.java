/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Core radio simulation and shared gameplay logic.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record TapeData(int length, List<TapeData.Cue> cues, String audio, int voiceSeconds) {
   public static final int MAX_TICKS = 6000;
   public static final int MAX_CUES = 4000;
   public static final TapeData EMPTY = new TapeData(0, List.of(), "", 0);
   public static final Codec<TapeData> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.INT.fieldOf("length").forGetter(TapeData::length),
            TapeData.Cue.CODEC.listOf().fieldOf("cues").forGetter(TapeData::cues),
            Codec.STRING.optionalFieldOf("audio", "").forGetter(TapeData::audio),
            Codec.INT.optionalFieldOf("voice", 0).forGetter(TapeData::voiceSeconds)
         )
         .apply(i, TapeData::new)
   );
   public static final StreamCodec<ByteBuf, TapeData> STREAM = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      TapeData::length,
      TapeData.Cue.STREAM.apply(ByteBufCodecs.list()),
      TapeData::cues,
      ByteBufCodecs.STRING_UTF8,
      TapeData::audio,
      ByteBufCodecs.VAR_INT,
      TapeData::voiceSeconds,
      TapeData::new
   );

   public boolean blank() {
      return this.length <= 0;
   }

   public static String time(int ticks) {
      int s = Math.max(0, ticks) / 20;
      return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
   }

   public boolean hasVoice() {
      return !this.audio.isEmpty();
   }

   public static TapeData of(int length, List<TapeData.Cue> cues, String audio, int voiceSeconds) {
      return new TapeData(length, new ArrayList<>(cues), audio, voiceSeconds);
   }

   public record Cue(int tick, ResourceLocation sound, float volume, float pitch, byte kind) {
      public static final Codec<TapeData.Cue> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.INT.fieldOf("t").forGetter(TapeData.Cue::tick),
               ResourceLocation.CODEC.fieldOf("s").forGetter(TapeData.Cue::sound),
               Codec.FLOAT.fieldOf("v").forGetter(TapeData.Cue::volume),
               Codec.FLOAT.fieldOf("p").forGetter(TapeData.Cue::pitch),
               Codec.BYTE.optionalFieldOf("k", (byte)0).forGetter(TapeData.Cue::kind)
            )
            .apply(i, TapeData.Cue::new)
      );
      public static final StreamCodec<ByteBuf, TapeData.Cue> STREAM = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         TapeData.Cue::tick,
         ResourceLocation.STREAM_CODEC,
         TapeData.Cue::sound,
         ByteBufCodecs.FLOAT,
         TapeData.Cue::volume,
         ByteBufCodecs.FLOAT,
         TapeData.Cue::pitch,
         ByteBufCodecs.BYTE,
         TapeData.Cue::kind,
         TapeData.Cue::new
      );
   }
}
