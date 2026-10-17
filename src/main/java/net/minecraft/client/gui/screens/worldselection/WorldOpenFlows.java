package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;

public class WorldOpenFlows {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final LevelStorageSource levelSource;

   public WorldOpenFlows(Minecraft param0, LevelStorageSource param1) {
      this.minecraft = param0;
      this.levelSource = param1;
   }

   public void loadLevel(Screen param0, String param1) {
      this.doLoadLevel(param0, param1, false, true);
   }

   public void createFreshLevel(String param0, LevelSettings param1, WorldOptions param2, Function<RegistryAccess, WorldDimensions> param3) {
      LevelStorageSource.LevelStorageAccess var5 = this.createWorldAccess(param0);
      if (var5 != null) {
         PackRepository var6 = ServerPacksSource.createPackRepository(var5);
         WorldDataConfiguration var7 = param1.getDataConfiguration();

         try {
            WorldLoader.PackConfig var8 = new WorldLoader.PackConfig(var6, var7, false, false);
            WorldStem var9 = this.loadWorldDataBlocking(
               var8,
               l_param3 -> {
                  WorldDimensions.Complete l_var4 = param3.apply(l_param3.datapackWorldgen())
                     .bake(l_param3.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM));
                  return new WorldLoader.DataLoadOutput<>(
                     new PrimaryLevelData(param1, param2, l_var4.specialWorldProperty(), l_var4.lifecycle()), l_var4.dimensionsRegistryAccess()
                  );
               },
               WorldStem::new
            );
            this.minecraft.doWorldLoad(param0, var5, var6, var9, true);
         } catch (Exception var8) {
            LOGGER.warn("Failed to load datapacks, can't proceed with server load", var8);
            safeCloseAccess(var5, param0);
         }
      }
   }

   @Nullable
   private LevelStorageSource.LevelStorageAccess createWorldAccess(String param0) {
      try {
         return this.levelSource.createAccess(param0);
      } catch (IOException var2) {
         LOGGER.warn("Failed to read level {} data", param0, var2);
         SystemToast.onWorldAccessFailure(this.minecraft, param0);
         this.minecraft.setScreen(null);
         return null;
      }
   }

   public void createLevelFromExistingSettings(
      LevelStorageSource.LevelStorageAccess param0, ReloadableServerResources param1, LayeredRegistryAccess<RegistryLayer> param2, WorldData param3
   ) {
      PackRepository var5 = ServerPacksSource.createPackRepository(param0);
      CloseableResourceManager var6 = new WorldLoader.PackConfig(var5, param3.getDataConfiguration(), false, false).createResourceManager().getSecond();
      this.minecraft.doWorldLoad(param0.getLevelId(), param0, var5, new WorldStem(var6, param1, param2, param3), true);
   }

   private WorldStem loadWorldStem(LevelStorageSource.LevelStorageAccess param0, boolean param1, PackRepository param2) throws Exception {
      WorldLoader.PackConfig var4 = this.getPackConfigFromLevelData(param0, param1, param2);
      return this.loadWorldDataBlocking(
         var4,
         l_param1 -> {
            DynamicOps<Tag> l_var2 = RegistryOps.create(NbtOps.INSTANCE, l_param1.datapackWorldgen());
            Registry<LevelStem> l_var3 = l_param1.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
            Pair<WorldData, WorldDimensions.Complete> l_var4 = param0.getDataTag(
               l_var2, l_param1.dataConfiguration(), l_var3, l_param1.datapackWorldgen().allRegistriesLifecycle()
            );
            if (l_var4 == null) {
               throw new IllegalStateException("Failed to load world");
            } else {
               return new WorldLoader.DataLoadOutput<>(l_var4.getFirst(), l_var4.getSecond().dimensionsRegistryAccess());
            }
         },
         WorldStem::new
      );
   }

   public Pair<LevelSettings, WorldCreationContext> recreateWorldData(LevelStorageSource.LevelStorageAccess param0) throws Exception {
      PackRepository var2 = ServerPacksSource.createPackRepository(param0);
      WorldLoader.PackConfig var3 = this.getPackConfigFromLevelData(param0, false, var2);

      record Data(LevelSettings levelSettings, WorldOptions options, Registry<LevelStem> existingDimensions) {
      }

      return this.loadWorldDataBlocking(
         var3,
         l_param1 -> {
            DynamicOps<Tag> l_var2 = RegistryOps.create(NbtOps.INSTANCE, l_param1.datapackWorldgen());
            Registry<LevelStem> l_var3 = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
            Pair<WorldData, WorldDimensions.Complete> l_var4 = param0.getDataTag(
               l_var2, l_param1.dataConfiguration(), l_var3, l_param1.datapackWorldgen().allRegistriesLifecycle()
            );
            if (l_var4 == null) {
               throw new IllegalStateException("Failed to load world");
            } else {
               return new WorldLoader.DataLoadOutput<>(
                  new Data(l_var4.getFirst().getLevelSettings(), l_var4.getFirst().worldGenOptions(), l_var4.getSecond().dimensions()),
                  l_param1.datapackDimensions()
               );
            }
         },
         (l_param0, l_param1, l_param2, l_param3) -> {
            l_param0.close();
            return Pair.of(
               l_param3.levelSettings,
               new WorldCreationContext(
                  l_param3.options, new WorldDimensions(l_param3.existingDimensions), l_param2, l_param1, l_param3.levelSettings.getDataConfiguration()
               )
            );
         }
      );
   }

   private WorldLoader.PackConfig getPackConfigFromLevelData(LevelStorageSource.LevelStorageAccess param0, boolean param1, PackRepository param2) {
      WorldDataConfiguration var4 = param0.getDataConfiguration();
      if (var4 == null) {
         throw new IllegalStateException("Failed to load data pack config");
      } else {
         return new WorldLoader.PackConfig(param2, var4, param1, false);
      }
   }

   public WorldStem loadWorldStem(LevelStorageSource.LevelStorageAccess param0, boolean param1) throws Exception {
      PackRepository var3 = ServerPacksSource.createPackRepository(param0);
      return this.loadWorldStem(param0, param1, var3);
   }

   private <D, R> R loadWorldDataBlocking(WorldLoader.PackConfig param0, WorldLoader.WorldDataSupplier<D> param1, WorldLoader.ResultFactory<D, R> param2) throws Exception {
      WorldLoader.InitConfig var4 = new WorldLoader.InitConfig(param0, Commands.CommandSelection.INTEGRATED, 2);
      CompletableFuture<R> var5 = WorldLoader.load(var4, param1, param2, Util.backgroundExecutor(), this.minecraft);
      this.minecraft.managedBlock(var5::isDone);
      return var5.get();
   }

   private void doLoadLevel(Screen param0, String param1, boolean param2, boolean param3) {
      LevelStorageSource.LevelStorageAccess var5 = this.createWorldAccess(param1);
      if (var5 != null) {
         PackRepository var6 = ServerPacksSource.createPackRepository(var5);

         WorldStem var7;
         try {
            var7 = this.loadWorldStem(var5, param2, var6);
         } catch (Exception var8) {
            LOGGER.warn("Failed to load level data or datapacks, can't proceed with server load", var8);
            if (!param2) {
               this.minecraft.setScreen(new DatapackLoadFailureScreen(() -> this.doLoadLevel(param0, param1, true, param3)));
            } else {
               this.minecraft
                  .setScreen(
                     new AlertScreen(
                        () -> this.minecraft.setScreen(null),
                        Component.translatable("datapackFailure.safeMode.failed.title"),
                        Component.translatable("datapackFailure.safeMode.failed.description"),
                        CommonComponents.GUI_TO_TITLE,
                        true
                     )
                  );
            }

            safeCloseAccess(var5, param1);
            return;
         }

         WorldData var8 = var7.worldData();
         boolean var9 = var8.worldGenOptions().isOldCustomizedWorld();
         boolean var10 = var8.worldGenSettingsLifecycle() != Lifecycle.stable();
         if (!param3 || !var9 && !var10) {
            this.minecraft.getDownloadedPackSource().loadBundledResourcePack(var5).thenApply(l_param0 -> true).exceptionallyComposeAsync(l_param0 -> {
               LOGGER.warn("Failed to load pack: ", l_param0);
               return this.promptBundledPackLoadFailure();
            }, this.minecraft).thenAcceptAsync(l_param5 -> {
               if (l_param5) {
                  this.minecraft.doWorldLoad(param1, var5, var6, var7, false);
               } else {
                  var7.close();
                  safeCloseAccess(var5, param1);
                  this.minecraft.getDownloadedPackSource().clearServerPack().thenRunAsync(() -> this.minecraft.setScreen(param0), this.minecraft);
               }
            }, this.minecraft).exceptionally(l_param0 -> {
               this.minecraft.delayCrash(CrashReport.forThrowable(l_param0, "Load world"));
               return null;
            });
         } else {
            this.askForBackup(param0, param1, var9, () -> this.doLoadLevel(param0, param1, param2, false));
            var7.close();
            safeCloseAccess(var5, param1);
         }
      }
   }

   private CompletableFuture<Boolean> promptBundledPackLoadFailure() {
      CompletableFuture<Boolean> var1 = new CompletableFuture<>();
      this.minecraft
         .setScreen(
            new ConfirmScreen(
               var1::complete,
               Component.translatable("multiplayer.texturePrompt.failure.line1"),
               Component.translatable("multiplayer.texturePrompt.failure.line2"),
               CommonComponents.GUI_PROCEED,
               CommonComponents.GUI_CANCEL
            )
         );
      return var1;
   }

   private static void safeCloseAccess(LevelStorageSource.LevelStorageAccess param0, String param1) {
      try {
         param0.close();
      } catch (IOException var2) {
         LOGGER.warn("Failed to unlock access to level {}", param1, var2);
      }
   }

   private void askForBackup(Screen param0, String param1, boolean param2, Runnable param3) {
      Component var5;
      Component var6;
      if (param2) {
         var5 = Component.translatable("selectWorld.backupQuestion.customized");
         var6 = Component.translatable("selectWorld.backupWarning.customized");
      } else {
         var5 = Component.translatable("selectWorld.backupQuestion.experimental");
         var6 = Component.translatable("selectWorld.backupWarning.experimental");
      }

      this.minecraft.setScreen(new BackupConfirmScreen(param0, (l_param2, l_param3) -> {
         if (l_param2) {
            EditWorldScreen.makeBackupAndShowToast(this.levelSource, param1);
         }

         param3.run();
      }, var5, var6, false));
   }

   public static void confirmWorldCreation(Minecraft param0, CreateWorldScreen param1, Lifecycle param2, Runnable param3, boolean param4) {
      BooleanConsumer var5 = l_param3 -> {
         if (l_param3) {
            param3.run();
         } else {
            param0.setScreen(param1);
         }
      };
      if (param4 || param2 == Lifecycle.stable()) {
         param3.run();
      } else if (param2 == Lifecycle.experimental()) {
         param0.setScreen(
            new ConfirmScreen(
               var5, Component.translatable("selectWorld.warning.experimental.title"), Component.translatable("selectWorld.warning.experimental.question")
            )
         );
      } else {
         param0.setScreen(
            new ConfirmScreen(
               var5, Component.translatable("selectWorld.warning.deprecated.title"), Component.translatable("selectWorld.warning.deprecated.question")
            )
         );
      }
   }
}
