package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class SnowflakeParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   protected SnowflakeParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, SpriteSet param13) {
      super(param0, param1, param3, param5);
      this.gravity = 0.225F;
      this.friction = 1.0F;
      this.sprites = param13;
      this.xd = param7 + (Math.random() * 2.0 - 1.0) * 0.05F;
      this.yd = param9 + (Math.random() * 2.0 - 1.0) * 0.05F;
      this.zd = param11 + (Math.random() * 2.0 - 1.0) * 0.05F;
      this.quadSize = 0.1F * (this.random.nextFloat() * this.random.nextFloat() * 1.0F + 1.0F);
      this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2)) + 2;
      this.setSpriteFromAge(param13);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      this.xd *= 0.95F;
      this.yd *= 0.9F;
      this.zd *= 0.95F;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         SnowflakeParticle var15 = new SnowflakeParticle(param1, param2, param4, param6, param8, param10, param12, this.sprites);
         var15.setColor(0.923F, 0.964F, 0.999F);
         return var15;
      }
   }
}
