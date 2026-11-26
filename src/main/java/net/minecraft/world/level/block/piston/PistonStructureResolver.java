package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class PistonStructureResolver {
   public static final int MAX_PUSH_DEPTH = 12;
   private final Level level;
   private final BlockPos pistonPos;
   private final boolean extending;
   private final BlockPos startPos;
   private final Direction pushDirection;
   private final List<BlockPos> toPush = Lists.newArrayList();
   private final List<BlockPos> toDestroy = Lists.newArrayList();
   private final Direction pistonDirection;

   public PistonStructureResolver(Level param0, BlockPos param1, Direction param2, boolean param3) {
      this.level = param0;
      this.pistonPos = param1;
      this.pistonDirection = param2;
      this.extending = param3;
      if (param3) {
         this.pushDirection = param2;
         this.startPos = param1.relative(param2);
      } else {
         this.pushDirection = param2.getOpposite();
         this.startPos = param1.relative(param2, 2);
      }
   }

   public boolean resolve() {
      this.toPush.clear();
      this.toDestroy.clear();
      BlockState var1 = this.level.getBlockState(this.startPos);
      if (!PistonBaseBlock.isPushable(var1, this.level, this.startPos, this.pushDirection, false, this.pistonDirection)) {
         if (this.extending && var1.getPistonPushReaction() == PushReaction.DESTROY) {
            this.toDestroy.add(this.startPos);
            return true;
         } else {
            return false;
         }
      } else {
         if (!this.addBlockLine(this.startPos, this.pushDirection)) {
            return false;
         }

         for (int var2 = 0; var2 < this.toPush.size(); var2++) {
            BlockPos var3 = this.toPush.get(var2);
            if (isSticky(this.level.getBlockState(var3)) && !this.addBranchingBlocks(var3)) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean isSticky(BlockState param0) {
      return param0.is(Blocks.SLIME_BLOCK) || param0.is(Blocks.HONEY_BLOCK);
   }

   private static boolean canStickToEachOther(BlockState param0, BlockState param1) {
      if (param0.is(Blocks.HONEY_BLOCK) && param1.is(Blocks.SLIME_BLOCK)) {
         return false;
      } else {
         return param0.is(Blocks.SLIME_BLOCK) && param1.is(Blocks.HONEY_BLOCK) ? false : isSticky(param0) || isSticky(param1);
      }
   }

   private boolean addBlockLine(BlockPos param0, Direction param1) {
      BlockState var3 = this.level.getBlockState(param0);
      if (var3.isAir()) {
         return true;
      }

      if (!PistonBaseBlock.isPushable(var3, this.level, param0, this.pushDirection, false, param1)) {
         return true;
      }

      if (param0.equals(this.pistonPos)) {
         return true;
      }

      if (this.toPush.contains(param0)) {
         return true;
      }

      int var4 = 1;
      if (var4 + this.toPush.size() > 12) {
         return false;
      }

      while (isSticky(var3)) {
         BlockPos var5 = param0.relative(this.pushDirection.getOpposite(), var4);
         BlockState var6 = var3;
         var3 = this.level.getBlockState(var5);
         if (var3.isAir()
            || !canStickToEachOther(var6, var3)
            || !PistonBaseBlock.isPushable(var3, this.level, var5, this.pushDirection, false, this.pushDirection.getOpposite())
            || var5.equals(this.pistonPos)) {
            break;
         }

         if (++var4 + this.toPush.size() > 12) {
            return false;
         }
      }

      int var5 = 0;

      for (int var6 = var4 - 1; var6 >= 0; var6--) {
         this.toPush.add(param0.relative(this.pushDirection.getOpposite(), var6));
         var5++;
      }

      int var6 = 1;

      while (true) {
         BlockPos var7 = param0.relative(this.pushDirection, var6);
         int var8 = this.toPush.indexOf(var7);
         if (var8 > -1) {
            this.reorderListAtCollision(var5, var8);

            for (int var9 = 0; var9 <= var8 + var5; var9++) {
               BlockPos var10 = this.toPush.get(var9);
               if (isSticky(this.level.getBlockState(var10)) && !this.addBranchingBlocks(var10)) {
                  return false;
               }
            }

            return true;
         }

         var3 = this.level.getBlockState(var7);
         if (var3.isAir()) {
            return true;
         }

         if (!PistonBaseBlock.isPushable(var3, this.level, var7, this.pushDirection, true, this.pushDirection) || var7.equals(this.pistonPos)) {
            return false;
         }

         if (var3.getPistonPushReaction() == PushReaction.DESTROY) {
            this.toDestroy.add(var7);
            return true;
         }

         if (this.toPush.size() >= 12) {
            return false;
         }

         this.toPush.add(var7);
         var5++;
         var6++;
      }
   }

   private void reorderListAtCollision(int param0, int param1) {
      List<BlockPos> var3 = Lists.newArrayList();
      List<BlockPos> var4 = Lists.newArrayList();
      List<BlockPos> var5 = Lists.newArrayList();
      var3.addAll(this.toPush.subList(0, param1));
      var4.addAll(this.toPush.subList(this.toPush.size() - param0, this.toPush.size()));
      var5.addAll(this.toPush.subList(param1, this.toPush.size() - param0));
      this.toPush.clear();
      this.toPush.addAll(var3);
      this.toPush.addAll(var4);
      this.toPush.addAll(var5);
   }

   private boolean addBranchingBlocks(BlockPos param0) {
      BlockState var2 = this.level.getBlockState(param0);

      for (Direction var6 : Direction.values()) {
         if (var6.getAxis() != this.pushDirection.getAxis()) {
            BlockPos var7 = param0.relative(var6);
            BlockState var8 = this.level.getBlockState(var7);
            if (canStickToEachOther(var8, var2) && !this.addBlockLine(var7, var6)) {
               return false;
            }
         }
      }

      return true;
   }

   public Direction getPushDirection() {
      return this.pushDirection;
   }

   public List<BlockPos> getToPush() {
      return this.toPush;
   }

   public List<BlockPos> getToDestroy() {
      return this.toDestroy;
   }
}
