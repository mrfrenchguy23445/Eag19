package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Snowball extends ThrowableItemProjectile {
   public Snowball(EntityType<? extends Snowball> param0, Level param1) {
      super(param0, param1);
   }

   public Snowball(Level param0, LivingEntity param1) {
      super(EntityType.SNOWBALL, param1, param0);
   }

   public Snowball(Level param0, double param1, double param3, double param5) {
      super(EntityType.SNOWBALL, param1, param3, param5, param0);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SNOWBALL;
   }

   private ParticleOptions getParticle() {
      ItemStack var1 = this.getItemRaw();
      return var1.isEmpty() ? ParticleTypes.ITEM_SNOWBALL : new ItemParticleOption(ParticleTypes.ITEM, var1);
   }

   @Override
   public void handleEntityEvent(byte param0) {
      if (param0 == 3) {
         ParticleOptions var2 = this.getParticle();

         for (int var3 = 0; var3 < 8; var3++) {
            this.level.addParticle(var2, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult param0) {
      super.onHitEntity(param0);
      Entity var2 = param0.getEntity();
      int var3 = var2 instanceof Blaze ? 3 : 0;
      var2.hurt(this.damageSources().thrown(this, this.getOwner()), var3);
   }

   @Override
   protected void onHit(HitResult param0) {
      super.onHit(param0);
      if (!this.level.isClientSide) {
         this.level.broadcastEntityEvent(this, (byte)3);
         this.discard();
      }
   }
}
