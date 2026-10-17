package net.minecraft.world.level.levelgen.presets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public class WorldPreset {
   public static final Codec<WorldPreset> DIRECT_CODEC = ExtraCodecs.validate(
      RecordCodecBuilder.create(
         param0 -> param0.group(
               Codec.unboundedMap(ResourceKey.codec(Registries.LEVEL_STEM), LevelStem.CODEC).fieldOf("dimensions").forGetter(l_param0 -> l_param0.dimensions)
            )
            .apply(param0, WorldPreset::new)
      ),
      WorldPreset::requireOverworld
   );
   public static final Codec<Holder<WorldPreset>> CODEC = RegistryFileCodec.create(Registries.WORLD_PRESET, DIRECT_CODEC);
   private final Map<ResourceKey<LevelStem>, LevelStem> dimensions;

   public WorldPreset(Map<ResourceKey<LevelStem>, LevelStem> param0) {
      this.dimensions = param0;
   }

   private Registry<LevelStem> createRegistry() {
      WritableRegistry<LevelStem> var1 = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.experimental());
      WorldDimensions.keysInOrder(this.dimensions.keySet().stream()).forEach(l_param1 -> {
         LevelStem l_var3 = this.dimensions.get(l_param1);
         if (l_var3 != null) {
            var1.register((ResourceKey<LevelStem>)l_param1, l_var3, Lifecycle.stable());
         }
      });
      return var1.freeze();
   }

   public WorldDimensions createWorldDimensions() {
      return new WorldDimensions(this.createRegistry());
   }

   public Optional<LevelStem> overworld() {
      return Optional.ofNullable(this.dimensions.get(LevelStem.OVERWORLD));
   }

   private static DataResult<WorldPreset> requireOverworld(WorldPreset param0) {
      return param0.overworld().isEmpty() ? DataResult.error(() -> "Missing overworld dimension") : DataResult.success(param0, Lifecycle.stable());
   }
}
