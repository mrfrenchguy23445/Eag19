package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptionsBase;
import net.minecraft.util.Mth;

public class DustParticleBase<T extends DustParticleOptionsBase> extends TextureSheetParticle {
   private final SpriteSet sprites;

   protected DustParticleBase(
      ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, T param13, SpriteSet param14
   ) {
      super(param0, param1, param3, param5, param7, param9, param11);
      this.friction = 0.96F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = param14;
      this.xd *= 0.1F;
      this.yd *= 0.1F;
      this.zd *= 0.1F;
      float var16 = this.random.nextFloat() * 0.4F + 0.6F;
      this.rCol = this.randomizeColor(param13.getColor().x(), var16);
      this.gCol = this.randomizeColor(param13.getColor().y(), var16);
      this.bCol = this.randomizeColor(param13.getColor().z(), var16);
      this.quadSize = this.quadSize * (0.75F * param13.getScale());
      int var17 = (int)(8.0 / (this.random.nextDouble() * 0.8 + 0.2));
      this.lifetime = (int)Math.max(var17 * param13.getScale(), 1.0F);
      this.setSpriteFromAge(param14);
   }

   protected float randomizeColor(float param0, float param1) {
      return (this.random.nextFloat() * 0.2F + 0.8F) * param0 * param1;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public float getQuadSize(float param0) {
      return this.quadSize * Mth.clamp((this.age + param0) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }
}
