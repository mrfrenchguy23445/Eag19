package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class TrapezoidHeight extends HeightProvider {
   public static final Codec<TrapezoidHeight> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(l_param0 -> l_param0.minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(l_param0 -> l_param0.maxInclusive),
            Codec.INT.optionalFieldOf("plateau", 0).forGetter(l_param0 -> l_param0.plateau)
         )
         .apply(param0, TrapezoidHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final int plateau;

   private TrapezoidHeight(VerticalAnchor param0, VerticalAnchor param1, int param2) {
      this.minInclusive = param0;
      this.maxInclusive = param1;
      this.plateau = param2;
   }

   public static TrapezoidHeight of(VerticalAnchor param0, VerticalAnchor param1, int param2) {
      return new TrapezoidHeight(param0, param1, param2);
   }

   public static TrapezoidHeight of(VerticalAnchor param0, VerticalAnchor param1) {
      return of(param0, param1, 0);
   }

   @Override
   public int sample(RandomSource param0, WorldGenerationContext param1) {
      int var3 = this.minInclusive.resolveY(param1);
      int var4 = this.maxInclusive.resolveY(param1);
      if (var3 > var4) {
         LOGGER.warn("Empty height range: {}", this);
         return var3;
      }

      int var5 = var4 - var3;
      if (this.plateau >= var5) {
         return Mth.randomBetweenInclusive(param0, var3, var4);
      }

      int var6 = (var5 - this.plateau) / 2;
      int var7 = var5 - var6;
      return var3 + Mth.randomBetweenInclusive(param0, 0, var7) + Mth.randomBetweenInclusive(param0, 0, var6);
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.TRAPEZOID;
   }

   @Override
   public String toString() {
      return this.plateau == 0
         ? "triangle (" + this.minInclusive + "-" + this.maxInclusive + ")"
         : "trapezoid(" + this.plateau + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
