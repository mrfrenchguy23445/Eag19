package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

public class BookEditScreen extends Screen {
   private static final int TEXT_WIDTH = 114;
   private static final int TEXT_HEIGHT = 128;
   private static final int IMAGE_WIDTH = 192;
   private static final int IMAGE_HEIGHT = 192;
   private static final Component EDIT_TITLE_LABEL = Component.translatable("book.editTitle");
   private static final Component FINALIZE_WARNING_LABEL = Component.translatable("book.finalizeWarning");
   private static final FormattedCharSequence BLACK_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.BLACK));
   private static final FormattedCharSequence GRAY_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.GRAY));
   private final Player owner;
   private final ItemStack book;
   private boolean isModified;
   private boolean isSigning;
   private int frameTick;
   private int currentPage;
   private final List<String> pages = Lists.newArrayList();
   private String title = "";
   private final TextFieldHelper pageEdit = new TextFieldHelper(
      this::getCurrentPageText,
      this::setCurrentPageText,
      this::getClipboard,
      this::setClipboard,
      l_param0 -> l_param0.length() < 1024 && this.font.wordWrapHeight(l_param0, 114) <= 128
   );
   private final TextFieldHelper titleEdit = new TextFieldHelper(
      () -> this.title, l_param0 -> this.title = l_param0, this::getClipboard, this::setClipboard, l_param0 -> l_param0.length() < 16
   );
   private long lastClickTime;
   private int lastIndex = -1;
   private PageButton forwardButton;
   private PageButton backButton;
   private Button doneButton;
   private Button signButton;
   private Button finalizeButton;
   private Button cancelButton;
   private final InteractionHand hand;
   @Nullable
   private BookEditScreen.DisplayCache displayCache = BookEditScreen.DisplayCache.EMPTY;
   private Component pageMsg = CommonComponents.EMPTY;
   private final Component ownerText;

   public BookEditScreen(Player param0, ItemStack param1, InteractionHand param2) {
      super(GameNarrator.NO_TITLE);
      this.owner = param0;
      this.book = param1;
      this.hand = param2;
      CompoundTag var4 = param1.getTag();
      if (var4 != null) {
         BookViewScreen.loadPages(var4, this.pages::add);
      }

      if (this.pages.isEmpty()) {
         this.pages.add("");
      }

      this.ownerText = Component.translatable("book.byAuthor", param0.getName()).withStyle(ChatFormatting.DARK_GRAY);
   }

   private void setClipboard(String param0) {
      if (this.minecraft != null) {
         TextFieldHelper.setClipboardContents(this.minecraft, param0);
      }
   }

   private String getClipboard() {
      return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
   }

   private int getNumPages() {
      return this.pages.size();
   }

   @Override
   public void tick() {
      super.tick();
      this.frameTick++;
   }

   @Override
   protected void init() {
      this.clearDisplayCache();
      this.signButton = this.addRenderableWidget(Button.builder(Component.translatable("book.signButton"), l_param0 -> {
         this.isSigning = true;
         this.updateButtonVisibility();
      }).bounds(this.width / 2 - 100, 196, 98, 20).build());
      this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, l_param0 -> {
         this.minecraft.setScreen(null);
         this.saveChanges(false);
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
      this.finalizeButton = this.addRenderableWidget(Button.builder(Component.translatable("book.finalizeButton"), l_param0 -> {
         if (this.isSigning) {
            this.saveChanges(true);
            this.minecraft.setScreen(null);
         }
      }).bounds(this.width / 2 - 100, 196, 98, 20).build());
      this.cancelButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> {
         if (this.isSigning) {
            this.isSigning = false;
         }

         this.updateButtonVisibility();
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
      int var1 = (this.width - 192) / 2;
      int var2 = 2;
      this.forwardButton = this.addRenderableWidget(new PageButton(var1 + 116, 159, true, l_param0 -> this.pageForward(), true));
      this.backButton = this.addRenderableWidget(new PageButton(var1 + 43, 159, false, l_param0 -> this.pageBack(), true));
      this.updateButtonVisibility();
   }

   private void pageBack() {
      if (this.currentPage > 0) {
         this.currentPage--;
      }

      this.updateButtonVisibility();
      this.clearDisplayCacheAfterPageChange();
   }

   private void pageForward() {
      if (this.currentPage < this.getNumPages() - 1) {
         this.currentPage++;
      } else {
         this.appendPageToBook();
         if (this.currentPage < this.getNumPages() - 1) {
            this.currentPage++;
         }
      }

      this.updateButtonVisibility();
      this.clearDisplayCacheAfterPageChange();
   }

   private void updateButtonVisibility() {
      this.backButton.visible = !this.isSigning && this.currentPage > 0;
      this.forwardButton.visible = !this.isSigning;
      this.doneButton.visible = !this.isSigning;
      this.signButton.visible = !this.isSigning;
      this.cancelButton.visible = this.isSigning;
      this.finalizeButton.visible = this.isSigning;
      this.finalizeButton.active = !this.title.trim().isEmpty();
   }

   private void eraseEmptyTrailingPages() {
      ListIterator<String> var1 = this.pages.listIterator(this.pages.size());

      while (var1.hasPrevious() && var1.previous().isEmpty()) {
         var1.remove();
      }
   }

   private void saveChanges(boolean param0) {
      if (this.isModified) {
         this.eraseEmptyTrailingPages();
         this.updateLocalCopy(param0);
         int var2 = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().selected : 40;
         this.minecraft.getConnection().send(new ServerboundEditBookPacket(var2, this.pages, param0 ? Optional.of(this.title.trim()) : Optional.empty()));
      }
   }

   private void updateLocalCopy(boolean param0) {
      ListTag var2 = new ListTag();
      this.pages.stream().map(StringTag::valueOf).forEach(var2::add);
      if (!this.pages.isEmpty()) {
         this.book.addTagElement("pages", var2);
      }

      if (param0) {
         this.book.addTagElement("author", StringTag.valueOf(this.owner.getGameProfile().getName()));
         this.book.addTagElement("title", StringTag.valueOf(this.title.trim()));
      }
   }

   private void appendPageToBook() {
      if (this.getNumPages() < 100) {
         this.pages.add("");
         this.isModified = true;
      }
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (super.keyPressed(param0, param1, param2)) {
         return true;
      } else if (this.isSigning) {
         return this.titleKeyPressed(param0, param1, param2);
      } else {
         boolean var4 = this.bookKeyPressed(param0, param1, param2);
         if (var4) {
            this.clearDisplayCache();
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean charTyped(char param0, int param1) {
      if (super.charTyped(param0, param1)) {
         return true;
      }

      if (this.isSigning) {
         boolean var3 = this.titleEdit.charTyped(param0);
         if (var3) {
            this.updateButtonVisibility();
            this.isModified = true;
            return true;
         } else {
            return false;
         }
      } else if (SharedConstants.isAllowedChatCharacter(param0)) {
         this.pageEdit.insertText(Character.toString(param0));
         this.clearDisplayCache();
         return true;
      } else {
         return false;
      }
   }

   private boolean bookKeyPressed(int param0, int param1, int param2) {
      if (Screen.isSelectAll(param0)) {
         this.pageEdit.selectAll();
         return true;
      }

      if (Screen.isCopy(param0)) {
         this.pageEdit.copy();
         return true;
      }

      if (Screen.isPaste(param0)) {
         this.pageEdit.paste();
         return true;
      }

      if (Screen.isCut(param0)) {
         this.pageEdit.cut();
         return true;
      }

      TextFieldHelper.CursorStep var4 = Screen.hasControlDown() ? TextFieldHelper.CursorStep.WORD : TextFieldHelper.CursorStep.CHARACTER;
      switch (param0) {
         case 257:
         case 335:
            this.pageEdit.insertText("\n");
            return true;
         case 259:
            this.pageEdit.removeFromCursor(-1, var4);
            return true;
         case 261:
            this.pageEdit.removeFromCursor(1, var4);
            return true;
         case 262:
            this.pageEdit.moveBy(1, Screen.hasShiftDown(), var4);
            return true;
         case 263:
            this.pageEdit.moveBy(-1, Screen.hasShiftDown(), var4);
            return true;
         case 264:
            this.keyDown();
            return true;
         case 265:
            this.keyUp();
            return true;
         case 266:
            this.backButton.onPress();
            return true;
         case 267:
            this.forwardButton.onPress();
            return true;
         case 268:
            this.keyHome();
            return true;
         case 269:
            this.keyEnd();
            return true;
         default:
            return false;
      }
   }

   private void keyUp() {
      this.changeLine(-1);
   }

   private void keyDown() {
      this.changeLine(1);
   }

   private void changeLine(int param0) {
      int var2 = this.pageEdit.getCursorPos();
      int var3 = this.getDisplayCache().changeLine(var2, param0);
      this.pageEdit.setCursorPos(var3, Screen.hasShiftDown());
   }

   private void keyHome() {
      if (Screen.hasControlDown()) {
         this.pageEdit.setCursorToStart(Screen.hasShiftDown());
      } else {
         int var1 = this.pageEdit.getCursorPos();
         int var2 = this.getDisplayCache().findLineStart(var1);
         this.pageEdit.setCursorPos(var2, Screen.hasShiftDown());
      }
   }

   private void keyEnd() {
      if (Screen.hasControlDown()) {
         this.pageEdit.setCursorToEnd(Screen.hasShiftDown());
      } else {
         BookEditScreen.DisplayCache var1 = this.getDisplayCache();
         int var2 = this.pageEdit.getCursorPos();
         int var3 = var1.findLineEnd(var2);
         this.pageEdit.setCursorPos(var3, Screen.hasShiftDown());
      }
   }

   private boolean titleKeyPressed(int param0, int param1, int param2) {
      switch (param0) {
         case 257:
         case 335:
            if (!this.title.isEmpty()) {
               this.saveChanges(true);
               this.minecraft.setScreen(null);
            }

            return true;
         case 259:
            this.titleEdit.removeCharsFromCursor(-1);
            this.updateButtonVisibility();
            this.isModified = true;
            return true;
         default:
            return false;
      }
   }

   private String getCurrentPageText() {
      return this.currentPage >= 0 && this.currentPage < this.pages.size() ? this.pages.get(this.currentPage) : "";
   }

   private void setCurrentPageText(String param0) {
      if (this.currentPage >= 0 && this.currentPage < this.pages.size()) {
         this.pages.set(this.currentPage, param0);
         this.isModified = true;
         this.clearDisplayCache();
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      this.setFocused(null);
      RenderSystem.setShaderTexture(0, BookViewScreen.BOOK_LOCATION);
      int var5 = (this.width - 192) / 2;
      int var6 = 2;
      blit(param0, var5, 2, 0, 0, 192, 192);
      if (this.isSigning) {
         boolean var7 = this.frameTick / 6 % 2 == 0;
         FormattedCharSequence var8 = FormattedCharSequence.composite(FormattedCharSequence.forward(this.title, Style.EMPTY), var7 ? BLACK_CURSOR : GRAY_CURSOR);
         int var9 = this.font.width(EDIT_TITLE_LABEL);
         this.font.draw(param0, EDIT_TITLE_LABEL, var5 + 36 + (114 - var9) / 2, 34.0F, 0);
         int var10 = this.font.width(var8);
         this.font.draw(param0, var8, var5 + 36 + (114 - var10) / 2, 50.0F, 0);
         int var11 = this.font.width(this.ownerText);
         this.font.draw(param0, this.ownerText, var5 + 36 + (114 - var11) / 2, 60.0F, 0);
         this.font.drawWordWrap(param0, FINALIZE_WARNING_LABEL, var5 + 36, 82, 114, 0);
      } else {
         int var7 = this.font.width(this.pageMsg);
         this.font.draw(param0, this.pageMsg, var5 - var7 + 192 - 44, 18.0F, 0);
         BookEditScreen.DisplayCache var8 = this.getDisplayCache();

         for (BookEditScreen.LineInfo var12 : var8.lines) {
            this.font.draw(param0, var12.asComponent, var12.x, var12.y, -16777216);
         }

         this.renderHighlight(param0, var8.selection);
         this.renderCursor(param0, var8.cursor, var8.cursorAtEnd);
      }

      super.render(param0, param1, param2, param3);
   }

   private void renderCursor(PoseStack param0, BookEditScreen.Pos2i param1, boolean param2) {
      if (this.frameTick / 6 % 2 == 0) {
         param1 = this.convertLocalToScreen(param1);
         if (!param2) {
            GuiComponent.fill(param0, param1.x, param1.y - 1, param1.x + 1, param1.y + 9, -16777216);
         } else {
            this.font.draw(param0, "_", param1.x, param1.y, 0);
         }
      }
   }

   private void renderHighlight(PoseStack param0, Rect2i[] param1) {
      RenderSystem.enableColorLogicOp();
      RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);

      for (Rect2i var6 : param1) {
         int var7 = var6.getX();
         int var8 = var6.getY();
         int var9 = var7 + var6.getWidth();
         int var10 = var8 + var6.getHeight();
         fill(param0, var7, var8, var9, var10, -16776961);
      }

      RenderSystem.disableColorLogicOp();
   }

   private BookEditScreen.Pos2i convertScreenToLocal(BookEditScreen.Pos2i param0) {
      return new BookEditScreen.Pos2i(param0.x - (this.width - 192) / 2 - 36, param0.y - 32);
   }

   private BookEditScreen.Pos2i convertLocalToScreen(BookEditScreen.Pos2i param0) {
      return new BookEditScreen.Pos2i(param0.x + (this.width - 192) / 2 + 36, param0.y + 32);
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (super.mouseClicked(param0, param2, param4)) {
         return true;
      }

      if (param4 == 0) {
         long var6 = Util.getMillis();
         BookEditScreen.DisplayCache var8 = this.getDisplayCache();
         int var9 = var8.getIndexAtPosition(this.font, this.convertScreenToLocal(new BookEditScreen.Pos2i((int)param0, (int)param2)));
         if (var9 >= 0) {
            if (var9 != this.lastIndex || var6 - this.lastClickTime >= 250L) {
               this.pageEdit.setCursorPos(var9, Screen.hasShiftDown());
            } else if (!this.pageEdit.isSelecting()) {
               this.selectWord(var9);
            } else {
               this.pageEdit.selectAll();
            }

            this.clearDisplayCache();
         }

         this.lastIndex = var9;
         this.lastClickTime = var6;
      }

      return true;
   }

   private void selectWord(int param0) {
      String var2 = this.getCurrentPageText();
      this.pageEdit.setSelectionRange(StringSplitter.getWordPosition(var2, -1, param0, false), StringSplitter.getWordPosition(var2, 1, param0, false));
   }

   @Override
   public boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      if (super.mouseDragged(param0, param2, param4, param5, param7)) {
         return true;
      }

      if (param4 == 0) {
         BookEditScreen.DisplayCache var10 = this.getDisplayCache();
         int var11 = var10.getIndexAtPosition(this.font, this.convertScreenToLocal(new BookEditScreen.Pos2i((int)param0, (int)param2)));
         this.pageEdit.setCursorPos(var11, true);
         this.clearDisplayCache();
      }

      return true;
   }

   private BookEditScreen.DisplayCache getDisplayCache() {
      if (this.displayCache == null) {
         this.displayCache = this.rebuildDisplayCache();
         this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, this.getNumPages());
      }

      return this.displayCache;
   }

   private void clearDisplayCache() {
      this.displayCache = null;
   }

   private void clearDisplayCacheAfterPageChange() {
      this.pageEdit.setCursorToEnd();
      this.clearDisplayCache();
   }

   private BookEditScreen.DisplayCache rebuildDisplayCache() {
      String var1 = this.getCurrentPageText();
      if (var1.isEmpty()) {
         return BookEditScreen.DisplayCache.EMPTY;
      }

      int var2 = this.pageEdit.getCursorPos();
      int var3 = this.pageEdit.getSelectionPos();
      IntList var4 = new IntArrayList();
      List<BookEditScreen.LineInfo> var5 = Lists.newArrayList();
      MutableInt var6 = new MutableInt();
      MutableBoolean var7 = new MutableBoolean();
      StringSplitter var8 = this.font.getSplitter();
      var8.splitLines(var1, 114, Style.EMPTY, true, (l_param5, l_param6, l_param7) -> {
         int l_var9 = var6.getAndIncrement();
         String l_var10 = var1.substring(l_param6, l_param7);
         var7.setValue(l_var10.endsWith("\n"));
         String l_var11 = StringUtils.stripEnd(l_var10, " \n");
         int l_var12 = l_var9 * 9;
         BookEditScreen.Pos2i l_var13 = this.convertLocalToScreen(new BookEditScreen.Pos2i(0, l_var12));
         var4.add(l_param6);
         var5.add(new BookEditScreen.LineInfo(l_param5, l_var11, l_var13.x, l_var13.y));
      });
      int[] var9 = var4.toIntArray();
      boolean var10 = var2 == var1.length();
      BookEditScreen.Pos2i var11;
      if (var10 && var7.isTrue()) {
         var11 = new BookEditScreen.Pos2i(0, var5.size() * 9);
      } else {
         int var12 = findLineFromPos(var9, var2);
         int var13 = this.font.width(var1.substring(var9[var12], var2));
         var11 = new BookEditScreen.Pos2i(var13, var12 * 9);
      }

      List<Rect2i> var12 = Lists.newArrayList();
      if (var2 != var3) {
         int var13 = Math.min(var2, var3);
         int var14 = Math.max(var2, var3);
         int var15 = findLineFromPos(var9, var13);
         int var16 = findLineFromPos(var9, var14);
         if (var15 == var16) {
            int var17 = var15 * 9;
            int var18 = var9[var15];
            var12.add(this.createPartialLineSelection(var1, var8, var13, var14, var17, var18));
         } else {
            int var17 = var15 + 1 > var9.length ? var1.length() : var9[var15 + 1];
            var12.add(this.createPartialLineSelection(var1, var8, var13, var17, var15 * 9, var9[var15]));

            for (int var18 = var15 + 1; var18 < var16; var18++) {
               int var19 = var18 * 9;
               String var20 = var1.substring(var9[var18], var9[var18 + 1]);
               int var21 = (int)var8.stringWidth(var20);
               var12.add(this.createSelection(new BookEditScreen.Pos2i(0, var19), new BookEditScreen.Pos2i(var21, var19 + 9)));
            }

            var12.add(this.createPartialLineSelection(var1, var8, var9[var16], var14, var16 * 9, var9[var16]));
         }
      }

      return new BookEditScreen.DisplayCache(var1, var11, var10, var9, var5.toArray(new BookEditScreen.LineInfo[0]), var12.toArray(new Rect2i[0]));
   }

   static int findLineFromPos(int[] param0, int param1) {
      int var2 = Arrays.binarySearch(param0, param1);
      return var2 < 0 ? -(var2 + 2) : var2;
   }

   private Rect2i createPartialLineSelection(String param0, StringSplitter param1, int param2, int param3, int param4, int param5) {
      String var7 = param0.substring(param5, param2);
      String var8 = param0.substring(param5, param3);
      BookEditScreen.Pos2i var9 = new BookEditScreen.Pos2i((int)param1.stringWidth(var7), param4);
      BookEditScreen.Pos2i var10 = new BookEditScreen.Pos2i((int)param1.stringWidth(var8), param4 + 9);
      return this.createSelection(var9, var10);
   }

   private Rect2i createSelection(BookEditScreen.Pos2i param0, BookEditScreen.Pos2i param1) {
      BookEditScreen.Pos2i var3 = this.convertLocalToScreen(param0);
      BookEditScreen.Pos2i var4 = this.convertLocalToScreen(param1);
      int var5 = Math.min(var3.x, var4.x);
      int var6 = Math.max(var3.x, var4.x);
      int var7 = Math.min(var3.y, var4.y);
      int var8 = Math.max(var3.y, var4.y);
      return new Rect2i(var5, var7, var6 - var5, var8 - var7);
   }

   static class DisplayCache {
      static final BookEditScreen.DisplayCache EMPTY = new BookEditScreen.DisplayCache(
         "",
         new BookEditScreen.Pos2i(0, 0),
         true,
         new int[]{0},
         new BookEditScreen.LineInfo[]{new BookEditScreen.LineInfo(Style.EMPTY, "", 0, 0)},
         new Rect2i[0]
      );
      private final String fullText;
      final BookEditScreen.Pos2i cursor;
      final boolean cursorAtEnd;
      private final int[] lineStarts;
      final BookEditScreen.LineInfo[] lines;
      final Rect2i[] selection;

      public DisplayCache(String param0, BookEditScreen.Pos2i param1, boolean param2, int[] param3, BookEditScreen.LineInfo[] param4, Rect2i[] param5) {
         this.fullText = param0;
         this.cursor = param1;
         this.cursorAtEnd = param2;
         this.lineStarts = param3;
         this.lines = param4;
         this.selection = param5;
      }

      public int getIndexAtPosition(Font param0, BookEditScreen.Pos2i param1) {
         int var3 = param1.y / 9;
         if (var3 < 0) {
            return 0;
         }

         if (var3 >= this.lines.length) {
            return this.fullText.length();
         }

         BookEditScreen.LineInfo var4 = this.lines[var3];
         return this.lineStarts[var3] + param0.getSplitter().plainIndexAtWidth(var4.contents, param1.x, var4.style);
      }

      public int changeLine(int param0, int param1) {
         int var3 = BookEditScreen.findLineFromPos(this.lineStarts, param0);
         int var4 = var3 + param1;
         int var5;
         if (0 <= var4 && var4 < this.lineStarts.length) {
            int var6 = param0 - this.lineStarts[var3];
            int var7 = this.lines[var4].contents.length();
            var5 = this.lineStarts[var4] + Math.min(var6, var7);
         } else {
            var5 = param0;
         }

         return var5;
      }

      public int findLineStart(int param0) {
         int var2 = BookEditScreen.findLineFromPos(this.lineStarts, param0);
         return this.lineStarts[var2];
      }

      public int findLineEnd(int param0) {
         int var2 = BookEditScreen.findLineFromPos(this.lineStarts, param0);
         return this.lineStarts[var2] + this.lines[var2].contents.length();
      }
   }

   static class LineInfo {
      final Style style;
      final String contents;
      final Component asComponent;
      final int x;
      final int y;

      public LineInfo(Style param0, String param1, int param2, int param3) {
         this.style = param0;
         this.contents = param1;
         this.x = param2;
         this.y = param3;
         this.asComponent = Component.literal(param1).setStyle(param0);
      }
   }

   static class Pos2i {
      public final int x;
      public final int y;

      Pos2i(int param0, int param1) {
         this.x = param0;
         this.y = param1;
      }
   }
}
