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
      StackTraceElement[] var2 = Thread.currentThread().getStackTrace();
      StackTraceElement var3 = var2[Math.min(3, var2.length)];
      LOGGER.info("[{}]@.({}:{}): {}", this.name, var3.getFileName(), var3.getLineNumber(), param0);
   }
}
