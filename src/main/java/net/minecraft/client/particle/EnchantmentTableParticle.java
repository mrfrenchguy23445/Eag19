package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class EnchantmentTableParticle extends TextureSheetParticle {
   private final double xStart;
   private final double yStart;
   private final double zStart;

   EnchantmentTableParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      super(param0, param1, param3, param5);
      this.xd = param7;
      this.yd = param9;
      this.zd = param11;
      this.xStart = param1;
      this.yStart = param3;
      this.zStart = param5;
      this.xo = param1 + param7;
      this.yo = param3 + param9;
      this.zo = param5 + param11;
      this.x = this.xo;
      this.y = this.yo;
      this.z = this.zo;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
      float var14 = this.random.nextFloat() * 0.6F + 0.4F;
      this.rCol = 0.9F * var14;
      this.gCol = 0.9F * var14;
      this.bCol = var14;
      this.hasPhysics = false;
      this.lifetime = (int)(Math.random() * 10.0) + 30;
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
   public int getLightColor(float param0) {
      int var2 = super.getLightColor(param0);
      float var3 = (float)this.age / this.lifetime;
      var3 *= var3;
      var3 *= var3;
      int var4 = var2 & 0xFF;
      int var5 = var2 >> 16 & 0xFF;
      var5 += (int)(var3 * 15.0F * 16.0F);
      if (var5 > 240) {
         var5 = 240;
      }

      return var4 | var5 << 16;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float var1 = (float)this.age / this.lifetime;
         var1 = 1.0F - var1;
         float var2 = 1.0F - var1;
         var2 *= var2;
         var2 *= var2;
         this.x = this.xStart + this.xd * var1;
         this.y = this.yStart + this.yd * var1 - var2 * 1.2F;
         this.z = this.zStart + this.zd * var1;
      }
   }

   public static class NautilusProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public NautilusProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         EnchantmentTableParticle var15 = new EnchantmentTableParticle(param1, param2, param4, param6, param8, param10, param12);
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
         EnchantmentTableParticle var15 = new EnchantmentTableParticle(param1, param2, param4, param6, param8, param10, param12);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }
}
