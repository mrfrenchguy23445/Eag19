package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.sounds.SoundEvents;

public class PageButton extends Button {
   private final boolean isForward;
   private final boolean playTurnSound;

   public PageButton(int param0, int param1, boolean param2, Button.OnPress param3, boolean param4) {
      super(param0, param1, 23, 13, CommonComponents.EMPTY, param3, DEFAULT_NARRATION);
      this.isForward = param2;
      this.playTurnSound = param4;
   }

   @Override
   public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
      RenderSystem.setShaderTexture(0, BookViewScreen.BOOK_LOCATION);
      int var5 = 0;
      int var6 = 192;
      if (this.isHoveredOrFocused()) {
         var5 += 23;
      }

      if (!this.isForward) {
         var6 += 13;
      }

      blit(param0, this.getX(), this.getY(), var5, var6, 23, 13);
   }

   @Override
   public void playDownSound(SoundManager param0) {
      if (this.playTurnSound) {
         param0.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
      }
   }
}
