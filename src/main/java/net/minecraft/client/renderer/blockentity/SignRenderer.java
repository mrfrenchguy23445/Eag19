package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;

public class SignRenderer implements BlockEntityRenderer<SignBlockEntity> {
   private static final String STICK = "stick";
   private static final int BLACK_TEXT_OUTLINE_COLOR = -988212;
   private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);
   private final Map<WoodType, SignRenderer.SignModel> signModels;
   private final Font font;

   public SignRenderer(BlockEntityRendererProvider.Context param0) {
      this.signModels = WoodType.values()
         .collect(
            ImmutableMap.toImmutableMap(
               l_param0 -> l_param0, l_param1 -> new SignRenderer.SignModel(param0.bakeLayer(ModelLayers.createSignModelName(l_param1)))
            )
         );
      this.font = param0.getFont();
   }

   public void render(SignBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      BlockState var7 = param0.getBlockState();
      param2.pushPose();
      float var8 = 0.6666667F;
      WoodType var9 = SignBlock.getWoodType(var7.getBlock());
      SignRenderer.SignModel var10 = this.signModels.get(var9);
      if (var7.getBlock() instanceof StandingSignBlock) {
         param2.translate(0.5F, 0.5F, 0.5F);
         float var11 = -RotationSegment.convertToDegrees(var7.getValue(StandingSignBlock.ROTATION));
         param2.mulPose(Axis.YP.rotationDegrees(var11));
         var10.stick.visible = true;
      } else {
         param2.translate(0.5F, 0.5F, 0.5F);
         float var11 = -var7.getValue(WallSignBlock.FACING).toYRot();
         param2.mulPose(Axis.YP.rotationDegrees(var11));
         param2.translate(0.0F, -0.3125F, -0.4375F);
         var10.stick.visible = false;
      }

      this.renderSign(param2, param3, param4, param5, 0.6666667F, var9, var10);
      this.renderSignText(param0, param2, param3, param4, 0.6666667F);
   }

   void renderSign(PoseStack param0, MultiBufferSource param1, int param2, int param3, float param4, WoodType param5, Model param6) {
      param0.pushPose();
      param0.scale(param4, -param4, -param4);
      Material var8 = this.getSignMaterial(param5);
      VertexConsumer var9 = var8.buffer(param1, param6::renderType);
      this.renderSignModel(param0, param2, param3, param6, var9);
      param0.popPose();
   }

   void renderSignModel(PoseStack param0, int param1, int param2, Model param3, VertexConsumer param4) {
      SignRenderer.SignModel var6 = (SignRenderer.SignModel)param3;
      var6.root.render(param0, param4, param1, param2);
   }

   Material getSignMaterial(WoodType param0) {
      return Sheets.getSignMaterial(param0);
   }

   void renderSignText(SignBlockEntity param0, PoseStack param1, MultiBufferSource param2, int param3, float param4) {
      float var6 = 0.015625F * param4;
      Vec3 var7 = this.getTextOffset(param4);
      param1.translate(var7.x, var7.y, var7.z);
      param1.scale(var6, -var6, var6);
      int var8 = getDarkColor(param0);
      int var9 = 4 * param0.getTextLineHeight() / 2;
      FormattedCharSequence[] var10 = param0.getRenderMessages(Minecraft.getInstance().isTextFilteringEnabled(), l_param1 -> {
         List<FormattedCharSequence> l_var3 = this.font.split(l_param1, param0.getMaxTextLineWidth());
         return l_var3.isEmpty() ? FormattedCharSequence.EMPTY : l_var3.get(0);
      });
      int var11;
      boolean var12;
      int var13;
      if (param0.hasGlowingText()) {
         var11 = param0.getColor().getTextColor();
         var12 = isOutlineVisible(param0, var11);
         var13 = 15728880;
      } else {
         var11 = var8;
         var12 = false;
         var13 = param3;
      }

      for (int var14 = 0; var14 < 4; var14++) {
         FormattedCharSequence var15 = var10[var14];
         float var16 = -this.font.width(var15) / 2;
         if (var12) {
            this.font.drawInBatch8xOutline(var15, var16, var14 * param0.getTextLineHeight() - var9, var11, var8, param1.last().pose(), param2, var13);
         } else {
            this.font
               .drawInBatch(
                  var15, var16, var14 * param0.getTextLineHeight() - var9, var11, false, param1.last().pose(), param2, Font.DisplayMode.NORMAL, 0, var13
               );
         }
      }

      param1.popPose();
   }

   Vec3 getTextOffset(float param0) {
      return new Vec3(0.0, 0.5F * param0, 0.07F * param0);
   }

   static boolean isOutlineVisible(SignBlockEntity param0, int param1) {
      if (param1 == DyeColor.BLACK.getTextColor()) {
         return true;
      }

      Minecraft var2 = Minecraft.getInstance();
      LocalPlayer var3 = var2.player;
      if (var3 != null && var2.options.getCameraType().isFirstPerson() && var3.isScoping()) {
         return true;
      }

      Entity var4 = var2.getCameraEntity();
      return var4 != null && var4.distanceToSqr(Vec3.atCenterOf(param0.getBlockPos())) < OUTLINE_RENDER_DISTANCE;
   }

   static int getDarkColor(SignBlockEntity param0) {
      int var1 = param0.getColor().getTextColor();
      if (var1 == DyeColor.BLACK.getTextColor() && param0.hasGlowingText()) {
         return -988212;
      }

      double var2 = 0.4;
      int var4 = (int)(FastColor.ARGB32.red(var1) * 0.4);
      int var5 = (int)(FastColor.ARGB32.green(var1) * 0.4);
      int var6 = (int)(FastColor.ARGB32.blue(var1) * 0.4);
      return FastColor.ARGB32.color(0, var4, var5, var6);
   }

   public static SignRenderer.SignModel createSignModel(EntityModelSet param0, WoodType param1) {
      return new SignRenderer.SignModel(param0.bakeLayer(ModelLayers.createSignModelName(param1)));
   }

   public static LayerDefinition createSignLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild("sign", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -14.0F, -1.0F, 24.0F, 12.0F, 2.0F), PartPose.ZERO);
      var1.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(0, 14).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.ZERO);
      return LayerDefinition.create(var0, 64, 32);
   }

   public static final class SignModel extends Model {
      public final ModelPart root;
      public final ModelPart stick;

      public SignModel(ModelPart param0) {
         super(RenderType::entityCutoutNoCull);
         this.root = param0;
         this.stick = param0.getChild("stick");
      }

      @Override
      public void renderToBuffer(PoseStack param0, VertexConsumer param1, int param2, int param3, float param4, float param5, float param6, float param7) {
         this.root.render(param0, param1, param2, param3, param4, param5, param6, param7);
      }
   }
}
