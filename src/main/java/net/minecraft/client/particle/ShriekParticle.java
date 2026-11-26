package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ShriekParticle extends TextureSheetParticle {
   private static final Vector3f ROTATION_VECTOR = new Vector3f(0.5F, 0.5F, 0.5F).normalize();
   private static final Vector3f TRANSFORM_VECTOR = new Vector3f(-1.0F, -1.0F, 0.0F);
   private static final float MAGICAL_X_ROT = 1.0472F;
   private int delay;

   ShriekParticle(ClientLevel param0, double param1, double param3, double param5, int param7) {
      super(param0, param1, param3, param5, 0.0, 0.0, 0.0);
      this.quadSize = 0.85F;
      this.delay = param7;
      this.lifetime = 30;
      this.gravity = 0.0F;
      this.xd = 0.0;
      this.yd = 0.1;
      this.zd = 0.0;
   }

   @Override
   public float getQuadSize(float param0) {
      return this.quadSize * Mth.clamp((this.age + param0) / this.lifetime * 0.75F, 0.0F, 1.0F);
   }

   @Override
   public void render(VertexConsumer param0, Camera param1, float param2) {
      if (this.delay <= 0) {
         this.alpha = 1.0F - Mth.clamp((this.age + param2) / this.lifetime, 0.0F, 1.0F);
         this.renderRotatedParticle(param0, param1, param2, l_param0 -> l_param0.mul(new Quaternionf().rotationX(-1.0472F)));
         this.renderRotatedParticle(param0, param1, param2, l_param0 -> l_param0.mul(new Quaternionf().rotationYXZ((float) -Math.PI, 1.0472F, 0.0F)));
      }
   }

   private void renderRotatedParticle(VertexConsumer param0, Camera param1, float param2, Consumer<Quaternionf> param3) {
      Vec3 var5 = param1.getPosition();
      float var6 = (float)(Mth.lerp(param2, this.xo, this.x) - var5.x());
      float var7 = (float)(Mth.lerp(param2, this.yo, this.y) - var5.y());
      float var8 = (float)(Mth.lerp(param2, this.zo, this.z) - var5.z());
      Quaternionf var9 = new Quaternionf().setAngleAxis(0.0F, ROTATION_VECTOR.x(), ROTATION_VECTOR.y(), ROTATION_VECTOR.z());
      param3.accept(var9);
      var9.transform(TRANSFORM_VECTOR);
      Vector3f[] var10 = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
      };
      float var11 = this.getQuadSize(param2);

      for (int var12 = 0; var12 < 4; var12++) {
         Vector3f var13 = var10[var12];
         var13.rotate(var9);
         var13.mul(var11);
         var13.add(var6, var7, var8);
      }

      int var12 = this.getLightColor(param2);
      this.makeCornerVertex(param0, var10[0], this.getU1(), this.getV1(), var12);
      this.makeCornerVertex(param0, var10[1], this.getU1(), this.getV0(), var12);
      this.makeCornerVertex(param0, var10[2], this.getU0(), this.getV0(), var12);
      this.makeCornerVertex(param0, var10[3], this.getU0(), this.getV1(), var12);
   }

   private void makeCornerVertex(VertexConsumer param0, Vector3f param1, float param2, float param3, int param4) {
      param0.vertex(param1.x(), param1.y(), param1.z()).uv(param2, param3).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(param4).endVertex();
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
      if (this.delay > 0) {
         this.delay--;
      } else {
         super.tick();
      }
   }

   public static class Provider implements ParticleProvider<ShriekParticleOption> {
      private final SpriteSet sprite;

      public Provider(SpriteSet param0) {
         this.sprite = param0;
      }

      public Particle createParticle(
         ShriekParticleOption param0, ClientLevel param1, double param2, double param4, double param6, double param8, double param10, double param12
      ) {
         ShriekParticle var15 = new ShriekParticle(param1, param2, param4, param6, param0.getDelay());
         var15.pickSprite(this.sprite);
         var15.setAlpha(1.0F);
         return var15;
      }
   }
}
