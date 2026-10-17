package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Animal;

public class AnimalMakeLove extends Behavior<Animal> {
   private static final int BREED_RANGE = 3;
   private static final int MIN_DURATION = 60;
   private static final int MAX_DURATION = 110;
   private final EntityType<? extends Animal> partnerType;
   private final float speedModifier;
   private long spawnChildAtTime;

   public AnimalMakeLove(EntityType<? extends Animal> param0, float param1) {
      super(
         ImmutableMap.of(
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.BREED_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED
         ),
         110
      );
      this.partnerType = param0;
      this.speedModifier = param1;
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Animal param1) {
      return param1.isInLove() && this.findValidBreedPartner(param1).isPresent();
   }

   protected void start(ServerLevel param0, Animal param1, long param2) {
      Animal var5 = this.findValidBreedPartner(param1).get();
      param1.getBrain().setMemory(MemoryModuleType.BREED_TARGET, var5);
      var5.getBrain().setMemory(MemoryModuleType.BREED_TARGET, param1);
      BehaviorUtils.lockGazeAndWalkToEachOther(param1, var5, this.speedModifier);
      int var6 = 60 + param1.getRandom().nextInt(50);
      this.spawnChildAtTime = param2 + var6;
   }

   protected boolean canStillUse(ServerLevel param0, Animal param1, long param2) {
      if (!this.hasBreedTargetOfRightType(param1)) {
         return false;
      }

      Animal var5 = this.getBreedTarget(param1);
      return var5.isAlive() && param1.canMate(var5) && BehaviorUtils.entityIsVisible(param1.getBrain(), var5) && param2 <= this.spawnChildAtTime;
   }

   protected void tick(ServerLevel param0, Animal param1, long param2) {
      Animal var5 = this.getBreedTarget(param1);
      BehaviorUtils.lockGazeAndWalkToEachOther(param1, var5, this.speedModifier);
      if (param1.closerThan(var5, 3.0)) {
         if (param2 >= this.spawnChildAtTime) {
            param1.spawnChildFromBreeding(param0, var5);
            param1.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
            var5.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
         }
      }
   }

   protected void stop(ServerLevel param0, Animal param1, long param2) {
      param1.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
      param1.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      param1.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      this.spawnChildAtTime = 0L;
   }

   private Animal getBreedTarget(Animal param0) {
      return (Animal)param0.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
   }

   private boolean hasBreedTargetOfRightType(Animal param0) {
      Brain<?> var2 = param0.getBrain();
      return var2.hasMemoryValue(MemoryModuleType.BREED_TARGET) && var2.getMemory(MemoryModuleType.BREED_TARGET).get().getType() == this.partnerType;
   }

   private Optional<? extends Animal> findValidBreedPartner(Animal param0) {
      return param0.getBrain()
         .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .get()
         .findClosest(l_param1 -> l_param1.getType() == this.partnerType && l_param1 instanceof Animal l_var3 && param0.canMate(l_var3))
         .map(Animal.class::cast);
   }
}
