package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;

public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity> {
   private static final int BANNER_WIDTH = 20;
   private static final int BANNER_HEIGHT = 40;
   private static final int MAX_PATTERNS = 16;
   public static final String FLAG = "flag";
   private static final String POLE = "pole";
   private static final String BAR = "bar";
   private final ModelPart flag;
   private final ModelPart pole;
   private final ModelPart bar;

   public BannerRenderer(BlockEntityRendererProvider.Context param0) {
      ModelPart var2 = param0.bakeLayer(ModelLayers.BANNER);
      this.flag = var2.getChild("flag");
      this.pole = var2.getChild("pole");
      this.bar = var2.getChild("bar");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild("flag", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F), PartPose.ZERO);
      var1.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -30.0F, -1.0F, 2.0F, 42.0F, 2.0F), PartPose.ZERO);
      var1.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(0, 42).addBox(-10.0F, -32.0F, -1.0F, 20.0F, 2.0F, 2.0F), PartPose.ZERO);
      return LayerDefinition.create(var0, 64, 64);
   }

   public void render(BannerBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      List<Pair<Holder<BannerPattern>, DyeColor>> var7 = param0.getPatterns();
      float var8 = 0.6666667F;
      boolean var9 = param0.getLevel() == null;
      param2.pushPose();
      long var10;
      if (var9) {
         var10 = 0L;
         param2.translate(0.5F, 0.5F, 0.5F);
         this.pole.visible = true;
      } else {
         var10 = param0.getLevel().getGameTime();
         BlockState var12 = param0.getBlockState();
         if (var12.getBlock() instanceof BannerBlock) {
            param2.translate(0.5F, 0.5F, 0.5F);
            float var13 = -RotationSegment.convertToDegrees(var12.getValue(BannerBlock.ROTATION));
            param2.mulPose(Axis.YP.rotationDegrees(var13));
            this.pole.visible = true;
         } else {
            param2.translate(0.5F, -0.16666667F, 0.5F);
            float var13 = -var12.getValue(WallBannerBlock.FACING).toYRot();
            param2.mulPose(Axis.YP.rotationDegrees(var13));
            param2.translate(0.0F, -0.3125F, -0.4375F);
            this.pole.visible = false;
         }
      }

      param2.pushPose();
      param2.scale(0.6666667F, -0.6666667F, -0.6666667F);
      VertexConsumer var12 = ModelBakery.BANNER_BASE.buffer(param3, RenderType::entitySolid);
      this.pole.render(param2, var12, param4, param5);
      this.bar.render(param2, var12, param4, param5);
      BlockPos var13 = param0.getBlockPos();
      float var14 = ((float)Math.floorMod(var13.getX() * 7 + var13.getY() * 9 + var13.getZ() * 13 + var10, 100L) + param1) / 100.0F;
      this.flag.xRot = (-0.0125F + 0.01F * Mth.cos((float) (Math.PI * 2) * var14)) * (float) Math.PI;
      this.flag.y = -32.0F;
      renderPatterns(param2, param3, param4, param5, this.flag, ModelBakery.BANNER_BASE, true, var7);
      param2.popPose();
      param2.popPose();
   }

   public static void renderPatterns(
      PoseStack param0,
      MultiBufferSource param1,
      int param2,
      int param3,
      ModelPart param4,
      Material param5,
      boolean param6,
      List<Pair<Holder<BannerPattern>, DyeColor>> param7
   ) {
      renderPatterns(param0, param1, param2, param3, param4, param5, param6, param7, false);
   }

   public static void renderPatterns(
      PoseStack param0,
      MultiBufferSource param1,
      int param2,
      int param3,
      ModelPart param4,
      Material param5,
      boolean param6,
      List<Pair<Holder<BannerPattern>, DyeColor>> param7,
      boolean param8
   ) {
      param4.render(param0, param5.buffer(param1, RenderType::entitySolid, param8), param2, param3);

      for (int var9 = 0; var9 < 17 && var9 < param7.size(); var9++) {
         Pair<Holder<BannerPattern>, DyeColor> var10 = param7.get(var9);
         float[] var11 = var10.getSecond().getTextureDiffuseColors();
         var10.getFirst()
            .unwrapKey()
            .map(
               l_param1 -> param6
                  ? Sheets.getBannerMaterial((ResourceKey<BannerPattern>)l_param1)
                  : Sheets.getShieldMaterial((ResourceKey<BannerPattern>)l_param1)
            )
            .ifPresent(
               l_param6 -> param4.render(param0, l_param6.buffer(param1, RenderType::entityNoOutline), param2, param3, var11[0], var11[1], var11[2], 1.0F)
            );
      }
   }
}
