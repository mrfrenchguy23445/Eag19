package net.minecraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;

public class MapRenderer implements AutoCloseable {
   private static final ResourceLocation MAP_ICONS_LOCATION = new ResourceLocation("textures/map/map_icons.png");
   static final RenderType MAP_ICONS = RenderType.text(MAP_ICONS_LOCATION);
   private static final int WIDTH = 128;
   private static final int HEIGHT = 128;
   final TextureManager textureManager;
   private final Int2ObjectMap<MapRenderer.MapInstance> maps = new Int2ObjectOpenHashMap<>();

   public MapRenderer(TextureManager param0) {
      this.textureManager = param0;
   }

   public void update(int param0, MapItemSavedData param1) {
      this.getOrCreateMapInstance(param0, param1).forceUpload();
   }

   public void render(PoseStack param0, MultiBufferSource param1, int param2, MapItemSavedData param3, boolean param4, int param5) {
      this.getOrCreateMapInstance(param2, param3).draw(param0, param1, param4, param5);
   }

   private MapRenderer.MapInstance getOrCreateMapInstance(int param0, MapItemSavedData param1) {
      return this.maps.compute(param0, (l_param1, l_param2) -> {
         if (l_param2 == null) {
            return new MapRenderer.MapInstance(l_param1, param1);
         }

         l_param2.replaceMapData(param1);
         return (MapRenderer.MapInstance)l_param2;
      });
   }

   public void resetData() {
      for (MapRenderer.MapInstance var2 : this.maps.values()) {
         var2.close();
      }

      this.maps.clear();
   }

   @Override
   public void close() {
      this.resetData();
   }

   class MapInstance implements AutoCloseable {
      private MapItemSavedData data;
      private final DynamicTexture texture;
      private final RenderType renderType;
      private boolean requiresUpload = true;

      MapInstance(int param1, MapItemSavedData param2) {
         this.data = param2;
         this.texture = new DynamicTexture(128, 128, true);
         ResourceLocation var4 = MapRenderer.this.textureManager.register("map/" + param1, this.texture);
         this.renderType = RenderType.text(var4);
      }

      void replaceMapData(MapItemSavedData param0) {
         boolean var2 = this.data != param0;
         this.data = param0;
         this.requiresUpload |= var2;
      }

      public void forceUpload() {
         this.requiresUpload = true;
      }

      private void updateTexture() {
         for (int var1 = 0; var1 < 128; var1++) {
            for (int var2 = 0; var2 < 128; var2++) {
               int var3 = var2 + var1 * 128;
               this.texture.getPixels().setPixelRGBA(var2, var1, MaterialColor.getColorFromPackedId(this.data.colors[var3]));
            }
         }

         this.texture.upload();
      }

      void draw(PoseStack param0, MultiBufferSource param1, boolean param2, int param3) {
         if (this.requiresUpload) {
            this.updateTexture();
            this.requiresUpload = false;
         }

         int var5 = 0;
         int var6 = 0;
         float var7 = 0.0F;
         Matrix4f var8 = param0.last().pose();
         VertexConsumer var9 = param1.getBuffer(this.renderType);
         var9.vertex(var8, 0.0F, 128.0F, -0.01F).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(param3).endVertex();
         var9.vertex(var8, 128.0F, 128.0F, -0.01F).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(param3).endVertex();
         var9.vertex(var8, 128.0F, 0.0F, -0.01F).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(param3).endVertex();
         var9.vertex(var8, 0.0F, 0.0F, -0.01F).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(param3).endVertex();
         int var10 = 0;

         for (MapDecoration var12 : this.data.getDecorations()) {
            if (!param2 || var12.renderOnFrame()) {
               param0.pushPose();
               param0.translate(0.0F + var12.getX() / 2.0F + 64.0F, 0.0F + var12.getY() / 2.0F + 64.0F, -0.02F);
               param0.mulPose(Axis.ZP.rotationDegrees(var12.getRot() * 360 / 16.0F));
               param0.scale(4.0F, 4.0F, 3.0F);
               param0.translate(-0.125F, 0.125F, 0.0F);
               byte var13 = var12.getImage();
               float var14 = (var13 % 16 + 0) / 16.0F;
               float var15 = (var13 / 16 + 0) / 16.0F;
               float var16 = (var13 % 16 + 1) / 16.0F;
               float var17 = (var13 / 16 + 1) / 16.0F;
               Matrix4f var18 = param0.last().pose();
               float var19 = -0.001F;
               VertexConsumer var20 = param1.getBuffer(MapRenderer.MAP_ICONS);
               var20.vertex(var18, -1.0F, 1.0F, var10 * -0.001F).color(255, 255, 255, 255).uv(var14, var15).uv2(param3).endVertex();
               var20.vertex(var18, 1.0F, 1.0F, var10 * -0.001F).color(255, 255, 255, 255).uv(var16, var15).uv2(param3).endVertex();
               var20.vertex(var18, 1.0F, -1.0F, var10 * -0.001F).color(255, 255, 255, 255).uv(var16, var17).uv2(param3).endVertex();
               var20.vertex(var18, -1.0F, -1.0F, var10 * -0.001F).color(255, 255, 255, 255).uv(var14, var17).uv2(param3).endVertex();
               param0.popPose();
               if (var12.getName() != null) {
                  Font var21 = Minecraft.getInstance().font;
                  Component var22 = var12.getName();
                  float var23 = var21.width(var22);
                  float var24 = Mth.clamp(25.0F / var23, 0.0F, 6.0F / 9.0F);
                  param0.pushPose();
                  param0.translate(0.0F + var12.getX() / 2.0F + 64.0F - var23 * var24 / 2.0F, 0.0F + var12.getY() / 2.0F + 64.0F + 4.0F, -0.025F);
                  param0.scale(var24, var24, 1.0F);
                  param0.translate(0.0F, 0.0F, -0.1F);
                  var21.drawInBatch(var22, 0.0F, 0.0F, -1, false, param0.last().pose(), param1, Font.DisplayMode.NORMAL, Integer.MIN_VALUE, param3);
                  param0.popPose();
               }

               var10++;
            }
         }
      }

      @Override
      public void close() {
         this.texture.close();
      }
   }
}
