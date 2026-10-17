package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;

public abstract class ObjectSelectionList<E extends ObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
   private static final Component USAGE_NARRATION = Component.translatable("narration.selection.usage");

   public ObjectSelectionList(Minecraft param0, int param1, int param2, int param3, int param4, int param5) {
      super(param0, param1, param2, param3, param4, param5);
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent param0) {
      if (this.getItemCount() == 0) {
         return null;
      } else if (this.isFocused() && param0 instanceof FocusNavigationEvent.ArrowNavigation var2) {
         E var3 = this.nextEntry(var2.direction());
         return var3 != null ? ComponentPath.path(this, ComponentPath.leaf(var3)) : null;
      } else if (!this.isFocused()) {
         E var2 = this.getSelected();
         if (var2 == null) {
            var2 = this.nextEntry(param0.getVerticalDirectionForInitialFocus());
         }

         return var2 == null ? null : ComponentPath.path(this, ComponentPath.leaf(var2));
      } else {
         return null;
      }
   }

   @Override
   public void updateNarration(NarrationElementOutput param0) {
      E var2 = this.getHovered();
      if (var2 != null) {
         this.narrateListElementPosition(param0.nest(), var2);
         var2.updateNarration(param0);
      } else {
         E var3 = this.getSelected();
         if (var3 != null) {
            this.narrateListElementPosition(param0.nest(), var3);
            var3.updateNarration(param0);
         }
      }

      if (this.isFocused()) {
         param0.add(NarratedElementType.USAGE, USAGE_NARRATION);
      }
   }

   public abstract static class Entry<E extends ObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements NarrationSupplier {
      public abstract Component getNarration();

      @Override
      public void updateNarration(NarrationElementOutput param0) {
         param0.add(NarratedElementType.TITLE, this.getNarration());
      }
   }
}
