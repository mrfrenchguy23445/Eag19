package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class CritParticle extends TextureSheetParticle {
   CritParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
      this.friction = 0.7F;
      this.gravity = 0.5F;
      this.xd *= 0.1F;
      this.yd *= 0.1F;
      this.zd *= 0.1F;
      this.xd += param7 * 0.4;
      this.yd += param9 * 0.4;
      this.zd += param11 * 0.4;
      float var14 = (float)(Math.random() * 0.3F + 0.6F);
      this.rCol = var14;
      this.gCol = var14;
      this.bCol = var14;
      this.quadSize *= 0.75F;
      this.lifetime = Math.max((int)(6.0 / (Math.random() * 0.8 + 0.6)), 1);
      this.hasPhysics = false;
      this.tick();
   }

   @Override
   public float getQuadSize(float param0) {
      return this.quadSize * Mth.clamp((this.age + param0) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      this.gCol *= 0.96F;
      this.bCol *= 0.9F;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public static class DamageIndicatorProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DamageIndicatorProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         CritParticle var15 = new CritParticle(param1, param2, param4, param6, param8, param10 + 1.0, param12);
         var15.setLifetime(20);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }

   public static class MagicProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public MagicProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         CritParticle var15 = new CritParticle(param1, param2, param4, param6, param8, param10, param12);
         var15.rCol *= 0.3F;
         var15.gCol *= 0.8F;
         var15.pickSprite(this.sprite);
         return var15;
      }
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         CritParticle var15 = new CritParticle(param1, param2, param4, param6, param8, param10, param12);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }
}
