package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

public class DistancePredicate {
   public static final DistancePredicate ANY = new DistancePredicate(
      MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY
   );
   private final MinMaxBounds.Doubles x;
   private final MinMaxBounds.Doubles y;
   private final MinMaxBounds.Doubles z;
   private final MinMaxBounds.Doubles horizontal;
   private final MinMaxBounds.Doubles absolute;

   public DistancePredicate(
      MinMaxBounds.Doubles param0, MinMaxBounds.Doubles param1, MinMaxBounds.Doubles param2, MinMaxBounds.Doubles param3, MinMaxBounds.Doubles param4
   ) {
      this.x = param0;
      this.y = param1;
      this.z = param2;
      this.horizontal = param3;
      this.absolute = param4;
   }

   public static DistancePredicate horizontal(MinMaxBounds.Doubles param0) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, param0, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate vertical(MinMaxBounds.Doubles param0) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, param0, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate absolute(MinMaxBounds.Doubles param0) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, param0);
   }

   public boolean matches(double param0, double param2, double param4, double param6, double param8, double param10) {
      float var13 = (float)(param0 - param6);
      float var14 = (float)(param2 - param8);
      float var15 = (float)(param4 - param10);
      if (!this.x.matches(Mth.abs(var13)) || !this.y.matches(Mth.abs(var14)) || !this.z.matches(Mth.abs(var15))) {
         return false;
      } else {
         return !this.horizontal.matchesSqr(var13 * var13 + var15 * var15) ? false : this.absolute.matchesSqr(var13 * var13 + var14 * var14 + var15 * var15);
      }
   }

   public static DistancePredicate fromJson(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(param0, "distance");
         MinMaxBounds.Doubles var2 = MinMaxBounds.Doubles.fromJson(var1.get("x"));
         MinMaxBounds.Doubles var3 = MinMaxBounds.Doubles.fromJson(var1.get("y"));
         MinMaxBounds.Doubles var4 = MinMaxBounds.Doubles.fromJson(var1.get("z"));
         MinMaxBounds.Doubles var5 = MinMaxBounds.Doubles.fromJson(var1.get("horizontal"));
         MinMaxBounds.Doubles var6 = MinMaxBounds.Doubles.fromJson(var1.get("absolute"));
         return new DistancePredicate(var2, var3, var4, var5, var6);
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      }

      JsonObject var1 = new JsonObject();
      var1.add("x", this.x.serializeToJson());
      var1.add("y", this.y.serializeToJson());
      var1.add("z", this.z.serializeToJson());
      var1.add("horizontal", this.horizontal.serializeToJson());
      var1.add("absolute", this.absolute.serializeToJson());
      return var1;
   }
}
