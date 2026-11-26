package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class PlayTagWithOtherKids {
   private static final int MAX_FLEE_XZ_DIST = 20;
   private static final int MAX_FLEE_Y_DIST = 8;
   private static final float FLEE_SPEED_MODIFIER = 0.6F;
   private static final float CHASE_SPEED_MODIFIER = 0.6F;
   private static final int MAX_CHASERS_PER_TARGET = 5;
   private static final int AVERAGE_WAIT_TIME_BETWEEN_RUNS = 10;

   public static BehaviorControl<PathfinderMob> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(
               l_param0.present(MemoryModuleType.VISIBLE_VILLAGER_BABIES),
               l_param0.absent(MemoryModuleType.WALK_TARGET),
               l_param0.registered(MemoryModuleType.LOOK_TARGET),
               l_param0.registered(MemoryModuleType.INTERACTION_TARGET)
            )
            .apply(l_param0, (l2_param1, l2_param2, l2_param3, l2_param4) -> (l3_param5, l3_param6, l3_param7) -> {
               if (l3_param5.getRandom().nextInt(10) != 0) {
                  return false;
               }

               List<LivingEntity> l3_var9 = l_param0.get(l2_param1);
               Optional<LivingEntity> l3_var10 = l3_var9.stream().filter(l4_param1 -> isFriendChasingMe(l3_param6, l4_param1)).findAny();
               if (!l3_var10.isPresent()) {
                  Optional<LivingEntity> l3_var11 = findSomeoneBeingChased(l3_var9);
                  if (l3_var11.isPresent()) {
                     chaseKid(l2_param4, l2_param3, l2_param2, l3_var11.get());
                     return true;
                  } else {
                     l3_var9.stream().findAny().ifPresent(l4_param3 -> chaseKid(l2_param4, l2_param3, l2_param2, l4_param3));
                     return true;
                  }
               } else {
                  for (int l3_var11 = 0; l3_var11 < 10; l3_var11++) {
                     Vec3 l3_var12 = LandRandomPos.getPos(l3_param6, 20, 8);
                     if (l3_var12 != null && l3_param5.isVillage(BlockPos.containing(l3_var12))) {
                        l2_param2.set(new WalkTarget(l3_var12, 0.6F, 0));
                        break;
                     }
                  }

                  return true;
               }
            })
      );
   }

   private static void chaseKid(
      MemoryAccessor<?, LivingEntity> param0, MemoryAccessor<?, PositionTracker> param1, MemoryAccessor<?, WalkTarget> param2, LivingEntity param3
   ) {
      param0.set(param3);
      param1.set(new EntityTracker(param3, true));
      param2.set(new WalkTarget(new EntityTracker(param3, false), 0.6F, 1));
   }

   private static Optional<LivingEntity> findSomeoneBeingChased(List<LivingEntity> param0) {
      Map<LivingEntity, Integer> var1 = checkHowManyChasersEachFriendHas(param0);
      return var1.entrySet()
         .stream()
         .sorted(Comparator.comparingInt(Entry::getValue))
         .filter(l_param0 -> l_param0.getValue() > 0 && l_param0.getValue() <= 5)
         .map(Entry::getKey)
         .findFirst();
   }

   private static Map<LivingEntity, Integer> checkHowManyChasersEachFriendHas(List<LivingEntity> param0) {
      Map<LivingEntity, Integer> var1 = Maps.newHashMap();
      param0.stream()
         .filter(PlayTagWithOtherKids::isChasingSomeone)
         .forEach(l_param1 -> var1.compute(whoAreYouChasing(l_param1), (l2_param0, l2_param1) -> l2_param1 == null ? 1 : l2_param1 + 1));
      return var1;
   }

   private static LivingEntity whoAreYouChasing(LivingEntity param0) {
      return param0.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
   }

   private static boolean isChasingSomeone(LivingEntity param0) {
      return param0.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
   }

   private static boolean isFriendChasingMe(LivingEntity param0, LivingEntity param1) {
      return param1.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).filter(l_param1 -> l_param1 == param0).isPresent();
   }
}
