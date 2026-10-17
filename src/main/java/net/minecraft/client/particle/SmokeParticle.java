package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class SmokeParticle extends BaseAshSmokeParticle {
   protected SmokeParticle(
      ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, float param13, SpriteSet param14
   ) {
      super(param0, param1, param3, param5, 0.1F, 0.1F, 0.1F, param7, param9, param11, param13, param14, 0.3F, 8, -0.1F, true);
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new SmokeParticle(param1, param2, param4, param6, param8, param10, param12, 1.0F, this.sprites);
      }
   }
}
