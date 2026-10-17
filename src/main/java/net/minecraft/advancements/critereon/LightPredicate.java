package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;

public class LightPredicate {
   public static final LightPredicate ANY = new LightPredicate(MinMaxBounds.Ints.ANY);
   private final MinMaxBounds.Ints composite;

   LightPredicate(MinMaxBounds.Ints param0) {
      this.composite = param0;
   }

   public boolean matches(ServerLevel param0, BlockPos param1) {
      if (this == ANY) {
         return true;
      } else {
         return !param0.isLoaded(param1) ? false : this.composite.matches(param0.getMaxLocalRawBrightness(param1));
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      }

      JsonObject var1 = new JsonObject();
      var1.add("light", this.composite.serializeToJson());
      return var1;
   }

   public static LightPredicate fromJson(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(param0, "light");
         MinMaxBounds.Ints var2 = MinMaxBounds.Ints.fromJson(var1.get("light"));
         return new LightPredicate(var2);
      } else {
         return ANY;
      }
   }

   public static class Builder {
      private MinMaxBounds.Ints composite = MinMaxBounds.Ints.ANY;

      public static LightPredicate.Builder light() {
         return new LightPredicate.Builder();
      }

      public LightPredicate.Builder setComposite(MinMaxBounds.Ints param0) {
         this.composite = param0;
         return this;
      }

      public LightPredicate build() {
         return new LightPredicate(this.composite);
      }
   }
}
