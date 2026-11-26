package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class BuriedTreasurePieces {
   public static class BuriedTreasurePiece extends StructurePiece {
      public BuriedTreasurePiece(BlockPos param0) {
         super(StructurePieceType.BURIED_TREASURE_PIECE, 0, new BoundingBox(param0));
      }

      public BuriedTreasurePiece(CompoundTag param0) {
         super(StructurePieceType.BURIED_TREASURE_PIECE, param0);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         int var8 = param0.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.boundingBox.minX(), this.boundingBox.minZ());
         BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos(this.boundingBox.minX(), var8, this.boundingBox.minZ());

         while (var9.getY() > param0.getMinBuildHeight()) {
            BlockState var10 = param0.getBlockState(var9);
            BlockState var11 = param0.getBlockState(var9.below());
            if (var11 == Blocks.SANDSTONE.defaultBlockState()
               || var11 == Blocks.STONE.defaultBlockState()
               || var11 == Blocks.ANDESITE.defaultBlockState()
               || var11 == Blocks.GRANITE.defaultBlockState()
               || var11 == Blocks.DIORITE.defaultBlockState()) {
               BlockState var12 = !var10.isAir() && !this.isLiquid(var10) ? var10 : Blocks.SAND.defaultBlockState();

               for (Direction var16 : Direction.values()) {
                  BlockPos var17 = var9.relative(var16);
                  BlockState var18 = param0.getBlockState(var17);
                  if (var18.isAir() || this.isLiquid(var18)) {
                     BlockPos var19 = var17.below();
                     BlockState var20 = param0.getBlockState(var19);
                     if ((var20.isAir() || this.isLiquid(var20)) && var16 != Direction.UP) {
                        param0.setBlock(var17, var11, 3);
                     } else {
                        param0.setBlock(var17, var12, 3);
                     }
                  }
               }

               this.boundingBox = new BoundingBox(var9);
               this.createChest(param0, param4, param3, var9, BuiltInLootTables.BURIED_TREASURE, null);
               return;
            }

            var9.move(0, -1, 0);
         }
      }

      private boolean isLiquid(BlockState param0) {
         return param0 == Blocks.WATER.defaultBlockState() || param0 == Blocks.LAVA.defaultBlockState();
      }
   }
}
