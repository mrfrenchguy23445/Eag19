package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class SoulParticle extends RisingParticle {
   private final SpriteSet sprites;
   protected boolean isGlowing;

   SoulParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, SpriteSet param13) {
      super(param0, param1, param3, param5, param7, param9, param11);
      this.sprites = param13;
      this.scale(1.5F);
      this.setSpriteFromAge(param13);
   }

   @Override
   public int getLightColor(float param0) {
      return this.isGlowing ? 240 : super.getLightColor(param0);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   public static class EmissiveProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public EmissiveProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         SoulParticle var15 = new SoulParticle(param1, param2, param4, param6, param8, param10, param12, this.sprite);
         var15.setAlpha(1.0F);
         var15.isGlowing = true;
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
         SoulParticle var15 = new SoulParticle(param1, param2, param4, param6, param8, param10, param12, this.sprite);
         var15.setAlpha(1.0F);
         return var15;
      }
   }
}
