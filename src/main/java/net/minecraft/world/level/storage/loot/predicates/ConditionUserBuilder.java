package net.minecraft.world.level.storage.loot.predicates;

import java.util.function.Function;

public interface ConditionUserBuilder<T extends ConditionUserBuilder<T>> {
   T when(LootItemCondition.Builder var1);

   default <E> T when(Iterable<E> param0, Function<E, LootItemCondition.Builder> param1) {
      T var3 = this.unwrap();

      for (E var5 : param0) {
         var3 = var3.when(param1.apply(var5));
      }

      return var3;
   }

   T unwrap();
}
