package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import org.slf4j.Logger;

public class FolderRepositorySource implements RepositorySource {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Path folder;
   private final PackType packType;
   private final PackSource packSource;

   public FolderRepositorySource(Path param0, PackType param1, PackSource param2) {
      this.folder = param0;
      this.packType = param1;
      this.packSource = param2;
   }

   private static String nameFromPath(Path param0) {
      return param0.getFileName().toString();
   }

   @Override
   public void loadPacks(Consumer<Pack> param0) {
      try {
         FileUtil.createDirectoriesSafe(this.folder);
         discoverPacks(
            this.folder,
            false,
            (l_param1, l_param2) -> {
               String l_var4 = nameFromPath(l_param1);
               Pack l_var5 = Pack.readMetaAndCreate(
                  "file/" + l_var4, Component.literal(l_var4), false, l_param2, this.packType, Pack.Position.TOP, this.packSource
               );
               if (l_var5 != null) {
                  param0.accept(l_var5);
               }
            }
         );
      } catch (IOException var2) {
         LOGGER.warn("Failed to list packs in {}", this.folder, var2);
      }
   }

   public static void discoverPacks(Path param0, boolean param1, BiConsumer<Path, Pack.ResourcesSupplier> param2) throws IOException {
      try (DirectoryStream<Path> var3 = Files.newDirectoryStream(param0)) {
         for (Path var5 : var3) {
            Pack.ResourcesSupplier var6 = detectPackResources(var5, param1);
            if (var6 != null) {
               param2.accept(var5, var6);
            }
         }
      }
   }

   @Nullable
   public static Pack.ResourcesSupplier detectPackResources(Path param0, boolean param1) {
      BasicFileAttributes var2;
      try {
         var2 = Files.readAttributes(param0, BasicFileAttributes.class);
      } catch (NoSuchFileException var3) {
         return null;
      } catch (IOException var3) {
         LOGGER.warn("Failed to read properties of '{}', ignoring", param0, var3);
         return null;
      }

      if (var2.isDirectory() && Files.isRegularFile(param0.resolve("pack.mcmeta"))) {
         return l_param2 -> new PathPackResources(l_param2, param0, param1);
      }

      if (var2.isRegularFile() && param0.getFileName().toString().endsWith(".zip")) {
         FileSystem var3 = param0.getFileSystem();
         if (var3 == FileSystems.getDefault() || var3 instanceof LinkFileSystem) {
            File var4 = param0.toFile();
            return l_param2 -> new FilePackResources(l_param2, var4, param1);
         }
      }

      LOGGER.info("Found non-pack entry '{}', ignoring", param0);
      return null;
   }
}
