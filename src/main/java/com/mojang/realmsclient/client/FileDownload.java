package com.mojang.realmsclient.client;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import net.lax1dude.eaglercraft.v1_8.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

public class FileDownload {
   static final Logger LOGGER = LogUtils.getLogger();
   volatile boolean cancelled;
   volatile boolean finished;
   volatile boolean error;
   volatile boolean extracting;
   @Nullable
   private volatile File tempFile;
   volatile File resourcePackPath;
   @Nullable
   private volatile HttpGet request;
   @Nullable
   private Thread currentThread;
   private final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(120000).setConnectTimeout(120000).build();
   private static final String[] INVALID_FILE_NAMES = new String[]{
      "CON",
      "COM",
      "PRN",
      "AUX",
      "CLOCK$",
      "NUL",
      "COM1",
      "COM2",
      "COM3",
      "COM4",
      "COM5",
      "COM6",
      "COM7",
      "COM8",
      "COM9",
      "LPT1",
      "LPT2",
      "LPT3",
      "LPT4",
      "LPT5",
      "LPT6",
      "LPT7",
      "LPT8",
      "LPT9"
   };

   public long contentLength(String param0) {
      CloseableHttpClient var2 = null;
      HttpGet var3 = null;

      try {
         var3 = new HttpGet(param0);
         var2 = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
         CloseableHttpResponse var4 = var2.execute(var3);
         return Long.parseLong(var4.getFirstHeader("Content-Length").getValue());
      } catch (Throwable var4) {
         LOGGER.error("Unable to get content length for download");
         return 0L;
      } finally {
         if (var3 != null) {
            var3.releaseConnection();
         }

         if (var2 != null) {
            try {
               var2.close();
            } catch (IOException var9) {
               LOGGER.error("Could not close http client", var9);
            }
         }
      }
   }

   public void download(WorldDownload param0, String param1, RealmsDownloadLatestWorldScreen.DownloadStatus param2, LevelStorageSource param3) {
      if (this.currentThread == null) {
         this.currentThread = new Thread(() -> {
            CloseableHttpClient l_var5 = null;

            try {
               this.tempFile = File.createTempFile("backup", ".tar.gz");
               this.request = new HttpGet(param0.downloadLink);
               l_var5 = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
               HttpResponse l_var6 = l_var5.execute(this.request);
               param2.totalBytes = Long.parseLong(l_var6.getFirstHeader("Content-Length").getValue());
               if (l_var6.getStatusLine().getStatusCode() == 200) {
                  OutputStream l_var7 = new FileOutputStream(this.tempFile);
                  FileDownload.ProgressListener l_var8 = new FileDownload.ProgressListener(param1.trim(), this.tempFile, param3, param2);
                  FileDownload.DownloadCountingOutputStream l_var9 = new FileDownload.DownloadCountingOutputStream(l_var7);
                  l_var9.setListener(l_var8);
                  IOUtils.copy(l_var6.getEntity().getContent(), l_var9);
                  return;
               }

               this.error = true;
               this.request.abort();
            } catch (Exception l_var6) {
               LOGGER.error("Caught exception while downloading: {}", l_var6.getMessage());
               this.error = true;
               return;
            } finally {
               this.request.releaseConnection();
               if (this.tempFile != null) {
                  this.tempFile.delete();
               }

               if (!this.error) {
                  if (!param0.resourcePackUrl.isEmpty() && !param0.resourcePackHash.isEmpty()) {
                     try {
                        this.tempFile = File.createTempFile("resources", ".tar.gz");
                        this.request = new HttpGet(param0.resourcePackUrl);
                        HttpResponse l_var15 = l_var5.execute(this.request);
                        param2.totalBytes = Long.parseLong(l_var15.getFirstHeader("Content-Length").getValue());
                        if (l_var15.getStatusLine().getStatusCode() != 200) {
                           this.error = true;
                           this.request.abort();
                           return;
                        }

                        OutputStream l_var16 = new FileOutputStream(this.tempFile);
                        FileDownload.ResourcePackProgressListener l_var17 = new FileDownload.ResourcePackProgressListener(this.tempFile, param2, param0);
                        FileDownload.DownloadCountingOutputStream l_var18 = new FileDownload.DownloadCountingOutputStream(l_var16);
                        l_var18.setListener(l_var17);
                        IOUtils.copy(l_var15.getEntity().getContent(), l_var18);
                     } catch (Exception l_var15) {
                        LOGGER.error("Caught exception while downloading: {}", l_var15.getMessage());
                        this.error = true;
                     } finally {
                        this.request.releaseConnection();
                        if (this.tempFile != null) {
                           this.tempFile.delete();
                        }
                     }
                  } else {
                     this.finished = true;
                  }
               }

               if (l_var5 != null) {
                  try {
                     l_var5.close();
                  } catch (IOException l_var15) {
                     LOGGER.error("Failed to close Realms download client");
                  }
               }
            }
         });
         this.currentThread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
         this.currentThread.start();
      }
   }

   public void cancel() {
      if (this.request != null) {
         this.request.abort();
      }

      if (this.tempFile != null) {
         this.tempFile.delete();
      }

      this.cancelled = true;
   }

   public boolean isFinished() {
      return this.finished;
   }

   public boolean isError() {
      return this.error;
   }

   public boolean isExtracting() {
      return this.extracting;
   }

   public static String findAvailableFolderName(String param0) {
      param0 = param0.replaceAll("[\\./\"]", "_");

      for (String var4 : INVALID_FILE_NAMES) {
         if (param0.equalsIgnoreCase(var4)) {
            param0 = "_" + param0 + "_";
         }
      }

      return param0;
   }

   void untarGzipArchive(String param0, @Nullable File param1, LevelStorageSource param2) throws IOException {
      Pattern var4 = Pattern.compile(".*-([0-9]+)$");
      int var6 = 1;

      for (char var10 : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         param0 = param0.replace(var10, '_');
      }

      if (StringUtils.isEmpty(param0)) {
         param0 = "Realm";
      }

      param0 = findAvailableFolderName(param0);

      try {
         for (LevelStorageSource.LevelDirectory var8 : param2.findLevelCandidates()) {
            String var9 = var8.directoryName();
            if (var9.toLowerCase(Locale.ROOT).startsWith(param0.toLowerCase(Locale.ROOT))) {
               Matcher var10 = var4.matcher(var9);
               if (var10.matches()) {
                  int var11 = Integer.parseInt(var10.group(1));
                  if (var11 > var6) {
                     var6 = var11;
                  }
               } else {
                  var6++;
               }
            }
         }
      } catch (Exception var7) {
         LOGGER.error("Error getting level list", var7);
         this.error = true;
         return;
      }

      String var5;
      if (param2.isNewLevelIdAcceptable(param0) && var6 <= 1) {
         var5 = param0;
      } else {
         var5 = param0 + (var6 == 1 ? "" : "-" + var6);
         if (!param2.isNewLevelIdAcceptable(var5)) {
            boolean var7 = false;

            while (!var7) {
               var6++;
               var5 = param0 + (var6 == 1 ? "" : "-" + var6);
               if (param2.isNewLevelIdAcceptable(var5)) {
                  var7 = true;
               }
            }
         }
      }

      TarArchiveInputStream var7 = null;
      File var8 = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "saves");

      try {
         var8.mkdir();
         var7 = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(param1))));

         for (TarArchiveEntry var9 = var7.getNextTarEntry(); var9 != null; var9 = var7.getNextTarEntry()) {
            File var10 = new File(var8, var9.getName().replace("world", var5));
            if (var9.isDirectory()) {
               var10.mkdirs();
            } else {
               var10.createNewFile();

               try (FileOutputStream var11 = new FileOutputStream(var10)) {
                  IOUtils.copy(var7, var11);
               }
            }
         }
      } catch (Exception var9) {
         LOGGER.error("Error extracting world", var9);
         this.error = true;
      } finally {
         if (var7 != null) {
            var7.close();
         }

         if (param1 != null) {
            param1.delete();
         }

         try (LevelStorageSource.LevelStorageAccess var15 = param2.createAccess(var5)) {
            var15.renameLevel(var5.trim());
            Path var16 = var15.getLevelPath(LevelResource.LEVEL_DATA_FILE);
            deletePlayerTag(var16.toFile());
         } catch (IOException var15) {
            LOGGER.error("Failed to rename unpacked realms level {}", var5, var15);
         }

         this.resourcePackPath = new File(var8, var5 + File.separator + "resources.zip");
      }
   }

   private static void deletePlayerTag(File param0) {
      if (param0.exists()) {
         try {
            CompoundTag var1 = NbtIo.readCompressed(param0);
            CompoundTag var2 = var1.getCompound("Data");
            var2.remove("Player");
            NbtIo.writeCompressed(var1, param0);
         } catch (Exception var1) {
            var1.printStackTrace();
         }
      }
   }

   static class DownloadCountingOutputStream extends CountingOutputStream {
      @Nullable
      private DownloadProgressListener listener;

      public DownloadCountingOutputStream(OutputStream param0) {
         super(param0);
      }

      public void setListener(DownloadProgressListener param0) {
         this.listener = param0;
      }

      @Override
      protected void afterWrite(int param0) throws IOException {
         super.afterWrite(param0);
         if (this.listener != null) {
            this.listener.onBytesWritten(this);
         }
      }
   }

   class ProgressListener implements DownloadProgressListener {
      private final String worldName;
      private final File tempFile;
      private final LevelStorageSource levelStorageSource;
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;

      ProgressListener(String param1, File param2, LevelStorageSource param3, RealmsDownloadLatestWorldScreen.DownloadStatus param4) {
         this.worldName = param1;
         this.tempFile = param2;
         this.levelStorageSource = param3;
         this.downloadStatus = param4;
      }

      @Override
      public void onBytesWritten(FileDownload.DownloadCountingOutputStream param0) {
         this.downloadStatus.bytesWritten = param0.getByteCount();
         if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled && !FileDownload.this.error) {
            try {
               FileDownload.this.extracting = true;
               FileDownload.this.untarGzipArchive(this.worldName, this.tempFile, this.levelStorageSource);
            } catch (IOException var2) {
               FileDownload.LOGGER.error("Error extracting archive", var2);
               FileDownload.this.error = true;
            }
         }
      }
   }

   class ResourcePackProgressListener implements DownloadProgressListener {
      private final File tempFile;
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;
      private final WorldDownload worldDownload;

      ResourcePackProgressListener(File param1, RealmsDownloadLatestWorldScreen.DownloadStatus param2, WorldDownload param3) {
         this.tempFile = param1;
         this.downloadStatus = param2;
         this.worldDownload = param3;
      }

      @Override
      public void onBytesWritten(FileDownload.DownloadCountingOutputStream param0) {
         this.downloadStatus.bytesWritten = param0.getByteCount();
         if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled) {
            try {
               String var2 = Hashing.sha1().hashBytes(Files.toByteArray(this.tempFile)).toString();
               if (var2.equals(this.worldDownload.resourcePackHash)) {
                  FileUtils.copyFile(this.tempFile, FileDownload.this.resourcePackPath);
                  FileDownload.this.finished = true;
               } else {
                  FileDownload.LOGGER.error("Resourcepack had wrong hash (expected {}, found {}). Deleting it.", this.worldDownload.resourcePackHash, var2);
                  FileUtils.deleteQuietly(this.tempFile);
                  FileDownload.this.error = true;
               }
            } catch (IOException var2) {
               FileDownload.LOGGER.error("Error copying resourcepack file: {}", var2.getMessage());
               FileDownload.this.error = true;
            }
         }
      }
   }
}
