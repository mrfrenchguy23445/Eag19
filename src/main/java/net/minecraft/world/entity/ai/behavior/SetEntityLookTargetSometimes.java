package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

@Deprecated
public class SetEntityLookTargetSometimes {
   public static BehaviorControl<LivingEntity> create(float param0, UniformInt param1) {
      return create(param0, param1, l_param0 -> true);
   }

   public static BehaviorControl<LivingEntity> create(EntityType<?> param0, float param1, UniformInt param2) {
      return create(param1, param2, l_param1 -> param0.equals(l_param1.getType()));
   }

   private static BehaviorControl<LivingEntity> create(float param0, UniformInt param1, Predicate<LivingEntity> param2) {
      float var3 = param0 * param0;
      SetEntityLookTargetSometimes.Ticker var4 = new SetEntityLookTargetSometimes.Ticker(param1);
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(l_param3.absent(MemoryModuleType.LOOK_TARGET), l_param3.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES))
            .apply(
               l_param3,
               (l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
                  Optional<LivingEntity> l3_var10 = l_param3.<NearestVisibleLivingEntities>get(l2_param5)
                     .findClosest(param2.and(l4_param2 -> l4_param2.distanceToSqr(l3_param7) <= var3));
                  if (l3_var10.isEmpty()) {
                     return false;
                  }

                  if (!var4.tickDownAndCheck(l3_param6.random)) {
                     return false;
                  }

                  l2_param4.set(new EntityTracker(l3_var10.get(), true));
                  return true;
               }
            )
      );
   }

   public static final class Ticker {
      private final UniformInt interval;
      private int ticksUntilNextStart;

      public Ticker(UniformInt param0) {
         if (param0.getMinValue() <= 1) {
            throw new IllegalArgumentException();
         }

         this.interval = param0;
      }

      public boolean tickDownAndCheck(RandomSource param0) {
         if (this.ticksUntilNextStart == 0) {
            this.ticksUntilNextStart = this.interval.sample(param0) - 1;
            return false;
         } else {
            return --this.ticksUntilNextStart == 0;
         }
      }
   }
}
