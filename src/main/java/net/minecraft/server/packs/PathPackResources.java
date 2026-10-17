package net.minecraft.server.packs;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

public class PathPackResources extends AbstractPackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Joiner PATH_JOINER = Joiner.on("/");
   private final Path root;

   public PathPackResources(String param0, Path param1, boolean param2) {
      super(param0, param2);
      this.root = param1;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... param0) {
      FileUtil.validatePath(param0);
      Path var2 = FileUtil.resolvePath(this.root, List.of(param0));
      return Files.exists(var2) ? IoSupplier.create(var2) : null;
   }

   public static boolean validatePath(Path param0) {
      return true;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType param0, ResourceLocation param1) {
      Path var3 = this.root.resolve(param0.getDirectory()).resolve(param1.getNamespace());
      return getResource(param1, var3);
   }

   public static IoSupplier<InputStream> getResource(ResourceLocation param0, Path param1) {
      return FileUtil.decomposePath(param0.getPath()).get().map(l_param1 -> {
         Path l_var2 = FileUtil.resolvePath(param1, (List<String>)l_param1);
         return returnFileIfExists(l_var2);
      }, l_param1 -> {
         LOGGER.error("Invalid path {}: {}", param0, l_param1.message());
         return null;
      });
   }

   @Nullable
   private static IoSupplier<InputStream> returnFileIfExists(Path param0) {
      return Files.exists(param0) && validatePath(param0) ? IoSupplier.create(param0) : null;
   }

   @Override
   public void listResources(PackType param0, String param1, String param2, PackResources.ResourceOutput param3) {
      FileUtil.decomposePath(param2).get().ifLeft(l_param3 -> {
         Path l_var5 = this.root.resolve(param0.getDirectory()).resolve(param1);
         listPath(param1, l_var5, (List<String>)l_param3, param3);
      }).ifRight(l_param1 -> LOGGER.error("Invalid path {}: {}", param2, l_param1.message()));
   }

   public static void listPath(String param0, Path param1, List<String> param2, PackResources.ResourceOutput param3) {
      Path var4 = FileUtil.resolvePath(param1, param2);

      try (Stream<Path> var5 = Files.find(var4, Integer.MAX_VALUE, (l_param0, l_param1) -> l_param1.isRegularFile())) {
         var5.forEach(l_param3 -> {
            String l_var4 = PATH_JOINER.join(param1.relativize(l_param3));
            ResourceLocation l_var5 = ResourceLocation.tryBuild(param0, l_var4);
            if (l_var5 == null) {
               Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", param0, l_var4));
            } else {
               param3.accept(l_var5, IoSupplier.create(l_param3));
            }
         });
      } catch (NoSuchFileException var10) {
      } catch (IOException var5) {
         LOGGER.error("Failed to list path {}", var4, var5);
      }
   }

   @Override
   public Set<String> getNamespaces(PackType param0) {
      Set<String> var2 = Sets.newHashSet();
      Path var3 = this.root.resolve(param0.getDirectory());

      try (DirectoryStream<Path> var4 = Files.newDirectoryStream(var3)) {
         for (Path var6 : var4) {
            String var7 = var6.getFileName().toString();
            if (var7.equals(var7.toLowerCase(Locale.ROOT))) {
               var2.add(var7);
            } else {
               LOGGER.warn("Ignored non-lowercase namespace: {} in {}", var7, this.root);
            }
         }
      } catch (NoSuchFileException var10) {
      } catch (IOException var4) {
         LOGGER.error("Failed to list path {}", var3, var4);
      }

      return var2;
   }

   @Override
   public void close() {
   }
}
