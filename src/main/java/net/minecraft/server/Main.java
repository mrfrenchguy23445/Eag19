package net.minecraft.server;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import net.minecraft.CrashReport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;

public class Main {
   private static final Logger LOGGER = LogUtils.getLogger();

   @DontObfuscate
   public static void main(String[] param0) {
      SharedConstants.tryDetectVersion();
      OptionParser var1 = new OptionParser();
      OptionSpec<Void> var2 = var1.accepts("nogui");
      OptionSpec<Void> var3 = var1.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
      OptionSpec<Void> var4 = var1.accepts("demo");
      OptionSpec<Void> var5 = var1.accepts("bonusChest");
      OptionSpec<Void> var6 = var1.accepts("forceUpgrade");
      OptionSpec<Void> var7 = var1.accepts("eraseCache");
      OptionSpec<Void> var8 = var1.accepts("safeMode", "Loads level with vanilla datapack only");
      OptionSpec<Void> var9 = var1.accepts("help").forHelp();
      OptionSpec<String> var10 = var1.accepts("singleplayer").withRequiredArg();
      OptionSpec<String> var11 = var1.accepts("universe").withRequiredArg().defaultsTo(".");
      OptionSpec<String> var12 = var1.accepts("world").withRequiredArg();
      OptionSpec<Integer> var13 = var1.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1);
      OptionSpec<String> var14 = var1.accepts("serverId").withRequiredArg();
      OptionSpec<Void> var15 = var1.accepts("jfrProfile");
      OptionSpec<Path> var16 = var1.accepts("pidFile").withRequiredArg().withValuesConvertedBy(new PathConverter());
      OptionSpec<String> var17 = var1.nonOptions();

      try {
         OptionSet var18 = var1.parse(param0);
         if (var18.has(var9)) {
            var1.printHelpOn(System.err);
            return;
         }

         Path var19 = var18.valueOf(var16);
         if (var19 != null) {
            writePidFile(var19);
         }

         CrashReport.preload();
         if (var18.has(var15)) {
            JvmProfiler.INSTANCE.start(Environment.SERVER);
         }

         Bootstrap.bootStrap();
         Bootstrap.validate();
         Util.startTimerHackThread();
         Path var20 = Paths.get("server.properties");
         DedicatedServerSettings var21 = new DedicatedServerSettings(var20);
         var21.forceSave();
         Path var22 = Paths.get("eula.txt");
         Eula var23 = new Eula(var22);
         if (var18.has(var3)) {
            LOGGER.info("Initialized '{}' and '{}'", var20.toAbsolutePath(), var22.toAbsolutePath());
            return;
         }

         if (!var23.hasAgreedToEULA()) {
            LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
            return;
         }

         File var24 = new File(var18.valueOf(var11));
         Services var25 = Services.create(new YggdrasilAuthenticationService(Proxy.NO_PROXY), var24);
         String var26 = Optional.ofNullable(var18.valueOf(var12)).orElse(var21.getProperties().levelName);
         LevelStorageSource var27 = LevelStorageSource.createDefault(var24.toPath());
         LevelStorageSource.LevelStorageAccess var28 = var27.createAccess(var26);
         LevelSummary var29 = var28.getSummary();
         if (var29 != null) {
            if (var29.requiresManualConversion()) {
               LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
               return;
            }

            if (!var29.isCompatible()) {
               LOGGER.info("This world was created by an incompatible version.");
               return;
            }
         }

         boolean var30 = var18.has(var8);
         if (var30) {
            LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
         }

         PackRepository var31 = ServerPacksSource.createPackRepository(var28.getLevelPath(LevelResource.DATAPACK_DIR));

         WorldStem var32;
         try {
            WorldLoader.InitConfig var33 = loadOrCreateConfig(var21.getProperties(), var28, var30, var31);
            var32 = Util.<WorldStem>blockUntilDone(
                  l_param6 -> WorldLoader.load(
                     var33,
                     l2_param5 -> {
                        Registry<LevelStem> l2_var6 = l2_param5.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
                        DynamicOps<Tag> l2_var7 = RegistryOps.create(NbtOps.INSTANCE, l2_param5.datapackWorldgen());
                        Pair<WorldData, WorldDimensions.Complete> l2_var8 = var28.getDataTag(
                           l2_var7, l2_param5.dataConfiguration(), l2_var6, l2_param5.datapackWorldgen().allRegistriesLifecycle()
                        );
                        if (l2_var8 != null) {
                           return new WorldLoader.DataLoadOutput<>(l2_var8.getFirst(), l2_var8.getSecond().dimensionsRegistryAccess());
                        }

                        LevelSettings l2_var9;
                        WorldOptions l2_var10;
                        WorldDimensions l2_var11;
                        if (var18.has(var4)) {
                           l2_var9 = MinecraftServer.DEMO_SETTINGS;
                           l2_var10 = WorldOptions.DEMO_OPTIONS;
                           l2_var11 = WorldPresets.createNormalWorldDimensions(l2_param5.datapackWorldgen());
                        } else {
                           DedicatedServerProperties l2_var12 = var21.getProperties();
                           l2_var9 = new LevelSettings(
                              l2_var12.levelName,
                              l2_var12.gamemode,
                              l2_var12.hardcore,
                              l2_var12.difficulty,
                              false,
                              new GameRules(),
                              l2_param5.dataConfiguration()
                           );
                           l2_var10 = var18.has(var5) ? l2_var12.worldOptions.withBonusChest(true) : l2_var12.worldOptions;
                           l2_var11 = l2_var12.createDimensions(l2_param5.datapackWorldgen());
                        }

                        WorldDimensions.Complete l2_var12 = l2_var11.bake(l2_var6);
                        Lifecycle l2_var13 = l2_var12.lifecycle().add(l2_param5.datapackWorldgen().allRegistriesLifecycle());
                        return new WorldLoader.DataLoadOutput<>(
                           new PrimaryLevelData(l2_var9, l2_var10, l2_var12.specialWorldProperty(), l2_var13), l2_var12.dimensionsRegistryAccess()
                        );
                     },
                     WorldStem::new,
                     Util.backgroundExecutor(),
                     l_param6
                  )
               )
               .get();
         } catch (Exception var33) {
            LOGGER.warn(
               "Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode", var33
            );
            return;
         }

         RegistryAccess.Frozen var33 = var32.registries().compositeAccess();
         if (var18.has(var6)) {
            forceUpgrade(var28, DataFixers.getDataFixer(), var18.has(var7), () -> true, var33.registryOrThrow(Registries.LEVEL_STEM));
         }

         WorldData var34 = var32.worldData();
         var28.saveDataTag(var33, var34);
         final DedicatedServer var35 = MinecraftServer.spin(
            l_param12 -> {
               DedicatedServer l_var13 = new DedicatedServer(
                  l_param12, var28, var31, var32, var21, DataFixers.getDataFixer(), var25, LoggerChunkProgressListener::new
               );
               l_var13.setSingleplayerProfile(var18.has(var10) ? new GameProfile(null, var18.valueOf(var10)) : null);
               l_var13.setPort(var18.valueOf(var13));
               l_var13.setDemo(var18.has(var4));
               l_var13.setId(var18.valueOf(var14));
               boolean l_var14 = !var18.has(var2) && !var18.valuesOf(var17).contains("nogui");
               if (l_var14 && !GraphicsEnvironment.isHeadless()) {
                  l_var13.showGui();
               }

               return l_var13;
            }
         );
         Thread var36 = new Thread("Server Shutdown Thread") {
            @Override
            public void run() {
               var35.halt(true);
            }
         };
         var36.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
         Runtime.getRuntime().addShutdownHook(var36);
      } catch (Exception var18) {
         LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", var18);
      }
   }

   private static void writePidFile(Path param0) {
      try {
         long var1 = ProcessHandle.current().pid();
         Files.writeString(param0, Long.toString(var1));
      } catch (IOException var1) {
         throw new UncheckedIOException(var1);
      }
   }

   private static WorldLoader.InitConfig loadOrCreateConfig(
      DedicatedServerProperties param0, LevelStorageSource.LevelStorageAccess param1, boolean param2, PackRepository param3
   ) {
      WorldDataConfiguration var4 = param1.getDataConfiguration();
      WorldDataConfiguration var5;
      boolean var6;
      if (var4 != null) {
         var6 = false;
         var5 = var4;
      } else {
         var6 = true;
         var5 = new WorldDataConfiguration(param0.initialDataPackConfiguration, FeatureFlags.DEFAULT_FLAGS);
      }

      WorldLoader.PackConfig var7 = new WorldLoader.PackConfig(param3, var5, param2, var6);
      return new WorldLoader.InitConfig(var7, Commands.CommandSelection.DEDICATED, param0.functionPermissionLevel);
   }

   private static void forceUpgrade(
      LevelStorageSource.LevelStorageAccess param0, DataFixer param1, boolean param2, BooleanSupplier param3, Registry<LevelStem> param4
   ) {
      LOGGER.info("Forcing world upgrade!");
      WorldUpgrader var5 = new WorldUpgrader(param0, param1, param4, param2);
      Component var6 = null;

      while (!var5.isFinished()) {
         Component var7 = var5.getStatus();
         if (var6 != var7) {
            var6 = var7;
            LOGGER.info(var5.getStatus().getString());
         }

         int var8 = var5.getTotalChunks();
         if (var8 > 0) {
            int var9 = var5.getConverted() + var5.getSkipped();
            LOGGER.info("{}% completed ({} / {} chunks)...", Mth.floor((float)var9 / var8 * 100.0F), var9, var8);
         }

         if (!param3.getAsBoolean()) {
            var5.cancel();
         } else {
            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var10) {
            }
         }
      }
   }
}
