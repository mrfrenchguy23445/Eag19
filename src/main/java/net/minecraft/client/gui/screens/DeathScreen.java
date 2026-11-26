package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class DeathScreen extends Screen {
   private int delayTicker;
   private final Component causeOfDeath;
   private final boolean hardcore;
   private Component deathScore;
   private final List<Button> exitButtons = Lists.newArrayList();
   @Nullable
   private Button exitToTitleButton;

   public DeathScreen(@Nullable Component param0, boolean param1) {
      super(Component.translatable(param1 ? "deathScreen.title.hardcore" : "deathScreen.title"));
      this.causeOfDeath = param0;
      this.hardcore = param1;
   }

   @Override
   protected void init() {
      this.delayTicker = 0;
      this.exitButtons.clear();
      Component var1 = this.hardcore ? Component.translatable("deathScreen.spectate") : Component.translatable("deathScreen.respawn");
      this.exitButtons.add(this.addRenderableWidget(Button.builder(var1, l_param0 -> {
         this.minecraft.player.respawn();
         l_param0.active = false;
      }).bounds(this.width / 2 - 100, this.height / 4 + 72, 200, 20).build()));
      this.exitToTitleButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("deathScreen.titleScreen"),
               l_param0 -> this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::handleExitToTitleScreen, true)
            )
            .bounds(this.width / 2 - 100, this.height / 4 + 96, 200, 20)
            .build()
      );
      this.exitButtons.add(this.exitToTitleButton);
      this.setButtonsActive(false);
      this.deathScore = Component.translatable("deathScreen.score")
         .append(": ")
         .append(Component.literal(Integer.toString(this.minecraft.player.getScore())).withStyle(ChatFormatting.YELLOW));
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void handleExitToTitleScreen() {
      if (this.hardcore) {
         this.exitToTitleScreen();
      } else {
         ConfirmScreen var1 = new DeathScreen.TitleConfirmScreen(
            l_param0 -> {
               if (l_param0) {
                  this.exitToTitleScreen();
               } else {
                  this.minecraft.player.respawn();
                  this.minecraft.setScreen(null);
               }
            },
            Component.translatable("deathScreen.quit.confirm"),
            CommonComponents.EMPTY,
            Component.translatable("deathScreen.titleScreen"),
            Component.translatable("deathScreen.respawn")
         );
         this.minecraft.setScreen(var1);
         var1.setDelay(20);
      }
   }

   private void exitToTitleScreen() {
      if (this.minecraft.level != null) {
         this.minecraft.level.disconnect();
      }

      this.minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
      this.minecraft.setScreen(new TitleScreen());
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      fillGradient(param0, 0, 0, this.width, this.height, 1615855616, -1602211792);
      param0.pushPose();
      param0.scale(2.0F, 2.0F, 2.0F);
      drawCenteredString(param0, this.font, this.title, this.width / 2 / 2, 30, 16777215);
      param0.popPose();
      if (this.causeOfDeath != null) {
         drawCenteredString(param0, this.font, this.causeOfDeath, this.width / 2, 85, 16777215);
      }

      drawCenteredString(param0, this.font, this.deathScore, this.width / 2, 100, 16777215);
      if (this.causeOfDeath != null && param2 > 85 && param2 < 85 + 9) {
         Style var5 = this.getClickedComponentStyleAt(param1);
         this.renderComponentHoverEffect(param0, var5, param1, param2);
      }

      super.render(param0, param1, param2, param3);
      if (this.exitToTitleButton != null && this.minecraft.getReportingContext().hasDraftReport()) {
         RenderSystem.setShaderTexture(0, AbstractWidget.WIDGETS_LOCATION);
         blit(param0, this.exitToTitleButton.getX() + this.exitToTitleButton.getWidth() - 17, this.exitToTitleButton.getY() + 3, 182, 24, 15, 15);
      }
   }

   @Nullable
   private Style getClickedComponentStyleAt(int param0) {
      if (this.causeOfDeath == null) {
         return null;
      }

      int var2 = this.minecraft.font.width(this.causeOfDeath);
      int var3 = this.width / 2 - var2 / 2;
      int var4 = this.width / 2 + var2 / 2;
      return param0 >= var3 && param0 <= var4 ? this.minecraft.font.getSplitter().componentStyleAtWidth(this.causeOfDeath, param0 - var3) : null;
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (this.causeOfDeath != null && param2 > 85.0 && param2 < 85 + 9) {
         Style var6 = this.getClickedComponentStyleAt((int)param0);
         if (var6 != null && var6.getClickEvent() != null && var6.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL) {
            this.handleComponentClicked(var6);
            return false;
         }
      }

      return super.mouseClicked(param0, param2, param4);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void tick() {
      super.tick();
      this.delayTicker++;
      if (this.delayTicker == 20) {
         this.setButtonsActive(true);
      }
   }

   private void setButtonsActive(boolean param0) {
      for (Button var3 : this.exitButtons) {
         var3.active = param0;
      }
   }

   public static class TitleConfirmScreen extends ConfirmScreen {
      public TitleConfirmScreen(BooleanConsumer param0, Component param1, Component param2, Component param3, Component param4) {
         super(param0, param1, param2, param3, param4);
      }
   }
}
