package net.minecraft.data.models.blockstates;

import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

public class VariantProperties {
   public static final VariantProperty<VariantProperties.Rotation> X_ROT = new VariantProperty<>("x", param0 -> new JsonPrimitive(param0.value));
   public static final VariantProperty<VariantProperties.Rotation> Y_ROT = new VariantProperty<>("y", param0 -> new JsonPrimitive(param0.value));
   public static final VariantProperty<ResourceLocation> MODEL = new VariantProperty<>("model", param0 -> new JsonPrimitive(param0.toString()));
   public static final VariantProperty<Boolean> UV_LOCK = new VariantProperty<>("uvlock", JsonPrimitive::new);
   public static final VariantProperty<Integer> WEIGHT = new VariantProperty<>("weight", JsonPrimitive::new);

   public enum Rotation {
      R0(0),
      R90(90),
      R180(180),
      R270(270);

      final int value;

      Rotation(int param2) {
         this.value = param2;
      }
   }
}
