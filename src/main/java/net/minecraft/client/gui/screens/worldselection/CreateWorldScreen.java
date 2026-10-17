package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class CreateWorldScreen extends Screen {
   private static final int GROUP_BOTTOM = 1;
   private static final int TAB_COLUMN_WIDTH = 210;
   private static final int FOOTER_HEIGHT = 36;
   private static final int TEXT_INDENT = 1;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TEMP_WORLD_PREFIX = "mcworld-";
   static final Component GAME_MODEL_LABEL = Component.translatable("selectWorld.gameMode");
   static final Component NAME_LABEL = Component.translatable("selectWorld.enterName");
   static final Component EXPERIMENTS_LABEL = Component.translatable("selectWorld.experiments");
   static final Component ALLOW_CHEATS_INFO = Component.translatable("selectWorld.allowCommands.info");
   private static final Component PREPARING_WORLD_DATA = Component.translatable("createWorld.preparing");
   private static final int HORIZONTAL_BUTTON_SPACING = 10;
   private static final int VERTICAL_BUTTON_SPACING = 8;
   public static final ResourceLocation HEADER_SEPERATOR = new ResourceLocation("textures/gui/header_separator.png");
   public static final ResourceLocation FOOTER_SEPERATOR = new ResourceLocation("textures/gui/footer_separator.png");
   final WorldCreationUiState uiState;
   private final TabManager tabManager = new TabManager(this::addRenderableWidget, l_param1 -> this.removeWidget(l_param1));
   private boolean recreated;
   @Nullable
   private final Screen lastScreen;
   @Nullable
   private Path tempDataPackDir;
   @Nullable
   private PackRepository tempDataPackRepository;
   @Nullable
   private GridLayout bottomButtons;
   @Nullable
   private TabNavigationBar tabNavigationBar;

   public static void openFresh(Minecraft param0, @Nullable Screen param1) {
      queueLoadScreen(param0, PREPARING_WORLD_DATA);
      PackRepository var2 = new PackRepository(new ServerPacksSource());
      WorldLoader.InitConfig var3 = createDefaultLoadConfig(var2, WorldDataConfiguration.DEFAULT);
      CompletableFuture<WorldCreationContext> var4 = WorldLoader.load(
         var3,
         l_param0 -> new WorldLoader.DataLoadOutput<>(
            new CreateWorldScreen.DataPackReloadCookie(
               new WorldGenSettings(WorldOptions.defaultWithRandomSeed(), WorldPresets.createNormalWorldDimensions(l_param0.datapackWorldgen())),
               l_param0.dataConfiguration()
            ),
            l_param0.datapackDimensions()
         ),
         (l_param0, l_param1, l_param2, l_param3) -> {
            l_param0.close();
            return new WorldCreationContext(l_param3.worldGenSettings(), l_param2, l_param1, l_param3.dataConfiguration());
         },
         Util.backgroundExecutor(),
         param0
      );
      param0.managedBlock(var4::isDone);
      param0.setScreen(new CreateWorldScreen(param0, param1, var4.join(), Optional.of(WorldPresets.NORMAL), OptionalLong.empty()));
   }

   public static CreateWorldScreen createFromExisting(
      Minecraft param0, @Nullable Screen param1, LevelSettings param2, WorldCreationContext param3, @Nullable Path param4
   ) {
      CreateWorldScreen var5 = new CreateWorldScreen(
         param0, param1, param3, WorldPresets.fromSettings(param3.selectedDimensions().dimensions()), OptionalLong.of(param3.options().seed())
      );
      var5.recreated = true;
      var5.uiState.setName(param2.levelName());
      var5.uiState.setAllowCheats(param2.allowCommands());
      var5.uiState.setDifficulty(param2.difficulty());
      var5.uiState.getGameRules().assignFrom(param2.gameRules(), null);
      if (param2.hardcore()) {
         var5.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.HARDCORE);
      } else if (param2.gameType().isSurvival()) {
         var5.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
      } else if (param2.gameType().isCreative()) {
         var5.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
      }

      var5.tempDataPackDir = param4;
      return var5;
   }

   private CreateWorldScreen(
      Minecraft param0, @Nullable Screen param1, WorldCreationContext param2, Optional<ResourceKey<WorldPreset>> param3, OptionalLong param4
   ) {
      super(Component.translatable("selectWorld.create"));
      this.lastScreen = param1;
      this.uiState = new WorldCreationUiState(param0.getLevelSource().getBaseDir(), param2, param3, param4);
   }

   public WorldCreationUiState getUiState() {
      return this.uiState;
   }

   @Override
   public void tick() {
      this.tabManager.tickCurrent();
   }

   @Override
   protected void init() {
      this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
         .addTabs(new CreateWorldScreen.GameTab(), new CreateWorldScreen.WorldTab(), new CreateWorldScreen.MoreTab())
         .build();
      this.addRenderableWidget(this.tabNavigationBar);
      this.bottomButtons = new GridLayout().columnSpacing(10);
      GridLayout.RowHelper var1 = this.bottomButtons.createRowHelper(2);
      var1.addChild(Button.builder(Component.translatable("selectWorld.create"), l_param0 -> this.onCreate()).build());
      var1.addChild(Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.popScreen()).build());
      this.bottomButtons.visitWidgets(l_param0 -> {
         l_param0.setTabOrderGroup(1);
         this.addRenderableWidget(l_param0);
      });
      this.tabNavigationBar.selectTab(0, false);
      this.uiState.onChanged();
      this.repositionElements();
   }

   @Override
   public void repositionElements() {
      if (this.tabNavigationBar != null && this.bottomButtons != null) {
         this.tabNavigationBar.setWidth(this.width);
         this.tabNavigationBar.arrangeElements();
         this.bottomButtons.arrangeElements();
         FrameLayout.centerInRectangle(this.bottomButtons, 0, this.height - 36, this.width, 36);
         int var1 = this.tabNavigationBar.getRectangle().bottom();
         ScreenRectangle var2 = new ScreenRectangle(0, var1, this.width, this.bottomButtons.getY() - var1);
         this.tabManager.setTabArea(var2);
      }
   }

   private static void queueLoadScreen(Minecraft param0, Component param1) {
      param0.forceSetScreen(new GenericDirtMessageScreen(param1));
   }

   private void onCreate() {
      WorldCreationContext var1 = this.uiState.getSettings();
      WorldDimensions.Complete var2 = var1.selectedDimensions().bake(var1.datapackDimensions());
      LayeredRegistryAccess<RegistryLayer> var3 = var1.worldgenRegistries().replaceFrom(RegistryLayer.DIMENSIONS, var2.dimensionsRegistryAccess());
      Lifecycle var4 = FeatureFlags.isExperimental(var1.dataConfiguration().enabledFeatures()) ? Lifecycle.experimental() : Lifecycle.stable();
      Lifecycle var5 = var3.compositeAccess().allRegistriesLifecycle();
      Lifecycle var6 = var5.add(var4);
      boolean var7 = !this.recreated && var5 == Lifecycle.stable();
      WorldOpenFlows.confirmWorldCreation(this.minecraft, this, var6, () -> this.createNewWorld(var2.specialWorldProperty(), var3, var6), var7);
   }

   private void createNewWorld(PrimaryLevelData.SpecialWorldProperty param0, LayeredRegistryAccess<RegistryLayer> param1, Lifecycle param2) {
      queueLoadScreen(this.minecraft, PREPARING_WORLD_DATA);
      Optional<LevelStorageSource.LevelStorageAccess> var4 = this.createNewWorldDirectory();
      if (!var4.isEmpty()) {
         this.removeTempDataPackDir();
         boolean var5 = param0 == PrimaryLevelData.SpecialWorldProperty.DEBUG;
         WorldCreationContext var6 = this.uiState.getSettings();
         LevelSettings var7 = this.createLevelSettings(var5);
         WorldData var8 = new PrimaryLevelData(var7, var6.options(), param0, param2);
         this.minecraft.createWorldOpenFlows().createLevelFromExistingSettings(var4.get(), var6.dataPackResources(), param1, var8);
      }
   }

   private LevelSettings createLevelSettings(boolean param0) {
      String var2 = this.uiState.getName().trim();
      if (param0) {
         GameRules var3 = new GameRules();
         var3.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
         return new LevelSettings(var2, GameType.SPECTATOR, false, Difficulty.PEACEFUL, true, var3, WorldDataConfiguration.DEFAULT);
      } else {
         return new LevelSettings(
            var2,
            this.uiState.getGameMode().gameType,
            this.uiState.isHardcore(),
            this.uiState.getDifficulty(),
            this.uiState.isAllowCheats(),
            this.uiState.getGameRules(),
            this.uiState.getSettings().dataConfiguration()
         );
      }
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (this.tabNavigationBar.keyPressed(param0)) {
         return true;
      }

      if (super.keyPressed(param0, param1, param2)) {
         return true;
      }

      if (param0 != 257 && param0 != 335) {
         return false;
      }

      this.onCreate();
      return true;
   }

   @Override
   public void onClose() {
      this.popScreen();
   }

   public void popScreen() {
      this.minecraft.setScreen(this.lastScreen);
      this.removeTempDataPackDir();
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      RenderSystem.setShaderTexture(0, FOOTER_SEPERATOR);
      blit(param0, 0, Mth.roundToward(this.height - 36 - 2, 2), 0.0F, 0.0F, this.width, 2, 32, 2);
      super.render(param0, param1, param2, param3);
   }

   @Override
   public void renderDirtBackground(PoseStack param0) {
      RenderSystem.setShaderTexture(0, LIGHT_DIRT_BACKGROUND);
      int var2 = 32;
      blit(param0, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
   }

   @Override
   protected <T extends GuiEventListener & NarratableEntry> T addWidget(T param0) {
      return super.addWidget(param0);
   }

   @Override
   protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T param0) {
      return super.addRenderableWidget(param0);
   }

   @Nullable
   private Path getTempDataPackDir() {
      if (this.tempDataPackDir == null) {
         try {
            this.tempDataPackDir = Files.createTempDirectory("mcworld-");
         } catch (IOException var1) {
            LOGGER.warn("Failed to create temporary dir", var1);
            SystemToast.onPackCopyFailure(this.minecraft, this.uiState.getTargetFolder());
            this.popScreen();
         }
      }

      return this.tempDataPackDir;
   }

   void openExperimentsScreen(WorldDataConfiguration param0) {
      Pair<Path, PackRepository> var2 = this.getDataPackSelectionSettings(param0);
      if (var2 != null) {
         this.minecraft
            .setScreen(new ExperimentsScreen(this, var2.getSecond(), l_param0 -> this.tryApplyNewDataPacks(l_param0, false, this::openExperimentsScreen)));
      }
   }

   void openDataPackSelectionScreen(WorldDataConfiguration param0) {
      Pair<Path, PackRepository> var2 = this.getDataPackSelectionSettings(param0);
      if (var2 != null) {
         this.minecraft
            .setScreen(
               new PackSelectionScreen(
                  var2.getSecond(),
                  l_param0 -> this.tryApplyNewDataPacks(l_param0, true, this::openDataPackSelectionScreen),
                  var2.getFirst(),
                  Component.translatable("dataPack.title")
               )
            );
      }
   }

   private void tryApplyNewDataPacks(PackRepository param0, boolean param1, Consumer<WorldDataConfiguration> param2) {
      List<String> var4 = ImmutableList.copyOf(param0.getSelectedIds());
      List<String> var5 = param0.getAvailableIds().stream().filter(l_param1 -> !var4.contains(l_param1)).collect(ImmutableList.toImmutableList());
      WorldDataConfiguration var6 = new WorldDataConfiguration(new DataPackConfig(var4, var5), this.uiState.getSettings().dataConfiguration().enabledFeatures());
      if (this.uiState.tryUpdateDataConfiguration(var6)) {
         this.minecraft.setScreen(this);
      } else {
         FeatureFlagSet var7 = param0.getRequestedFeatureFlags();
         if (FeatureFlags.isExperimental(var7) && param1) {
            this.minecraft.setScreen(new ConfirmExperimentalFeaturesScreen(param0.getSelectedPacks(), l_param3 -> {
               if (l_param3) {
                  this.applyNewPackConfig(param0, var6, param2);
               } else {
                  param2.accept(this.uiState.getSettings().dataConfiguration());
               }
            }));
         } else {
            this.applyNewPackConfig(param0, var6, param2);
         }
      }
   }

   private void applyNewPackConfig(PackRepository param0, WorldDataConfiguration param1, Consumer<WorldDataConfiguration> param2) {
      this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("dataPack.validation.working")));
      WorldLoader.InitConfig var4 = createDefaultLoadConfig(param0, param1);
      WorldLoader.<CreateWorldScreen.DataPackReloadCookie, WorldCreationContext>load(
            var4,
            l_param0 -> {
               if (l_param0.datapackWorldgen().registryOrThrow(Registries.WORLD_PRESET).size() == 0) {
                  throw new IllegalStateException("Needs at least one world preset to continue");
               }

               if (l_param0.datapackWorldgen().registryOrThrow(Registries.BIOME).size() == 0) {
                  throw new IllegalStateException("Needs at least one biome continue");
               }

               WorldCreationContext l_var2 = this.uiState.getSettings();
               DynamicOps<JsonElement> l_var3 = RegistryOps.create(JsonOps.INSTANCE, l_var2.worldgenLoadContext());
               DataResult<JsonElement> l_var4 = WorldGenSettings.encode(l_var3, l_var2.options(), l_var2.selectedDimensions()).setLifecycle(Lifecycle.stable());
               DynamicOps<JsonElement> l_var5 = RegistryOps.create(JsonOps.INSTANCE, l_param0.datapackWorldgen());
               WorldGenSettings l_var6 = l_var4.<WorldGenSettings>flatMap(l2_param1 -> WorldGenSettings.CODEC.parse(l_var5, l2_param1))
                  .getOrThrow(false, Util.prefix("Error parsing worldgen settings after loading data packs: ", LOGGER::error));
               return new WorldLoader.DataLoadOutput<>(
                  new CreateWorldScreen.DataPackReloadCookie(l_var6, l_param0.dataConfiguration()), l_param0.datapackDimensions()
               );
            },
            (l_param0, l_param1, l_param2, l_param3) -> {
               l_param0.close();
               return new WorldCreationContext(l_param3.worldGenSettings(), l_param2, l_param1, l_param3.dataConfiguration());
            },
            Util.backgroundExecutor(),
            this.minecraft
         )
         .thenAcceptAsync(this.uiState::setSettings, this.minecraft)
         .handle(
            (l_param1, l_param2) -> {
               if (l_param2 != null) {
                  LOGGER.warn("Failed to validate datapack", l_param2);
                  this.minecraft
                     .setScreen(
                        new ConfirmScreen(
                           l2_param1 -> {
                              if (l2_param1) {
                                 param2.accept(this.uiState.getSettings().dataConfiguration());
                              } else {
                                 param2.accept(WorldDataConfiguration.DEFAULT);
                              }
                           },
                           Component.translatable("dataPack.validation.failed"),
                           CommonComponents.EMPTY,
                           Component.translatable("dataPack.validation.back"),
                           Component.translatable("dataPack.validation.reset")
                        )
                     );
               } else {
                  this.minecraft.setScreen(this);
               }

               return null;
            }
         );
   }

   private static WorldLoader.InitConfig createDefaultLoadConfig(PackRepository param0, WorldDataConfiguration param1) {
      WorldLoader.PackConfig var2 = new WorldLoader.PackConfig(param0, param1, false, true);
      return new WorldLoader.InitConfig(var2, Commands.CommandSelection.INTEGRATED, 2);
   }

   private void removeTempDataPackDir() {
      if (this.tempDataPackDir != null) {
         try (Stream<Path> var1 = Files.walk(this.tempDataPackDir)) {
            var1.sorted(Comparator.reverseOrder()).forEach(l_param0 -> {
               try {
                  Files.delete(l_param0);
               } catch (IOException l_var1) {
                  LOGGER.warn("Failed to remove temporary file {}", l_param0, l_var1);
               }
            });
         } catch (IOException var1) {
            LOGGER.warn("Failed to list temporary dir {}", this.tempDataPackDir);
         }

         this.tempDataPackDir = null;
      }
   }

   private static void copyBetweenDirs(Path param0, Path param1, Path param2) {
      try {
         Util.copyBetweenDirs(param0, param1, param2);
      } catch (IOException var3) {
         LOGGER.warn("Failed to copy datapack file from {} to {}", param2, param1);
         throw new UncheckedIOException(var3);
      }
   }

   private Optional<LevelStorageSource.LevelStorageAccess> createNewWorldDirectory() {
      String var1 = this.uiState.getTargetFolder();

      try {
         LevelStorageSource.LevelStorageAccess var2 = this.minecraft.getLevelSource().createAccess(var1);
         if (this.tempDataPackDir == null) {
            return Optional.of(var2);
         }

         try (Stream<Path> var3 = Files.walk(this.tempDataPackDir)) {
            Path var4 = var2.getLevelPath(LevelResource.DATAPACK_DIR);
            FileUtil.createDirectoriesSafe(var4);
            var3.filter(l_param0 -> !l_param0.equals(this.tempDataPackDir)).forEach(l_param1 -> copyBetweenDirs(this.tempDataPackDir, var4, l_param1));
            return Optional.of(var2);
         } catch (IOException | UncheckedIOException var3) {
            LOGGER.warn("Failed to copy datapacks to world {}", var1, var3);
            var2.close();
         }
      } catch (IOException | UncheckedIOException var2) {
         LOGGER.warn("Failed to create access for {}", var1, var2);
      }

      SystemToast.onPackCopyFailure(this.minecraft, var1);
      this.popScreen();
      return Optional.empty();
   }

   @Nullable
   public static Path createTempDataPackDirFromExistingWorld(Path param0, Minecraft param1) {
      MutableObject<Path> var2 = new MutableObject<>();

      try (Stream<Path> var3 = Files.walk(param0)) {
         var3.filter(l_param1 -> !l_param1.equals(param0)).forEach(l_param2 -> {
            Path l_var3 = var2.getValue();
            if (l_var3 == null) {
               try {
                  l_var3 = Files.createTempDirectory("mcworld-");
               } catch (IOException l_var4) {
                  LOGGER.warn("Failed to create temporary dir");
                  throw new UncheckedIOException(l_var4);
               }

               var2.setValue(l_var3);
            }

            copyBetweenDirs(param0, l_var3, l_param2);
         });
      } catch (IOException | UncheckedIOException var3) {
         LOGGER.warn("Failed to copy datapacks from world {}", param0, var3);
         SystemToast.onPackCopyFailure(param1, param0.toString());
         return null;
      }

      return var2.getValue();
   }

   @Nullable
   private Pair<Path, PackRepository> getDataPackSelectionSettings(WorldDataConfiguration param0) {
      Path var2 = this.getTempDataPackDir();
      if (var2 != null) {
         if (this.tempDataPackRepository == null) {
            this.tempDataPackRepository = ServerPacksSource.createPackRepository(var2);
            this.tempDataPackRepository.reload();
         }

         this.tempDataPackRepository.setSelected(param0.dataPacks().getEnabled());
         return Pair.of(var2, this.tempDataPackRepository);
      } else {
         return null;
      }
   }

   record DataPackReloadCookie(WorldGenSettings worldGenSettings, WorldDataConfiguration dataConfiguration) {
   }

   class GameTab extends GridLayoutTab {
      private static final Component TITLE = Component.translatable("createWorld.tab.game.title");
      private static final Component ALLOW_CHEATS = Component.translatable("selectWorld.allowCommands");
      private final EditBox nameEdit;

      GameTab() {
         super(TITLE);
         GridLayout.RowHelper var2 = this.layout.rowSpacing(8).createRowHelper(1);
         LayoutSettings var3 = var2.newCellSettings();
         GridLayout.RowHelper var4 = new GridLayout().rowSpacing(4).createRowHelper(1);
         var4.addChild(new StringWidget(CreateWorldScreen.NAME_LABEL, CreateWorldScreen.this.minecraft.font), var4.newCellSettings().paddingLeft(1));
         this.nameEdit = var4.addChild(
            new EditBox(CreateWorldScreen.this.font, 0, 0, 208, 20, Component.translatable("selectWorld.enterName")), var4.newCellSettings().padding(1)
         );
         this.nameEdit.setValue(CreateWorldScreen.this.uiState.getName());
         this.nameEdit.setResponder(CreateWorldScreen.this.uiState::setName);
         CreateWorldScreen.this.uiState
            .addListener(
               l_param0 -> this.nameEdit
                  .setTooltip(
                     Tooltip.create(
                        Component.translatable("selectWorld.targetFolder", Component.literal(l_param0.getTargetFolder()).withStyle(ChatFormatting.ITALIC))
                     )
                  )
            );
         CreateWorldScreen.this.setInitialFocus(this.nameEdit);
         var2.addChild(var4.getGrid(), var2.newCellSettings().alignHorizontallyCenter());
         CycleButton<WorldCreationUiState.SelectedGameMode> var5 = var2.addChild(
            CycleButton.<WorldCreationUiState.SelectedGameMode>builder(l_param0 -> l_param0.displayName)
               .withValues(
                  WorldCreationUiState.SelectedGameMode.SURVIVAL,
                  WorldCreationUiState.SelectedGameMode.HARDCORE,
                  WorldCreationUiState.SelectedGameMode.CREATIVE
               )
               .create(0, 0, 210, 20, CreateWorldScreen.GAME_MODEL_LABEL, (l_param0, l_param1) -> CreateWorldScreen.this.uiState.setGameMode(l_param1)),
            var3
         );
         CreateWorldScreen.this.uiState.addListener(l_param1 -> {
            var5.setValue(l_param1.getGameMode());
            var5.active = !l_param1.isDebug();
            var5.setTooltip(Tooltip.create(l_param1.getGameMode().getInfo()));
         });
         CycleButton<Difficulty> var6 = var2.addChild(
            CycleButton.builder(Difficulty::getDisplayName)
               .withValues(Difficulty.values())
               .create(
                  0, 0, 210, 20, Component.translatable("options.difficulty"), (l_param0, l_param1) -> CreateWorldScreen.this.uiState.setDifficulty(l_param1)
               ),
            var3
         );
         CreateWorldScreen.this.uiState.addListener(l_param1 -> {
            var6.setValue(CreateWorldScreen.this.uiState.getDifficulty());
            var6.active = !CreateWorldScreen.this.uiState.isHardcore();
            var6.setTooltip(Tooltip.create(CreateWorldScreen.this.uiState.getDifficulty().getInfo()));
         });
         CycleButton<Boolean> var7 = var2.addChild(
            CycleButton.onOffBuilder()
               .withTooltip(l_param0 -> Tooltip.create(CreateWorldScreen.ALLOW_CHEATS_INFO))
               .create(0, 0, 210, 20, ALLOW_CHEATS, (l_param0, l_param1) -> CreateWorldScreen.this.uiState.setAllowCheats(l_param1))
         );
         CreateWorldScreen.this.uiState.addListener(l_param1 -> {
            var7.setValue(CreateWorldScreen.this.uiState.isAllowCheats());
            var7.active = !CreateWorldScreen.this.uiState.isDebug() && !CreateWorldScreen.this.uiState.isHardcore();
         });
      }

      @Override
      public void tick() {
         this.nameEdit.tick();
      }
   }

   class MoreTab extends GridLayoutTab {
      private static final Component TITLE = Component.translatable("createWorld.tab.more.title");
      private static final Component GAME_RULES_LABEL = Component.translatable("selectWorld.gameRules");
      private static final Component DATA_PACKS_LABEL = Component.translatable("selectWorld.dataPacks");

      MoreTab() {
         super(TITLE);
         GridLayout.RowHelper var2 = this.layout.rowSpacing(8).createRowHelper(1);
         var2.addChild(Button.builder(GAME_RULES_LABEL, l_param0 -> this.openGameRulesScreen()).width(210).build());
         var2.addChild(
            Button.builder(
                  CreateWorldScreen.EXPERIMENTS_LABEL,
                  l_param0 -> CreateWorldScreen.this.openExperimentsScreen(CreateWorldScreen.this.uiState.getSettings().dataConfiguration())
               )
               .width(210)
               .build()
         );
         var2.addChild(
            Button.builder(
                  DATA_PACKS_LABEL,
                  l_param0 -> CreateWorldScreen.this.openDataPackSelectionScreen(CreateWorldScreen.this.uiState.getSettings().dataConfiguration())
               )
               .width(210)
               .build()
         );
      }

      private void openGameRulesScreen() {
         CreateWorldScreen.this.minecraft.setScreen(new EditGameRulesScreen(CreateWorldScreen.this.uiState.getGameRules().copy(), l_param0 -> {
            CreateWorldScreen.this.minecraft.setScreen(CreateWorldScreen.this);
            l_param0.ifPresent(CreateWorldScreen.this.uiState::setGameRules);
         }));
      }
   }

   class WorldTab extends GridLayoutTab {
      private static final Component TITLE = Component.translatable("createWorld.tab.world.title");
      private static final Component AMPLIFIED_HELP_TEXT = Component.translatable("generator.minecraft.amplified.info");
      private static final Component GENERATE_STRUCTURES = Component.translatable("selectWorld.mapFeatures");
      private static final Component GENERATE_STRUCTURES_INFO = Component.translatable("selectWorld.mapFeatures.info");
      private static final Component BONUS_CHEST = Component.translatable("selectWorld.bonusItems");
      private static final Component SEED_LABEL = Component.translatable("selectWorld.enterSeed");
      static final Component SEED_EMPTY_HINT = Component.translatable("selectWorld.seedInfo").withStyle(ChatFormatting.DARK_GRAY);
      private static final int WORLD_TAB_WIDTH = 310;
      private final EditBox seedEdit;
      private final Button customizeTypeButton;

      WorldTab() {
         super(TITLE);
         GridLayout.RowHelper var2 = this.layout.columnSpacing(10).rowSpacing(8).createRowHelper(2);
         CycleButton<WorldCreationUiState.WorldTypeEntry> var3 = var2.addChild(
            CycleButton.builder(WorldCreationUiState.WorldTypeEntry::describePreset)
               .withValues(this.createWorldTypeValueSupplier())
               .withCustomNarration(CreateWorldScreen.WorldTab::createTypeButtonNarration)
               .create(
                  0, 0, 150, 20, Component.translatable("selectWorld.mapType"), (l_param0, l_param1) -> CreateWorldScreen.this.uiState.setWorldType(l_param1)
               )
         );
         var3.setValue(CreateWorldScreen.this.uiState.getWorldType());
         CreateWorldScreen.this.uiState.addListener(l_param1 -> {
            WorldCreationUiState.WorldTypeEntry l_var3 = l_param1.getWorldType();
            var3.setValue(l_var3);
            if (l_var3.isAmplified()) {
               var3.setTooltip(Tooltip.create(AMPLIFIED_HELP_TEXT));
            } else {
               var3.setTooltip(null);
            }

            var3.active = CreateWorldScreen.this.uiState.getWorldType().preset() != null;
         });
         this.customizeTypeButton = var2.addChild(
            Button.builder(Component.translatable("selectWorld.customizeType"), l_param0 -> this.openPresetEditor()).build()
         );
         CreateWorldScreen.this.uiState.addListener(l_param0 -> this.customizeTypeButton.active = !l_param0.isDebug() && l_param0.getPresetEditor() != null);
         GridLayout.RowHelper var4 = new GridLayout().rowSpacing(4).createRowHelper(1);
         var4.addChild(new StringWidget(SEED_LABEL, CreateWorldScreen.this.font).alignLeft());
         this.seedEdit = var4.addChild(new EditBox(CreateWorldScreen.this.font, 0, 0, 308, 20, Component.translatable("selectWorld.enterSeed")) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(CreateWorldScreen.WorldTab.SEED_EMPTY_HINT);
            }
         }, var2.newCellSettings().padding(1));
         this.seedEdit.setHint(SEED_EMPTY_HINT);
         this.seedEdit.setValue(CreateWorldScreen.this.uiState.getSeed());
         this.seedEdit.setResponder(l_param0 -> CreateWorldScreen.this.uiState.setSeed(this.seedEdit.getValue()));
         var2.addChild(var4.getGrid(), 2);
         SwitchGrid.Builder var5 = SwitchGrid.builder(310).withPaddingLeft(1);
         var5.addSwitch(GENERATE_STRUCTURES, CreateWorldScreen.this.uiState::isGenerateStructures, CreateWorldScreen.this.uiState::setGenerateStructures)
            .withIsActiveCondition(() -> !CreateWorldScreen.this.uiState.isDebug())
            .withInfo(GENERATE_STRUCTURES_INFO);
         var5.addSwitch(BONUS_CHEST, CreateWorldScreen.this.uiState::isBonusChest, CreateWorldScreen.this.uiState::setBonusChest)
            .withIsActiveCondition(() -> !CreateWorldScreen.this.uiState.isHardcore() && !CreateWorldScreen.this.uiState.isDebug());
         SwitchGrid var6 = var5.build(l_param1 -> var2.addChild(l_param1, 2));
         CreateWorldScreen.this.uiState.addListener(l_param1 -> var6.refreshStates());
      }

      private void openPresetEditor() {
         PresetEditor var1 = CreateWorldScreen.this.uiState.getPresetEditor();
         if (var1 != null) {
            CreateWorldScreen.this.minecraft.setScreen(var1.createEditScreen(CreateWorldScreen.this, CreateWorldScreen.this.uiState.getSettings()));
         }
      }

      private CycleButton.ValueListSupplier<WorldCreationUiState.WorldTypeEntry> createWorldTypeValueSupplier() {
         return new CycleButton.ValueListSupplier<WorldCreationUiState.WorldTypeEntry>() {
            @Override
            public List<WorldCreationUiState.WorldTypeEntry> getSelectedList() {
               return CycleButton.DEFAULT_ALT_LIST_SELECTOR.getAsBoolean()
                  ? CreateWorldScreen.this.uiState.getAltPresetList()
                  : CreateWorldScreen.this.uiState.getNormalPresetList();
            }

            @Override
            public List<WorldCreationUiState.WorldTypeEntry> getDefaultList() {
               return CreateWorldScreen.this.uiState.getNormalPresetList();
            }
         };
      }

      private static MutableComponent createTypeButtonNarration(CycleButton<WorldCreationUiState.WorldTypeEntry> param0) {
         return param0.getValue().isAmplified()
            ? CommonComponents.joinForNarration(param0.createDefaultNarrationMessage(), AMPLIFIED_HELP_TEXT)
            : param0.createDefaultNarrationMessage();
      }

      @Override
      public void tick() {
         this.seedEdit.tick();
      }
   }
}
