package net.minecraft.world.inventory;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class StonecutterMenu extends AbstractContainerMenu {
   public static final int INPUT_SLOT = 0;
   public static final int RESULT_SLOT = 1;
   private static final int INV_SLOT_START = 2;
   private static final int INV_SLOT_END = 29;
   private static final int USE_ROW_SLOT_START = 29;
   private static final int USE_ROW_SLOT_END = 38;
   private final ContainerLevelAccess access;
   private final DataSlot selectedRecipeIndex = DataSlot.standalone();
   private final Level level;
   private List<StonecutterRecipe> recipes = Lists.newArrayList();
   private ItemStack input = ItemStack.EMPTY;
   long lastSoundTime;
   final Slot inputSlot;
   final Slot resultSlot;
   Runnable slotUpdateListener = () -> {};
   public final Container container = new SimpleContainer(1) {
      @Override
      public void setChanged() {
         super.setChanged();
         StonecutterMenu.this.slotsChanged(this);
         StonecutterMenu.this.slotUpdateListener.run();
      }
   };
   final ResultContainer resultContainer = new ResultContainer();

   public StonecutterMenu(int param0, Inventory param1) {
      this(param0, param1, ContainerLevelAccess.NULL);
   }

   public StonecutterMenu(int param0, Inventory param1, final ContainerLevelAccess param2) {
      super(MenuType.STONECUTTER, param0);
      this.access = param2;
      this.level = param1.player.level;
      this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
      this.resultSlot = this.addSlot(new Slot(this.resultContainer, 1, 143, 33) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return false;
         }

         @Override
         public void onTake(Player l_param0, ItemStack l_param1) {
            l_param1.onCraftedBy(l_param0.level, l_param0, l_param1.getCount());
            StonecutterMenu.this.resultContainer.awardUsedRecipes(l_param0);
            ItemStack l_var3 = StonecutterMenu.this.inputSlot.remove(1);
            if (!l_var3.isEmpty()) {
               StonecutterMenu.this.setupResultSlot();
            }

            param2.execute((l2_param0, l2_param1) -> {
               long l2_var3 = l2_param0.getGameTime();
               if (StonecutterMenu.this.lastSoundTime != l2_var3) {
                  l2_param0.playSound((Player)null, l2_param1, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                  StonecutterMenu.this.lastSoundTime = l2_var3;
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

      this.addDataSlot(this.selectedRecipeIndex);
   }

   public int getSelectedRecipeIndex() {
      return this.selectedRecipeIndex.get();
   }

   public List<StonecutterRecipe> getRecipes() {
      return this.recipes;
   }

   public int getNumRecipes() {
      return this.recipes.size();
   }

   public boolean hasInputItem() {
      return this.inputSlot.hasItem() && !this.recipes.isEmpty();
   }

   @Override
   public boolean stillValid(Player param0) {
      return stillValid(this.access, param0, Blocks.STONECUTTER);
   }

   @Override
   public boolean clickMenuButton(Player param0, int param1) {
      if (this.isValidRecipeIndex(param1)) {
         this.selectedRecipeIndex.set(param1);
         this.setupResultSlot();
      }

      return true;
   }

   private boolean isValidRecipeIndex(int param0) {
      return param0 >= 0 && param0 < this.recipes.size();
   }

   @Override
   public void slotsChanged(Container param0) {
      ItemStack var2 = this.inputSlot.getItem();
      if (!var2.is(this.input.getItem())) {
         this.input = var2.copy();
         this.setupRecipeList(param0, var2);
      }
   }

   private void setupRecipeList(Container param0, ItemStack param1) {
      this.recipes.clear();
      this.selectedRecipeIndex.set(-1);
      this.resultSlot.set(ItemStack.EMPTY);
      if (!param1.isEmpty()) {
         this.recipes = this.level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, param0, this.level);
      }
   }

   void setupResultSlot() {
      if (!this.recipes.isEmpty() && this.isValidRecipeIndex(this.selectedRecipeIndex.get())) {
         StonecutterRecipe var1 = this.recipes.get(this.selectedRecipeIndex.get());
         ItemStack var2 = var1.assemble(this.container, this.level.registryAccess());
         if (var2.isItemEnabled(this.level.enabledFeatures())) {
            this.resultContainer.setRecipeUsed(var1);
            this.resultSlot.set(var2);
         } else {
            this.resultSlot.set(ItemStack.EMPTY);
         }
      } else {
         this.resultSlot.set(ItemStack.EMPTY);
      }

      this.broadcastChanges();
   }

   @Override
   public MenuType<?> getType() {
      return MenuType.STONECUTTER;
   }

   public void registerUpdateListener(Runnable param0) {
      this.slotUpdateListener = param0;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack param0, Slot param1) {
      return param1.container != this.resultContainer && super.canTakeItemForPickAll(param0, param1);
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         Item var6 = var5.getItem();
         var3 = var5.copy();
         if (param1 == 1) {
            var6.onCraftedBy(var5, param0.level, param0);
            if (!this.moveItemStackTo(var5, 2, 38, true)) {
               return ItemStack.EMPTY;
            }

            var4.onQuickCraft(var5, var3);
         } else if (param1 == 0) {
            if (!this.moveItemStackTo(var5, 2, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.level.getRecipeManager().getRecipeFor(RecipeType.STONECUTTING, new SimpleContainer(var5), this.level).isPresent()) {
            if (!this.moveItemStackTo(var5, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (param1 >= 2 && param1 < 29) {
            if (!this.moveItemStackTo(var5, 29, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (param1 >= 29 && param1 < 38 && !this.moveItemStackTo(var5, 2, 29, false)) {
            return ItemStack.EMPTY;
         }

         if (var5.isEmpty()) {
            var4.setByPlayer(ItemStack.EMPTY);
         }

         var4.setChanged();
         if (var5.getCount() == var3.getCount()) {
            return ItemStack.EMPTY;
         }

         var4.onTake(param0, var5);
         this.broadcastChanges();
      }

      return var3;
   }

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      this.resultContainer.removeItemNoUpdate(1);
      this.access.execute((l_param1, l_param2) -> this.clearContainer(param0, this.container));
   }
}
