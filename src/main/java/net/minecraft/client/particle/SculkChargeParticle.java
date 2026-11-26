package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SculkChargeParticleOptions;

public class SculkChargeParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   SculkChargeParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, SpriteSet param13) {
      super(param0, param1, param3, param5, param7, param9, param11);
      this.friction = 0.96F;
      this.sprites = param13;
      this.scale(1.5F);
      this.hasPhysics = false;
      this.setSpriteFromAge(param13);
   }

   @Override
   public int getLightColor(float param0) {
      return 240;
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

   public record Provider(SpriteSet sprite) implements ParticleProvider<SculkChargeParticleOptions> {
      public Particle createParticle(
         SculkChargeParticleOptions param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         SculkChargeParticle var15 = new SculkChargeParticle(param1, param2, param4, param6, param8, param10, param12, this.sprite);
         var15.setAlpha(1.0F);
         var15.setParticleSpeed(param8, param10, param12);
         var15.oRoll = param0.roll();
         var15.roll = param0.roll();
         var15.setLifetime(param1.random.nextInt(12) + 8);
         return var15;
      }
   }
}
