package net.minecraft.client.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;

public class PopupScreen extends Screen {
   private static final int BUTTON_PADDING = 20;
   private static final int BUTTON_MARGIN = 5;
   private static final int BUTTON_HEIGHT = 20;
   private final Component narrationMessage;
   private final FormattedText message;
   private final ImmutableList<PopupScreen.ButtonOption> buttonOptions;
   private MultiLineLabel messageLines = MultiLineLabel.EMPTY;
   private int contentTop;
   private int buttonWidth;

   protected PopupScreen(Component param0, List<Component> param1, ImmutableList<PopupScreen.ButtonOption> param2) {
      super(param0);
      this.message = FormattedText.composite(param1);
      this.narrationMessage = CommonComponents.joinForNarration(param0, ComponentUtils.formatList(param1, CommonComponents.EMPTY));
      this.buttonOptions = param2;
   }

   @Override
   public Component getNarrationMessage() {
      return this.narrationMessage;
   }

   @Override
   public void init() {
      for (PopupScreen.ButtonOption var2 : this.buttonOptions) {
         this.buttonWidth = Math.max(this.buttonWidth, 20 + this.font.width(var2.message) + 20);
      }

      int var1 = 5 + this.buttonWidth + 5;
      int var2 = var1 * this.buttonOptions.size();
      this.messageLines = MultiLineLabel.create(this.font, this.message, var2);
      int var3 = this.messageLines.getLineCount() * 9;
      this.contentTop = (int)(this.height / 2.0 - var3 / 2.0);
      int var4 = this.contentTop + var3 + 9 * 2;
      int var5 = (int)(this.width / 2.0 - var2 / 2.0);

      for (PopupScreen.ButtonOption var7 : this.buttonOptions) {
         this.addRenderableWidget(Button.builder(var7.message, var7.onPress).bounds(var5, var4, this.buttonWidth, 20).build());
         var5 += var1;
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderDirtBackground(param0);
      drawCenteredString(param0, this.font, this.title, this.width / 2, this.contentTop - 9 * 2, -1);
      this.messageLines.renderCentered(param0, this.width / 2, this.contentTop);
      super.render(param0, param1, param2, param3);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   public static final class ButtonOption {
      final Component message;
      final Button.OnPress onPress;

      public ButtonOption(Component param0, Button.OnPress param1) {
         this.message = param0;
         this.onPress = param1;
      }
   }
}
