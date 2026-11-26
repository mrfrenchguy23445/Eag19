package net.minecraft.data.worldgen.biome;

import javax.annotation.Nullable;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class OverworldBiomes {
   protected static final int NORMAL_WATER_COLOR = 4159204;
   protected static final int NORMAL_WATER_FOG_COLOR = 329011;
   private static final int OVERWORLD_FOG_COLOR = 12638463;
   @Nullable
   private static final Music NORMAL_MUSIC = null;

   protected static int calculateSkyColor(float param0) {
      float var1 = param0;
      var1 /= 3.0F;
      var1 = Mth.clamp(var1, -1.0F, 1.0F);
      return Mth.hsvToRgb(0.62222224F - var1 * 0.05F, 0.5F + var1 * 0.1F, 1.0F);
   }

   private static Biome biome(
      boolean param0, float param1, float param2, MobSpawnSettings.Builder param3, BiomeGenerationSettings.Builder param4, @Nullable Music param5
   ) {
      return biome(param0, param1, param2, 4159204, 329011, null, null, param3, param4, param5);
   }

   private static Biome biome(
      boolean param0,
      float param1,
      float param2,
      int param3,
      int param4,
      @Nullable Integer param5,
      @Nullable Integer param6,
      MobSpawnSettings.Builder param7,
      BiomeGenerationSettings.Builder param8,
      @Nullable Music param9
   ) {
      BiomeSpecialEffects.Builder var10 = new BiomeSpecialEffects.Builder()
         .waterColor(param3)
         .waterFogColor(param4)
         .fogColor(12638463)
         .skyColor(calculateSkyColor(param1))
         .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
         .backgroundMusic(param9);
      if (param5 != null) {
         var10.grassColorOverride(param5);
      }

      if (param6 != null) {
         var10.foliageColorOverride(param6);
      }

      return new Biome.BiomeBuilder()
         .hasPrecipitation(param0)
         .temperature(param1)
         .downfall(param2)
         .specialEffects(var10.build())
         .mobSpawnSettings(param7.build())
         .generationSettings(param8.build())
         .build();
   }

   private static void globalOverworldGeneration(BiomeGenerationSettings.Builder param0) {
      BiomeDefaultFeatures.addDefaultCarversAndLakes(param0);
      BiomeDefaultFeatures.addDefaultCrystalFormations(param0);
      BiomeDefaultFeatures.addDefaultMonsterRoom(param0);
      BiomeDefaultFeatures.addDefaultUndergroundVariety(param0);
      BiomeDefaultFeatures.addDefaultSprings(param0);
      BiomeDefaultFeatures.addSurfaceFreezing(param0);
   }

   public static Biome oldGrowthTaiga(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(var3);
      var3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4));
      var3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
      var3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
      if (param2) {
         BiomeDefaultFeatures.commonSpawns(var3);
      } else {
         BiomeDefaultFeatures.caveSpawns(var3);
         BiomeDefaultFeatures.monsters(var3, 100, 25, 100, false);
      }

      BiomeGenerationSettings.Builder var4 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var4);
      BiomeDefaultFeatures.addMossyStoneBlock(var4);
      BiomeDefaultFeatures.addFerns(var4);
      BiomeDefaultFeatures.addDefaultOres(var4);
      BiomeDefaultFeatures.addDefaultSoftDisks(var4);
      var4.addFeature(
         GenerationStep.Decoration.VEGETAL_DECORATION,
         param2 ? VegetationPlacements.TREES_OLD_GROWTH_SPRUCE_TAIGA : VegetationPlacements.TREES_OLD_GROWTH_PINE_TAIGA
      );
      BiomeDefaultFeatures.addDefaultFlowers(var4);
      BiomeDefaultFeatures.addGiantTaigaVegetation(var4);
      BiomeDefaultFeatures.addDefaultMushrooms(var4);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var4);
      BiomeDefaultFeatures.addCommonBerryBushes(var4);
      Music var5 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_OLD_GROWTH_TAIGA);
      return biome(true, param2 ? 0.25F : 0.3F, 0.8F, var3, var4, var5);
   }

   public static Biome sparseJungle(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.baseJungleSpawns(var2);
      return baseJungle(param0, param1, 0.8F, false, true, false, var2);
   }

   public static Biome jungle(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.baseJungleSpawns(var2);
      var2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PARROT, 40, 1, 2))
         .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.OCELOT, 2, 1, 3))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PANDA, 1, 1, 2));
      return baseJungle(param0, param1, 0.9F, false, false, true, var2);
   }

   public static Biome bambooJungle(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.baseJungleSpawns(var2);
      var2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PARROT, 40, 1, 2))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PANDA, 80, 1, 2))
         .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.OCELOT, 2, 1, 1));
      return baseJungle(param0, param1, 0.9F, true, false, true, var2);
   }

   private static Biome baseJungle(
      HolderGetter<PlacedFeature> param0,
      HolderGetter<ConfiguredWorldCarver<?>> param1,
      float param2,
      boolean param3,
      boolean param4,
      boolean param5,
      MobSpawnSettings.Builder param6
   ) {
      BiomeGenerationSettings.Builder var7 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var7);
      BiomeDefaultFeatures.addDefaultOres(var7);
      BiomeDefaultFeatures.addDefaultSoftDisks(var7);
      if (param3) {
         BiomeDefaultFeatures.addBambooVegetation(var7);
      } else {
         if (param5) {
            BiomeDefaultFeatures.addLightBambooVegetation(var7);
         }

         if (param4) {
            BiomeDefaultFeatures.addSparseJungleTrees(var7);
         } else {
            BiomeDefaultFeatures.addJungleTrees(var7);
         }
      }

      BiomeDefaultFeatures.addWarmFlowers(var7);
      BiomeDefaultFeatures.addJungleGrass(var7);
      BiomeDefaultFeatures.addDefaultMushrooms(var7);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var7);
      BiomeDefaultFeatures.addJungleVines(var7);
      if (param4) {
         BiomeDefaultFeatures.addSparseJungleMelons(var7);
      } else {
         BiomeDefaultFeatures.addJungleMelons(var7);
      }

      Music var8 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_JUNGLE_AND_FOREST);
      return biome(true, 0.95F, param2, param6, var7, var8);
   }

   public static Biome windsweptHills(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(var3);
      var3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 5, 4, 6));
      BiomeDefaultFeatures.commonSpawns(var3);
      BiomeGenerationSettings.Builder var4 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var4);
      BiomeDefaultFeatures.addDefaultOres(var4);
      BiomeDefaultFeatures.addDefaultSoftDisks(var4);
      if (param2) {
         BiomeDefaultFeatures.addMountainForestTrees(var4);
      } else {
         BiomeDefaultFeatures.addMountainTrees(var4);
      }

      BiomeDefaultFeatures.addDefaultFlowers(var4);
      BiomeDefaultFeatures.addDefaultGrass(var4);
      BiomeDefaultFeatures.addDefaultMushrooms(var4);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var4);
      BiomeDefaultFeatures.addExtraEmeralds(var4);
      BiomeDefaultFeatures.addInfestedStone(var4);
      return biome(true, 0.2F, 0.3F, var3, var4, NORMAL_MUSIC);
   }

   public static Biome desert(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.desertSpawns(var2);
      BiomeGenerationSettings.Builder var3 = new BiomeGenerationSettings.Builder(param0, param1);
      BiomeDefaultFeatures.addFossilDecoration(var3);
      globalOverworldGeneration(var3);
      BiomeDefaultFeatures.addDefaultOres(var3);
      BiomeDefaultFeatures.addDefaultSoftDisks(var3);
      BiomeDefaultFeatures.addDefaultFlowers(var3);
      BiomeDefaultFeatures.addDefaultGrass(var3);
      BiomeDefaultFeatures.addDesertVegetation(var3);
      BiomeDefaultFeatures.addDefaultMushrooms(var3);
      BiomeDefaultFeatures.addDesertExtraVegetation(var3);
      BiomeDefaultFeatures.addDesertExtraDecoration(var3);
      return biome(false, 2.0F, 0.0F, var2, var3, NORMAL_MUSIC);
   }

   public static Biome plains(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2, boolean param3, boolean param4) {
      MobSpawnSettings.Builder var5 = new MobSpawnSettings.Builder();
      BiomeGenerationSettings.Builder var6 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var6);
      if (param3) {
         var5.creatureGenerationProbability(0.07F);
         BiomeDefaultFeatures.snowySpawns(var5);
         if (param4) {
            var6.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_SPIKE);
            var6.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_PATCH);
         }
      } else {
         BiomeDefaultFeatures.plainsSpawns(var5);
         BiomeDefaultFeatures.addPlainGrass(var6);
         if (param2) {
            var6.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUNFLOWER);
         }
      }

      BiomeDefaultFeatures.addDefaultOres(var6);
      BiomeDefaultFeatures.addDefaultSoftDisks(var6);
      if (param3) {
         BiomeDefaultFeatures.addSnowyTrees(var6);
         BiomeDefaultFeatures.addDefaultFlowers(var6);
         BiomeDefaultFeatures.addDefaultGrass(var6);
      } else {
         BiomeDefaultFeatures.addPlainVegetation(var6);
      }

      BiomeDefaultFeatures.addDefaultMushrooms(var6);
      if (param2) {
         var6.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUGAR_CANE);
         var6.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN);
      } else {
         BiomeDefaultFeatures.addDefaultExtraVegetation(var6);
      }

      float var7 = param3 ? 0.0F : 0.8F;
      return biome(true, var7, param3 ? 0.5F : 0.4F, var5, var6, NORMAL_MUSIC);
   }

   public static Biome mushroomFields(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.mooshroomSpawns(var2);
      BiomeGenerationSettings.Builder var3 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var3);
      BiomeDefaultFeatures.addDefaultOres(var3);
      BiomeDefaultFeatures.addDefaultSoftDisks(var3);
      BiomeDefaultFeatures.addMushroomFieldVegetation(var3);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var3);
      return biome(true, 0.9F, 1.0F, var2, var3, NORMAL_MUSIC);
   }

   public static Biome savanna(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2, boolean param3) {
      BiomeGenerationSettings.Builder var4 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var4);
      if (!param2) {
         BiomeDefaultFeatures.addSavannaGrass(var4);
      }

      BiomeDefaultFeatures.addDefaultOres(var4);
      BiomeDefaultFeatures.addDefaultSoftDisks(var4);
      if (param2) {
         BiomeDefaultFeatures.addShatteredSavannaTrees(var4);
         BiomeDefaultFeatures.addDefaultFlowers(var4);
         BiomeDefaultFeatures.addShatteredSavannaGrass(var4);
      } else {
         BiomeDefaultFeatures.addSavannaTrees(var4);
         BiomeDefaultFeatures.addWarmFlowers(var4);
         BiomeDefaultFeatures.addSavannaExtraGrass(var4);
      }

      BiomeDefaultFeatures.addDefaultMushrooms(var4);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var4);
      MobSpawnSettings.Builder var5 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(var5);
      var5.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.HORSE, 1, 2, 6))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DONKEY, 1, 1, 1));
      BiomeDefaultFeatures.commonSpawns(var5);
      if (param3) {
         var5.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 8, 4, 4));
      }

      return biome(false, 2.0F, 0.0F, var5, var4, NORMAL_MUSIC);
   }

   public static Biome badlands(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.commonSpawns(var3);
      BiomeGenerationSettings.Builder var4 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var4);
      BiomeDefaultFeatures.addDefaultOres(var4);
      BiomeDefaultFeatures.addExtraGold(var4);
      BiomeDefaultFeatures.addDefaultSoftDisks(var4);
      if (param2) {
         BiomeDefaultFeatures.addBadlandsTrees(var4);
      }

      BiomeDefaultFeatures.addBadlandGrass(var4);
      BiomeDefaultFeatures.addDefaultMushrooms(var4);
      BiomeDefaultFeatures.addBadlandExtraVegetation(var4);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(false)
         .temperature(2.0F)
         .downfall(0.0F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(4159204)
               .waterFogColor(329011)
               .fogColor(12638463)
               .skyColor(calculateSkyColor(2.0F))
               .foliageColorOverride(10387789)
               .grassColorOverride(9470285)
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .build()
         )
         .mobSpawnSettings(var3.build())
         .generationSettings(var4.build())
         .build();
   }

   private static Biome baseOcean(MobSpawnSettings.Builder param0, int param1, int param2, BiomeGenerationSettings.Builder param3) {
      return biome(true, 0.5F, 0.5F, param1, param2, null, null, param0, param3, NORMAL_MUSIC);
   }

   private static BiomeGenerationSettings.Builder baseOceanGeneration(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      BiomeGenerationSettings.Builder var2 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var2);
      BiomeDefaultFeatures.addDefaultOres(var2);
      BiomeDefaultFeatures.addDefaultSoftDisks(var2);
      BiomeDefaultFeatures.addWaterTrees(var2);
      BiomeDefaultFeatures.addDefaultFlowers(var2);
      BiomeDefaultFeatures.addDefaultGrass(var2);
      BiomeDefaultFeatures.addDefaultMushrooms(var2);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var2);
      return var2;
   }

   public static Biome coldOcean(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.oceanSpawns(var3, 3, 4, 15);
      var3.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 15, 1, 5));
      BiomeGenerationSettings.Builder var4 = baseOceanGeneration(param0, param1);
      var4.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, param2 ? AquaticPlacements.SEAGRASS_DEEP_COLD : AquaticPlacements.SEAGRASS_COLD);
      BiomeDefaultFeatures.addDefaultSeagrass(var4);
      BiomeDefaultFeatures.addColdOceanExtraVegetation(var4);
      return baseOcean(var3, 4020182, 329011, var4);
   }

   public static Biome ocean(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.oceanSpawns(var3, 1, 4, 10);
      var3.addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DOLPHIN, 1, 1, 2));
      BiomeGenerationSettings.Builder var4 = baseOceanGeneration(param0, param1);
      var4.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, param2 ? AquaticPlacements.SEAGRASS_DEEP : AquaticPlacements.SEAGRASS_NORMAL);
      BiomeDefaultFeatures.addDefaultSeagrass(var4);
      BiomeDefaultFeatures.addColdOceanExtraVegetation(var4);
      return baseOcean(var3, 4159204, 329011, var4);
   }

   public static Biome lukeWarmOcean(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      if (param2) {
         BiomeDefaultFeatures.oceanSpawns(var3, 8, 4, 8);
      } else {
         BiomeDefaultFeatures.oceanSpawns(var3, 10, 2, 15);
      }

      var3.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 5, 1, 3))
         .addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 25, 8, 8))
         .addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DOLPHIN, 2, 1, 2));
      BiomeGenerationSettings.Builder var4 = baseOceanGeneration(param0, param1);
      var4.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, param2 ? AquaticPlacements.SEAGRASS_DEEP_WARM : AquaticPlacements.SEAGRASS_WARM);
      if (param2) {
         BiomeDefaultFeatures.addDefaultSeagrass(var4);
      }

      BiomeDefaultFeatures.addLukeWarmKelp(var4);
      return baseOcean(var3, 4566514, 267827, var4);
   }

   public static Biome warmOcean(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder()
         .addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 15, 1, 3));
      BiomeDefaultFeatures.warmOceanSpawns(var2, 10, 4);
      BiomeGenerationSettings.Builder var3 = baseOceanGeneration(param0, param1)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.WARM_OCEAN_VEGETATION)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_WARM)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEA_PICKLE);
      return baseOcean(var2, 4445678, 270131, var3);
   }

   public static Biome frozenOcean(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder()
         .addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 1, 1, 4))
         .addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 15, 1, 5))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.POLAR_BEAR, 1, 1, 2));
      BiomeDefaultFeatures.commonSpawns(var3);
      var3.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 5, 1, 1));
      float var4 = param2 ? 0.5F : 0.0F;
      BiomeGenerationSettings.Builder var5 = new BiomeGenerationSettings.Builder(param0, param1);
      BiomeDefaultFeatures.addIcebergs(var5);
      globalOverworldGeneration(var5);
      BiomeDefaultFeatures.addBlueIce(var5);
      BiomeDefaultFeatures.addDefaultOres(var5);
      BiomeDefaultFeatures.addDefaultSoftDisks(var5);
      BiomeDefaultFeatures.addWaterTrees(var5);
      BiomeDefaultFeatures.addDefaultFlowers(var5);
      BiomeDefaultFeatures.addDefaultGrass(var5);
      BiomeDefaultFeatures.addDefaultMushrooms(var5);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var5);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(true)
         .temperature(var4)
         .temperatureAdjustment(Biome.TemperatureModifier.FROZEN)
         .downfall(0.5F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(3750089)
               .waterFogColor(329011)
               .fogColor(12638463)
               .skyColor(calculateSkyColor(var4))
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .build()
         )
         .mobSpawnSettings(var3.build())
         .generationSettings(var5.build())
         .build();
   }

   public static Biome forest(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2, boolean param3, boolean param4) {
      BiomeGenerationSettings.Builder var5 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var5);
      if (param4) {
         var5.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FOREST_FLOWERS);
      } else {
         BiomeDefaultFeatures.addForestFlowers(var5);
      }

      BiomeDefaultFeatures.addDefaultOres(var5);
      BiomeDefaultFeatures.addDefaultSoftDisks(var5);
      if (param4) {
         var5.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_FLOWER_FOREST);
         var5.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FLOWER_FOREST);
         BiomeDefaultFeatures.addDefaultGrass(var5);
      } else {
         if (param2) {
            if (param3) {
               BiomeDefaultFeatures.addTallBirchTrees(var5);
            } else {
               BiomeDefaultFeatures.addBirchTrees(var5);
            }
         } else {
            BiomeDefaultFeatures.addOtherBirchTrees(var5);
         }

         BiomeDefaultFeatures.addDefaultFlowers(var5);
         BiomeDefaultFeatures.addForestGrass(var5);
      }

      BiomeDefaultFeatures.addDefaultMushrooms(var5);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var5);
      MobSpawnSettings.Builder var6 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(var6);
      BiomeDefaultFeatures.commonSpawns(var6);
      if (param4) {
         var6.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
      } else if (!param2) {
         var6.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 5, 4, 4));
      }

      float var7 = param2 ? 0.6F : 0.7F;
      Music var8 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_JUNGLE_AND_FOREST);
      return biome(true, var7, param2 ? 0.6F : 0.8F, var6, var5, var8);
   }

   public static Biome taiga(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(var3);
      var3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
      BiomeDefaultFeatures.commonSpawns(var3);
      float var4 = param2 ? -0.5F : 0.25F;
      BiomeGenerationSettings.Builder var5 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var5);
      BiomeDefaultFeatures.addFerns(var5);
      BiomeDefaultFeatures.addDefaultOres(var5);
      BiomeDefaultFeatures.addDefaultSoftDisks(var5);
      BiomeDefaultFeatures.addTaigaTrees(var5);
      BiomeDefaultFeatures.addDefaultFlowers(var5);
      BiomeDefaultFeatures.addTaigaGrass(var5);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var5);
      if (param2) {
         BiomeDefaultFeatures.addRareBerryBushes(var5);
      } else {
         BiomeDefaultFeatures.addCommonBerryBushes(var5);
      }

      return biome(true, var4, param2 ? 0.4F : 0.8F, param2 ? 4020182 : 4159204, 329011, null, null, var3, var5, NORMAL_MUSIC);
   }

   public static Biome darkForest(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(var2);
      BiomeDefaultFeatures.commonSpawns(var2);
      BiomeGenerationSettings.Builder var3 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var3);
      var3.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.DARK_FOREST_VEGETATION);
      BiomeDefaultFeatures.addForestFlowers(var3);
      BiomeDefaultFeatures.addDefaultOres(var3);
      BiomeDefaultFeatures.addDefaultSoftDisks(var3);
      BiomeDefaultFeatures.addDefaultFlowers(var3);
      BiomeDefaultFeatures.addForestGrass(var3);
      BiomeDefaultFeatures.addDefaultMushrooms(var3);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var3);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_JUNGLE_AND_FOREST);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(true)
         .temperature(0.7F)
         .downfall(0.8F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(4159204)
               .waterFogColor(329011)
               .fogColor(12638463)
               .skyColor(calculateSkyColor(0.7F))
               .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.DARK_FOREST)
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .backgroundMusic(var4)
               .build()
         )
         .mobSpawnSettings(var2.build())
         .generationSettings(var3.build())
         .build();
   }

   public static Biome swamp(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(var2);
      BiomeDefaultFeatures.commonSpawns(var2);
      var2.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SLIME, 1, 1, 1));
      var2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FROG, 10, 2, 5));
      BiomeGenerationSettings.Builder var3 = new BiomeGenerationSettings.Builder(param0, param1);
      BiomeDefaultFeatures.addFossilDecoration(var3);
      globalOverworldGeneration(var3);
      BiomeDefaultFeatures.addDefaultOres(var3);
      BiomeDefaultFeatures.addSwampClayDisk(var3);
      BiomeDefaultFeatures.addSwampVegetation(var3);
      BiomeDefaultFeatures.addDefaultMushrooms(var3);
      BiomeDefaultFeatures.addSwampExtraVegetation(var3);
      var3.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SWAMP);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SWAMP);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(true)
         .temperature(0.8F)
         .downfall(0.9F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(6388580)
               .waterFogColor(2302743)
               .fogColor(12638463)
               .skyColor(calculateSkyColor(0.8F))
               .foliageColorOverride(6975545)
               .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP)
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .backgroundMusic(var4)
               .build()
         )
         .mobSpawnSettings(var2.build())
         .generationSettings(var3.build())
         .build();
   }

   public static Biome mangroveSwamp(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.commonSpawns(var2);
      var2.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SLIME, 1, 1, 1));
      var2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FROG, 10, 2, 5));
      var2.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 25, 8, 8));
      BiomeGenerationSettings.Builder var3 = new BiomeGenerationSettings.Builder(param0, param1);
      BiomeDefaultFeatures.addFossilDecoration(var3);
      globalOverworldGeneration(var3);
      BiomeDefaultFeatures.addDefaultOres(var3);
      BiomeDefaultFeatures.addMangroveSwampDisks(var3);
      BiomeDefaultFeatures.addMangroveSwampVegetation(var3);
      var3.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SWAMP);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SWAMP);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(true)
         .temperature(0.8F)
         .downfall(0.9F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(3832426)
               .waterFogColor(5077600)
               .fogColor(12638463)
               .skyColor(calculateSkyColor(0.8F))
               .foliageColorOverride(9285927)
               .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP)
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .backgroundMusic(var4)
               .build()
         )
         .mobSpawnSettings(var2.build())
         .generationSettings(var3.build())
         .build();
   }

   public static Biome river(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder()
         .addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 2, 1, 4))
         .addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 5, 1, 5));
      BiomeDefaultFeatures.commonSpawns(var3);
      var3.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.DROWNED, param2 ? 1 : 100, 1, 1));
      BiomeGenerationSettings.Builder var4 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var4);
      BiomeDefaultFeatures.addDefaultOres(var4);
      BiomeDefaultFeatures.addDefaultSoftDisks(var4);
      BiomeDefaultFeatures.addWaterTrees(var4);
      BiomeDefaultFeatures.addDefaultFlowers(var4);
      BiomeDefaultFeatures.addDefaultGrass(var4);
      BiomeDefaultFeatures.addDefaultMushrooms(var4);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var4);
      if (!param2) {
         var4.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_RIVER);
      }

      float var5 = param2 ? 0.0F : 0.5F;
      return biome(true, var5, 0.5F, param2 ? 3750089 : 4159204, 329011, null, null, var3, var4, NORMAL_MUSIC);
   }

   public static Biome beach(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2, boolean param3) {
      MobSpawnSettings.Builder var4 = new MobSpawnSettings.Builder();
      boolean var5 = !param3 && !param2;
      if (var5) {
         var4.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.TURTLE, 5, 2, 5));
      }

      BiomeDefaultFeatures.commonSpawns(var4);
      BiomeGenerationSettings.Builder var6 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var6);
      BiomeDefaultFeatures.addDefaultOres(var6);
      BiomeDefaultFeatures.addDefaultSoftDisks(var6);
      BiomeDefaultFeatures.addDefaultFlowers(var6);
      BiomeDefaultFeatures.addDefaultGrass(var6);
      BiomeDefaultFeatures.addDefaultMushrooms(var6);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var6);
      float var7;
      if (param2) {
         var7 = 0.05F;
      } else if (param3) {
         var7 = 0.2F;
      } else {
         var7 = 0.8F;
      }

      return biome(true, var7, var5 ? 0.4F : 0.3F, param2 ? 4020182 : 4159204, 329011, null, null, var4, var6, NORMAL_MUSIC);
   }

   public static Biome theVoid(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      BiomeGenerationSettings.Builder var2 = new BiomeGenerationSettings.Builder(param0, param1);
      var2.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, MiscOverworldPlacements.VOID_START_PLATFORM);
      return biome(false, 0.5F, 0.5F, new MobSpawnSettings.Builder(), var2, NORMAL_MUSIC);
   }

   public static Biome meadowOrCherryGrove(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1, boolean param2) {
      BiomeGenerationSettings.Builder var3 = new BiomeGenerationSettings.Builder(param0, param1);
      MobSpawnSettings.Builder var4 = new MobSpawnSettings.Builder();
      var4.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(param2 ? EntityType.PIG : EntityType.DONKEY, 1, 1, 2))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 2, 6))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 2, 2, 4));
      BiomeDefaultFeatures.commonSpawns(var4);
      globalOverworldGeneration(var3);
      BiomeDefaultFeatures.addPlainGrass(var3);
      BiomeDefaultFeatures.addDefaultOres(var3);
      BiomeDefaultFeatures.addDefaultSoftDisks(var3);
      if (param2) {
         BiomeDefaultFeatures.addCherryGroveVegetation(var3);
      } else {
         BiomeDefaultFeatures.addMeadowVegetation(var3);
      }

      BiomeDefaultFeatures.addExtraEmeralds(var3);
      BiomeDefaultFeatures.addInfestedStone(var3);
      Music var5 = Musics.createGameMusic(param2 ? SoundEvents.MUSIC_BIOME_CHERRY_GROVE : SoundEvents.MUSIC_BIOME_MEADOW);
      return param2
         ? biome(true, 0.5F, 0.8F, 6141935, 6141935, 11983713, 11983713, var4, var3, var5)
         : biome(true, 0.5F, 0.8F, 937679, 329011, null, null, var4, var3, var5);
   }

   public static Biome frozenPeaks(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      BiomeGenerationSettings.Builder var2 = new BiomeGenerationSettings.Builder(param0, param1);
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      var3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3));
      BiomeDefaultFeatures.commonSpawns(var3);
      globalOverworldGeneration(var2);
      BiomeDefaultFeatures.addFrozenSprings(var2);
      BiomeDefaultFeatures.addDefaultOres(var2);
      BiomeDefaultFeatures.addDefaultSoftDisks(var2);
      BiomeDefaultFeatures.addExtraEmeralds(var2);
      BiomeDefaultFeatures.addInfestedStone(var2);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FROZEN_PEAKS);
      return biome(true, -0.7F, 0.9F, var3, var2, var4);
   }

   public static Biome jaggedPeaks(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      BiomeGenerationSettings.Builder var2 = new BiomeGenerationSettings.Builder(param0, param1);
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      var3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3));
      BiomeDefaultFeatures.commonSpawns(var3);
      globalOverworldGeneration(var2);
      BiomeDefaultFeatures.addFrozenSprings(var2);
      BiomeDefaultFeatures.addDefaultOres(var2);
      BiomeDefaultFeatures.addDefaultSoftDisks(var2);
      BiomeDefaultFeatures.addExtraEmeralds(var2);
      BiomeDefaultFeatures.addInfestedStone(var2);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_JAGGED_PEAKS);
      return biome(true, -0.7F, 0.9F, var3, var2, var4);
   }

   public static Biome stonyPeaks(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      BiomeGenerationSettings.Builder var2 = new BiomeGenerationSettings.Builder(param0, param1);
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.commonSpawns(var3);
      globalOverworldGeneration(var2);
      BiomeDefaultFeatures.addDefaultOres(var2);
      BiomeDefaultFeatures.addDefaultSoftDisks(var2);
      BiomeDefaultFeatures.addExtraEmeralds(var2);
      BiomeDefaultFeatures.addInfestedStone(var2);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_STONY_PEAKS);
      return biome(true, 1.0F, 0.3F, var3, var2, var4);
   }

   public static Biome snowySlopes(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      BiomeGenerationSettings.Builder var2 = new BiomeGenerationSettings.Builder(param0, param1);
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      var3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3));
      BiomeDefaultFeatures.commonSpawns(var3);
      globalOverworldGeneration(var2);
      BiomeDefaultFeatures.addFrozenSprings(var2);
      BiomeDefaultFeatures.addDefaultOres(var2);
      BiomeDefaultFeatures.addDefaultSoftDisks(var2);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var2);
      BiomeDefaultFeatures.addExtraEmeralds(var2);
      BiomeDefaultFeatures.addInfestedStone(var2);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SNOWY_SLOPES);
      return biome(true, -0.3F, 0.9F, var3, var2, var4);
   }

   public static Biome grove(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      BiomeGenerationSettings.Builder var2 = new BiomeGenerationSettings.Builder(param0, param1);
      MobSpawnSettings.Builder var3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals(var3);
      var3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
      BiomeDefaultFeatures.commonSpawns(var3);
      globalOverworldGeneration(var2);
      BiomeDefaultFeatures.addFrozenSprings(var2);
      BiomeDefaultFeatures.addDefaultOres(var2);
      BiomeDefaultFeatures.addDefaultSoftDisks(var2);
      BiomeDefaultFeatures.addGroveTrees(var2);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var2);
      BiomeDefaultFeatures.addExtraEmeralds(var2);
      BiomeDefaultFeatures.addInfestedStone(var2);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_GROVE);
      return biome(true, -0.2F, 0.8F, var3, var2, var4);
   }

   public static Biome lushCaves(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      var2.addSpawn(MobCategory.AXOLOTLS, new MobSpawnSettings.SpawnerData(EntityType.AXOLOTL, 10, 4, 6));
      var2.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 25, 8, 8));
      BiomeDefaultFeatures.commonSpawns(var2);
      BiomeGenerationSettings.Builder var3 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var3);
      BiomeDefaultFeatures.addPlainGrass(var3);
      BiomeDefaultFeatures.addDefaultOres(var3);
      BiomeDefaultFeatures.addLushCavesSpecialOres(var3);
      BiomeDefaultFeatures.addDefaultSoftDisks(var3);
      BiomeDefaultFeatures.addLushCavesVegetationFeatures(var3);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_LUSH_CAVES);
      return biome(true, 0.5F, 0.5F, var2, var3, var4);
   }

   public static Biome dripstoneCaves(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.dripstoneCavesSpawns(var2);
      BiomeGenerationSettings.Builder var3 = new BiomeGenerationSettings.Builder(param0, param1);
      globalOverworldGeneration(var3);
      BiomeDefaultFeatures.addPlainGrass(var3);
      BiomeDefaultFeatures.addDefaultOres(var3, true);
      BiomeDefaultFeatures.addDefaultSoftDisks(var3);
      BiomeDefaultFeatures.addPlainVegetation(var3);
      BiomeDefaultFeatures.addDefaultMushrooms(var3);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var3);
      BiomeDefaultFeatures.addDripstone(var3);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_DRIPSTONE_CAVES);
      return biome(true, 0.8F, 0.4F, var2, var3, var4);
   }

   public static Biome deepDark(HolderGetter<PlacedFeature> param0, HolderGetter<ConfiguredWorldCarver<?>> param1) {
      MobSpawnSettings.Builder var2 = new MobSpawnSettings.Builder();
      BiomeGenerationSettings.Builder var3 = new BiomeGenerationSettings.Builder(param0, param1);
      var3.addCarver(GenerationStep.Carving.AIR, Carvers.CAVE);
      var3.addCarver(GenerationStep.Carving.AIR, Carvers.CAVE_EXTRA_UNDERGROUND);
      var3.addCarver(GenerationStep.Carving.AIR, Carvers.CANYON);
      BiomeDefaultFeatures.addDefaultCrystalFormations(var3);
      BiomeDefaultFeatures.addDefaultMonsterRoom(var3);
      BiomeDefaultFeatures.addDefaultUndergroundVariety(var3);
      BiomeDefaultFeatures.addSurfaceFreezing(var3);
      BiomeDefaultFeatures.addPlainGrass(var3);
      BiomeDefaultFeatures.addDefaultOres(var3);
      BiomeDefaultFeatures.addDefaultSoftDisks(var3);
      BiomeDefaultFeatures.addPlainVegetation(var3);
      BiomeDefaultFeatures.addDefaultMushrooms(var3);
      BiomeDefaultFeatures.addDefaultExtraVegetation(var3);
      BiomeDefaultFeatures.addSculk(var3);
      Music var4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_DEEP_DARK);
      return biome(true, 0.8F, 0.4F, var2, var3, var4);
   }
}
