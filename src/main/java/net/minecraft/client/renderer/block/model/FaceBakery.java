package net.minecraft.client.renderer.block.model;

import com.mojang.math.Transformation;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockMath;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class FaceBakery {
   public static final int VERTEX_INT_SIZE = 8;
   private static final float RESCALE_22_5 = 1.0F / (float)Math.cos((float) (Math.PI / 8)) - 1.0F;
   private static final float RESCALE_45 = 1.0F / (float)Math.cos((float) (Math.PI / 4)) - 1.0F;
   public static final int VERTEX_COUNT = 4;
   private static final int COLOR_INDEX = 3;
   public static final int UV_INDEX = 4;

   public BakedQuad bakeQuad(
      Vector3f param0,
      Vector3f param1,
      BlockElementFace param2,
      TextureAtlasSprite param3,
      Direction param4,
      ModelState param5,
      @Nullable BlockElementRotation param6,
      boolean param7,
      ResourceLocation param8
   ) {
      BlockFaceUV var10 = param2.uv;
      if (param5.isUvLocked()) {
         var10 = recomputeUVs(param2.uv, param4, param5.getRotation(), param8);
      }

      float[] var11 = new float[var10.uvs.length];
      System.arraycopy(var10.uvs, 0, var11, 0, var11.length);
      float var12 = param3.uvShrinkRatio();
      float var13 = (var10.uvs[0] + var10.uvs[0] + var10.uvs[2] + var10.uvs[2]) / 4.0F;
      float var14 = (var10.uvs[1] + var10.uvs[1] + var10.uvs[3] + var10.uvs[3]) / 4.0F;
      var10.uvs[0] = Mth.lerp(var12, var10.uvs[0], var13);
      var10.uvs[2] = Mth.lerp(var12, var10.uvs[2], var13);
      var10.uvs[1] = Mth.lerp(var12, var10.uvs[1], var14);
      var10.uvs[3] = Mth.lerp(var12, var10.uvs[3], var14);
      int[] var15 = this.makeVertices(var10, param3, param4, this.setupShape(param0, param1), param5.getRotation(), param6, param7);
      Direction var16 = calculateFacing(var15);
      System.arraycopy(var11, 0, var10.uvs, 0, var11.length);
      if (param6 == null) {
         this.recalculateWinding(var15, var16);
      }

      return new BakedQuad(var15, param2.tintIndex, var16, param3, param7);
   }

   public static BlockFaceUV recomputeUVs(BlockFaceUV param0, Direction param1, Transformation param2, ResourceLocation param3) {
      Matrix4f var4 = BlockMath.getUVLockTransform(param2, param1, () -> "Unable to resolve UVLock for model: " + param3).getMatrix();
      float var5 = param0.getU(param0.getReverseIndex(0));
      float var6 = param0.getV(param0.getReverseIndex(0));
      Vector4f var7 = var4.transform(new Vector4f(var5 / 16.0F, var6 / 16.0F, 0.0F, 1.0F));
      float var8 = 16.0F * var7.x();
      float var9 = 16.0F * var7.y();
      float var10 = param0.getU(param0.getReverseIndex(2));
      float var11 = param0.getV(param0.getReverseIndex(2));
      Vector4f var12 = var4.transform(new Vector4f(var10 / 16.0F, var11 / 16.0F, 0.0F, 1.0F));
      float var13 = 16.0F * var12.x();
      float var14 = 16.0F * var12.y();
      float var15;
      float var16;
      if (Math.signum(var10 - var5) == Math.signum(var13 - var8)) {
         var15 = var8;
         var16 = var13;
      } else {
         var15 = var13;
         var16 = var8;
      }

      float var17;
      float var18;
      if (Math.signum(var11 - var6) == Math.signum(var14 - var9)) {
         var17 = var9;
         var18 = var14;
      } else {
         var17 = var14;
         var18 = var9;
      }

      float var19 = (float)Math.toRadians(param0.rotation);
      Matrix3f var20 = new Matrix3f(var4);
      Vector3f var21 = var20.transform(new Vector3f(Mth.cos(var19), Mth.sin(var19), 0.0F));
      int var22 = Math.floorMod(-((int)Math.round(Math.toDegrees(Math.atan2(var21.y(), var21.x())) / 90.0)) * 90, 360);
      return new BlockFaceUV(new float[]{var15, var17, var16, var18}, var22);
   }

   private int[] makeVertices(
      BlockFaceUV param0,
      TextureAtlasSprite param1,
      Direction param2,
      float[] param3,
      Transformation param4,
      @Nullable BlockElementRotation param5,
      boolean param6
   ) {
      int[] var8 = new int[32];

      for (int var9 = 0; var9 < 4; var9++) {
         this.bakeVertex(var8, var9, param2, param0, param3, param1, param4, param5, param6);
      }

      return var8;
   }

   private float[] setupShape(Vector3f param0, Vector3f param1) {
      float[] var3 = new float[Direction.values().length];
      var3[FaceInfo.Constants.MIN_X] = param0.x() / 16.0F;
      var3[FaceInfo.Constants.MIN_Y] = param0.y() / 16.0F;
      var3[FaceInfo.Constants.MIN_Z] = param0.z() / 16.0F;
      var3[FaceInfo.Constants.MAX_X] = param1.x() / 16.0F;
      var3[FaceInfo.Constants.MAX_Y] = param1.y() / 16.0F;
      var3[FaceInfo.Constants.MAX_Z] = param1.z() / 16.0F;
      return var3;
   }

   private void bakeVertex(
      int[] param0,
      int param1,
      Direction param2,
      BlockFaceUV param3,
      float[] param4,
      TextureAtlasSprite param5,
      Transformation param6,
      @Nullable BlockElementRotation param7,
      boolean param8
   ) {
      FaceInfo.VertexInfo var10 = FaceInfo.fromFacing(param2).getVertexInfo(param1);
      Vector3f var11 = new Vector3f(param4[var10.xFace], param4[var10.yFace], param4[var10.zFace]);
      this.applyElementRotation(var11, param7);
      this.applyModelRotation(var11, param6);
      this.fillVertex(param0, param1, var11, param5, param3);
   }

   private void fillVertex(int[] param0, int param1, Vector3f param2, TextureAtlasSprite param3, BlockFaceUV param4) {
      int var6 = param1 * 8;
      param0[var6] = Float.floatToRawIntBits(param2.x());
      param0[var6 + 1] = Float.floatToRawIntBits(param2.y());
      param0[var6 + 2] = Float.floatToRawIntBits(param2.z());
      param0[var6 + 3] = -1;
      param0[var6 + 4] = Float.floatToRawIntBits(param3.getU(param4.getU(param1)));
      param0[var6 + 4 + 1] = Float.floatToRawIntBits(param3.getV(param4.getV(param1)));
   }

   private void applyElementRotation(Vector3f param0, @Nullable BlockElementRotation param1) {
      if (param1 != null) {
         Vector3f var3;
         Vector3f var4;
         switch (param1.axis()) {
            case X:
               var3 = new Vector3f(1.0F, 0.0F, 0.0F);
               var4 = new Vector3f(0.0F, 1.0F, 1.0F);
               break;
            case Y:
               var3 = new Vector3f(0.0F, 1.0F, 0.0F);
               var4 = new Vector3f(1.0F, 0.0F, 1.0F);
               break;
            case Z:
               var3 = new Vector3f(0.0F, 0.0F, 1.0F);
               var4 = new Vector3f(1.0F, 1.0F, 0.0F);
               break;
            default:
               throw new IllegalArgumentException("There are only 3 axes");
         }

         Quaternionf var5 = new Quaternionf().rotationAxis(param1.angle() * (float) (Math.PI / 180.0), var3);
         if (param1.rescale()) {
            if (Math.abs(param1.angle()) == 22.5F) {
               var4.mul(RESCALE_22_5);
            } else {
               var4.mul(RESCALE_45);
            }

            var4.add(1.0F, 1.0F, 1.0F);
         } else {
            var4.set(1.0F, 1.0F, 1.0F);
         }

         this.rotateVertexBy(param0, new Vector3f(param1.origin()), new Matrix4f().rotation(var5), var4);
      }
   }

   public void applyModelRotation(Vector3f param0, Transformation param1) {
      if (param1 != Transformation.identity()) {
         this.rotateVertexBy(param0, new Vector3f(0.5F, 0.5F, 0.5F), param1.getMatrix(), new Vector3f(1.0F, 1.0F, 1.0F));
      }
   }

   private void rotateVertexBy(Vector3f param0, Vector3f param1, Matrix4f param2, Vector3f param3) {
      Vector4f var5 = param2.transform(new Vector4f(param0.x() - param1.x(), param0.y() - param1.y(), param0.z() - param1.z(), 1.0F));
      var5.mul(new Vector4f(param3, 1.0F));
      param0.set(var5.x() + param1.x(), var5.y() + param1.y(), var5.z() + param1.z());
   }

   public static Direction calculateFacing(int[] param0) {
      Vector3f var1 = new Vector3f(Float.intBitsToFloat(param0[0]), Float.intBitsToFloat(param0[1]), Float.intBitsToFloat(param0[2]));
      Vector3f var2 = new Vector3f(Float.intBitsToFloat(param0[8]), Float.intBitsToFloat(param0[9]), Float.intBitsToFloat(param0[10]));
      Vector3f var3 = new Vector3f(Float.intBitsToFloat(param0[16]), Float.intBitsToFloat(param0[17]), Float.intBitsToFloat(param0[18]));
      Vector3f var4 = new Vector3f(var1).sub(var2);
      Vector3f var5 = new Vector3f(var3).sub(var2);
      Vector3f var6 = new Vector3f(var5).cross(var4).normalize();
      if (!var6.isFinite()) {
         return Direction.UP;
      }

      Direction var7 = null;
      float var8 = 0.0F;

      for (Direction var12 : Direction.values()) {
         Vec3i var13 = var12.getNormal();
         Vector3f var14 = new Vector3f(var13.getX(), var13.getY(), var13.getZ());
         float var15 = var6.dot(var14);
         if (var15 >= 0.0F && var15 > var8) {
            var8 = var15;
            var7 = var12;
         }
      }

      return var7 == null ? Direction.UP : var7;
   }

   private void recalculateWinding(int[] param0, Direction param1) {
      int[] var3 = new int[param0.length];
      System.arraycopy(param0, 0, var3, 0, param0.length);
      float[] var4 = new float[Direction.values().length];
      var4[FaceInfo.Constants.MIN_X] = 999.0F;
      var4[FaceInfo.Constants.MIN_Y] = 999.0F;
      var4[FaceInfo.Constants.MIN_Z] = 999.0F;
      var4[FaceInfo.Constants.MAX_X] = -999.0F;
      var4[FaceInfo.Constants.MAX_Y] = -999.0F;
      var4[FaceInfo.Constants.MAX_Z] = -999.0F;

      for (int var5 = 0; var5 < 4; var5++) {
         int var6 = 8 * var5;
         float var7 = Float.intBitsToFloat(var3[var6]);
         float var8 = Float.intBitsToFloat(var3[var6 + 1]);
         float var9 = Float.intBitsToFloat(var3[var6 + 2]);
         if (var7 < var4[FaceInfo.Constants.MIN_X]) {
            var4[FaceInfo.Constants.MIN_X] = var7;
         }

         if (var8 < var4[FaceInfo.Constants.MIN_Y]) {
            var4[FaceInfo.Constants.MIN_Y] = var8;
         }

         if (var9 < var4[FaceInfo.Constants.MIN_Z]) {
            var4[FaceInfo.Constants.MIN_Z] = var9;
         }

         if (var7 > var4[FaceInfo.Constants.MAX_X]) {
            var4[FaceInfo.Constants.MAX_X] = var7;
         }

         if (var8 > var4[FaceInfo.Constants.MAX_Y]) {
            var4[FaceInfo.Constants.MAX_Y] = var8;
         }

         if (var9 > var4[FaceInfo.Constants.MAX_Z]) {
            var4[FaceInfo.Constants.MAX_Z] = var9;
         }
      }

      FaceInfo var5 = FaceInfo.fromFacing(param1);

      for (int var6 = 0; var6 < 4; var6++) {
         int var7 = 8 * var6;
         FaceInfo.VertexInfo var8 = var5.getVertexInfo(var6);
         float var9 = var4[var8.xFace];
         float var10 = var4[var8.yFace];
         float var11 = var4[var8.zFace];
         param0[var7] = Float.floatToRawIntBits(var9);
         param0[var7 + 1] = Float.floatToRawIntBits(var10);
         param0[var7 + 2] = Float.floatToRawIntBits(var11);

         for (int var12 = 0; var12 < 4; var12++) {
            int var13 = 8 * var12;
            float var14 = Float.intBitsToFloat(var3[var13]);
            float var15 = Float.intBitsToFloat(var3[var13 + 1]);
            float var16 = Float.intBitsToFloat(var3[var13 + 2]);
            if (Mth.equal(var9, var14) && Mth.equal(var10, var15) && Mth.equal(var11, var16)) {
               param0[var7 + 4] = var3[var13 + 4];
               param0[var7 + 4 + 1] = var3[var13 + 4 + 1];
            }
         }
      }
   }
}
