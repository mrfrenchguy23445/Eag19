package net.minecraft.world.level.biome;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public class MultiNoiseBiomeSource extends BiomeSource {
   private static final MapCodec<Holder<Biome>> ENTRY_CODEC = Biome.CODEC.fieldOf("biome");
   public static final MapCodec<Climate.ParameterList<Holder<Biome>>> DIRECT_CODEC = Climate.ParameterList.codec(ENTRY_CODEC).fieldOf("biomes");
   private static final MapCodec<Holder<MultiNoiseBiomeSourceParameterList>> PRESET_CODEC = MultiNoiseBiomeSourceParameterList.CODEC
      .fieldOf("preset")
      .withLifecycle(Lifecycle.stable());
   public static final Codec<MultiNoiseBiomeSource> CODEC = Codec.mapEither(DIRECT_CODEC, PRESET_CODEC)
      .xmap(MultiNoiseBiomeSource::new, param0 -> param0.parameters)
      .codec();
   private final Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters;

   private MultiNoiseBiomeSource(Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> param0) {
      this.parameters = param0;
   }

   public static MultiNoiseBiomeSource createFromList(Climate.ParameterList<Holder<Biome>> param0) {
      return new MultiNoiseBiomeSource(Either.left(param0));
   }

   public static MultiNoiseBiomeSource createFromPreset(Holder<MultiNoiseBiomeSourceParameterList> param0) {
      return new MultiNoiseBiomeSource(Either.right(param0));
   }

   private Climate.ParameterList<Holder<Biome>> parameters() {
      return this.parameters.map(l_param0 -> l_param0, l_param0 -> l_param0.value().parameters());
   }

   @Override
   protected Stream<Holder<Biome>> collectPossibleBiomes() {
      return this.parameters().values().stream().map(Pair::getSecond);
   }

   @Override
   protected Codec<? extends BiomeSource> codec() {
      return CODEC;
   }

   public boolean stable(ResourceKey<MultiNoiseBiomeSourceParameterList> param0) {
      Optional<Holder<MultiNoiseBiomeSourceParameterList>> var2 = this.parameters.right();
      return var2.isPresent() && var2.get().is(param0);
   }

   @Override
   public Holder<Biome> getNoiseBiome(int param0, int param1, int param2, Climate.Sampler param3) {
      return this.getNoiseBiome(param3.sample(param0, param1, param2));
   }

   @VisibleForDebug
   public Holder<Biome> getNoiseBiome(Climate.TargetPoint param0) {
      return this.parameters().findValue(param0);
   }

   @Override
   public void addDebugInfo(List<String> param0, BlockPos param1, Climate.Sampler param2) {
      int var4 = QuartPos.fromBlock(param1.getX());
      int var5 = QuartPos.fromBlock(param1.getY());
      int var6 = QuartPos.fromBlock(param1.getZ());
      Climate.TargetPoint var7 = param2.sample(var4, var5, var6);
      float var8 = Climate.unquantizeCoord(var7.continentalness());
      float var9 = Climate.unquantizeCoord(var7.erosion());
      float var10 = Climate.unquantizeCoord(var7.temperature());
      float var11 = Climate.unquantizeCoord(var7.humidity());
      float var12 = Climate.unquantizeCoord(var7.weirdness());
      double var13 = NoiseRouterData.peaksAndValleys(var12);
      OverworldBiomeBuilder var15 = new OverworldBiomeBuilder();
      param0.add(
         "Biome builder PV: "
            + OverworldBiomeBuilder.getDebugStringForPeaksAndValleys(var13)
            + " C: "
            + var15.getDebugStringForContinentalness(var8)
            + " E: "
            + var15.getDebugStringForErosion(var9)
            + " T: "
            + var15.getDebugStringForTemperature(var10)
            + " H: "
            + var15.getDebugStringForHumidity(var11)
      );
   }
}
