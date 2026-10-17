package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class VibrationSignalParticle extends TextureSheetParticle {
   private final PositionSource target;
   private float rot;
   private float rotO;
   private float pitch;
   private float pitchO;

   VibrationSignalParticle(ClientLevel param0, double param1, double param3, double param5, PositionSource param7, int param8) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
      this.quadSize = 0.3F;
      this.target = param7;
      this.lifetime = param8;
      Optional<Vec3> var10 = param7.getPosition(param0);
      if (var10.isPresent()) {
         Vec3 var11 = var10.get();
         double var12 = param1 - var11.x();
         double var14 = param3 - var11.y();
         double var16 = param5 - var11.z();
         this.rotO = this.rot = (float)Mth.atan2(var12, var16);
         this.pitchO = this.pitch = (float)Mth.atan2(var14, Math.sqrt(var12 * var12 + var16 * var16));
      }
   }

   @Override
   public void render(VertexConsumer param0, Camera param1, float param2) {
      float var4 = Mth.sin((this.age + param2 - (float) (Math.PI * 2)) * 0.05F) * 2.0F;
      float var5 = Mth.lerp(param2, this.rotO, this.rot);
      float var6 = Mth.lerp(param2, this.pitchO, this.pitch) + (float) (Math.PI / 2);
      this.renderSignal(param0, param1, param2, l_param3 -> l_param3.rotateY(var5).rotateX(-var6).rotateY(var4));
      this.renderSignal(param0, param1, param2, l_param3 -> l_param3.rotateY((float) -Math.PI + var5).rotateX(var6).rotateY(var4));
   }

   private void renderSignal(VertexConsumer param0, Camera param1, float param2, Consumer<Quaternionf> param3) {
      Vec3 var5 = param1.getPosition();
      float var6 = (float)(Mth.lerp(param2, this.xo, this.x) - var5.x());
      float var7 = (float)(Mth.lerp(param2, this.yo, this.y) - var5.y());
      float var8 = (float)(Mth.lerp(param2, this.zo, this.z) - var5.z());
      Vector3f var9 = new Vector3f(0.5F, 0.5F, 0.5F).normalize();
      Quaternionf var10 = new Quaternionf().setAngleAxis(0.0F, var9.x(), var9.y(), var9.z());
      param3.accept(var10);
      Vector3f[] var11 = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
      };
      float var12 = this.getQuadSize(param2);

      for (int var13 = 0; var13 < 4; var13++) {
         Vector3f var14 = var11[var13];
         var14.rotate(var10);
         var14.mul(var12);
         var14.add(var6, var7, var8);
      }

      float var13 = this.getU0();
      float var14 = this.getU1();
      float var15 = this.getV0();
      float var16 = this.getV1();
      int var17 = this.getLightColor(param2);
      param0.vertex(var11[0].x(), var11[0].y(), var11[0].z()).uv(var14, var16).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(var17).endVertex();
      param0.vertex(var11[1].x(), var11[1].y(), var11[1].z()).uv(var14, var15).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(var17).endVertex();
      param0.vertex(var11[2].x(), var11[2].y(), var11[2].z()).uv(var13, var15).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(var17).endVertex();
      param0.vertex(var11[3].x(), var11[3].y(), var11[3].z()).uv(var13, var16).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(var17).endVertex();
   }

   @Override
   public int getLightColor(float param0) {
      return 240;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         Optional<Vec3> var1 = this.target.getPosition(this.level);
         if (var1.isEmpty()) {
            this.remove();
         } else {
            int var2 = this.lifetime - this.age;
            double var3 = 1.0 / var2;
            Vec3 var5 = var1.get();
            this.x = Mth.lerp(var3, this.x, var5.x());
            this.y = Mth.lerp(var3, this.y, var5.y());
            this.z = Mth.lerp(var3, this.z, var5.z());
            double var6 = this.x - var5.x();
            double var8 = this.y - var5.y();
            double var10 = this.z - var5.z();
            this.rotO = this.rot;
            this.rot = (float)Mth.atan2(var6, var10);
            this.pitchO = this.pitch;
            this.pitch = (float)Mth.atan2(var8, Math.sqrt(var6 * var6 + var10 * var10));
         }
      }
   }

   public static class Provider implements ParticleProvider<VibrationParticleOption> {
      private final SpriteSet sprite;

      public Provider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         VibrationParticleOption param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         VibrationSignalParticle var15 = new VibrationSignalParticle(param1, param2, param4, param6, param0.getDestination(), param0.getArrivalInTicks());
         var15.pickSprite(this.sprite);
         var15.setAlpha(1.0F);
         return var15;
      }
   }
}
