package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class LegacyStuffWrapper {
   @Deprecated
   public static int[] getPixels(ResourceManager param0, ResourceLocation param1) throws IOException {
      try (
         InputStream var2 = param0.open(param1);
         NativeImage var3 = NativeImage.read(var2);
      ) {
         return var3.makePixelArray();
      }
   }
}
