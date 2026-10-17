package net.minecraft.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.slf4j.Logger;

public class RegistryDataLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final List<RegistryDataLoader.RegistryData<?>> WORLDGEN_REGISTRIES = List.of(
      new RegistryDataLoader.RegistryData<>(Registries.DIMENSION_TYPE, DimensionType.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.BIOME, Biome.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.CHAT_TYPE, ChatType.CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.CONFIGURED_CARVER, ConfiguredWorldCarver.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.CONFIGURED_FEATURE, ConfiguredFeature.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.PLACED_FEATURE, PlacedFeature.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.STRUCTURE, Structure.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.STRUCTURE_SET, StructureSet.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.PROCESSOR_LIST, StructureProcessorType.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TEMPLATE_POOL, StructureTemplatePool.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.NOISE_SETTINGS, NoiseGeneratorSettings.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.NOISE, NormalNoise.NoiseParameters.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.DENSITY_FUNCTION, DensityFunction.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.WORLD_PRESET, WorldPreset.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.FLAT_LEVEL_GENERATOR_PRESET, FlatLevelGeneratorPreset.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TRIM_PATTERN, TrimPattern.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.TRIM_MATERIAL, TrimMaterial.DIRECT_CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.DAMAGE_TYPE, DamageType.CODEC),
      new RegistryDataLoader.RegistryData<>(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, MultiNoiseBiomeSourceParameterList.DIRECT_CODEC)
   );
   public static final List<RegistryDataLoader.RegistryData<?>> DIMENSION_REGISTRIES = List.of(
      new RegistryDataLoader.RegistryData<>(Registries.LEVEL_STEM, LevelStem.CODEC)
   );

   public static RegistryAccess.Frozen load(ResourceManager param0, RegistryAccess param1, List<RegistryDataLoader.RegistryData<?>> param2) {
      Map<ResourceKey<?>, Exception> var3 = new HashMap<>();
      List<Pair<WritableRegistry<?>, RegistryDataLoader.Loader>> var4 = param2.stream().map(l_param1 -> l_param1.create(Lifecycle.stable(), var3)).toList();
      RegistryOps.RegistryInfoLookup var5 = createContext(param1, var4);
      var4.forEach(l_param2 -> l_param2.getSecond().load(param0, var5));
      var4.forEach(l_param1 -> {
         Registry<?> l_var2 = l_param1.getFirst();

         try {
            l_var2.freeze();
         } catch (Exception l_var3) {
            var3.put(l_var2.key(), l_var3);
         }
      });
      if (!var3.isEmpty()) {
         logErrors(var3);
         throw new IllegalStateException("Failed to load registries due to above errors");
      } else {
         return new RegistryAccess.ImmutableRegistryAccess(var4.stream().map(Pair::getFirst).toList()).freeze();
      }
   }

   private static RegistryOps.RegistryInfoLookup createContext(RegistryAccess param0, List<Pair<WritableRegistry<?>, RegistryDataLoader.Loader>> param1) {
      final Map<ResourceKey<? extends Registry<?>>, RegistryOps.RegistryInfo<?>> var2 = new HashMap<>();
      param0.registries().forEach(l_param1 -> var2.put(l_param1.key(), createInfoForContextRegistry(l_param1.value())));
      param1.forEach(l_param1 -> var2.put(l_param1.getFirst().key(), createInfoForNewRegistry(l_param1.getFirst())));
      return new RegistryOps.RegistryInfoLookup() {
         @Override
         public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> l_param0) {
            return Optional.ofNullable((RegistryOps.RegistryInfo<T>)var2.get(l_param0));
         }
      };
   }

   private static <T> RegistryOps.RegistryInfo<T> createInfoForNewRegistry(WritableRegistry<T> param0) {
      return new RegistryOps.RegistryInfo<>(param0.asLookup(), param0.createRegistrationLookup(), param0.registryLifecycle());
   }

   private static <T> RegistryOps.RegistryInfo<T> createInfoForContextRegistry(Registry<T> param0) {
      return new RegistryOps.RegistryInfo<>(param0.asLookup(), param0.asTagAddingLookup(), param0.registryLifecycle());
   }

   private static void logErrors(Map<ResourceKey<?>, Exception> param0) {
      StringWriter var1 = new StringWriter();
      PrintWriter var2 = new PrintWriter(var1);
      Map<ResourceLocation, Map<ResourceLocation, Exception>> var3 = param0.entrySet()
         .stream()
         .collect(Collectors.groupingBy(l_param0 -> l_param0.getKey().registry(), Collectors.toMap(l_param0 -> l_param0.getKey().location(), Entry::getValue)));
      var3.entrySet().stream().sorted(Entry.comparingByKey()).forEach(l_param1 -> {
         var2.printf("> Errors in registry %s:%n", l_param1.getKey());
         l_param1.getValue().entrySet().stream().sorted(Entry.comparingByKey()).forEach(l2_param1 -> {
            var2.printf(">> Errors in element %s:%n", l2_param1.getKey());
            l2_param1.getValue().printStackTrace(var2);
         });
      });
      var2.flush();
      LOGGER.error("Registry loading errors:\n{}", var1);
   }

   private static String registryDirPath(ResourceLocation param0) {
      return param0.getPath();
   }

   static <E> void loadRegistryContents(
      RegistryOps.RegistryInfoLookup param0,
      ResourceManager param1,
      ResourceKey<? extends Registry<E>> param2,
      WritableRegistry<E> param3,
      Decoder<E> param4,
      Map<ResourceKey<?>, Exception> param5
   ) {
      String var6 = registryDirPath(param2.location());
      FileToIdConverter var7 = FileToIdConverter.json(var6);
      RegistryOps<JsonElement> var8 = RegistryOps.create(JsonOps.INSTANCE, param0);

      for (Entry<ResourceLocation, Resource> var10 : var7.listMatchingResources(param1).entrySet()) {
         ResourceLocation var11 = var10.getKey();
         ResourceKey<E> var12 = ResourceKey.create(param2, var7.fileToId(var11));
         Resource var13 = var10.getValue();

         try (Reader var14 = var13.openAsReader()) {
            JsonElement var15 = JsonParser.parseReader(var14);
            DataResult<E> var16 = param4.parse(var8, var15);
            E var17 = var16.getOrThrow(false, l_param0 -> {});
            param3.register(var12, var17, var13.isBuiltin() ? Lifecycle.stable() : var16.lifecycle());
         } catch (Exception var14) {
            param5.put(var12, new IllegalStateException(String.format(Locale.ROOT, "Failed to parse %s from pack %s", var11, var13.sourcePackId()), var14));
         }
      }
   }

   interface Loader {
      void load(ResourceManager var1, RegistryOps.RegistryInfoLookup var2);
   }

   public record RegistryData<T>(ResourceKey<? extends Registry<T>> key, Codec<T> elementCodec) {
      Pair<WritableRegistry<?>, RegistryDataLoader.Loader> create(Lifecycle param0, Map<ResourceKey<?>, Exception> param1) {
         WritableRegistry<T> var3 = new MappedRegistry<>(this.key, param0);
         RegistryDataLoader.Loader var4 = (l_param2, l_param3) -> RegistryDataLoader.loadRegistryContents(
            l_param3, l_param2, this.key, var3, this.elementCodec, param1
         );
         return Pair.of(var3, var4);
      }
   }
}
