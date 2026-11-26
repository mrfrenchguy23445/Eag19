package net.minecraft.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.WorldVersion;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

public class HashCache {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String HEADER_MARKER = "// ";
   private final Path rootDir;
   private final Path cacheDir;
   private final String versionId;
   private final Map<String, HashCache.ProviderCache> caches;
   private final Set<String> cachesToWrite = new HashSet<>();
   private final Set<Path> cachePaths = new HashSet<>();
   private final int initialCount;
   private int writes;

   private Path getProviderCachePath(String param0) {
      return this.cacheDir.resolve(Hashing.sha1().hashString(param0, StandardCharsets.UTF_8).toString());
   }

   public HashCache(Path param0, Collection<String> param1, WorldVersion param2) throws IOException {
      this.versionId = param2.getName();
      this.rootDir = param0;
      this.cacheDir = param0.resolve(".cache");
      Files.createDirectories(this.cacheDir);
      Map<String, HashCache.ProviderCache> var4 = new HashMap<>();
      int var5 = 0;

      for (String var7 : param1) {
         Path var8 = this.getProviderCachePath(var7);
         this.cachePaths.add(var8);
         HashCache.ProviderCache var9 = readCache(param0, var8);
         var4.put(var7, var9);
         var5 += var9.count();
      }

      this.caches = var4;
      this.initialCount = var5;
   }

   private static HashCache.ProviderCache readCache(Path param0, Path param1) {
      if (Files.isReadable(param1)) {
         try {
            return HashCache.ProviderCache.load(param0, param1);
         } catch (Exception var2) {
            LOGGER.warn("Failed to parse cache {}, discarding", param1, var2);
         }
      }

      return new HashCache.ProviderCache("unknown", ImmutableMap.of());
   }

   public boolean shouldRunInThisVersion(String param0) {
      HashCache.ProviderCache var2 = this.caches.get(param0);
      return var2 == null || !var2.version.equals(this.versionId);
   }

   public CompletableFuture<HashCache.UpdateResult> generateUpdate(String param0, HashCache.UpdateFunction param1) {
      HashCache.ProviderCache var3 = this.caches.get(param0);
      if (var3 == null) {
         throw new IllegalStateException("Provider not registered: " + param0);
      }

      HashCache.CacheUpdater var4 = new HashCache.CacheUpdater(param0, this.versionId, var3);
      return param1.update(var4).thenApply(l_param1 -> var4.close());
   }

   public void applyUpdate(HashCache.UpdateResult param0) {
      this.caches.put(param0.providerId(), param0.cache());
      this.cachesToWrite.add(param0.providerId());
      this.writes = this.writes + param0.writes();
   }

   public void purgeStaleAndWrite() throws IOException {
      Set<Path> var1 = new HashSet<>();
      this.caches.forEach((l_param1, l_param2) -> {
         if (this.cachesToWrite.contains(l_param1)) {
            Path l_var4 = this.getProviderCachePath(l_param1);
            l_param2.save(this.rootDir, l_var4, DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) + "\t" + l_param1);
         }

         var1.addAll(l_param2.data().keySet());
      });
      var1.add(this.rootDir.resolve("version.json"));
      MutableInt var2 = new MutableInt();
      MutableInt var3 = new MutableInt();

      try (Stream<Path> var4 = Files.walk(this.rootDir)) {
         var4.forEach(l_param3 -> {
            if (!Files.isDirectory(l_param3)) {
               if (!this.cachePaths.contains(l_param3)) {
                  var2.increment();
                  if (!var1.contains(l_param3)) {
                     try {
                        Files.delete(l_param3);
                     } catch (IOException l_var5) {
                        LOGGER.warn("Failed to delete file {}", l_param3, l_var5);
                     }

                     var3.increment();
                  }
               }
            }
         });
      }

      LOGGER.info(
         "Caching: total files: {}, old count: {}, new count: {}, removed stale: {}, written: {}", var2, this.initialCount, var1.size(), var3, this.writes
      );
   }

   class CacheUpdater implements CachedOutput {
      private final String provider;
      private final HashCache.ProviderCache oldCache;
      private final HashCache.ProviderCacheBuilder newCache;
      private final AtomicInteger writes = new AtomicInteger();
      private volatile boolean closed;

      CacheUpdater(String param1, String param2, HashCache.ProviderCache param3) {
         this.provider = param1;
         this.oldCache = param3;
         this.newCache = new HashCache.ProviderCacheBuilder(param2);
      }

      private boolean shouldWrite(Path param0, HashCode param1) {
         return !Objects.equals(this.oldCache.get(param0), param1) || !Files.exists(param0);
      }

      @Override
      public void writeIfNeeded(Path param0, byte[] param1, HashCode param2) throws IOException {
         if (this.closed) {
            throw new IllegalStateException("Cannot write to cache as it has already been closed");
         }

         if (this.shouldWrite(param0, param2)) {
            this.writes.incrementAndGet();
            Files.createDirectories(param0.getParent());
            Files.write(param0, param1);
         }

         this.newCache.put(param0, param2);
      }

      public HashCache.UpdateResult close() {
         this.closed = true;
         return new HashCache.UpdateResult(this.provider, this.newCache.build(), this.writes.get());
      }
   }

   record ProviderCache(String version, ImmutableMap<Path, HashCode> data) {

      @Nullable
      public HashCode get(Path param0) {
         return this.data.get(param0);
      }

      public int count() {
         return this.data.size();
      }

      public static HashCache.ProviderCache load(Path param0, Path param1) throws IOException {
         try (BufferedReader var2 = Files.newBufferedReader(param1, StandardCharsets.UTF_8)) {
            String var3 = var2.readLine();
            if (!var3.startsWith("// ")) {
               throw new IllegalStateException("Missing cache file header");
            }

            String[] var4 = var3.substring("// ".length()).split("\t", 2);
            String var5 = var4[0];
            Builder<Path, HashCode> var6 = ImmutableMap.builder();
            var2.lines().forEach(l_param2 -> {
               int l_var3 = l_param2.indexOf(32);
               var6.put(param0.resolve(l_param2.substring(l_var3 + 1)), HashCode.fromString(l_param2.substring(0, l_var3)));
            });
            return new HashCache.ProviderCache(var5, var6.build());
         }
      }

      public void save(Path param0, Path param1, String param2) {
         try (BufferedWriter var4 = Files.newBufferedWriter(param1, StandardCharsets.UTF_8)) {
            var4.write("// ");
            var4.write(this.version);
            var4.write(9);
            var4.write(param2);
            var4.newLine();

            for (Entry<Path, HashCode> var6 : this.data.entrySet()) {
               var4.write(var6.getValue().toString());
               var4.write(32);
               var4.write(param0.relativize(var6.getKey()).toString());
               var4.newLine();
            }
         } catch (IOException var4) {
            HashCache.LOGGER.warn("Unable write cachefile {}: {}", param1, var4);
         }
      }
   }

   record ProviderCacheBuilder(String version, ConcurrentMap<Path, HashCode> data) {
      ProviderCacheBuilder(String param0) {
         this(param0, new ConcurrentHashMap<>());
      }

      public void put(Path param0, HashCode param1) {
         this.data.put(param0, param1);
      }

      public HashCache.ProviderCache build() {
         return new HashCache.ProviderCache(this.version, ImmutableMap.copyOf(this.data));
      }
   }

   @FunctionalInterface
   public interface UpdateFunction {
      CompletableFuture<?> update(CachedOutput var1);
   }

   public record UpdateResult(String providerId, HashCache.ProviderCache cache, int writes) {
   }
}
