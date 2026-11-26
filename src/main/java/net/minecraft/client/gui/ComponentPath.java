package net.minecraft.client.gui;

import javax.annotation.Nullable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

public interface ComponentPath {
   static ComponentPath leaf(GuiEventListener param0) {
      return new ComponentPath.Leaf(param0);
   }

   @Nullable
   static ComponentPath path(ContainerEventHandler param0, @Nullable ComponentPath param1) {
      return param1 == null ? null : new ComponentPath.Path(param0, param1);
   }

   static ComponentPath path(GuiEventListener param0, ContainerEventHandler... param1) {
      ComponentPath var2 = leaf(param0);

      for (ContainerEventHandler var6 : param1) {
         var2 = path(var6, var2);
      }

      return var2;
   }

   GuiEventListener component();

   void applyFocus(boolean var1);

   record Leaf(GuiEventListener component) implements ComponentPath {
      @Override
      public void applyFocus(boolean param0) {
         this.component.setFocused(param0);
      }
   }

   record Path(ContainerEventHandler component, ComponentPath childPath) implements ComponentPath {
      @Override
      public void applyFocus(boolean param0) {
         if (!param0) {
            this.component.setFocused(null);
         } else {
            this.component.setFocused(this.childPath.component());
         }

         this.childPath.applyFocus(param0);
      }
   }
}
