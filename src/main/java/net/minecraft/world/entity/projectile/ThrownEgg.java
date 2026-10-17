package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownEgg extends ThrowableItemProjectile {
   public ThrownEgg(EntityType<? extends ThrownEgg> param0, Level param1) {
      super(param0, param1);
   }

   public ThrownEgg(Level param0, LivingEntity param1) {
      super(EntityType.EGG, param1, param0);
   }

   public ThrownEgg(Level param0, double param1, double param3, double param5) {
      super(EntityType.EGG, param1, param3, param5, param0);
   }

   @Override
   public void handleEntityEvent(byte param0) {
      if (param0 == 3) {
         double var2 = 0.08;

         for (int var4 = 0; var4 < 8; var4++) {
            this.level
               .addParticle(
                  new ItemParticleOption(ParticleTypes.ITEM, this.getItem()),
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  (this.random.nextFloat() - 0.5) * 0.08,
                  (this.random.nextFloat() - 0.5) * 0.08,
                  (this.random.nextFloat() - 0.5) * 0.08
               );
         }
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult param0) {
      super.onHitEntity(param0);
      param0.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
   }

   @Override
   protected void onHit(HitResult param0) {
      super.onHit(param0);
      if (!this.level.isClientSide) {
         if (this.random.nextInt(8) == 0) {
            int var2 = 1;
            if (this.random.nextInt(32) == 0) {
               var2 = 4;
            }

            for (int var3 = 0; var3 < var2; var3++) {
               Chicken var4 = EntityType.CHICKEN.create(this.level);
               if (var4 != null) {
                  var4.setAge(-24000);
                  var4.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                  this.level.addFreshEntity(var4);
               }
            }
         }

         this.level.broadcastEntityEvent(this, (byte)3);
         this.discard();
      }
   }

   @Override
   protected Item getDefaultItem() {
      return Items.EGG;
   }
}
