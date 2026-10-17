package net.minecraft.client.gui.screens.controls;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MouseSettingsScreen;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ControlsScreen extends OptionsSubScreen {
   private static final int ROW_SPACING = 24;

   public ControlsScreen(Screen param0, Options param1) {
      super(param0, param1, Component.translatable("controls.title"));
   }

   @Override
   protected void init() {
      super.init();
      int var1 = this.width / 2 - 155;
      int var2 = var1 + 160;
      int var3 = this.height / 6 - 12;
      this.addRenderableWidget(
         Button.builder(Component.translatable("options.mouse_settings"), l_param0 -> this.minecraft.setScreen(new MouseSettingsScreen(this, this.options)))
            .bounds(var1, var3, 150, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("controls.keybinds"), l_param0 -> this.minecraft.setScreen(new KeyBindsScreen(this, this.options)))
            .bounds(var2, var3, 150, 20)
            .build()
      );
      var3 += 24;
      this.addRenderableWidget(this.options.toggleCrouch().createButton(this.options, var1, var3, 150));
      this.addRenderableWidget(this.options.toggleSprint().createButton(this.options, var2, var3, 150));
      var3 += 24;
      this.addRenderableWidget(this.options.autoJump().createButton(this.options, var1, var3, 150));
      this.addRenderableWidget(this.options.operatorItemsTab().createButton(this.options, var2, var3, 150));
      var3 += 24;
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.minecraft.setScreen(this.lastScreen)).bounds(this.width / 2 - 100, var3, 200, 20).build()
      );
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 15, 16777215);
      super.render(param0, param1, param2, param3);
   }
}
