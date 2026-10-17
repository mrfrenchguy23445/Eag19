package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;

@FunctionalInterface
public interface TaskChainer {
   Logger LOGGER = LogUtils.getLogger();

   static TaskChainer immediate(Executor param0) {
      return l_param1 -> l_param1.submit(param0).exceptionally(l2_param0 -> {
         LOGGER.error("Task failed", l2_param0);
         return null;
      });
   }

   void append(TaskChainer.DelayedTask var1);

   interface DelayedTask {
      CompletableFuture<?> submit(Executor var1);
   }
}
