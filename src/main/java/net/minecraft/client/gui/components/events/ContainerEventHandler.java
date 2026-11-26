package net.minecraft.client.gui.components.events;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2i;

public interface ContainerEventHandler extends GuiEventListener {
   List<? extends GuiEventListener> children();

   default Optional<GuiEventListener> getChildAt(double param0, double param2) {
      for (GuiEventListener var6 : this.children()) {
         if (var6.isMouseOver(param0, param2)) {
            return Optional.of(var6);
         }
      }

      return Optional.empty();
   }

   @Override
   default boolean mouseClicked(double param0, double param2, int param4) {
      for (GuiEventListener var7 : this.children()) {
         if (var7.mouseClicked(param0, param2, param4)) {
            this.setFocused(var7);
            if (param4 == 0) {
               this.setDragging(true);
            }

            return true;
         }
      }

      return false;
   }

   @Override
   default boolean mouseReleased(double param0, double param2, int param4) {
      this.setDragging(false);
      return this.getChildAt(param0, param2).filter(l_param5 -> l_param5.mouseReleased(param0, param2, param4)).isPresent();
   }

   @Override
   default boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      return this.getFocused() != null && this.isDragging() && param4 == 0 ? this.getFocused().mouseDragged(param0, param2, param4, param5, param7) : false;
   }

   boolean isDragging();

   void setDragging(boolean var1);

   @Override
   default boolean mouseScrolled(double param0, double param2, double param4) {
      return this.getChildAt(param0, param2).filter(l_param6 -> l_param6.mouseScrolled(param0, param2, param4)).isPresent();
   }

   @Override
   default boolean keyPressed(int param0, int param1, int param2) {
      return this.getFocused() != null && this.getFocused().keyPressed(param0, param1, param2);
   }

   @Override
   default boolean keyReleased(int param0, int param1, int param2) {
      return this.getFocused() != null && this.getFocused().keyReleased(param0, param1, param2);
   }

   @Override
   default boolean charTyped(char param0, int param1) {
      return this.getFocused() != null && this.getFocused().charTyped(param0, param1);
   }

   @Nullable
   GuiEventListener getFocused();

   void setFocused(@Nullable GuiEventListener var1);

   @Override
   default void setFocused(boolean param0) {
   }

   @Override
   default boolean isFocused() {
      return this.getFocused() != null;
   }

   @Nullable
   @Override
   default ComponentPath getCurrentFocusPath() {
      GuiEventListener var1 = this.getFocused();
      return var1 != null ? ComponentPath.path(this, var1.getCurrentFocusPath()) : null;
   }

   default void magicalSpecialHackyFocus(@Nullable GuiEventListener param0) {
      this.setFocused(param0);
   }

   @Nullable
   @Override
   default ComponentPath nextFocusPath(FocusNavigationEvent param0) {
      GuiEventListener var2 = this.getFocused();
      if (var2 != null) {
         ComponentPath var3 = var2.nextFocusPath(param0);
         if (var3 != null) {
            return ComponentPath.path(this, var3);
         }
      }

      if (param0 instanceof FocusNavigationEvent.TabNavigation var3) {
         return this.handleTabNavigation(var3);
      } else {
         return param0 instanceof FocusNavigationEvent.ArrowNavigation var3 ? this.handleArrowNavigation(var3) : null;
      }
   }

   @Nullable
   private ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation param0) {
      boolean var2 = param0.forward();
      GuiEventListener var3 = this.getFocused();
      List<? extends GuiEventListener> var4 = new ArrayList<>(this.children());
      Collections.sort(var4, Comparator.comparingInt(l_param0 -> l_param0.getTabOrderGroup()));
      int var6 = var4.indexOf(var3);
      int var5;
      if (var3 != null && var6 >= 0) {
         var5 = var6 + (var2 ? 1 : 0);
      } else if (var2) {
         var5 = 0;
      } else {
         var5 = var4.size();
      }

      ListIterator<? extends GuiEventListener> var7 = var4.listIterator(var5);
      BooleanSupplier var8 = var2 ? var7::hasNext : var7::hasPrevious;
      Supplier<? extends GuiEventListener> var9 = var2 ? var7::next : var7::previous;

      while (var8.getAsBoolean()) {
         GuiEventListener var10 = var9.get();
         ComponentPath var11 = var10.nextFocusPath(param0);
         if (var11 != null) {
            return ComponentPath.path(this, var11);
         }
      }

      return null;
   }

   @Nullable
   private ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation param0) {
      GuiEventListener var2 = this.getFocused();
      if (var2 == null) {
         ScreenDirection var3 = param0.direction();
         ScreenRectangle var4 = this.getRectangle().getBorder(var3.getOpposite());
         return ComponentPath.path(this, this.nextFocusPathInDirection(var4, var3, null, param0));
      } else {
         ScreenRectangle var3 = var2.getRectangle();
         return ComponentPath.path(this, this.nextFocusPathInDirection(var3, param0.direction(), var2, param0));
      }
   }

   @Nullable
   private ComponentPath nextFocusPathInDirection(
      ScreenRectangle param0, ScreenDirection param1, @Nullable GuiEventListener param2, FocusNavigationEvent param3
   ) {
      ScreenAxis var5 = param1.getAxis();
      ScreenAxis var6 = var5.orthogonal();
      ScreenDirection var7 = var6.getPositive();
      int var8 = param0.getBoundInDirection(param1.getOpposite());
      List<GuiEventListener> var9 = new ArrayList<>();

      for (GuiEventListener var11 : this.children()) {
         if (var11 != param2) {
            ScreenRectangle var12 = var11.getRectangle();
            if (var12.overlapsInAxis(param0, var6)) {
               int var13 = var12.getBoundInDirection(param1.getOpposite());
               if (param1.isAfter(var13, var8)) {
                  var9.add(var11);
               } else if (var13 == var8 && param1.isAfter(var12.getBoundInDirection(param1), param0.getBoundInDirection(param1))) {
                  var9.add(var11);
               }
            }
         }
      }

      Comparator<GuiEventListener> var10 = Comparator.comparing(
         l_param1 -> l_param1.getRectangle().getBoundInDirection(param1.getOpposite()), param1.coordinateValueComparator()
      );
      Comparator<GuiEventListener> var11 = Comparator.comparing(
         l_param1 -> l_param1.getRectangle().getBoundInDirection(var7.getOpposite()), var7.coordinateValueComparator()
      );
      var9.sort(var10.thenComparing(var11));

      for (GuiEventListener var13 : var9) {
         ComponentPath var14 = var13.nextFocusPath(param3);
         if (var14 != null) {
            return var14;
         }
      }

      return this.nextFocusPathVaguelyInDirection(param0, param1, param2, param3);
   }

   @Nullable
   private ComponentPath nextFocusPathVaguelyInDirection(
      ScreenRectangle param0, ScreenDirection param1, @Nullable GuiEventListener param2, FocusNavigationEvent param3
   ) {
      ScreenAxis var5 = param1.getAxis();
      ScreenAxis var6 = var5.orthogonal();
      List<Pair<GuiEventListener, Long>> var7 = new ArrayList<>();
      ScreenPosition var8 = ScreenPosition.of(var5, param0.getBoundInDirection(param1), param0.getCenterInAxis(var6));

      for (GuiEventListener var10 : this.children()) {
         if (var10 != param2) {
            ScreenRectangle var11 = var10.getRectangle();
            ScreenPosition var12 = ScreenPosition.of(var5, var11.getBoundInDirection(param1.getOpposite()), var11.getCenterInAxis(var6));
            if (param1.isAfter(var12.getCoordinate(var5), var8.getCoordinate(var5))) {
               long var13 = Vector2i.distanceSquared(var8.x(), var8.y(), var12.x(), var12.y());
               var7.add(Pair.of(var10, var13));
            }
         }
      }

      var7.sort(Comparator.comparingDouble(Pair::getSecond));

      for (Pair<GuiEventListener, Long> var10 : var7) {
         ComponentPath var11 = var10.getFirst().nextFocusPath(param3);
         if (var11 != null) {
            return var11;
         }
      }

      return null;
   }
}
