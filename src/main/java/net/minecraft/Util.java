package net.minecraft;

import com.google.common.base.Ticker;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.PartialResult;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformApplication;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SingleKeyCache;
import net.minecraft.util.TimeSource;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

public class Util {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_MAX_THREADS = 255;
   private static final String MAX_THREADS_SYSTEM_PROPERTY = "max.bg.threads";
   private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
   private static final Executor BACKGROUND_EXECUTOR = new DirectExecutor("Main");
   private static final Executor IO_POOL = new DirectExecutor("IO");
   private static final DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
   public static TimeSource.NanoTimeSource timeSource = System::nanoTime;
   public static final Ticker TICKER = new Ticker() {
      @Override
      public long read() {
         return Util.timeSource.getAsLong();
      }
   };
   public static final UUID NIL_UUID = new UUID(0L, 0L);
   private static Consumer<String> thePauser = param0 -> {};

   public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
      return Collectors.toMap(Entry::getKey, Entry::getValue);
   }

   public static <T extends Comparable<T>> String getPropertyName(Property<T> param0, Object param1) {
      return param0.getName((T)param1);
   }

   public static String makeDescriptionId(String param0, @Nullable ResourceLocation param1) {
      return param1 == null ? param0 + ".unregistered_sadface" : param0 + "." + param1.getNamespace() + "." + param1.getPath().replace('/', '.');
   }

   public static long getMillis() {
      return getNanos() / 1000000L;
   }

   public static long getNanos() {
      return timeSource.getAsLong();
   }

   public static long getEpochMillis() {
      return Instant.now().toEpochMilli();
   }

   public static String getFilenameFormattedDateTime() {
      return FILENAME_DATE_TIME_FORMATTER.format(ZonedDateTime.now());
   }

   /**
    * Runs submitted tasks inline on the calling thread.
    *
    * <p>Stands in for the background/IO thread pools that upstream creates with
    * {@code ForkJoinPool} and {@code Executors.newCachedThreadPool}. Neither
    * {@code ExecutorService} nor the classes behind those factories exist in the WASM-GC TeaVM
    * classlib, and the browser client has no worker threads, so running inline is the only
    * available behaviour. The name is retained purely for log output.
    */
   private static final class DirectExecutor implements Executor {
      private final String name;

      DirectExecutor(String name) {
         this.name = name;
      }

      @Override
      public void execute(Runnable param0) {
         try {
            param0.run();
         } catch (Throwable var1) {
            LOGGER.debug("{} task failed", this.name, var1);
            onTaskFailure(var1);
         }
      }

      @Override
      public String toString() {
         return "DirectExecutor(" + this.name + ")";
      }
   }

   /**
    * Retained for reference only.
    *
    * <p>Upstream reads the {@code max.bg.threads} system property here to size the background
    * {@code ForkJoinPool}. That pool is replaced by a direct executor (see
    * {@link #BACKGROUND_EXECUTOR}), so the property has no effect and this method is unused.
    */
   private static int getMaxThreads() {
      String var0 = System.getProperty("max.bg.threads");
      if (var0 != null) {
         try {
            int var1 = Integer.parseInt(var0);
            if (var1 >= 1 && var1 <= 255) {
               return var1;
            }

            LOGGER.error("Wrong {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", var0, 255);
         } catch (NumberFormatException var1) {
            LOGGER.error("Could not parse {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", var0, 255);
         }
      }

      return 255;
   }

   public static Executor backgroundExecutor() {
      return BACKGROUND_EXECUTOR;
   }

   public static Executor ioPool() {
      return IO_POOL;
   }

   /**
    * No-op retained for call-site compatibility.
    *
    * <p>Upstream shuts the background and IO thread pools down here. Both are direct
    * executors on WASM-GC -- they own no threads and have no queued work that outlives the
    * caller -- so there is nothing to shut down.
    */
   public static void shutdownExecutors() {
   }

   public static void throwAsRuntime(Throwable param0) {
      throw param0 instanceof RuntimeException ? (RuntimeException)param0 : new RuntimeException(param0);
   }

   /**
    * Reports a failure that escaped a task submitted to one of the executors above.
    *
    * <p>Upstream passes an equivalent handler as a {@code Thread.UncaughtExceptionHandler} to
    * the thread pool factories. With direct executors there is no worker thread, so the handler
    * is invoked directly and the submitting executor's name stands in for the thread name.
    */
   private static void onTaskFailure(Throwable param0) {
      onTaskFailure(param0, "worker");
   }

   private static void onTaskFailure(Throwable param0, String param1) {
      pauseInIde(param0);
      if (param0 instanceof CompletionException) {
         param0 = param0.getCause();
      }

      if (param0 instanceof ReportedException) {
         Bootstrap.realStdoutPrintln(((ReportedException)param0).getReport().getFriendlyReport());
         // Upstream ends the process here, and the end is the point: a ReportedException
         // means the data fixer could not bring some schema up to date, so the world that
         // follows would be silently wrong. There is no process to end. The tab cannot be
         // closed from here (window.close() is only honoured for script-opened windows, and
         // asking the user to close the tab is worse than saying what happened), so the
         // fatal is recorded and control returns instead.
         //
         // Returning rather than falling through is deliberate. The only way to reach this
         // method is DirectExecutor.execute's catch, which has already swallowed the
         // throwable, so nothing upstream of here would react either way -- but the generic
         // "Caught exception in thread %s" below is a different claim, that an unexpected
         // worker crashed. This is a known-fatal report, and the log should say so.
         //
         // system.exit() is absent from the WASM-GC classlib for the same reason
         // Runtime.exec is: a WebAssembly module in a browser has nothing to terminate.
         LOGGER.error("ReportedException: the game cannot continue with this data. "
            + "A desktop client exits here; this client will keep running, so do not trust "
            + "the world it is in.");
         return;
      }

      LOGGER.error(String.format(Locale.ROOT, "Caught exception in thread %s", param1), param0);
   }

   @Nullable
   public static Type<?> fetchChoiceType(TypeReference param0, String param1) {
      return !SharedConstants.CHECK_DATA_FIXER_SCHEMA ? null : doFetchChoiceType(param0, param1);
   }

   @Nullable
   private static Type<?> doFetchChoiceType(TypeReference param0, String param1) {
      Type<?> var2 = null;

      try {
         var2 = DataFixers.getDataFixer()
            .getSchema(DataFixUtils.makeKey(SharedConstants.getCurrentVersion().getDataVersion().getVersion()))
            .getChoiceType(param0, param1);
      } catch (IllegalArgumentException var3) {
         LOGGER.error("No data fixer registered for {}", param1);
         if (SharedConstants.IS_RUNNING_IN_IDE) {
            throw var3;
         }
      }

      return var2;
   }

   public static Runnable wrapThreadWithTaskName(String param0, Runnable param1) {
      return SharedConstants.IS_RUNNING_IN_IDE ? () -> {
         Thread l_var2 = Thread.currentThread();
         String l_var3 = l_var2.getName();
         l_var2.setName(param0);

         try {
            param1.run();
         } finally {
            l_var2.setName(l_var3);
         }
      } : param1;
   }

   public static <V> Supplier<V> wrapThreadWithTaskName(String param0, Supplier<V> param1) {
      return SharedConstants.IS_RUNNING_IN_IDE ? () -> {
         Thread l_var2 = Thread.currentThread();
         String l_var3 = l_var2.getName();
         l_var2.setName(param0);

         try {
            return param1.get();
         } finally {
            l_var2.setName(l_var3);
         }
      } : param1;
   }

   public static Util.OS getPlatform() {
      String var0 = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      if (var0.contains("win")) {
         return Util.OS.WINDOWS;
      } else if (var0.contains("mac")) {
         return Util.OS.OSX;
      } else if (var0.contains("solaris")) {
         return Util.OS.SOLARIS;
      } else if (var0.contains("sunos")) {
         return Util.OS.SOLARIS;
      } else if (var0.contains("linux")) {
         return Util.OS.LINUX;
      } else {
         return var0.contains("unix") ? Util.OS.LINUX : Util.OS.UNKNOWN;
      }
   }

   public static Stream<String> getVmArguments() {
      // Upstream reads java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()
      // and keeps the -X flags, to work out the JVM's max heap when sizing caches. JMX does not exist
      // in the WASM-GC TeaVM classlib, and it could not report anything useful even if it did: the
      // memory ceiling on this target is set by the TeaVM WASM memory configuration, not by -Xmx.
      // The empty stream is the honest answer and keeps the call sites unchanged.
      return Stream.empty();
   }

   public static <T> T lastOf(List<T> param0) {
      return param0.get(param0.size() - 1);
   }

   public static <T> T findNextInIterable(Iterable<T> param0, @Nullable T param1) {
      Iterator<T> var2 = param0.iterator();
      T var3 = var2.next();
      if (param1 != null) {
         T var4 = var3;

         while (var4 != param1) {
            if (var2.hasNext()) {
               var4 = var2.next();
            }
         }

         if (var2.hasNext()) {
            return var2.next();
         }
      }

      return var3;
   }

   public static <T> T findPreviousInIterable(Iterable<T> param0, @Nullable T param1) {
      Iterator<T> var2 = param0.iterator();
      T var3 = null;

      while (var2.hasNext()) {
         T var4 = var2.next();
         if (var4 == param1) {
            if (var3 == null) {
               var3 = var2.hasNext() ? Iterators.getLast(var2) : param1;
            }
            break;
         }

         var3 = var4;
      }

      return var3;
   }

   public static <T> T make(Supplier<T> param0) {
      return param0.get();
   }

   public static <T> T make(T param0, Consumer<T> param1) {
      param1.accept(param0);
      return param0;
   }

   public static <K> Strategy<K> identityStrategy() {
      return (Strategy<K>)(Strategy<?>)Util.IdentityStrategy.INSTANCE;
   }

   public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<V>> param0) {
      if (param0.isEmpty()) {
         return CompletableFuture.completedFuture(List.of());
      }

      if (param0.size() == 1) {
         return param0.get(0).thenApply(List::of);
      }

      CompletableFuture<Void> var1 = CompletableFuture.allOf(param0.toArray(new CompletableFuture[0]));
      return var1.thenApply(l_param1 -> param0.stream().map(CompletableFuture::join).toList());
   }

   public static <V> CompletableFuture<List<V>> sequenceFailFast(List<? extends CompletableFuture<? extends V>> param0) {
      CompletableFuture<List<V>> var1 = new CompletableFuture<>();
      return fallibleSequence(param0, var1::completeExceptionally).applyToEither(var1, Function.identity());
   }

   public static <V> CompletableFuture<List<V>> sequenceFailFastAndCancel(List<? extends CompletableFuture<? extends V>> param0) {
      CompletableFuture<List<V>> var1 = new CompletableFuture<>();
      return fallibleSequence(param0, l_param2 -> {
         if (var1.completeExceptionally(l_param2)) {
            for (CompletableFuture<? extends V> l_var4 : param0) {
               l_var4.cancel(true);
            }
         }
      }).applyToEither(var1, Function.identity());
   }

   private static <V> CompletableFuture<List<V>> fallibleSequence(List<? extends CompletableFuture<? extends V>> param0, Consumer<Throwable> param1) {
      List<V> var2 = Lists.newArrayListWithCapacity(param0.size());
      CompletableFuture<?>[] var3 = new CompletableFuture[param0.size()];
      param0.forEach(l_param3 -> {
         int l_var4 = var2.size();
         var2.add(null);
         var3[l_var4] = l_param3.whenComplete((l2_param3, l2_param4) -> {
            if (l2_param4 != null) {
               param1.accept(l2_param4);
            } else {
               var2.set(l_var4, (V)l2_param3);
            }
         });
      });
      return CompletableFuture.allOf(var3).thenApply(l_param1 -> var2);
   }

   public static <T> Optional<T> ifElse(Optional<T> param0, Consumer<T> param1, Runnable param2) {
      if (param0.isPresent()) {
         param1.accept(param0.get());
      } else {
         param2.run();
      }

      return param0;
   }

   public static <T> Supplier<T> name(Supplier<T> param0, Supplier<String> param1) {
      return param0;
   }

   public static Runnable name(Runnable param0, Supplier<String> param1) {
      return param0;
   }

   public static void logAndPauseIfInIde(String param0) {
      LOGGER.error(param0);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         doPause(param0);
      }
   }

   public static void logAndPauseIfInIde(String param0, Throwable param1) {
      LOGGER.error(param0, param1);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         doPause(param0);
      }
   }

   public static <T extends Throwable> T pauseInIde(T param0) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         LOGGER.error("Trying to throw a fatal exception, pausing in IDE", param0);
         doPause(param0.getMessage());
      }

      return param0;
   }

   public static void setPause(Consumer<String> param0) {
      thePauser = param0;
   }

   private static void doPause(String param0) {
      Instant var1 = Instant.now();
      LOGGER.warn("Did you remember to set a breakpoint here?");
      boolean var2 = Duration.between(var1, Instant.now()).toMillis() > 500L;
      if (!var2) {
         thePauser.accept(param0);
      }
   }

   public static String describeError(Throwable param0) {
      if (param0.getCause() != null) {
         return describeError(param0.getCause());
      } else {
         return param0.getMessage() != null ? param0.getMessage() : param0.toString();
      }
   }

   public static <T> T getRandom(T[] param0, RandomSource param1) {
      return param0[param1.nextInt(param0.length)];
   }

   public static int getRandom(int[] param0, RandomSource param1) {
      return param0[param1.nextInt(param0.length)];
   }

   public static <T> T getRandom(List<T> param0, RandomSource param1) {
      return param0.get(param1.nextInt(param0.size()));
   }

   public static <T> Optional<T> getRandomSafe(List<T> param0, RandomSource param1) {
      return param0.isEmpty() ? Optional.empty() : Optional.of(getRandom(param0, param1));
   }

   private static BooleanSupplier createRenamer(final Path param0, final Path param1) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            try {
               Files.move(param0, param1);
               return true;
            } catch (IOException l_var1) {
               Util.LOGGER.error("Failed to rename", l_var1);
               return false;
            }
         }

         @Override
         public String toString() {
            return "rename " + param0 + " to " + param1;
         }
      };
   }

   private static BooleanSupplier createDeleter(final Path param0) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            try {
               Files.deleteIfExists(param0);
               return true;
            } catch (IOException l_var1) {
               Util.LOGGER.warn("Failed to delete", l_var1);
               return false;
            }
         }

         @Override
         public String toString() {
            return "delete old " + param0;
         }
      };
   }

   private static BooleanSupplier createFileDeletedCheck(final Path param0) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return !Files.exists(param0);
         }

         @Override
         public String toString() {
            return "verify that " + param0 + " is deleted";
         }
      };
   }

   private static BooleanSupplier createFileCreatedCheck(final Path param0) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return Files.isRegularFile(param0);
         }

         @Override
         public String toString() {
            return "verify that " + param0 + " is present";
         }
      };
   }

   private static boolean executeInSequence(BooleanSupplier... param0) {
      for (BooleanSupplier var4 : param0) {
         if (!var4.getAsBoolean()) {
            LOGGER.warn("Failed to execute {}", var4);
            return false;
         }
      }

      return true;
   }

   private static boolean runWithRetries(int param0, String param1, BooleanSupplier... param2) {
      for (int var3 = 0; var3 < param0; var3++) {
         if (executeInSequence(param2)) {
            return true;
         }

         LOGGER.error("Failed to {}, retrying {}/{}", param1, var3, param0);
      }

      LOGGER.error("Failed to {}, aborting, progress might be lost", param1);
      return false;
   }

   public static void safeReplaceFile(File param0, File param1, File param2) {
      safeReplaceFile(toPath(param0), toPath(param1), toPath(param2));
   }

   /**
    * The one replacement for {@code java.io.File.toPath()} in this port.
    *
    * <p>{@code toPath()} does not exist on the WASM-GC TeaVM classlib's {@code File}: the
    * modelled class has {@code getAbsolutePath}, {@code getPath}, {@code getName} and the
    * usual accessors, but no {@code toPath}. Nine call sites reached it from the client
    * entry point, so this is where they all go rather than open-coding the conversion.
    *
    * <p>The JDK implements {@code toPath()} as
    * {@code FileSystems.getDefault().getPath(getPath())} -- by name, through the default
    * filesystem, with no normalisation of its own. So this is the same conversion and not
    * an approximation of it: {@link Paths#get(String)} is that exact call.
    *
    * <p>The absolute form is used rather than {@code getPath()} deliberately. Eaglercraft
    * serves these files out of {@code assets.epk} against its own virtual filesystem, which
    * has no meaningful working directory for a relative path to resolve against, so a
    * relative {@code File} would resolve against nothing and name the wrong place. Every
    * caller that reaches this helper already holds a {@code File} that was built from
    * {@code gameDirectory}, which is itself absolute. The same reasoning and the same call
    * are already spelled out at GameConfig.getExternalAssetSource.
    */
   public static Path toPath(File param0) {
      return Paths.get(param0.getAbsolutePath());
   }

   /**
    * The one replacement for {@code java.net.URL.openConnection(Proxy)} in this port.
    *
    * <p>{@code openConnection(Proxy)} is absent from the WASM-GC TeaVM classlib's URL,
    * which models only the no-argument form. Three call sites reached it: the Realms
    * request path, {@code HttpTexture}'s download and {@code HttpUtil.downloadTo}.
    *
    * <p>Passing {@code Proxy.NO_PROXY} to the two-argument form is exactly what the JDK
    * does internally -- it nulls the proxy out and takes the direct path -- so the
    * one-argument form is the same connection, not a cheaper one. That is the whole
    * reason the helper is a pass-through rather than a warning.
    *
    * <p>A proxy that is <em>not</em> {@code NO_PROXY} throws rather than being ignored.
    * Silently dropping it would send the request direct while the caller believed it was
    * tunnelled, which for the Realms and resource-pack paths means authenticating to a
    * host the player did not intend to reach. Nothing in this port can supply a working
    * proxy anyway: {@code Minecraft.proxy} is assigned {@code Proxy.NO_PROXY}
    * unconditionally, and {@code java.net.SocketAddress} has no address model here at
    * all. So the case is unreachable, and the exception is a statement that it stays so.
    */
   public static URLConnection openConnection(URL param0, Proxy param1) throws IOException {
      if (param1 != null && param1 != Proxy.NO_PROXY) {
         throw new UnsupportedOperationException(
            "A proxy was configured, but a browser has no proxy to configure; requested via " + param0);
      }

      return param0.openConnection();
   }

   public static void safeReplaceOrMoveFile(File param0, File param1, File param2, boolean param3) {
      safeReplaceOrMoveFile(toPath(param0), toPath(param1), toPath(param2), param3);
   }

   public static void safeReplaceFile(Path param0, Path param1, Path param2) {
      safeReplaceOrMoveFile(param0, param1, param2, false);
   }

   public static void safeReplaceOrMoveFile(Path param0, Path param1, Path param2, boolean param3) {
      int var4 = 10;
      if (!Files.exists(param0)
         || runWithRetries(10, "create backup " + param2, createDeleter(param2), createRenamer(param0, param2), createFileCreatedCheck(param2))) {
         if (runWithRetries(10, "remove old " + param0, createDeleter(param0), createFileDeletedCheck(param0))) {
            if (!runWithRetries(10, "replace " + param0 + " with " + param1, createRenamer(param1, param0), createFileCreatedCheck(param0)) && !param3) {
               runWithRetries(10, "restore " + param0 + " from " + param2, createRenamer(param2, param0), createFileCreatedCheck(param0));
            }
         }
      }
   }

   public static int offsetByCodepoints(String param0, int param1, int param2) {
      int var3 = param0.length();
      if (param2 >= 0) {
         for (int var4 = 0; param1 < var3 && var4 < param2; var4++) {
            if (Character.isHighSurrogate(param0.charAt(param1++)) && param1 < var3 && Character.isLowSurrogate(param0.charAt(param1))) {
               param1++;
            }
         }
      } else {
         for (int var4 = param2; param1 > 0 && var4 < 0; var4++) {
            param1--;
            if (Character.isLowSurrogate(param0.charAt(param1)) && param1 > 0 && Character.isHighSurrogate(param0.charAt(param1 - 1))) {
               param1--;
            }
         }
      }

      return param1;
   }

   public static Consumer<String> prefix(String param0, Consumer<String> param1) {
      return l_param2 -> param1.accept(param0 + l_param2);
   }

   public static DataResult<int[]> fixedSize(IntStream param0, int param1) {
      int[] var2 = param0.limit(param1 + 1).toArray();
      if (var2.length != param1) {
         Supplier<String> var3 = () -> "Input is not a list of " + param1 + " ints";
         return var2.length >= param1 ? DataResult.error(var3, Arrays.copyOf(var2, param1)) : DataResult.error(var3);
      } else {
         return DataResult.success(var2);
      }
   }

   public static <T> DataResult<List<T>> fixedSize(List<T> param0, int param1) {
      if (param0.size() != param1) {
         Supplier<String> var2 = () -> "Input is not a list of " + param1 + " elements";
         return param0.size() >= param1 ? DataResult.error(var2, param0.subList(0, param1)) : DataResult.error(var2);
      } else {
         return DataResult.success(param0);
      }
   }

   public static void startTimerHackThread() {
      Thread var0 = new Thread("Timer hack thread") {
         @Override
         public void run() {
            while (true) {
               try {
                  Thread.sleep(2147483647L);
               } catch (InterruptedException l_var1) {
                  Util.LOGGER.warn("Timer hack thread interrupted, that really should not happen");
                  return;
               }
            }
         }
      };
      var0.setDaemon(true);
      var0.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      var0.start();
   }

   public static void copyBetweenDirs(Path param0, Path param1, Path param2) throws IOException {
      Path var3 = param0.relativize(param2);
      Path var4 = param1.resolve(var3);
      Files.copy(param2, var4);
   }

   public static String sanitizeName(String param0, CharPredicate param1) {
      return param0.toLowerCase(Locale.ROOT)
         .chars()
         .mapToObj(l_param1 -> param1.test((char)l_param1) ? Character.toString((char)l_param1) : "_")
         .collect(Collectors.joining());
   }

   public static <K, V> SingleKeyCache<K, V> singleKeyCache(Function<K, V> param0) {
      return new SingleKeyCache<>(param0);
   }

   public static <T, R> Function<T, R> memoize(final Function<T, R> param0) {
      return new Function<T, R>() {
         private final Map<T, R> cache = new ConcurrentHashMap<>();

         @Override
         public R apply(T l_param0) {
            return this.cache.computeIfAbsent(l_param0, param0);
         }

         @Override
         public String toString() {
            return "memoize/1[function=" + param0 + ", size=" + this.cache.size() + "]";
         }
      };
   }

   public static <T, U, R> BiFunction<T, U, R> memoize(final BiFunction<T, U, R> param0) {
      return new BiFunction<T, U, R>() {
         private final Map<Pair<T, U>, R> cache = new ConcurrentHashMap<>();

         @Override
         public R apply(T l_param0, U l_param1) {
            return this.cache.computeIfAbsent(Pair.of(l_param0, l_param1), l2_param1 -> param0.apply(l2_param1.getFirst(), l2_param1.getSecond()));
         }

         @Override
         public String toString() {
            return "memoize/2[function=" + param0 + ", size=" + this.cache.size() + "]";
         }
      };
   }

   public static <T> List<T> toShuffledList(Stream<T> param0, RandomSource param1) {
      ObjectArrayList<T> var2 = param0.collect(ObjectArrayList.toList());
      shuffle(var2, param1);
      return var2;
   }

   public static IntArrayList toShuffledList(IntStream param0, RandomSource param1) {
      IntArrayList var2 = IntArrayList.wrap(param0.toArray());
      int var3 = var2.size();

      for (int var4 = var3; var4 > 1; var4--) {
         int var5 = param1.nextInt(var4);
         var2.set(var4 - 1, var2.set(var5, var2.getInt(var4 - 1)));
      }

      return var2;
   }

   public static <T> List<T> shuffledCopy(T[] param0, RandomSource param1) {
      ObjectArrayList<T> var2 = new ObjectArrayList<>(param0);
      shuffle(var2, param1);
      return var2;
   }

   public static <T> List<T> shuffledCopy(ObjectArrayList<T> param0, RandomSource param1) {
      ObjectArrayList<T> var2 = new ObjectArrayList<>(param0);
      shuffle(var2, param1);
      return var2;
   }

   public static <T> void shuffle(ObjectArrayList<T> param0, RandomSource param1) {
      int var2 = param0.size();

      for (int var3 = var2; var3 > 1; var3--) {
         int var4 = param1.nextInt(var3);
         param0.set(var3 - 1, param0.set(var4, param0.get(var3 - 1)));
      }
   }

   /**
    * Fisher-Yates over an arbitrary {@link List}.
    *
    * <p>This exists because {@code java.util.Collections.shuffle()} is unusable on WASM-GC: it
    * bottoms out in {@code java.util.random.RandomGenerator.nextInt(int)}, whose interface
    * dispatch has no vtable entry in this build and therefore compiles to an {@code unreachable}
    * trap. Reaching one kills the client outright, so every shuffle site has to go through
    * Minecraft's self-contained {@link RandomSource} instead.
    *
    * <p>Kept separate from {@link #shuffle(ObjectArrayList, RandomSource)}, which needs index
    * random access for its swap trick.
    */
   public static <T> void shuffleList(List<T> param0, RandomSource param1) {
      for (int var2 = param0.size() - 1; var2 > 0; var2--) {
         int var3 = param1.nextInt(var2 + 1);
         T var4 = param0.get(var2);
         param0.set(var2, param0.get(var3));
         param0.set(var3, var4);
      }
   }

   public static <T> CompletableFuture<T> blockUntilDone(Function<Executor, CompletableFuture<T>> param0) {
      return blockUntilDone(param0, CompletableFuture::isDone);
   }

   public static <T> T blockUntilDone(Function<Executor, T> param0, Predicate<T> param1) {
      BlockingQueue<Runnable> var2 = new LinkedBlockingQueue<>();
      T var3 = param0.apply(var2::add);

      while (!param1.test(var3)) {
         try {
            Runnable var4 = var2.poll(100L, TimeUnit.MILLISECONDS);
            if (var4 != null) {
               var4.run();
            }
         } catch (InterruptedException var4) {
            LOGGER.warn("Interrupted wait");
            break;
         }
      }

      int var4 = var2.size();
      if (var4 > 0) {
         LOGGER.warn("Tasks left in queue: {}", var4);
      }

      return var3;
   }

   public static <T> ToIntFunction<T> createIndexLookup(List<T> param0) {
      return createIndexLookup(param0, Object2IntOpenHashMap::new);
   }

   public static <T> ToIntFunction<T> createIndexLookup(List<T> param0, IntFunction<Object2IntMap<T>> param1) {
      Object2IntMap<T> var2 = param1.apply(param0.size());

      for (int var3 = 0; var3 < param0.size(); var3++) {
         var2.put(param0.get(var3), var3);
      }

      return var2;
   }

   public static <T, E extends Exception> T getOrThrow(DataResult<T> param0, Function<String, E> param1) throws E {
      Optional<PartialResult<T>> var2 = param0.error();
      if (var2.isPresent()) {
         throw param1.apply(var2.get().message());
      } else {
         return param0.result().orElseThrow();
      }
   }

   enum IdentityStrategy implements Strategy<Object> {
      INSTANCE;

      @Override
      public int hashCode(Object param0) {
         return System.identityHashCode(param0);
      }

      @Override
      public boolean equals(Object param0, Object param1) {
         return param0 == param1;
      }
   }

   public enum OS {
      LINUX("linux"),
      SOLARIS("solaris"),
      WINDOWS("windows") {
         @Override
         protected String[] getOpenUrlArguments(URL param0) {
            return new String[]{"rundll32", "url.dll,FileProtocolHandler", param0.toString()};
         }
      },
      OSX("mac") {
         @Override
         protected String[] getOpenUrlArguments(URL param0) {
            return new String[]{"open", param0.toString()};
         }
      },
      UNKNOWN("unknown");

      private final String telemetryName;

      OS(String param2) {
         this.telemetryName = param2;
      }

      public void openUrl(URL param0) {
         // Runtime.getRuntime().exec and java.lang.Process are both gone, which takes out two
         // link errors at once. Neither is on the WASM-GC TeaVM classlib, and neither can be:
         // a browser tab has no child processes, so there is nothing for exec to return.
         //
         // PlatformApplication.openLink is the port's existing equivalent and does the real
         // thing rather than logging and giving up. On the web targets it is a
         // window.open(url, "_blank", "noopener,noreferrer"), and it parses the URL through
         // java.net.URI first, refusing a malformed one -- a check the exec path never made.
         // Minecraft.java uses the same seam (PlatformApplication.showPopup) for the same
         // reason.
         //
         // There are no stream closes to port. Upstream closed all three to reap the child
         // process; with no child there are no streams, and closing something that does not
         // exist is not a behaviour worth reproducing.
         //
         // getOpenUrlArguments is left alone. It is the desktop command line each OS constant
         // builds, it is protected rather than private, and it is now unreachable -- which
         // means TeaVM simply never links it. Deleting the desktop reference is a separate
         // decision from making the web one work.
         PlatformApplication.openLink(param0.toString());
      }

      public void openUri(URI param0) {
         try {
            this.openUrl(param0.toURL());
         } catch (MalformedURLException var2) {
            Util.LOGGER.error("Couldn't open uri '{}'", param0, var2);
         }
      }

      public void openFile(File param0) {
         try {
            this.openUrl(param0.toURI().toURL());
         } catch (MalformedURLException var2) {
            Util.LOGGER.error("Couldn't open file '{}'", param0, var2);
         }
      }

      protected String[] getOpenUrlArguments(URL param0) {
         String var2 = param0.toString();
         if ("file".equals(param0.getProtocol())) {
            var2 = var2.replace("file:", "file://");
         }

         return new String[]{"xdg-open", var2};
      }

      public void openUri(String param0) {
         try {
            this.openUrl(new URI(param0).toURL());
         } catch (URISyntaxException | MalformedURLException | IllegalArgumentException var2) {
            Util.LOGGER.error("Couldn't open uri '{}'", param0, var2);
         }
      }

      public String telemetryName() {
         return this.telemetryName;
      }
   }
}
