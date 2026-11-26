package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;

public class DustParticle extends DustParticleBase<DustParticleOptions> {
   protected DustParticle(
      ClientLevel param0,
      double param1,
      double param3,
      double param5,
      double param7,
      double param9,
      double param11,
      DustParticleOptions param13,
      SpriteSet param14
   ) {
      super(param0, param1, param3, param5, param7, param9, param11, param13, param14);
   }

   public static class Provider implements ParticleProvider<DustParticleOptions> {
      private final SpriteSet sprites;

      public Provider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         DustParticleOptions param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new DustParticle(param1, param2, param4, param6, param8, param10, param12, param0, this.sprites);
      }
   }
}
