package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class SingleQuadParticle extends Particle {
   protected float quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;

   protected SingleQuadParticle(ClientLevel param0, double param1, double param3, double param5) {
      super(param0, param1, param3, param5);
   }

   protected SingleQuadParticle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      super(param0, param1, param3, param5, param7, param9, param11);
   }

   @Override
   public void render(VertexConsumer param0, Camera param1, float param2) {
      Vec3 var4 = param1.getPosition();
      float var5 = (float)(Mth.lerp(param2, this.xo, this.x) - var4.x());
      float var6 = (float)(Mth.lerp(param2, this.yo, this.y) - var4.y());
      float var7 = (float)(Mth.lerp(param2, this.zo, this.z) - var4.z());
      Quaternionf var8;
      if (this.roll == 0.0F) {
         var8 = param1.rotation();
      } else {
         var8 = new Quaternionf(param1.rotation());
         var8.rotateZ(Mth.lerp(param2, this.oRoll, this.roll));
      }

      Vector3f[] var9 = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
      };
      float var10 = this.getQuadSize(param2);

      for (int var11 = 0; var11 < 4; var11++) {
         Vector3f var12 = var9[var11];
         var12.rotate(var8);
         var12.mul(var10);
         var12.add(var5, var6, var7);
      }

      float var11 = this.getU0();
      float var12 = this.getU1();
      float var13 = this.getV0();
      float var14 = this.getV1();
      int var15 = this.getLightColor(param2);
      param0.vertex(var9[0].x(), var9[0].y(), var9[0].z()).uv(var12, var14).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(var15).endVertex();
      param0.vertex(var9[1].x(), var9[1].y(), var9[1].z()).uv(var12, var13).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(var15).endVertex();
      param0.vertex(var9[2].x(), var9[2].y(), var9[2].z()).uv(var11, var13).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(var15).endVertex();
      param0.vertex(var9[3].x(), var9[3].y(), var9[3].z()).uv(var11, var14).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(var15).endVertex();
   }

   public float getQuadSize(float param0) {
      return this.quadSize;
   }

   @Override
   public Particle scale(float param0) {
      this.quadSize *= param0;
      return super.scale(param0);
   }

   protected abstract float getU0();

   protected abstract float getU1();

   protected abstract float getV0();

   protected abstract float getV1();
}
