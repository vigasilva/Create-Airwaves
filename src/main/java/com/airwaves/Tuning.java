/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * General mod bootstrap, registry, configuration, or shared type.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Tuning(boolean fm, double freq) {
   public static final Tuning DEFAULT = new Tuning(true, 98.1);
   public static final Codec<Tuning> CODEC = RecordCodecBuilder.create(
      i -> i.group(Codec.BOOL.fieldOf("fm").forGetter(Tuning::fm), Codec.DOUBLE.fieldOf("freq").forGetter(Tuning::freq)).apply(i, Tuning::new)
   );
   public static final StreamCodec<ByteBuf, Tuning> STREAM = StreamCodec.composite(
      ByteBufCodecs.BOOL, Tuning::fm, ByteBufCodecs.DOUBLE, Tuning::freq, Tuning::new
   );

   public static double min(boolean fm) {
      return fm ? 87.5 : 530.0;
   }

   public static double max(boolean fm) {
      return fm ? 108.0 : 1700.0;
   }

   public Tuning clamped() {
      double f = Double.isFinite(this.freq) ? this.freq : (this.fm ? 98.1 : 1000.0);
      f = Math.max(min(this.fm), Math.min(max(this.fm), f));
      return new Tuning(this.fm, Math.round(f * 100.0) / 100.0);
   }

   public boolean matches(Tuning o) {
      return this.fm == o.fm && Math.round(this.freq * 100.0) == Math.round(o.freq * 100.0);
   }

   public String display() {
      return this.fm ? String.format(Locale.ROOT, "%.2f MHz FM", this.freq) : String.format(Locale.ROOT, "%.1f kHz AM", this.freq);
   }
}
