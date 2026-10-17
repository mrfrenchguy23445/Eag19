package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DemoIntroScreen extends Screen {
   private static final ResourceLocation DEMO_BACKGROUND_LOCATION = new ResourceLocation("textures/gui/demo_background.png");
   private MultiLineLabel movementMessage = MultiLineLabel.EMPTY;
   private MultiLineLabel durationMessage = MultiLineLabel.EMPTY;

   public DemoIntroScreen() {
      super(Component.translatable("demo.help.title"));
   }

   @Override
   protected void init() {
      int var1 = -16;
      this.addRenderableWidget(Button.builder(Component.translatable("demo.help.buy"), l_param0 -> {
         l_param0.active = false;
         Util.getPlatform().openUri("https://aka.ms/BuyMinecraftJava");
      }).bounds(this.width / 2 - 116, this.height / 2 + 62 + -16, 114, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("demo.help.later"), l_param0 -> {
         this.minecraft.setScreen(null);
         this.minecraft.mouseHandler.grabMouse();
      }).bounds(this.width / 2 + 2, this.height / 2 + 62 + -16, 114, 20).build());
      Options var2 = this.minecraft.options;
      this.movementMessage = MultiLineLabel.create(
         this.font,
         Component.translatable(
            "demo.help.movementShort",
            var2.keyUp.getTranslatedKeyMessage(),
            var2.keyLeft.getTranslatedKeyMessage(),
            var2.keyDown.getTranslatedKeyMessage(),
            var2.keyRight.getTranslatedKeyMessage()
         ),
         Component.translatable("demo.help.movementMouse"),
         Component.translatable("demo.help.jump", var2.keyJump.getTranslatedKeyMessage()),
         Component.translatable("demo.help.inventory", var2.keyInventory.getTranslatedKeyMessage())
      );
      this.durationMessage = MultiLineLabel.create(this.font, Component.translatable("demo.help.fullWrapped"), 218);
   }

   @Override
   public void renderBackground(PoseStack param0) {
      super.renderBackground(param0);
      RenderSystem.setShaderTexture(0, DEMO_BACKGROUND_LOCATION);
      int var2 = (this.width - 248) / 2;
      int var3 = (this.height - 166) / 2;
      blit(param0, var2, var3, 0, 0, 248, 166);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      int var5 = (this.width - 248) / 2 + 10;
      int var6 = (this.height - 166) / 2 + 8;
      this.font.draw(param0, this.title, var5, var6, 2039583);
      var6 = this.movementMessage.renderLeftAlignedNoShadow(param0, var5, var6 + 12, 12, 5197647);
      this.durationMessage.renderLeftAlignedNoShadow(param0, var5, var6 + 20, 9, 2039583);
      super.render(param0, param1, param2, param3);
   }
}
