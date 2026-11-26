package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;

public interface Layout extends LayoutElement {
   void visitChildren(Consumer<LayoutElement> var1);

   @Override
   default void visitWidgets(Consumer<AbstractWidget> param0) {
      this.visitChildren(l_param1 -> l_param1.visitWidgets(param0));
   }

   default void arrangeElements() {
      this.visitChildren(l_param0 -> {
         if (l_param0 instanceof Layout l_var1) {
            l_var1.arrangeElements();
         }
      });
   }
}
