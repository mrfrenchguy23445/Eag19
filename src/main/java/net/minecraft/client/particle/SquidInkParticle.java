package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor;

public class SquidInkParticle extends SimpleAnimatedParticle {
   SquidInkParticle(
      ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, int param13, SpriteSet param14
   ) {
      super(param0, param1, param3, param5, param14, 0.0F);
      this.friction = 0.92F;
      this.quadSize = 0.5F;
      this.setAlpha(1.0F);
      this.setColor(FastColor.ARGB32.red(param13), FastColor.ARGB32.green(param13), FastColor.ARGB32.blue(param13));
      this.lifetime = (int)(this.quadSize * 12.0F / (Math.random() * 0.8F + 0.2F));
      this.setSpriteFromAge(param14);
      this.hasPhysics = false;
      this.xd = param7;
      this.yd = param9;
      this.zd = param11;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setSpriteFromAge(this.sprites);
         if (this.age > this.lifetime / 2) {
            this.setAlpha(1.0F - ((float)this.age - this.lifetime / 2) / this.lifetime);
         }

         if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
            this.yd -= 0.0074F;
         }
      }
   }

   public static class GlowInkProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public GlowInkProvider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new SquidInkParticle(param1, param2, param4, param6, param8, param10, param12, FastColor.ARGB32.color(255, 204, 31, 102), this.sprites);
      }
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new SquidInkParticle(param1, param2, param4, param6, param8, param10, param12, FastColor.ARGB32.color(255, 255, 255, 255), this.sprites);
      }
   }
}
