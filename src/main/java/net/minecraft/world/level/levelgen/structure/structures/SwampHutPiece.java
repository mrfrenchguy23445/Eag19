package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class SwampHutPiece extends ScatteredFeaturePiece {
   private boolean spawnedWitch;
   private boolean spawnedCat;

   public SwampHutPiece(RandomSource param0, int param1, int param2) {
      super(StructurePieceType.SWAMPLAND_HUT, param1, 64, param2, 7, 7, 9, getRandomHorizontalDirection(param0));
   }

   public SwampHutPiece(CompoundTag param0) {
      super(StructurePieceType.SWAMPLAND_HUT, param0);
      this.spawnedWitch = param0.getBoolean("Witch");
      this.spawnedCat = param0.getBoolean("Cat");
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
      super.addAdditionalSaveData(param0, param1);
      param1.putBoolean("Witch", this.spawnedWitch);
      param1.putBoolean("Cat", this.spawnedCat);
   }

   @Override
   public void postProcess(
      WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
   ) {
      if (this.updateAverageGroundHeight(param0, param4, 0)) {
         this.generateBox(param0, param4, 1, 1, 1, 5, 1, 7, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(param0, param4, 1, 4, 2, 5, 4, 7, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(param0, param4, 2, 1, 0, 4, 1, 0, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(param0, param4, 2, 2, 2, 3, 3, 2, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(param0, param4, 1, 2, 3, 1, 3, 6, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(param0, param4, 5, 2, 3, 5, 3, 6, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(param0, param4, 2, 2, 7, 4, 3, 7, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(param0, param4, 1, 0, 2, 1, 3, 2, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.generateBox(param0, param4, 5, 0, 2, 5, 3, 2, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.generateBox(param0, param4, 1, 0, 7, 1, 3, 7, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.generateBox(param0, param4, 5, 0, 7, 5, 3, 7, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.placeBlock(param0, Blocks.OAK_FENCE.defaultBlockState(), 2, 3, 2, param4);
         this.placeBlock(param0, Blocks.OAK_FENCE.defaultBlockState(), 3, 3, 7, param4);
         this.placeBlock(param0, Blocks.AIR.defaultBlockState(), 1, 3, 4, param4);
         this.placeBlock(param0, Blocks.AIR.defaultBlockState(), 5, 3, 4, param4);
         this.placeBlock(param0, Blocks.AIR.defaultBlockState(), 5, 3, 5, param4);
         this.placeBlock(param0, Blocks.POTTED_RED_MUSHROOM.defaultBlockState(), 1, 3, 5, param4);
         this.placeBlock(param0, Blocks.CRAFTING_TABLE.defaultBlockState(), 3, 2, 6, param4);
         this.placeBlock(param0, Blocks.CAULDRON.defaultBlockState(), 4, 2, 6, param4);
         this.placeBlock(param0, Blocks.OAK_FENCE.defaultBlockState(), 1, 2, 1, param4);
         this.placeBlock(param0, Blocks.OAK_FENCE.defaultBlockState(), 5, 2, 1, param4);
         BlockState var8 = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         BlockState var9 = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
         BlockState var10 = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
         BlockState var11 = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         this.generateBox(param0, param4, 0, 4, 1, 6, 4, 1, var8, var8, false);
         this.generateBox(param0, param4, 0, 4, 2, 0, 4, 7, var9, var9, false);
         this.generateBox(param0, param4, 6, 4, 2, 6, 4, 7, var10, var10, false);
         this.generateBox(param0, param4, 0, 4, 8, 6, 4, 8, var11, var11, false);
         this.placeBlock(param0, var8.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 0, 4, 1, param4);
         this.placeBlock(param0, var8.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 6, 4, 1, param4);
         this.placeBlock(param0, var11.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 0, 4, 8, param4);
         this.placeBlock(param0, var11.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 6, 4, 8, param4);

         for (int var12 = 2; var12 <= 7; var12 += 5) {
            for (int var13 = 1; var13 <= 5; var13 += 4) {
               this.fillColumnDown(param0, Blocks.OAK_LOG.defaultBlockState(), var13, -1, var12, param4);
            }
         }

         if (!this.spawnedWitch) {
            BlockPos var12 = this.getWorldPos(2, 2, 5);
            if (param4.isInside(var12)) {
               this.spawnedWitch = true;
               Witch var13 = EntityType.WITCH.create(param0.getLevel());
               if (var13 != null) {
                  var13.setPersistenceRequired();
                  var13.moveTo(var12.getX() + 0.5, var12.getY(), var12.getZ() + 0.5, 0.0F, 0.0F);
                  var13.finalizeSpawn(param0, param0.getCurrentDifficultyAt(var12), MobSpawnType.STRUCTURE, null, null);
                  param0.addFreshEntityWithPassengers(var13);
               }
            }
         }

         this.spawnCat(param0, param4);
      }
   }

   private void spawnCat(ServerLevelAccessor param0, BoundingBox param1) {
      if (!this.spawnedCat) {
         BlockPos var3 = this.getWorldPos(2, 2, 5);
         if (param1.isInside(var3)) {
            this.spawnedCat = true;
            Cat var4 = EntityType.CAT.create(param0.getLevel());
            if (var4 != null) {
               var4.setPersistenceRequired();
               var4.moveTo(var3.getX() + 0.5, var3.getY(), var3.getZ() + 0.5, 0.0F, 0.0F);
               var4.finalizeSpawn(param0, param0.getCurrentDifficultyAt(var3), MobSpawnType.STRUCTURE, null, null);
               param0.addFreshEntityWithPassengers(var4);
            }
         }
      }
   }
}
