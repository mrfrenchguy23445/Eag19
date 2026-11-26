package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public class BakedGlyph {
   private final RenderType normalType;
   private final RenderType seeThroughType;
   private final RenderType polygonOffsetType;
   private final float u0;
   private final float u1;
   private final float v0;
   private final float v1;
   private final float left;
   private final float right;
   private final float up;
   private final float down;

   public BakedGlyph(
      RenderType param0,
      RenderType param1,
      RenderType param2,
      float param3,
      float param4,
      float param5,
      float param6,
      float param7,
      float param8,
      float param9,
      float param10
   ) {
      this.normalType = param0;
      this.seeThroughType = param1;
      this.polygonOffsetType = param2;
      this.u0 = param3;
      this.u1 = param4;
      this.v0 = param5;
      this.v1 = param6;
      this.left = param7;
      this.right = param8;
      this.up = param9;
      this.down = param10;
   }

   public void render(
      boolean param0, float param1, float param2, Matrix4f param3, VertexConsumer param4, float param5, float param6, float param7, float param8, int param9
   ) {
      int var11 = 3;
      float var12 = param1 + this.left;
      float var13 = param1 + this.right;
      float var14 = this.up - 3.0F;
      float var15 = this.down - 3.0F;
      float var16 = param2 + var14;
      float var17 = param2 + var15;
      float var18 = param0 ? 1.0F - 0.25F * var14 : 0.0F;
      float var19 = param0 ? 1.0F - 0.25F * var15 : 0.0F;
      param4.vertex(param3, var12 + var18, var16, 0.0F).color(param5, param6, param7, param8).uv(this.u0, this.v0).uv2(param9).endVertex();
      param4.vertex(param3, var12 + var19, var17, 0.0F).color(param5, param6, param7, param8).uv(this.u0, this.v1).uv2(param9).endVertex();
      param4.vertex(param3, var13 + var19, var17, 0.0F).color(param5, param6, param7, param8).uv(this.u1, this.v1).uv2(param9).endVertex();
      param4.vertex(param3, var13 + var18, var16, 0.0F).color(param5, param6, param7, param8).uv(this.u1, this.v0).uv2(param9).endVertex();
   }

   public void renderEffect(BakedGlyph.Effect param0, Matrix4f param1, VertexConsumer param2, int param3) {
      param2.vertex(param1, param0.x0, param0.y0, param0.depth).color(param0.r, param0.g, param0.b, param0.a).uv(this.u0, this.v0).uv2(param3).endVertex();
      param2.vertex(param1, param0.x1, param0.y0, param0.depth).color(param0.r, param0.g, param0.b, param0.a).uv(this.u0, this.v1).uv2(param3).endVertex();
      param2.vertex(param1, param0.x1, param0.y1, param0.depth).color(param0.r, param0.g, param0.b, param0.a).uv(this.u1, this.v1).uv2(param3).endVertex();
      param2.vertex(param1, param0.x0, param0.y1, param0.depth).color(param0.r, param0.g, param0.b, param0.a).uv(this.u1, this.v0).uv2(param3).endVertex();
   }

   public RenderType renderType(Font.DisplayMode param0) {
      return switch (param0) {
         case NORMAL -> this.normalType;
         case SEE_THROUGH -> this.seeThroughType;
         case POLYGON_OFFSET -> this.polygonOffsetType;
      };
   }

   public static class Effect {
      protected final float x0;
      protected final float y0;
      protected final float x1;
      protected final float y1;
      protected final float depth;
      protected final float r;
      protected final float g;
      protected final float b;
      protected final float a;

      public Effect(float param0, float param1, float param2, float param3, float param4, float param5, float param6, float param7, float param8) {
         this.x0 = param0;
         this.y0 = param1;
         this.x1 = param2;
         this.y1 = param3;
         this.depth = param4;
         this.r = param5;
         this.g = param6;
         this.b = param7;
         this.a = param8;
      }
   }
}
