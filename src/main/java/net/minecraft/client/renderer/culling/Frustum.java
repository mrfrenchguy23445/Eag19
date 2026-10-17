package net.minecraft.client.renderer.culling;

import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Frustum {
   public static final int OFFSET_STEP = 4;
   private final FrustumIntersection intersection = new FrustumIntersection();
   private final Matrix4f matrix = new Matrix4f();
   private Vector4f viewVector;
   private double camX;
   private double camY;
   private double camZ;

   public Frustum(Matrix4f param0, Matrix4f param1) {
      this.calculateFrustum(param0, param1);
   }

   public Frustum(Frustum param0) {
      this.intersection.set(param0.matrix);
      this.matrix.set(param0.matrix);
      this.camX = param0.camX;
      this.camY = param0.camY;
      this.camZ = param0.camZ;
      this.viewVector = param0.viewVector;
   }

   public Frustum offsetToFullyIncludeCameraCube(int param0) {
      double var2 = Math.floor(this.camX / param0) * param0;
      double var4 = Math.floor(this.camY / param0) * param0;
      double var6 = Math.floor(this.camZ / param0) * param0;
      double var8 = Math.ceil(this.camX / param0) * param0;
      double var10 = Math.ceil(this.camY / param0) * param0;

      for (double var12 = Math.ceil(this.camZ / param0) * param0;
         this.intersection
               .intersectAab(
                  (float)(var2 - this.camX),
                  (float)(var4 - this.camY),
                  (float)(var6 - this.camZ),
                  (float)(var8 - this.camX),
                  (float)(var10 - this.camY),
                  (float)(var12 - this.camZ)
               )
            != -2;
         this.camZ = this.camZ - this.viewVector.z() * 4.0F
      ) {
         this.camX = this.camX - this.viewVector.x() * 4.0F;
         this.camY = this.camY - this.viewVector.y() * 4.0F;
      }

      return this;
   }

   public void prepare(double param0, double param2, double param4) {
      this.camX = param0;
      this.camY = param2;
      this.camZ = param4;
   }

   private void calculateFrustum(Matrix4f param0, Matrix4f param1) {
      param1.mul(param0, this.matrix);
      this.intersection.set(this.matrix);
      this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
   }

   public boolean isVisible(AABB param0) {
      return this.cubeInFrustum(param0.minX, param0.minY, param0.minZ, param0.maxX, param0.maxY, param0.maxZ);
   }

   private boolean cubeInFrustum(double param0, double param2, double param4, double param6, double param8, double param10) {
      float var13 = (float)(param0 - this.camX);
      float var14 = (float)(param2 - this.camY);
      float var15 = (float)(param4 - this.camZ);
      float var16 = (float)(param6 - this.camX);
      float var17 = (float)(param8 - this.camY);
      float var18 = (float)(param10 - this.camZ);
      return this.intersection.testAab(var13, var14, var15, var16, var17, var18);
   }
}
