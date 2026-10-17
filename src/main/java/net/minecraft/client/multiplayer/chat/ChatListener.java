package net.minecraft.client.multiplayer.chat;

import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.StringDecomposer;
import org.apache.commons.lang3.StringUtils;

public class ChatListener {
   private final Minecraft minecraft;
   private final Deque<ChatListener.Message> delayedMessageQueue = Queues.newArrayDeque();
   private long messageDelay;
   private long previousMessageTime;

   public ChatListener(Minecraft param0) {
      this.minecraft = param0;
   }

   public void tick() {
      if (this.messageDelay != 0L) {
         if (Util.getMillis() >= this.previousMessageTime + this.messageDelay) {
            ChatListener.Message var1 = this.delayedMessageQueue.poll();

            while (var1 != null && !var1.accept()) {
               var1 = this.delayedMessageQueue.poll();
            }
         }
      }
   }

   public void setMessageDelay(double param0) {
      long var3 = (long)(param0 * 1000.0);
      if (var3 == 0L && this.messageDelay > 0L) {
         this.delayedMessageQueue.forEach(ChatListener.Message::accept);
         this.delayedMessageQueue.clear();
      }

      this.messageDelay = var3;
   }

   public void acceptNextDelayedMessage() {
      this.delayedMessageQueue.remove().accept();
   }

   public long queueSize() {
      return this.delayedMessageQueue.size();
   }

   public void clearQueue() {
      this.delayedMessageQueue.forEach(ChatListener.Message::accept);
      this.delayedMessageQueue.clear();
   }

   public boolean removeFromDelayedMessageQueue(MessageSignature param0) {
      return this.delayedMessageQueue.removeIf(l_param1 -> param0.equals(l_param1.signature()));
   }

   private boolean willDelayMessages() {
      return this.messageDelay > 0L && Util.getMillis() < this.previousMessageTime + this.messageDelay;
   }

   private void handleMessage(@Nullable MessageSignature param0, BooleanSupplier param1) {
      if (this.willDelayMessages()) {
         this.delayedMessageQueue.add(new ChatListener.Message(param0, param1));
      } else {
         param1.getAsBoolean();
      }
   }

   public void handlePlayerChatMessage(PlayerChatMessage param0, GameProfile param1, ChatType.Bound param2) {
      boolean var4 = this.minecraft.options.onlyShowSecureChat().get();
      PlayerChatMessage var5 = var4 ? param0.removeUnsignedContent() : param0;
      Component var6 = param2.decorate(var5.decoratedContent());
      Instant var7 = Instant.now();
      this.handleMessage(param0.signature(), () -> {
         boolean l_var7 = this.showMessageToPlayer(param2, param0, var6, param1, var4, var7);
         ClientPacketListener l_var8 = this.minecraft.getConnection();
         if (l_var8 != null) {
            l_var8.markMessageAsProcessed(param0, l_var7);
         }

         return l_var7;
      });
   }

   public void handleDisguisedChatMessage(Component param0, ChatType.Bound param1) {
      Instant var3 = Instant.now();
      this.handleMessage(null, () -> {
         Component l_var4 = param1.decorate(param0);
         this.minecraft.gui.getChat().addMessage(l_var4);
         this.narrateChatMessage(param1, param0);
         this.logSystemMessage(l_var4, var3);
         this.previousMessageTime = Util.getMillis();
         return true;
      });
   }

   private boolean showMessageToPlayer(ChatType.Bound param0, PlayerChatMessage param1, Component param2, GameProfile param3, boolean param4, Instant param5) {
      ChatTrustLevel var7 = this.evaluateTrustLevel(param1, param2, param5);
      if (param4 && var7.isNotSecure()) {
         return false;
      }

      if (!this.minecraft.isBlocked(param1.sender()) && !param1.isFullyFiltered()) {
         GuiMessageTag var8 = var7.createTag(param1);
         MessageSignature var9 = param1.signature();
         FilterMask var10 = param1.filterMask();
         if (var10.isEmpty()) {
            this.minecraft.gui.getChat().addMessage(param2, var9, var8);
            this.narrateChatMessage(param0, param1.decoratedContent());
         } else {
            Component var11 = var10.applyWithFormatting(param1.signedContent());
            if (var11 != null) {
               this.minecraft.gui.getChat().addMessage(param0.decorate(var11), var9, var8);
               this.narrateChatMessage(param0, var11);
            }
         }

         this.logPlayerMessage(param1, param0, param3, var7);
         this.previousMessageTime = Util.getMillis();
         return true;
      } else {
         return false;
      }
   }

   private void narrateChatMessage(ChatType.Bound param0, Component param1) {
      this.minecraft.getNarrator().sayChat(param0.decorateNarration(param1));
   }

   private ChatTrustLevel evaluateTrustLevel(PlayerChatMessage param0, Component param1, Instant param2) {
      return this.isSenderLocalPlayer(param0.sender()) ? ChatTrustLevel.SECURE : ChatTrustLevel.evaluate(param0, param1, param2);
   }

   private void logPlayerMessage(PlayerChatMessage param0, ChatType.Bound param1, GameProfile param2, ChatTrustLevel param3) {
      ChatLog var5 = this.minecraft.getReportingContext().chatLog();
      var5.push(LoggedChatMessage.player(param2, param0, param3));
   }

   private void logSystemMessage(Component param0, Instant param1) {
      ChatLog var3 = this.minecraft.getReportingContext().chatLog();
      var3.push(LoggedChatMessage.system(param0, param1));
   }

   public void handleSystemMessage(Component param0, boolean param1) {
      if (!this.minecraft.options.hideMatchedNames().get() || !this.minecraft.isBlocked(this.guessChatUUID(param0))) {
         if (param1) {
            this.minecraft.gui.setOverlayMessage(param0, false);
         } else {
            this.minecraft.gui.getChat().addMessage(param0);
            this.logSystemMessage(param0, Instant.now());
         }

         this.minecraft.getNarrator().say(param0);
      }
   }

   private UUID guessChatUUID(Component param0) {
      String var2 = StringDecomposer.getPlainText(param0);
      String var3 = StringUtils.substringBetween(var2, "<", ">");
      return var3 == null ? Util.NIL_UUID : this.minecraft.getPlayerSocialManager().getDiscoveredUUID(var3);
   }

   private boolean isSenderLocalPlayer(UUID param0) {
      if (this.minecraft.isLocalServer() && this.minecraft.player != null) {
         UUID var2 = this.minecraft.player.getGameProfile().getId();
         return var2.equals(param0);
      } else {
         return false;
      }
   }

   record Message(@Nullable MessageSignature signature, BooleanSupplier handler) {
      public boolean accept() {
         return this.handler.getAsBoolean();
      }
   }
}
