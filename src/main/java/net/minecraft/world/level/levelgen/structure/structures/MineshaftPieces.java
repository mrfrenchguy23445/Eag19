package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.slf4j.Logger;

public class MineshaftPieces {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_SHAFT_WIDTH = 3;
   private static final int DEFAULT_SHAFT_HEIGHT = 3;
   private static final int DEFAULT_SHAFT_LENGTH = 5;
   private static final int MAX_PILLAR_HEIGHT = 20;
   private static final int MAX_CHAIN_HEIGHT = 50;
   private static final int MAX_DEPTH = 8;
   public static final int MAGIC_START_Y = 50;

   private static MineshaftPieces.MineShaftPiece createRandomShaftPiece(
      StructurePieceAccessor param0,
      RandomSource param1,
      int param2,
      int param3,
      int param4,
      @Nullable Direction param5,
      int param6,
      MineshaftStructure.Type param7
   ) {
      int var8 = param1.nextInt(100);
      if (var8 >= 80) {
         BoundingBox var9 = MineshaftPieces.MineShaftCrossing.findCrossing(param0, param1, param2, param3, param4, param5);
         if (var9 != null) {
            return new MineshaftPieces.MineShaftCrossing(param6, var9, param5, param7);
         }
      } else if (var8 >= 70) {
         BoundingBox var9 = MineshaftPieces.MineShaftStairs.findStairs(param0, param1, param2, param3, param4, param5);
         if (var9 != null) {
            return new MineshaftPieces.MineShaftStairs(param6, var9, param5, param7);
         }
      } else {
         BoundingBox var9 = MineshaftPieces.MineShaftCorridor.findCorridorSize(param0, param1, param2, param3, param4, param5);
         if (var9 != null) {
            return new MineshaftPieces.MineShaftCorridor(param6, param1, var9, param5, param7);
         }
      }

      return null;
   }

   static MineshaftPieces.MineShaftPiece generateAndAddPiece(
      StructurePiece param0, StructurePieceAccessor param1, RandomSource param2, int param3, int param4, int param5, Direction param6, int param7
   ) {
      if (param7 > 8) {
         return null;
      }

      if (Math.abs(param3 - param0.getBoundingBox().minX()) <= 80 && Math.abs(param5 - param0.getBoundingBox().minZ()) <= 80) {
         MineshaftStructure.Type var8 = ((MineshaftPieces.MineShaftPiece)param0).type;
         MineshaftPieces.MineShaftPiece var9 = createRandomShaftPiece(param1, param2, param3, param4, param5, param6, param7 + 1, var8);
         if (var9 != null) {
            param1.addPiece(var9);
            var9.addChildren(param0, param1, param2);
         }

         return var9;
      } else {
         return null;
      }
   }

   public static class MineShaftCorridor extends MineshaftPieces.MineShaftPiece {
      private final boolean hasRails;
      private final boolean spiderCorridor;
      private boolean hasPlacedSpider;
      private final int numSections;

      public MineShaftCorridor(CompoundTag param0) {
         super(StructurePieceType.MINE_SHAFT_CORRIDOR, param0);
         this.hasRails = param0.getBoolean("hr");
         this.spiderCorridor = param0.getBoolean("sc");
         this.hasPlacedSpider = param0.getBoolean("hps");
         this.numSections = param0.getInt("Num");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putBoolean("hr", this.hasRails);
         param1.putBoolean("sc", this.spiderCorridor);
         param1.putBoolean("hps", this.hasPlacedSpider);
         param1.putInt("Num", this.numSections);
      }

      public MineShaftCorridor(int param0, RandomSource param1, BoundingBox param2, Direction param3, MineshaftStructure.Type param4) {
         super(StructurePieceType.MINE_SHAFT_CORRIDOR, param0, param4, param2);
         this.setOrientation(param3);
         this.hasRails = param1.nextInt(3) == 0;
         this.spiderCorridor = !this.hasRails && param1.nextInt(23) == 0;
         if (this.getOrientation().getAxis() == Direction.Axis.Z) {
            this.numSections = param2.getZSpan() / 5;
         } else {
            this.numSections = param2.getXSpan() / 5;
         }
      }

      @Nullable
      public static BoundingBox findCorridorSize(StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5) {
         for (int var6 = param1.nextInt(3) + 2; var6 > 0; var6--) {
            int var8 = var6 * 5;

            BoundingBox var7 = switch (param5) {
               default -> new BoundingBox(0, 0, -(var8 - 1), 2, 2, 0);
               case SOUTH -> new BoundingBox(0, 0, 0, 2, 2, var8 - 1);
               case WEST -> new BoundingBox(-(var8 - 1), 0, 0, 0, 2, 2);
               case EAST -> new BoundingBox(0, 0, 0, var8 - 1, 2, 2);
            };
            var7.move(param2, param3, param4);
            if (param0.findCollisionPiece(var7) == null) {
               return var7;
            }
         }

         return null;
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         int var4 = this.getGenDepth();
         int var5 = param2.nextInt(4);
         Direction var6 = this.getOrientation();
         if (var6 != null) {
            switch (var6) {
               case NORTH:
               default:
                  if (var5 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.minZ() - 1,
                        var6,
                        var4
                     );
                  } else if (var5 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.minZ(),
                        Direction.WEST,
                        var4
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.minZ(),
                        Direction.EAST,
                        var4
                     );
                  }
                  break;
               case SOUTH:
                  if (var5 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.maxZ() + 1,
                        var6,
                        var4
                     );
                  } else if (var5 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.maxZ() - 3,
                        Direction.WEST,
                        var4
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.maxZ() - 3,
                        Direction.EAST,
                        var4
                     );
                  }
                  break;
               case WEST:
                  if (var5 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.minZ(),
                        var6,
                        var4
                     );
                  } else if (var5 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        var4
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        var4
                     );
                  }
                  break;
               case EAST:
                  if (var5 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.minZ(),
                        var6,
                        var4
                     );
                  } else if (var5 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.maxX() - 3,
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        var4
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        param0,
                        param1,
                        param2,
                        this.boundingBox.maxX() - 3,
                        this.boundingBox.minY() - 1 + param2.nextInt(3),
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        var4
                     );
                  }
            }
         }

         if (var4 < 8) {
            if (var6 != Direction.NORTH && var6 != Direction.SOUTH) {
               for (int var7 = this.boundingBox.minX() + 3; var7 + 3 <= this.boundingBox.maxX(); var7 += 5) {
                  int var8 = param2.nextInt(5);
                  if (var8 == 0) {
                     MineshaftPieces.generateAndAddPiece(
                        param0, param1, param2, var7, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, var4 + 1
                     );
                  } else if (var8 == 1) {
                     MineshaftPieces.generateAndAddPiece(
                        param0, param1, param2, var7, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, var4 + 1
                     );
                  }
               }
            } else {
               for (int var7 = this.boundingBox.minZ() + 3; var7 + 3 <= this.boundingBox.maxZ(); var7 += 5) {
                  int var8 = param2.nextInt(5);
                  if (var8 == 0) {
                     MineshaftPieces.generateAndAddPiece(
                        param0, param1, param2, this.boundingBox.minX() - 1, this.boundingBox.minY(), var7, Direction.WEST, var4 + 1
                     );
                  } else if (var8 == 1) {
                     MineshaftPieces.generateAndAddPiece(
                        param0, param1, param2, this.boundingBox.maxX() + 1, this.boundingBox.minY(), var7, Direction.EAST, var4 + 1
                     );
                  }
               }
            }
         }
      }

      @Override
      protected boolean createChest(WorldGenLevel param0, BoundingBox param1, RandomSource param2, int param3, int param4, int param5, ResourceLocation param6) {
         BlockPos var8 = this.getWorldPos(param3, param4, param5);
         if (param1.isInside(var8) && param0.getBlockState(var8).isAir() && !param0.getBlockState(var8.below()).isAir()) {
            BlockState var9 = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, param2.nextBoolean() ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
            this.placeBlock(param0, var9, param3, param4, param5, param1);
            MinecartChest var10 = new MinecartChest(param0.getLevel(), var8.getX() + 0.5, var8.getY() + 0.5, var8.getZ() + 0.5);
            var10.setLootTable(param6, param2.nextLong());
            param0.addFreshEntity(var10);
            return true;
         } else {
            return false;
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         if (!this.isInInvalidLocation(param0, param4)) {
            int var8 = 0;
            int var9 = 2;
            int var10 = 0;
            int var11 = 2;
            int var12 = this.numSections * 5 - 1;
            BlockState var13 = this.type.getPlanksState();
            this.generateBox(param0, param4, 0, 0, 0, 2, 1, var12, CAVE_AIR, CAVE_AIR, false);
            this.generateMaybeBox(param0, param4, param3, 0.8F, 0, 2, 0, 2, 2, var12, CAVE_AIR, CAVE_AIR, false, false);
            if (this.spiderCorridor) {
               this.generateMaybeBox(param0, param4, param3, 0.6F, 0, 0, 0, 2, 1, var12, Blocks.COBWEB.defaultBlockState(), CAVE_AIR, false, true);
            }

            for (int var14 = 0; var14 < this.numSections; var14++) {
               int var15 = 2 + var14 * 5;
               this.placeSupport(param0, param4, 0, 0, var15, 2, 2, param3);
               this.maybePlaceCobWeb(param0, param4, param3, 0.1F, 0, 2, var15 - 1);
               this.maybePlaceCobWeb(param0, param4, param3, 0.1F, 2, 2, var15 - 1);
               this.maybePlaceCobWeb(param0, param4, param3, 0.1F, 0, 2, var15 + 1);
               this.maybePlaceCobWeb(param0, param4, param3, 0.1F, 2, 2, var15 + 1);
               this.maybePlaceCobWeb(param0, param4, param3, 0.05F, 0, 2, var15 - 2);
               this.maybePlaceCobWeb(param0, param4, param3, 0.05F, 2, 2, var15 - 2);
               this.maybePlaceCobWeb(param0, param4, param3, 0.05F, 0, 2, var15 + 2);
               this.maybePlaceCobWeb(param0, param4, param3, 0.05F, 2, 2, var15 + 2);
               if (param3.nextInt(100) == 0) {
                  this.createChest(param0, param4, param3, 2, 0, var15 - 1, BuiltInLootTables.ABANDONED_MINESHAFT);
               }

               if (param3.nextInt(100) == 0) {
                  this.createChest(param0, param4, param3, 0, 0, var15 + 1, BuiltInLootTables.ABANDONED_MINESHAFT);
               }

               if (this.spiderCorridor && !this.hasPlacedSpider) {
                  int var16 = 1;
                  int var17 = var15 - 1 + param3.nextInt(3);
                  BlockPos var18 = this.getWorldPos(1, 0, var17);
                  if (param4.isInside(var18) && this.isInterior(param0, 1, 0, var17, param4)) {
                     this.hasPlacedSpider = true;
                     param0.setBlock(var18, Blocks.SPAWNER.defaultBlockState(), 2);
                     if (param0.getBlockEntity(var18) instanceof SpawnerBlockEntity var20) {
                        var20.setEntityId(EntityType.CAVE_SPIDER, param3);
                     }
                  }
               }
            }

            for (int var14 = 0; var14 <= 2; var14++) {
               for (int var15 = 0; var15 <= var12; var15++) {
                  this.setPlanksBlock(param0, param4, var13, var14, -1, var15);
               }
            }

            int var14 = 2;
            this.placeDoubleLowerOrUpperSupport(param0, param4, 0, -1, 2);
            if (this.numSections > 1) {
               int var15 = var12 - 2;
               this.placeDoubleLowerOrUpperSupport(param0, param4, 0, -1, var15);
            }

            if (this.hasRails) {
               BlockState var15 = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);

               for (int var16 = 0; var16 <= var12; var16++) {
                  BlockState var17 = this.getBlock(param0, 1, -1, var16, param4);
                  if (!var17.isAir() && var17.isSolidRender(param0, this.getWorldPos(1, -1, var16))) {
                     float var18 = this.isInterior(param0, 1, 0, var16, param4) ? 0.7F : 0.9F;
                     this.maybeGenerateBlock(param0, param4, param3, var18, 1, 0, var16, var15);
                  }
               }
            }
         }
      }

      private void placeDoubleLowerOrUpperSupport(WorldGenLevel param0, BoundingBox param1, int param2, int param3, int param4) {
         BlockState var6 = this.type.getWoodState();
         BlockState var7 = this.type.getPlanksState();
         if (this.getBlock(param0, param2, param3, param4, param1).is(var7.getBlock())) {
            this.fillPillarDownOrChainUp(param0, var6, param2, param3, param4, param1);
         }

         if (this.getBlock(param0, param2 + 2, param3, param4, param1).is(var7.getBlock())) {
            this.fillPillarDownOrChainUp(param0, var6, param2 + 2, param3, param4, param1);
         }
      }

      @Override
      protected void fillColumnDown(WorldGenLevel param0, BlockState param1, int param2, int param3, int param4, BoundingBox param5) {
         BlockPos.MutableBlockPos var7 = this.getWorldPos(param2, param3, param4);
         if (param5.isInside(var7)) {
            int var8 = var7.getY();

            while (this.isReplaceableByStructures(param0.getBlockState(var7)) && var7.getY() > param0.getMinBuildHeight() + 1) {
               var7.move(Direction.DOWN);
            }

            if (this.canPlaceColumnOnTopOf(param0, var7, param0.getBlockState(var7))) {
               while (var7.getY() < var8) {
                  var7.move(Direction.UP);
                  param0.setBlock(var7, param1, 2);
               }
            }
         }
      }

      protected void fillPillarDownOrChainUp(WorldGenLevel param0, BlockState param1, int param2, int param3, int param4, BoundingBox param5) {
         BlockPos.MutableBlockPos var7 = this.getWorldPos(param2, param3, param4);
         if (param5.isInside(var7)) {
            int var8 = var7.getY();
            int var9 = 1;
            boolean var10 = true;

            for (boolean var11 = true; var10 || var11; var9++) {
               if (var10) {
                  var7.setY(var8 - var9);
                  BlockState var12 = param0.getBlockState(var7);
                  boolean var13 = this.isReplaceableByStructures(var12) && !var12.is(Blocks.LAVA);
                  if (!var13 && this.canPlaceColumnOnTopOf(param0, var7, var12)) {
                     fillColumnBetween(param0, param1, var7, var8 - var9 + 1, var8);
                     return;
                  }

                  var10 = var9 <= 20 && var13 && var7.getY() > param0.getMinBuildHeight() + 1;
               }

               if (var11) {
                  var7.setY(var8 + var9);
                  BlockState var12 = param0.getBlockState(var7);
                  boolean var13 = this.isReplaceableByStructures(var12);
                  if (!var13 && this.canHangChainBelow(param0, var7, var12)) {
                     param0.setBlock(var7.setY(var8 + 1), this.type.getFenceState(), 2);
                     fillColumnBetween(param0, Blocks.CHAIN.defaultBlockState(), var7, var8 + 2, var8 + var9);
                     return;
                  }

                  var11 = var9 <= 50 && var13 && var7.getY() < param0.getMaxBuildHeight() - 1;
               }
            }
         }
      }

      private static void fillColumnBetween(WorldGenLevel param0, BlockState param1, BlockPos.MutableBlockPos param2, int param3, int param4) {
         for (int var5 = param3; var5 < param4; var5++) {
            param0.setBlock(param2.setY(var5), param1, 2);
         }
      }

      private boolean canPlaceColumnOnTopOf(LevelReader param0, BlockPos param1, BlockState param2) {
         return param2.isFaceSturdy(param0, param1, Direction.UP);
      }

      private boolean canHangChainBelow(LevelReader param0, BlockPos param1, BlockState param2) {
         return Block.canSupportCenter(param0, param1, Direction.DOWN) && !(param2.getBlock() instanceof FallingBlock);
      }

      private void placeSupport(WorldGenLevel param0, BoundingBox param1, int param2, int param3, int param4, int param5, int param6, RandomSource param7) {
         if (this.isSupportingBox(param0, param1, param2, param6, param5, param4)) {
            BlockState var9 = this.type.getPlanksState();
            BlockState var10 = this.type.getFenceState();
            this.generateBox(param0, param1, param2, param3, param4, param2, param5 - 1, param4, var10.setValue(FenceBlock.WEST, true), CAVE_AIR, false);
            this.generateBox(param0, param1, param6, param3, param4, param6, param5 - 1, param4, var10.setValue(FenceBlock.EAST, true), CAVE_AIR, false);
            if (param7.nextInt(4) == 0) {
               this.generateBox(param0, param1, param2, param5, param4, param2, param5, param4, var9, CAVE_AIR, false);
               this.generateBox(param0, param1, param6, param5, param4, param6, param5, param4, var9, CAVE_AIR, false);
            } else {
               this.generateBox(param0, param1, param2, param5, param4, param6, param5, param4, var9, CAVE_AIR, false);
               this.maybeGenerateBlock(
                  param0,
                  param1,
                  param7,
                  0.05F,
                  param2 + 1,
                  param5,
                  param4 - 1,
                  Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH)
               );
               this.maybeGenerateBlock(
                  param0,
                  param1,
                  param7,
                  0.05F,
                  param2 + 1,
                  param5,
                  param4 + 1,
                  Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH)
               );
            }
         }
      }

      private void maybePlaceCobWeb(WorldGenLevel param0, BoundingBox param1, RandomSource param2, float param3, int param4, int param5, int param6) {
         if (this.isInterior(param0, param4, param5, param6, param1)
            && param2.nextFloat() < param3
            && this.hasSturdyNeighbours(param0, param1, param4, param5, param6, 2)) {
            this.placeBlock(param0, Blocks.COBWEB.defaultBlockState(), param4, param5, param6, param1);
         }
      }

      private boolean hasSturdyNeighbours(WorldGenLevel param0, BoundingBox param1, int param2, int param3, int param4, int param5) {
         BlockPos.MutableBlockPos var7 = this.getWorldPos(param2, param3, param4);
         int var8 = 0;

         for (Direction var12 : Direction.values()) {
            var7.move(var12);
            if (param1.isInside(var7) && param0.getBlockState(var7).isFaceSturdy(param0, var7, var12.getOpposite())) {
               if (++var8 >= param5) {
                  return true;
               }
            }

            var7.move(var12.getOpposite());
         }

         return false;
      }
   }

   public static class MineShaftCrossing extends MineshaftPieces.MineShaftPiece {
      private final Direction direction;
      private final boolean isTwoFloored;

      public MineShaftCrossing(CompoundTag param0) {
         super(StructurePieceType.MINE_SHAFT_CROSSING, param0);
         this.isTwoFloored = param0.getBoolean("tf");
         this.direction = Direction.from2DDataValue(param0.getInt("D"));
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putBoolean("tf", this.isTwoFloored);
         param1.putInt("D", this.direction.get2DDataValue());
      }

      public MineShaftCrossing(int param0, BoundingBox param1, @Nullable Direction param2, MineshaftStructure.Type param3) {
         super(StructurePieceType.MINE_SHAFT_CROSSING, param0, param3, param1);
         this.direction = param2;
         this.isTwoFloored = param1.getYSpan() > 3;
      }

      @Nullable
      public static BoundingBox findCrossing(StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5) {
         int var6;
         if (param1.nextInt(4) == 0) {
            var6 = 6;
         } else {
            var6 = 2;
         }
         BoundingBox var7 = switch (param5) {
            default -> new BoundingBox(-1, 0, -4, 3, var6, 0);
            case SOUTH -> new BoundingBox(-1, 0, 0, 3, var6, 4);
            case WEST -> new BoundingBox(-4, 0, -1, 0, var6, 3);
            case EAST -> new BoundingBox(0, 0, -1, 4, var6, 3);
         };
         var7.move(param2, param3, param4);
         return param0.findCollisionPiece(var7) != null ? null : var7;
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         int var4 = this.getGenDepth();
         switch (this.direction) {
            case NORTH:
            default:
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, var4
               );
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, var4
               );
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, var4
               );
               break;
            case SOUTH:
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, var4
               );
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, var4
               );
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, var4
               );
               break;
            case WEST:
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, var4
               );
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, var4
               );
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, var4
               );
               break;
            case EAST:
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, var4
               );
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, var4
               );
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, var4
               );
         }

         if (this.isTwoFloored) {
            if (param2.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() - 1, Direction.NORTH, var4
               );
            }

            if (param2.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() - 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, Direction.WEST, var4
               );
            }

            if (param2.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.maxX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, Direction.EAST, var4
               );
            }

            if (param2.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  param0, param1, param2, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.maxZ() + 1, Direction.SOUTH, var4
               );
            }
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         if (!this.isInInvalidLocation(param0, param4)) {
            BlockState var8 = this.type.getPlanksState();
            if (this.isTwoFloored) {
               this.generateBox(
                  param0,
                  param4,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.minY() + 3 - 1,
                  this.boundingBox.maxZ(),
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  param0,
                  param4,
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX(),
                  this.boundingBox.minY() + 3 - 1,
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  param0,
                  param4,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.maxY() - 2,
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  param0,
                  param4,
                  this.boundingBox.minX(),
                  this.boundingBox.maxY() - 2,
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  param0,
                  param4,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY() + 3,
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.minY() + 3,
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
            } else {
               this.generateBox(
                  param0,
                  param4,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  param0,
                  param4,
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
            }

            this.placeSupportPillar(param0, param4, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
            this.placeSupportPillar(param0, param4, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
            this.placeSupportPillar(param0, param4, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
            this.placeSupportPillar(param0, param4, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
            int var9 = this.boundingBox.minY() - 1;

            for (int var10 = this.boundingBox.minX(); var10 <= this.boundingBox.maxX(); var10++) {
               for (int var11 = this.boundingBox.minZ(); var11 <= this.boundingBox.maxZ(); var11++) {
                  this.setPlanksBlock(param0, param4, var8, var10, var9, var11);
               }
            }
         }
      }

      private void placeSupportPillar(WorldGenLevel param0, BoundingBox param1, int param2, int param3, int param4, int param5) {
         if (!this.getBlock(param0, param2, param5 + 1, param4, param1).isAir()) {
            this.generateBox(param0, param1, param2, param3, param4, param2, param5, param4, this.type.getPlanksState(), CAVE_AIR, false);
         }
      }
   }

   abstract static class MineShaftPiece extends StructurePiece {
      protected MineshaftStructure.Type type;

      public MineShaftPiece(StructurePieceType param0, int param1, MineshaftStructure.Type param2, BoundingBox param3) {
         super(param0, param1, param3);
         this.type = param2;
      }

      public MineShaftPiece(StructurePieceType param0, CompoundTag param1) {
         super(param0, param1);
         this.type = MineshaftStructure.Type.byId(param1.getInt("MST"));
      }

      @Override
      protected boolean canBeReplaced(LevelReader param0, int param1, int param2, int param3, BoundingBox param4) {
         BlockState var6 = this.getBlock(param0, param1, param2, param3, param4);
         return !var6.is(this.type.getPlanksState().getBlock())
            && !var6.is(this.type.getWoodState().getBlock())
            && !var6.is(this.type.getFenceState().getBlock())
            && !var6.is(Blocks.CHAIN);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         param1.putInt("MST", this.type.ordinal());
      }

      protected boolean isSupportingBox(BlockGetter param0, BoundingBox param1, int param2, int param3, int param4, int param5) {
         for (int var7 = param2; var7 <= param3; var7++) {
            if (this.getBlock(param0, var7, param4 + 1, param5, param1).isAir()) {
               return false;
            }
         }

         return true;
      }

      protected boolean isInInvalidLocation(LevelAccessor param0, BoundingBox param1) {
         int var3 = Math.max(this.boundingBox.minX() - 1, param1.minX());
         int var4 = Math.max(this.boundingBox.minY() - 1, param1.minY());
         int var5 = Math.max(this.boundingBox.minZ() - 1, param1.minZ());
         int var6 = Math.min(this.boundingBox.maxX() + 1, param1.maxX());
         int var7 = Math.min(this.boundingBox.maxY() + 1, param1.maxY());
         int var8 = Math.min(this.boundingBox.maxZ() + 1, param1.maxZ());
         BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos((var3 + var6) / 2, (var4 + var7) / 2, (var5 + var8) / 2);
         if (param0.getBiome(var9).is(BiomeTags.MINESHAFT_BLOCKING)) {
            return true;
         }

         for (int var10 = var3; var10 <= var6; var10++) {
            for (int var11 = var5; var11 <= var8; var11++) {
               if (param0.getBlockState(var9.set(var10, var4, var11)).getMaterial().isLiquid()) {
                  return true;
               }

               if (param0.getBlockState(var9.set(var10, var7, var11)).getMaterial().isLiquid()) {
                  return true;
               }
            }
         }

         for (int var10 = var3; var10 <= var6; var10++) {
            for (int var11 = var4; var11 <= var7; var11++) {
               if (param0.getBlockState(var9.set(var10, var11, var5)).getMaterial().isLiquid()) {
                  return true;
               }

               if (param0.getBlockState(var9.set(var10, var11, var8)).getMaterial().isLiquid()) {
                  return true;
               }
            }
         }

         for (int var10 = var5; var10 <= var8; var10++) {
            for (int var11 = var4; var11 <= var7; var11++) {
               if (param0.getBlockState(var9.set(var3, var11, var10)).getMaterial().isLiquid()) {
                  return true;
               }

               if (param0.getBlockState(var9.set(var6, var11, var10)).getMaterial().isLiquid()) {
                  return true;
               }
            }
         }

         return false;
      }

      protected void setPlanksBlock(WorldGenLevel param0, BoundingBox param1, BlockState param2, int param3, int param4, int param5) {
         if (this.isInterior(param0, param3, param4, param5, param1)) {
            BlockPos var7 = this.getWorldPos(param3, param4, param5);
            BlockState var8 = param0.getBlockState(var7);
            if (!var8.isFaceSturdy(param0, var7, Direction.UP)) {
               param0.setBlock(var7, param2, 2);
            }
         }
      }
   }

   public static class MineShaftRoom extends MineshaftPieces.MineShaftPiece {
      private final List<BoundingBox> childEntranceBoxes = Lists.newLinkedList();

      public MineShaftRoom(int param0, RandomSource param1, int param2, int param3, MineshaftStructure.Type param4) {
         super(
            StructurePieceType.MINE_SHAFT_ROOM,
            param0,
            param4,
            new BoundingBox(param2, 50, param3, param2 + 7 + param1.nextInt(6), 54 + param1.nextInt(6), param3 + 7 + param1.nextInt(6))
         );
         this.type = param4;
      }

      public MineShaftRoom(CompoundTag param0) {
         super(StructurePieceType.MINE_SHAFT_ROOM, param0);
         BoundingBox.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, param0.getList("Entrances", 11))
            .resultOrPartial(MineshaftPieces.LOGGER::error)
            .ifPresent(this.childEntranceBoxes::addAll);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         int var4 = this.getGenDepth();
         int var6 = this.boundingBox.getYSpan() - 3 - 1;
         if (var6 <= 0) {
            var6 = 1;
         }

         int var5 = 0;

         while (var5 < this.boundingBox.getXSpan()) {
            var5 += param2.nextInt(this.boundingBox.getXSpan());
            if (var5 + 3 > this.boundingBox.getXSpan()) {
               break;
            }

            MineshaftPieces.MineShaftPiece var7 = MineshaftPieces.generateAndAddPiece(
               param0,
               param1,
               param2,
               this.boundingBox.minX() + var5,
               this.boundingBox.minY() + param2.nextInt(var6) + 1,
               this.boundingBox.minZ() - 1,
               Direction.NORTH,
               var4
            );
            if (var7 != null) {
               BoundingBox var8 = var7.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox(var8.minX(), var8.minY(), this.boundingBox.minZ(), var8.maxX(), var8.maxY(), this.boundingBox.minZ() + 1));
            }

            var5 += 4;
         }

         var5 = 0;

         while (var5 < this.boundingBox.getXSpan()) {
            var5 += param2.nextInt(this.boundingBox.getXSpan());
            if (var5 + 3 > this.boundingBox.getXSpan()) {
               break;
            }

            MineshaftPieces.MineShaftPiece var7 = MineshaftPieces.generateAndAddPiece(
               param0,
               param1,
               param2,
               this.boundingBox.minX() + var5,
               this.boundingBox.minY() + param2.nextInt(var6) + 1,
               this.boundingBox.maxZ() + 1,
               Direction.SOUTH,
               var4
            );
            if (var7 != null) {
               BoundingBox var8 = var7.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox(var8.minX(), var8.minY(), this.boundingBox.maxZ() - 1, var8.maxX(), var8.maxY(), this.boundingBox.maxZ()));
            }

            var5 += 4;
         }

         var5 = 0;

         while (var5 < this.boundingBox.getZSpan()) {
            var5 += param2.nextInt(this.boundingBox.getZSpan());
            if (var5 + 3 > this.boundingBox.getZSpan()) {
               break;
            }

            MineshaftPieces.MineShaftPiece var7 = MineshaftPieces.generateAndAddPiece(
               param0,
               param1,
               param2,
               this.boundingBox.minX() - 1,
               this.boundingBox.minY() + param2.nextInt(var6) + 1,
               this.boundingBox.minZ() + var5,
               Direction.WEST,
               var4
            );
            if (var7 != null) {
               BoundingBox var8 = var7.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox(this.boundingBox.minX(), var8.minY(), var8.minZ(), this.boundingBox.minX() + 1, var8.maxY(), var8.maxZ()));
            }

            var5 += 4;
         }

         var5 = 0;

         while (var5 < this.boundingBox.getZSpan()) {
            var5 += param2.nextInt(this.boundingBox.getZSpan());
            if (var5 + 3 > this.boundingBox.getZSpan()) {
               break;
            }

            StructurePiece var7 = MineshaftPieces.generateAndAddPiece(
               param0,
               param1,
               param2,
               this.boundingBox.maxX() + 1,
               this.boundingBox.minY() + param2.nextInt(var6) + 1,
               this.boundingBox.minZ() + var5,
               Direction.EAST,
               var4
            );
            if (var7 != null) {
               BoundingBox var8 = var7.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox(this.boundingBox.maxX() - 1, var8.minY(), var8.minZ(), this.boundingBox.maxX(), var8.maxY(), var8.maxZ()));
            }

            var5 += 4;
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         if (!this.isInInvalidLocation(param0, param4)) {
            this.generateBox(
               param0,
               param4,
               this.boundingBox.minX(),
               this.boundingBox.minY() + 1,
               this.boundingBox.minZ(),
               this.boundingBox.maxX(),
               Math.min(this.boundingBox.minY() + 3, this.boundingBox.maxY()),
               this.boundingBox.maxZ(),
               CAVE_AIR,
               CAVE_AIR,
               false
            );

            for (BoundingBox var9 : this.childEntranceBoxes) {
               this.generateBox(param0, param4, var9.minX(), var9.maxY() - 2, var9.minZ(), var9.maxX(), var9.maxY(), var9.maxZ(), CAVE_AIR, CAVE_AIR, false);
            }

            this.generateUpperHalfSphere(
               param0,
               param4,
               this.boundingBox.minX(),
               this.boundingBox.minY() + 4,
               this.boundingBox.minZ(),
               this.boundingBox.maxX(),
               this.boundingBox.maxY(),
               this.boundingBox.maxZ(),
               CAVE_AIR,
               false
            );
         }
      }

      @Override
      public void move(int param0, int param1, int param2) {
         super.move(param0, param1, param2);

         for (BoundingBox var5 : this.childEntranceBoxes) {
            var5.move(param0, param1, param2);
         }
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         BoundingBox.CODEC
            .listOf()
            .encodeStart(NbtOps.INSTANCE, this.childEntranceBoxes)
            .resultOrPartial(MineshaftPieces.LOGGER::error)
            .ifPresent(l_param1 -> param1.put("Entrances", l_param1));
      }
   }

   public static class MineShaftStairs extends MineshaftPieces.MineShaftPiece {
      public MineShaftStairs(int param0, BoundingBox param1, Direction param2, MineshaftStructure.Type param3) {
         super(StructurePieceType.MINE_SHAFT_STAIRS, param0, param3, param1);
         this.setOrientation(param2);
      }

      public MineShaftStairs(CompoundTag param0) {
         super(StructurePieceType.MINE_SHAFT_STAIRS, param0);
      }

      @Nullable
      public static BoundingBox findStairs(StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5) {
         BoundingBox var6 = switch (param5) {
            default -> new BoundingBox(0, -5, -8, 2, 2, 0);
            case SOUTH -> new BoundingBox(0, -5, 0, 2, 2, 8);
            case WEST -> new BoundingBox(-8, -5, 0, 0, 2, 2);
            case EAST -> new BoundingBox(0, -5, 0, 8, 2, 2);
         };
         var6.move(param2, param3, param4);
         return param0.findCollisionPiece(var6) != null ? null : var6;
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         int var4 = this.getGenDepth();
         Direction var5 = this.getOrientation();
         if (var5 != null) {
            switch (var5) {
               case NORTH:
               default:
                  MineshaftPieces.generateAndAddPiece(
                     param0, param1, param2, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, var4
                  );
                  break;
               case SOUTH:
                  MineshaftPieces.generateAndAddPiece(
                     param0, param1, param2, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, var4
                  );
                  break;
               case WEST:
                  MineshaftPieces.generateAndAddPiece(
                     param0, param1, param2, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.WEST, var4
                  );
                  break;
               case EAST:
                  MineshaftPieces.generateAndAddPiece(
                     param0, param1, param2, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.EAST, var4
                  );
            }
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         if (!this.isInInvalidLocation(param0, param4)) {
            this.generateBox(param0, param4, 0, 5, 0, 2, 7, 1, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(param0, param4, 0, 0, 7, 2, 2, 8, CAVE_AIR, CAVE_AIR, false);

            for (int var8 = 0; var8 < 5; var8++) {
               this.generateBox(param0, param4, 0, 5 - var8 - (var8 < 4 ? 1 : 0), 2 + var8, 2, 7 - var8, 2 + var8, CAVE_AIR, CAVE_AIR, false);
            }
         }
      }
   }
}
