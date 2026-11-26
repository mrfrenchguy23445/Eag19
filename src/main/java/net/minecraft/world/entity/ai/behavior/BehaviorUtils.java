package net.minecraft.world.entity.ai.behavior;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class BehaviorUtils {
   private BehaviorUtils() {
   }

   public static void lockGazeAndWalkToEachOther(LivingEntity param0, LivingEntity param1, float param2) {
      lookAtEachOther(param0, param1);
      setWalkAndLookTargetMemoriesToEachOther(param0, param1, param2);
   }

   public static boolean entityIsVisible(Brain<?> param0, LivingEntity param1) {
      Optional<NearestVisibleLivingEntities> var2 = param0.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
      return var2.isPresent() && var2.get().contains(param1);
   }

   public static boolean targetIsValid(Brain<?> param0, MemoryModuleType<? extends LivingEntity> param1, EntityType<?> param2) {
      return targetIsValid(param0, param1, l_param1 -> l_param1.getType() == param2);
   }

   private static boolean targetIsValid(Brain<?> param0, MemoryModuleType<? extends LivingEntity> param1, Predicate<LivingEntity> param2) {
      return param0.getMemory(param1).filter(param2).filter(LivingEntity::isAlive).filter(l_param1 -> entityIsVisible(param0, l_param1)).isPresent();
   }

   private static void lookAtEachOther(LivingEntity param0, LivingEntity param1) {
      lookAtEntity(param0, param1);
      lookAtEntity(param1, param0);
   }

   public static void lookAtEntity(LivingEntity param0, LivingEntity param1) {
      param0.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(param1, true));
   }

   private static void setWalkAndLookTargetMemoriesToEachOther(LivingEntity param0, LivingEntity param1, float param2) {
      int var3 = 2;
      setWalkAndLookTargetMemories(param0, param1, param2, 2);
      setWalkAndLookTargetMemories(param1, param0, param2, 2);
   }

   public static void setWalkAndLookTargetMemories(LivingEntity param0, Entity param1, float param2, int param3) {
      setWalkAndLookTargetMemories(param0, new EntityTracker(param1, true), param2, param3);
   }

   public static void setWalkAndLookTargetMemories(LivingEntity param0, BlockPos param1, float param2, int param3) {
      setWalkAndLookTargetMemories(param0, new BlockPosTracker(param1), param2, param3);
   }

   public static void setWalkAndLookTargetMemories(LivingEntity param0, PositionTracker param1, float param2, int param3) {
      WalkTarget var4 = new WalkTarget(param1, param2, param3);
      param0.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, param1);
      param0.getBrain().setMemory(MemoryModuleType.WALK_TARGET, var4);
   }

   public static void throwItem(LivingEntity param0, ItemStack param1, Vec3 param2) {
      Vec3 var3 = new Vec3(0.3F, 0.3F, 0.3F);
      throwItem(param0, param1, param2, var3, 0.3F);
   }

   public static void throwItem(LivingEntity param0, ItemStack param1, Vec3 param2, Vec3 param3, float param4) {
      double var5 = param0.getEyeY() - param4;
      ItemEntity var7 = new ItemEntity(param0.level, param0.getX(), var5, param0.getZ(), param1);
      var7.setThrower(param0.getUUID());
      Vec3 var8 = param2.subtract(param0.position());
      var8 = var8.normalize().multiply(param3.x, param3.y, param3.z);
      var7.setDeltaMovement(var8);
      var7.setDefaultPickUpDelay();
      param0.level.addFreshEntity(var7);
   }

   public static SectionPos findSectionClosestToVillage(ServerLevel param0, SectionPos param1, int param2) {
      int var3 = param0.sectionsToVillage(param1);
      return SectionPos.cube(param1, param2)
         .filter(l_param2 -> param0.sectionsToVillage(l_param2) < var3)
         .min(Comparator.comparingInt(param0::sectionsToVillage))
         .orElse(param1);
   }

   public static boolean isWithinAttackRange(Mob param0, LivingEntity param1, int param2) {
      if (param0.getMainHandItem().getItem() instanceof ProjectileWeaponItem var3 && param0.canFireProjectileWeapon(var3)) {
         int var4 = var3.getDefaultProjectileRange() - param2;
         return param0.closerThan(param1, var4);
      } else {
         return param0.isWithinMeleeAttackRange(param1);
      }
   }

   public static boolean isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(LivingEntity param0, LivingEntity param1, double param2) {
      Optional<LivingEntity> var4 = param0.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
      if (var4.isEmpty()) {
         return false;
      }

      double var5 = param0.distanceToSqr(var4.get().position());
      double var7 = param0.distanceToSqr(param1.position());
      return var7 > var5 + param2 * param2;
   }

   public static boolean canSee(LivingEntity param0, LivingEntity param1) {
      Brain<?> var2 = param0.getBrain();
      return !var2.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         ? false
         : var2.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().contains(param1);
   }

   public static LivingEntity getNearestTarget(LivingEntity param0, Optional<LivingEntity> param1, LivingEntity param2) {
      return param1.isEmpty() ? param2 : getTargetNearestMe(param0, param1.get(), param2);
   }

   public static LivingEntity getTargetNearestMe(LivingEntity param0, LivingEntity param1, LivingEntity param2) {
      Vec3 var3 = param1.position();
      Vec3 var4 = param2.position();
      return param0.distanceToSqr(var3) < param0.distanceToSqr(var4) ? param1 : param2;
   }

   public static Optional<LivingEntity> getLivingEntityFromUUIDMemory(LivingEntity param0, MemoryModuleType<UUID> param1) {
      Optional<UUID> var2 = param0.getBrain().getMemory(param1);
      return var2.<Entity>map(l_param1 -> ((ServerLevel)param0.level).getEntity(l_param1))
         .map(l_param0 -> l_param0 instanceof LivingEntity l_var1 ? l_var1 : null);
   }

   @Nullable
   public static Vec3 getRandomSwimmablePos(PathfinderMob param0, int param1, int param2) {
      Vec3 var3 = DefaultRandomPos.getPos(param0, param1, param2);
      int var4 = 0;

      while (
         var3 != null
            && !param0.level.getBlockState(BlockPos.containing(var3)).isPathfindable(param0.level, BlockPos.containing(var3), PathComputationType.WATER)
            && var4++ < 10
      ) {
         var3 = DefaultRandomPos.getPos(param0, param1, param2);
      }

      return var3;
   }

   public static boolean isBreeding(LivingEntity param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET);
   }
}
