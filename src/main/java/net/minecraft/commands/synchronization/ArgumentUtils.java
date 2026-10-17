package net.minecraft.commands.synchronization;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

public class ArgumentUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final byte NUMBER_FLAG_MIN = 1;
   private static final byte NUMBER_FLAG_MAX = 2;

   public static int createNumberFlags(boolean param0, boolean param1) {
      int var2 = 0;
      if (param0) {
         var2 |= 1;
      }

      if (param1) {
         var2 |= 2;
      }

      return var2;
   }

   public static boolean numberHasMin(byte param0) {
      return (param0 & 1) != 0;
   }

   public static boolean numberHasMax(byte param0) {
      return (param0 & 2) != 0;
   }

   private static <A extends ArgumentType<?>> void serializeCap(JsonObject param0, ArgumentTypeInfo.Template<A> param1) {
      serializeCap(param0, param1.type(), param1);
   }

   private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
      JsonObject param0, ArgumentTypeInfo<A, T> param1, ArgumentTypeInfo.Template<A> param2
   ) {
      param1.serializeToJson((T)param2, param0);
   }

   private static <T extends ArgumentType<?>> void serializeArgumentToJson(JsonObject param0, T param1) {
      ArgumentTypeInfo.Template<T> var2 = ArgumentTypeInfos.unpack(param1);
      param0.addProperty("type", "argument");
      param0.addProperty("parser", BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(var2.type()).toString());
      JsonObject var3 = new JsonObject();
      serializeCap(var3, var2);
      if (var3.size() > 0) {
         param0.add("properties", var3);
      }
   }

   public static <S> JsonObject serializeNodeToJson(CommandDispatcher<S> param0, CommandNode<S> param1) {
      JsonObject var2 = new JsonObject();
      if (param1 instanceof RootCommandNode) {
         var2.addProperty("type", "root");
      } else if (param1 instanceof LiteralCommandNode) {
         var2.addProperty("type", "literal");
      } else if (param1 instanceof ArgumentCommandNode<?, ?> var3) {
         serializeArgumentToJson(var2, var3.getType());
      } else {
         LOGGER.error("Could not serialize node {} ({})!", param1, param1.getClass());
         var2.addProperty("type", "unknown");
      }

      JsonObject var3 = new JsonObject();

      for (CommandNode<S> var5 : param1.getChildren()) {
         var3.add(var5.getName(), serializeNodeToJson(param0, var5));
      }

      if (var3.size() > 0) {
         var2.add("children", var3);
      }

      if (param1.getCommand() != null) {
         var2.addProperty("executable", true);
      }

      if (param1.getRedirect() != null) {
         Collection<String> var4 = param0.getPath(param1.getRedirect());
         if (!var4.isEmpty()) {
            JsonArray var5 = new JsonArray();

            for (String var7 : var4) {
               var5.add(var7);
            }

            var2.add("redirect", var5);
         }
      }

      return var2;
   }

   public static <T> Set<ArgumentType<?>> findUsedArgumentTypes(CommandNode<T> param0) {
      Set<CommandNode<T>> var1 = Sets.newIdentityHashSet();
      Set<ArgumentType<?>> var2 = Sets.newHashSet();
      findUsedArgumentTypes(param0, var2, var1);
      return var2;
   }

   private static <T> void findUsedArgumentTypes(CommandNode<T> param0, Set<ArgumentType<?>> param1, Set<CommandNode<T>> param2) {
      if (param2.add(param0)) {
         if (param0 instanceof ArgumentCommandNode<?, ?> var3) {
            param1.add(var3.getType());
         }

         param0.getChildren().forEach(l_param2 -> findUsedArgumentTypes((CommandNode<T>)l_param2, param1, param2));
         CommandNode<T> var3 = param0.getRedirect();
         if (var3 != null) {
            findUsedArgumentTypes(var3, param1, param2);
         }
      }
   }
}
