package net.minecraft.client.gui.narration;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;

public class NarrationThunk<T> {
   private final T contents;
   private final BiConsumer<Consumer<String>, T> converter;
   public static final NarrationThunk<?> EMPTY = new NarrationThunk<>(Unit.INSTANCE, (param0, param1) -> {});

   private NarrationThunk(T param0, BiConsumer<Consumer<String>, T> param1) {
      this.contents = param0;
      this.converter = param1;
   }

   public static NarrationThunk<?> from(String param0) {
      return new NarrationThunk<>(param0, Consumer::accept);
   }

   public static NarrationThunk<?> from(Component param0) {
      return new NarrationThunk<>(param0, (l_param0, l_param1) -> l_param0.accept(l_param1.getString()));
   }

   public static NarrationThunk<?> from(List<Component> param0) {
      return new NarrationThunk<>(param0, (l_param1, l_param2) -> param0.stream().map(Component::getString).forEach(l_param1));
   }

   public void getText(Consumer<String> param0) {
      this.converter.accept(param0, this.contents);
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return !(param0 instanceof NarrationThunk<?> var2) ? false : var2.converter == this.converter && var2.contents.equals(this.contents);
      }
   }

   @Override
   public int hashCode() {
      int var1 = this.contents.hashCode();
      return 31 * var1 + this.converter.hashCode();
   }
}
