package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

public abstract class StructurePiece {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
   protected BoundingBox boundingBox;
   @Nullable
   private Direction orientation;
   private Mirror mirror;
   private Rotation rotation;
   protected int genDepth;
   private final StructurePieceType type;
   private static final Set<Block> SHAPE_CHECK_BLOCKS = ImmutableSet.<Block>builder()
      .add(Blocks.NETHER_BRICK_FENCE)
      .add(Blocks.TORCH)
      .add(Blocks.WALL_TORCH)
      .add(Blocks.OAK_FENCE)
      .add(Blocks.SPRUCE_FENCE)
      .add(Blocks.DARK_OAK_FENCE)
      .add(Blocks.ACACIA_FENCE)
      .add(Blocks.BIRCH_FENCE)
      .add(Blocks.JUNGLE_FENCE)
      .add(Blocks.LADDER)
      .add(Blocks.IRON_BARS)
      .build();

   protected StructurePiece(StructurePieceType param0, int param1, BoundingBox param2) {
      this.type = param0;
      this.genDepth = param1;
      this.boundingBox = param2;
   }

   public StructurePiece(StructurePieceType param0, CompoundTag param1) {
      this(
         param0,
         param1.getInt("GD"),
         BoundingBox.CODEC
            .parse(NbtOps.INSTANCE, param1.get("BB"))
            .resultOrPartial(LOGGER::error)
            .orElseThrow(() -> new IllegalArgumentException("Invalid boundingbox"))
      );
      int var3 = param1.getInt("O");
      this.setOrientation(var3 == -1 ? null : Direction.from2DDataValue(var3));
   }

   protected static BoundingBox makeBoundingBox(int param0, int param1, int param2, Direction param3, int param4, int param5, int param6) {
      return param3.getAxis() == Direction.Axis.Z
         ? new BoundingBox(param0, param1, param2, param0 + param4 - 1, param1 + param5 - 1, param2 + param6 - 1)
         : new BoundingBox(param0, param1, param2, param0 + param6 - 1, param1 + param5 - 1, param2 + param4 - 1);
   }

   protected static Direction getRandomHorizontalDirection(RandomSource param0) {
      return Direction.Plane.HORIZONTAL.getRandomDirection(param0);
   }

   public final CompoundTag createTag(StructurePieceSerializationContext param0) {
      CompoundTag var2 = new CompoundTag();
      var2.putString("id", BuiltInRegistries.STRUCTURE_PIECE.getKey(this.getType()).toString());
      BoundingBox.CODEC.encodeStart(NbtOps.INSTANCE, this.boundingBox).resultOrPartial(LOGGER::error).ifPresent(l_param1 -> var2.put("BB", l_param1));
      Direction var3 = this.getOrientation();
      var2.putInt("O", var3 == null ? -1 : var3.get2DDataValue());
      var2.putInt("GD", this.genDepth);
      this.addAdditionalSaveData(param0, var2);
      return var2;
   }

   protected abstract void addAdditionalSaveData(StructurePieceSerializationContext var1, CompoundTag var2);

   public void addChildren(StructurePiece param0, StructurePieceAccessor param1, RandomSource param2) {
   }

   public abstract void postProcess(
      WorldGenLevel var1, StructureManager var2, ChunkGenerator var3, RandomSource var4, BoundingBox var5, ChunkPos var6, BlockPos var7
   );

   public BoundingBox getBoundingBox() {
      return this.boundingBox;
   }

   public int getGenDepth() {
      return this.genDepth;
   }

   public void setGenDepth(int param0) {
      this.genDepth = param0;
   }

   public boolean isCloseToChunk(ChunkPos param0, int param1) {
      int var3 = param0.getMinBlockX();
      int var4 = param0.getMinBlockZ();
      return this.boundingBox.intersects(var3 - param1, var4 - param1, var3 + 15 + param1, var4 + 15 + param1);
   }

   public BlockPos getLocatorPosition() {
      return new BlockPos(this.boundingBox.getCenter());
   }

   protected BlockPos.MutableBlockPos getWorldPos(int param0, int param1, int param2) {
      return new BlockPos.MutableBlockPos(this.getWorldX(param0, param2), this.getWorldY(param1), this.getWorldZ(param0, param2));
   }

   protected int getWorldX(int param0, int param1) {
      Direction var3 = this.getOrientation();
      if (var3 == null) {
         return param0;
      }

      switch (var3) {
         case NORTH:
         case SOUTH:
            return this.boundingBox.minX() + param0;
         case WEST:
            return this.boundingBox.maxX() - param1;
         case EAST:
            return this.boundingBox.minX() + param1;
         default:
            return param0;
      }
   }

   protected int getWorldY(int param0) {
      return this.getOrientation() == null ? param0 : param0 + this.boundingBox.minY();
   }

   protected int getWorldZ(int param0, int param1) {
      Direction var3 = this.getOrientation();
      if (var3 == null) {
         return param1;
      }

      switch (var3) {
         case NORTH:
            return this.boundingBox.maxZ() - param1;
         case SOUTH:
            return this.boundingBox.minZ() + param1;
         case WEST:
         case EAST:
            return this.boundingBox.minZ() + param0;
         default:
            return param1;
      }
   }

   protected void placeBlock(WorldGenLevel param0, BlockState param1, int param2, int param3, int param4, BoundingBox param5) {
      BlockPos var7 = this.getWorldPos(param2, param3, param4);
      if (param5.isInside(var7)) {
         if (this.canBeReplaced(param0, param2, param3, param4, param5)) {
            if (this.mirror != Mirror.NONE) {
               param1 = param1.mirror(this.mirror);
            }

            if (this.rotation != Rotation.NONE) {
               param1 = param1.rotate(this.rotation);
            }

            param0.setBlock(var7, param1, 2);
            FluidState var8 = param0.getFluidState(var7);
            if (!var8.isEmpty()) {
               param0.scheduleTick(var7, var8.getType(), 0);
            }

            if (SHAPE_CHECK_BLOCKS.contains(param1.getBlock())) {
               param0.getChunk(var7).markPosForPostprocessing(var7);
            }
         }
      }
   }

   protected boolean canBeReplaced(LevelReader param0, int param1, int param2, int param3, BoundingBox param4) {
      return true;
   }

   protected BlockState getBlock(BlockGetter param0, int param1, int param2, int param3, BoundingBox param4) {
      BlockPos var6 = this.getWorldPos(param1, param2, param3);
      return !param4.isInside(var6) ? Blocks.AIR.defaultBlockState() : param0.getBlockState(var6);
   }

   protected boolean isInterior(LevelReader param0, int param1, int param2, int param3, BoundingBox param4) {
      BlockPos var6 = this.getWorldPos(param1, param2 + 1, param3);
      return !param4.isInside(var6) ? false : var6.getY() < param0.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, var6.getX(), var6.getZ());
   }

   protected void generateAirBox(WorldGenLevel param0, BoundingBox param1, int param2, int param3, int param4, int param5, int param6, int param7) {
      for (int var9 = param3; var9 <= param6; var9++) {
         for (int var10 = param2; var10 <= param5; var10++) {
            for (int var11 = param4; var11 <= param7; var11++) {
               this.placeBlock(param0, Blocks.AIR.defaultBlockState(), var10, var9, var11, param1);
            }
         }
      }
   }

   protected void generateBox(
      WorldGenLevel param0,
      BoundingBox param1,
      int param2,
      int param3,
      int param4,
      int param5,
      int param6,
      int param7,
      BlockState param8,
      BlockState param9,
      boolean param10
   ) {
      for (int var12 = param3; var12 <= param6; var12++) {
         for (int var13 = param2; var13 <= param5; var13++) {
            for (int var14 = param4; var14 <= param7; var14++) {
               if (!param10 || !this.getBlock(param0, var13, var12, var14, param1).isAir()) {
                  if (var12 != param3 && var12 != param6 && var13 != param2 && var13 != param5 && var14 != param4 && var14 != param7) {
                     this.placeBlock(param0, param9, var13, var12, var14, param1);
                  } else {
                     this.placeBlock(param0, param8, var13, var12, var14, param1);
                  }
               }
            }
         }
      }
   }

   protected void generateBox(WorldGenLevel param0, BoundingBox param1, BoundingBox param2, BlockState param3, BlockState param4, boolean param5) {
      this.generateBox(param0, param1, param2.minX(), param2.minY(), param2.minZ(), param2.maxX(), param2.maxY(), param2.maxZ(), param3, param4, param5);
   }

   protected void generateBox(
      WorldGenLevel param0,
      BoundingBox param1,
      int param2,
      int param3,
      int param4,
      int param5,
      int param6,
      int param7,
      boolean param8,
      RandomSource param9,
      StructurePiece.BlockSelector param10
   ) {
      for (int var12 = param3; var12 <= param6; var12++) {
         for (int var13 = param2; var13 <= param5; var13++) {
            for (int var14 = param4; var14 <= param7; var14++) {
               if (!param8 || !this.getBlock(param0, var13, var12, var14, param1).isAir()) {
                  param10.next(
                     param9,
                     var13,
                     var12,
                     var14,
                     var12 == param3 || var12 == param6 || var13 == param2 || var13 == param5 || var14 == param4 || var14 == param7
                  );
                  this.placeBlock(param0, param10.getNext(), var13, var12, var14, param1);
               }
            }
         }
      }
   }

   protected void generateBox(
      WorldGenLevel param0, BoundingBox param1, BoundingBox param2, boolean param3, RandomSource param4, StructurePiece.BlockSelector param5
   ) {
      this.generateBox(param0, param1, param2.minX(), param2.minY(), param2.minZ(), param2.maxX(), param2.maxY(), param2.maxZ(), param3, param4, param5);
   }

   protected void generateMaybeBox(
      WorldGenLevel param0,
      BoundingBox param1,
      RandomSource param2,
      float param3,
      int param4,
      int param5,
      int param6,
      int param7,
      int param8,
      int param9,
      BlockState param10,
      BlockState param11,
      boolean param12,
      boolean param13
   ) {
      for (int var15 = param5; var15 <= param8; var15++) {
         for (int var16 = param4; var16 <= param7; var16++) {
            for (int var17 = param6; var17 <= param9; var17++) {
               if (!(param2.nextFloat() > param3)
                  && (!param12 || !this.getBlock(param0, var16, var15, var17, param1).isAir())
                  && (!param13 || this.isInterior(param0, var16, var15, var17, param1))) {
                  if (var15 != param5 && var15 != param8 && var16 != param4 && var16 != param7 && var17 != param6 && var17 != param9) {
                     this.placeBlock(param0, param11, var16, var15, var17, param1);
                  } else {
                     this.placeBlock(param0, param10, var16, var15, var17, param1);
                  }
               }
            }
         }
      }
   }

   protected void maybeGenerateBlock(
      WorldGenLevel param0, BoundingBox param1, RandomSource param2, float param3, int param4, int param5, int param6, BlockState param7
   ) {
      if (param2.nextFloat() < param3) {
         this.placeBlock(param0, param7, param4, param5, param6, param1);
      }
   }

   protected void generateUpperHalfSphere(
      WorldGenLevel param0, BoundingBox param1, int param2, int param3, int param4, int param5, int param6, int param7, BlockState param8, boolean param9
   ) {
      float var11 = param5 - param2 + 1;
      float var12 = param6 - param3 + 1;
      float var13 = param7 - param4 + 1;
      float var14 = param2 + var11 / 2.0F;
      float var15 = param4 + var13 / 2.0F;

      for (int var16 = param3; var16 <= param6; var16++) {
         float var17 = (var16 - param3) / var12;

         for (int var18 = param2; var18 <= param5; var18++) {
            float var19 = (var18 - var14) / (var11 * 0.5F);

            for (int var20 = param4; var20 <= param7; var20++) {
               float var21 = (var20 - var15) / (var13 * 0.5F);
               if (!param9 || !this.getBlock(param0, var18, var16, var20, param1).isAir()) {
                  float var22 = var19 * var19 + var17 * var17 + var21 * var21;
                  if (var22 <= 1.05F) {
                     this.placeBlock(param0, param8, var18, var16, var20, param1);
                  }
               }
            }
         }
      }
   }

   protected void fillColumnDown(WorldGenLevel param0, BlockState param1, int param2, int param3, int param4, BoundingBox param5) {
      BlockPos.MutableBlockPos var7 = this.getWorldPos(param2, param3, param4);
      if (param5.isInside(var7)) {
         while (this.isReplaceableByStructures(param0.getBlockState(var7)) && var7.getY() > param0.getMinBuildHeight() + 1) {
            param0.setBlock(var7, param1, 2);
            var7.move(Direction.DOWN);
         }
      }
   }

   protected boolean isReplaceableByStructures(BlockState param0) {
      return param0.isAir()
         || param0.getMaterial().isLiquid()
         || param0.is(Blocks.GLOW_LICHEN)
         || param0.is(Blocks.SEAGRASS)
         || param0.is(Blocks.TALL_SEAGRASS);
   }

   protected boolean createChest(WorldGenLevel param0, BoundingBox param1, RandomSource param2, int param3, int param4, int param5, ResourceLocation param6) {
      return this.createChest(param0, param1, param2, this.getWorldPos(param3, param4, param5), param6, null);
   }

   public static BlockState reorient(BlockGetter param0, BlockPos param1, BlockState param2) {
      Direction var3 = null;

      for (Direction var5 : Direction.Plane.HORIZONTAL) {
         BlockPos var6 = param1.relative(var5);
         BlockState var7 = param0.getBlockState(var6);
         if (var7.is(Blocks.CHEST)) {
            return param2;
         }

         if (var7.isSolidRender(param0, var6)) {
            if (var3 != null) {
               var3 = null;
               break;
            }

            var3 = var5;
         }
      }

      if (var3 != null) {
         return param2.setValue(HorizontalDirectionalBlock.FACING, var3.getOpposite());
      }

      Direction var4 = param2.getValue(HorizontalDirectionalBlock.FACING);
      BlockPos var5 = param1.relative(var4);
      if (param0.getBlockState(var5).isSolidRender(param0, var5)) {
         var4 = var4.getOpposite();
         var5 = param1.relative(var4);
      }

      if (param0.getBlockState(var5).isSolidRender(param0, var5)) {
         var4 = var4.getClockWise();
         var5 = param1.relative(var4);
      }

      if (param0.getBlockState(var5).isSolidRender(param0, var5)) {
         var4 = var4.getOpposite();
         var5 = param1.relative(var4);
      }

      return param2.setValue(HorizontalDirectionalBlock.FACING, var4);
   }

   protected boolean createChest(
      ServerLevelAccessor param0, BoundingBox param1, RandomSource param2, BlockPos param3, ResourceLocation param4, @Nullable BlockState param5
   ) {
      if (param1.isInside(param3) && !param0.getBlockState(param3).is(Blocks.CHEST)) {
         if (param5 == null) {
            param5 = reorient(param0, param3, Blocks.CHEST.defaultBlockState());
         }

         param0.setBlock(param3, param5, 2);
         BlockEntity var7 = param0.getBlockEntity(param3);
         if (var7 instanceof ChestBlockEntity) {
            ((ChestBlockEntity)var7).setLootTable(param4, param2.nextLong());
         }

         return true;
      } else {
         return false;
      }
   }

   protected boolean createDispenser(
      WorldGenLevel param0, BoundingBox param1, RandomSource param2, int param3, int param4, int param5, Direction param6, ResourceLocation param7
   ) {
      BlockPos var9 = this.getWorldPos(param3, param4, param5);
      if (param1.isInside(var9) && !param0.getBlockState(var9).is(Blocks.DISPENSER)) {
         this.placeBlock(param0, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, param6), param3, param4, param5, param1);
         BlockEntity var10 = param0.getBlockEntity(var9);
         if (var10 instanceof DispenserBlockEntity) {
            ((DispenserBlockEntity)var10).setLootTable(param7, param2.nextLong());
         }

         return true;
      } else {
         return false;
      }
   }

   public void move(int param0, int param1, int param2) {
      this.boundingBox.move(param0, param1, param2);
   }

   public static BoundingBox createBoundingBox(Stream<StructurePiece> param0) {
      return BoundingBox.encapsulatingBoxes(param0.map(StructurePiece::getBoundingBox)::iterator)
         .orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox without pieces"));
   }

   @Nullable
   public static StructurePiece findCollisionPiece(List<StructurePiece> param0, BoundingBox param1) {
      for (StructurePiece var3 : param0) {
         if (var3.getBoundingBox().intersects(param1)) {
            return var3;
         }
      }

      return null;
   }

   @Nullable
   public Direction getOrientation() {
      return this.orientation;
   }

   public void setOrientation(@Nullable Direction param0) {
      this.orientation = param0;
      if (param0 == null) {
         this.rotation = Rotation.NONE;
         this.mirror = Mirror.NONE;
      } else {
         switch (param0) {
            case SOUTH:
               this.mirror = Mirror.LEFT_RIGHT;
               this.rotation = Rotation.NONE;
               break;
            case WEST:
               this.mirror = Mirror.LEFT_RIGHT;
               this.rotation = Rotation.CLOCKWISE_90;
               break;
            case EAST:
               this.mirror = Mirror.NONE;
               this.rotation = Rotation.CLOCKWISE_90;
               break;
            default:
               this.mirror = Mirror.NONE;
               this.rotation = Rotation.NONE;
         }
      }
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public StructurePieceType getType() {
      return this.type;
   }

   public abstract static class BlockSelector {
      protected BlockState next = Blocks.AIR.defaultBlockState();

      public abstract void next(RandomSource var1, int var2, int var3, int var4, boolean var5);

      public BlockState getNext() {
         return this.next;
      }
   }
}
