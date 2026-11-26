package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class NoteParticle extends TextureSheetParticle {
   NoteParticle(ClientLevel param0, double param1, double param3, double param5, double param7) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
      this.friction = 0.66F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.xd *= 0.01F;
      this.yd *= 0.01F;
      this.zd *= 0.01F;
      this.yd += 0.2;
      this.rCol = Math.max(0.0F, Mth.sin(((float)param7 + 0.0F) * (float) (Math.PI * 2)) * 0.65F + 0.35F);
      this.gCol = Math.max(0.0F, Mth.sin(((float)param7 + 0.33333334F) * (float) (Math.PI * 2)) * 0.65F + 0.35F);
      this.bCol = Math.max(0.0F, Mth.sin(((float)param7 + 0.6666667F) * (float) (Math.PI * 2)) * 0.65F + 0.35F);
      this.quadSize *= 1.5F;
      this.lifetime = 6;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public float getQuadSize(float param0) {
      return this.quadSize * Mth.clamp((this.age + param0) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         NoteParticle var15 = new NoteParticle(param1, param2, param4, param6, param8);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }
}
