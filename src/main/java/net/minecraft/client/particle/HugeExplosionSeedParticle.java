package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

public class HugeExplosionSeedParticle extends NoRenderParticle {
   private int life;
   private final int lifeTime = 8;

   HugeExplosionSeedParticle(ClientLevel param0, double param1, double param3, double param5) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
   }

   @Override
   public void tick() {
      for (int var1 = 0; var1 < 6; var1++) {
         double var2 = this.x + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
         double var4 = this.y + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
         double var6 = this.z + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
         this.level.addParticle(ParticleTypes.EXPLOSION, var2, var4, var6, (float)this.life / this.lifeTime, 0.0, 0.0);
      }

      this.life++;
      if (this.life == this.lifeTime) {
         this.remove();
      }
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      public Particle createParticle(
         SimpleParticleType param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         return new HugeExplosionSeedParticle(param1, param2, param4, param6);
      }
   }
}
