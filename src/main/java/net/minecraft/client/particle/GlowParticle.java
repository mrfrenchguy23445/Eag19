package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class GlowParticle extends TextureSheetParticle {
   static final RandomSource RANDOM = RandomSource.create();
   private final SpriteSet sprites;

   GlowParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, SpriteSet param13) {
      super(param0, param1, param3, param5, param7, param9, param11);
      this.friction = 0.96F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = param13;
      this.quadSize *= 0.75F;
      this.hasPhysics = false;
      this.setSpriteFromAge(param13);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Override
   public int getLightColor(float param0) {
      float var2 = (this.age + param0) / this.lifetime;
      var2 = Mth.clamp(var2, 0.0F, 1.0F);
      int var3 = super.getLightColor(param0);
      int var4 = var3 & 0xFF;
      int var5 = var3 >> 16 & 0xFF;
      var4 += (int)(var2 * 15.0F * 16.0F);
      if (var4 > 240) {
         var4 = 240;
      }

      return var4 | var5 << 16;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   public static class ElectricSparkProvider implements ParticleProvider<SimpleParticleType> {
      private final double SPEED_FACTOR = 0.25;
      private final SpriteSet sprite;

      public ElectricSparkProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         GlowParticle var15 = new GlowParticle(param1, param2, param4, param6, 0.0, 0.0, 0.0, this.sprite);
         var15.setColor(1.0F, 0.9F, 1.0F);
         var15.setParticleSpeed(param8 * 0.25, param10 * 0.25, param12 * 0.25);
         int var16 = 2;
         int var17 = 4;
         var15.setLifetime(param1.random.nextInt(2) + 2);
         return var15;
      }
   }

   public static class GlowSquidProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public GlowSquidProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         GlowParticle var15 = new GlowParticle(
            param1, param2, param4, param6, 0.5 - GlowParticle.RANDOM.nextDouble(), param10, 0.5 - GlowParticle.RANDOM.nextDouble(), this.sprite
         );
         if (param1.random.nextBoolean()) {
            var15.setColor(0.6F, 1.0F, 0.8F);
         } else {
            var15.setColor(0.08F, 0.4F, 0.4F);
         }

         var15.yd *= 0.2F;
         if (param8 == 0.0 && param12 == 0.0) {
            var15.xd *= 0.1F;
            var15.zd *= 0.1F;
         }

         var15.setLifetime((int)(8.0 / (param1.random.nextDouble() * 0.8 + 0.2)));
         return var15;
      }
   }

   public static class ScrapeProvider implements ParticleProvider<SimpleParticleType> {
      private final double SPEED_FACTOR = 0.01;
      private final SpriteSet sprite;

      public ScrapeProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         GlowParticle var15 = new GlowParticle(param1, param2, param4, param6, 0.0, 0.0, 0.0, this.sprite);
         if (param1.random.nextBoolean()) {
            var15.setColor(0.29F, 0.58F, 0.51F);
         } else {
            var15.setColor(0.43F, 0.77F, 0.62F);
         }

         var15.setParticleSpeed(param8 * 0.01, param10 * 0.01, param12 * 0.01);
         int var16 = 10;
         int var17 = 40;
         var15.setLifetime(param1.random.nextInt(30) + 10);
         return var15;
      }
   }

   public static class WaxOffProvider implements ParticleProvider<SimpleParticleType> {
      private final double SPEED_FACTOR = 0.01;
      private final SpriteSet sprite;

      public WaxOffProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         GlowParticle var15 = new GlowParticle(param1, param2, param4, param6, 0.0, 0.0, 0.0, this.sprite);
         var15.setColor(1.0F, 0.9F, 1.0F);
         var15.setParticleSpeed(param8 * 0.01 / 2.0, param10 * 0.01, param12 * 0.01 / 2.0);
         int var16 = 10;
         int var17 = 40;
         var15.setLifetime(param1.random.nextInt(30) + 10);
         return var15;
      }
   }

   public static class WaxOnProvider implements ParticleProvider<SimpleParticleType> {
      private final double SPEED_FACTOR = 0.01;
      private final SpriteSet sprite;

      public WaxOnProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         GlowParticle var15 = new GlowParticle(param1, param2, param4, param6, 0.0, 0.0, 0.0, this.sprite);
         var15.setColor(0.91F, 0.55F, 0.08F);
         var15.setParticleSpeed(param8 * 0.01 / 2.0, param10 * 0.01, param12 * 0.01 / 2.0);
         int var16 = 10;
         int var17 = 40;
         var15.setLifetime(param1.random.nextInt(30) + 10);
         return var15;
      }
   }
}
