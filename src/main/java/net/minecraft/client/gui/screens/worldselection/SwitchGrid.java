package net.minecraft.client.gui.screens.worldselection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

class SwitchGrid {
   private static final int DEFAULT_SWITCH_BUTTON_WIDTH = 44;
   private final List<SwitchGrid.LabeledSwitch> switches;

   SwitchGrid(List<SwitchGrid.LabeledSwitch> param0) {
      this.switches = param0;
   }

   public void refreshStates() {
      this.switches.forEach(SwitchGrid.LabeledSwitch::refreshState);
   }

   public static SwitchGrid.Builder builder(int param0) {
      return new SwitchGrid.Builder(param0);
   }

   public static class Builder {
      final int width;
      private final List<SwitchGrid.SwitchBuilder> switchBuilders = new ArrayList<>();
      int paddingLeft;
      int rowSpacing = 4;
      int rowCount;
      Optional<SwitchGrid.InfoUnderneathSettings> infoUnderneath = Optional.empty();

      public Builder(int param0) {
         this.width = param0;
      }

      void increaseRow() {
         this.rowCount++;
      }

      public SwitchGrid.SwitchBuilder addSwitch(Component param0, BooleanSupplier param1, Consumer<Boolean> param2) {
         SwitchGrid.SwitchBuilder var4 = new SwitchGrid.SwitchBuilder(param0, param1, param2, 44);
         this.switchBuilders.add(var4);
         return var4;
      }

      public SwitchGrid.Builder withPaddingLeft(int param0) {
         this.paddingLeft = param0;
         return this;
      }

      public SwitchGrid.Builder withRowSpacing(int param0) {
         this.rowSpacing = param0;
         return this;
      }

      public SwitchGrid build(Consumer<LayoutElement> param0) {
         GridLayout var2 = new GridLayout().rowSpacing(this.rowSpacing);
         var2.addChild(SpacerElement.width(this.width - 44), 0, 0);
         var2.addChild(SpacerElement.width(44), 0, 1);
         List<SwitchGrid.LabeledSwitch> var3 = new ArrayList<>();
         this.rowCount = 0;

         for (SwitchGrid.SwitchBuilder var5 : this.switchBuilders) {
            var3.add(var5.build(this, var2, 0));
         }

         var2.arrangeElements();
         param0.accept(var2);
         SwitchGrid var4 = new SwitchGrid(var3);
         var4.refreshStates();
         return var4;
      }

      public SwitchGrid.Builder withInfoUnderneath(int param0, boolean param1) {
         this.infoUnderneath = Optional.of(new SwitchGrid.InfoUnderneathSettings(param0, param1));
         return this;
      }
   }

   record InfoUnderneathSettings(int maxInfoRows, boolean alwaysMaxHeight) {
   }

   record LabeledSwitch(CycleButton<Boolean> button, BooleanSupplier stateSupplier, @Nullable BooleanSupplier isActiveCondition) {
      public void refreshState() {
         this.button.setValue(this.stateSupplier.getAsBoolean());
         if (this.isActiveCondition != null) {
            this.button.active = this.isActiveCondition.getAsBoolean();
         }
      }
   }

   public static class SwitchBuilder {
      private final Component label;
      private final BooleanSupplier stateSupplier;
      private final Consumer<Boolean> onClicked;
      @Nullable
      private Component info;
      @Nullable
      private BooleanSupplier isActiveCondition;
      private final int buttonWidth;

      SwitchBuilder(Component param0, BooleanSupplier param1, Consumer<Boolean> param2, int param3) {
         this.label = param0;
         this.stateSupplier = param1;
         this.onClicked = param2;
         this.buttonWidth = param3;
      }

      public SwitchGrid.SwitchBuilder withIsActiveCondition(BooleanSupplier param0) {
         this.isActiveCondition = param0;
         return this;
      }

      public SwitchGrid.SwitchBuilder withInfo(Component param0) {
         this.info = param0;
         return this;
      }

      SwitchGrid.LabeledSwitch build(SwitchGrid.Builder param0, GridLayout param1, int param2) {
         param0.increaseRow();
         StringWidget var4 = new StringWidget(this.label, Minecraft.getInstance().font).alignLeft();
         param1.addChild(var4, param0.rowCount, param2, param1.newCellSettings().align(0.0F, 0.5F).paddingLeft(param0.paddingLeft));
         Optional<SwitchGrid.InfoUnderneathSettings> var5 = param0.infoUnderneath;
         CycleButton.Builder<Boolean> var6 = CycleButton.onOffBuilder(this.stateSupplier.getAsBoolean());
         var6.displayOnlyValue();
         boolean var7 = this.info != null && !var5.isPresent();
         if (var7) {
            Tooltip var8 = Tooltip.create(this.info);
            var6.withTooltip(l_param1 -> var8);
         }

         if (this.info != null && !var7) {
            var6.withCustomNarration(l_param0 -> CommonComponents.joinForNarration(this.label, l_param0.createDefaultNarrationMessage(), this.info));
         } else {
            var6.withCustomNarration(l_param0 -> CommonComponents.joinForNarration(this.label, l_param0.createDefaultNarrationMessage()));
         }

         CycleButton<Boolean> var8 = var6.create(0, 0, this.buttonWidth, 20, Component.empty(), (l_param0, l_param1) -> this.onClicked.accept(l_param1));
         if (this.isActiveCondition != null) {
            var8.active = this.isActiveCondition.getAsBoolean();
         }

         param1.addChild(var8, param0.rowCount, param2 + 1, param1.newCellSettings().alignHorizontallyRight());
         if (this.info != null) {
            var5.ifPresent(l_param3 -> {
               Component l_var5 = this.info.copy().withStyle(ChatFormatting.GRAY);
               Font l_var6 = Minecraft.getInstance().font;
               MultiLineTextWidget l_var7 = new MultiLineTextWidget(l_var5, l_var6);
               l_var7.setMaxWidth(param0.width - param0.paddingLeft - this.buttonWidth);
               l_var7.setMaxRows(l_param3.maxInfoRows());
               param0.increaseRow();
               int l_var8 = l_param3.alwaysMaxHeight ? 9 * l_param3.maxInfoRows - l_var7.getHeight() : 0;
               param1.addChild(l_var7, param0.rowCount, param2, param1.newCellSettings().paddingTop(-param0.rowSpacing).paddingBottom(l_var8));
            });
         }

         return new SwitchGrid.LabeledSwitch(var8, this.stateSupplier, this.isActiveCondition);
      }
   }
}
