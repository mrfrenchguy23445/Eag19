package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TradeWithVillager extends Behavior<Villager> {
   private static final int INTERACT_DIST_SQR = 5;
   private static final float SPEED_MODIFIER = 0.5F;
   private Set<Item> trades = ImmutableSet.of();

   public TradeWithVillager() {
      super(
         ImmutableMap.of(
            MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
         )
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Villager param1) {
      return BehaviorUtils.targetIsValid(param1.getBrain(), MemoryModuleType.INTERACTION_TARGET, EntityType.VILLAGER);
   }

   protected boolean canStillUse(ServerLevel param0, Villager param1, long param2) {
      return this.checkExtraStartConditions(param0, param1);
   }

   protected void start(ServerLevel param0, Villager param1, long param2) {
      Villager var5 = (Villager)param1.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(param1, var5, 0.5F);
      this.trades = figureOutWhatIAmWillingToTrade(param1, var5);
   }

   protected void tick(ServerLevel param0, Villager param1, long param2) {
      Villager var5 = (Villager)param1.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      if (!(param1.distanceToSqr(var5) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(param1, var5, 0.5F);
         param1.gossip(param0, var5, param2);
         if (param1.hasExcessFood() && (param1.getVillagerData().getProfession() == VillagerProfession.FARMER || var5.wantsMoreFood())) {
            throwHalfStack(param1, Villager.FOOD_POINTS.keySet(), var5);
         }

         if (var5.getVillagerData().getProfession() == VillagerProfession.FARMER
            && param1.getInventory().countItem(Items.WHEAT) > Items.WHEAT.getMaxStackSize() / 2) {
            throwHalfStack(param1, ImmutableSet.of(Items.WHEAT), var5);
         }

         if (!this.trades.isEmpty() && param1.getInventory().hasAnyOf(this.trades)) {
            throwHalfStack(param1, this.trades, var5);
         }
      }
   }

   protected void stop(ServerLevel param0, Villager param1, long param2) {
      param1.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
   }

   private static Set<Item> figureOutWhatIAmWillingToTrade(Villager param0, Villager param1) {
      ImmutableSet<Item> var2 = param1.getVillagerData().getProfession().requestedItems();
      ImmutableSet<Item> var3 = param0.getVillagerData().getProfession().requestedItems();
      return var2.stream().filter(l_param1 -> !var3.contains(l_param1)).collect(Collectors.toSet());
   }

   private static void throwHalfStack(Villager param0, Set<Item> param1, LivingEntity param2) {
      SimpleContainer var3 = param0.getInventory();
      ItemStack var4 = ItemStack.EMPTY;
      int var5 = 0;

      while (var5 < var3.getContainerSize()) {
         ItemStack var6;
         Item var7;
         int var8;
         label28: {
            var6 = var3.getItem(var5);
            if (!var6.isEmpty()) {
               var7 = var6.getItem();
               if (param1.contains(var7)) {
                  if (var6.getCount() > var6.getMaxStackSize() / 2) {
                     var8 = var6.getCount() / 2;
                     break label28;
                  }

                  if (var6.getCount() > 24) {
                     var8 = var6.getCount() - 24;
                     break label28;
                  }
               }
            }

            var5++;
            continue;
         }

         var6.shrink(var8);
         var4 = new ItemStack(var7, var8);
         break;
      }

      if (!var4.isEmpty()) {
         BehaviorUtils.throwItem(param0, var4, param2.position());
      }
   }
}
