package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class PlayerCloudParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   PlayerCloudParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11, SpriteSet param13) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
      this.friction = 0.96F;
      this.sprites = param13;
      float var15 = 2.5F;
      this.xd *= 0.1F;
      this.yd *= 0.1F;
      this.zd *= 0.1F;
      this.xd += param7;
      this.yd += param9;
      this.zd += param11;
      float var16 = 1.0F - (float)(Math.random() * 0.3F);
      this.rCol = var16;
      this.gCol = var16;
      this.bCol = var16;
      this.quadSize *= 1.875F;
      int var17 = (int)(8.0 / (Math.random() * 0.8 + 0.3));
      this.lifetime = (int)Math.max(var17 * 2.5F, 1.0F);
      this.hasPhysics = false;
      this.setSpriteFromAge(param13);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Override
   public float getQuadSize(float param0) {
      return this.quadSize * Mth.clamp((this.age + param0) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setSpriteFromAge(this.sprites);
         Player var1 = this.level.getNearestPlayer(this.x, this.y, this.z, 2.0, false);
         if (var1 != null) {
            double var2 = var1.getY();
            if (this.y > var2) {
               this.y = this.y + (var2 - this.y) * 0.2;
               this.yd = this.yd + (var1.getDeltaMovement().y - this.yd) * 0.2;
               this.setPos(this.x, this.y, this.z);
            }
         }
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
         return new PlayerCloudParticle(param1, param2, param4, param6, param8, param10, param12, this.sprites);
      }
   }

   public static class SneezeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SneezeProvider(SpriteSet param0) {
         this.sprites = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         Particle var15 = new PlayerCloudParticle(param1, param2, param4, param6, param8, param10, param12, this.sprites);
         var15.setColor(200.0F, 50.0F, 120.0F);
         var15.setAlpha(0.4F);
         return var15;
      }
   }
}
