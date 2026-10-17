package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.random.SimpleWeightedRandomList;

public record SpawnData(CompoundTag entityToSpawn, Optional<SpawnData.CustomSpawnRules> customSpawnRules) {
   public static final String ENTITY_TAG = "entity";
   public static final Codec<SpawnData> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            CompoundTag.CODEC.fieldOf("entity").forGetter(l_param0 -> l_param0.entityToSpawn),
            SpawnData.CustomSpawnRules.CODEC.optionalFieldOf("custom_spawn_rules").forGetter(l_param0 -> l_param0.customSpawnRules)
         )
         .apply(param0, SpawnData::new)
   );
   public static final Codec<SimpleWeightedRandomList<SpawnData>> LIST_CODEC = SimpleWeightedRandomList.wrappedCodecAllowingEmpty(CODEC);

   public SpawnData() {
      this(new CompoundTag(), Optional.empty());
   }

   public SpawnData(CompoundTag entityToSpawn, Optional<SpawnData.CustomSpawnRules> customSpawnRules) {
      if (entityToSpawn.contains("id")) {
         ResourceLocation var3 = ResourceLocation.tryParse(entityToSpawn.getString("id"));
         if (var3 != null) {
            entityToSpawn.putString("id", var3.toString());
         } else {
            entityToSpawn.remove("id");
         }
      }

      this.entityToSpawn = entityToSpawn;
      this.customSpawnRules = customSpawnRules;
   }

   public CompoundTag getEntityToSpawn() {
      return this.entityToSpawn;
   }

   public Optional<SpawnData.CustomSpawnRules> getCustomSpawnRules() {
      return this.customSpawnRules;
   }

   public record CustomSpawnRules(InclusiveRange<Integer> blockLightLimit, InclusiveRange<Integer> skyLightLimit) {
      private static final InclusiveRange<Integer> LIGHT_RANGE = new InclusiveRange<>(0, 15);
      public static final Codec<SpawnData.CustomSpawnRules> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               InclusiveRange.INT
                  .optionalFieldOf("block_light_limit", LIGHT_RANGE)
                  .flatXmap(SpawnData.CustomSpawnRules::checkLightBoundaries, SpawnData.CustomSpawnRules::checkLightBoundaries)
                  .forGetter(l_param0 -> l_param0.blockLightLimit),
               InclusiveRange.INT
                  .optionalFieldOf("sky_light_limit", LIGHT_RANGE)
                  .flatXmap(SpawnData.CustomSpawnRules::checkLightBoundaries, SpawnData.CustomSpawnRules::checkLightBoundaries)
                  .forGetter(l_param0 -> l_param0.skyLightLimit)
            )
            .apply(param0, SpawnData.CustomSpawnRules::new)
      );

      private static DataResult<InclusiveRange<Integer>> checkLightBoundaries(InclusiveRange<Integer> param0) {
         return !LIGHT_RANGE.contains(param0) ? DataResult.error(() -> "Light values must be withing range " + LIGHT_RANGE) : DataResult.success(param0);
      }
   }
}
