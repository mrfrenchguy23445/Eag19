package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public abstract class DisplayRenderer<T extends Display> extends EntityRenderer<T> {
   private static final float MAX_SHADOW_RADIUS = 64.0F;
   private final EntityRenderDispatcher entityRenderDispatcher;

   protected DisplayRenderer(EntityRendererProvider.Context param0) {
      super(param0);
      this.entityRenderDispatcher = param0.getEntityRenderDispatcher();
   }

   public ResourceLocation getTextureLocation(T param0) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   public void render(T param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      float var7 = param0.calculateInterpolationProgress(param2);
      this.shadowRadius = Math.min(param0.getShadowRadius(var7), 64.0F);
      this.shadowStrength = param0.getShadowStrength(var7);
      int var8 = param0.getPackedBrightnessOverride();
      int var9 = var8 != -1 ? var8 : param5;
      super.render(param0, param1, param2, param3, param4, var9);
      param3.pushPose();
      param3.mulPose(this.calculateOrientation(param0));
      Transformation var10 = param0.transformation(var7);
      param3.mulPoseMatrix(var10.getMatrix());
      param3.last().normal().rotate(var10.getLeftRotation()).rotate(var10.getRightRotation());
      this.renderInner(param0, param3, param4, var9, var7);
      param3.popPose();
   }

   private Quaternionf calculateOrientation(T param0) {
      Camera var2 = this.entityRenderDispatcher.camera;

      return switch (param0.getBillboardConstraints()) {
         case FIXED -> param0.orientation();
         case HORIZONTAL -> new Quaternionf().rotationYXZ((float) (-Math.PI / 180.0) * param0.getYRot(), (float) (-Math.PI / 180.0) * var2.getXRot(), 0.0F);
         case VERTICAL -> new Quaternionf()
            .rotationYXZ((float) Math.PI - (float) (Math.PI / 180.0) * var2.getYRot(), (float) (Math.PI / 180.0) * param0.getXRot(), 0.0F);
         case CENTER -> new Quaternionf()
            .rotationYXZ((float) Math.PI - (float) (Math.PI / 180.0) * var2.getYRot(), (float) (-Math.PI / 180.0) * var2.getXRot(), 0.0F);
      };
   }

   protected abstract void renderInner(T var1, PoseStack var2, MultiBufferSource var3, int var4, float var5);

   public static class BlockDisplayRenderer extends DisplayRenderer<Display.BlockDisplay> {
      private final BlockRenderDispatcher blockRenderer;

      protected BlockDisplayRenderer(EntityRendererProvider.Context param0) {
         super(param0);
         this.blockRenderer = param0.getBlockRenderDispatcher();
      }

      public void renderInner(Display.BlockDisplay param0, PoseStack param1, MultiBufferSource param2, int param3, float param4) {
         this.blockRenderer.renderSingleBlock(param0.getBlockState(), param1, param2, param3, OverlayTexture.NO_OVERLAY);
      }
   }

   public static class ItemDisplayRenderer extends DisplayRenderer<Display.ItemDisplay> {
      private final ItemRenderer itemRenderer;

      protected ItemDisplayRenderer(EntityRendererProvider.Context param0) {
         super(param0);
         this.itemRenderer = param0.getItemRenderer();
      }

      public void renderInner(Display.ItemDisplay param0, PoseStack param1, MultiBufferSource param2, int param3, float param4) {
         this.itemRenderer
            .renderStatic(
               param0.getItemStack(), param0.getItemTransform(), param3, OverlayTexture.NO_OVERLAY, param1, param2, param0.getLevel(), param0.getId()
            );
      }
   }

   public static class TextDisplayRenderer extends DisplayRenderer<Display.TextDisplay> {
      private final Font font;

      protected TextDisplayRenderer(EntityRendererProvider.Context param0) {
         super(param0);
         this.font = param0.getFont();
      }

      private Display.TextDisplay.CachedInfo splitLines(Component param0, int param1) {
         List<FormattedCharSequence> var3 = this.font.split(param0, param1);
         List<Display.TextDisplay.CachedLine> var4 = new ArrayList<>(var3.size());
         int var5 = 0;

         for (FormattedCharSequence var7 : var3) {
            int var8 = this.font.width(var7);
            var5 = Math.max(var5, var8);
            var4.add(new Display.TextDisplay.CachedLine(var7, var8));
         }

         return new Display.TextDisplay.CachedInfo(var4, var5);
      }

      public void renderInner(Display.TextDisplay param0, PoseStack param1, MultiBufferSource param2, int param3, float param4) {
         byte var6 = param0.getFlags();
         boolean var7 = (var6 & 2) != 0;
         boolean var8 = (var6 & 4) != 0;
         boolean var9 = (var6 & 1) != 0;
         Display.TextDisplay.Align var10 = Display.TextDisplay.getAlign(var6);
         byte var11 = param0.getTextOpacity(param4);
         int var12;
         if (var8) {
            float var13 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            var12 = (int)(var13 * 255.0F) << 24;
         } else {
            var12 = param0.getBackgroundColor(param4);
         }

         float var13 = 0.0F;
         Matrix4f var14 = param1.last().pose();
         var14.rotate((float) Math.PI, 0.0F, 1.0F, 0.0F);
         var14.scale(-0.025F, -0.025F, -0.025F);
         Display.TextDisplay.CachedInfo var15 = param0.cacheDisplay(this::splitLines);
         int var16 = 9 + 1;
         int var17 = var15.width();
         int var18 = var15.lines().size() * var16;
         var14.translate(1.0F - var17 / 2.0F, -var18, 0.0F);
         if (var12 != 0) {
            VertexConsumer var19 = param2.getBuffer(var7 ? RenderType.textBackgroundSeeThrough() : RenderType.textBackground());
            var19.vertex(var14, -1.0F, -1.0F, 0.0F).color(var12).uv2(param3).endVertex();
            var19.vertex(var14, -1.0F, var18, 0.0F).color(var12).uv2(param3).endVertex();
            var19.vertex(var14, var17, var18, 0.0F).color(var12).uv2(param3).endVertex();
            var19.vertex(var14, var17, -1.0F, 0.0F).color(var12).uv2(param3).endVertex();
         }

         for (Display.TextDisplay.CachedLine var20 : var15.lines()) {
            float var21 = switch (var10) {
               case LEFT -> 0.0F;
               case RIGHT -> var17 - var20.width();
               case CENTER -> var17 / 2.0F - var20.width() / 2.0F;
            };
            this.font
               .drawInBatch(
                  var20.contents(),
                  var21,
                  var13,
                  var11 << 24 | 16777215,
                  var9,
                  var14,
                  param2,
                  var7 ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET,
                  0,
                  param3
               );
            var13 += var16;
         }
      }
   }
}
