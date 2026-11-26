package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class StrongholdPieces {
   private static final int SMALL_DOOR_WIDTH = 3;
   private static final int SMALL_DOOR_HEIGHT = 3;
   private static final int MAX_DEPTH = 50;
   private static final int LOWEST_Y_POSITION = 10;
   private static final boolean CHECK_AIR = true;
   public static final int MAGIC_START_Y = 64;
   private static final StrongholdPieces.PieceWeight[] STRONGHOLD_PIECE_WEIGHTS = new StrongholdPieces.PieceWeight[]{
      new StrongholdPieces.PieceWeight(StrongholdPieces.Straight.class, 40, 0),
      new StrongholdPieces.PieceWeight(StrongholdPieces.PrisonHall.class, 5, 5),
      new StrongholdPieces.PieceWeight(StrongholdPieces.LeftTurn.class, 20, 0),
      new StrongholdPieces.PieceWeight(StrongholdPieces.RightTurn.class, 20, 0),
      new StrongholdPieces.PieceWeight(StrongholdPieces.RoomCrossing.class, 10, 6),
      new StrongholdPieces.PieceWeight(StrongholdPieces.StraightStairsDown.class, 5, 5),
      new StrongholdPieces.PieceWeight(StrongholdPieces.StairsDown.class, 5, 5),
      new StrongholdPieces.PieceWeight(StrongholdPieces.FiveCrossing.class, 5, 4),
      new StrongholdPieces.PieceWeight(StrongholdPieces.ChestCorridor.class, 5, 4),
      new StrongholdPieces.PieceWeight(StrongholdPieces.Library.class, 10, 2) {
         @Override
         public boolean doPlace(int param0) {
            return super.doPlace(param0) && param0 > 4;
         }
      },
      new StrongholdPieces.PieceWeight(StrongholdPieces.PortalRoom.class, 20, 1) {
         @Override
         public boolean doPlace(int param0) {
            return super.doPlace(param0) && param0 > 5;
         }
      }
   };
   private static List<StrongholdPieces.PieceWeight> currentPieces;
   static Class<? extends StrongholdPieces.StrongholdPiece> imposedPiece;
   private static int totalWeight;
   static final StrongholdPieces.SmoothStoneSelector SMOOTH_STONE_SELECTOR = new StrongholdPieces.SmoothStoneSelector();

   public static void resetPieces() {
      currentPieces = Lists.newArrayList();

      for (StrongholdPieces.PieceWeight var3 : STRONGHOLD_PIECE_WEIGHTS) {
         var3.placeCount = 0;
         currentPieces.add(var3);
      }

      imposedPiece = null;
   }

   private static boolean updatePieceWeight() {
      boolean var0 = false;
      totalWeight = 0;

      for (StrongholdPieces.PieceWeight var2 : currentPieces) {
         if (var2.maxPlaceCount > 0 && var2.placeCount < var2.maxPlaceCount) {
            var0 = true;
         }

         totalWeight = totalWeight + var2.weight;
      }

      return var0;
   }

   private static StrongholdPieces.StrongholdPiece findAndCreatePieceFactory(
      Class<? extends StrongholdPieces.StrongholdPiece> param0,
      StructurePieceAccessor param1,
      RandomSource param2,
      int param3,
      int param4,
      int param5,
      @Nullable Direction param6,
      int param7
   ) {
      StrongholdPieces.StrongholdPiece var8 = null;
      if (param0 == StrongholdPieces.Straight.class) {
         var8 = StrongholdPieces.Straight.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.PrisonHall.class) {
         var8 = StrongholdPieces.PrisonHall.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.LeftTurn.class) {
         var8 = StrongholdPieces.LeftTurn.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.RightTurn.class) {
         var8 = StrongholdPieces.RightTurn.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.RoomCrossing.class) {
         var8 = StrongholdPieces.RoomCrossing.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.StraightStairsDown.class) {
         var8 = StrongholdPieces.StraightStairsDown.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.StairsDown.class) {
         var8 = StrongholdPieces.StairsDown.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.FiveCrossing.class) {
         var8 = StrongholdPieces.FiveCrossing.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.ChestCorridor.class) {
         var8 = StrongholdPieces.ChestCorridor.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.Library.class) {
         var8 = StrongholdPieces.Library.createPiece(param1, param2, param3, param4, param5, param6, param7);
      } else if (param0 == StrongholdPieces.PortalRoom.class) {
         var8 = StrongholdPieces.PortalRoom.createPiece(param1, param3, param4, param5, param6, param7);
      }

      return var8;
   }

   private static StrongholdPieces.StrongholdPiece generatePieceFromSmallDoor(
      StrongholdPieces.StartPiece param0, StructurePieceAccessor param1, RandomSource param2, int param3, int param4, int param5, Direction param6, int param7
   ) {
      if (!updatePieceWeight()) {
         return null;
      }

      if (imposedPiece != null) {
         StrongholdPieces.StrongholdPiece var8 = findAndCreatePieceFactory(imposedPiece, param1, param2, param3, param4, param5, param6, param7);
         imposedPiece = null;
         if (var8 != null) {
            return var8;
         }
      }

      int var8 = 0;

      while (var8 < 5) {
         var8++;
         int var9 = param2.nextInt(totalWeight);

         for (StrongholdPieces.PieceWeight var11 : currentPieces) {
            var9 -= var11.weight;
            if (var9 < 0) {
               if (!var11.doPlace(param7) || var11 == param0.previousPiece) {
                  break;
               }

               StrongholdPieces.StrongholdPiece var12 = findAndCreatePieceFactory(var11.pieceClass, param1, param2, param3, param4, param5, param6, param7);
               if (var12 != null) {
                  var11.placeCount++;
                  param0.previousPiece = var11;
                  if (!var11.isValid()) {
                     currentPieces.remove(var11);
                  }

                  return var12;
               }
            }
         }
      }

      BoundingBox var9 = StrongholdPieces.FillerCorridor.findPieceBox(param1, param2, param3, param4, param5, param6);
      return var9 != null && var9.minY() > 1 ? new StrongholdPieces.FillerCorridor(param7, var9, param6) : null;
   }

   static StructurePiece generateAndAddPiece(
      StrongholdPieces.StartPiece param0,
      StructurePieceAccessor param1,
      RandomSource param2,
      int param3,
      int param4,
      int param5,
      @Nullable Direction param6,
      int param7
   ) {
      if (param7 > 50) {
         return null;
      }

      if (Math.abs(param3 - param0.getBoundingBox().minX()) <= 112 && Math.abs(param5 - param0.getBoundingBox().minZ()) <= 112) {
         StructurePiece var8 = generatePieceFromSmallDoor(param0, param1, param2, param3, param4, param5, param6, param7 + 1);
         if (var8 != null) {
            param1.addPiece(var8);
            param0.pendingChildren.add(var8);
         }

         return var8;
      } else {
         return null;
      }
   }

   public static class ChestCorridor extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 5;
      private static final int DEPTH = 7;
      private boolean hasPlacedChest;

      public ChestCorridor(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, param0, param2);
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
      }

      public ChestCorridor(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, param0);
         this.hasPlacedChest = param0.getBoolean("Chest");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putBoolean("Chest", this.hasPlacedChest);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)param0, param1, param2, 1, 1);
      }

      public static StrongholdPieces.ChestCorridor createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -1, -1, 0, 5, 5, 7, param5);
         return isOkBox(var7) && param0.findCollisionPiece(var7) == null ? new StrongholdPieces.ChestCorridor(param6, param1, var7, param5) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 4, 4, 6, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 1, 1, 0);
         this.generateSmallDoor(param0, param3, param4, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
         this.generateBox(param0, param4, 3, 1, 2, 3, 1, 4, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), false);
         this.placeBlock(param0, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 1, param4);
         this.placeBlock(param0, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 5, param4);
         this.placeBlock(param0, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 2, param4);
         this.placeBlock(param0, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 4, param4);

         for (int var8 = 2; var8 <= 4; var8++) {
            this.placeBlock(param0, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2, 1, var8, param4);
         }

         if (!this.hasPlacedChest && param4.isInside(this.getWorldPos(3, 2, 3))) {
            this.hasPlacedChest = true;
            this.createChest(param0, param4, param3, 3, 2, 3, BuiltInLootTables.STRONGHOLD_CORRIDOR);
         }
      }
   }

   public static class FillerCorridor extends StrongholdPieces.StrongholdPiece {
      private final int steps;

      public FillerCorridor(int param0, BoundingBox param1, Direction param2) {
         super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, param0, param1);
         this.setOrientation(param2);
         this.steps = param2 != Direction.NORTH && param2 != Direction.SOUTH ? param1.getXSpan() : param1.getZSpan();
      }

      public FillerCorridor(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, param0);
         this.steps = param0.getInt("Steps");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putInt("Steps", this.steps);
      }

      public static BoundingBox findPieceBox(StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5) {
         int var6 = 3;
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -1, -1, 0, 5, 5, 4, param5);
         StructurePiece var8 = param0.findCollisionPiece(var7);
         if (var8 == null) {
            return null;
         }

         if (var8.getBoundingBox().minY() == var7.minY()) {
            for (int var9 = 2; var9 >= 1; var9--) {
               var7 = BoundingBox.orientBox(param2, param3, param4, -1, -1, 0, 5, 5, var9, param5);
               if (!var8.getBoundingBox().intersects(var7)) {
                  return BoundingBox.orientBox(param2, param3, param4, -1, -1, 0, 5, 5, var9 + 1, param5);
               }
            }
         }

         return null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         for (int var8 = 0; var8 < this.steps; var8++) {
            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 0, 0, var8, param4);
            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 1, 0, var8, param4);
            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 2, 0, var8, param4);
            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 3, 0, var8, param4);
            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 4, 0, var8, param4);

            for (int var9 = 1; var9 <= 3; var9++) {
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 0, var9, var8, param4);
               this.placeBlock(param0, Blocks.CAVE_AIR.defaultBlockState(), 1, var9, var8, param4);
               this.placeBlock(param0, Blocks.CAVE_AIR.defaultBlockState(), 2, var9, var8, param4);
               this.placeBlock(param0, Blocks.CAVE_AIR.defaultBlockState(), 3, var9, var8, param4);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 4, var9, var8, param4);
            }

            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 0, 4, var8, param4);
            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, var8, param4);
            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, var8, param4);
            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 3, 4, var8, param4);
            this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 4, 4, var8, param4);
         }
      }
   }

   public static class FiveCrossing extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 10;
      protected static final int HEIGHT = 9;
      protected static final int DEPTH = 11;
      private final boolean leftLow;
      private final boolean leftHigh;
      private final boolean rightLow;
      private final boolean rightHigh;

      public FiveCrossing(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, param0, param2);
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
         this.leftLow = param1.nextBoolean();
         this.leftHigh = param1.nextBoolean();
         this.rightLow = param1.nextBoolean();
         this.rightHigh = param1.nextInt(3) > 0;
      }

      public FiveCrossing(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, param0);
         this.leftLow = param0.getBoolean("leftLow");
         this.leftHigh = param0.getBoolean("leftHigh");
         this.rightLow = param0.getBoolean("rightLow");
         this.rightHigh = param0.getBoolean("rightHigh");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putBoolean("leftLow", this.leftLow);
         param1.putBoolean("leftHigh", this.leftHigh);
         param1.putBoolean("rightLow", this.rightLow);
         param1.putBoolean("rightHigh", this.rightHigh);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         int var4 = 3;
         int var5 = 5;
         Direction var6 = this.getOrientation();
         if (var6 == Direction.WEST || var6 == Direction.NORTH) {
            var4 = 8 - var4;
            var5 = 8 - var5;
         }

         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)param0, param1, param2, 5, 1);
         if (this.leftLow) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)param0, param1, param2, var4, 1);
         }

         if (this.leftHigh) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)param0, param1, param2, var5, 7);
         }

         if (this.rightLow) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)param0, param1, param2, var4, 1);
         }

         if (this.rightHigh) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)param0, param1, param2, var5, 7);
         }
      }

      public static StrongholdPieces.FiveCrossing createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -4, -3, 0, 10, 9, 11, param5);
         return isOkBox(var7) && param0.findCollisionPiece(var7) == null ? new StrongholdPieces.FiveCrossing(param6, param1, var7, param5) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 9, 8, 10, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 4, 3, 0);
         if (this.leftLow) {
            this.generateBox(param0, param4, 0, 3, 1, 0, 5, 3, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightLow) {
            this.generateBox(param0, param4, 9, 3, 1, 9, 5, 3, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.leftHigh) {
            this.generateBox(param0, param4, 0, 5, 7, 0, 7, 9, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightHigh) {
            this.generateBox(param0, param4, 9, 5, 7, 9, 7, 9, CAVE_AIR, CAVE_AIR, false);
         }

         this.generateBox(param0, param4, 5, 1, 10, 7, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(param0, param4, 1, 2, 1, 8, 2, 6, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 1, 5, 4, 4, 9, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 8, 1, 5, 8, 4, 9, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 1, 4, 7, 3, 4, 9, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 1, 3, 5, 3, 3, 6, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 1, 3, 4, 3, 3, 4, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(param0, param4, 1, 4, 6, 3, 4, 6, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(param0, param4, 5, 1, 7, 7, 1, 8, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 5, 1, 9, 7, 1, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(param0, param4, 5, 2, 7, 7, 2, 7, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(param0, param4, 4, 5, 7, 4, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(param0, param4, 8, 5, 7, 8, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(
            param0,
            param4,
            5,
            5,
            7,
            7,
            5,
            9,
            Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
            Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
            false
         );
         this.placeBlock(param0, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 6, 5, 6, param4);
      }
   }

   public static class LeftTurn extends StrongholdPieces.Turn {
      public LeftTurn(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_LEFT_TURN, param0, param2);
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
      }

      public LeftTurn(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_LEFT_TURN, param0);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         Direction var4 = this.getOrientation();
         if (var4 != Direction.NORTH && var4 != Direction.EAST) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)param0, param1, param2, 1, 1);
         } else {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)param0, param1, param2, 1, 1);
         }
      }

      public static StrongholdPieces.LeftTurn createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -1, -1, 0, 5, 5, 5, param5);
         return isOkBox(var7) && param0.findCollisionPiece(var7) == null ? new StrongholdPieces.LeftTurn(param6, param1, var7, param5) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 4, 4, 4, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 1, 1, 0);
         Direction var8 = this.getOrientation();
         if (var8 != Direction.NORTH && var8 != Direction.EAST) {
            this.generateBox(param0, param4, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
         } else {
            this.generateBox(param0, param4, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class Library extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 14;
      protected static final int HEIGHT = 6;
      protected static final int TALL_HEIGHT = 11;
      protected static final int DEPTH = 15;
      private final boolean isTall;

      public Library(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_LIBRARY, param0, param2);
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
         this.isTall = param2.getYSpan() > 6;
      }

      public Library(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_LIBRARY, param0);
         this.isTall = param0.getBoolean("Tall");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putBoolean("Tall", this.isTall);
      }

      public static StrongholdPieces.Library createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -4, -1, 0, 14, 11, 15, param5);
         if (!isOkBox(var7) || param0.findCollisionPiece(var7) != null) {
            var7 = BoundingBox.orientBox(param2, param3, param4, -4, -1, 0, 14, 6, 15, param5);
            if (!isOkBox(var7) || param0.findCollisionPiece(var7) != null) {
               return null;
            }
         }

         return new StrongholdPieces.Library(param6, param1, var7, param5);
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         int var8 = 11;
         if (!this.isTall) {
            var8 = 6;
         }

         this.generateBox(param0, param4, 0, 0, 0, 13, var8 - 1, 14, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 4, 1, 0);
         this.generateMaybeBox(
            param0, param4, param3, 0.07F, 2, 1, 1, 11, 4, 13, Blocks.COBWEB.defaultBlockState(), Blocks.COBWEB.defaultBlockState(), false, false
         );
         int var9 = 1;
         int var10 = 12;

         for (int var11 = 1; var11 <= 13; var11++) {
            if ((var11 - 1) % 4 == 0) {
               this.generateBox(param0, param4, 1, 1, var11, 1, 4, var11, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               this.generateBox(param0, param4, 12, 1, var11, 12, 4, var11, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               this.placeBlock(param0, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 2, 3, var11, param4);
               this.placeBlock(param0, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 11, 3, var11, param4);
               if (this.isTall) {
                  this.generateBox(
                     param0, param4, 1, 6, var11, 1, 9, var11, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false
                  );
                  this.generateBox(
                     param0, param4, 12, 6, var11, 12, 9, var11, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false
                  );
               }
            } else {
               this.generateBox(param0, param4, 1, 1, var11, 1, 4, var11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               this.generateBox(param0, param4, 12, 1, var11, 12, 4, var11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               if (this.isTall) {
                  this.generateBox(param0, param4, 1, 6, var11, 1, 9, var11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                  this.generateBox(
                     param0, param4, 12, 6, var11, 12, 9, var11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false
                  );
               }
            }
         }

         for (int var11 = 3; var11 < 12; var11 += 2) {
            this.generateBox(param0, param4, 3, 1, var11, 4, 3, var11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            this.generateBox(param0, param4, 6, 1, var11, 7, 3, var11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            this.generateBox(param0, param4, 9, 1, var11, 10, 3, var11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
         }

         if (this.isTall) {
            this.generateBox(param0, param4, 1, 5, 1, 3, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(param0, param4, 10, 5, 1, 12, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(param0, param4, 4, 5, 1, 9, 5, 2, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(param0, param4, 4, 5, 12, 9, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 11, param4);
            this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 8, 5, 11, param4);
            this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 10, param4);
            BlockState var11 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState var12 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            this.generateBox(param0, param4, 3, 6, 3, 3, 6, 11, var12, var12, false);
            this.generateBox(param0, param4, 10, 6, 3, 10, 6, 9, var12, var12, false);
            this.generateBox(param0, param4, 4, 6, 2, 9, 6, 2, var11, var11, false);
            this.generateBox(param0, param4, 4, 6, 12, 7, 6, 12, var11, var11, false);
            this.placeBlock(param0, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 3, 6, 2, param4);
            this.placeBlock(param0, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 3, 6, 12, param4);
            this.placeBlock(param0, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 10, 6, 2, param4);

            for (int var13 = 0; var13 <= 2; var13++) {
               this.placeBlock(
                  param0,
                  Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true),
                  8 + var13,
                  6,
                  12 - var13,
                  param4
               );
               if (var13 != 2) {
                  this.placeBlock(
                     param0,
                     Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true),
                     8 + var13,
                     6,
                     11 - var13,
                     param4
                  );
               }
            }

            BlockState var13 = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            this.placeBlock(param0, var13, 10, 1, 13, param4);
            this.placeBlock(param0, var13, 10, 2, 13, param4);
            this.placeBlock(param0, var13, 10, 3, 13, param4);
            this.placeBlock(param0, var13, 10, 4, 13, param4);
            this.placeBlock(param0, var13, 10, 5, 13, param4);
            this.placeBlock(param0, var13, 10, 6, 13, param4);
            this.placeBlock(param0, var13, 10, 7, 13, param4);
            int var14 = 7;
            int var15 = 7;
            BlockState var16 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true);
            this.placeBlock(param0, var16, 6, 9, 7, param4);
            BlockState var17 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true);
            this.placeBlock(param0, var17, 7, 9, 7, param4);
            this.placeBlock(param0, var16, 6, 8, 7, param4);
            this.placeBlock(param0, var17, 7, 8, 7, param4);
            BlockState var18 = var12.setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            this.placeBlock(param0, var18, 6, 7, 7, param4);
            this.placeBlock(param0, var18, 7, 7, 7, param4);
            this.placeBlock(param0, var16, 5, 7, 7, param4);
            this.placeBlock(param0, var17, 8, 7, 7, param4);
            this.placeBlock(param0, var16.setValue(FenceBlock.NORTH, true), 6, 7, 6, param4);
            this.placeBlock(param0, var16.setValue(FenceBlock.SOUTH, true), 6, 7, 8, param4);
            this.placeBlock(param0, var17.setValue(FenceBlock.NORTH, true), 7, 7, 6, param4);
            this.placeBlock(param0, var17.setValue(FenceBlock.SOUTH, true), 7, 7, 8, param4);
            BlockState var19 = Blocks.TORCH.defaultBlockState();
            this.placeBlock(param0, var19, 5, 8, 7, param4);
            this.placeBlock(param0, var19, 8, 8, 7, param4);
            this.placeBlock(param0, var19, 6, 8, 6, param4);
            this.placeBlock(param0, var19, 6, 8, 8, param4);
            this.placeBlock(param0, var19, 7, 8, 6, param4);
            this.placeBlock(param0, var19, 7, 8, 8, param4);
         }

         this.createChest(param0, param4, param3, 3, 3, 5, BuiltInLootTables.STRONGHOLD_LIBRARY);
         if (this.isTall) {
            this.placeBlock(param0, CAVE_AIR, 12, 9, 1, param4);
            this.createChest(param0, param4, param3, 12, 8, 1, BuiltInLootTables.STRONGHOLD_LIBRARY);
         }
      }
   }

   static class PieceWeight {
      public final Class<? extends StrongholdPieces.StrongholdPiece> pieceClass;
      public final int weight;
      public int placeCount;
      public final int maxPlaceCount;

      public PieceWeight(Class<? extends StrongholdPieces.StrongholdPiece> param0, int param1, int param2) {
         this.pieceClass = param0;
         this.weight = param1;
         this.maxPlaceCount = param2;
      }

      public boolean doPlace(int param0) {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }

      public boolean isValid() {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }
   }

   public static class PortalRoom extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 11;
      protected static final int HEIGHT = 8;
      protected static final int DEPTH = 16;
      private boolean hasPlacedSpawner;

      public PortalRoom(int param0, BoundingBox param1, Direction param2) {
         super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, param0, param1);
         this.setOrientation(param2);
      }

      public PortalRoom(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, param0);
         this.hasPlacedSpawner = param0.getBoolean("Mob");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putBoolean("Mob", this.hasPlacedSpawner);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         if (param0 != null) {
            ((StrongholdPieces.StartPiece)param0).portalRoomPiece = this;
         }
      }

      public static StrongholdPieces.PortalRoom createPiece(StructurePieceAccessor param0, int param1, int param2, int param3, Direction param4, int param5) {
         BoundingBox var6 = BoundingBox.orientBox(param1, param2, param3, -4, -1, 0, 11, 8, 16, param4);
         return isOkBox(var6) && param0.findCollisionPiece(var6) == null ? new StrongholdPieces.PortalRoom(param5, var6, param4) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 10, 7, 15, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES, 4, 1, 0);
         int var8 = 6;
         this.generateBox(param0, param4, 1, 6, 1, 1, 6, 14, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 9, 6, 1, 9, 6, 14, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 2, 6, 1, 8, 6, 2, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 2, 6, 14, 8, 6, 14, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 1, 1, 1, 2, 1, 4, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 8, 1, 1, 9, 1, 4, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 1, 1, 1, 1, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         this.generateBox(param0, param4, 9, 1, 1, 9, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         this.generateBox(param0, param4, 3, 1, 8, 7, 1, 12, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 1, 9, 6, 1, 11, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         BlockState var9 = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
         BlockState var10 = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true);

         for (int var11 = 3; var11 < 14; var11 += 2) {
            this.generateBox(param0, param4, 0, 3, var11, 0, 4, var11, var9, var9, false);
            this.generateBox(param0, param4, 10, 3, var11, 10, 4, var11, var9, var9, false);
         }

         for (int var11 = 2; var11 < 9; var11 += 2) {
            this.generateBox(param0, param4, var11, 3, 15, var11, 4, 15, var10, var10, false);
         }

         BlockState var11 = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         this.generateBox(param0, param4, 4, 1, 5, 6, 1, 7, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 2, 6, 6, 2, 7, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 3, 7, 6, 3, 7, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);

         for (int var12 = 4; var12 <= 6; var12++) {
            this.placeBlock(param0, var11, var12, 1, 4, param4);
            this.placeBlock(param0, var11, var12, 2, 5, param4);
            this.placeBlock(param0, var11, var12, 3, 6, param4);
         }

         BlockState var12 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.NORTH);
         BlockState var13 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.SOUTH);
         BlockState var14 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.EAST);
         BlockState var15 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.WEST);
         boolean var16 = true;
         boolean[] var17 = new boolean[12];

         for (int var18 = 0; var18 < var17.length; var18++) {
            var17[var18] = param3.nextFloat() > 0.9F;
            var16 &= var17[var18];
         }

         this.placeBlock(param0, var12.setValue(EndPortalFrameBlock.HAS_EYE, var17[0]), 4, 3, 8, param4);
         this.placeBlock(param0, var12.setValue(EndPortalFrameBlock.HAS_EYE, var17[1]), 5, 3, 8, param4);
         this.placeBlock(param0, var12.setValue(EndPortalFrameBlock.HAS_EYE, var17[2]), 6, 3, 8, param4);
         this.placeBlock(param0, var13.setValue(EndPortalFrameBlock.HAS_EYE, var17[3]), 4, 3, 12, param4);
         this.placeBlock(param0, var13.setValue(EndPortalFrameBlock.HAS_EYE, var17[4]), 5, 3, 12, param4);
         this.placeBlock(param0, var13.setValue(EndPortalFrameBlock.HAS_EYE, var17[5]), 6, 3, 12, param4);
         this.placeBlock(param0, var14.setValue(EndPortalFrameBlock.HAS_EYE, var17[6]), 3, 3, 9, param4);
         this.placeBlock(param0, var14.setValue(EndPortalFrameBlock.HAS_EYE, var17[7]), 3, 3, 10, param4);
         this.placeBlock(param0, var14.setValue(EndPortalFrameBlock.HAS_EYE, var17[8]), 3, 3, 11, param4);
         this.placeBlock(param0, var15.setValue(EndPortalFrameBlock.HAS_EYE, var17[9]), 7, 3, 9, param4);
         this.placeBlock(param0, var15.setValue(EndPortalFrameBlock.HAS_EYE, var17[10]), 7, 3, 10, param4);
         this.placeBlock(param0, var15.setValue(EndPortalFrameBlock.HAS_EYE, var17[11]), 7, 3, 11, param4);
         if (var16) {
            BlockState var18 = Blocks.END_PORTAL.defaultBlockState();
            this.placeBlock(param0, var18, 4, 3, 9, param4);
            this.placeBlock(param0, var18, 5, 3, 9, param4);
            this.placeBlock(param0, var18, 6, 3, 9, param4);
            this.placeBlock(param0, var18, 4, 3, 10, param4);
            this.placeBlock(param0, var18, 5, 3, 10, param4);
            this.placeBlock(param0, var18, 6, 3, 10, param4);
            this.placeBlock(param0, var18, 4, 3, 11, param4);
            this.placeBlock(param0, var18, 5, 3, 11, param4);
            this.placeBlock(param0, var18, 6, 3, 11, param4);
         }

         if (!this.hasPlacedSpawner) {
            BlockPos var18 = this.getWorldPos(5, 3, 6);
            if (param4.isInside(var18)) {
               this.hasPlacedSpawner = true;
               param0.setBlock(var18, Blocks.SPAWNER.defaultBlockState(), 2);
               if (param0.getBlockEntity(var18) instanceof SpawnerBlockEntity var20) {
                  var20.setEntityId(EntityType.SILVERFISH, param3);
               }
            }
         }
      }
   }

   public static class PrisonHall extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 9;
      protected static final int HEIGHT = 5;
      protected static final int DEPTH = 11;

      public PrisonHall(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_PRISON_HALL, param0, param2);
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
      }

      public PrisonHall(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_PRISON_HALL, param0);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)param0, param1, param2, 1, 1);
      }

      public static StrongholdPieces.PrisonHall createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -1, -1, 0, 9, 5, 11, param5);
         return isOkBox(var7) && param0.findCollisionPiece(var7) == null ? new StrongholdPieces.PrisonHall(param6, param1, var7, param5) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 8, 4, 10, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 1, 1, 0);
         this.generateBox(param0, param4, 1, 1, 10, 3, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(param0, param4, 4, 1, 1, 4, 3, 1, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 1, 3, 4, 3, 3, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 1, 7, 4, 3, 7, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 1, 9, 4, 3, 9, false, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);

         for (int var8 = 1; var8 <= 3; var8++) {
            this.placeBlock(
               param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, var8, 4, param4
            );
            this.placeBlock(
               param0,
               Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true).setValue(IronBarsBlock.EAST, true),
               4,
               var8,
               5,
               param4
            );
            this.placeBlock(
               param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, var8, 6, param4
            );
            this.placeBlock(
               param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 5, var8, 5, param4
            );
            this.placeBlock(
               param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 6, var8, 5, param4
            );
            this.placeBlock(
               param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 7, var8, 5, param4
            );
         }

         this.placeBlock(param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, 3, 2, param4);
         this.placeBlock(param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, 3, 8, param4);
         BlockState var8 = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST);
         BlockState var9 = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
         this.placeBlock(param0, var8, 4, 1, 2, param4);
         this.placeBlock(param0, var9, 4, 2, 2, param4);
         this.placeBlock(param0, var8, 4, 1, 8, param4);
         this.placeBlock(param0, var9, 4, 2, 8, param4);
      }
   }

   public static class RightTurn extends StrongholdPieces.Turn {
      public RightTurn(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_RIGHT_TURN, param0, param2);
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
      }

      public RightTurn(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_RIGHT_TURN, param0);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         Direction var4 = this.getOrientation();
         if (var4 != Direction.NORTH && var4 != Direction.EAST) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)param0, param1, param2, 1, 1);
         } else {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)param0, param1, param2, 1, 1);
         }
      }

      public static StrongholdPieces.RightTurn createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -1, -1, 0, 5, 5, 5, param5);
         return isOkBox(var7) && param0.findCollisionPiece(var7) == null ? new StrongholdPieces.RightTurn(param6, param1, var7, param5) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 4, 4, 4, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 1, 1, 0);
         Direction var8 = this.getOrientation();
         if (var8 != Direction.NORTH && var8 != Direction.EAST) {
            this.generateBox(param0, param4, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
         } else {
            this.generateBox(param0, param4, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class RoomCrossing extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 11;
      protected static final int HEIGHT = 7;
      protected static final int DEPTH = 11;
      protected final int type;

      public RoomCrossing(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, param0, param2);
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
         this.type = param1.nextInt(5);
      }

      public RoomCrossing(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, param0);
         this.type = param0.getInt("Type");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putInt("Type", this.type);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)param0, param1, param2, 4, 1);
         this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)param0, param1, param2, 1, 4);
         this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)param0, param1, param2, 1, 4);
      }

      public static StrongholdPieces.RoomCrossing createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -4, -1, 0, 11, 7, 11, param5);
         return isOkBox(var7) && param0.findCollisionPiece(var7) == null ? new StrongholdPieces.RoomCrossing(param6, param1, var7, param5) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 10, 6, 10, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 4, 1, 0);
         this.generateBox(param0, param4, 4, 1, 10, 6, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(param0, param4, 0, 1, 4, 0, 3, 6, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(param0, param4, 10, 1, 4, 10, 3, 6, CAVE_AIR, CAVE_AIR, false);
         switch (this.type) {
            case 0:
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, param4);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, param4);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, param4);
               this.placeBlock(param0, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 4, 3, 5, param4);
               this.placeBlock(param0, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 6, 3, 5, param4);
               this.placeBlock(param0, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 5, 3, 4, param4);
               this.placeBlock(param0, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH), 5, 3, 6, param4);
               this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 4, param4);
               this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 5, param4);
               this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 6, param4);
               this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 4, param4);
               this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 5, param4);
               this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 6, param4);
               this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 4, param4);
               this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 6, param4);
               break;
            case 1:
               for (int var8 = 0; var8 < 5; var8++) {
                  this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 3, 1, 3 + var8, param4);
                  this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 7, 1, 3 + var8, param4);
                  this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 3 + var8, 1, 3, param4);
                  this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 3 + var8, 1, 7, param4);
               }

               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, param4);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, param4);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, param4);
               this.placeBlock(param0, Blocks.WATER.defaultBlockState(), 5, 4, 5, param4);
               break;
            case 2:
               for (int var8 = 1; var8 <= 9; var8++) {
                  this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 1, 3, var8, param4);
                  this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 9, 3, var8, param4);
               }

               for (int var8 = 1; var8 <= 9; var8++) {
                  this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), var8, 3, 1, param4);
                  this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), var8, 3, 9, param4);
               }

               this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 4, param4);
               this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 6, param4);
               this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 4, param4);
               this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 6, param4);
               this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 4, 1, 5, param4);
               this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 6, 1, 5, param4);
               this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 4, 3, 5, param4);
               this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 6, 3, 5, param4);

               for (int var8 = 1; var8 <= 3; var8++) {
                  this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 4, var8, 4, param4);
                  this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 6, var8, 4, param4);
                  this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 4, var8, 6, param4);
                  this.placeBlock(param0, Blocks.COBBLESTONE.defaultBlockState(), 6, var8, 6, param4);
               }

               this.placeBlock(param0, Blocks.TORCH.defaultBlockState(), 5, 3, 5, param4);

               for (int var8 = 2; var8 <= 8; var8++) {
                  this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 2, 3, var8, param4);
                  this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 3, 3, var8, param4);
                  if (var8 <= 3 || var8 >= 7) {
                     this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 4, 3, var8, param4);
                     this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 5, 3, var8, param4);
                     this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 6, 3, var8, param4);
                  }

                  this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 7, 3, var8, param4);
                  this.placeBlock(param0, Blocks.OAK_PLANKS.defaultBlockState(), 8, 3, var8, param4);
               }

               BlockState var8 = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
               this.placeBlock(param0, var8, 9, 1, 3, param4);
               this.placeBlock(param0, var8, 9, 2, 3, param4);
               this.placeBlock(param0, var8, 9, 3, 3, param4);
               this.createChest(param0, param4, param3, 3, 4, 8, BuiltInLootTables.STRONGHOLD_CROSSING);
         }
      }
   }

   static class SmoothStoneSelector extends StructurePiece.BlockSelector {
      @Override
      public void next(RandomSource param0, int param1, int param2, int param3, boolean param4) {
         if (param4) {
            float var6 = param0.nextFloat();
            if (var6 < 0.2F) {
               this.next = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            } else if (var6 < 0.5F) {
               this.next = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            } else if (var6 < 0.55F) {
               this.next = Blocks.INFESTED_STONE_BRICKS.defaultBlockState();
            } else {
               this.next = Blocks.STONE_BRICKS.defaultBlockState();
            }
         } else {
            this.next = Blocks.CAVE_AIR.defaultBlockState();
         }
      }
   }

   public static class StairsDown extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 5;
      private final boolean isSource;

      public StairsDown(StructurePieceType param0, int param1, int param2, int param3, Direction param4) {
         super(param0, param1, makeBoundingBox(param2, 64, param3, param4, 5, 11, 5));
         this.isSource = true;
         this.setOrientation(param4);
         this.entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
      }

      public StairsDown(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_STAIRS_DOWN, param0, param2);
         this.isSource = false;
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
      }

      public StairsDown(StructurePieceType param0, CompoundTag param1) {
         super(param0, param1);
         this.isSource = param1.getBoolean("Source");
      }

      public StairsDown(CompoundTag param0) {
         this(StructurePieceType.STRONGHOLD_STAIRS_DOWN, param0);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putBoolean("Source", this.isSource);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         if (this.isSource) {
            StrongholdPieces.imposedPiece = StrongholdPieces.FiveCrossing.class;
         }

         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)param0, param1, param2, 1, 1);
      }

      public static StrongholdPieces.StairsDown createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -1, -7, 0, 5, 11, 5, param5);
         return isOkBox(var7) && param0.findCollisionPiece(var7) == null ? new StrongholdPieces.StairsDown(param6, param1, var7, param5) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 4, 10, 4, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 1, 7, 0);
         this.generateSmallDoor(param0, param3, param4, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 2, 6, 1, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 1, param4);
         this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 6, 1, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 2, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, 3, param4);
         this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 5, 3, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, 3, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 3, param4);
         this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 4, 3, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 2, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 3, 2, 1, param4);
         this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 3, 1, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 2, 2, 1, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 1, param4);
         this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 2, 1, param4);
         this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 2, param4);
         this.placeBlock(param0, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 1, 3, param4);
      }
   }

   public static class StartPiece extends StrongholdPieces.StairsDown {
      public StrongholdPieces.PieceWeight previousPiece;
      @Nullable
      public StrongholdPieces.PortalRoom portalRoomPiece;
      public final List<StructurePiece> pendingChildren = Lists.newArrayList();

      public StartPiece(RandomSource param0, int param1, int param2) {
         super(StructurePieceType.STRONGHOLD_START, 0, param1, param2, getRandomHorizontalDirection(param0));
      }

      public StartPiece(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_START, param0);
      }

      @Override
      public BlockPos getLocatorPosition() {
         return this.portalRoomPiece != null ? this.portalRoomPiece.getLocatorPosition() : super.getLocatorPosition();
      }
   }

   public static class Straight extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 5;
      private static final int DEPTH = 7;
      private final boolean leftChild;
      private final boolean rightChild;

      public Straight(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT, param0, param2);
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
         this.leftChild = param1.nextInt(2) == 0;
         this.rightChild = param1.nextInt(2) == 0;
      }

      public Straight(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT, param0);
         this.leftChild = param0.getBoolean("Left");
         this.rightChild = param0.getBoolean("Right");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putBoolean("Left", this.leftChild);
         param1.putBoolean("Right", this.rightChild);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)param0, param1, param2, 1, 1);
         if (this.leftChild) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)param0, param1, param2, 1, 2);
         }

         if (this.rightChild) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)param0, param1, param2, 1, 2);
         }
      }

      public static StrongholdPieces.Straight createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -1, -1, 0, 5, 5, 7, param5);
         return isOkBox(var7) && param0.findCollisionPiece(var7) == null ? new StrongholdPieces.Straight(param6, param1, var7, param5) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 4, 4, 6, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 1, 1, 0);
         this.generateSmallDoor(param0, param3, param4, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
         BlockState var8 = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST);
         BlockState var9 = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST);
         this.maybeGenerateBlock(param0, param4, param3, 0.1F, 1, 2, 1, var8);
         this.maybeGenerateBlock(param0, param4, param3, 0.1F, 3, 2, 1, var9);
         this.maybeGenerateBlock(param0, param4, param3, 0.1F, 1, 2, 5, var8);
         this.maybeGenerateBlock(param0, param4, param3, 0.1F, 3, 2, 5, var9);
         if (this.leftChild) {
            this.generateBox(param0, param4, 0, 1, 2, 0, 3, 4, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightChild) {
            this.generateBox(param0, param4, 4, 1, 2, 4, 3, 4, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class StraightStairsDown extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 8;

      public StraightStairsDown(int param0, RandomSource param1, BoundingBox param2, Direction param3) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, param0, param2);
         this.setOrientation(param3);
         this.entryDoor = this.randomSmallDoor(param1);
      }

      public StraightStairsDown(CompoundTag param0) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, param0);
      }

      @Override
      public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)param0, param1, param2, 1, 1);
      }

      public static StrongholdPieces.StraightStairsDown createPiece(
         StructurePieceAccessor param0, RandomSource param1, int param2, int param3, int param4, Direction param5, int param6
      ) {
         BoundingBox var7 = BoundingBox.orientBox(param2, param3, param4, -1, -7, 0, 5, 11, 8, param5);
         return isOkBox(var7) && param0.findCollisionPiece(var7) == null ? new StrongholdPieces.StraightStairsDown(param6, param1, var7, param5) : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
      ) {
         this.generateBox(param0, param4, 0, 0, 0, 4, 10, 7, true, param3, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(param0, param3, param4, this.entryDoor, 1, 7, 0);
         this.generateSmallDoor(param0, param3, param4, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 7);
         BlockState var8 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);

         for (int var9 = 0; var9 < 6; var9++) {
            this.placeBlock(param0, var8, 1, 6 - var9, 1 + var9, param4);
            this.placeBlock(param0, var8, 2, 6 - var9, 1 + var9, param4);
            this.placeBlock(param0, var8, 3, 6 - var9, 1 + var9, param4);
            if (var9 < 5) {
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5 - var9, 1 + var9, param4);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 2, 5 - var9, 1 + var9, param4);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), 3, 5 - var9, 1 + var9, param4);
            }
         }
      }
   }

   abstract static class StrongholdPiece extends StructurePiece {
      protected StrongholdPieces.StrongholdPiece.SmallDoorType entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;

      protected StrongholdPiece(StructurePieceType param0, int param1, BoundingBox param2) {
         super(param0, param1, param2);
      }

      public StrongholdPiece(StructurePieceType param0, CompoundTag param1) {
         super(param0, param1);
         this.entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.valueOf(param1.getString("EntryDoor"));
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         param1.putString("EntryDoor", this.entryDoor.name());
      }

      protected void generateSmallDoor(
         WorldGenLevel param0,
         RandomSource param1,
         BoundingBox param2,
         StrongholdPieces.StrongholdPiece.SmallDoorType param3,
         int param4,
         int param5,
         int param6
      ) {
         switch (param3) {
            case OPENING:
               this.generateBox(param0, param2, param4, param5, param6, param4 + 3 - 1, param5 + 3 - 1, param6, CAVE_AIR, CAVE_AIR, false);
               break;
            case WOOD_DOOR:
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4, param5, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4, param5 + 1, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4, param5 + 2, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4 + 1, param5 + 2, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4 + 2, param5 + 2, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4 + 2, param5 + 1, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4 + 2, param5, param6, param2);
               this.placeBlock(param0, Blocks.OAK_DOOR.defaultBlockState(), param4 + 1, param5, param6, param2);
               this.placeBlock(
                  param0, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), param4 + 1, param5 + 1, param6, param2
               );
               break;
            case GRATES:
               this.placeBlock(param0, Blocks.CAVE_AIR.defaultBlockState(), param4 + 1, param5, param6, param2);
               this.placeBlock(param0, Blocks.CAVE_AIR.defaultBlockState(), param4 + 1, param5 + 1, param6, param2);
               this.placeBlock(param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true), param4, param5, param6, param2);
               this.placeBlock(param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true), param4, param5 + 1, param6, param2);
               this.placeBlock(
                  param0,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                  param4,
                  param5 + 2,
                  param6,
                  param2
               );
               this.placeBlock(
                  param0,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                  param4 + 1,
                  param5 + 2,
                  param6,
                  param2
               );
               this.placeBlock(
                  param0,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                  param4 + 2,
                  param5 + 2,
                  param6,
                  param2
               );
               this.placeBlock(param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true), param4 + 2, param5 + 1, param6, param2);
               this.placeBlock(param0, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true), param4 + 2, param5, param6, param2);
               break;
            case IRON_DOOR:
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4, param5, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4, param5 + 1, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4, param5 + 2, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4 + 1, param5 + 2, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4 + 2, param5 + 2, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4 + 2, param5 + 1, param6, param2);
               this.placeBlock(param0, Blocks.STONE_BRICKS.defaultBlockState(), param4 + 2, param5, param6, param2);
               this.placeBlock(param0, Blocks.IRON_DOOR.defaultBlockState(), param4 + 1, param5, param6, param2);
               this.placeBlock(
                  param0, Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), param4 + 1, param5 + 1, param6, param2
               );
               this.placeBlock(
                  param0, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.NORTH), param4 + 2, param5 + 1, param6 + 1, param2
               );
               this.placeBlock(
                  param0, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.SOUTH), param4 + 2, param5 + 1, param6 - 1, param2
               );
         }
      }

      protected StrongholdPieces.StrongholdPiece.SmallDoorType randomSmallDoor(RandomSource param0) {
         int var2 = param0.nextInt(5);
         switch (var2) {
            case 0:
            case 1:
            default:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
            case 2:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.WOOD_DOOR;
            case 3:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES;
            case 4:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.IRON_DOOR;
         }
      }

      @Nullable
      protected StructurePiece generateSmallDoorChildForward(
         StrongholdPieces.StartPiece param0, StructurePieceAccessor param1, RandomSource param2, int param3, int param4
      ) {
         Direction var6 = this.getOrientation();
         if (var6 != null) {
            switch (var6) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.minX() + param3,
                     this.boundingBox.minY() + param4,
                     this.boundingBox.minZ() - 1,
                     var6,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.minX() + param3,
                     this.boundingBox.minY() + param4,
                     this.boundingBox.maxZ() + 1,
                     var6,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + param4,
                     this.boundingBox.minZ() + param3,
                     var6,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + param4,
                     this.boundingBox.minZ() + param3,
                     var6,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      @Nullable
      protected StructurePiece generateSmallDoorChildLeft(
         StrongholdPieces.StartPiece param0, StructurePieceAccessor param1, RandomSource param2, int param3, int param4
      ) {
         Direction var6 = this.getOrientation();
         if (var6 != null) {
            switch (var6) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + param3,
                     this.boundingBox.minZ() + param4,
                     Direction.WEST,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + param3,
                     this.boundingBox.minZ() + param4,
                     Direction.WEST,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.minX() + param4,
                     this.boundingBox.minY() + param3,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.minX() + param4,
                     this.boundingBox.minY() + param3,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      @Nullable
      protected StructurePiece generateSmallDoorChildRight(
         StrongholdPieces.StartPiece param0, StructurePieceAccessor param1, RandomSource param2, int param3, int param4
      ) {
         Direction var6 = this.getOrientation();
         if (var6 != null) {
            switch (var6) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + param3,
                     this.boundingBox.minZ() + param4,
                     Direction.EAST,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + param3,
                     this.boundingBox.minZ() + param4,
                     Direction.EAST,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.minX() + param4,
                     this.boundingBox.minY() + param3,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     param0,
                     param1,
                     param2,
                     this.boundingBox.minX() + param4,
                     this.boundingBox.minY() + param3,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      protected static boolean isOkBox(BoundingBox param0) {
         return param0 != null && param0.minY() > 10;
      }

      protected enum SmallDoorType {
         OPENING,
         WOOD_DOOR,
         GRATES,
         IRON_DOOR;
      }
   }

   public abstract static class Turn extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 5;
      protected static final int HEIGHT = 5;
      protected static final int DEPTH = 5;

      protected Turn(StructurePieceType param0, int param1, BoundingBox param2) {
         super(param0, param1, param2);
      }

      public Turn(StructurePieceType param0, CompoundTag param1) {
         super(param0, param1);
      }
   }
}
