package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class SonicBoomParticle extends HugeExplosionParticle {
   protected SonicBoomParticle(ClientLevel param0, double param1, double param3, double param5, double param7, SpriteSet param9) {
      super(param0, param1, param3, param5, param7, param9);
      this.lifetime = 16;
      this.quadSize = 1.5F;
      this.setSpriteFromAge(param9);
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new SonicBoomParticle(param1, param2, param4, param6, param8, this.sprites);
      }
   }
}
