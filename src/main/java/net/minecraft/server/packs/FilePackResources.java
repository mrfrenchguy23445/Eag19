package net.minecraft.server.packs;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

public class FilePackResources extends AbstractPackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Splitter SPLITTER = Splitter.on('/').omitEmptyStrings().limit(3);
   private final File file;
   @Nullable
   private ZipFile zipFile;
   private boolean failedToLoad;

   public FilePackResources(String param0, File param1, boolean param2) {
      super(param0, param2);
      this.file = param1;
   }

   @Nullable
   private ZipFile getOrCreateZipFile() {
      if (this.failedToLoad) {
         return null;
      }

      if (this.zipFile == null) {
         try {
            this.zipFile = new ZipFile(this.file);
         } catch (IOException var1) {
            LOGGER.error("Failed to open pack {}", this.file, var1);
            this.failedToLoad = true;
            return null;
         }
      }

      return this.zipFile;
   }

   private static String getPathFromLocation(PackType param0, ResourceLocation param1) {
      return String.format(Locale.ROOT, "%s/%s/%s", param0.getDirectory(), param1.getNamespace(), param1.getPath());
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... param0) {
      return this.getResource(String.join("/", param0));
   }

   @Override
   public IoSupplier<InputStream> getResource(PackType param0, ResourceLocation param1) {
      return this.getResource(getPathFromLocation(param0, param1));
   }

   @Nullable
   private IoSupplier<InputStream> getResource(String param0) {
      ZipFile var2 = this.getOrCreateZipFile();
      if (var2 == null) {
         return null;
      }

      ZipEntry var3 = var2.getEntry(param0);
      return var3 == null ? null : IoSupplier.create(var2, var3);
   }

   @Override
   public Set<String> getNamespaces(PackType param0) {
      ZipFile var2 = this.getOrCreateZipFile();
      if (var2 == null) {
         return Set.of();
      }

      Enumeration<? extends ZipEntry> var3 = var2.entries();
      Set<String> var4 = Sets.newHashSet();

      while (var3.hasMoreElements()) {
         ZipEntry var5 = var3.nextElement();
         String var6 = var5.getName();
         if (var6.startsWith(param0.getDirectory() + "/")) {
            List<String> var7 = Lists.newArrayList(SPLITTER.split(var6));
            if (var7.size() > 1) {
               String var8 = var7.get(1);
               if (var8.equals(var8.toLowerCase(Locale.ROOT))) {
                  var4.add(var8);
               } else {
                  LOGGER.warn("Ignored non-lowercase namespace: {} in {}", var8, this.file);
               }
            }
         }
      }

      return var4;
   }

   @Override
   protected void finalize() throws Throwable {
      this.close();
      super.finalize();
   }

   @Override
   public void close() {
      if (this.zipFile != null) {
         // TeaVM's java.util.zip.ZipFile does not implement java.io.Closeable, so routing it
         // through IOUtils.closeQuietly(Closeable) throws an illegal cast in WASM-GC. Close the
         // concrete type directly instead.
         try {
            this.zipFile.close();
         } catch (IOException var1) {
            LOGGER.warn("Failed to close pack {}", this.file, var1);
         }
         this.zipFile = null;
      }
   }

   @Override
   public void listResources(PackType param0, String param1, String param2, PackResources.ResourceOutput param3) {
      ZipFile var5 = this.getOrCreateZipFile();
      if (var5 != null) {
         Enumeration<? extends ZipEntry> var6 = var5.entries();
         String var7 = param0.getDirectory() + "/" + param1 + "/";
         String var8 = var7 + param2 + "/";

         while (var6.hasMoreElements()) {
            ZipEntry var9 = var6.nextElement();
            if (!var9.isDirectory()) {
               String var10 = var9.getName();
               if (var10.startsWith(var8)) {
                  String var11 = var10.substring(var7.length());
                  ResourceLocation var12 = ResourceLocation.tryBuild(param1, var11);
                  if (var12 != null) {
                     param3.accept(var12, IoSupplier.create(var5, var9));
                  } else {
                     LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", param1, var11);
                  }
               }
            }
         }
      }
   }
}
