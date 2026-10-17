package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class BiasedToBottomHeight extends HeightProvider {
   public static final Codec<BiasedToBottomHeight> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(l_param0 -> l_param0.minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(l_param0 -> l_param0.maxInclusive),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("inner", 1).forGetter(l_param0 -> l_param0.inner)
         )
         .apply(param0, BiasedToBottomHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final int inner;

   private BiasedToBottomHeight(VerticalAnchor param0, VerticalAnchor param1, int param2) {
      this.minInclusive = param0;
      this.maxInclusive = param1;
      this.inner = param2;
   }

   public static BiasedToBottomHeight of(VerticalAnchor param0, VerticalAnchor param1, int param2) {
      return new BiasedToBottomHeight(param0, param1, param2);
   }

   @Override
   public int sample(RandomSource param0, WorldGenerationContext param1) {
      int var3 = this.minInclusive.resolveY(param1);
      int var4 = this.maxInclusive.resolveY(param1);
      if (var4 - var3 - this.inner + 1 <= 0) {
         LOGGER.warn("Empty height range: {}", this);
         return var3;
      } else {
         int var5 = param0.nextInt(var4 - var3 - this.inner + 1);
         return param0.nextInt(var5 + this.inner) + var3;
      }
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.BIASED_TO_BOTTOM;
   }

   @Override
   public String toString() {
      return "biased[" + this.minInclusive + "-" + this.maxInclusive + " inner: " + this.inner + "]";
   }
}
