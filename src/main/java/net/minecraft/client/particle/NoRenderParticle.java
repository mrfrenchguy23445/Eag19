package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;

public class NoRenderParticle extends Particle {
   protected NoRenderParticle(ClientLevel param0, double param1, double param3, double param5) {
      super(param0, param1, param3, param5);
   }

   protected NoRenderParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      super(param0, param1, param3, param5, param7, param9, param11);
   }

   @Override
   public final void render(VertexConsumer param0, Camera param1, float param2) {
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.NO_RENDER;
   }
}
