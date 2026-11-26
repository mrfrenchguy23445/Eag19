package com.mojang.blaze3d.vertex;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public interface VertexConsumer {
   VertexConsumer vertex(double var1, double var3, double var5);

   VertexConsumer color(int var1, int var2, int var3, int var4);

   VertexConsumer uv(float var1, float var2);

   VertexConsumer overlayCoords(int var1, int var2);

   VertexConsumer uv2(int var1, int var2);

   VertexConsumer normal(float var1, float var2, float var3);

   void endVertex();

   default void vertex(
      float param0,
      float param1,
      float param2,
      float param3,
      float param4,
      float param5,
      float param6,
      float param7,
      float param8,
      int param9,
      int param10,
      float param11,
      float param12,
      float param13
   ) {
      this.vertex(param0, param1, param2);
      this.color(param3, param4, param5, param6);
      this.uv(param7, param8);
      this.overlayCoords(param9);
      this.uv2(param10);
      this.normal(param11, param12, param13);
      this.endVertex();
   }

   void defaultColor(int var1, int var2, int var3, int var4);

   void unsetDefaultColor();

   default VertexConsumer color(float param0, float param1, float param2, float param3) {
      return this.color((int)(param0 * 255.0F), (int)(param1 * 255.0F), (int)(param2 * 255.0F), (int)(param3 * 255.0F));
   }

   default VertexConsumer color(int param0) {
      return this.color(FastColor.ARGB32.red(param0), FastColor.ARGB32.green(param0), FastColor.ARGB32.blue(param0), FastColor.ARGB32.alpha(param0));
   }

   default VertexConsumer uv2(int param0) {
      return this.uv2(param0 & 65535, param0 >> 16 & 65535);
   }

   default VertexConsumer overlayCoords(int param0) {
      return this.overlayCoords(param0 & 65535, param0 >> 16 & 65535);
   }

   default void putBulkData(PoseStack.Pose param0, BakedQuad param1, float param2, float param3, float param4, int param5, int param6) {
      this.putBulkData(param0, param1, new float[]{1.0F, 1.0F, 1.0F, 1.0F}, param2, param3, param4, new int[]{param5, param5, param5, param5}, param6, false);
   }

   default void putBulkData(
      PoseStack.Pose param0, BakedQuad param1, float[] param2, float param3, float param4, float param5, int[] param6, int param7, boolean param8
   ) {
      float[] var10 = new float[]{param2[0], param2[1], param2[2], param2[3]};
      int[] var11 = new int[]{param6[0], param6[1], param6[2], param6[3]};
      int[] var12 = param1.getVertices();
      Vec3i var13 = param1.getDirection().getNormal();
      Matrix4f var14 = param0.pose();
      Vector3f var15 = param0.normal().transform(new Vector3f(var13.getX(), var13.getY(), var13.getZ()));
      int var16 = 8;
      int var17 = var12.length / 8;

      // Upstream decodes one vertex at a time out of native memory: a MemoryStack frame holds a
      // single malloc of exactly one BLOCK-sized vertex, refilled by an absolute IntBuffer write
      // each iteration and read back through absolute getFloat()/get(). LWJGL's MemoryStack has no
      // counterpart in the WASM-GC classlib, but nothing here actually needs it -- the buffer is
      // private to this method, its lifetime is exactly the loop, and every access is absolute, so
      // a plain direct ByteBuffer is behaviourally identical. Allocating once outside the loop
      // rather than once per iteration is the only difference, and it is a strict improvement:
      // LWJGL's stackPush/malloc recycles a thread-local stack, so this replaces a pool hit with
      // one allocation for the whole call.
      ByteBuffer var19 = ByteBuffer.allocateDirect(DefaultVertexFormat.BLOCK.getVertexSize());
      IntBuffer var20 = var19.asIntBuffer();

      for (int var21 = 0; var21 < var17; var21++) {
         var20.clear();
         var20.put(var12, var21 * 8, 8);
         float var22 = var19.getFloat(0);
         float var23 = var19.getFloat(4);
         float var24 = var19.getFloat(8);
         float var25;
         float var26;
         float var27;
         if (param8) {
            float var28 = (var19.get(12) & 0xFF) / 255.0F;
            float var29 = (var19.get(13) & 0xFF) / 255.0F;
            float var30 = (var19.get(14) & 0xFF) / 255.0F;
            var25 = var28 * var10[var21] * param3;
            var26 = var29 * var10[var21] * param4;
            var27 = var30 * var10[var21] * param5;
         } else {
            var25 = var10[var21] * param3;
            var26 = var10[var21] * param4;
            var27 = var10[var21] * param5;
         }

         int var28 = var11[var21];
         float var29 = var19.getFloat(16);
         float var30 = var19.getFloat(20);
         Vector4f var31 = var14.transform(new Vector4f(var22, var23, var24, 1.0F));
         this.vertex(var31.x(), var31.y(), var31.z(), var25, var26, var27, 1.0F, var29, var30, param7, var28, var15.x(), var15.y(), var15.z());
      }
   }

   default VertexConsumer vertex(Matrix4f param0, float param1, float param2, float param3) {
      Vector4f var5 = param0.transform(new Vector4f(param1, param2, param3, 1.0F));
      return this.vertex(var5.x(), var5.y(), var5.z());
   }

   default VertexConsumer normal(Matrix3f param0, float param1, float param2, float param3) {
      Vector3f var5 = param0.transform(new Vector3f(param1, param2, param3));
      return this.normal(var5.x(), var5.y(), var5.z());
   }
}
