package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

public class LocateHidingPlace {
   public static OneShot<LivingEntity> create(int param0, float param1, int param2) {
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(
               l_param3.absent(MemoryModuleType.WALK_TARGET),
               l_param3.registered(MemoryModuleType.HOME),
               l_param3.registered(MemoryModuleType.HIDING_PLACE),
               l_param3.registered(MemoryModuleType.PATH),
               l_param3.registered(MemoryModuleType.LOOK_TARGET),
               l_param3.registered(MemoryModuleType.BREED_TARGET),
               l_param3.registered(MemoryModuleType.INTERACTION_TARGET)
            )
            .apply(
               l_param3,
               (l2_param4, l2_param5, l2_param6, l2_param7, l2_param8, l2_param9, l2_param10) -> (l3_param11, l3_param12, l3_param13) -> {
                  l3_param11.getPoiManager()
                     .find(l4_param0 -> l4_param0.is(PoiTypes.HOME), l4_param0 -> true, l3_param12.blockPosition(), param2 + 1, PoiManager.Occupancy.ANY)
                     .filter(l4_param2 -> l4_param2.closerToCenterThan(l3_param12.position(), param2))
                     .or(
                        () -> l3_param11.getPoiManager()
                           .getRandom(
                              l5_param0 -> l5_param0.is(PoiTypes.HOME),
                              l5_param0 -> true,
                              PoiManager.Occupancy.ANY,
                              l3_param12.blockPosition(),
                              param0,
                              l3_param12.getRandom()
                           )
                     )
                     .or(() -> l_param3.<GlobalPos>tryGet(l2_param5).map(GlobalPos::pos))
                     .ifPresent(l4_param10 -> {
                        l2_param7.erase();
                        l2_param8.erase();
                        l2_param9.erase();
                        l2_param10.erase();
                        l2_param6.set(GlobalPos.of(l3_param11.dimension(), l4_param10));
                        if (!l4_param10.closerToCenterThan(l3_param12.position(), param2)) {
                           l2_param4.set(new WalkTarget(l4_param10, param1, param2));
                        }
                     });
                  return true;
               }
            )
      );
   }
}
