package net.minecraft.client.resources;

import java.io.IOException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.GrassColor;

public class GrassColorReloadListener extends SimplePreparableReloadListener<int[]> {
   private static final ResourceLocation LOCATION = new ResourceLocation("textures/colormap/grass.png");

   protected int[] prepare(ResourceManager param0, ProfilerFiller param1) {
      try {
         return LegacyStuffWrapper.getPixels(param0, LOCATION);
      } catch (IOException var3) {
         throw new IllegalStateException("Failed to load grass color texture", var3);
      }
   }

   protected void apply(int[] param0, ResourceManager param1, ProfilerFiller param2) {
      GrassColor.init(param0);
   }
}
