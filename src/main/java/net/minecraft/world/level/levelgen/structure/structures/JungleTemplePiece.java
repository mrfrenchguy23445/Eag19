package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class JungleTemplePiece extends ScatteredFeaturePiece {
   public static final int WIDTH = 12;
   public static final int DEPTH = 15;
   private boolean placedMainChest;
   private boolean placedHiddenChest;
   private boolean placedTrap1;
   private boolean placedTrap2;
   private static final JungleTemplePiece.MossStoneSelector STONE_SELECTOR = new JungleTemplePiece.MossStoneSelector();

   public JungleTemplePiece(RandomSource param0, int param1, int param2) {
      super(StructurePieceType.JUNGLE_PYRAMID_PIECE, param1, 64, param2, 12, 10, 15, getRandomHorizontalDirection(param0));
   }

   public JungleTemplePiece(CompoundTag param0) {
      super(StructurePieceType.JUNGLE_PYRAMID_PIECE, param0);
      this.placedMainChest = param0.getBoolean("placedMainChest");
      this.placedHiddenChest = param0.getBoolean("placedHiddenChest");
      this.placedTrap1 = param0.getBoolean("placedTrap1");
      this.placedTrap2 = param0.getBoolean("placedTrap2");
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
      super.addAdditionalSaveData(param0, param1);
      param1.putBoolean("placedMainChest", this.placedMainChest);
      param1.putBoolean("placedHiddenChest", this.placedHiddenChest);
      param1.putBoolean("placedTrap1", this.placedTrap1);
      param1.putBoolean("placedTrap2", this.placedTrap2);
   }

   @Override
   public void postProcess(
      WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
   ) {
      if (this.updateAverageGroundHeight(param0, param4, 0)) {
         this.generateBox(param0, param4, 0, -4, 0, this.width - 1, 0, this.depth - 1, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 2, 1, 2, 9, 2, 2, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 2, 1, 12, 9, 2, 12, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 2, 1, 3, 2, 2, 11, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 9, 1, 3, 9, 2, 11, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 1, 3, 1, 10, 6, 1, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 1, 3, 13, 10, 6, 13, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 1, 3, 2, 1, 6, 12, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 10, 3, 2, 10, 6, 12, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 2, 3, 2, 9, 3, 12, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 2, 6, 2, 9, 6, 12, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 3, 7, 3, 8, 7, 11, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 8, 4, 7, 8, 10, false, param3, STONE_SELECTOR);
         this.generateAirBox(param0, param4, 3, 1, 3, 8, 2, 11);
         this.generateAirBox(param0, param4, 4, 3, 6, 7, 3, 9);
         this.generateAirBox(param0, param4, 2, 4, 2, 9, 5, 12);
         this.generateAirBox(param0, param4, 4, 6, 5, 7, 6, 9);
         this.generateAirBox(param0, param4, 5, 7, 6, 6, 7, 8);
         this.generateAirBox(param0, param4, 5, 1, 2, 6, 2, 2);
         this.generateAirBox(param0, param4, 5, 2, 12, 6, 2, 12);
         this.generateAirBox(param0, param4, 5, 5, 1, 6, 5, 1);
         this.generateAirBox(param0, param4, 5, 5, 13, 6, 5, 13);
         this.placeBlock(param0, Blocks.AIR.defaultBlockState(), 1, 5, 5, param4);
         this.placeBlock(param0, Blocks.AIR.defaultBlockState(), 10, 5, 5, param4);
         this.placeBlock(param0, Blocks.AIR.defaultBlockState(), 1, 5, 9, param4);
         this.placeBlock(param0, Blocks.AIR.defaultBlockState(), 10, 5, 9, param4);

         for (int var8 = 0; var8 <= 14; var8 += 14) {
            this.generateBox(param0, param4, 2, 4, var8, 2, 5, var8, false, param3, STONE_SELECTOR);
            this.generateBox(param0, param4, 4, 4, var8, 4, 5, var8, false, param3, STONE_SELECTOR);
            this.generateBox(param0, param4, 7, 4, var8, 7, 5, var8, false, param3, STONE_SELECTOR);
            this.generateBox(param0, param4, 9, 4, var8, 9, 5, var8, false, param3, STONE_SELECTOR);
         }

         this.generateBox(param0, param4, 5, 6, 0, 6, 6, 0, false, param3, STONE_SELECTOR);

         for (int var8 = 0; var8 <= 11; var8 += 11) {
            for (int var9 = 2; var9 <= 12; var9 += 2) {
               this.generateBox(param0, param4, var8, 4, var9, var8, 5, var9, false, param3, STONE_SELECTOR);
            }

            this.generateBox(param0, param4, var8, 6, 5, var8, 6, 5, false, param3, STONE_SELECTOR);
            this.generateBox(param0, param4, var8, 6, 9, var8, 6, 9, false, param3, STONE_SELECTOR);
         }

         this.generateBox(param0, param4, 2, 7, 2, 2, 9, 2, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 9, 7, 2, 9, 9, 2, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 2, 7, 12, 2, 9, 12, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 9, 7, 12, 9, 9, 12, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 9, 4, 4, 9, 4, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 7, 9, 4, 7, 9, 4, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 9, 10, 4, 9, 10, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 7, 9, 10, 7, 9, 10, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 5, 9, 7, 6, 9, 7, false, param3, STONE_SELECTOR);
         BlockState var8 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
         BlockState var9 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
         BlockState var10 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         BlockState var11 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         this.placeBlock(param0, var11, 5, 9, 6, param4);
         this.placeBlock(param0, var11, 6, 9, 6, param4);
         this.placeBlock(param0, var10, 5, 9, 8, param4);
         this.placeBlock(param0, var10, 6, 9, 8, param4);
         this.placeBlock(param0, var11, 4, 0, 0, param4);
         this.placeBlock(param0, var11, 5, 0, 0, param4);
         this.placeBlock(param0, var11, 6, 0, 0, param4);
         this.placeBlock(param0, var11, 7, 0, 0, param4);
         this.placeBlock(param0, var11, 4, 1, 8, param4);
         this.placeBlock(param0, var11, 4, 2, 9, param4);
         this.placeBlock(param0, var11, 4, 3, 10, param4);
         this.placeBlock(param0, var11, 7, 1, 8, param4);
         this.placeBlock(param0, var11, 7, 2, 9, param4);
         this.placeBlock(param0, var11, 7, 3, 10, param4);
         this.generateBox(param0, param4, 4, 1, 9, 4, 1, 9, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 7, 1, 9, 7, 1, 9, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 4, 1, 10, 7, 2, 10, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 5, 4, 5, 6, 4, 5, false, param3, STONE_SELECTOR);
         this.placeBlock(param0, var8, 4, 4, 5, param4);
         this.placeBlock(param0, var9, 7, 4, 5, param4);

         for (int var12 = 0; var12 < 4; var12++) {
            this.placeBlock(param0, var10, 5, 0 - var12, 6 + var12, param4);
            this.placeBlock(param0, var10, 6, 0 - var12, 6 + var12, param4);
            this.generateAirBox(param0, param4, 5, 0 - var12, 7 + var12, 6, 0 - var12, 9 + var12);
         }

         this.generateAirBox(param0, param4, 1, -3, 12, 10, -1, 13);
         this.generateAirBox(param0, param4, 1, -3, 1, 3, -1, 13);
         this.generateAirBox(param0, param4, 1, -3, 1, 9, -1, 5);

         for (int var12 = 1; var12 <= 13; var12 += 2) {
            this.generateBox(param0, param4, 1, -3, var12, 1, -2, var12, false, param3, STONE_SELECTOR);
         }

         for (int var12 = 2; var12 <= 12; var12 += 2) {
            this.generateBox(param0, param4, 1, -1, var12, 3, -1, var12, false, param3, STONE_SELECTOR);
         }

         this.generateBox(param0, param4, 2, -2, 1, 5, -2, 1, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 7, -2, 1, 9, -2, 1, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 6, -3, 1, 6, -3, 1, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 6, -1, 1, 6, -1, 1, false, param3, STONE_SELECTOR);
         this.placeBlock(
            param0,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.EAST).setValue(TripWireHookBlock.ATTACHED, true),
            1,
            -3,
            8,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.WEST).setValue(TripWireHookBlock.ATTACHED, true),
            4,
            -3,
            8,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.EAST, true).setValue(TripWireBlock.WEST, true).setValue(TripWireBlock.ATTACHED, true),
            2,
            -3,
            8,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.EAST, true).setValue(TripWireBlock.WEST, true).setValue(TripWireBlock.ATTACHED, true),
            3,
            -3,
            8,
            param4
         );
         BlockState var12 = Blocks.REDSTONE_WIRE
            .defaultBlockState()
            .setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE)
            .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE);
         this.placeBlock(param0, var12, 5, -3, 7, param4);
         this.placeBlock(param0, var12, 5, -3, 6, param4);
         this.placeBlock(param0, var12, 5, -3, 5, param4);
         this.placeBlock(param0, var12, 5, -3, 4, param4);
         this.placeBlock(param0, var12, 5, -3, 3, param4);
         this.placeBlock(param0, var12, 5, -3, 2, param4);
         this.placeBlock(
            param0,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            5,
            -3,
            1,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            4,
            -3,
            1,
            param4
         );
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3, -3, 1, param4);
         if (!this.placedTrap1) {
            this.placedTrap1 = this.createDispenser(param0, param4, param3, 3, -2, 1, Direction.NORTH, BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER);
         }

         this.placeBlock(param0, Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true), 3, -2, 2, param4);
         this.placeBlock(
            param0,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.NORTH).setValue(TripWireHookBlock.ATTACHED, true),
            7,
            -3,
            1,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.SOUTH).setValue(TripWireHookBlock.ATTACHED, true),
            7,
            -3,
            5,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true).setValue(TripWireBlock.ATTACHED, true),
            7,
            -3,
            2,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true).setValue(TripWireBlock.ATTACHED, true),
            7,
            -3,
            3,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true).setValue(TripWireBlock.ATTACHED, true),
            7,
            -3,
            4,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            8,
            -3,
            6,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE),
            9,
            -3,
            6,
            param4
         );
         this.placeBlock(
            param0,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.UP),
            9,
            -3,
            5,
            param4
         );
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 9, -3, 4, param4);
         this.placeBlock(param0, var12, 9, -2, 4, param4);
         if (!this.placedTrap2) {
            this.placedTrap2 = this.createDispenser(param0, param4, param3, 9, -2, 3, Direction.WEST, BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER);
         }

         this.placeBlock(param0, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true), 8, -1, 3, param4);
         this.placeBlock(param0, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true), 8, -2, 3, param4);
         if (!this.placedMainChest) {
            this.placedMainChest = this.createChest(param0, param4, param3, 8, -3, 3, BuiltInLootTables.JUNGLE_TEMPLE);
         }

         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 9, -3, 2, param4);
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 8, -3, 1, param4);
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 4, -3, 5, param4);
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 5, -2, 5, param4);
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 5, -1, 5, param4);
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 6, -3, 5, param4);
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 7, -2, 5, param4);
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 7, -1, 5, param4);
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 8, -3, 5, param4);
         this.generateBox(param0, param4, 9, -1, 1, 9, -1, 5, false, param3, STONE_SELECTOR);
         this.generateAirBox(param0, param4, 8, -3, 8, 10, -1, 10);
         this.placeBlock(param0, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 8, -2, 11, param4);
         this.placeBlock(param0, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 9, -2, 11, param4);
         this.placeBlock(param0, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 10, -2, 11, param4);
         BlockState var13 = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACING, Direction.NORTH).setValue(LeverBlock.FACE, AttachFace.WALL);
         this.placeBlock(param0, var13, 8, -2, 12, param4);
         this.placeBlock(param0, var13, 9, -2, 12, param4);
         this.placeBlock(param0, var13, 10, -2, 12, param4);
         this.generateBox(param0, param4, 8, -3, 8, 8, -3, 10, false, param3, STONE_SELECTOR);
         this.generateBox(param0, param4, 10, -3, 8, 10, -3, 10, false, param3, STONE_SELECTOR);
         this.placeBlock(param0, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 10, -2, 9, param4);
         this.placeBlock(param0, var12, 8, -2, 9, param4);
         this.placeBlock(param0, var12, 8, -2, 10, param4);
         this.placeBlock(
            param0,
            Blocks.REDSTONE_WIRE
               .defaultBlockState()
               .setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE)
               .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE)
               .setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE)
               .setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            10,
            -1,
            9,
            param4
         );
         this.placeBlock(param0, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 9, -2, 8, param4);
         this.placeBlock(param0, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), 10, -2, 8, param4);
         this.placeBlock(param0, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), 10, -1, 8, param4);
         this.placeBlock(param0, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.NORTH), 10, -2, 10, param4);
         if (!this.placedHiddenChest) {
            this.placedHiddenChest = this.createChest(param0, param4, param3, 9, -3, 10, BuiltInLootTables.JUNGLE_TEMPLE);
         }
      }
   }

   static class MossStoneSelector extends StructurePiece.BlockSelector {
      @Override
      public void next(RandomSource param0, int param1, int param2, int param3, boolean param4) {
         if (param0.nextFloat() < 0.4F) {
            this.next = Blocks.COBBLESTONE.defaultBlockState();
         } else {
            this.next = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
         }
      }
   }
}
