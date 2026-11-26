package net.minecraft.client.telemetry;

import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

public class ClientTelemetryManager implements AutoCloseable {
   private static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);

   /**
    * Runs telemetry submissions inline instead of on a dedicated thread.
    *
    * <p>Upstream builds a single-thread executor with a naming {@code ThreadFactory}. Neither
    * {@code java.util.concurrent.Executors} nor {@code ThreadFactory} exists in the WASM-GC TeaVM
    * classlib, and spawning a thread is not merely inconvenient there either -- {@code Thread.start}
    * requires the {@code org.teavm.platform.Platform} support this target does not provide. This is
    * the same substitution already made for {@code Util.backgroundExecutor()}, and it matches how
    * Eaglercraft itself runs background work: on the one available thread.
    *
    * <p>The practical effect is that a telemetry upload now blocks the calling tick instead of
    * happening concurrently. Telemetry is fire-and-forget reporting to Mojang, is rate-limited and
    * small, and the call sites already tolerate slow submission, so this is acceptable -- but it is a
    * real behavioural difference, not a transparent one.
    */
   private static final Executor EXECUTOR = Runnable::run;
   private final UserApiService userApiService;
   private final TelemetryPropertyMap deviceSessionProperties;
   private final Path logDirectory;
   private final CompletableFuture<Optional<TelemetryLogManager>> logManager;

   public ClientTelemetryManager(Minecraft param0, UserApiService param1, User param2) {
      this.userApiService = param1;
      TelemetryPropertyMap.Builder var4 = TelemetryPropertyMap.builder();
      param2.getXuid().ifPresent(l_param1 -> var4.put(TelemetryProperty.USER_ID, l_param1));
      param2.getClientId().ifPresent(l_param1 -> var4.put(TelemetryProperty.CLIENT_ID, l_param1));
      var4.put(TelemetryProperty.MINECRAFT_SESSION_ID, UUID.randomUUID());
      var4.put(TelemetryProperty.GAME_VERSION, SharedConstants.getCurrentVersion().getId());
      var4.put(TelemetryProperty.OPERATING_SYSTEM, Util.getPlatform().telemetryName());
      var4.put(TelemetryProperty.PLATFORM, System.getProperty("os.name"));
      var4.put(TelemetryProperty.CLIENT_MODDED, Minecraft.checkModStatus().shouldReportAsModified());
      this.deviceSessionProperties = var4.build();
      this.logDirectory = Util.toPath(param0.gameDirectory).resolve("logs/telemetry");
      // Port note (wasm14): telemetry is disabled in this build, which exists to reach the title
      // screen. Upstream this opened a TelemetryLogManager over a directory under the game folder
      // and handed every future logger to Util.backgroundExecutor(). Two problems, only the
      // second of which is cosmetic: a browser has no directory to open, and TelemetryLogManager
      // issues its first supplyAsync straight into a whole-program Runnable union that pulled in
      // IOWorker's chunk-store lambdas -- i.e. the region-file writer -- from nothing more than
      // constructing the client. With the log manager gone there is nothing to open, so the
      // session sender below short-circuits to DISABLED and close() has nothing to close.
      this.logManager = CompletableFuture.completedFuture(Optional.empty());
   }

   public WorldSessionTelemetryManager createWorldSessionManager(boolean param0, @Nullable Duration param1) {
      return new WorldSessionTelemetryManager(this.createWorldSessionEventSender(), param0, param1);
   }

   private TelemetryEventSender createWorldSessionEventSender() {
      // Port note (wasm14): see the constructor -- telemetry is off in this build, and the log
      // manager it would have written through does not exist. Returning DISABLED here keeps
      // WorldSessionTelemetryManager's own logic intact (it is what it checks first anyway when
      // the session is not opted in) and keeps userApiService.newTelemetrySession, whose
      // implementation would hand us a TelemetrySession backed by the same HTTP transport.
      return TelemetryEventSender.DISABLED;
   }

   public Path getLogDirectory() {
      return this.logDirectory;
   }

   @Override
   public void close() {
      this.logManager.thenAccept(l_param0 -> l_param0.ifPresent(TelemetryLogManager::close));
   }
}
