package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WoodlandMansionStructure extends Structure {
   public static final Codec<WoodlandMansionStructure> CODEC = simpleCodec(WoodlandMansionStructure::new);

   public WoodlandMansionStructure(Structure.StructureSettings param0) {
      super(param0);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext param0) {
      Rotation var2 = Rotation.getRandom(param0.random());
      BlockPos var3 = this.getLowestYIn5by5BoxOffset7Blocks(param0, var2);
      return var3.getY() < 60
         ? Optional.empty()
         : Optional.of(new Structure.GenerationStub(var3, l_param3 -> this.generatePieces(l_param3, param0, var3, var2)));
   }

   private void generatePieces(StructurePiecesBuilder param0, Structure.GenerationContext param1, BlockPos param2, Rotation param3) {
      List<WoodlandMansionPieces.WoodlandMansionPiece> var5 = Lists.newLinkedList();
      WoodlandMansionPieces.generateMansion(param1.structureTemplateManager(), param2, param3, var5, param1.random());
      var5.forEach(param0::addPiece);
   }

   @Override
   public void afterPlace(
      WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, PiecesContainer param6
   ) {
      BlockPos.MutableBlockPos var8 = new BlockPos.MutableBlockPos();
      int var9 = param0.getMinBuildHeight();
      BoundingBox var10 = param6.calculateBoundingBox();
      int var11 = var10.minY();

      for (int var12 = param4.minX(); var12 <= param4.maxX(); var12++) {
         for (int var13 = param4.minZ(); var13 <= param4.maxZ(); var13++) {
            var8.set(var12, var11, var13);
            if (!param0.isEmptyBlock(var8) && var10.isInside(var8) && param6.isInsidePiece(var8)) {
               for (int var14 = var11 - 1; var14 > var9; var14--) {
                  var8.setY(var14);
                  if (!param0.isEmptyBlock(var8) && !param0.getBlockState(var8).getMaterial().isLiquid()) {
                     break;
                  }

                  param0.setBlock(var8, Blocks.COBBLESTONE.defaultBlockState(), 2);
               }
            }
         }
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.WOODLAND_MANSION;
   }
}
