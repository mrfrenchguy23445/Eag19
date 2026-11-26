package net.minecraft.client.animation;

import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class KeyframeAnimations {
   public static void animate(HierarchicalModel<?> param0, AnimationDefinition param1, long param2, float param4, Vector3f param5) {
      float var6 = getElapsedSeconds(param1, param2);

      for (Entry<String, List<AnimationChannel>> var8 : param1.boneAnimations().entrySet()) {
         Optional<ModelPart> var9 = param0.getAnyDescendantWithName(var8.getKey());
         List<AnimationChannel> var10 = var8.getValue();
         var9.ifPresent(l_param4 -> var10.forEach(l2_param4 -> {
            Keyframe[] l2_var5 = l2_param4.keyframes();
            int l2_var6 = Math.max(0, Mth.binarySearch(0, l2_var5.length, l3_param2 -> var6 <= l2_var5[l3_param2].timestamp()) - 1);
            int l2_var7 = Math.min(l2_var5.length - 1, l2_var6 + 1);
            Keyframe l2_var8 = l2_var5[l2_var6];
            Keyframe l2_var9 = l2_var5[l2_var7];
            float l2_var10 = var6 - l2_var8.timestamp();
            float l2_var11 = Mth.clamp(l2_var10 / (l2_var9.timestamp() - l2_var8.timestamp()), 0.0F, 1.0F);
            l2_var9.interpolation().apply(param5, l2_var11, l2_var5, l2_var6, l2_var7, param4);
            l2_param4.target().apply(l_param4, param5);
         }));
      }
   }

   private static float getElapsedSeconds(AnimationDefinition param0, long param1) {
      float var3 = (float)param1 / 1000.0F;
      return param0.looping() ? var3 % param0.lengthInSeconds() : var3;
   }

   public static Vector3f posVec(float param0, float param1, float param2) {
      return new Vector3f(param0, -param1, param2);
   }

   public static Vector3f degreeVec(float param0, float param1, float param2) {
      return new Vector3f(param0 * (float) (Math.PI / 180.0), param1 * (float) (Math.PI / 180.0), param2 * (float) (Math.PI / 180.0));
   }

   public static Vector3f scaleVec(double param0, double param2, double param4) {
      return new Vector3f((float)(param0 - 1.0), (float)(param2 - 1.0), (float)(param4 - 1.0));
   }
}
