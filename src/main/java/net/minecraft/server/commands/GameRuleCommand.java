package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameRules;

public class GameRuleCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      final LiteralArgumentBuilder<CommandSourceStack> var1 = Commands.literal("gamerule").requires(l_param0 -> l_param0.hasPermission(2));
      GameRules.visitGameRuleTypes(
         new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> l_param0, GameRules.Type<T> l_param1) {
               var1.then(
                  Commands.literal(l_param0.getId())
                     .executes(l2_param1 -> GameRuleCommand.queryRule(l2_param1.getSource(), l_param0))
                     .then(l_param1.createArgument("value").executes(l2_param1 -> GameRuleCommand.setRule(l2_param1, l_param0)))
               );
            }
         }
      );
      param0.register(var1);
   }

   static <T extends GameRules.Value<T>> int setRule(CommandContext<CommandSourceStack> param0, GameRules.Key<T> param1) {
      CommandSourceStack var2 = param0.getSource();
      T var3 = var2.getServer().getGameRules().getRule(param1);
      var3.setFromArgument(param0, "value");
      var2.sendSuccess(Component.translatable("commands.gamerule.set", param1.getId(), var3.toString()), true);
      return var3.getCommandResult();
   }

   static <T extends GameRules.Value<T>> int queryRule(CommandSourceStack param0, GameRules.Key<T> param1) {
      T var2 = param0.getServer().getGameRules().getRule(param1);
      param0.sendSuccess(Component.translatable("commands.gamerule.query", param1.getId(), var2.toString()), false);
      return var2.getCommandResult();
   }
}
