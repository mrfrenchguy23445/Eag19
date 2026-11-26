package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.io.OutputStream;
import org.slf4j.Logger;

public class DebugLoggedPrintStream extends LoggedPrintStream {
   private static final Logger LOGGER = LogUtils.getLogger();

   public DebugLoggedPrintStream(String param0, OutputStream param1) {
      super(param0, param1);
   }

   @Override
   protected void logLine(String param0) {
      StackTraceElement var3 = null;
      try {
         StackTraceElement[] var2 = Thread.currentThread().getStackTrace();
         if (var2 != null && var2.length > 3) {
            var3 = var2[3];
         }
      } catch (Throwable var4) {
         var3 = null;
      }
      if (var3 != null) {
         LOGGER.info("[{}]@.({}:{}): {}", this.name, var3.getFileName(), var3.getLineNumber(), param0);
      } else {
         LOGGER.info("[{}]: {}", this.name, param0);
      }
   }
}
