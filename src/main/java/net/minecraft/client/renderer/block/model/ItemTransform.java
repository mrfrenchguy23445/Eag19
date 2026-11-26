package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Type;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ItemTransform {
   public static final ItemTransform NO_TRANSFORM = new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));
   public final Vector3f rotation;
   public final Vector3f translation;
   public final Vector3f scale;

   public ItemTransform(Vector3f param0, Vector3f param1, Vector3f param2) {
      this.rotation = new Vector3f(param0);
      this.translation = new Vector3f(param1);
      this.scale = new Vector3f(param2);
   }

   public void apply(boolean param0, PoseStack param1) {
      if (this != NO_TRANSFORM) {
         float var3 = this.rotation.x();
         float var4 = this.rotation.y();
         float var5 = this.rotation.z();
         if (param0) {
            var4 = -var4;
            var5 = -var5;
         }

         int var6 = param0 ? -1 : 1;
         param1.translate(var6 * this.translation.x(), this.translation.y(), this.translation.z());
         param1.mulPose(new Quaternionf().rotationXYZ(var3 * (float) (Math.PI / 180.0), var4 * (float) (Math.PI / 180.0), var5 * (float) (Math.PI / 180.0)));
         param1.scale(this.scale.x(), this.scale.y(), this.scale.z());
      }
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      }

      if (this.getClass() != param0.getClass()) {
         return false;
      }

      ItemTransform var2 = (ItemTransform)param0;
      return this.rotation.equals(var2.rotation) && this.scale.equals(var2.scale) && this.translation.equals(var2.translation);
   }

   @Override
   public int hashCode() {
      int var1 = this.rotation.hashCode();
      var1 = 31 * var1 + this.translation.hashCode();
      return 31 * var1 + this.scale.hashCode();
   }

   protected static class Deserializer implements JsonDeserializer<ItemTransform> {
      private static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
      private static final Vector3f DEFAULT_TRANSLATION = new Vector3f(0.0F, 0.0F, 0.0F);
      private static final Vector3f DEFAULT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);
      public static final float MAX_TRANSLATION = 5.0F;
      public static final float MAX_SCALE = 4.0F;

      public ItemTransform deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         JsonObject var4 = param0.getAsJsonObject();
         Vector3f var5 = this.getVector3f(var4, "rotation", DEFAULT_ROTATION);
         Vector3f var6 = this.getVector3f(var4, "translation", DEFAULT_TRANSLATION);
         var6.mul(0.0625F);
         var6.set(Mth.clamp(var6.x, -5.0F, 5.0F), Mth.clamp(var6.y, -5.0F, 5.0F), Mth.clamp(var6.z, -5.0F, 5.0F));
         Vector3f var7 = this.getVector3f(var4, "scale", DEFAULT_SCALE);
         var7.set(Mth.clamp(var7.x, -4.0F, 4.0F), Mth.clamp(var7.y, -4.0F, 4.0F), Mth.clamp(var7.z, -4.0F, 4.0F));
         return new ItemTransform(var5, var6, var7);
      }

      private Vector3f getVector3f(JsonObject param0, String param1, Vector3f param2) {
         if (!param0.has(param1)) {
            return param2;
         }

         JsonArray var4 = GsonHelper.getAsJsonArray(param0, param1);
         if (var4.size() != 3) {
            throw new JsonParseException("Expected 3 " + param1 + " values, found: " + var4.size());
         }

         float[] var5 = new float[3];

         for (int var6 = 0; var6 < var5.length; var6++) {
            var5[var6] = GsonHelper.convertToFloat(var4.get(var6), param1 + "[" + var6 + "]");
         }

         return new Vector3f(var5[0], var5[1], var5[2]);
      }
   }
}
