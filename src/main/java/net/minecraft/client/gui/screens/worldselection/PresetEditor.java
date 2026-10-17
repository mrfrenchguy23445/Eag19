package net.minecraft.client.gui.screens.worldselection;

import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public interface PresetEditor {
   Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> EDITORS = Map.of(
      Optional.of(WorldPresets.FLAT),
      (param0, param1) -> {
         ChunkGenerator var2 = param1.selectedDimensions().overworld();
         RegistryAccess var3 = param1.worldgenLoadContext();
         HolderGetter<Biome> var4 = var3.lookupOrThrow(Registries.BIOME);
         HolderGetter<StructureSet> var5 = var3.lookupOrThrow(Registries.STRUCTURE_SET);
         HolderGetter<PlacedFeature> var6 = var3.lookupOrThrow(Registries.PLACED_FEATURE);
         return new CreateFlatWorldScreen(
            param0,
            l_param1 -> param0.getUiState().updateDimensions(flatWorldConfigurator(l_param1)),
            var2 instanceof FlatLevelSource ? ((FlatLevelSource)var2).settings() : FlatLevelGeneratorSettings.getDefault(var4, var5, var6)
         );
      },
      Optional.of(WorldPresets.SINGLE_BIOME_SURFACE),
      (param0, param1) -> new CreateBuffetWorldScreen(param0, param1, l_param1 -> param0.getUiState().updateDimensions(fixedBiomeConfigurator(l_param1)))
   );

   Screen createEditScreen(CreateWorldScreen var1, WorldCreationContext var2);

   private static WorldCreationContext.DimensionsUpdater flatWorldConfigurator(FlatLevelGeneratorSettings param0) {
      return (l_param1, l_param2) -> {
         ChunkGenerator l_var3 = new FlatLevelSource(param0);
         return l_param2.replaceOverworldGenerator(l_param1, l_var3);
      };
   }

   private static WorldCreationContext.DimensionsUpdater fixedBiomeConfigurator(Holder<Biome> param0) {
      return (l_param1, l_param2) -> {
         Registry<NoiseGeneratorSettings> l_var3 = l_param1.registryOrThrow(Registries.NOISE_SETTINGS);
         Holder<NoiseGeneratorSettings> l_var4 = l_var3.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
         BiomeSource l_var5 = new FixedBiomeSource(param0);
         ChunkGenerator l_var6 = new NoiseBasedChunkGenerator(l_var5, l_var4);
         return l_param2.replaceOverworldGenerator(l_param1, l_var6);
      };
   }
}
