package net.minecraft.world.level.levelgen.flat;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.LayerConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.slf4j.Logger;

public class FlatLevelGeneratorSettings {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<FlatLevelGeneratorSettings> CODEC = RecordCodecBuilder.<FlatLevelGeneratorSettings>create(
         param0 -> param0.group(
               RegistryCodecs.homogeneousList(Registries.STRUCTURE_SET)
                  .optionalFieldOf("structure_overrides")
                  .forGetter(l_param0 -> l_param0.structureOverrides),
               FlatLayerInfo.CODEC.listOf().fieldOf("layers").forGetter(FlatLevelGeneratorSettings::getLayersInfo),
               Codec.BOOL.fieldOf("lakes").orElse(false).forGetter(l_param0 -> l_param0.addLakes),
               Codec.BOOL.fieldOf("features").orElse(false).forGetter(l_param0 -> l_param0.decoration),
               Biome.CODEC.optionalFieldOf("biome").orElseGet(Optional::empty).forGetter(l_param0 -> Optional.of(l_param0.biome)),
               RegistryOps.retrieveElement(Biomes.PLAINS),
               RegistryOps.retrieveElement(MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND),
               RegistryOps.retrieveElement(MiscOverworldPlacements.LAKE_LAVA_SURFACE)
            )
            .apply(param0, FlatLevelGeneratorSettings::new)
      )
      .comapFlatMap(FlatLevelGeneratorSettings::validateHeight, Function.identity())
      .stable();
   private final Optional<HolderSet<StructureSet>> structureOverrides;
   private final List<FlatLayerInfo> layersInfo = Lists.newArrayList();
   private final Holder<Biome> biome;
   private final List<BlockState> layers;
   private boolean voidGen;
   private boolean decoration;
   private boolean addLakes;
   private final List<Holder<PlacedFeature>> lakes;

   private static DataResult<FlatLevelGeneratorSettings> validateHeight(FlatLevelGeneratorSettings param0) {
      int var1 = param0.layersInfo.stream().mapToInt(FlatLayerInfo::getHeight).sum();
      return var1 > DimensionType.Y_SIZE ? DataResult.error(() -> "Sum of layer heights is > " + DimensionType.Y_SIZE, param0) : DataResult.success(param0);
   }

   private FlatLevelGeneratorSettings(
      Optional<HolderSet<StructureSet>> param0,
      List<FlatLayerInfo> param1,
      boolean param2,
      boolean param3,
      Optional<Holder<Biome>> param4,
      Holder.Reference<Biome> param5,
      Holder<PlacedFeature> param6,
      Holder<PlacedFeature> param7
   ) {
      this(param0, getBiome(param4, param5), List.of(param6, param7));
      if (param2) {
         this.setAddLakes();
      }

      if (param3) {
         this.setDecoration();
      }

      this.layersInfo.addAll(param1);
      this.updateLayers();
   }

   private static Holder<Biome> getBiome(Optional<? extends Holder<Biome>> param0, Holder<Biome> param1) {
      if (param0.isEmpty()) {
         LOGGER.error("Unknown biome, defaulting to plains");
         return param1;
      } else {
         return (Holder<Biome>)param0.get();
      }
   }

   public FlatLevelGeneratorSettings(Optional<HolderSet<StructureSet>> param0, Holder<Biome> param1, List<Holder<PlacedFeature>> param2) {
      this.structureOverrides = param0;
      this.biome = param1;
      this.layers = Lists.newArrayList();
      this.lakes = param2;
   }

   public FlatLevelGeneratorSettings withBiomeAndLayers(List<FlatLayerInfo> param0, Optional<HolderSet<StructureSet>> param1, Holder<Biome> param2) {
      FlatLevelGeneratorSettings var4 = new FlatLevelGeneratorSettings(param1, param2, this.lakes);

      for (FlatLayerInfo var6 : param0) {
         var4.layersInfo.add(new FlatLayerInfo(var6.getHeight(), var6.getBlockState().getBlock()));
         var4.updateLayers();
      }

      if (this.decoration) {
         var4.setDecoration();
      }

      if (this.addLakes) {
         var4.setAddLakes();
      }

      return var4;
   }

   public void setDecoration() {
      this.decoration = true;
   }

   public void setAddLakes() {
      this.addLakes = true;
   }

   public BiomeGenerationSettings adjustGenerationSettings(Holder<Biome> param0) {
      if (!param0.equals(this.biome)) {
         return param0.value().getGenerationSettings();
      }

      BiomeGenerationSettings var2 = this.getBiome().value().getGenerationSettings();
      BiomeGenerationSettings.PlainBuilder var3 = new BiomeGenerationSettings.PlainBuilder();
      if (this.addLakes) {
         for (Holder<PlacedFeature> var5 : this.lakes) {
            var3.addFeature(GenerationStep.Decoration.LAKES, var5);
         }
      }

      boolean var4 = (!this.voidGen || param0.is(Biomes.THE_VOID)) && this.decoration;
      if (var4) {
         List<HolderSet<PlacedFeature>> var5 = var2.features();

         for (int var6 = 0; var6 < var5.size(); var6++) {
            if (var6 != GenerationStep.Decoration.UNDERGROUND_STRUCTURES.ordinal()
               && var6 != GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal()
               && (!this.addLakes || var6 != GenerationStep.Decoration.LAKES.ordinal())) {
               for (Holder<PlacedFeature> var9 : var5.get(var6)) {
                  var3.addFeature(var6, var9);
               }
            }
         }
      }

      List<BlockState> var5 = this.getLayers();

      for (int var6 = 0; var6 < var5.size(); var6++) {
         BlockState var7 = var5.get(var6);
         if (!Heightmap.Types.MOTION_BLOCKING.isOpaque().test(var7)) {
            var5.set(var6, null);
            var3.addFeature(
               GenerationStep.Decoration.TOP_LAYER_MODIFICATION, PlacementUtils.inlinePlaced(Feature.FILL_LAYER, new LayerConfiguration(var6, var7))
            );
         }
      }

      return var3.build();
   }

   public Optional<HolderSet<StructureSet>> structureOverrides() {
      return this.structureOverrides;
   }

   public Holder<Biome> getBiome() {
      return this.biome;
   }

   public List<FlatLayerInfo> getLayersInfo() {
      return this.layersInfo;
   }

   public List<BlockState> getLayers() {
      return this.layers;
   }

   public void updateLayers() {
      this.layers.clear();

      for (FlatLayerInfo var2 : this.layersInfo) {
         for (int var3 = 0; var3 < var2.getHeight(); var3++) {
            this.layers.add(var2.getBlockState());
         }
      }

      this.voidGen = this.layers.stream().allMatch(l_param0 -> l_param0.is(Blocks.AIR));
   }

   public static FlatLevelGeneratorSettings getDefault(HolderGetter<Biome> param0, HolderGetter<StructureSet> param1, HolderGetter<PlacedFeature> param2) {
      HolderSet<StructureSet> var3 = HolderSet.direct(param1.getOrThrow(BuiltinStructureSets.STRONGHOLDS), param1.getOrThrow(BuiltinStructureSets.VILLAGES));
      FlatLevelGeneratorSettings var4 = new FlatLevelGeneratorSettings(Optional.of(var3), getDefaultBiome(param0), createLakesList(param2));
      var4.getLayersInfo().add(new FlatLayerInfo(1, Blocks.BEDROCK));
      var4.getLayersInfo().add(new FlatLayerInfo(2, Blocks.DIRT));
      var4.getLayersInfo().add(new FlatLayerInfo(1, Blocks.GRASS_BLOCK));
      var4.updateLayers();
      return var4;
   }

   public static Holder<Biome> getDefaultBiome(HolderGetter<Biome> param0) {
      return param0.getOrThrow(Biomes.PLAINS);
   }

   public static List<Holder<PlacedFeature>> createLakesList(HolderGetter<PlacedFeature> param0) {
      return List.of(param0.getOrThrow(MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND), param0.getOrThrow(MiscOverworldPlacements.LAKE_LAVA_SURFACE));
   }
}
