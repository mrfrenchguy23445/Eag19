package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

public class ShareToLanScreen extends Screen {
   private static final int PORT_LOWER_BOUND = 1024;
   private static final int PORT_HIGHER_BOUND = 65535;
   private static final Component ALLOW_COMMANDS_LABEL = Component.translatable("selectWorld.allowCommands");
   private static final Component GAME_MODE_LABEL = Component.translatable("selectWorld.gameMode");
   private static final Component INFO_TEXT = Component.translatable("lanServer.otherPlayers");
   private static final Component PORT_INFO_TEXT = Component.translatable("lanServer.port");
   private static final Component PORT_UNAVAILABLE = Component.translatable("lanServer.port.unavailable.new", 1024, 65535);
   private static final Component INVALID_PORT = Component.translatable("lanServer.port.invalid.new", 1024, 65535);
   private static final int INVALID_PORT_COLOR = 16733525;
   private final Screen lastScreen;
   private GameType gameMode = GameType.SURVIVAL;
   private boolean commands;
   private int port = HttpUtil.getAvailablePort();
   @Nullable
   private EditBox portEdit;

   public ShareToLanScreen(Screen param0) {
      super(Component.translatable("lanServer.title"));
      this.lastScreen = param0;
   }

   @Override
   protected void init() {
      IntegratedServer var1 = this.minecraft.getSingleplayerServer();
      this.gameMode = var1.getDefaultGameType();
      this.commands = var1.getWorldData().getAllowCommands();
      this.addRenderableWidget(
         CycleButton.builder(GameType::getShortDisplayName)
            .withValues(GameType.SURVIVAL, GameType.SPECTATOR, GameType.CREATIVE, GameType.ADVENTURE)
            .withInitialValue(this.gameMode)
            .create(this.width / 2 - 155, 100, 150, 20, GAME_MODE_LABEL, (l_param0, l_param1) -> this.gameMode = l_param1)
      );
      this.addRenderableWidget(
         CycleButton.onOffBuilder(this.commands)
            .create(this.width / 2 + 5, 100, 150, 20, ALLOW_COMMANDS_LABEL, (l_param0, l_param1) -> this.commands = l_param1)
      );
      Button var2 = Button.builder(Component.translatable("lanServer.start"), l_param1 -> {
         this.minecraft.setScreen(null);
         Component l_var3;
         if (var1.publishServer(this.gameMode, this.commands, this.port)) {
            l_var3 = PublishCommand.getSuccessMessage(this.port);
         } else {
            l_var3 = Component.translatable("commands.publish.failed");
         }

         this.minecraft.gui.getChat().addMessage(l_var3);
         this.minecraft.updateTitle();
      }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build();
      this.portEdit = new EditBox(this.font, this.width / 2 - 75, 160, 150, 20, Component.translatable("lanServer.port"));
      this.portEdit.setResponder(l_param1 -> {
         Component l_var3 = this.tryParsePort(l_param1);
         this.portEdit.setHint(Component.literal(this.port + "").withStyle(ChatFormatting.DARK_GRAY));
         if (l_var3 == null) {
            this.portEdit.setTextColor(14737632);
            this.portEdit.setTooltip(null);
            var2.active = true;
         } else {
            this.portEdit.setTextColor(16733525);
            this.portEdit.setTooltip(Tooltip.create(l_var3));
            var2.active = false;
         }
      });
      this.portEdit.setHint(Component.literal(this.port + "").withStyle(ChatFormatting.DARK_GRAY));
      this.addRenderableWidget(this.portEdit);
      this.addRenderableWidget(var2);
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.minecraft.setScreen(this.lastScreen))
            .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
            .build()
      );
   }

   @Override
   public void tick() {
      super.tick();
      if (this.portEdit != null) {
         this.portEdit.tick();
      }
   }

   @Nullable
   private Component tryParsePort(String param0) {
      if (param0.isBlank()) {
         this.port = HttpUtil.getAvailablePort();
         return null;
      }

      try {
         this.port = Integer.parseInt(param0);
         if (this.port < 1024 || this.port > 65535) {
            return INVALID_PORT;
         } else {
            return !HttpUtil.isPortAvailable(this.port) ? PORT_UNAVAILABLE : null;
         }
      } catch (NumberFormatException var2) {
         this.port = HttpUtil.getAvailablePort();
         return INVALID_PORT;
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 50, 16777215);
      drawCenteredString(param0, this.font, INFO_TEXT, this.width / 2, 82, 16777215);
      drawCenteredString(param0, this.font, PORT_INFO_TEXT, this.width / 2, 142, 16777215);
      super.render(param0, param1, param2, param3);
   }
}
