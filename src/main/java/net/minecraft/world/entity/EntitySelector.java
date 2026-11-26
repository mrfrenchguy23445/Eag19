package net.minecraft.world.entity;

import com.google.common.base.Predicates;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;

public final class EntitySelector {
   public static final Predicate<Entity> ENTITY_STILL_ALIVE = Entity::isAlive;
   public static final Predicate<Entity> LIVING_ENTITY_STILL_ALIVE = param0 -> param0.isAlive() && param0 instanceof LivingEntity;
   public static final Predicate<Entity> ENTITY_NOT_BEING_RIDDEN = param0 -> param0.isAlive() && !param0.isVehicle() && !param0.isPassenger();
   public static final Predicate<Entity> CONTAINER_ENTITY_SELECTOR = param0 -> param0 instanceof Container && param0.isAlive();
   public static final Predicate<Entity> NO_CREATIVE_OR_SPECTATOR = param0 -> !(param0 instanceof Player)
      || !param0.isSpectator() && !((Player)param0).isCreative();
   public static final Predicate<Entity> NO_SPECTATORS = param0 -> !param0.isSpectator();
   public static final Predicate<Entity> CAN_BE_COLLIDED_WITH = NO_SPECTATORS.and(Entity::canBeCollidedWith);

   private EntitySelector() {
   }

   public static Predicate<Entity> withinDistance(double param0, double param2, double param4, double param6) {
      double var8 = param6 * param6;
      return l_param8 -> l_param8 != null && l_param8.distanceToSqr(param0, param2, param4) <= var8;
   }

   public static Predicate<Entity> pushableBy(Entity param0) {
      Team var1 = param0.getTeam();
      Team.CollisionRule var2 = var1 == null ? Team.CollisionRule.ALWAYS : var1.getCollisionRule();
      return (Predicate<Entity>)(var2 == Team.CollisionRule.NEVER
         ? Predicates.<Entity>alwaysFalse()
         : NO_SPECTATORS.and(
            l_param3 -> {
               if (!l_param3.isPushable()) {
                  return false;
               }

               if (!param0.level.isClientSide || l_param3 instanceof Player && ((Player)l_param3).isLocalPlayer()) {
                  Team l_var4 = l_param3.getTeam();
                  Team.CollisionRule l_var5 = l_var4 == null ? Team.CollisionRule.ALWAYS : l_var4.getCollisionRule();
                  if (l_var5 == Team.CollisionRule.NEVER) {
                     return false;
                  }

                  boolean l_var6 = var1 != null && var1.isAlliedTo(l_var4);
                  return (var2 == Team.CollisionRule.PUSH_OWN_TEAM || l_var5 == Team.CollisionRule.PUSH_OWN_TEAM) && l_var6
                     ? false
                     : var2 != Team.CollisionRule.PUSH_OTHER_TEAMS && l_var5 != Team.CollisionRule.PUSH_OTHER_TEAMS || l_var6;
               } else {
                  return false;
               }
            }
         ));
   }

   public static Predicate<Entity> notRiding(Entity param0) {
      return l_param1 -> {
         while (l_param1.isPassenger()) {
            l_param1 = l_param1.getVehicle();
            if (l_param1 == param0) {
               return false;
            }
         }

         return true;
      };
   }

   public static class MobCanWearArmorEntitySelector implements Predicate<Entity> {
      private final ItemStack itemStack;

      public MobCanWearArmorEntitySelector(ItemStack param0) {
         this.itemStack = param0;
      }

      public boolean test(@Nullable Entity param0) {
         if (!param0.isAlive()) {
            return false;
         } else {
            return !(param0 instanceof LivingEntity var2) ? false : var2.canTakeItem(this.itemStack);
         }
      }
   }
}
