package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;

public class MessageArgument implements SignedArgument<MessageArgument.Message> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");

   public static MessageArgument message() {
      return new MessageArgument();
   }

   public static Component getMessage(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      MessageArgument.Message var2 = param0.getArgument(param1, MessageArgument.Message.class);
      return var2.resolveComponent(param0.getSource());
   }

   public static void resolveChatMessage(CommandContext<CommandSourceStack> param0, String param1, Consumer<PlayerChatMessage> param2) throws CommandSyntaxException {
      MessageArgument.Message var3 = param0.getArgument(param1, MessageArgument.Message.class);
      CommandSourceStack var4 = param0.getSource();
      Component var5 = var3.resolveComponent(var4);
      CommandSigningContext var6 = var4.getSigningContext();
      PlayerChatMessage var7 = var6.getArgument(param1);
      if (var7 != null) {
         resolveSignedMessage(param2, var4, var7.withUnsignedContent(var5));
      } else {
         resolveDisguisedMessage(param2, var4, PlayerChatMessage.system(var3.text).withUnsignedContent(var5));
      }
   }

   private static void resolveSignedMessage(Consumer<PlayerChatMessage> param0, CommandSourceStack param1, PlayerChatMessage param2) {
      MinecraftServer var3 = param1.getServer();
      CompletableFuture<FilteredText> var4 = filterPlainText(param1, param2);
      CompletableFuture<Component> var5 = var3.getChatDecorator().decorate(param1.getPlayer(), param2.decoratedContent());
      param1.getChatMessageChainer().append(l_param4 -> CompletableFuture.allOf(var4, var5).thenAcceptAsync(l2_param4 -> {
         PlayerChatMessage l2_var5 = param2.withUnsignedContent(var5.join()).filter(var4.join().mask());
         param0.accept(l2_var5);
      }, l_param4));
   }

   private static void resolveDisguisedMessage(Consumer<PlayerChatMessage> param0, CommandSourceStack param1, PlayerChatMessage param2) {
      MinecraftServer var3 = param1.getServer();
      CompletableFuture<Component> var4 = var3.getChatDecorator().decorate(param1.getPlayer(), param2.decoratedContent());
      param1.getChatMessageChainer().append(l_param3 -> var4.thenAcceptAsync(l2_param2 -> param0.accept(param2.withUnsignedContent(l2_param2)), l_param3));
   }

   private static CompletableFuture<FilteredText> filterPlainText(CommandSourceStack param0, PlayerChatMessage param1) {
      ServerPlayer var2 = param0.getPlayer();
      return var2 != null && param1.hasSignatureFrom(var2.getUUID())
         ? var2.getTextFilter().processStreamMessage(param1.signedContent())
         : CompletableFuture.completedFuture(FilteredText.passThrough(param1.signedContent()));
   }

   public MessageArgument.Message parse(StringReader param0) throws CommandSyntaxException {
      return MessageArgument.Message.parseText(param0, true);
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Message {
      final String text;
      private final MessageArgument.Part[] parts;

      public Message(String param0, MessageArgument.Part[] param1) {
         this.text = param0;
         this.parts = param1;
      }

      public String getText() {
         return this.text;
      }

      public MessageArgument.Part[] getParts() {
         return this.parts;
      }

      Component resolveComponent(CommandSourceStack param0) throws CommandSyntaxException {
         return this.toComponent(param0, param0.hasPermission(2));
      }

      public Component toComponent(CommandSourceStack param0, boolean param1) throws CommandSyntaxException {
         if (this.parts.length != 0 && param1) {
            MutableComponent var3 = Component.literal(this.text.substring(0, this.parts[0].getStart()));
            int var4 = this.parts[0].getStart();

            for (MessageArgument.Part var8 : this.parts) {
               Component var9 = var8.toComponent(param0);
               if (var4 < var8.getStart()) {
                  var3.append(this.text.substring(var4, var8.getStart()));
               }

               if (var9 != null) {
                  var3.append(var9);
               }

               var4 = var8.getEnd();
            }

            if (var4 < this.text.length()) {
               var3.append(this.text.substring(var4));
            }

            return var3;
         } else {
            return Component.literal(this.text);
         }
      }

      public static MessageArgument.Message parseText(StringReader param0, boolean param1) throws CommandSyntaxException {
         String var2 = param0.getString().substring(param0.getCursor(), param0.getTotalLength());
         if (!param1) {
            param0.setCursor(param0.getTotalLength());
            return new MessageArgument.Message(var2, new MessageArgument.Part[0]);
         }

         List<MessageArgument.Part> var3 = Lists.newArrayList();
         int var4 = param0.getCursor();

         while (true) {
            int var5;
            EntitySelector var6;
            while (true) {
               if (!param0.canRead()) {
                  return new MessageArgument.Message(var2, var3.toArray(new MessageArgument.Part[0]));
               }

               if (param0.peek() == '@') {
                  var5 = param0.getCursor();

                  try {
                     EntitySelectorParser var7 = new EntitySelectorParser(param0);
                     var6 = var7.parse();
                     break;
                  } catch (CommandSyntaxException var7) {
                     if (var7.getType() != EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE
                        && var7.getType() != EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
                        throw var7;
                     }

                     param0.setCursor(var5 + 1);
                  }
               } else {
                  param0.skip();
               }
            }

            var3.add(new MessageArgument.Part(var5 - var4, param0.getCursor() - var4, var6));
         }
      }
   }

   public static class Part {
      private final int start;
      private final int end;
      private final EntitySelector selector;

      public Part(int param0, int param1, EntitySelector param2) {
         this.start = param0;
         this.end = param1;
         this.selector = param2;
      }

      public int getStart() {
         return this.start;
      }

      public int getEnd() {
         return this.end;
      }

      public EntitySelector getSelector() {
         return this.selector;
      }

      @Nullable
      public Component toComponent(CommandSourceStack param0) throws CommandSyntaxException {
         return EntitySelector.joinNames(this.selector.findEntities(param0));
      }
   }
}
