package net.minecraft.world.inventory;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class LoomMenu extends AbstractContainerMenu {
   private static final int PATTERN_NOT_SET = -1;
   private static final int INV_SLOT_START = 4;
   private static final int INV_SLOT_END = 31;
   private static final int USE_ROW_SLOT_START = 31;
   private static final int USE_ROW_SLOT_END = 40;
   private final ContainerLevelAccess access;
   final DataSlot selectedBannerPatternIndex = DataSlot.standalone();
   private List<Holder<BannerPattern>> selectablePatterns = List.of();
   Runnable slotUpdateListener = () -> {};
   final Slot bannerSlot;
   final Slot dyeSlot;
   private final Slot patternSlot;
   private final Slot resultSlot;
   long lastSoundTime;
   private final Container inputContainer = new SimpleContainer(3) {
      @Override
      public void setChanged() {
         super.setChanged();
         LoomMenu.this.slotsChanged(this);
         LoomMenu.this.slotUpdateListener.run();
      }
   };
   private final Container outputContainer = new SimpleContainer(1) {
      @Override
      public void setChanged() {
         super.setChanged();
         LoomMenu.this.slotUpdateListener.run();
      }
   };

   public LoomMenu(int param0, Inventory param1) {
      this(param0, param1, ContainerLevelAccess.NULL);
   }

   public LoomMenu(int param0, Inventory param1, final ContainerLevelAccess param2) {
      super(MenuType.LOOM, param0);
      this.access = param2;
      this.bannerSlot = this.addSlot(new Slot(this.inputContainer, 0, 13, 26) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return l_param0.getItem() instanceof BannerItem;
         }
      });
      this.dyeSlot = this.addSlot(new Slot(this.inputContainer, 1, 33, 26) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return l_param0.getItem() instanceof DyeItem;
         }
      });
      this.patternSlot = this.addSlot(new Slot(this.inputContainer, 2, 23, 45) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return l_param0.getItem() instanceof BannerPatternItem;
         }
      });
      this.resultSlot = this.addSlot(new Slot(this.outputContainer, 0, 143, 58) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return false;
         }

         @Override
         public void onTake(Player l_param0, ItemStack l_param1) {
            LoomMenu.this.bannerSlot.remove(1);
            LoomMenu.this.dyeSlot.remove(1);
            if (!LoomMenu.this.bannerSlot.hasItem() || !LoomMenu.this.dyeSlot.hasItem()) {
               LoomMenu.this.selectedBannerPatternIndex.set(-1);
            }

            param2.execute((l2_param0, l2_param1) -> {
               long l2_var3 = l2_param0.getGameTime();
               if (LoomMenu.this.lastSoundTime != l2_var3) {
                  l2_param0.playSound((Player)null, l2_param1, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                  LoomMenu.this.lastSoundTime = l2_var3;
               }
            });
            super.onTake(l_param0, l_param1);
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

      this.addDataSlot(this.selectedBannerPatternIndex);
   }

   @Override
   public boolean stillValid(Player param0) {
      return stillValid(this.access, param0, Blocks.LOOM);
   }

   @Override
   public boolean clickMenuButton(Player param0, int param1) {
      if (param1 >= 0 && param1 < this.selectablePatterns.size()) {
         this.selectedBannerPatternIndex.set(param1);
         this.setupResultSlot(this.selectablePatterns.get(param1));
         return true;
      } else {
         return false;
      }
   }

   private List<Holder<BannerPattern>> getSelectablePatterns(ItemStack param0) {
      if (param0.isEmpty()) {
         return BuiltInRegistries.BANNER_PATTERN.getTag(BannerPatternTags.NO_ITEM_REQUIRED).map(ImmutableList::copyOf).orElse(ImmutableList.of());
      } else {
         return param0.getItem() instanceof BannerPatternItem var2
            ? BuiltInRegistries.BANNER_PATTERN.getTag(var2.getBannerPattern()).map(ImmutableList::copyOf).orElse(ImmutableList.of())
            : List.of();
      }
   }

   private boolean isValidPatternIndex(int param0) {
      return param0 >= 0 && param0 < this.selectablePatterns.size();
   }

   @Override
   public void slotsChanged(Container param0) {
      ItemStack var2 = this.bannerSlot.getItem();
      ItemStack var3 = this.dyeSlot.getItem();
      ItemStack var4 = this.patternSlot.getItem();
      if (!var2.isEmpty() && !var3.isEmpty()) {
         int var5 = this.selectedBannerPatternIndex.get();
         boolean var6 = this.isValidPatternIndex(var5);
         List<Holder<BannerPattern>> var7 = this.selectablePatterns;
         this.selectablePatterns = this.getSelectablePatterns(var4);
         Holder<BannerPattern> var8;
         if (this.selectablePatterns.size() == 1) {
            this.selectedBannerPatternIndex.set(0);
            var8 = this.selectablePatterns.get(0);
         } else if (!var6) {
            this.selectedBannerPatternIndex.set(-1);
            var8 = null;
         } else {
            Holder<BannerPattern> var9 = var7.get(var5);
            int var10 = this.selectablePatterns.indexOf(var9);
            if (var10 != -1) {
               var8 = var9;
               this.selectedBannerPatternIndex.set(var10);
            } else {
               var8 = null;
               this.selectedBannerPatternIndex.set(-1);
            }
         }

         if (var8 != null) {
            CompoundTag var9 = BlockItem.getBlockEntityData(var2);
            boolean var10 = var9 != null && var9.contains("Patterns", 9) && !var2.isEmpty() && var9.getList("Patterns", 10).size() >= 6;
            if (var10) {
               this.selectedBannerPatternIndex.set(-1);
               this.resultSlot.set(ItemStack.EMPTY);
            } else {
               this.setupResultSlot(var8);
            }
         } else {
            this.resultSlot.set(ItemStack.EMPTY);
         }

         this.broadcastChanges();
      } else {
         this.resultSlot.set(ItemStack.EMPTY);
         this.selectablePatterns = List.of();
         this.selectedBannerPatternIndex.set(-1);
      }
   }

   public List<Holder<BannerPattern>> getSelectablePatterns() {
      return this.selectablePatterns;
   }

   public int getSelectedBannerPatternIndex() {
      return this.selectedBannerPatternIndex.get();
   }

   public void registerUpdateListener(Runnable param0) {
      this.slotUpdateListener = param0;
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         if (param1 == this.resultSlot.index) {
            if (!this.moveItemStackTo(var5, 4, 40, true)) {
               return ItemStack.EMPTY;
            }

            var4.onQuickCraft(var5, var3);
         } else if (param1 != this.dyeSlot.index && param1 != this.bannerSlot.index && param1 != this.patternSlot.index) {
            if (var5.getItem() instanceof BannerItem) {
               if (!this.moveItemStackTo(var5, this.bannerSlot.index, this.bannerSlot.index + 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (var5.getItem() instanceof DyeItem) {
               if (!this.moveItemStackTo(var5, this.dyeSlot.index, this.dyeSlot.index + 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (var5.getItem() instanceof BannerPatternItem) {
               if (!this.moveItemStackTo(var5, this.patternSlot.index, this.patternSlot.index + 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (param1 >= 4 && param1 < 31) {
               if (!this.moveItemStackTo(var5, 31, 40, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (param1 >= 31 && param1 < 40 && !this.moveItemStackTo(var5, 4, 31, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(var5, 4, 40, false)) {
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

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      this.access.execute((l_param1, l_param2) -> this.clearContainer(param0, this.inputContainer));
   }

   private void setupResultSlot(Holder<BannerPattern> param0) {
      ItemStack var2 = this.bannerSlot.getItem();
      ItemStack var3 = this.dyeSlot.getItem();
      ItemStack var4 = ItemStack.EMPTY;
      if (!var2.isEmpty() && !var3.isEmpty()) {
         var4 = var2.copy();
         var4.setCount(1);
         DyeColor var5 = ((DyeItem)var3.getItem()).getDyeColor();
         CompoundTag var6 = BlockItem.getBlockEntityData(var4);
         ListTag var7;
         if (var6 != null && var6.contains("Patterns", 9)) {
            var7 = var6.getList("Patterns", 10);
         } else {
            var7 = new ListTag();
            if (var6 == null) {
               var6 = new CompoundTag();
            }

            var6.put("Patterns", var7);
         }

         CompoundTag var8 = new CompoundTag();
         var8.putString("Pattern", param0.value().getHashname());
         var8.putInt("Color", var5.getId());
         var7.add(var8);
         BlockItem.setBlockEntityData(var4, BlockEntityType.BANNER, var6);
      }

      if (!ItemStack.matches(var4, this.resultSlot.getItem())) {
         this.resultSlot.set(var4);
      }
   }

   public Slot getBannerSlot() {
      return this.bannerSlot;
   }

   public Slot getDyeSlot() {
      return this.dyeSlot;
   }

   public Slot getPatternSlot() {
      return this.patternSlot;
   }

   public Slot getResultSlot() {
      return this.resultSlot;
   }
}
