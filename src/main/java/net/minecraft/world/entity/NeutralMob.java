package net.minecraft.world.entity;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public interface NeutralMob {
   String TAG_ANGER_TIME = "AngerTime";
   String TAG_ANGRY_AT = "AngryAt";

   int getRemainingPersistentAngerTime();

   void setRemainingPersistentAngerTime(int var1);

   @Nullable
   UUID getPersistentAngerTarget();

   void setPersistentAngerTarget(@Nullable UUID var1);

   void startPersistentAngerTimer();

   default void addPersistentAngerSaveData(CompoundTag param0) {
      param0.putInt("AngerTime", this.getRemainingPersistentAngerTime());
      if (this.getPersistentAngerTarget() != null) {
         param0.putUUID("AngryAt", this.getPersistentAngerTarget());
      }
   }

   default void readPersistentAngerSaveData(Level param0, CompoundTag param1) {
      this.setRemainingPersistentAngerTime(param1.getInt("AngerTime"));
      if (param0 instanceof ServerLevel) {
         if (!param1.hasUUID("AngryAt")) {
            this.setPersistentAngerTarget(null);
         } else {
            UUID var3 = param1.getUUID("AngryAt");
            this.setPersistentAngerTarget(var3);
            Entity var4 = ((ServerLevel)param0).getEntity(var3);
            if (var4 != null) {
               if (var4 instanceof Mob) {
                  this.setLastHurtByMob((Mob)var4);
               }

               if (var4.getType() == EntityType.PLAYER) {
                  this.setLastHurtByPlayer((Player)var4);
               }
            }
         }
      }
   }

   default void updatePersistentAnger(ServerLevel param0, boolean param1) {
      LivingEntity var3 = this.getTarget();
      UUID var4 = this.getPersistentAngerTarget();
      if ((var3 == null || var3.isDeadOrDying()) && var4 != null && param0.getEntity(var4) instanceof Mob) {
         this.stopBeingAngry();
      } else {
         if (var3 != null && !Objects.equals(var4, var3.getUUID())) {
            this.setPersistentAngerTarget(var3.getUUID());
            this.startPersistentAngerTimer();
         }

         if (this.getRemainingPersistentAngerTime() > 0 && (var3 == null || var3.getType() != EntityType.PLAYER || !param1)) {
            this.setRemainingPersistentAngerTime(this.getRemainingPersistentAngerTime() - 1);
            if (this.getRemainingPersistentAngerTime() == 0) {
               this.stopBeingAngry();
            }
         }
      }
   }

   default boolean isAngryAt(LivingEntity param0) {
      if (!this.canAttack(param0)) {
         return false;
      } else {
         return param0.getType() == EntityType.PLAYER && this.isAngryAtAllPlayers(param0.level)
            ? true
            : param0.getUUID().equals(this.getPersistentAngerTarget());
      }
   }

   default boolean isAngryAtAllPlayers(Level param0) {
      return param0.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER) && this.isAngry() && this.getPersistentAngerTarget() == null;
   }

   default boolean isAngry() {
      return this.getRemainingPersistentAngerTime() > 0;
   }

   default void playerDied(Player param0) {
      if (param0.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
         if (param0.getUUID().equals(this.getPersistentAngerTarget())) {
            this.stopBeingAngry();
         }
      }
   }

   default void forgetCurrentTargetAndRefreshUniversalAnger() {
      this.stopBeingAngry();
      this.startPersistentAngerTimer();
   }

   default void stopBeingAngry() {
      this.setLastHurtByMob(null);
      this.setPersistentAngerTarget(null);
      this.setTarget(null);
      this.setRemainingPersistentAngerTime(0);
   }

   @Nullable
   LivingEntity getLastHurtByMob();

   void setLastHurtByMob(@Nullable LivingEntity var1);

   void setLastHurtByPlayer(@Nullable Player var1);

   void setTarget(@Nullable LivingEntity var1);

   boolean canAttack(LivingEntity var1);

   @Nullable
   LivingEntity getTarget();
}
