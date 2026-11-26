package net.minecraft.client.gui.screens.social;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class SocialInteractionsScreen extends Screen {
   protected static final ResourceLocation SOCIAL_INTERACTIONS_LOCATION = new ResourceLocation("textures/gui/social_interactions.png");
   private static final Component TAB_ALL = Component.translatable("gui.socialInteractions.tab_all");
   private static final Component TAB_HIDDEN = Component.translatable("gui.socialInteractions.tab_hidden");
   private static final Component TAB_BLOCKED = Component.translatable("gui.socialInteractions.tab_blocked");
   private static final Component TAB_ALL_SELECTED = TAB_ALL.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component TAB_HIDDEN_SELECTED = TAB_HIDDEN.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component TAB_BLOCKED_SELECTED = TAB_BLOCKED.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component SEARCH_HINT = Component.translatable("gui.socialInteractions.search_hint")
      .withStyle(ChatFormatting.ITALIC)
      .withStyle(ChatFormatting.GRAY);
   static final Component EMPTY_SEARCH = Component.translatable("gui.socialInteractions.search_empty").withStyle(ChatFormatting.GRAY);
   private static final Component EMPTY_HIDDEN = Component.translatable("gui.socialInteractions.empty_hidden").withStyle(ChatFormatting.GRAY);
   private static final Component EMPTY_BLOCKED = Component.translatable("gui.socialInteractions.empty_blocked").withStyle(ChatFormatting.GRAY);
   private static final Component BLOCKING_HINT = Component.translatable("gui.socialInteractions.blocking_hint");
   private static final int BG_BORDER_SIZE = 8;
   private static final int BG_WIDTH = 236;
   private static final int SEARCH_HEIGHT = 16;
   private static final int MARGIN_Y = 64;
   public static final int SEARCH_START = 72;
   public static final int LIST_START = 88;
   private static final int IMAGE_WIDTH = 238;
   private static final int BUTTON_HEIGHT = 20;
   private static final int ITEM_HEIGHT = 36;
   SocialInteractionsPlayerList socialInteractionsPlayerList;
   EditBox searchBox;
   private String lastSearch = "";
   private SocialInteractionsScreen.Page page = SocialInteractionsScreen.Page.ALL;
   private Button allButton;
   private Button hiddenButton;
   private Button blockedButton;
   private Button blockingHintButton;
   @Nullable
   private Component serverLabel;
   private int playerCount;
   private boolean initialized;

   public SocialInteractionsScreen() {
      super(Component.translatable("gui.socialInteractions.title"));
      this.updateServerLabel(Minecraft.getInstance());
   }

   private int windowHeight() {
      return Math.max(52, this.height - 128 - 16);
   }

   private int listEnd() {
      return 80 + this.windowHeight() - 8;
   }

   private int marginX() {
      return (this.width - 238) / 2;
   }

   @Override
   public Component getNarrationMessage() {
      return this.serverLabel != null ? CommonComponents.joinForNarration(super.getNarrationMessage(), this.serverLabel) : super.getNarrationMessage();
   }

   @Override
   public void tick() {
      super.tick();
      this.searchBox.tick();
   }

   @Override
   protected void init() {
      if (this.initialized) {
         this.socialInteractionsPlayerList.updateSize(this.width, this.height, 88, this.listEnd());
      } else {
         this.socialInteractionsPlayerList = new SocialInteractionsPlayerList(this, this.minecraft, this.width, this.height, 88, this.listEnd(), 36);
      }

      int var1 = this.socialInteractionsPlayerList.getRowWidth() / 3;
      int var2 = this.socialInteractionsPlayerList.getRowLeft();
      int var3 = this.socialInteractionsPlayerList.getRowRight();
      int var4 = this.font.width(BLOCKING_HINT) + 40;
      int var5 = 64 + this.windowHeight();
      int var6 = (this.width - var4) / 2 + 3;
      this.allButton = this.addRenderableWidget(
         Button.builder(TAB_ALL, l_param0 -> this.showPage(SocialInteractionsScreen.Page.ALL)).bounds(var2, 45, var1, 20).build()
      );
      this.hiddenButton = this.addRenderableWidget(
         Button.builder(TAB_HIDDEN, l_param0 -> this.showPage(SocialInteractionsScreen.Page.HIDDEN)).bounds((var2 + var3 - var1) / 2 + 1, 45, var1, 20).build()
      );
      this.blockedButton = this.addRenderableWidget(
         Button.builder(TAB_BLOCKED, l_param0 -> this.showPage(SocialInteractionsScreen.Page.BLOCKED)).bounds(var3 - var1 + 1, 45, var1, 20).build()
      );
      String var7 = this.searchBox != null ? this.searchBox.getValue() : "";
      this.searchBox = new EditBox(this.font, this.marginX() + 29, 75, 198, 13, SEARCH_HINT) {
         @Override
         protected MutableComponent createNarrationMessage() {
            return !SocialInteractionsScreen.this.searchBox.getValue().isEmpty() && SocialInteractionsScreen.this.socialInteractionsPlayerList.isEmpty()
               ? super.createNarrationMessage().append(", ").append(SocialInteractionsScreen.EMPTY_SEARCH)
               : super.createNarrationMessage();
         }
      };
      this.searchBox.setMaxLength(16);
      this.searchBox.setVisible(true);
      this.searchBox.setTextColor(16777215);
      this.searchBox.setValue(var7);
      this.searchBox.setHint(SEARCH_HINT);
      this.searchBox.setResponder(this::checkSearchStringUpdate);
      this.addWidget(this.searchBox);
      this.addWidget(this.socialInteractionsPlayerList);
      this.blockingHintButton = this.addRenderableWidget(
         Button.builder(BLOCKING_HINT, l_param0 -> this.minecraft.setScreen(new ConfirmLinkScreen(l2_param0 -> {
            if (l2_param0) {
               Util.getPlatform().openUri("https://aka.ms/javablocking");
            }

            this.minecraft.setScreen(this);
         }, "https://aka.ms/javablocking", true))).bounds(var6, var5, var4, 20).build()
      );
      this.initialized = true;
      this.showPage(this.page);
   }

   private void showPage(SocialInteractionsScreen.Page param0) {
      this.page = param0;
      this.allButton.setMessage(TAB_ALL);
      this.hiddenButton.setMessage(TAB_HIDDEN);
      this.blockedButton.setMessage(TAB_BLOCKED);
      boolean var2 = false;
      switch (param0) {
         case ALL: {
            this.allButton.setMessage(TAB_ALL_SELECTED);
            Collection<UUID> var3 = this.minecraft.player.connection.getOnlinePlayerIds();
            this.socialInteractionsPlayerList.updatePlayerList(var3, this.socialInteractionsPlayerList.getScrollAmount(), true);
            break;
         }
         case HIDDEN: {
            this.hiddenButton.setMessage(TAB_HIDDEN_SELECTED);
            Set<UUID> var3 = this.minecraft.getPlayerSocialManager().getHiddenPlayers();
            var2 = var3.isEmpty();
            this.socialInteractionsPlayerList.updatePlayerList(var3, this.socialInteractionsPlayerList.getScrollAmount(), false);
            break;
         }
         case BLOCKED: {
            this.blockedButton.setMessage(TAB_BLOCKED_SELECTED);
            PlayerSocialManager var3 = this.minecraft.getPlayerSocialManager();
            Set<UUID> var4 = this.minecraft.player.connection.getOnlinePlayerIds().stream().filter(var3::isBlocked).collect(Collectors.toSet());
            var2 = var4.isEmpty();
            this.socialInteractionsPlayerList.updatePlayerList(var4, this.socialInteractionsPlayerList.getScrollAmount(), false);
         }
      }

      GameNarrator var3 = this.minecraft.getNarrator();
      if (!this.searchBox.getValue().isEmpty() && this.socialInteractionsPlayerList.isEmpty() && !this.searchBox.isFocused()) {
         var3.sayNow(EMPTY_SEARCH);
      } else if (var2) {
         if (param0 == SocialInteractionsScreen.Page.HIDDEN) {
            var3.sayNow(EMPTY_HIDDEN);
         } else if (param0 == SocialInteractionsScreen.Page.BLOCKED) {
            var3.sayNow(EMPTY_BLOCKED);
         }
      }
   }

   @Override
   public void renderBackground(PoseStack param0) {
      int var2 = this.marginX() + 3;
      super.renderBackground(param0);
      RenderSystem.setShaderTexture(0, SOCIAL_INTERACTIONS_LOCATION);
      blitNineSliced(param0, var2, 64, 236, this.windowHeight() + 16, 8, 236, 34, 1, 1);
      blit(param0, var2 + 10, 76, 243, 1, 12, 12);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.updateServerLabel(this.minecraft);
      this.renderBackground(param0);
      if (this.serverLabel != null) {
         drawString(param0, this.minecraft.font, this.serverLabel, this.marginX() + 8, 35, -1);
      }

      if (!this.socialInteractionsPlayerList.isEmpty()) {
         this.socialInteractionsPlayerList.render(param0, param1, param2, param3);
      } else if (!this.searchBox.getValue().isEmpty()) {
         drawCenteredString(param0, this.minecraft.font, EMPTY_SEARCH, this.width / 2, (72 + this.listEnd()) / 2, -1);
      } else if (this.page == SocialInteractionsScreen.Page.HIDDEN) {
         drawCenteredString(param0, this.minecraft.font, EMPTY_HIDDEN, this.width / 2, (72 + this.listEnd()) / 2, -1);
      } else if (this.page == SocialInteractionsScreen.Page.BLOCKED) {
         drawCenteredString(param0, this.minecraft.font, EMPTY_BLOCKED, this.width / 2, (72 + this.listEnd()) / 2, -1);
      }

      this.searchBox.render(param0, param1, param2, param3);
      this.blockingHintButton.visible = this.page == SocialInteractionsScreen.Page.BLOCKED;
      super.render(param0, param1, param2, param3);
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (this.searchBox.isFocused()) {
         this.searchBox.mouseClicked(param0, param2, param4);
      }

      return super.mouseClicked(param0, param2, param4) || this.socialInteractionsPlayerList.mouseClicked(param0, param2, param4);
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (!this.searchBox.isFocused() && this.minecraft.options.keySocialInteractions.matches(param0, param1)) {
         this.minecraft.setScreen(null);
         return true;
      } else {
         return super.keyPressed(param0, param1, param2);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private void checkSearchStringUpdate(String param0) {
      param0 = param0.toLowerCase(Locale.ROOT);
      if (!param0.equals(this.lastSearch)) {
         this.socialInteractionsPlayerList.setFilter(param0);
         this.lastSearch = param0;
         this.showPage(this.page);
      }
   }

   private void updateServerLabel(Minecraft param0) {
      int var2 = param0.getConnection().getOnlinePlayers().size();
      if (this.playerCount != var2) {
         String var3 = "";
         ServerData var4 = param0.getCurrentServer();
         if (param0.isLocalServer()) {
            var3 = param0.getSingleplayerServer().getMotd();
         } else if (var4 != null) {
            var3 = var4.name;
         }

         if (var2 > 1) {
            this.serverLabel = Component.translatable("gui.socialInteractions.server_label.multiple", var3, var2);
         } else {
            this.serverLabel = Component.translatable("gui.socialInteractions.server_label.single", var3, var2);
         }

         this.playerCount = var2;
      }
   }

   public void onAddPlayer(PlayerInfo param0) {
      this.socialInteractionsPlayerList.addPlayer(param0, this.page);
   }

   public void onRemovePlayer(UUID param0) {
      this.socialInteractionsPlayerList.removePlayer(param0);
   }

   public enum Page {
      ALL,
      HIDDEN,
      BLOCKED;
   }
}
