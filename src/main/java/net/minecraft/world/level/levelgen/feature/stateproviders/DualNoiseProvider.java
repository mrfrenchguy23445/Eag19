package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class DualNoiseProvider extends NoiseProvider {
   public static final Codec<DualNoiseProvider> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            InclusiveRange.codec(Codec.INT, 1, 64).fieldOf("variety").forGetter(l_param0 -> l_param0.variety),
            NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("slow_noise").forGetter(l_param0 -> l_param0.slowNoiseParameters),
            ExtraCodecs.POSITIVE_FLOAT.fieldOf("slow_scale").forGetter(l_param0 -> l_param0.slowScale)
         )
         .and(noiseProviderCodec(param0))
         .apply(param0, DualNoiseProvider::new)
   );
   private final InclusiveRange<Integer> variety;
   private final NormalNoise.NoiseParameters slowNoiseParameters;
   private final float slowScale;
   private final NormalNoise slowNoise;

   public DualNoiseProvider(
      InclusiveRange<Integer> param0,
      NormalNoise.NoiseParameters param1,
      float param2,
      long param3,
      NormalNoise.NoiseParameters param5,
      float param6,
      List<BlockState> param7
   ) {
      super(param3, param5, param6, param7);
      this.variety = param0;
      this.slowNoiseParameters = param1;
      this.slowScale = param2;
      this.slowNoise = NormalNoise.create(new WorldgenRandom(new LegacyRandomSource(param3)), param1);
   }

   @Override
   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.DUAL_NOISE_PROVIDER;
   }

   @Override
   public BlockState getState(RandomSource param0, BlockPos param1) {
      double var3 = this.getSlowNoiseValue(param1);
      int var5 = (int)Mth.clampedMap(var3, -1.0, 1.0, this.variety.minInclusive().intValue(), this.variety.maxInclusive() + 1);
      List<BlockState> var6 = Lists.newArrayListWithCapacity(var5);

      for (int var7 = 0; var7 < var5; var7++) {
         var6.add(this.getRandomState(this.states, this.getSlowNoiseValue(param1.offset(var7 * 54545, 0, var7 * 34234))));
      }

      return this.getRandomState(var6, param1, this.scale);
   }

   protected double getSlowNoiseValue(BlockPos param0) {
      return this.slowNoise.getValue(param0.getX() * this.slowScale, param0.getY() * this.slowScale, param0.getZ() * this.slowScale);
   }
}
