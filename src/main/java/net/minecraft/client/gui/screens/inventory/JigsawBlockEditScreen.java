package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;

public class JigsawBlockEditScreen extends Screen {
   private static final int MAX_LEVELS = 7;
   private static final Component JOINT_LABEL = Component.translatable("jigsaw_block.joint_label");
   private static final Component POOL_LABEL = Component.translatable("jigsaw_block.pool");
   private static final Component NAME_LABEL = Component.translatable("jigsaw_block.name");
   private static final Component TARGET_LABEL = Component.translatable("jigsaw_block.target");
   private static final Component FINAL_STATE_LABEL = Component.translatable("jigsaw_block.final_state");
   private final JigsawBlockEntity jigsawEntity;
   private EditBox nameEdit;
   private EditBox targetEdit;
   private EditBox poolEdit;
   private EditBox finalStateEdit;
   int levels;
   private boolean keepJigsaws = true;
   private CycleButton<JigsawBlockEntity.JointType> jointButton;
   private Button doneButton;
   private Button generateButton;
   private JigsawBlockEntity.JointType joint;

   public JigsawBlockEditScreen(JigsawBlockEntity param0) {
      super(GameNarrator.NO_TITLE);
      this.jigsawEntity = param0;
   }

   @Override
   public void tick() {
      this.nameEdit.tick();
      this.targetEdit.tick();
      this.poolEdit.tick();
      this.finalStateEdit.tick();
   }

   private void onDone() {
      this.sendToServer();
      this.minecraft.setScreen(null);
   }

   private void onCancel() {
      this.minecraft.setScreen(null);
   }

   private void sendToServer() {
      this.minecraft
         .getConnection()
         .send(
            new ServerboundSetJigsawBlockPacket(
               this.jigsawEntity.getBlockPos(),
               new ResourceLocation(this.nameEdit.getValue()),
               new ResourceLocation(this.targetEdit.getValue()),
               new ResourceLocation(this.poolEdit.getValue()),
               this.finalStateEdit.getValue(),
               this.joint
            )
         );
   }

   private void sendGenerate() {
      this.minecraft.getConnection().send(new ServerboundJigsawGeneratePacket(this.jigsawEntity.getBlockPos(), this.levels, this.keepJigsaws));
   }

   @Override
   public void onClose() {
      this.onCancel();
   }

   @Override
   protected void init() {
      this.poolEdit = new EditBox(this.font, this.width / 2 - 152, 20, 300, 20, Component.translatable("jigsaw_block.pool"));
      this.poolEdit.setMaxLength(128);
      this.poolEdit.setValue(this.jigsawEntity.getPool().location().toString());
      this.poolEdit.setResponder(l_param0 -> this.updateValidity());
      this.addWidget(this.poolEdit);
      this.nameEdit = new EditBox(this.font, this.width / 2 - 152, 55, 300, 20, Component.translatable("jigsaw_block.name"));
      this.nameEdit.setMaxLength(128);
      this.nameEdit.setValue(this.jigsawEntity.getName().toString());
      this.nameEdit.setResponder(l_param0 -> this.updateValidity());
      this.addWidget(this.nameEdit);
      this.targetEdit = new EditBox(this.font, this.width / 2 - 152, 90, 300, 20, Component.translatable("jigsaw_block.target"));
      this.targetEdit.setMaxLength(128);
      this.targetEdit.setValue(this.jigsawEntity.getTarget().toString());
      this.targetEdit.setResponder(l_param0 -> this.updateValidity());
      this.addWidget(this.targetEdit);
      this.finalStateEdit = new EditBox(this.font, this.width / 2 - 152, 125, 300, 20, Component.translatable("jigsaw_block.final_state"));
      this.finalStateEdit.setMaxLength(256);
      this.finalStateEdit.setValue(this.jigsawEntity.getFinalState());
      this.addWidget(this.finalStateEdit);
      this.joint = this.jigsawEntity.getJoint();
      int var1 = this.font.width(JOINT_LABEL) + 10;
      this.jointButton = this.addRenderableWidget(
         CycleButton.builder(JigsawBlockEntity.JointType::getTranslatedName)
            .withValues(JigsawBlockEntity.JointType.values())
            .withInitialValue(this.joint)
            .displayOnlyValue()
            .create(this.width / 2 - 152 + var1, 150, 300 - var1, 20, JOINT_LABEL, (l_param0, l_param1) -> this.joint = l_param1)
      );
      boolean var2 = JigsawBlock.getFrontFacing(this.jigsawEntity.getBlockState()).getAxis().isVertical();
      this.jointButton.active = var2;
      this.jointButton.visible = var2;
      this.addRenderableWidget(new AbstractSliderButton(this.width / 2 - 154, 180, 100, 20, CommonComponents.EMPTY, 0.0) {
         {
            this.updateMessage();
         }

         @Override
         protected void updateMessage() {
            this.setMessage(Component.translatable("jigsaw_block.levels", JigsawBlockEditScreen.this.levels));
         }

         @Override
         protected void applyValue() {
            JigsawBlockEditScreen.this.levels = Mth.floor(Mth.clampedLerp(0.0, 7.0, this.value));
         }
      });
      this.addRenderableWidget(
         CycleButton.onOffBuilder(this.keepJigsaws)
            .create(this.width / 2 - 50, 180, 100, 20, Component.translatable("jigsaw_block.keep_jigsaws"), (l_param0, l_param1) -> this.keepJigsaws = l_param1)
      );
      this.generateButton = this.addRenderableWidget(Button.builder(Component.translatable("jigsaw_block.generate"), l_param0 -> {
         this.onDone();
         this.sendGenerate();
      }).bounds(this.width / 2 + 54, 180, 100, 20).build());
      this.doneButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.onDone()).bounds(this.width / 2 - 4 - 150, 210, 150, 20).build()
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.onCancel()).bounds(this.width / 2 + 4, 210, 150, 20).build());
      this.setInitialFocus(this.poolEdit);
      this.updateValidity();
   }

   private void updateValidity() {
      boolean var1 = ResourceLocation.isValidResourceLocation(this.nameEdit.getValue())
         && ResourceLocation.isValidResourceLocation(this.targetEdit.getValue())
         && ResourceLocation.isValidResourceLocation(this.poolEdit.getValue());
      this.doneButton.active = var1;
      this.generateButton.active = var1;
   }

   @Override
   public void resize(Minecraft param0, int param1, int param2) {
      String var4 = this.nameEdit.getValue();
      String var5 = this.targetEdit.getValue();
      String var6 = this.poolEdit.getValue();
      String var7 = this.finalStateEdit.getValue();
      int var8 = this.levels;
      JigsawBlockEntity.JointType var9 = this.joint;
      this.init(param0, param1, param2);
      this.nameEdit.setValue(var4);
      this.targetEdit.setValue(var5);
      this.poolEdit.setValue(var6);
      this.finalStateEdit.setValue(var7);
      this.levels = var8;
      this.joint = var9;
      this.jointButton.setValue(var9);
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (super.keyPressed(param0, param1, param2)) {
         return true;
      }

      if (!this.doneButton.active || param0 != 257 && param0 != 335) {
         return false;
      }

      this.onDone();
      return true;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      drawString(param0, this.font, POOL_LABEL, this.width / 2 - 153, 10, 10526880);
      this.poolEdit.render(param0, param1, param2, param3);
      drawString(param0, this.font, NAME_LABEL, this.width / 2 - 153, 45, 10526880);
      this.nameEdit.render(param0, param1, param2, param3);
      drawString(param0, this.font, TARGET_LABEL, this.width / 2 - 153, 80, 10526880);
      this.targetEdit.render(param0, param1, param2, param3);
      drawString(param0, this.font, FINAL_STATE_LABEL, this.width / 2 - 153, 115, 10526880);
      this.finalStateEdit.render(param0, param1, param2, param3);
      if (JigsawBlock.getFrontFacing(this.jigsawEntity.getBlockState()).getAxis().isVertical()) {
         drawString(param0, this.font, JOINT_LABEL, this.width / 2 - 153, 156, 16777215);
      }

      super.render(param0, param1, param2, param3);
   }
}
