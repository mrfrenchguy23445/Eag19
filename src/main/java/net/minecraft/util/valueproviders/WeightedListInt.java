package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;

public class WeightedListInt extends IntProvider {
   public static final Codec<WeightedListInt> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(SimpleWeightedRandomList.wrappedCodec(IntProvider.CODEC).fieldOf("distribution").forGetter(l_param0 -> l_param0.distribution))
         .apply(param0, WeightedListInt::new)
   );
   private final SimpleWeightedRandomList<IntProvider> distribution;
   private final int minValue;
   private final int maxValue;

   public WeightedListInt(SimpleWeightedRandomList<IntProvider> param0) {
      this.distribution = param0;
      List<WeightedEntry.Wrapper<IntProvider>> var2 = param0.unwrap();
      int var3 = Integer.MAX_VALUE;
      int var4 = Integer.MIN_VALUE;

      for (WeightedEntry.Wrapper<IntProvider> var6 : var2) {
         int var7 = var6.getData().getMinValue();
         int var8 = var6.getData().getMaxValue();
         var3 = Math.min(var3, var7);
         var4 = Math.max(var4, var8);
      }

      this.minValue = var3;
      this.maxValue = var4;
   }

   @Override
   public int sample(RandomSource param0) {
      return this.distribution.getRandomValue(param0).orElseThrow(IllegalStateException::new).sample(param0);
   }

   @Override
   public int getMinValue() {
      return this.minValue;
   }

   @Override
   public int getMaxValue() {
      return this.maxValue;
   }

   @Override
   public IntProviderType<?> getType() {
      return IntProviderType.WEIGHTED_LIST;
   }
}
