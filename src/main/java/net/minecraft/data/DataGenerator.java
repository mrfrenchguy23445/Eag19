package net.minecraft.data;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.WorldVersion;
import net.minecraft.server.Bootstrap;
import org.slf4j.Logger;

public class DataGenerator {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Path rootOutputFolder;
   private final PackOutput vanillaPackOutput;
   final Set<String> allProviderIds = new HashSet<>();
   final Map<String, DataProvider> providersToRun = new LinkedHashMap<>();
   private final WorldVersion version;
   private final boolean alwaysGenerate;

   public DataGenerator(Path param0, WorldVersion param1, boolean param2) {
      this.rootOutputFolder = param0;
      this.vanillaPackOutput = new PackOutput(this.rootOutputFolder);
      this.version = param1;
      this.alwaysGenerate = param2;
   }

   public void run() throws IOException {
      HashCache var1 = new HashCache(this.rootOutputFolder, this.allProviderIds, this.version);
      Stopwatch var2 = Stopwatch.createStarted();
      Stopwatch var3 = Stopwatch.createUnstarted();
      this.providersToRun.forEach((l_param2, l_param3) -> {
         if (!this.alwaysGenerate && !var1.shouldRunInThisVersion(l_param2)) {
            LOGGER.debug("Generator {} already run for version {}", l_param2, this.version.getName());
         } else {
            LOGGER.info("Starting provider: {}", l_param2);
            var3.start();
            var1.applyUpdate(var1.generateUpdate(l_param2, l_param3::run).join());
            var3.stop();
            LOGGER.info("{} finished after {} ms", l_param2, var3.elapsed(TimeUnit.MILLISECONDS));
            var3.reset();
         }
      });
      LOGGER.info("All providers took: {} ms", var2.elapsed(TimeUnit.MILLISECONDS));
      var1.purgeStaleAndWrite();
   }

   public DataGenerator.PackGenerator getVanillaPack(boolean param0) {
      return new DataGenerator.PackGenerator(param0, "vanilla", this.vanillaPackOutput);
   }

   public DataGenerator.PackGenerator getBuiltinDatapack(boolean param0, String param1) {
      Path var3 = this.vanillaPackOutput.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("minecraft").resolve("datapacks").resolve(param1);
      return new DataGenerator.PackGenerator(param0, param1, new PackOutput(var3));
   }

   static {
      Bootstrap.bootStrap();
   }

   public class PackGenerator {
      private final boolean toRun;
      private final String providerPrefix;
      private final PackOutput output;

      PackGenerator(boolean param1, String param2, PackOutput param3) {
         this.toRun = param1;
         this.providerPrefix = param2;
         this.output = param3;
      }

      public <T extends DataProvider> T addProvider(DataProvider.Factory<T> param0) {
         T var2 = param0.create(this.output);
         String var3 = this.providerPrefix + "/" + var2.getName();
         if (!DataGenerator.this.allProviderIds.add(var3)) {
            throw new IllegalStateException("Duplicate provider: " + var3);
         }

         if (this.toRun) {
            DataGenerator.this.providersToRun.put(var3, var2);
         }

         return var2;
      }
   }
}
