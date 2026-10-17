package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;

public class SpongeBlock extends Block {
   public static final int MAX_DEPTH = 6;
   public static final int MAX_COUNT = 64;

   protected SpongeBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param3.is(param0.getBlock())) {
         this.tryAbsorbWater(param1, param2);
      }
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      this.tryAbsorbWater(param1, param2);
      super.neighborChanged(param0, param1, param2, param3, param4, param5);
   }

   protected void tryAbsorbWater(Level param0, BlockPos param1) {
      if (this.removeWaterBreadthFirstSearch(param0, param1)) {
         param0.setBlock(param1, Blocks.WET_SPONGE.defaultBlockState(), 2);
         param0.levelEvent(2001, param1, Block.getId(Blocks.WATER.defaultBlockState()));
      }
   }

   private boolean removeWaterBreadthFirstSearch(Level param0, BlockPos param1) {
      Queue<Tuple<BlockPos, Integer>> var3 = Lists.newLinkedList();
      var3.add(new Tuple<>(param1, 0));
      int var4 = 0;

      while (!var3.isEmpty()) {
         Tuple<BlockPos, Integer> var5 = var3.poll();
         BlockPos var6 = var5.getA();
         int var7 = var5.getB();

         for (Direction var11 : Direction.values()) {
            BlockPos var12 = var6.relative(var11);
            BlockState var13 = param0.getBlockState(var12);
            FluidState var14 = param0.getFluidState(var12);
            Material var15 = var13.getMaterial();
            if (var14.is(FluidTags.WATER)) {
               if (var13.getBlock() instanceof BucketPickup && !((BucketPickup)var13.getBlock()).pickupBlock(param0, var12, var13).isEmpty()) {
                  var4++;
                  if (var7 < 6) {
                     var3.add(new Tuple<>(var12, var7 + 1));
                  }
               } else if (var13.getBlock() instanceof LiquidBlock) {
                  param0.setBlock(var12, Blocks.AIR.defaultBlockState(), 3);
                  var4++;
                  if (var7 < 6) {
                     var3.add(new Tuple<>(var12, var7 + 1));
                  }
               } else if (var15 == Material.WATER_PLANT || var15 == Material.REPLACEABLE_WATER_PLANT) {
                  BlockEntity var16 = var13.hasBlockEntity() ? param0.getBlockEntity(var12) : null;
                  dropResources(var13, param0, var12, var16);
                  param0.setBlock(var12, Blocks.AIR.defaultBlockState(), 3);
                  var4++;
                  if (var7 < 6) {
                     var3.add(new Tuple<>(var12, var7 + 1));
                  }
               }
            }
         }

         if (var4 > 64) {
            break;
         }
      }

      return var4 > 0;
   }
}
