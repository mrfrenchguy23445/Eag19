package com.mojang.math;

import org.apache.commons.lang3.tuple.Triple;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MatrixUtil {
   private static final float G = 3.0F + 2.0F * Math.sqrt(2.0F);
   private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle((float) (java.lang.Math.PI / 4));

   private MatrixUtil() {
   }

   public static Matrix4f mulComponentWise(Matrix4f param0, float param1) {
      return param0.set(
         param0.m00() * param1,
         param0.m01() * param1,
         param0.m02() * param1,
         param0.m03() * param1,
         param0.m10() * param1,
         param0.m11() * param1,
         param0.m12() * param1,
         param0.m13() * param1,
         param0.m20() * param1,
         param0.m21() * param1,
         param0.m22() * param1,
         param0.m23() * param1,
         param0.m30() * param1,
         param0.m31() * param1,
         param0.m32() * param1,
         param0.m33() * param1
      );
   }

   private static GivensParameters approxGivensQuat(float param0, float param1, float param2) {
      float var3 = 2.0F * (param0 - param2);
      float var4 = param1;
      return G * var4 * var4 < var3 * var3 ? GivensParameters.fromUnnormalized(var4, var3) : PI_4;
   }

   private static GivensParameters qrGivensQuat(float param0, float param1) {
      float var2 = (float)java.lang.Math.hypot(param0, param1);
      float var3 = var2 > 1.0E-6F ? param1 : 0.0F;
      float var4 = Math.abs(param0) + Math.max(var2, 1.0E-6F);
      if (param0 < 0.0F) {
         float var5 = var3;
         var3 = var4;
         var4 = var5;
      }

      return GivensParameters.fromUnnormalized(var3, var4);
   }

   private static void similarityTransform(Matrix3f param0, Matrix3f param1) {
      param0.mul(param1);
      param1.transpose();
      param1.mul(param0);
      param0.set(param1);
   }

   private static void stepJacobi(Matrix3f param0, Matrix3f param1, Quaternionf param2, Quaternionf param3) {
      if (param0.m01 * param0.m01 + param0.m10 * param0.m10 > 1.0E-6F) {
         GivensParameters var4 = approxGivensQuat(param0.m00, 0.5F * (param0.m01 + param0.m10), param0.m11);
         Quaternionf var5 = var4.aroundZ(param2);
         param3.mul(var5);
         var4.aroundZ(param1);
         similarityTransform(param0, param1);
      }

      if (param0.m02 * param0.m02 + param0.m20 * param0.m20 > 1.0E-6F) {
         GivensParameters var4 = approxGivensQuat(param0.m00, 0.5F * (param0.m02 + param0.m20), param0.m22).inverse();
         Quaternionf var5 = var4.aroundY(param2);
         param3.mul(var5);
         var4.aroundY(param1);
         similarityTransform(param0, param1);
      }

      if (param0.m12 * param0.m12 + param0.m21 * param0.m21 > 1.0E-6F) {
         GivensParameters var4 = approxGivensQuat(param0.m11, 0.5F * (param0.m12 + param0.m21), param0.m22);
         Quaternionf var5 = var4.aroundX(param2);
         param3.mul(var5);
         var4.aroundX(param1);
         similarityTransform(param0, param1);
      }
   }

   public static Quaternionf eigenvalueJacobi(Matrix3f param0, int param1) {
      Quaternionf var2 = new Quaternionf();
      Matrix3f var3 = new Matrix3f();
      Quaternionf var4 = new Quaternionf();

      for (int var5 = 0; var5 < param1; var5++) {
         stepJacobi(param0, var3, var4, var2);
      }

      var2.normalize();
      return var2;
   }

   public static Triple<Quaternionf, Vector3f, Quaternionf> svdDecompose(Matrix3f param0) {
      Matrix3f var1 = new Matrix3f(param0);
      var1.transpose();
      var1.mul(param0);
      Quaternionf var2 = eigenvalueJacobi(var1, 5);
      boolean var3 = var1.m00 < 1.0E-6;
      boolean var4 = var1.m11 < 1.0E-6;
      Matrix3f var5 = var1;
      Matrix3f var6 = param0.rotate(var2);
      float var7 = 1.0F;
      Quaternionf var8 = new Quaternionf();
      Quaternionf var9 = new Quaternionf();
      GivensParameters var10;
      if (var3) {
         var10 = qrGivensQuat(var6.m11, -var6.m10);
      } else {
         var10 = qrGivensQuat(var6.m00, var6.m01);
      }

      Quaternionf var11 = var10.aroundZ(var9);
      Matrix3f var12 = var10.aroundZ(var5);
      var7 *= var12.m22;
      var8.mul(var11);
      var12.transpose().mul(var6);
      var5 = var6;
      if (var3) {
         var10 = qrGivensQuat(var12.m22, -var12.m20);
      } else {
         var10 = qrGivensQuat(var12.m00, var12.m02);
      }

      var10 = var10.inverse();
      Quaternionf var13 = var10.aroundY(var9);
      Matrix3f var14 = var10.aroundY(var5);
      var7 *= var14.m11;
      var8.mul(var13);
      var14.transpose().mul(var12);
      var5 = var12;
      if (var4) {
         var10 = qrGivensQuat(var14.m22, -var14.m21);
      } else {
         var10 = qrGivensQuat(var14.m11, var14.m12);
      }

      Quaternionf var15 = var10.aroundX(var9);
      Matrix3f var16 = var10.aroundX(var5);
      var7 *= var16.m00;
      var8.mul(var15);
      var16.transpose().mul(var14);
      var7 = 1.0F / var7;
      var8.mul(Math.sqrt(var7));
      Vector3f var17 = new Vector3f(var16.m00 * var7, var16.m11 * var7, var16.m22 * var7);
      return Triple.of(var8, var17, var2.conjugate());
   }
}
