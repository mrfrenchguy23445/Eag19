package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

public class CreateFlatWorldScreen extends Screen {
   private static final int SLOT_TEX_SIZE = 128;
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   protected final CreateWorldScreen parent;
   private final Consumer<FlatLevelGeneratorSettings> applySettings;
   FlatLevelGeneratorSettings generator;
   private Component columnType;
   private Component columnHeight;
   private CreateFlatWorldScreen.DetailsList list;
   private Button deleteLayerButton;

   public CreateFlatWorldScreen(CreateWorldScreen param0, Consumer<FlatLevelGeneratorSettings> param1, FlatLevelGeneratorSettings param2) {
      super(Component.translatable("createWorld.customize.flat.title"));
      this.parent = param0;
      this.applySettings = param1;
      this.generator = param2;
   }

   public FlatLevelGeneratorSettings settings() {
      return this.generator;
   }

   public void setConfig(FlatLevelGeneratorSettings param0) {
      this.generator = param0;
   }

   @Override
   protected void init() {
      this.columnType = Component.translatable("createWorld.customize.flat.tile");
      this.columnHeight = Component.translatable("createWorld.customize.flat.height");
      this.list = new CreateFlatWorldScreen.DetailsList();
      this.addWidget(this.list);
      this.deleteLayerButton = this.addRenderableWidget(Button.builder(Component.translatable("createWorld.customize.flat.removeLayer"), l_param0 -> {
         if (this.hasValidSelection()) {
            List<FlatLayerInfo> l_var2 = this.generator.getLayersInfo();
            int l_var3 = this.list.children().indexOf(this.list.getSelected());
            int l_var4 = l_var2.size() - l_var3 - 1;
            l_var2.remove(l_var4);
            this.list.setSelected(l_var2.isEmpty() ? null : this.list.children().get(Math.min(l_var3, l_var2.size() - 1)));
            this.generator.updateLayers();
            this.list.resetRows();
            this.updateButtonValidity();
         }
      }).bounds(this.width / 2 - 155, this.height - 52, 150, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("createWorld.customize.presets"), l_param0 -> {
         this.minecraft.setScreen(new PresetFlatWorldScreen(this));
         this.generator.updateLayers();
         this.updateButtonValidity();
      }).bounds(this.width / 2 + 5, this.height - 52, 150, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, l_param0 -> {
         this.applySettings.accept(this.generator);
         this.minecraft.setScreen(this.parent);
         this.generator.updateLayers();
      }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> {
         this.minecraft.setScreen(this.parent);
         this.generator.updateLayers();
      }).bounds(this.width / 2 + 5, this.height - 28, 150, 20).build());
      this.generator.updateLayers();
      this.updateButtonValidity();
   }

   void updateButtonValidity() {
      this.deleteLayerButton.active = this.hasValidSelection();
   }

   private boolean hasValidSelection() {
      return this.list.getSelected() != null;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      this.list.render(param0, param1, param2, param3);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 8, 16777215);
      int var5 = this.width / 2 - 92 - 16;
      drawString(param0, this.font, this.columnType, var5, 32, 16777215);
      drawString(param0, this.font, this.columnHeight, var5 + 2 + 213 - this.font.width(this.columnHeight), 32, 16777215);
      super.render(param0, param1, param2, param3);
   }

   class DetailsList extends ObjectSelectionList<CreateFlatWorldScreen.DetailsList.Entry> {
      public DetailsList() {
         super(
            CreateFlatWorldScreen.this.minecraft,
            CreateFlatWorldScreen.this.width,
            CreateFlatWorldScreen.this.height,
            43,
            CreateFlatWorldScreen.this.height - 60,
            24
         );

         for (int var2 = 0; var2 < CreateFlatWorldScreen.this.generator.getLayersInfo().size(); var2++) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.Entry());
         }
      }

      public void setSelected(@Nullable CreateFlatWorldScreen.DetailsList.Entry param0) {
         super.setSelected(param0);
         CreateFlatWorldScreen.this.updateButtonValidity();
      }

      @Override
      protected int getScrollbarPosition() {
         return this.width - 70;
      }

      public void resetRows() {
         int var1 = this.children().indexOf(this.getSelected());
         this.clearEntries();

         for (int var2 = 0; var2 < CreateFlatWorldScreen.this.generator.getLayersInfo().size(); var2++) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.Entry());
         }

         List<CreateFlatWorldScreen.DetailsList.Entry> var2 = this.children();
         if (var1 >= 0 && var1 < var2.size()) {
            this.setSelected(var2.get(var1));
         }
      }

      class Entry extends ObjectSelectionList.Entry<CreateFlatWorldScreen.DetailsList.Entry> {
         @Override
         public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
            FlatLayerInfo var11 = CreateFlatWorldScreen.this.generator
               .getLayersInfo()
               .get(CreateFlatWorldScreen.this.generator.getLayersInfo().size() - param1 - 1);
            BlockState var12 = var11.getBlockState();
            ItemStack var13 = this.getDisplayItem(var12);
            this.blitSlot(param0, param3, param2, var13);
            CreateFlatWorldScreen.this.font.draw(param0, var13.getHoverName(), param3 + 18 + 5, param2 + 3, 16777215);
            Component var14;
            if (param1 == 0) {
               var14 = Component.translatable("createWorld.customize.flat.layer.top", var11.getHeight());
            } else if (param1 == CreateFlatWorldScreen.this.generator.getLayersInfo().size() - 1) {
               var14 = Component.translatable("createWorld.customize.flat.layer.bottom", var11.getHeight());
            } else {
               var14 = Component.translatable("createWorld.customize.flat.layer", var11.getHeight());
            }

            CreateFlatWorldScreen.this.font.draw(param0, var14, param3 + 2 + 213 - CreateFlatWorldScreen.this.font.width(var14), param2 + 3, 16777215);
         }

         private ItemStack getDisplayItem(BlockState param0) {
            Item var2 = param0.getBlock().asItem();
            if (var2 == Items.AIR) {
               if (param0.is(Blocks.WATER)) {
                  var2 = Items.WATER_BUCKET;
               } else if (param0.is(Blocks.LAVA)) {
                  var2 = Items.LAVA_BUCKET;
               }
            }

            return new ItemStack(var2);
         }

         @Override
         public Component getNarration() {
            FlatLayerInfo var1 = CreateFlatWorldScreen.this.generator
               .getLayersInfo()
               .get(CreateFlatWorldScreen.this.generator.getLayersInfo().size() - DetailsList.this.children().indexOf(this) - 1);
            ItemStack var2 = this.getDisplayItem(var1.getBlockState());
            return !var2.isEmpty() ? Component.translatable("narrator.select", var2.getHoverName()) : CommonComponents.EMPTY;
         }

         @Override
         public boolean mouseClicked(double param0, double param2, int param4) {
            if (param4 == 0) {
               DetailsList.this.setSelected(this);
               return true;
            } else {
               return false;
            }
         }

         private void blitSlot(PoseStack param0, int param1, int param2, ItemStack param3) {
            this.blitSlotBg(param0, param1 + 1, param2 + 1);
            if (!param3.isEmpty()) {
               CreateFlatWorldScreen.this.itemRenderer.renderGuiItem(param0, param3, param1 + 2, param2 + 2);
            }
         }

         private void blitSlotBg(PoseStack param0, int param1, int param2) {
            RenderSystem.setShaderTexture(0, GuiComponent.STATS_ICON_LOCATION);
            GuiComponent.blit(param0, param1, param2, 0, 0.0F, 0.0F, 18, 18, 128, 128);
         }
      }
   }
}
