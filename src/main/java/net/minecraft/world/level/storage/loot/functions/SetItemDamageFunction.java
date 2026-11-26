package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.logging.LogUtils;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.slf4j.Logger;

public class SetItemDamageFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   final NumberProvider damage;
   final boolean add;

   SetItemDamageFunction(LootItemCondition[] param0, NumberProvider param1, boolean param2) {
      super(param0);
      this.damage = param1;
      this.add = param2;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_DAMAGE;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.damage.getReferencedContextParams();
   }

   @Override
   public ItemStack run(ItemStack param0, LootContext param1) {
      if (param0.isDamageableItem()) {
         int var3 = param0.getMaxDamage();
         float var4 = this.add ? 1.0F - (float)param0.getDamageValue() / var3 : 0.0F;
         float var5 = 1.0F - Mth.clamp(this.damage.getFloat(param1) + var4, 0.0F, 1.0F);
         param0.setDamageValue(Mth.floor(var5 * var3));
      } else {
         LOGGER.warn("Couldn't set damage of loot item {}", param0);
      }

      return param0;
   }

   public static LootItemConditionalFunction.Builder<?> setDamage(NumberProvider param0) {
      return simpleBuilder(l_param1 -> new SetItemDamageFunction(l_param1, param0, false));
   }

   public static LootItemConditionalFunction.Builder<?> setDamage(NumberProvider param0, boolean param1) {
      return simpleBuilder(l_param2 -> new SetItemDamageFunction(l_param2, param0, param1));
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<SetItemDamageFunction> {
      public void serialize(JsonObject param0, SetItemDamageFunction param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         param0.add("damage", param2.serialize(param1.damage));
         param0.addProperty("add", param1.add);
      }

      public SetItemDamageFunction deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         NumberProvider var4 = GsonHelper.getAsObject(param0, "damage", param1, NumberProvider.class);
         boolean var5 = GsonHelper.getAsBoolean(param0, "add", false);
         return new SetItemDamageFunction(param2, var4, var5);
      }
   }
}
