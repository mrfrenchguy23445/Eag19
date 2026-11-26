package net.minecraft.world.level.storage.loot.functions;

import java.util.Arrays;
import java.util.function.Function;

public interface FunctionUserBuilder<T extends FunctionUserBuilder<T>> {
   T apply(LootItemFunction.Builder var1);

   default <E> T apply(Iterable<E> param0, Function<E, LootItemFunction.Builder> param1) {
      T var3 = this.unwrap();

      for (E var5 : param0) {
         var3 = var3.apply(param1.apply(var5));
      }

      return var3;
   }

   default <E> T apply(E[] param0, Function<E, LootItemFunction.Builder> param1) {
      return this.apply(Arrays.asList(param0), param1);
   }

   T unwrap();
}
