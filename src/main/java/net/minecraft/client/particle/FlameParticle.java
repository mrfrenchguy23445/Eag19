package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class FlameParticle extends RisingParticle {
   FlameParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      super(param0, param1, param3, param5, param7, param9, param11);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public void move(double param0, double param2, double param4) {
      this.setBoundingBox(this.getBoundingBox().move(param0, param2, param4));
      this.setLocationFromBoundingbox();
   }

   @Override
   public float getQuadSize(float param0) {
      float var2 = (this.age + param0) / this.lifetime;
      return this.quadSize * (1.0F - var2 * var2 * 0.5F);
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

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         FlameParticle var15 = new FlameParticle(param1, param2, param4, param6, param8, param10, param12);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }

   public static class SmallFlameProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SmallFlameProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         FlameParticle var15 = new FlameParticle(param1, param2, param4, param6, param8, param10, param12);
         var15.pickSprite(this.sprite);
         var15.scale(0.5F);
         return var15;
      }
   }
}
