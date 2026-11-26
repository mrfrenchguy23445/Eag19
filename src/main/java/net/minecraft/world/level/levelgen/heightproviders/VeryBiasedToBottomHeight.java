package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class VeryBiasedToBottomHeight extends HeightProvider {
   public static final Codec<VeryBiasedToBottomHeight> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(l_param0 -> l_param0.minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(l_param0 -> l_param0.maxInclusive),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("inner", 1).forGetter(l_param0 -> l_param0.inner)
         )
         .apply(param0, VeryBiasedToBottomHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final int inner;

   private VeryBiasedToBottomHeight(VerticalAnchor param0, VerticalAnchor param1, int param2) {
      this.minInclusive = param0;
      this.maxInclusive = param1;
      this.inner = param2;
   }

   public static VeryBiasedToBottomHeight of(VerticalAnchor param0, VerticalAnchor param1, int param2) {
      return new VeryBiasedToBottomHeight(param0, param1, param2);
   }

   @Override
   public int sample(RandomSource param0, WorldGenerationContext param1) {
      int var3 = this.minInclusive.resolveY(param1);
      int var4 = this.maxInclusive.resolveY(param1);
      if (var4 - var3 - this.inner + 1 <= 0) {
         LOGGER.warn("Empty height range: {}", this);
         return var3;
      } else {
         int var5 = Mth.nextInt(param0, var3 + this.inner, var4);
         int var6 = Mth.nextInt(param0, var3, var5 - 1);
         return Mth.nextInt(param0, var3, var6 - 1 + this.inner);
      }
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.VERY_BIASED_TO_BOTTOM;
   }

   @Override
   public String toString() {
      return "biased[" + this.minInclusive + "-" + this.maxInclusive + " inner: " + this.inner + "]";
   }
}
