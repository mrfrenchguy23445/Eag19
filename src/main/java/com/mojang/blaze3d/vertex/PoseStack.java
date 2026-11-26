package com.mojang.blaze3d.vertex;

import com.google.common.collect.Queues;
import java.util.Deque;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class PoseStack {
   private final Deque<PoseStack.Pose> poseStack = Util.make(Queues.newArrayDeque(), l_param0 -> {
      Matrix4f l_var1 = new Matrix4f();
      Matrix3f l_var2 = new Matrix3f();
      l_param0.add(new PoseStack.Pose(l_var1, l_var2));
   });

   public void translate(double param0, double param2, double param4) {
      this.translate((float)param0, (float)param2, (float)param4);
   }

   public void translate(float param0, float param1, float param2) {
      PoseStack.Pose var4 = this.poseStack.getLast();
      var4.pose.translate(param0, param1, param2);
   }

   public void scale(float param0, float param1, float param2) {
      PoseStack.Pose var4 = this.poseStack.getLast();
      var4.pose.scale(param0, param1, param2);
      if (param0 == param1 && param1 == param2) {
         if (param0 > 0.0F) {
            return;
         }

         var4.normal.scale(-1.0F);
      }

      float var5 = 1.0F / param0;
      float var6 = 1.0F / param1;
      float var7 = 1.0F / param2;
      float var8 = Mth.fastInvCubeRoot(var5 * var6 * var7);
      var4.normal.scale(var8 * var5, var8 * var6, var8 * var7);
   }

   public void mulPose(Quaternionf param0) {
      PoseStack.Pose var2 = this.poseStack.getLast();
      var2.pose.rotate(param0);
      var2.normal.rotate(param0);
   }

   public void rotateAround(Quaternionf param0, float param1, float param2, float param3) {
      PoseStack.Pose var5 = this.poseStack.getLast();
      var5.pose.rotateAround(param0, param1, param2, param3);
      var5.normal.rotate(param0);
   }

   public void pushPose() {
      PoseStack.Pose var1 = this.poseStack.getLast();
      this.poseStack.addLast(new PoseStack.Pose(new Matrix4f(var1.pose), new Matrix3f(var1.normal)));
   }

   public void popPose() {
      this.poseStack.removeLast();
   }

   public PoseStack.Pose last() {
      return this.poseStack.getLast();
   }

   public boolean clear() {
      return this.poseStack.size() == 1;
   }

   public void setIdentity() {
      PoseStack.Pose var1 = this.poseStack.getLast();
      var1.pose.identity();
      var1.normal.identity();
   }

   public void mulPoseMatrix(Matrix4f param0) {
      this.poseStack.getLast().pose.mul(param0);
   }

   public static final class Pose {
      final Matrix4f pose;
      final Matrix3f normal;

      Pose(Matrix4f param0, Matrix3f param1) {
         this.pose = param0;
         this.normal = param1;
      }

      public Matrix4f pose() {
         return this.pose;
      }

      public Matrix3f normal() {
         return this.normal;
      }
   }
}
