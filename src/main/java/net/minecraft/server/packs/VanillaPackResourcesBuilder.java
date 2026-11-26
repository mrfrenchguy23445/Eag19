package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.Util;
import org.slf4j.Logger;

public class VanillaPackResourcesBuilder {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static Consumer<VanillaPackResourcesBuilder> developmentConfig = param0 -> {};
   private static final Map<PackType, Path> ROOT_DIR_BY_TYPE = Util.make(() -> {
      synchronized (VanillaPackResources.class) {
         Builder<PackType, Path> var1 = ImmutableMap.builder();

         for (PackType var5 : PackType.values()) {
            String var6 = "/" + var5.getDirectory() + "/.mcassetsroot";
            // Class.getResource does not exist on the WASM-GC classlib's Class (TClass has
            // getResourceAsStream, but no getResource returning a URL). It would have
            // answered null here regardless: there is no .mcassetsroot on the Eaglercraft
            // classpath, because vanilla assets are served out of assets.epk by the
            // resource-pack layer rather than from a classpath directory. So ROOT_DIR_BY_TYPE
            // stays empty and every asset read is served by the EPK, which is the only
            // source this client has. The lookup is kept as written out rather than deleted,
            // so the shape of the upstream method is still recognisable.
            URL var7 = null;
            if (var7 == null) {
               LOGGER.debug("No classpath root for pack type {}; assets come from assets.epk", var5.getDirectory());
            } else {
               try {
                  URI var8 = var7.toURI();
                  String var9 = var8.getScheme();
                  if (!"jar".equals(var9) && !"file".equals(var9)) {
                     LOGGER.warn("Assets URL '{}' uses unexpected schema", var8);
                  }

                  Path var10 = safeGetPath(var8);
                  var1.put(var5, var10.getParent());
               } catch (Exception var8) {
                  LOGGER.error("Couldn't resolve path to vanilla assets", var8);
               }
            }
         }

         return var1.build();
      }
   });
   private final Set<Path> rootPaths = new LinkedHashSet<>();
   private final Map<PackType, Set<Path>> pathsForType = new EnumMap<>(PackType.class);
   private BuiltInMetadata metadata = BuiltInMetadata.of();
   private final Set<String> namespaces = new HashSet<>();

   /**
    * Mounts a zip filesystem for {@code param0} and retries, or logs and retries.
    *
    * <p>Upstream's version catches {@code FileSystemNotFoundException} from the first
    * {@code Paths.get}, mounts the zip filesystem with {@code FileSystems.newFileSystem}, and
    * calls {@code Paths.get} again. That mounting step is gone. {@code FileSystemProvider}
    * is not modelled by this port (see {@code TFileSystem}'s header, which records the same
    * decision for the same reason), and more to the point nothing can need it here: the only
    * caller passes a {@code jar:} URL for the game's own classpath resources, and on the
    * browser client those are not jars at all -- they are {@code assets.epk}, read by the
    * resource loader. The one caller already has {@code var7} hard-null with a comment
    * saying so, so this method is not reached at all; it is kept only so the upstream shape
    * of the enclosing method stays recognisable.
    *
    * <p>What is left is upstream's behaviour for every URL that is not a jar: try once, and
    * on any failure say which URL failed and why, then try once more.
    */
   private static Path safeGetPath(URI param0) throws IOException {
      try {
         return Paths.get(param0);
      } catch (Throwable var1) {
         LOGGER.warn("Unable to get path for: {}", param0, var1);
      }

      return Paths.get(param0);
   }

   private boolean validateDirPath(Path param0) {
      if (!Files.exists(param0)) {
         return false;
      } else if (!Files.isDirectory(param0)) {
         throw new IllegalArgumentException("Path " + param0.toAbsolutePath() + " is not directory");
      } else {
         return true;
      }
   }

   private void pushRootPath(Path param0) {
      if (this.validateDirPath(param0)) {
         this.rootPaths.add(param0);
      }
   }

   private void pushPathForType(PackType param0, Path param1) {
      if (this.validateDirPath(param1)) {
         this.pathsForType.computeIfAbsent(param0, l_param0 -> new LinkedHashSet<>()).add(param1);
      }
   }

   public VanillaPackResourcesBuilder pushJarResources() {
      ROOT_DIR_BY_TYPE.forEach((l_param0, l_param1) -> {
         this.pushRootPath(l_param1.getParent());
         this.pushPathForType(l_param0, l_param1);
      });
      return this;
   }

   public VanillaPackResourcesBuilder pushClasspathResources(PackType param0, Class<?> param1) {
      Enumeration<URL> var3 = null;

      try {
         var3 = param1.getClassLoader().getResources(param0.getDirectory() + "/");
      } catch (IOException var8) {
      }

      while (var3 != null && var3.hasMoreElements()) {
         URL var4 = var3.nextElement();

         try {
            URI var5 = var4.toURI();
            if ("file".equals(var5.getScheme())) {
               Path var6 = Paths.get(var5);
               this.pushRootPath(var6.getParent());
               this.pushPathForType(param0, var6);
            }
         } catch (Exception var5) {
            LOGGER.error("Failed to extract path from {}", var4, var5);
         }
      }

      return this;
   }

   public VanillaPackResourcesBuilder applyDevelopmentConfig() {
      developmentConfig.accept(this);
      return this;
   }

   public VanillaPackResourcesBuilder pushUniversalPath(Path param0) {
      this.pushRootPath(param0);

      for (PackType var5 : PackType.values()) {
         this.pushPathForType(var5, param0.resolve(var5.getDirectory()));
      }

      return this;
   }

   public VanillaPackResourcesBuilder pushAssetPath(PackType param0, Path param1) {
      this.pushRootPath(param1);
      this.pushPathForType(param0, param1);
      return this;
   }

   public VanillaPackResourcesBuilder setMetadata(BuiltInMetadata param0) {
      this.metadata = param0;
      return this;
   }

   public VanillaPackResourcesBuilder exposeNamespace(String... param0) {
      this.namespaces.addAll(Arrays.asList(param0));
      return this;
   }

   public VanillaPackResources build() {
      Map<PackType, List<Path>> var1 = new EnumMap<>(PackType.class);

      for (PackType var5 : PackType.values()) {
         List<Path> var6 = copyAndReverse(this.pathsForType.getOrDefault(var5, Set.of()));
         var1.put(var5, var6);
      }

      return new VanillaPackResources(this.metadata, Set.copyOf(this.namespaces), copyAndReverse(this.rootPaths), var1);
   }

   private static List<Path> copyAndReverse(Collection<Path> param0) {
      List<Path> var1 = new ArrayList<>(param0);
      Collections.reverse(var1);
      return List.copyOf(var1);
   }
}
