package net.minecraft.world.entity.projectile;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownPotion extends ThrowableItemProjectile implements ItemSupplier {
   public static final double SPLASH_RANGE = 4.0;
   private static final double SPLASH_RANGE_SQ = 16.0;
   public static final Predicate<LivingEntity> WATER_SENSITIVE_OR_ON_FIRE = param0 -> param0.isSensitiveToWater() || param0.isOnFire();

   public ThrownPotion(EntityType<? extends ThrownPotion> param0, Level param1) {
      super(param0, param1);
   }

   public ThrownPotion(Level param0, LivingEntity param1) {
      super(EntityType.POTION, param1, param0);
   }

   public ThrownPotion(Level param0, double param1, double param3, double param5) {
      super(EntityType.POTION, param1, param3, param5, param0);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SPLASH_POTION;
   }

   @Override
   protected float getGravity() {
      return 0.05F;
   }

   @Override
   protected void onHitBlock(BlockHitResult param0) {
      super.onHitBlock(param0);
      if (!this.level.isClientSide) {
         ItemStack var2 = this.getItem();
         Potion var3 = PotionUtils.getPotion(var2);
         List<MobEffectInstance> var4 = PotionUtils.getMobEffects(var2);
         boolean var5 = var3 == Potions.WATER && var4.isEmpty();
         Direction var6 = param0.getDirection();
         BlockPos var7 = param0.getBlockPos();
         BlockPos var8 = var7.relative(var6);
         if (var5) {
            this.dowseFire(var8);
            this.dowseFire(var8.relative(var6.getOpposite()));

            for (Direction var10 : Direction.Plane.HORIZONTAL) {
               this.dowseFire(var8.relative(var10));
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult param0) {
      super.onHit(param0);
      if (!this.level.isClientSide) {
         ItemStack var2 = this.getItem();
         Potion var3 = PotionUtils.getPotion(var2);
         List<MobEffectInstance> var4 = PotionUtils.getMobEffects(var2);
         boolean var5 = var3 == Potions.WATER && var4.isEmpty();
         if (var5) {
            this.applyWater();
         } else if (!var4.isEmpty()) {
            if (this.isLingering()) {
               this.makeAreaOfEffectCloud(var2, var3);
            } else {
               this.applySplash(var4, param0.getType() == HitResult.Type.ENTITY ? ((EntityHitResult)param0).getEntity() : null);
            }
         }

         int var6 = var3.hasInstantEffects() ? 2007 : 2002;
         this.level.levelEvent(var6, this.blockPosition(), PotionUtils.getColor(var2));
         this.discard();
      }
   }

   private void applyWater() {
      AABB var1 = this.getBoundingBox().inflate(4.0, 2.0, 4.0);

      for (LivingEntity var4 : this.level.getEntitiesOfClass(LivingEntity.class, var1, WATER_SENSITIVE_OR_ON_FIRE)) {
         double var5 = this.distanceToSqr(var4);
         if (var5 < 16.0) {
            if (var4.isSensitiveToWater()) {
               var4.hurt(this.damageSources().indirectMagic(this, this.getOwner()), 1.0F);
            }

            if (var4.isOnFire() && var4.isAlive()) {
               var4.extinguishFire();
            }
         }
      }

      for (Axolotl var5 : this.level.getEntitiesOfClass(Axolotl.class, var1)) {
         var5.rehydrate();
      }
   }

   private void applySplash(List<MobEffectInstance> param0, @Nullable Entity param1) {
      AABB var3 = this.getBoundingBox().inflate(4.0, 2.0, 4.0);
      List<LivingEntity> var4 = this.level.getEntitiesOfClass(LivingEntity.class, var3);
      if (!var4.isEmpty()) {
         Entity var5 = this.getEffectSource();

         for (LivingEntity var7 : var4) {
            if (var7.isAffectedByPotions()) {
               double var8 = this.distanceToSqr(var7);
               if (var8 < 16.0) {
                  double var10;
                  if (var7 == param1) {
                     var10 = 1.0;
                  } else {
                     var10 = 1.0 - Math.sqrt(var8) / 4.0;
                  }

                  for (MobEffectInstance var13 : param0) {
                     MobEffect var14 = var13.getEffect();
                     if (var14.isInstantenous()) {
                        var14.applyInstantenousEffect(this, this.getOwner(), var7, var13.getAmplifier(), var10);
                     } else {
                        int var15 = var13.mapDuration(l_param2 -> (int)(var10 * l_param2 + 0.5));
                        MobEffectInstance var16 = new MobEffectInstance(var14, var15, var13.getAmplifier(), var13.isAmbient(), var13.isVisible());
                        if (!var16.endsWithin(20)) {
                           var7.addEffect(var16, var5);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void makeAreaOfEffectCloud(ItemStack param0, Potion param1) {
      AreaEffectCloud var3 = new AreaEffectCloud(this.level, this.getX(), this.getY(), this.getZ());
      Entity var4 = this.getOwner();
      if (var4 instanceof LivingEntity) {
         var3.setOwner((LivingEntity)var4);
      }

      var3.setRadius(3.0F);
      var3.setRadiusOnUse(-0.5F);
      var3.setWaitTime(10);
      var3.setRadiusPerTick(-var3.getRadius() / var3.getDuration());
      var3.setPotion(param1);

      for (MobEffectInstance var6 : PotionUtils.getCustomEffects(param0)) {
         var3.addEffect(new MobEffectInstance(var6));
      }

      CompoundTag var5 = param0.getTag();
      if (var5 != null && var5.contains("CustomPotionColor", 99)) {
         var3.setFixedColor(var5.getInt("CustomPotionColor"));
      }

      this.level.addFreshEntity(var3);
   }

   private boolean isLingering() {
      return this.getItem().is(Items.LINGERING_POTION);
   }

   private void dowseFire(BlockPos param0) {
      BlockState var2 = this.level.getBlockState(param0);
      if (var2.is(BlockTags.FIRE)) {
         this.level.removeBlock(param0, false);
      } else if (AbstractCandleBlock.isLit(var2)) {
         AbstractCandleBlock.extinguish(null, var2, this.level, param0);
      } else if (CampfireBlock.isLitCampfire(var2)) {
         this.level.levelEvent(null, 1009, param0, 0);
         CampfireBlock.dowse(this.getOwner(), this.level, param0, var2);
         this.level.setBlockAndUpdate(param0, var2.setValue(CampfireBlock.LIT, false));
      }
   }
}
