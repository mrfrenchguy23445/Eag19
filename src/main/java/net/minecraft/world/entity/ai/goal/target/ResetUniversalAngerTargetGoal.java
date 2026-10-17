package net.minecraft.world.entity.ai.goal.target;

import java.util.List;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;

public class ResetUniversalAngerTargetGoal<T extends Mob & NeutralMob> extends Goal {
   private static final int ALERT_RANGE_Y = 10;
   private final T mob;
   private final boolean alertOthersOfSameType;
   private int lastHurtByPlayerTimestamp;

   public ResetUniversalAngerTargetGoal(T param0, boolean param1) {
      this.mob = param0;
      this.alertOthersOfSameType = param1;
   }

   @Override
   public boolean canUse() {
      return this.mob.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER) && this.wasHurtByPlayer();
   }

   private boolean wasHurtByPlayer() {
      return this.mob.getLastHurtByMob() != null
         && this.mob.getLastHurtByMob().getType() == EntityType.PLAYER
         && this.mob.getLastHurtByMobTimestamp() > this.lastHurtByPlayerTimestamp;
   }

   @Override
   public void start() {
      this.lastHurtByPlayerTimestamp = this.mob.getLastHurtByMobTimestamp();
      this.mob.forgetCurrentTargetAndRefreshUniversalAnger();
      if (this.alertOthersOfSameType) {
         this.getNearbyMobsOfSameType()
            .stream()
            .filter(l_param0 -> l_param0 != this.mob)
            .map(l_param0 -> (NeutralMob)l_param0)
            .forEach(NeutralMob::forgetCurrentTargetAndRefreshUniversalAnger);
      }

      super.start();
   }

   private List<? extends Mob> getNearbyMobsOfSameType() {
      double var1 = this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
      AABB var3 = AABB.unitCubeFromLowerCorner(this.mob.position()).inflate(var1, 10.0, var1);
      return this.mob.level.getEntitiesOfClass((Class<? extends Mob>)this.mob.getClass(), var3, EntitySelector.NO_SPECTATORS);
   }
}
