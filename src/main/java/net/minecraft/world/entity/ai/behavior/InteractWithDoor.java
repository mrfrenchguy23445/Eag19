package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.OptionalBox.Mu;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

public class InteractWithDoor {
   private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
   private static final double SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN = 3.0;
   private static final double MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS = 2.0;

   public static BehaviorControl<LivingEntity> create() {
      MutableObject<Node> var0 = new MutableObject<>(null);
      MutableInt var1 = new MutableInt(0);
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(
               l_param2.present(MemoryModuleType.PATH),
               l_param2.registered(MemoryModuleType.DOORS_TO_CLOSE),
               l_param2.registered(MemoryModuleType.NEAREST_LIVING_ENTITIES)
            )
            .apply(
               l_param2,
               (l2_param3, l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
                  Path l3_var10 = l_param2.get(l2_param3);
                  Optional<Set<GlobalPos>> l3_var11 = l_param2.tryGet(l2_param4);
                  if (!l3_var10.notStarted() && !l3_var10.isDone()) {
                     if (Objects.equals(var0.getValue(), l3_var10.getNextNode())) {
                        var1.setValue(20);
                     } else if (var1.decrementAndGet() > 0) {
                        return false;
                     }

                     var0.setValue(l3_var10.getNextNode());
                     Node l3_var12 = l3_var10.getPreviousNode();
                     Node l3_var13 = l3_var10.getNextNode();
                     BlockPos l3_var14 = l3_var12.asBlockPos();
                     BlockState l3_var15 = l3_param6.getBlockState(l3_var14);
                     if (l3_var15.is(BlockTags.WOODEN_DOORS, l4_param0 -> l4_param0.getBlock() instanceof DoorBlock)) {
                        DoorBlock l3_var16 = (DoorBlock)l3_var15.getBlock();
                        if (!l3_var16.isOpen(l3_var15)) {
                           l3_var16.setOpen(l3_param7, l3_param6, l3_var15, l3_var14, true);
                        }

                        l3_var11 = rememberDoorToClose(l2_param4, l3_var11, l3_param6, l3_var14);
                     }

                     BlockPos l3_var16 = l3_var13.asBlockPos();
                     BlockState l3_var17 = l3_param6.getBlockState(l3_var16);
                     if (l3_var17.is(BlockTags.WOODEN_DOORS, l4_param0 -> l4_param0.getBlock() instanceof DoorBlock)) {
                        DoorBlock l3_var18 = (DoorBlock)l3_var17.getBlock();
                        if (!l3_var18.isOpen(l3_var17)) {
                           l3_var18.setOpen(l3_param7, l3_param6, l3_var17, l3_var16, true);
                           l3_var11 = rememberDoorToClose(l2_param4, l3_var11, l3_param6, l3_var16);
                        }
                     }

                     l3_var11.ifPresent(
                        l4_param6 -> closeDoorsThatIHaveOpenedOrPassedThrough(
                           l3_param6, l3_param7, l3_var12, l3_var13, (Set<GlobalPos>)l4_param6, l_param2.tryGet(l2_param5)
                        )
                     );
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }

   public static void closeDoorsThatIHaveOpenedOrPassedThrough(
      ServerLevel param0, LivingEntity param1, @Nullable Node param2, @Nullable Node param3, Set<GlobalPos> param4, Optional<List<LivingEntity>> param5
   ) {
      Iterator<GlobalPos> var6 = param4.iterator();

      while (var6.hasNext()) {
         GlobalPos var7 = var6.next();
         BlockPos var8 = var7.pos();
         if ((param2 == null || !param2.asBlockPos().equals(var8)) && (param3 == null || !param3.asBlockPos().equals(var8))) {
            if (isDoorTooFarAway(param0, param1, var7)) {
               var6.remove();
            } else {
               BlockState var9 = param0.getBlockState(var8);
               if (!var9.is(BlockTags.WOODEN_DOORS, l_param0 -> l_param0.getBlock() instanceof DoorBlock)) {
                  var6.remove();
               } else {
                  DoorBlock var10 = (DoorBlock)var9.getBlock();
                  if (!var10.isOpen(var9)) {
                     var6.remove();
                  } else if (areOtherMobsComingThroughDoor(param1, var8, param5)) {
                     var6.remove();
                  } else {
                     var10.setOpen(param1, param0, var9, var8, false);
                     var6.remove();
                  }
               }
            }
         }
      }
   }

   private static boolean areOtherMobsComingThroughDoor(LivingEntity param0, BlockPos param1, Optional<List<LivingEntity>> param2) {
      return param2.isEmpty()
         ? false
         : param2.get()
            .stream()
            .filter(l_param1 -> l_param1.getType() == param0.getType())
            .filter(l_param1 -> param1.closerToCenterThan(l_param1.position(), 2.0))
            .anyMatch(l_param1 -> isMobComingThroughDoor(l_param1.getBrain(), param1));
   }

   private static boolean isMobComingThroughDoor(Brain<?> param0, BlockPos param1) {
      if (!param0.hasMemoryValue(MemoryModuleType.PATH)) {
         return false;
      }

      Path var2 = param0.getMemory(MemoryModuleType.PATH).get();
      if (var2.isDone()) {
         return false;
      }

      Node var3 = var2.getPreviousNode();
      if (var3 == null) {
         return false;
      }

      Node var4 = var2.getNextNode();
      return param1.equals(var3.asBlockPos()) || param1.equals(var4.asBlockPos());
   }

   private static boolean isDoorTooFarAway(ServerLevel param0, LivingEntity param1, GlobalPos param2) {
      return param2.dimension() != param0.dimension() || !param2.pos().closerToCenterThan(param1.position(), 3.0);
   }

   private static Optional<Set<GlobalPos>> rememberDoorToClose(
      MemoryAccessor<Mu, Set<GlobalPos>> param0, Optional<Set<GlobalPos>> param1, ServerLevel param2, BlockPos param3
   ) {
      GlobalPos var4 = GlobalPos.of(param2.dimension(), param3);
      return Optional.of(param1.<Set<GlobalPos>>map(l_param1 -> {
         l_param1.add(var4);
         return l_param1;
      }).orElseGet(() -> {
         Set<GlobalPos> l_var2 = Sets.newHashSet(var4);
         param0.set(l_var2);
         return l_var2;
      }));
   }
}
