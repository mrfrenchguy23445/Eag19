package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.apache.commons.lang3.mutable.MutableBoolean;

public record PlacedFeature(Holder<ConfiguredFeature<?, ?>> feature, List<PlacementModifier> placement) {
   public static final Codec<PlacedFeature> DIRECT_CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            ConfiguredFeature.CODEC.fieldOf("feature").forGetter(l_param0 -> l_param0.feature),
            PlacementModifier.CODEC.listOf().fieldOf("placement").forGetter(l_param0 -> l_param0.placement)
         )
         .apply(param0, PlacedFeature::new)
   );
   public static final Codec<Holder<PlacedFeature>> CODEC = RegistryFileCodec.create(Registries.PLACED_FEATURE, DIRECT_CODEC);
   public static final Codec<HolderSet<PlacedFeature>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.PLACED_FEATURE, DIRECT_CODEC);
   public static final Codec<List<HolderSet<PlacedFeature>>> LIST_OF_LISTS_CODEC = RegistryCodecs.homogeneousList(Registries.PLACED_FEATURE, DIRECT_CODEC, true)
      .listOf();

   public boolean place(WorldGenLevel param0, ChunkGenerator param1, RandomSource param2, BlockPos param3) {
      return this.placeWithContext(new PlacementContext(param0, param1, Optional.empty()), param2, param3);
   }

   public boolean placeWithBiomeCheck(WorldGenLevel param0, ChunkGenerator param1, RandomSource param2, BlockPos param3) {
      return this.placeWithContext(new PlacementContext(param0, param1, Optional.of(this)), param2, param3);
   }

   private boolean placeWithContext(PlacementContext param0, RandomSource param1, BlockPos param2) {
      Stream<BlockPos> var4 = Stream.of(param2);

      for (PlacementModifier var6 : this.placement) {
         var4 = var4.flatMap(l_param3 -> var6.getPositions(param0, param1, l_param3));
      }

      ConfiguredFeature<?, ?> var5 = this.feature.value();
      MutableBoolean var6 = new MutableBoolean();
      var4.forEach(l_param4 -> {
         if (var5.place(param0.getLevel(), param0.generator(), param1, l_param4)) {
            var6.setTrue();
         }
      });
      return var6.isTrue();
   }

   public Stream<ConfiguredFeature<?, ?>> getFeatures() {
      return this.feature.value().getFeatures();
   }

   @Override
   public String toString() {
      return "Placed " + this.feature;
   }
}
