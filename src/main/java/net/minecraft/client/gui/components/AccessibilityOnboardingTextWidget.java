package net.minecraft.client.gui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class AccessibilityOnboardingTextWidget extends MultiLineTextWidget {
   private static final int BORDER_COLOR_FOCUSED = -1;
   private static final int BORDER_COLOR = -6250336;
   private static final int BACKGROUND_COLOR = 1426063360;
   private static final int PADDING = 3;
   private static final int BORDER = 1;

   public AccessibilityOnboardingTextWidget(Font param0, Component param1, int param2) {
      super(param1, param0);
      this.setMaxWidth(param2);
      this.setCentered(true);
      this.active = true;
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput param0) {
      param0.add(NarratedElementType.TITLE, this.getMessage());
   }

   @Override
   public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
      int var5 = this.getX() - 3;
      int var6 = this.getY() - 3;
      int var7 = this.getX() + this.getWidth() + 3;
      int var8 = this.getY() + this.getHeight() + 3;
      int var9 = this.isFocused() ? -1 : -6250336;
      fill(param0, var5 - 1, var6 - 1, var5, var8 + 1, var9);
      fill(param0, var7, var6 - 1, var7 + 1, var8 + 1, var9);
      fill(param0, var5, var6, var7, var6 - 1, var9);
      fill(param0, var5, var8, var7, var8 + 1, var9);
      fill(param0, var5, var6, var7, var8, 1426063360);
      super.renderWidget(param0, param1, param2, param3);
   }

   @Override
   public void playDownSound(SoundManager param0) {
   }
}
