package net.minecraft.client.animation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public record AnimationChannel(AnimationChannel.Target target, Keyframe... keyframes) {
   public interface Interpolation {
      Vector3f apply(Vector3f var1, float var2, Keyframe[] var3, int var4, int var5, float var6);
   }

   public static class Interpolations {
      public static final AnimationChannel.Interpolation LINEAR = (param0, param1, param2, param3, param4, param5) -> {
         Vector3f var6 = param2[param3].target();
         Vector3f var7 = param2[param4].target();
         return var6.lerp(var7, param1, param0).mul(param5);
      };
      public static final AnimationChannel.Interpolation CATMULLROM = (param0, param1, param2, param3, param4, param5) -> {
         Vector3f var6 = param2[Math.max(0, param3 - 1)].target();
         Vector3f var7 = param2[param3].target();
         Vector3f var8 = param2[param4].target();
         Vector3f var9 = param2[Math.min(param2.length - 1, param4 + 1)].target();
         param0.set(
            Mth.catmullrom(param1, var6.x(), var7.x(), var8.x(), var9.x()) * param5,
            Mth.catmullrom(param1, var6.y(), var7.y(), var8.y(), var9.y()) * param5,
            Mth.catmullrom(param1, var6.z(), var7.z(), var8.z(), var9.z()) * param5
         );
         return param0;
      };
   }

   public interface Target {
      void apply(ModelPart var1, Vector3f var2);
   }

   public static class Targets {
      public static final AnimationChannel.Target POSITION = ModelPart::offsetPos;
      public static final AnimationChannel.Target ROTATION = ModelPart::offsetRotation;
      public static final AnimationChannel.Target SCALE = ModelPart::offsetScale;
   }
}
