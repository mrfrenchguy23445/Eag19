package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

public class CommandBlockEditScreen extends AbstractCommandBlockEditScreen {
   private final CommandBlockEntity autoCommandBlock;
   private CycleButton<CommandBlockEntity.Mode> modeButton;
   private CycleButton<Boolean> conditionalButton;
   private CycleButton<Boolean> autoexecButton;
   private CommandBlockEntity.Mode mode = CommandBlockEntity.Mode.REDSTONE;
   private boolean conditional;
   private boolean autoexec;

   public CommandBlockEditScreen(CommandBlockEntity param0) {
      this.autoCommandBlock = param0;
   }

   @Override
   BaseCommandBlock getCommandBlock() {
      return this.autoCommandBlock.getCommandBlock();
   }

   @Override
   int getPreviousY() {
      return 135;
   }

   @Override
   protected void init() {
      super.init();
      this.modeButton = this.addRenderableWidget(
         CycleButton.<CommandBlockEntity.Mode>builder(l_param0 -> {
               switch (l_param0) {
                  case SEQUENCE:
                     return Component.translatable("advMode.mode.sequence");
                  case AUTO:
                     return Component.translatable("advMode.mode.auto");
                  case REDSTONE:
                  default:
                     return Component.translatable("advMode.mode.redstone");
               }
            })
            .withValues(CommandBlockEntity.Mode.values())
            .displayOnlyValue()
            .withInitialValue(this.mode)
            .create(this.width / 2 - 50 - 100 - 4, 165, 100, 20, Component.translatable("advMode.mode"), (l_param0, l_param1) -> this.mode = l_param1)
      );
      this.conditionalButton = this.addRenderableWidget(
         CycleButton.booleanBuilder(Component.translatable("advMode.mode.conditional"), Component.translatable("advMode.mode.unconditional"))
            .displayOnlyValue()
            .withInitialValue(this.conditional)
            .create(this.width / 2 - 50, 165, 100, 20, Component.translatable("advMode.type"), (l_param0, l_param1) -> this.conditional = l_param1)
      );
      this.autoexecButton = this.addRenderableWidget(
         CycleButton.booleanBuilder(Component.translatable("advMode.mode.autoexec.bat"), Component.translatable("advMode.mode.redstoneTriggered"))
            .displayOnlyValue()
            .withInitialValue(this.autoexec)
            .create(this.width / 2 + 50 + 4, 165, 100, 20, Component.translatable("advMode.triggering"), (l_param0, l_param1) -> this.autoexec = l_param1)
      );
      this.enableControls(false);
   }

   private void enableControls(boolean param0) {
      this.doneButton.active = param0;
      this.outputButton.active = param0;
      this.modeButton.active = param0;
      this.conditionalButton.active = param0;
      this.autoexecButton.active = param0;
   }

   public void updateGui() {
      BaseCommandBlock var1 = this.autoCommandBlock.getCommandBlock();
      this.commandEdit.setValue(var1.getCommand());
      boolean var2 = var1.isTrackOutput();
      this.mode = this.autoCommandBlock.getMode();
      this.conditional = this.autoCommandBlock.isConditional();
      this.autoexec = this.autoCommandBlock.isAutomatic();
      this.outputButton.setValue(var2);
      this.modeButton.setValue(this.mode);
      this.conditionalButton.setValue(this.conditional);
      this.autoexecButton.setValue(this.autoexec);
      this.updatePreviousOutput(var2);
      this.enableControls(true);
   }

   @Override
   public void resize(Minecraft param0, int param1, int param2) {
      super.resize(param0, param1, param2);
      this.enableControls(true);
   }

   @Override
   protected void populateAndSendPacket(BaseCommandBlock param0) {
      this.minecraft
         .getConnection()
         .send(
            new ServerboundSetCommandBlockPacket(
               BlockPos.containing(param0.getPosition()), this.commandEdit.getValue(), this.mode, param0.isTrackOutput(), this.conditional, this.autoexec
            )
         );
   }
}
