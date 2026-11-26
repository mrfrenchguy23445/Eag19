package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec2;

public class WorldBorderCommand {
   private static final SimpleCommandExceptionType ERROR_SAME_CENTER = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.center.failed")
   );
   private static final SimpleCommandExceptionType ERROR_SAME_SIZE = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.set.failed.nochange")
   );
   private static final SimpleCommandExceptionType ERROR_TOO_SMALL = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.set.failed.small")
   );
   private static final SimpleCommandExceptionType ERROR_TOO_BIG = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.set.failed.big", 5.999997E7F)
   );
   private static final SimpleCommandExceptionType ERROR_TOO_FAR_OUT = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.set.failed.far", 2.9999984E7)
   );
   private static final SimpleCommandExceptionType ERROR_SAME_WARNING_TIME = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.warning.time.failed")
   );
   private static final SimpleCommandExceptionType ERROR_SAME_WARNING_DISTANCE = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.warning.distance.failed")
   );
   private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_BUFFER = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.damage.buffer.failed")
   );
   private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_AMOUNT = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.damage.amount.failed")
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("worldborder")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("add")
                  .then(
                     Commands.argument("distance", DoubleArgumentType.doubleArg(-5.999997E7F, 5.999997E7F))
                        .executes(
                           l_param0 -> setSize(
                              l_param0.getSource(),
                              l_param0.getSource().getLevel().getWorldBorder().getSize() + DoubleArgumentType.getDouble(l_param0, "distance"),
                              0L
                           )
                        )
                        .then(
                           Commands.argument("time", IntegerArgumentType.integer(0))
                              .executes(
                                 l_param0 -> setSize(
                                    l_param0.getSource(),
                                    l_param0.getSource().getLevel().getWorldBorder().getSize() + DoubleArgumentType.getDouble(l_param0, "distance"),
                                    l_param0.getSource().getLevel().getWorldBorder().getLerpRemainingTime()
                                       + IntegerArgumentType.getInteger(l_param0, "time") * 1000L
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("set")
                  .then(
                     Commands.argument("distance", DoubleArgumentType.doubleArg(-5.999997E7F, 5.999997E7F))
                        .executes(l_param0 -> setSize(l_param0.getSource(), DoubleArgumentType.getDouble(l_param0, "distance"), 0L))
                        .then(
                           Commands.argument("time", IntegerArgumentType.integer(0))
                              .executes(
                                 l_param0 -> setSize(
                                    l_param0.getSource(),
                                    DoubleArgumentType.getDouble(l_param0, "distance"),
                                    IntegerArgumentType.getInteger(l_param0, "time") * 1000L
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("center")
                  .then(
                     Commands.argument("pos", Vec2Argument.vec2()).executes(l_param0 -> setCenter(l_param0.getSource(), Vec2Argument.getVec2(l_param0, "pos")))
                  )
            )
            .then(
               Commands.literal("damage")
                  .then(
                     Commands.literal("amount")
                        .then(
                           Commands.argument("damagePerBlock", FloatArgumentType.floatArg(0.0F))
                              .executes(l_param0 -> setDamageAmount(l_param0.getSource(), FloatArgumentType.getFloat(l_param0, "damagePerBlock")))
                        )
                  )
                  .then(
                     Commands.literal("buffer")
                        .then(
                           Commands.argument("distance", FloatArgumentType.floatArg(0.0F))
                              .executes(l_param0 -> setDamageBuffer(l_param0.getSource(), FloatArgumentType.getFloat(l_param0, "distance")))
                        )
                  )
            )
            .then(Commands.literal("get").executes(l_param0 -> getSize(l_param0.getSource())))
            .then(
               Commands.literal("warning")
                  .then(
                     Commands.literal("distance")
                        .then(
                           Commands.argument("distance", IntegerArgumentType.integer(0))
                              .executes(l_param0 -> setWarningDistance(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "distance")))
                        )
                  )
                  .then(
                     Commands.literal("time")
                        .then(
                           Commands.argument("time", IntegerArgumentType.integer(0))
                              .executes(l_param0 -> setWarningTime(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "time")))
                        )
                  )
            )
      );
   }

   private static int setDamageBuffer(CommandSourceStack param0, float param1) throws CommandSyntaxException {
      WorldBorder var2 = param0.getServer().overworld().getWorldBorder();
      if (var2.getDamageSafeZone() == param1) {
         throw ERROR_SAME_DAMAGE_BUFFER.create();
      }

      var2.setDamageSafeZone(param1);
      param0.sendSuccess(Component.translatable("commands.worldborder.damage.buffer.success", String.format(Locale.ROOT, "%.2f", param1)), true);
      return (int)param1;
   }

   private static int setDamageAmount(CommandSourceStack param0, float param1) throws CommandSyntaxException {
      WorldBorder var2 = param0.getServer().overworld().getWorldBorder();
      if (var2.getDamagePerBlock() == param1) {
         throw ERROR_SAME_DAMAGE_AMOUNT.create();
      }

      var2.setDamagePerBlock(param1);
      param0.sendSuccess(Component.translatable("commands.worldborder.damage.amount.success", String.format(Locale.ROOT, "%.2f", param1)), true);
      return (int)param1;
   }

   private static int setWarningTime(CommandSourceStack param0, int param1) throws CommandSyntaxException {
      WorldBorder var2 = param0.getServer().overworld().getWorldBorder();
      if (var2.getWarningTime() == param1) {
         throw ERROR_SAME_WARNING_TIME.create();
      }

      var2.setWarningTime(param1);
      param0.sendSuccess(Component.translatable("commands.worldborder.warning.time.success", param1), true);
      return param1;
   }

   private static int setWarningDistance(CommandSourceStack param0, int param1) throws CommandSyntaxException {
      WorldBorder var2 = param0.getServer().overworld().getWorldBorder();
      if (var2.getWarningBlocks() == param1) {
         throw ERROR_SAME_WARNING_DISTANCE.create();
      }

      var2.setWarningBlocks(param1);
      param0.sendSuccess(Component.translatable("commands.worldborder.warning.distance.success", param1), true);
      return param1;
   }

   private static int getSize(CommandSourceStack param0) {
      double var1 = param0.getServer().overworld().getWorldBorder().getSize();
      param0.sendSuccess(Component.translatable("commands.worldborder.get", String.format(Locale.ROOT, "%.0f", var1)), false);
      return Mth.floor(var1 + 0.5);
   }

   private static int setCenter(CommandSourceStack param0, Vec2 param1) throws CommandSyntaxException {
      WorldBorder var2 = param0.getServer().overworld().getWorldBorder();
      if (var2.getCenterX() == param1.x && var2.getCenterZ() == param1.y) {
         throw ERROR_SAME_CENTER.create();
      } else if (!(Math.abs(param1.x) > 2.9999984E7) && !(Math.abs(param1.y) > 2.9999984E7)) {
         var2.setCenter(param1.x, param1.y);
         param0.sendSuccess(
            Component.translatable(
               "commands.worldborder.center.success", String.format(Locale.ROOT, "%.2f", param1.x), String.format(Locale.ROOT, "%.2f", param1.y)
            ),
            true
         );
         return 0;
      } else {
         throw ERROR_TOO_FAR_OUT.create();
      }
   }

   private static int setSize(CommandSourceStack param0, double param1, long param3) throws CommandSyntaxException {
      WorldBorder var5 = param0.getServer().overworld().getWorldBorder();
      double var6 = var5.getSize();
      if (var6 == param1) {
         throw ERROR_SAME_SIZE.create();
      }

      if (param1 < 1.0) {
         throw ERROR_TOO_SMALL.create();
      }

      if (param1 > 5.999997E7F) {
         throw ERROR_TOO_BIG.create();
      }

      if (param3 > 0L) {
         var5.lerpSizeBetween(var6, param1, param3);
         if (param1 > var6) {
            param0.sendSuccess(
               Component.translatable("commands.worldborder.set.grow", String.format(Locale.ROOT, "%.1f", param1), Long.toString(param3 / 1000L)), true
            );
         } else {
            param0.sendSuccess(
               Component.translatable("commands.worldborder.set.shrink", String.format(Locale.ROOT, "%.1f", param1), Long.toString(param3 / 1000L)), true
            );
         }
      } else {
         var5.setSize(param1);
         param0.sendSuccess(Component.translatable("commands.worldborder.set.immediate", String.format(Locale.ROOT, "%.1f", param1)), true);
      }

      return (int)(param1 - var6);
   }
}
