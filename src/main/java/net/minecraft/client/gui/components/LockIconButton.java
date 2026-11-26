package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class LockIconButton extends Button {
   private boolean locked;

   public LockIconButton(int param0, int param1, Button.OnPress param2) {
      super(param0, param1, 20, 20, Component.translatable("narrator.button.difficulty_lock"), param2, DEFAULT_NARRATION);
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      return CommonComponents.joinForNarration(
         super.createNarrationMessage(),
         this.isLocked()
            ? Component.translatable("narrator.button.difficulty_lock.locked")
            : Component.translatable("narrator.button.difficulty_lock.unlocked")
      );
   }

   public boolean isLocked() {
      return this.locked;
   }

   public void setLocked(boolean param0) {
      this.locked = param0;
   }

   @Override
   public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
      RenderSystem.setShaderTexture(0, Button.WIDGETS_LOCATION);
      LockIconButton.Icon var5;
      if (!this.active) {
         var5 = this.locked ? LockIconButton.Icon.LOCKED_DISABLED : LockIconButton.Icon.UNLOCKED_DISABLED;
      } else if (this.isHoveredOrFocused()) {
         var5 = this.locked ? LockIconButton.Icon.LOCKED_HOVER : LockIconButton.Icon.UNLOCKED_HOVER;
      } else {
         var5 = this.locked ? LockIconButton.Icon.LOCKED : LockIconButton.Icon.UNLOCKED;
      }

      blit(param0, this.getX(), this.getY(), var5.getX(), var5.getY(), this.width, this.height);
   }

   enum Icon {
      LOCKED(0, 146),
      LOCKED_HOVER(0, 166),
      LOCKED_DISABLED(0, 186),
      UNLOCKED(20, 146),
      UNLOCKED_HOVER(20, 166),
      UNLOCKED_DISABLED(20, 186);

      private final int x;
      private final int y;

      Icon(int param2, int param3) {
         this.x = param2;
         this.y = param3;
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }
   }
}
