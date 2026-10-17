package net.minecraft.client.gui.components;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class ContainerObjectSelectionList<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
   public ContainerObjectSelectionList(Minecraft param0, int param1, int param2, int param3, int param4, int param5) {
      super(param0, param1, param2, param3, param4, param5);
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent param0) {
      if (this.getItemCount() == 0) {
         return null;
      } else if (!(param0 instanceof FocusNavigationEvent.ArrowNavigation var2)) {
         return super.nextFocusPath(param0);
      } else {
         E var3 = this.getFocused();
         if (var2.direction().getAxis() == ScreenAxis.HORIZONTAL && var3 != null) {
            return ComponentPath.path(this, var3.nextFocusPath(param0));
         }

         int var4 = -1;
         ScreenDirection var5 = var2.direction();
         if (var3 != null) {
            var4 = var3.children().indexOf(var3.getFocused());
         }

         if (var4 == -1) {
            switch (var5) {
               case LEFT:
                  var4 = Integer.MAX_VALUE;
                  var5 = ScreenDirection.DOWN;
                  break;
               case RIGHT:
                  var4 = 0;
                  var5 = ScreenDirection.DOWN;
                  break;
               default:
                  var4 = 0;
            }
         }

         E var6 = var3;

         ComponentPath var7;
         do {
            var6 = this.nextEntry(var5, l_param0 -> !l_param0.children().isEmpty(), var6);
            if (var6 == null) {
               return null;
            }

            var7 = var6.focusPathAtIndex(var2, var4);
         } while (var7 == null);

         return ComponentPath.path(this, var7);
      }
   }

   @Override
   public void setFocused(@Nullable GuiEventListener param0) {
      super.setFocused(param0);
      if (param0 == null) {
         this.setSelected(null);
      }
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      return this.isFocused() ? NarratableEntry.NarrationPriority.FOCUSED : super.narrationPriority();
   }

   @Override
   protected boolean isSelectedItem(int param0) {
      return false;
   }

   @Override
   public void updateNarration(NarrationElementOutput param0) {
      E var2 = this.getHovered();
      if (var2 != null) {
         var2.updateNarration(param0.nest());
         this.narrateListElementPosition(param0, var2);
      } else {
         E var3 = this.getFocused();
         if (var3 != null) {
            var3.updateNarration(param0.nest());
            this.narrateListElementPosition(param0, var3);
         }
      }

      param0.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
   }

   public abstract static class Entry<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements ContainerEventHandler {
      @Nullable
      private GuiEventListener focused;
      @Nullable
      private NarratableEntry lastNarratable;
      private boolean dragging;

      @Override
      public boolean isDragging() {
         return this.dragging;
      }

      @Override
      public void setDragging(boolean param0) {
         this.dragging = param0;
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         return ContainerEventHandler.super.mouseClicked(param0, param2, param4);
      }

      @Override
      public void setFocused(@Nullable GuiEventListener param0) {
         if (this.focused != null) {
            this.focused.setFocused(false);
         }

         if (param0 != null) {
            param0.setFocused(true);
         }

         this.focused = param0;
      }

      @Nullable
      @Override
      public GuiEventListener getFocused() {
         return this.focused;
      }

      @Nullable
      public ComponentPath focusPathAtIndex(FocusNavigationEvent param0, int param1) {
         if (this.children().isEmpty()) {
            return null;
         }

         ComponentPath var3 = this.children().get(Math.min(param1, this.children().size() - 1)).nextFocusPath(param0);
         return ComponentPath.path(this, var3);
      }

      @Nullable
      @Override
      public ComponentPath nextFocusPath(FocusNavigationEvent param0) {
         if (param0 instanceof FocusNavigationEvent.ArrowNavigation var2) {
            int var3 = switch (var2.direction()) {
               case LEFT -> -1;
               case RIGHT -> 1;
               case UP, DOWN -> 0;
            };
            if (var3 == 0) {
               return null;
            }

            int var4 = Mth.clamp(var3 + this.children().indexOf(this.getFocused()), 0, this.children().size() - 1);

            for (int var5 = var4; var5 >= 0 && var5 < this.children().size(); var5 += var3) {
               GuiEventListener var6 = this.children().get(var5);
               ComponentPath var7 = var6.nextFocusPath(param0);
               if (var7 != null) {
                  return ComponentPath.path(this, var7);
               }
            }
         }

         return ContainerEventHandler.super.nextFocusPath(param0);
      }

      public abstract List<? extends NarratableEntry> narratables();

      void updateNarration(NarrationElementOutput param0) {
         List<? extends NarratableEntry> var2 = this.narratables();
         Screen.NarratableSearchResult var3 = Screen.findNarratableWidget(var2, this.lastNarratable);
         if (var3 != null) {
            if (var3.priority.isTerminal()) {
               this.lastNarratable = var3.entry;
            }

            if (var2.size() > 1) {
               param0.add(NarratedElementType.POSITION, Component.translatable("narrator.position.object_list", var3.index + 1, var2.size()));
               if (var3.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                  param0.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
               }
            }

            var3.entry.updateNarration(param0.nest());
         }
      }
   }
}
