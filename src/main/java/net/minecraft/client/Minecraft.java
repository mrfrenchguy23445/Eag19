package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.GlDebug;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.TimerQuery;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.FileUtil;
import net.minecraft.Optionull;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BanNoticeScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.profiling.ClientMetricsSamplersProvider;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.client.searchtree.IdSearchTree;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindResolver;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ProcessorChunkProgressListener;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FileZipper;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.ModCheck;
import net.minecraft.util.Mth;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.lax1dude.eaglercraft.v1_8.PlatformLocale;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformApplication;
import net.lax1dude.eaglercraft.v1_8.FileUtils;
import net.lax1dude.eaglercraft.v1_8.BootDiag;
import net.lax1dude.eaglercraft.v1_8.profile.EaglerProfile;
import org.joml.Matrix4f;
import org.slf4j.Logger;

public class Minecraft extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler {
   static Minecraft instance;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final boolean ON_OSX = Util.getPlatform() == Util.OS.OSX;
   private static final int MAX_TICKS_PER_UPDATE = 10;
   public static final ResourceLocation DEFAULT_FONT = new ResourceLocation("default");
   public static final ResourceLocation UNIFORM_FONT = new ResourceLocation("uniform");
   public static final ResourceLocation ALT_FONT = new ResourceLocation("alt");
   private static final ResourceLocation REGIONAL_COMPLIANCIES = new ResourceLocation("regional_compliancies.json");
   private static final CompletableFuture<Unit> RESOURCE_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
   private static final Component SOCIAL_INTERACTIONS_NOT_AVAILABLE = Component.translatable("multiplayer.socialInteractions.not_available");
   public static final String UPDATE_DRIVERS_ADVICE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
   private final Path resourcePackDirectory;
   private final PropertyMap profileProperties;
   private final TextureManager textureManager;
   private final DataFixer fixerUpper;
   private final VirtualScreen virtualScreen;
   private final Window window;
   private final Timer timer = new Timer(20.0F, 0L);
   private final RenderBuffers renderBuffers;
   public final LevelRenderer levelRenderer;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemRenderer itemRenderer;
   public final ParticleEngine particleEngine;
   private final SearchRegistry searchRegistry = new SearchRegistry();
   /**
    * The session identity. Not {@code final} so a profile edit can re-point it:
    * everything that reads {@link #getUser()} -- the Eagler handshake username,
    * the offline UUID, the skin lookup -- must agree with
    * {@link net.lax1dude.eaglercraft.v1_8.profile.EaglerProfile} without needing
    * a reload. See {@link #refreshUserFromProfile()}.
    */
   private User user;
   public final Font font;
   public final Font fontFilterFishy;
   public final GameRenderer gameRenderer;
   public final DebugRenderer debugRenderer;
   private final AtomicReference<StoringChunkProgressListener> progressListener = new AtomicReference<>();
   public final Gui gui;
   public final Options options;
   private final HotbarManager hotbarManager;
   public final MouseHandler mouseHandler;
   public final KeyboardHandler keyboardHandler;
   private InputType lastInputType = InputType.NONE;
   public final File gameDirectory;
   private final String launchedVersion;
   private final String versionType;
   private final Proxy proxy;
   private final LevelStorageSource levelSource;
   public final FrameTimer frameTimer = new FrameTimer();
   private final boolean is64bit;
   private final boolean demo;
   private final boolean allowsMultiplayer;
   private final boolean allowsChat;
   private final ReloadableResourceManager resourceManager;
   private final VanillaPackResources vanillaPackResources;
   private final DownloadedPackSource downloadedPackSource;
   private final PackRepository resourcePackRepository;
   private final LanguageManager languageManager;
   private final BlockColors blockColors;
   private final ItemColors itemColors;
   private final RenderTarget mainRenderTarget;
   private final SoundManager soundManager;
   private final MusicManager musicManager;
   private final FontManager fontManager;
   private final SplashManager splashManager;
   private final GpuWarnlistManager gpuWarnlistManager;
   private final PeriodicNotificationManager regionalCompliancies = new PeriodicNotificationManager(REGIONAL_COMPLIANCIES, Minecraft::countryEqualsISO3);
   private final MinecraftSessionService minecraftSessionService;
   private final SignatureValidator serviceSignatureValidator;
   private final UserApiService userApiService;
   private final SkinManager skinManager;
   private final ModelManager modelManager;
   private final BlockRenderDispatcher blockRenderer;
   private final PaintingTextureManager paintingTextures;
   private final MobEffectTextureManager mobEffectTextures;
   private final ToastComponent toast;
   private final Tutorial tutorial;
   private final PlayerSocialManager playerSocialManager;
   private final EntityModelSet entityModels;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final ClientTelemetryManager telemetryManager;
   private final ProfileKeyPairManager profileKeyPairManager;
   private final RealmsDataFetcher realmsDataFetcher;
   @Nullable
   public MultiPlayerGameMode gameMode;
   @Nullable
   public ClientLevel level;
   @Nullable
   public LocalPlayer player;
   @Nullable
   private IntegratedServer singleplayerServer;
   @Nullable
   private Connection pendingConnection;
   private boolean isLocalServer;
   @Nullable
   public Entity cameraEntity;
   @Nullable
   public Entity crosshairPickEntity;
   @Nullable
   public HitResult hitResult;
   private int rightClickDelay;
   protected int missTime;
   private volatile boolean pause;
   private float pausePartialTick;
   private long lastNanoTime = Util.getNanos();
   private long lastTime;
   private int frames;
   public boolean noRender;
   @Nullable
   public Screen screen;
   @Nullable
   private Overlay overlay;
   private boolean connectedToRealms;
   private Thread gameThread;
   private volatile boolean running;
   @Nullable
   private Supplier<CrashReport> delayedCrash;
   private static int fps;
   public String fpsString = "";
   private long frameTimeNs;
   public boolean wireframe;
   public boolean chunkPath;
   public boolean chunkVisibility;
   public boolean smartCull = true;
   private boolean windowActive;
   private final Queue<Runnable> progressTasks = Queues.newConcurrentLinkedQueue();
   @Nullable
   private CompletableFuture<Void> pendingReload;
   @Nullable
   private TutorialToast socialInteractionsToast;
   private ProfilerFiller profiler = InactiveProfiler.INSTANCE;
   private int fpsPieRenderTicks;
   private final ContinuousProfiler fpsPieProfiler = new ContinuousProfiler(Util.timeSource, () -> this.fpsPieRenderTicks);
   @Nullable
   private ProfileResults fpsPieResults;
   private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
   private final ResourceLoadStateTracker reloadStateTracker = new ResourceLoadStateTracker();
   private long savedCpuDuration;
   private double gpuUtilization;
   @Nullable
   private TimerQuery.FrameProfile currentFrameProfile;
   private final Realms32BitWarningStatus realms32BitWarningStatus;
   private final GameNarrator narrator;
   private final ChatListener chatListener;
   private ReportingContext reportingContext;
   private String debugPath = "root";

   public Minecraft(GameConfig param0) {
      super("Client");
      instance = this;
      BootDiag.probe("Minecraft ctor start");
      this.gameDirectory = param0.location.gameDirectory;
      File var2 = param0.location.assetDirectory;
      // java.io.File.toPath() does not exist in the WASM-GC TeaVM classlib, so the
         // equivalent Path is built from the absolute path string instead.
         this.resourcePackDirectory = Paths.get(param0.location.resourcePackDirectory.getAbsolutePath());
      this.launchedVersion = param0.game.launchVersion;
      this.versionType = param0.game.versionType;
      this.profileProperties = new PropertyMap();
      ClientPackSource var3 = new ClientPackSource(param0.location.getExternalAssetSource());
      this.downloadedPackSource = new DownloadedPackSource(new File(this.gameDirectory, "server-resource-packs"));
      RepositorySource var4 = new FolderRepositorySource(this.resourcePackDirectory, PackType.CLIENT_RESOURCES, PackSource.DEFAULT);
      this.resourcePackRepository = new PackRepository(var3, this.downloadedPackSource, var4);
      this.vanillaPackResources = var3.getVanillaPack();
      this.proxy = Proxy.NO_PROXY;
      this.user = param0.user.user;
      this.minecraftSessionService = null;
      this.userApiService = UserApiService.OFFLINE;
      this.serviceSignatureValidator = SignatureValidator.NO_VALIDATION;
      LOGGER.info("Setting user: {}", this.user.getName());
      LOGGER.debug("(Session ID is {})", this.user.getSessionId());
      this.demo = param0.game.demo;
      this.allowsMultiplayer = !param0.game.disableMultiplayer;
      this.allowsChat = !param0.game.disableChat;
      this.is64bit = checkIs64Bit();
      this.singleplayerServer = null;
      String var5;
      int var6;
      if (this.allowsMultiplayer() && param0.server.hostname != null) {
         var5 = param0.server.hostname;
         var6 = param0.server.port;
      } else {
         var5 = null;
         var6 = 0;
      }

      KeybindResolver.setKeyResolver(KeyMapping::createNameSupplier);
      this.fixerUpper = DataFixers.getDataFixer();
      this.toast = new ToastComponent(this);
      this.gameThread = Thread.currentThread();
      this.options = new Options(this, this.gameDirectory);
      RenderSystem.setShaderGlintAlpha(this.options.glintStrength().get());
      this.running = true;
      this.tutorial = new Tutorial(this, this.options);
      this.hotbarManager = new HotbarManager(this.gameDirectory, this.fixerUpper);
      LOGGER.info("Backend library: {}", RenderSystem.getBackendDescription());
      DisplayData var7;
      if (this.options.overrideHeight > 0 && this.options.overrideWidth > 0) {
         var7 = new DisplayData(
            this.options.overrideWidth,
            this.options.overrideHeight,
            param0.display.fullscreenWidth,
            param0.display.fullscreenHeight,
            param0.display.isFullscreen
         );
      } else {
         var7 = param0.display;
      }

      Util.timeSource = RenderSystem.initBackendSystem();
      this.virtualScreen = new VirtualScreen(this);
      this.window = this.virtualScreen.newWindow(var7, this.options.fullscreenVideoModeString, this.createTitle());
      this.setWindowActive(true);

      try {
         // Eaglercraft: macOS .icns branch (MacosUtil) removed; PNG icons work everywhere.
         this.window.setIcon(this.getIconFile("icons", "icon_16x16.png"), this.getIconFile("icons", "icon_32x32.png"));
      } catch (IOException var8) {
         LOGGER.error("Couldn't set icon", var8);
      }

      this.window.setFramerateLimit(this.options.framerateLimit().get());
      this.mouseHandler = new MouseHandler(this);
      this.mouseHandler.setup(this.window.getWindow());
      this.keyboardHandler = new KeyboardHandler(this);
      this.keyboardHandler.setup(this.window.getWindow());
      RenderSystem.initRenderer(this.options.glDebugVerbosity, false);
      this.mainRenderTarget = new MainTarget(this.window.getWidth(), this.window.getHeight());
      this.mainRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.mainRenderTarget.clear(ON_OSX);
      this.resourceManager = new ReloadableResourceManager(PackType.CLIENT_RESOURCES);
      this.resourcePackRepository.reload();
      this.options.loadSelectedResourcePacks(this.resourcePackRepository);
      this.languageManager = new LanguageManager(this.options.languageCode);
      this.resourceManager.registerReloadListener(this.languageManager);
      this.textureManager = new TextureManager(this.resourceManager);
      this.resourceManager.registerReloadListener(this.textureManager);
      this.skinManager = new SkinManager(this.textureManager, new File(var2, "skins"), this.minecraftSessionService);
      this.levelSource = new LevelStorageSource(
            Paths.get(this.gameDirectory.getAbsolutePath()).resolve("saves"),
            Paths.get(this.gameDirectory.getAbsolutePath()).resolve("backups"),
            this.fixerUpper
         );
      this.soundManager = new SoundManager(this.options);
      this.resourceManager.registerReloadListener(this.soundManager);
      this.splashManager = new SplashManager(this.user);
      this.resourceManager.registerReloadListener(this.splashManager);
      this.musicManager = new MusicManager(this);
      this.fontManager = new FontManager(this.textureManager);
      this.font = this.fontManager.createFont();
      this.fontFilterFishy = this.fontManager.createFontFilterFishy();
      this.resourceManager.registerReloadListener(this.fontManager.getReloadListener());
      this.selectMainFont(this.isEnforceUnicode());
      this.resourceManager.registerReloadListener(new GrassColorReloadListener());
      this.resourceManager.registerReloadListener(new FoliageColorReloadListener());
      this.window.setErrorSection("Startup");
      RenderSystem.setupDefaultState(0, 0, this.window.getWidth(), this.window.getHeight());
      this.window.setErrorSection("Post startup");
      this.blockColors = BlockColors.createDefault();
      this.itemColors = ItemColors.createDefault(this.blockColors);
      this.modelManager = new ModelManager(this.textureManager, this.blockColors, this.options.mipmapLevels().get());
      this.resourceManager.registerReloadListener(this.modelManager);
      this.entityModels = new EntityModelSet();
      this.resourceManager.registerReloadListener(this.entityModels);
      this.blockEntityRenderDispatcher = new BlockEntityRenderDispatcher(
         this.font, this.entityModels, this::getBlockRenderer, this::getItemRenderer, this::getEntityRenderDispatcher
      );
      this.resourceManager.registerReloadListener(this.blockEntityRenderDispatcher);
      BlockEntityWithoutLevelRenderer var8 = new BlockEntityWithoutLevelRenderer(this.blockEntityRenderDispatcher, this.entityModels);
      this.resourceManager.registerReloadListener(var8);
      this.itemRenderer = new ItemRenderer(this, this.textureManager, this.modelManager, this.itemColors, var8);
      this.resourceManager.registerReloadListener(this.itemRenderer);
      this.renderBuffers = new RenderBuffers();
      this.playerSocialManager = new PlayerSocialManager(this, this.userApiService);
      this.blockRenderer = new BlockRenderDispatcher(this.modelManager.getBlockModelShaper(), var8, this.blockColors);
      this.resourceManager.registerReloadListener(this.blockRenderer);
      this.entityRenderDispatcher = new EntityRenderDispatcher(
         this, this.textureManager, this.itemRenderer, this.blockRenderer, this.font, this.options, this.entityModels
      );
      this.resourceManager.registerReloadListener(this.entityRenderDispatcher);
      this.gameRenderer = new GameRenderer(this, this.entityRenderDispatcher.getItemInHandRenderer(), this.resourceManager, this.renderBuffers);
      this.resourceManager.registerReloadListener(this.gameRenderer.createReloadListener());
      this.levelRenderer = new LevelRenderer(this, this.entityRenderDispatcher, this.blockEntityRenderDispatcher, this.renderBuffers);
      this.resourceManager.registerReloadListener(this.levelRenderer);
      this.createSearchTrees();
      this.resourceManager.registerReloadListener(this.searchRegistry);
      this.particleEngine = new ParticleEngine(this.level, this.textureManager);
      this.resourceManager.registerReloadListener(this.particleEngine);
      this.paintingTextures = new PaintingTextureManager(this.textureManager);
      this.resourceManager.registerReloadListener(this.paintingTextures);
      this.mobEffectTextures = new MobEffectTextureManager(this.textureManager);
      this.resourceManager.registerReloadListener(this.mobEffectTextures);
      this.gpuWarnlistManager = new GpuWarnlistManager();
      this.resourceManager.registerReloadListener(this.gpuWarnlistManager);
      this.resourceManager.registerReloadListener(this.regionalCompliancies);
      this.gui = new Gui(this, this.itemRenderer);
      this.debugRenderer = new DebugRenderer(this);
      this.realmsDataFetcher = new RealmsDataFetcher(RealmsClient.create(this));
      RenderSystem.setErrorCallback(this::onFullscreenError);
      if (this.mainRenderTarget.width != this.window.getWidth() || this.mainRenderTarget.height != this.window.getHeight()) {
         StringBuilder var9 = new StringBuilder(
            "Recovering from unsupported resolution ("
               + this.window.getWidth()
               + "x"
               + this.window.getHeight()
               + ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions)."
         );
         if (GlDebug.isDebugEnabled()) {
            var9.append("\n\nReported GL debug messages:\n").append(String.join("\n", GlDebug.getLastOpenGlDebugMessages()));
         }

         this.window.setWindowed(this.mainRenderTarget.width, this.mainRenderTarget.height);
         // TinyFileDialogs.tinyfd_messageBox was a native native-window dialog, replaced here by
         // PlatformApplication.showPopup, which is the Eaglercraft equivalent (a DOM popup on the web
         // targets). The message is unchanged, and the crash screen below still fires regardless --
         // upstream showed the dialog in addition to the screen, not instead of it.
         PlatformApplication.showPopup(var9.toString());
      } else if (this.options.fullscreen().get() && !this.window.isFullscreen()) {
         this.window.toggleFullScreen();
         this.options.fullscreen().set(this.window.isFullscreen());
      }

      this.window.updateVsync(this.options.enableVsync().get());
      this.window.updateRawMouseInput(this.options.rawMouseInput().get());
      this.window.setDefaultErrorCallback();
      this.resizeDisplay();
      this.gameRenderer.preloadUiShader(this.vanillaPackResources.asProvider());
      this.telemetryManager = new ClientTelemetryManager(this, this.userApiService, this.user);
      this.profileKeyPairManager = ProfileKeyPairManager.create(this.userApiService, this.user, Paths.get(this.gameDirectory.getAbsolutePath()));
      this.realms32BitWarningStatus = new Realms32BitWarningStatus(this);
      this.narrator = new GameNarrator(this);
      this.chatListener = new ChatListener(this);
      this.chatListener.setMessageDelay(this.options.chatDelay().get());
      this.reportingContext = ReportingContext.create(ReportEnvironment.local(), this.userApiService);
      LoadingOverlay.registerTextures(this);
      BootDiag.probe("Minecraft ctor: before createReload");
      List<PackResources> var9 = this.resourcePackRepository.openAllSelected();
      this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.INITIAL, var9);
      ReloadInstance var10 = this.resourceManager.createReload(Util.backgroundExecutor(), this, RESOURCE_RELOAD_INITIAL_TASK, var9);
      BootDiag.probe("Minecraft ctor: createReload returned");
      this.setOverlay(new LoadingOverlay(this, var10, l_param0 -> Util.ifElse(l_param0, this::rollbackResourcePacks, () -> {
         if (SharedConstants.IS_RUNNING_IN_IDE) {
            this.selfTest();
         }

         this.reloadStateTracker.finishReload();
      }), false));
      BootDiag.probe("Minecraft ctor: LoadingOverlay set, screen assignment next");
      if (var5 != null) {
         ServerAddress var11 = new ServerAddress(var5, var6);
         var10.done()
            .thenRunAsync(
               () -> ConnectScreen.startConnecting(
                  new TitleScreen(), this, var11, new ServerData(I18n.get("selectServer.defaultName"), var11.toString(), false)
               ),
               this
            );
      } else if (this.shouldShowBanNotice()) {
         this.setScreen(BanNoticeScreen.create(l_param0 -> {
            if (l_param0) {
               Util.getPlatform().openUri("https://aka.ms/mcjavamoderation");
            }

            this.setScreen(new TitleScreen(true));
         }, this.multiplayerBan()));
      } else if (this.options.onboardAccessibility) {
         this.setScreen(new AccessibilityOnboardingScreen(this.options));
      } else {
         // The launch sequence owns the first screen: profile editor, then --
         // once only -- the attribution disclaimer, then the title screen.
         this.setScreen(net.lax1dude.eaglercraft.v1_8.profile.BootFlow.firstScreen());
      }
   }

   private IoSupplier<InputStream> getIconFile(String... param0) throws IOException {
      IoSupplier<InputStream> var2 = this.vanillaPackResources.getRootResource(param0);
      if (var2 == null) {
         throw new FileNotFoundException(String.join("/", param0));
      } else {
         return var2;
      }
   }

   private static boolean countryEqualsISO3(Object param0) {
      try {
         // Was Locale.getDefault().getISO3Country(). The method is absent from the WASM-GC
         // classlib -- java.util.Locale is a class the classlib owns, so it cannot be shimmed
         // -- and it is what decides whether the player is in a region Mojang has a
         // regional_compliancies.json notification for. That JSON is keyed by alpha-3 code, so
         // without this every notification is filtered out silently.
         //
         // PlatformLocale carries the same table the JDK consults, generated from this JDK's
         // own getISO3Country() over getISOCountries() so that all 249 ISO 3166-1 codes match,
         // and it throws MissingResourceException for the same two cases the JDK does, because
         // the catch below is the "this locale has no country" path.
         return PlatformLocale.iso3Country(Locale.getDefault()).equals(param0);
      } catch (MissingResourceException var1) {
         return false;
      }
   }

   public void updateTitle() {
      this.window.setTitle(this.createTitle());
   }

   private String createTitle() {
      StringBuilder var1 = new StringBuilder("Minecraft");
      if (checkModStatus().shouldReportAsModified()) {
         var1.append("*");
      }

      var1.append(" ");
      var1.append(SharedConstants.getCurrentVersion().getName());
      ClientPacketListener var2 = this.getConnection();
      if (var2 != null && var2.getConnection().isConnected()) {
         var1.append(" - ");
         if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
            var1.append(I18n.get("title.singleplayer"));
         } else if (this.isConnectedToRealms()) {
            var1.append(I18n.get("title.multiplayer.realms"));
         } else if (this.singleplayerServer == null && (this.getCurrentServer() == null || !this.getCurrentServer().isLan())) {
            var1.append(I18n.get("title.multiplayer.other"));
         } else {
            var1.append(I18n.get("title.multiplayer.lan"));
         }
      }

      return var1.toString();
   }

   public static ModCheck checkModStatus() {
      return ModCheck.identify("vanilla", ClientBrandRetriever::getClientModName, "Client", Minecraft.class);
   }

   private void rollbackResourcePacks(Throwable param0) {
      if (this.resourcePackRepository.getSelectedIds().size() > 1) {
         this.clearResourcePacksOnError(param0, null);
      } else {
         Util.throwAsRuntime(param0);
      }
   }

   public void clearResourcePacksOnError(Throwable param0, @Nullable Component param1) {
      LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", param0);
      this.reloadStateTracker.startRecovery(param0);
      this.resourcePackRepository.setSelected(Collections.emptyList());
      this.options.resourcePacks.clear();
      this.options.incompatibleResourcePacks.clear();
      this.options.save();
      this.reloadResourcePacks(true).thenRun(() -> this.addResourcePackLoadFailToast(param1));
   }

   private void abortResourcePackRecovery() {
      this.setOverlay(null);
      if (this.level != null) {
         this.level.disconnect();
         this.clearLevel();
      }

      this.setScreen(new TitleScreen());
      this.addResourcePackLoadFailToast(null);
   }

   private void addResourcePackLoadFailToast(@Nullable Component param0) {
      ToastComponent var2 = this.getToasts();
      SystemToast.addOrUpdate(var2, SystemToast.SystemToastIds.PACK_LOAD_FAILURE, Component.translatable("resourcePack.load_fail"), param0);
   }

   public void run() {
      this.gameThread = Thread.currentThread();
      BootDiag.probe("Minecraft.run(): loop entered");
      // Runtime.availableProcessors() is not available on WASM-GC TeaVM. The browser
      // build is single-threaded, so the "more than 4 cores" case never applies.
      if (1 > 4) {
         this.gameThread.setPriority(10);
      }

      try {
         boolean var1 = false;

         while (this.running) {
            if (this.delayedCrash != null) {
               crash(this.delayedCrash.get());
               return;
            }

            try {
               SingleTickProfiler var2 = SingleTickProfiler.createTickProfiler("Renderer");
               boolean var3 = this.shouldRenderFpsPie();
               this.profiler = this.constructProfiler(var3, var2);
               this.profiler.startTick();
               this.metricsRecorder.startTick();
               this.runTick(!var1);
               this.metricsRecorder.endTick();
               this.profiler.endTick();
               this.finishProfilers(var3, var2);
            } catch (OutOfMemoryError var2) {
               if (var1) {
                  throw var2;
               }

               this.emergencySave();
               this.setScreen(new OutOfMemoryScreen());
               System.gc();
               LOGGER.error(LogUtils.FATAL_MARKER, "Out of memory", var2);
               var1 = true;
            }
         }
      } catch (ReportedException var1) {
         this.fillReport(var1.getReport());
         this.emergencySave();
         LOGGER.error(LogUtils.FATAL_MARKER, "Reported exception thrown!", var1);
         crash(var1.getReport());
      } catch (Throwable var1) {
         CrashReport var2 = this.fillReport(new CrashReport("Unexpected error", var1));
         LOGGER.error(LogUtils.FATAL_MARKER, "Unreported exception thrown!", var1);
         this.emergencySave();
         crash(var2);
      }
   }

   void selectMainFont(boolean param0) {
      this.fontManager.setRenames(param0 ? ImmutableMap.of(DEFAULT_FONT, UNIFORM_FONT) : ImmutableMap.of());
   }

   private void createSearchTrees() {
      this.searchRegistry
         .register(
            SearchRegistry.CREATIVE_NAMES,
            l_param0 -> new FullTextSearchTree<>(
               l2_param0 -> l2_param0.getTooltipLines(null, TooltipFlag.Default.NORMAL.asCreative())
                  .stream()
                  .map(l3_param0 -> ChatFormatting.stripFormatting(l3_param0.getString()).trim())
                  .filter(l3_param0 -> !l3_param0.isEmpty()),
               l2_param0 -> Stream.of(BuiltInRegistries.ITEM.getKey(l2_param0.getItem())),
               l_param0
            )
         );
      this.searchRegistry
         .register(SearchRegistry.CREATIVE_TAGS, l_param0 -> new IdSearchTree<>(l2_param0 -> l2_param0.getTags().map(TagKey::location), l_param0));
      this.searchRegistry
         .register(
            SearchRegistry.RECIPE_COLLECTIONS,
            l_param0 -> new FullTextSearchTree<>(
               l2_param0 -> l2_param0.getRecipes()
                  .stream()
                  .flatMap(l3_param1 -> l3_param1.getResultItem(l2_param0.registryAccess()).getTooltipLines(null, TooltipFlag.Default.NORMAL).stream())
                  .map(l3_param0 -> ChatFormatting.stripFormatting(l3_param0.getString()).trim())
                  .filter(l3_param0 -> !l3_param0.isEmpty()),
               l2_param0 -> l2_param0.getRecipes()
                  .stream()
                  .map(l3_param1 -> BuiltInRegistries.ITEM.getKey(l3_param1.getResultItem(l2_param0.registryAccess()).getItem())),
               l_param0
            )
         );
      CreativeModeTabs.searchTab().setSearchTreeBuilder(l_param0 -> {
         this.populateSearchTree(SearchRegistry.CREATIVE_NAMES, l_param0);
         this.populateSearchTree(SearchRegistry.CREATIVE_TAGS, l_param0);
      });
   }

   // The parameters are now (Integer code, String message) rather than LWJGL's
   // (int code, long utf8Address) because there is no GLFW error channel to hand a raw pointer to;
   // see GLX._setGlfwErrorCallback.
   private void onFullscreenError(Integer param0, String param1) {
      this.options.enableVsync().set(false);
      this.options.save();
   }

   private static boolean checkIs64Bit() {
      String[] var0 = new String[]{"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"};

      for (String var4 : var0) {
         String var5 = System.getProperty(var4);
         if (var5 != null && var5.contains("64")) {
            return true;
         }
      }

      return false;
   }

   public RenderTarget getMainRenderTarget() {
      return this.mainRenderTarget;
   }

   public String getLaunchedVersion() {
      return this.launchedVersion;
   }

   public String getVersionType() {
      return this.versionType;
   }

   public void delayCrash(CrashReport param0) {
      this.delayedCrash = () -> this.fillReport(param0);
   }

   public void delayCrashRaw(CrashReport param0) {
      this.delayedCrash = () -> param0;
   }

   public static void crash(CrashReport param0) {
      File var1 = new File(getInstance().gameDirectory, "crash-reports");
      File var2 = new File(var1, "crash-" + Util.getFilenameFormattedDateTime() + "-client.txt");
      Bootstrap.realStdoutPrintln(param0.getFriendlyReport());
      if (param0.getSaveFile() != null) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + param0.getSaveFile());
         net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime.exit();
      } else if (param0.saveToFile(var2)) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + var2.getAbsolutePath());
         net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime.exit();
      } else {
         Bootstrap.realStdoutPrintln("#@?@# Game crashed! Crash report could not be saved. #@?@#");
         net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime.exit();
      }
   }

   public boolean isEnforceUnicode() {
      return this.options.forceUnicodeFont().get();
   }

   public CompletableFuture<Void> reloadResourcePacks() {
      return this.reloadResourcePacks(false);
   }

   private CompletableFuture<Void> reloadResourcePacks(boolean param0) {
      if (this.pendingReload != null) {
         return this.pendingReload;
      }

      CompletableFuture<Void> var2 = new CompletableFuture<>();
      if (!param0 && this.overlay instanceof LoadingOverlay) {
         this.pendingReload = var2;
         return var2;
      }

      this.resourcePackRepository.reload();
      List<PackResources> var3 = this.resourcePackRepository.openAllSelected();
      if (!param0) {
         this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.MANUAL, var3);
      }

      this.setOverlay(
         new LoadingOverlay(
            this,
            this.resourceManager.createReload(Util.backgroundExecutor(), this, RESOURCE_RELOAD_INITIAL_TASK, var3),
            l_param2 -> Util.ifElse(l_param2, l2_param1 -> {
               if (param0) {
                  this.abortResourcePackRecovery();
               } else {
                  this.rollbackResourcePacks(l2_param1);
               }
            }, () -> {
               this.levelRenderer.allChanged();
               this.reloadStateTracker.finishReload();
               var2.complete(null);
            }),
            true
         )
      );
      return var2;
   }

   private void selfTest() {
      boolean var1 = false;
      BlockModelShaper var2 = this.getBlockRenderer().getBlockModelShaper();
      BakedModel var3 = var2.getModelManager().getMissingModel();

      for (Block var5 : BuiltInRegistries.BLOCK) {
         for (BlockState var7 : var5.getStateDefinition().getPossibleStates()) {
            if (var7.getRenderShape() == RenderShape.MODEL) {
               BakedModel var8 = var2.getBlockModel(var7);
               if (var8 == var3) {
                  LOGGER.debug("Missing model for: {}", var7);
                  var1 = true;
               }
            }
         }
      }

      TextureAtlasSprite var4 = var3.getParticleIcon();

      for (Block var6 : BuiltInRegistries.BLOCK) {
         for (BlockState var8 : var6.getStateDefinition().getPossibleStates()) {
            TextureAtlasSprite var9 = var2.getParticleIcon(var8);
            if (!var8.isAir() && var9 == var4) {
               LOGGER.debug("Missing particle icon for: {}", var8);
               var1 = true;
            }
         }
      }

      for (Item var6 : BuiltInRegistries.ITEM) {
         ItemStack var7 = var6.getDefaultInstance();
         String var8 = var7.getDescriptionId();
         String var9 = Component.translatable(var8).getString();
         if (var9.toLowerCase(Locale.ROOT).equals(var6.getDescriptionId())) {
            LOGGER.debug("Missing translation for: {} {} {}", var7, var8, var6);
         }
      }

      var1 |= MenuScreens.selfTest();
      var1 |= EntityRenderers.validateRegistrations();
      if (var1) {
         throw new IllegalStateException("Your game data is foobar, fix the errors above!");
      }
   }

   public LevelStorageSource getLevelSource() {
      return this.levelSource;
   }

   private void openChatScreen(String param0) {
      Minecraft.ChatStatus var2 = this.getChatStatus();
      if (!var2.isChatAllowed(this.isLocalServer())) {
         if (this.gui.isShowingChatDisabledByPlayer()) {
            this.gui.setChatDisabledByPlayerShown(false);
            this.setScreen(new ConfirmLinkScreen(l_param0 -> {
               if (l_param0) {
                  Util.getPlatform().openUri("https://aka.ms/JavaAccountSettings");
               }

               this.setScreen(null);
            }, Minecraft.ChatStatus.INFO_DISABLED_BY_PROFILE, "https://aka.ms/JavaAccountSettings", true));
         } else {
            Component var3 = var2.getMessage();
            this.gui.setOverlayMessage(var3, false);
            this.narrator.sayNow(var3);
            this.gui.setChatDisabledByPlayerShown(var2 == Minecraft.ChatStatus.DISABLED_BY_PROFILE);
         }
      } else {
         this.setScreen(new ChatScreen(param0));
      }
   }

   public void setScreen(@Nullable Screen param0) {
      if (SharedConstants.IS_RUNNING_IN_IDE && Thread.currentThread() != this.gameThread) {
         LOGGER.error("setScreen called from non-game thread");
      }

      if (this.screen != null) {
         this.screen.removed();
      }

      if (param0 == null && this.level == null) {
         param0 = new TitleScreen();
      } else if (param0 == null && this.player.isDeadOrDying()) {
         if (this.player.shouldShowDeathScreen()) {
            param0 = new DeathScreen(null, this.level.getLevelData().isHardcore());
         } else {
            this.player.respawn();
         }
      }

      this.screen = param0;
      if (this.screen != null) {
         this.screen.added();
      }

      // Which screen is up is the single most useful thing to know when a
      // client is misbehaving, and the game's own logger is a no-op in this
      // build, so it goes to stdout where the browser console can see it.
      try {
         System.out.println("[screen] " + (param0 == null ? "null" : param0.getClass().getName()));
      } catch (Throwable ignored) {
      }

      BufferUploader.reset();
      if (param0 != null) {
         this.mouseHandler.releaseMouse();
         KeyMapping.releaseAll();
         param0.init(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
         this.noRender = false;
      } else {
         this.soundManager.resume();
         this.mouseHandler.grabMouse();
      }

      this.updateTitle();
   }

   public void setOverlay(@Nullable Overlay param0) {
      this.overlay = param0;
   }

   public void destroy() {
      try {
         LOGGER.info("Stopping!");

         try {
            this.narrator.destroy();
         } catch (Throwable var7) {
         }

         try {
            if (this.level != null) {
               this.level.disconnect();
            }

            this.clearLevel();
         } catch (Throwable var6) {
         }

         if (this.screen != null) {
            this.screen.removed();
         }

         this.close();
      } finally {
         Util.timeSource = System::nanoTime;
         if (this.delayedCrash == null) {
            net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime.exit();
         }
      }
   }

   @Override
   public void close() {
      if (this.currentFrameProfile != null) {
         this.currentFrameProfile.cancel();
      }

      try {
         this.telemetryManager.close();
         this.regionalCompliancies.close();
         this.modelManager.close();
         this.fontManager.close();
         this.gameRenderer.close();
         this.levelRenderer.close();
         this.soundManager.destroy();
         this.particleEngine.close();
         this.mobEffectTextures.close();
         this.paintingTextures.close();
         this.textureManager.close();
         this.resourceManager.close();
         Util.shutdownExecutors();
      } catch (Throwable var1) {
         LOGGER.error("Shutdown failure!", var1);
         throw var1;
      } finally {
         this.virtualScreen.close();
         this.window.close();
      }
   }

   private void runTick(boolean param0) {
      if (this.singleplayerServer != null) this.singleplayerServer.tickCooperative();
      this.window.setErrorSection("Pre render");
      long var2 = Util.getNanos();
      if (this.window.shouldClose()) {
         this.stop();
      }

      if (this.pendingReload != null && !(this.overlay instanceof LoadingOverlay)) {
         CompletableFuture<Void> var4 = this.pendingReload;
         this.pendingReload = null;
         this.reloadResourcePacks().thenRun(() -> var4.complete(null));
      }

      Runnable var4;
      while ((var4 = this.progressTasks.poll()) != null) {
         var4.run();
      }

      if (param0) {
         int var5 = this.timer.advanceTime(Util.getMillis());
         this.profiler.push("scheduledExecutables");
         this.runAllTasks();
         this.profiler.pop();
         this.profiler.push("tick");

         for (int var6 = 0; var6 < Math.min(10, var5); var6++) {
            this.profiler.incrementCounter("clientTick");
            this.tick();
         }

         this.profiler.pop();
      }

      this.mouseHandler.turnPlayer();
      this.window.setErrorSection("Render");
      this.profiler.push("sound");
      this.soundManager.updateSource(this.gameRenderer.getMainCamera());
      this.profiler.pop();
      this.profiler.push("render");
      long var5 = Util.getNanos();
      boolean var7;
      if (!this.options.renderDebug && !this.metricsRecorder.isRecording()) {
         var7 = false;
         this.gpuUtilization = 0.0;
      } else {
         var7 = this.currentFrameProfile == null || this.currentFrameProfile.isDone();
         if (var7) {
            TimerQuery.getInstance().ifPresent(TimerQuery::beginProfile);
         }
      }

      RenderSystem.clear(16640, ON_OSX);
      this.mainRenderTarget.bindWrite(true);
      FogRenderer.setupNoFog();
      this.profiler.push("display");
      RenderSystem.enableCull();
      this.profiler.pop();
      if (!this.noRender) {
         this.profiler.popPush("gameRenderer");
         this.gameRenderer.render(this.pause ? this.pausePartialTick : this.timer.partialTick, var2, param0);
         this.profiler.pop();
      }

      if (this.fpsPieResults != null) {
         this.profiler.push("fpsPie");
         this.renderFpsMeter(new PoseStack(), this.fpsPieResults);
         this.profiler.pop();
      }

      this.profiler.push("blit");
      this.mainRenderTarget.unbindWrite();
      this.mainRenderTarget.blitToScreen(this.window.getWidth(), this.window.getHeight());
      this.frameTimeNs = Util.getNanos() - var5;
      if (var7) {
         TimerQuery.getInstance().ifPresent(l_param0 -> this.currentFrameProfile = l_param0.endProfile());
      }

      this.profiler.popPush("updateDisplay");
      this.window.updateDisplay();
      int var8 = this.getFramerateLimit();
      if (var8 < 260) {
         RenderSystem.limitDisplayFPS(var8);
      }

      this.profiler.popPush("yield");
      // Thread.yield() is a no-op on the TeaVM WASM-GC target and is in fact worse than that: the
      // classlib routes it through Thread.switchContext(), which requires the org.teavm.runtime.Fiber
      // support that this target does not provide, so merely calling it fails the build. The
      // profiler push/pop around it is kept so the capture output still shows this point in the
      // frame; the frame limiter just above is what actually yields the time slice.
      this.profiler.pop();
      this.window.setErrorSection("Post render");
      this.frames++;
      boolean var9 = this.hasSingleplayerServer()
         && (this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPauseScreen())
         && !this.singleplayerServer.isPublished();
      if (this.pause != var9) {
         if (this.pause) {
            this.pausePartialTick = this.timer.partialTick;
         } else {
            this.timer.partialTick = this.pausePartialTick;
         }

         this.pause = var9;
      }

      long var10 = Util.getNanos();
      long var12 = var10 - this.lastNanoTime;
      if (var7) {
         this.savedCpuDuration = var12;
      }

      this.frameTimer.logFrameDuration(var12);
      this.lastNanoTime = var10;
      this.profiler.push("fpsUpdate");
      if (this.currentFrameProfile != null && this.currentFrameProfile.isDone()) {
         this.gpuUtilization = this.currentFrameProfile.get() * 100.0 / this.savedCpuDuration;
      }

      while (Util.getMillis() >= this.lastTime + 1000L) {
         String var14;
         if (this.gpuUtilization > 0.0) {
            var14 = " GPU: " + (this.gpuUtilization > 100.0 ? ChatFormatting.RED + "100%" : Math.round(this.gpuUtilization) + "%");
         } else {
            var14 = "";
         }

         fps = this.frames;
         this.fpsString = String.format(
            Locale.ROOT,
            "%d fps T: %s%s%s%s B: %d%s",
            fps,
            var8 == 260 ? "inf" : var8,
            this.options.enableVsync().get() ? " vsync" : "",
            this.options.graphicsMode().get(),
            this.options.cloudStatus().get() == CloudStatus.OFF
               ? ""
               : (this.options.cloudStatus().get() == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"),
            this.options.biomeBlendRadius().get(),
            var14
         );
         this.lastTime += 1000L;
         this.frames = 0;
      }

      this.profiler.pop();
   }

   private boolean shouldRenderFpsPie() {
      return this.options.renderDebug && this.options.renderDebugCharts && !this.options.hideGui;
   }

   private ProfilerFiller constructProfiler(boolean param0, @Nullable SingleTickProfiler param1) {
      if (!param0) {
         this.fpsPieProfiler.disable();
         if (!this.metricsRecorder.isRecording() && param1 == null) {
            return InactiveProfiler.INSTANCE;
         }
      }

      ProfilerFiller var3;
      if (param0) {
         if (!this.fpsPieProfiler.isEnabled()) {
            this.fpsPieRenderTicks = 0;
            this.fpsPieProfiler.enable();
         }

         this.fpsPieRenderTicks++;
         var3 = this.fpsPieProfiler.getFiller();
      } else {
         var3 = InactiveProfiler.INSTANCE;
      }

      if (this.metricsRecorder.isRecording()) {
         var3 = ProfilerFiller.tee(var3, this.metricsRecorder.getProfiler());
      }

      return SingleTickProfiler.decorateFiller(var3, param1);
   }

   private void finishProfilers(boolean param0, @Nullable SingleTickProfiler param1) {
      if (param1 != null) {
         param1.endTick();
      }

      if (param0) {
         this.fpsPieResults = this.fpsPieProfiler.getResults();
      } else {
         this.fpsPieResults = null;
      }

      this.profiler = this.fpsPieProfiler.getFiller();
   }

   @Override
   public void resizeDisplay() {
      int var1 = this.window.calculateScale(this.options.guiScale().get(), this.isEnforceUnicode());
      this.window.setGuiScale(var1);
      if (this.screen != null) {
         this.screen.resize(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
      }

      RenderTarget var2 = this.getMainRenderTarget();
      var2.resize(this.window.getWidth(), this.window.getHeight(), ON_OSX);
      this.gameRenderer.resize(this.window.getWidth(), this.window.getHeight());
      this.mouseHandler.setIgnoreFirstMove();
   }

   @Override
   public void cursorEntered() {
      this.mouseHandler.cursorEntered();
   }

   public int getFps() {
      return fps;
   }

   public long getFrameTimeNs() {
      return this.frameTimeNs;
   }

   private int getFramerateLimit() {
      return this.level != null || this.screen == null && this.overlay == null ? this.window.getFramerateLimit() : 60;
   }

   public void emergencySave() {
      try {
         MemoryReserve.release();
         this.levelRenderer.clear();
      } catch (Throwable var3) {
      }

      try {
         System.gc();
         if (this.isLocalServer && this.singleplayerServer != null) {
            this.singleplayerServer.halt(true);
         }

         this.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
      } catch (Throwable var2) {
      }

      System.gc();
   }

   public boolean debugClientMetricsStart(Consumer<Component> param0) {
      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsStop();
         return false;
      }

      Consumer<ProfileResults> var2 = l_param1 -> {
         if (l_param1 != EmptyProfileResults.EMPTY) {
            int l_var3 = l_param1.getTickDuration();
            double l_var4 = (double)l_param1.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
            this.execute(
               () -> param0.accept(
                  Component.translatable(
                     "commands.debug.stopped", String.format(Locale.ROOT, "%.2f", l_var4), l_var3, String.format(Locale.ROOT, "%.2f", l_var3 / l_var4)
                  )
               )
            );
         }
      };
      Consumer<Path> var3 = l_param1 -> {
         Component l_var3 = Component.literal(l_param1.toString())
            .withStyle(ChatFormatting.UNDERLINE)
            .withStyle(l2_param1 -> l2_param1.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, l_param1.toFile().getParent())));
         this.execute(() -> param0.accept(Component.translatable("debug.profiling.stop", l_var3)));
      };
      SystemReport var4 = fillSystemReport(new SystemReport(), this, this.languageManager, this.launchedVersion, this.options);
      Consumer<List<Path>> var5 = l_param2 -> {
         Path l_var4 = this.archiveProfilingReport(var4, l_param2);
         var3.accept(l_var4);
      };
      Consumer<Path> var6;
      if (this.singleplayerServer == null) {
         var6 = l_param1 -> var5.accept(ImmutableList.of(l_param1));
      } else {
         this.singleplayerServer.fillSystemReport(var4);
         CompletableFuture<Path> var7 = new CompletableFuture<>();
         CompletableFuture<Path> var8 = new CompletableFuture<>();
         CompletableFuture.allOf(var7, var8).thenRunAsync(() -> var5.accept(ImmutableList.of(var7.join(), var8.join())), Util.ioPool());
         this.singleplayerServer.startRecordingMetrics(l_param0 -> {}, var8::complete);
         var6 = var7::complete;
      }

      this.metricsRecorder = ActiveMetricsRecorder.createStarted(
         new ClientMetricsSamplersProvider(Util.timeSource, this.levelRenderer), Util.timeSource, Util.ioPool(), new MetricsPersister("client"), l_param1 -> {
            this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
            var2.accept(l_param1);
         }, var6
      );
      return true;
   }

   private void debugClientMetricsStop() {
      this.metricsRecorder.end();
      if (this.singleplayerServer != null) {
         this.singleplayerServer.finishRecordingMetrics();
      }
   }

   private void debugClientMetricsCancel() {
      this.metricsRecorder.cancel();
      if (this.singleplayerServer != null) {
         this.singleplayerServer.cancelRecordingMetrics();
      }
   }

   private Path archiveProfilingReport(SystemReport param0, List<Path> param1) {
      String var4;
      if (this.isLocalServer()) {
         var4 = this.getSingleplayerServer().getWorldData().getLevelName();
      } else {
         ServerData var5 = this.getCurrentServer();
         var4 = var5 != null ? var5.name : "unknown";
      }

      Path var3;
      try {
         String var5 = String.format(Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), var4, SharedConstants.getCurrentVersion().getId());
         String var6 = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, var5, ".zip");
         var3 = MetricsPersister.PROFILING_RESULTS_DIR.resolve(var6);
      } catch (IOException var5) {
         throw new UncheckedIOException(var5);
      }

      try (FileZipper var5 = new FileZipper(var3)) {
         var5.add(Paths.get("system.txt"), param0.toLineSeparatedString());
         var5.add(Paths.get("client").resolve(this.options.getFile().getName()), this.options.dumpOptionsForReport());
         param1.forEach(var5::add);
      } finally {
         for (Path var10 : param1) {
            try {
               FileUtils.forceDelete(var10.toFile());
            } catch (IOException var11) {
               LOGGER.warn("Failed to delete temporary profiling result {}", var10, var11);
            }
         }
      }

      return var3;
   }

   public void debugFpsMeterKeyPress(int param0) {
      if (this.fpsPieResults != null) {
         List<ResultField> var2 = this.fpsPieResults.getTimes(this.debugPath);
         if (!var2.isEmpty()) {
            ResultField var3 = var2.remove(0);
            if (param0 == 0) {
               if (!var3.name.isEmpty()) {
                  int var4 = this.debugPath.lastIndexOf(30);
                  if (var4 >= 0) {
                     this.debugPath = this.debugPath.substring(0, var4);
                  }
               }
            } else {
               param0--;
               if (param0 < var2.size() && !"unspecified".equals(var2.get(param0).name)) {
                  if (!this.debugPath.isEmpty()) {
                     this.debugPath = this.debugPath + "\u001e";
                  }

                  this.debugPath = this.debugPath + var2.get(param0).name;
               }
            }
         }
      }
   }

   private void renderFpsMeter(PoseStack param0, ProfileResults param1) {
      List<ResultField> var3 = param1.getTimes(this.debugPath);
      ResultField var4 = var3.remove(0);
      RenderSystem.clear(256, ON_OSX);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f var5 = new Matrix4f().setOrtho(0.0F, this.window.getWidth(), this.window.getHeight(), 0.0F, 1000.0F, 3000.0F);
      RenderSystem.setProjectionMatrix(var5);
      PoseStack var6 = RenderSystem.getModelViewStack();
      var6.pushPose();
      var6.setIdentity();
      var6.translate(0.0F, 0.0F, -2000.0F);
      RenderSystem.applyModelViewMatrix();
      RenderSystem.lineWidth(1.0F);
      Tesselator var7 = Tesselator.getInstance();
      BufferBuilder var8 = var7.getBuilder();
      int var9 = 160;
      int var10 = this.window.getWidth() - 160 - 10;
      int var11 = this.window.getHeight() - 320;
      RenderSystem.enableBlend();
      var8.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      var8.vertex(var10 - 176.0F, var11 - 96.0F - 16.0F, 0.0).color(200, 0, 0, 0).endVertex();
      var8.vertex(var10 - 176.0F, var11 + 320, 0.0).color(200, 0, 0, 0).endVertex();
      var8.vertex(var10 + 176.0F, var11 + 320, 0.0).color(200, 0, 0, 0).endVertex();
      var8.vertex(var10 + 176.0F, var11 - 96.0F - 16.0F, 0.0).color(200, 0, 0, 0).endVertex();
      var7.end();
      RenderSystem.disableBlend();
      double var12 = 0.0;

      for (ResultField var15 : var3) {
         int var16 = Mth.floor(var15.percentage / 4.0) + 1;
         var8.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         int var17 = var15.getColor();
         int var18 = var17 >> 16 & 0xFF;
         int var19 = var17 >> 8 & 0xFF;
         int var20 = var17 & 0xFF;
         var8.vertex(var10, var11, 0.0).color(var18, var19, var20, 255).endVertex();

         for (int var21 = var16; var21 >= 0; var21--) {
            float var22 = (float)((var12 + var15.percentage * var21 / var16) * (float) (Math.PI * 2) / 100.0);
            float var23 = Mth.sin(var22) * 160.0F;
            float var24 = Mth.cos(var22) * 160.0F * 0.5F;
            var8.vertex(var10 + var23, var11 - var24, 0.0).color(var18, var19, var20, 255).endVertex();
         }

         var7.end();
         var8.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

         for (int var21 = var16; var21 >= 0; var21--) {
            float var22 = (float)((var12 + var15.percentage * var21 / var16) * (float) (Math.PI * 2) / 100.0);
            float var23 = Mth.sin(var22) * 160.0F;
            float var24 = Mth.cos(var22) * 160.0F * 0.5F;
            if (!(var24 > 0.0F)) {
               var8.vertex(var10 + var23, var11 - var24, 0.0).color(var18 >> 1, var19 >> 1, var20 >> 1, 255).endVertex();
               var8.vertex(var10 + var23, var11 - var24 + 10.0F, 0.0).color(var18 >> 1, var19 >> 1, var20 >> 1, 255).endVertex();
            }
         }

         var7.end();
         var12 += var15.percentage;
      }

      DecimalFormat var14 = new DecimalFormat("##0.00");
      var14.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
      String var15 = ProfileResults.demanglePath(var4.name);
      String var16 = "";
      if (!"unspecified".equals(var15)) {
         var16 = var16 + "[0] ";
      }

      if (var15.isEmpty()) {
         var16 = var16 + "ROOT ";
      } else {
         var16 = var16 + var15 + " ";
      }

      int var17 = 16777215;
      this.font.drawShadow(param0, var16, var10 - 160, var11 - 80 - 16, 16777215);
      var16 = var14.format(var4.globalPercentage) + "%";
      this.font.drawShadow(param0, var16, var10 + 160 - this.font.width(var16), var11 - 80 - 16, 16777215);

      for (int var15x = 0; var15x < var3.size(); var15x++) {
         ResultField var16x = var3.get(var15x);
         StringBuilder var17x = new StringBuilder();
         if ("unspecified".equals(var16x.name)) {
            var17x.append("[?] ");
         } else {
            var17x.append("[").append(var15x + 1).append("] ");
         }

         String var18 = var17x.append(var16x.name).toString();
         this.font.drawShadow(param0, var18, var10 - 160, var11 + 80 + var15x * 8 + 20, var16x.getColor());
         var18 = var14.format(var16x.percentage) + "%";
         this.font.drawShadow(param0, var18, var10 + 160 - 50 - this.font.width(var18), var11 + 80 + var15x * 8 + 20, var16x.getColor());
         var18 = var14.format(var16x.globalPercentage) + "%";
         this.font.drawShadow(param0, var18, var10 + 160 - this.font.width(var18), var11 + 80 + var15x * 8 + 20, var16x.getColor());
      }

      var6.popPose();
      RenderSystem.applyModelViewMatrix();
   }

   public void stop() {
      this.running = false;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void pauseGame(boolean param0) {
      if (this.screen == null) {
         boolean var2 = this.hasSingleplayerServer() && !this.singleplayerServer.isPublished();
         if (var2) {
            this.setScreen(new PauseScreen(!param0));
            this.soundManager.pause();
         } else {
            this.setScreen(new PauseScreen(true));
         }
      }
   }

   private void continueAttack(boolean param0) {
      if (!param0) {
         this.missTime = 0;
      }

      if (this.missTime <= 0 && !this.player.isUsingItem()) {
         if (param0 && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult var2 = (BlockHitResult)this.hitResult;
            BlockPos var3 = var2.getBlockPos();
            if (!this.level.getBlockState(var3).isAir()) {
               Direction var4 = var2.getDirection();
               if (this.gameMode.continueDestroyBlock(var3, var4)) {
                  this.particleEngine.crack(var3, var4);
                  this.player.swing(InteractionHand.MAIN_HAND);
               }
            }
         } else {
            this.gameMode.stopDestroyBlock();
         }
      }
   }

   private boolean startAttack() {
      if (this.missTime > 0) {
         return false;
      }

      if (this.hitResult == null) {
         LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
         if (this.gameMode.hasMissTime()) {
            this.missTime = 10;
         }

         return false;
      } else {
         if (this.player.isHandsBusy()) {
            return false;
         }

         ItemStack var1 = this.player.getItemInHand(InteractionHand.MAIN_HAND);
         if (!var1.isItemEnabled(this.level.enabledFeatures())) {
            return false;
         }

         boolean var2 = false;
         switch (this.hitResult.getType()) {
            case ENTITY:
               this.gameMode.attack(this.player, ((EntityHitResult)this.hitResult).getEntity());
               break;
            case BLOCK:
               BlockHitResult var3 = (BlockHitResult)this.hitResult;
               BlockPos var4 = var3.getBlockPos();
               if (!this.level.getBlockState(var4).isAir()) {
                  this.gameMode.startDestroyBlock(var4, var3.getDirection());
                  if (this.level.getBlockState(var4).isAir()) {
                     var2 = true;
                  }
                  break;
               }
            case MISS:
               if (this.gameMode.hasMissTime()) {
                  this.missTime = 10;
               }

               this.player.resetAttackStrengthTicker();
         }

         this.player.swing(InteractionHand.MAIN_HAND);
         return var2;
      }
   }

   private void startUseItem() {
      if (!this.gameMode.isDestroying()) {
         this.rightClickDelay = 4;
         if (!this.player.isHandsBusy()) {
            if (this.hitResult == null) {
               LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
            }

            for (InteractionHand var4 : InteractionHand.values()) {
               ItemStack var5 = this.player.getItemInHand(var4);
               if (!var5.isItemEnabled(this.level.enabledFeatures())) {
                  return;
               }

               if (this.hitResult != null) {
                  switch (this.hitResult.getType()) {
                     case ENTITY:
                        EntityHitResult var6 = (EntityHitResult)this.hitResult;
                        Entity var7 = var6.getEntity();
                        if (!this.level.getWorldBorder().isWithinBounds(var7.blockPosition())) {
                           return;
                        }

                        InteractionResult var8 = this.gameMode.interactAt(this.player, var7, var6, var4);
                        if (!var8.consumesAction()) {
                           var8 = this.gameMode.interact(this.player, var7, var4);
                        }

                        if (var8.consumesAction()) {
                           if (var8.shouldSwing()) {
                              this.player.swing(var4);
                           }

                           return;
                        }
                        break;
                     case BLOCK:
                        BlockHitResult var9 = (BlockHitResult)this.hitResult;
                        int var10 = var5.getCount();
                        InteractionResult var11 = this.gameMode.useItemOn(this.player, var4, var9);
                        if (var11.consumesAction()) {
                           if (var11.shouldSwing()) {
                              this.player.swing(var4);
                              if (!var5.isEmpty() && (var5.getCount() != var10 || this.gameMode.hasInfiniteItems())) {
                                 this.gameRenderer.itemInHandRenderer.itemUsed(var4);
                              }
                           }

                           return;
                        }

                        if (var11 == InteractionResult.FAIL) {
                           return;
                        }
                  }
               }

               if (!var5.isEmpty()) {
                  InteractionResult var6 = this.gameMode.useItem(this.player, var4);
                  if (var6.consumesAction()) {
                     if (var6.shouldSwing()) {
                        this.player.swing(var4);
                     }

                     this.gameRenderer.itemInHandRenderer.itemUsed(var4);
                     return;
                  }
               }
            }
         }
      }
   }

   public MusicManager getMusicManager() {
      return this.musicManager;
   }

   public void tick() {
      if (this.rightClickDelay > 0) {
         this.rightClickDelay--;
      }

      this.profiler.push("gui");
      this.chatListener.tick();
      this.gui.tick(this.pause);
      this.profiler.pop();
      this.gameRenderer.pick(1.0F);
      this.tutorial.onLookAt(this.level, this.hitResult);
      this.profiler.push("gameMode");
      if (!this.pause && this.level != null) {
         this.gameMode.tick();
      }

      this.profiler.popPush("textures");
      if (this.level != null) {
         this.textureManager.tick();
      }

      if (this.screen != null || this.player == null) {
         if (this.screen instanceof InBedChatScreen var1 && !this.player.isSleeping()) {
            var1.onPlayerWokeUp();
         }
      } else if (this.player.isDeadOrDying() && !(this.screen instanceof DeathScreen)) {
         this.setScreen(null);
      } else if (this.player.isSleeping() && this.level != null) {
         this.setScreen(new InBedChatScreen());
      }

      if (this.screen != null) {
         this.missTime = 10000;
      }

      if (this.screen != null) {
         Screen.wrapScreenError(() -> this.screen.tick(), "Ticking screen", this.screen.getClass().getCanonicalName());
      }

      if (!this.options.renderDebug) {
         this.gui.clearCache();
      }

      if (this.overlay == null && (this.screen == null || this.screen.passEvents)) {
         this.profiler.popPush("Keybindings");
         this.handleKeybinds();
         if (this.missTime > 0) {
            this.missTime--;
         }
      }

      if (this.level != null) {
         this.profiler.popPush("gameRenderer");
         if (!this.pause) {
            this.gameRenderer.tick();
         }

         this.profiler.popPush("levelRenderer");
         if (!this.pause) {
            this.levelRenderer.tick();
         }

         this.profiler.popPush("level");
         if (!this.pause) {
            if (this.level.getSkyFlashTime() > 0) {
               this.level.setSkyFlashTime(this.level.getSkyFlashTime() - 1);
            }

            this.level.tickEntities();
         }
      } else if (this.gameRenderer.currentEffect() != null) {
         this.gameRenderer.shutdownEffect();
      }

      if (!this.pause) {
         this.musicManager.tick();
      }

      this.soundManager.tick(this.pause);
      if (this.level != null) {
         if (!this.pause) {
            if (!this.options.joinedFirstServer && this.isMultiplayerServer()) {
               Component var1 = Component.translatable("tutorial.socialInteractions.title");
               Component var2 = Component.translatable("tutorial.socialInteractions.description", Tutorial.key("socialInteractions"));
               this.socialInteractionsToast = new TutorialToast(TutorialToast.Icons.SOCIAL_INTERACTIONS, var1, var2, true);
               this.tutorial.addTimedToast(this.socialInteractionsToast, 160);
               this.options.joinedFirstServer = true;
               this.options.save();
            }

            this.tutorial.tick();

            try {
               this.level.tick(() -> true);
            } catch (Throwable var1) {
               CrashReport var2 = CrashReport.forThrowable(var1, "Exception in world tick");
               if (this.level == null) {
                  CrashReportCategory var3 = var2.addCategory("Affected level");
                  var3.setDetail("Problem", "Level is null!");
               } else {
                  this.level.fillReportDetails(var2);
               }

               throw new ReportedException(var2);
            }
         }

         this.profiler.popPush("animateTick");
         if (!this.pause && this.level != null) {
            this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
         }

         this.profiler.popPush("particles");
         if (!this.pause) {
            this.particleEngine.tick();
         }
      } else if (this.pendingConnection != null) {
         this.profiler.popPush("pendingConnection");
         this.pendingConnection.tick();
      }

      this.profiler.popPush("keyboard");
      this.keyboardHandler.tick();
      this.profiler.popPush("inputEvents");
      // Eaglercraft delivers keyboard and mouse through queues the game drains, rather than through
      // the GLFW callbacks upstream registers in MouseHandler.setup/KeyboardHandler.setup. Draining
      // here, once per tick and on the game thread, is where those callbacks used to run; each
      // drained event is re-posted through this.execute() so the ordering relative to the rest of
      // the tick matches the push/replay queue semantics the callbacks already relied on.
      this.mouseHandler.pumpEvents();
      this.keyboardHandler.pumpEvents();
      this.profiler.pop();
   }

   private boolean isMultiplayerServer() {
      return !this.isLocalServer || this.singleplayerServer != null && this.singleplayerServer.isPublished();
   }

   private void handleKeybinds() {
      while (this.options.keyTogglePerspective.consumeClick()) {
         CameraType var1 = this.options.getCameraType();
         this.options.setCameraType(this.options.getCameraType().cycle());
         if (var1.isFirstPerson() != this.options.getCameraType().isFirstPerson()) {
            this.gameRenderer.checkEntityPostEffect(this.options.getCameraType().isFirstPerson() ? this.getCameraEntity() : null);
         }

         this.levelRenderer.needsUpdate();
      }

      while (this.options.keySmoothCamera.consumeClick()) {
         this.options.smoothCamera = !this.options.smoothCamera;
      }

      for (int var1 = 0; var1 < 9; var1++) {
         boolean var2 = this.options.keySaveHotbarActivator.isDown();
         boolean var3 = this.options.keyLoadHotbarActivator.isDown();
         if (this.options.keyHotbarSlots[var1].consumeClick()) {
            if (this.player.isSpectator()) {
               this.gui.getSpectatorGui().onHotbarSelected(var1);
            } else if (!this.player.isCreative() || this.screen != null || !var3 && !var2) {
               this.player.getInventory().selected = var1;
            } else {
               CreativeModeInventoryScreen.handleHotbarLoadOrSave(this, var1, var3, var2);
            }
         }
      }

      while (this.options.keySocialInteractions.consumeClick()) {
         if (!this.isMultiplayerServer()) {
            this.player.displayClientMessage(SOCIAL_INTERACTIONS_NOT_AVAILABLE, true);
            this.narrator.sayNow(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
         } else {
            if (this.socialInteractionsToast != null) {
               this.tutorial.removeTimedToast(this.socialInteractionsToast);
               this.socialInteractionsToast = null;
            }

            this.setScreen(new SocialInteractionsScreen());
         }
      }

      while (this.options.keyInventory.consumeClick()) {
         if (this.gameMode.isServerControlledInventory()) {
            this.player.sendOpenInventory();
         } else {
            this.tutorial.onOpenInventory();
            this.setScreen(new InventoryScreen(this.player));
         }
      }

      while (this.options.keyAdvancements.consumeClick()) {
         this.setScreen(new AdvancementsScreen(this.player.connection.getAdvancements()));
      }

      while (this.options.keySwapOffhand.consumeClick()) {
         if (!this.player.isSpectator()) {
            this.getConnection()
               .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
         }
      }

      while (this.options.keyDrop.consumeClick()) {
         if (!this.player.isSpectator() && this.player.drop(Screen.hasControlDown())) {
            this.player.swing(InteractionHand.MAIN_HAND);
         }
      }

      while (this.options.keyChat.consumeClick()) {
         this.openChatScreen("");
      }

      if (this.screen == null && this.overlay == null && this.options.keyCommand.consumeClick()) {
         this.openChatScreen("/");
      }

      boolean var1 = false;
      if (this.player.isUsingItem()) {
         if (!this.options.keyUse.isDown()) {
            this.gameMode.releaseUsingItem(this.player);
         }

         while (this.options.keyAttack.consumeClick()) {
         }

         while (this.options.keyUse.consumeClick()) {
         }

         while (this.options.keyPickItem.consumeClick()) {
         }
      } else {
         while (this.options.keyAttack.consumeClick()) {
            var1 |= this.startAttack();
         }

         while (this.options.keyUse.consumeClick()) {
            this.startUseItem();
         }

         while (this.options.keyPickItem.consumeClick()) {
            this.pickBlock();
         }
      }

      if (this.options.keyUse.isDown() && this.rightClickDelay == 0 && !this.player.isUsingItem()) {
         this.startUseItem();
      }

      this.continueAttack(this.screen == null && !var1 && this.options.keyAttack.isDown() && this.mouseHandler.isMouseGrabbed());
   }

   public ClientTelemetryManager getTelemetryManager() {
      return this.telemetryManager;
   }

   public double getGpuUtilization() {
      return this.gpuUtilization;
   }

   public ProfileKeyPairManager getProfileKeyPairManager() {
      return this.profileKeyPairManager;
   }

   public WorldOpenFlows createWorldOpenFlows() {
      return new WorldOpenFlows(this, this.levelSource);
   }

   public void doWorldLoad(String param0, LevelStorageSource.LevelStorageAccess param1, PackRepository param2, WorldStem param3, boolean param4) {
      this.clearLevel();
      this.progressListener.set(null);
      Services services = Services.createOffline(this.gameDirectory);
      services.profileCache().setExecutor(this);
      SkullBlockEntity.setup(services, this);
      GameProfileCache.setUsesAuthentication(false);
      try {
         this.singleplayerServer = new IntegratedServer(Thread.currentThread(), this, param1, param2, param3, services,
            radius -> {
               StoringChunkProgressListener listener = new StoringChunkProgressListener(radius);
               listener.start();
               this.progressListener.set(listener);
               this.setScreen(new net.minecraft.client.gui.screens.StartingServerScreen());
               return listener;
            });
         this.isLocalServer = true;
         this.singleplayerServer.startCooperative();
         Connection connection = this.singleplayerServer.getConnection().connectLocalClient();
         connection.setListener(new ClientHandshakePacketListenerImpl(connection, this, null, null, param4, null, status -> {}));
         this.pendingConnection = connection;
         connection.send(new ClientIntentionPacket("local", 0, ConnectionProtocol.LOGIN));
         connection.send(new ServerboundHelloPacket(this.getUser().getName(), java.util.Optional.of(this.getUser().getProfileId())));
      } catch (Exception failure) {
         LOGGER.error("Failed to start integrated server for {}", param0, failure);
         try {
            if (this.singleplayerServer != null) {
               this.singleplayerServer.halt(false);
            } else {
               param3.close();
               param1.close();
            }
         } catch (Exception cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
         } finally {
            this.singleplayerServer = null;
            this.pendingConnection = null;
            this.isLocalServer = false;
            services.profileCache().clearExecutor();
            SkullBlockEntity.clear();
         }
         this.setScreen(new net.minecraft.client.gui.screens.AlertScreen(
            () -> this.setScreen(new TitleScreen()),
            Component.translatable("selectWorld.unable_to_load"),
            Component.literal("The world could not be started. Your saved worlds are still available. See the game log for details.")));
      }
   }

   /** Render progress while the cooperative server pumps its world-generation tasks. */
   public void renderWorldLoading() {
      this.runTick(false);
   }

   public void setLevel(ClientLevel param0) {
      ProgressScreen var2 = new ProgressScreen(true);
      var2.progressStartNoAbort(Component.translatable("connect.joining"));
      this.updateScreenAndTick(var2);
      this.level = param0;
      this.updateLevelInEngines(param0);
      if (!this.isLocalServer) {
         Services var3 = Services.createOffline(this.gameDirectory);
         var3.profileCache().setExecutor(this);
         SkullBlockEntity.setup(var3, this);
         GameProfileCache.setUsesAuthentication(false);
      }
   }

   public void clearLevel() {
      this.clearLevel(new ProgressScreen(true));
   }

   public void clearLevel(Screen param0) {
      ClientPacketListener var2 = this.getConnection();
      if (var2 != null) {
         this.dropAllTasks();
         var2.close();
      }

      this.playerSocialManager.stopOnlineMode();
      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsCancel();
      }

      IntegratedServer var3 = this.singleplayerServer;
      this.singleplayerServer = null;
      if (var3 != null) var3.halt(false);
      this.isLocalServer = false;
      this.gameRenderer.resetData();
      this.gameMode = null;
      this.narrator.clear();
      this.updateScreenAndTick(param0);
      if (this.level != null) {
         if (var3 != null) {
            this.profiler.push("waitForServer");

            while (!var3.isShutdown()) {
               this.runTick(false);
            }

            this.profiler.pop();
         }

         this.downloadedPackSource.clearServerPack();
         this.gui.onDisconnected();
         this.isLocalServer = false;
      }

      this.level = null;
      this.updateLevelInEngines(null);
      this.player = null;
      SkullBlockEntity.clear();
   }

   private void updateScreenAndTick(Screen param0) {
      this.profiler.push("forcedTick");
      this.soundManager.stop();
      this.cameraEntity = null;
      this.pendingConnection = null;
      this.setScreen(param0);
      this.runTick(false);
      this.profiler.pop();
   }

   public void forceSetScreen(Screen param0) {
      this.profiler.push("forcedTick");
      this.setScreen(param0);
      this.runTick(false);
      this.profiler.pop();
   }

   private void updateLevelInEngines(@Nullable ClientLevel param0) {
      this.levelRenderer.setLevel(param0);
      this.particleEngine.setLevel(param0);
      this.blockEntityRenderDispatcher.setLevel(param0);
      this.updateTitle();
   }

   public boolean telemetryOptInExtra() {
      return this.extraTelemetryAvailable() && this.options.telemetryOptInExtra().get();
   }

   public boolean extraTelemetryAvailable() {
      return this.allowsTelemetry() && this.userApiService.properties().flag(UserFlag.OPTIONAL_TELEMETRY_AVAILABLE);
   }

   public boolean allowsTelemetry() {
      return this.userApiService.properties().flag(UserFlag.TELEMETRY_ENABLED);
   }

   public boolean allowsMultiplayer() {
      return this.allowsMultiplayer && this.userApiService.properties().flag(UserFlag.SERVERS_ALLOWED) && this.multiplayerBan() == null;
   }

   public boolean allowsRealms() {
      return this.userApiService.properties().flag(UserFlag.REALMS_ALLOWED) && this.multiplayerBan() == null;
   }

   public boolean shouldShowBanNotice() {
      return this.multiplayerBan() != null;
   }

   @Nullable
   public BanDetails multiplayerBan() {
      return this.userApiService.properties().bannedScopes().get("MULTIPLAYER");
   }

   public boolean isBlocked(UUID param0) {
      return this.getChatStatus().isChatAllowed(false)
         ? this.playerSocialManager.shouldHideMessageFrom(param0)
         : (this.player == null || !param0.equals(this.player.getUUID())) && !param0.equals(Util.NIL_UUID);
   }

   public Minecraft.ChatStatus getChatStatus() {
      if (this.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
         return Minecraft.ChatStatus.DISABLED_BY_OPTIONS;
      } else if (!this.allowsChat) {
         return Minecraft.ChatStatus.DISABLED_BY_LAUNCHER;
      } else {
         return !this.userApiService.properties().flag(UserFlag.CHAT_ALLOWED) ? Minecraft.ChatStatus.DISABLED_BY_PROFILE : Minecraft.ChatStatus.ENABLED;
      }
   }

   public final boolean isDemo() {
      return this.demo;
   }

   @Nullable
   public ClientPacketListener getConnection() {
      return this.player == null ? null : this.player.connection;
   }

   public static boolean renderNames() {
      return !instance.options.hideGui;
   }

   public static boolean useFancyGraphics() {
      return instance.options.graphicsMode().get().getId() >= GraphicsStatus.FANCY.getId();
   }

   public static boolean useShaderTransparency() {
      return !instance.gameRenderer.isPanoramicMode() && instance.options.graphicsMode().get().getId() >= GraphicsStatus.FABULOUS.getId();
   }

   public static boolean useAmbientOcclusion() {
      return instance.options.ambientOcclusion().get();
   }

   private void pickBlock() {
      if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
         boolean var1 = this.player.getAbilities().instabuild;
         BlockEntity var2 = null;
         HitResult.Type var4 = this.hitResult.getType();
         ItemStack var3;
         if (var4 == HitResult.Type.BLOCK) {
            BlockPos var5 = ((BlockHitResult)this.hitResult).getBlockPos();
            BlockState var6 = this.level.getBlockState(var5);
            if (var6.isAir()) {
               return;
            }

            Block var7 = var6.getBlock();
            var3 = var7.getCloneItemStack(this.level, var5, var6);
            if (var3.isEmpty()) {
               return;
            }

            if (var1 && Screen.hasControlDown() && var6.hasBlockEntity()) {
               var2 = this.level.getBlockEntity(var5);
            }
         } else {
            if (var4 != HitResult.Type.ENTITY || !var1) {
               return;
            }

            Entity var5 = ((EntityHitResult)this.hitResult).getEntity();
            var3 = var5.getPickResult();
            if (var3 == null) {
               return;
            }
         }

         if (var3.isEmpty()) {
            String var5 = "";
            if (var4 == HitResult.Type.BLOCK) {
               var5 = BuiltInRegistries.BLOCK.getKey(this.level.getBlockState(((BlockHitResult)this.hitResult).getBlockPos()).getBlock()).toString();
            } else if (var4 == HitResult.Type.ENTITY) {
               var5 = BuiltInRegistries.ENTITY_TYPE.getKey(((EntityHitResult)this.hitResult).getEntity().getType()).toString();
            }

            LOGGER.warn("Picking on: [{}] {} gave null item", var4, var5);
         } else {
            Inventory var5 = this.player.getInventory();
            if (var2 != null) {
               this.addCustomNbtData(var3, var2);
            }

            int var6 = var5.findSlotMatchingItem(var3);
            if (var1) {
               var5.setPickedItem(var3);
               this.gameMode.handleCreativeModeItemAdd(this.player.getItemInHand(InteractionHand.MAIN_HAND), 36 + var5.selected);
            } else if (var6 != -1) {
               if (Inventory.isHotbarSlot(var6)) {
                  var5.selected = var6;
               } else {
                  this.gameMode.handlePickItem(var6);
               }
            }
         }
      }
   }

   private void addCustomNbtData(ItemStack param0, BlockEntity param1) {
      CompoundTag var3 = param1.saveWithFullMetadata();
      BlockItem.setBlockEntityData(param0, param1.getType(), var3);
      if (param0.getItem() instanceof PlayerHeadItem && var3.contains("SkullOwner")) {
         CompoundTag var4 = var3.getCompound("SkullOwner");
         CompoundTag var5 = param0.getOrCreateTag();
         var5.put("SkullOwner", var4);
         CompoundTag var6 = var5.getCompound("BlockEntityTag");
         var6.remove("SkullOwner");
         var6.remove("x");
         var6.remove("y");
         var6.remove("z");
      } else {
         CompoundTag var4 = new CompoundTag();
         ListTag var5 = new ListTag();
         var5.add(StringTag.valueOf("\"(+NBT)\""));
         var4.put("Lore", var5);
         param0.addTagElement("display", var4);
      }
   }

   public CrashReport fillReport(CrashReport param0) {
      SystemReport var2 = param0.getSystemReport();
      fillSystemReport(var2, this, this.languageManager, this.launchedVersion, this.options);
      if (this.level != null) {
         this.level.fillReportDetails(param0);
      }

      if (this.singleplayerServer != null) {
         this.singleplayerServer.fillSystemReport(var2);
      }

      this.reloadStateTracker.fillCrashReport(param0);
      return param0;
   }

   public static void fillReport(@Nullable Minecraft param0, @Nullable LanguageManager param1, String param2, @Nullable Options param3, CrashReport param4) {
      SystemReport var5 = param4.getSystemReport();
      fillSystemReport(var5, param0, param1, param2, param3);
   }

   private static SystemReport fillSystemReport(
      SystemReport param0, @Nullable Minecraft param1, @Nullable LanguageManager param2, String param3, Options param4
   ) {
      param0.setDetail("Launched Version", () -> param3);
      param0.setDetail("Backend library", RenderSystem::getBackendDescription);
      param0.setDetail("Backend API", RenderSystem::getApiDescription);
      param0.setDetail("Window size", () -> param1 != null ? param1.window.getWidth() + "x" + param1.window.getHeight() : "<not initialized>");
      param0.setDetail("GL Caps", RenderSystem::getCapsString);
      param0.setDetail("GL debug messages", () -> GlDebug.isDebugEnabled() ? String.join("\n", GlDebug.getLastOpenGlDebugMessages()) : "<disabled>");
      param0.setDetail("Using VBOs", () -> "Yes");
      param0.setDetail("Is Modded", () -> checkModStatus().fullDescription());
      param0.setDetail("Type", "Client (map_client.txt)");
      if (param4 != null) {
         if (instance != null) {
            String var5 = instance.getGpuWarnlistManager().getAllWarnings();
            if (var5 != null) {
               param0.setDetail("GPU Warnings", var5);
            }
         }

         param0.setDetail("Graphics mode", param4.graphicsMode().get().toString());
         param0.setDetail("Resource Packs", () -> {
            StringBuilder l_var1 = new StringBuilder();

            for (String l_var3 : param4.resourcePacks) {
               if (l_var1.length() > 0) {
                  l_var1.append(", ");
               }

               l_var1.append(l_var3);
               if (param4.incompatibleResourcePacks.contains(l_var3)) {
                  l_var1.append(" (incompatible)");
               }
            }

            return l_var1.toString();
         });
      }

      if (param2 != null) {
         param0.setDetail("Current Language", () -> param2.getSelected());
      }

      param0.setDetail("CPU", GlUtil::getCpuInfo);
      return param0;
   }

   public static Minecraft getInstance() {
      return instance;
   }

   public CompletableFuture<Void> delayTextureReload() {
      return this.submit((java.util.function.Supplier<CompletableFuture<Void>>)this::reloadResourcePacks).thenCompose(l_param0 -> (CompletionStage<Void>)l_param0);
   }

   public void updateReportEnvironment(ReportEnvironment param0) {
      if (!this.reportingContext.matches(param0)) {
         this.reportingContext = ReportingContext.create(param0, this.userApiService);
      }
   }

   @Nullable
   public ServerData getCurrentServer() {
      return Optionull.map(this.getConnection(), ClientPacketListener::getServerData);
   }

   public boolean isLocalServer() {
      return this.isLocalServer;
   }

   public boolean hasSingleplayerServer() {
      return this.isLocalServer && this.singleplayerServer != null;
   }

   @Nullable
   public IntegratedServer getSingleplayerServer() {
      return this.singleplayerServer;
   }

   public boolean isSingleplayer() {
      IntegratedServer var1 = this.getSingleplayerServer();
      return var1 != null && !var1.isPublished();
   }

   public User getUser() {
      return this.user;
   }

   /**
    * Rebuilds the session {@link User} from the local Eagler profile.
    *
    * <p>The profile editor writes to {@link EaglerProfile} and local storage, but
    * the handshake reads {@code this.user}, which was built once at launch. Left
    * stale, a name typed mid-session would be saved and then silently ignored
    * until the next reload. Call this after a successful profile save.</p>
    */
   public void refreshUserFromProfile() {
      User previous = this.user;
      this.user = new User(
         EaglerProfile.getName(),
         EaglerProfile.getPlayerUUIDString(),
         previous.getAccessToken(),
         previous.getXuid(),
         previous.getClientId(),
         previous.getType()
      );
   }

   public PropertyMap getProfileProperties() {
      if (this.profileProperties.isEmpty()) {
         GameProfile var1 = this.getMinecraftSessionService().fillProfileProperties(this.user.getGameProfile(), false);
         this.profileProperties.putAll(var1.getProperties());
      }

      return this.profileProperties;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public TextureManager getTextureManager() {
      return this.textureManager;
   }

   public ResourceManager getResourceManager() {
      return this.resourceManager;
   }

   public PackRepository getResourcePackRepository() {
      return this.resourcePackRepository;
   }

   public VanillaPackResources getVanillaPackResources() {
      return this.vanillaPackResources;
   }

   public DownloadedPackSource getDownloadedPackSource() {
      return this.downloadedPackSource;
   }

   public Path getResourcePackDirectory() {
      return this.resourcePackDirectory;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public Function<ResourceLocation, TextureAtlasSprite> getTextureAtlas(ResourceLocation param0) {
      return this.modelManager.getAtlas(param0)::getSprite;
   }

   public boolean is64Bit() {
      return this.is64bit;
   }

   public boolean isPaused() {
      return this.pause;
   }

   public GpuWarnlistManager getGpuWarnlistManager() {
      return this.gpuWarnlistManager;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public Music getSituationalMusic() {
      if (this.screen instanceof WinScreen) {
         return Musics.CREDITS;
      }

      if (this.player != null) {
         if (this.player.level.dimension() == Level.END) {
            return this.gui.getBossOverlay().shouldPlayMusic() ? Musics.END_BOSS : Musics.END;
         } else {
            Holder<Biome> var1 = this.player.level.getBiome(this.player.blockPosition());
            if (!this.musicManager.isPlayingMusic(Musics.UNDER_WATER) && (!this.player.isUnderWater() || !var1.is(BiomeTags.PLAYS_UNDERWATER_MUSIC))) {
               return this.player.level.dimension() != Level.NETHER && this.player.getAbilities().instabuild && this.player.getAbilities().mayfly
                  ? Musics.CREATIVE
                  : var1.value().getBackgroundMusic().orElse(Musics.GAME);
            } else {
               return Musics.UNDER_WATER;
            }
         }
      } else {
         return Musics.MENU;
      }
   }

   public MinecraftSessionService getMinecraftSessionService() {
      return this.minecraftSessionService;
   }

   public SkinManager getSkinManager() {
      return this.skinManager;
   }

   @Nullable
   public Entity getCameraEntity() {
      return this.cameraEntity;
   }

   public void setCameraEntity(Entity param0) {
      this.cameraEntity = param0;
      this.gameRenderer.checkEntityPostEffect(param0);
   }

   public boolean shouldEntityAppearGlowing(Entity param0) {
      return param0.isCurrentlyGlowing()
         || this.player != null && this.player.isSpectator() && this.options.keySpectatorOutlines.isDown() && param0.getType() == EntityType.PLAYER;
   }

   @Override
   protected Thread getRunningThread() {
      return this.gameThread;
   }

   @Override
   protected Runnable wrapRunnable(Runnable param0) {
      return param0;
   }

   @Override
   protected boolean shouldRun(Runnable param0) {
      return true;
   }

   public BlockRenderDispatcher getBlockRenderer() {
      return this.blockRenderer;
   }

   public EntityRenderDispatcher getEntityRenderDispatcher() {
      return this.entityRenderDispatcher;
   }

   public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
      return this.blockEntityRenderDispatcher;
   }

   public ItemRenderer getItemRenderer() {
      return this.itemRenderer;
   }

   public <T> SearchTree<T> getSearchTree(SearchRegistry.Key<T> param0) {
      return this.searchRegistry.getTree(param0);
   }

   public <T> void populateSearchTree(SearchRegistry.Key<T> param0, List<T> param1) {
      this.searchRegistry.populate(param0, param1);
   }

   public FrameTimer getFrameTimer() {
      return this.frameTimer;
   }

   public boolean isConnectedToRealms() {
      return this.connectedToRealms;
   }

   public void setConnectedToRealms(boolean param0) {
      this.connectedToRealms = param0;
   }

   public DataFixer getFixerUpper() {
      return this.fixerUpper;
   }

   public float getFrameTime() {
      return this.timer.partialTick;
   }

   public float getDeltaFrameTime() {
      return this.timer.tickDelta;
   }

   public BlockColors getBlockColors() {
      return this.blockColors;
   }

   public boolean showOnlyReducedInfo() {
      return this.player != null && this.player.isReducedDebugInfo() || this.options.reducedDebugInfo().get();
   }

   public ToastComponent getToasts() {
      return this.toast;
   }

   public Tutorial getTutorial() {
      return this.tutorial;
   }

   public boolean isWindowActive() {
      return this.windowActive;
   }

   public HotbarManager getHotbarManager() {
      return this.hotbarManager;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public PaintingTextureManager getPaintingTextures() {
      return this.paintingTextures;
   }

   public MobEffectTextureManager getMobEffectTextures() {
      return this.mobEffectTextures;
   }

   @Override
   public void setWindowActive(boolean param0) {
      this.windowActive = param0;
   }

   public Component grabPanoramixScreenshot(File param0, int param1, int param2) {
      int var4 = this.window.getWidth();
      int var5 = this.window.getHeight();
      RenderTarget var6 = new TextureTarget(param1, param2, true, ON_OSX);
      float var7 = this.player.getXRot();
      float var8 = this.player.getYRot();
      float var9 = this.player.xRotO;
      float var10 = this.player.yRotO;
      this.gameRenderer.setRenderBlockOutline(false);

      try {
         this.gameRenderer.setPanoramicMode(true);
         this.levelRenderer.graphicsChanged();
         this.window.setWidth(param1);
         this.window.setHeight(param2);

         for (int var11 = 0; var11 < 6; var11++) {
            switch (var11) {
               case 0:
                  this.player.setYRot(var8);
                  this.player.setXRot(0.0F);
                  break;
               case 1:
                  this.player.setYRot((var8 + 90.0F) % 360.0F);
                  this.player.setXRot(0.0F);
                  break;
               case 2:
                  this.player.setYRot((var8 + 180.0F) % 360.0F);
                  this.player.setXRot(0.0F);
                  break;
               case 3:
                  this.player.setYRot((var8 - 90.0F) % 360.0F);
                  this.player.setXRot(0.0F);
                  break;
               case 4:
                  this.player.setYRot(var8);
                  this.player.setXRot(-90.0F);
                  break;
               case 5:
               default:
                  this.player.setYRot(var8);
                  this.player.setXRot(90.0F);
            }

            this.player.yRotO = this.player.getYRot();
            this.player.xRotO = this.player.getXRot();
            var6.bindWrite(true);
            this.gameRenderer.renderLevel(1.0F, 0L, new PoseStack());

            try {
               Thread.sleep(10L);
            } catch (InterruptedException var17) {
            }

            Screenshot.grab(param0, "panorama_" + var11 + ".png", var6, l_param0 -> {});
         }

         Component var11 = Component.literal(param0.getName())
            .withStyle(ChatFormatting.UNDERLINE)
            .withStyle(l_param1 -> l_param1.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, param0.getAbsolutePath())));
         return Component.translatable("screenshot.success", var11);
      } catch (Exception var11) {
         LOGGER.error("Couldn't save image", var11);
         return Component.translatable("screenshot.failure", var11.getMessage());
      } finally {
         this.player.setXRot(var7);
         this.player.setYRot(var8);
         this.player.xRotO = var9;
         this.player.yRotO = var10;
         this.gameRenderer.setRenderBlockOutline(true);
         this.window.setWidth(var4);
         this.window.setHeight(var5);
         var6.destroyBuffers();
         this.gameRenderer.setPanoramicMode(false);
         this.levelRenderer.graphicsChanged();
         this.getMainRenderTarget().bindWrite(true);
      }
   }

   private Component grabHugeScreenshot(File param0, int param1, int param2, int param3, int param4) {
      try {
         ByteBuffer var6 = GlUtil.allocateMemory(param1 * param2 * 3);
         Screenshot var7 = new Screenshot(param0, param3, param4, param2);
         float var8 = (float)param3 / param1;
         float var9 = (float)param4 / param2;
         float var10 = var8 > var9 ? var8 : var9;

         for (int var11 = (param4 - 1) / param2 * param2; var11 >= 0; var11 -= param2) {
            for (int var12 = 0; var12 < param3; var12 += param1) {
               RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
               float var13 = (param3 - param1) / 2.0F * 2.0F - var12 * 2;
               float var14 = (param4 - param2) / 2.0F * 2.0F - var11 * 2;
               var13 /= param1;
               var14 /= param2;
               this.gameRenderer.renderZoomed(var10, var13, var14);
               var6.clear();
               RenderSystem.pixelStore(3333, 1);
               RenderSystem.pixelStore(3317, 1);
               RenderSystem.readPixels(0, 0, param1, param2, 32992, 5121, var6);
               var7.addRegion(var6, var12, var11, param1, param2);
            }

            var7.saveRow();
         }

         File var11 = var7.close();
         GlUtil.freeMemory(var6);
         Component var12 = Component.literal(var11.getName())
            .withStyle(ChatFormatting.UNDERLINE)
            .withStyle(l_param1 -> l_param1.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, var11.getAbsolutePath())));
         return Component.translatable("screenshot.success", var12);
      } catch (Exception var6) {
         LOGGER.warn("Couldn't save screenshot", var6);
         return Component.translatable("screenshot.failure", var6.getMessage());
      }
   }

   public ProfilerFiller getProfiler() {
      return this.profiler;
   }

   @Nullable
   public StoringChunkProgressListener getProgressListener() {
      return this.progressListener.get();
   }

   public SplashManager getSplashManager() {
      return this.splashManager;
   }

   @Nullable
   public Overlay getOverlay() {
      return this.overlay;
   }

   public PlayerSocialManager getPlayerSocialManager() {
      return this.playerSocialManager;
   }

   public boolean renderOnThread() {
      return false;
   }

   public Window getWindow() {
      return this.window;
   }

   public RenderBuffers renderBuffers() {
      return this.renderBuffers;
   }

   public void updateMaxMipLevel(int param0) {
      this.modelManager.updateMaxMipLevel(param0);
   }

   public EntityModelSet getEntityModels() {
      return this.entityModels;
   }

   public boolean isTextFilteringEnabled() {
      return this.userApiService.properties().flag(UserFlag.PROFANITY_FILTER_ENABLED);
   }

   public void prepareForMultiplayer() {
      this.playerSocialManager.startOnlineMode();
      this.getProfileKeyPairManager().prepareKeyPair();
   }

   public Realms32BitWarningStatus getRealms32BitWarningStatus() {
      return this.realms32BitWarningStatus;
   }

   public SignatureValidator getServiceSignatureValidator() {
      return this.serviceSignatureValidator;
   }

   public InputType getLastInputType() {
      return this.lastInputType;
   }

   public void setLastInputType(InputType param0) {
      this.lastInputType = param0;
   }

   public GameNarrator getNarrator() {
      return this.narrator;
   }

   public ChatListener getChatListener() {
      return this.chatListener;
   }

   public ReportingContext getReportingContext() {
      return this.reportingContext;
   }

   public RealmsDataFetcher realmsDataFetcher() {
      return this.realmsDataFetcher;
   }

   public enum ChatStatus {
      ENABLED(CommonComponents.EMPTY) {
         @Override
         public boolean isChatAllowed(boolean param0) {
            return true;
         }
      },
      DISABLED_BY_OPTIONS(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED)) {
         @Override
         public boolean isChatAllowed(boolean param0) {
            return false;
         }
      },
      DISABLED_BY_LAUNCHER(Component.translatable("chat.disabled.launcher").withStyle(ChatFormatting.RED)) {
         @Override
         public boolean isChatAllowed(boolean param0) {
            return param0;
         }
      },
      DISABLED_BY_PROFILE(
         Component.translatable("chat.disabled.profile", Component.keybind(Minecraft.instance.options.keyChat.getName())).withStyle(ChatFormatting.RED)
      ) {
         @Override
         public boolean isChatAllowed(boolean param0) {
            return param0;
         }
      };

      static final Component INFO_DISABLED_BY_PROFILE = Component.translatable("chat.disabled.profile.moreInfo");
      private final Component message;

      ChatStatus(Component param2) {
         this.message = param2;
      }

      public Component getMessage() {
         return this.message;
      }

      public abstract boolean isChatAllowed(boolean var1);
   }
}
