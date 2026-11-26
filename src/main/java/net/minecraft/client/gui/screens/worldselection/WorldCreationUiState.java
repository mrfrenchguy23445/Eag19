package net.minecraft.client.gui.screens.worldselection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

public class WorldCreationUiState {
   private static final Component DEFAULT_WORLD_NAME = Component.translatable("selectWorld.newWorld");
   private final List<Consumer<WorldCreationUiState>> listeners = new ArrayList<>();
   private String name = DEFAULT_WORLD_NAME.getString();
   private WorldCreationUiState.SelectedGameMode gameMode = WorldCreationUiState.SelectedGameMode.SURVIVAL;
   private Difficulty difficulty = Difficulty.NORMAL;
   @Nullable
   private Boolean allowCheats;
   private String seed;
   private boolean generateStructures;
   private boolean bonusChest;
   private final Path savesFolder;
   private String targetFolder;
   private WorldCreationContext settings;
   private WorldCreationUiState.WorldTypeEntry worldType;
   private final List<WorldCreationUiState.WorldTypeEntry> normalPresetList = new ArrayList<>();
   private final List<WorldCreationUiState.WorldTypeEntry> altPresetList = new ArrayList<>();
   private GameRules gameRules = new GameRules();

   public WorldCreationUiState(Path param0, WorldCreationContext param1, Optional<ResourceKey<WorldPreset>> param2, OptionalLong param3) {
      this.savesFolder = param0;
      this.settings = param1;
      this.worldType = new WorldCreationUiState.WorldTypeEntry(findPreset(param1, param2).orElse(null));
      this.updatePresetLists();
      this.seed = param3.isPresent() ? Long.toString(param3.getAsLong()) : "";
      this.generateStructures = param1.options().generateStructures();
      this.bonusChest = param1.options().generateBonusChest();
      this.targetFolder = this.findResultFolder(this.name);
   }

   public void addListener(Consumer<WorldCreationUiState> param0) {
      this.listeners.add(param0);
   }

   public void onChanged() {
      boolean var1 = this.isBonusChest();
      if (var1 != this.settings.options().generateBonusChest()) {
         this.settings = this.settings.withOptions(l_param1 -> l_param1.withBonusChest(var1));
      }

      boolean var2 = this.isGenerateStructures();
      if (var2 != this.settings.options().generateStructures()) {
         this.settings = this.settings.withOptions(l_param1 -> l_param1.withStructures(var2));
      }

      for (Consumer<WorldCreationUiState> var4 : this.listeners) {
         var4.accept(this);
      }
   }

   public void setName(String param0) {
      this.name = param0;
      this.targetFolder = this.findResultFolder(param0);
      this.onChanged();
   }

   private String findResultFolder(String param0) {
      String var2 = param0.trim();

      try {
         return FileUtil.findAvailableName(this.savesFolder, !var2.isEmpty() ? var2 : DEFAULT_WORLD_NAME.getString(), "");
      } catch (Exception var5) {
         try {
            return FileUtil.findAvailableName(this.savesFolder, "World", "");
         } catch (IOException var3) {
            throw new RuntimeException("Could not create save folder", var3);
         }
      }
   }

   public String getName() {
      return this.name;
   }

   public String getTargetFolder() {
      return this.targetFolder;
   }

   public void setGameMode(WorldCreationUiState.SelectedGameMode param0) {
      this.gameMode = param0;
      this.onChanged();
   }

   public WorldCreationUiState.SelectedGameMode getGameMode() {
      return this.isDebug() ? WorldCreationUiState.SelectedGameMode.DEBUG : this.gameMode;
   }

   public void setDifficulty(Difficulty param0) {
      this.difficulty = param0;
      this.onChanged();
   }

   public Difficulty getDifficulty() {
      return this.isHardcore() ? Difficulty.HARD : this.difficulty;
   }

   public boolean isHardcore() {
      return this.getGameMode() == WorldCreationUiState.SelectedGameMode.HARDCORE;
   }

   public void setAllowCheats(boolean param0) {
      this.allowCheats = param0;
      this.onChanged();
   }

   public boolean isAllowCheats() {
      if (this.isDebug()) {
         return true;
      } else if (this.isHardcore()) {
         return false;
      } else {
         return this.allowCheats == null ? this.getGameMode() == WorldCreationUiState.SelectedGameMode.CREATIVE : this.allowCheats;
      }
   }

   public void setSeed(String param0) {
      this.seed = param0;
      this.settings = this.settings.withOptions(l_param0 -> l_param0.withSeed(WorldOptions.parseSeed(this.getSeed())));
      this.onChanged();
   }

   public String getSeed() {
      return this.seed;
   }

   public void setGenerateStructures(boolean param0) {
      this.generateStructures = param0;
      this.onChanged();
   }

   public boolean isGenerateStructures() {
      return this.isDebug() ? false : this.generateStructures;
   }

   public void setBonusChest(boolean param0) {
      this.bonusChest = param0;
      this.onChanged();
   }

   public boolean isBonusChest() {
      return !this.isDebug() && !this.isHardcore() ? this.bonusChest : false;
   }

   public void setSettings(WorldCreationContext param0) {
      this.settings = param0;
      this.updatePresetLists();
      this.onChanged();
   }

   public WorldCreationContext getSettings() {
      return this.settings;
   }

   public void updateDimensions(WorldCreationContext.DimensionsUpdater param0) {
      this.settings = this.settings.withDimensions(param0);
      this.onChanged();
   }

   protected boolean tryUpdateDataConfiguration(WorldDataConfiguration param0) {
      WorldDataConfiguration var2 = this.settings.dataConfiguration();
      if (var2.dataPacks().getEnabled().equals(param0.dataPacks().getEnabled()) && var2.enabledFeatures().equals(param0.enabledFeatures())) {
         this.settings = new WorldCreationContext(
            this.settings.options(),
            this.settings.datapackDimensions(),
            this.settings.selectedDimensions(),
            this.settings.worldgenRegistries(),
            this.settings.dataPackResources(),
            param0
         );
         return true;
      } else {
         return false;
      }
   }

   public boolean isDebug() {
      return this.settings.selectedDimensions().isDebug();
   }

   public void setWorldType(WorldCreationUiState.WorldTypeEntry param0) {
      this.worldType = param0;
      Holder<WorldPreset> var2 = param0.preset();
      if (var2 != null) {
         this.updateDimensions((l_param1, l_param2) -> var2.value().createWorldDimensions());
      }
   }

   public WorldCreationUiState.WorldTypeEntry getWorldType() {
      return this.worldType;
   }

   @Nullable
   public PresetEditor getPresetEditor() {
      Holder<WorldPreset> var1 = this.getWorldType().preset();
      return var1 != null ? PresetEditor.EDITORS.get(var1.unwrapKey()) : null;
   }

   public List<WorldCreationUiState.WorldTypeEntry> getNormalPresetList() {
      return this.normalPresetList;
   }

   public List<WorldCreationUiState.WorldTypeEntry> getAltPresetList() {
      return this.altPresetList;
   }

   private void updatePresetLists() {
      Registry<WorldPreset> var1 = this.getSettings().worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET);
      this.normalPresetList.clear();
      this.normalPresetList
         .addAll(getNonEmptyList(var1, WorldPresetTags.NORMAL).orElseGet(() -> var1.holders().map(WorldCreationUiState.WorldTypeEntry::new).toList()));
      this.altPresetList.clear();
      this.altPresetList.addAll(getNonEmptyList(var1, WorldPresetTags.EXTENDED).orElse(this.normalPresetList));
      Holder<WorldPreset> var2 = this.worldType.preset();
      if (var2 != null) {
         this.worldType = findPreset(this.getSettings(), var2.unwrapKey()).map(WorldCreationUiState.WorldTypeEntry::new).orElse(this.normalPresetList.get(0));
      }
   }

   private static Optional<Holder<WorldPreset>> findPreset(WorldCreationContext param0, Optional<ResourceKey<WorldPreset>> param1) {
      return param1.flatMap(l_param1 -> param0.worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET).getHolder((ResourceKey<WorldPreset>)l_param1));
   }

   private static Optional<List<WorldCreationUiState.WorldTypeEntry>> getNonEmptyList(Registry<WorldPreset> param0, TagKey<WorldPreset> param1) {
      return param0.getTag(param1)
         .map(l_param0 -> l_param0.stream().map(WorldCreationUiState.WorldTypeEntry::new).toList())
         .filter(l_param0 -> !l_param0.isEmpty());
   }

   public void setGameRules(GameRules param0) {
      this.gameRules = param0;
      this.onChanged();
   }

   public GameRules getGameRules() {
      return this.gameRules;
   }

   public enum SelectedGameMode {
      SURVIVAL("survival", GameType.SURVIVAL),
      HARDCORE("hardcore", GameType.SURVIVAL),
      CREATIVE("creative", GameType.CREATIVE),
      DEBUG("spectator", GameType.SPECTATOR);

      public final GameType gameType;
      public final Component displayName;
      private final Component info;

      SelectedGameMode(String param2, GameType param3) {
         this.gameType = param3;
         this.displayName = Component.translatable("selectWorld.gameMode." + param2);
         this.info = Component.translatable("selectWorld.gameMode." + param2 + ".info");
      }

      public Component getInfo() {
         return this.info;
      }
   }

   public record WorldTypeEntry(@Nullable Holder<WorldPreset> preset) {
      private static final Component CUSTOM_WORLD_DESCRIPTION = Component.translatable("generator.custom");

      public Component describePreset() {
         return Optional.ofNullable(this.preset)
            .flatMap(Holder::unwrapKey)
            .map(l_param0 -> (Component)Component.translatable(l_param0.location().toLanguageKey("generator")))
            .orElse(CUSTOM_WORLD_DESCRIPTION);
      }

      public boolean isAmplified() {
         return Optional.ofNullable(this.preset).flatMap(Holder::unwrapKey).filter(l_param0 -> l_param0.equals(WorldPresets.AMPLIFIED)).isPresent();
      }
   }
}
