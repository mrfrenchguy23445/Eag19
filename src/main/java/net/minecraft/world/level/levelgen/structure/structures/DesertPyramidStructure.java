package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.data.loot.packs.UpdateOneTwentyBuiltInLootTables;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

public class DesertPyramidStructure extends SinglePieceStructure {
   public static final Codec<DesertPyramidStructure> CODEC = simpleCodec(DesertPyramidStructure::new);

   public DesertPyramidStructure(Structure.StructureSettings param0) {
      super(DesertPyramidPiece::new, 21, 21, param0);
   }

   @Override
   public void afterPlace(
      WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, PiecesContainer param6
   ) {
      if (param0.enabledFeatures().contains(FeatureFlags.UPDATE_1_20)) {
         Set<BlockPos> var8 = SortedArraySet.create(Vec3i::compareTo);

         for (StructurePiece var10 : param6.pieces()) {
            if (var10 instanceof DesertPyramidPiece var11) {
               var8.addAll(var11.getPotentialSuspiciousSandWorldPositions());
            }
         }

         ObjectArrayList<BlockPos> var9 = new ObjectArrayList<>(var8.stream().toList());
         Util.shuffle(var9, param3);
         int var10 = Math.min(var8.size(), param3.nextInt(5, 8));

         for (BlockPos var12 : var9) {
            if (var10 > 0) {
               var10--;
               param0.setBlock(var12, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 2);
               param0.getBlockEntity(var12, BlockEntityType.SUSPICIOUS_SAND)
                  .ifPresent(l_param1 -> l_param1.setLootTable(UpdateOneTwentyBuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY, var12.asLong()));
            } else {
               param0.setBlock(var12, Blocks.SAND.defaultBlockState(), 2);
            }
         }
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.DESERT_PYRAMID;
   }
}
