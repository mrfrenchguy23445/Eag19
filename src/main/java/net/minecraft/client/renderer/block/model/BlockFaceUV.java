package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;

public class BlockFaceUV {
   public float[] uvs;
   public final int rotation;

   public BlockFaceUV(@Nullable float[] param0, int param1) {
      this.uvs = param0;
      this.rotation = param1;
   }

   public float getU(int param0) {
      if (this.uvs == null) {
         throw new NullPointerException("uvs");
      }

      int var2 = this.getShiftedIndex(param0);
      return this.uvs[var2 != 0 && var2 != 1 ? 2 : 0];
   }

   public float getV(int param0) {
      if (this.uvs == null) {
         throw new NullPointerException("uvs");
      }

      int var2 = this.getShiftedIndex(param0);
      return this.uvs[var2 != 0 && var2 != 3 ? 3 : 1];
   }

   private int getShiftedIndex(int param0) {
      return (param0 + this.rotation / 90) % 4;
   }

   public int getReverseIndex(int param0) {
      return (param0 + 4 - this.rotation / 90) % 4;
   }

   public void setMissingUv(float[] param0) {
      if (this.uvs == null) {
         this.uvs = param0;
      }
   }

   protected static class Deserializer implements JsonDeserializer<BlockFaceUV> {
      private static final int DEFAULT_ROTATION = 0;

      public BlockFaceUV deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         JsonObject var4 = param0.getAsJsonObject();
         float[] var5 = this.getUVs(var4);
         int var6 = this.getRotation(var4);
         return new BlockFaceUV(var5, var6);
      }

      protected int getRotation(JsonObject param0) {
         int var2 = GsonHelper.getAsInt(param0, "rotation", 0);
         if (var2 >= 0 && var2 % 90 == 0 && var2 / 90 <= 3) {
            return var2;
         } else {
            throw new JsonParseException("Invalid rotation " + var2 + " found, only 0/90/180/270 allowed");
         }
      }

      @Nullable
      private float[] getUVs(JsonObject param0) {
         if (!param0.has("uv")) {
            return null;
         }

         JsonArray var2 = GsonHelper.getAsJsonArray(param0, "uv");
         if (var2.size() != 4) {
            throw new JsonParseException("Expected 4 uv values, found: " + var2.size());
         }

         float[] var3 = new float[4];

         for (int var4 = 0; var4 < var3.length; var4++) {
            var3[var4] = GsonHelper.convertToFloat(var2.get(var4), "uv[" + var4 + "]");
         }

         return var3;
      }
   }
}
