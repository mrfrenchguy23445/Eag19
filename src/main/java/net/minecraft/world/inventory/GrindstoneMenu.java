package net.minecraft.world.inventory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class GrindstoneMenu extends AbstractContainerMenu {
   public static final int MAX_NAME_LENGTH = 35;
   public static final int INPUT_SLOT = 0;
   public static final int ADDITIONAL_SLOT = 1;
   public static final int RESULT_SLOT = 2;
   private static final int INV_SLOT_START = 3;
   private static final int INV_SLOT_END = 30;
   private static final int USE_ROW_SLOT_START = 30;
   private static final int USE_ROW_SLOT_END = 39;
   private final Container resultSlots = new ResultContainer();
   final Container repairSlots = new SimpleContainer(2) {
      @Override
      public void setChanged() {
         super.setChanged();
         GrindstoneMenu.this.slotsChanged(this);
      }
   };
   private final ContainerLevelAccess access;

   public GrindstoneMenu(int param0, Inventory param1) {
      this(param0, param1, ContainerLevelAccess.NULL);
   }

   public GrindstoneMenu(int param0, Inventory param1, final ContainerLevelAccess param2) {
      super(MenuType.GRINDSTONE, param0);
      this.access = param2;
      this.addSlot(new Slot(this.repairSlots, 0, 49, 19) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return l_param0.isDamageableItem() || l_param0.is(Items.ENCHANTED_BOOK) || l_param0.isEnchanted();
         }
      });
      this.addSlot(new Slot(this.repairSlots, 1, 49, 40) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return l_param0.isDamageableItem() || l_param0.is(Items.ENCHANTED_BOOK) || l_param0.isEnchanted();
         }
      });
      this.addSlot(new Slot(this.resultSlots, 2, 129, 34) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return false;
         }

         @Override
         public void onTake(Player l_param0, ItemStack l_param1) {
            param2.execute((l2_param0, l2_param1) -> {
               if (l2_param0 instanceof ServerLevel) {
                  ExperienceOrb.award((ServerLevel)l2_param0, Vec3.atCenterOf(l2_param1), this.getExperienceAmount(l2_param0));
               }

               l2_param0.levelEvent(1042, l2_param1, 0);
            });
            GrindstoneMenu.this.repairSlots.setItem(0, ItemStack.EMPTY);
            GrindstoneMenu.this.repairSlots.setItem(1, ItemStack.EMPTY);
         }

         private int getExperienceAmount(Level l_param0) {
            int l_var2 = 0;
            l_var2 += this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(0));
            l_var2 += this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(1));
            if (l_var2 > 0) {
               int l_var3 = (int)Math.ceil(l_var2 / 2.0);
               return l_var3 + l_param0.random.nextInt(l_var3);
            } else {
               return 0;
            }
         }

         private int getExperienceFromItem(ItemStack l_param0) {
            int l_var2 = 0;
            Map<Enchantment, Integer> l_var3 = EnchantmentHelper.getEnchantments(l_param0);

            for (Entry<Enchantment, Integer> l_var5 : l_var3.entrySet()) {
               Enchantment l_var6 = l_var5.getKey();
               Integer l_var7 = l_var5.getValue();
               if (!l_var6.isCurse()) {
                  l_var2 += l_var6.getMinCost(l_var7);
               }
            }

            return l_var2;
         }
      });

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var5 = 0; var5 < 9; var5++) {
            this.addSlot(new Slot(param1, var5 + var4 * 9 + 9, 8 + var5 * 18, 84 + var4 * 18));
         }
      }

      for (int var4 = 0; var4 < 9; var4++) {
         this.addSlot(new Slot(param1, var4, 8 + var4 * 18, 142));
      }
   }

   @Override
   public void slotsChanged(Container param0) {
      super.slotsChanged(param0);
      if (param0 == this.repairSlots) {
         this.createResult();
      }
   }

   private void createResult() {
      ItemStack var1 = this.repairSlots.getItem(0);
      ItemStack var2 = this.repairSlots.getItem(1);
      boolean var3 = !var1.isEmpty() || !var2.isEmpty();
      boolean var4 = !var1.isEmpty() && !var2.isEmpty();
      if (!var3) {
         this.resultSlots.setItem(0, ItemStack.EMPTY);
      } else {
         boolean var5 = !var1.isEmpty() && !var1.is(Items.ENCHANTED_BOOK) && !var1.isEnchanted()
            || !var2.isEmpty() && !var2.is(Items.ENCHANTED_BOOK) && !var2.isEnchanted();
         if (var1.getCount() > 1 || var2.getCount() > 1 || !var4 && var5) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.broadcastChanges();
            return;
         }

         int var7 = 1;
         int var6;
         ItemStack var8;
         if (var4) {
            if (!var1.is(var2.getItem())) {
               this.resultSlots.setItem(0, ItemStack.EMPTY);
               this.broadcastChanges();
               return;
            }

            Item var9 = var1.getItem();
            int var10 = var9.getMaxDamage() - var1.getDamageValue();
            int var11 = var9.getMaxDamage() - var2.getDamageValue();
            int var12 = var10 + var11 + var9.getMaxDamage() * 5 / 100;
            var6 = Math.max(var9.getMaxDamage() - var12, 0);
            var8 = this.mergeEnchants(var1, var2);
            if (!var8.isDamageableItem()) {
               if (!ItemStack.matches(var1, var2)) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.broadcastChanges();
                  return;
               }

               var7 = 2;
            }
         } else {
            boolean var9 = !var1.isEmpty();
            var6 = var9 ? var1.getDamageValue() : var2.getDamageValue();
            var8 = var9 ? var1 : var2;
         }

         this.resultSlots.setItem(0, this.removeNonCurses(var8, var6, var7));
      }

      this.broadcastChanges();
   }

   private ItemStack mergeEnchants(ItemStack param0, ItemStack param1) {
      ItemStack var3 = param0.copy();
      Map<Enchantment, Integer> var4 = EnchantmentHelper.getEnchantments(param1);

      for (Entry<Enchantment, Integer> var6 : var4.entrySet()) {
         Enchantment var7 = var6.getKey();
         if (!var7.isCurse() || EnchantmentHelper.getItemEnchantmentLevel(var7, var3) == 0) {
            var3.enchant(var7, var6.getValue());
         }
      }

      return var3;
   }

   private ItemStack removeNonCurses(ItemStack param0, int param1, int param2) {
      ItemStack var4 = param0.copy();
      var4.removeTagKey("Enchantments");
      var4.removeTagKey("StoredEnchantments");
      if (param1 > 0) {
         var4.setDamageValue(param1);
      } else {
         var4.removeTagKey("Damage");
      }

      var4.setCount(param2);
      Map<Enchantment, Integer> var5 = EnchantmentHelper.getEnchantments(param0)
         .entrySet()
         .stream()
         .filter(l_param0 -> l_param0.getKey().isCurse())
         .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      EnchantmentHelper.setEnchantments(var5, var4);
      var4.setRepairCost(0);
      if (var4.is(Items.ENCHANTED_BOOK) && var5.size() == 0) {
         var4 = new ItemStack(Items.BOOK);
         if (param0.hasCustomHoverName()) {
            var4.setHoverName(param0.getHoverName());
         }
      }

      for (int var6 = 0; var6 < var5.size(); var6++) {
         var4.setRepairCost(AnvilMenu.calculateIncreasedRepairCost(var4.getBaseRepairCost()));
      }

      return var4;
   }

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      this.access.execute((l_param1, l_param2) -> this.clearContainer(param0, this.repairSlots));
   }

   @Override
   public boolean stillValid(Player param0) {
      return stillValid(this.access, param0, Blocks.GRINDSTONE);
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         ItemStack var6 = this.repairSlots.getItem(0);
         ItemStack var7 = this.repairSlots.getItem(1);
         if (param1 == 2) {
            if (!this.moveItemStackTo(var5, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            var4.onQuickCraft(var5, var3);
         } else if (param1 != 0 && param1 != 1) {
            if (!var6.isEmpty() && !var7.isEmpty()) {
               if (param1 >= 3 && param1 < 30) {
                  if (!this.moveItemStackTo(var5, 30, 39, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (param1 >= 30 && param1 < 39 && !this.moveItemStackTo(var5, 3, 30, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(var5, 0, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(var5, 3, 39, false)) {
            return ItemStack.EMPTY;
         }

         if (var5.isEmpty()) {
            var4.setByPlayer(ItemStack.EMPTY);
         } else {
            var4.setChanged();
         }

         if (var5.getCount() == var3.getCount()) {
            return ItemStack.EMPTY;
         }

         var4.onTake(param0, var5);
      }

      return var3;
   }
}
