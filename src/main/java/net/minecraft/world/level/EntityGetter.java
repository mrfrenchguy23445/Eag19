package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface EntityGetter {
   List<Entity> getEntities(@Nullable Entity var1, AABB var2, Predicate<? super Entity> var3);

   <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> var1, AABB var2, Predicate<? super T> var3);

   default <T extends Entity> List<T> getEntitiesOfClass(Class<T> param0, AABB param1, Predicate<? super T> param2) {
      return this.getEntities(EntityTypeTest.forClass(param0), param1, param2);
   }

   List<? extends Player> players();

   default List<Entity> getEntities(@Nullable Entity param0, AABB param1) {
      return this.getEntities(param0, param1, EntitySelector.NO_SPECTATORS);
   }

   default boolean isUnobstructed(@Nullable Entity param0, VoxelShape param1) {
      if (param1.isEmpty()) {
         return true;
      }

      for (Entity var4 : this.getEntities(param0, param1.bounds())) {
         if (!var4.isRemoved()
            && var4.blocksBuilding
            && (param0 == null || !var4.isPassengerOfSameVehicle(param0))
            && Shapes.joinIsNotEmpty(param1, Shapes.create(var4.getBoundingBox()), BooleanOp.AND)) {
            return false;
         }
      }

      return true;
   }

   default <T extends Entity> List<T> getEntitiesOfClass(Class<T> param0, AABB param1) {
      return this.getEntitiesOfClass(param0, param1, EntitySelector.NO_SPECTATORS);
   }

   default List<VoxelShape> getEntityCollisions(@Nullable Entity param0, AABB param1) {
      if (param1.getSize() < 1.0E-7) {
         return List.of();
      }

      Predicate<Entity> var3 = param0 == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(param0::canCollideWith);
      List<Entity> var4 = this.getEntities(param0, param1.inflate(1.0E-7), var3);
      if (var4.isEmpty()) {
         return List.of();
      }

      Builder<VoxelShape> var5 = ImmutableList.builderWithExpectedSize(var4.size());

      for (Entity var7 : var4) {
         var5.add(Shapes.create(var7.getBoundingBox()));
      }

      return var5.build();
   }

   @Nullable
   default Player getNearestPlayer(double param0, double param2, double param4, double param6, @Nullable Predicate<Entity> param8) {
      double var10 = -1.0;
      Player var12 = null;

      for (Player var14 : this.players()) {
         if (param8 == null || param8.test(var14)) {
            double var15 = var14.distanceToSqr(param0, param2, param4);
            if ((param6 < 0.0 || var15 < param6 * param6) && (var10 == -1.0 || var15 < var10)) {
               var10 = var15;
               var12 = var14;
            }
         }
      }

      return var12;
   }

   @Nullable
   default Player getNearestPlayer(Entity param0, double param1) {
      return this.getNearestPlayer(param0.getX(), param0.getY(), param0.getZ(), param1, false);
   }

   @Nullable
   default Player getNearestPlayer(double param0, double param2, double param4, double param6, boolean param8) {
      Predicate<Entity> var10 = param8 ? EntitySelector.NO_CREATIVE_OR_SPECTATOR : EntitySelector.NO_SPECTATORS;
      return this.getNearestPlayer(param0, param2, param4, param6, var10);
   }

   default boolean hasNearbyAlivePlayer(double param0, double param2, double param4, double param6) {
      for (Player var10 : this.players()) {
         if (EntitySelector.NO_SPECTATORS.test(var10) && EntitySelector.LIVING_ENTITY_STILL_ALIVE.test(var10)) {
            double var11 = var10.distanceToSqr(param0, param2, param4);
            if (param6 < 0.0 || var11 < param6 * param6) {
               return true;
            }
         }
      }

      return false;
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions param0, LivingEntity param1) {
      return this.getNearestEntity(this.players(), param0, param1, param1.getX(), param1.getY(), param1.getZ());
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions param0, LivingEntity param1, double param2, double param4, double param6) {
      return this.getNearestEntity(this.players(), param0, param1, param2, param4, param6);
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions param0, double param1, double param3, double param5) {
      return this.getNearestEntity(this.players(), param0, null, param1, param3, param5);
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      Class<? extends T> param0, TargetingConditions param1, @Nullable LivingEntity param2, double param3, double param5, double param7, AABB param9
   ) {
      return this.getNearestEntity(this.getEntitiesOfClass(param0, param9, l_param0 -> true), param1, param2, param3, param5, param7);
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      List<? extends T> param0, TargetingConditions param1, @Nullable LivingEntity param2, double param3, double param5, double param7
   ) {
      double var10 = -1.0;
      T var12 = null;

      for (T var14 : param0) {
         if (param1.test(param2, var14)) {
            double var15 = var14.distanceToSqr(param3, param5, param7);
            if (var10 == -1.0 || var15 < var10) {
               var10 = var15;
               var12 = var14;
            }
         }
      }

      return var12;
   }

   default List<Player> getNearbyPlayers(TargetingConditions param0, LivingEntity param1, AABB param2) {
      List<Player> var4 = Lists.newArrayList();

      for (Player var6 : this.players()) {
         if (param2.contains(var6.getX(), var6.getY(), var6.getZ()) && param0.test(param1, var6)) {
            var4.add(var6);
         }
      }

      return var4;
   }

   default <T extends LivingEntity> List<T> getNearbyEntities(Class<T> param0, TargetingConditions param1, LivingEntity param2, AABB param3) {
      List<T> var5 = this.getEntitiesOfClass(param0, param3, l_param0 -> true);
      List<T> var6 = Lists.newArrayList();

      for (T var8 : var5) {
         if (param1.test(param2, var8)) {
            var6.add(var8);
         }
      }

      return var6;
   }

   @Nullable
   default Player getPlayerByUUID(UUID param0) {
      for (int var2 = 0; var2 < this.players().size(); var2++) {
         Player var3 = this.players().get(var2);
         if (param0.equals(var3.getUUID())) {
            return var3;
         }
      }

      return null;
   }
}
