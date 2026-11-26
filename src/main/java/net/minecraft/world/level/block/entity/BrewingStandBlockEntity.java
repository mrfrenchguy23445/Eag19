package net.minecraft.world.level.block.entity;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BrewingStandBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
   private static final int INGREDIENT_SLOT = 3;
   private static final int FUEL_SLOT = 4;
   private static final int[] SLOTS_FOR_UP = new int[]{3};
   private static final int[] SLOTS_FOR_DOWN = new int[]{0, 1, 2, 3};
   private static final int[] SLOTS_FOR_SIDES = new int[]{0, 1, 2, 4};
   public static final int FUEL_USES = 20;
   public static final int DATA_BREW_TIME = 0;
   public static final int DATA_FUEL_USES = 1;
   public static final int NUM_DATA_VALUES = 2;
   private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
   int brewTime;
   private boolean[] lastPotionCount;
   private Item ingredient;
   int fuel;
   protected final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(int param0) {
         switch (param0) {
            case 0:
               return BrewingStandBlockEntity.this.brewTime;
            case 1:
               return BrewingStandBlockEntity.this.fuel;
            default:
               return 0;
         }
      }

      @Override
      public void set(int param0, int param1) {
         switch (param0) {
            case 0:
               BrewingStandBlockEntity.this.brewTime = param1;
               break;
            case 1:
               BrewingStandBlockEntity.this.fuel = param1;
         }
      }

      @Override
      public int getCount() {
         return 2;
      }
   };

   public BrewingStandBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.BREWING_STAND, param0, param1);
   }

   @Override
   protected Component getDefaultName() {
      return Component.translatable("container.brewing");
   }

   @Override
   public int getContainerSize() {
      return this.items.size();
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack var2 : this.items) {
         if (!var2.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public static void serverTick(Level param0, BlockPos param1, BlockState param2, BrewingStandBlockEntity param3) {
      ItemStack var4 = param3.items.get(4);
      if (param3.fuel <= 0 && var4.is(Items.BLAZE_POWDER)) {
         param3.fuel = 20;
         var4.shrink(1);
         setChanged(param0, param1, param2);
      }

      boolean var5 = isBrewable(param3.items);
      boolean var6 = param3.brewTime > 0;
      ItemStack var7 = param3.items.get(3);
      if (var6) {
         param3.brewTime--;
         boolean var8 = param3.brewTime == 0;
         if (var8 && var5) {
            doBrew(param0, param1, param3.items);
            setChanged(param0, param1, param2);
         } else if (!var5 || !var7.is(param3.ingredient)) {
            param3.brewTime = 0;
            setChanged(param0, param1, param2);
         }
      } else if (var5 && param3.fuel > 0) {
         param3.fuel--;
         param3.brewTime = 400;
         param3.ingredient = var7.getItem();
         setChanged(param0, param1, param2);
      }

      boolean[] var8 = param3.getPotionBits();
      if (!Arrays.equals(var8, param3.lastPotionCount)) {
         param3.lastPotionCount = var8;
         BlockState var9 = param2;
         if (!(var9.getBlock() instanceof BrewingStandBlock)) {
            return;
         }

         for (int var10 = 0; var10 < BrewingStandBlock.HAS_BOTTLE.length; var10++) {
            var9 = var9.setValue(BrewingStandBlock.HAS_BOTTLE[var10], var8[var10]);
         }

         param0.setBlock(param1, var9, 2);
      }
   }

   private boolean[] getPotionBits() {
      boolean[] var1 = new boolean[3];

      for (int var2 = 0; var2 < 3; var2++) {
         if (!this.items.get(var2).isEmpty()) {
            var1[var2] = true;
         }
      }

      return var1;
   }

   private static boolean isBrewable(NonNullList<ItemStack> param0) {
      ItemStack var1 = param0.get(3);
      if (var1.isEmpty()) {
         return false;
      }

      if (!PotionBrewing.isIngredient(var1)) {
         return false;
      }

      for (int var2 = 0; var2 < 3; var2++) {
         ItemStack var3 = param0.get(var2);
         if (!var3.isEmpty() && PotionBrewing.hasMix(var3, var1)) {
            return true;
         }
      }

      return false;
   }

   private static void doBrew(Level param0, BlockPos param1, NonNullList<ItemStack> param2) {
      ItemStack var3 = param2.get(3);

      for (int var4 = 0; var4 < 3; var4++) {
         param2.set(var4, PotionBrewing.mix(var3, param2.get(var4)));
      }

      var3.shrink(1);
      if (var3.getItem().hasCraftingRemainingItem()) {
         ItemStack var4 = new ItemStack(var3.getItem().getCraftingRemainingItem());
         if (var3.isEmpty()) {
            var3 = var4;
         } else {
            Containers.dropItemStack(param0, param1.getX(), param1.getY(), param1.getZ(), var4);
         }
      }

      param2.set(3, var3);
      param0.levelEvent(1035, param1, 0);
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(param0, this.items);
      this.brewTime = param0.getShort("BrewTime");
      this.fuel = param0.getByte("Fuel");
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      param0.putShort("BrewTime", (short)this.brewTime);
      ContainerHelper.saveAllItems(param0, this.items);
      param0.putByte("Fuel", (byte)this.fuel);
   }

   @Override
   public ItemStack getItem(int param0) {
      return param0 >= 0 && param0 < this.items.size() ? this.items.get(param0) : ItemStack.EMPTY;
   }

   @Override
   public ItemStack removeItem(int param0, int param1) {
      return ContainerHelper.removeItem(this.items, param0, param1);
   }

   @Override
   public ItemStack removeItemNoUpdate(int param0) {
      return ContainerHelper.takeItem(this.items, param0);
   }

   @Override
   public void setItem(int param0, ItemStack param1) {
      if (param0 >= 0 && param0 < this.items.size()) {
         this.items.set(param0, param1);
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return Container.stillValidBlockEntity(this, param0);
   }

   @Override
   public boolean canPlaceItem(int param0, ItemStack param1) {
      if (param0 == 3) {
         return PotionBrewing.isIngredient(param1);
      } else {
         return param0 == 4
            ? param1.is(Items.BLAZE_POWDER)
            : (param1.is(Items.POTION) || param1.is(Items.SPLASH_POTION) || param1.is(Items.LINGERING_POTION) || param1.is(Items.GLASS_BOTTLE))
               && this.getItem(param0).isEmpty();
      }
   }

   @Override
   public int[] getSlotsForFace(Direction param0) {
      if (param0 == Direction.UP) {
         return SLOTS_FOR_UP;
      } else {
         return param0 == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_SIDES;
      }
   }

   @Override
   public boolean canPlaceItemThroughFace(int param0, ItemStack param1, @Nullable Direction param2) {
      return this.canPlaceItem(param0, param1);
   }

   @Override
   public boolean canTakeItemThroughFace(int param0, ItemStack param1, Direction param2) {
      return param0 == 3 ? param1.is(Items.GLASS_BOTTLE) : true;
   }

   @Override
   public void clearContent() {
      this.items.clear();
   }

   @Override
   protected AbstractContainerMenu createMenu(int param0, Inventory param1) {
      return new BrewingStandMenu(param0, param1, this, this.dataAccess);
   }
}
