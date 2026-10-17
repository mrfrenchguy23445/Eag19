package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

public class HopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {
   public static final int MOVE_ITEM_SPEED = 8;
   public static final int HOPPER_CONTAINER_SIZE = 5;
   private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
   private int cooldownTime = -1;
   private long tickedGameTime;

   public HopperBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.HOPPER, param0, param1);
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(param0)) {
         ContainerHelper.loadAllItems(param0, this.items);
      }

      this.cooldownTime = param0.getInt("TransferCooldown");
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      if (!this.trySaveLootTable(param0)) {
         ContainerHelper.saveAllItems(param0, this.items);
      }

      param0.putInt("TransferCooldown", this.cooldownTime);
   }

   @Override
   public int getContainerSize() {
      return this.items.size();
   }

   @Override
   public ItemStack removeItem(int param0, int param1) {
      this.unpackLootTable(null);
      return ContainerHelper.removeItem(this.getItems(), param0, param1);
   }

   @Override
   public void setItem(int param0, ItemStack param1) {
      this.unpackLootTable(null);
      this.getItems().set(param0, param1);
      if (param1.getCount() > this.getMaxStackSize()) {
         param1.setCount(this.getMaxStackSize());
      }
   }

   @Override
   protected Component getDefaultName() {
      return Component.translatable("container.hopper");
   }

   public static void pushItemsTick(Level param0, BlockPos param1, BlockState param2, HopperBlockEntity param3) {
      param3.cooldownTime--;
      param3.tickedGameTime = param0.getGameTime();
      if (!param3.isOnCooldown()) {
         param3.setCooldown(0);
         tryMoveItems(param0, param1, param2, param3, () -> suckInItems(param0, param3));
      }
   }

   private static boolean tryMoveItems(Level param0, BlockPos param1, BlockState param2, HopperBlockEntity param3, BooleanSupplier param4) {
      if (param0.isClientSide) {
         return false;
      }

      if (!param3.isOnCooldown() && param2.getValue(HopperBlock.ENABLED)) {
         boolean var5 = false;
         if (!param3.isEmpty()) {
            var5 = ejectItems(param0, param1, param2, param3);
         }

         if (!param3.inventoryFull()) {
            var5 |= param4.getAsBoolean();
         }

         if (var5) {
            param3.setCooldown(8);
            setChanged(param0, param1, param2);
            return true;
         }
      }

      return false;
   }

   private boolean inventoryFull() {
      for (ItemStack var2 : this.items) {
         if (var2.isEmpty() || var2.getCount() != var2.getMaxStackSize()) {
            return false;
         }
      }

      return true;
   }

   private static boolean ejectItems(Level param0, BlockPos param1, BlockState param2, Container param3) {
      Container var4 = getAttachedContainer(param0, param1, param2);
      if (var4 == null) {
         return false;
      }

      Direction var5 = param2.getValue(HopperBlock.FACING).getOpposite();
      if (isFullContainer(var4, var5)) {
         return false;
      }

      for (int var6 = 0; var6 < param3.getContainerSize(); var6++) {
         if (!param3.getItem(var6).isEmpty()) {
            ItemStack var7 = param3.getItem(var6).copy();
            ItemStack var8 = addItem(param3, var4, param3.removeItem(var6, 1), var5);
            if (var8.isEmpty()) {
               var4.setChanged();
               return true;
            }

            param3.setItem(var6, var7);
         }
      }

      return false;
   }

   private static IntStream getSlots(Container param0, Direction param1) {
      return param0 instanceof WorldlyContainer
         ? IntStream.of(((WorldlyContainer)param0).getSlotsForFace(param1))
         : IntStream.range(0, param0.getContainerSize());
   }

   private static boolean isFullContainer(Container param0, Direction param1) {
      return getSlots(param0, param1).allMatch(l_param1 -> {
         ItemStack l_var2 = param0.getItem(l_param1);
         return l_var2.getCount() >= l_var2.getMaxStackSize();
      });
   }

   private static boolean isEmptyContainer(Container param0, Direction param1) {
      return getSlots(param0, param1).allMatch(l_param1 -> param0.getItem(l_param1).isEmpty());
   }

   public static boolean suckInItems(Level param0, Hopper param1) {
      Container var2 = getSourceContainer(param0, param1);
      if (var2 != null) {
         Direction var3 = Direction.DOWN;
         return isEmptyContainer(var2, var3) ? false : getSlots(var2, var3).anyMatch(l_param3 -> tryTakeInItemFromSlot(param1, var2, l_param3, var3));
      }

      for (ItemEntity var4 : getItemsAtAndAbove(param0, param1)) {
         if (addItem(param1, var4)) {
            return true;
         }
      }

      return false;
   }

   private static boolean tryTakeInItemFromSlot(Hopper param0, Container param1, int param2, Direction param3) {
      ItemStack var4 = param1.getItem(param2);
      if (!var4.isEmpty() && canTakeItemFromContainer(param0, param1, var4, param2, param3)) {
         ItemStack var5 = var4.copy();
         ItemStack var6 = addItem(param1, param0, param1.removeItem(param2, 1), null);
         if (var6.isEmpty()) {
            param1.setChanged();
            return true;
         }

         param1.setItem(param2, var5);
      }

      return false;
   }

   public static boolean addItem(Container param0, ItemEntity param1) {
      boolean var2 = false;
      ItemStack var3 = param1.getItem().copy();
      ItemStack var4 = addItem(null, param0, var3, null);
      if (var4.isEmpty()) {
         var2 = true;
         param1.discard();
      } else {
         param1.setItem(var4);
      }

      return var2;
   }

   public static ItemStack addItem(@Nullable Container param0, Container param1, ItemStack param2, @Nullable Direction param3) {
      if (param1 instanceof WorldlyContainer var4 && param3 != null) {
         int[] var5 = var4.getSlotsForFace(param3);

         for (int var6 = 0; var6 < var5.length && !param2.isEmpty(); var6++) {
            param2 = tryMoveInItem(param0, param1, param2, var5[var6], param3);
         }
      } else {
         int var5 = param1.getContainerSize();

         for (int var6 = 0; var6 < var5 && !param2.isEmpty(); var6++) {
            param2 = tryMoveInItem(param0, param1, param2, var6, param3);
         }
      }

      return param2;
   }

   private static boolean canPlaceItemInContainer(Container param0, ItemStack param1, int param2, @Nullable Direction param3) {
      return !param0.canPlaceItem(param2, param1) ? false : !(param0 instanceof WorldlyContainer var4 && !var4.canPlaceItemThroughFace(param2, param1, param3));
   }

   private static boolean canTakeItemFromContainer(Container param0, Container param1, ItemStack param2, int param3, Direction param4) {
      return !param1.canTakeItem(param0, param3, param2)
         ? false
         : !(param1 instanceof WorldlyContainer var5 && !var5.canTakeItemThroughFace(param3, param2, param4));
   }

   private static ItemStack tryMoveInItem(@Nullable Container param0, Container param1, ItemStack param2, int param3, @Nullable Direction param4) {
      ItemStack var5 = param1.getItem(param3);
      if (canPlaceItemInContainer(param1, param2, param3, param4)) {
         boolean var6 = false;
         boolean var7 = param1.isEmpty();
         if (var5.isEmpty()) {
            param1.setItem(param3, param2);
            param2 = ItemStack.EMPTY;
            var6 = true;
         } else if (canMergeItems(var5, param2)) {
            int var8 = param2.getMaxStackSize() - var5.getCount();
            int var9 = Math.min(param2.getCount(), var8);
            param2.shrink(var9);
            var5.grow(var9);
            var6 = var9 > 0;
         }

         if (var6) {
            if (var7 && param1 instanceof HopperBlockEntity var8 && !var8.isOnCustomCooldown()) {
               int var9 = 0;
               if (param0 instanceof HopperBlockEntity var10 && var8.tickedGameTime >= var10.tickedGameTime) {
                  var9 = 1;
               }

               var8.setCooldown(8 - var9);
            }

            param1.setChanged();
         }
      }

      return param2;
   }

   @Nullable
   private static Container getAttachedContainer(Level param0, BlockPos param1, BlockState param2) {
      Direction var3 = param2.getValue(HopperBlock.FACING);
      return getContainerAt(param0, param1.relative(var3));
   }

   @Nullable
   private static Container getSourceContainer(Level param0, Hopper param1) {
      return getContainerAt(param0, param1.getLevelX(), param1.getLevelY() + 1.0, param1.getLevelZ());
   }

   public static List<ItemEntity> getItemsAtAndAbove(Level param0, Hopper param1) {
      return param1.getSuckShape()
         .toAabbs()
         .stream()
         .flatMap(
            l_param2 -> param0.getEntitiesOfClass(
                  ItemEntity.class,
                  l_param2.move(param1.getLevelX() - 0.5, param1.getLevelY() - 0.5, param1.getLevelZ() - 0.5),
                  EntitySelector.ENTITY_STILL_ALIVE
               )
               .stream()
         )
         .collect(Collectors.toList());
   }

   @Nullable
   public static Container getContainerAt(Level param0, BlockPos param1) {
      return getContainerAt(param0, param1.getX() + 0.5, param1.getY() + 0.5, param1.getZ() + 0.5);
   }

   @Nullable
   private static Container getContainerAt(Level param0, double param1, double param3, double param5) {
      Container var7 = null;
      BlockPos var8 = BlockPos.containing(param1, param3, param5);
      BlockState var9 = param0.getBlockState(var8);
      Block var10 = var9.getBlock();
      if (var10 instanceof WorldlyContainerHolder) {
         var7 = ((WorldlyContainerHolder)var10).getContainer(var9, param0, var8);
      } else if (var9.hasBlockEntity()) {
         BlockEntity var11 = param0.getBlockEntity(var8);
         if (var11 instanceof Container) {
            var7 = (Container)var11;
            if (var7 instanceof ChestBlockEntity && var10 instanceof ChestBlock) {
               var7 = ChestBlock.getContainer((ChestBlock)var10, var9, param0, var8, true);
            }
         }
      }

      if (var7 == null) {
         List<Entity> var11 = param0.getEntities(
            (Entity)null,
            new AABB(param1 - 0.5, param3 - 0.5, param5 - 0.5, param1 + 0.5, param3 + 0.5, param5 + 0.5),
            EntitySelector.CONTAINER_ENTITY_SELECTOR
         );
         if (!var11.isEmpty()) {
            var7 = (Container)var11.get(param0.random.nextInt(var11.size()));
         }
      }

      return var7;
   }

   private static boolean canMergeItems(ItemStack param0, ItemStack param1) {
      if (!param0.is(param1.getItem())) {
         return false;
      } else if (param0.getDamageValue() != param1.getDamageValue()) {
         return false;
      } else {
         return param0.getCount() > param0.getMaxStackSize() ? false : ItemStack.tagMatches(param0, param1);
      }
   }

   @Override
   public double getLevelX() {
      return this.worldPosition.getX() + 0.5;
   }

   @Override
   public double getLevelY() {
      return this.worldPosition.getY() + 0.5;
   }

   @Override
   public double getLevelZ() {
      return this.worldPosition.getZ() + 0.5;
   }

   private void setCooldown(int param0) {
      this.cooldownTime = param0;
   }

   private boolean isOnCooldown() {
      return this.cooldownTime > 0;
   }

   private boolean isOnCustomCooldown() {
      return this.cooldownTime > 8;
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> param0) {
      this.items = param0;
   }

   public static void entityInside(Level param0, BlockPos param1, BlockState param2, Entity param3, HopperBlockEntity param4) {
      if (param3 instanceof ItemEntity
         && Shapes.joinIsNotEmpty(
            Shapes.create(param3.getBoundingBox().move(-param1.getX(), -param1.getY(), -param1.getZ())), param4.getSuckShape(), BooleanOp.AND
         )) {
         tryMoveItems(param0, param1, param2, param4, () -> addItem(param4, (ItemEntity)param3));
      }
   }

   @Override
   protected AbstractContainerMenu createMenu(int param0, Inventory param1) {
      return new HopperMenu(param0, param1, this);
   }
}
