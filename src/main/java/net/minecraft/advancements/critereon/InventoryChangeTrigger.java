package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class InventoryChangeTrigger extends SimpleCriterionTrigger<InventoryChangeTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("inventory_changed");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public InventoryChangeTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      JsonObject var4 = GsonHelper.getAsJsonObject(param0, "slots", new JsonObject());
      MinMaxBounds.Ints var5 = MinMaxBounds.Ints.fromJson(var4.get("occupied"));
      MinMaxBounds.Ints var6 = MinMaxBounds.Ints.fromJson(var4.get("full"));
      MinMaxBounds.Ints var7 = MinMaxBounds.Ints.fromJson(var4.get("empty"));
      ItemPredicate[] var8 = ItemPredicate.fromJsonArray(param0.get("items"));
      return new InventoryChangeTrigger.TriggerInstance(param1, var5, var6, var7, var8);
   }

   public void trigger(ServerPlayer param0, Inventory param1, ItemStack param2) {
      int var4 = 0;
      int var5 = 0;
      int var6 = 0;

      for (int var7 = 0; var7 < param1.getContainerSize(); var7++) {
         ItemStack var8 = param1.getItem(var7);
         if (var8.isEmpty()) {
            var5++;
         } else {
            var6++;
            if (var8.getCount() >= var8.getMaxStackSize()) {
               var4++;
            }
         }
      }

      this.trigger(param0, param1, param2, var4, var5, var6);
   }

   private void trigger(ServerPlayer param0, Inventory param1, ItemStack param2, int param3, int param4, int param5) {
      this.trigger(param0, l_param5 -> l_param5.matches(param1, param2, param3, param4, param5));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final MinMaxBounds.Ints slotsOccupied;
      private final MinMaxBounds.Ints slotsFull;
      private final MinMaxBounds.Ints slotsEmpty;
      private final ItemPredicate[] predicates;

      public TriggerInstance(
         EntityPredicate.Composite param0, MinMaxBounds.Ints param1, MinMaxBounds.Ints param2, MinMaxBounds.Ints param3, ItemPredicate[] param4
      ) {
         super(InventoryChangeTrigger.ID, param0);
         this.slotsOccupied = param1;
         this.slotsFull = param2;
         this.slotsEmpty = param3;
         this.predicates = param4;
      }

      public static InventoryChangeTrigger.TriggerInstance hasItems(ItemPredicate... param0) {
         return new InventoryChangeTrigger.TriggerInstance(
            EntityPredicate.Composite.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, param0
         );
      }

      public static InventoryChangeTrigger.TriggerInstance hasItems(ItemLike... param0) {
         ItemPredicate[] var1 = new ItemPredicate[param0.length];

         for (int var2 = 0; var2 < param0.length; var2++) {
            var1[var2] = new ItemPredicate(
               null,
               ImmutableSet.of(param0[var2].asItem()),
               MinMaxBounds.Ints.ANY,
               MinMaxBounds.Ints.ANY,
               EnchantmentPredicate.NONE,
               EnchantmentPredicate.NONE,
               null,
               NbtPredicate.ANY
            );
         }

         return hasItems(var1);
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         if (!this.slotsOccupied.isAny() || !this.slotsFull.isAny() || !this.slotsEmpty.isAny()) {
            JsonObject var3 = new JsonObject();
            var3.add("occupied", this.slotsOccupied.serializeToJson());
            var3.add("full", this.slotsFull.serializeToJson());
            var3.add("empty", this.slotsEmpty.serializeToJson());
            var2.add("slots", var3);
         }

         if (this.predicates.length > 0) {
            JsonArray var3 = new JsonArray();

            for (ItemPredicate var7 : this.predicates) {
               var3.add(var7.serializeToJson());
            }

            var2.add("items", var3);
         }

         return var2;
      }

      public boolean matches(Inventory param0, ItemStack param1, int param2, int param3, int param4) {
         if (!this.slotsFull.matches(param2)) {
            return false;
         }

         if (!this.slotsEmpty.matches(param3)) {
            return false;
         }

         if (!this.slotsOccupied.matches(param4)) {
            return false;
         }

         int var6 = this.predicates.length;
         if (var6 == 0) {
            return true;
         }

         if (var6 != 1) {
            List<ItemPredicate> var7 = new ObjectArrayList<>(this.predicates);
            int var8 = param0.getContainerSize();

            for (int var9 = 0; var9 < var8; var9++) {
               if (var7.isEmpty()) {
                  return true;
               }

               ItemStack var10 = param0.getItem(var9);
               if (!var10.isEmpty()) {
                  var7.removeIf(l_param1 -> l_param1.matches(var10));
               }
            }

            return var7.isEmpty();
         } else {
            return !param1.isEmpty() && this.predicates[0].matches(param1);
         }
      }
   }
}
