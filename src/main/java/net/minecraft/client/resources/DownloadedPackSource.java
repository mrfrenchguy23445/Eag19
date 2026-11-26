package net.minecraft.client.resources;

import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.lax1dude.eaglercraft.v1_8.FileUtils;
import org.slf4j.Logger;

public class DownloadedPackSource implements RepositorySource {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
   private static final int MAX_PACK_SIZE_BYTES = 262144000;
   private static final int MAX_KEPT_PACKS = 10;
   private static final String SERVER_ID = "server";
   private static final Component SERVER_NAME = Component.translatable("resourcePack.server.name");
   private static final Component APPLYING_PACK_TEXT = Component.translatable("multiplayer.applyingPack");
   private final File serverPackDir;
   private final ReentrantLock downloadLock = new ReentrantLock();
   @Nullable
   private CompletableFuture<?> currentDownload;
   @Nullable
   private Pack serverPack;

   public DownloadedPackSource(File param0) {
      this.serverPackDir = param0;
   }

   @Override
   public void loadPacks(Consumer<Pack> param0) {
      if (this.serverPack != null) {
         param0.accept(this.serverPack);
      }
   }

   private static Map<String, String> getDownloadHeaders() {
      return Map.of(
         "X-Minecraft-Username",
         Minecraft.getInstance().getUser().getName(),
         "X-Minecraft-UUID",
         Minecraft.getInstance().getUser().getUuid(),
         "X-Minecraft-Version",
         SharedConstants.getCurrentVersion().getName(),
         "X-Minecraft-Version-ID",
         SharedConstants.getCurrentVersion().getId(),
         "X-Minecraft-Pack-Format",
         String.valueOf(SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES)),
         "User-Agent",
         "Minecraft Java/" + SharedConstants.getCurrentVersion().getName()
      );
   }

   public CompletableFuture<?> downloadAndSelectResourcePack(URL param0, String param1, boolean param2) {
      String var4 = Hashing.sha1().hashString(param0.toString(), StandardCharsets.UTF_8).toString();
      String var5 = SHA1.matcher(param1).matches() ? param1 : "";
      this.downloadLock.lock();

      try {
         Minecraft var6 = Minecraft.getInstance();
         File var7 = new File(this.serverPackDir, var4);
         CompletableFuture<?> var8;
         if (var7.exists()) {
            var8 = CompletableFuture.completedFuture("");
         } else {
            ProgressScreen var9 = new ProgressScreen(param2);
            Map<String, String> var10 = getDownloadHeaders();
            var6.executeBlocking(() -> var6.setScreen(var9));
            var8 = HttpUtil.downloadTo(var7, param0, var10, 262144000, var9, var6.getProxy());
         }

         this.currentDownload = var8.<Void>thenCompose(l_param4 -> {
               if (!this.checkHash(var5, var7)) {
                  return CompletableFuture.failedFuture(new RuntimeException("Hash check failure for file " + var7 + ", see log"));
               }

               var6.execute(() -> {
                  if (!param2) {
                     var6.setScreen(new GenericDirtMessageScreen(APPLYING_PACK_TEXT));
                  }
               });
               return this.setServerPack(var7, PackSource.SERVER);
            })
            .exceptionallyCompose(
               l_param2 -> this.clearServerPack()
                  .thenAcceptAsync(l2_param2 -> {
                     LOGGER.warn("Pack application failed: {}, deleting file {}", l_param2.getMessage(), var7);
                     deleteQuietly(var7);
                  }, Util.ioPool())
                  .thenAcceptAsync(
                     l2_param1 -> var6.setScreen(
                        new ConfirmScreen(
                           l3_param1 -> {
                              if (l3_param1) {
                                 var6.setScreen(null);
                              } else {
                                 ClientPacketListener l3_var2 = var6.getConnection();
                                 if (l3_var2 != null) {
                                    l3_var2.getConnection().disconnect(Component.translatable("connect.aborted"));
                                 }
                              }
                           },
                           Component.translatable("multiplayer.texturePrompt.failure.line1"),
                           Component.translatable("multiplayer.texturePrompt.failure.line2"),
                           CommonComponents.GUI_PROCEED,
                           Component.translatable("menu.disconnect")
                        )
                     ),
                     var6
                  )
            )
            .thenAcceptAsync(l_param0 -> this.clearOldDownloads(), Util.ioPool());
         return this.currentDownload;
      } finally {
         this.downloadLock.unlock();
      }
   }

   private static void deleteQuietly(File param0) {
      try {
         Files.delete(Util.toPath(param0));
      } catch (IOException var1) {
         LOGGER.warn("Failed to delete file {}: {}", param0, var1.getMessage());
      }
   }

   public CompletableFuture<Void> clearServerPack() {
      this.downloadLock.lock();

      try {
         if (this.currentDownload != null) {
            this.currentDownload.cancel(true);
         }

         this.currentDownload = null;
         if (this.serverPack != null) {
            this.serverPack = null;
            return Minecraft.getInstance().delayTextureReload();
         }
      } finally {
         this.downloadLock.unlock();
      }

      return CompletableFuture.completedFuture(null);
   }

   private boolean checkHash(String param0, File param1) {
      try {
         String var3 = com.google.common.io.Files.asByteSource(param1).hash(Hashing.sha1()).toString();
         if (param0.isEmpty()) {
            LOGGER.info("Found file {} without verification hash", param1);
            return true;
         }

         if (var3.toLowerCase(Locale.ROOT).equals(param0.toLowerCase(Locale.ROOT))) {
            LOGGER.info("Found file {} matching requested hash {}", param1, param0);
            return true;
         }

         LOGGER.warn("File {} had wrong hash (expected {}, found {}).", param1, param0, var3);
      } catch (IOException var3) {
         LOGGER.warn("File {} couldn't be hashed.", param1, var3);
      }

      return false;
   }

   private void clearOldDownloads() {
      if (this.serverPackDir.isDirectory()) {
         try {
            // Was FileUtils.listFiles(dir, TrueFileFilter.TRUE, null), sorted by
            // LastModifiedFileComparator.LASTMODIFIED_REVERSE. Both of those were the only use of
            // commons-io's comparator and filefilter subpackages in the whole game, so they went
            // with the FileUtils swap: the filter accepted everything and the comparator is
            // newest-first, which is a lambda each. The try/catch stays -- listFiles on an
            // unreadable directory returns an empty list rather than throwing, so what it still
            // catches is the sort, and swallowing that is no worse than before.
            List<File> var1 = new ArrayList<>(FileUtils.listFiles(this.serverPackDir, file -> true));
            var1.sort((file, other) -> Long.compare(other.lastModified(), file.lastModified()));
            int var2 = 0;

            for (File var4 : var1) {
               if (var2++ >= 10) {
                  LOGGER.info("Deleting old server resource pack {}", var4.getName());
                  FileUtils.deleteQuietly(var4);
               }
            }
         } catch (Exception var1) {
            LOGGER.error("Error while deleting old server resource pack : {}", var1.getMessage());
         }
      }
   }

   public CompletableFuture<Void> setServerPack(File param0, PackSource param1) {
      Pack.ResourcesSupplier var3 = l_param1 -> new FilePackResources(l_param1, param0, false);
      Pack.Info var4 = Pack.readPackInfo("server", var3);
      if (var4 == null) {
         return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid pack metadata at " + param0));
      }

      LOGGER.info("Applying server pack {}", param0);
      this.serverPack = Pack.create("server", SERVER_NAME, true, var3, var4, PackType.CLIENT_RESOURCES, Pack.Position.TOP, true, param1);
      return Minecraft.getInstance().delayTextureReload();
   }

   public CompletableFuture<Void> loadBundledResourcePack(LevelStorageSource.LevelStorageAccess param0) {
      Path var2 = param0.getLevelPath(LevelResource.MAP_RESOURCE_FILE);
      return Files.exists(var2) && !Files.isDirectory(var2) ? this.setServerPack(var2.toFile(), PackSource.WORLD) : CompletableFuture.completedFuture(null);
   }
}
