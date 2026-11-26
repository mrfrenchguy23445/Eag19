package net.minecraft.world.inventory;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

public abstract class AbstractContainerMenu {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int SLOT_CLICKED_OUTSIDE = -999;
   public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;
   public static final int QUICKCRAFT_TYPE_GREEDY = 1;
   public static final int QUICKCRAFT_TYPE_CLONE = 2;
   public static final int QUICKCRAFT_HEADER_START = 0;
   public static final int QUICKCRAFT_HEADER_CONTINUE = 1;
   public static final int QUICKCRAFT_HEADER_END = 2;
   public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;
   private final NonNullList<ItemStack> lastSlots = NonNullList.create();
   public final NonNullList<Slot> slots = NonNullList.create();
   private final List<DataSlot> dataSlots = Lists.newArrayList();
   private ItemStack carried = ItemStack.EMPTY;
   private final NonNullList<ItemStack> remoteSlots = NonNullList.create();
   private final IntList remoteDataSlots = new IntArrayList();
   private ItemStack remoteCarried = ItemStack.EMPTY;
   private int stateId;
   @Nullable
   private final MenuType<?> menuType;
   public final int containerId;
   private int quickcraftType = -1;
   private int quickcraftStatus;
   private final Set<Slot> quickcraftSlots = Sets.newHashSet();
   private final List<ContainerListener> containerListeners = Lists.newArrayList();
   @Nullable
   private ContainerSynchronizer synchronizer;
   private boolean suppressRemoteUpdates;

   protected AbstractContainerMenu(@Nullable MenuType<?> param0, int param1) {
      this.menuType = param0;
      this.containerId = param1;
   }

   protected static boolean stillValid(ContainerLevelAccess param0, Player param1, Block param2) {
      return param0.evaluate(
         (l_param2, l_param3) -> !l_param2.getBlockState(l_param3).is(param2)
            ? false
            : param1.distanceToSqr(l_param3.getX() + 0.5, l_param3.getY() + 0.5, l_param3.getZ() + 0.5) <= 64.0,
         true
      );
   }

   public MenuType<?> getType() {
      if (this.menuType == null) {
         throw new UnsupportedOperationException("Unable to construct this menu by type");
      } else {
         return this.menuType;
      }
   }

   protected static void checkContainerSize(Container param0, int param1) {
      int var2 = param0.getContainerSize();
      if (var2 < param1) {
         throw new IllegalArgumentException("Container size " + var2 + " is smaller than expected " + param1);
      }
   }

   protected static void checkContainerDataCount(ContainerData param0, int param1) {
      int var2 = param0.getCount();
      if (var2 < param1) {
         throw new IllegalArgumentException("Container data count " + var2 + " is smaller than expected " + param1);
      }
   }

   public boolean isValidSlotIndex(int param0) {
      return param0 == -1 || param0 == -999 || param0 < this.slots.size();
   }

   protected Slot addSlot(Slot param0) {
      param0.index = this.slots.size();
      this.slots.add(param0);
      this.lastSlots.add(ItemStack.EMPTY);
      this.remoteSlots.add(ItemStack.EMPTY);
      return param0;
   }

   protected DataSlot addDataSlot(DataSlot param0) {
      this.dataSlots.add(param0);
      this.remoteDataSlots.add(0);
      return param0;
   }

   protected void addDataSlots(ContainerData param0) {
      for (int var2 = 0; var2 < param0.getCount(); var2++) {
         this.addDataSlot(DataSlot.forContainer(param0, var2));
      }
   }

   public void addSlotListener(ContainerListener param0) {
      if (!this.containerListeners.contains(param0)) {
         this.containerListeners.add(param0);
         this.broadcastChanges();
      }
   }

   public void setSynchronizer(ContainerSynchronizer param0) {
      this.synchronizer = param0;
      this.sendAllDataToRemote();
   }

   public void sendAllDataToRemote() {
      int var1 = 0;

      for (int var2 = this.slots.size(); var1 < var2; var1++) {
         this.remoteSlots.set(var1, this.slots.get(var1).getItem().copy());
      }

      this.remoteCarried = this.getCarried().copy();
      var1 = 0;

      for (int var2 = this.dataSlots.size(); var1 < var2; var1++) {
         this.remoteDataSlots.set(var1, this.dataSlots.get(var1).get());
      }

      if (this.synchronizer != null) {
         this.synchronizer.sendInitialData(this, this.remoteSlots, this.remoteCarried, this.remoteDataSlots.toIntArray());
      }
   }

   public void removeSlotListener(ContainerListener param0) {
      this.containerListeners.remove(param0);
   }

   public NonNullList<ItemStack> getItems() {
      NonNullList<ItemStack> var1 = NonNullList.create();

      for (Slot var3 : this.slots) {
         var1.add(var3.getItem());
      }

      return var1;
   }

   public void broadcastChanges() {
      for (int var1 = 0; var1 < this.slots.size(); var1++) {
         ItemStack var2 = this.slots.get(var1).getItem();
         Supplier<ItemStack> var3 = Suppliers.<ItemStack>memoize(var2::copy);
         this.triggerSlotListeners(var1, var2, var3);
         this.synchronizeSlotToRemote(var1, var2, var3);
      }

      this.synchronizeCarriedToRemote();

      for (int var1 = 0; var1 < this.dataSlots.size(); var1++) {
         DataSlot var2 = this.dataSlots.get(var1);
         int var3 = var2.get();
         if (var2.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(var1, var3);
         }

         this.synchronizeDataSlotToRemote(var1, var3);
      }
   }

   public void broadcastFullState() {
      for (int var1 = 0; var1 < this.slots.size(); var1++) {
         ItemStack var2 = this.slots.get(var1).getItem();
         this.triggerSlotListeners(var1, var2, var2::copy);
      }

      for (int var1 = 0; var1 < this.dataSlots.size(); var1++) {
         DataSlot var2 = this.dataSlots.get(var1);
         if (var2.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(var1, var2.get());
         }
      }

      this.sendAllDataToRemote();
   }

   private void updateDataSlotListeners(int param0, int param1) {
      for (ContainerListener var4 : this.containerListeners) {
         var4.dataChanged(this, param0, param1);
      }
   }

   private void triggerSlotListeners(int param0, ItemStack param1, Supplier<ItemStack> param2) {
      ItemStack var4 = this.lastSlots.get(param0);
      if (!ItemStack.matches(var4, param1)) {
         ItemStack var5 = param2.get();
         this.lastSlots.set(param0, var5);

         for (ContainerListener var7 : this.containerListeners) {
            var7.slotChanged(this, param0, var5);
         }
      }
   }

   private void synchronizeSlotToRemote(int param0, ItemStack param1, Supplier<ItemStack> param2) {
      if (!this.suppressRemoteUpdates) {
         ItemStack var4 = this.remoteSlots.get(param0);
         if (!ItemStack.matches(var4, param1)) {
            ItemStack var5 = param2.get();
            this.remoteSlots.set(param0, var5);
            if (this.synchronizer != null) {
               this.synchronizer.sendSlotChange(this, param0, var5);
            }
         }
      }
   }

   private void synchronizeDataSlotToRemote(int param0, int param1) {
      if (!this.suppressRemoteUpdates) {
         int var3 = this.remoteDataSlots.getInt(param0);
         if (var3 != param1) {
            this.remoteDataSlots.set(param0, param1);
            if (this.synchronizer != null) {
               this.synchronizer.sendDataChange(this, param0, param1);
            }
         }
      }
   }

   private void synchronizeCarriedToRemote() {
      if (!this.suppressRemoteUpdates) {
         if (!ItemStack.matches(this.getCarried(), this.remoteCarried)) {
            this.remoteCarried = this.getCarried().copy();
            if (this.synchronizer != null) {
               this.synchronizer.sendCarriedChange(this, this.remoteCarried);
            }
         }
      }
   }

   public void setRemoteSlot(int param0, ItemStack param1) {
      this.remoteSlots.set(param0, param1.copy());
   }

   public void setRemoteSlotNoCopy(int param0, ItemStack param1) {
      if (param0 >= 0 && param0 < this.remoteSlots.size()) {
         this.remoteSlots.set(param0, param1);
      } else {
         LOGGER.debug("Incorrect slot index: {} available slots: {}", param0, this.remoteSlots.size());
      }
   }

   public void setRemoteCarried(ItemStack param0) {
      this.remoteCarried = param0.copy();
   }

   public boolean clickMenuButton(Player param0, int param1) {
      return false;
   }

   public Slot getSlot(int param0) {
      return this.slots.get(param0);
   }

   public abstract ItemStack quickMoveStack(Player var1, int var2);

   public void clicked(int param0, int param1, ClickType param2, Player param3) {
      try {
         this.doClick(param0, param1, param2, param3);
      } catch (Exception var5) {
         CrashReport var6 = CrashReport.forThrowable(var5, "Container click");
         CrashReportCategory var7 = var6.addCategory("Click info");
         var7.setDetail("Menu Type", () -> this.menuType != null ? BuiltInRegistries.MENU.getKey(this.menuType).toString() : "<no type>");
         var7.setDetail("Menu Class", () -> this.getClass().getCanonicalName());
         var7.setDetail("Slot Count", this.slots.size());
         var7.setDetail("Slot", param0);
         var7.setDetail("Button", param1);
         var7.setDetail("Type", param2);
         throw new ReportedException(var6);
      }
   }

   private void doClick(int param0, int param1, ClickType param2, Player param3) {
      Inventory var5 = param3.getInventory();
      if (param2 == ClickType.QUICK_CRAFT) {
         int var6 = this.quickcraftStatus;
         this.quickcraftStatus = getQuickcraftHeader(param1);
         if ((var6 != 1 || this.quickcraftStatus != 2) && var6 != this.quickcraftStatus) {
            this.resetQuickCraft();
         } else if (this.getCarried().isEmpty()) {
            this.resetQuickCraft();
         } else if (this.quickcraftStatus == 0) {
            this.quickcraftType = getQuickcraftType(param1);
            if (isValidQuickcraftType(this.quickcraftType, param3)) {
               this.quickcraftStatus = 1;
               this.quickcraftSlots.clear();
            } else {
               this.resetQuickCraft();
            }
         } else if (this.quickcraftStatus == 1) {
            Slot var7 = this.slots.get(param0);
            ItemStack var8 = this.getCarried();
            if (canItemQuickReplace(var7, var8, true)
               && var7.mayPlace(var8)
               && (this.quickcraftType == 2 || var8.getCount() > this.quickcraftSlots.size())
               && this.canDragTo(var7)) {
               this.quickcraftSlots.add(var7);
            }
         } else if (this.quickcraftStatus == 2) {
            if (!this.quickcraftSlots.isEmpty()) {
               if (this.quickcraftSlots.size() == 1) {
                  int var7 = this.quickcraftSlots.iterator().next().index;
                  this.resetQuickCraft();
                  this.doClick(var7, this.quickcraftType, ClickType.PICKUP, param3);
                  return;
               }

               ItemStack var7 = this.getCarried().copy();
               int var8 = this.getCarried().getCount();

               for (Slot var10 : this.quickcraftSlots) {
                  ItemStack var11 = this.getCarried();
                  if (var10 != null
                     && canItemQuickReplace(var10, var11, true)
                     && var10.mayPlace(var11)
                     && (this.quickcraftType == 2 || var11.getCount() >= this.quickcraftSlots.size())
                     && this.canDragTo(var10)) {
                     ItemStack var12 = var7.copy();
                     int var13 = var10.hasItem() ? var10.getItem().getCount() : 0;
                     getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, var12, var13);
                     int var14 = Math.min(var12.getMaxStackSize(), var10.getMaxStackSize(var12));
                     if (var12.getCount() > var14) {
                        var12.setCount(var14);
                     }

                     var8 -= var12.getCount() - var13;
                     var10.setByPlayer(var12);
                  }
               }

               var7.setCount(var8);
               this.setCarried(var7);
            }

            this.resetQuickCraft();
         } else {
            this.resetQuickCraft();
         }
      } else if (this.quickcraftStatus != 0) {
         this.resetQuickCraft();
      } else if ((param2 == ClickType.PICKUP || param2 == ClickType.QUICK_MOVE) && (param1 == 0 || param1 == 1)) {
         ClickAction var6 = param1 == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
         if (param0 == -999) {
            if (!this.getCarried().isEmpty()) {
               if (var6 == ClickAction.PRIMARY) {
                  param3.drop(this.getCarried(), true);
                  this.setCarried(ItemStack.EMPTY);
               } else {
                  param3.drop(this.getCarried().split(1), true);
               }
            }
         } else if (param2 == ClickType.QUICK_MOVE) {
            if (param0 < 0) {
               return;
            }

            Slot var7 = this.slots.get(param0);
            if (!var7.mayPickup(param3)) {
               return;
            }

            ItemStack var8 = this.quickMoveStack(param3, param0);

            while (!var8.isEmpty() && ItemStack.isSame(var7.getItem(), var8)) {
               var8 = this.quickMoveStack(param3, param0);
            }
         } else {
            if (param0 < 0) {
               return;
            }

            Slot var7 = this.slots.get(param0);
            ItemStack var8 = var7.getItem();
            ItemStack var9 = this.getCarried();
            param3.updateTutorialInventoryAction(var9, var7.getItem(), var6);
            if (!this.tryItemClickBehaviourOverride(param3, var6, var7, var8, var9)) {
               if (var8.isEmpty()) {
                  if (!var9.isEmpty()) {
                     int var10 = var6 == ClickAction.PRIMARY ? var9.getCount() : 1;
                     this.setCarried(var7.safeInsert(var9, var10));
                  }
               } else if (var7.mayPickup(param3)) {
                  if (var9.isEmpty()) {
                     int var10 = var6 == ClickAction.PRIMARY ? var8.getCount() : (var8.getCount() + 1) / 2;
                     Optional<ItemStack> var11 = var7.tryRemove(var10, Integer.MAX_VALUE, param3);
                     var11.ifPresent(l_param2 -> {
                        this.setCarried(l_param2);
                        var7.onTake(param3, l_param2);
                     });
                  } else if (var7.mayPlace(var9)) {
                     if (ItemStack.isSameItemSameTags(var8, var9)) {
                        int var10 = var6 == ClickAction.PRIMARY ? var9.getCount() : 1;
                        this.setCarried(var7.safeInsert(var9, var10));
                     } else if (var9.getCount() <= var7.getMaxStackSize(var9)) {
                        this.setCarried(var8);
                        var7.setByPlayer(var9);
                     }
                  } else if (ItemStack.isSameItemSameTags(var8, var9)) {
                     Optional<ItemStack> var10 = var7.tryRemove(var8.getCount(), var9.getMaxStackSize() - var9.getCount(), param3);
                     var10.ifPresent(l_param3 -> {
                        var9.grow(l_param3.getCount());
                        var7.onTake(param3, l_param3);
                     });
                  }
               }
            }

            var7.setChanged();
         }
      } else if (param2 == ClickType.SWAP) {
         Slot var6 = this.slots.get(param0);
         ItemStack var7 = var5.getItem(param1);
         ItemStack var8 = var6.getItem();
         if (!var7.isEmpty() || !var8.isEmpty()) {
            if (var7.isEmpty()) {
               if (var6.mayPickup(param3)) {
                  var5.setItem(param1, var8);
                  var6.onSwapCraft(var8.getCount());
                  var6.setByPlayer(ItemStack.EMPTY);
                  var6.onTake(param3, var8);
               }
            } else if (var8.isEmpty()) {
               if (var6.mayPlace(var7)) {
                  int var9 = var6.getMaxStackSize(var7);
                  if (var7.getCount() > var9) {
                     var6.setByPlayer(var7.split(var9));
                  } else {
                     var5.setItem(param1, ItemStack.EMPTY);
                     var6.setByPlayer(var7);
                  }
               }
            } else if (var6.mayPickup(param3) && var6.mayPlace(var7)) {
               int var9 = var6.getMaxStackSize(var7);
               if (var7.getCount() > var9) {
                  var6.setByPlayer(var7.split(var9));
                  var6.onTake(param3, var8);
                  if (!var5.add(var8)) {
                     param3.drop(var8, true);
                  }
               } else {
                  var5.setItem(param1, var8);
                  var6.setByPlayer(var7);
                  var6.onTake(param3, var8);
               }
            }
         }
      } else if (param2 == ClickType.CLONE && param3.getAbilities().instabuild && this.getCarried().isEmpty() && param0 >= 0) {
         Slot var6 = this.slots.get(param0);
         if (var6.hasItem()) {
            ItemStack var7 = var6.getItem().copy();
            var7.setCount(var7.getMaxStackSize());
            this.setCarried(var7);
         }
      } else if (param2 == ClickType.THROW && this.getCarried().isEmpty() && param0 >= 0) {
         Slot var6 = this.slots.get(param0);
         int var7 = param1 == 0 ? 1 : var6.getItem().getCount();
         ItemStack var8 = var6.safeTake(var7, Integer.MAX_VALUE, param3);
         param3.drop(var8, true);
      } else if (param2 == ClickType.PICKUP_ALL && param0 >= 0) {
         Slot var6 = this.slots.get(param0);
         ItemStack var7 = this.getCarried();
         if (!var7.isEmpty() && (!var6.hasItem() || !var6.mayPickup(param3))) {
            int var8 = param1 == 0 ? 0 : this.slots.size() - 1;
            int var9 = param1 == 0 ? 1 : -1;

            for (int var10 = 0; var10 < 2; var10++) {
               for (int var11 = var8; var11 >= 0 && var11 < this.slots.size() && var7.getCount() < var7.getMaxStackSize(); var11 += var9) {
                  Slot var12 = this.slots.get(var11);
                  if (var12.hasItem() && canItemQuickReplace(var12, var7, true) && var12.mayPickup(param3) && this.canTakeItemForPickAll(var7, var12)) {
                     ItemStack var13 = var12.getItem();
                     if (var10 != 0 || var13.getCount() != var13.getMaxStackSize()) {
                        ItemStack var14 = var12.safeTake(var13.getCount(), var7.getMaxStackSize() - var7.getCount(), param3);
                        var7.grow(var14.getCount());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryItemClickBehaviourOverride(Player param0, ClickAction param1, Slot param2, ItemStack param3, ItemStack param4) {
      FeatureFlagSet var6 = param0.getLevel().enabledFeatures();
      return param4.isItemEnabled(var6) && param4.overrideStackedOnOther(param2, param1, param0)
         ? true
         : param3.isItemEnabled(var6) && param3.overrideOtherStackedOnMe(param4, param2, param1, param0, this.createCarriedSlotAccess());
   }

   private SlotAccess createCarriedSlotAccess() {
      return new SlotAccess() {
         @Override
         public ItemStack get() {
            return AbstractContainerMenu.this.getCarried();
         }

         @Override
         public boolean set(ItemStack l_param0) {
            AbstractContainerMenu.this.setCarried(l_param0);
            return true;
         }
      };
   }

   public boolean canTakeItemForPickAll(ItemStack param0, Slot param1) {
      return true;
   }

   public void removed(Player param0) {
      if (param0 instanceof ServerPlayer) {
         ItemStack var2 = this.getCarried();
         if (!var2.isEmpty()) {
            if (param0.isAlive() && !((ServerPlayer)param0).hasDisconnected()) {
               param0.getInventory().placeItemBackInInventory(var2);
            } else {
               param0.drop(var2, false);
            }

            this.setCarried(ItemStack.EMPTY);
         }
      }
   }

   protected void clearContainer(Player param0, Container param1) {
      if (!param0.isAlive() || param0 instanceof ServerPlayer && ((ServerPlayer)param0).hasDisconnected()) {
         for (int var3 = 0; var3 < param1.getContainerSize(); var3++) {
            param0.drop(param1.removeItemNoUpdate(var3), false);
         }
      } else {
         for (int var3 = 0; var3 < param1.getContainerSize(); var3++) {
            Inventory var4 = param0.getInventory();
            if (var4.player instanceof ServerPlayer) {
               var4.placeItemBackInInventory(param1.removeItemNoUpdate(var3));
            }
         }
      }
   }

   public void slotsChanged(Container param0) {
      this.broadcastChanges();
   }

   public void setItem(int param0, int param1, ItemStack param2) {
      this.getSlot(param0).set(param2);
      this.stateId = param1;
   }

   public void initializeContents(int param0, List<ItemStack> param1, ItemStack param2) {
      for (int var4 = 0; var4 < param1.size(); var4++) {
         this.getSlot(var4).set(param1.get(var4));
      }

      this.carried = param2;
      this.stateId = param0;
   }

   public void setData(int param0, int param1) {
      this.dataSlots.get(param0).set(param1);
   }

   public abstract boolean stillValid(Player var1);

   protected boolean moveItemStackTo(ItemStack param0, int param1, int param2, boolean param3) {
      boolean var5 = false;
      int var6 = param1;
      if (param3) {
         var6 = param2 - 1;
      }

      if (param0.isStackable()) {
         while (!param0.isEmpty() && (param3 ? var6 >= param1 : var6 < param2)) {
            Slot var7 = this.slots.get(var6);
            ItemStack var8 = var7.getItem();
            if (!var8.isEmpty() && ItemStack.isSameItemSameTags(param0, var8)) {
               int var9 = var8.getCount() + param0.getCount();
               if (var9 <= param0.getMaxStackSize()) {
                  param0.setCount(0);
                  var8.setCount(var9);
                  var7.setChanged();
                  var5 = true;
               } else if (var8.getCount() < param0.getMaxStackSize()) {
                  param0.shrink(param0.getMaxStackSize() - var8.getCount());
                  var8.setCount(param0.getMaxStackSize());
                  var7.setChanged();
                  var5 = true;
               }
            }

            if (param3) {
               var6--;
            } else {
               var6++;
            }
         }
      }

      if (!param0.isEmpty()) {
         if (param3) {
            var6 = param2 - 1;
         } else {
            var6 = param1;
         }

         while (param3 ? var6 >= param1 : var6 < param2) {
            Slot var7 = this.slots.get(var6);
            ItemStack var8 = var7.getItem();
            if (var8.isEmpty() && var7.mayPlace(param0)) {
               if (param0.getCount() > var7.getMaxStackSize()) {
                  var7.setByPlayer(param0.split(var7.getMaxStackSize()));
               } else {
                  var7.setByPlayer(param0.split(param0.getCount()));
               }

               var7.setChanged();
               var5 = true;
               break;
            }

            if (param3) {
               var6--;
            } else {
               var6++;
            }
         }
      }

      return var5;
   }

   public static int getQuickcraftType(int param0) {
      return param0 >> 2 & 3;
   }

   public static int getQuickcraftHeader(int param0) {
      return param0 & 3;
   }

   public static int getQuickcraftMask(int param0, int param1) {
      return param0 & 3 | (param1 & 3) << 2;
   }

   public static boolean isValidQuickcraftType(int param0, Player param1) {
      if (param0 == 0) {
         return true;
      } else {
         return param0 == 1 ? true : param0 == 2 && param1.getAbilities().instabuild;
      }
   }

   protected void resetQuickCraft() {
      this.quickcraftStatus = 0;
      this.quickcraftSlots.clear();
   }

   public static boolean canItemQuickReplace(@Nullable Slot param0, ItemStack param1, boolean param2) {
      boolean var3 = param0 == null || !param0.hasItem();
      return !var3 && ItemStack.isSameItemSameTags(param1, param0.getItem())
         ? param0.getItem().getCount() + (param2 ? 0 : param1.getCount()) <= param1.getMaxStackSize()
         : var3;
   }

   public static void getQuickCraftSlotCount(Set<Slot> param0, int param1, ItemStack param2, int param3) {
      switch (param1) {
         case 0:
            param2.setCount(Mth.floor((float)param2.getCount() / param0.size()));
            break;
         case 1:
            param2.setCount(1);
            break;
         case 2:
            param2.setCount(param2.getItem().getMaxStackSize());
      }

      param2.grow(param3);
   }

   public boolean canDragTo(Slot param0) {
      return true;
   }

   public static int getRedstoneSignalFromBlockEntity(@Nullable BlockEntity param0) {
      return param0 instanceof Container ? getRedstoneSignalFromContainer((Container)param0) : 0;
   }

   public static int getRedstoneSignalFromContainer(@Nullable Container param0) {
      if (param0 == null) {
         return 0;
      }

      int var1 = 0;
      float var2 = 0.0F;

      for (int var3 = 0; var3 < param0.getContainerSize(); var3++) {
         ItemStack var4 = param0.getItem(var3);
         if (!var4.isEmpty()) {
            var2 += (float)var4.getCount() / Math.min(param0.getMaxStackSize(), var4.getMaxStackSize());
            var1++;
         }
      }

      var2 /= param0.getContainerSize();
      return Mth.floor(var2 * 14.0F) + (var1 > 0 ? 1 : 0);
   }

   public void setCarried(ItemStack param0) {
      this.carried = param0;
   }

   public ItemStack getCarried() {
      return this.carried;
   }

   public void suppressRemoteUpdates() {
      this.suppressRemoteUpdates = true;
   }

   public void resumeRemoteUpdates() {
      this.suppressRemoteUpdates = false;
   }

   public void transferState(AbstractContainerMenu param0) {
      Table<Container, Integer, Integer> var2 = HashBasedTable.create();

      for (int var3 = 0; var3 < param0.slots.size(); var3++) {
         Slot var4 = param0.slots.get(var3);
         var2.put(var4.container, var4.getContainerSlot(), var3);
      }

      for (int var3 = 0; var3 < this.slots.size(); var3++) {
         Slot var4 = this.slots.get(var3);
         Integer var5 = var2.get(var4.container, var4.getContainerSlot());
         if (var5 != null) {
            this.lastSlots.set(var3, param0.lastSlots.get(var5));
            this.remoteSlots.set(var3, param0.remoteSlots.get(var5));
         }
      }
   }

   public OptionalInt findSlot(Container param0, int param1) {
      for (int var3 = 0; var3 < this.slots.size(); var3++) {
         Slot var4 = this.slots.get(var3);
         if (var4.container == param0 && param1 == var4.getContainerSlot()) {
            return OptionalInt.of(var3);
         }
      }

      return OptionalInt.empty();
   }

   public int getStateId() {
      return this.stateId;
   }

   public int incrementStateId() {
      this.stateId = this.stateId + 1 & 32767;
      return this.stateId;
   }
}
