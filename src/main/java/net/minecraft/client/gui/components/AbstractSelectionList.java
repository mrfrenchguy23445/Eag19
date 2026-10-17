package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class AbstractSelectionList<E extends AbstractSelectionList.Entry<E>>
   extends AbstractContainerEventHandler
   implements Renderable,
   NarratableEntry {
   protected final Minecraft minecraft;
   protected final int itemHeight;
   private final List<E> children = new AbstractSelectionList.TrackedList();
   protected int width;
   protected int height;
   protected int y0;
   protected int y1;
   protected int x1;
   protected int x0;
   protected boolean centerListVertically = true;
   private double scrollAmount;
   private boolean renderSelection = true;
   private boolean renderHeader;
   protected int headerHeight;
   private boolean scrolling;
   @Nullable
   private E selected;
   private boolean renderBackground = true;
   private boolean renderTopAndBottom = true;
   @Nullable
   private E hovered;

   public AbstractSelectionList(Minecraft param0, int param1, int param2, int param3, int param4, int param5) {
      this.minecraft = param0;
      this.width = param1;
      this.height = param2;
      this.y0 = param3;
      this.y1 = param4;
      this.itemHeight = param5;
      this.x0 = 0;
      this.x1 = param1;
   }

   public void setRenderSelection(boolean param0) {
      this.renderSelection = param0;
   }

   protected void setRenderHeader(boolean param0, int param1) {
      this.renderHeader = param0;
      this.headerHeight = param1;
      if (!param0) {
         this.headerHeight = 0;
      }
   }

   public int getRowWidth() {
      return 220;
   }

   @Nullable
   public E getSelected() {
      return this.selected;
   }

   public void setSelected(@Nullable E param0) {
      this.selected = param0;
   }

   public E getFirstElement() {
      return this.children.get(0);
   }

   public void setRenderBackground(boolean param0) {
      this.renderBackground = param0;
   }

   public void setRenderTopAndBottom(boolean param0) {
      this.renderTopAndBottom = param0;
   }

   @Nullable
   public E getFocused() {
      return (E)super.getFocused();
   }

   @Override
   public final List<E> children() {
      return this.children;
   }

   protected final void clearEntries() {
      this.children.clear();
      this.selected = null;
   }

   protected void replaceEntries(Collection<E> param0) {
      this.clearEntries();
      this.children.addAll(param0);
   }

   protected E getEntry(int param0) {
      return this.children().get(param0);
   }

   protected int addEntry(E param0) {
      this.children.add(param0);
      return this.children.size() - 1;
   }

   protected void addEntryToTop(E param0) {
      double var2 = this.getMaxScroll() - this.getScrollAmount();
      this.children.add(0, param0);
      this.setScrollAmount(this.getMaxScroll() - var2);
   }

   protected boolean removeEntryFromTop(E param0) {
      double var2 = this.getMaxScroll() - this.getScrollAmount();
      boolean var4 = this.removeEntry(param0);
      this.setScrollAmount(this.getMaxScroll() - var2);
      return var4;
   }

   protected int getItemCount() {
      return this.children().size();
   }

   protected boolean isSelectedItem(int param0) {
      return Objects.equals(this.getSelected(), this.children().get(param0));
   }

   @Nullable
   protected final E getEntryAtPosition(double param0, double param2) {
      int var5 = this.getRowWidth() / 2;
      int var6 = this.x0 + this.width / 2;
      int var7 = var6 - var5;
      int var8 = var6 + var5;
      int var9 = Mth.floor(param2 - this.y0) - this.headerHeight + (int)this.getScrollAmount() - 4;
      int var10 = var9 / this.itemHeight;
      return param0 < this.getScrollbarPosition() && param0 >= var7 && param0 <= var8 && var10 >= 0 && var9 >= 0 && var10 < this.getItemCount()
         ? this.children().get(var10)
         : null;
   }

   public void updateSize(int param0, int param1, int param2, int param3) {
      this.width = param0;
      this.height = param1;
      this.y0 = param2;
      this.y1 = param3;
      this.x0 = 0;
      this.x1 = param0;
   }

   public void setLeftPos(int param0) {
      this.x0 = param0;
      this.x1 = param0 + this.width;
   }

   protected int getMaxPosition() {
      return this.getItemCount() * this.itemHeight + this.headerHeight;
   }

   protected void clickedHeader(int param0, int param1) {
   }

   protected void renderHeader(PoseStack param0, int param1, int param2) {
   }

   protected void renderBackground(PoseStack param0) {
   }

   protected void renderDecorations(PoseStack param0, int param1, int param2) {
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      int var5 = this.getScrollbarPosition();
      int var6 = var5 + 6;
      this.hovered = this.isMouseOver(param1, param2) ? this.getEntryAtPosition(param1, param2) : null;
      if (this.renderBackground) {
         RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
         RenderSystem.setShaderColor(0.125F, 0.125F, 0.125F, 1.0F);
         int var7 = 32;
         blit(param0, this.x0, this.y0, this.x1, this.y1 + (int)this.getScrollAmount(), this.x1 - this.x0, this.y1 - this.y0, 32, 32);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      int var7 = this.getRowLeft();
      int var8 = this.y0 + 4 - (int)this.getScrollAmount();
      this.enableScissor();
      if (this.renderHeader) {
         this.renderHeader(param0, var7, var8);
      }

      this.renderList(param0, param1, param2, param3);
      disableScissor();
      if (this.renderTopAndBottom) {
         RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
         int var9 = 32;
         RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
         blit(param0, this.x0, 0, 0.0F, 0.0F, this.width, this.y0, 32, 32);
         blit(param0, this.x0, this.y1, 0.0F, this.y1, this.width, this.height - this.y1, 32, 32);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         int var10 = 4;
         fillGradient(param0, this.x0, this.y0, this.x1, this.y0 + 4, -16777216, 0);
         fillGradient(param0, this.x0, this.y1 - 4, this.x1, this.y1, 0, -16777216);
      }

      int var9 = this.getMaxScroll();
      if (var9 > 0) {
         int var10 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / this.getMaxPosition());
         var10 = Mth.clamp(var10, 32, this.y1 - this.y0 - 8);
         int var11 = (int)this.getScrollAmount() * (this.y1 - this.y0 - var10) / var9 + this.y0;
         if (var11 < this.y0) {
            var11 = this.y0;
         }

         fill(param0, var5, this.y0, var6, this.y1, -16777216);
         fill(param0, var5, var11, var6, var11 + var10, -8355712);
         fill(param0, var5, var11, var6 - 1, var11 + var10 - 1, -4144960);
      }

      this.renderDecorations(param0, param1, param2);
      RenderSystem.disableBlend();
   }

   protected void enableScissor() {
      enableScissor(this.x0, this.y0, this.x1, this.y1);
   }

   protected void centerScrollOn(E param0) {
      this.setScrollAmount(this.children().indexOf(param0) * this.itemHeight + this.itemHeight / 2 - (this.y1 - this.y0) / 2);
   }

   protected void ensureVisible(E param0) {
      int var2 = this.getRowTop(this.children().indexOf(param0));
      int var3 = var2 - this.y0 - 4 - this.itemHeight;
      if (var3 < 0) {
         this.scroll(var3);
      }

      int var4 = this.y1 - var2 - this.itemHeight - this.itemHeight;
      if (var4 < 0) {
         this.scroll(-var4);
      }
   }

   private void scroll(int param0) {
      this.setScrollAmount(this.getScrollAmount() + param0);
   }

   public double getScrollAmount() {
      return this.scrollAmount;
   }

   public void setScrollAmount(double param0) {
      this.scrollAmount = Mth.clamp(param0, 0.0, this.getMaxScroll());
   }

   public int getMaxScroll() {
      return Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
   }

   public int getScrollBottom() {
      return (int)this.getScrollAmount() - this.height - this.headerHeight;
   }

   protected void updateScrollingState(double param0, double param2, int param4) {
      this.scrolling = param4 == 0 && param0 >= this.getScrollbarPosition() && param0 < this.getScrollbarPosition() + 6;
   }

   protected int getScrollbarPosition() {
      return this.width / 2 + 124;
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      this.updateScrollingState(param0, param2, param4);
      if (!this.isMouseOver(param0, param2)) {
         return false;
      }

      E var6 = this.getEntryAtPosition(param0, param2);
      if (var6 != null) {
         if (var6.mouseClicked(param0, param2, param4)) {
            E var7 = this.getFocused();
            if (var7 != var6 && var7 instanceof ContainerEventHandler var8) {
               var8.setFocused(null);
            }

            this.setFocused(var6);
            this.setDragging(true);
            return true;
         }
      } else if (param4 == 0) {
         this.clickedHeader((int)(param0 - (this.x0 + this.width / 2 - this.getRowWidth() / 2)), (int)(param2 - this.y0) + (int)this.getScrollAmount() - 4);
         return true;
      }

      return this.scrolling;
   }

   @Override
   public boolean mouseReleased(double param0, double param2, int param4) {
      if (this.getFocused() != null) {
         this.getFocused().mouseReleased(param0, param2, param4);
      }

      return false;
   }

   @Override
   public boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      if (super.mouseDragged(param0, param2, param4, param5, param7)) {
         return true;
      }

      if (param4 == 0 && this.scrolling) {
         if (param2 < this.y0) {
            this.setScrollAmount(0.0);
         } else if (param2 > this.y1) {
            this.setScrollAmount(this.getMaxScroll());
         } else {
            double var10 = Math.max(1, this.getMaxScroll());
            int var12 = this.y1 - this.y0;
            int var13 = Mth.clamp((int)((float)(var12 * var12) / this.getMaxPosition()), 32, var12 - 8);
            double var14 = Math.max(1.0, var10 / (var12 - var13));
            this.setScrollAmount(this.getScrollAmount() + param7 * var14);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double param0, double param2, double param4) {
      this.setScrollAmount(this.getScrollAmount() - param4 * this.itemHeight / 2.0);
      return true;
   }

   @Override
   public void setFocused(@Nullable GuiEventListener param0) {
      super.setFocused(param0);
      int var2 = this.children.indexOf(param0);
      if (var2 >= 0) {
         E var3 = this.children.get(var2);
         this.setSelected(var3);
         if (this.minecraft.getLastInputType().isKeyboard()) {
            this.ensureVisible(var3);
         }
      }
   }

   @Nullable
   protected E nextEntry(ScreenDirection param0) {
      return this.nextEntry(param0, l_param0 -> true);
   }

   @Nullable
   protected E nextEntry(ScreenDirection param0, Predicate<E> param1) {
      return this.nextEntry(param0, param1, this.getSelected());
   }

   @Nullable
   protected E nextEntry(ScreenDirection param0, Predicate<E> param1, @Nullable E param2) {
      int var4 = switch (param0) {
         case RIGHT, LEFT -> 0;
         case UP -> -1;
         case DOWN -> 1;
      };
      if (!this.children().isEmpty() && var4 != 0) {
         int var5;
         if (param2 == null) {
            var5 = var4 > 0 ? 0 : this.children().size() - 1;
         } else {
            var5 = this.children().indexOf(param2) + var4;
         }

         for (int var6 = var5; var6 >= 0 && var6 < this.children.size(); var6 += var4) {
            E var7 = this.children().get(var6);
            if (param1.test(var7)) {
               return var7;
            }
         }
      }

      return null;
   }

   @Override
   public boolean isMouseOver(double param0, double param2) {
      return param2 >= this.y0 && param2 <= this.y1 && param0 >= this.x0 && param0 <= this.x1;
   }

   protected void renderList(PoseStack param0, int param1, int param2, float param3) {
      int var5 = this.getRowLeft();
      int var6 = this.getRowWidth();
      int var7 = this.itemHeight - 4;
      int var8 = this.getItemCount();

      for (int var9 = 0; var9 < var8; var9++) {
         int var10 = this.getRowTop(var9);
         int var11 = this.getRowBottom(var9);
         if (var11 >= this.y0 && var10 <= this.y1) {
            this.renderItem(param0, param1, param2, param3, var9, var5, var10, var6, var7);
         }
      }
   }

   protected void renderItem(PoseStack param0, int param1, int param2, float param3, int param4, int param5, int param6, int param7, int param8) {
      E var10 = this.getEntry(param4);
      var10.renderBack(param0, param4, param6, param5, param7, param8, param1, param2, Objects.equals(this.hovered, var10), param3);
      if (this.renderSelection && this.isSelectedItem(param4)) {
         int var11 = this.isFocused() ? -1 : -8355712;
         this.renderSelection(param0, param6, param7, param8, var11, -16777216);
      }

      var10.render(param0, param4, param6, param5, param7, param8, param1, param2, Objects.equals(this.hovered, var10), param3);
   }

   protected void renderSelection(PoseStack param0, int param1, int param2, int param3, int param4, int param5) {
      int var7 = this.x0 + (this.width - param2) / 2;
      int var8 = this.x0 + (this.width + param2) / 2;
      fill(param0, var7, param1 - 2, var8, param1 + param3 + 2, param4);
      fill(param0, var7 + 1, param1 - 1, var8 - 1, param1 + param3 + 1, param5);
   }

   public int getRowLeft() {
      return this.x0 + this.width / 2 - this.getRowWidth() / 2 + 2;
   }

   public int getRowRight() {
      return this.getRowLeft() + this.getRowWidth();
   }

   protected int getRowTop(int param0) {
      return this.y0 + 4 - (int)this.getScrollAmount() + param0 * this.itemHeight + this.headerHeight;
   }

   protected int getRowBottom(int param0) {
      return this.getRowTop(param0) + this.itemHeight;
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      if (this.isFocused()) {
         return NarratableEntry.NarrationPriority.FOCUSED;
      } else {
         return this.hovered != null ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
      }
   }

   @Nullable
   protected E remove(int param0) {
      E var2 = this.children.get(param0);
      return this.removeEntry(this.children.get(param0)) ? var2 : null;
   }

   protected boolean removeEntry(E param0) {
      boolean var2 = this.children.remove(param0);
      if (var2 && param0 == this.getSelected()) {
         this.setSelected(null);
      }

      return var2;
   }

   @Nullable
   protected E getHovered() {
      return this.hovered;
   }

   void bindEntryToSelf(AbstractSelectionList.Entry<E> param0) {
      param0.list = this;
   }

   protected void narrateListElementPosition(NarrationElementOutput param0, E param1) {
      List<E> var3 = this.children();
      if (var3.size() > 1) {
         int var4 = var3.indexOf(param1);
         if (var4 != -1) {
            param0.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", var4 + 1, var3.size()));
         }
      }
   }

   @Override
   public ScreenRectangle getRectangle() {
      return new ScreenRectangle(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0);
   }

   protected abstract static class Entry<E extends AbstractSelectionList.Entry<E>> implements GuiEventListener {
      @Deprecated
      AbstractSelectionList<E> list;

      @Override
      public void setFocused(boolean param0) {
      }

      @Override
      public boolean isFocused() {
         return this.list.getFocused() == this;
      }

      public abstract void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10);

      public void renderBack(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
      }

      @Override
      public boolean isMouseOver(double param0, double param2) {
         return Objects.equals(this.list.getEntryAtPosition(param0, param2), this);
      }
   }

   class TrackedList extends AbstractList<E> {
      private final List<E> delegate = Lists.newArrayList();

      public E get(int param0) {
         return this.delegate.get(param0);
      }

      @Override
      public int size() {
         return this.delegate.size();
      }

      public E set(int param0, E param1) {
         E var3 = this.delegate.set(param0, param1);
         AbstractSelectionList.this.bindEntryToSelf(param1);
         return var3;
      }

      public void add(int param0, E param1) {
         this.delegate.add(param0, param1);
         AbstractSelectionList.this.bindEntryToSelf(param1);
      }

      public E remove(int param0) {
         return this.delegate.remove(param0);
      }
   }
}
