package net.minecraft.client.gui.screens;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FlatLevelGeneratorPresetTags;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.slf4j.Logger;

public class PresetFlatWorldScreen extends Screen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int SLOT_TEX_SIZE = 128;
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   private static final ResourceKey<Biome> DEFAULT_BIOME = Biomes.PLAINS;
   public static final Component UNKNOWN_PRESET = Component.translatable("flat_world_preset.unknown");
   private final CreateFlatWorldScreen parent;
   private Component shareText;
   private Component listText;
   private PresetFlatWorldScreen.PresetsList list;
   private Button selectButton;
   EditBox export;
   FlatLevelGeneratorSettings settings;

   public PresetFlatWorldScreen(CreateFlatWorldScreen param0) {
      super(Component.translatable("createWorld.customize.presets.title"));
      this.parent = param0;
   }

   @Nullable
   private static FlatLayerInfo getLayerInfoFromString(HolderGetter<Block> param0, String param1, int param2) {
      List<String> var3 = Splitter.on('*').limit(2).splitToList(param1);
      int var4;
      String var5;
      if (var3.size() == 2) {
         var5 = var3.get(1);

         try {
            var4 = Math.max(Integer.parseInt(var3.get(0)), 0);
         } catch (NumberFormatException var6) {
            LOGGER.error("Error while parsing flat world string", var6);
            return null;
         }
      } else {
         var5 = var3.get(0);
         var4 = 1;
      }

      int var6 = Math.min(param2 + var4, DimensionType.Y_SIZE);
      int var7 = var6 - param2;

      Optional<Holder.Reference<Block>> var8;
      try {
         var8 = param0.get(ResourceKey.create(Registries.BLOCK, new ResourceLocation(var5)));
      } catch (Exception var9) {
         LOGGER.error("Error while parsing flat world string", var9);
         return null;
      }

      if (var8.isEmpty()) {
         LOGGER.error("Error while parsing flat world string => Unknown block, {}", var5);
         return null;
      } else {
         return new FlatLayerInfo(var7, var8.get().value());
      }
   }

   private static List<FlatLayerInfo> getLayersInfoFromString(HolderGetter<Block> param0, String param1) {
      List<FlatLayerInfo> var2 = Lists.newArrayList();
      String[] var3 = param1.split(",");
      int var4 = 0;

      for (String var8 : var3) {
         FlatLayerInfo var9 = getLayerInfoFromString(param0, var8, var4);
         if (var9 == null) {
            return Collections.emptyList();
         }

         var2.add(var9);
         var4 += var9.getHeight();
      }

      return var2;
   }

   public static FlatLevelGeneratorSettings fromString(
      HolderGetter<Block> param0,
      HolderGetter<Biome> param1,
      HolderGetter<StructureSet> param2,
      HolderGetter<PlacedFeature> param3,
      String param4,
      FlatLevelGeneratorSettings param5
   ) {
      Iterator<String> var6 = Splitter.on(';').split(param4).iterator();
      if (!var6.hasNext()) {
         return FlatLevelGeneratorSettings.getDefault(param1, param2, param3);
      }

      List<FlatLayerInfo> var7 = getLayersInfoFromString(param0, var6.next());
      if (var7.isEmpty()) {
         return FlatLevelGeneratorSettings.getDefault(param1, param2, param3);
      }

      Holder.Reference<Biome> var8 = param1.getOrThrow(DEFAULT_BIOME);
      Holder<Biome> var9 = var8;
      if (var6.hasNext()) {
         String var10 = var6.next();
         var9 = Optional.ofNullable(ResourceLocation.tryParse(var10))
            .map(l_param0 -> ResourceKey.create(Registries.BIOME, l_param0))
            .flatMap(param1::get)
            .orElseGet(() -> {
               LOGGER.warn("Invalid biome: {}", var10);
               return var8;
            });
      }

      return param5.withBiomeAndLayers(var7, param5.structureOverrides(), var9);
   }

   static String save(FlatLevelGeneratorSettings param0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < param0.getLayersInfo().size(); var2++) {
         if (var2 > 0) {
            var1.append(",");
         }

         var1.append(param0.getLayersInfo().get(var2));
      }

      var1.append(";");
      var1.append(param0.getBiome().unwrapKey().map(ResourceKey::location).orElseThrow(() -> new IllegalStateException("Biome not registered")));
      return var1.toString();
   }

   @Override
   protected void init() {
      this.shareText = Component.translatable("createWorld.customize.presets.share");
      this.listText = Component.translatable("createWorld.customize.presets.list");
      this.export = new EditBox(this.font, 50, 40, this.width - 100, 20, this.shareText);
      this.export.setMaxLength(1230);
      WorldCreationContext var1 = this.parent.parent.getUiState().getSettings();
      RegistryAccess var2 = var1.worldgenLoadContext();
      FeatureFlagSet var3 = var1.dataConfiguration().enabledFeatures();
      HolderGetter<Biome> var4 = var2.lookupOrThrow(Registries.BIOME);
      HolderGetter<StructureSet> var5 = var2.lookupOrThrow(Registries.STRUCTURE_SET);
      HolderGetter<PlacedFeature> var6 = var2.lookupOrThrow(Registries.PLACED_FEATURE);
      HolderGetter<Block> var7 = var2.lookupOrThrow(Registries.BLOCK).filterFeatures(var3);
      this.export.setValue(save(this.parent.settings()));
      this.settings = this.parent.settings();
      this.addWidget(this.export);
      this.list = new PresetFlatWorldScreen.PresetsList(var2, var3);
      this.addWidget(this.list);
      this.selectButton = this.addRenderableWidget(Button.builder(Component.translatable("createWorld.customize.presets.select"), l_param4 -> {
         FlatLevelGeneratorSettings l_var6 = fromString(var7, var4, var5, var6, this.export.getValue(), this.settings);
         this.parent.setConfig(l_var6);
         this.minecraft.setScreen(this.parent);
      }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.minecraft.setScreen(this.parent))
            .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
            .build()
      );
      this.updateButtonValidity(this.list.getSelected() != null);
   }

   @Override
   public boolean mouseScrolled(double param0, double param2, double param4) {
      return this.list.mouseScrolled(param0, param2, param4);
   }

   @Override
   public void resize(Minecraft param0, int param1, int param2) {
      String var4 = this.export.getValue();
      this.init(param0, param1, param2);
      this.export.setValue(var4);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      this.list.render(param0, param1, param2, param3);
      param0.pushPose();
      param0.translate(0.0F, 0.0F, 400.0F);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 8, 16777215);
      drawString(param0, this.font, this.shareText, 50, 30, 10526880);
      drawString(param0, this.font, this.listText, 50, 70, 10526880);
      param0.popPose();
      this.export.render(param0, param1, param2, param3);
      super.render(param0, param1, param2, param3);
   }

   @Override
   public void tick() {
      this.export.tick();
      super.tick();
   }

   public void updateButtonValidity(boolean param0) {
      this.selectButton.active = param0 || this.export.getValue().length() > 1;
   }

   class PresetsList extends ObjectSelectionList<PresetFlatWorldScreen.PresetsList.Entry> {
      public PresetsList(RegistryAccess param1, FeatureFlagSet param2) {
         super(
            PresetFlatWorldScreen.this.minecraft,
            PresetFlatWorldScreen.this.width,
            PresetFlatWorldScreen.this.height,
            80,
            PresetFlatWorldScreen.this.height - 37,
            24
         );

         for (Holder<FlatLevelGeneratorPreset> var5 : param1.registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
            .getTagOrEmpty(FlatLevelGeneratorPresetTags.VISIBLE)) {
            Set<Block> var6 = var5.value()
               .settings()
               .getLayersInfo()
               .stream()
               .map(l_param0 -> l_param0.getBlockState().getBlock())
               .filter(l_param1 -> !l_param1.isEnabled(param2))
               .collect(Collectors.toSet());
            if (!var6.isEmpty()) {
               PresetFlatWorldScreen.LOGGER
                  .info(
                     "Discarding flat world preset {} since it contains experimental blocks {}",
                     var5.unwrapKey().map(l_param0 -> l_param0.location().toString()).orElse("<unknown>"),
                     var6
                  );
            } else {
               this.addEntry(new PresetFlatWorldScreen.PresetsList.Entry(var5));
            }
         }
      }

      public void setSelected(@Nullable PresetFlatWorldScreen.PresetsList.Entry param0) {
         super.setSelected(param0);
         PresetFlatWorldScreen.this.updateButtonValidity(param0 != null);
      }

      @Override
      public boolean keyPressed(int param0, int param1, int param2) {
         if (super.keyPressed(param0, param1, param2)) {
            return true;
         }

         if ((param0 == 257 || param0 == 335) && this.getSelected() != null) {
            this.getSelected().select();
         }

         return false;
      }

      public class Entry extends ObjectSelectionList.Entry<PresetFlatWorldScreen.PresetsList.Entry> {
         private final FlatLevelGeneratorPreset preset;
         private final Component name;

         public Entry(Holder<FlatLevelGeneratorPreset> param1) {
            this.preset = param1.value();
            this.name = param1.unwrapKey()
               .map(l_param0 -> (Component)Component.translatable(l_param0.location().toLanguageKey("flat_world_preset")))
               .orElse(PresetFlatWorldScreen.UNKNOWN_PRESET);
         }

         @Override
         public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
            this.blitSlot(param0, param3, param2, this.preset.displayItem().value());
            PresetFlatWorldScreen.this.font.draw(param0, this.name, param3 + 18 + 5, param2 + 6, 16777215);
         }

         @Override
         public boolean mouseClicked(double param0, double param2, int param4) {
            if (param4 == 0) {
               this.select();
            }

            return false;
         }

         void select() {
            PresetsList.this.setSelected(this);
            PresetFlatWorldScreen.this.settings = this.preset.settings();
            PresetFlatWorldScreen.this.export.setValue(PresetFlatWorldScreen.save(PresetFlatWorldScreen.this.settings));
            PresetFlatWorldScreen.this.export.moveCursorToStart();
         }

         private void blitSlot(PoseStack param0, int param1, int param2, Item param3) {
            this.blitSlotBg(param0, param1 + 1, param2 + 1);
            PresetFlatWorldScreen.this.itemRenderer.renderGuiItem(param0, new ItemStack(param3), param1 + 2, param2 + 2);
         }

         private void blitSlotBg(PoseStack param0, int param1, int param2) {
            RenderSystem.setShaderTexture(0, GuiComponent.STATS_ICON_LOCATION);
            GuiComponent.blit(param0, param1, param2, 0, 0.0F, 0.0F, 18, 18, 128, 128);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.name);
         }
      }
   }
}
