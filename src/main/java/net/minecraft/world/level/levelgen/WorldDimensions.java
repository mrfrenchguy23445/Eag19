package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.PrimaryLevelData;

public record WorldDimensions(Registry<LevelStem> dimensions) {
   public static final MapCodec<WorldDimensions> CODEC = RecordCodecBuilder.mapCodec(
      param0 -> param0.group(
            RegistryCodecs.fullCodec(Registries.LEVEL_STEM, Lifecycle.stable(), LevelStem.CODEC).fieldOf("dimensions").forGetter(WorldDimensions::dimensions)
         )
         .apply(param0, param0.stable(WorldDimensions::new))
   );
   private static final Set<ResourceKey<LevelStem>> BUILTIN_ORDER = ImmutableSet.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END);
   private static final int VANILLA_DIMENSION_COUNT = BUILTIN_ORDER.size();

   public WorldDimensions {
      LevelStem var2 = dimensions.get(LevelStem.OVERWORLD);
      if (var2 == null) {
         throw new IllegalStateException("Overworld settings missing");
      }
   }

   public static Stream<ResourceKey<LevelStem>> keysInOrder(Stream<ResourceKey<LevelStem>> param0) {
      return Stream.concat(BUILTIN_ORDER.stream(), param0.filter(l_param0 -> !BUILTIN_ORDER.contains(l_param0)));
   }

   public WorldDimensions replaceOverworldGenerator(RegistryAccess param0, ChunkGenerator param1) {
      Registry<DimensionType> var3 = param0.registryOrThrow(Registries.DIMENSION_TYPE);
      Registry<LevelStem> var4 = withOverworld(var3, this.dimensions, param1);
      return new WorldDimensions(var4);
   }

   public static Registry<LevelStem> withOverworld(Registry<DimensionType> param0, Registry<LevelStem> param1, ChunkGenerator param2) {
      LevelStem var3 = param1.get(LevelStem.OVERWORLD);
      Holder<DimensionType> var4 = var3 == null ? param0.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD) : var3.type();
      return withOverworld(param1, var4, param2);
   }

   public static Registry<LevelStem> withOverworld(Registry<LevelStem> param0, Holder<DimensionType> param1, ChunkGenerator param2) {
      WritableRegistry<LevelStem> var3 = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.experimental());
      var3.register(LevelStem.OVERWORLD, new LevelStem(param1, param2), Lifecycle.stable());

      for (java.util.Map.Entry<ResourceKey<LevelStem>, LevelStem> var5 : param0.entrySet()) {
         ResourceKey<LevelStem> var6 = var5.getKey();
         if (var6 != LevelStem.OVERWORLD) {
            var3.register(var6, var5.getValue(), param0.lifecycle(var5.getValue()));
         }
      }

      return var3.freeze();
   }

   public ChunkGenerator overworld() {
      LevelStem var1 = this.dimensions.get(LevelStem.OVERWORLD);
      if (var1 == null) {
         throw new IllegalStateException("Overworld settings missing");
      } else {
         return var1.generator();
      }
   }

   public Optional<LevelStem> get(ResourceKey<LevelStem> param0) {
      return this.dimensions.getOptional(param0);
   }

   public ImmutableSet<ResourceKey<Level>> levels() {
      return this.dimensions().entrySet().stream().map(java.util.Map.Entry::getKey).map(Registries::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
   }

   public boolean isDebug() {
      return this.overworld() instanceof DebugLevelSource;
   }

   private static PrimaryLevelData.SpecialWorldProperty specialWorldProperty(Registry<LevelStem> param0) {
      return param0.getOptional(LevelStem.OVERWORLD).map(l_param0 -> {
         ChunkGenerator l_var1 = l_param0.generator();
         if (l_var1 instanceof DebugLevelSource) {
            return PrimaryLevelData.SpecialWorldProperty.DEBUG;
         } else {
            return l_var1 instanceof FlatLevelSource ? PrimaryLevelData.SpecialWorldProperty.FLAT : PrimaryLevelData.SpecialWorldProperty.NONE;
         }
      }).orElse(PrimaryLevelData.SpecialWorldProperty.NONE);
   }

   static Lifecycle checkStability(ResourceKey<LevelStem> param0, LevelStem param1) {
      return isVanillaLike(param0, param1) ? Lifecycle.stable() : Lifecycle.experimental();
   }

   private static boolean isVanillaLike(ResourceKey<LevelStem> param0, LevelStem param1) {
      if (param0 == LevelStem.OVERWORLD) {
         return isStableOverworld(param1);
      } else if (param0 == LevelStem.NETHER) {
         return isStableNether(param1);
      } else {
         return param0 == LevelStem.END ? isStableEnd(param1) : false;
      }
   }

   private static boolean isStableOverworld(LevelStem param0) {
      Holder<DimensionType> var1 = param0.type();
      return !var1.is(BuiltinDimensionTypes.OVERWORLD) && !var1.is(BuiltinDimensionTypes.OVERWORLD_CAVES)
         ? false
         : !(param0.generator().getBiomeSource() instanceof MultiNoiseBiomeSource var2 && !var2.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
   }

   private static boolean isStableNether(LevelStem param0) {
      return param0.type().is(BuiltinDimensionTypes.NETHER)
         && param0.generator() instanceof NoiseBasedChunkGenerator var2
         && var2.stable(NoiseGeneratorSettings.NETHER)
         && var2.getBiomeSource() instanceof MultiNoiseBiomeSource var1
         && var1.stable(MultiNoiseBiomeSourceParameterLists.NETHER);
   }

   private static boolean isStableEnd(LevelStem param0) {
      return param0.type().is(BuiltinDimensionTypes.END)
         && param0.generator() instanceof NoiseBasedChunkGenerator var1
         && var1.stable(NoiseGeneratorSettings.END)
         && var1.getBiomeSource() instanceof TheEndBiomeSource;
   }

   public WorldDimensions.Complete bake(Registry<LevelStem> param0) {
      Stream<ResourceKey<LevelStem>> var2 = Stream.concat(param0.registryKeySet().stream(), this.dimensions.registryKeySet().stream()).distinct();

      record Entry(ResourceKey<LevelStem> key, LevelStem value) {

         Lifecycle lifecycle() {
            return WorldDimensions.checkStability(this.key, this.value);
         }
      }

      List<Entry> var3 = new ArrayList<>();
      keysInOrder(var2)
         .forEach(
            l_param2 -> param0.getOptional((ResourceKey<LevelStem>)l_param2)
               .or(() -> this.dimensions.getOptional(l_param2))
               .ifPresent(l2_param2 -> var3.add(new Entry(l_param2, l2_param2)))
         );
      Lifecycle var4 = var3.size() == VANILLA_DIMENSION_COUNT ? Lifecycle.stable() : Lifecycle.experimental();
      WritableRegistry<LevelStem> var5 = new MappedRegistry<>(Registries.LEVEL_STEM, var4);
      var3.forEach(l_param1 -> var5.register(l_param1.key, l_param1.value, l_param1.lifecycle()));
      Registry<LevelStem> var6 = var5.freeze();
      PrimaryLevelData.SpecialWorldProperty var7 = specialWorldProperty(var6);
      return new WorldDimensions.Complete(var6.freeze(), var7);
   }

   public record Complete(Registry<LevelStem> dimensions, PrimaryLevelData.SpecialWorldProperty specialWorldProperty) {
      public Lifecycle lifecycle() {
         return this.dimensions.registryLifecycle();
      }

      public RegistryAccess.Frozen dimensionsRegistryAccess() {
         return new RegistryAccess.ImmutableRegistryAccess(List.of(this.dimensions)).freeze();
      }
   }
}
