package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class AvoidEntityGoal<T extends LivingEntity> extends Goal {
   protected final PathfinderMob mob;
   private final double walkSpeedModifier;
   private final double sprintSpeedModifier;
   @Nullable
   protected T toAvoid;
   protected final float maxDist;
   @Nullable
   protected Path path;
   protected final PathNavigation pathNav;
   protected final Class<T> avoidClass;
   protected final Predicate<LivingEntity> avoidPredicate;
   protected final Predicate<LivingEntity> predicateOnAvoidEntity;
   private final TargetingConditions avoidEntityTargeting;

   public AvoidEntityGoal(PathfinderMob param0, Class<T> param1, float param2, double param3, double param5) {
      this(param0, param1, l_param0 -> true, param2, param3, param5, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
   }

   public AvoidEntityGoal(
      PathfinderMob param0, Class<T> param1, Predicate<LivingEntity> param2, float param3, double param4, double param6, Predicate<LivingEntity> param8
   ) {
      this.mob = param0;
      this.avoidClass = param1;
      this.avoidPredicate = param2;
      this.maxDist = param3;
      this.walkSpeedModifier = param4;
      this.sprintSpeedModifier = param6;
      this.predicateOnAvoidEntity = param8;
      this.pathNav = param0.getNavigation();
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      this.avoidEntityTargeting = TargetingConditions.forCombat().range(param3).selector(param8.and(param2));
   }

   public AvoidEntityGoal(PathfinderMob param0, Class<T> param1, float param2, double param3, double param5, Predicate<LivingEntity> param7) {
      this(param0, param1, l_param0 -> true, param2, param3, param5, param7);
   }

   @Override
   public boolean canUse() {
      this.toAvoid = this.mob
         .level
         .getNearestEntity(
            this.mob.level.getEntitiesOfClass(this.avoidClass, this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist), l_param0 -> true),
            this.avoidEntityTargeting,
            this.mob,
            this.mob.getX(),
            this.mob.getY(),
            this.mob.getZ()
         );
      if (this.toAvoid == null) {
         return false;
      }

      Vec3 var1 = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.toAvoid.position());
      if (var1 == null) {
         return false;
      }

      if (this.toAvoid.distanceToSqr(var1.x, var1.y, var1.z) < this.toAvoid.distanceToSqr(this.mob)) {
         return false;
      }

      this.path = this.pathNav.createPath(var1.x, var1.y, var1.z, 0);
      return this.path != null;
   }

   @Override
   public boolean canContinueToUse() {
      return !this.pathNav.isDone();
   }

   @Override
   public void start() {
      this.pathNav.moveTo(this.path, this.walkSpeedModifier);
   }

   @Override
   public void stop() {
      this.toAvoid = null;
   }

   @Override
   public void tick() {
      if (this.mob.distanceToSqr(this.toAvoid) < 49.0) {
         this.mob.getNavigation().setSpeedModifier(this.sprintSpeedModifier);
      } else {
         this.mob.getNavigation().setSpeedModifier(this.walkSpeedModifier);
      }
   }
}
