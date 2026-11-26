package net.minecraft.world.entity.projectile;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public abstract class Fireball extends AbstractHurtingProjectile implements ItemSupplier {
   private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(Fireball.class, EntityDataSerializers.ITEM_STACK);

   public Fireball(EntityType<? extends Fireball> param0, Level param1) {
      super(param0, param1);
   }

   public Fireball(
      EntityType<? extends Fireball> param0, double param1, double param3, double param5, double param7, double param9, double param11, Level param13
   ) {
      super(param0, param1, param3, param5, param7, param9, param11, param13);
   }

   public Fireball(EntityType<? extends Fireball> param0, LivingEntity param1, double param2, double param4, double param6, Level param8) {
      super(param0, param1, param2, param4, param6, param8);
   }

   public void setItem(ItemStack param0) {
      if (!param0.is(Items.FIRE_CHARGE) || param0.hasTag()) {
         this.getEntityData().set(DATA_ITEM_STACK, Util.make(param0.copy(), l_param0 -> l_param0.setCount(1)));
      }
   }

   protected ItemStack getItemRaw() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   public ItemStack getItem() {
      ItemStack var1 = this.getItemRaw();
      return var1.isEmpty() ? new ItemStack(Items.FIRE_CHARGE) : var1;
   }

   @Override
   protected void defineSynchedData() {
      this.getEntityData().define(DATA_ITEM_STACK, ItemStack.EMPTY);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      ItemStack var2 = this.getItemRaw();
      if (!var2.isEmpty()) {
         param0.put("Item", var2.save(new CompoundTag()));
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      ItemStack var2 = ItemStack.of(param0.getCompound("Item"));
      this.setItem(var2);
   }
}
