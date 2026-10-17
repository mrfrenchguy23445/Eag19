package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TeleportCommand {
   private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(
      Component.translatable("commands.teleport.invalidPosition")
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      LiteralCommandNode<CommandSourceStack> var1 = param0.register(
         Commands.literal("teleport")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("location", Vec3Argument.vec3())
                  .executes(
                     l_param0 -> teleportToPos(
                        l_param0.getSource(),
                        Collections.singleton(l_param0.getSource().getEntityOrException()),
                        l_param0.getSource().getLevel(),
                        Vec3Argument.getCoordinates(l_param0, "location"),
                        WorldCoordinates.current(),
                        null
                     )
                  )
            )
            .then(
               Commands.argument("destination", EntityArgument.entity())
                  .executes(
                     l_param0 -> teleportToEntity(
                        l_param0.getSource(),
                        Collections.singleton(l_param0.getSource().getEntityOrException()),
                        EntityArgument.getEntity(l_param0, "destination")
                     )
                  )
            )
            .then(
               Commands.argument("targets", EntityArgument.entities())
                  .then(
                     Commands.argument("location", Vec3Argument.vec3())
                        .executes(
                           l_param0 -> teleportToPos(
                              l_param0.getSource(),
                              EntityArgument.getEntities(l_param0, "targets"),
                              l_param0.getSource().getLevel(),
                              Vec3Argument.getCoordinates(l_param0, "location"),
                              null,
                              null
                           )
                        )
                        .then(
                           Commands.argument("rotation", RotationArgument.rotation())
                              .executes(
                                 l_param0 -> teleportToPos(
                                    l_param0.getSource(),
                                    EntityArgument.getEntities(l_param0, "targets"),
                                    l_param0.getSource().getLevel(),
                                    Vec3Argument.getCoordinates(l_param0, "location"),
                                    RotationArgument.getRotation(l_param0, "rotation"),
                                    null
                                 )
                              )
                        )
                        .then(
                           Commands.literal("facing")
                              .then(
                                 Commands.literal("entity")
                                    .then(
                                       Commands.argument("facingEntity", EntityArgument.entity())
                                          .executes(
                                             l_param0 -> teleportToPos(
                                                l_param0.getSource(),
                                                EntityArgument.getEntities(l_param0, "targets"),
                                                l_param0.getSource().getLevel(),
                                                Vec3Argument.getCoordinates(l_param0, "location"),
                                                null,
                                                new TeleportCommand.LookAt(EntityArgument.getEntity(l_param0, "facingEntity"), EntityAnchorArgument.Anchor.FEET)
                                             )
                                          )
                                          .then(
                                             Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                                .executes(
                                                   l_param0 -> teleportToPos(
                                                      l_param0.getSource(),
                                                      EntityArgument.getEntities(l_param0, "targets"),
                                                      l_param0.getSource().getLevel(),
                                                      Vec3Argument.getCoordinates(l_param0, "location"),
                                                      null,
                                                      new TeleportCommand.LookAt(
                                                         EntityArgument.getEntity(l_param0, "facingEntity"),
                                                         EntityAnchorArgument.getAnchor(l_param0, "facingAnchor")
                                                      )
                                                   )
                                                )
                                          )
                                    )
                              )
                              .then(
                                 Commands.argument("facingLocation", Vec3Argument.vec3())
                                    .executes(
                                       l_param0 -> teleportToPos(
                                          l_param0.getSource(),
                                          EntityArgument.getEntities(l_param0, "targets"),
                                          l_param0.getSource().getLevel(),
                                          Vec3Argument.getCoordinates(l_param0, "location"),
                                          null,
                                          new TeleportCommand.LookAt(Vec3Argument.getVec3(l_param0, "facingLocation"))
                                       )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.argument("destination", EntityArgument.entity())
                        .executes(
                           l_param0 -> teleportToEntity(
                              l_param0.getSource(), EntityArgument.getEntities(l_param0, "targets"), EntityArgument.getEntity(l_param0, "destination")
                           )
                        )
                  )
            )
      );
      param0.register(Commands.literal("tp").requires(l_param0 -> l_param0.hasPermission(2)).redirect(var1));
   }

   private static int teleportToEntity(CommandSourceStack param0, Collection<? extends Entity> param1, Entity param2) throws CommandSyntaxException {
      for (Entity var4 : param1) {
         performTeleport(
            param0,
            var4,
            (ServerLevel)param2.level,
            param2.getX(),
            param2.getY(),
            param2.getZ(),
            EnumSet.noneOf(RelativeMovement.class),
            param2.getYRot(),
            param2.getXRot(),
            null
         );
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.teleport.success.entity.single", param1.iterator().next().getDisplayName(), param2.getDisplayName()), true
         );
      } else {
         param0.sendSuccess(Component.translatable("commands.teleport.success.entity.multiple", param1.size(), param2.getDisplayName()), true);
      }

      return param1.size();
   }

   private static int teleportToPos(
      CommandSourceStack param0,
      Collection<? extends Entity> param1,
      ServerLevel param2,
      Coordinates param3,
      @Nullable Coordinates param4,
      @Nullable TeleportCommand.LookAt param5
   ) throws CommandSyntaxException {
      Vec3 var6 = param3.getPosition(param0);
      Vec2 var7 = param4 == null ? null : param4.getRotation(param0);
      Set<RelativeMovement> var8 = EnumSet.noneOf(RelativeMovement.class);
      if (param3.isXRelative()) {
         var8.add(RelativeMovement.X);
      }

      if (param3.isYRelative()) {
         var8.add(RelativeMovement.Y);
      }

      if (param3.isZRelative()) {
         var8.add(RelativeMovement.Z);
      }

      if (param4 == null) {
         var8.add(RelativeMovement.X_ROT);
         var8.add(RelativeMovement.Y_ROT);
      } else {
         if (param4.isXRelative()) {
            var8.add(RelativeMovement.X_ROT);
         }

         if (param4.isYRelative()) {
            var8.add(RelativeMovement.Y_ROT);
         }
      }

      for (Entity var10 : param1) {
         if (param4 == null) {
            performTeleport(param0, var10, param2, var6.x, var6.y, var6.z, var8, var10.getYRot(), var10.getXRot(), param5);
         } else {
            performTeleport(param0, var10, param2, var6.x, var6.y, var6.z, var8, var7.y, var7.x, param5);
         }
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable(
               "commands.teleport.success.location.single",
               param1.iterator().next().getDisplayName(),
               formatDouble(var6.x),
               formatDouble(var6.y),
               formatDouble(var6.z)
            ),
            true
         );
      } else {
         param0.sendSuccess(
            Component.translatable(
               "commands.teleport.success.location.multiple", param1.size(), formatDouble(var6.x), formatDouble(var6.y), formatDouble(var6.z)
            ),
            true
         );
      }

      return param1.size();
   }

   private static String formatDouble(double param0) {
      return String.format(Locale.ROOT, "%f", param0);
   }

   private static void performTeleport(
      CommandSourceStack param0,
      Entity param1,
      ServerLevel param2,
      double param3,
      double param5,
      double param7,
      Set<RelativeMovement> param9,
      float param10,
      float param11,
      @Nullable TeleportCommand.LookAt param12
   ) throws CommandSyntaxException {
      BlockPos var13 = BlockPos.containing(param3, param5, param7);
      if (!Level.isInSpawnableBounds(var13)) {
         throw INVALID_POSITION.create();
      }

      float var14 = Mth.wrapDegrees(param10);
      float var15 = Mth.wrapDegrees(param11);
      if (param1.teleportTo(param2, param3, param5, param7, param9, var14, var15)) {
         if (param12 != null) {
            param12.perform(param0, param1);
         }

         if (!(param1 instanceof LivingEntity var16 && var16.isFallFlying())) {
            param1.setDeltaMovement(param1.getDeltaMovement().multiply(1.0, 0.0, 1.0));
            param1.setOnGround(true);
         }

         if (param1 instanceof PathfinderMob var16x) {
            var16x.getNavigation().stop();
         }
      }
   }

   static class LookAt {
      private final Vec3 position;
      private final Entity entity;
      private final EntityAnchorArgument.Anchor anchor;

      public LookAt(Entity param0, EntityAnchorArgument.Anchor param1) {
         this.entity = param0;
         this.anchor = param1;
         this.position = param1.apply(param0);
      }

      public LookAt(Vec3 param0) {
         this.entity = null;
         this.position = param0;
         this.anchor = null;
      }

      public void perform(CommandSourceStack param0, Entity param1) {
         if (this.entity != null) {
            if (param1 instanceof ServerPlayer) {
               ((ServerPlayer)param1).lookAt(param0.getAnchor(), this.entity, this.anchor);
            } else {
               param1.lookAt(param0.getAnchor(), this.position);
            }
         } else {
            param1.lookAt(param0.getAnchor(), this.position);
         }
      }
   }
}
