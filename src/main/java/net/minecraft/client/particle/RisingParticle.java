package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;

public abstract class RisingParticle extends TextureSheetParticle {
   protected RisingParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      super(param0, param1, param3, param5, param7, param9, param11);
      this.friction = 0.96F;
      this.xd = this.xd * 0.01F + param7;
      this.yd = this.yd * 0.01F + param9;
      this.zd = this.zd * 0.01F + param11;
      this.x = this.x + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
      this.y = this.y + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
      this.z = this.z + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
      this.lifetime = (int)(8.0 / (Math.random() * 0.8 + 0.2)) + 4;
   }
}
