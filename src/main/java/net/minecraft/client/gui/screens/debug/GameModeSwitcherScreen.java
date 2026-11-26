package net.minecraft.client.gui.screens.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public class GameModeSwitcherScreen extends Screen {
   static final ResourceLocation GAMEMODE_SWITCHER_LOCATION = new ResourceLocation("textures/gui/container/gamemode_switcher.png");
   private static final int SPRITE_SHEET_WIDTH = 128;
   private static final int SPRITE_SHEET_HEIGHT = 128;
   private static final int SLOT_AREA = 26;
   private static final int SLOT_PADDING = 5;
   private static final int SLOT_AREA_PADDED = 31;
   private static final int HELP_TIPS_OFFSET_Y = 5;
   private static final int ALL_SLOTS_WIDTH = GameModeSwitcherScreen.GameModeIcon.values().length * 31 - 5;
   private static final Component SELECT_KEY = Component.translatable(
      "debug.gamemodes.select_next", Component.translatable("debug.gamemodes.press_f4").withStyle(ChatFormatting.AQUA)
   );
   private final Optional<GameModeSwitcherScreen.GameModeIcon> previousHovered;
   private Optional<GameModeSwitcherScreen.GameModeIcon> currentlyHovered = Optional.empty();
   private int firstMouseX;
   private int firstMouseY;
   private boolean setFirstMousePos;
   private final List<GameModeSwitcherScreen.GameModeSlot> slots = Lists.newArrayList();

   public GameModeSwitcherScreen() {
      super(GameNarrator.NO_TITLE);
      this.previousHovered = GameModeSwitcherScreen.GameModeIcon.getFromGameType(this.getDefaultSelected());
   }

   private GameType getDefaultSelected() {
      MultiPlayerGameMode var1 = Minecraft.getInstance().gameMode;
      GameType var2 = var1.getPreviousPlayerMode();
      if (var2 != null) {
         return var2;
      } else {
         return var1.getPlayerMode() == GameType.CREATIVE ? GameType.SURVIVAL : GameType.CREATIVE;
      }
   }

   @Override
   protected void init() {
      super.init();
      this.currentlyHovered = this.previousHovered.isPresent()
         ? this.previousHovered
         : GameModeSwitcherScreen.GameModeIcon.getFromGameType(this.minecraft.gameMode.getPlayerMode());

      for (int var1 = 0; var1 < GameModeSwitcherScreen.GameModeIcon.VALUES.length; var1++) {
         GameModeSwitcherScreen.GameModeIcon var2 = GameModeSwitcherScreen.GameModeIcon.VALUES[var1];
         this.slots.add(new GameModeSwitcherScreen.GameModeSlot(var2, this.width / 2 - ALL_SLOTS_WIDTH / 2 + var1 * 31, this.height / 2 - 31));
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      if (!this.checkToClose()) {
         param0.pushPose();
         RenderSystem.enableBlend();
         RenderSystem.setShaderTexture(0, GAMEMODE_SWITCHER_LOCATION);
         int var5 = this.width / 2 - 62;
         int var6 = this.height / 2 - 31 - 27;
         blit(param0, var5, var6, 0.0F, 0.0F, 125, 75, 128, 128);
         param0.popPose();
         super.render(param0, param1, param2, param3);
         this.currentlyHovered.ifPresent(l_param1 -> drawCenteredString(param0, this.font, l_param1.getName(), this.width / 2, this.height / 2 - 31 - 20, -1));
         drawCenteredString(param0, this.font, SELECT_KEY, this.width / 2, this.height / 2 + 5, 16777215);
         if (!this.setFirstMousePos) {
            this.firstMouseX = param1;
            this.firstMouseY = param2;
            this.setFirstMousePos = true;
         }

         boolean var7 = this.firstMouseX == param1 && this.firstMouseY == param2;

         for (GameModeSwitcherScreen.GameModeSlot var9 : this.slots) {
            var9.render(param0, param1, param2, param3);
            this.currentlyHovered.ifPresent(l_param1 -> var9.setSelected(l_param1 == var9.icon));
            if (!var7 && var9.isHoveredOrFocused()) {
               this.currentlyHovered = Optional.of(var9.icon);
            }
         }
      }
   }

   private void switchToHoveredGameMode() {
      switchToHoveredGameMode(this.minecraft, this.currentlyHovered);
   }

   private static void switchToHoveredGameMode(Minecraft param0, Optional<GameModeSwitcherScreen.GameModeIcon> param1) {
      if (param0.gameMode != null && param0.player != null && param1.isPresent()) {
         Optional<GameModeSwitcherScreen.GameModeIcon> var2 = GameModeSwitcherScreen.GameModeIcon.getFromGameType(param0.gameMode.getPlayerMode());
         GameModeSwitcherScreen.GameModeIcon var3 = param1.get();
         if (var2.isPresent() && param0.player.hasPermissions(2) && var3 != var2.get()) {
            param0.player.connection.sendUnsignedCommand(var3.getCommand());
         }
      }
   }

   private boolean checkToClose() {
      if (!InputConstants.isKeyDown(this.minecraft.getWindow().getWindow(), 292)) {
         this.switchToHoveredGameMode();
         this.minecraft.setScreen(null);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (param0 == 293 && this.currentlyHovered.isPresent()) {
         this.setFirstMousePos = false;
         this.currentlyHovered = this.currentlyHovered.get().getNext();
         return true;
      } else {
         return super.keyPressed(param0, param1, param2);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   enum GameModeIcon {
      CREATIVE(Component.translatable("gameMode.creative"), "gamemode creative", new ItemStack(Blocks.GRASS_BLOCK)),
      SURVIVAL(Component.translatable("gameMode.survival"), "gamemode survival", new ItemStack(Items.IRON_SWORD)),
      ADVENTURE(Component.translatable("gameMode.adventure"), "gamemode adventure", new ItemStack(Items.MAP)),
      SPECTATOR(Component.translatable("gameMode.spectator"), "gamemode spectator", new ItemStack(Items.ENDER_EYE));

      protected static final GameModeSwitcherScreen.GameModeIcon[] VALUES = values();
      private static final int ICON_AREA = 16;
      protected static final int ICON_TOP_LEFT = 5;
      final Component name;
      final String command;
      final ItemStack renderStack;

      GameModeIcon(Component param2, String param3, ItemStack param4) {
         this.name = param2;
         this.command = param3;
         this.renderStack = param4;
      }

      void drawIcon(PoseStack param0, ItemRenderer param1, int param2, int param3) {
         param1.renderAndDecorateItem(param0, this.renderStack, param2, param3);
      }

      Component getName() {
         return this.name;
      }

      String getCommand() {
         return this.command;
      }

      Optional<GameModeSwitcherScreen.GameModeIcon> getNext() {
         switch (this) {
            case CREATIVE:
               return Optional.of(SURVIVAL);
            case SURVIVAL:
               return Optional.of(ADVENTURE);
            case ADVENTURE:
               return Optional.of(SPECTATOR);
            default:
               return Optional.of(CREATIVE);
         }
      }

      static Optional<GameModeSwitcherScreen.GameModeIcon> getFromGameType(GameType param0) {
         switch (param0) {
            case SPECTATOR:
               return Optional.of(SPECTATOR);
            case SURVIVAL:
               return Optional.of(SURVIVAL);
            case CREATIVE:
               return Optional.of(CREATIVE);
            case ADVENTURE:
               return Optional.of(ADVENTURE);
            default:
               return Optional.empty();
         }
      }
   }

   public class GameModeSlot extends AbstractWidget {
      final GameModeSwitcherScreen.GameModeIcon icon;
      private boolean isSelected;

      public GameModeSlot(GameModeSwitcherScreen.GameModeIcon param1, int param2, int param3) {
         super(param2, param3, 26, 26, param1.getName());
         this.icon = param1;
      }

      @Override
      public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
         Minecraft var5 = Minecraft.getInstance();
         this.drawSlot(param0, var5.getTextureManager());
         this.icon.drawIcon(param0, GameModeSwitcherScreen.this.itemRenderer, this.getX() + 5, this.getY() + 5);
         if (this.isSelected) {
            this.drawSelection(param0, var5.getTextureManager());
         }
      }

      @Override
      public void updateWidgetNarration(NarrationElementOutput param0) {
         this.defaultButtonNarrationText(param0);
      }

      @Override
      public boolean isHoveredOrFocused() {
         return super.isHoveredOrFocused() || this.isSelected;
      }

      public void setSelected(boolean param0) {
         this.isSelected = param0;
      }

      private void drawSlot(PoseStack param0, TextureManager param1) {
         RenderSystem.setShaderTexture(0, GameModeSwitcherScreen.GAMEMODE_SWITCHER_LOCATION);
         param0.pushPose();
         param0.translate(this.getX(), this.getY(), 0.0F);
         blit(param0, 0, 0, 0.0F, 75.0F, 26, 26, 128, 128);
         param0.popPose();
      }

      private void drawSelection(PoseStack param0, TextureManager param1) {
         RenderSystem.setShaderTexture(0, GameModeSwitcherScreen.GAMEMODE_SWITCHER_LOCATION);
         param0.pushPose();
         param0.translate(this.getX(), this.getY(), 0.0F);
         blit(param0, 0, 0, 26.0F, 75.0F, 26, 26, 128, 128);
         param0.popPose();
      }
   }
}
