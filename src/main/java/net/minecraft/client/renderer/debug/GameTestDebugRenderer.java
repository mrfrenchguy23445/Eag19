package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;

public class GameTestDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final float PADDING = 0.02F;
   private final Map<BlockPos, GameTestDebugRenderer.Marker> markers = Maps.newHashMap();

   public void addMarker(BlockPos param0, int param1, String param2, int param3) {
      this.markers.put(param0, new GameTestDebugRenderer.Marker(param1, param2, Util.getMillis() + param3));
   }

   @Override
   public void clear() {
      this.markers.clear();
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      long var9 = Util.getMillis();
      this.markers.entrySet().removeIf(l_param2 -> var9 > l_param2.getValue().removeAtTime);
      this.markers.forEach((l_param2, l_param3) -> this.renderMarker(param0, param1, l_param2, l_param3));
   }

   private void renderMarker(PoseStack param0, MultiBufferSource param1, BlockPos param2, GameTestDebugRenderer.Marker param3) {
      DebugRenderer.renderFilledBox(param0, param1, param2, 0.02F, param3.getR(), param3.getG(), param3.getB(), param3.getA() * 0.75F);
      if (!param3.text.isEmpty()) {
         double var5 = param2.getX() + 0.5;
         double var7 = param2.getY() + 1.2;
         double var9 = param2.getZ() + 0.5;
         DebugRenderer.renderFloatingText(param0, param1, param3.text, var5, var7, var9, -1, 0.01F, true, 0.0F, true);
      }
   }

   static class Marker {
      public int color;
      public String text;
      public long removeAtTime;

      public Marker(int param0, String param1, long param2) {
         this.color = param0;
         this.text = param1;
         this.removeAtTime = param2;
      }

      public float getR() {
         return (this.color >> 16 & 0xFF) / 255.0F;
      }

      public float getG() {
         return (this.color >> 8 & 0xFF) / 255.0F;
      }

      public float getB() {
         return (this.color & 0xFF) / 255.0F;
      }

      public float getA() {
         return (this.color >> 24 & 0xFF) / 255.0F;
      }
   }
}
