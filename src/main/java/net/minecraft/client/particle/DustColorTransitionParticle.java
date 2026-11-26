package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustColorTransitionOptions;
import org.joml.Vector3f;

public class DustColorTransitionParticle extends DustParticleBase<DustColorTransitionOptions> {
   private final Vector3f fromColor;
   private final Vector3f toColor;

   protected DustColorTransitionParticle(
      ClientLevel param0,
      double param1,
      double param3,
      double param5,
      double param7,
      double param9,
      double param11,
      DustColorTransitionOptions param13,
      SpriteSet param14
   ) {
      super(param0, param1, param3, param5, param7, param9, param11, param13, param14);
      float var16 = this.random.nextFloat() * 0.4F + 0.6F;
      this.fromColor = this.randomizeColor(param13.getFromColor(), var16);
      this.toColor = this.randomizeColor(param13.getToColor(), var16);
   }

   private Vector3f randomizeColor(Vector3f param0, float param1) {
      return new Vector3f(this.randomizeColor(param0.x(), param1), this.randomizeColor(param0.y(), param1), this.randomizeColor(param0.z(), param1));
   }

   private void lerpColors(float param0) {
      float var2 = (this.age + param0) / (this.lifetime + 1.0F);
      Vector3f var3 = new Vector3f(this.fromColor).lerp(this.toColor, var2);
      this.rCol = var3.x();
      this.gCol = var3.y();
      this.bCol = var3.z();
   }

   @Override
   public void render(VertexConsumer param0, Camera param1, float param2) {
      this.lerpColors(param2);
      super.render(param0, param1, param2);
   }

   public static class Provider implements ParticleProvider<DustColorTransitionOptions> {
      private final SpriteSet sprites;

      public Provider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         DustColorTransitionOptions param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new DustColorTransitionParticle(param1, param2, param4, param6, param8, param10, param12, param0, this.sprites);
      }
   }
}
