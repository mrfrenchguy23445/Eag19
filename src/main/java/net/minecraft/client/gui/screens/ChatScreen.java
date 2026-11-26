package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

public class ChatScreen extends Screen {
   public static final double MOUSE_SCROLL_SPEED = 7.0;
   private static final Component USAGE_TEXT = Component.translatable("chat_screen.usage");
   private static final int TOOLTIP_MAX_WIDTH = 210;
   private String historyBuffer = "";
   private int historyPos = -1;
   protected EditBox input;
   private String initial;
   CommandSuggestions commandSuggestions;

   public ChatScreen(String param0) {
      super(Component.translatable("chat_screen.title"));
      this.initial = param0;
   }

   @Override
   protected void init() {
      this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
      this.input = new EditBox(this.minecraft.fontFilterFishy, 4, this.height - 12, this.width - 4, 12, Component.translatable("chat.editBox")) {
         @Override
         protected MutableComponent createNarrationMessage() {
            return super.createNarrationMessage().append(ChatScreen.this.commandSuggestions.getNarrationMessage());
         }
      };
      this.input.setMaxLength(256);
      this.input.setBordered(false);
      this.input.setValue(this.initial);
      this.input.setResponder(this::onEdited);
      this.input.setCanLoseFocus(false);
      this.addWidget(this.input);
      this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.input, this.font, false, false, 1, 10, true, -805306368);
      this.commandSuggestions.updateCommandInfo();
      this.setInitialFocus(this.input);
   }

   @Override
   public void resize(Minecraft param0, int param1, int param2) {
      String var4 = this.input.getValue();
      this.init(param0, param1, param2);
      this.setChatLine(var4);
      this.commandSuggestions.updateCommandInfo();
   }

   @Override
   public void removed() {
      this.minecraft.gui.getChat().resetChatScroll();
   }

   @Override
   public void tick() {
      this.input.tick();
   }

   private void onEdited(String param0) {
      String var2 = this.input.getValue();
      this.commandSuggestions.setAllowSuggestions(!var2.equals(this.initial));
      this.commandSuggestions.updateCommandInfo();
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (this.commandSuggestions.keyPressed(param0, param1, param2)) {
         return true;
      }

      if (super.keyPressed(param0, param1, param2)) {
         return true;
      }

      if (param0 == 256) {
         this.minecraft.setScreen(null);
         return true;
      }

      if (param0 == 257 || param0 == 335) {
         if (this.handleChatInput(this.input.getValue(), true)) {
            this.minecraft.setScreen(null);
         }

         return true;
      } else if (param0 == 265) {
         this.moveInHistory(-1);
         return true;
      } else if (param0 == 264) {
         this.moveInHistory(1);
         return true;
      } else if (param0 == 266) {
         this.minecraft.gui.getChat().scrollChat(this.minecraft.gui.getChat().getLinesPerPage() - 1);
         return true;
      } else if (param0 == 267) {
         this.minecraft.gui.getChat().scrollChat(-this.minecraft.gui.getChat().getLinesPerPage() + 1);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double param0, double param2, double param4) {
      param4 = Mth.clamp(param4, -1.0, 1.0);
      if (this.commandSuggestions.mouseScrolled(param4)) {
         return true;
      }

      if (!hasShiftDown()) {
         param4 *= 7.0;
      }

      this.minecraft.gui.getChat().scrollChat((int)param4);
      return true;
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (this.commandSuggestions.mouseClicked((int)param0, (int)param2, param4)) {
         return true;
      }

      if (param4 == 0) {
         ChatComponent var6 = this.minecraft.gui.getChat();
         if (var6.handleChatQueueClicked(param0, param2)) {
            return true;
         }

         Style var7 = this.getComponentStyleAt(param0, param2);
         if (var7 != null && this.handleComponentClicked(var7)) {
            this.initial = this.input.getValue();
            return true;
         }
      }

      return this.input.mouseClicked(param0, param2, param4) ? true : super.mouseClicked(param0, param2, param4);
   }

   @Override
   protected void insertText(String param0, boolean param1) {
      if (param1) {
         this.input.setValue(param0);
      } else {
         this.input.insertText(param0);
      }
   }

   public void moveInHistory(int param0) {
      int var2 = this.historyPos + param0;
      int var3 = this.minecraft.gui.getChat().getRecentChat().size();
      var2 = Mth.clamp(var2, 0, var3);
      if (var2 != this.historyPos) {
         if (var2 == var3) {
            this.historyPos = var3;
            this.input.setValue(this.historyBuffer);
         } else {
            if (this.historyPos == var3) {
               this.historyBuffer = this.input.getValue();
            }

            this.input.setValue(this.minecraft.gui.getChat().getRecentChat().get(var2));
            this.commandSuggestions.setAllowSuggestions(false);
            this.historyPos = var2;
         }
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      fill(param0, 2, this.height - 14, this.width - 2, this.height - 2, this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
      this.input.render(param0, param1, param2, param3);
      super.render(param0, param1, param2, param3);
      this.commandSuggestions.render(param0, param1, param2);
      GuiMessageTag var5 = this.minecraft.gui.getChat().getMessageTagAt(param1, param2);
      if (var5 != null && var5.text() != null) {
         this.renderTooltip(param0, this.font.split(var5.text(), 210), param1, param2);
      } else {
         Style var6 = this.getComponentStyleAt(param1, param2);
         if (var6 != null && var6.getHoverEvent() != null) {
            this.renderComponentHoverEffect(param0, var6, param1, param2);
         }
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private void setChatLine(String param0) {
      this.input.setValue(param0);
   }

   @Override
   protected void updateNarrationState(NarrationElementOutput param0) {
      param0.add(NarratedElementType.TITLE, this.getTitle());
      param0.add(NarratedElementType.USAGE, USAGE_TEXT);
      String var2 = this.input.getValue();
      if (!var2.isEmpty()) {
         param0.nest().add(NarratedElementType.TITLE, Component.translatable("chat_screen.message", var2));
      }
   }

   @Nullable
   private Style getComponentStyleAt(double param0, double param2) {
      return this.minecraft.gui.getChat().getClickedComponentStyleAt(param0, param2);
   }

   public boolean handleChatInput(String param0, boolean param1) {
      param0 = this.normalizeChatMessage(param0);
      if (param0.isEmpty()) {
         return true;
      }

      if (param1) {
         this.minecraft.gui.getChat().addRecentChat(param0);
      }

      if (param0.startsWith("/")) {
         this.minecraft.player.connection.sendCommand(param0.substring(1));
      } else {
         this.minecraft.player.connection.sendChat(param0);
      }

      return true;
   }

   public String normalizeChatMessage(String param0) {
      return StringUtil.trimChatMessage(StringUtils.normalizeSpace(param0.trim()));
   }
}
