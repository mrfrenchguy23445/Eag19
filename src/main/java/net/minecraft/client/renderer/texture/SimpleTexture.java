package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class SimpleTexture extends AbstractTexture {
   static final Logger LOGGER = LogUtils.getLogger();
   protected final ResourceLocation location;

   public SimpleTexture(ResourceLocation param0) {
      this.location = param0;
   }

   @Override
   public void load(ResourceManager param0) throws IOException {
      SimpleTexture.TextureImage var2 = this.getTextureImage(param0);
      var2.throwIfError();
      TextureMetadataSection var5 = var2.getTextureMetadata();
      boolean var3;
      boolean var4;
      if (var5 != null) {
         var3 = var5.isBlur();
         var4 = var5.isClamp();
      } else {
         var3 = false;
         var4 = false;
      }

      NativeImage var6 = var2.getImage();
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(() -> this.doLoad(var6, var3, var4));
      } else {
         this.doLoad(var6, var3, var4);
      }
   }

   private void doLoad(NativeImage param0, boolean param1, boolean param2) {
      TextureUtil.prepareImage(this.getId(), 0, param0.getWidth(), param0.getHeight());
      param0.upload(0, 0, 0, 0, 0, param0.getWidth(), param0.getHeight(), param1, param2, false, true);
   }

   protected SimpleTexture.TextureImage getTextureImage(ResourceManager param0) {
      return SimpleTexture.TextureImage.load(param0, this.location);
   }

   protected static class TextureImage implements Closeable {
      @Nullable
      private final TextureMetadataSection metadata;
      @Nullable
      private final NativeImage image;
      @Nullable
      private final IOException exception;

      public TextureImage(IOException param0) {
         this.exception = param0;
         this.metadata = null;
         this.image = null;
      }

      public TextureImage(@Nullable TextureMetadataSection param0, NativeImage param1) {
         this.exception = null;
         this.metadata = param0;
         this.image = param1;
      }

      public static SimpleTexture.TextureImage load(ResourceManager param0, ResourceLocation param1) {
         try {
            Resource var2 = param0.getResourceOrThrow(param1);

            NativeImage var3;
            try (InputStream var4 = var2.open()) {
               var3 = NativeImage.read(var4);
            }

            TextureMetadataSection var4 = null;

            try {
               var4 = var2.metadata().getSection(TextureMetadataSection.SERIALIZER).orElse(null);
            } catch (RuntimeException var5) {
               SimpleTexture.LOGGER.warn("Failed reading metadata of: {}", param1, var5);
            }

            return new SimpleTexture.TextureImage(var4, var3);
         } catch (IOException var2) {
            return new SimpleTexture.TextureImage(var2);
         }
      }

      @Nullable
      public TextureMetadataSection getTextureMetadata() {
         return this.metadata;
      }

      public NativeImage getImage() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         } else {
            return this.image;
         }
      }

      @Override
      public void close() {
         if (this.image != null) {
            this.image.close();
         }
      }

      public void throwIfError() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }
   }
}
