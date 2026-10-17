package net.minecraft.world.entity.projectile;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EyeOfEnder extends Entity implements ItemSupplier {
   private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(EyeOfEnder.class, EntityDataSerializers.ITEM_STACK);
   private double tx;
   private double ty;
   private double tz;
   private int life;
   private boolean surviveAfterDeath;

   public EyeOfEnder(EntityType<? extends EyeOfEnder> param0, Level param1) {
      super(param0, param1);
   }

   public EyeOfEnder(Level param0, double param1, double param3, double param5) {
      this(EntityType.EYE_OF_ENDER, param0);
      this.setPos(param1, param3, param5);
   }

   public void setItem(ItemStack param0) {
      if (!param0.is(Items.ENDER_EYE) || param0.hasTag()) {
         this.getEntityData().set(DATA_ITEM_STACK, Util.make(param0.copy(), l_param0 -> l_param0.setCount(1)));
      }
   }

   private ItemStack getItemRaw() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   public ItemStack getItem() {
      ItemStack var1 = this.getItemRaw();
      return var1.isEmpty() ? new ItemStack(Items.ENDER_EYE) : var1;
   }

   @Override
   protected void defineSynchedData() {
      this.getEntityData().define(DATA_ITEM_STACK, ItemStack.EMPTY);
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double param0) {
      double var3 = this.getBoundingBox().getSize() * 4.0;
      if (Double.isNaN(var3)) {
         var3 = 4.0;
      }

      var3 *= 64.0;
      return param0 < var3 * var3;
   }

   public void signalTo(BlockPos param0) {
      double var2 = param0.getX();
      int var4 = param0.getY();
      double var5 = param0.getZ();
      double var7 = var2 - this.getX();
      double var9 = var5 - this.getZ();
      double var11 = Math.sqrt(var7 * var7 + var9 * var9);
      if (var11 > 12.0) {
         this.tx = this.getX() + var7 / var11 * 12.0;
         this.tz = this.getZ() + var9 / var11 * 12.0;
         this.ty = this.getY() + 8.0;
      } else {
         this.tx = var2;
         this.ty = var4;
         this.tz = var5;
      }

      this.life = 0;
      this.surviveAfterDeath = this.random.nextInt(5) > 0;
   }

   @Override
   public void lerpMotion(double param0, double param2, double param4) {
      this.setDeltaMovement(param0, param2, param4);
      if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
         double var7 = Math.sqrt(param0 * param0 + param4 * param4);
         this.setYRot((float)(Mth.atan2(param0, param4) * 180.0F / (float)Math.PI));
         this.setXRot((float)(Mth.atan2(param2, var7) * 180.0F / (float)Math.PI));
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
      }
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 var1 = this.getDeltaMovement();
      double var2 = this.getX() + var1.x;
      double var4 = this.getY() + var1.y;
      double var6 = this.getZ() + var1.z;
      double var8 = var1.horizontalDistance();
      this.setXRot(Projectile.lerpRotation(this.xRotO, (float)(Mth.atan2(var1.y, var8) * 180.0F / (float)Math.PI)));
      this.setYRot(Projectile.lerpRotation(this.yRotO, (float)(Mth.atan2(var1.x, var1.z) * 180.0F / (float)Math.PI)));
      if (!this.level.isClientSide) {
         double var10 = this.tx - var2;
         double var12 = this.tz - var6;
         float var14 = (float)Math.sqrt(var10 * var10 + var12 * var12);
         float var15 = (float)Mth.atan2(var12, var10);
         double var16 = Mth.lerp(0.0025, var8, var14);
         double var18 = var1.y;
         if (var14 < 1.0F) {
            var16 *= 0.8;
            var18 *= 0.8;
         }

         int var20 = this.getY() < this.ty ? 1 : -1;
         var1 = new Vec3(Math.cos(var15) * var16, var18 + (var20 - var18) * 0.015F, Math.sin(var15) * var16);
         this.setDeltaMovement(var1);
      }

      float var10 = 0.25F;
      if (this.isInWater()) {
         for (int var11 = 0; var11 < 4; var11++) {
            this.level.addParticle(ParticleTypes.BUBBLE, var2 - var1.x * 0.25, var4 - var1.y * 0.25, var6 - var1.z * 0.25, var1.x, var1.y, var1.z);
         }
      } else {
         this.level
            .addParticle(
               ParticleTypes.PORTAL,
               var2 - var1.x * 0.25 + this.random.nextDouble() * 0.6 - 0.3,
               var4 - var1.y * 0.25 - 0.5,
               var6 - var1.z * 0.25 + this.random.nextDouble() * 0.6 - 0.3,
               var1.x,
               var1.y,
               var1.z
            );
      }

      if (!this.level.isClientSide) {
         this.setPos(var2, var4, var6);
         this.life++;
         if (this.life > 80 && !this.level.isClientSide) {
            this.playSound(SoundEvents.ENDER_EYE_DEATH, 1.0F, 1.0F);
            this.discard();
            if (this.surviveAfterDeath) {
               this.level.addFreshEntity(new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), this.getItem()));
            } else {
               this.level.levelEvent(2003, this.blockPosition(), 0);
            }
         }
      } else {
         this.setPosRaw(var2, var4, var6);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      ItemStack var2 = this.getItemRaw();
      if (!var2.isEmpty()) {
         param0.put("Item", var2.save(new CompoundTag()));
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      ItemStack var2 = ItemStack.of(param0.getCompound("Item"));
      this.setItem(var2);
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   public boolean isAttackable() {
      return false;
   }
}
