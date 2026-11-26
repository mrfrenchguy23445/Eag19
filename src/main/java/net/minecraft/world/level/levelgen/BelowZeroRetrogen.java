package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.BitSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;

public final class BelowZeroRetrogen {
   private static final BitSet EMPTY = new BitSet(0);
   private static final Codec<BitSet> BITSET_CODEC = Codec.LONG_STREAM
      .xmap(param0 -> BitSet.valueOf(param0.toArray()), param0 -> LongStream.of(param0.toLongArray()));
   private static final Codec<ChunkStatus> NON_EMPTY_CHUNK_STATUS = BuiltInRegistries.CHUNK_STATUS
      .byNameCodec()
      .comapFlatMap(
         param0 -> param0 == ChunkStatus.EMPTY ? DataResult.error(() -> "target_status cannot be empty") : DataResult.success((ChunkStatus)param0),
         Function.identity()
      );
   public static final Codec<BelowZeroRetrogen> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            NON_EMPTY_CHUNK_STATUS.fieldOf("target_status").forGetter(BelowZeroRetrogen::targetStatus),
            BITSET_CODEC.optionalFieldOf("missing_bedrock")
               .forGetter(l_param0 -> l_param0.missingBedrock.isEmpty() ? Optional.empty() : Optional.of(l_param0.missingBedrock))
         )
         .apply(param0, BelowZeroRetrogen::new)
   );
   private static final Set<ResourceKey<Biome>> RETAINED_RETROGEN_BIOMES = Set.of(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES);
   public static final LevelHeightAccessor UPGRADE_HEIGHT_ACCESSOR = new LevelHeightAccessor() {
      @Override
      public int getHeight() {
         return 64;
      }

      @Override
      public int getMinBuildHeight() {
         return -64;
      }
   };
   private final ChunkStatus targetStatus;
   private final BitSet missingBedrock;

   private BelowZeroRetrogen(ChunkStatus param0, Optional<BitSet> param1) {
      this.targetStatus = param0;
      this.missingBedrock = param1.orElse(EMPTY);
   }

   @Nullable
   public static BelowZeroRetrogen read(CompoundTag param0) {
      ChunkStatus var1 = ChunkStatus.byName(param0.getString("target_status"));
      return var1 == ChunkStatus.EMPTY ? null : new BelowZeroRetrogen(var1, Optional.of(BitSet.valueOf(param0.getLongArray("missing_bedrock"))));
   }

   public static void replaceOldBedrock(ProtoChunk param0) {
      int var1 = 4;
      BlockPos.betweenClosed(0, 0, 0, 15, 4, 15).forEach(l_param1 -> {
         if (param0.getBlockState(l_param1).is(Blocks.BEDROCK)) {
            param0.setBlockState(l_param1, Blocks.DEEPSLATE.defaultBlockState(), false);
         }
      });
   }

   public void applyBedrockMask(ProtoChunk param0) {
      LevelHeightAccessor var2 = param0.getHeightAccessorForGeneration();
      int var3 = var2.getMinBuildHeight();
      int var4 = var2.getMaxBuildHeight() - 1;

      for (int var5 = 0; var5 < 16; var5++) {
         for (int var6 = 0; var6 < 16; var6++) {
            if (this.hasBedrockHole(var5, var6)) {
               BlockPos.betweenClosed(var5, var3, var6, var5, var4, var6)
                  .forEach(l_param1 -> param0.setBlockState(l_param1, Blocks.AIR.defaultBlockState(), false));
            }
         }
      }
   }

   public ChunkStatus targetStatus() {
      return this.targetStatus;
   }

   public boolean hasBedrockHoles() {
      return !this.missingBedrock.isEmpty();
   }

   public boolean hasBedrockHole(int param0, int param1) {
      return this.missingBedrock.get((param1 & 15) * 16 + (param0 & 15));
   }

   public static BiomeResolver getBiomeResolver(BiomeResolver param0, ChunkAccess param1) {
      if (!param1.isUpgrading()) {
         return param0;
      }

      Predicate<ResourceKey<Biome>> var2 = RETAINED_RETROGEN_BIOMES::contains;
      return (l_param3, l_param4, l_param5, l_param6) -> {
         Holder<Biome> l_var7 = param0.getNoiseBiome(l_param3, l_param4, l_param5, l_param6);
         return l_var7.is(var2) ? l_var7 : param1.getNoiseBiome(l_param3, 0, l_param5);
      };
   }
}
