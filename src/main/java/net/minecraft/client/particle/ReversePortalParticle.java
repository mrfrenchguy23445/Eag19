package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class ReversePortalParticle extends PortalParticle {
   ReversePortalParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      super(param0, param1, param3, param5, param7, param9, param11);
      this.quadSize *= 1.5F;
      this.lifetime = (int)(Math.random() * 2.0) + 60;
   }

   @Override
   public float getQuadSize(float param0) {
      float var2 = 1.0F - (this.age + param0) / (this.lifetime * 1.5F);
      return this.quadSize * var2;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float var1 = (float)this.age / this.lifetime;
         this.x = this.x + this.xd * var1;
         this.y = this.y + this.yd * var1;
         this.z = this.z + this.zd * var1;
      }
   }

   public static class ReversePortalProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ReversePortalProvider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         ReversePortalParticle var15 = new ReversePortalParticle(param1, param2, param4, param6, param8, param10, param12);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }
}
