package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;

public class ExperimentsScreen extends Screen {
   private static final int MAIN_CONTENT_WIDTH = 310;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final Screen parent;
   private final PackRepository packRepository;
   private final Consumer<PackRepository> output;
   private final Object2BooleanMap<Pack> packs = new Object2BooleanLinkedOpenHashMap<>();

   protected ExperimentsScreen(Screen param0, PackRepository param1, Consumer<PackRepository> param2) {
      super(Component.translatable("experiments_screen.title"));
      this.parent = param0;
      this.packRepository = param1;
      this.output = param2;

      for (Pack var5 : param1.getAvailablePacks()) {
         if (var5.getPackSource() == PackSource.FEATURE) {
            this.packs.put(var5, param1.getSelectedPacks().contains(var5));
         }
      }
   }

   @Override
   protected void init() {
      this.layout.addToHeader(new StringWidget(Component.translatable("selectWorld.experiments"), this.font));
      GridLayout.RowHelper var1 = this.layout.addToContents(new GridLayout()).createRowHelper(1);
      var1.addChild(
         new MultiLineTextWidget(Component.translatable("selectWorld.experiments.info").withStyle(ChatFormatting.RED), this.font).setMaxWidth(310),
         var1.newCellSettings().paddingBottom(15)
      );
      SwitchGrid.Builder var2 = SwitchGrid.builder(310).withInfoUnderneath(2, true).withRowSpacing(4);
      this.packs
         .forEach(
            (l_param1, l_param2) -> var2.addSwitch(
                  getHumanReadableTitle(l_param1), () -> this.packs.getBoolean(l_param1), l2_param1 -> this.packs.put(l_param1, l2_param1.booleanValue())
               )
               .withInfo(l_param1.getDescription())
         );
      var2.build(var1::addChild);
      GridLayout.RowHelper var3 = this.layout.addToFooter(new GridLayout().columnSpacing(10)).createRowHelper(2);
      var3.addChild(Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.onDone()).build());
      var3.addChild(Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.onClose()).build());
      this.layout.visitWidgets(l_param1 -> {
         AbstractWidget var10000 = this.addRenderableWidget(l_param1);
      });
      this.repositionElements();
   }

   private static Component getHumanReadableTitle(Pack param0) {
      String var1 = "dataPack." + param0.getId() + ".name";
      return I18n.exists(var1) ? Component.translatable(var1) : param0.getTitle();
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   private void onDone() {
      List<Pack> var1 = new ArrayList<>(this.packRepository.getSelectedPacks());
      List<Pack> var2 = new ArrayList<>();
      this.packs.forEach((l_param2, l_param3) -> {
         var1.remove(l_param2);
         if (l_param3) {
            var2.add(l_param2);
         }
      });
      var1.addAll(Lists.reverse(var2));
      this.packRepository.setSelected(var1.stream().map(Pack::getId).toList());
      this.output.accept(this.packRepository);
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
      RenderSystem.setShaderColor(0.125F, 0.125F, 0.125F, 1.0F);
      int var5 = 32;
      blit(
         param0, 0, this.layout.getHeaderHeight(), 0.0F, 0.0F, this.width, this.height - this.layout.getHeaderHeight() - this.layout.getFooterHeight(), 32, 32
      );
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      super.render(param0, param1, param2, param3);
   }
}
