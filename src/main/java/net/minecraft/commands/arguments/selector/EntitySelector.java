package net.minecraft.commands.arguments.selector;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntitySelector {
   public static final int INFINITE = Integer.MAX_VALUE;
   public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_ARBITRARY = (param0, param1) -> {};
   private static final EntityTypeTest<Entity, ?> ANY_TYPE = new EntityTypeTest<Entity, Entity>() {
      public Entity tryCast(Entity param0) {
         return param0;
      }

      @Override
      public Class<? extends Entity> getBaseClass() {
         return Entity.class;
      }
   };
   private final int maxResults;
   private final boolean includesEntities;
   private final boolean worldLimited;
   private final Predicate<Entity> predicate;
   private final MinMaxBounds.Doubles range;
   private final Function<Vec3, Vec3> position;
   @Nullable
   private final AABB aabb;
   private final BiConsumer<Vec3, List<? extends Entity>> order;
   private final boolean currentEntity;
   @Nullable
   private final String playerName;
   @Nullable
   private final UUID entityUUID;
   private final EntityTypeTest<Entity, ?> type;
   private final boolean usesSelector;

   public EntitySelector(
      int param0,
      boolean param1,
      boolean param2,
      Predicate<Entity> param3,
      MinMaxBounds.Doubles param4,
      Function<Vec3, Vec3> param5,
      @Nullable AABB param6,
      BiConsumer<Vec3, List<? extends Entity>> param7,
      boolean param8,
      @Nullable String param9,
      @Nullable UUID param10,
      @Nullable EntityType<?> param11,
      boolean param12
   ) {
      this.maxResults = param0;
      this.includesEntities = param1;
      this.worldLimited = param2;
      this.predicate = param3;
      this.range = param4;
      this.position = param5;
      this.aabb = param6;
      this.order = param7;
      this.currentEntity = param8;
      this.playerName = param9;
      this.entityUUID = param10;
      this.type = param11 == null ? ANY_TYPE : param11;
      this.usesSelector = param12;
   }

   public int getMaxResults() {
      return this.maxResults;
   }

   public boolean includesEntities() {
      return this.includesEntities;
   }

   public boolean isSelfSelector() {
      return this.currentEntity;
   }

   public boolean isWorldLimited() {
      return this.worldLimited;
   }

   public boolean usesSelector() {
      return this.usesSelector;
   }

   private void checkPermissions(CommandSourceStack param0) throws CommandSyntaxException {
      if (this.usesSelector && !param0.hasPermission(2)) {
         throw EntityArgument.ERROR_SELECTORS_NOT_ALLOWED.create();
      }
   }

   public Entity findSingleEntity(CommandSourceStack param0) throws CommandSyntaxException {
      this.checkPermissions(param0);
      List<? extends Entity> var2 = this.findEntities(param0);
      if (var2.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else if (var2.size() > 1) {
         throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
      } else {
         return var2.get(0);
      }
   }

   public List<? extends Entity> findEntities(CommandSourceStack param0) throws CommandSyntaxException {
      return this.findEntitiesRaw(param0).stream().filter(l_param1 -> l_param1.getType().isEnabled(param0.enabledFeatures())).toList();
   }

   private List<? extends Entity> findEntitiesRaw(CommandSourceStack param0) throws CommandSyntaxException {
      this.checkPermissions(param0);
      if (!this.includesEntities) {
         return this.findPlayers(param0);
      }

      if (this.playerName != null) {
         ServerPlayer var2 = param0.getServer().getPlayerList().getPlayerByName(this.playerName);
         return var2 == null ? Collections.emptyList() : Lists.newArrayList(var2);
      }

      if (this.entityUUID != null) {
         for (ServerLevel var3 : param0.getServer().getAllLevels()) {
            Entity var4 = var3.getEntity(this.entityUUID);
            if (var4 != null) {
               return Lists.newArrayList(var4);
            }
         }

         return Collections.emptyList();
      } else {
         Vec3 var2 = this.position.apply(param0.getPosition());
         Predicate<Entity> var3 = this.getPredicate(var2);
         if (this.currentEntity) {
            return param0.getEntity() != null && var3.test(param0.getEntity()) ? Lists.newArrayList(param0.getEntity()) : Collections.emptyList();
         }

         List<Entity> var4 = Lists.newArrayList();
         if (this.isWorldLimited()) {
            this.addEntities(var4, param0.getLevel(), var2, var3);
         } else {
            for (ServerLevel var6 : param0.getServer().getAllLevels()) {
               this.addEntities(var4, var6, var2, var3);
            }
         }

         return this.sortAndLimit(var2, var4);
      }
   }

   private void addEntities(List<Entity> param0, ServerLevel param1, Vec3 param2, Predicate<Entity> param3) {
      int var5 = this.getResultLimit();
      if (param0.size() < var5) {
         if (this.aabb != null) {
            param1.getEntities(this.type, this.aabb.move(param2), param3, param0, var5);
         } else {
            param1.getEntities(this.type, param3, param0, var5);
         }
      }
   }

   private int getResultLimit() {
      return this.order == ORDER_ARBITRARY ? this.maxResults : Integer.MAX_VALUE;
   }

   public ServerPlayer findSinglePlayer(CommandSourceStack param0) throws CommandSyntaxException {
      this.checkPermissions(param0);
      List<ServerPlayer> var2 = this.findPlayers(param0);
      if (var2.size() != 1) {
         throw EntityArgument.NO_PLAYERS_FOUND.create();
      } else {
         return var2.get(0);
      }
   }

   public List<ServerPlayer> findPlayers(CommandSourceStack param0) throws CommandSyntaxException {
      this.checkPermissions(param0);
      if (this.playerName != null) {
         ServerPlayer var2 = param0.getServer().getPlayerList().getPlayerByName(this.playerName);
         return var2 == null ? Collections.emptyList() : Lists.newArrayList(var2);
      }

      if (this.entityUUID != null) {
         ServerPlayer var2 = param0.getServer().getPlayerList().getPlayer(this.entityUUID);
         return var2 == null ? Collections.emptyList() : Lists.newArrayList(var2);
      }

      Vec3 var2 = this.position.apply(param0.getPosition());
      Predicate<Entity> var3 = this.getPredicate(var2);
      if (this.currentEntity) {
         if (param0.getEntity() instanceof ServerPlayer) {
            ServerPlayer var4 = (ServerPlayer)param0.getEntity();
            if (var3.test(var4)) {
               return Lists.newArrayList(var4);
            }
         }

         return Collections.emptyList();
      } else {
         int var5 = this.getResultLimit();
         List<ServerPlayer> var4;
         if (this.isWorldLimited()) {
            var4 = param0.getLevel().getPlayers(var3, var5);
         } else {
            var4 = Lists.newArrayList();

            for (ServerPlayer var7 : param0.getServer().getPlayerList().getPlayers()) {
               if (var3.test(var7)) {
                  var4.add(var7);
                  if (var4.size() >= var5) {
                     return var4;
                  }
               }
            }
         }

         return this.sortAndLimit(var2, var4);
      }
   }

   private Predicate<Entity> getPredicate(Vec3 param0) {
      Predicate<Entity> var2 = this.predicate;
      if (this.aabb != null) {
         AABB var3 = this.aabb.move(param0);
         var2 = var2.and(l_param1 -> var3.intersects(l_param1.getBoundingBox()));
      }

      if (!this.range.isAny()) {
         var2 = var2.and(l_param1 -> this.range.matchesSqr(l_param1.distanceToSqr(param0)));
      }

      return var2;
   }

   private <T extends Entity> List<T> sortAndLimit(Vec3 param0, List<T> param1) {
      if (param1.size() > 1) {
         this.order.accept(param0, param1);
      }

      return param1.subList(0, Math.min(this.maxResults, param1.size()));
   }

   public static Component joinNames(List<? extends Entity> param0) {
      return ComponentUtils.formatList(param0, Entity::getDisplayName);
   }
}
