package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WalkNodeEvaluator extends NodeEvaluator {
   public static final double SPACE_BETWEEN_WALL_POSTS = 0.5;
   private static final double DEFAULT_MOB_JUMP_HEIGHT = 1.125;
   protected float oldWaterCost;
   private final Long2ObjectMap<BlockPathTypes> pathTypesByPosCache = new Long2ObjectOpenHashMap<>();
   private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap<>();

   @Override
   public void prepare(PathNavigationRegion param0, Mob param1) {
      super.prepare(param0, param1);
      this.oldWaterCost = param1.getPathfindingMalus(BlockPathTypes.WATER);
   }

   @Override
   public void done() {
      this.mob.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
      this.pathTypesByPosCache.clear();
      this.collisionCache.clear();
      super.done();
   }

   @Override
   public Node getStart() {
      BlockPos.MutableBlockPos var2 = new BlockPos.MutableBlockPos();
      int var1 = this.mob.getBlockY();
      BlockState var3 = this.level.getBlockState(var2.set(this.mob.getX(), var1, this.mob.getZ()));
      if (!this.mob.canStandOnFluid(var3.getFluidState())) {
         if (this.canFloat() && this.mob.isInWater()) {
            while (true) {
               if (!var3.is(Blocks.WATER) && var3.getFluidState() != Fluids.WATER.getSource(false)) {
                  var1--;
                  break;
               }

               var3 = this.level.getBlockState(var2.set(this.mob.getX(), ++var1, this.mob.getZ()));
            }
         } else if (this.mob.isOnGround()) {
            var1 = Mth.floor(this.mob.getY() + 0.5);
         } else {
            BlockPos var4 = this.mob.blockPosition();

            while (
               (this.level.getBlockState(var4).isAir() || this.level.getBlockState(var4).isPathfindable(this.level, var4, PathComputationType.LAND))
                  && var4.getY() > this.mob.level.getMinBuildHeight()
            ) {
               var4 = var4.below();
            }

            var1 = var4.above().getY();
         }
      } else {
         while (this.mob.canStandOnFluid(var3.getFluidState())) {
            var3 = this.level.getBlockState(var2.set(this.mob.getX(), ++var1, this.mob.getZ()));
         }

         var1--;
      }

      BlockPos var4 = this.mob.blockPosition();
      if (!this.canStartAt(var2.set(var4.getX(), var1, var4.getZ()))) {
         AABB var5 = this.mob.getBoundingBox();
         if (this.canStartAt(var2.set(var5.minX, var1, var5.minZ))
            || this.canStartAt(var2.set(var5.minX, var1, var5.maxZ))
            || this.canStartAt(var2.set(var5.maxX, var1, var5.minZ))
            || this.canStartAt(var2.set(var5.maxX, var1, var5.maxZ))) {
            return this.getStartNode(var2);
         }
      }

      return this.getStartNode(new BlockPos(var4.getX(), var1, var4.getZ()));
   }

   protected Node getStartNode(BlockPos param0) {
      Node var2 = this.getNode(param0);
      var2.type = this.getBlockPathType(this.mob, var2.asBlockPos());
      var2.costMalus = this.mob.getPathfindingMalus(var2.type);
      return var2;
   }

   protected boolean canStartAt(BlockPos param0) {
      BlockPathTypes var2 = this.getBlockPathType(this.mob, param0);
      return var2 != BlockPathTypes.OPEN && this.mob.getPathfindingMalus(var2) >= 0.0F;
   }

   @Override
   public Target getGoal(double param0, double param2, double param4) {
      return this.getTargetFromNode(this.getNode(Mth.floor(param0), Mth.floor(param2), Mth.floor(param4)));
   }

   @Override
   public int getNeighbors(Node[] param0, Node param1) {
      int var3 = 0;
      int var4 = 0;
      BlockPathTypes var5 = this.getCachedBlockType(this.mob, param1.x, param1.y + 1, param1.z);
      BlockPathTypes var6 = this.getCachedBlockType(this.mob, param1.x, param1.y, param1.z);
      if (this.mob.getPathfindingMalus(var5) >= 0.0F && var6 != BlockPathTypes.STICKY_HONEY) {
         var4 = Mth.floor(Math.max(1.0F, this.mob.maxUpStep()));
      }

      double var7 = this.getFloorLevel(new BlockPos(param1.x, param1.y, param1.z));
      Node var9 = this.findAcceptedNode(param1.x, param1.y, param1.z + 1, var4, var7, Direction.SOUTH, var6);
      if (this.isNeighborValid(var9, param1)) {
         param0[var3++] = var9;
      }

      Node var10 = this.findAcceptedNode(param1.x - 1, param1.y, param1.z, var4, var7, Direction.WEST, var6);
      if (this.isNeighborValid(var10, param1)) {
         param0[var3++] = var10;
      }

      Node var11 = this.findAcceptedNode(param1.x + 1, param1.y, param1.z, var4, var7, Direction.EAST, var6);
      if (this.isNeighborValid(var11, param1)) {
         param0[var3++] = var11;
      }

      Node var12 = this.findAcceptedNode(param1.x, param1.y, param1.z - 1, var4, var7, Direction.NORTH, var6);
      if (this.isNeighborValid(var12, param1)) {
         param0[var3++] = var12;
      }

      Node var13 = this.findAcceptedNode(param1.x - 1, param1.y, param1.z - 1, var4, var7, Direction.NORTH, var6);
      if (this.isDiagonalValid(param1, var10, var12, var13)) {
         param0[var3++] = var13;
      }

      Node var14 = this.findAcceptedNode(param1.x + 1, param1.y, param1.z - 1, var4, var7, Direction.NORTH, var6);
      if (this.isDiagonalValid(param1, var11, var12, var14)) {
         param0[var3++] = var14;
      }

      Node var15 = this.findAcceptedNode(param1.x - 1, param1.y, param1.z + 1, var4, var7, Direction.SOUTH, var6);
      if (this.isDiagonalValid(param1, var10, var9, var15)) {
         param0[var3++] = var15;
      }

      Node var16 = this.findAcceptedNode(param1.x + 1, param1.y, param1.z + 1, var4, var7, Direction.SOUTH, var6);
      if (this.isDiagonalValid(param1, var11, var9, var16)) {
         param0[var3++] = var16;
      }

      return var3;
   }

   protected boolean isNeighborValid(@Nullable Node param0, Node param1) {
      return param0 != null && !param0.closed && (param0.costMalus >= 0.0F || param1.costMalus < 0.0F);
   }

   protected boolean isDiagonalValid(Node param0, @Nullable Node param1, @Nullable Node param2, @Nullable Node param3) {
      if (param3 == null || param2 == null || param1 == null) {
         return false;
      } else if (param3.closed) {
         return false;
      } else if (param2.y > param0.y || param1.y > param0.y) {
         return false;
      } else if (param1.type != BlockPathTypes.WALKABLE_DOOR && param2.type != BlockPathTypes.WALKABLE_DOOR && param3.type != BlockPathTypes.WALKABLE_DOOR) {
         boolean var5 = param2.type == BlockPathTypes.FENCE && param1.type == BlockPathTypes.FENCE && this.mob.getBbWidth() < 0.5;
         return param3.costMalus >= 0.0F
            && (param2.y < param0.y || param2.costMalus >= 0.0F || var5)
            && (param1.y < param0.y || param1.costMalus >= 0.0F || var5);
      } else {
         return false;
      }
   }

   private static boolean doesBlockHavePartialCollision(BlockPathTypes param0) {
      return param0 == BlockPathTypes.FENCE || param0 == BlockPathTypes.DOOR_WOOD_CLOSED || param0 == BlockPathTypes.DOOR_IRON_CLOSED;
   }

   private boolean canReachWithoutCollision(Node param0) {
      AABB var2 = this.mob.getBoundingBox();
      Vec3 var3 = new Vec3(
         param0.x - this.mob.getX() + var2.getXsize() / 2.0,
         param0.y - this.mob.getY() + var2.getYsize() / 2.0,
         param0.z - this.mob.getZ() + var2.getZsize() / 2.0
      );
      int var4 = Mth.ceil(var3.length() / var2.getSize());
      var3 = var3.scale(1.0F / var4);

      for (int var5 = 1; var5 <= var4; var5++) {
         var2 = var2.move(var3);
         if (this.hasCollisions(var2)) {
            return false;
         }
      }

      return true;
   }

   protected double getFloorLevel(BlockPos param0) {
      return (this.canFloat() || this.isAmphibious()) && this.level.getFluidState(param0).is(FluidTags.WATER)
         ? param0.getY() + 0.5
         : getFloorLevel(this.level, param0);
   }

   public static double getFloorLevel(BlockGetter param0, BlockPos param1) {
      BlockPos var2 = param1.below();
      VoxelShape var3 = param0.getBlockState(var2).getCollisionShape(param0, var2);
      return var2.getY() + (var3.isEmpty() ? 0.0 : var3.max(Direction.Axis.Y));
   }

   protected boolean isAmphibious() {
      return false;
   }

   @Nullable
   protected Node findAcceptedNode(int param0, int param1, int param2, int param3, double param4, Direction param6, BlockPathTypes param7) {
      Node var9 = null;
      BlockPos.MutableBlockPos var10 = new BlockPos.MutableBlockPos();
      double var11 = this.getFloorLevel(var10.set(param0, param1, param2));
      if (var11 - param4 > this.getMobJumpHeight()) {
         return null;
      }

      BlockPathTypes var13 = this.getCachedBlockType(this.mob, param0, param1, param2);
      float var14 = this.mob.getPathfindingMalus(var13);
      double var15 = this.mob.getBbWidth() / 2.0;
      if (var14 >= 0.0F) {
         var9 = this.getNodeAndUpdateCostToMax(param0, param1, param2, var13, var14);
      }

      if (doesBlockHavePartialCollision(param7) && var9 != null && var9.costMalus >= 0.0F && !this.canReachWithoutCollision(var9)) {
         var9 = null;
      }

      if (var13 != BlockPathTypes.WALKABLE && (!this.isAmphibious() || var13 != BlockPathTypes.WATER)) {
         if ((var9 == null || var9.costMalus < 0.0F)
            && param3 > 0
            && (var13 != BlockPathTypes.FENCE || this.canWalkOverFences())
            && var13 != BlockPathTypes.UNPASSABLE_RAIL
            && var13 != BlockPathTypes.TRAPDOOR
            && var13 != BlockPathTypes.POWDER_SNOW) {
            var9 = this.findAcceptedNode(param0, param1 + 1, param2, param3 - 1, param4, param6, param7);
            if (var9 != null && (var9.type == BlockPathTypes.OPEN || var9.type == BlockPathTypes.WALKABLE) && this.mob.getBbWidth() < 1.0F) {
               double var17 = param0 - param6.getStepX() + 0.5;
               double var19 = param2 - param6.getStepZ() + 0.5;
               AABB var21 = new AABB(
                  var17 - var15,
                  this.getFloorLevel(var10.set(var17, param1 + 1, var19)) + 0.001,
                  var19 - var15,
                  var17 + var15,
                  this.mob.getBbHeight() + this.getFloorLevel(var10.set((double)var9.x, (double)var9.y, (double)var9.z)) - 0.002,
                  var19 + var15
               );
               if (this.hasCollisions(var21)) {
                  var9 = null;
               }
            }
         }

         if (!this.isAmphibious() && var13 == BlockPathTypes.WATER && !this.canFloat()) {
            if (this.getCachedBlockType(this.mob, param0, param1 - 1, param2) != BlockPathTypes.WATER) {
               return var9;
            }

            while (param1 > this.mob.level.getMinBuildHeight()) {
               var13 = this.getCachedBlockType(this.mob, param0, --param1, param2);
               if (var13 != BlockPathTypes.WATER) {
                  return var9;
               }

               var9 = this.getNodeAndUpdateCostToMax(param0, param1, param2, var13, this.mob.getPathfindingMalus(var13));
            }
         }

         if (var13 == BlockPathTypes.OPEN) {
            int var17 = 0;
            int var18 = param1;

            while (var13 == BlockPathTypes.OPEN) {
               if (--param1 < this.mob.level.getMinBuildHeight()) {
                  return this.getBlockedNode(param0, var18, param2);
               }

               if (var17++ >= this.mob.getMaxFallDistance()) {
                  return this.getBlockedNode(param0, param1, param2);
               }

               var13 = this.getCachedBlockType(this.mob, param0, param1, param2);
               var14 = this.mob.getPathfindingMalus(var13);
               if (var13 != BlockPathTypes.OPEN && var14 >= 0.0F) {
                  var9 = this.getNodeAndUpdateCostToMax(param0, param1, param2, var13, var14);
                  break;
               }

               if (var14 < 0.0F) {
                  return this.getBlockedNode(param0, param1, param2);
               }
            }
         }

         if (doesBlockHavePartialCollision(var13) && var9 == null) {
            var9 = this.getNode(param0, param1, param2);
            var9.closed = true;
            var9.type = var13;
            var9.costMalus = var13.getMalus();
         }

         return var9;
      } else {
         return var9;
      }
   }

   private double getMobJumpHeight() {
      return Math.max(1.125, this.mob.maxUpStep());
   }

   private Node getNodeAndUpdateCostToMax(int param0, int param1, int param2, BlockPathTypes param3, float param4) {
      Node var6 = this.getNode(param0, param1, param2);
      var6.type = param3;
      var6.costMalus = Math.max(var6.costMalus, param4);
      return var6;
   }

   private Node getBlockedNode(int param0, int param1, int param2) {
      Node var4 = this.getNode(param0, param1, param2);
      var4.type = BlockPathTypes.BLOCKED;
      var4.costMalus = -1.0F;
      return var4;
   }

   private boolean hasCollisions(AABB param0) {
      return this.collisionCache.computeIfAbsent(param0, l_param1 -> !this.level.noCollision(this.mob, param0));
   }

   @Override
   public BlockPathTypes getBlockPathType(BlockGetter param0, int param1, int param2, int param3, Mob param4) {
      EnumSet<BlockPathTypes> var6 = EnumSet.noneOf(BlockPathTypes.class);
      BlockPathTypes var7 = BlockPathTypes.BLOCKED;
      var7 = this.getBlockPathTypes(param0, param1, param2, param3, var6, var7, param4.blockPosition());
      if (var6.contains(BlockPathTypes.FENCE)) {
         return BlockPathTypes.FENCE;
      }

      if (var6.contains(BlockPathTypes.UNPASSABLE_RAIL)) {
         return BlockPathTypes.UNPASSABLE_RAIL;
      }

      BlockPathTypes var8 = BlockPathTypes.BLOCKED;

      for (BlockPathTypes var10 : var6) {
         if (param4.getPathfindingMalus(var10) < 0.0F) {
            return var10;
         }

         if (param4.getPathfindingMalus(var10) >= param4.getPathfindingMalus(var8)) {
            var8 = var10;
         }
      }

      return var7 == BlockPathTypes.OPEN && param4.getPathfindingMalus(var8) == 0.0F && this.entityWidth <= 1 ? BlockPathTypes.OPEN : var8;
   }

   public BlockPathTypes getBlockPathTypes(
      BlockGetter param0, int param1, int param2, int param3, EnumSet<BlockPathTypes> param4, BlockPathTypes param5, BlockPos param6
   ) {
      for (int var8 = 0; var8 < this.entityWidth; var8++) {
         for (int var9 = 0; var9 < this.entityHeight; var9++) {
            for (int var10 = 0; var10 < this.entityDepth; var10++) {
               int var11 = var8 + param1;
               int var12 = var9 + param2;
               int var13 = var10 + param3;
               BlockPathTypes var14 = this.getBlockPathType(param0, var11, var12, var13);
               var14 = this.evaluateBlockPathType(param0, param6, var14);
               if (var8 == 0 && var9 == 0 && var10 == 0) {
                  param5 = var14;
               }

               param4.add(var14);
            }
         }
      }

      return param5;
   }

   protected BlockPathTypes evaluateBlockPathType(BlockGetter param0, BlockPos param1, BlockPathTypes param2) {
      boolean var4 = this.canPassDoors();
      if (param2 == BlockPathTypes.DOOR_WOOD_CLOSED && this.canOpenDoors() && var4) {
         param2 = BlockPathTypes.WALKABLE_DOOR;
      }

      if (param2 == BlockPathTypes.DOOR_OPEN && !var4) {
         param2 = BlockPathTypes.BLOCKED;
      }

      if (param2 == BlockPathTypes.RAIL
         && !(param0.getBlockState(param1).getBlock() instanceof BaseRailBlock)
         && !(param0.getBlockState(param1.below()).getBlock() instanceof BaseRailBlock)) {
         param2 = BlockPathTypes.UNPASSABLE_RAIL;
      }

      return param2;
   }

   protected BlockPathTypes getBlockPathType(Mob param0, BlockPos param1) {
      return this.getCachedBlockType(param0, param1.getX(), param1.getY(), param1.getZ());
   }

   protected BlockPathTypes getCachedBlockType(Mob param0, int param1, int param2, int param3) {
      return this.pathTypesByPosCache
         .computeIfAbsent(BlockPos.asLong(param1, param2, param3), l_param4 -> this.getBlockPathType(this.level, param1, param2, param3, param0));
   }

   @Override
   public BlockPathTypes getBlockPathType(BlockGetter param0, int param1, int param2, int param3) {
      return getBlockPathTypeStatic(param0, new BlockPos.MutableBlockPos(param1, param2, param3));
   }

   public static BlockPathTypes getBlockPathTypeStatic(BlockGetter param0, BlockPos.MutableBlockPos param1) {
      int var2 = param1.getX();
      int var3 = param1.getY();
      int var4 = param1.getZ();
      BlockPathTypes var5 = getBlockPathTypeRaw(param0, param1);
      if (var5 == BlockPathTypes.OPEN && var3 >= param0.getMinBuildHeight() + 1) {
         BlockPathTypes var6 = getBlockPathTypeRaw(param0, param1.set(var2, var3 - 1, var4));
         var5 = var6 != BlockPathTypes.WALKABLE && var6 != BlockPathTypes.OPEN && var6 != BlockPathTypes.WATER && var6 != BlockPathTypes.LAVA
            ? BlockPathTypes.WALKABLE
            : BlockPathTypes.OPEN;
         if (var6 == BlockPathTypes.DAMAGE_FIRE) {
            var5 = BlockPathTypes.DAMAGE_FIRE;
         }

         if (var6 == BlockPathTypes.DAMAGE_OTHER) {
            var5 = BlockPathTypes.DAMAGE_OTHER;
         }

         if (var6 == BlockPathTypes.STICKY_HONEY) {
            var5 = BlockPathTypes.STICKY_HONEY;
         }

         if (var6 == BlockPathTypes.POWDER_SNOW) {
            var5 = BlockPathTypes.DANGER_POWDER_SNOW;
         }
      }

      if (var5 == BlockPathTypes.WALKABLE) {
         var5 = checkNeighbourBlocks(param0, param1.set(var2, var3, var4), var5);
      }

      return var5;
   }

   public static BlockPathTypes checkNeighbourBlocks(BlockGetter param0, BlockPos.MutableBlockPos param1, BlockPathTypes param2) {
      int var3 = param1.getX();
      int var4 = param1.getY();
      int var5 = param1.getZ();

      for (int var6 = -1; var6 <= 1; var6++) {
         for (int var7 = -1; var7 <= 1; var7++) {
            for (int var8 = -1; var8 <= 1; var8++) {
               if (var6 != 0 || var8 != 0) {
                  param1.set(var3 + var6, var4 + var7, var5 + var8);
                  BlockState var9 = param0.getBlockState(param1);
                  if (var9.is(Blocks.CACTUS) || var9.is(Blocks.SWEET_BERRY_BUSH)) {
                     return BlockPathTypes.DANGER_OTHER;
                  }

                  if (isBurningBlock(var9)) {
                     return BlockPathTypes.DANGER_FIRE;
                  }

                  if (param0.getFluidState(param1).is(FluidTags.WATER)) {
                     return BlockPathTypes.WATER_BORDER;
                  }
               }
            }
         }
      }

      return param2;
   }

   protected static BlockPathTypes getBlockPathTypeRaw(BlockGetter param0, BlockPos param1) {
      BlockState var2 = param0.getBlockState(param1);
      Block var3 = var2.getBlock();
      Material var4 = var2.getMaterial();
      if (var2.isAir()) {
         return BlockPathTypes.OPEN;
      }

      if (var2.is(BlockTags.TRAPDOORS) || var2.is(Blocks.LILY_PAD) || var2.is(Blocks.BIG_DRIPLEAF)) {
         return BlockPathTypes.TRAPDOOR;
      }

      if (var2.is(Blocks.POWDER_SNOW)) {
         return BlockPathTypes.POWDER_SNOW;
      }

      if (var2.is(Blocks.CACTUS) || var2.is(Blocks.SWEET_BERRY_BUSH)) {
         return BlockPathTypes.DAMAGE_OTHER;
      }

      if (var2.is(Blocks.HONEY_BLOCK)) {
         return BlockPathTypes.STICKY_HONEY;
      }

      if (var2.is(Blocks.COCOA)) {
         return BlockPathTypes.COCOA;
      }

      FluidState var5 = param0.getFluidState(param1);
      if (var5.is(FluidTags.LAVA)) {
         return BlockPathTypes.LAVA;
      }

      if (isBurningBlock(var2)) {
         return BlockPathTypes.DAMAGE_FIRE;
      }

      if (DoorBlock.isWoodenDoor(var2) && !var2.getValue(DoorBlock.OPEN)) {
         return BlockPathTypes.DOOR_WOOD_CLOSED;
      }

      if (var3 instanceof DoorBlock && var4 == Material.METAL && !var2.getValue(DoorBlock.OPEN)) {
         return BlockPathTypes.DOOR_IRON_CLOSED;
      }

      if (var3 instanceof DoorBlock && var2.getValue(DoorBlock.OPEN)) {
         return BlockPathTypes.DOOR_OPEN;
      }

      if (var3 instanceof BaseRailBlock) {
         return BlockPathTypes.RAIL;
      }

      if (var3 instanceof LeavesBlock) {
         return BlockPathTypes.LEAVES;
      }

      if (!var2.is(BlockTags.FENCES) && !var2.is(BlockTags.WALLS) && (!(var3 instanceof FenceGateBlock) || var2.getValue(FenceGateBlock.OPEN))) {
         if (!var2.isPathfindable(param0, param1, PathComputationType.LAND)) {
            return BlockPathTypes.BLOCKED;
         } else {
            return var5.is(FluidTags.WATER) ? BlockPathTypes.WATER : BlockPathTypes.OPEN;
         }
      } else {
         return BlockPathTypes.FENCE;
      }
   }

   public static boolean isBurningBlock(BlockState param0) {
      return param0.is(BlockTags.FIRE)
         || param0.is(Blocks.LAVA)
         || param0.is(Blocks.MAGMA_BLOCK)
         || CampfireBlock.isLitCampfire(param0)
         || param0.is(Blocks.LAVA_CAULDRON);
   }
}
