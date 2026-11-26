package net.minecraft.world.level.entity;

import javax.annotation.Nullable;

public interface EntityTypeTest<B, T extends B> {
   static <B, T extends B> EntityTypeTest<B, T> forClass(final Class<T> param0) {
      return new EntityTypeTest<B, T>() {
         @Nullable
         @Override
         public T tryCast(B l_param0) {
            return (T)(param0.isInstance(l_param0) ? l_param0 : null);
         }

         @Override
         public Class<? extends B> getBaseClass() {
            return param0;
         }
      };
   }

   @Nullable
   T tryCast(B var1);

   Class<? extends B> getBaseClass();
}
