package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.allay.AllayAi;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GoAndGiveItemsToTarget<E extends LivingEntity & InventoryCarrier> extends Behavior<E> {
   private static final int CLOSE_ENOUGH_DISTANCE_TO_TARGET = 3;
   private static final int ITEM_PICKUP_COOLDOWN_AFTER_THROWING = 60;
   private final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter;
   private final float speedModifier;

   public GoAndGiveItemsToTarget(Function<LivingEntity, Optional<PositionTracker>> param0, float param1, int param2) {
      super(
         Map.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
            MemoryStatus.REGISTERED
         ),
         param2
      );
      this.targetPositionGetter = param0;
      this.speedModifier = param1;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel param0, E param1) {
      return this.canThrowItemToTarget(param1);
   }

   @Override
   protected boolean canStillUse(ServerLevel param0, E param1, long param2) {
      return this.canThrowItemToTarget(param1);
   }

   @Override
   protected void start(ServerLevel param0, E param1, long param2) {
      this.targetPositionGetter.apply(param1).ifPresent(l_param1 -> BehaviorUtils.setWalkAndLookTargetMemories(param1, l_param1, this.speedModifier, 3));
   }

   @Override
   protected void tick(ServerLevel param0, E param1, long param2) {
      Optional<PositionTracker> var5 = this.targetPositionGetter.apply(param1);
      if (!var5.isEmpty()) {
         PositionTracker var6 = var5.get();
         double var7 = var6.currentPosition().distanceTo(param1.getEyePosition());
         if (var7 < 3.0) {
            ItemStack var9 = param1.getInventory().removeItem(0, 1);
            if (!var9.isEmpty()) {
               throwItem(param1, var9, getThrowPosition(var6));
               if (param1 instanceof Allay var10) {
                  AllayAi.getLikedPlayer(var10).ifPresent(l_param2 -> this.triggerDropItemOnBlock(var6, var9, l_param2));
               }

               param1.getBrain().setMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, 60);
            }
         }
      }
   }

   private void triggerDropItemOnBlock(PositionTracker param0, ItemStack param1, ServerPlayer param2) {
      BlockPos var4 = param0.currentBlockPosition().below();
      CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.trigger(param2, var4, param1);
   }

   private boolean canThrowItemToTarget(E param0) {
      if (param0.getInventory().isEmpty()) {
         return false;
      }

      Optional<PositionTracker> var2 = this.targetPositionGetter.apply(param0);
      return var2.isPresent();
   }

   private static Vec3 getThrowPosition(PositionTracker param0) {
      return param0.currentPosition().add(0.0, 1.0, 0.0);
   }

   public static void throwItem(LivingEntity param0, ItemStack param1, Vec3 param2) {
      Vec3 var3 = new Vec3(0.2F, 0.3F, 0.2F);
      BehaviorUtils.throwItem(param0, param1, param2, var3, 0.2F);
      Level var4 = param0.level;
      if (var4.getGameTime() % 7L == 0L && var4.random.nextDouble() < 0.9) {
         float var5 = Util.getRandom(Allay.THROW_SOUND_PITCHES, var4.getRandom());
         var4.playSound(null, param0, SoundEvents.ALLAY_THROW, SoundSource.NEUTRAL, 1.0F, var5);
      }
   }
}
