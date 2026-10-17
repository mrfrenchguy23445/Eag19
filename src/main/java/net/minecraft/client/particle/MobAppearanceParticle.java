package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ElderGuardianRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class MobAppearanceParticle extends Particle {
   private final Model model;
   private final RenderType renderType = RenderType.entityTranslucent(ElderGuardianRenderer.GUARDIAN_ELDER_LOCATION);

   MobAppearanceParticle(ClientLevel param0, double param1, double param3, double param5) {
      super(param0, param1, param3, param5);
      this.model = new GuardianModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELDER_GUARDIAN));
      this.gravity = 0.0F;
      this.lifetime = 30;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.CUSTOM;
   }

   @Override
   public void render(VertexConsumer param0, Camera param1, float param2) {
      float var4 = (this.age + param2) / this.lifetime;
      float var5 = 0.05F + 0.5F * Mth.sin(var4 * (float) Math.PI);
      PoseStack var6 = new PoseStack();
      var6.mulPose(param1.rotation());
      var6.mulPose(Axis.XP.rotationDegrees(150.0F * var4 - 60.0F));
      var6.scale(-1.0F, -1.0F, 1.0F);
      var6.translate(0.0F, -1.101F, 1.5F);
      MultiBufferSource.BufferSource var7 = Minecraft.getInstance().renderBuffers().bufferSource();
      VertexConsumer var8 = var7.getBuffer(this.renderType);
      this.model.renderToBuffer(var6, var8, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, var5);
      var7.endBatch();
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new MobAppearanceParticle(param1, param2, param4, param6);
      }
   }
}
