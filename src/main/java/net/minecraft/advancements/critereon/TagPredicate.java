package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;

public class TagPredicate<T> {
   private final TagKey<T> tag;
   private final boolean expected;

   public TagPredicate(TagKey<T> param0, boolean param1) {
      this.tag = param0;
      this.expected = param1;
   }

   public static <T> TagPredicate<T> is(TagKey<T> param0) {
      return new TagPredicate<>(param0, true);
   }

   public static <T> TagPredicate<T> isNot(TagKey<T> param0) {
      return new TagPredicate<>(param0, false);
   }

   public boolean matches(Holder<T> param0) {
      return param0.is(this.tag) == this.expected;
   }

   public JsonElement serializeToJson() {
      JsonObject var1 = new JsonObject();
      var1.addProperty("id", this.tag.location().toString());
      var1.addProperty("expected", this.expected);
      return var1;
   }

   public static <T> TagPredicate<T> fromJson(@Nullable JsonElement param0, ResourceKey<? extends Registry<T>> param1) {
      if (param0 == null) {
         throw new JsonParseException("Expected a tag predicate");
      }

      JsonObject var2 = GsonHelper.convertToJsonObject(param0, "Tag Predicate");
      ResourceLocation var3 = new ResourceLocation(GsonHelper.getAsString(var2, "id"));
      boolean var4 = GsonHelper.getAsBoolean(var2, "expected");
      return new TagPredicate<>(TagKey.create(param1, var3), var4);
   }
}
