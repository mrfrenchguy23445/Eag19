package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class UseBonemeal extends Behavior<Villager> {
   private static final int BONEMEALING_DURATION = 80;
   private long nextWorkCycleTime;
   private long lastBonemealingSession;
   private int timeWorkedSoFar;
   private Optional<BlockPos> cropPos = Optional.empty();

   public UseBonemeal() {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Villager param1) {
      if (param1.tickCount % 10 == 0 && (this.lastBonemealingSession == 0L || this.lastBonemealingSession + 160L <= param1.tickCount)) {
         if (param1.getInventory().countItem(Items.BONE_MEAL) <= 0) {
            return false;
         }

         this.cropPos = this.pickNextTarget(param0, param1);
         return this.cropPos.isPresent();
      } else {
         return false;
      }
   }

   protected boolean canStillUse(ServerLevel param0, Villager param1, long param2) {
      return this.timeWorkedSoFar < 80 && this.cropPos.isPresent();
   }

   private Optional<BlockPos> pickNextTarget(ServerLevel param0, Villager param1) {
      BlockPos.MutableBlockPos var3 = new BlockPos.MutableBlockPos();
      Optional<BlockPos> var4 = Optional.empty();
      int var5 = 0;

      for (int var6 = -1; var6 <= 1; var6++) {
         for (int var7 = -1; var7 <= 1; var7++) {
            for (int var8 = -1; var8 <= 1; var8++) {
               var3.setWithOffset(param1.blockPosition(), var6, var7, var8);
               if (this.validPos(var3, param0)) {
                  if (param0.random.nextInt(++var5) == 0) {
                     var4 = Optional.of(var3.immutable());
                  }
               }
            }
         }
      }

      return var4;
   }

   private boolean validPos(BlockPos param0, ServerLevel param1) {
      BlockState var3 = param1.getBlockState(param0);
      Block var4 = var3.getBlock();
      return var4 instanceof CropBlock && !((CropBlock)var4).isMaxAge(var3);
   }

   protected void start(ServerLevel param0, Villager param1, long param2) {
      this.setCurrentCropAsTarget(param1);
      param1.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE_MEAL));
      this.nextWorkCycleTime = param2;
      this.timeWorkedSoFar = 0;
   }

   private void setCurrentCropAsTarget(Villager param0) {
      this.cropPos.ifPresent(l_param1 -> {
         BlockPosTracker l_var2 = new BlockPosTracker(l_param1);
         param0.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, l_var2);
         param0.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(l_var2, 0.5F, 1));
      });
   }

   protected void stop(ServerLevel param0, Villager param1, long param2) {
      param1.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      this.lastBonemealingSession = param1.tickCount;
   }

   protected void tick(ServerLevel param0, Villager param1, long param2) {
      BlockPos var5 = this.cropPos.get();
      if (param2 >= this.nextWorkCycleTime && var5.closerToCenterThan(param1.position(), 1.0)) {
         ItemStack var6 = ItemStack.EMPTY;
         SimpleContainer var7 = param1.getInventory();
         int var8 = var7.getContainerSize();

         for (int var9 = 0; var9 < var8; var9++) {
            ItemStack var10 = var7.getItem(var9);
            if (var10.is(Items.BONE_MEAL)) {
               var6 = var10;
               break;
            }
         }

         if (!var6.isEmpty() && BoneMealItem.growCrop(var6, param0, var5)) {
            param0.levelEvent(1505, var5, 0);
            this.cropPos = this.pickNextTarget(param0, param1);
            this.setCurrentCropAsTarget(param1);
            this.nextWorkCycleTime = param2 + 40L;
         }

         this.timeWorkedSoFar++;
      }
   }
}
