package net.minecraft.client.gui.screens.worldselection;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.flag.FeatureFlags;

public class ConfirmExperimentalFeaturesScreen extends Screen {
   private static final Component TITLE = Component.translatable("selectWorld.experimental.title");
   private static final Component MESSAGE = Component.translatable("selectWorld.experimental.message");
   private static final Component DETAILS_BUTTON = Component.translatable("selectWorld.experimental.details");
   private static final int COLUMN_SPACING = 10;
   private static final int DETAILS_BUTTON_WIDTH = 100;
   private final BooleanConsumer callback;
   final Collection<Pack> enabledPacks;
   private final GridLayout layout = new GridLayout().columnSpacing(10).rowSpacing(20);

   public ConfirmExperimentalFeaturesScreen(Collection<Pack> param0, BooleanConsumer param1) {
      super(TITLE);
      this.enabledPacks = param0;
      this.callback = param1;
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), MESSAGE);
   }

   @Override
   protected void init() {
      super.init();
      GridLayout.RowHelper var1 = this.layout.createRowHelper(2);
      LayoutSettings var2 = var1.newCellSettings().alignHorizontallyCenter();
      var1.addChild(new StringWidget(this.title, this.font), 2, var2);
      MultiLineTextWidget var3 = var1.addChild(new MultiLineTextWidget(MESSAGE, this.font).setCentered(true), 2, var2);
      var3.setMaxWidth(310);
      var1.addChild(
         Button.builder(DETAILS_BUTTON, l_param0 -> this.minecraft.setScreen(new ConfirmExperimentalFeaturesScreen.DetailsScreen())).width(100).build(),
         2,
         var2
      );
      var1.addChild(Button.builder(CommonComponents.GUI_PROCEED, l_param0 -> this.callback.accept(true)).build());
      var1.addChild(Button.builder(CommonComponents.GUI_BACK, l_param0 -> this.callback.accept(false)).build());
      this.layout.visitWidgets(l_param1 -> {
         AbstractWidget var10000 = this.addRenderableWidget(l_param1);
      });
      this.layout.arrangeElements();
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      FrameLayout.alignInRectangle(this.layout, 0, 0, this.width, this.height, 0.5F, 0.5F);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      super.render(param0, param1, param2, param3);
   }

   @Override
   public void onClose() {
      this.callback.accept(false);
   }

   class DetailsScreen extends Screen {
      private ConfirmExperimentalFeaturesScreen.DetailsScreen.PackList packList;

      DetailsScreen() {
         super(Component.translatable("selectWorld.experimental.details.title"));
      }

      @Override
      public void onClose() {
         this.minecraft.setScreen(ConfirmExperimentalFeaturesScreen.this);
      }

      @Override
      protected void init() {
         super.init();
         this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_BACK, l_param0 -> this.onClose()).bounds(this.width / 2 - 100, this.height / 4 + 120 + 24, 200, 20).build()
         );
         this.packList = new ConfirmExperimentalFeaturesScreen.DetailsScreen.PackList(this.minecraft, ConfirmExperimentalFeaturesScreen.this.enabledPacks);
         this.addWidget(this.packList);
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, float param3) {
         this.renderBackground(param0);
         this.packList.render(param0, param1, param2, param3);
         drawCenteredString(param0, this.font, this.title, this.width / 2, 10, 16777215);
         super.render(param0, param1, param2, param3);
      }

      class PackList extends ObjectSelectionList<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry> {
         public PackList(Minecraft param1, Collection<Pack> param2) {
            super(param1, DetailsScreen.this.width, DetailsScreen.this.height, 32, DetailsScreen.this.height - 64, (9 + 2) * 3);

            for (Pack var5 : param2) {
               String var6 = FeatureFlags.printMissingFlags(FeatureFlags.VANILLA_SET, var5.getRequestedFeatures());
               if (!var6.isEmpty()) {
                  Component var7 = ComponentUtils.mergeStyles(var5.getTitle().copy(), Style.EMPTY.withBold(true));
                  Component var8 = Component.translatable("selectWorld.experimental.details.entry", var6);
                  this.addEntry(DetailsScreen.this.new PackListEntry(var7, var8, MultiLineLabel.create(DetailsScreen.this.font, var8, this.getRowWidth())));
               }
            }
         }

         @Override
         public int getRowWidth() {
            return this.width * 3 / 4;
         }
      }

      class PackListEntry extends ObjectSelectionList.Entry<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry> {
         private final Component packId;
         private final Component message;
         private final MultiLineLabel splitMessage;

         PackListEntry(Component param1, Component param2, MultiLineLabel param3) {
            this.packId = param1;
            this.message = param2;
            this.splitMessage = param3;
         }

         @Override
         public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
            GuiComponent.drawString(param0, DetailsScreen.this.minecraft.font, this.packId, param3, param2, 16777215);
            this.splitMessage.renderLeftAligned(param0, param3, param2 + 12, 9, 16777215);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.packId, this.message));
         }
      }
   }
}
