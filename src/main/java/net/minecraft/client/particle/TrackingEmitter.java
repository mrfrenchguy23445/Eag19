package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class TrackingEmitter extends NoRenderParticle {
   private final Entity entity;
   private int life;
   private final int lifeTime;
   private final ParticleOptions particleType;

   public TrackingEmitter(ClientLevel param0, Entity param1, ParticleOptions param2) {
      this(param0, param1, param2, 3);
   }

   public TrackingEmitter(ClientLevel param0, Entity param1, ParticleOptions param2, int param3) {
      this(param0, param1, param2, param3, param1.getDeltaMovement());
   }

   private TrackingEmitter(ClientLevel param0, Entity param1, ParticleOptions param2, int param3, Vec3 param4) {
      super(param0, param1.getX(), param1.getY(0.5), param1.getZ(), param4.x, param4.y, param4.z);
      this.entity = param1;
      this.lifeTime = param3;
      this.particleType = param2;
      this.tick();
   }

   @Override
   public void tick() {
      for (int var1 = 0; var1 < 16; var1++) {
         double var2 = this.random.nextFloat() * 2.0F - 1.0F;
         double var4 = this.random.nextFloat() * 2.0F - 1.0F;
         double var6 = this.random.nextFloat() * 2.0F - 1.0F;
         if (!(var2 * var2 + var4 * var4 + var6 * var6 > 1.0)) {
            double var8 = this.entity.getX(var2 / 4.0);
            double var10 = this.entity.getY(0.5 + var4 / 4.0);
            double var12 = this.entity.getZ(var6 / 4.0);
            this.level.addParticle(this.particleType, false, var8, var10, var12, var2, var4 + 0.2, var6);
         }
      }

      this.life++;
      if (this.life >= this.lifeTime) {
         this.remove();
      }
   }
}
