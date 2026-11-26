package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class CampfireSmokeParticle extends TextureSheetParticle {
   CampfireSmokeParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, boolean param13) {
      super(param0, param1, param3, param5);
      this.scale(3.0F);
      this.setSize(0.25F, 0.25F);
      if (param13) {
         this.lifetime = this.random.nextInt(50) + 280;
      } else {
         this.lifetime = this.random.nextInt(50) + 80;
      }

      this.gravity = 3.0E-6F;
      this.xd = param7;
      this.yd = param9 + this.random.nextFloat() / 500.0F;
      this.zd = param11;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ < this.lifetime && !(this.alpha <= 0.0F)) {
         this.xd = this.xd + this.random.nextFloat() / 5000.0F * (this.random.nextBoolean() ? 1 : -1);
         this.zd = this.zd + this.random.nextFloat() / 5000.0F * (this.random.nextBoolean() ? 1 : -1);
         this.yd = this.yd - this.gravity;
         this.move(this.xd, this.yd, this.zd);
         if (this.age >= this.lifetime - 60 && this.alpha > 0.01F) {
            this.alpha -= 0.015F;
         }
      } else {
         this.remove();
      }
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static class CosyProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public CosyProvider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         CampfireSmokeParticle var15 = new CampfireSmokeParticle(param1, param2, param4, param6, param8, param10, param12, false);
         var15.setAlpha(0.9F);
         var15.pickSprite(this.sprites);
         return var15;
      }
   }

   public static class SignalProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SignalProvider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         CampfireSmokeParticle var15 = new CampfireSmokeParticle(param1, param2, param4, param6, param8, param10, param12, true);
         var15.setAlpha(0.95F);
         var15.pickSprite(this.sprites);
         return var15;
      }
   }
}
