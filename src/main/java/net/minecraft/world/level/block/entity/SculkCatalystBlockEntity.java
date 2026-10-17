package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

public class SculkCatalystBlockEntity extends BlockEntity implements GameEventListener {
   private final BlockPositionSource blockPosSource = new BlockPositionSource(this.worldPosition);
   private final SculkSpreader sculkSpreader = SculkSpreader.createLevelSpreader();

   public SculkCatalystBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.SCULK_CATALYST, param0, param1);
   }

   @Override
   public PositionSource getListenerSource() {
      return this.blockPosSource;
   }

   @Override
   public int getListenerRadius() {
      return 8;
   }

   @Override
   public GameEventListener.DeliveryMode getDeliveryMode() {
      return GameEventListener.DeliveryMode.BY_DISTANCE;
   }

   @Override
   public boolean handleGameEvent(ServerLevel param0, GameEvent param1, GameEvent.Context param2, Vec3 param3) {
      if (param1 == GameEvent.ENTITY_DIE && param2.sourceEntity() instanceof LivingEntity var5) {
         if (!var5.wasExperienceConsumed()) {
            int var6 = var5.getExperienceReward();
            if (var5.shouldDropExperience() && var6 > 0) {
               this.sculkSpreader.addCursors(BlockPos.containing(param3.relative(Direction.UP, 0.5)), var6);
               this.tryAwardItSpreadsAdvancement(var5);
            }

            var5.skipDropExperience();
            SculkCatalystBlock.bloom(param0, this.worldPosition, this.getBlockState(), param0.getRandom());
         }

         return true;
      } else {
         return false;
      }
   }

   private void tryAwardItSpreadsAdvancement(LivingEntity param0) {
      if (param0.getLastHurtByMob() instanceof ServerPlayer var3) {
         DamageSource var4 = param0.getLastDamageSource() == null ? this.level.damageSources().playerAttack(var3) : param0.getLastDamageSource();
         CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST.trigger(var3, param0, var4);
      }
   }

   public static void serverTick(Level param0, BlockPos param1, BlockState param2, SculkCatalystBlockEntity param3) {
      param3.sculkSpreader.updateCursors(param0, param1, param0.getRandom(), true);
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      this.sculkSpreader.load(param0);
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      this.sculkSpreader.save(param0);
      super.saveAdditional(param0);
   }

   @VisibleForTesting
   public SculkSpreader getSculkSpreader() {
      return this.sculkSpreader;
   }
}
