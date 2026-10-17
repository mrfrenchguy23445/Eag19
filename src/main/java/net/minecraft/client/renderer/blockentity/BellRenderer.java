package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BellBlockEntity;

public class BellRenderer implements BlockEntityRenderer<BellBlockEntity> {
   public static final Material BELL_RESOURCE_LOCATION = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/bell/bell_body"));
   private static final String BELL_BODY = "bell_body";
   private final ModelPart bellBody;

   public BellRenderer(BlockEntityRendererProvider.Context param0) {
      ModelPart var2 = param0.bakeLayer(ModelLayers.BELL);
      this.bellBody = var2.getChild("bell_body");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      PartDefinition var2 = var1.addOrReplaceChild(
         "bell_body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offset(8.0F, 12.0F, 8.0F)
      );
      var2.addOrReplaceChild(
         "bell_base", CubeListBuilder.create().texOffs(0, 13).addBox(4.0F, 4.0F, 4.0F, 8.0F, 2.0F, 8.0F), PartPose.offset(-8.0F, -12.0F, -8.0F)
      );
      return LayerDefinition.create(var0, 32, 32);
   }

   public void render(BellBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      float var7 = param0.ticks + param1;
      float var8 = 0.0F;
      float var9 = 0.0F;
      if (param0.shaking) {
         float var10 = Mth.sin(var7 / (float) Math.PI) / (4.0F + var7 / 3.0F);
         if (param0.clickDirection == Direction.NORTH) {
            var8 = -var10;
         } else if (param0.clickDirection == Direction.SOUTH) {
            var8 = var10;
         } else if (param0.clickDirection == Direction.EAST) {
            var9 = -var10;
         } else if (param0.clickDirection == Direction.WEST) {
            var9 = var10;
         }
      }

      this.bellBody.xRot = var8;
      this.bellBody.zRot = var9;
      VertexConsumer var10 = BELL_RESOURCE_LOCATION.buffer(param3, RenderType::entitySolid);
      this.bellBody.render(param2, var10, param4, param5);
   }
}
