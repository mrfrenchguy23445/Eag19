package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

public class BaseAshSmokeParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   protected BaseAshSmokeParticle(
      ClientLevel param0,
      double param1,
      double param3,
      double param5,
      float param7,
      float param8,
      float param9,
      double param10,
      double param12,
      double param14,
      float param16,
      SpriteSet param17,
      float param18,
      int param19,
      float param20,
      boolean param21
   ) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
      this.friction = 0.96F;
      this.gravity = param20;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = param17;
      this.xd *= param7;
      this.yd *= param8;
      this.zd *= param9;
      this.xd += param10;
      this.yd += param12;
      this.zd += param14;
      float var23 = param0.random.nextFloat() * param18;
      this.rCol = var23;
      this.gCol = var23;
      this.bCol = var23;
      this.quadSize *= 0.75F * param16;
      this.lifetime = (int)(param19 / (param0.random.nextFloat() * 0.8 + 0.2) * param16);
      this.lifetime = Math.max(this.lifetime, 1);
      this.setSpriteFromAge(param17);
      this.hasPhysics = param21;
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
