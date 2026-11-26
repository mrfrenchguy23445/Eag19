package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;

public class SimpleAnimatedParticle extends TextureSheetParticle {
   protected final SpriteSet sprites;
   private float fadeR;
   private float fadeG;
   private float fadeB;
   private boolean hasFade;

   protected SimpleAnimatedParticle(ClientLevel param0, double param1, double param3, double param5, SpriteSet param7, float param8) {
      super(param0, param1, param3, param5);
      this.friction = 0.91F;
      this.gravity = param8;
      this.sprites = param7;
   }

   public void setColor(int param0) {
      float var2 = ((param0 & 0xFF0000) >> 16) / 255.0F;
      float var3 = ((param0 & 0xFF00) >> 8) / 255.0F;
      float var4 = ((param0 & 0xFF) >> 0) / 255.0F;
      float var5 = 1.0F;
      this.setColor(var2 * 1.0F, var3 * 1.0F, var4 * 1.0F);
   }

   public void setFadeColor(int param0) {
      this.fadeR = ((param0 & 0xFF0000) >> 16) / 255.0F;
      this.fadeG = ((param0 & 0xFF00) >> 8) / 255.0F;
      this.fadeB = ((param0 & 0xFF) >> 0) / 255.0F;
      this.hasFade = true;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      if (this.age > this.lifetime / 2) {
         this.setAlpha(1.0F - ((float)this.age - this.lifetime / 2) / this.lifetime);
         if (this.hasFade) {
            this.rCol = this.rCol + (this.fadeR - this.rCol) * 0.2F;
            this.gCol = this.gCol + (this.fadeG - this.gCol) * 0.2F;
            this.bCol = this.bCol + (this.fadeB - this.bCol) * 0.2F;
         }
      }
   }

   @Override
   public int getLightColor(float param0) {
      return 15728880;
   }
}
