package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseThresholdProvider extends NoiseBasedStateProvider {
   public static final Codec<NoiseThresholdProvider> CODEC = RecordCodecBuilder.create(
      param0 -> noiseCodec(param0)
         .and(
            param0.group(
               Codec.floatRange(-1.0F, 1.0F).fieldOf("threshold").forGetter(l_param0 -> l_param0.threshold),
               Codec.floatRange(0.0F, 1.0F).fieldOf("high_chance").forGetter(l_param0 -> l_param0.highChance),
               BlockState.CODEC.fieldOf("default_state").forGetter(l_param0 -> l_param0.defaultState),
               Codec.list(BlockState.CODEC).fieldOf("low_states").forGetter(l_param0 -> l_param0.lowStates),
               Codec.list(BlockState.CODEC).fieldOf("high_states").forGetter(l_param0 -> l_param0.highStates)
            )
         )
         .apply(param0, NoiseThresholdProvider::new)
   );
   private final float threshold;
   private final float highChance;
   private final BlockState defaultState;
   private final List<BlockState> lowStates;
   private final List<BlockState> highStates;

   public NoiseThresholdProvider(
      long param0,
      NormalNoise.NoiseParameters param2,
      float param3,
      float param4,
      float param5,
      BlockState param6,
      List<BlockState> param7,
      List<BlockState> param8
   ) {
      super(param0, param2, param3);
      this.threshold = param4;
      this.highChance = param5;
      this.defaultState = param6;
      this.lowStates = param7;
      this.highStates = param8;
   }

   @Override
   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.NOISE_THRESHOLD_PROVIDER;
   }

   @Override
   public BlockState getState(RandomSource param0, BlockPos param1) {
      double var3 = this.getNoiseValue(param1, this.scale);
      if (var3 < this.threshold) {
         return Util.getRandom(this.lowStates, param0);
      } else {
         return param0.nextFloat() < this.highChance ? Util.getRandom(this.highStates, param0) : this.defaultState;
      }
   }
}
