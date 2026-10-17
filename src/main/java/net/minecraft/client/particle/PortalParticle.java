package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class PortalParticle extends TextureSheetParticle {
   private final double xStart;
   private final double yStart;
   private final double zStart;

   protected PortalParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      super(param0, param1, param3, param5);
      this.xd = param7;
      this.yd = param9;
      this.zd = param11;
      this.x = param1;
      this.y = param3;
      this.z = param5;
      this.xStart = this.x;
      this.yStart = this.y;
      this.zStart = this.z;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.2F + 0.5F);
      float var14 = this.random.nextFloat() * 0.6F + 0.4F;
      this.rCol = var14 * 0.9F;
      this.gCol = var14 * 0.3F;
      this.bCol = var14;
      this.lifetime = (int)(Math.random() * 10.0) + 40;
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
      var2 = 1.0F - var2;
      var2 *= var2;
      var2 = 1.0F - var2;
      return this.quadSize * var2;
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
         float var2 = var1;
         var1 = -var1 + var1 * var1 * 2.0F;
         var1 = 1.0F - var1;
         this.x = this.xStart + this.xd * var1;
         this.y = this.yStart + this.yd * var1 + (1.0F - var2);
         this.z = this.zStart + this.zd * var1;
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
         PortalParticle var15 = new PortalParticle(param1, param2, param4, param6, param8, param10, param12);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }
}
