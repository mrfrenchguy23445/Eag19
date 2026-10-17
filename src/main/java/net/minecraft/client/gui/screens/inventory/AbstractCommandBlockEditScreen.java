package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.BaseCommandBlock;

public abstract class AbstractCommandBlockEditScreen extends Screen {
   private static final Component SET_COMMAND_LABEL = Component.translatable("advMode.setCommand");
   private static final Component COMMAND_LABEL = Component.translatable("advMode.command");
   private static final Component PREVIOUS_OUTPUT_LABEL = Component.translatable("advMode.previousOutput");
   protected EditBox commandEdit;
   protected EditBox previousEdit;
   protected Button doneButton;
   protected Button cancelButton;
   protected CycleButton<Boolean> outputButton;
   CommandSuggestions commandSuggestions;

   public AbstractCommandBlockEditScreen() {
      super(GameNarrator.NO_TITLE);
   }

   @Override
   public void tick() {
      this.commandEdit.tick();
   }

   abstract BaseCommandBlock getCommandBlock();

   abstract int getPreviousY();

   @Override
   protected void init() {
      this.doneButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.onDone()).bounds(this.width / 2 - 4 - 150, this.height / 4 + 120 + 12, 150, 20).build()
      );
      this.cancelButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.onClose()).bounds(this.width / 2 + 4, this.height / 4 + 120 + 12, 150, 20).build()
      );
      boolean var1 = this.getCommandBlock().isTrackOutput();
      this.outputButton = this.addRenderableWidget(
         CycleButton.booleanBuilder(Component.literal("O"), Component.literal("X"))
            .withInitialValue(var1)
            .displayOnlyValue()
            .create(this.width / 2 + 150 - 20, this.getPreviousY(), 20, 20, Component.translatable("advMode.trackOutput"), (l_param0, l_param1) -> {
               BaseCommandBlock l_var3 = this.getCommandBlock();
               l_var3.setTrackOutput(l_param1);
               this.updatePreviousOutput(l_param1);
            })
      );
      this.commandEdit = new EditBox(this.font, this.width / 2 - 150, 50, 300, 20, Component.translatable("advMode.command")) {
         @Override
         protected MutableComponent createNarrationMessage() {
            return super.createNarrationMessage().append(AbstractCommandBlockEditScreen.this.commandSuggestions.getNarrationMessage());
         }
      };
      this.commandEdit.setMaxLength(32500);
      this.commandEdit.setResponder(this::onEdited);
      this.addWidget(this.commandEdit);
      this.previousEdit = new EditBox(this.font, this.width / 2 - 150, this.getPreviousY(), 276, 20, Component.translatable("advMode.previousOutput"));
      this.previousEdit.setMaxLength(32500);
      this.previousEdit.setEditable(false);
      this.previousEdit.setValue("-");
      this.addWidget(this.previousEdit);
      this.setInitialFocus(this.commandEdit);
      this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.commandEdit, this.font, true, true, 0, 7, false, Integer.MIN_VALUE);
      this.commandSuggestions.setAllowSuggestions(true);
      this.commandSuggestions.updateCommandInfo();
      this.updatePreviousOutput(var1);
   }

   @Override
   public void resize(Minecraft param0, int param1, int param2) {
      String var4 = this.commandEdit.getValue();
      this.init(param0, param1, param2);
      this.commandEdit.setValue(var4);
      this.commandSuggestions.updateCommandInfo();
   }

   protected void updatePreviousOutput(boolean param0) {
      this.previousEdit.setValue(param0 ? this.getCommandBlock().getLastOutput().getString() : "-");
   }

   protected void onDone() {
      BaseCommandBlock var1 = this.getCommandBlock();
      this.populateAndSendPacket(var1);
      if (!var1.isTrackOutput()) {
         var1.setLastOutput(null);
      }

      this.minecraft.setScreen(null);
   }

   protected abstract void populateAndSendPacket(BaseCommandBlock var1);

   private void onEdited(String param0) {
      this.commandSuggestions.updateCommandInfo();
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (this.commandSuggestions.keyPressed(param0, param1, param2)) {
         return true;
      }

      if (super.keyPressed(param0, param1, param2)) {
         return true;
      }

      if (param0 != 257 && param0 != 335) {
         return false;
      }

      this.onDone();
      return true;
   }

   @Override
   public boolean mouseScrolled(double param0, double param2, double param4) {
      return this.commandSuggestions.mouseScrolled(param4) ? true : super.mouseScrolled(param0, param2, param4);
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      return this.commandSuggestions.mouseClicked(param0, param2, param4) ? true : super.mouseClicked(param0, param2, param4);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      drawCenteredString(param0, this.font, SET_COMMAND_LABEL, this.width / 2, 20, 16777215);
      drawString(param0, this.font, COMMAND_LABEL, this.width / 2 - 150, 40, 10526880);
      this.commandEdit.render(param0, param1, param2, param3);
      int var5 = 75;
      if (!this.previousEdit.getValue().isEmpty()) {
         var5 += 5 * 9 + 1 + this.getPreviousY() - 135;
         drawString(param0, this.font, PREVIOUS_OUTPUT_LABEL, this.width / 2 - 150, var5 + 4, 10526880);
         this.previousEdit.render(param0, param1, param2, param3);
      }

      super.render(param0, param1, param2, param3);
      this.commandSuggestions.render(param0, param1, param2);
   }
}
