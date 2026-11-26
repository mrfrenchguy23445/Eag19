package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class SpinAttackEffectLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
   public static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/trident_riptide.png");
   public static final String BOX = "box";
   private final ModelPart box;

   public SpinAttackEffectLayer(RenderLayerParent<T, PlayerModel<T>> param0, EntityModelSet param1) {
      super(param0);
      ModelPart var3 = param1.bakeLayer(ModelLayers.PLAYER_SPIN_ATTACK);
      this.box = var3.getChild("box");
   }

   public static LayerDefinition createLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild("box", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 32.0F, 16.0F), PartPose.ZERO);
      return LayerDefinition.create(var0, 64, 64);
   }

   public void render(
      PoseStack param0, MultiBufferSource param1, int param2, T param3, float param4, float param5, float param6, float param7, float param8, float param9
   ) {
      if (param3.isAutoSpinAttack()) {
         VertexConsumer var11 = param1.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

         for (int var12 = 0; var12 < 3; var12++) {
            param0.pushPose();
            float var13 = param7 * -(45 + var12 * 5);
            param0.mulPose(Axis.YP.rotationDegrees(var13));
            float var14 = 0.75F * var12;
            param0.scale(var14, var14, var14);
            param0.translate(0.0F, -0.2F + 0.6F * var12, 0.0F);
            this.box.render(param0, var11, param2, OverlayTexture.NO_OVERLAY);
            param0.popPose();
         }
      }
   }
}
