package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;

public class WaterCurrentDownParticle extends TextureSheetParticle {
   private float angle;

   WaterCurrentDownParticle(ClientLevel param0, double param1, double param3, double param5) {
      super(param0, param1, param3, param5);
      this.lifetime = (int)(Math.random() * 60.0) + 30;
      this.hasPhysics = false;
      this.xd = 0.0;
      this.yd = -0.05;
      this.zd = 0.0;
      this.setSize(0.02F, 0.02F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
      this.gravity = 0.002F;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float var1 = 0.6F;
         this.xd = this.xd + 0.6F * Mth.cos(this.angle);
         this.zd = this.zd + 0.6F * Mth.sin(this.angle);
         this.xd *= 0.07;
         this.zd *= 0.07;
         this.move(this.xd, this.yd, this.zd);
         if (!this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER) || this.onGround) {
            this.remove();
         }

         this.angle += 0.08F;
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
         WaterCurrentDownParticle var15 = new WaterCurrentDownParticle(param1, param2, param4, param6);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }
}
