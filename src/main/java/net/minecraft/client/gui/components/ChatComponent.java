package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.slf4j.Logger;

public class ChatComponent extends GuiComponent {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_CHAT_HISTORY = 100;
   private static final int MESSAGE_NOT_FOUND = -1;
   private static final int MESSAGE_INDENT = 4;
   private static final int MESSAGE_TAG_MARGIN_LEFT = 4;
   private static final int BOTTOM_MARGIN = 40;
   private static final int TIME_BEFORE_MESSAGE_DELETION = 60;
   private static final Component DELETED_CHAT_MESSAGE = Component.translatable("chat.deleted_marker").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
   private final Minecraft minecraft;
   private final List<String> recentChat = Lists.newArrayList();
   private final List<GuiMessage> allMessages = Lists.newArrayList();
   private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
   private int chatScrollbarPos;
   private boolean newMessageSinceScroll;
   private final List<ChatComponent.DelayedMessageDeletion> messageDeletionQueue = new ArrayList<>();

   public ChatComponent(Minecraft param0) {
      this.minecraft = param0;
   }

   public void tick() {
      if (!this.messageDeletionQueue.isEmpty()) {
         this.processMessageDeletionQueue();
      }
   }

   public void render(PoseStack param0, int param1, int param2, int param3) {
      if (!this.isChatHidden()) {
         int var5 = this.getLinesPerPage();
         int var6 = this.trimmedMessages.size();
         if (var6 > 0) {
            boolean var7 = this.isChatFocused();
            float var8 = (float)this.getScale();
            int var9 = Mth.ceil(this.getWidth() / var8);
            int var10 = this.minecraft.getWindow().getGuiScaledHeight();
            param0.pushPose();
            param0.scale(var8, var8, 1.0F);
            param0.translate(4.0F, 0.0F, 0.0F);
            int var11 = Mth.floor((var10 - 40) / var8);
            int var12 = this.getMessageEndIndexAt(this.screenToChatX(param2), this.screenToChatY(param3));
            double var13 = this.minecraft.options.chatOpacity().get() * 0.9F + 0.1F;
            double var15 = this.minecraft.options.textBackgroundOpacity().get();
            double var17 = this.minecraft.options.chatLineSpacing().get();
            int var19 = this.getLineHeight();
            int var20 = (int)Math.round(-8.0 * (var17 + 1.0) + 4.0 * var17);
            int var21 = 0;

            for (int var22 = 0; var22 + this.chatScrollbarPos < this.trimmedMessages.size() && var22 < var5; var22++) {
               int var23 = var22 + this.chatScrollbarPos;
               GuiMessage.Line var24 = this.trimmedMessages.get(var23);
               if (var24 != null) {
                  int var25 = param1 - var24.addedTime();
                  if (var25 < 200 || var7) {
                     double var26 = var7 ? 1.0 : getTimeFactor(var25);
                     int var28 = (int)(255.0 * var26 * var13);
                     int var29 = (int)(255.0 * var26 * var15);
                     var21++;
                     if (var28 > 3) {
                        int var30 = 0;
                        int var31 = var11 - var22 * var19;
                        int var32 = var31 + var20;
                        param0.pushPose();
                        param0.translate(0.0F, 0.0F, 50.0F);
                        fill(param0, -4, var31 - var19, 0 + var9 + 4 + 4, var31, var29 << 24);
                        GuiMessageTag var33 = var24.tag();
                        if (var33 != null) {
                           int var34 = var33.indicatorColor() | var28 << 24;
                           fill(param0, -4, var31 - var19, -2, var31, var34);
                           if (var23 == var12 && var33.icon() != null) {
                              int var35 = this.getTagIconLeft(var24);
                              int var36 = var32 + 9;
                              this.drawTagIcon(param0, var35, var36, var33.icon());
                           }
                        }

                        param0.translate(0.0F, 0.0F, 50.0F);
                        this.minecraft.font.drawShadow(param0, var24.content(), 0.0F, var32, 16777215 + (var28 << 24));
                        param0.popPose();
                     }
                  }
               }
            }

            long var22 = this.minecraft.getChatListener().queueSize();
            if (var22 > 0L) {
               int var24 = (int)(128.0 * var13);
               int var25 = (int)(255.0 * var15);
               param0.pushPose();
               param0.translate(0.0F, var11, 50.0F);
               fill(param0, -2, 0, var9 + 4, 9, var25 << 24);
               param0.translate(0.0F, 0.0F, 50.0F);
               this.minecraft.font.drawShadow(param0, Component.translatable("chat.queue", var22), 0.0F, 1.0F, 16777215 + (var24 << 24));
               param0.popPose();
            }

            if (var7) {
               int var24 = this.getLineHeight();
               int var25 = var6 * var24;
               int var26 = var21 * var24;
               int var27 = this.chatScrollbarPos * var26 / var6 - var11;
               int var28 = var26 * var26 / var25;
               if (var25 != var26) {
                  int var29 = var27 > 0 ? 170 : 96;
                  int var30 = this.newMessageSinceScroll ? 13382451 : 3355562;
                  int var31 = var9 + 4;
                  fill(param0, var31, -var27, var31 + 2, -var27 - var28, var30 + (var29 << 24));
                  fill(param0, var31 + 2, -var27, var31 + 1, -var27 - var28, 13421772 + (var29 << 24));
               }
            }

            param0.popPose();
         }
      }
   }

   private void drawTagIcon(PoseStack param0, int param1, int param2, GuiMessageTag.Icon param3) {
      int var5 = param2 - param3.height - 1;
      param3.draw(param0, param1, var5);
   }

   private int getTagIconLeft(GuiMessage.Line param0) {
      return this.minecraft.font.width(param0.content()) + 4;
   }

   private boolean isChatHidden() {
      return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
   }

   private static double getTimeFactor(int param0) {
      double var1 = param0 / 200.0;
      var1 = 1.0 - var1;
      var1 *= 10.0;
      var1 = Mth.clamp(var1, 0.0, 1.0);
      return var1 * var1;
   }

   public void clearMessages(boolean param0) {
      this.minecraft.getChatListener().clearQueue();
      this.messageDeletionQueue.clear();
      this.trimmedMessages.clear();
      this.allMessages.clear();
      if (param0) {
         this.recentChat.clear();
      }
   }

   public void addMessage(Component param0) {
      this.addMessage(param0, null, this.minecraft.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system());
   }

   public void addMessage(Component param0, @Nullable MessageSignature param1, @Nullable GuiMessageTag param2) {
      this.logChatMessage(param0, param2);
      this.addMessage(param0, param1, this.minecraft.gui.getGuiTicks(), param2, false);
   }

   private void logChatMessage(Component param0, @Nullable GuiMessageTag param1) {
      String var3 = param0.getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
      String var4 = Optionull.map(param1, GuiMessageTag::logTag);
      if (var4 != null) {
         LOGGER.info("[{}] [CHAT] {}", var4, var3);
      } else {
         LOGGER.info("[CHAT] {}", var3);
      }
   }

   private void addMessage(Component param0, @Nullable MessageSignature param1, int param2, @Nullable GuiMessageTag param3, boolean param4) {
      int var6 = Mth.floor(this.getWidth() / this.getScale());
      if (param3 != null && param3.icon() != null) {
         var6 -= param3.icon().width + 4 + 2;
      }

      List<FormattedCharSequence> var7 = ComponentRenderUtils.wrapComponents(param0, var6, this.minecraft.font);
      boolean var8 = this.isChatFocused();

      for (int var9 = 0; var9 < var7.size(); var9++) {
         FormattedCharSequence var10 = var7.get(var9);
         if (var8 && this.chatScrollbarPos > 0) {
            this.newMessageSinceScroll = true;
            this.scrollChat(1);
         }

         boolean var11 = var9 == var7.size() - 1;
         this.trimmedMessages.add(0, new GuiMessage.Line(param2, var10, param3, var11));
      }

      while (this.trimmedMessages.size() > 100) {
         this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
      }

      if (!param4) {
         this.allMessages.add(0, new GuiMessage(param2, param0, param1, param3));

         while (this.allMessages.size() > 100) {
            this.allMessages.remove(this.allMessages.size() - 1);
         }
      }
   }

   private void processMessageDeletionQueue() {
      int var1 = this.minecraft.gui.getGuiTicks();
      this.messageDeletionQueue.removeIf(l_param1 -> var1 >= l_param1.deletableAfter() ? this.deleteMessageOrDelay(l_param1.signature()) == null : false);
   }

   public void deleteMessage(MessageSignature param0) {
      ChatComponent.DelayedMessageDeletion var2 = this.deleteMessageOrDelay(param0);
      if (var2 != null) {
         this.messageDeletionQueue.add(var2);
      }
   }

   @Nullable
   private ChatComponent.DelayedMessageDeletion deleteMessageOrDelay(MessageSignature param0) {
      int var2 = this.minecraft.gui.getGuiTicks();
      ListIterator<GuiMessage> var3 = this.allMessages.listIterator();

      while (var3.hasNext()) {
         GuiMessage var4 = var3.next();
         if (param0.equals(var4.signature())) {
            int var5 = var4.addedTime() + 60;
            if (var2 >= var5) {
               var3.set(this.createDeletedMarker(var4));
               this.refreshTrimmedMessage();
               return null;
            }

            return new ChatComponent.DelayedMessageDeletion(param0, var5);
         }
      }

      return null;
   }

   private GuiMessage createDeletedMarker(GuiMessage param0) {
      return new GuiMessage(param0.addedTime(), DELETED_CHAT_MESSAGE, null, GuiMessageTag.system());
   }

   public void rescaleChat() {
      this.resetChatScroll();
      this.refreshTrimmedMessage();
   }

   private void refreshTrimmedMessage() {
      this.trimmedMessages.clear();

      for (int var1 = this.allMessages.size() - 1; var1 >= 0; var1--) {
         GuiMessage var2 = this.allMessages.get(var1);
         this.addMessage(var2.content(), var2.signature(), var2.addedTime(), var2.tag(), true);
      }
   }

   public List<String> getRecentChat() {
      return this.recentChat;
   }

   public void addRecentChat(String param0) {
      if (this.recentChat.isEmpty() || !this.recentChat.get(this.recentChat.size() - 1).equals(param0)) {
         this.recentChat.add(param0);
      }
   }

   public void resetChatScroll() {
      this.chatScrollbarPos = 0;
      this.newMessageSinceScroll = false;
   }

   public void scrollChat(int param0) {
      this.chatScrollbarPos += param0;
      int var2 = this.trimmedMessages.size();
      if (this.chatScrollbarPos > var2 - this.getLinesPerPage()) {
         this.chatScrollbarPos = var2 - this.getLinesPerPage();
      }

      if (this.chatScrollbarPos <= 0) {
         this.chatScrollbarPos = 0;
         this.newMessageSinceScroll = false;
      }
   }

   public boolean handleChatQueueClicked(double param0, double param2) {
      if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden()) {
         ChatListener var5 = this.minecraft.getChatListener();
         if (var5.queueSize() == 0L) {
            return false;
         } else {
            double var6 = param0 - 2.0;
            double var8 = this.minecraft.getWindow().getGuiScaledHeight() - param2 - 40.0;
            if (var6 <= Mth.floor(this.getWidth() / this.getScale()) && var8 < 0.0 && var8 > Mth.floor(-9.0 * this.getScale())) {
               var5.acceptNextDelayedMessage();
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   @Nullable
   public Style getClickedComponentStyleAt(double param0, double param2) {
      double var5 = this.screenToChatX(param0);
      double var7 = this.screenToChatY(param2);
      int var9 = this.getMessageLineIndexAt(var5, var7);
      if (var9 >= 0 && var9 < this.trimmedMessages.size()) {
         GuiMessage.Line var10 = this.trimmedMessages.get(var9);
         return this.minecraft.font.getSplitter().componentStyleAtWidth(var10.content(), Mth.floor(var5));
      } else {
         return null;
      }
   }

   @Nullable
   public GuiMessageTag getMessageTagAt(double param0, double param2) {
      double var5 = this.screenToChatX(param0);
      double var7 = this.screenToChatY(param2);
      int var9 = this.getMessageEndIndexAt(var5, var7);
      if (var9 >= 0 && var9 < this.trimmedMessages.size()) {
         GuiMessage.Line var10 = this.trimmedMessages.get(var9);
         GuiMessageTag var11 = var10.tag();
         if (var11 != null && this.hasSelectedMessageTag(var5, var10, var11)) {
            return var11;
         }
      }

      return null;
   }

   private boolean hasSelectedMessageTag(double param0, GuiMessage.Line param2, GuiMessageTag param3) {
      if (param0 < 0.0) {
         return true;
      }

      GuiMessageTag.Icon var5 = param3.icon();
      if (var5 == null) {
         return false;
      }

      int var6 = this.getTagIconLeft(param2);
      int var7 = var6 + var5.width;
      return param0 >= var6 && param0 <= var7;
   }

   private double screenToChatX(double param0) {
      return param0 / this.getScale() - 4.0;
   }

   private double screenToChatY(double param0) {
      double var3 = this.minecraft.getWindow().getGuiScaledHeight() - param0 - 40.0;
      return var3 / (this.getScale() * this.getLineHeight());
   }

   private int getMessageEndIndexAt(double param0, double param2) {
      int var5 = this.getMessageLineIndexAt(param0, param2);
      if (var5 == -1) {
         return -1;
      }

      while (var5 >= 0) {
         if (this.trimmedMessages.get(var5).endOfEntry()) {
            return var5;
         }

         var5--;
      }

      return var5;
   }

   private int getMessageLineIndexAt(double param0, double param2) {
      if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden()) {
         if (!(param0 < -4.0) && !(param0 > Mth.floor(this.getWidth() / this.getScale()))) {
            int var5 = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
            if (param2 >= 0.0 && param2 < var5) {
               int var6 = Mth.floor(param2 + this.chatScrollbarPos);
               if (var6 >= 0 && var6 < this.trimmedMessages.size()) {
                  return var6;
               }
            }

            return -1;
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private boolean isChatFocused() {
      return this.minecraft.screen instanceof ChatScreen;
   }

   public int getWidth() {
      return getWidth(this.minecraft.options.chatWidth().get());
   }

   public int getHeight() {
      return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get());
   }

   public double getScale() {
      return this.minecraft.options.chatScale().get();
   }

   public static int getWidth(double param0) {
      int var2 = 320;
      int var3 = 40;
      return Mth.floor(param0 * 280.0 + 40.0);
   }

   public static int getHeight(double param0) {
      int var2 = 180;
      int var3 = 20;
      return Mth.floor(param0 * 160.0 + 20.0);
   }

   public static double defaultUnfocusedPct() {
      int var0 = 180;
      int var1 = 20;
      return 70.0 / (getHeight(1.0) - 20);
   }

   public int getLinesPerPage() {
      return this.getHeight() / this.getLineHeight();
   }

   private int getLineHeight() {
      return (int)(9.0 * (this.minecraft.options.chatLineSpacing().get() + 1.0));
   }

   record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
   }
}
