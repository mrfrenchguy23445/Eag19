package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;

public class ComponentUtils {
   public static final String DEFAULT_SEPARATOR_TEXT = ", ";
   public static final Component DEFAULT_SEPARATOR = Component.literal(", ").withStyle(ChatFormatting.GRAY);
   public static final Component DEFAULT_NO_STYLE_SEPARATOR = Component.literal(", ");

   public static MutableComponent mergeStyles(MutableComponent param0, Style param1) {
      if (param1.isEmpty()) {
         return param0;
      } else {
         Style var2 = param0.getStyle();
         if (var2.isEmpty()) {
            return param0.setStyle(param1);
         } else {
            return var2.equals(param1) ? param0 : param0.setStyle(var2.applyTo(param1));
         }
      }
   }

   public static Optional<MutableComponent> updateForEntity(
      @Nullable CommandSourceStack param0, Optional<Component> param1, @Nullable Entity param2, int param3
   ) throws CommandSyntaxException {
      return param1.isPresent() ? Optional.of(updateForEntity(param0, param1.get(), param2, param3)) : Optional.empty();
   }

   public static MutableComponent updateForEntity(@Nullable CommandSourceStack param0, Component param1, @Nullable Entity param2, int param3) throws CommandSyntaxException {
      if (param3 > 100) {
         return param1.copy();
      }

      MutableComponent var4 = param1.getContents().resolve(param0, param2, param3 + 1);

      for (Component var6 : param1.getSiblings()) {
         var4.append(updateForEntity(param0, var6, param2, param3 + 1));
      }

      return var4.withStyle(resolveStyle(param0, param1.getStyle(), param2, param3));
   }

   private static Style resolveStyle(@Nullable CommandSourceStack param0, Style param1, @Nullable Entity param2, int param3) throws CommandSyntaxException {
      HoverEvent var4 = param1.getHoverEvent();
      if (var4 != null) {
         Component var5 = var4.getValue(HoverEvent.Action.SHOW_TEXT);
         if (var5 != null) {
            HoverEvent var6 = new HoverEvent(HoverEvent.Action.SHOW_TEXT, updateForEntity(param0, var5, param2, param3 + 1));
            return param1.withHoverEvent(var6);
         }
      }

      return param1;
   }

   public static Component getDisplayName(GameProfile param0) {
      if (param0.getName() != null) {
         return Component.literal(param0.getName());
      } else {
         return param0.getId() != null ? Component.literal(param0.getId().toString()) : Component.literal("(unknown)");
      }
   }

   public static Component formatList(Collection<String> param0) {
      return formatAndSortList(param0, l_param0 -> Component.literal(l_param0).withStyle(ChatFormatting.GREEN));
   }

   public static <T extends Comparable<T>> Component formatAndSortList(Collection<T> param0, Function<T, Component> param1) {
      if (param0.isEmpty()) {
         return CommonComponents.EMPTY;
      }

      if (param0.size() == 1) {
         return param1.apply(param0.iterator().next());
      }

      List<T> var2 = Lists.newArrayList(param0);
      var2.sort(Comparable::compareTo);
      return formatList(var2, param1);
   }

   public static <T> Component formatList(Collection<? extends T> param0, Function<T, Component> param1) {
      return formatList(param0, DEFAULT_SEPARATOR, param1);
   }

   public static <T> MutableComponent formatList(Collection<? extends T> param0, Optional<? extends Component> param1, Function<T, Component> param2) {
      return formatList(param0, DataFixUtils.orElse(param1, DEFAULT_SEPARATOR), param2);
   }

   public static Component formatList(Collection<? extends Component> param0, Component param1) {
      return formatList(param0, param1, Function.identity());
   }

   public static <T> MutableComponent formatList(Collection<? extends T> param0, Component param1, Function<T, Component> param2) {
      if (param0.isEmpty()) {
         return Component.empty();
      }

      if (param0.size() == 1) {
         return param2.apply((T)param0.iterator().next()).copy();
      }

      MutableComponent var3 = Component.empty();
      boolean var4 = true;

      for (T var6 : param0) {
         if (!var4) {
            var3.append(param1);
         }

         var3.append(param2.apply(var6));
         var4 = false;
      }

      return var3;
   }

   public static MutableComponent wrapInSquareBrackets(Component param0) {
      return Component.translatable("chat.square_brackets", param0);
   }

   public static Component fromMessage(Message param0) {
      return param0 instanceof Component var1 ? var1 : Component.literal(param0.getString());
   }

   public static boolean isTranslationResolvable(@Nullable Component param0) {
      if (param0 != null && param0.getContents() instanceof TranslatableContents var1) {
         String var2 = var1.getKey();
         String var3 = var1.getFallback();
         return var3 != null || Language.getInstance().has(var2);
      } else {
         return true;
      }
   }

   public static MutableComponent copyOnClickText(String param0) {
      return wrapInSquareBrackets(
         Component.literal(param0)
            .withStyle(
               l_param1 -> l_param1.withColor(ChatFormatting.GREEN)
                  .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, param0))
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click")))
                  .withInsertion(param0)
            )
      );
   }
}
