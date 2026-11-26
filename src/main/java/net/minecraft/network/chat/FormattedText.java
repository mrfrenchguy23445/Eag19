package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.Unit;

public interface FormattedText {
   Optional<Unit> STOP_ITERATION = Optional.of(Unit.INSTANCE);
   FormattedText EMPTY = new FormattedText() {
      @Override
      public <T> Optional<T> visit(FormattedText.ContentConsumer<T> param0) {
         return Optional.empty();
      }

      @Override
      public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> param0, Style param1) {
         return Optional.empty();
      }
   };

   <T> Optional<T> visit(FormattedText.ContentConsumer<T> var1);

   <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> var1, Style var2);

   static FormattedText of(final String param0) {
      return new FormattedText() {
         @Override
         public <T> Optional<T> visit(FormattedText.ContentConsumer<T> l_param0) {
            return l_param0.accept(param0);
         }

         @Override
         public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> l_param0, Style l_param1) {
            return l_param0.accept(l_param1, param0);
         }
      };
   }

   static FormattedText of(final String param0, final Style param1) {
      return new FormattedText() {
         @Override
         public <T> Optional<T> visit(FormattedText.ContentConsumer<T> l_param0) {
            return l_param0.accept(param0);
         }

         @Override
         public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> l_param0, Style l_param1) {
            return l_param0.accept(param1.applyTo(l_param1), param0);
         }
      };
   }

   static FormattedText composite(FormattedText... param0) {
      return composite(ImmutableList.copyOf(param0));
   }

   static FormattedText composite(final List<? extends FormattedText> param0) {
      return new FormattedText() {
         @Override
         public <T> Optional<T> visit(FormattedText.ContentConsumer<T> l_param0) {
            for (FormattedText l_var3 : param0) {
               Optional<T> l_var4 = l_var3.visit(l_param0);
               if (l_var4.isPresent()) {
                  return l_var4;
               }
            }

            return Optional.empty();
         }

         @Override
         public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> l_param0, Style l_param1) {
            for (FormattedText l_var4 : param0) {
               Optional<T> l_var5 = l_var4.visit(l_param0, l_param1);
               if (l_var5.isPresent()) {
                  return l_var5;
               }
            }

            return Optional.empty();
         }
      };
   }

   default String getString() {
      StringBuilder var1 = new StringBuilder();
      this.visit(l_param1 -> {
         var1.append(l_param1);
         return Optional.empty();
      });
      return var1.toString();
   }

   interface ContentConsumer<T> {
      Optional<T> accept(String var1);
   }

   interface StyledContentConsumer<T> {
      Optional<T> accept(Style var1, String var2);
   }
}
