package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class WhiteAshParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected WhiteAshParticle(
      ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, float param13, SpriteSet param14
   ) {
      super(param0, param1, param3, param5, 0.1F, -0.1F, 0.1F, param7, param9, param11, param13, param14, 0.0F, 20, 0.0125F, false);
      this.rCol = 0.7294118F;
      this.gCol = 0.69411767F;
      this.bCol = 0.7607843F;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         RandomSource var15 = param1.random;
         double var16 = var15.nextFloat() * -1.9 * var15.nextFloat() * 0.1;
         double var18 = var15.nextFloat() * -0.5 * var15.nextFloat() * 0.1 * 5.0;
         double var20 = var15.nextFloat() * -1.9 * var15.nextFloat() * 0.1;
         return new WhiteAshParticle(param1, param2, param4, param6, var16, var18, var20, 1.0F, this.sprites);
      }
   }
}
