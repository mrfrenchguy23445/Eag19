package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

public class LavaParticle extends TextureSheetParticle {
   LavaParticle(ClientLevel param0, double param1, double param3, double param5) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
      this.gravity = 0.75F;
      this.friction = 0.999F;
      this.xd *= 0.8F;
      this.yd *= 0.8F;
      this.zd *= 0.8F;
      this.yd = this.random.nextFloat() * 0.4F + 0.05F;
      this.quadSize = this.quadSize * (this.random.nextFloat() * 2.0F + 0.2F);
      this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public int getLightColor(float param0) {
      int var2 = super.getLightColor(param0);
      int var3 = 240;
      int var4 = var2 >> 16 & 0xFF;
      return 240 | var4 << 16;
   }

   @Override
   public float getQuadSize(float param0) {
      float var2 = (this.age + param0) / this.lifetime;
      return this.quadSize * (1.0F - var2 * var2);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         float var1 = (float)this.age / this.lifetime;
         if (this.random.nextFloat() > var1) {
            this.level.addParticle(ParticleTypes.SMOKE, this.x, this.y, this.z, this.xd, this.yd, this.zd);
         }
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
         LavaParticle var15 = new LavaParticle(param1, param2, param4, param6);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }
}
