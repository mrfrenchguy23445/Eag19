package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownEnderpearl extends ThrowableItemProjectile {
   public ThrownEnderpearl(EntityType<? extends ThrownEnderpearl> param0, Level param1) {
      super(param0, param1);
   }

   public ThrownEnderpearl(Level param0, LivingEntity param1) {
      super(EntityType.ENDER_PEARL, param1, param0);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.ENDER_PEARL;
   }

   @Override
   protected void onHitEntity(EntityHitResult param0) {
      super.onHitEntity(param0);
      param0.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
   }

   @Override
   protected void onHit(HitResult param0) {
      super.onHit(param0);

      for (int var2 = 0; var2 < 32; var2++) {
         this.level
            .addParticle(
               ParticleTypes.PORTAL,
               this.getX(),
               this.getY() + this.random.nextDouble() * 2.0,
               this.getZ(),
               this.random.nextGaussian(),
               0.0,
               this.random.nextGaussian()
            );
      }

      if (!this.level.isClientSide && !this.isRemoved()) {
         Entity var2 = this.getOwner();
         if (var2 instanceof ServerPlayer var3) {
            if (var3.connection.isAcceptingMessages() && var3.level == this.level && !var3.isSleeping()) {
               if (this.random.nextFloat() < 0.05F && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
                  Endermite var4 = EntityType.ENDERMITE.create(this.level);
                  if (var4 != null) {
                     var4.moveTo(var2.getX(), var2.getY(), var2.getZ(), var2.getYRot(), var2.getXRot());
                     this.level.addFreshEntity(var4);
                  }
               }

               if (var2.isPassenger()) {
                  var3.dismountTo(this.getX(), this.getY(), this.getZ());
               } else {
                  var2.teleportTo(this.getX(), this.getY(), this.getZ());
               }

               var2.resetFallDistance();
               var2.hurt(this.damageSources().fall(), 5.0F);
            }
         } else if (var2 != null) {
            var2.teleportTo(this.getX(), this.getY(), this.getZ());
            var2.resetFallDistance();
         }

         this.discard();
      }
   }

   @Override
   public void tick() {
      Entity var1 = this.getOwner();
      if (var1 instanceof Player && !var1.isAlive()) {
         this.discard();
      } else {
         super.tick();
      }
   }

   @Nullable
   @Override
   public Entity changeDimension(ServerLevel param0) {
      Entity var2 = this.getOwner();
      if (var2 != null && var2.level.dimension() != param0.dimension()) {
         this.setOwner(null);
      }

      return super.changeDimension(param0);
   }
}
