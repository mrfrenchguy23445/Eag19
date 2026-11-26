package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.pathfinder.Path;

public class VillagerMakeLove extends Behavior<Villager> {
   private static final int INTERACT_DIST_SQR = 5;
   private static final float SPEED_MODIFIER = 0.5F;
   private long birthTimestamp;

   public VillagerMakeLove() {
      super(
         ImmutableMap.of(
            MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
         ),
         350,
         350
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Villager param1) {
      return this.isBreedingPossible(param1);
   }

   protected boolean canStillUse(ServerLevel param0, Villager param1, long param2) {
      return param2 <= this.birthTimestamp && this.isBreedingPossible(param1);
   }

   protected void start(ServerLevel param0, Villager param1, long param2) {
      AgeableMob var5 = param1.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(param1, var5, 0.5F);
      param0.broadcastEntityEvent(var5, (byte)18);
      param0.broadcastEntityEvent(param1, (byte)18);
      int var6 = 275 + param1.getRandom().nextInt(50);
      this.birthTimestamp = param2 + var6;
   }

   protected void tick(ServerLevel param0, Villager param1, long param2) {
      Villager var5 = (Villager)param1.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      if (!(param1.distanceToSqr(var5) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(param1, var5, 0.5F);
         if (param2 >= this.birthTimestamp) {
            param1.eatAndDigestFood();
            var5.eatAndDigestFood();
            this.tryToGiveBirth(param0, param1, var5);
         } else if (param1.getRandom().nextInt(35) == 0) {
            param0.broadcastEntityEvent(var5, (byte)12);
            param0.broadcastEntityEvent(param1, (byte)12);
         }
      }
   }

   private void tryToGiveBirth(ServerLevel param0, Villager param1, Villager param2) {
      Optional<BlockPos> var4 = this.takeVacantBed(param0, param1);
      if (!var4.isPresent()) {
         param0.broadcastEntityEvent(param2, (byte)13);
         param0.broadcastEntityEvent(param1, (byte)13);
      } else {
         Optional<Villager> var5 = this.breed(param0, param1, param2);
         if (var5.isPresent()) {
            this.giveBedToChild(param0, var5.get(), var4.get());
         } else {
            param0.getPoiManager().release(var4.get());
            DebugPackets.sendPoiTicketCountPacket(param0, var4.get());
         }
      }
   }

   protected void stop(ServerLevel param0, Villager param1, long param2) {
      param1.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
   }

   private boolean isBreedingPossible(Villager param0) {
      Brain<Villager> var2 = param0.getBrain();
      Optional<AgeableMob> var3 = var2.getMemory(MemoryModuleType.BREED_TARGET).filter(l_param0 -> l_param0.getType() == EntityType.VILLAGER);
      return !var3.isPresent()
         ? false
         : BehaviorUtils.targetIsValid(var2, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER) && param0.canBreed() && var3.get().canBreed();
   }

   private Optional<BlockPos> takeVacantBed(ServerLevel param0, Villager param1) {
      return param0.getPoiManager()
         .take(l_param0 -> l_param0.is(PoiTypes.HOME), (l_param1, l_param2) -> this.canReach(param1, l_param2, l_param1), param1.blockPosition(), 48);
   }

   private boolean canReach(Villager param0, BlockPos param1, Holder<PoiType> param2) {
      Path var4 = param0.getNavigation().createPath(param1, param2.value().validRange());
      return var4 != null && var4.canReach();
   }

   private Optional<Villager> breed(ServerLevel param0, Villager param1, Villager param2) {
      Villager var4 = param1.getBreedOffspring(param0, param2);
      if (var4 == null) {
         return Optional.empty();
      }

      param1.setAge(6000);
      param2.setAge(6000);
      var4.setAge(-24000);
      var4.moveTo(param1.getX(), param1.getY(), param1.getZ(), 0.0F, 0.0F);
      param0.addFreshEntityWithPassengers(var4);
      param0.broadcastEntityEvent(var4, (byte)12);
      return Optional.of(var4);
   }

   private void giveBedToChild(ServerLevel param0, Villager param1, BlockPos param2) {
      GlobalPos var4 = GlobalPos.of(param0.dimension(), param2);
      param1.getBrain().setMemory(MemoryModuleType.HOME, var4);
   }
}
