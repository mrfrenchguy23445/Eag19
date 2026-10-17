package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;

public class BookViewScreen extends Screen {
   public static final int PAGE_INDICATOR_TEXT_Y_OFFSET = 16;
   public static final int PAGE_TEXT_X_OFFSET = 36;
   public static final int PAGE_TEXT_Y_OFFSET = 30;
   public static final BookViewScreen.BookAccess EMPTY_ACCESS = new BookViewScreen.BookAccess() {
      @Override
      public int getPageCount() {
         return 0;
      }

      @Override
      public FormattedText getPageRaw(int param0) {
         return FormattedText.EMPTY;
      }
   };
   public static final ResourceLocation BOOK_LOCATION = new ResourceLocation("textures/gui/book.png");
   protected static final int TEXT_WIDTH = 114;
   protected static final int TEXT_HEIGHT = 128;
   protected static final int IMAGE_WIDTH = 192;
   protected static final int IMAGE_HEIGHT = 192;
   private BookViewScreen.BookAccess bookAccess;
   private int currentPage;
   private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
   private int cachedPage = -1;
   private Component pageMsg = CommonComponents.EMPTY;
   private PageButton forwardButton;
   private PageButton backButton;
   private final boolean playTurnSound;

   public BookViewScreen(BookViewScreen.BookAccess param0) {
      this(param0, true);
   }

   public BookViewScreen() {
      this(EMPTY_ACCESS, false);
   }

   private BookViewScreen(BookViewScreen.BookAccess param0, boolean param1) {
      super(GameNarrator.NO_TITLE);
      this.bookAccess = param0;
      this.playTurnSound = param1;
   }

   public void setBookAccess(BookViewScreen.BookAccess param0) {
      this.bookAccess = param0;
      this.currentPage = Mth.clamp(this.currentPage, 0, param0.getPageCount());
      this.updateButtonVisibility();
      this.cachedPage = -1;
   }

   public boolean setPage(int param0) {
      int var2 = Mth.clamp(param0, 0, this.bookAccess.getPageCount() - 1);
      if (var2 != this.currentPage) {
         this.currentPage = var2;
         this.updateButtonVisibility();
         this.cachedPage = -1;
         return true;
      } else {
         return false;
      }
   }

   protected boolean forcePage(int param0) {
      return this.setPage(param0);
   }

   @Override
   protected void init() {
      this.createMenuControls();
      this.createPageControlButtons();
   }

   protected void createMenuControls() {
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.onClose()).bounds(this.width / 2 - 100, 196, 200, 20).build());
   }

   protected void createPageControlButtons() {
      int var1 = (this.width - 192) / 2;
      int var2 = 2;
      this.forwardButton = this.addRenderableWidget(new PageButton(var1 + 116, 159, true, l_param0 -> this.pageForward(), this.playTurnSound));
      this.backButton = this.addRenderableWidget(new PageButton(var1 + 43, 159, false, l_param0 -> this.pageBack(), this.playTurnSound));
      this.updateButtonVisibility();
   }

   private int getNumPages() {
      return this.bookAccess.getPageCount();
   }

   protected void pageBack() {
      if (this.currentPage > 0) {
         this.currentPage--;
      }

      this.updateButtonVisibility();
   }

   protected void pageForward() {
      if (this.currentPage < this.getNumPages() - 1) {
         this.currentPage++;
      }

      this.updateButtonVisibility();
   }

   private void updateButtonVisibility() {
      this.forwardButton.visible = this.currentPage < this.getNumPages() - 1;
      this.backButton.visible = this.currentPage > 0;
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (super.keyPressed(param0, param1, param2)) {
         return true;
      }

      switch (param0) {
         case 266:
            this.backButton.onPress();
            return true;
         case 267:
            this.forwardButton.onPress();
            return true;
         default:
            return false;
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      RenderSystem.setShaderTexture(0, BOOK_LOCATION);
      int var5 = (this.width - 192) / 2;
      int var6 = 2;
      blit(param0, var5, 2, 0, 0, 192, 192);
      if (this.cachedPage != this.currentPage) {
         FormattedText var7 = this.bookAccess.getPage(this.currentPage);
         this.cachedPageComponents = this.font.split(var7, 114);
         this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
      }

      this.cachedPage = this.currentPage;
      int var7 = this.font.width(this.pageMsg);
      this.font.draw(param0, this.pageMsg, var5 - var7 + 192 - 44, 18.0F, 0);
      int var8 = Math.min(128 / 9, this.cachedPageComponents.size());

      for (int var9 = 0; var9 < var8; var9++) {
         FormattedCharSequence var10 = this.cachedPageComponents.get(var9);
         this.font.draw(param0, var10, var5 + 36, 32 + var9 * 9, 0);
      }

      Style var9 = this.getClickedComponentStyleAt(param1, param2);
      if (var9 != null) {
         this.renderComponentHoverEffect(param0, var9, param1, param2);
      }

      super.render(param0, param1, param2, param3);
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (param4 == 0) {
         Style var6 = this.getClickedComponentStyleAt(param0, param2);
         if (var6 != null && this.handleComponentClicked(var6)) {
            return true;
         }
      }

      return super.mouseClicked(param0, param2, param4);
   }

   @Override
   public boolean handleComponentClicked(Style param0) {
      ClickEvent var2 = param0.getClickEvent();
      if (var2 == null) {
         return false;
      }

      if (var2.getAction() == ClickEvent.Action.CHANGE_PAGE) {
         String var3 = var2.getValue();

         try {
            int var4 = Integer.parseInt(var3) - 1;
            return this.forcePage(var4);
         } catch (Exception var5) {
            return false;
         }
      } else {
         boolean var3 = super.handleComponentClicked(param0);
         if (var3 && var2.getAction() == ClickEvent.Action.RUN_COMMAND) {
            this.closeScreen();
         }

         return var3;
      }
   }

   protected void closeScreen() {
      this.minecraft.setScreen(null);
   }

   @Nullable
   public Style getClickedComponentStyleAt(double param0, double param2) {
      if (this.cachedPageComponents.isEmpty()) {
         return null;
      }

      int var5 = Mth.floor(param0 - (this.width - 192) / 2 - 36.0);
      int var6 = Mth.floor(param2 - 2.0 - 30.0);
      if (var5 >= 0 && var6 >= 0) {
         int var7 = Math.min(128 / 9, this.cachedPageComponents.size());
         if (var5 <= 114 && var6 < 9 * var7 + var7) {
            int var8 = var6 / 9;
            if (var8 >= 0 && var8 < this.cachedPageComponents.size()) {
               FormattedCharSequence var9 = this.cachedPageComponents.get(var8);
               return this.minecraft.font.getSplitter().componentStyleAtWidth(var9, var5);
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   static List<String> loadPages(CompoundTag param0) {
      Builder<String> var1 = ImmutableList.builder();
      loadPages(param0, var1::add);
      return var1.build();
   }

   public static void loadPages(CompoundTag param0, Consumer<String> param1) {
      ListTag var3 = param0.getList("pages", 8).copy();
      IntFunction<String> var2;
      if (Minecraft.getInstance().isTextFilteringEnabled() && param0.contains("filtered_pages", 10)) {
         CompoundTag var4 = param0.getCompound("filtered_pages");
         var2 = l_param2 -> {
            String l_var3 = String.valueOf(l_param2);
            return var4.contains(l_var3) ? var4.getString(l_var3) : var3.getString(l_param2);
         };
      } else {
         var2 = var3::getString;
      }

      for (int var4 = 0; var4 < var3.size(); var4++) {
         param1.accept(var2.apply(var4));
      }
   }

   public interface BookAccess {
      int getPageCount();

      FormattedText getPageRaw(int var1);

      default FormattedText getPage(int param0) {
         return param0 >= 0 && param0 < this.getPageCount() ? this.getPageRaw(param0) : FormattedText.EMPTY;
      }

      static BookViewScreen.BookAccess fromItem(ItemStack param0) {
         if (param0.is(Items.WRITTEN_BOOK)) {
            return new BookViewScreen.WrittenBookAccess(param0);
         } else {
            return param0.is(Items.WRITABLE_BOOK) ? new BookViewScreen.WritableBookAccess(param0) : BookViewScreen.EMPTY_ACCESS;
         }
      }
   }

   public static class WritableBookAccess implements BookViewScreen.BookAccess {
      private final List<String> pages;

      public WritableBookAccess(ItemStack param0) {
         this.pages = readPages(param0);
      }

      private static List<String> readPages(ItemStack param0) {
         CompoundTag var1 = param0.getTag();
         return var1 != null ? BookViewScreen.loadPages(var1) : ImmutableList.of();
      }

      @Override
      public int getPageCount() {
         return this.pages.size();
      }

      @Override
      public FormattedText getPageRaw(int param0) {
         return FormattedText.of(this.pages.get(param0));
      }
   }

   public static class WrittenBookAccess implements BookViewScreen.BookAccess {
      private final List<String> pages;

      public WrittenBookAccess(ItemStack param0) {
         this.pages = readPages(param0);
      }

      private static List<String> readPages(ItemStack param0) {
         CompoundTag var1 = param0.getTag();
         return var1 != null && WrittenBookItem.makeSureTagIsValid(var1)
            ? BookViewScreen.loadPages(var1)
            : ImmutableList.of(Component.Serializer.toJson(Component.translatable("book.invalid.tag").withStyle(ChatFormatting.DARK_RED)));
      }

      @Override
      public int getPageCount() {
         return this.pages.size();
      }

      @Override
      public FormattedText getPageRaw(int param0) {
         String var2 = this.pages.get(param0);

         try {
            FormattedText var3 = Component.Serializer.fromJson(var2);
            if (var3 != null) {
               return var3;
            }
         } catch (Exception var4) {
         }

         return FormattedText.of(var2);
      }
   }
}
