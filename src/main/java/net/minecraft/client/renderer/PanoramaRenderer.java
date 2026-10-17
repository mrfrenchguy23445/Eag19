package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class PanoramaRenderer {
   private final Minecraft minecraft;
   private final CubeMap cubeMap;
   private float spin;
   private float bob;

   public PanoramaRenderer(CubeMap param0) {
      this.cubeMap = param0;
      this.minecraft = Minecraft.getInstance();
   }

   public void render(float param0, float param1) {
      float var3 = (float)(param0 * this.minecraft.options.panoramaSpeed().get());
      this.spin = wrap(this.spin + var3 * 0.1F, 360.0F);
      this.bob = wrap(this.bob + var3 * 0.001F, (float) (Math.PI * 2));
      this.cubeMap.render(this.minecraft, Mth.sin(this.bob) * 5.0F + 25.0F, -this.spin, param1);
   }

   private static float wrap(float param0, float param1) {
      return param0 > param1 ? param0 - param1 : param0;
   }
}
