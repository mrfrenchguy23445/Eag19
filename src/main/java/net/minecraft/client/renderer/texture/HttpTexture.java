package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.lax1dude.eaglercraft.v1_8.FileUtils;
import org.slf4j.Logger;

public class HttpTexture extends SimpleTexture {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SKIN_WIDTH = 64;
   private static final int SKIN_HEIGHT = 64;
   private static final int LEGACY_SKIN_HEIGHT = 32;
   @Nullable
   private final File file;
   private final String urlString;
   private final boolean processLegacySkin;
   @Nullable
   private final Runnable onDownloaded;
   @Nullable
   private CompletableFuture<?> future;
   private boolean uploaded;

   public HttpTexture(@Nullable File param0, String param1, ResourceLocation param2, boolean param3, @Nullable Runnable param4) {
      super(param2);
      this.file = param0;
      this.urlString = param1;
      this.processLegacySkin = param3;
      this.onDownloaded = param4;
   }

   private void loadCallback(NativeImage param0) {
      if (this.onDownloaded != null) {
         this.onDownloaded.run();
      }

      Minecraft.getInstance().execute(() -> {
         this.uploaded = true;
         if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> this.upload(param0));
         } else {
            this.upload(param0);
         }
      });
   }

   private void upload(NativeImage param0) {
      TextureUtil.prepareImage(this.getId(), param0.getWidth(), param0.getHeight());
      param0.upload(0, 0, 0, true);
   }

   @Override
   public void load(ResourceManager param0) throws IOException {
      Minecraft.getInstance().execute(() -> {
         if (!this.uploaded) {
            try {
               super.load(param0);
            } catch (IOException l_var2) {
               LOGGER.warn("Failed to load texture: {}", this.location, l_var2);
            }

            this.uploaded = true;
         }
      });
      if (this.future == null) {
         NativeImage var2;
         if (this.file != null && this.file.isFile()) {
            LOGGER.debug("Loading http texture from local cache ({})", this.file);
            FileInputStream var3 = new FileInputStream(this.file);
            var2 = this.load(var3);
         } else {
            var2 = null;
         }

         if (var2 != null) {
            this.loadCallback(var2);
         } else {
            // Port note (wasm14): remote texture download is disabled in this build, which
            // exists to reach the title screen. Upstream this ran on Util.backgroundExecutor()
            // and fetched this.urlString through java.net.URL.openConnection -- on the WASM-GC
            // classlib that resolves to an XHR-backed HttpURLConnection whose performRequest
            // parks on org.teavm.runtime.Fiber, which this target does not provide, so the
            // download failed at link time and would fail at run time for the same reason. A
            // browser also has no file to cache the bytes into (this.file), so the local-cache
            // branch above is the only path that can ever succeed here.
            //
            // Skins and capes are what this class carries; Eaglercraft serves those from its own
            // storage rather than from Mojang's CDN, so the download comes back when the skin
            // protocol does. Until then a missing cache entry means the default skin.
            LOGGER.debug("Not downloading http texture from {}: remote texture fetch is disabled", this.urlString);
         }
      }
   }

   @Nullable
   private NativeImage load(InputStream param0) {
      NativeImage var2 = null;

      try {
         var2 = NativeImage.read(param0);
         if (this.processLegacySkin) {
            var2 = this.processLegacySkin(var2);
         }
      } catch (Exception var3) {
         LOGGER.warn("Error while loading the skin texture", var3);
      }

      return var2;
   }

   @Nullable
   private NativeImage processLegacySkin(NativeImage param0) {
      int var2 = param0.getHeight();
      int var3 = param0.getWidth();
      if (var3 == 64 && (var2 == 32 || var2 == 64)) {
         boolean var4 = var2 == 32;
         if (var4) {
            NativeImage var5 = new NativeImage(64, 64, true);
            var5.copyFrom(param0);
            param0.close();
            param0 = var5;
            param0.fillRect(0, 32, 64, 32, 0);
            param0.copyRect(4, 16, 16, 32, 4, 4, true, false);
            param0.copyRect(8, 16, 16, 32, 4, 4, true, false);
            param0.copyRect(0, 20, 24, 32, 4, 12, true, false);
            param0.copyRect(4, 20, 16, 32, 4, 12, true, false);
            param0.copyRect(8, 20, 8, 32, 4, 12, true, false);
            param0.copyRect(12, 20, 16, 32, 4, 12, true, false);
            param0.copyRect(44, 16, -8, 32, 4, 4, true, false);
            param0.copyRect(48, 16, -8, 32, 4, 4, true, false);
            param0.copyRect(40, 20, 0, 32, 4, 12, true, false);
            param0.copyRect(44, 20, -8, 32, 4, 12, true, false);
            param0.copyRect(48, 20, -16, 32, 4, 12, true, false);
            param0.copyRect(52, 20, -8, 32, 4, 12, true, false);
         }

         setNoAlpha(param0, 0, 0, 32, 16);
         if (var4) {
            doNotchTransparencyHack(param0, 32, 0, 64, 32);
         }

         setNoAlpha(param0, 0, 16, 64, 32);
         setNoAlpha(param0, 16, 48, 48, 64);
         return param0;
      } else {
         param0.close();
         LOGGER.warn("Discarding incorrectly sized ({}x{}) skin texture from {}", var3, var2, this.urlString);
         return null;
      }
   }

   private static void doNotchTransparencyHack(NativeImage param0, int param1, int param2, int param3, int param4) {
      for (int var5 = param1; var5 < param3; var5++) {
         for (int var6 = param2; var6 < param4; var6++) {
            int var7 = param0.getPixelRGBA(var5, var6);
            if ((var7 >> 24 & 0xFF) < 128) {
               return;
            }
         }
      }

      for (int var5 = param1; var5 < param3; var5++) {
         for (int var6 = param2; var6 < param4; var6++) {
            param0.setPixelRGBA(var5, var6, param0.getPixelRGBA(var5, var6) & 16777215);
         }
      }
   }

   private static void setNoAlpha(NativeImage param0, int param1, int param2, int param3, int param4) {
      for (int var5 = param1; var5 < param3; var5++) {
         for (int var6 = param2; var6 < param4; var6++) {
            param0.setPixelRGBA(var5, var6, param0.getPixelRGBA(var5, var6) | 0xFF000000);
         }
      }
   }
}
