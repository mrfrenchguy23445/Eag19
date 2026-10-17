package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.realmsclient.RealmsMainScreen;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.network.chat.Component;

public class PauseScreen extends Screen {
   private static final int COLUMNS = 2;
   private static final int MENU_PADDING_TOP = 50;
   private static final int BUTTON_PADDING = 4;
   private static final int BUTTON_WIDTH_FULL = 204;
   private static final int BUTTON_WIDTH_HALF = 98;
   private static final Component RETURN_TO_GAME = Component.translatable("menu.returnToGame");
   private static final Component ADVANCEMENTS = Component.translatable("gui.advancements");
   private static final Component STATS = Component.translatable("gui.stats");
   private static final Component SEND_FEEDBACK = Component.translatable("menu.sendFeedback");
   private static final Component REPORT_BUGS = Component.translatable("menu.reportBugs");
   private static final Component OPTIONS = Component.translatable("menu.options");
   private static final Component SHARE_TO_LAN = Component.translatable("menu.shareToLan");
   private static final Component PLAYER_REPORTING = Component.translatable("menu.playerReporting");
   private static final Component RETURN_TO_MENU = Component.translatable("menu.returnToMenu");
   private static final Component DISCONNECT = Component.translatable("menu.disconnect");
   private static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");
   private static final Component GAME = Component.translatable("menu.game");
   private static final Component PAUSED = Component.translatable("menu.paused");
   private final boolean showPauseMenu;
   @Nullable
   private Button disconnectButton;

   public PauseScreen(boolean param0) {
      super(param0 ? GAME : PAUSED);
      this.showPauseMenu = param0;
   }

   @Override
   protected void init() {
      if (this.showPauseMenu) {
         this.createPauseMenu();
      }

      this.addRenderableWidget(new StringWidget(0, this.showPauseMenu ? 40 : 10, this.width, 9, this.title, this.font));
   }

   private void createPauseMenu() {
      GridLayout var1 = new GridLayout();
      var1.defaultCellSetting().padding(4, 4, 4, 0);
      GridLayout.RowHelper var2 = var1.createRowHelper(2);
      var2.addChild(Button.builder(RETURN_TO_GAME, l_param0 -> {
         this.minecraft.setScreen(null);
         this.minecraft.mouseHandler.grabMouse();
      }).width(204).build(), 2, var1.newCellSettings().paddingTop(50));
      var2.addChild(this.openScreenButton(ADVANCEMENTS, () -> new AdvancementsScreen(this.minecraft.player.connection.getAdvancements())));
      var2.addChild(this.openScreenButton(STATS, () -> new StatsScreen(this, this.minecraft.player.getStats())));
      var2.addChild(
         this.openLinkButton(
            SEND_FEEDBACK, SharedConstants.getCurrentVersion().isStable() ? "https://aka.ms/javafeedback?ref=game" : "https://aka.ms/snapshotfeedback?ref=game"
         )
      );
      var2.addChild(this.openLinkButton(REPORT_BUGS, "https://aka.ms/snapshotbugs?ref=game")).active = !SharedConstants.getCurrentVersion()
         .getDataVersion()
         .isSideSeries();
      var2.addChild(this.openScreenButton(OPTIONS, () -> new OptionsScreen(this, this.minecraft.options)));
      if (this.minecraft.hasSingleplayerServer() && !this.minecraft.getSingleplayerServer().isPublished()) {
         var2.addChild(this.openScreenButton(SHARE_TO_LAN, () -> new ShareToLanScreen(this)));
      } else {
         var2.addChild(this.openScreenButton(PLAYER_REPORTING, SocialInteractionsScreen::new));
      }

      Component var3 = this.minecraft.isLocalServer() ? RETURN_TO_MENU : DISCONNECT;
      this.disconnectButton = var2.addChild(Button.builder(var3, l_param0 -> {
         l_param0.active = false;
         this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::onDisconnect, true);
      }).width(204).build(), 2);
      var1.arrangeElements();
      FrameLayout.alignInRectangle(var1, 0, 0, this.width, this.height, 0.5F, 0.25F);
      var1.visitWidgets(this::addRenderableWidget);
   }

   private void onDisconnect() {
      boolean var1 = this.minecraft.isLocalServer();
      this.minecraft.level.disconnect();
      if (var1) {
         this.minecraft.clearLevel(new GenericDirtMessageScreen(SAVING_LEVEL));
      } else {
         this.minecraft.clearLevel();
      }

      TitleScreen var3 = new TitleScreen();
      // Port note (wasm14): the "connected to Realms" branch that reopened RealmsMainScreen is
      // gone with Realms support; see TitleScreen.createNormalMenuOptions.
      if (var1) {
         this.minecraft.setScreen(var3);
      } else {
         this.minecraft.setScreen(new JoinMultiplayerScreen(var3));
      }
   }

   @Override
   public void tick() {
      super.tick();
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      if (this.showPauseMenu) {
         this.renderBackground(param0);
      }

      super.render(param0, param1, param2, param3);
      if (this.showPauseMenu && this.minecraft != null && this.minecraft.getReportingContext().hasDraftReport() && this.disconnectButton != null) {
         RenderSystem.setShaderTexture(0, AbstractWidget.WIDGETS_LOCATION);
         blit(param0, this.disconnectButton.getX() + this.disconnectButton.getWidth() - 17, this.disconnectButton.getY() + 3, 182, 24, 15, 15);
      }
   }

   private Button openScreenButton(Component param0, Supplier<Screen> param1) {
      return Button.builder(param0, l_param1 -> this.minecraft.setScreen(param1.get())).width(98).build();
   }

   private Button openLinkButton(Component param0, String param1) {
      return this.openScreenButton(param0, () -> new ConfirmLinkScreen(l2_param1 -> {
         if (l2_param1) {
            Util.getPlatform().openUri(param1);
         }

         this.minecraft.setScreen(this);
      }, param1, true));
   }
}
