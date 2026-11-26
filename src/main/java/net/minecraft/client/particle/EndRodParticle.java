package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class EndRodParticle extends SimpleAnimatedParticle {
   EndRodParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, SpriteSet param13) {
      super(param0, param1, param3, param5, param13, 0.0125F);
      this.xd = param7;
      this.yd = param9;
      this.zd = param11;
      this.quadSize *= 0.75F;
      this.lifetime = 60 + this.random.nextInt(12);
      this.setFadeColor(15916745);
      this.setSpriteFromAge(param13);
   }

   @Override
   public void move(double param0, double param2, double param4) {
      this.setBoundingBox(this.getBoundingBox().move(param0, param2, param4));
      this.setLocationFromBoundingbox();
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new EndRodParticle(param1, param2, param4, param6, param8, param10, param12, this.sprites);
      }
   }
}
