package net.minecraft.client.gui.components;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public class CommandSuggestions {
   private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
   private static final Style UNPARSED_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);
   private static final Style LITERAL_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
   private static final List<Style> ARGUMENT_STYLES = Stream.of(
         ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD
      )
      .map(Style.EMPTY::withColor)
      .collect(ImmutableList.toImmutableList());
   final Minecraft minecraft;
   final Screen screen;
   final EditBox input;
   final Font font;
   private final boolean commandsOnly;
   private final boolean onlyShowIfCursorPastError;
   final int lineStartOffset;
   final int suggestionLineLimit;
   final boolean anchorToBottom;
   final int fillColor;
   private final List<FormattedCharSequence> commandUsage = Lists.newArrayList();
   private int commandUsagePosition;
   private int commandUsageWidth;
   @Nullable
   private ParseResults<SharedSuggestionProvider> currentParse;
   @Nullable
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Nullable
   private CommandSuggestions.SuggestionsList suggestions;
   private boolean allowSuggestions;
   boolean keepSuggestions;

   public CommandSuggestions(
      Minecraft param0, Screen param1, EditBox param2, Font param3, boolean param4, boolean param5, int param6, int param7, boolean param8, int param9
   ) {
      this.minecraft = param0;
      this.screen = param1;
      this.input = param2;
      this.font = param3;
      this.commandsOnly = param4;
      this.onlyShowIfCursorPastError = param5;
      this.lineStartOffset = param6;
      this.suggestionLineLimit = param7;
      this.anchorToBottom = param8;
      this.fillColor = param9;
      param2.setFormatter(this::formatChat);
   }

   public void setAllowSuggestions(boolean param0) {
      this.allowSuggestions = param0;
      if (!param0) {
         this.suggestions = null;
      }
   }

   public boolean keyPressed(int param0, int param1, int param2) {
      if (this.suggestions != null && this.suggestions.keyPressed(param0, param1, param2)) {
         return true;
      } else if (this.screen.getFocused() == this.input && param0 == 258) {
         this.showSuggestions(true);
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double param0) {
      return this.suggestions != null && this.suggestions.mouseScrolled(Mth.clamp(param0, -1.0, 1.0));
   }

   public boolean mouseClicked(double param0, double param2, int param4) {
      return this.suggestions != null && this.suggestions.mouseClicked((int)param0, (int)param2, param4);
   }

   public void showSuggestions(boolean param0) {
      if (this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {
         Suggestions var2 = this.pendingSuggestions.join();
         if (!var2.isEmpty()) {
            int var3 = 0;

            for (Suggestion var5 : var2.getList()) {
               var3 = Math.max(var3, this.font.width(var5.getText()));
            }

            int var4 = Mth.clamp(this.input.getScreenX(var2.getRange().getStart()), 0, this.input.getScreenX(0) + this.input.getInnerWidth() - var3);
            int var5 = this.anchorToBottom ? this.screen.height - 12 : 72;
            this.suggestions = new CommandSuggestions.SuggestionsList(var4, var5, var3, this.sortSuggestions(var2), param0);
         }
      }
   }

   public void hide() {
      this.suggestions = null;
   }

   private List<Suggestion> sortSuggestions(Suggestions param0) {
      String var2 = this.input.getValue().substring(0, this.input.getCursorPosition());
      int var3 = getLastWordIndex(var2);
      String var4 = var2.substring(var3).toLowerCase(Locale.ROOT);
      List<Suggestion> var5 = Lists.newArrayList();
      List<Suggestion> var6 = Lists.newArrayList();

      for (Suggestion var8 : param0.getList()) {
         if (!var8.getText().startsWith(var4) && !var8.getText().startsWith("minecraft:" + var4)) {
            var6.add(var8);
         } else {
            var5.add(var8);
         }
      }

      var5.addAll(var6);
      return var5;
   }

   public void updateCommandInfo() {
      String var1 = this.input.getValue();
      if (this.currentParse != null && !this.currentParse.getReader().getString().equals(var1)) {
         this.currentParse = null;
      }

      if (!this.keepSuggestions) {
         this.input.setSuggestion(null);
         this.suggestions = null;
      }

      this.commandUsage.clear();
      StringReader var2 = new StringReader(var1);
      boolean var3 = var2.canRead() && var2.peek() == '/';
      if (var3) {
         var2.skip();
      }

      boolean var4 = this.commandsOnly || var3;
      int var5 = this.input.getCursorPosition();
      if (var4) {
         CommandDispatcher<SharedSuggestionProvider> var6 = this.minecraft.player.connection.getCommands();
         if (this.currentParse == null) {
            this.currentParse = var6.parse(var2, this.minecraft.player.connection.getSuggestionsProvider());
         }

         int var7 = this.onlyShowIfCursorPastError ? var2.getCursor() : 1;
         if (var5 >= var7 && (this.suggestions == null || !this.keepSuggestions)) {
            this.pendingSuggestions = var6.getCompletionSuggestions(this.currentParse, var5);
            this.pendingSuggestions.thenRun(() -> {
               if (this.pendingSuggestions.isDone()) {
                  this.updateUsageInfo();
               }
            });
         }
      } else {
         String var6 = var1.substring(0, var5);
         int var7 = getLastWordIndex(var6);
         Collection<String> var8 = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSugggestions();
         this.pendingSuggestions = SharedSuggestionProvider.suggest(var8, new SuggestionsBuilder(var6, var7));
      }
   }

   private static int getLastWordIndex(String param0) {
      if (Strings.isNullOrEmpty(param0)) {
         return 0;
      }

      int var1 = 0;
      Matcher var2 = WHITESPACE_PATTERN.matcher(param0);

      while (var2.find()) {
         var1 = var2.end();
      }

      return var1;
   }

   private static FormattedCharSequence getExceptionMessage(CommandSyntaxException param0) {
      Component var1 = ComponentUtils.fromMessage(param0.getRawMessage());
      String var2 = param0.getContext();
      return var2 == null
         ? var1.getVisualOrderText()
         : Component.translatable("command.context.parse_error", var1, param0.getCursor(), var2).getVisualOrderText();
   }

   private void updateUsageInfo() {
      if (this.input.getCursorPosition() == this.input.getValue().length()) {
         if (this.pendingSuggestions.join().isEmpty() && !this.currentParse.getExceptions().isEmpty()) {
            int var1 = 0;

            for (Entry<CommandNode<SharedSuggestionProvider>, CommandSyntaxException> var3 : this.currentParse.getExceptions().entrySet()) {
               CommandSyntaxException var4 = var3.getValue();
               if (var4.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect()) {
                  var1++;
               } else {
                  this.commandUsage.add(getExceptionMessage(var4));
               }
            }

            if (var1 > 0) {
               this.commandUsage.add(getExceptionMessage(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create()));
            }
         } else if (this.currentParse.getReader().canRead()) {
            this.commandUsage.add(getExceptionMessage(Commands.getParseException(this.currentParse)));
         }
      }

      this.commandUsagePosition = 0;
      this.commandUsageWidth = this.screen.width;
      if (this.commandUsage.isEmpty()) {
         this.fillNodeUsage(ChatFormatting.GRAY);
      }

      this.suggestions = null;
      if (this.allowSuggestions && this.minecraft.options.autoSuggestions().get()) {
         this.showSuggestions(false);
      }
   }

   private void fillNodeUsage(ChatFormatting param0) {
      CommandContextBuilder<SharedSuggestionProvider> var2 = this.currentParse.getContext();
      SuggestionContext<SharedSuggestionProvider> var3 = var2.findSuggestionContext(this.input.getCursorPosition());
      Map<CommandNode<SharedSuggestionProvider>, String> var4 = this.minecraft
         .player
         .connection
         .getCommands()
         .getSmartUsage(var3.parent, this.minecraft.player.connection.getSuggestionsProvider());
      List<FormattedCharSequence> var5 = Lists.newArrayList();
      int var6 = 0;
      Style var7 = Style.EMPTY.withColor(param0);

      for (Entry<CommandNode<SharedSuggestionProvider>, String> var9 : var4.entrySet()) {
         if (!(var9.getKey() instanceof LiteralCommandNode)) {
            var5.add(FormattedCharSequence.forward(var9.getValue(), var7));
            var6 = Math.max(var6, this.font.width(var9.getValue()));
         }
      }

      if (!var5.isEmpty()) {
         this.commandUsage.addAll(var5);
         this.commandUsagePosition = Mth.clamp(this.input.getScreenX(var3.startPos), 0, this.input.getScreenX(0) + this.input.getInnerWidth() - var6);
         this.commandUsageWidth = var6;
      }
   }

   private FormattedCharSequence formatChat(String param0, int param1) {
      return this.currentParse != null ? formatText(this.currentParse, param0, param1) : FormattedCharSequence.forward(param0, Style.EMPTY);
   }

   @Nullable
   static String calculateSuggestionSuffix(String param0, String param1) {
      return param1.startsWith(param0) ? param1.substring(param0.length()) : null;
   }

   private static FormattedCharSequence formatText(ParseResults<SharedSuggestionProvider> param0, String param1, int param2) {
      List<FormattedCharSequence> var3 = Lists.newArrayList();
      int var4 = 0;
      int var5 = -1;
      CommandContextBuilder<SharedSuggestionProvider> var6 = param0.getContext().getLastChild();

      for (ParsedArgument<SharedSuggestionProvider, ?> var8 : var6.getArguments().values()) {
         if (++var5 >= ARGUMENT_STYLES.size()) {
            var5 = 0;
         }

         int var9 = Math.max(var8.getRange().getStart() - param2, 0);
         if (var9 >= param1.length()) {
            break;
         }

         int var10 = Math.min(var8.getRange().getEnd() - param2, param1.length());
         if (var10 > 0) {
            var3.add(FormattedCharSequence.forward(param1.substring(var4, var9), LITERAL_STYLE));
            var3.add(FormattedCharSequence.forward(param1.substring(var9, var10), ARGUMENT_STYLES.get(var5)));
            var4 = var10;
         }
      }

      if (param0.getReader().canRead()) {
         int var7 = Math.max(param0.getReader().getCursor() - param2, 0);
         if (var7 < param1.length()) {
            int var8 = Math.min(var7 + param0.getReader().getRemainingLength(), param1.length());
            var3.add(FormattedCharSequence.forward(param1.substring(var4, var7), LITERAL_STYLE));
            var3.add(FormattedCharSequence.forward(param1.substring(var7, var8), UNPARSED_STYLE));
            var4 = var8;
         }
      }

      var3.add(FormattedCharSequence.forward(param1.substring(var4), LITERAL_STYLE));
      return FormattedCharSequence.composite(var3);
   }

   public void render(PoseStack param0, int param1, int param2) {
      if (!this.renderSuggestions(param0, param1, param2)) {
         this.renderUsage(param0);
      }
   }

   public boolean renderSuggestions(PoseStack param0, int param1, int param2) {
      if (this.suggestions != null) {
         this.suggestions.render(param0, param1, param2);
         return true;
      } else {
         return false;
      }
   }

   public void renderUsage(PoseStack param0) {
      int var2 = 0;

      for (FormattedCharSequence var4 : this.commandUsage) {
         int var5 = this.anchorToBottom ? this.screen.height - 14 - 13 - 12 * var2 : 72 + 12 * var2;
         GuiComponent.fill(param0, this.commandUsagePosition - 1, var5, this.commandUsagePosition + this.commandUsageWidth + 1, var5 + 12, this.fillColor);
         this.font.drawShadow(param0, var4, this.commandUsagePosition, var5 + 2, -1);
         var2++;
      }
   }

   public Component getNarrationMessage() {
      return this.suggestions != null ? CommonComponents.NEW_LINE.copy().append(this.suggestions.getNarrationMessage()) : CommonComponents.EMPTY;
   }

   public class SuggestionsList {
      private final Rect2i rect;
      private final String originalContents;
      private final List<Suggestion> suggestionList;
      private int offset;
      private int current;
      private Vec2 lastMouse = Vec2.ZERO;
      private boolean tabCycles;
      private int lastNarratedEntry;

      SuggestionsList(int param1, int param2, int param3, List<Suggestion> param4, boolean param5) {
         int var7 = param1 - 1;
         int var8 = CommandSuggestions.this.anchorToBottom ? param2 - 3 - Math.min(param4.size(), CommandSuggestions.this.suggestionLineLimit) * 12 : param2;
         this.rect = new Rect2i(var7, var8, param3 + 1, Math.min(param4.size(), CommandSuggestions.this.suggestionLineLimit) * 12);
         this.originalContents = CommandSuggestions.this.input.getValue();
         this.lastNarratedEntry = param5 ? -1 : 0;
         this.suggestionList = param4;
         this.select(0);
      }

      public void render(PoseStack param0, int param1, int param2) {
         int var4 = Math.min(this.suggestionList.size(), CommandSuggestions.this.suggestionLineLimit);
         int var5 = -5592406;
         boolean var6 = this.offset > 0;
         boolean var7 = this.suggestionList.size() > this.offset + var4;
         boolean var8 = var6 || var7;
         boolean var9 = this.lastMouse.x != param1 || this.lastMouse.y != param2;
         if (var9) {
            this.lastMouse = new Vec2(param1, param2);
         }

         if (var8) {
            GuiComponent.fill(
               param0, this.rect.getX(), this.rect.getY() - 1, this.rect.getX() + this.rect.getWidth(), this.rect.getY(), CommandSuggestions.this.fillColor
            );
            GuiComponent.fill(
               param0,
               this.rect.getX(),
               this.rect.getY() + this.rect.getHeight(),
               this.rect.getX() + this.rect.getWidth(),
               this.rect.getY() + this.rect.getHeight() + 1,
               CommandSuggestions.this.fillColor
            );
            if (var6) {
               for (int var10 = 0; var10 < this.rect.getWidth(); var10++) {
                  if (var10 % 2 == 0) {
                     GuiComponent.fill(param0, this.rect.getX() + var10, this.rect.getY() - 1, this.rect.getX() + var10 + 1, this.rect.getY(), -1);
                  }
               }
            }

            if (var7) {
               for (int var10 = 0; var10 < this.rect.getWidth(); var10++) {
                  if (var10 % 2 == 0) {
                     GuiComponent.fill(
                        param0,
                        this.rect.getX() + var10,
                        this.rect.getY() + this.rect.getHeight(),
                        this.rect.getX() + var10 + 1,
                        this.rect.getY() + this.rect.getHeight() + 1,
                        -1
                     );
                  }
               }
            }
         }

         boolean var10 = false;

         for (int var11 = 0; var11 < var4; var11++) {
            Suggestion var12 = this.suggestionList.get(var11 + this.offset);
            GuiComponent.fill(
               param0,
               this.rect.getX(),
               this.rect.getY() + 12 * var11,
               this.rect.getX() + this.rect.getWidth(),
               this.rect.getY() + 12 * var11 + 12,
               CommandSuggestions.this.fillColor
            );
            if (param1 > this.rect.getX()
               && param1 < this.rect.getX() + this.rect.getWidth()
               && param2 > this.rect.getY() + 12 * var11
               && param2 < this.rect.getY() + 12 * var11 + 12) {
               if (var9) {
                  this.select(var11 + this.offset);
               }

               var10 = true;
            }

            CommandSuggestions.this.font
               .drawShadow(
                  param0, var12.getText(), this.rect.getX() + 1, this.rect.getY() + 2 + 12 * var11, var11 + this.offset == this.current ? -256 : -5592406
               );
         }

         if (var10) {
            Message var11 = this.suggestionList.get(this.current).getTooltip();
            if (var11 != null) {
               CommandSuggestions.this.screen.renderTooltip(param0, ComponentUtils.fromMessage(var11), param1, param2);
            }
         }
      }

      public boolean mouseClicked(int param0, int param1, int param2) {
         if (!this.rect.contains(param0, param1)) {
            return false;
         }

         int var4 = (param1 - this.rect.getY()) / 12 + this.offset;
         if (var4 >= 0 && var4 < this.suggestionList.size()) {
            this.select(var4);
            this.useSuggestion();
         }

         return true;
      }

      public boolean mouseScrolled(double param0) {
         int var3 = (int)(
            CommandSuggestions.this.minecraft.mouseHandler.xpos()
               * CommandSuggestions.this.minecraft.getWindow().getGuiScaledWidth()
               / CommandSuggestions.this.minecraft.getWindow().getScreenWidth()
         );
         int var4 = (int)(
            CommandSuggestions.this.minecraft.mouseHandler.ypos()
               * CommandSuggestions.this.minecraft.getWindow().getGuiScaledHeight()
               / CommandSuggestions.this.minecraft.getWindow().getScreenHeight()
         );
         if (this.rect.contains(var3, var4)) {
            this.offset = Mth.clamp((int)(this.offset - param0), 0, Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0));
            return true;
         } else {
            return false;
         }
      }

      public boolean keyPressed(int param0, int param1, int param2) {
         if (param0 == 265) {
            this.cycle(-1);
            this.tabCycles = false;
            return true;
         }

         if (param0 == 264) {
            this.cycle(1);
            this.tabCycles = false;
            return true;
         }

         if (param0 == 258) {
            if (this.tabCycles) {
               this.cycle(Screen.hasShiftDown() ? -1 : 1);
            }

            this.useSuggestion();
            return true;
         } else if (param0 == 256) {
            CommandSuggestions.this.hide();
            return true;
         } else {
            return false;
         }
      }

      public void cycle(int param0) {
         this.select(this.current + param0);
         int var2 = this.offset;
         int var3 = this.offset + CommandSuggestions.this.suggestionLineLimit - 1;
         if (this.current < var2) {
            this.offset = Mth.clamp(this.current, 0, Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0));
         } else if (this.current > var3) {
            this.offset = Mth.clamp(
               this.current + CommandSuggestions.this.lineStartOffset - CommandSuggestions.this.suggestionLineLimit,
               0,
               Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0)
            );
         }
      }

      public void select(int param0) {
         this.current = param0;
         if (this.current < 0) {
            this.current = this.current + this.suggestionList.size();
         }

         if (this.current >= this.suggestionList.size()) {
            this.current = this.current - this.suggestionList.size();
         }

         Suggestion var2 = this.suggestionList.get(this.current);
         CommandSuggestions.this.input
            .setSuggestion(CommandSuggestions.calculateSuggestionSuffix(CommandSuggestions.this.input.getValue(), var2.apply(this.originalContents)));
         if (this.lastNarratedEntry != this.current) {
            CommandSuggestions.this.minecraft.getNarrator().sayNow(this.getNarrationMessage());
         }
      }

      public void useSuggestion() {
         Suggestion var1 = this.suggestionList.get(this.current);
         CommandSuggestions.this.keepSuggestions = true;
         CommandSuggestions.this.input.setValue(var1.apply(this.originalContents));
         int var2 = var1.getRange().getStart() + var1.getText().length();
         CommandSuggestions.this.input.setCursorPosition(var2);
         CommandSuggestions.this.input.setHighlightPos(var2);
         this.select(this.current);
         CommandSuggestions.this.keepSuggestions = false;
         this.tabCycles = true;
      }

      Component getNarrationMessage() {
         this.lastNarratedEntry = this.current;
         Suggestion var1 = this.suggestionList.get(this.current);
         Message var2 = var1.getTooltip();
         return var2 != null
            ? Component.translatable("narration.suggestion.tooltip", this.current + 1, this.suggestionList.size(), var1.getText(), var2)
            : Component.translatable("narration.suggestion", this.current + 1, this.suggestionList.size(), var1.getText());
      }
   }
}
