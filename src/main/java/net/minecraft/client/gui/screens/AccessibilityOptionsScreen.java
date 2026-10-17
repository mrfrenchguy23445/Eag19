package net.minecraft.client.gui.screens;

import net.minecraft.Util;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class AccessibilityOptionsScreen extends SimpleOptionsSubScreen {
   private static OptionInstance<?>[] options(Options param0) {
      return new OptionInstance[]{
         param0.narrator(),
         param0.showSubtitles(),
         param0.highContrast(),
         param0.autoJump(),
         param0.textBackgroundOpacity(),
         param0.backgroundForChatOnly(),
         param0.chatOpacity(),
         param0.chatLineSpacing(),
         param0.chatDelay(),
         param0.notificationDisplayTime(),
         param0.toggleCrouch(),
         param0.toggleSprint(),
         param0.screenEffectScale(),
         param0.fovEffectScale(),
         param0.darknessEffectScale(),
         param0.damageTiltStrength(),
         param0.glintSpeed(),
         param0.glintStrength(),
         param0.hideLightningFlash(),
         param0.darkMojangStudiosBackground(),
         param0.panoramaSpeed()
      };
   }

   public AccessibilityOptionsScreen(Screen param0, Options param1) {
      super(param0, param1, Component.translatable("options.accessibility.title"), options(param1));
   }

   @Override
   protected void init() {
      super.init();
      AbstractWidget var1 = this.list.findOption(this.options.highContrast());
      if (var1 != null && !this.minecraft.getResourcePackRepository().getAvailableIds().contains("high_contrast")) {
         var1.active = false;
         var1.setTooltip(Tooltip.create(Component.translatable("options.accessibility.high_contrast.error.tooltip")));
      }
   }

   @Override
   protected void createFooter() {
      this.addRenderableWidget(
         Button.builder(Component.translatable("options.accessibility.link"), l_param0 -> this.minecraft.setScreen(new ConfirmLinkScreen(l2_param0 -> {
            if (l2_param0) {
               Util.getPlatform().openUri("https://aka.ms/MinecraftJavaAccessibility");
            }

            this.minecraft.setScreen(this);
         }, "https://aka.ms/MinecraftJavaAccessibility", true))).bounds(this.width / 2 - 155, this.height - 27, 150, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.minecraft.setScreen(this.lastScreen))
            .bounds(this.width / 2 + 5, this.height - 27, 150, 20)
            .build()
      );
   }
}
