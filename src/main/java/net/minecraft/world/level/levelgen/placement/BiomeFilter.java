package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public class BiomeFilter extends PlacementFilter {
   private static final BiomeFilter INSTANCE = new BiomeFilter();
   public static Codec<BiomeFilter> CODEC = Codec.unit(() -> INSTANCE);

   private BiomeFilter() {
   }

   public static BiomeFilter biome() {
      return INSTANCE;
   }

   @Override
   protected boolean shouldPlace(PlacementContext param0, RandomSource param1, BlockPos param2) {
      // Cooperative-port (TeaVM wasm-gc): hoisted the capturing-lambda orElseThrow
      // supplier (allocated per surviving candidate position in vanilla).
      Optional<PlacedFeature> var4 = param0.topFeature();
      if (var4.isEmpty()) {
         throw new IllegalStateException(
            "Tried to biome check an unregistered feature, or a feature that should not restrict the biome"
         );
      }
      PlacedFeature var5 = var4.get();
      Holder<Biome> var6 = param0.getLevel().getBiome(param2);
      return param0.generator().getBiomeGenerationSettings(var6).hasFeature(var5);
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.BIOME_FILTER;
   }
}
