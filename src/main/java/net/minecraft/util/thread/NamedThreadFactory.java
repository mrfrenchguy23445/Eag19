package net.minecraft.util.thread;

import com.mojang.logging.LogUtils;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

public class NamedThreadFactory implements ThreadFactory {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ThreadGroup group;
   private final AtomicInteger threadNumber = new AtomicInteger(1);
   private final String namePrefix;

   public NamedThreadFactory(String param0) {
      SecurityManager var2 = System.getSecurityManager();
      this.group = var2 != null ? var2.getThreadGroup() : Thread.currentThread().getThreadGroup();
      this.namePrefix = param0 + "-";
   }

   @Override
   public Thread newThread(Runnable param0) {
      Thread var2 = new Thread(this.group, param0, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
      var2.setUncaughtExceptionHandler((l_param1, l_param2) -> {
         LOGGER.error("Caught exception in thread {} from {}", l_param1, param0);
         LOGGER.error("", l_param2);
      });
      if (var2.getPriority() != 5) {
         var2.setPriority(5);
      }

      return var2;
   }
}
