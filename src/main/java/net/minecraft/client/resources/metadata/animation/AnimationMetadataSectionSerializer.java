package net.minecraft.client.resources.metadata.animation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javax.annotation.Nullable;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.Validate;

public class AnimationMetadataSectionSerializer implements MetadataSectionSerializer<AnimationMetadataSection> {
   public AnimationMetadataSection fromJson(JsonObject param0) {
      Builder<AnimationFrame> var2 = ImmutableList.builder();
      int var3 = GsonHelper.getAsInt(param0, "frametime", 1);
      if (var3 != 1) {
         Validate.inclusiveBetween(1L, 2147483647L, var3, "Invalid default frame time");
      }

      if (param0.has("frames")) {
         try {
            JsonArray var4 = GsonHelper.getAsJsonArray(param0, "frames");

            for (int var5 = 0; var5 < var4.size(); var5++) {
               JsonElement var6 = var4.get(var5);
               AnimationFrame var7 = this.getFrame(var5, var6);
               if (var7 != null) {
                  var2.add(var7);
               }
            }
         } catch (ClassCastException var4) {
            throw new JsonParseException("Invalid animation->frames: expected array, was " + param0.get("frames"), var4);
         }
      }

      int var4 = GsonHelper.getAsInt(param0, "width", -1);
      int var5 = GsonHelper.getAsInt(param0, "height", -1);
      if (var4 != -1) {
         Validate.inclusiveBetween(1L, 2147483647L, var4, "Invalid width");
      }

      if (var5 != -1) {
         Validate.inclusiveBetween(1L, 2147483647L, var5, "Invalid height");
      }

      boolean var6 = GsonHelper.getAsBoolean(param0, "interpolate", false);
      return new AnimationMetadataSection(var2.build(), var4, var5, var3, var6);
   }

   @Nullable
   private AnimationFrame getFrame(int param0, JsonElement param1) {
      if (param1.isJsonPrimitive()) {
         return new AnimationFrame(GsonHelper.convertToInt(param1, "frames[" + param0 + "]"));
      }

      if (param1.isJsonObject()) {
         JsonObject var3 = GsonHelper.convertToJsonObject(param1, "frames[" + param0 + "]");
         int var4 = GsonHelper.getAsInt(var3, "time", -1);
         if (var3.has("time")) {
            Validate.inclusiveBetween(1L, 2147483647L, var4, "Invalid frame time");
         }

         int var5 = GsonHelper.getAsInt(var3, "index");
         Validate.inclusiveBetween(0L, 2147483647L, var5, "Invalid frame index");
         return new AnimationFrame(var5, var4);
      } else {
         return null;
      }
   }

   @Override
   public String getMetadataSectionName() {
      return "animation";
   }
}
