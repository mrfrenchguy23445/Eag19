package net.minecraft.world.inventory;

import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class AnvilMenu extends ItemCombinerMenu {
   public static final int INPUT_SLOT = 0;
   public static final int ADDITIONAL_SLOT = 1;
   public static final int RESULT_SLOT = 2;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final boolean DEBUG_COST = false;
   public static final int MAX_NAME_LENGTH = 50;
   private int repairItemCountCost;
   private String itemName;
   private final DataSlot cost = DataSlot.standalone();
   private static final int COST_FAIL = 0;
   private static final int COST_BASE = 1;
   private static final int COST_ADDED_BASE = 1;
   private static final int COST_REPAIR_MATERIAL = 1;
   private static final int COST_REPAIR_SACRIFICE = 2;
   private static final int COST_INCOMPATIBLE_PENALTY = 1;
   private static final int COST_RENAME = 1;
   private static final int INPUT_SLOT_X_PLACEMENT = 27;
   private static final int ADDITIONAL_SLOT_X_PLACEMENT = 76;
   private static final int RESULT_SLOT_X_PLACEMENT = 134;
   private static final int SLOT_Y_PLACEMENT = 47;

   public AnvilMenu(int param0, Inventory param1) {
      this(param0, param1, ContainerLevelAccess.NULL);
   }

   public AnvilMenu(int param0, Inventory param1, ContainerLevelAccess param2) {
      super(MenuType.ANVIL, param0, param1, param2);
      this.addDataSlot(this.cost);
   }

   @Override
   protected ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
      return ItemCombinerMenuSlotDefinition.create()
         .withSlot(0, 27, 47, l_param0 -> true)
         .withSlot(1, 76, 47, l_param0 -> true)
         .withResultSlot(2, 134, 47)
         .build();
   }

   @Override
   protected boolean isValidBlock(BlockState param0) {
      return param0.is(BlockTags.ANVIL);
   }

   @Override
   protected boolean mayPickup(Player param0, boolean param1) {
      return (param0.getAbilities().instabuild || param0.experienceLevel >= this.cost.get()) && this.cost.get() > 0;
   }

   @Override
   protected void onTake(Player param0, ItemStack param1) {
      if (!param0.getAbilities().instabuild) {
         param0.giveExperienceLevels(-this.cost.get());
      }

      this.inputSlots.setItem(0, ItemStack.EMPTY);
      if (this.repairItemCountCost > 0) {
         ItemStack var3 = this.inputSlots.getItem(1);
         if (!var3.isEmpty() && var3.getCount() > this.repairItemCountCost) {
            var3.shrink(this.repairItemCountCost);
            this.inputSlots.setItem(1, var3);
         } else {
            this.inputSlots.setItem(1, ItemStack.EMPTY);
         }
      } else {
         this.inputSlots.setItem(1, ItemStack.EMPTY);
      }

      this.cost.set(0);
      this.access.execute((l_param1, l_param2) -> {
         BlockState l_var3 = l_param1.getBlockState(l_param2);
         if (!param0.getAbilities().instabuild && l_var3.is(BlockTags.ANVIL) && param0.getRandom().nextFloat() < 0.12F) {
            BlockState l_var4 = AnvilBlock.damage(l_var3);
            if (l_var4 == null) {
               l_param1.removeBlock(l_param2, false);
               l_param1.levelEvent(1029, l_param2, 0);
            } else {
               l_param1.setBlock(l_param2, l_var4, 2);
               l_param1.levelEvent(1030, l_param2, 0);
            }
         } else {
            l_param1.levelEvent(1030, l_param2, 0);
         }
      });
   }

   @Override
   public void createResult() {
      ItemStack var1 = this.inputSlots.getItem(0);
      this.cost.set(1);
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;
      if (var1.isEmpty()) {
         this.resultSlots.setItem(0, ItemStack.EMPTY);
         this.cost.set(0);
      } else {
         ItemStack var5 = var1.copy();
         ItemStack var6 = this.inputSlots.getItem(1);
         Map<Enchantment, Integer> var7 = EnchantmentHelper.getEnchantments(var5);
         var3 += var1.getBaseRepairCost() + (var6.isEmpty() ? 0 : var6.getBaseRepairCost());
         this.repairItemCountCost = 0;
         if (!var6.isEmpty()) {
            boolean var8 = var6.is(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantments(var6).isEmpty();
            if (var5.isDamageableItem() && var5.getItem().isValidRepairItem(var1, var6)) {
               int var9 = Math.min(var5.getDamageValue(), var5.getMaxDamage() / 4);
               if (var9 <= 0) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.cost.set(0);
                  return;
               }

               int var10;
               for (var10 = 0; var9 > 0 && var10 < var6.getCount(); var10++) {
                  int var11 = var5.getDamageValue() - var9;
                  var5.setDamageValue(var11);
                  var2++;
                  var9 = Math.min(var5.getDamageValue(), var5.getMaxDamage() / 4);
               }

               this.repairItemCountCost = var10;
            } else {
               if (!var8 && (!var5.is(var6.getItem()) || !var5.isDamageableItem())) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.cost.set(0);
                  return;
               }

               if (var5.isDamageableItem() && !var8) {
                  int var9 = var1.getMaxDamage() - var1.getDamageValue();
                  int var10 = var6.getMaxDamage() - var6.getDamageValue();
                  int var11 = var10 + var5.getMaxDamage() * 12 / 100;
                  int var12 = var9 + var11;
                  int var13 = var5.getMaxDamage() - var12;
                  if (var13 < 0) {
                     var13 = 0;
                  }

                  if (var13 < var5.getDamageValue()) {
                     var5.setDamageValue(var13);
                     var2 += 2;
                  }
               }

               Map<Enchantment, Integer> var9 = EnchantmentHelper.getEnchantments(var6);
               boolean var10 = false;
               boolean var11 = false;

               for (Enchantment var13 : var9.keySet()) {
                  if (var13 != null) {
                     int var14 = var7.getOrDefault(var13, 0);
                     int var15 = var9.get(var13);
                     var15 = var14 == var15 ? var15 + 1 : Math.max(var15, var14);
                     boolean var16 = var13.canEnchant(var1);
                     if (this.player.getAbilities().instabuild || var1.is(Items.ENCHANTED_BOOK)) {
                        var16 = true;
                     }

                     for (Enchantment var18 : var7.keySet()) {
                        if (var18 != var13 && !var13.isCompatibleWith(var18)) {
                           var16 = false;
                           var2++;
                        }
                     }

                     if (!var16) {
                        var11 = true;
                     } else {
                        var10 = true;
                        if (var15 > var13.getMaxLevel()) {
                           var15 = var13.getMaxLevel();
                        }

                        var7.put(var13, var15);
                        int var17 = 0;
                        switch (var13.getRarity()) {
                           case COMMON:
                              var17 = 1;
                              break;
                           case UNCOMMON:
                              var17 = 2;
                              break;
                           case RARE:
                              var17 = 4;
                              break;
                           case VERY_RARE:
                              var17 = 8;
                        }

                        if (var8) {
                           var17 = Math.max(1, var17 / 2);
                        }

                        var2 += var17 * var15;
                        if (var1.getCount() > 1) {
                           var2 = 40;
                        }
                     }
                  }
               }

               if (var11 && !var10) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.cost.set(0);
                  return;
               }
            }
         }

         if (StringUtils.isBlank(this.itemName)) {
            if (var1.hasCustomHoverName()) {
               var4 = 1;
               var2 += var4;
               var5.resetHoverName();
            }
         } else if (!this.itemName.equals(var1.getHoverName().getString())) {
            var4 = 1;
            var2 += var4;
            var5.setHoverName(Component.literal(this.itemName));
         }

         this.cost.set(var3 + var2);
         if (var2 <= 0) {
            var5 = ItemStack.EMPTY;
         }

         if (var4 == var2 && var4 > 0 && this.cost.get() >= 40) {
            this.cost.set(39);
         }

         if (this.cost.get() >= 40 && !this.player.getAbilities().instabuild) {
            var5 = ItemStack.EMPTY;
         }

         if (!var5.isEmpty()) {
            int var8 = var5.getBaseRepairCost();
            if (!var6.isEmpty() && var8 < var6.getBaseRepairCost()) {
               var8 = var6.getBaseRepairCost();
            }

            if (var4 != var2 || var4 == 0) {
               var8 = calculateIncreasedRepairCost(var8);
            }

            var5.setRepairCost(var8);
            EnchantmentHelper.setEnchantments(var7, var5);
         }

         this.resultSlots.setItem(0, var5);
         this.broadcastChanges();
      }
   }

   public static int calculateIncreasedRepairCost(int param0) {
      return param0 * 2 + 1;
   }

   public void setItemName(String param0) {
      this.itemName = param0;
      if (this.getSlot(2).hasItem()) {
         ItemStack var2 = this.getSlot(2).getItem();
         if (StringUtils.isBlank(param0)) {
            var2.resetHoverName();
         } else {
            var2.setHoverName(Component.literal(this.itemName));
         }
      }

      this.createResult();
   }

   public int getCost() {
      return this.cost.get();
   }
}
