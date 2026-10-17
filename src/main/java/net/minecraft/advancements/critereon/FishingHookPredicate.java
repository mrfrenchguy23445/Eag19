package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public class FishingHookPredicate implements EntitySubPredicate {
   public static final FishingHookPredicate ANY = new FishingHookPredicate(false);
   private static final String IN_OPEN_WATER_KEY = "in_open_water";
   private final boolean inOpenWater;

   private FishingHookPredicate(boolean param0) {
      this.inOpenWater = param0;
   }

   public static FishingHookPredicate inOpenWater(boolean param0) {
      return new FishingHookPredicate(param0);
   }

   public static FishingHookPredicate fromJson(JsonObject param0) {
      JsonElement var1 = param0.get("in_open_water");
      return var1 != null ? new FishingHookPredicate(GsonHelper.convertToBoolean(var1, "in_open_water")) : ANY;
   }

   @Override
   public JsonObject serializeCustomData() {
      if (this == ANY) {
         return new JsonObject();
      }

      JsonObject var1 = new JsonObject();
      var1.add("in_open_water", new JsonPrimitive(this.inOpenWater));
      return var1;
   }

   @Override
   public EntitySubPredicate.Type type() {
      return EntitySubPredicate.Types.FISHING_HOOK;
   }

   @Override
   public boolean matches(Entity param0, ServerLevel param1, @Nullable Vec3 param2) {
      if (this == ANY) {
         return true;
      } else {
         return !(param0 instanceof FishingHook var4) ? false : this.inOpenWater == var4.isOpenWaterFishing();
      }
   }
}
