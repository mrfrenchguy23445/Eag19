package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public class InBedChatScreen extends ChatScreen {
   private Button leaveBedButton;

   public InBedChatScreen() {
      super("");
   }

   @Override
   protected void init() {
      super.init();
      this.leaveBedButton = Button.builder(Component.translatable("multiplayer.stopSleeping"), l_param0 -> this.sendWakeUp())
         .bounds(this.width / 2 - 100, this.height - 40, 200, 20)
         .build();
      this.addRenderableWidget(this.leaveBedButton);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      if (!this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer())) {
         this.leaveBedButton.render(param0, param1, param2, param3);
      } else {
         super.render(param0, param1, param2, param3);
      }
   }

   @Override
   public void onClose() {
      this.sendWakeUp();
   }

   @Override
   public boolean charTyped(char param0, int param1) {
      return !this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer()) ? true : super.charTyped(param0, param1);
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (param0 == 256) {
         this.sendWakeUp();
      }

      if (!this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer())) {
         return true;
      }

      if (param0 != 257 && param0 != 335) {
         return super.keyPressed(param0, param1, param2);
      }

      if (this.handleChatInput(this.input.getValue(), true)) {
         this.minecraft.setScreen(null);
         this.input.setValue("");
         this.minecraft.gui.getChat().resetChatScroll();
      }

      return true;
   }

   private void sendWakeUp() {
      ClientPacketListener var1 = this.minecraft.player.connection;
      var1.send(new ServerboundPlayerCommandPacket(this.minecraft.player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
   }

   public void onPlayerWokeUp() {
      if (this.input.getValue().isEmpty()) {
         this.minecraft.setScreen(null);
      } else {
         this.minecraft.setScreen(new ChatScreen(this.input.getValue()));
      }
   }
}
