package net.minecraft.network.chat.contents;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;

public class TranslatableContents implements ComponentContents {
   public static final Object[] NO_ARGS = new Object[0];
   private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
   private static final FormattedText TEXT_NULL = FormattedText.of("null");
   private final String key;
   @Nullable
   private final String fallback;
   private final Object[] args;
   @Nullable
   private Language decomposedWith;
   private List<FormattedText> decomposedParts = ImmutableList.of();
   private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

   public TranslatableContents(String param0, @Nullable String param1, Object[] param2) {
      this.key = param0;
      this.fallback = param1;
      this.args = param2;
   }

   private void decompose() {
      Language var1 = Language.getInstance();
      if (var1 != this.decomposedWith) {
         this.decomposedWith = var1;
         String var2 = this.fallback != null ? var1.getOrDefault(this.key, this.fallback) : var1.getOrDefault(this.key);

         try {
            Builder<FormattedText> var3 = ImmutableList.builder();
            this.decomposeTemplate(var2, var3::add);
            this.decomposedParts = var3.build();
         } catch (TranslatableFormatException var3) {
            this.decomposedParts = ImmutableList.of(FormattedText.of(var2));
         }
      }
   }

   private void decomposeTemplate(String param0, Consumer<FormattedText> param1) {
      Matcher var3 = FORMAT_PATTERN.matcher(param0);

      try {
         int var4 = 0;
         int var5 = 0;

         while (var3.find(var5)) {
            int var6 = var3.start();
            int var7 = var3.end();
            if (var6 > var5) {
               String var8 = param0.substring(var5, var6);
               if (var8.indexOf(37) != -1) {
                  throw new IllegalArgumentException();
               }

               param1.accept(FormattedText.of(var8));
            }

            String var8 = var3.group(2);
            String var9 = param0.substring(var6, var7);
            if ("%".equals(var8) && "%%".equals(var9)) {
               param1.accept(TEXT_PERCENT);
            } else {
               if (!"s".equals(var8)) {
                  throw new TranslatableFormatException(this, "Unsupported format: '" + var9 + "'");
               }

               String var10 = var3.group(1);
               int var11 = var10 != null ? Integer.parseInt(var10) - 1 : var4++;
               param1.accept(this.getArgument(var11));
            }

            var5 = var7;
         }

         if (var5 < param0.length()) {
            String var6 = param0.substring(var5);
            if (var6.indexOf(37) != -1) {
               throw new IllegalArgumentException();
            }

            param1.accept(FormattedText.of(var6));
         }
      } catch (IllegalArgumentException var4) {
         throw new TranslatableFormatException(this, var4);
      }
   }

   private FormattedText getArgument(int param0) {
      if (param0 >= 0 && param0 < this.args.length) {
         Object var2 = this.args[param0];
         if (var2 instanceof Component) {
            return (Component)var2;
         } else {
            return var2 == null ? TEXT_NULL : FormattedText.of(var2.toString());
         }
      } else {
         throw new TranslatableFormatException(this, param0);
      }
   }

   @Override
   public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> param0, Style param1) {
      this.decompose();

      for (FormattedText var4 : this.decomposedParts) {
         Optional<T> var5 = var4.visit(param0, param1);
         if (var5.isPresent()) {
            return var5;
         }
      }

      return Optional.empty();
   }

   @Override
   public <T> Optional<T> visit(FormattedText.ContentConsumer<T> param0) {
      this.decompose();

      for (FormattedText var3 : this.decomposedParts) {
         Optional<T> var4 = var3.visit(param0);
         if (var4.isPresent()) {
            return var4;
         }
      }

      return Optional.empty();
   }

   @Override
   public MutableComponent resolve(@Nullable CommandSourceStack param0, @Nullable Entity param1, int param2) throws CommandSyntaxException {
      Object[] var4 = new Object[this.args.length];

      for (int var5 = 0; var5 < var4.length; var5++) {
         Object var6 = this.args[var5];
         if (var6 instanceof Component) {
            var4[var5] = ComponentUtils.updateForEntity(param0, (Component)var6, param1, param2);
         } else {
            var4[var5] = var6;
         }
      }

      return MutableComponent.create(new TranslatableContents(this.key, this.fallback, var4));
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0
         ? true
         : param0 instanceof TranslatableContents var2
            && Objects.equals(this.key, var2.key)
            && Objects.equals(this.fallback, var2.fallback)
            && Arrays.equals(this.args, var2.args);
   }

   @Override
   public int hashCode() {
      int var1 = Objects.hashCode(this.key);
      var1 = 31 * var1 + Objects.hashCode(this.fallback);
      return 31 * var1 + Arrays.hashCode(this.args);
   }

   @Override
   public String toString() {
      return "translation{key='"
         + this.key
         + "'"
         + (this.fallback != null ? ", fallback='" + this.fallback + "'" : "")
         + ", args="
         + Arrays.toString(this.args)
         + "}";
   }

   public String getKey() {
      return this.key;
   }

   @Nullable
   public String getFallback() {
      return this.fallback;
   }

   public Object[] getArgs() {
      return this.args;
   }
}
