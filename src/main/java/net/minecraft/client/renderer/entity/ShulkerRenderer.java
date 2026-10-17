package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.layers.ShulkerHeadLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ShulkerRenderer extends MobRenderer<Shulker, ShulkerModel<Shulker>> {
   private static final ResourceLocation DEFAULT_TEXTURE_LOCATION = new ResourceLocation(
      "textures/" + Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION.texture().getPath() + ".png"
   );
   private static final ResourceLocation[] TEXTURE_LOCATION = Sheets.SHULKER_TEXTURE_LOCATION
      .stream()
      .map(param0 -> new ResourceLocation("textures/" + param0.texture().getPath() + ".png"))
      .toArray(ResourceLocation[]::new);

   public ShulkerRenderer(EntityRendererProvider.Context param0) {
      super(param0, new ShulkerModel<>(param0.bakeLayer(ModelLayers.SHULKER)), 0.0F);
      this.addLayer(new ShulkerHeadLayer(this));
   }

   public Vec3 getRenderOffset(Shulker param0, float param1) {
      return param0.getRenderPosition(param1).orElse(super.getRenderOffset(param0, param1));
   }

   public boolean shouldRender(Shulker param0, Frustum param1, double param2, double param4, double param6) {
      return super.shouldRender(param0, param1, param2, param4, param6) ? true : param0.getRenderPosition(0.0F).filter(l_param2 -> {
         EntityType<?> l_var3 = param0.getType();
         float l_var4 = l_var3.getHeight() / 2.0F;
         float l_var5 = l_var3.getWidth() / 2.0F;
         Vec3 l_var6 = Vec3.atBottomCenterOf(param0.blockPosition());
         return param1.isVisible(new AABB(l_param2.x, l_param2.y + l_var4, l_param2.z, l_var6.x, l_var6.y + l_var4, l_var6.z).inflate(l_var5, l_var4, l_var5));
      }).isPresent();
   }

   public ResourceLocation getTextureLocation(Shulker param0) {
      return getTextureLocation(param0.getColor());
   }

   public static ResourceLocation getTextureLocation(@Nullable DyeColor param0) {
      return param0 == null ? DEFAULT_TEXTURE_LOCATION : TEXTURE_LOCATION[param0.getId()];
   }

   protected void setupRotations(Shulker param0, PoseStack param1, float param2, float param3, float param4) {
      super.setupRotations(param0, param1, param2, param3 + 180.0F, param4);
      param1.translate(0.0, 0.5, 0.0);
      param1.mulPose(param0.getAttachFace().getOpposite().getRotation());
      param1.translate(0.0, -0.5, 0.0);
   }
}
