package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.timers.FunctionCallback;
import net.minecraft.world.level.timers.FunctionTagCallback;
import net.minecraft.world.level.timers.TimerQueue;

public class ScheduleCommand {
   private static final SimpleCommandExceptionType ERROR_SAME_TICK = new SimpleCommandExceptionType(Component.translatable("commands.schedule.same_tick"));
   private static final DynamicCommandExceptionType ERROR_CANT_REMOVE = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.schedule.cleared.failure", param0)
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_SCHEDULE = (param0, param1) -> SharedSuggestionProvider.suggest(
      param0.getSource().getServer().getWorldData().overworldData().getScheduledEvents().getEventsIds(), param1
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("schedule")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("function")
                  .then(
                     Commands.argument("function", FunctionArgument.functions())
                        .suggests(FunctionCommand.SUGGEST_FUNCTION)
                        .then(
                           Commands.argument("time", TimeArgument.time())
                              .executes(
                                 l_param0 -> schedule(
                                    l_param0.getSource(),
                                    FunctionArgument.getFunctionOrTag(l_param0, "function"),
                                    IntegerArgumentType.getInteger(l_param0, "time"),
                                    true
                                 )
                              )
                              .then(
                                 Commands.literal("append")
                                    .executes(
                                       l_param0 -> schedule(
                                          l_param0.getSource(),
                                          FunctionArgument.getFunctionOrTag(l_param0, "function"),
                                          IntegerArgumentType.getInteger(l_param0, "time"),
                                          false
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("replace")
                                    .executes(
                                       l_param0 -> schedule(
                                          l_param0.getSource(),
                                          FunctionArgument.getFunctionOrTag(l_param0, "function"),
                                          IntegerArgumentType.getInteger(l_param0, "time"),
                                          true
                                       )
                                    )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("clear")
                  .then(
                     Commands.argument("function", StringArgumentType.greedyString())
                        .suggests(SUGGEST_SCHEDULE)
                        .executes(l_param0 -> remove(l_param0.getSource(), StringArgumentType.getString(l_param0, "function")))
                  )
            )
      );
   }

   private static int schedule(
      CommandSourceStack param0, Pair<ResourceLocation, Either<CommandFunction, Collection<CommandFunction>>> param1, int param2, boolean param3
   ) throws CommandSyntaxException {
      if (param2 == 0) {
         throw ERROR_SAME_TICK.create();
      }

      long var4 = param0.getLevel().getGameTime() + param2;
      ResourceLocation var6 = param1.getFirst();
      TimerQueue<MinecraftServer> var7 = param0.getServer().getWorldData().overworldData().getScheduledEvents();
      param1.getSecond().ifLeft(l_param7 -> {
         String l_var8 = var6.toString();
         if (param3) {
            var7.remove(l_var8);
         }

         var7.schedule(l_var8, var4, new FunctionCallback(var6));
         param0.sendSuccess(Component.translatable("commands.schedule.created.function", var6, param2, var4), true);
      }).ifRight(l_param7 -> {
         String l_var8 = "#" + var6;
         if (param3) {
            var7.remove(l_var8);
         }

         var7.schedule(l_var8, var4, new FunctionTagCallback(var6));
         param0.sendSuccess(Component.translatable("commands.schedule.created.tag", var6, param2, var4), true);
      });
      return Math.floorMod(var4, Integer.MAX_VALUE);
   }

   private static int remove(CommandSourceStack param0, String param1) throws CommandSyntaxException {
      int var2 = param0.getServer().getWorldData().overworldData().getScheduledEvents().remove(param1);
      if (var2 == 0) {
         throw ERROR_CANT_REMOVE.create(param1);
      }

      param0.sendSuccess(Component.translatable("commands.schedule.cleared.success", var2, param1), true);
      return var2;
   }
}
