package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
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

   // Cooperative-port optimisation (TeaVM wasm-gc): vanilla expands positions through a
   // per-modifier Stream.flatMap chain - one pipeline node, capturing lambda and
   // spliterator per placement modifier per candidate position - which is pure
   // allocation overhead in this runtime and a large fraction of the features stage
   // (~1489ms). Expand list-style instead. Scratch buffers are static because
   // PlacedFeature instances are registry singletons and worldgen is single-threaded
   // in this port; a depth counter localizes the buffers if placement ever re-enters.
   private static final List<BlockPos> SCAN_A = new ArrayList<>();
   private static final List<BlockPos> SCAN_B = new ArrayList<>();
   private static int scanDepth = 0;

   private boolean placeWithContext(PlacementContext param0, RandomSource param1, BlockPos param2) {
      List<BlockPos> var4;
      List<BlockPos> var5;
      if (scanDepth == 0) {
         var4 = SCAN_A;
         var5 = SCAN_B;
      } else {
         var4 = new ArrayList<>();
         var5 = new ArrayList<>();
      }
      ++scanDepth;

      try {
         var4.clear();
         var4.add(param2);

         for (int var7 = 0, var8 = this.placement.size(); var7 < var8; var7++) {
            PlacementModifier var9 = this.placement.get(var7);
            var5.clear();
            for (int var10 = 0, var11 = var4.size(); var10 < var11; var10++) {
               var9.appendPositions(param0, param1, var4.get(var10), var5);
            }
            List<BlockPos> var12 = var4;
            var4 = var5;
            var5 = var12;
         }

         ConfiguredFeature<?, ?> var13 = this.feature.value();
         boolean var14 = false;
         for (int var15 = 0, var16 = var4.size(); var15 < var16; var15++) {
            if (var13.place(param0.getLevel(), param0.generator(), param1, var4.get(var15))) {
               var14 = true;
            }
         }
         return var14;
      } finally {
         var4.clear();
         var5.clear();
         --scanDepth;
      }
   }

   public Stream<ConfiguredFeature<?, ?>> getFeatures() {
      return this.feature.value().getFeatures();
   }

   @Override
   public String toString() {
      return "Placed " + this.feature;
   }
}
