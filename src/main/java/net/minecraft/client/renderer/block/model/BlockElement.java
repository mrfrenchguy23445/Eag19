package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class BlockElement {
   private static final boolean DEFAULT_RESCALE = false;
   private static final float MIN_EXTENT = -16.0F;
   private static final float MAX_EXTENT = 32.0F;
   public final Vector3f from;
   public final Vector3f to;
   public final Map<Direction, BlockElementFace> faces;
   public final BlockElementRotation rotation;
   public final boolean shade;

   public BlockElement(Vector3f param0, Vector3f param1, Map<Direction, BlockElementFace> param2, @Nullable BlockElementRotation param3, boolean param4) {
      this.from = param0;
      this.to = param1;
      this.faces = param2;
      this.rotation = param3;
      this.shade = param4;
      this.fillUvs();
   }

   private void fillUvs() {
      for (Entry<Direction, BlockElementFace> var2 : this.faces.entrySet()) {
         float[] var3 = this.uvsByFace(var2.getKey());
         var2.getValue().uv.setMissingUv(var3);
      }
   }

   private float[] uvsByFace(Direction param0) {
      switch (param0) {
         case DOWN:
            return new float[]{this.from.x(), 16.0F - this.to.z(), this.to.x(), 16.0F - this.from.z()};
         case UP:
            return new float[]{this.from.x(), this.from.z(), this.to.x(), this.to.z()};
         case NORTH:
         default:
            return new float[]{16.0F - this.to.x(), 16.0F - this.to.y(), 16.0F - this.from.x(), 16.0F - this.from.y()};
         case SOUTH:
            return new float[]{this.from.x(), 16.0F - this.to.y(), this.to.x(), 16.0F - this.from.y()};
         case WEST:
            return new float[]{this.from.z(), 16.0F - this.to.y(), this.to.z(), 16.0F - this.from.y()};
         case EAST:
            return new float[]{16.0F - this.to.z(), 16.0F - this.to.y(), 16.0F - this.from.z(), 16.0F - this.from.y()};
      }
   }

   protected static class Deserializer implements JsonDeserializer<BlockElement> {
      private static final boolean DEFAULT_SHADE = true;

      public BlockElement deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         JsonObject var4 = param0.getAsJsonObject();
         Vector3f var5 = this.getFrom(var4);
         Vector3f var6 = this.getTo(var4);
         BlockElementRotation var7 = this.getRotation(var4);
         Map<Direction, BlockElementFace> var8 = this.getFaces(param2, var4);
         if (var4.has("shade") && !GsonHelper.isBooleanValue(var4, "shade")) {
            throw new JsonParseException("Expected shade to be a Boolean");
         }

         boolean var9 = GsonHelper.getAsBoolean(var4, "shade", true);
         return new BlockElement(var5, var6, var8, var7, var9);
      }

      @Nullable
      private BlockElementRotation getRotation(JsonObject param0) {
         BlockElementRotation var2 = null;
         if (param0.has("rotation")) {
            JsonObject var3 = GsonHelper.getAsJsonObject(param0, "rotation");
            Vector3f var4 = this.getVector3f(var3, "origin");
            var4.mul(0.0625F);
            Direction.Axis var5 = this.getAxis(var3);
            float var6 = this.getAngle(var3);
            boolean var7 = GsonHelper.getAsBoolean(var3, "rescale", false);
            var2 = new BlockElementRotation(var4, var5, var6, var7);
         }

         return var2;
      }

      private float getAngle(JsonObject param0) {
         float var2 = GsonHelper.getAsFloat(param0, "angle");
         if (var2 != 0.0F && Mth.abs(var2) != 22.5F && Mth.abs(var2) != 45.0F) {
            throw new JsonParseException("Invalid rotation " + var2 + " found, only -45/-22.5/0/22.5/45 allowed");
         } else {
            return var2;
         }
      }

      private Direction.Axis getAxis(JsonObject param0) {
         String var2 = GsonHelper.getAsString(param0, "axis");
         Direction.Axis var3 = Direction.Axis.byName(var2.toLowerCase(Locale.ROOT));
         if (var3 == null) {
            throw new JsonParseException("Invalid rotation axis: " + var2);
         } else {
            return var3;
         }
      }

      private Map<Direction, BlockElementFace> getFaces(JsonDeserializationContext param0, JsonObject param1) {
         Map<Direction, BlockElementFace> var3 = this.filterNullFromFaces(param0, param1);
         if (var3.isEmpty()) {
            throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
         } else {
            return var3;
         }
      }

      private Map<Direction, BlockElementFace> filterNullFromFaces(JsonDeserializationContext param0, JsonObject param1) {
         Map<Direction, BlockElementFace> var3 = Maps.newEnumMap(Direction.class);
         JsonObject var4 = GsonHelper.getAsJsonObject(param1, "faces");

         for (Entry<String, JsonElement> var6 : var4.entrySet()) {
            Direction var7 = this.getFacing(var6.getKey());
            var3.put(var7, param0.deserialize(var6.getValue(), BlockElementFace.class));
         }

         return var3;
      }

      private Direction getFacing(String param0) {
         Direction var2 = Direction.byName(param0);
         if (var2 == null) {
            throw new JsonParseException("Unknown facing: " + param0);
         } else {
            return var2;
         }
      }

      private Vector3f getTo(JsonObject param0) {
         Vector3f var2 = this.getVector3f(param0, "to");
         if (!(var2.x() < -16.0F) && !(var2.y() < -16.0F) && !(var2.z() < -16.0F) && !(var2.x() > 32.0F) && !(var2.y() > 32.0F) && !(var2.z() > 32.0F)) {
            return var2;
         } else {
            throw new JsonParseException("'to' specifier exceeds the allowed boundaries: " + var2);
         }
      }

      private Vector3f getFrom(JsonObject param0) {
         Vector3f var2 = this.getVector3f(param0, "from");
         if (!(var2.x() < -16.0F) && !(var2.y() < -16.0F) && !(var2.z() < -16.0F) && !(var2.x() > 32.0F) && !(var2.y() > 32.0F) && !(var2.z() > 32.0F)) {
            return var2;
         } else {
            throw new JsonParseException("'from' specifier exceeds the allowed boundaries: " + var2);
         }
      }

      private Vector3f getVector3f(JsonObject param0, String param1) {
         JsonArray var3 = GsonHelper.getAsJsonArray(param0, param1);
         if (var3.size() != 3) {
            throw new JsonParseException("Expected 3 " + param1 + " values, found: " + var3.size());
         }

         float[] var4 = new float[3];

         for (int var5 = 0; var5 < var4.length; var5++) {
            var4[var5] = GsonHelper.convertToFloat(var3.get(var5), param1 + "[" + var5 + "]");
         }

         return new Vector3f(var4[0], var4[1], var4[2]);
      }
   }
}
