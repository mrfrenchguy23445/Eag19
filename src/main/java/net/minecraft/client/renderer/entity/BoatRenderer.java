package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ChestRaftModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.RaftModel;
import net.minecraft.client.model.WaterPatchModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;

public class BoatRenderer extends EntityRenderer<Boat> {
   private final Map<Boat.Type, Pair<ResourceLocation, ListModel<Boat>>> boatResources;

   public BoatRenderer(EntityRendererProvider.Context param0, boolean param1) {
      super(param0);
      this.shadowRadius = 0.8F;
      this.boatResources = Stream.of(Boat.Type.values())
         .collect(
            ImmutableMap.toImmutableMap(
               l_param0 -> l_param0,
               l_param2 -> Pair.of(new ResourceLocation(getTextureLocation(l_param2, param1)), this.createBoatModel(param0, l_param2, param1))
            )
         );
   }

   private ListModel<Boat> createBoatModel(EntityRendererProvider.Context param0, Boat.Type param1, boolean param2) {
      ModelLayerLocation var4 = param2 ? ModelLayers.createChestBoatModelName(param1) : ModelLayers.createBoatModelName(param1);
      ModelPart var5 = param0.bakeLayer(var4);
      if (param1 == Boat.Type.BAMBOO) {
         return param2 ? new ChestRaftModel(var5) : new RaftModel(var5);
      } else {
         return param2 ? new ChestBoatModel(var5) : new BoatModel(var5);
      }
   }

   private static String getTextureLocation(Boat.Type param0, boolean param1) {
      return param1 ? "textures/entity/chest_boat/" + param0.getName() + ".png" : "textures/entity/boat/" + param0.getName() + ".png";
   }

   public void render(Boat param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      param3.pushPose();
      param3.translate(0.0F, 0.375F, 0.0F);
      param3.mulPose(Axis.YP.rotationDegrees(180.0F - param1));
      float var7 = param0.getHurtTime() - param2;
      float var8 = param0.getDamage() - param2;
      if (var8 < 0.0F) {
         var8 = 0.0F;
      }

      if (var7 > 0.0F) {
         param3.mulPose(Axis.XP.rotationDegrees(Mth.sin(var7) * var7 * var8 / 10.0F * param0.getHurtDir()));
      }

      float var9 = param0.getBubbleAngle(param2);
      if (!Mth.equal(var9, 0.0F)) {
         param3.mulPose(new Quaternionf().setAngleAxis(param0.getBubbleAngle(param2) * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
      }

      Pair<ResourceLocation, ListModel<Boat>> var10 = this.boatResources.get(param0.getVariant());
      ResourceLocation var11 = var10.getFirst();
      ListModel<Boat> var12 = var10.getSecond();
      param3.scale(-1.0F, -1.0F, 1.0F);
      param3.mulPose(Axis.YP.rotationDegrees(90.0F));
      var12.setupAnim(param0, param2, 0.0F, -0.1F, 0.0F, 0.0F);
      VertexConsumer var13 = param4.getBuffer(var12.renderType(var11));
      var12.renderToBuffer(param3, var13, param5, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
      if (!param0.isUnderWater()) {
         VertexConsumer var14 = param4.getBuffer(RenderType.waterMask());
         if (var12 instanceof WaterPatchModel var15) {
            var15.waterPatch().render(param3, var14, param5, OverlayTexture.NO_OVERLAY);
         }
      }

      param3.popPose();
      super.render(param0, param1, param2, param3, param4, param5);
   }

   public ResourceLocation getTextureLocation(Boat param0) {
      return this.boatResources.get(param0.getVariant()).getFirst();
   }
}
