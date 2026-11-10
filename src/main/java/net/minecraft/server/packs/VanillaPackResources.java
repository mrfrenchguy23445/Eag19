package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final BuiltInMetadata metadata;
   private final Set<String> namespaces;
   private final List<Path> rootPaths;
   private final Map<PackType, List<Path>> pathsForType;

   VanillaPackResources(BuiltInMetadata param0, Set<String> param1, List<Path> param2, Map<PackType, List<Path>> param3) {
      this.metadata = param0;
      this.namespaces = param1;
      this.rootPaths = param2;
      this.pathsForType = param3;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... param0) {
      FileUtil.validatePath(param0);
      List<String> var2 = List.of(param0);

      for (Path var4 : this.rootPaths) {
         Path var5 = FileUtil.resolvePath(var4, var2);
         if (Files.exists(var5) && PathPackResources.validatePath(var5)) {
            return IoSupplier.create(var5);
         }
      }

      // PORT NOTE (Eag19 / TeaVM WASM-GC): see epkResource. Root files live at the EPK
      // root (e.g. "pack.mcmeta"), so they are addressed without the pack-type prefix.
      // assets.epk has no root entries today, so this returns null and getMetadataSection
      // falls back to the BuiltInMetadata supplied by ClientPackSource, which is correct.
      return epkSupplier(String.join("/", param0));
   }

   public void listRawPaths(PackType param0, ResourceLocation param1, Consumer<Path> param2) {
      FileUtil.decomposePath(param1.getPath()).get().ifLeft(l_param3 -> {
         String l_var5 = param1.getNamespace();

         for (Path l_var7 : this.pathsForType.get(param0)) {
            Path l_var8 = l_var7.resolve(l_var5);
            param2.accept(FileUtil.resolvePath(l_var8, (List<String>)l_param3));
         }
      }).ifRight(l_param1 -> LOGGER.error("Invalid path {}: {}", param1, l_param1.message()));
   }

   @Override
   public void listResources(PackType param0, String param1, String param2, PackResources.ResourceOutput param3) {
      FileUtil.decomposePath(param2).get().ifLeft(l_param3 -> {
         List<Path> l_var5 = this.pathsForType.get(param0);
         Map<ResourceLocation, IoSupplier<InputStream>> l_var6 = new HashMap<>();

         for (Path l_var8 : l_var5) {
            getResources(l_var6::putIfAbsent, param1, l_var8, (List<String>)l_param3);
         }

         // Eag19: filesystem roots can contain only part of the vanilla assets. Always
         // merge the EPK listing as the fallback layer, matching getResource() below.
         // Without this, a single existing asset root suppresses EPK directory listings,
         // so atlas definitions/sprites present only in assets.epk silently disappear.
         listEpkResources(param0, param1, param2, l_var6::putIfAbsent);
         l_var6.forEach(param3);
      }).ifRight(l_param1 -> LOGGER.error("Invalid path {}: {}", param2, l_param1.message()));
   }

   private static void getResources(PackResources.ResourceOutput param0, String param1, Path param2, List<String> param3) {
      Path var4 = param2.resolve(param1);
      PathPackResources.listPath(param1, var4, param3, param0);
   }

   /**
    * Eag19 / TeaVM WASM-GC: read a single asset straight out of assets.epk.
    *
    * <p>The browser client carries no asset directory (see {@code VanillaPackResourcesBuilder}
    * and {@code ClientPackSource}); every vanilla asset is a numbered entry in
    * {@code assets.epk}, which {@code PlatformAssets} loads into an in-memory index at
    * startup. This is that index's read path. {@code getResourceBytes} returns {@code null}
    * (or a negative-cache marker) for an absent entry, which maps cleanly to the pack
    * contract's "no such resource".
    */
   @Nullable
   private static IoSupplier<InputStream> epkSupplier(String param0) {
      byte[] var1 = EagRuntime.getResourceBytes(param0);
      return var1 != null ? (IoSupplier<InputStream>)(() -> new ByteArrayInputStream(var1)) : null;
   }

   /** Eag19: map a {@code ResourceLocation} to its {@code assets/<ns>/<path>} EPK entry. */
   @Nullable
   private static IoSupplier<InputStream> epkResource(PackType param0, ResourceLocation param1) {
      return epkSupplier(param0.getDirectory() + "/" + param1.getNamespace() + "/" + param1.getPath());
   }

   /**
    * Eag19: recursive directory listing out of assets.epk, matching
    * {@link PathPackResources#listPath} semantics.
    *
    * <p>{@code param2} is the directory (relative to {@code <packtype>/<namespace>}) whose
    * files should be emitted; the empty string means the whole namespace. Every EPK key
    * under that directory becomes a {@code ResourceLocation} addressed relative to
    * {@code <packtype>/<namespace>}, exactly as {@code listPath} does with
    * {@code param1.relativize(file)}.
    */
   private static void listEpkResources(PackType param0, String param1, String param2, PackResources.ResourceOutput param3) {
      String var4 = param0.getDirectory() + "/" + param1 + "/";
      String var5 = param2.isEmpty() ? var4 : var4 + param2 + "/";

      for (String var6 : EagRuntime.getAllResourcePaths(var5)) {
         String var7 = var6.substring(var4.length());
         ResourceLocation var8 = ResourceLocation.tryBuild(param1, var7);
         if (var8 == null) {
            LOGGER.warn("Invalid path in pack: {}:{}, ignoring", param1, var7);
         } else {
            byte[] var9 = EagRuntime.getResourceBytes(var6);
            if (var9 != null) {
               param3.accept(var8, (IoSupplier<InputStream>)(() -> new ByteArrayInputStream(var9)));
            }
         }
      }
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType param0, ResourceLocation param1) {
      IoSupplier<InputStream> var2 = FileUtil.decomposePath(param1.getPath()).get().map(l_param2 -> {
         String l_var4 = param1.getNamespace();

         for (Path l_var6 : this.pathsForType.get(param0)) {
            Path l_var7 = FileUtil.resolvePath(l_var6.resolve(l_var4), (List<String>)l_param2);
            if (Files.exists(l_var7) && PathPackResources.validatePath(l_var7)) {
               return IoSupplier.create(l_var7);
            }
         }

         return null;
      }, l_param1 -> {
         LOGGER.error("Invalid path {}: {}", param1, l_param1.message());
         return null;
      });

      // PORT NOTE (Eag19 / TeaVM WASM-GC): the browser client mounts no asset directory, so
      // the filesystem lookup above finds nothing and every read must come out of assets.epk
      // through the Eaglercraft asset index. Without this the very first asset read in the
      // game - GameRenderer.preloadUiShader - threw "could not preload blit shader" and the
      // title screen never rendered.
      if (var2 == null) {
         var2 = epkResource(param0, param1);
      }

      return var2;
   }

   @Override
   public Set<String> getNamespaces(PackType param0) {
      return this.namespaces;
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionSerializer<T> param0) {
      IoSupplier<InputStream> var2 = this.getRootResource("pack.mcmeta");
      if (var2 != null) {
         try (InputStream var3 = var2.get()) {
            T var4 = AbstractPackResources.getMetadataFromStream(param0, var3);
            if (var4 != null) {
               return var4;
            }
         } catch (IOException var8) {
         }
      }

      return this.metadata.get(param0);
   }

   @Override
   public String packId() {
      return "vanilla";
   }

   @Override
   public boolean isBuiltin() {
      return true;
   }

   @Override
   public void close() {
   }

   public ResourceProvider asProvider() {
      return l_param0 -> Optional.ofNullable(this.getResource(PackType.CLIENT_RESOURCES, l_param0))
         .map(l2_param0 -> new Resource(this, (IoSupplier<InputStream>)l2_param0));
   }
}
