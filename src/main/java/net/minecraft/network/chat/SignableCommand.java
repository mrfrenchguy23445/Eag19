package net.minecraft.network.chat;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.SignedArgument;

public record SignableCommand<S>(List<SignableCommand.Argument<S>> arguments) {
   public static <S> SignableCommand<S> of(ParseResults<S> param0) {
      String var1 = param0.getReader().getString();
      CommandContextBuilder<S> var2 = param0.getContext();
      CommandContextBuilder<S> var3 = var2;
      List<SignableCommand.Argument<S>> var4 = collectArguments(var1, var3);

      CommandContextBuilder<S> var5;
      while ((var5 = var3.getChild()) != null) {
         boolean var6 = var5.getRootNode() != var2.getRootNode();
         if (!var6) {
            break;
         }

         var4.addAll(collectArguments(var1, var5));
         var3 = var5;
      }

      return new SignableCommand<>(var4);
   }

   private static <S> List<SignableCommand.Argument<S>> collectArguments(String param0, CommandContextBuilder<S> param1) {
      List<SignableCommand.Argument<S>> var2 = new ArrayList<>();

      for (ParsedCommandNode<S> var4 : param1.getNodes()) {
         if (var4.getNode() instanceof ArgumentCommandNode<S, ?> var5 && var5.getType() instanceof SignedArgument) {
            ParsedArgument<S, ?> var6 = param1.getArguments().get(var5.getName());
            if (var6 != null) {
               String var7 = var6.getRange().get(param0);
               var2.add(new SignableCommand.Argument<>(var5, var7));
            }
         }
      }

      return var2;
   }

   public record Argument<S>(ArgumentCommandNode<S, ?> node, String value) {
      public String name() {
         return this.node.getName();
      }
   }
}
