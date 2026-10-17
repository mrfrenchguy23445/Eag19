package net.minecraft.client.particle;

import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class SuspendedParticle extends TextureSheetParticle {
   SuspendedParticle(ClientLevel param0, SpriteSet param1, double param2, double param4, double param6) {
      super(param0, param2, param4 - 0.125, param6);
      this.setSize(0.01F, 0.01F);
      this.pickSprite(param1);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
      this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   SuspendedParticle(ClientLevel param0, SpriteSet param1, double param2, double param4, double param6, double param8, double param10, double param12) {
      super(param0, param2, param4 - 0.125, param6, param8, param10, param12);
      this.setSize(0.01F, 0.01F);
      this.pickSprite(param1);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.6F);
      this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public static class CrimsonSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public CrimsonSporeProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         RandomSource var15 = param1.random;
         double var16 = var15.nextGaussian() * 1.0E-6F;
         double var18 = var15.nextGaussian() * 1.0E-4F;
         double var20 = var15.nextGaussian() * 1.0E-6F;
         SuspendedParticle var22 = new SuspendedParticle(param1, this.sprite, param2, param4, param6, var16, var18, var20);
         var22.setColor(0.9F, 0.4F, 0.5F);
         return var22;
      }
   }

   public static class SporeBlossomAirProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SporeBlossomAirProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         SuspendedParticle var15 = new SuspendedParticle(param1, this.sprite, param2, param4, param6, 0.0, -0.8F, 0.0) {
            @Override
            public Optional<ParticleGroup> getParticleGroup() {
               return Optional.of(ParticleGroup.SPORE_BLOSSOM);
            }
         };
         var15.lifetime = Mth.randomBetweenInclusive(param1.random, 500, 1000);
         var15.gravity = 0.01F;
         var15.setColor(0.32F, 0.5F, 0.22F);
         return var15;
      }
   }

   public static class UnderwaterProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public UnderwaterProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         SuspendedParticle var15 = new SuspendedParticle(param1, this.sprite, param2, param4, param6);
         var15.setColor(0.4F, 0.4F, 0.7F);
         return var15;
      }
   }

   public static class WarpedSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WarpedSporeProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         double var15 = param1.random.nextFloat() * -1.9 * param1.random.nextFloat() * 0.1;
         SuspendedParticle var17 = new SuspendedParticle(param1, this.sprite, param2, param4, param6, 0.0, var15, 0.0);
         var17.setColor(0.1F, 0.1F, 0.3F);
         var17.setSize(0.001F, 0.001F);
         return var17;
      }
   }
}
