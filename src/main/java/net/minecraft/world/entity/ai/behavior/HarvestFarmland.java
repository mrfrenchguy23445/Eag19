package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class HarvestFarmland extends Behavior<Villager> {
   private static final int HARVEST_DURATION = 200;
   public static final float SPEED_MODIFIER = 0.5F;
   @Nullable
   private BlockPos aboveFarmlandPos;
   private long nextOkStartTime;
   private int timeWorkedSoFar;
   private final List<BlockPos> validFarmlandAroundVillager = Lists.newArrayList();

   public HarvestFarmland() {
      super(
         ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.SECONDARY_JOB_SITE,
            MemoryStatus.VALUE_PRESENT
         )
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Villager param1) {
      if (!param0.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
         return false;
      }

      if (param1.getVillagerData().getProfession() != VillagerProfession.FARMER) {
         return false;
      }

      BlockPos.MutableBlockPos var3 = param1.blockPosition().mutable();
      this.validFarmlandAroundVillager.clear();

      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               var3.set(param1.getX() + var4, param1.getY() + var5, param1.getZ() + var6);
               if (this.validPos(var3, param0)) {
                  this.validFarmlandAroundVillager.add(new BlockPos(var3));
               }
            }
         }
      }

      this.aboveFarmlandPos = this.getValidFarmland(param0);
      return this.aboveFarmlandPos != null;
   }

   @Nullable
   private BlockPos getValidFarmland(ServerLevel param0) {
      return this.validFarmlandAroundVillager.isEmpty()
         ? null
         : this.validFarmlandAroundVillager.get(param0.getRandom().nextInt(this.validFarmlandAroundVillager.size()));
   }

   private boolean validPos(BlockPos param0, ServerLevel param1) {
      BlockState var3 = param1.getBlockState(param0);
      Block var4 = var3.getBlock();
      Block var5 = param1.getBlockState(param0.below()).getBlock();
      return var4 instanceof CropBlock && ((CropBlock)var4).isMaxAge(var3) || var3.isAir() && var5 instanceof FarmBlock;
   }

   protected void start(ServerLevel param0, Villager param1, long param2) {
      if (param2 > this.nextOkStartTime && this.aboveFarmlandPos != null) {
         param1.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.aboveFarmlandPos));
         param1.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5F, 1));
      }
   }

   protected void stop(ServerLevel param0, Villager param1, long param2) {
      param1.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      param1.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      this.timeWorkedSoFar = 0;
      this.nextOkStartTime = param2 + 40L;
   }

   protected void tick(ServerLevel param0, Villager param1, long param2) {
      if (this.aboveFarmlandPos == null || this.aboveFarmlandPos.closerToCenterThan(param1.position(), 1.0)) {
         if (this.aboveFarmlandPos != null && param2 > this.nextOkStartTime) {
            BlockState var5 = param0.getBlockState(this.aboveFarmlandPos);
            Block var6 = var5.getBlock();
            Block var7 = param0.getBlockState(this.aboveFarmlandPos.below()).getBlock();
            if (var6 instanceof CropBlock && ((CropBlock)var6).isMaxAge(var5)) {
               param0.destroyBlock(this.aboveFarmlandPos, true, param1);
            }

            if (var5.isAir() && var7 instanceof FarmBlock && param1.hasFarmSeeds()) {
               SimpleContainer var8 = param1.getInventory();

               for (int var9 = 0; var9 < var8.getContainerSize(); var9++) {
                  ItemStack var10 = var8.getItem(var9);
                  boolean var11 = false;
                  if (!var10.isEmpty()) {
                     if (var10.is(Items.WHEAT_SEEDS)) {
                        BlockState var12 = Blocks.WHEAT.defaultBlockState();
                        param0.setBlockAndUpdate(this.aboveFarmlandPos, var12);
                        param0.gameEvent(GameEvent.BLOCK_PLACE, this.aboveFarmlandPos, GameEvent.Context.of(param1, var12));
                        var11 = true;
                     } else if (var10.is(Items.POTATO)) {
                        BlockState var12 = Blocks.POTATOES.defaultBlockState();
                        param0.setBlockAndUpdate(this.aboveFarmlandPos, var12);
                        param0.gameEvent(GameEvent.BLOCK_PLACE, this.aboveFarmlandPos, GameEvent.Context.of(param1, var12));
                        var11 = true;
                     } else if (var10.is(Items.CARROT)) {
                        BlockState var12 = Blocks.CARROTS.defaultBlockState();
                        param0.setBlockAndUpdate(this.aboveFarmlandPos, var12);
                        param0.gameEvent(GameEvent.BLOCK_PLACE, this.aboveFarmlandPos, GameEvent.Context.of(param1, var12));
                        var11 = true;
                     } else if (var10.is(Items.BEETROOT_SEEDS)) {
                        BlockState var12 = Blocks.BEETROOTS.defaultBlockState();
                        param0.setBlockAndUpdate(this.aboveFarmlandPos, var12);
                        param0.gameEvent(GameEvent.BLOCK_PLACE, this.aboveFarmlandPos, GameEvent.Context.of(param1, var12));
                        var11 = true;
                     }
                  }

                  if (var11) {
                     param0.playSound(
                        null,
                        this.aboveFarmlandPos.getX(),
                        this.aboveFarmlandPos.getY(),
                        this.aboveFarmlandPos.getZ(),
                        SoundEvents.CROP_PLANTED,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                     );
                     var10.shrink(1);
                     if (var10.isEmpty()) {
                        var8.setItem(var9, ItemStack.EMPTY);
                     }
                     break;
                  }
               }
            }

            if (var6 instanceof CropBlock && !((CropBlock)var6).isMaxAge(var5)) {
               this.validFarmlandAroundVillager.remove(this.aboveFarmlandPos);
               this.aboveFarmlandPos = this.getValidFarmland(param0);
               if (this.aboveFarmlandPos != null) {
                  this.nextOkStartTime = param2 + 20L;
                  param1.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5F, 1));
                  param1.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.aboveFarmlandPos));
               }
            }
         }

         this.timeWorkedSoFar++;
      }
   }

   protected boolean canStillUse(ServerLevel param0, Villager param1, long param2) {
      return this.timeWorkedSoFar < 200;
   }
}
