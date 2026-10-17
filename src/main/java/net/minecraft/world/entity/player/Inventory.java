package net.minecraft.world.entity.player;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class Inventory implements Container, Nameable {
   public static final int POP_TIME_DURATION = 5;
   public static final int INVENTORY_SIZE = 36;
   private static final int SELECTION_SIZE = 9;
   public static final int SLOT_OFFHAND = 40;
   public static final int NOT_FOUND_INDEX = -1;
   public static final int[] ALL_ARMOR_SLOTS = new int[]{0, 1, 2, 3};
   public static final int[] HELMET_SLOT_ONLY = new int[]{3};
   public final NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
   public final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
   public final NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);
   private final List<NonNullList<ItemStack>> compartments = ImmutableList.of(this.items, this.armor, this.offhand);
   public int selected;
   public final Player player;
   private int timesChanged;

   public Inventory(Player param0) {
      this.player = param0;
   }

   public ItemStack getSelected() {
      return isHotbarSlot(this.selected) ? this.items.get(this.selected) : ItemStack.EMPTY;
   }

   public static int getSelectionSize() {
      return 9;
   }

   private boolean hasRemainingSpaceForItem(ItemStack param0, ItemStack param1) {
      return !param0.isEmpty()
         && ItemStack.isSameItemSameTags(param0, param1)
         && param0.isStackable()
         && param0.getCount() < param0.getMaxStackSize()
         && param0.getCount() < this.getMaxStackSize();
   }

   public int getFreeSlot() {
      for (int var1 = 0; var1 < this.items.size(); var1++) {
         if (this.items.get(var1).isEmpty()) {
            return var1;
         }
      }

      return -1;
   }

   public void setPickedItem(ItemStack param0) {
      int var2 = this.findSlotMatchingItem(param0);
      if (isHotbarSlot(var2)) {
         this.selected = var2;
      } else {
         if (var2 == -1) {
            this.selected = this.getSuitableHotbarSlot();
            if (!this.items.get(this.selected).isEmpty()) {
               int var3 = this.getFreeSlot();
               if (var3 != -1) {
                  this.items.set(var3, this.items.get(this.selected));
               }
            }

            this.items.set(this.selected, param0);
         } else {
            this.pickSlot(var2);
         }
      }
   }

   public void pickSlot(int param0) {
      this.selected = this.getSuitableHotbarSlot();
      ItemStack var2 = this.items.get(this.selected);
      this.items.set(this.selected, this.items.get(param0));
      this.items.set(param0, var2);
   }

   public static boolean isHotbarSlot(int param0) {
      return param0 >= 0 && param0 < 9;
   }

   public int findSlotMatchingItem(ItemStack param0) {
      for (int var2 = 0; var2 < this.items.size(); var2++) {
         if (!this.items.get(var2).isEmpty() && ItemStack.isSameItemSameTags(param0, this.items.get(var2))) {
            return var2;
         }
      }

      return -1;
   }

   public int findSlotMatchingUnusedItem(ItemStack param0) {
      for (int var2 = 0; var2 < this.items.size(); var2++) {
         ItemStack var3 = this.items.get(var2);
         if (!this.items.get(var2).isEmpty()
            && ItemStack.isSameItemSameTags(param0, this.items.get(var2))
            && !this.items.get(var2).isDamaged()
            && !var3.isEnchanted()
            && !var3.hasCustomHoverName()) {
            return var2;
         }
      }

      return -1;
   }

   public int getSuitableHotbarSlot() {
      for (int var1 = 0; var1 < 9; var1++) {
         int var2 = (this.selected + var1) % 9;
         if (this.items.get(var2).isEmpty()) {
            return var2;
         }
      }

      for (int var1 = 0; var1 < 9; var1++) {
         int var2 = (this.selected + var1) % 9;
         if (!this.items.get(var2).isEnchanted()) {
            return var2;
         }
      }

      return this.selected;
   }

   public void swapPaint(double param0) {
      int var3 = (int)Math.signum(param0);
      this.selected -= var3;

      while (this.selected < 0) {
         this.selected += 9;
      }

      while (this.selected >= 9) {
         this.selected -= 9;
      }
   }

   public int clearOrCountMatchingItems(Predicate<ItemStack> param0, int param1, Container param2) {
      int var4 = 0;
      boolean var5 = param1 == 0;
      var4 += ContainerHelper.clearOrCountMatchingItems(this, param0, param1 - var4, var5);
      var4 += ContainerHelper.clearOrCountMatchingItems(param2, param0, param1 - var4, var5);
      ItemStack var6 = this.player.containerMenu.getCarried();
      var4 += ContainerHelper.clearOrCountMatchingItems(var6, param0, param1 - var4, var5);
      if (var6.isEmpty()) {
         this.player.containerMenu.setCarried(ItemStack.EMPTY);
      }

      return var4;
   }

   private int addResource(ItemStack param0) {
      int var2 = this.getSlotWithRemainingSpace(param0);
      if (var2 == -1) {
         var2 = this.getFreeSlot();
      }

      return var2 == -1 ? param0.getCount() : this.addResource(var2, param0);
   }

   private int addResource(int param0, ItemStack param1) {
      Item var3 = param1.getItem();
      int var4 = param1.getCount();
      ItemStack var5 = this.getItem(param0);
      if (var5.isEmpty()) {
         var5 = new ItemStack(var3, 0);
         if (param1.hasTag()) {
            var5.setTag(param1.getTag().copy());
         }

         this.setItem(param0, var5);
      }

      int var6 = var4;
      if (var6 > var5.getMaxStackSize() - var5.getCount()) {
         var6 = var5.getMaxStackSize() - var5.getCount();
      }

      if (var6 > this.getMaxStackSize() - var5.getCount()) {
         var6 = this.getMaxStackSize() - var5.getCount();
      }

      if (var6 == 0) {
         return var4;
      }

      var4 -= var6;
      var5.grow(var6);
      var5.setPopTime(5);
      return var4;
   }

   public int getSlotWithRemainingSpace(ItemStack param0) {
      if (this.hasRemainingSpaceForItem(this.getItem(this.selected), param0)) {
         return this.selected;
      }

      if (this.hasRemainingSpaceForItem(this.getItem(40), param0)) {
         return 40;
      }

      for (int var2 = 0; var2 < this.items.size(); var2++) {
         if (this.hasRemainingSpaceForItem(this.items.get(var2), param0)) {
            return var2;
         }
      }

      return -1;
   }

   public void tick() {
      for (NonNullList<ItemStack> var2 : this.compartments) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            if (!var2.get(var3).isEmpty()) {
               var2.get(var3).inventoryTick(this.player.level, this.player, var3, this.selected == var3);
            }
         }
      }
   }

   public boolean add(ItemStack param0) {
      return this.add(-1, param0);
   }

   public boolean add(int param0, ItemStack param1) {
      if (param1.isEmpty()) {
         return false;
      }

      try {
         if (param1.isDamaged()) {
            if (param0 == -1) {
               param0 = this.getFreeSlot();
            }

            if (param0 >= 0) {
               this.items.set(param0, param1.copy());
               this.items.get(param0).setPopTime(5);
               param1.setCount(0);
               return true;
            } else if (this.player.getAbilities().instabuild) {
               param1.setCount(0);
               return true;
            } else {
               return false;
            }
         } else {
            int var3;
            do {
               var3 = param1.getCount();
               if (param0 == -1) {
                  param1.setCount(this.addResource(param1));
               } else {
                  param1.setCount(this.addResource(param0, param1));
               }
            } while (!param1.isEmpty() && param1.getCount() < var3);

            if (param1.getCount() == var3 && this.player.getAbilities().instabuild) {
               param1.setCount(0);
               return true;
            } else {
               return param1.getCount() < var3;
            }
         }
      } catch (Throwable var3) {
         CrashReport var4 = CrashReport.forThrowable(var3, "Adding item to inventory");
         CrashReportCategory var5 = var4.addCategory("Item being added");
         var5.setDetail("Item ID", Item.getId(param1.getItem()));
         var5.setDetail("Item data", param1.getDamageValue());
         var5.setDetail("Item name", () -> param1.getHoverName().getString());
         throw new ReportedException(var4);
      }
   }

   public void placeItemBackInInventory(ItemStack param0) {
      this.placeItemBackInInventory(param0, true);
   }

   public void placeItemBackInInventory(ItemStack param0, boolean param1) {
      while (!param0.isEmpty()) {
         int var3 = this.getSlotWithRemainingSpace(param0);
         if (var3 == -1) {
            var3 = this.getFreeSlot();
         }

         if (var3 == -1) {
            this.player.drop(param0, false);
            break;
         }

         int var4 = param0.getMaxStackSize() - this.getItem(var3).getCount();
         if (this.add(var3, param0.split(var4)) && param1 && this.player instanceof ServerPlayer) {
            ((ServerPlayer)this.player).connection.send(new ClientboundContainerSetSlotPacket(-2, 0, var3, this.getItem(var3)));
         }
      }
   }

   @Override
   public ItemStack removeItem(int param0, int param1) {
      List<ItemStack> var3 = null;

      for (NonNullList<ItemStack> var5 : this.compartments) {
         if (param0 < var5.size()) {
            var3 = var5;
            break;
         }

         param0 -= var5.size();
      }

      return var3 != null && !var3.get(param0).isEmpty() ? ContainerHelper.removeItem(var3, param0, param1) : ItemStack.EMPTY;
   }

   public void removeItem(ItemStack param0) {
      for (NonNullList<ItemStack> var3 : this.compartments) {
         for (int var4 = 0; var4 < var3.size(); var4++) {
            if (var3.get(var4) == param0) {
               var3.set(var4, ItemStack.EMPTY);
               break;
            }
         }
      }
   }

   @Override
   public ItemStack removeItemNoUpdate(int param0) {
      NonNullList<ItemStack> var2 = null;

      for (NonNullList<ItemStack> var4 : this.compartments) {
         if (param0 < var4.size()) {
            var2 = var4;
            break;
         }

         param0 -= var4.size();
      }

      if (var2 != null && !var2.get(param0).isEmpty()) {
         ItemStack var3 = var2.get(param0);
         var2.set(param0, ItemStack.EMPTY);
         return var3;
      } else {
         return ItemStack.EMPTY;
      }
   }

   @Override
   public void setItem(int param0, ItemStack param1) {
      NonNullList<ItemStack> var3 = null;

      for (NonNullList<ItemStack> var5 : this.compartments) {
         if (param0 < var5.size()) {
            var3 = var5;
            break;
         }

         param0 -= var5.size();
      }

      if (var3 != null) {
         var3.set(param0, param1);
      }
   }

   public float getDestroySpeed(BlockState param0) {
      return this.items.get(this.selected).getDestroySpeed(param0);
   }

   public ListTag save(ListTag param0) {
      for (int var2 = 0; var2 < this.items.size(); var2++) {
         if (!this.items.get(var2).isEmpty()) {
            CompoundTag var3 = new CompoundTag();
            var3.putByte("Slot", (byte)var2);
            this.items.get(var2).save(var3);
            param0.add(var3);
         }
      }

      for (int var2 = 0; var2 < this.armor.size(); var2++) {
         if (!this.armor.get(var2).isEmpty()) {
            CompoundTag var3 = new CompoundTag();
            var3.putByte("Slot", (byte)(var2 + 100));
            this.armor.get(var2).save(var3);
            param0.add(var3);
         }
      }

      for (int var2 = 0; var2 < this.offhand.size(); var2++) {
         if (!this.offhand.get(var2).isEmpty()) {
            CompoundTag var3 = new CompoundTag();
            var3.putByte("Slot", (byte)(var2 + 150));
            this.offhand.get(var2).save(var3);
            param0.add(var3);
         }
      }

      return param0;
   }

   public void load(ListTag param0) {
      this.items.clear();
      this.armor.clear();
      this.offhand.clear();

      for (int var2 = 0; var2 < param0.size(); var2++) {
         CompoundTag var3 = param0.getCompound(var2);
         int var4 = var3.getByte("Slot") & 255;
         ItemStack var5 = ItemStack.of(var3);
         if (!var5.isEmpty()) {
            if (var4 >= 0 && var4 < this.items.size()) {
               this.items.set(var4, var5);
            } else if (var4 >= 100 && var4 < this.armor.size() + 100) {
               this.armor.set(var4 - 100, var5);
            } else if (var4 >= 150 && var4 < this.offhand.size() + 150) {
               this.offhand.set(var4 - 150, var5);
            }
         }
      }
   }

   @Override
   public int getContainerSize() {
      return this.items.size() + this.armor.size() + this.offhand.size();
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack var2 : this.items) {
         if (!var2.isEmpty()) {
            return false;
         }
      }

      for (ItemStack var2 : this.armor) {
         if (!var2.isEmpty()) {
            return false;
         }
      }

      for (ItemStack var2 : this.offhand) {
         if (!var2.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(int param0) {
      List<ItemStack> var2 = null;

      for (NonNullList<ItemStack> var4 : this.compartments) {
         if (param0 < var4.size()) {
            var2 = var4;
            break;
         }

         param0 -= var4.size();
      }

      return var2 == null ? ItemStack.EMPTY : var2.get(param0);
   }

   @Override
   public Component getName() {
      return Component.translatable("container.inventory");
   }

   public ItemStack getArmor(int param0) {
      return this.armor.get(param0);
   }

   public void hurtArmor(DamageSource param0, float param1, int[] param2) {
      if (!(param1 <= 0.0F)) {
         param1 /= 4.0F;
         if (param1 < 1.0F) {
            param1 = 1.0F;
         }

         for (int var7 : param2) {
            ItemStack var8 = this.armor.get(var7);
            if ((!param0.is(DamageTypeTags.IS_FIRE) || !var8.getItem().isFireResistant()) && var8.getItem() instanceof ArmorItem) {
               var8.hurtAndBreak(
                  (int)param1, this.player, l_param1 -> l_param1.broadcastBreakEvent(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, var7))
               );
            }
         }
      }
   }

   public void dropAll() {
      for (List<ItemStack> var2 : this.compartments) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            ItemStack var4 = var2.get(var3);
            if (!var4.isEmpty()) {
               this.player.drop(var4, true, false);
               var2.set(var3, ItemStack.EMPTY);
            }
         }
      }
   }

   @Override
   public void setChanged() {
      this.timesChanged++;
   }

   public int getTimesChanged() {
      return this.timesChanged;
   }

   @Override
   public boolean stillValid(Player param0) {
      return this.player.isRemoved() ? false : !(param0.distanceToSqr(this.player) > 64.0);
   }

   public boolean contains(ItemStack param0) {
      for (List<ItemStack> var3 : this.compartments) {
         for (ItemStack var5 : var3) {
            if (!var5.isEmpty() && ItemStack.isSameItemSameTags(var5, param0)) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean contains(TagKey<Item> param0) {
      for (List<ItemStack> var3 : this.compartments) {
         for (ItemStack var5 : var3) {
            if (!var5.isEmpty() && var5.is(param0)) {
               return true;
            }
         }
      }

      return false;
   }

   public void replaceWith(Inventory param0) {
      for (int var2 = 0; var2 < this.getContainerSize(); var2++) {
         this.setItem(var2, param0.getItem(var2));
      }

      this.selected = param0.selected;
   }

   @Override
   public void clearContent() {
      for (List<ItemStack> var2 : this.compartments) {
         var2.clear();
      }
   }

   public void fillStackedContents(StackedContents param0) {
      for (ItemStack var3 : this.items) {
         param0.accountSimpleStack(var3);
      }
   }

   public ItemStack removeFromSelected(boolean param0) {
      ItemStack var2 = this.getSelected();
      return var2.isEmpty() ? ItemStack.EMPTY : this.removeItem(this.selected, param0 ? var2.getCount() : 1);
   }
}
