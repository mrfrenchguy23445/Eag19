package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;

public class WaterDropParticle extends TextureSheetParticle {
   protected WaterDropParticle(ClientLevel param0, double param1, double param3, double param5) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
      this.xd *= 0.3F;
      this.yd = Math.random() * 0.2F + 0.1F;
      this.zd *= 0.3F;
      this.setSize(0.01F, 0.01F);
      this.gravity = 0.06F;
      this.lifetime = (int)(8.0 / (Math.random() * 0.8 + 0.2));
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
      if (this.lifetime-- <= 0) {
         this.remove();
      } else {
         this.yd = this.yd - this.gravity;
         this.move(this.xd, this.yd, this.zd);
         this.xd *= 0.98F;
         this.yd *= 0.98F;
         this.zd *= 0.98F;
         if (this.onGround) {
            if (Math.random() < 0.5) {
               this.remove();
            }

            this.xd *= 0.7F;
            this.zd *= 0.7F;
         }

         BlockPos var1 = BlockPos.containing(this.x, this.y, this.z);
         double var2 = Math.max(
            this.level.getBlockState(var1).getCollisionShape(this.level, var1).max(Direction.Axis.Y, this.x - var1.getX(), this.z - var1.getZ()),
            this.level.getFluidState(var1).getHeight(this.level, var1)
         );
         if (var2 > 0.0 && this.y < var1.getY() + var2) {
            this.remove();
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
         WaterDropParticle var15 = new WaterDropParticle(param1, param2, param4, param6);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }
}
