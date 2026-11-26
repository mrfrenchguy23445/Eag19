package net.minecraft.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface AbortableIterationConsumer<T> {
   AbortableIterationConsumer.Continuation accept(T var1);

   static <T> AbortableIterationConsumer<T> forConsumer(Consumer<T> param0) {
      return l_param1 -> {
         param0.accept(l_param1);
         return AbortableIterationConsumer.Continuation.CONTINUE;
      };
   }

   enum Continuation {
      CONTINUE,
      ABORT;

      public boolean shouldAbort() {
         return this == ABORT;
      }
   }
}
