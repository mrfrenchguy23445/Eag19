package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.stream.IntStream;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class AbstractSignEditScreen extends Screen {
   protected final SignBlockEntity sign;
   protected final String[] messages;
   protected final WoodType woodType;
   private int frame;
   private int line;
   private TextFieldHelper signField;

   public AbstractSignEditScreen(SignBlockEntity param0, boolean param1) {
      this(param0, param1, Component.translatable("sign.edit"));
   }

   public AbstractSignEditScreen(SignBlockEntity param0, boolean param1, Component param2) {
      super(param2);
      this.woodType = SignBlock.getWoodType(param0.getBlockState().getBlock());
      this.messages = IntStream.range(0, 4).mapToObj(l_param2 -> param0.getMessage(l_param2, param1)).map(Component::getString).toArray(String[]::new);
      this.sign = param0;
   }

   @Override
   protected void init() {
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.onDone()).bounds(this.width / 2 - 100, this.height / 4 + 120, 200, 20).build()
      );
      this.sign.setEditable(false);
      this.signField = new TextFieldHelper(
         () -> this.messages[this.line],
         l_param0 -> {
            this.messages[this.line] = l_param0;
            this.sign.setMessage(this.line, Component.literal(l_param0));
         },
         TextFieldHelper.createClipboardGetter(this.minecraft),
         TextFieldHelper.createClipboardSetter(this.minecraft),
         l_param0 -> this.minecraft.font.width(l_param0) <= this.sign.getMaxTextLineWidth()
      );
   }

   @Override
   public void removed() {
      ClientPacketListener var1 = this.minecraft.getConnection();
      if (var1 != null) {
         var1.send(new ServerboundSignUpdatePacket(this.sign.getBlockPos(), this.messages[0], this.messages[1], this.messages[2], this.messages[3]));
      }

      this.sign.setEditable(true);
   }

   @Override
   public void tick() {
      this.frame++;
      if (!this.sign.getType().isValid(this.sign.getBlockState())) {
         this.onDone();
      }
   }

   private void onDone() {
      this.sign.setChanged();
      this.minecraft.setScreen(null);
   }

   @Override
   public boolean charTyped(char param0, int param1) {
      this.signField.charTyped(param0);
      return true;
   }

   @Override
   public void onClose() {
      this.onDone();
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (param0 == 265) {
         this.line = this.line - 1 & 3;
         this.signField.setCursorToEnd();
         return true;
      } else if (param0 == 264 || param0 == 257 || param0 == 335) {
         this.line = this.line + 1 & 3;
         this.signField.setCursorToEnd();
         return true;
      } else {
         return this.signField.keyPressed(param0) ? true : super.keyPressed(param0, param1, param2);
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      Lighting.setupForFlatItems();
      this.renderBackground(param0);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 40, 16777215);
      this.renderSign(param0);
      Lighting.setupFor3DItems();
      super.render(param0, param1, param2, param3);
   }

   protected abstract void renderSignBackground(PoseStack var1, MultiBufferSource.BufferSource var2, BlockState var3);

   protected abstract Vector3f getSignTextScale();

   protected void offsetSign(PoseStack param0, BlockState param1) {
      param0.translate(this.width / 2.0F, 90.0F, 50.0F);
   }

   private void renderSign(PoseStack param0) {
      MultiBufferSource.BufferSource var2 = this.minecraft.renderBuffers().bufferSource();
      BlockState var3 = this.sign.getBlockState();
      param0.pushPose();
      this.offsetSign(param0, var3);
      param0.pushPose();
      this.renderSignBackground(param0, var2, var3);
      param0.popPose();
      this.renderSignText(param0, var2);
      param0.popPose();
   }

   private void renderSignText(PoseStack param0, MultiBufferSource.BufferSource param1) {
      param0.translate(0.0F, 0.0F, 4.0F);
      Vector3f var3 = this.getSignTextScale();
      param0.scale(var3.x(), var3.y(), var3.z());
      int var4 = this.sign.getColor().getTextColor();
      boolean var5 = this.frame / 6 % 2 == 0;
      int var6 = this.signField.getCursorPos();
      int var7 = this.signField.getSelectionPos();
      int var8 = 4 * this.sign.getTextLineHeight() / 2;
      int var9 = this.line * this.sign.getTextLineHeight() - var8;
      Matrix4f var10 = param0.last().pose();

      for (int var11 = 0; var11 < this.messages.length; var11++) {
         String var12 = this.messages[var11];
         if (var12 != null) {
            if (this.font.isBidirectional()) {
               var12 = this.font.bidirectionalShaping(var12);
            }

            float var13 = -this.minecraft.font.width(var12) / 2;
            this.minecraft
               .font
               .drawInBatch(var12, var13, var11 * this.sign.getTextLineHeight() - var8, var4, false, var10, param1, Font.DisplayMode.NORMAL, 0, 15728880, false);
            if (var11 == this.line && var6 >= 0 && var5) {
               int var14 = this.minecraft.font.width(var12.substring(0, Math.max(Math.min(var6, var12.length()), 0)));
               int var15 = var14 - this.minecraft.font.width(var12) / 2;
               if (var6 >= var12.length()) {
                  this.minecraft.font.drawInBatch("_", var15, var9, var4, false, var10, param1, Font.DisplayMode.NORMAL, 0, 15728880, false);
               }
            }
         }
      }

      param1.endBatch();

      for (int var11 = 0; var11 < this.messages.length; var11++) {
         String var12 = this.messages[var11];
         if (var12 != null && var11 == this.line && var6 >= 0) {
            int var13 = this.minecraft.font.width(var12.substring(0, Math.max(Math.min(var6, var12.length()), 0)));
            int var14 = var13 - this.minecraft.font.width(var12) / 2;
            if (var5 && var6 < var12.length()) {
               fill(param0, var14, var9 - 1, var14 + 1, var9 + this.sign.getTextLineHeight(), 0xFF000000 | var4);
            }

            if (var7 != var6) {
               int var15 = Math.min(var6, var7);
               int var16 = Math.max(var6, var7);
               int var17 = this.minecraft.font.width(var12.substring(0, var15)) - this.minecraft.font.width(var12) / 2;
               int var18 = this.minecraft.font.width(var12.substring(0, var16)) - this.minecraft.font.width(var12) / 2;
               int var19 = Math.min(var17, var18);
               int var20 = Math.max(var17, var18);
               RenderSystem.enableColorLogicOp();
               RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
               fill(param0, var19, var9, var20, var9 + this.sign.getTextLineHeight(), -16776961);
               RenderSystem.disableColorLogicOp();
            }
         }
      }
   }
}
