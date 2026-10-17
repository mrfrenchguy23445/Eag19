package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.slf4j.Logger;

public class FutureChain implements TaskChainer, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private CompletableFuture<?> head = CompletableFuture.completedFuture(null);
   private final Executor checkedExecutor;
   private volatile boolean closed;

   public FutureChain(Executor param0) {
      this.checkedExecutor = l_param1 -> {
         if (!this.closed) {
            param0.execute(l_param1);
         }
      };
   }

   @Override
   public void append(TaskChainer.DelayedTask param0) {
      this.head = this.head.thenComposeAsync(l_param1 -> param0.submit(this.checkedExecutor), this.checkedExecutor).exceptionally(l_param0 -> {
         if (l_param0 instanceof CompletionException l_var1) {
            l_param0 = l_var1.getCause();
         }

         if (l_param0 instanceof CancellationException l_var1) {
            throw l_var1;
         } else {
            LOGGER.error("Chain link failed, continuing to next one", l_param0);
            return null;
         }
      });
   }

   @Override
   public void close() {
      this.closed = true;
   }
}
