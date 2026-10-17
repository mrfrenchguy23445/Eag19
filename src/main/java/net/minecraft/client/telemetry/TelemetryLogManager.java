package net.minecraft.client.telemetry;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.eventlog.EventLogDirectory;
import org.slf4j.Logger;

public class TelemetryLogManager implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String RAW_EXTENSION = ".json";
   private static final int EXPIRY_DAYS = 7;
   private final EventLogDirectory directory;
   @Nullable
   private CompletableFuture<Optional<TelemetryEventLog>> sessionLog;

   private TelemetryLogManager(EventLogDirectory param0) {
      this.directory = param0;
   }

   public static CompletableFuture<Optional<TelemetryLogManager>> open(Path param0) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            EventLogDirectory l_var1 = EventLogDirectory.open(param0, ".json");
            l_var1.listFiles().prune(LocalDate.now(), 7).compressAll();
            return Optional.of(new TelemetryLogManager(l_var1));
         } catch (Exception l_var1) {
            LOGGER.error("Failed to create telemetry log manager", l_var1);
            return Optional.empty();
         }
      }, Util.backgroundExecutor());
   }

   public CompletableFuture<Optional<TelemetryEventLogger>> openLogger() {
      if (this.sessionLog == null) {
         this.sessionLog = CompletableFuture.supplyAsync(() -> {
            try {
               EventLogDirectory.RawFile l_var1 = this.directory.createNewFile(LocalDate.now());
               FileChannel l_var2 = l_var1.openChannel();
               return Optional.of(new TelemetryEventLog(l_var2, Util.backgroundExecutor()));
            } catch (IOException l_var1) {
               LOGGER.error("Failed to open channel for telemetry event log", l_var1);
               return Optional.empty();
            }
         }, Util.backgroundExecutor());
      }

      return this.sessionLog.thenApply(l_param0 -> l_param0.map(TelemetryEventLog::logger));
   }

   @Override
   public void close() {
      if (this.sessionLog != null) {
         this.sessionLog.thenAccept(l_param0 -> l_param0.ifPresent(TelemetryEventLog::close));
      }
   }
}
