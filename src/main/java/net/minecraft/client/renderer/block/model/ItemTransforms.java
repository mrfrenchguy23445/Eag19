package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraft.world.item.ItemDisplayContext;

public class ItemTransforms {
   public static final ItemTransforms NO_TRANSFORMS = new ItemTransforms();
   public final ItemTransform thirdPersonLeftHand;
   public final ItemTransform thirdPersonRightHand;
   public final ItemTransform firstPersonLeftHand;
   public final ItemTransform firstPersonRightHand;
   public final ItemTransform head;
   public final ItemTransform gui;
   public final ItemTransform ground;
   public final ItemTransform fixed;

   private ItemTransforms() {
      this(
         ItemTransform.NO_TRANSFORM,
         ItemTransform.NO_TRANSFORM,
         ItemTransform.NO_TRANSFORM,
         ItemTransform.NO_TRANSFORM,
         ItemTransform.NO_TRANSFORM,
         ItemTransform.NO_TRANSFORM,
         ItemTransform.NO_TRANSFORM,
         ItemTransform.NO_TRANSFORM
      );
   }

   public ItemTransforms(ItemTransforms param0) {
      this.thirdPersonLeftHand = param0.thirdPersonLeftHand;
      this.thirdPersonRightHand = param0.thirdPersonRightHand;
      this.firstPersonLeftHand = param0.firstPersonLeftHand;
      this.firstPersonRightHand = param0.firstPersonRightHand;
      this.head = param0.head;
      this.gui = param0.gui;
      this.ground = param0.ground;
      this.fixed = param0.fixed;
   }

   public ItemTransforms(
      ItemTransform param0,
      ItemTransform param1,
      ItemTransform param2,
      ItemTransform param3,
      ItemTransform param4,
      ItemTransform param5,
      ItemTransform param6,
      ItemTransform param7
   ) {
      this.thirdPersonLeftHand = param0;
      this.thirdPersonRightHand = param1;
      this.firstPersonLeftHand = param2;
      this.firstPersonRightHand = param3;
      this.head = param4;
      this.gui = param5;
      this.ground = param6;
      this.fixed = param7;
   }

   public ItemTransform getTransform(ItemDisplayContext param0) {
      return switch (param0) {
         case THIRD_PERSON_LEFT_HAND -> this.thirdPersonLeftHand;
         case THIRD_PERSON_RIGHT_HAND -> this.thirdPersonRightHand;
         case FIRST_PERSON_LEFT_HAND -> this.firstPersonLeftHand;
         case FIRST_PERSON_RIGHT_HAND -> this.firstPersonRightHand;
         case HEAD -> this.head;
         case GUI -> this.gui;
         case GROUND -> this.ground;
         case FIXED -> this.fixed;
         default -> ItemTransform.NO_TRANSFORM;
      };
   }

   public boolean hasTransform(ItemDisplayContext param0) {
      return this.getTransform(param0) != ItemTransform.NO_TRANSFORM;
   }

   protected static class Deserializer implements JsonDeserializer<ItemTransforms> {
      public ItemTransforms deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         JsonObject var4 = param0.getAsJsonObject();
         ItemTransform var5 = this.getTransform(param2, var4, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
         ItemTransform var6 = this.getTransform(param2, var4, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
         if (var6 == ItemTransform.NO_TRANSFORM) {
            var6 = var5;
         }

         ItemTransform var7 = this.getTransform(param2, var4, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
         ItemTransform var8 = this.getTransform(param2, var4, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
         if (var8 == ItemTransform.NO_TRANSFORM) {
            var8 = var7;
         }

         ItemTransform var9 = this.getTransform(param2, var4, ItemDisplayContext.HEAD);
         ItemTransform var10 = this.getTransform(param2, var4, ItemDisplayContext.GUI);
         ItemTransform var11 = this.getTransform(param2, var4, ItemDisplayContext.GROUND);
         ItemTransform var12 = this.getTransform(param2, var4, ItemDisplayContext.FIXED);
         return new ItemTransforms(var6, var5, var8, var7, var9, var10, var11, var12);
      }

      private ItemTransform getTransform(JsonDeserializationContext param0, JsonObject param1, ItemDisplayContext param2) {
         String var4 = param2.getSerializedName();
         return param1.has(var4) ? param0.deserialize(param1.get(var4), ItemTransform.class) : ItemTransform.NO_TRANSFORM;
      }
   }
}
