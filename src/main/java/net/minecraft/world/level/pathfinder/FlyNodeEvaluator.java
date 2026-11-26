package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FlyNodeEvaluator extends WalkNodeEvaluator {
   private final Long2ObjectMap<BlockPathTypes> pathTypeByPosCache = new Long2ObjectOpenHashMap<>();
   private static final float SMALL_MOB_INFLATED_START_NODE_BOUNDING_BOX = 1.5F;
   private static final int MAX_START_NODE_CANDIDATES = 10;

   @Override
   public void prepare(PathNavigationRegion param0, Mob param1) {
      super.prepare(param0, param1);
      this.pathTypeByPosCache.clear();
      this.oldWaterCost = param1.getPathfindingMalus(BlockPathTypes.WATER);
   }

   @Override
   public void done() {
      this.mob.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
      this.pathTypeByPosCache.clear();
      super.done();
   }

   @Override
   public Node getStart() {
      int var1;
      if (this.canFloat() && this.mob.isInWater()) {
         var1 = this.mob.getBlockY();
         BlockPos.MutableBlockPos var2 = new BlockPos.MutableBlockPos(this.mob.getX(), var1, this.mob.getZ());

         for (BlockState var3 = this.level.getBlockState(var2); var3.is(Blocks.WATER); var3 = this.level.getBlockState(var2)) {
            var2.set(this.mob.getX(), ++var1, this.mob.getZ());
         }
      } else {
         var1 = Mth.floor(this.mob.getY() + 0.5);
      }

      BlockPos var2 = BlockPos.containing(this.mob.getX(), var1, this.mob.getZ());
      if (!this.canStartAt(var2)) {
         for (BlockPos var4 : this.iteratePathfindingStartNodeCandidatePositions(this.mob)) {
            if (this.canStartAt(var4)) {
               return super.getStartNode(var4);
            }
         }
      }

      return super.getStartNode(var2);
   }

   @Override
   protected boolean canStartAt(BlockPos param0) {
      BlockPathTypes var2 = this.getBlockPathType(this.mob, param0);
      return this.mob.getPathfindingMalus(var2) >= 0.0F;
   }

   @Override
   public Target getGoal(double param0, double param2, double param4) {
      return this.getTargetFromNode(this.getNode(Mth.floor(param0), Mth.floor(param2), Mth.floor(param4)));
   }

   @Override
   public int getNeighbors(Node[] param0, Node param1) {
      int var3 = 0;
      Node var4 = this.findAcceptedNode(param1.x, param1.y, param1.z + 1);
      if (this.isOpen(var4)) {
         param0[var3++] = var4;
      }

      Node var5 = this.findAcceptedNode(param1.x - 1, param1.y, param1.z);
      if (this.isOpen(var5)) {
         param0[var3++] = var5;
      }

      Node var6 = this.findAcceptedNode(param1.x + 1, param1.y, param1.z);
      if (this.isOpen(var6)) {
         param0[var3++] = var6;
      }

      Node var7 = this.findAcceptedNode(param1.x, param1.y, param1.z - 1);
      if (this.isOpen(var7)) {
         param0[var3++] = var7;
      }

      Node var8 = this.findAcceptedNode(param1.x, param1.y + 1, param1.z);
      if (this.isOpen(var8)) {
         param0[var3++] = var8;
      }

      Node var9 = this.findAcceptedNode(param1.x, param1.y - 1, param1.z);
      if (this.isOpen(var9)) {
         param0[var3++] = var9;
      }

      Node var10 = this.findAcceptedNode(param1.x, param1.y + 1, param1.z + 1);
      if (this.isOpen(var10) && this.hasMalus(var4) && this.hasMalus(var8)) {
         param0[var3++] = var10;
      }

      Node var11 = this.findAcceptedNode(param1.x - 1, param1.y + 1, param1.z);
      if (this.isOpen(var11) && this.hasMalus(var5) && this.hasMalus(var8)) {
         param0[var3++] = var11;
      }

      Node var12 = this.findAcceptedNode(param1.x + 1, param1.y + 1, param1.z);
      if (this.isOpen(var12) && this.hasMalus(var6) && this.hasMalus(var8)) {
         param0[var3++] = var12;
      }

      Node var13 = this.findAcceptedNode(param1.x, param1.y + 1, param1.z - 1);
      if (this.isOpen(var13) && this.hasMalus(var7) && this.hasMalus(var8)) {
         param0[var3++] = var13;
      }

      Node var14 = this.findAcceptedNode(param1.x, param1.y - 1, param1.z + 1);
      if (this.isOpen(var14) && this.hasMalus(var4) && this.hasMalus(var9)) {
         param0[var3++] = var14;
      }

      Node var15 = this.findAcceptedNode(param1.x - 1, param1.y - 1, param1.z);
      if (this.isOpen(var15) && this.hasMalus(var5) && this.hasMalus(var9)) {
         param0[var3++] = var15;
      }

      Node var16 = this.findAcceptedNode(param1.x + 1, param1.y - 1, param1.z);
      if (this.isOpen(var16) && this.hasMalus(var6) && this.hasMalus(var9)) {
         param0[var3++] = var16;
      }

      Node var17 = this.findAcceptedNode(param1.x, param1.y - 1, param1.z - 1);
      if (this.isOpen(var17) && this.hasMalus(var7) && this.hasMalus(var9)) {
         param0[var3++] = var17;
      }

      Node var18 = this.findAcceptedNode(param1.x + 1, param1.y, param1.z - 1);
      if (this.isOpen(var18) && this.hasMalus(var7) && this.hasMalus(var6)) {
         param0[var3++] = var18;
      }

      Node var19 = this.findAcceptedNode(param1.x + 1, param1.y, param1.z + 1);
      if (this.isOpen(var19) && this.hasMalus(var4) && this.hasMalus(var6)) {
         param0[var3++] = var19;
      }

      Node var20 = this.findAcceptedNode(param1.x - 1, param1.y, param1.z - 1);
      if (this.isOpen(var20) && this.hasMalus(var7) && this.hasMalus(var5)) {
         param0[var3++] = var20;
      }

      Node var21 = this.findAcceptedNode(param1.x - 1, param1.y, param1.z + 1);
      if (this.isOpen(var21) && this.hasMalus(var4) && this.hasMalus(var5)) {
         param0[var3++] = var21;
      }

      Node var22 = this.findAcceptedNode(param1.x + 1, param1.y + 1, param1.z - 1);
      if (this.isOpen(var22)
         && this.hasMalus(var18)
         && this.hasMalus(var7)
         && this.hasMalus(var6)
         && this.hasMalus(var8)
         && this.hasMalus(var13)
         && this.hasMalus(var12)) {
         param0[var3++] = var22;
      }

      Node var23 = this.findAcceptedNode(param1.x + 1, param1.y + 1, param1.z + 1);
      if (this.isOpen(var23)
         && this.hasMalus(var19)
         && this.hasMalus(var4)
         && this.hasMalus(var6)
         && this.hasMalus(var8)
         && this.hasMalus(var10)
         && this.hasMalus(var12)) {
         param0[var3++] = var23;
      }

      Node var24 = this.findAcceptedNode(param1.x - 1, param1.y + 1, param1.z - 1);
      if (this.isOpen(var24)
         && this.hasMalus(var20)
         && this.hasMalus(var7)
         && this.hasMalus(var5)
         && this.hasMalus(var8)
         && this.hasMalus(var13)
         && this.hasMalus(var11)) {
         param0[var3++] = var24;
      }

      Node var25 = this.findAcceptedNode(param1.x - 1, param1.y + 1, param1.z + 1);
      if (this.isOpen(var25)
         && this.hasMalus(var21)
         && this.hasMalus(var4)
         && this.hasMalus(var5)
         && this.hasMalus(var8)
         && this.hasMalus(var10)
         && this.hasMalus(var11)) {
         param0[var3++] = var25;
      }

      Node var26 = this.findAcceptedNode(param1.x + 1, param1.y - 1, param1.z - 1);
      if (this.isOpen(var26)
         && this.hasMalus(var18)
         && this.hasMalus(var7)
         && this.hasMalus(var6)
         && this.hasMalus(var9)
         && this.hasMalus(var17)
         && this.hasMalus(var16)) {
         param0[var3++] = var26;
      }

      Node var27 = this.findAcceptedNode(param1.x + 1, param1.y - 1, param1.z + 1);
      if (this.isOpen(var27)
         && this.hasMalus(var19)
         && this.hasMalus(var4)
         && this.hasMalus(var6)
         && this.hasMalus(var9)
         && this.hasMalus(var14)
         && this.hasMalus(var16)) {
         param0[var3++] = var27;
      }

      Node var28 = this.findAcceptedNode(param1.x - 1, param1.y - 1, param1.z - 1);
      if (this.isOpen(var28)
         && this.hasMalus(var20)
         && this.hasMalus(var7)
         && this.hasMalus(var5)
         && this.hasMalus(var9)
         && this.hasMalus(var17)
         && this.hasMalus(var15)) {
         param0[var3++] = var28;
      }

      Node var29 = this.findAcceptedNode(param1.x - 1, param1.y - 1, param1.z + 1);
      if (this.isOpen(var29)
         && this.hasMalus(var21)
         && this.hasMalus(var4)
         && this.hasMalus(var5)
         && this.hasMalus(var9)
         && this.hasMalus(var14)
         && this.hasMalus(var15)) {
         param0[var3++] = var29;
      }

      return var3;
   }

   private boolean hasMalus(@Nullable Node param0) {
      return param0 != null && param0.costMalus >= 0.0F;
   }

   private boolean isOpen(@Nullable Node param0) {
      return param0 != null && !param0.closed;
   }

   @Nullable
   protected Node findAcceptedNode(int param0, int param1, int param2) {
      Node var4 = null;
      BlockPathTypes var5 = this.getCachedBlockPathType(param0, param1, param2);
      float var6 = this.mob.getPathfindingMalus(var5);
      if (var6 >= 0.0F) {
         var4 = this.getNode(param0, param1, param2);
         var4.type = var5;
         var4.costMalus = Math.max(var4.costMalus, var6);
         if (var5 == BlockPathTypes.WALKABLE) {
            var4.costMalus++;
         }
      }

      return var4;
   }

   private BlockPathTypes getCachedBlockPathType(int param0, int param1, int param2) {
      return this.pathTypeByPosCache
         .computeIfAbsent(BlockPos.asLong(param0, param1, param2), l_param3 -> this.getBlockPathType(this.level, param0, param1, param2, this.mob));
   }

   @Override
   public BlockPathTypes getBlockPathType(BlockGetter param0, int param1, int param2, int param3, Mob param4) {
      EnumSet<BlockPathTypes> var6 = EnumSet.noneOf(BlockPathTypes.class);
      BlockPathTypes var7 = BlockPathTypes.BLOCKED;
      BlockPos var8 = param4.blockPosition();
      var7 = super.getBlockPathTypes(param0, param1, param2, param3, var6, var7, var8);
      if (var6.contains(BlockPathTypes.FENCE)) {
         return BlockPathTypes.FENCE;
      }

      BlockPathTypes var9 = BlockPathTypes.BLOCKED;

      for (BlockPathTypes var11 : var6) {
         if (param4.getPathfindingMalus(var11) < 0.0F) {
            return var11;
         }

         if (param4.getPathfindingMalus(var11) >= param4.getPathfindingMalus(var9)) {
            var9 = var11;
         }
      }

      return var7 == BlockPathTypes.OPEN && param4.getPathfindingMalus(var9) == 0.0F ? BlockPathTypes.OPEN : var9;
   }

   @Override
   public BlockPathTypes getBlockPathType(BlockGetter param0, int param1, int param2, int param3) {
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();
      BlockPathTypes var6 = getBlockPathTypeRaw(param0, var5.set(param1, param2, param3));
      if (var6 == BlockPathTypes.OPEN && param2 >= param0.getMinBuildHeight() + 1) {
         BlockPathTypes var7 = getBlockPathTypeRaw(param0, var5.set(param1, param2 - 1, param3));
         if (var7 == BlockPathTypes.DAMAGE_FIRE || var7 == BlockPathTypes.LAVA) {
            var6 = BlockPathTypes.DAMAGE_FIRE;
         } else if (var7 == BlockPathTypes.DAMAGE_OTHER) {
            var6 = BlockPathTypes.DAMAGE_OTHER;
         } else if (var7 == BlockPathTypes.COCOA) {
            var6 = BlockPathTypes.COCOA;
         } else if (var7 == BlockPathTypes.FENCE) {
            if (!var5.equals(this.mob.blockPosition())) {
               var6 = BlockPathTypes.FENCE;
            }
         } else {
            var6 = var7 != BlockPathTypes.WALKABLE && var7 != BlockPathTypes.OPEN && var7 != BlockPathTypes.WATER
               ? BlockPathTypes.WALKABLE
               : BlockPathTypes.OPEN;
         }
      }

      if (var6 == BlockPathTypes.WALKABLE || var6 == BlockPathTypes.OPEN) {
         var6 = checkNeighbourBlocks(param0, var5.set(param1, param2, param3), var6);
      }

      return var6;
   }

   private Iterable<BlockPos> iteratePathfindingStartNodeCandidatePositions(Mob param0) {
      float var2 = 1.0F;
      AABB var3 = param0.getBoundingBox();
      boolean var4 = var3.getSize() < 1.0;
      if (!var4) {
         return List.of(
            BlockPos.containing(var3.minX, param0.getBlockY(), var3.minZ),
            BlockPos.containing(var3.minX, param0.getBlockY(), var3.maxZ),
            BlockPos.containing(var3.maxX, param0.getBlockY(), var3.minZ),
            BlockPos.containing(var3.maxX, param0.getBlockY(), var3.maxZ)
         );
      }

      double var5 = Math.max(0.0, (1.5 - var3.getZsize()) / 2.0);
      double var7 = Math.max(0.0, (1.5 - var3.getXsize()) / 2.0);
      double var9 = Math.max(0.0, (1.5 - var3.getYsize()) / 2.0);
      AABB var11 = var3.inflate(var7, var9, var5);
      return BlockPos.randomBetweenClosed(
         param0.getRandom(),
         10,
         Mth.floor(var11.minX),
         Mth.floor(var11.minY),
         Mth.floor(var11.minZ),
         Mth.floor(var11.maxX),
         Mth.floor(var11.maxY),
         Mth.floor(var11.maxZ)
      );
   }
}
