package net.minecraft.client.gui.screens.inventory.tooltip;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

public class TooltipRenderUtil {
   public static final int MOUSE_OFFSET = 12;
   private static final int PADDING = 3;
   public static final int PADDING_LEFT = 3;
   public static final int PADDING_RIGHT = 3;
   public static final int PADDING_TOP = 3;
   public static final int PADDING_BOTTOM = 3;
   private static final int BACKGROUND_COLOR = -267386864;
   private static final int BORDER_COLOR_TOP = 1347420415;
   private static final int BORDER_COLOR_BOTTOM = 1344798847;

   public static void renderTooltipBackground(
      TooltipRenderUtil.BlitPainter param0, Matrix4f param1, BufferBuilder param2, int param3, int param4, int param5, int param6, int param7
   ) {
      int var8 = param3 - 3;
      int var9 = param4 - 3;
      int var10 = param5 + 3 + 3;
      int var11 = param6 + 3 + 3;
      renderHorizontalLine(param0, param1, param2, var8, var9 - 1, var10, param7, -267386864);
      renderHorizontalLine(param0, param1, param2, var8, var9 + var11, var10, param7, -267386864);
      renderRectangle(param0, param1, param2, var8, var9, var10, var11, param7, -267386864);
      renderVerticalLine(param0, param1, param2, var8 - 1, var9, var11, param7, -267386864);
      renderVerticalLine(param0, param1, param2, var8 + var10, var9, var11, param7, -267386864);
      renderFrameGradient(param0, param1, param2, var8, var9 + 1, var10, var11, param7, 1347420415, 1344798847);
   }

   private static void renderFrameGradient(
      TooltipRenderUtil.BlitPainter param0,
      Matrix4f param1,
      BufferBuilder param2,
      int param3,
      int param4,
      int param5,
      int param6,
      int param7,
      int param8,
      int param9
   ) {
      renderVerticalLineGradient(param0, param1, param2, param3, param4, param6 - 2, param7, param8, param9);
      renderVerticalLineGradient(param0, param1, param2, param3 + param5 - 1, param4, param6 - 2, param7, param8, param9);
      renderHorizontalLine(param0, param1, param2, param3, param4 - 1, param5, param7, param8);
      renderHorizontalLine(param0, param1, param2, param3, param4 - 1 + param6 - 1, param5, param7, param9);
   }

   private static void renderVerticalLine(
      TooltipRenderUtil.BlitPainter param0, Matrix4f param1, BufferBuilder param2, int param3, int param4, int param5, int param6, int param7
   ) {
      param0.blit(param1, param2, param3, param4, param3 + 1, param4 + param5, param6, param7, param7);
   }

   private static void renderVerticalLineGradient(
      TooltipRenderUtil.BlitPainter param0, Matrix4f param1, BufferBuilder param2, int param3, int param4, int param5, int param6, int param7, int param8
   ) {
      param0.blit(param1, param2, param3, param4, param3 + 1, param4 + param5, param6, param7, param8);
   }

   private static void renderHorizontalLine(
      TooltipRenderUtil.BlitPainter param0, Matrix4f param1, BufferBuilder param2, int param3, int param4, int param5, int param6, int param7
   ) {
      param0.blit(param1, param2, param3, param4, param3 + param5, param4 + 1, param6, param7, param7);
   }

   private static void renderRectangle(
      TooltipRenderUtil.BlitPainter param0, Matrix4f param1, BufferBuilder param2, int param3, int param4, int param5, int param6, int param7, int param8
   ) {
      param0.blit(param1, param2, param3, param4, param3 + param5, param4 + param6, param7, param8, param8);
   }

   @FunctionalInterface
   public interface BlitPainter {
      void blit(Matrix4f var1, BufferBuilder var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9);
   }
}
