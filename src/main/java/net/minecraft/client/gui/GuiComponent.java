package net.minecraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Divisor;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public abstract class GuiComponent {
   public static final ResourceLocation BACKGROUND_LOCATION = new ResourceLocation("textures/gui/options_background.png");
   public static final ResourceLocation STATS_ICON_LOCATION = new ResourceLocation("textures/gui/container/stats_icons.png");
   public static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
   public static final ResourceLocation LIGHT_DIRT_BACKGROUND = new ResourceLocation("textures/gui/light_dirt_background.png");
   private static final GuiComponent.ScissorStack SCISSOR_STACK = new GuiComponent.ScissorStack();

   protected static void hLine(PoseStack param0, int param1, int param2, int param3, int param4) {
      if (param2 < param1) {
         int var5 = param1;
         param1 = param2;
         param2 = var5;
      }

      fill(param0, param1, param3, param2 + 1, param3 + 1, param4);
   }

   protected static void vLine(PoseStack param0, int param1, int param2, int param3, int param4) {
      if (param3 < param2) {
         int var5 = param2;
         param2 = param3;
         param3 = var5;
      }

      fill(param0, param1, param2 + 1, param1 + 1, param3, param4);
   }

   public static void enableScissor(int param0, int param1, int param2, int param3) {
      applyScissor(SCISSOR_STACK.push(new ScreenRectangle(param0, param1, param2 - param0, param3 - param1)));
   }

   public static void disableScissor() {
      applyScissor(SCISSOR_STACK.pop());
   }

   private static void applyScissor(@Nullable ScreenRectangle param0) {
      if (param0 != null) {
         Window var1 = Minecraft.getInstance().getWindow();
         int var2 = var1.getHeight();
         double var3 = var1.getGuiScale();
         double var5 = param0.left() * var3;
         double var7 = var2 - param0.bottom() * var3;
         double var9 = param0.width() * var3;
         double var11 = param0.height() * var3;
         RenderSystem.enableScissor((int)var5, (int)var7, Math.max(0, (int)var9), Math.max(0, (int)var11));
      } else {
         RenderSystem.disableScissor();
      }
   }

   public static void fill(PoseStack param0, int param1, int param2, int param3, int param4, int param5) {
      fill(param0, param1, param2, param3, param4, 0, param5);
   }

   public static void fill(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6) {
      Matrix4f var7 = param0.last().pose();
      if (param1 < param3) {
         int var8 = param1;
         param1 = param3;
         param3 = var8;
      }

      if (param2 < param4) {
         int var8 = param2;
         param2 = param4;
         param4 = var8;
      }

      float var8 = FastColor.ARGB32.alpha(param6) / 255.0F;
      float var9 = FastColor.ARGB32.red(param6) / 255.0F;
      float var10 = FastColor.ARGB32.green(param6) / 255.0F;
      float var11 = FastColor.ARGB32.blue(param6) / 255.0F;
      BufferBuilder var12 = Tesselator.getInstance().getBuilder();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      var12.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      var12.vertex(var7, param1, param2, param5).color(var9, var10, var11, var8).endVertex();
      var12.vertex(var7, param1, param4, param5).color(var9, var10, var11, var8).endVertex();
      var12.vertex(var7, param3, param4, param5).color(var9, var10, var11, var8).endVertex();
      var12.vertex(var7, param3, param2, param5).color(var9, var10, var11, var8).endVertex();
      BufferUploader.drawWithShader(var12.end());
      RenderSystem.disableBlend();
   }

   protected static void fillGradient(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6) {
      fillGradient(param0, param1, param2, param3, param4, param5, param6, 0);
   }

   protected static void fillGradient(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator var8 = Tesselator.getInstance();
      BufferBuilder var9 = var8.getBuilder();
      var9.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      fillGradient(param0.last().pose(), var9, param1, param2, param3, param4, param7, param5, param6);
      var8.end();
      RenderSystem.disableBlend();
   }

   protected static void fillGradient(Matrix4f param0, BufferBuilder param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8) {
      float var9 = FastColor.ARGB32.alpha(param7) / 255.0F;
      float var10 = FastColor.ARGB32.red(param7) / 255.0F;
      float var11 = FastColor.ARGB32.green(param7) / 255.0F;
      float var12 = FastColor.ARGB32.blue(param7) / 255.0F;
      float var13 = FastColor.ARGB32.alpha(param8) / 255.0F;
      float var14 = FastColor.ARGB32.red(param8) / 255.0F;
      float var15 = FastColor.ARGB32.green(param8) / 255.0F;
      float var16 = FastColor.ARGB32.blue(param8) / 255.0F;
      param1.vertex(param0, param2, param3, param6).color(var10, var11, var12, var9).endVertex();
      param1.vertex(param0, param2, param5, param6).color(var14, var15, var16, var13).endVertex();
      param1.vertex(param0, param4, param5, param6).color(var14, var15, var16, var13).endVertex();
      param1.vertex(param0, param4, param3, param6).color(var10, var11, var12, var9).endVertex();
   }

   public static void drawCenteredString(PoseStack param0, Font param1, String param2, int param3, int param4, int param5) {
      param1.drawShadow(param0, param2, param3 - param1.width(param2) / 2, param4, param5);
   }

   public static void drawCenteredString(PoseStack param0, Font param1, Component param2, int param3, int param4, int param5) {
      FormattedCharSequence var6 = param2.getVisualOrderText();
      param1.drawShadow(param0, var6, param3 - param1.width(var6) / 2, param4, param5);
   }

   public static void drawCenteredString(PoseStack param0, Font param1, FormattedCharSequence param2, int param3, int param4, int param5) {
      param1.drawShadow(param0, param2, param3 - param1.width(param2) / 2, param4, param5);
   }

   public static void drawString(PoseStack param0, Font param1, String param2, int param3, int param4, int param5) {
      param1.drawShadow(param0, param2, param3, param4, param5);
   }

   public static void drawString(PoseStack param0, Font param1, FormattedCharSequence param2, int param3, int param4, int param5) {
      param1.drawShadow(param0, param2, param3, param4, param5);
   }

   public static void drawString(PoseStack param0, Font param1, Component param2, int param3, int param4, int param5) {
      param1.drawShadow(param0, param2, param3, param4, param5);
   }

   public static void blitOutlineBlack(int param0, int param1, BiConsumer<Integer, Integer> param2) {
      RenderSystem.blendFuncSeparate(
         GlStateManager.SourceFactor.ZERO,
         GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
         GlStateManager.SourceFactor.SRC_ALPHA,
         GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
      );
      param2.accept(param0 + 1, param1);
      param2.accept(param0 - 1, param1);
      param2.accept(param0, param1 + 1);
      param2.accept(param0, param1 - 1);
      RenderSystem.defaultBlendFunc();
      param2.accept(param0, param1);
   }

   public static void blit(PoseStack param0, int param1, int param2, int param3, int param4, int param5, TextureAtlasSprite param6) {
      innerBlit(param0.last().pose(), param1, param1 + param4, param2, param2 + param5, param3, param6.getU0(), param6.getU1(), param6.getV0(), param6.getV1());
   }

   public static void blit(
      PoseStack param0,
      int param1,
      int param2,
      int param3,
      int param4,
      int param5,
      TextureAtlasSprite param6,
      float param7,
      float param8,
      float param9,
      float param10
   ) {
      innerBlit(
         param0.last().pose(),
         param1,
         param1 + param4,
         param2,
         param2 + param5,
         param3,
         param6.getU0(),
         param6.getU1(),
         param6.getV0(),
         param6.getV1(),
         param7,
         param8,
         param9,
         param10
      );
   }

   public static void renderOutline(PoseStack param0, int param1, int param2, int param3, int param4, int param5) {
      fill(param0, param1, param2, param1 + param3, param2 + 1, param5);
      fill(param0, param1, param2 + param4 - 1, param1 + param3, param2 + param4, param5);
      fill(param0, param1, param2 + 1, param1 + 1, param2 + param4 - 1, param5);
      fill(param0, param1 + param3 - 1, param2 + 1, param1 + param3, param2 + param4 - 1, param5);
   }

   public static void blit(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6) {
      blit(param0, param1, param2, 0, param3, param4, param5, param6, 256, 256);
   }

   public static void blit(PoseStack param0, int param1, int param2, int param3, float param4, float param5, int param6, int param7, int param8, int param9) {
      blit(param0, param1, param1 + param6, param2, param2 + param7, param3, param6, param7, param4, param5, param8, param9);
   }

   public static void blit(
      PoseStack param0, int param1, int param2, int param3, int param4, float param5, float param6, int param7, int param8, int param9, int param10
   ) {
      blit(param0, param1, param1 + param3, param2, param2 + param4, 0, param7, param8, param5, param6, param9, param10);
   }

   public static void blit(PoseStack param0, int param1, int param2, float param3, float param4, int param5, int param6, int param7, int param8) {
      blit(param0, param1, param2, param5, param6, param3, param4, param5, param6, param7, param8);
   }

   private static void blit(
      PoseStack param0,
      int param1,
      int param2,
      int param3,
      int param4,
      int param5,
      int param6,
      int param7,
      float param8,
      float param9,
      int param10,
      int param11
   ) {
      innerBlit(
         param0.last().pose(),
         param1,
         param2,
         param3,
         param4,
         param5,
         (param8 + 0.0F) / param10,
         (param8 + param6) / param10,
         (param9 + 0.0F) / param11,
         (param9 + param7) / param11
      );
   }

   private static void innerBlit(
      Matrix4f param0, int param1, int param2, int param3, int param4, int param5, float param6, float param7, float param8, float param9
   ) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      BufferBuilder var10 = Tesselator.getInstance().getBuilder();
      var10.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      var10.vertex(param0, param1, param3, param5).uv(param6, param8).endVertex();
      var10.vertex(param0, param1, param4, param5).uv(param6, param9).endVertex();
      var10.vertex(param0, param2, param4, param5).uv(param7, param9).endVertex();
      var10.vertex(param0, param2, param3, param5).uv(param7, param8).endVertex();
      BufferUploader.drawWithShader(var10.end());
   }

   private static void innerBlit(
      Matrix4f param0,
      int param1,
      int param2,
      int param3,
      int param4,
      int param5,
      float param6,
      float param7,
      float param8,
      float param9,
      float param10,
      float param11,
      float param12,
      float param13
   ) {
      RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
      RenderSystem.enableBlend();
      BufferBuilder var14 = Tesselator.getInstance().getBuilder();
      var14.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
      var14.vertex(param0, param1, param3, param5).color(param10, param11, param12, param13).uv(param6, param8).endVertex();
      var14.vertex(param0, param1, param4, param5).color(param10, param11, param12, param13).uv(param6, param9).endVertex();
      var14.vertex(param0, param2, param4, param5).color(param10, param11, param12, param13).uv(param7, param9).endVertex();
      var14.vertex(param0, param2, param3, param5).color(param10, param11, param12, param13).uv(param7, param8).endVertex();
      BufferUploader.drawWithShader(var14.end());
      RenderSystem.disableBlend();
   }

   public static void blitNineSliced(
      PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9
   ) {
      blitNineSliced(param0, param1, param2, param3, param4, param5, param5, param5, param5, param6, param7, param8, param9);
   }

   public static void blitNineSliced(
      PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, int param10
   ) {
      blitNineSliced(param0, param1, param2, param3, param4, param5, param6, param5, param6, param7, param8, param9, param10);
   }

   public static void blitNineSliced(
      PoseStack param0,
      int param1,
      int param2,
      int param3,
      int param4,
      int param5,
      int param6,
      int param7,
      int param8,
      int param9,
      int param10,
      int param11,
      int param12
   ) {
      param5 = Math.min(param5, param3 / 2);
      param7 = Math.min(param7, param3 / 2);
      param6 = Math.min(param6, param4 / 2);
      param8 = Math.min(param8, param4 / 2);
      if (param3 == param9 && param4 == param10) {
         blit(param0, param1, param2, param11, param12, param3, param4);
      } else if (param4 == param10) {
         blit(param0, param1, param2, param11, param12, param5, param4);
         blitRepeating(param0, param1 + param5, param2, param3 - param7 - param5, param4, param11 + param5, param12, param9 - param7 - param5, param10);
         blit(param0, param1 + param3 - param7, param2, param11 + param9 - param7, param12, param7, param4);
      } else if (param3 == param9) {
         blit(param0, param1, param2, param11, param12, param3, param6);
         blitRepeating(param0, param1, param2 + param6, param3, param4 - param8 - param6, param11, param12 + param6, param9, param10 - param8 - param6);
         blit(param0, param1, param2 + param4 - param8, param11, param12 + param10 - param8, param3, param8);
      } else {
         blit(param0, param1, param2, param11, param12, param5, param6);
         blitRepeating(param0, param1 + param5, param2, param3 - param7 - param5, param6, param11 + param5, param12, param9 - param7 - param5, param6);
         blit(param0, param1 + param3 - param7, param2, param11 + param9 - param7, param12, param7, param6);
         blit(param0, param1, param2 + param4 - param8, param11, param12 + param10 - param8, param5, param8);
         blitRepeating(
            param0,
            param1 + param5,
            param2 + param4 - param8,
            param3 - param7 - param5,
            param8,
            param11 + param5,
            param12 + param10 - param8,
            param9 - param7 - param5,
            param8
         );
         blit(param0, param1 + param3 - param7, param2 + param4 - param8, param11 + param9 - param7, param12 + param10 - param8, param7, param8);
         blitRepeating(param0, param1, param2 + param6, param5, param4 - param8 - param6, param11, param12 + param6, param5, param10 - param8 - param6);
         blitRepeating(
            param0,
            param1 + param5,
            param2 + param6,
            param3 - param7 - param5,
            param4 - param8 - param6,
            param11 + param5,
            param12 + param6,
            param9 - param7 - param5,
            param10 - param8 - param6
         );
         blitRepeating(
            param0,
            param1 + param3 - param7,
            param2 + param6,
            param5,
            param4 - param8 - param6,
            param11 + param9 - param7,
            param12 + param6,
            param7,
            param10 - param8 - param6
         );
      }
   }

   public static void blitRepeating(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8) {
      int var9 = param1;
      IntIterator var10 = slices(param3, param7);

      while (var10.hasNext()) {
         int var11 = var10.nextInt();
         int var12 = (param7 - var11) / 2;
         int var13 = param2;
         IntIterator var14 = slices(param4, param8);

         while (var14.hasNext()) {
            int var15 = var14.nextInt();
            int var16 = (param8 - var15) / 2;
            blit(param0, var9, var13, param5 + var12, param6 + var16, var11, var15);
            var13 += var15;
         }

         var9 += var11;
      }
   }

   private static IntIterator slices(int param0, int param1) {
      int var2 = Mth.positiveCeilDiv(param0, param1);
      return new Divisor(param0, var2);
   }

   static class ScissorStack {
      private final Deque<ScreenRectangle> stack = new ArrayDeque<>();

      public ScreenRectangle push(ScreenRectangle param0) {
         ScreenRectangle var2 = this.stack.peekLast();
         if (var2 != null) {
            ScreenRectangle var3 = Objects.requireNonNullElse(param0.intersection(var2), ScreenRectangle.empty());
            this.stack.addLast(var3);
            return var3;
         } else {
            this.stack.addLast(param0);
            return param0;
         }
      }

      @Nullable
      public ScreenRectangle pop() {
         if (this.stack.isEmpty()) {
            throw new IllegalStateException("Scissor stack underflow");
         }

         this.stack.removeLast();
         return this.stack.peekLast();
      }
   }
}
