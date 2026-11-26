package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.datafixers.Products.P5;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public abstract class StructurePlacement {
   public static final Codec<StructurePlacement> CODEC = BuiltInRegistries.STRUCTURE_PLACEMENT
      .byNameCodec()
      .dispatch(StructurePlacement::type, StructurePlacementType::codec);
   private static final int HIGHLY_ARBITRARY_RANDOM_SALT = 10387320;
   private final Vec3i locateOffset;
   private final StructurePlacement.FrequencyReductionMethod frequencyReductionMethod;
   private final float frequency;
   private final int salt;
   private final Optional<StructurePlacement.ExclusionZone> exclusionZone;

   protected static <S extends StructurePlacement> P5<Mu<S>, Vec3i, StructurePlacement.FrequencyReductionMethod, Float, Integer, Optional<StructurePlacement.ExclusionZone>> placementCodec(
      Instance<S> param0
   ) {
      return param0.group(
         Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(StructurePlacement::locateOffset),
         StructurePlacement.FrequencyReductionMethod.CODEC
            .optionalFieldOf("frequency_reduction_method", StructurePlacement.FrequencyReductionMethod.DEFAULT)
            .forGetter(StructurePlacement::frequencyReductionMethod),
         Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(StructurePlacement::frequency),
         ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(StructurePlacement::salt),
         StructurePlacement.ExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(StructurePlacement::exclusionZone)
      );
   }

   protected StructurePlacement(
      Vec3i param0, StructurePlacement.FrequencyReductionMethod param1, float param2, int param3, Optional<StructurePlacement.ExclusionZone> param4
   ) {
      this.locateOffset = param0;
      this.frequencyReductionMethod = param1;
      this.frequency = param2;
      this.salt = param3;
      this.exclusionZone = param4;
   }

   protected Vec3i locateOffset() {
      return this.locateOffset;
   }

   protected StructurePlacement.FrequencyReductionMethod frequencyReductionMethod() {
      return this.frequencyReductionMethod;
   }

   protected float frequency() {
      return this.frequency;
   }

   protected int salt() {
      return this.salt;
   }

   protected Optional<StructurePlacement.ExclusionZone> exclusionZone() {
      return this.exclusionZone;
   }

   public boolean isStructureChunk(ChunkGeneratorStructureState param0, int param1, int param2) {
      if (!this.isPlacementChunk(param0, param1, param2)) {
         return false;
      } else {
         return this.frequency < 1.0F && !this.frequencyReductionMethod.shouldGenerate(param0.getLevelSeed(), this.salt, param1, param2, this.frequency)
            ? false
            : !this.exclusionZone.isPresent() || !this.exclusionZone.get().isPlacementForbidden(param0, param1, param2);
      }
   }

   protected abstract boolean isPlacementChunk(ChunkGeneratorStructureState var1, int var2, int var3);

   public BlockPos getLocatePos(ChunkPos param0) {
      return new BlockPos(param0.getMinBlockX(), 0, param0.getMinBlockZ()).offset(this.locateOffset());
   }

   public abstract StructurePlacementType<?> type();

   private static boolean probabilityReducer(long param0, int param2, int param3, int param4, float param5) {
      WorldgenRandom var6 = new WorldgenRandom(new LegacyRandomSource(0L));
      var6.setLargeFeatureWithSalt(param0, param2, param3, param4);
      return var6.nextFloat() < param5;
   }

   private static boolean legacyProbabilityReducerWithDouble(long param0, int param2, int param3, int param4, float param5) {
      WorldgenRandom var6 = new WorldgenRandom(new LegacyRandomSource(0L));
      var6.setLargeFeatureSeed(param0, param3, param4);
      return var6.nextDouble() < param5;
   }

   private static boolean legacyArbitrarySaltProbabilityReducer(long param0, int param2, int param3, int param4, float param5) {
      WorldgenRandom var6 = new WorldgenRandom(new LegacyRandomSource(0L));
      var6.setLargeFeatureWithSalt(param0, param3, param4, 10387320);
      return var6.nextFloat() < param5;
   }

   private static boolean legacyPillagerOutpostReducer(long param0, int param2, int param3, int param4, float param5) {
      int var6 = param3 >> 4;
      int var7 = param4 >> 4;
      WorldgenRandom var8 = new WorldgenRandom(new LegacyRandomSource(0L));
      var8.setSeed(var6 ^ var7 << 4 ^ param0);
      var8.nextInt();
      return var8.nextInt((int)(1.0F / param5)) == 0;
   }

   @Deprecated
   public record ExclusionZone(Holder<StructureSet> otherSet, int chunkCount) {
      public static final Codec<StructurePlacement.ExclusionZone> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               RegistryFileCodec.create(Registries.STRUCTURE_SET, StructureSet.DIRECT_CODEC, false)
                  .fieldOf("other_set")
                  .forGetter(StructurePlacement.ExclusionZone::otherSet),
               Codec.intRange(1, 16).fieldOf("chunk_count").forGetter(StructurePlacement.ExclusionZone::chunkCount)
            )
            .apply(param0, StructurePlacement.ExclusionZone::new)
      );

      boolean isPlacementForbidden(ChunkGeneratorStructureState param0, int param1, int param2) {
         return param0.hasStructureChunkInRange(this.otherSet, param1, param2, this.chunkCount);
      }
   }

   @FunctionalInterface
   public interface FrequencyReducer {
      boolean shouldGenerate(long var1, int var3, int var4, int var5, float var6);
   }

   public enum FrequencyReductionMethod implements StringRepresentable {
      DEFAULT("default", StructurePlacement::probabilityReducer),
      LEGACY_TYPE_1("legacy_type_1", StructurePlacement::legacyPillagerOutpostReducer),
      LEGACY_TYPE_2("legacy_type_2", StructurePlacement::legacyArbitrarySaltProbabilityReducer),
      LEGACY_TYPE_3("legacy_type_3", StructurePlacement::legacyProbabilityReducerWithDouble);

      public static final Codec<StructurePlacement.FrequencyReductionMethod> CODEC = StringRepresentable.fromEnum(
         StructurePlacement.FrequencyReductionMethod::values
      );
      private final String name;
      private final StructurePlacement.FrequencyReducer reducer;

      FrequencyReductionMethod(String param2, StructurePlacement.FrequencyReducer param3) {
         this.name = param2;
         this.reducer = param3;
      }

      public boolean shouldGenerate(long param0, int param2, int param3, int param4, float param5) {
         return this.reducer.shouldGenerate(param0, param2, param3, param4, param5);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
