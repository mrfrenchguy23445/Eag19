package net.minecraft.network.syncher;

import java.util.Optional;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;

public interface EntityDataSerializer<T> {
   void write(FriendlyByteBuf var1, T var2);

   T read(FriendlyByteBuf var1);

   default EntityDataAccessor<T> createAccessor(int param0) {
      return new EntityDataAccessor<>(param0, this);
   }

   T copy(T var1);

   static <T> EntityDataSerializer<T> simple(final FriendlyByteBuf.Writer<T> param0, final FriendlyByteBuf.Reader<T> param1) {
      return new EntityDataSerializer.ForValueType<T>() {
         @Override
         public void write(FriendlyByteBuf l_param0, T l_param1) {
            param0.accept(l_param0, l_param1);
         }

         @Override
         public T read(FriendlyByteBuf l_param0) {
            return param1.apply(l_param0);
         }
      };
   }

   static <T> EntityDataSerializer<Optional<T>> optional(FriendlyByteBuf.Writer<T> param0, FriendlyByteBuf.Reader<T> param1) {
      return simple(param0.asOptional(), param1.asOptional());
   }

   static <T extends Enum<T>> EntityDataSerializer<T> simpleEnum(Class<T> param0) {
      return simple(FriendlyByteBuf::writeEnum, l_param1 -> l_param1.readEnum(param0));
   }

   static <T> EntityDataSerializer<T> simpleId(IdMap<T> param0) {
      return simple((l_param1, l_param2) -> l_param1.writeId(param0, (T)l_param2), l_param1 -> l_param1.readById(param0));
   }

   interface ForValueType<T> extends EntityDataSerializer<T> {
      @Override
      default T copy(T param0) {
         return param0;
      }
   }
}
