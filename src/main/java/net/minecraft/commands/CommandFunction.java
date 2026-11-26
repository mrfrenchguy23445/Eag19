package net.minecraft.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;

public class CommandFunction {
   private final CommandFunction.Entry[] entries;
   final ResourceLocation id;

   public CommandFunction(ResourceLocation param0, CommandFunction.Entry[] param1) {
      this.id = param0;
      this.entries = param1;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public CommandFunction.Entry[] getEntries() {
      return this.entries;
   }

   public static CommandFunction fromLines(
      ResourceLocation param0, CommandDispatcher<CommandSourceStack> param1, CommandSourceStack param2, List<String> param3
   ) {
      List<CommandFunction.Entry> var4 = Lists.newArrayListWithCapacity(param3.size());

      for (int var5 = 0; var5 < param3.size(); var5++) {
         int var6 = var5 + 1;
         String var7 = param3.get(var5).trim();
         StringReader var8 = new StringReader(var7);
         if (var8.canRead() && var8.peek() != '#') {
            if (var8.peek() == '/') {
               var8.skip();
               if (var8.peek() == '/') {
                  throw new IllegalArgumentException(
                     "Unknown or invalid command '" + var7 + "' on line " + var6 + " (if you intended to make a comment, use '#' not '//')"
                  );
               }

               String var9 = var8.readUnquotedString();
               throw new IllegalArgumentException(
                  "Unknown or invalid command '" + var7 + "' on line " + var6 + " (did you mean '" + var9 + "'? Do not use a preceding forwards slash.)"
               );
            }

            try {
               ParseResults<CommandSourceStack> var9 = param1.parse(var8, param2);
               if (var9.getReader().canRead()) {
                  throw Commands.getParseException(var9);
               }

               var4.add(new CommandFunction.CommandEntry(var9));
            } catch (CommandSyntaxException var9) {
               throw new IllegalArgumentException("Whilst parsing command on line " + var6 + ": " + var9.getMessage());
            }
         }
      }

      return new CommandFunction(param0, var4.toArray(new CommandFunction.Entry[0]));
   }

   public static class CacheableFunction {
      public static final CommandFunction.CacheableFunction NONE = new CommandFunction.CacheableFunction((ResourceLocation)null);
      @Nullable
      private final ResourceLocation id;
      private boolean resolved;
      private Optional<CommandFunction> function = Optional.empty();

      public CacheableFunction(@Nullable ResourceLocation param0) {
         this.id = param0;
      }

      public CacheableFunction(CommandFunction param0) {
         this.resolved = true;
         this.id = null;
         this.function = Optional.of(param0);
      }

      public Optional<CommandFunction> get(ServerFunctionManager param0) {
         if (!this.resolved) {
            if (this.id != null) {
               this.function = param0.get(this.id);
            }

            this.resolved = true;
         }

         return this.function;
      }

      @Nullable
      public ResourceLocation getId() {
         return this.function.<ResourceLocation>map(l_param0 -> l_param0.id).orElse(this.id);
      }
   }

   public static class CommandEntry implements CommandFunction.Entry {
      private final ParseResults<CommandSourceStack> parse;

      public CommandEntry(ParseResults<CommandSourceStack> param0) {
         this.parse = param0;
      }

      @Override
      public void execute(
         ServerFunctionManager param0,
         CommandSourceStack param1,
         Deque<ServerFunctionManager.QueuedCommand> param2,
         int param3,
         int param4,
         @Nullable ServerFunctionManager.TraceCallbacks param5
      ) throws CommandSyntaxException {
         if (param5 != null) {
            String var7 = this.parse.getReader().getString();
            param5.onCommand(param4, var7);
            int var8 = this.execute(param0, param1);
            param5.onReturn(param4, var7, var8);
         } else {
            this.execute(param0, param1);
         }
      }

      private int execute(ServerFunctionManager param0, CommandSourceStack param1) throws CommandSyntaxException {
         return param0.getDispatcher().execute(Commands.mapSource(this.parse, l_param1 -> param1));
      }

      @Override
      public String toString() {
         return this.parse.getReader().getString();
      }
   }

   @FunctionalInterface
   public interface Entry {
      void execute(
         ServerFunctionManager var1,
         CommandSourceStack var2,
         Deque<ServerFunctionManager.QueuedCommand> var3,
         int var4,
         int var5,
         @Nullable ServerFunctionManager.TraceCallbacks var6
      ) throws CommandSyntaxException;
   }

   public static class FunctionEntry implements CommandFunction.Entry {
      private final CommandFunction.CacheableFunction function;

      public FunctionEntry(CommandFunction param0) {
         this.function = new CommandFunction.CacheableFunction(param0);
      }

      @Override
      public void execute(
         ServerFunctionManager param0,
         CommandSourceStack param1,
         Deque<ServerFunctionManager.QueuedCommand> param2,
         int param3,
         int param4,
         @Nullable ServerFunctionManager.TraceCallbacks param5
      ) {
         Util.ifElse(this.function.get(param0), l_param5 -> {
            CommandFunction.Entry[] l_var6 = l_param5.getEntries();
            if (param5 != null) {
               param5.onCall(param4, l_param5.getId(), l_var6.length);
            }

            int l_var7 = param3 - param2.size();
            int l_var8 = Math.min(l_var6.length, l_var7);

            for (int l_var9 = l_var8 - 1; l_var9 >= 0; l_var9--) {
               param2.addFirst(new ServerFunctionManager.QueuedCommand(param1, param4 + 1, l_var6[l_var9]));
            }
         }, () -> {
            if (param5 != null) {
               param5.onCall(param4, this.function.getId(), -1);
            }
         });
      }

      @Override
      public String toString() {
         return "function " + this.function.getId();
      }
   }
}
