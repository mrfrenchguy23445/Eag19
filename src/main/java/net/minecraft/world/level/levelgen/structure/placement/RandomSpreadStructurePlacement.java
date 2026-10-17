package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class RandomSpreadStructurePlacement extends StructurePlacement {
   public static final Codec<RandomSpreadStructurePlacement> CODEC = RecordCodecBuilder.<RandomSpreadStructurePlacement>mapCodec(
         param0 -> placementCodec(param0)
            .and(
               param0.group(
                  Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
                  Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
                  RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType)
               )
            )
            .apply(param0, RandomSpreadStructurePlacement::new)
      )
      .flatXmap(
         param0 -> param0.spacing <= param0.separation
            ? DataResult.error(() -> "Spacing has to be larger than separation")
            : DataResult.success((RandomSpreadStructurePlacement)param0),
         DataResult::success
      )
      .codec();
   private final int spacing;
   private final int separation;
   private final RandomSpreadType spreadType;

   public RandomSpreadStructurePlacement(
      Vec3i param0,
      StructurePlacement.FrequencyReductionMethod param1,
      float param2,
      int param3,
      Optional<StructurePlacement.ExclusionZone> param4,
      int param5,
      int param6,
      RandomSpreadType param7
   ) {
      super(param0, param1, param2, param3, param4);
      this.spacing = param5;
      this.separation = param6;
      this.spreadType = param7;
   }

   public RandomSpreadStructurePlacement(int param0, int param1, RandomSpreadType param2, int param3) {
      this(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, param3, Optional.empty(), param0, param1, param2);
   }

   public int spacing() {
      return this.spacing;
   }

   public int separation() {
      return this.separation;
   }

   public RandomSpreadType spreadType() {
      return this.spreadType;
   }

   public ChunkPos getPotentialStructureChunk(long param0, int param2, int param3) {
      int var5 = Math.floorDiv(param2, this.spacing);
      int var6 = Math.floorDiv(param3, this.spacing);
      WorldgenRandom var7 = new WorldgenRandom(new LegacyRandomSource(0L));
      var7.setLargeFeatureWithSalt(param0, var5, var6, this.salt());
      int var8 = this.spacing - this.separation;
      int var9 = this.spreadType.evaluate(var7, var8);
      int var10 = this.spreadType.evaluate(var7, var8);
      return new ChunkPos(var5 * this.spacing + var9, var6 * this.spacing + var10);
   }

   @Override
   protected boolean isPlacementChunk(ChunkGeneratorStructureState param0, int param1, int param2) {
      ChunkPos var4 = this.getPotentialStructureChunk(param0.getLevelSeed(), param1, param2);
      return var4.x == param1 && var4.z == param2;
   }

   @Override
   public StructurePlacementType<?> type() {
      return StructurePlacementType.RANDOM_SPREAD;
   }
}
