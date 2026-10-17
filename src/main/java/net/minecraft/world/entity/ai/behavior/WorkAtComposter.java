package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WorkAtComposter extends WorkAtPoi {
   private static final List<Item> COMPOSTABLE_ITEMS = ImmutableList.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);

   @Override
   protected void useWorkstation(ServerLevel param0, Villager param1) {
      Optional<GlobalPos> var3 = param1.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      if (var3.isPresent()) {
         GlobalPos var4 = var3.get();
         BlockState var5 = param0.getBlockState(var4.pos());
         if (var5.is(Blocks.COMPOSTER)) {
            this.makeBread(param1);
            this.compostItems(param0, param1, var4, var5);
         }
      }
   }

   private void compostItems(ServerLevel param0, Villager param1, GlobalPos param2, BlockState param3) {
      BlockPos var5 = param2.pos();
      if (param3.getValue(ComposterBlock.LEVEL) == 8) {
         param3 = ComposterBlock.extractProduce(param1, param3, param0, var5);
      }

      int var6 = 20;
      int var7 = 10;
      int[] var8 = new int[COMPOSTABLE_ITEMS.size()];
      SimpleContainer var9 = param1.getInventory();
      int var10 = var9.getContainerSize();
      BlockState var11 = param3;

      for (int var12 = var10 - 1; var12 >= 0 && var6 > 0; var12--) {
         ItemStack var13 = var9.getItem(var12);
         int var14 = COMPOSTABLE_ITEMS.indexOf(var13.getItem());
         if (var14 != -1) {
            int var15 = var13.getCount();
            int var16 = var8[var14] + var15;
            var8[var14] = var16;
            int var17 = Math.min(Math.min(var16 - 10, var6), var15);
            if (var17 > 0) {
               var6 -= var17;

               for (int var18 = 0; var18 < var17; var18++) {
                  var11 = ComposterBlock.insertItem(param1, var11, param0, var13, var5);
                  if (var11.getValue(ComposterBlock.LEVEL) == 7) {
                     this.spawnComposterFillEffects(param0, param3, var5, var11);
                     return;
                  }
               }
            }
         }
      }

      this.spawnComposterFillEffects(param0, param3, var5, var11);
   }

   private void spawnComposterFillEffects(ServerLevel param0, BlockState param1, BlockPos param2, BlockState param3) {
      param0.levelEvent(1500, param2, param3 != param1 ? 1 : 0);
   }

   private void makeBread(Villager param0) {
      SimpleContainer var2 = param0.getInventory();
      if (var2.countItem(Items.BREAD) <= 36) {
         int var3 = var2.countItem(Items.WHEAT);
         int var4 = 3;
         int var5 = 3;
         int var6 = Math.min(3, var3 / 3);
         if (var6 != 0) {
            int var7 = var6 * 3;
            var2.removeItemType(Items.WHEAT, var7);
            ItemStack var8 = var2.addItem(new ItemStack(Items.BREAD, var6));
            if (!var8.isEmpty()) {
               param0.spawnAtLocation(var8, 0.5F);
            }
         }
      }
   }
}
