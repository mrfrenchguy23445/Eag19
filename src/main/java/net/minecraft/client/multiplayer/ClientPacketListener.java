package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DebugQueryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DemoIntroScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsUpdateListener;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.debug.BeeDebugRenderer;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.client.renderer.debug.GoalSelectorDebugRenderer;
import net.minecraft.client.renderer.debug.NeighborsUpdateRenderer;
import net.minecraft.client.renderer.debug.WorldGenAttemptRenderer;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.BeeSoundInstance;
import net.minecraft.client.resources.sounds.GuardianAttackSoundInstance;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SnifferSoundInstance;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Position;
import net.minecraft.core.PositionImpl;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Crypt;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class ClientPacketListener implements TickablePacketListener, ClientGamePacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
   private static final Component UNSECURE_SERVER_TOAST_TITLE = Component.translatable("multiplayer.unsecureserver.toast.title");
   private static final Component UNSERURE_SERVER_TOAST = Component.translatable("multiplayer.unsecureserver.toast");
   private static final Component INVALID_PACKET = Component.translatable("multiplayer.disconnect.invalid_packet");
   private static final Component CHAT_VALIDATION_FAILED_ERROR = Component.translatable("multiplayer.disconnect.chat_validation_failed");
   private static final int PENDING_OFFSET_THRESHOLD = 64;
   private final Connection connection;
   private final List<ClientPacketListener.DeferredPacket> deferredPackets = new ArrayList<>();
   @Nullable
   private final ServerData serverData;
   private final GameProfile localGameProfile;
   private final Screen callbackScreen;
   private final Minecraft minecraft;
   private ClientLevel level;
   private ClientLevel.ClientLevelData levelData;
   private final Map<UUID, PlayerInfo> playerInfoMap = Maps.newHashMap();
   private final Set<PlayerInfo> listedPlayers = new ReferenceOpenHashSet<>();
   private final ClientAdvancements advancements;
   private final ClientSuggestionProvider suggestionsProvider;
   private final DebugQueryHandler debugQueryHandler = new DebugQueryHandler(this);
   private int serverChunkRadius = 3;
   private int serverSimulationDistance = 3;
   private final RandomSource random = RandomSource.createThreadSafe();
   private CommandDispatcher<SharedSuggestionProvider> commands = new CommandDispatcher<>();
   private final RecipeManager recipeManager = new RecipeManager();
   private final UUID id = UUID.randomUUID();
   private Set<ResourceKey<Level>> levels;
   private LayeredRegistryAccess<ClientRegistryLayer> registryAccess = ClientRegistryLayer.createRegistryAccess();
   private FeatureFlagSet enabledFeatures = FeatureFlags.DEFAULT_FLAGS;
   private final WorldSessionTelemetryManager telemetryManager;
   @Nullable
   private LocalChatSession chatSession;
   private SignedMessageChain.Encoder signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
   private LastSeenMessagesTracker lastSeenMessages = new LastSeenMessagesTracker(20);
   private MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();

   public ClientPacketListener(
      Minecraft param0, Screen param1, Connection param2, @Nullable ServerData param3, GameProfile param4, WorldSessionTelemetryManager param5
   ) {
      this.minecraft = param0;
      this.callbackScreen = param1;
      this.connection = param2;
      this.serverData = param3;
      this.localGameProfile = param4;
      this.advancements = new ClientAdvancements(param0);
      this.suggestionsProvider = new ClientSuggestionProvider(this, param0);
      this.telemetryManager = param5;
   }

   public ClientSuggestionProvider getSuggestionsProvider() {
      return this.suggestionsProvider;
   }

   public void close() {
      this.level = null;
      this.telemetryManager.onDisconnect();
   }

   public RecipeManager getRecipeManager() {
      return this.recipeManager;
   }

   @Override
   public void handleLogin(ClientboundLoginPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.gameMode = new MultiPlayerGameMode(this.minecraft, this);
      this.registryAccess = this.registryAccess.replaceFrom(ClientRegistryLayer.REMOTE, param0.registryHolder());
      if (!this.connection.isMemoryConnection()) {
         this.registryAccess.compositeAccess().registries().forEach(l_param0 -> l_param0.value().resetTags());
      }

      List<ResourceKey<Level>> var2 = Lists.newArrayList(param0.levels());
      Collections.shuffle(var2);
      this.levels = Sets.newLinkedHashSet(var2);
      ResourceKey<Level> var3 = param0.dimension();
      Holder<DimensionType> var4 = this.registryAccess.compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(param0.dimensionType());
      this.serverChunkRadius = param0.chunkRadius();
      this.serverSimulationDistance = param0.simulationDistance();
      boolean var5 = param0.isDebug();
      boolean var6 = param0.isFlat();
      ClientLevel.ClientLevelData var7 = new ClientLevel.ClientLevelData(Difficulty.NORMAL, param0.hardcore(), var6);
      this.levelData = var7;
      this.level = new ClientLevel(
         this,
         var7,
         var3,
         var4,
         this.serverChunkRadius,
         this.serverSimulationDistance,
         this.minecraft::getProfiler,
         this.minecraft.levelRenderer,
         var5,
         param0.seed()
      );
      this.minecraft.setLevel(this.level);
      if (this.minecraft.player == null) {
         this.minecraft.player = this.minecraft.gameMode.createPlayer(this.level, new StatsCounter(), new ClientRecipeBook());
         this.minecraft.player.setYRot(-180.0F);
         if (this.minecraft.getSingleplayerServer() != null) {
            this.minecraft.getSingleplayerServer().setUUID(this.minecraft.player.getUUID());
         }
      }

      this.minecraft.debugRenderer.clear();
      this.minecraft.player.resetPos();
      int var8 = param0.playerId();
      this.minecraft.player.setId(var8);
      this.level.addPlayer(var8, this.minecraft.player);
      this.minecraft.player.input = new KeyboardInput(this.minecraft.options);
      this.minecraft.gameMode.adjustPlayer(this.minecraft.player);
      this.minecraft.cameraEntity = this.minecraft.player;
      this.minecraft.setScreen(new ReceivingLevelScreen());
      this.minecraft.player.setReducedDebugInfo(param0.reducedDebugInfo());
      this.minecraft.player.setShowDeathScreen(param0.showDeathScreen());
      this.minecraft.player.setLastDeathLocation(param0.lastDeathLocation());
      this.minecraft.gameMode.setLocalMode(param0.gameType(), param0.previousGameType());
      this.minecraft.options.setServerRenderDistance(param0.chunkRadius());
      this.minecraft.options.broadcastOptions();
      this.connection
         .send(
            new ServerboundCustomPayloadPacket(
               ServerboundCustomPayloadPacket.BRAND, new FriendlyByteBuf(Unpooled.buffer()).writeUtf(ClientBrandRetriever.getClientModName())
            )
         );
      this.chatSession = null;
      this.lastSeenMessages = new LastSeenMessagesTracker(20);
      this.messageSignatureCache = MessageSignatureCache.createDefault();
      if (this.connection.isEncrypted()) {
         this.minecraft.getProfileKeyPairManager().prepareKeyPair().thenAcceptAsync(l_param0 -> l_param0.ifPresent(this::setKeyPair), this.minecraft);
      }

      this.telemetryManager.onPlayerInfoReceived(param0.gameType(), param0.hardcore());
   }

   @Override
   public void handleAddEntity(ClientboundAddEntityPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      EntityType<?> var2 = param0.getType();
      Entity var3 = var2.create(this.level);
      if (var3 != null) {
         var3.recreateFromPacket(param0);
         int var4 = param0.getId();
         this.level.putNonPlayerEntity(var4, var3);
         this.postAddEntitySoundInstance(var3);
      } else {
         LOGGER.warn("Skipping Entity with id {}", var2);
      }
   }

   private void postAddEntitySoundInstance(Entity param0) {
      if (param0 instanceof AbstractMinecart) {
         this.minecraft.getSoundManager().play(new MinecartSoundInstance((AbstractMinecart)param0));
      } else if (param0 instanceof Bee) {
         boolean var2 = ((Bee)param0).isAngry();
         BeeSoundInstance var3;
         if (var2) {
            var3 = new BeeAggressiveSoundInstance((Bee)param0);
         } else {
            var3 = new BeeFlyingSoundInstance((Bee)param0);
         }

         this.minecraft.getSoundManager().queueTickingSound(var3);
      }
   }

   @Override
   public void handleAddExperienceOrb(ClientboundAddExperienceOrbPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      double var2 = param0.getX();
      double var4 = param0.getY();
      double var6 = param0.getZ();
      Entity var8 = new ExperienceOrb(this.level, var2, var4, var6, param0.getValue());
      var8.syncPacketPositionCodec(var2, var4, var6);
      var8.setYRot(0.0F);
      var8.setXRot(0.0F);
      var8.setId(param0.getId());
      this.level.putNonPlayerEntity(param0.getId(), var8);
   }

   @Override
   public void handleSetEntityMotion(ClientboundSetEntityMotionPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getId());
      if (var2 != null) {
         var2.lerpMotion(param0.getXa() / 8000.0, param0.getYa() / 8000.0, param0.getZa() / 8000.0);
      }
   }

   @Override
   public void handleSetEntityData(ClientboundSetEntityDataPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.id());
      if (var2 != null) {
         var2.getEntityData().assignValues(param0.packedItems());
      }
   }

   @Override
   public void handleAddPlayer(ClientboundAddPlayerPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      PlayerInfo var2 = this.getPlayerInfo(param0.getPlayerId());
      if (var2 == null) {
         LOGGER.warn("Server attempted to add player prior to sending player info (Player id {})", param0.getPlayerId());
      } else {
         double var3 = param0.getX();
         double var5 = param0.getY();
         double var7 = param0.getZ();
         float var9 = param0.getyRot() * 360 / 256.0F;
         float var10 = param0.getxRot() * 360 / 256.0F;
         int var11 = param0.getEntityId();
         RemotePlayer var12 = new RemotePlayer(this.minecraft.level, var2.getProfile());
         var12.setId(var11);
         var12.syncPacketPositionCodec(var3, var5, var7);
         var12.absMoveTo(var3, var5, var7, var9, var10);
         var12.setOldPosAndRot();
         this.level.addPlayer(var11, var12);
      }
   }

   @Override
   public void handleTeleportEntity(ClientboundTeleportEntityPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getId());
      if (var2 != null) {
         double var3 = param0.getX();
         double var5 = param0.getY();
         double var7 = param0.getZ();
         var2.syncPacketPositionCodec(var3, var5, var7);
         if (!var2.isControlledByLocalInstance()) {
            float var9 = param0.getyRot() * 360 / 256.0F;
            float var10 = param0.getxRot() * 360 / 256.0F;
            var2.lerpTo(var3, var5, var7, var9, var10, 3, true);
            var2.setOnGround(param0.isOnGround());
         }
      }
   }

   @Override
   public void handleSetCarriedItem(ClientboundSetCarriedItemPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      if (Inventory.isHotbarSlot(param0.getSlot())) {
         this.minecraft.player.getInventory().selected = param0.getSlot();
      }
   }

   @Override
   public void handleMoveEntity(ClientboundMoveEntityPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = param0.getEntity(this.level);
      if (var2 != null) {
         if (!var2.isControlledByLocalInstance()) {
            if (param0.hasPosition()) {
               VecDeltaCodec var3 = var2.getPositionCodec();
               Vec3 var4 = var3.decode(param0.getXa(), param0.getYa(), param0.getZa());
               var3.setBase(var4);
               float var5 = param0.hasRotation() ? param0.getyRot() * 360 / 256.0F : var2.getYRot();
               float var6 = param0.hasRotation() ? param0.getxRot() * 360 / 256.0F : var2.getXRot();
               var2.lerpTo(var4.x(), var4.y(), var4.z(), var5, var6, 3, false);
            } else if (param0.hasRotation()) {
               float var3 = param0.getyRot() * 360 / 256.0F;
               float var4 = param0.getxRot() * 360 / 256.0F;
               var2.lerpTo(var2.getX(), var2.getY(), var2.getZ(), var3, var4, 3, false);
            }

            var2.setOnGround(param0.isOnGround());
         }
      }
   }

   @Override
   public void handleRotateMob(ClientboundRotateHeadPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = param0.getEntity(this.level);
      if (var2 != null) {
         float var3 = param0.getYHeadRot() * 360 / 256.0F;
         var2.lerpHeadTo(var3, 3);
      }
   }

   @Override
   public void handleRemoveEntities(ClientboundRemoveEntitiesPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      param0.getEntityIds().forEach(l_param0 -> this.level.removeEntity(l_param0, Entity.RemovalReason.DISCARDED));
   }

   @Override
   public void handleMovePlayer(ClientboundPlayerPositionPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Player var2 = this.minecraft.player;
      Vec3 var3 = var2.getDeltaMovement();
      boolean var4 = param0.getRelativeArguments().contains(RelativeMovement.X);
      boolean var5 = param0.getRelativeArguments().contains(RelativeMovement.Y);
      boolean var6 = param0.getRelativeArguments().contains(RelativeMovement.Z);
      double var7;
      double var9;
      if (var4) {
         var7 = var3.x();
         var9 = var2.getX() + param0.getX();
         var2.xOld = var2.xOld + param0.getX();
         var2.xo = var2.xo + param0.getX();
      } else {
         var7 = 0.0;
         var9 = param0.getX();
         var2.xOld = var9;
         var2.xo = var9;
      }

      double var11;
      double var13;
      if (var5) {
         var11 = var3.y();
         var13 = var2.getY() + param0.getY();
         var2.yOld = var2.yOld + param0.getY();
         var2.yo = var2.yo + param0.getY();
      } else {
         var11 = 0.0;
         var13 = param0.getY();
         var2.yOld = var13;
         var2.yo = var13;
      }

      double var15;
      double var17;
      if (var6) {
         var15 = var3.z();
         var17 = var2.getZ() + param0.getZ();
         var2.zOld = var2.zOld + param0.getZ();
         var2.zo = var2.zo + param0.getZ();
      } else {
         var15 = 0.0;
         var17 = param0.getZ();
         var2.zOld = var17;
         var2.zo = var17;
      }

      var2.setPos(var9, var13, var17);
      var2.setDeltaMovement(var7, var11, var15);
      float var19 = param0.getYRot();
      float var20 = param0.getXRot();
      if (param0.getRelativeArguments().contains(RelativeMovement.X_ROT)) {
         var2.setXRot(var2.getXRot() + var20);
         var2.xRotO += var20;
      } else {
         var2.setXRot(var20);
         var2.xRotO = var20;
      }

      if (param0.getRelativeArguments().contains(RelativeMovement.Y_ROT)) {
         var2.setYRot(var2.getYRot() + var19);
         var2.yRotO += var19;
      } else {
         var2.setYRot(var19);
         var2.yRotO = var19;
      }

      this.connection.send(new ServerboundAcceptTeleportationPacket(param0.getId()));
      this.connection.send(new ServerboundMovePlayerPacket.PosRot(var2.getX(), var2.getY(), var2.getZ(), var2.getYRot(), var2.getXRot(), false));
   }

   @Override
   public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      int var2 = 19 | (param0.shouldSuppressLightUpdates() ? 128 : 0);
      param0.runUpdates((l_param1, l_param2) -> this.level.setServerVerifiedBlockState(l_param1, l_param2, var2));
   }

   @Override
   public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.updateLevelChunk(param0.getX(), param0.getZ(), param0.getChunkData());
      this.queueLightUpdate(param0.getX(), param0.getZ(), param0.getLightData());
   }

   @Override
   public void handleChunksBiomes(ClientboundChunksBiomesPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);

      for (ClientboundChunksBiomesPacket.ChunkBiomeData var3 : param0.chunkBiomeData()) {
         this.level.getChunkSource().replaceBiomes(var3.pos().x, var3.pos().z, var3.getReadBuffer());
      }

      for (ClientboundChunksBiomesPacket.ChunkBiomeData var3 : param0.chunkBiomeData()) {
         this.level.onChunkLoaded(new ChunkPos(var3.pos().x, var3.pos().z));
      }

      for (ClientboundChunksBiomesPacket.ChunkBiomeData var3 : param0.chunkBiomeData()) {
         for (int var4 = -1; var4 <= 1; var4++) {
            for (int var5 = -1; var5 <= 1; var5++) {
               for (int var6 = this.level.getMinSection(); var6 < this.level.getMaxSection(); var6++) {
                  this.minecraft.levelRenderer.setSectionDirty(var3.pos().x + var4, var6, var3.pos().z + var5);
               }
            }
         }
      }
   }

   private void updateLevelChunk(int param0, int param1, ClientboundLevelChunkPacketData param2) {
      this.level
         .getChunkSource()
         .replaceWithPacketData(param0, param1, param2.getReadBuffer(), param2.getHeightmaps(), param2.getBlockEntitiesTagsConsumer(param0, param1));
   }

   private void queueLightUpdate(int param0, int param1, ClientboundLightUpdatePacketData param2) {
      this.level.queueLightUpdate(() -> {
         this.applyLightData(param0, param1, param2);
         LevelChunk l_var4 = this.level.getChunkSource().getChunk(param0, param1, false);
         if (l_var4 != null) {
            this.enableChunkLight(l_var4, param0, param1);
         }
      });
   }

   private void enableChunkLight(LevelChunk param0, int param1, int param2) {
      LevelLightEngine var4 = this.level.getChunkSource().getLightEngine();
      LevelChunkSection[] var5 = param0.getSections();
      ChunkPos var6 = param0.getPos();
      var4.enableLightSources(var6, true);

      for (int var7 = 0; var7 < var5.length; var7++) {
         LevelChunkSection var8 = var5[var7];
         int var9 = this.level.getSectionYFromSectionIndex(var7);
         var4.updateSectionStatus(SectionPos.of(var6, var9), var8.hasOnlyAir());
         this.level.setSectionDirtyWithNeighbors(param1, var9, param2);
      }

      this.level.setLightReady(param1, param2);
   }

   @Override
   public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      int var2 = param0.getX();
      int var3 = param0.getZ();
      ClientChunkCache var4 = this.level.getChunkSource();
      var4.drop(var2, var3);
      this.queueLightUpdate(param0);
   }

   private void queueLightUpdate(ClientboundForgetLevelChunkPacket param0) {
      this.level.queueLightUpdate(() -> {
         LevelLightEngine l_var2 = this.level.getLightEngine();

         for (int l_var3 = this.level.getMinSection(); l_var3 < this.level.getMaxSection(); l_var3++) {
            l_var2.updateSectionStatus(SectionPos.of(param0.getX(), l_var3, param0.getZ()), true);
         }

         l_var2.enableLightSources(new ChunkPos(param0.getX(), param0.getZ()), false);
         this.level.setLightReady(param0.getX(), param0.getZ());
      });
   }

   @Override
   public void handleBlockUpdate(ClientboundBlockUpdatePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.level.setServerVerifiedBlockState(param0.getPos(), param0.getBlockState(), 19);
   }

   @Override
   public void handleDisconnect(ClientboundDisconnectPacket param0) {
      this.connection.disconnect(param0.getReason());
   }

   @Override
   public void onDisconnect(Component param0) {
      this.minecraft.clearLevel();
      this.telemetryManager.onDisconnect();
      if (this.callbackScreen != null) {
         if (this.callbackScreen instanceof RealmsScreen) {
            this.minecraft.setScreen(new DisconnectedRealmsScreen(this.callbackScreen, GENERIC_DISCONNECT_MESSAGE, param0));
         } else {
            this.minecraft.setScreen(new DisconnectedScreen(this.callbackScreen, GENERIC_DISCONNECT_MESSAGE, param0));
         }
      } else {
         this.minecraft.setScreen(new DisconnectedScreen(new JoinMultiplayerScreen(new TitleScreen()), GENERIC_DISCONNECT_MESSAGE, param0));
      }
   }

   public void send(Packet<?> param0) {
      this.connection.send(param0);
   }

   @Override
   public void handleTakeItemEntity(ClientboundTakeItemEntityPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getItemId());
      LivingEntity var3 = (LivingEntity)this.level.getEntity(param0.getPlayerId());
      if (var3 == null) {
         var3 = this.minecraft.player;
      }

      if (var2 != null) {
         if (var2 instanceof ExperienceOrb) {
            this.level
               .playLocalSound(
                  var2.getX(),
                  var2.getY(),
                  var2.getZ(),
                  SoundEvents.EXPERIENCE_ORB_PICKUP,
                  SoundSource.PLAYERS,
                  0.1F,
                  (this.random.nextFloat() - this.random.nextFloat()) * 0.35F + 0.9F,
                  false
               );
         } else {
            this.level
               .playLocalSound(
                  var2.getX(),
                  var2.getY(),
                  var2.getZ(),
                  SoundEvents.ITEM_PICKUP,
                  SoundSource.PLAYERS,
                  0.2F,
                  (this.random.nextFloat() - this.random.nextFloat()) * 1.4F + 2.0F,
                  false
               );
         }

         this.minecraft
            .particleEngine
            .add(new ItemPickupParticle(this.minecraft.getEntityRenderDispatcher(), this.minecraft.renderBuffers(), this.level, var2, var3));
         if (var2 instanceof ItemEntity var4) {
            ItemStack var5 = var4.getItem();
            var5.shrink(param0.getAmount());
            if (var5.isEmpty()) {
               this.level.removeEntity(param0.getItemId(), Entity.RemovalReason.DISCARDED);
            }
         } else if (!(var2 instanceof ExperienceOrb)) {
            this.level.removeEntity(param0.getItemId(), Entity.RemovalReason.DISCARDED);
         }
      }
   }

   @Override
   public void handleSystemChat(ClientboundSystemChatPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.getChatListener().handleSystemMessage(param0.content(), param0.overlay());
   }

   @Override
   public void handlePlayerChat(ClientboundPlayerChatPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Optional<SignedMessageBody> var2 = param0.body().unpack(this.messageSignatureCache);
      Optional<ChatType.Bound> var3 = param0.chatType().resolve(this.registryAccess.compositeAccess());
      if (!var2.isEmpty() && !var3.isEmpty()) {
         UUID var4 = param0.sender();
         PlayerInfo var5 = this.getPlayerInfo(var4);
         if (var5 == null) {
            this.connection.disconnect(CHAT_VALIDATION_FAILED_ERROR);
         } else {
            RemoteChatSession var6 = var5.getChatSession();
            SignedMessageLink var7;
            if (var6 != null) {
               var7 = new SignedMessageLink(param0.index(), var4, var6.sessionId());
            } else {
               var7 = SignedMessageLink.unsigned(var4);
            }

            PlayerChatMessage var8 = new PlayerChatMessage(var7, param0.signature(), var2.get(), param0.unsignedContent(), param0.filterMask());
            if (!var5.getMessageValidator().updateAndValidate(var8)) {
               this.connection.disconnect(CHAT_VALIDATION_FAILED_ERROR);
            } else {
               this.minecraft.getChatListener().handlePlayerChatMessage(var8, var5.getProfile(), var3.get());
               this.messageSignatureCache.push(var8);
            }
         }
      } else {
         this.connection.disconnect(INVALID_PACKET);
      }
   }

   @Override
   public void handleDisguisedChat(ClientboundDisguisedChatPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Optional<ChatType.Bound> var2 = param0.chatType().resolve(this.registryAccess.compositeAccess());
      if (var2.isEmpty()) {
         this.connection.disconnect(INVALID_PACKET);
      } else {
         this.minecraft.getChatListener().handleDisguisedChatMessage(param0.message(), var2.get());
      }
   }

   @Override
   public void handleDeleteChat(ClientboundDeleteChatPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Optional<MessageSignature> var2 = param0.messageSignature().unpack(this.messageSignatureCache);
      if (var2.isEmpty()) {
         this.connection.disconnect(INVALID_PACKET);
      } else {
         this.lastSeenMessages.ignorePending(var2.get());
         if (!this.minecraft.getChatListener().removeFromDelayedMessageQueue(var2.get())) {
            this.minecraft.gui.getChat().deleteMessage(var2.get());
         }
      }
   }

   @Override
   public void handleAnimate(ClientboundAnimatePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getId());
      if (var2 != null) {
         if (param0.getAction() == 0) {
            LivingEntity var3 = (LivingEntity)var2;
            var3.swing(InteractionHand.MAIN_HAND);
         } else if (param0.getAction() == 3) {
            LivingEntity var3 = (LivingEntity)var2;
            var3.swing(InteractionHand.OFF_HAND);
         } else if (param0.getAction() == 2) {
            Player var3 = (Player)var2;
            var3.stopSleepInBed(false, false);
         } else if (param0.getAction() == 4) {
            this.minecraft.particleEngine.createTrackingEmitter(var2, ParticleTypes.CRIT);
         } else if (param0.getAction() == 5) {
            this.minecraft.particleEngine.createTrackingEmitter(var2, ParticleTypes.ENCHANTED_HIT);
         }
      }
   }

   @Override
   public void handleHurtAnimation(ClientboundHurtAnimationPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.id());
      if (var2 != null) {
         var2.animateHurt(param0.yaw());
      }
   }

   @Override
   public void handleSetTime(ClientboundSetTimePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.level.setGameTime(param0.getGameTime());
      this.minecraft.level.setDayTime(param0.getDayTime());
      this.telemetryManager.setTime(param0.getGameTime());
   }

   @Override
   public void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.level.setDefaultSpawnPos(param0.getPos(), param0.getAngle());
      if (this.minecraft.screen instanceof ReceivingLevelScreen var2) {
         var2.loadingPacketsReceived();
      }
   }

   @Override
   public void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getVehicle());
      if (var2 == null) {
         LOGGER.warn("Received passengers for unknown entity");
      } else {
         boolean var3 = var2.hasIndirectPassenger(this.minecraft.player);
         var2.ejectPassengers();

         for (int var7 : param0.getPassengers()) {
            Entity var8 = this.level.getEntity(var7);
            if (var8 != null) {
               var8.startRiding(var2, true);
               if (var8 == this.minecraft.player && !var3) {
                  if (var2 instanceof Boat) {
                     this.minecraft.player.yRotO = var2.getYRot();
                     this.minecraft.player.setYRot(var2.getYRot());
                     this.minecraft.player.setYHeadRot(var2.getYRot());
                  }

                  Component var9 = Component.translatable("mount.onboard", this.minecraft.options.keyShift.getTranslatedKeyMessage());
                  this.minecraft.gui.setOverlayMessage(var9, false);
                  this.minecraft.getNarrator().sayNow(var9);
               }
            }
         }
      }
   }

   @Override
   public void handleEntityLinkPacket(ClientboundSetEntityLinkPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getSourceId());
      if (var2 instanceof Mob) {
         ((Mob)var2).setDelayedLeashHolderId(param0.getDestId());
      }
   }

   private static ItemStack findTotem(Player param0) {
      for (InteractionHand var4 : InteractionHand.values()) {
         ItemStack var5 = param0.getItemInHand(var4);
         if (var5.is(Items.TOTEM_OF_UNDYING)) {
            return var5;
         }
      }

      return new ItemStack(Items.TOTEM_OF_UNDYING);
   }

   @Override
   public void handleEntityEvent(ClientboundEntityEventPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = param0.getEntity(this.level);
      if (var2 != null) {
         switch (param0.getEventId()) {
            case 21:
               this.minecraft.getSoundManager().play(new GuardianAttackSoundInstance((Guardian)var2));
               break;
            case 35:
               int var3 = 40;
               this.minecraft.particleEngine.createTrackingEmitter(var2, ParticleTypes.TOTEM_OF_UNDYING, 30);
               this.level.playLocalSound(var2.getX(), var2.getY(), var2.getZ(), SoundEvents.TOTEM_USE, var2.getSoundSource(), 1.0F, 1.0F, false);
               if (var2 == this.minecraft.player) {
                  this.minecraft.gameRenderer.displayItemActivation(findTotem(this.minecraft.player));
               }
               break;
            case 63:
               this.minecraft.getSoundManager().play(new SnifferSoundInstance((Sniffer)var2));
               break;
            default:
               var2.handleEntityEvent(param0.getEventId());
         }
      }
   }

   @Override
   public void handleDamageEvent(ClientboundDamageEventPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.entityId());
      if (var2 != null) {
         var2.handleDamageEvent(param0.getSource(this.level));
      }
   }

   @Override
   public void handleSetHealth(ClientboundSetHealthPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.player.hurtTo(param0.getHealth());
      this.minecraft.player.getFoodData().setFoodLevel(param0.getFood());
      this.minecraft.player.getFoodData().setSaturation(param0.getSaturation());
   }

   @Override
   public void handleSetExperience(ClientboundSetExperiencePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.player.setExperienceValues(param0.getExperienceProgress(), param0.getTotalExperience(), param0.getExperienceLevel());
   }

   @Override
   public void handleRespawn(ClientboundRespawnPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      ResourceKey<Level> var2 = param0.getDimension();
      Holder<DimensionType> var3 = this.registryAccess.compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(param0.getDimensionType());
      LocalPlayer var4 = this.minecraft.player;
      int var5 = var4.getId();
      if (var2 != var4.level.dimension()) {
         Scoreboard var6 = this.level.getScoreboard();
         Map<String, MapItemSavedData> var7 = this.level.getAllMapData();
         boolean var8 = param0.isDebug();
         boolean var9 = param0.isFlat();
         ClientLevel.ClientLevelData var10 = new ClientLevel.ClientLevelData(this.levelData.getDifficulty(), this.levelData.isHardcore(), var9);
         this.levelData = var10;
         this.level = new ClientLevel(
            this,
            var10,
            var2,
            var3,
            this.serverChunkRadius,
            this.serverSimulationDistance,
            this.minecraft::getProfiler,
            this.minecraft.levelRenderer,
            var8,
            param0.getSeed()
         );
         this.level.setScoreboard(var6);
         this.level.addMapData(var7);
         this.minecraft.setLevel(this.level);
         this.minecraft.setScreen(new ReceivingLevelScreen());
      }

      String var6 = var4.getServerBrand();
      this.minecraft.cameraEntity = null;
      if (var4.hasContainerOpen()) {
         var4.closeContainer();
      }

      LocalPlayer var7 = this.minecraft.gameMode.createPlayer(this.level, var4.getStats(), var4.getRecipeBook(), var4.isShiftKeyDown(), var4.isSprinting());
      var7.setId(var5);
      this.minecraft.player = var7;
      if (var2 != var4.level.dimension()) {
         this.minecraft.getMusicManager().stopPlaying();
      }

      this.minecraft.cameraEntity = var7;
      if (param0.shouldKeep((byte)2)) {
         List<SynchedEntityData.DataValue<?>> var8 = var4.getEntityData().getNonDefaultValues();
         if (var8 != null) {
            var7.getEntityData().assignValues(var8);
         }
      }

      if (param0.shouldKeep((byte)1)) {
         var7.getAttributes().assignValues(var4.getAttributes());
      }

      var7.resetPos();
      var7.setServerBrand(var6);
      this.level.addPlayer(var5, var7);
      var7.setYRot(-180.0F);
      var7.input = new KeyboardInput(this.minecraft.options);
      this.minecraft.gameMode.adjustPlayer(var7);
      var7.setReducedDebugInfo(var4.isReducedDebugInfo());
      var7.setShowDeathScreen(var4.shouldShowDeathScreen());
      var7.setLastDeathLocation(param0.getLastDeathLocation());
      if (this.minecraft.screen instanceof DeathScreen || this.minecraft.screen instanceof DeathScreen.TitleConfirmScreen) {
         this.minecraft.setScreen(null);
      }

      this.minecraft.gameMode.setLocalMode(param0.getPlayerGameType(), param0.getPreviousPlayerGameType());
   }

   @Override
   public void handleExplosion(ClientboundExplodePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Explosion var2 = new Explosion(this.minecraft.level, null, param0.getX(), param0.getY(), param0.getZ(), param0.getPower(), param0.getToBlow());
      var2.finalizeExplosion(true);
      this.minecraft
         .player
         .setDeltaMovement(this.minecraft.player.getDeltaMovement().add(param0.getKnockbackX(), param0.getKnockbackY(), param0.getKnockbackZ()));
   }

   @Override
   public void handleHorseScreenOpen(ClientboundHorseScreenOpenPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getEntityId());
      if (var2 instanceof AbstractHorse) {
         LocalPlayer var3 = this.minecraft.player;
         AbstractHorse var4 = (AbstractHorse)var2;
         SimpleContainer var5 = new SimpleContainer(param0.getSize());
         HorseInventoryMenu var6 = new HorseInventoryMenu(param0.getContainerId(), var3.getInventory(), var5, var4);
         var3.containerMenu = var6;
         this.minecraft.setScreen(new HorseInventoryScreen(var6, var3.getInventory(), var4));
      }
   }

   @Override
   public void handleOpenScreen(ClientboundOpenScreenPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      MenuScreens.create(param0.getType(), this.minecraft, param0.getContainerId(), param0.getTitle());
   }

   @Override
   public void handleContainerSetSlot(ClientboundContainerSetSlotPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Player var2 = this.minecraft.player;
      ItemStack var3 = param0.getItem();
      int var4 = param0.getSlot();
      this.minecraft.getTutorial().onGetItem(var3);
      if (param0.getContainerId() == -1) {
         if (!(this.minecraft.screen instanceof CreativeModeInventoryScreen)) {
            var2.containerMenu.setCarried(var3);
         }
      } else if (param0.getContainerId() == -2) {
         var2.getInventory().setItem(var4, var3);
      } else {
         boolean var5 = false;
         if (this.minecraft.screen instanceof CreativeModeInventoryScreen var6) {
            var5 = !var6.isInventoryOpen();
         }

         if (param0.getContainerId() == 0 && InventoryMenu.isHotbarSlot(var4)) {
            if (!var3.isEmpty()) {
               ItemStack var6 = var2.inventoryMenu.getSlot(var4).getItem();
               if (var6.isEmpty() || var6.getCount() < var3.getCount()) {
                  var3.setPopTime(5);
               }
            }

            var2.inventoryMenu.setItem(var4, param0.getStateId(), var3);
         } else if (param0.getContainerId() == var2.containerMenu.containerId && (param0.getContainerId() != 0 || !var5)) {
            var2.containerMenu.setItem(var4, param0.getStateId(), var3);
         }
      }
   }

   @Override
   public void handleContainerContent(ClientboundContainerSetContentPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Player var2 = this.minecraft.player;
      if (param0.getContainerId() == 0) {
         var2.inventoryMenu.initializeContents(param0.getStateId(), param0.getItems(), param0.getCarriedItem());
      } else if (param0.getContainerId() == var2.containerMenu.containerId) {
         var2.containerMenu.initializeContents(param0.getStateId(), param0.getItems(), param0.getCarriedItem());
      }
   }

   @Override
   public void handleOpenSignEditor(ClientboundOpenSignEditorPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      BlockPos var2 = param0.getPos();
      BlockEntity var3 = this.level.getBlockEntity(var2);
      if (!(var3 instanceof SignBlockEntity)) {
         BlockState var4 = this.level.getBlockState(var2);
         var3 = new SignBlockEntity(var2, var4);
         var3.setLevel(this.level);
      }

      this.minecraft.player.openTextEdit((SignBlockEntity)var3);
   }

   @Override
   public void handleBlockEntityData(ClientboundBlockEntityDataPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      BlockPos var2 = param0.getPos();
      this.minecraft.level.getBlockEntity(var2, param0.getType()).ifPresent(l_param1 -> {
         CompoundTag l_var3 = param0.getTag();
         if (l_var3 != null) {
            l_param1.load(l_var3);
         }

         if (l_param1 instanceof CommandBlockEntity && this.minecraft.screen instanceof CommandBlockEditScreen) {
            ((CommandBlockEditScreen)this.minecraft.screen).updateGui();
         }
      });
   }

   @Override
   public void handleContainerSetData(ClientboundContainerSetDataPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Player var2 = this.minecraft.player;
      if (var2.containerMenu != null && var2.containerMenu.containerId == param0.getContainerId()) {
         var2.containerMenu.setData(param0.getId(), param0.getValue());
      }
   }

   @Override
   public void handleSetEquipment(ClientboundSetEquipmentPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getEntity());
      if (var2 != null) {
         param0.getSlots().forEach(l_param1 -> var2.setItemSlot(l_param1.getFirst(), l_param1.getSecond()));
      }
   }

   @Override
   public void handleContainerClose(ClientboundContainerClosePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.player.clientSideCloseContainer();
   }

   @Override
   public void handleBlockEvent(ClientboundBlockEventPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.level.blockEvent(param0.getPos(), param0.getBlock(), param0.getB0(), param0.getB1());
   }

   @Override
   public void handleBlockDestruction(ClientboundBlockDestructionPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.level.destroyBlockProgress(param0.getId(), param0.getPos(), param0.getProgress());
   }

   @Override
   public void handleGameEvent(ClientboundGameEventPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Player var2 = this.minecraft.player;
      ClientboundGameEventPacket.Type var3 = param0.getEvent();
      float var4 = param0.getParam();
      int var5 = Mth.floor(var4 + 0.5F);
      if (var3 == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) {
         var2.displayClientMessage(Component.translatable("block.minecraft.spawn.not_valid"), false);
      } else if (var3 == ClientboundGameEventPacket.START_RAINING) {
         this.level.getLevelData().setRaining(true);
         this.level.setRainLevel(0.0F);
      } else if (var3 == ClientboundGameEventPacket.STOP_RAINING) {
         this.level.getLevelData().setRaining(false);
         this.level.setRainLevel(1.0F);
      } else if (var3 == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
         this.minecraft.gameMode.setLocalMode(GameType.byId(var5));
      } else if (var3 == ClientboundGameEventPacket.WIN_GAME) {
         if (var5 == 0) {
            this.minecraft.player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            this.minecraft.setScreen(new ReceivingLevelScreen());
         } else if (var5 == 1) {
            this.minecraft.setScreen(new WinScreen(true, () -> {
               this.minecraft.player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
               this.minecraft.setScreen(null);
            }));
         }
      } else if (var3 == ClientboundGameEventPacket.DEMO_EVENT) {
         Options var6 = this.minecraft.options;
         if (var4 == 0.0F) {
            this.minecraft.setScreen(new DemoIntroScreen());
         } else if (var4 == 101.0F) {
            this.minecraft
               .gui
               .getChat()
               .addMessage(
                  Component.translatable(
                     "demo.help.movement",
                     var6.keyUp.getTranslatedKeyMessage(),
                     var6.keyLeft.getTranslatedKeyMessage(),
                     var6.keyDown.getTranslatedKeyMessage(),
                     var6.keyRight.getTranslatedKeyMessage()
                  )
               );
         } else if (var4 == 102.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.jump", var6.keyJump.getTranslatedKeyMessage()));
         } else if (var4 == 103.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.inventory", var6.keyInventory.getTranslatedKeyMessage()));
         } else if (var4 == 104.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.day.6", var6.keyScreenshot.getTranslatedKeyMessage()));
         }
      } else if (var3 == ClientboundGameEventPacket.ARROW_HIT_PLAYER) {
         this.level.playSound(var2, var2.getX(), var2.getEyeY(), var2.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18F, 0.45F);
      } else if (var3 == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
         this.level.setRainLevel(var4);
      } else if (var3 == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
         this.level.setThunderLevel(var4);
      } else if (var3 == ClientboundGameEventPacket.PUFFER_FISH_STING) {
         this.level.playSound(var2, var2.getX(), var2.getY(), var2.getZ(), SoundEvents.PUFFER_FISH_STING, SoundSource.NEUTRAL, 1.0F, 1.0F);
      } else if (var3 == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
         this.level.addParticle(ParticleTypes.ELDER_GUARDIAN, var2.getX(), var2.getY(), var2.getZ(), 0.0, 0.0, 0.0);
         if (var5 == 1) {
            this.level.playSound(var2, var2.getX(), var2.getY(), var2.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.0F);
         }
      } else if (var3 == ClientboundGameEventPacket.IMMEDIATE_RESPAWN) {
         this.minecraft.player.setShowDeathScreen(var4 == 0.0F);
      }
   }

   @Override
   public void handleMapItemData(ClientboundMapItemDataPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      MapRenderer var2 = this.minecraft.gameRenderer.getMapRenderer();
      int var3 = param0.getMapId();
      String var4 = MapItem.makeKey(var3);
      MapItemSavedData var5 = this.minecraft.level.getMapData(var4);
      if (var5 == null) {
         var5 = MapItemSavedData.createForClient(param0.getScale(), param0.isLocked(), this.minecraft.level.dimension());
         this.minecraft.level.overrideMapData(var4, var5);
      }

      param0.applyToMap(var5);
      var2.update(var3, var5);
   }

   @Override
   public void handleLevelEvent(ClientboundLevelEventPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      if (param0.isGlobalEvent()) {
         this.minecraft.level.globalLevelEvent(param0.getType(), param0.getPos(), param0.getData());
      } else {
         this.minecraft.level.levelEvent(param0.getType(), param0.getPos(), param0.getData());
      }
   }

   @Override
   public void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.advancements.update(param0);
   }

   @Override
   public void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      ResourceLocation var2 = param0.getTab();
      if (var2 == null) {
         this.advancements.setSelectedTab(null, false);
      } else {
         Advancement var3 = this.advancements.getAdvancements().get(var2);
         this.advancements.setSelectedTab(var3, false);
      }
   }

   @Override
   public void handleCommands(ClientboundCommandsPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.commands = new CommandDispatcher<>(param0.getRoot(CommandBuildContext.simple(this.registryAccess.compositeAccess(), this.enabledFeatures)));
   }

   @Override
   public void handleStopSoundEvent(ClientboundStopSoundPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.getSoundManager().stop(param0.getName(), param0.getSource());
   }

   @Override
   public void handleCommandSuggestions(ClientboundCommandSuggestionsPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.suggestionsProvider.completeCustomSuggestions(param0.getId(), param0.getSuggestions());
   }

   @Override
   public void handleUpdateRecipes(ClientboundUpdateRecipesPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.recipeManager.replaceRecipes(param0.getRecipes());
      ClientRecipeBook var2 = this.minecraft.player.getRecipeBook();
      var2.setupCollections(this.recipeManager.getRecipes(), this.minecraft.level.registryAccess());
      this.minecraft.populateSearchTree(SearchRegistry.RECIPE_COLLECTIONS, var2.getCollections());
   }

   @Override
   public void handleLookAt(ClientboundPlayerLookAtPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Vec3 var2 = param0.getPosition(this.level);
      if (var2 != null) {
         this.minecraft.player.lookAt(param0.getFromAnchor(), var2);
      }
   }

   @Override
   public void handleTagQueryPacket(ClientboundTagQueryPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      if (!this.debugQueryHandler.handleResponse(param0.getTransactionId(), param0.getTag())) {
         LOGGER.debug("Got unhandled response to tag query {}", param0.getTransactionId());
      }
   }

   @Override
   public void handleAwardStats(ClientboundAwardStatsPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);

      for (Entry<Stat<?>, Integer> var3 : param0.getStats().entrySet()) {
         Stat<?> var4 = var3.getKey();
         int var5 = var3.getValue();
         this.minecraft.player.getStats().setValue(this.minecraft.player, var4, var5);
      }

      if (this.minecraft.screen instanceof StatsUpdateListener) {
         ((StatsUpdateListener)this.minecraft.screen).onStatsUpdated();
      }
   }

   @Override
   public void handleAddOrRemoveRecipes(ClientboundRecipePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      ClientRecipeBook var2 = this.minecraft.player.getRecipeBook();
      var2.setBookSettings(param0.getBookSettings());
      ClientboundRecipePacket.State var3 = param0.getState();
      switch (var3) {
         case REMOVE:
            for (ResourceLocation var5 : param0.getRecipes()) {
               this.recipeManager.byKey(var5).ifPresent(var2::remove);
            }
            break;
         case INIT:
            for (ResourceLocation var5 : param0.getRecipes()) {
               this.recipeManager.byKey(var5).ifPresent(var2::add);
            }

            for (ResourceLocation var5 : param0.getHighlights()) {
               this.recipeManager.byKey(var5).ifPresent(var2::addHighlight);
            }
            break;
         case ADD:
            for (ResourceLocation var5 : param0.getRecipes()) {
               this.recipeManager.byKey(var5).ifPresent(l_param1 -> {
                  var2.add((Recipe<?>)l_param1);
                  var2.addHighlight((Recipe<?>)l_param1);
                  if (l_param1.showNotification()) {
                     RecipeToast.addOrUpdate(this.minecraft.getToasts(), (Recipe<?>)l_param1);
                  }
               });
            }
      }

      var2.getCollections().forEach(l_param1 -> l_param1.updateKnownRecipes(var2));
      if (this.minecraft.screen instanceof RecipeUpdateListener) {
         ((RecipeUpdateListener)this.minecraft.screen).recipesUpdated();
      }
   }

   @Override
   public void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getEntityId());
      if (var2 instanceof LivingEntity) {
         MobEffect var3 = param0.getEffect();
         if (var3 != null) {
            MobEffectInstance var4 = new MobEffectInstance(
               var3,
               param0.getEffectDurationTicks(),
               param0.getEffectAmplifier(),
               param0.isEffectAmbient(),
               param0.isEffectVisible(),
               param0.effectShowsIcon(),
               null,
               Optional.ofNullable(param0.getFactorData())
            );
            ((LivingEntity)var2).forceAddEffect(var4, null);
         }
      }
   }

   @Override
   public void handleUpdateTags(ClientboundUpdateTagsPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      param0.getTags().forEach(this::updateTagsForRegistry);
      if (!this.connection.isMemoryConnection()) {
         Blocks.rebuildCache();
      }

      CreativeModeTabs.searchTab().rebuildSearchTree();
   }

   @Override
   public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(param0.features());
   }

   private <T> void updateTagsForRegistry(ResourceKey<? extends Registry<? extends T>> param0, TagNetworkSerialization.NetworkPayload param1) {
      if (!param1.isEmpty()) {
         Registry<T> var3 = this.registryAccess.compositeAccess().registry(param0).orElseThrow(() -> new IllegalStateException("Unknown registry " + param0));
         ResourceKey<? extends Registry<T>> var4 = (ResourceKey<? extends Registry<T>>)param0;
         Map<TagKey<T>, List<Holder<T>>> var5 = new HashMap<>();
         TagNetworkSerialization.deserializeTagsFromNetwork(var4, var3, param1, var5::put);
         var3.bindTags(var5);
      }
   }

   @Override
   public void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket param0) {
   }

   @Override
   public void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket param0) {
   }

   @Override
   public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getPlayerId());
      if (var2 == this.minecraft.player) {
         if (this.minecraft.player.shouldShowDeathScreen()) {
            this.minecraft.setScreen(new DeathScreen(param0.getMessage(), this.level.getLevelData().isHardcore()));
         } else {
            this.minecraft.player.respawn();
         }
      }
   }

   @Override
   public void handleChangeDifficulty(ClientboundChangeDifficultyPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.levelData.setDifficulty(param0.getDifficulty());
      this.levelData.setDifficultyLocked(param0.isLocked());
   }

   @Override
   public void handleSetCamera(ClientboundSetCameraPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = param0.getEntity(this.level);
      if (var2 != null) {
         this.minecraft.setCameraEntity(var2);
      }
   }

   @Override
   public void handleInitializeBorder(ClientboundInitializeBorderPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      WorldBorder var2 = this.level.getWorldBorder();
      var2.setCenter(param0.getNewCenterX(), param0.getNewCenterZ());
      long var3 = param0.getLerpTime();
      if (var3 > 0L) {
         var2.lerpSizeBetween(param0.getOldSize(), param0.getNewSize(), var3);
      } else {
         var2.setSize(param0.getNewSize());
      }

      var2.setAbsoluteMaxSize(param0.getNewAbsoluteMaxSize());
      var2.setWarningBlocks(param0.getWarningBlocks());
      var2.setWarningTime(param0.getWarningTime());
   }

   @Override
   public void handleSetBorderCenter(ClientboundSetBorderCenterPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.level.getWorldBorder().setCenter(param0.getNewCenterX(), param0.getNewCenterZ());
   }

   @Override
   public void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.level.getWorldBorder().lerpSizeBetween(param0.getOldSize(), param0.getNewSize(), param0.getLerpTime());
   }

   @Override
   public void handleSetBorderSize(ClientboundSetBorderSizePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.level.getWorldBorder().setSize(param0.getSize());
   }

   @Override
   public void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.level.getWorldBorder().setWarningBlocks(param0.getWarningBlocks());
   }

   @Override
   public void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.level.getWorldBorder().setWarningTime(param0.getWarningDelay());
   }

   @Override
   public void handleTitlesClear(ClientboundClearTitlesPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.gui.clear();
      if (param0.shouldResetTimes()) {
         this.minecraft.gui.resetTitleTimes();
      }
   }

   @Override
   public void handleServerData(ClientboundServerDataPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      if (this.serverData != null) {
         this.serverData.motd = param0.getMotd();
         param0.getIconBytes().ifPresent(this.serverData::setIconBytes);
         this.serverData.setEnforcesSecureChat(param0.enforcesSecureChat());
         ServerList.saveSingleServer(this.serverData);
         if (!param0.enforcesSecureChat()) {
            SystemToast var2 = SystemToast.multiline(
               this.minecraft, SystemToast.SystemToastIds.UNSECURE_SERVER_WARNING, UNSECURE_SERVER_TOAST_TITLE, UNSERURE_SERVER_TOAST
            );
            this.minecraft.getToasts().addToast(var2);
         }
      }
   }

   @Override
   public void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.suggestionsProvider.modifyCustomCompletions(param0.action(), param0.entries());
   }

   @Override
   public void setActionBarText(ClientboundSetActionBarTextPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.gui.setOverlayMessage(param0.getText(), false);
   }

   @Override
   public void setTitleText(ClientboundSetTitleTextPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.gui.setTitle(param0.getText());
   }

   @Override
   public void setSubtitleText(ClientboundSetSubtitleTextPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.gui.setSubtitle(param0.getText());
   }

   @Override
   public void setTitlesAnimation(ClientboundSetTitlesAnimationPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.gui.setTimes(param0.getFadeIn(), param0.getStay(), param0.getFadeOut());
   }

   @Override
   public void handleTabListCustomisation(ClientboundTabListPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.gui.getTabList().setHeader(param0.getHeader().getString().isEmpty() ? null : param0.getHeader());
      this.minecraft.gui.getTabList().setFooter(param0.getFooter().getString().isEmpty() ? null : param0.getFooter());
   }

   @Override
   public void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = param0.getEntity(this.level);
      if (var2 instanceof LivingEntity) {
         ((LivingEntity)var2).removeEffectNoUpdate(param0.getEffect());
      }
   }

   @Override
   public void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);

      for (UUID var3 : param0.profileIds()) {
         this.minecraft.getPlayerSocialManager().removePlayer(var3);
         PlayerInfo var4 = this.playerInfoMap.remove(var3);
         if (var4 != null) {
            this.listedPlayers.remove(var4);
         }
      }
   }

   @Override
   public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);

      for (ClientboundPlayerInfoUpdatePacket.Entry var3 : param0.newEntries()) {
         PlayerInfo var4 = new PlayerInfo(var3.profile(), this.enforcesSecureChat());
         if (this.playerInfoMap.putIfAbsent(var3.profileId(), var4) == null) {
            this.minecraft.getPlayerSocialManager().addPlayer(var4);
         }
      }

      for (ClientboundPlayerInfoUpdatePacket.Entry var3 : param0.entries()) {
         PlayerInfo var4 = this.playerInfoMap.get(var3.profileId());
         if (var4 == null) {
            LOGGER.warn("Ignoring player info update for unknown player {}", var3.profileId());
         } else {
            for (ClientboundPlayerInfoUpdatePacket.Action var6 : param0.actions()) {
               this.applyPlayerInfoUpdate(var6, var3, var4);
            }
         }
      }
   }

   private void applyPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket.Action param0, ClientboundPlayerInfoUpdatePacket.Entry param1, PlayerInfo param2) {
      switch (param0) {
         case INITIALIZE_CHAT:
            this.initializeChatSession(param1, param2);
            break;
         case UPDATE_GAME_MODE:
            param2.setGameMode(param1.gameMode());
            break;
         case UPDATE_LISTED:
            if (param1.listed()) {
               this.listedPlayers.add(param2);
            } else {
               this.listedPlayers.remove(param2);
            }
            break;
         case UPDATE_LATENCY:
            param2.setLatency(param1.latency());
            break;
         case UPDATE_DISPLAY_NAME:
            param2.setTabListDisplayName(param1.displayName());
      }
   }

   private void initializeChatSession(ClientboundPlayerInfoUpdatePacket.Entry param0, PlayerInfo param1) {
      GameProfile var3 = param1.getProfile();
      RemoteChatSession.Data var4 = param0.chatSession();
      if (var4 != null) {
         try {
            RemoteChatSession var5 = var4.validate(var3, this.minecraft.getServiceSignatureValidator(), ProfilePublicKey.EXPIRY_GRACE_PERIOD);
            param1.setChatSession(var5);
         } catch (ProfilePublicKey.ValidationException var5) {
            LOGGER.error("Failed to validate profile key for player: '{}'", var3.getName(), var5);
            this.connection.disconnect(var5.getComponent());
         }
      } else {
         param1.clearChatSession(this.enforcesSecureChat());
      }
   }

   private boolean enforcesSecureChat() {
      return this.serverData != null && this.serverData.enforcesSecureChat();
   }

   @Override
   public void handleKeepAlive(ClientboundKeepAlivePacket param0) {
      this.sendWhen(new ServerboundKeepAlivePacket(param0.getId()), () -> !RenderSystem.isFrozenAtPollEvents(), Duration.ofMinutes(1L));
   }

   private void sendWhen(Packet<ServerGamePacketListener> param0, BooleanSupplier param1, Duration param2) {
      if (param1.getAsBoolean()) {
         this.send(param0);
      } else {
         this.deferredPackets.add(new ClientPacketListener.DeferredPacket(param0, param1, Util.getMillis() + param2.toMillis()));
      }
   }

   private void sendDeferredPackets() {
      Iterator<ClientPacketListener.DeferredPacket> var1 = this.deferredPackets.iterator();

      while (var1.hasNext()) {
         ClientPacketListener.DeferredPacket var2 = var1.next();
         if (var2.sendCondition().getAsBoolean()) {
            this.send(var2.packet);
            var1.remove();
         } else if (var2.expirationTime() <= Util.getMillis()) {
            var1.remove();
         }
      }
   }

   @Override
   public void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Player var2 = this.minecraft.player;
      var2.getAbilities().flying = param0.isFlying();
      var2.getAbilities().instabuild = param0.canInstabuild();
      var2.getAbilities().invulnerable = param0.isInvulnerable();
      var2.getAbilities().mayfly = param0.canFly();
      var2.getAbilities().setFlyingSpeed(param0.getFlyingSpeed());
      var2.getAbilities().setWalkingSpeed(param0.getWalkingSpeed());
   }

   @Override
   public void handleSoundEvent(ClientboundSoundPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft
         .level
         .playSeededSound(
            this.minecraft.player,
            param0.getX(),
            param0.getY(),
            param0.getZ(),
            param0.getSound(),
            param0.getSource(),
            param0.getVolume(),
            param0.getPitch(),
            param0.getSeed()
         );
   }

   @Override
   public void handleSoundEntityEvent(ClientboundSoundEntityPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getId());
      if (var2 != null) {
         this.minecraft
            .level
            .playSeededSound(this.minecraft.player, var2, param0.getSound(), param0.getSource(), param0.getVolume(), param0.getPitch(), param0.getSeed());
      }
   }

   @Override
   public void handleResourcePack(ClientboundResourcePackPacket param0) {
      URL var2 = parseResourcePackUrl(param0.getUrl());
      if (var2 == null) {
         this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
      } else {
         String var3 = param0.getHash();
         boolean var4 = param0.isRequired();
         if (this.serverData != null && this.serverData.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED) {
            this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
            this.downloadCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(var2, var3, true));
         } else if (this.serverData != null
            && this.serverData.getResourcePackStatus() != ServerData.ServerPackStatus.PROMPT
            && (!var4 || this.serverData.getResourcePackStatus() != ServerData.ServerPackStatus.DISABLED)) {
            this.send(ServerboundResourcePackPacket.Action.DECLINED);
            if (var4) {
               this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
            }
         } else {
            this.minecraft
               .execute(
                  () -> this.minecraft
                     .setScreen(
                        new ConfirmScreen(
                           l2_param3 -> {
                              this.minecraft.setScreen(null);
                              if (l2_param3) {
                                 if (this.serverData != null) {
                                    this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                                 }

                                 this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
                                 this.downloadCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(var2, var3, true));
                              } else {
                                 this.send(ServerboundResourcePackPacket.Action.DECLINED);
                                 if (var4) {
                                    this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                                 } else if (this.serverData != null) {
                                    this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
                                 }
                              }

                              if (this.serverData != null) {
                                 ServerList.saveSingleServer(this.serverData);
                              }
                           },
                           var4 ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"),
                           preparePackPrompt(
                              var4
                                 ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                                 : Component.translatable("multiplayer.texturePrompt.line2"),
                              param0.getPrompt()
                           ),
                           var4 ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES,
                           var4 ? Component.translatable("menu.disconnect") : CommonComponents.GUI_NO
                        )
                     )
               );
         }
      }
   }

   private static Component preparePackPrompt(Component param0, @Nullable Component param1) {
      return param1 == null ? param0 : Component.translatable("multiplayer.texturePrompt.serverPrompt", param0, param1);
   }

   @Nullable
   private static URL parseResourcePackUrl(String param0) {
      try {
         URL var1 = new URL(param0);
         String var2 = var1.getProtocol();
         return !"http".equals(var2) && !"https".equals(var2) ? null : var1;
      } catch (MalformedURLException var1) {
         return null;
      }
   }

   private void downloadCallback(CompletableFuture<?> param0) {
      param0.thenRun(() -> this.send(ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED)).exceptionally(l_param0 -> {
         this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
         return null;
      });
   }

   private void send(ServerboundResourcePackPacket.Action param0) {
      this.connection.send(new ServerboundResourcePackPacket(param0));
   }

   @Override
   public void handleBossUpdate(ClientboundBossEventPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.minecraft.gui.getBossOverlay().update(param0);
   }

   @Override
   public void handleItemCooldown(ClientboundCooldownPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      if (param0.getDuration() == 0) {
         this.minecraft.player.getCooldowns().removeCooldown(param0.getItem());
      } else {
         this.minecraft.player.getCooldowns().addCooldown(param0.getItem(), param0.getDuration());
      }
   }

   @Override
   public void handleMoveVehicle(ClientboundMoveVehiclePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.minecraft.player.getRootVehicle();
      if (var2 != this.minecraft.player && var2.isControlledByLocalInstance()) {
         var2.absMoveTo(param0.getX(), param0.getY(), param0.getZ(), param0.getYRot(), param0.getXRot());
         this.connection.send(new ServerboundMoveVehiclePacket(var2));
      }
   }

   @Override
   public void handleOpenBook(ClientboundOpenBookPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      ItemStack var2 = this.minecraft.player.getItemInHand(param0.getHand());
      if (var2.is(Items.WRITTEN_BOOK)) {
         this.minecraft.setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(var2)));
      }
   }

   @Override
   public void handleCustomPayload(ClientboundCustomPayloadPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      ResourceLocation var2 = param0.getIdentifier();
      FriendlyByteBuf var3 = null;

      try {
         var3 = param0.getData();
         if (ClientboundCustomPayloadPacket.BRAND.equals(var2)) {
            String var4 = var3.readUtf();
            this.minecraft.player.setServerBrand(var4);
            this.telemetryManager.onServerBrandReceived(var4);
         } else if (ClientboundCustomPayloadPacket.DEBUG_PATHFINDING_PACKET.equals(var2)) {
            int var4 = var3.readInt();
            float var5 = var3.readFloat();
            Path var6 = Path.createFromStream(var3);
            this.minecraft.debugRenderer.pathfindingRenderer.addPath(var4, var6, var5);
         } else if (ClientboundCustomPayloadPacket.DEBUG_NEIGHBORSUPDATE_PACKET.equals(var2)) {
            long var4 = var3.readVarLong();
            BlockPos var6 = var3.readBlockPos();
            ((NeighborsUpdateRenderer)this.minecraft.debugRenderer.neighborsUpdateRenderer).addUpdate(var4, var6);
         } else if (ClientboundCustomPayloadPacket.DEBUG_STRUCTURES_PACKET.equals(var2)) {
            DimensionType var4 = this.registryAccess.compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE).get(var3.readResourceLocation());
            BoundingBox var5 = new BoundingBox(var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt());
            int var6 = var3.readInt();
            List<BoundingBox> var7 = Lists.newArrayList();
            List<Boolean> var8 = Lists.newArrayList();

            for (int var9 = 0; var9 < var6; var9++) {
               var7.add(new BoundingBox(var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt()));
               var8.add(var3.readBoolean());
            }

            this.minecraft.debugRenderer.structureRenderer.addBoundingBox(var5, var7, var8, var4);
         } else if (ClientboundCustomPayloadPacket.DEBUG_WORLDGENATTEMPT_PACKET.equals(var2)) {
            ((WorldGenAttemptRenderer)this.minecraft.debugRenderer.worldGenAttemptRenderer)
               .addPos(var3.readBlockPos(), var3.readFloat(), var3.readFloat(), var3.readFloat(), var3.readFloat(), var3.readFloat());
         } else if (ClientboundCustomPayloadPacket.DEBUG_VILLAGE_SECTIONS.equals(var2)) {
            int var4 = var3.readInt();

            for (int var5 = 0; var5 < var4; var5++) {
               this.minecraft.debugRenderer.villageSectionsDebugRenderer.setVillageSection(var3.readSectionPos());
            }

            int var5 = var3.readInt();

            for (int var6 = 0; var6 < var5; var6++) {
               this.minecraft.debugRenderer.villageSectionsDebugRenderer.setNotVillageSection(var3.readSectionPos());
            }
         } else if (ClientboundCustomPayloadPacket.DEBUG_POI_ADDED_PACKET.equals(var2)) {
            BlockPos var4 = var3.readBlockPos();
            String var5 = var3.readUtf();
            int var6 = var3.readInt();
            BrainDebugRenderer.PoiInfo var7 = new BrainDebugRenderer.PoiInfo(var4, var5, var6);
            this.minecraft.debugRenderer.brainDebugRenderer.addPoi(var7);
         } else if (ClientboundCustomPayloadPacket.DEBUG_POI_REMOVED_PACKET.equals(var2)) {
            BlockPos var4 = var3.readBlockPos();
            this.minecraft.debugRenderer.brainDebugRenderer.removePoi(var4);
         } else if (ClientboundCustomPayloadPacket.DEBUG_POI_TICKET_COUNT_PACKET.equals(var2)) {
            BlockPos var4 = var3.readBlockPos();
            int var5 = var3.readInt();
            this.minecraft.debugRenderer.brainDebugRenderer.setFreeTicketCount(var4, var5);
         } else if (ClientboundCustomPayloadPacket.DEBUG_GOAL_SELECTOR.equals(var2)) {
            BlockPos var4 = var3.readBlockPos();
            int var5 = var3.readInt();
            int var6 = var3.readInt();
            List<GoalSelectorDebugRenderer.DebugGoal> var7 = Lists.newArrayList();

            for (int var8 = 0; var8 < var6; var8++) {
               int var9 = var3.readInt();
               boolean var10 = var3.readBoolean();
               String var11 = var3.readUtf(255);
               var7.add(new GoalSelectorDebugRenderer.DebugGoal(var4, var9, var11, var10));
            }

            this.minecraft.debugRenderer.goalSelectorRenderer.addGoalSelector(var5, var7);
         } else if (ClientboundCustomPayloadPacket.DEBUG_RAIDS.equals(var2)) {
            int var4 = var3.readInt();
            Collection<BlockPos> var5 = Lists.newArrayList();

            for (int var6 = 0; var6 < var4; var6++) {
               var5.add(var3.readBlockPos());
            }

            this.minecraft.debugRenderer.raidDebugRenderer.setRaidCenters(var5);
         } else if (ClientboundCustomPayloadPacket.DEBUG_BRAIN.equals(var2)) {
            double var4 = var3.readDouble();
            double var6 = var3.readDouble();
            double var8 = var3.readDouble();
            Position var10 = new PositionImpl(var4, var6, var8);
            UUID var11 = var3.readUUID();
            int var12 = var3.readInt();
            String var13 = var3.readUtf();
            String var14 = var3.readUtf();
            int var15 = var3.readInt();
            float var16 = var3.readFloat();
            float var17 = var3.readFloat();
            String var18 = var3.readUtf();
            Path var19 = var3.readNullable(Path::createFromStream);
            boolean var20 = var3.readBoolean();
            int var21 = var3.readInt();
            BrainDebugRenderer.BrainDump var22 = new BrainDebugRenderer.BrainDump(
               var11, var12, var13, var14, var15, var16, var17, var10, var18, var19, var20, var21
            );
            int var23 = var3.readVarInt();

            for (int var24 = 0; var24 < var23; var24++) {
               String var25 = var3.readUtf();
               var22.activities.add(var25);
            }

            int var24 = var3.readVarInt();

            for (int var25 = 0; var25 < var24; var25++) {
               String var26 = var3.readUtf();
               var22.behaviors.add(var26);
            }

            int var25 = var3.readVarInt();

            for (int var26 = 0; var26 < var25; var26++) {
               String var27 = var3.readUtf();
               var22.memories.add(var27);
            }

            int var26 = var3.readVarInt();

            for (int var27 = 0; var27 < var26; var27++) {
               BlockPos var28 = var3.readBlockPos();
               var22.pois.add(var28);
            }

            int var27 = var3.readVarInt();

            for (int var28 = 0; var28 < var27; var28++) {
               BlockPos var29 = var3.readBlockPos();
               var22.potentialPois.add(var29);
            }

            int var28 = var3.readVarInt();

            for (int var29 = 0; var29 < var28; var29++) {
               String var30 = var3.readUtf();
               var22.gossips.add(var30);
            }

            this.minecraft.debugRenderer.brainDebugRenderer.addOrUpdateBrainDump(var22);
         } else if (ClientboundCustomPayloadPacket.DEBUG_BEE.equals(var2)) {
            double var4 = var3.readDouble();
            double var6 = var3.readDouble();
            double var8 = var3.readDouble();
            Position var10 = new PositionImpl(var4, var6, var8);
            UUID var11 = var3.readUUID();
            int var12 = var3.readInt();
            BlockPos var13 = var3.readNullable(FriendlyByteBuf::readBlockPos);
            BlockPos var14 = var3.readNullable(FriendlyByteBuf::readBlockPos);
            int var15 = var3.readInt();
            Path var16 = var3.readNullable(Path::createFromStream);
            BeeDebugRenderer.BeeInfo var17 = new BeeDebugRenderer.BeeInfo(var11, var12, var10, var16, var13, var14, var15);
            int var18 = var3.readVarInt();

            for (int var19 = 0; var19 < var18; var19++) {
               String var20 = var3.readUtf();
               var17.goals.add(var20);
            }

            int var19 = var3.readVarInt();

            for (int var20 = 0; var20 < var19; var20++) {
               BlockPos var21 = var3.readBlockPos();
               var17.blacklistedHives.add(var21);
            }

            this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateBeeInfo(var17);
         } else if (ClientboundCustomPayloadPacket.DEBUG_HIVE.equals(var2)) {
            BlockPos var4 = var3.readBlockPos();
            String var5 = var3.readUtf();
            int var6 = var3.readInt();
            int var7 = var3.readInt();
            boolean var8 = var3.readBoolean();
            BeeDebugRenderer.HiveInfo var9 = new BeeDebugRenderer.HiveInfo(var4, var5, var6, var7, var8, this.level.getGameTime());
            this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateHiveInfo(var9);
         } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_CLEAR.equals(var2)) {
            this.minecraft.debugRenderer.gameTestDebugRenderer.clear();
         } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_ADD_MARKER.equals(var2)) {
            BlockPos var4 = var3.readBlockPos();
            int var5 = var3.readInt();
            String var6 = var3.readUtf();
            int var7 = var3.readInt();
            this.minecraft.debugRenderer.gameTestDebugRenderer.addMarker(var4, var5, var6, var7);
         } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_EVENT.equals(var2)) {
            GameEvent var4 = BuiltInRegistries.GAME_EVENT.get(new ResourceLocation(var3.readUtf()));
            Vec3 var5 = new Vec3(var3.readDouble(), var3.readDouble(), var3.readDouble());
            this.minecraft.debugRenderer.gameEventListenerRenderer.trackGameEvent(var4, var5);
         } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_EVENT_LISTENER.equals(var2)) {
            ResourceLocation var4 = var3.readResourceLocation();
            PositionSource var5 = BuiltInRegistries.POSITION_SOURCE_TYPE
               .getOptional(var4)
               .orElseThrow(() -> new IllegalArgumentException("Unknown position source type " + var4))
               .read(var3);
            int var6 = var3.readVarInt();
            this.minecraft.debugRenderer.gameEventListenerRenderer.trackListener(var5, var6);
         } else {
            LOGGER.warn("Unknown custom packed identifier: {}", var2);
         }
      } finally {
         if (var3 != null) {
            var3.release();
         }
      }
   }

   @Override
   public void handleAddObjective(ClientboundSetObjectivePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Scoreboard var2 = this.level.getScoreboard();
      String var3 = param0.getObjectiveName();
      if (param0.getMethod() == 0) {
         var2.addObjective(var3, ObjectiveCriteria.DUMMY, param0.getDisplayName(), param0.getRenderType());
      } else if (var2.hasObjective(var3)) {
         Objective var4 = var2.getObjective(var3);
         if (param0.getMethod() == 1) {
            var2.removeObjective(var4);
         } else if (param0.getMethod() == 2) {
            var4.setRenderType(param0.getRenderType());
            var4.setDisplayName(param0.getDisplayName());
         }
      }
   }

   @Override
   public void handleSetScore(ClientboundSetScorePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Scoreboard var2 = this.level.getScoreboard();
      String var3 = param0.getObjectiveName();
      switch (param0.getMethod()) {
         case CHANGE:
            Objective var4 = var2.getOrCreateObjective(var3);
            Score var5 = var2.getOrCreatePlayerScore(param0.getOwner(), var4);
            var5.setScore(param0.getScore());
            break;
         case REMOVE:
            var2.resetPlayerScore(param0.getOwner(), var2.getObjective(var3));
      }
   }

   @Override
   public void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Scoreboard var2 = this.level.getScoreboard();
      String var3 = param0.getObjectiveName();
      Objective var4 = var3 == null ? null : var2.getOrCreateObjective(var3);
      var2.setDisplayObjective(param0.getSlot(), var4);
   }

   @Override
   public void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Scoreboard var2 = this.level.getScoreboard();
      ClientboundSetPlayerTeamPacket.Action var4 = param0.getTeamAction();
      PlayerTeam var3;
      if (var4 == ClientboundSetPlayerTeamPacket.Action.ADD) {
         var3 = var2.addPlayerTeam(param0.getName());
      } else {
         var3 = var2.getPlayerTeam(param0.getName());
         if (var3 == null) {
            LOGGER.warn(
               "Received packet for unknown team {}: team action: {}, player action: {}", param0.getName(), param0.getTeamAction(), param0.getPlayerAction()
            );
            return;
         }
      }

      Optional<ClientboundSetPlayerTeamPacket.Parameters> var5 = param0.getParameters();
      var5.ifPresent(l_param1 -> {
         var3.setDisplayName(l_param1.getDisplayName());
         var3.setColor(l_param1.getColor());
         var3.unpackOptions(l_param1.getOptions());
         Team.Visibility l_var2 = Team.Visibility.byName(l_param1.getNametagVisibility());
         if (l_var2 != null) {
            var3.setNameTagVisibility(l_var2);
         }

         Team.CollisionRule l_var3 = Team.CollisionRule.byName(l_param1.getCollisionRule());
         if (l_var3 != null) {
            var3.setCollisionRule(l_var3);
         }

         var3.setPlayerPrefix(l_param1.getPlayerPrefix());
         var3.setPlayerSuffix(l_param1.getPlayerSuffix());
      });
      ClientboundSetPlayerTeamPacket.Action var6 = param0.getPlayerAction();
      if (var6 == ClientboundSetPlayerTeamPacket.Action.ADD) {
         for (String var8 : param0.getPlayers()) {
            var2.addPlayerToTeam(var8, var3);
         }
      } else if (var6 == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
         for (String var8 : param0.getPlayers()) {
            var2.removePlayerFromTeam(var8, var3);
         }
      }

      if (var4 == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
         var2.removePlayerTeam(var3);
      }
   }

   @Override
   public void handleParticleEvent(ClientboundLevelParticlesPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      if (param0.getCount() == 0) {
         double var2 = param0.getMaxSpeed() * param0.getXDist();
         double var4 = param0.getMaxSpeed() * param0.getYDist();
         double var6 = param0.getMaxSpeed() * param0.getZDist();

         try {
            this.level.addParticle(param0.getParticle(), param0.isOverrideLimiter(), param0.getX(), param0.getY(), param0.getZ(), var2, var4, var6);
         } catch (Throwable var8) {
            LOGGER.warn("Could not spawn particle effect {}", param0.getParticle());
         }
      } else {
         for (int var2 = 0; var2 < param0.getCount(); var2++) {
            double var3 = this.random.nextGaussian() * param0.getXDist();
            double var5 = this.random.nextGaussian() * param0.getYDist();
            double var7 = this.random.nextGaussian() * param0.getZDist();
            double var9 = this.random.nextGaussian() * param0.getMaxSpeed();
            double var11 = this.random.nextGaussian() * param0.getMaxSpeed();
            double var13 = this.random.nextGaussian() * param0.getMaxSpeed();

            try {
               this.level
                  .addParticle(
                     param0.getParticle(), param0.isOverrideLimiter(), param0.getX() + var3, param0.getY() + var5, param0.getZ() + var7, var9, var11, var13
                  );
            } catch (Throwable var15) {
               LOGGER.warn("Could not spawn particle effect {}", param0.getParticle());
               return;
            }
         }
      }
   }

   @Override
   public void handlePing(ClientboundPingPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.send(new ServerboundPongPacket(param0.getId()));
   }

   @Override
   public void handleUpdateAttributes(ClientboundUpdateAttributesPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      Entity var2 = this.level.getEntity(param0.getEntityId());
      if (var2 != null) {
         if (!(var2 instanceof LivingEntity)) {
            throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + var2 + ")");
         }

         AttributeMap var3 = ((LivingEntity)var2).getAttributes();

         for (ClientboundUpdateAttributesPacket.AttributeSnapshot var5 : param0.getValues()) {
            AttributeInstance var6 = var3.getInstance(var5.getAttribute());
            if (var6 == null) {
               LOGGER.warn("Entity {} does not have attribute {}", var2, BuiltInRegistries.ATTRIBUTE.getKey(var5.getAttribute()));
            } else {
               var6.setBaseValue(var5.getBase());
               var6.removeModifiers();

               for (AttributeModifier var8 : var5.getModifiers()) {
                  var6.addTransientModifier(var8);
               }
            }
         }
      }
   }

   @Override
   public void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      AbstractContainerMenu var2 = this.minecraft.player.containerMenu;
      if (var2.containerId == param0.getContainerId()) {
         this.recipeManager.byKey(param0.getRecipe()).ifPresent(l_param1 -> {
            if (this.minecraft.screen instanceof RecipeUpdateListener) {
               RecipeBookComponent l_var3 = ((RecipeUpdateListener)this.minecraft.screen).getRecipeBookComponent();
               l_var3.setupGhostRecipe((Recipe<?>)l_param1, var2.slots);
            }
         });
      }
   }

   @Override
   public void handleLightUpdatePacket(ClientboundLightUpdatePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      int var2 = param0.getX();
      int var3 = param0.getZ();
      ClientboundLightUpdatePacketData var4 = param0.getLightData();
      this.level.queueLightUpdate(() -> this.applyLightData(var2, var3, var4));
   }

   private void applyLightData(int param0, int param1, ClientboundLightUpdatePacketData param2) {
      LevelLightEngine var4 = this.level.getChunkSource().getLightEngine();
      BitSet var5 = param2.getSkyYMask();
      BitSet var6 = param2.getEmptySkyYMask();
      Iterator<byte[]> var7 = param2.getSkyUpdates().iterator();
      this.readSectionList(param0, param1, var4, LightLayer.SKY, var5, var6, var7, param2.getTrustEdges());
      BitSet var8 = param2.getBlockYMask();
      BitSet var9 = param2.getEmptyBlockYMask();
      Iterator<byte[]> var10 = param2.getBlockUpdates().iterator();
      this.readSectionList(param0, param1, var4, LightLayer.BLOCK, var8, var9, var10, param2.getTrustEdges());
      this.level.setLightReady(param0, param1);
   }

   @Override
   public void handleMerchantOffers(ClientboundMerchantOffersPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      AbstractContainerMenu var2 = this.minecraft.player.containerMenu;
      if (param0.getContainerId() == var2.containerId && var2 instanceof MerchantMenu var3) {
         var3.setOffers(new MerchantOffers(param0.getOffers().createTag()));
         var3.setXp(param0.getVillagerXp());
         var3.setMerchantLevel(param0.getVillagerLevel());
         var3.setShowProgressBar(param0.showProgress());
         var3.setCanRestock(param0.canRestock());
      }
   }

   @Override
   public void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.serverChunkRadius = param0.getRadius();
      this.minecraft.options.setServerRenderDistance(this.serverChunkRadius);
      this.level.getChunkSource().updateViewRadius(param0.getRadius());
   }

   @Override
   public void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.serverSimulationDistance = param0.simulationDistance();
      this.level.setServerSimulationDistance(this.serverSimulationDistance);
   }

   @Override
   public void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.level.getChunkSource().updateViewCenter(param0.getX(), param0.getZ());
   }

   @Override
   public void handleBlockChangedAck(ClientboundBlockChangedAckPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);
      this.level.handleBlockChangedAck(param0.sequence());
   }

   @Override
   public void handleBundlePacket(ClientboundBundlePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.minecraft);

      for (Packet<ClientGamePacketListener> var3 : param0.subPackets()) {
         var3.handle(this);
      }
   }

   private void readSectionList(
      int param0, int param1, LevelLightEngine param2, LightLayer param3, BitSet param4, BitSet param5, Iterator<byte[]> param6, boolean param7
   ) {
      for (int var9 = 0; var9 < param2.getLightSectionCount(); var9++) {
         int var10 = param2.getMinLightSection() + var9;
         boolean var11 = param4.get(var9);
         boolean var12 = param5.get(var9);
         if (var11 || var12) {
            param2.queueSectionData(
               param3, SectionPos.of(param0, var10, param1), var11 ? new DataLayer((byte[])param6.next().clone()) : new DataLayer(), param7
            );
            this.level.setSectionDirtyWithNeighbors(param0, var10, param1);
         }
      }
   }

   public Connection getConnection() {
      return this.connection;
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   public Collection<PlayerInfo> getListedOnlinePlayers() {
      return this.listedPlayers;
   }

   public Collection<PlayerInfo> getOnlinePlayers() {
      return this.playerInfoMap.values();
   }

   public Collection<UUID> getOnlinePlayerIds() {
      return this.playerInfoMap.keySet();
   }

   @Nullable
   public PlayerInfo getPlayerInfo(UUID param0) {
      return this.playerInfoMap.get(param0);
   }

   @Nullable
   public PlayerInfo getPlayerInfo(String param0) {
      for (PlayerInfo var3 : this.playerInfoMap.values()) {
         if (var3.getProfile().getName().equals(param0)) {
            return var3;
         }
      }

      return null;
   }

   public GameProfile getLocalGameProfile() {
      return this.localGameProfile;
   }

   public ClientAdvancements getAdvancements() {
      return this.advancements;
   }

   public CommandDispatcher<SharedSuggestionProvider> getCommands() {
      return this.commands;
   }

   public ClientLevel getLevel() {
      return this.level;
   }

   public DebugQueryHandler getDebugQueryHandler() {
      return this.debugQueryHandler;
   }

   public UUID getId() {
      return this.id;
   }

   public Set<ResourceKey<Level>> levels() {
      return this.levels;
   }

   public RegistryAccess registryAccess() {
      return this.registryAccess.compositeAccess();
   }

   public void markMessageAsProcessed(PlayerChatMessage param0, boolean param1) {
      MessageSignature var3 = param0.signature();
      if (var3 != null && this.lastSeenMessages.addPending(var3, param1) && this.lastSeenMessages.offset() > 64) {
         this.sendChatAcknowledgement();
      }
   }

   private void sendChatAcknowledgement() {
      int var1 = this.lastSeenMessages.getAndClearOffset();
      if (var1 > 0) {
         this.send(new ServerboundChatAckPacket(var1));
      }
   }

   public void sendChat(String param0) {
      Instant var2 = Instant.now();
      long var3 = Crypt.SaltSupplier.getLong();
      LastSeenMessagesTracker.Update var5 = this.lastSeenMessages.generateAndApplyUpdate();
      MessageSignature var6 = this.signedMessageEncoder.pack(new SignedMessageBody(param0, var2, var3, var5.lastSeen()));
      this.send(new ServerboundChatPacket(param0, var2, var3, var6, var5.update()));
   }

   public void sendCommand(String param0) {
      Instant var2 = Instant.now();
      long var3 = Crypt.SaltSupplier.getLong();
      LastSeenMessagesTracker.Update var5 = this.lastSeenMessages.generateAndApplyUpdate();
      ArgumentSignatures var6 = ArgumentSignatures.signCommand(SignableCommand.of(this.parseCommand(param0)), l_param4 -> {
         SignedMessageBody l_var6 = new SignedMessageBody(l_param4, var2, var3, var5.lastSeen());
         return this.signedMessageEncoder.pack(l_var6);
      });
      this.send(new ServerboundChatCommandPacket(param0, var2, var3, var6, var5.update()));
   }

   public boolean sendUnsignedCommand(String param0) {
      if (SignableCommand.of(this.parseCommand(param0)).arguments().isEmpty()) {
         LastSeenMessagesTracker.Update var2 = this.lastSeenMessages.generateAndApplyUpdate();
         this.send(new ServerboundChatCommandPacket(param0, Instant.now(), 0L, ArgumentSignatures.EMPTY, var2.update()));
         return true;
      } else {
         return false;
      }
   }

   private ParseResults<SharedSuggestionProvider> parseCommand(String param0) {
      return this.commands.parse(param0, this.suggestionsProvider);
   }

   @Override
   public void tick() {
      if (this.connection.isEncrypted()) {
         ProfileKeyPairManager var1 = this.minecraft.getProfileKeyPairManager();
         if (var1.shouldRefreshKeyPair()) {
            var1.prepareKeyPair().thenAcceptAsync(l_param0 -> l_param0.ifPresent(this::setKeyPair), this.minecraft);
         }
      }

      this.sendDeferredPackets();
      this.telemetryManager.tick();
   }

   public void setKeyPair(ProfileKeyPair param0) {
      if (this.localGameProfile.getId().equals(this.minecraft.getUser().getProfileId())) {
         if (this.chatSession == null || !this.chatSession.keyPair().equals(param0)) {
            this.chatSession = LocalChatSession.create(param0);
            this.signedMessageEncoder = this.chatSession.createMessageEncoder(this.localGameProfile.getId());
            this.send(new ServerboundChatSessionUpdatePacket(this.chatSession.asRemote().asData()));
         }
      }
   }

   @Nullable
   public ServerData getServerData() {
      return this.serverData;
   }

   public FeatureFlagSet enabledFeatures() {
      return this.enabledFeatures;
   }

   public boolean isFeatureEnabled(FeatureFlagSet param0) {
      return param0.isSubsetOf(this.enabledFeatures());
   }

   record DeferredPacket(Packet<ServerGamePacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
   }
}
