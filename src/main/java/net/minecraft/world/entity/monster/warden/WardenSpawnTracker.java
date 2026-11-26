package net.minecraft.world.entity.monster.warden;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WardenSpawnTracker {
   public static final Codec<WardenSpawnTracker> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks_since_last_warning").orElse(0).forGetter(l_param0 -> l_param0.ticksSinceLastWarning),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("warning_level").orElse(0).forGetter(l_param0 -> l_param0.warningLevel),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").orElse(0).forGetter(l_param0 -> l_param0.cooldownTicks)
         )
         .apply(param0, WardenSpawnTracker::new)
   );
   public static final int MAX_WARNING_LEVEL = 4;
   private static final double PLAYER_SEARCH_RADIUS = 16.0;
   private static final int WARNING_CHECK_DIAMETER = 48;
   private static final int DECREASE_WARNING_LEVEL_EVERY_INTERVAL = 12000;
   private static final int WARNING_LEVEL_INCREASE_COOLDOWN = 200;
   private int ticksSinceLastWarning;
   private int warningLevel;
   private int cooldownTicks;

   public WardenSpawnTracker(int param0, int param1, int param2) {
      this.ticksSinceLastWarning = param0;
      this.warningLevel = param1;
      this.cooldownTicks = param2;
   }

   public void tick() {
      if (this.ticksSinceLastWarning >= 12000) {
         this.decreaseWarningLevel();
         this.ticksSinceLastWarning = 0;
      } else {
         this.ticksSinceLastWarning++;
      }

      if (this.cooldownTicks > 0) {
         this.cooldownTicks--;
      }
   }

   public void reset() {
      this.ticksSinceLastWarning = 0;
      this.warningLevel = 0;
      this.cooldownTicks = 0;
   }

   public static OptionalInt tryWarn(ServerLevel param0, BlockPos param1, ServerPlayer param2) {
      if (hasNearbyWarden(param0, param1)) {
         return OptionalInt.empty();
      }

      List<ServerPlayer> var3 = getNearbyPlayers(param0, param1);
      if (!var3.contains(param2)) {
         var3.add(param2);
      }

      if (var3.stream().anyMatch(l_param0 -> l_param0.getWardenSpawnTracker().map(WardenSpawnTracker::onCooldown).orElse(false))) {
         return OptionalInt.empty();
      } else {
         Optional<WardenSpawnTracker> var4 = var3.stream()
            .flatMap(l_param0 -> l_param0.getWardenSpawnTracker().stream())
            .max(Comparator.comparingInt(WardenSpawnTracker::getWarningLevel));
         if (var4.isPresent()) {
            WardenSpawnTracker var5 = var4.get();
            var5.increaseWarningLevel();
            var3.forEach(l_param1 -> l_param1.getWardenSpawnTracker().ifPresent(l2_param1 -> l2_param1.copyData(var5)));
            return OptionalInt.of(var5.warningLevel);
         } else {
            return OptionalInt.empty();
         }
      }
   }

   private boolean onCooldown() {
      return this.cooldownTicks > 0;
   }

   private static boolean hasNearbyWarden(ServerLevel param0, BlockPos param1) {
      AABB var2 = AABB.ofSize(Vec3.atCenterOf(param1), 48.0, 48.0, 48.0);
      return !param0.getEntitiesOfClass(Warden.class, var2).isEmpty();
   }

   private static List<ServerPlayer> getNearbyPlayers(ServerLevel param0, BlockPos param1) {
      Vec3 var2 = Vec3.atCenterOf(param1);
      Predicate<ServerPlayer> var3 = l_param1 -> l_param1.position().closerThan(var2, 16.0);
      return param0.getPlayers(var3.and(LivingEntity::isAlive).and(EntitySelector.NO_SPECTATORS));
   }

   private void increaseWarningLevel() {
      if (!this.onCooldown()) {
         this.ticksSinceLastWarning = 0;
         this.cooldownTicks = 200;
         this.setWarningLevel(this.getWarningLevel() + 1);
      }
   }

   private void decreaseWarningLevel() {
      this.setWarningLevel(this.getWarningLevel() - 1);
   }

   public void setWarningLevel(int param0) {
      this.warningLevel = Mth.clamp(param0, 0, 4);
   }

   public int getWarningLevel() {
      return this.warningLevel;
   }

   private void copyData(WardenSpawnTracker param0) {
      this.warningLevel = param0.warningLevel;
      this.cooldownTicks = param0.cooldownTicks;
      this.ticksSinceLastWarning = param0.ticksSinceLastWarning;
   }
}
