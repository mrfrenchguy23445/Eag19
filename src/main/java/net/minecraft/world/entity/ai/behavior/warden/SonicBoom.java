package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

public class SonicBoom extends Behavior<Warden> {
   private static final int DISTANCE_XZ = 15;
   private static final int DISTANCE_Y = 20;
   private static final double KNOCKBACK_VERTICAL = 0.5;
   private static final double KNOCKBACK_HORIZONTAL = 2.5;
   public static final int COOLDOWN = 40;
   private static final int TICKS_BEFORE_PLAYING_SOUND = Mth.ceil(34.0);
   private static final int DURATION = Mth.ceil(60.0F);

   public SonicBoom() {
      super(
         ImmutableMap.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.SONIC_BOOM_COOLDOWN,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN,
            MemoryStatus.REGISTERED,
            MemoryModuleType.SONIC_BOOM_SOUND_DELAY,
            MemoryStatus.REGISTERED
         ),
         DURATION
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Warden param1) {
      return param1.closerThan(param1.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get(), 15.0, 20.0);
   }

   protected boolean canStillUse(ServerLevel param0, Warden param1, long param2) {
      return true;
   }

   protected void start(ServerLevel param0, Warden param1, long param2) {
      param1.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, DURATION);
      param1.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_SOUND_DELAY, Unit.INSTANCE, TICKS_BEFORE_PLAYING_SOUND);
      param0.broadcastEntityEvent(param1, (byte)62);
      param1.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 1.0F);
   }

   protected void tick(ServerLevel param0, Warden param1, long param2) {
      param1.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(l_param1 -> param1.getLookControl().setLookAt(l_param1.position()));
      if (!param1.getBrain().hasMemoryValue(MemoryModuleType.SONIC_BOOM_SOUND_DELAY)
         && !param1.getBrain().hasMemoryValue(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN)) {
         param1.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN, Unit.INSTANCE, DURATION - TICKS_BEFORE_PLAYING_SOUND);
         param1.getBrain()
            .getMemory(MemoryModuleType.ATTACK_TARGET)
            .filter(param1::canTargetEntity)
            .filter(l_param1 -> param1.closerThan(l_param1, 15.0, 20.0))
            .ifPresent(l_param2 -> {
               Vec3 l_var3 = param1.position().add(0.0, 1.6F, 0.0);
               Vec3 l_var4 = l_param2.getEyePosition().subtract(l_var3);
               Vec3 l_var5 = l_var4.normalize();

               for (int l_var6 = 1; l_var6 < Mth.floor(l_var4.length()) + 7; l_var6++) {
                  Vec3 l_var7 = l_var3.add(l_var5.scale(l_var6));
                  param0.sendParticles(ParticleTypes.SONIC_BOOM, l_var7.x, l_var7.y, l_var7.z, 1, 0.0, 0.0, 0.0, 0.0);
               }

               param1.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
               l_param2.hurt(param0.damageSources().sonicBoom(param1), 10.0F);
               double l_var6 = 0.5 * (1.0 - l_param2.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
               double l_var8 = 2.5 * (1.0 - l_param2.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
               l_param2.push(l_var5.x() * l_var8, l_var5.y() * l_var6, l_var5.z() * l_var8);
            });
      }
   }

   protected void stop(ServerLevel param0, Warden param1, long param2) {
      setCooldown(param1, 40);
   }

   public static void setCooldown(LivingEntity param0, int param1) {
      param0.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_COOLDOWN, Unit.INSTANCE, param1);
   }
}
