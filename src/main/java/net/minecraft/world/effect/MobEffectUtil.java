package net.minecraft.world.effect;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class MobEffectUtil {
   public static Component formatDuration(MobEffectInstance param0, float param1) {
      if (param0.isInfiniteDuration()) {
         return Component.translatable("effect.duration.infinite");
      }

      int var2 = Mth.floor(param0.getDuration() * param1);
      return Component.literal(StringUtil.formatTickDuration(var2));
   }

   public static boolean hasDigSpeed(LivingEntity param0) {
      return param0.hasEffect(MobEffects.DIG_SPEED) || param0.hasEffect(MobEffects.CONDUIT_POWER);
   }

   public static int getDigSpeedAmplification(LivingEntity param0) {
      int var1 = 0;
      int var2 = 0;
      if (param0.hasEffect(MobEffects.DIG_SPEED)) {
         var1 = param0.getEffect(MobEffects.DIG_SPEED).getAmplifier();
      }

      if (param0.hasEffect(MobEffects.CONDUIT_POWER)) {
         var2 = param0.getEffect(MobEffects.CONDUIT_POWER).getAmplifier();
      }

      return Math.max(var1, var2);
   }

   public static boolean hasWaterBreathing(LivingEntity param0) {
      return param0.hasEffect(MobEffects.WATER_BREATHING) || param0.hasEffect(MobEffects.CONDUIT_POWER);
   }

   public static List<ServerPlayer> addEffectToPlayersAround(
      ServerLevel param0, @Nullable Entity param1, Vec3 param2, double param3, MobEffectInstance param5, int param6
   ) {
      MobEffect var7 = param5.getEffect();
      List<ServerPlayer> var8 = param0.getPlayers(
         l_param7 -> l_param7.gameMode.isSurvival()
            && (param1 == null || !param1.isAlliedTo(l_param7))
            && param2.closerThan(l_param7.position(), param3)
            && (!l_param7.hasEffect(var7) || l_param7.getEffect(var7).getAmplifier() < param5.getAmplifier() || l_param7.getEffect(var7).endsWithin(param6 - 1))
      );
      var8.forEach(l_param2 -> l_param2.addEffect(new MobEffectInstance(param5), param1));
      return var8;
   }
}
