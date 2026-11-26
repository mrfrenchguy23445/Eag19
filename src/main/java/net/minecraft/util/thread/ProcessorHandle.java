package net.minecraft.util.thread;

import com.mojang.datafixers.util.Either;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ProcessorHandle<Msg> extends AutoCloseable {
   String name();

   void tell(Msg var1);

   @Override
   default void close() {
   }

   default <Source> CompletableFuture<Source> ask(Function<? super ProcessorHandle<Source>, ? extends Msg> param0) {
      CompletableFuture<Source> var2 = new CompletableFuture<>();
      Msg var3 = (Msg)param0.apply(of("ask future procesor handle", var2::complete));
      this.tell(var3);
      return var2;
   }

   default <Source> CompletableFuture<Source> askEither(Function<? super ProcessorHandle<Either<Source, Exception>>, ? extends Msg> param0) {
      CompletableFuture<Source> var2 = new CompletableFuture<>();
      Msg var3 = (Msg)param0.apply(of("ask future procesor handle", l_param1 -> {
         l_param1.ifLeft(var2::complete);
         l_param1.ifRight(var2::completeExceptionally);
      }));
      this.tell(var3);
      return var2;
   }

   static <Msg> ProcessorHandle<Msg> of(final String param0, final Consumer<Msg> param1) {
      return new ProcessorHandle<Msg>() {
         @Override
         public String name() {
            return param0;
         }

         @Override
         public void tell(Msg l_param0) {
            param1.accept(l_param0);
         }

         @Override
         public String toString() {
            return param0;
         }
      };
   }
}
