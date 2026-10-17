package com.mojang.realmsclient.util;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsTextureManager {
   private static final Map<String, RealmsTextureManager.RealmsTexture> TEXTURES = Maps.newHashMap();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation TEMPLATE_ICON_LOCATION = new ResourceLocation("textures/gui/presets/isles.png");

   public static ResourceLocation worldTemplate(String param0, @Nullable String param1) {
      return param1 == null ? TEMPLATE_ICON_LOCATION : getTexture(param0, param1);
   }

   private static ResourceLocation getTexture(String param0, String param1) {
      RealmsTextureManager.RealmsTexture var2 = TEXTURES.get(param0);
      if (var2 != null && var2.image().equals(param1)) {
         return var2.textureId;
      } else {
         NativeImage var3 = loadImage(param1);
         if (var3 == null) {
            ResourceLocation var4 = MissingTextureAtlasSprite.getLocation();
            TEXTURES.put(param0, new RealmsTextureManager.RealmsTexture(param1, var4));
            return var4;
         } else {
            ResourceLocation var4 = new ResourceLocation("realms", "dynamic/" + param0);
            Minecraft.getInstance().getTextureManager().register(var4, new DynamicTexture(var3));
            TEXTURES.put(param0, new RealmsTextureManager.RealmsTexture(param1, var4));
            return var4;
         }
      }
   }

   @Nullable
   private static NativeImage loadImage(String param0) {
      byte[] var1 = Base64.getDecoder().decode(param0);
      // Port note (LWJGL removal): was a MemoryUtil.memAlloc'd direct buffer wrapped and
      // flipped into NativeImage.read, then freed. NativeImage.read(byte[]) now does the
      // decode straight from the array, so the buffer and its memFree are both gone.
      try {
         return NativeImage.read(var1);
      } catch (IOException var3) {
         LOGGER.warn("Failed to load world image: {}", param0, var3);
      }

      return null;
   }

   public record RealmsTexture(String image, ResourceLocation textureId) {
   }
}
