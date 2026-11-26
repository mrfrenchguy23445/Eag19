package net.minecraft.server.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.scores.Team;

public class SpreadPlayersCommand {
   private static final int MAX_ITERATION_COUNT = 10000;
   private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_TEAMS = new Dynamic4CommandExceptionType(
      (param0, param1, param2, param3) -> Component.translatable("commands.spreadplayers.failed.teams", param0, param1, param2, param3)
   );
   private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_ENTITIES = new Dynamic4CommandExceptionType(
      (param0, param1, param2, param3) -> Component.translatable("commands.spreadplayers.failed.entities", param0, param1, param2, param3)
   );
   private static final Dynamic2CommandExceptionType ERROR_INVALID_MAX_HEIGHT = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.spreadplayers.failed.invalid.height", param0, param1)
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("spreadplayers")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("center", Vec2Argument.vec2())
                  .then(
                     Commands.argument("spreadDistance", FloatArgumentType.floatArg(0.0F))
                        .then(
                           Commands.argument("maxRange", FloatArgumentType.floatArg(1.0F))
                              .then(
                                 Commands.argument("respectTeams", BoolArgumentType.bool())
                                    .then(
                                       Commands.argument("targets", EntityArgument.entities())
                                          .executes(
                                             l_param0 -> spreadPlayers(
                                                l_param0.getSource(),
                                                Vec2Argument.getVec2(l_param0, "center"),
                                                FloatArgumentType.getFloat(l_param0, "spreadDistance"),
                                                FloatArgumentType.getFloat(l_param0, "maxRange"),
                                                l_param0.getSource().getLevel().getMaxBuildHeight(),
                                                BoolArgumentType.getBool(l_param0, "respectTeams"),
                                                EntityArgument.getEntities(l_param0, "targets")
                                             )
                                          )
                                    )
                              )
                              .then(
                                 Commands.literal("under")
                                    .then(
                                       Commands.argument("maxHeight", IntegerArgumentType.integer())
                                          .then(
                                             Commands.argument("respectTeams", BoolArgumentType.bool())
                                                .then(
                                                   Commands.argument("targets", EntityArgument.entities())
                                                      .executes(
                                                         l_param0 -> spreadPlayers(
                                                            l_param0.getSource(),
                                                            Vec2Argument.getVec2(l_param0, "center"),
                                                            FloatArgumentType.getFloat(l_param0, "spreadDistance"),
                                                            FloatArgumentType.getFloat(l_param0, "maxRange"),
                                                            IntegerArgumentType.getInteger(l_param0, "maxHeight"),
                                                            BoolArgumentType.getBool(l_param0, "respectTeams"),
                                                            EntityArgument.getEntities(l_param0, "targets")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int spreadPlayers(
      CommandSourceStack param0, Vec2 param1, float param2, float param3, int param4, boolean param5, Collection<? extends Entity> param6
   ) throws CommandSyntaxException {
      ServerLevel var7 = param0.getLevel();
      int var8 = var7.getMinBuildHeight();
      if (param4 < var8) {
         throw ERROR_INVALID_MAX_HEIGHT.create(param4, var8);
      }

      RandomSource var9 = RandomSource.create();
      double var10 = param1.x - param3;
      double var12 = param1.y - param3;
      double var14 = param1.x + param3;
      double var16 = param1.y + param3;
      SpreadPlayersCommand.Position[] var18 = createInitialPositions(var9, param5 ? getNumberOfTeams(param6) : param6.size(), var10, var12, var14, var16);
      spreadPositions(param1, param2, var7, var9, var10, var12, var14, var16, param4, var18, param5);
      double var19 = setPlayerPositions(param6, var7, var18, param4, param5);
      param0.sendSuccess(
         Component.translatable(
            "commands.spreadplayers.success." + (param5 ? "teams" : "entities"), var18.length, param1.x, param1.y, String.format(Locale.ROOT, "%.2f", var19)
         ),
         true
      );
      return var18.length;
   }

   private static int getNumberOfTeams(Collection<? extends Entity> param0) {
      Set<Team> var1 = Sets.newHashSet();

      for (Entity var3 : param0) {
         if (var3 instanceof Player) {
            var1.add(var3.getTeam());
         } else {
            var1.add(null);
         }
      }

      return var1.size();
   }

   private static void spreadPositions(
      Vec2 param0,
      double param1,
      ServerLevel param3,
      RandomSource param4,
      double param5,
      double param7,
      double param9,
      double param11,
      int param13,
      SpreadPlayersCommand.Position[] param14,
      boolean param15
   ) throws CommandSyntaxException {
      boolean var16 = true;
      double var18 = Float.MAX_VALUE;

      int var17;
      for (var17 = 0; var17 < 10000 && var16; var17++) {
         var16 = false;
         var18 = Float.MAX_VALUE;

         for (int var20 = 0; var20 < param14.length; var20++) {
            SpreadPlayersCommand.Position var21 = param14[var20];
            int var22 = 0;
            SpreadPlayersCommand.Position var23 = new SpreadPlayersCommand.Position();

            for (int var24 = 0; var24 < param14.length; var24++) {
               if (var20 != var24) {
                  SpreadPlayersCommand.Position var25 = param14[var24];
                  double var26 = var21.dist(var25);
                  var18 = Math.min(var26, var18);
                  if (var26 < param1) {
                     var22++;
                     var23.x = var23.x + (var25.x - var21.x);
                     var23.z = var23.z + (var25.z - var21.z);
                  }
               }
            }

            if (var22 > 0) {
               var23.x /= var22;
               var23.z /= var22;
               double var24 = var23.getLength();
               if (var24 > 0.0) {
                  var23.normalize();
                  var21.moveAway(var23);
               } else {
                  var21.randomize(param4, param5, param7, param9, param11);
               }

               var16 = true;
            }

            if (var21.clamp(param5, param7, param9, param11)) {
               var16 = true;
            }
         }

         if (!var16) {
            for (SpreadPlayersCommand.Position var23 : param14) {
               if (!var23.isSafe(param3, param13)) {
                  var23.randomize(param4, param5, param7, param9, param11);
                  var16 = true;
               }
            }
         }
      }

      if (var18 == Float.MAX_VALUE) {
         var18 = 0.0;
      }

      if (var17 >= 10000) {
         if (param15) {
            throw ERROR_FAILED_TO_SPREAD_TEAMS.create(param14.length, param0.x, param0.y, String.format(Locale.ROOT, "%.2f", var18));
         } else {
            throw ERROR_FAILED_TO_SPREAD_ENTITIES.create(param14.length, param0.x, param0.y, String.format(Locale.ROOT, "%.2f", var18));
         }
      }
   }

   private static double setPlayerPositions(
      Collection<? extends Entity> param0, ServerLevel param1, SpreadPlayersCommand.Position[] param2, int param3, boolean param4
   ) {
      double var5 = 0.0;
      int var7 = 0;
      Map<Team, SpreadPlayersCommand.Position> var8 = Maps.newHashMap();

      for (Entity var10 : param0) {
         SpreadPlayersCommand.Position var11;
         if (param4) {
            Team var12 = var10 instanceof Player ? var10.getTeam() : null;
            if (!var8.containsKey(var12)) {
               var8.put(var12, param2[var7++]);
            }

            var11 = var8.get(var12);
         } else {
            var11 = param2[var7++];
         }

         var10.teleportTo(
            param1, Mth.floor(var11.x) + 0.5, var11.getSpawnY(param1, param3), Mth.floor(var11.z) + 0.5, Set.of(), var10.getYRot(), var10.getXRot()
         );
         double var12 = Double.MAX_VALUE;

         for (SpreadPlayersCommand.Position var17 : param2) {
            if (var11 != var17) {
               double var18 = var11.dist(var17);
               var12 = Math.min(var18, var12);
            }
         }

         var5 += var12;
      }

      return param0.size() < 2 ? 0.0 : var5 / param0.size();
   }

   private static SpreadPlayersCommand.Position[] createInitialPositions(
      RandomSource param0, int param1, double param2, double param4, double param6, double param8
   ) {
      SpreadPlayersCommand.Position[] var10 = new SpreadPlayersCommand.Position[param1];

      for (int var11 = 0; var11 < var10.length; var11++) {
         SpreadPlayersCommand.Position var12 = new SpreadPlayersCommand.Position();
         var12.randomize(param0, param2, param4, param6, param8);
         var10[var11] = var12;
      }

      return var10;
   }

   static class Position {
      double x;
      double z;

      double dist(SpreadPlayersCommand.Position param0) {
         double var2 = this.x - param0.x;
         double var4 = this.z - param0.z;
         return Math.sqrt(var2 * var2 + var4 * var4);
      }

      void normalize() {
         double var1 = this.getLength();
         this.x /= var1;
         this.z /= var1;
      }

      double getLength() {
         return Math.sqrt(this.x * this.x + this.z * this.z);
      }

      public void moveAway(SpreadPlayersCommand.Position param0) {
         this.x = this.x - param0.x;
         this.z = this.z - param0.z;
      }

      public boolean clamp(double param0, double param2, double param4, double param6) {
         boolean var9 = false;
         if (this.x < param0) {
            this.x = param0;
            var9 = true;
         } else if (this.x > param4) {
            this.x = param4;
            var9 = true;
         }

         if (this.z < param2) {
            this.z = param2;
            var9 = true;
         } else if (this.z > param6) {
            this.z = param6;
            var9 = true;
         }

         return var9;
      }

      public int getSpawnY(BlockGetter param0, int param1) {
         BlockPos.MutableBlockPos var3 = new BlockPos.MutableBlockPos(this.x, param1 + 1, this.z);
         boolean var4 = param0.getBlockState(var3).isAir();
         var3.move(Direction.DOWN);
         boolean var5 = param0.getBlockState(var3).isAir();

         while (var3.getY() > param0.getMinBuildHeight()) {
            var3.move(Direction.DOWN);
            boolean var6 = param0.getBlockState(var3).isAir();
            if (!var6 && var5 && var4) {
               return var3.getY() + 1;
            }

            var4 = var5;
            var5 = var6;
         }

         return param1 + 1;
      }

      public boolean isSafe(BlockGetter param0, int param1) {
         BlockPos var3 = BlockPos.containing(this.x, this.getSpawnY(param0, param1) - 1, this.z);
         BlockState var4 = param0.getBlockState(var3);
         Material var5 = var4.getMaterial();
         return var3.getY() < param1 && !var5.isLiquid() && var5 != Material.FIRE;
      }

      public void randomize(RandomSource param0, double param1, double param3, double param5, double param7) {
         this.x = Mth.nextDouble(param0, param1, param5);
         this.z = Mth.nextDouble(param0, param3, param7);
      }
   }
}
