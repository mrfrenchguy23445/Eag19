package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class WoodlandMansionPieces {
   public static void generateMansion(
      StructureTemplateManager param0, BlockPos param1, Rotation param2, List<WoodlandMansionPieces.WoodlandMansionPiece> param3, RandomSource param4
   ) {
      WoodlandMansionPieces.MansionGrid var5 = new WoodlandMansionPieces.MansionGrid(param4);
      WoodlandMansionPieces.MansionPiecePlacer var6 = new WoodlandMansionPieces.MansionPiecePlacer(param0, param4);
      var6.createMansion(param1, param2, param3, var5);
   }

   public static void main(String[] param0) {
      RandomSource var1 = RandomSource.create();
      long var2 = var1.nextLong();
      System.out.println("Seed: " + var2);
      var1.setSeed(var2);
      WoodlandMansionPieces.MansionGrid var4 = new WoodlandMansionPieces.MansionGrid(var1);
      var4.print();
   }

   static class FirstFloorRoomCollection extends WoodlandMansionPieces.FloorRoomCollection {
      @Override
      public String get1x1(RandomSource param0) {
         return "1x1_a" + (param0.nextInt(5) + 1);
      }

      @Override
      public String get1x1Secret(RandomSource param0) {
         return "1x1_as" + (param0.nextInt(4) + 1);
      }

      @Override
      public String get1x2SideEntrance(RandomSource param0, boolean param1) {
         return "1x2_a" + (param0.nextInt(9) + 1);
      }

      @Override
      public String get1x2FrontEntrance(RandomSource param0, boolean param1) {
         return "1x2_b" + (param0.nextInt(5) + 1);
      }

      @Override
      public String get1x2Secret(RandomSource param0) {
         return "1x2_s" + (param0.nextInt(2) + 1);
      }

      @Override
      public String get2x2(RandomSource param0) {
         return "2x2_a" + (param0.nextInt(4) + 1);
      }

      @Override
      public String get2x2Secret(RandomSource param0) {
         return "2x2_s1";
      }
   }

   abstract static class FloorRoomCollection {
      public abstract String get1x1(RandomSource var1);

      public abstract String get1x1Secret(RandomSource var1);

      public abstract String get1x2SideEntrance(RandomSource var1, boolean var2);

      public abstract String get1x2FrontEntrance(RandomSource var1, boolean var2);

      public abstract String get1x2Secret(RandomSource var1);

      public abstract String get2x2(RandomSource var1);

      public abstract String get2x2Secret(RandomSource var1);
   }

   static class MansionGrid {
      private static final int DEFAULT_SIZE = 11;
      private static final int CLEAR = 0;
      private static final int CORRIDOR = 1;
      private static final int ROOM = 2;
      private static final int START_ROOM = 3;
      private static final int TEST_ROOM = 4;
      private static final int BLOCKED = 5;
      private static final int ROOM_1x1 = 65536;
      private static final int ROOM_1x2 = 131072;
      private static final int ROOM_2x2 = 262144;
      private static final int ROOM_ORIGIN_FLAG = 1048576;
      private static final int ROOM_DOOR_FLAG = 2097152;
      private static final int ROOM_STAIRS_FLAG = 4194304;
      private static final int ROOM_CORRIDOR_FLAG = 8388608;
      private static final int ROOM_TYPE_MASK = 983040;
      private static final int ROOM_ID_MASK = 65535;
      private final RandomSource random;
      final WoodlandMansionPieces.SimpleGrid baseGrid;
      final WoodlandMansionPieces.SimpleGrid thirdFloorGrid;
      final WoodlandMansionPieces.SimpleGrid[] floorRooms;
      final int entranceX;
      final int entranceY;

      public MansionGrid(RandomSource param0) {
         this.random = param0;
         int var2 = 11;
         this.entranceX = 7;
         this.entranceY = 4;
         this.baseGrid = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.baseGrid.set(this.entranceX, this.entranceY, this.entranceX + 1, this.entranceY + 1, 3);
         this.baseGrid.set(this.entranceX - 1, this.entranceY, this.entranceX - 1, this.entranceY + 1, 2);
         this.baseGrid.set(this.entranceX + 2, this.entranceY - 2, this.entranceX + 3, this.entranceY + 3, 5);
         this.baseGrid.set(this.entranceX + 1, this.entranceY - 2, this.entranceX + 1, this.entranceY - 1, 1);
         this.baseGrid.set(this.entranceX + 1, this.entranceY + 2, this.entranceX + 1, this.entranceY + 3, 1);
         this.baseGrid.set(this.entranceX - 1, this.entranceY - 1, 1);
         this.baseGrid.set(this.entranceX - 1, this.entranceY + 2, 1);
         this.baseGrid.set(0, 0, 11, 1, 5);
         this.baseGrid.set(0, 9, 11, 11, 5);
         this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY - 2, Direction.WEST, 6);
         this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY + 3, Direction.WEST, 6);
         this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY - 1, Direction.WEST, 3);
         this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY + 2, Direction.WEST, 3);

         while (this.cleanEdges(this.baseGrid)) {
         }

         this.floorRooms = new WoodlandMansionPieces.SimpleGrid[3];
         this.floorRooms[0] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.floorRooms[1] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.floorRooms[2] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.identifyRooms(this.baseGrid, this.floorRooms[0]);
         this.identifyRooms(this.baseGrid, this.floorRooms[1]);
         this.floorRooms[0].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
         this.floorRooms[1].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
         this.thirdFloorGrid = new WoodlandMansionPieces.SimpleGrid(this.baseGrid.width, this.baseGrid.height, 5);
         this.setupThirdFloor();
         this.identifyRooms(this.thirdFloorGrid, this.floorRooms[2]);
      }

      public static boolean isHouse(WoodlandMansionPieces.SimpleGrid param0, int param1, int param2) {
         int var3 = param0.get(param1, param2);
         return var3 == 1 || var3 == 2 || var3 == 3 || var3 == 4;
      }

      public boolean isRoomId(WoodlandMansionPieces.SimpleGrid param0, int param1, int param2, int param3, int param4) {
         return (this.floorRooms[param3].get(param1, param2) & 65535) == param4;
      }

      @Nullable
      public Direction get1x2RoomDirection(WoodlandMansionPieces.SimpleGrid param0, int param1, int param2, int param3, int param4) {
         for (Direction var7 : Direction.Plane.HORIZONTAL) {
            if (this.isRoomId(param0, param1 + var7.getStepX(), param2 + var7.getStepZ(), param3, param4)) {
               return var7;
            }
         }

         return null;
      }

      private void recursiveCorridor(WoodlandMansionPieces.SimpleGrid param0, int param1, int param2, Direction param3, int param4) {
         if (param4 > 0) {
            param0.set(param1, param2, 1);
            param0.setif(param1 + param3.getStepX(), param2 + param3.getStepZ(), 0, 1);

            for (int var6 = 0; var6 < 8; var6++) {
               Direction var7 = Direction.from2DDataValue(this.random.nextInt(4));
               if (var7 != param3.getOpposite() && (var7 != Direction.EAST || !this.random.nextBoolean())) {
                  int var8 = param1 + param3.getStepX();
                  int var9 = param2 + param3.getStepZ();
                  if (param0.get(var8 + var7.getStepX(), var9 + var7.getStepZ()) == 0
                     && param0.get(var8 + var7.getStepX() * 2, var9 + var7.getStepZ() * 2) == 0) {
                     this.recursiveCorridor(
                        param0, param1 + param3.getStepX() + var7.getStepX(), param2 + param3.getStepZ() + var7.getStepZ(), var7, param4 - 1
                     );
                     break;
                  }
               }
            }

            Direction var6 = param3.getClockWise();
            Direction var7 = param3.getCounterClockWise();
            param0.setif(param1 + var6.getStepX(), param2 + var6.getStepZ(), 0, 2);
            param0.setif(param1 + var7.getStepX(), param2 + var7.getStepZ(), 0, 2);
            param0.setif(param1 + param3.getStepX() + var6.getStepX(), param2 + param3.getStepZ() + var6.getStepZ(), 0, 2);
            param0.setif(param1 + param3.getStepX() + var7.getStepX(), param2 + param3.getStepZ() + var7.getStepZ(), 0, 2);
            param0.setif(param1 + param3.getStepX() * 2, param2 + param3.getStepZ() * 2, 0, 2);
            param0.setif(param1 + var6.getStepX() * 2, param2 + var6.getStepZ() * 2, 0, 2);
            param0.setif(param1 + var7.getStepX() * 2, param2 + var7.getStepZ() * 2, 0, 2);
         }
      }

      private boolean cleanEdges(WoodlandMansionPieces.SimpleGrid param0) {
         boolean var2 = false;

         for (int var3 = 0; var3 < param0.height; var3++) {
            for (int var4 = 0; var4 < param0.width; var4++) {
               if (param0.get(var4, var3) == 0) {
                  int var5 = 0;
                  var5 += isHouse(param0, var4 + 1, var3) ? 1 : 0;
                  var5 += isHouse(param0, var4 - 1, var3) ? 1 : 0;
                  var5 += isHouse(param0, var4, var3 + 1) ? 1 : 0;
                  var5 += isHouse(param0, var4, var3 - 1) ? 1 : 0;
                  if (var5 >= 3) {
                     param0.set(var4, var3, 2);
                     var2 = true;
                  } else if (var5 == 2) {
                     int var6 = 0;
                     var6 += isHouse(param0, var4 + 1, var3 + 1) ? 1 : 0;
                     var6 += isHouse(param0, var4 - 1, var3 + 1) ? 1 : 0;
                     var6 += isHouse(param0, var4 + 1, var3 - 1) ? 1 : 0;
                     var6 += isHouse(param0, var4 - 1, var3 - 1) ? 1 : 0;
                     if (var6 <= 1) {
                        param0.set(var4, var3, 2);
                        var2 = true;
                     }
                  }
               }
            }
         }

         return var2;
      }

      private void setupThirdFloor() {
         List<Tuple<Integer, Integer>> var1 = Lists.newArrayList();
         WoodlandMansionPieces.SimpleGrid var2 = this.floorRooms[1];

         for (int var3 = 0; var3 < this.thirdFloorGrid.height; var3++) {
            for (int var4 = 0; var4 < this.thirdFloorGrid.width; var4++) {
               int var5 = var2.get(var4, var3);
               int var6 = var5 & 983040;
               if (var6 == 131072 && (var5 & 2097152) == 2097152) {
                  var1.add(new Tuple<>(var4, var3));
               }
            }
         }

         if (var1.isEmpty()) {
            this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
         } else {
            Tuple<Integer, Integer> var3 = var1.get(this.random.nextInt(var1.size()));
            int var4 = var2.get(var3.getA(), var3.getB());
            var2.set(var3.getA(), var3.getB(), var4 | 4194304);
            Direction var5 = this.get1x2RoomDirection(this.baseGrid, var3.getA(), var3.getB(), 1, var4 & 65535);
            int var6 = var3.getA() + var5.getStepX();
            int var7 = var3.getB() + var5.getStepZ();

            for (int var8 = 0; var8 < this.thirdFloorGrid.height; var8++) {
               for (int var9 = 0; var9 < this.thirdFloorGrid.width; var9++) {
                  if (!isHouse(this.baseGrid, var9, var8)) {
                     this.thirdFloorGrid.set(var9, var8, 5);
                  } else if (var9 == var3.getA() && var8 == var3.getB()) {
                     this.thirdFloorGrid.set(var9, var8, 3);
                  } else if (var9 == var6 && var8 == var7) {
                     this.thirdFloorGrid.set(var9, var8, 3);
                     this.floorRooms[2].set(var9, var8, 8388608);
                  }
               }
            }

            List<Direction> var8 = Lists.newArrayList();

            for (Direction var10 : Direction.Plane.HORIZONTAL) {
               if (this.thirdFloorGrid.get(var6 + var10.getStepX(), var7 + var10.getStepZ()) == 0) {
                  var8.add(var10);
               }
            }

            if (var8.isEmpty()) {
               this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
               var2.set(var3.getA(), var3.getB(), var4);
            } else {
               Direction var9 = var8.get(this.random.nextInt(var8.size()));
               this.recursiveCorridor(this.thirdFloorGrid, var6 + var9.getStepX(), var7 + var9.getStepZ(), var9, 4);

               while (this.cleanEdges(this.thirdFloorGrid)) {
               }
            }
         }
      }

      private void identifyRooms(WoodlandMansionPieces.SimpleGrid param0, WoodlandMansionPieces.SimpleGrid param1) {
         ObjectArrayList<Tuple<Integer, Integer>> var3 = new ObjectArrayList<>();

         for (int var4 = 0; var4 < param0.height; var4++) {
            for (int var5 = 0; var5 < param0.width; var5++) {
               if (param0.get(var5, var4) == 2) {
                  var3.add(new Tuple<>(var5, var4));
               }
            }
         }

         Util.shuffle(var3, this.random);
         int var4 = 10;

         for (Tuple<Integer, Integer> var6 : var3) {
            int var7 = var6.getA();
            int var8 = var6.getB();
            if (param1.get(var7, var8) == 0) {
               int var9 = var7;
               int var10 = var7;
               int var11 = var8;
               int var12 = var8;
               int var13 = 65536;
               if (param1.get(var7 + 1, var8) == 0
                  && param1.get(var7, var8 + 1) == 0
                  && param1.get(var7 + 1, var8 + 1) == 0
                  && param0.get(var7 + 1, var8) == 2
                  && param0.get(var7, var8 + 1) == 2
                  && param0.get(var7 + 1, var8 + 1) == 2) {
                  var10++;
                  var12++;
                  var13 = 262144;
               } else if (param1.get(var7 - 1, var8) == 0
                  && param1.get(var7, var8 + 1) == 0
                  && param1.get(var7 - 1, var8 + 1) == 0
                  && param0.get(var7 - 1, var8) == 2
                  && param0.get(var7, var8 + 1) == 2
                  && param0.get(var7 - 1, var8 + 1) == 2) {
                  var9--;
                  var12++;
                  var13 = 262144;
               } else if (param1.get(var7 - 1, var8) == 0
                  && param1.get(var7, var8 - 1) == 0
                  && param1.get(var7 - 1, var8 - 1) == 0
                  && param0.get(var7 - 1, var8) == 2
                  && param0.get(var7, var8 - 1) == 2
                  && param0.get(var7 - 1, var8 - 1) == 2) {
                  var9--;
                  var11--;
                  var13 = 262144;
               } else if (param1.get(var7 + 1, var8) == 0 && param0.get(var7 + 1, var8) == 2) {
                  var10++;
                  var13 = 131072;
               } else if (param1.get(var7, var8 + 1) == 0 && param0.get(var7, var8 + 1) == 2) {
                  var12++;
                  var13 = 131072;
               } else if (param1.get(var7 - 1, var8) == 0 && param0.get(var7 - 1, var8) == 2) {
                  var9--;
                  var13 = 131072;
               } else if (param1.get(var7, var8 - 1) == 0 && param0.get(var7, var8 - 1) == 2) {
                  var11--;
                  var13 = 131072;
               }

               int var14 = this.random.nextBoolean() ? var9 : var10;
               int var15 = this.random.nextBoolean() ? var11 : var12;
               int var16 = 2097152;
               if (!param0.edgesTo(var14, var15, 1)) {
                  var14 = var14 == var9 ? var10 : var9;
                  var15 = var15 == var11 ? var12 : var11;
                  if (!param0.edgesTo(var14, var15, 1)) {
                     var15 = var15 == var11 ? var12 : var11;
                     if (!param0.edgesTo(var14, var15, 1)) {
                        var14 = var14 == var9 ? var10 : var9;
                        var15 = var15 == var11 ? var12 : var11;
                        if (!param0.edgesTo(var14, var15, 1)) {
                           var16 = 0;
                           var14 = var9;
                           var15 = var11;
                        }
                     }
                  }
               }

               for (int var17 = var11; var17 <= var12; var17++) {
                  for (int var18 = var9; var18 <= var10; var18++) {
                     if (var18 == var14 && var17 == var15) {
                        param1.set(var18, var17, 1048576 | var16 | var13 | var4);
                     } else {
                        param1.set(var18, var17, var13 | var4);
                     }
                  }
               }

               var4++;
            }
         }
      }

      public void print() {
         for (int var1 = 0; var1 < 2; var1++) {
            WoodlandMansionPieces.SimpleGrid var2 = var1 == 0 ? this.baseGrid : this.thirdFloorGrid;

            for (int var3 = 0; var3 < var2.height; var3++) {
               for (int var4 = 0; var4 < var2.width; var4++) {
                  int var5 = var2.get(var4, var3);
                  if (var5 == 1) {
                     System.out.print("+");
                  } else if (var5 == 4) {
                     System.out.print("x");
                  } else if (var5 == 2) {
                     System.out.print("X");
                  } else if (var5 == 3) {
                     System.out.print("O");
                  } else if (var5 == 5) {
                     System.out.print("#");
                  } else {
                     System.out.print(" ");
                  }
               }

               System.out.println("");
            }

            System.out.println("");
         }
      }
   }

   static class MansionPiecePlacer {
      private final StructureTemplateManager structureTemplateManager;
      private final RandomSource random;
      private int startX;
      private int startY;

      public MansionPiecePlacer(StructureTemplateManager param0, RandomSource param1) {
         this.structureTemplateManager = param0;
         this.random = param1;
      }

      public void createMansion(
         BlockPos param0, Rotation param1, List<WoodlandMansionPieces.WoodlandMansionPiece> param2, WoodlandMansionPieces.MansionGrid param3
      ) {
         WoodlandMansionPieces.PlacementData var5 = new WoodlandMansionPieces.PlacementData();
         var5.position = param0;
         var5.rotation = param1;
         var5.wallType = "wall_flat";
         WoodlandMansionPieces.PlacementData var6 = new WoodlandMansionPieces.PlacementData();
         this.entrance(param2, var5);
         var6.position = var5.position.above(8);
         var6.rotation = var5.rotation;
         var6.wallType = "wall_window";
         if (!param2.isEmpty()) {
         }

         WoodlandMansionPieces.SimpleGrid var7 = param3.baseGrid;
         WoodlandMansionPieces.SimpleGrid var8 = param3.thirdFloorGrid;
         this.startX = param3.entranceX + 1;
         this.startY = param3.entranceY + 1;
         int var9 = param3.entranceX + 1;
         int var10 = param3.entranceY;
         this.traverseOuterWalls(param2, var5, var7, Direction.SOUTH, this.startX, this.startY, var9, var10);
         this.traverseOuterWalls(param2, var6, var7, Direction.SOUTH, this.startX, this.startY, var9, var10);
         WoodlandMansionPieces.PlacementData var11 = new WoodlandMansionPieces.PlacementData();
         var11.position = var5.position.above(19);
         var11.rotation = var5.rotation;
         var11.wallType = "wall_window";
         boolean var12 = false;

         for (int var13 = 0; var13 < var8.height && !var12; var13++) {
            for (int var14 = var8.width - 1; var14 >= 0 && !var12; var14--) {
               if (WoodlandMansionPieces.MansionGrid.isHouse(var8, var14, var13)) {
                  var11.position = var11.position.relative(param1.rotate(Direction.SOUTH), 8 + (var13 - this.startY) * 8);
                  var11.position = var11.position.relative(param1.rotate(Direction.EAST), (var14 - this.startX) * 8);
                  this.traverseWallPiece(param2, var11);
                  this.traverseOuterWalls(param2, var11, var8, Direction.SOUTH, var14, var13, var14, var13);
                  var12 = true;
               }
            }
         }

         this.createRoof(param2, param0.above(16), param1, var7, var8);
         this.createRoof(param2, param0.above(27), param1, var8, null);
         if (!param2.isEmpty()) {
         }

         WoodlandMansionPieces.FloorRoomCollection[] var13 = new WoodlandMansionPieces.FloorRoomCollection[]{
            new WoodlandMansionPieces.FirstFloorRoomCollection(),
            new WoodlandMansionPieces.SecondFloorRoomCollection(),
            new WoodlandMansionPieces.ThirdFloorRoomCollection()
         };

         for (int var14 = 0; var14 < 3; var14++) {
            BlockPos var15 = param0.above(8 * var14 + (var14 == 2 ? 3 : 0));
            WoodlandMansionPieces.SimpleGrid var16 = param3.floorRooms[var14];
            WoodlandMansionPieces.SimpleGrid var17 = var14 == 2 ? var8 : var7;
            String var18 = var14 == 0 ? "carpet_south_1" : "carpet_south_2";
            String var19 = var14 == 0 ? "carpet_west_1" : "carpet_west_2";

            for (int var20 = 0; var20 < var17.height; var20++) {
               for (int var21 = 0; var21 < var17.width; var21++) {
                  if (var17.get(var21, var20) == 1) {
                     BlockPos var22 = var15.relative(param1.rotate(Direction.SOUTH), 8 + (var20 - this.startY) * 8);
                     var22 = var22.relative(param1.rotate(Direction.EAST), (var21 - this.startX) * 8);
                     param2.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "corridor_floor", var22, param1));
                     if (var17.get(var21, var20 - 1) == 1 || (var16.get(var21, var20 - 1) & 8388608) == 8388608) {
                        param2.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "carpet_north", var22.relative(param1.rotate(Direction.EAST), 1).above(), param1
                           )
                        );
                     }

                     if (var17.get(var21 + 1, var20) == 1 || (var16.get(var21 + 1, var20) & 8388608) == 8388608) {
                        param2.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              "carpet_east",
                              var22.relative(param1.rotate(Direction.SOUTH), 1).relative(param1.rotate(Direction.EAST), 5).above(),
                              param1
                           )
                        );
                     }

                     if (var17.get(var21, var20 + 1) == 1 || (var16.get(var21, var20 + 1) & 8388608) == 8388608) {
                        param2.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              var18,
                              var22.relative(param1.rotate(Direction.SOUTH), 5).relative(param1.rotate(Direction.WEST), 1),
                              param1
                           )
                        );
                     }

                     if (var17.get(var21 - 1, var20) == 1 || (var16.get(var21 - 1, var20) & 8388608) == 8388608) {
                        param2.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              var19,
                              var22.relative(param1.rotate(Direction.WEST), 1).relative(param1.rotate(Direction.NORTH), 1),
                              param1
                           )
                        );
                     }
                  }
               }
            }

            String var20 = var14 == 0 ? "indoors_wall_1" : "indoors_wall_2";
            String var21 = var14 == 0 ? "indoors_door_1" : "indoors_door_2";
            List<Direction> var22 = Lists.newArrayList();

            for (int var23 = 0; var23 < var17.height; var23++) {
               for (int var24 = 0; var24 < var17.width; var24++) {
                  boolean var25 = var14 == 2 && var17.get(var24, var23) == 3;
                  if (var17.get(var24, var23) == 2 || var25) {
                     int var26 = var16.get(var24, var23);
                     int var27 = var26 & 983040;
                     int var28 = var26 & 65535;
                     var25 = var25 && (var26 & 8388608) == 8388608;
                     var22.clear();
                     if ((var26 & 2097152) == 2097152) {
                        for (Direction var30 : Direction.Plane.HORIZONTAL) {
                           if (var17.get(var24 + var30.getStepX(), var23 + var30.getStepZ()) == 1) {
                              var22.add(var30);
                           }
                        }
                     }

                     Direction var29 = null;
                     if (!var22.isEmpty()) {
                        var29 = var22.get(this.random.nextInt(var22.size()));
                     } else if ((var26 & 1048576) == 1048576) {
                        var29 = Direction.UP;
                     }

                     BlockPos var30 = var15.relative(param1.rotate(Direction.SOUTH), 8 + (var23 - this.startY) * 8);
                     var30 = var30.relative(param1.rotate(Direction.EAST), -1 + (var24 - this.startX) * 8);
                     if (WoodlandMansionPieces.MansionGrid.isHouse(var17, var24 - 1, var23) && !param3.isRoomId(var17, var24 - 1, var23, var14, var28)) {
                        param2.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, var29 == Direction.WEST ? var21 : var20, var30, param1)
                        );
                     }

                     if (var17.get(var24 + 1, var23) == 1 && !var25) {
                        BlockPos var31 = var30.relative(param1.rotate(Direction.EAST), 8);
                        param2.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, var29 == Direction.EAST ? var21 : var20, var31, param1)
                        );
                     }

                     if (WoodlandMansionPieces.MansionGrid.isHouse(var17, var24, var23 + 1) && !param3.isRoomId(var17, var24, var23 + 1, var14, var28)) {
                        BlockPos var31 = var30.relative(param1.rotate(Direction.SOUTH), 7);
                        var31 = var31.relative(param1.rotate(Direction.EAST), 7);
                        param2.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, var29 == Direction.SOUTH ? var21 : var20, var31, param1.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if (var17.get(var24, var23 - 1) == 1 && !var25) {
                        BlockPos var31 = var30.relative(param1.rotate(Direction.NORTH), 1);
                        var31 = var31.relative(param1.rotate(Direction.EAST), 7);
                        param2.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, var29 == Direction.NORTH ? var21 : var20, var31, param1.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if (var27 == 65536) {
                        this.addRoom1x1(param2, var30, param1, var29, var13[var14]);
                     } else if (var27 == 131072 && var29 != null) {
                        Direction var31 = param3.get1x2RoomDirection(var17, var24, var23, var14, var28);
                        boolean var32 = (var26 & 4194304) == 4194304;
                        this.addRoom1x2(param2, var30, param1, var31, var29, var13[var14], var32);
                     } else if (var27 == 262144 && var29 != null && var29 != Direction.UP) {
                        Direction var31 = var29.getClockWise();
                        if (!param3.isRoomId(var17, var24 + var31.getStepX(), var23 + var31.getStepZ(), var14, var28)) {
                           var31 = var31.getOpposite();
                        }

                        this.addRoom2x2(param2, var30, param1, var31, var29, var13[var14]);
                     } else if (var27 == 262144 && var29 == Direction.UP) {
                        this.addRoom2x2Secret(param2, var30, param1, var13[var14]);
                     }
                  }
               }
            }
         }
      }

      private void traverseOuterWalls(
         List<WoodlandMansionPieces.WoodlandMansionPiece> param0,
         WoodlandMansionPieces.PlacementData param1,
         WoodlandMansionPieces.SimpleGrid param2,
         Direction param3,
         int param4,
         int param5,
         int param6,
         int param7
      ) {
         int var9 = param4;
         int var10 = param5;
         Direction var11 = param3;

         do {
            if (!WoodlandMansionPieces.MansionGrid.isHouse(param2, var9 + param3.getStepX(), var10 + param3.getStepZ())) {
               this.traverseTurn(param0, param1);
               param3 = param3.getClockWise();
               if (var9 != param6 || var10 != param7 || var11 != param3) {
                  this.traverseWallPiece(param0, param1);
               }
            } else if (WoodlandMansionPieces.MansionGrid.isHouse(param2, var9 + param3.getStepX(), var10 + param3.getStepZ())
               && WoodlandMansionPieces.MansionGrid.isHouse(
                  param2,
                  var9 + param3.getStepX() + param3.getCounterClockWise().getStepX(),
                  var10 + param3.getStepZ() + param3.getCounterClockWise().getStepZ()
               )) {
               this.traverseInnerTurn(param0, param1);
               var9 += param3.getStepX();
               var10 += param3.getStepZ();
               param3 = param3.getCounterClockWise();
            } else {
               var9 += param3.getStepX();
               var10 += param3.getStepZ();
               if (var9 != param6 || var10 != param7 || var11 != param3) {
                  this.traverseWallPiece(param0, param1);
               }
            }
         } while (var9 != param6 || var10 != param7 || var11 != param3);
      }

      private void createRoof(
         List<WoodlandMansionPieces.WoodlandMansionPiece> param0,
         BlockPos param1,
         Rotation param2,
         WoodlandMansionPieces.SimpleGrid param3,
         @Nullable WoodlandMansionPieces.SimpleGrid param4
      ) {
         for (int var6 = 0; var6 < param3.height; var6++) {
            for (int var7 = 0; var7 < param3.width; var7++) {
               BlockPos var8 = param1;
               var8 = var8.relative(param2.rotate(Direction.SOUTH), 8 + (var6 - this.startY) * 8);
               var8 = var8.relative(param2.rotate(Direction.EAST), (var7 - this.startX) * 8);
               boolean var9 = param4 != null && WoodlandMansionPieces.MansionGrid.isHouse(param4, var7, var6);
               if (WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6) && !var9) {
                  param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof", var8.above(3), param2));
                  if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 + 1, var6)) {
                     BlockPos var10 = var8.relative(param2.rotate(Direction.EAST), 6);
                     param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_front", var10, param2));
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 - 1, var6)) {
                     BlockPos var10 = var8.relative(param2.rotate(Direction.EAST), 0);
                     var10 = var10.relative(param2.rotate(Direction.SOUTH), 7);
                     param0.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", var10, param2.getRotated(Rotation.CLOCKWISE_180)
                        )
                     );
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 - 1)) {
                     BlockPos var10 = var8.relative(param2.rotate(Direction.WEST), 1);
                     param0.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", var10, param2.getRotated(Rotation.COUNTERCLOCKWISE_90)
                        )
                     );
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 + 1)) {
                     BlockPos var10 = var8.relative(param2.rotate(Direction.EAST), 6);
                     var10 = var10.relative(param2.rotate(Direction.SOUTH), 6);
                     param0.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", var10, param2.getRotated(Rotation.CLOCKWISE_90)
                        )
                     );
                  }
               }
            }
         }

         if (param4 != null) {
            for (int var6 = 0; var6 < param3.height; var6++) {
               for (int var7 = 0; var7 < param3.width; var7++) {
                  BlockPos var8 = param1;
                  var8 = var8.relative(param2.rotate(Direction.SOUTH), 8 + (var6 - this.startY) * 8);
                  var8 = var8.relative(param2.rotate(Direction.EAST), (var7 - this.startX) * 8);
                  boolean var9 = WoodlandMansionPieces.MansionGrid.isHouse(param4, var7, var6);
                  if (WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6) && var9) {
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 + 1, var6)) {
                        BlockPos var10 = var8.relative(param2.rotate(Direction.EAST), 7);
                        param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "small_wall", var10, param2));
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 - 1, var6)) {
                        BlockPos var10 = var8.relative(param2.rotate(Direction.WEST), 1);
                        var10 = var10.relative(param2.rotate(Direction.SOUTH), 6);
                        param0.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", var10, param2.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 - 1)) {
                        BlockPos var10 = var8.relative(param2.rotate(Direction.WEST), 0);
                        var10 = var10.relative(param2.rotate(Direction.NORTH), 1);
                        param0.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", var10, param2.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 + 1)) {
                        BlockPos var10 = var8.relative(param2.rotate(Direction.EAST), 6);
                        var10 = var10.relative(param2.rotate(Direction.SOUTH), 7);
                        param0.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", var10, param2.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 + 1, var6)) {
                        if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 - 1)) {
                           BlockPos var10 = var8.relative(param2.rotate(Direction.EAST), 7);
                           var10 = var10.relative(param2.rotate(Direction.NORTH), 2);
                           param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "small_wall_corner", var10, param2));
                        }

                        if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 + 1)) {
                           BlockPos var10 = var8.relative(param2.rotate(Direction.EAST), 8);
                           var10 = var10.relative(param2.rotate(Direction.SOUTH), 7);
                           param0.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", var10, param2.getRotated(Rotation.CLOCKWISE_90)
                              )
                           );
                        }
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 - 1, var6)) {
                        if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 - 1)) {
                           BlockPos var10 = var8.relative(param2.rotate(Direction.WEST), 2);
                           var10 = var10.relative(param2.rotate(Direction.NORTH), 1);
                           param0.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", var10, param2.getRotated(Rotation.COUNTERCLOCKWISE_90)
                              )
                           );
                        }

                        if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 + 1)) {
                           BlockPos var10 = var8.relative(param2.rotate(Direction.WEST), 1);
                           var10 = var10.relative(param2.rotate(Direction.SOUTH), 8);
                           param0.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", var10, param2.getRotated(Rotation.CLOCKWISE_180)
                              )
                           );
                        }
                     }
                  }
               }
            }
         }

         for (int var6 = 0; var6 < param3.height; var6++) {
            for (int var7 = 0; var7 < param3.width; var7++) {
               BlockPos var8 = param1;
               var8 = var8.relative(param2.rotate(Direction.SOUTH), 8 + (var6 - this.startY) * 8);
               var8 = var8.relative(param2.rotate(Direction.EAST), (var7 - this.startX) * 8);
               boolean var9 = param4 != null && WoodlandMansionPieces.MansionGrid.isHouse(param4, var7, var6);
               if (WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6) && !var9) {
                  if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 + 1, var6)) {
                     BlockPos var10 = var8.relative(param2.rotate(Direction.EAST), 6);
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 + 1)) {
                        BlockPos var11 = var10.relative(param2.rotate(Direction.SOUTH), 6);
                        param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_corner", var11, param2));
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 + 1, var6 + 1)) {
                        BlockPos var11 = var10.relative(param2.rotate(Direction.SOUTH), 5);
                        param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_inner_corner", var11, param2));
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 - 1)) {
                        param0.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", var10, param2.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 + 1, var6 - 1)) {
                        BlockPos var11 = var8.relative(param2.rotate(Direction.EAST), 9);
                        var11 = var11.relative(param2.rotate(Direction.NORTH), 2);
                        param0.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", var11, param2.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 - 1, var6)) {
                     BlockPos var10 = var8.relative(param2.rotate(Direction.EAST), 0);
                     var10 = var10.relative(param2.rotate(Direction.SOUTH), 0);
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 + 1)) {
                        BlockPos var11 = var10.relative(param2.rotate(Direction.SOUTH), 6);
                        param0.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", var11, param2.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 - 1, var6 + 1)) {
                        BlockPos var11 = var10.relative(param2.rotate(Direction.SOUTH), 8);
                        var11 = var11.relative(param2.rotate(Direction.WEST), 3);
                        param0.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", var11, param2.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(param3, var7, var6 - 1)) {
                        param0.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", var10, param2.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(param3, var7 - 1, var6 - 1)) {
                        BlockPos var11 = var10.relative(param2.rotate(Direction.SOUTH), 1);
                        param0.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", var11, param2.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     }
                  }
               }
            }
         }
      }

      private void entrance(List<WoodlandMansionPieces.WoodlandMansionPiece> param0, WoodlandMansionPieces.PlacementData param1) {
         Direction var3 = param1.rotation.rotate(Direction.WEST);
         param0.add(
            new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "entrance", param1.position.relative(var3, 9), param1.rotation)
         );
         param1.position = param1.position.relative(param1.rotation.rotate(Direction.SOUTH), 16);
      }

      private void traverseWallPiece(List<WoodlandMansionPieces.WoodlandMansionPiece> param0, WoodlandMansionPieces.PlacementData param1) {
         param0.add(
            new WoodlandMansionPieces.WoodlandMansionPiece(
               this.structureTemplateManager, param1.wallType, param1.position.relative(param1.rotation.rotate(Direction.EAST), 7), param1.rotation
            )
         );
         param1.position = param1.position.relative(param1.rotation.rotate(Direction.SOUTH), 8);
      }

      private void traverseTurn(List<WoodlandMansionPieces.WoodlandMansionPiece> param0, WoodlandMansionPieces.PlacementData param1) {
         param1.position = param1.position.relative(param1.rotation.rotate(Direction.SOUTH), -1);
         param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "wall_corner", param1.position, param1.rotation));
         param1.position = param1.position.relative(param1.rotation.rotate(Direction.SOUTH), -7);
         param1.position = param1.position.relative(param1.rotation.rotate(Direction.WEST), -6);
         param1.rotation = param1.rotation.getRotated(Rotation.CLOCKWISE_90);
      }

      private void traverseInnerTurn(List<WoodlandMansionPieces.WoodlandMansionPiece> param0, WoodlandMansionPieces.PlacementData param1) {
         param1.position = param1.position.relative(param1.rotation.rotate(Direction.SOUTH), 6);
         param1.position = param1.position.relative(param1.rotation.rotate(Direction.EAST), 8);
         param1.rotation = param1.rotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
      }

      private void addRoom1x1(
         List<WoodlandMansionPieces.WoodlandMansionPiece> param0,
         BlockPos param1,
         Rotation param2,
         Direction param3,
         WoodlandMansionPieces.FloorRoomCollection param4
      ) {
         Rotation var6 = Rotation.NONE;
         String var7 = param4.get1x1(this.random);
         if (param3 != Direction.EAST) {
            if (param3 == Direction.NORTH) {
               var6 = var6.getRotated(Rotation.COUNTERCLOCKWISE_90);
            } else if (param3 == Direction.WEST) {
               var6 = var6.getRotated(Rotation.CLOCKWISE_180);
            } else if (param3 == Direction.SOUTH) {
               var6 = var6.getRotated(Rotation.CLOCKWISE_90);
            } else {
               var7 = param4.get1x1Secret(this.random);
            }
         }

         BlockPos var8 = StructureTemplate.getZeroPositionWithTransform(new BlockPos(1, 0, 0), Mirror.NONE, var6, 7, 7);
         var6 = var6.getRotated(param2);
         var8 = var8.rotate(param2);
         BlockPos var9 = param1.offset(var8.getX(), 0, var8.getZ());
         param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, var7, var9, var6));
      }

      private void addRoom1x2(
         List<WoodlandMansionPieces.WoodlandMansionPiece> param0,
         BlockPos param1,
         Rotation param2,
         Direction param3,
         Direction param4,
         WoodlandMansionPieces.FloorRoomCollection param5,
         boolean param6
      ) {
         if (param4 == Direction.EAST && param3 == Direction.SOUTH) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 1);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, param5.get1x2SideEntrance(this.random, param6), var8, param2)
            );
         } else if (param4 == Direction.EAST && param3 == Direction.NORTH) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 1);
            var8 = var8.relative(param2.rotate(Direction.SOUTH), 6);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, param5.get1x2SideEntrance(this.random, param6), var8, param2, Mirror.LEFT_RIGHT
               )
            );
         } else if (param4 == Direction.WEST && param3 == Direction.NORTH) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 7);
            var8 = var8.relative(param2.rotate(Direction.SOUTH), 6);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, param5.get1x2SideEntrance(this.random, param6), var8, param2.getRotated(Rotation.CLOCKWISE_180)
               )
            );
         } else if (param4 == Direction.WEST && param3 == Direction.SOUTH) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 7);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, param5.get1x2SideEntrance(this.random, param6), var8, param2, Mirror.FRONT_BACK
               )
            );
         } else if (param4 == Direction.SOUTH && param3 == Direction.EAST) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 1);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  param5.get1x2SideEntrance(this.random, param6),
                  var8,
                  param2.getRotated(Rotation.CLOCKWISE_90),
                  Mirror.LEFT_RIGHT
               )
            );
         } else if (param4 == Direction.SOUTH && param3 == Direction.WEST) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 7);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, param5.get1x2SideEntrance(this.random, param6), var8, param2.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (param4 == Direction.NORTH && param3 == Direction.WEST) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 7);
            var8 = var8.relative(param2.rotate(Direction.SOUTH), 6);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  param5.get1x2SideEntrance(this.random, param6),
                  var8,
                  param2.getRotated(Rotation.CLOCKWISE_90),
                  Mirror.FRONT_BACK
               )
            );
         } else if (param4 == Direction.NORTH && param3 == Direction.EAST) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 1);
            var8 = var8.relative(param2.rotate(Direction.SOUTH), 6);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, param5.get1x2SideEntrance(this.random, param6), var8, param2.getRotated(Rotation.COUNTERCLOCKWISE_90)
               )
            );
         } else if (param4 == Direction.SOUTH && param3 == Direction.NORTH) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 1);
            var8 = var8.relative(param2.rotate(Direction.NORTH), 8);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, param5.get1x2FrontEntrance(this.random, param6), var8, param2)
            );
         } else if (param4 == Direction.NORTH && param3 == Direction.SOUTH) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 7);
            var8 = var8.relative(param2.rotate(Direction.SOUTH), 14);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, param5.get1x2FrontEntrance(this.random, param6), var8, param2.getRotated(Rotation.CLOCKWISE_180)
               )
            );
         } else if (param4 == Direction.WEST && param3 == Direction.EAST) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 15);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, param5.get1x2FrontEntrance(this.random, param6), var8, param2.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (param4 == Direction.EAST && param3 == Direction.WEST) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.WEST), 7);
            var8 = var8.relative(param2.rotate(Direction.SOUTH), 6);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, param5.get1x2FrontEntrance(this.random, param6), var8, param2.getRotated(Rotation.COUNTERCLOCKWISE_90)
               )
            );
         } else if (param4 == Direction.UP && param3 == Direction.EAST) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 15);
            param0.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, param5.get1x2Secret(this.random), var8, param2.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (param4 == Direction.UP && param3 == Direction.SOUTH) {
            BlockPos var8 = param1.relative(param2.rotate(Direction.EAST), 1);
            var8 = var8.relative(param2.rotate(Direction.NORTH), 0);
            param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, param5.get1x2Secret(this.random), var8, param2));
         }
      }

      private void addRoom2x2(
         List<WoodlandMansionPieces.WoodlandMansionPiece> param0,
         BlockPos param1,
         Rotation param2,
         Direction param3,
         Direction param4,
         WoodlandMansionPieces.FloorRoomCollection param5
      ) {
         int var7 = 0;
         int var8 = 0;
         Rotation var9 = param2;
         Mirror var10 = Mirror.NONE;
         if (param4 == Direction.EAST && param3 == Direction.SOUTH) {
            var7 = -7;
         } else if (param4 == Direction.EAST && param3 == Direction.NORTH) {
            var7 = -7;
            var8 = 6;
            var10 = Mirror.LEFT_RIGHT;
         } else if (param4 == Direction.NORTH && param3 == Direction.EAST) {
            var7 = 1;
            var8 = 14;
            var9 = param2.getRotated(Rotation.COUNTERCLOCKWISE_90);
         } else if (param4 == Direction.NORTH && param3 == Direction.WEST) {
            var7 = 7;
            var8 = 14;
            var9 = param2.getRotated(Rotation.COUNTERCLOCKWISE_90);
            var10 = Mirror.LEFT_RIGHT;
         } else if (param4 == Direction.SOUTH && param3 == Direction.WEST) {
            var7 = 7;
            var8 = -8;
            var9 = param2.getRotated(Rotation.CLOCKWISE_90);
         } else if (param4 == Direction.SOUTH && param3 == Direction.EAST) {
            var7 = 1;
            var8 = -8;
            var9 = param2.getRotated(Rotation.CLOCKWISE_90);
            var10 = Mirror.LEFT_RIGHT;
         } else if (param4 == Direction.WEST && param3 == Direction.NORTH) {
            var7 = 15;
            var8 = 6;
            var9 = param2.getRotated(Rotation.CLOCKWISE_180);
         } else if (param4 == Direction.WEST && param3 == Direction.SOUTH) {
            var7 = 15;
            var10 = Mirror.FRONT_BACK;
         }

         BlockPos var11 = param1.relative(param2.rotate(Direction.EAST), var7);
         var11 = var11.relative(param2.rotate(Direction.SOUTH), var8);
         param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, param5.get2x2(this.random), var11, var9, var10));
      }

      private void addRoom2x2Secret(
         List<WoodlandMansionPieces.WoodlandMansionPiece> param0, BlockPos param1, Rotation param2, WoodlandMansionPieces.FloorRoomCollection param3
      ) {
         BlockPos var5 = param1.relative(param2.rotate(Direction.EAST), 1);
         param0.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, param3.get2x2Secret(this.random), var5, param2, Mirror.NONE));
      }
   }

   static class PlacementData {
      public Rotation rotation;
      public BlockPos position;
      public String wallType;
   }

   static class SecondFloorRoomCollection extends WoodlandMansionPieces.FloorRoomCollection {
      @Override
      public String get1x1(RandomSource param0) {
         return "1x1_b" + (param0.nextInt(4) + 1);
      }

      @Override
      public String get1x1Secret(RandomSource param0) {
         return "1x1_as" + (param0.nextInt(4) + 1);
      }

      @Override
      public String get1x2SideEntrance(RandomSource param0, boolean param1) {
         return param1 ? "1x2_c_stairs" : "1x2_c" + (param0.nextInt(4) + 1);
      }

      @Override
      public String get1x2FrontEntrance(RandomSource param0, boolean param1) {
         return param1 ? "1x2_d_stairs" : "1x2_d" + (param0.nextInt(5) + 1);
      }

      @Override
      public String get1x2Secret(RandomSource param0) {
         return "1x2_se" + (param0.nextInt(1) + 1);
      }

      @Override
      public String get2x2(RandomSource param0) {
         return "2x2_b" + (param0.nextInt(5) + 1);
      }

      @Override
      public String get2x2Secret(RandomSource param0) {
         return "2x2_s1";
      }
   }

   static class SimpleGrid {
      private final int[][] grid;
      final int width;
      final int height;
      private final int valueIfOutside;

      public SimpleGrid(int param0, int param1, int param2) {
         this.width = param0;
         this.height = param1;
         this.valueIfOutside = param2;
         this.grid = new int[param0][param1];
      }

      public void set(int param0, int param1, int param2) {
         if (param0 >= 0 && param0 < this.width && param1 >= 0 && param1 < this.height) {
            this.grid[param0][param1] = param2;
         }
      }

      public void set(int param0, int param1, int param2, int param3, int param4) {
         for (int var6 = param1; var6 <= param3; var6++) {
            for (int var7 = param0; var7 <= param2; var7++) {
               this.set(var7, var6, param4);
            }
         }
      }

      public int get(int param0, int param1) {
         return param0 >= 0 && param0 < this.width && param1 >= 0 && param1 < this.height ? this.grid[param0][param1] : this.valueIfOutside;
      }

      public void setif(int param0, int param1, int param2, int param3) {
         if (this.get(param0, param1) == param2) {
            this.set(param0, param1, param3);
         }
      }

      public boolean edgesTo(int param0, int param1, int param2) {
         return this.get(param0 - 1, param1) == param2
            || this.get(param0 + 1, param1) == param2
            || this.get(param0, param1 + 1) == param2
            || this.get(param0, param1 - 1) == param2;
      }
   }

   static class ThirdFloorRoomCollection extends WoodlandMansionPieces.SecondFloorRoomCollection {
   }

   public static class WoodlandMansionPiece extends TemplateStructurePiece {
      public WoodlandMansionPiece(StructureTemplateManager param0, String param1, BlockPos param2, Rotation param3) {
         this(param0, param1, param2, param3, Mirror.NONE);
      }

      public WoodlandMansionPiece(StructureTemplateManager param0, String param1, BlockPos param2, Rotation param3, Mirror param4) {
         super(StructurePieceType.WOODLAND_MANSION_PIECE, 0, param0, makeLocation(param1), param1, makeSettings(param4, param3), param2);
      }

      public WoodlandMansionPiece(StructureTemplateManager param0, CompoundTag param1) {
         super(
            StructurePieceType.WOODLAND_MANSION_PIECE,
            param1,
            param0,
            l_param1 -> makeSettings(Mirror.valueOf(param1.getString("Mi")), Rotation.valueOf(param1.getString("Rot")))
         );
      }

      @Override
      protected ResourceLocation makeTemplateLocation() {
         return makeLocation(this.templateName);
      }

      private static ResourceLocation makeLocation(String param0) {
         return new ResourceLocation("woodland_mansion/" + param0);
      }

      private static StructurePlaceSettings makeSettings(Mirror param0, Rotation param1) {
         return new StructurePlaceSettings().setIgnoreEntities(true).setRotation(param1).setMirror(param0).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putString("Rot", this.placeSettings.getRotation().name());
         param1.putString("Mi", this.placeSettings.getMirror().name());
      }

      @Override
      protected void handleDataMarker(String param0, BlockPos param1, ServerLevelAccessor param2, RandomSource param3, BoundingBox param4) {
         if (param0.startsWith("Chest")) {
            Rotation var6 = this.placeSettings.getRotation();
            BlockState var7 = Blocks.CHEST.defaultBlockState();
            if ("ChestWest".equals(param0)) {
               var7 = var7.setValue(ChestBlock.FACING, var6.rotate(Direction.WEST));
            } else if ("ChestEast".equals(param0)) {
               var7 = var7.setValue(ChestBlock.FACING, var6.rotate(Direction.EAST));
            } else if ("ChestSouth".equals(param0)) {
               var7 = var7.setValue(ChestBlock.FACING, var6.rotate(Direction.SOUTH));
            } else if ("ChestNorth".equals(param0)) {
               var7 = var7.setValue(ChestBlock.FACING, var6.rotate(Direction.NORTH));
            }

            this.createChest(param2, param4, param3, param1, BuiltInLootTables.WOODLAND_MANSION, var7);
         } else {
            List<Mob> var6 = new ArrayList<>();
            switch (param0) {
               case "Mage":
                  var6.add(EntityType.EVOKER.create(param2.getLevel()));
                  break;
               case "Warrior":
                  var6.add(EntityType.VINDICATOR.create(param2.getLevel()));
                  break;
               case "Group of Allays":
                  int var9 = param2.getRandom().nextInt(3) + 1;

                  for (int var10 = 0; var10 < var9; var10++) {
                     var6.add(EntityType.ALLAY.create(param2.getLevel()));
                  }
                  break;
               default:
                  return;
            }

            for (Mob var8 : var6) {
               if (var8 != null) {
                  var8.setPersistenceRequired();
                  var8.moveTo(param1, 0.0F, 0.0F);
                  var8.finalizeSpawn(param2, param2.getCurrentDifficultyAt(var8.blockPosition()), MobSpawnType.STRUCTURE, null, null);
                  param2.addFreshEntityWithPassengers(var8);
                  param2.setBlock(param1, Blocks.AIR.defaultBlockState(), 2);
               }
            }
         }
      }
   }
}
