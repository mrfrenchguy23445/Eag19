package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class CompositeEntryBase extends LootPoolEntryContainer {
   protected final LootPoolEntryContainer[] children;
   private final ComposableEntryContainer composedChildren;

   protected CompositeEntryBase(LootPoolEntryContainer[] param0, LootItemCondition[] param1) {
      super(param1);
      this.children = param0;
      this.composedChildren = this.compose(param0);
   }

   @Override
   public void validate(ValidationContext param0) {
      super.validate(param0);
      if (this.children.length == 0) {
         param0.reportProblem("Empty children list");
      }

      for (int var2 = 0; var2 < this.children.length; var2++) {
         this.children[var2].validate(param0.forChild(".entry[" + var2 + "]"));
      }
   }

   protected abstract ComposableEntryContainer compose(ComposableEntryContainer[] var1);

   @Override
   public final boolean expand(LootContext param0, Consumer<LootPoolEntry> param1) {
      return !this.canRun(param0) ? false : this.composedChildren.expand(param0, param1);
   }

   public static <T extends CompositeEntryBase> LootPoolEntryContainer.Serializer<T> createSerializer(
      final CompositeEntryBase.CompositeEntryConstructor<T> param0
   ) {
      return new LootPoolEntryContainer.Serializer<T>() {
         public void serializeCustom(JsonObject l_param0, T l_param1, JsonSerializationContext l_param2) {
            l_param0.add("children", l_param2.serialize(l_param1.children));
         }

         public final T deserializeCustom(JsonObject l_param0, JsonDeserializationContext l_param1, LootItemCondition[] l_param2) {
            LootPoolEntryContainer[] l_var4 = GsonHelper.getAsObject(l_param0, "children", l_param1, LootPoolEntryContainer[].class);
            return param0.create(l_var4, l_param2);
         }
      };
   }

   @FunctionalInterface
   public interface CompositeEntryConstructor<T extends CompositeEntryBase> {
      T create(LootPoolEntryContainer[] var1, LootItemCondition[] var2);
   }
}
