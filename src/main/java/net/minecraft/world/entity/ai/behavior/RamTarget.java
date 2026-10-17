package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.phys.Vec3;

public class RamTarget extends Behavior<Goat> {
   public static final int TIME_OUT_DURATION = 200;
   public static final float RAM_SPEED_FORCE_FACTOR = 1.65F;
   private final Function<Goat, UniformInt> getTimeBetweenRams;
   private final TargetingConditions ramTargeting;
   private final float speed;
   private final ToDoubleFunction<Goat> getKnockbackForce;
   private Vec3 ramDirection;
   private final Function<Goat, SoundEvent> getImpactSound;
   private final Function<Goat, SoundEvent> getHornBreakSound;

   public RamTarget(
      Function<Goat, UniformInt> param0,
      TargetingConditions param1,
      float param2,
      ToDoubleFunction<Goat> param3,
      Function<Goat, SoundEvent> param4,
      Function<Goat, SoundEvent> param5
   ) {
      super(ImmutableMap.of(MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_PRESENT), 200);
      this.getTimeBetweenRams = param0;
      this.ramTargeting = param1;
      this.speed = param2;
      this.getKnockbackForce = param3;
      this.getImpactSound = param4;
      this.getHornBreakSound = param5;
      this.ramDirection = Vec3.ZERO;
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Goat param1) {
      return param1.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
   }

   protected boolean canStillUse(ServerLevel param0, Goat param1, long param2) {
      return param1.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
   }

   protected void start(ServerLevel param0, Goat param1, long param2) {
      BlockPos var5 = param1.blockPosition();
      Brain<?> var6 = param1.getBrain();
      Vec3 var7 = var6.getMemory(MemoryModuleType.RAM_TARGET).get();
      this.ramDirection = new Vec3(var5.getX() - var7.x(), 0.0, var5.getZ() - var7.z()).normalize();
      var6.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(var7, this.speed, 0));
   }

   protected void tick(ServerLevel param0, Goat param1, long param2) {
      List<LivingEntity> var5 = param0.getNearbyEntities(LivingEntity.class, this.ramTargeting, param1, param1.getBoundingBox());
      Brain<?> var6 = param1.getBrain();
      if (!var5.isEmpty()) {
         LivingEntity var7 = var5.get(0);
         var7.hurt(param0.damageSources().noAggroMobAttack(param1), (float)param1.getAttributeValue(Attributes.ATTACK_DAMAGE));
         int var8 = param1.hasEffect(MobEffects.MOVEMENT_SPEED) ? param1.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1 : 0;
         int var9 = param1.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ? param1.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() + 1 : 0;
         float var10 = 0.25F * (var8 - var9);
         float var11 = Mth.clamp(param1.getSpeed() * 1.65F, 0.2F, 3.0F) + var10;
         float var12 = var7.isDamageSourceBlocked(param0.damageSources().mobAttack(param1)) ? 0.5F : 1.0F;
         var7.knockback(var12 * var11 * this.getKnockbackForce.applyAsDouble(param1), this.ramDirection.x(), this.ramDirection.z());
         this.finishRam(param0, param1);
         param0.playSound(null, param1, this.getImpactSound.apply(param1), SoundSource.NEUTRAL, 1.0F, 1.0F);
      } else if (this.hasRammedHornBreakingBlock(param0, param1)) {
         param0.playSound(null, param1, this.getImpactSound.apply(param1), SoundSource.NEUTRAL, 1.0F, 1.0F);
         boolean var7 = param1.dropHorn();
         if (var7) {
            param0.playSound(null, param1, this.getHornBreakSound.apply(param1), SoundSource.NEUTRAL, 1.0F, 1.0F);
         }

         this.finishRam(param0, param1);
      } else {
         Optional<WalkTarget> var7 = var6.getMemory(MemoryModuleType.WALK_TARGET);
         Optional<Vec3> var8 = var6.getMemory(MemoryModuleType.RAM_TARGET);
         boolean var9 = var7.isEmpty() || var8.isEmpty() || var7.get().getTarget().currentPosition().closerThan(var8.get(), 0.25);
         if (var9) {
            this.finishRam(param0, param1);
         }
      }
   }

   private boolean hasRammedHornBreakingBlock(ServerLevel param0, Goat param1) {
      Vec3 var3 = param1.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize();
      BlockPos var4 = BlockPos.containing(param1.position().add(var3));
      return param0.getBlockState(var4).is(BlockTags.SNAPS_GOAT_HORN) || param0.getBlockState(var4.above()).is(BlockTags.SNAPS_GOAT_HORN);
   }

   protected void finishRam(ServerLevel param0, Goat param1) {
      param0.broadcastEntityEvent(param1, (byte)59);
      param1.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getTimeBetweenRams.apply(param1).sample(param0.random));
      param1.getBrain().eraseMemory(MemoryModuleType.RAM_TARGET);
   }
}
