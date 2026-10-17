package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class HugeExplosionParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   protected HugeExplosionParticle(ClientLevel param0, double param1, double param3, double param5, double param7, SpriteSet param9) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
      this.lifetime = 6 + this.random.nextInt(4);
      float var11 = this.random.nextFloat() * 0.6F + 0.4F;
      this.rCol = var11;
      this.gCol = var11;
      this.bCol = var11;
      this.quadSize = 2.0F * (1.0F - (float)param7 * 0.5F);
      this.sprites = param9;
      this.setSpriteFromAge(param9);
   }

   @Override
   public int getLightColor(float param0) {
      return 15728880;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
      }
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_LIT;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new HugeExplosionParticle(param1, param2, param4, param6, param8, this.sprites);
      }
   }
}
