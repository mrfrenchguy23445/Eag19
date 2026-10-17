package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;

public class SkinCustomizationScreen extends OptionsSubScreen {
   public SkinCustomizationScreen(Screen param0, Options param1) {
      super(param0, param1, Component.translatable("options.skinCustomisation.title"));
   }

   @Override
   protected void init() {
      int var1 = 0;

      for (PlayerModelPart var5 : PlayerModelPart.values()) {
         this.addRenderableWidget(
            CycleButton.onOffBuilder(this.options.isModelPartEnabled(var5))
               .create(
                  this.width / 2 - 155 + var1 % 2 * 160,
                  this.height / 6 + 24 * (var1 >> 1),
                  150,
                  20,
                  var5.getName(),
                  (l_param1, l_param2) -> this.options.toggleModelPart(var5, l_param2)
               )
         );
         var1++;
      }

      this.addRenderableWidget(
         this.options.mainHand().createButton(this.options, this.width / 2 - 155 + var1 % 2 * 160, this.height / 6 + 24 * (var1 >> 1), 150)
      );
      if (++var1 % 2 == 1) {
         var1++;
      }

      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.minecraft.setScreen(this.lastScreen))
            .bounds(this.width / 2 - 100, this.height / 6 + 24 * (var1 >> 1), 200, 20)
            .build()
      );
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 20, 16777215);
      super.render(param0, param1, param2, param3);
   }
}
