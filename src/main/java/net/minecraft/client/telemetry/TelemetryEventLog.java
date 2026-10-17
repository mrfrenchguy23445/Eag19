package net.minecraft.client.telemetry;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.Executor;
import net.minecraft.util.eventlog.JsonEventLog;
import net.minecraft.util.thread.ProcessorMailbox;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class TelemetryEventLog implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final JsonEventLog<TelemetryEventInstance> log;
   private final ProcessorMailbox<Runnable> mailbox;

   public TelemetryEventLog(FileChannel param0, Executor param1) {
      this.log = new JsonEventLog<>(TelemetryEventInstance.CODEC, param0);
      this.mailbox = ProcessorMailbox.create(param1, "telemetry-event-log");
   }

   public TelemetryEventLogger logger() {
      return l_param0 -> this.mailbox.tell(() -> {
         try {
            this.log.write(l_param0);
         } catch (IOException l2_var2) {
            LOGGER.error("Failed to write telemetry event to log", l2_var2);
         }
      });
   }

   @Override
   public void close() {
      this.mailbox.tell(() -> IOUtils.closeQuietly(this.log));
      this.mailbox.close();
   }
}
