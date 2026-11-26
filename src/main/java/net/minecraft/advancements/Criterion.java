package net.minecraft.advancements;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class Criterion {
   @Nullable
   private final CriterionTriggerInstance trigger;

   public Criterion(CriterionTriggerInstance param0) {
      this.trigger = param0;
   }

   public Criterion() {
      this.trigger = null;
   }

   public void serializeToNetwork(FriendlyByteBuf param0) {
   }

   public static Criterion criterionFromJson(JsonObject param0, DeserializationContext param1) {
      ResourceLocation var2 = new ResourceLocation(GsonHelper.getAsString(param0, "trigger"));
      CriterionTrigger<?> var3 = CriteriaTriggers.getCriterion(var2);
      if (var3 == null) {
         throw new JsonSyntaxException("Invalid criterion trigger: " + var2);
      }

      CriterionTriggerInstance var4 = var3.createInstance(GsonHelper.getAsJsonObject(param0, "conditions", new JsonObject()), param1);
      return new Criterion(var4);
   }

   public static Criterion criterionFromNetwork(FriendlyByteBuf param0) {
      return new Criterion();
   }

   public static Map<String, Criterion> criteriaFromJson(JsonObject param0, DeserializationContext param1) {
      Map<String, Criterion> var2 = Maps.newHashMap();

      for (Entry<String, JsonElement> var4 : param0.entrySet()) {
         var2.put(var4.getKey(), criterionFromJson(GsonHelper.convertToJsonObject(var4.getValue(), "criterion"), param1));
      }

      return var2;
   }

   public static Map<String, Criterion> criteriaFromNetwork(FriendlyByteBuf param0) {
      return param0.readMap(FriendlyByteBuf::readUtf, Criterion::criterionFromNetwork);
   }

   public static void serializeToNetwork(Map<String, Criterion> param0, FriendlyByteBuf param1) {
      param1.writeMap(param0, FriendlyByteBuf::writeUtf, (l_param0, l_param1) -> l_param1.serializeToNetwork(l_param0));
   }

   @Nullable
   public CriterionTriggerInstance getTrigger() {
      return this.trigger;
   }

   public JsonElement serializeToJson() {
      if (this.trigger == null) {
         throw new JsonSyntaxException("Missing trigger");
      }

      JsonObject var1 = new JsonObject();
      var1.addProperty("trigger", this.trigger.getCriterion().toString());
      JsonObject var2 = this.trigger.serializeToJson(SerializationContext.INSTANCE);
      if (var2.size() != 0) {
         var1.add("conditions", var2);
      }

      return var1;
   }
}
