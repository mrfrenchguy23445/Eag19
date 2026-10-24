package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public abstract class PlayerList {
   public static final File USERBANLIST_FILE = new File("banned-players.json");
   public static final File IPBANLIST_FILE = new File("banned-ips.json");
   public static final File OPLIST_FILE = new File("ops.json");
   public static final File WHITELIST_FILE = new File("whitelist.json");
   public static final Component CHAT_FILTERED_FULL = Component.translatable("chat.filtered_full");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SEND_PLAYER_INFO_INTERVAL = 600;
   private static final SimpleDateFormat BAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
   private final MinecraftServer server;
   private final List<ServerPlayer> players = Lists.newArrayList();
   private final Map<UUID, ServerPlayer> playersByUUID = Maps.newHashMap();
   private final UserBanList bans = new UserBanList(USERBANLIST_FILE);
   private final IpBanList ipBans = new IpBanList(IPBANLIST_FILE);
   private final ServerOpList ops = new ServerOpList(OPLIST_FILE);
   private final UserWhiteList whitelist = new UserWhiteList(WHITELIST_FILE);
   private final Map<UUID, ServerStatsCounter> stats = Maps.newHashMap();
   private final Map<UUID, PlayerAdvancements> advancements = Maps.newHashMap();
   private final PlayerDataStorage playerIo;
   private boolean doWhiteList;
   private final LayeredRegistryAccess<RegistryLayer> registries;
   private final RegistryAccess.Frozen synchronizedRegistries;
   protected final int maxPlayers;
   private int viewDistance;
   private int simulationDistance;
   private boolean allowCheatsForAllPlayers;
   private static final boolean ALLOW_LOGOUTIVATOR = false;
   private int sendAllPlayerInfoIn;

   public PlayerList(MinecraftServer param0, LayeredRegistryAccess<RegistryLayer> param1, PlayerDataStorage param2, int param3) {
      this.server = param0;
      this.registries = param1;
      this.synchronizedRegistries = new RegistryAccess.ImmutableRegistryAccess(RegistrySynchronization.networkedRegistries(param1)).freeze();
      this.maxPlayers = param3;
      this.playerIo = param2;
   }

   public void placeNewPlayer(Connection param0, ServerPlayer param1) {
      GameProfile var3 = param1.getGameProfile();
      GameProfileCache var4 = this.server.getProfileCache();
      Optional<GameProfile> var5 = var4.get(var3.getId());
      String var6 = var5.map(GameProfile::getName).orElse(var3.getName());
      var4.add(var3);
      CompoundTag var7 = this.load(param1);
      ResourceKey<Level> var8 = var7 != null
         ? DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, var7.get("Dimension"))).resultOrPartial(LOGGER::error).orElse(Level.OVERWORLD)
         : Level.OVERWORLD;
      ServerLevel var9 = this.server.getLevel(var8);
      ServerLevel var10;
      if (var9 == null) {
         LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", var8);
         var10 = this.server.overworld();
      } else {
         var10 = var9;
      }

      param1.setLevel(var10);
      String var11 = "local";
      if (param0.getRemoteAddress() != null) {
         var11 = param0.getRemoteAddress().toString();
      }

      LOGGER.info(
         "{}[{}] logged in with entity id {} at ({}, {}, {})", param1.getName().getString(), var11, param1.getId(), param1.getX(), param1.getY(), param1.getZ()
      );
      LevelData var12 = var10.getLevelData();
      param1.loadGameTypes(var7);
      ServerGamePacketListenerImpl var13 = new ServerGamePacketListenerImpl(this.server, param0, param1);
      GameRules var14 = var10.getGameRules();
      boolean var15 = var14.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
      boolean var16 = var14.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO);
      var13.send(
         new ClientboundLoginPacket(
            param1.getId(),
            var12.isHardcore(),
            param1.gameMode.getGameModeForPlayer(),
            param1.gameMode.getPreviousGameModeForPlayer(),
            this.server.levelKeys(),
            this.synchronizedRegistries,
            var10.dimensionTypeId(),
            var10.dimension(),
            BiomeManager.obfuscateSeed(var10.getSeed()),
            this.getMaxPlayers(),
            this.viewDistance,
            this.simulationDistance,
            var16,
            !var15,
            var10.isDebug(),
            var10.isFlat(),
            param1.getLastDeathLocation()
         )
      );
      var13.send(new ClientboundUpdateEnabledFeaturesPacket(FeatureFlags.REGISTRY.toNames(var10.enabledFeatures())));
      var13.send(
         new ClientboundCustomPayloadPacket(
            ClientboundCustomPayloadPacket.BRAND, new FriendlyByteBuf(Unpooled.buffer()).writeUtf(this.getServer().getServerModName())
         )
      );
      var13.send(new ClientboundChangeDifficultyPacket(var12.getDifficulty(), var12.isDifficultyLocked()));
      var13.send(new ClientboundPlayerAbilitiesPacket(param1.getAbilities()));
      var13.send(new ClientboundSetCarriedItemPacket(param1.getInventory().selected));
      var13.send(new ClientboundUpdateRecipesPacket(this.server.getRecipeManager().getRecipes()));
      var13.send(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
      this.sendPlayerPermissionLevel(param1);
      param1.getStats().markAllDirty();
      param1.getRecipeBook().sendInitialRecipeBook(param1);
      this.updateEntireScoreboard(var10.getScoreboard(), param1);
      this.server.invalidateStatus();
      MutableComponent var17;
      if (param1.getGameProfile().getName().equalsIgnoreCase(var6)) {
         var17 = Component.translatable("multiplayer.player.joined", param1.getDisplayName());
      } else {
         var17 = Component.translatable("multiplayer.player.joined.renamed", param1.getDisplayName(), var6);
      }

      this.broadcastSystemMessage(var17.withStyle(ChatFormatting.YELLOW), false);
      var13.teleport(param1.getX(), param1.getY(), param1.getZ(), param1.getYRot(), param1.getXRot());
      ServerStatus var18 = this.server.getStatus();
      if (var18 != null) {
         param1.sendServerStatus(var18);
      }

      param1.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(this.players));
      this.players.add(param1);
      this.playersByUUID.put(param1.getUUID(), param1);
      this.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(param1)));
      this.sendLevelInfo(param1, var10);
      var10.addNewPlayer(param1);
      this.server.getCustomBossEvents().onPlayerConnect(param1);
      this.server
         .getServerResourcePack()
         .ifPresent(l_param1 -> param1.sendTexturePack(l_param1.url(), l_param1.hash(), l_param1.isRequired(), l_param1.prompt()));

      for (MobEffectInstance var20 : param1.getActiveEffects()) {
         var13.send(new ClientboundUpdateMobEffectPacket(param1.getId(), var20));
      }

      if (var7 != null && var7.contains("RootVehicle", 10)) {
         CompoundTag var19 = var7.getCompound("RootVehicle");
         Entity var20 = EntityType.loadEntityRecursive(var19.getCompound("Entity"), var10, l_param1 -> !var10.addWithUUID(l_param1) ? null : l_param1);
         if (var20 != null) {
            UUID var21;
            if (var19.hasUUID("Attach")) {
               var21 = var19.getUUID("Attach");
            } else {
               var21 = null;
            }

            if (var20.getUUID().equals(var21)) {
               param1.startRiding(var20, true);
            } else {
               for (Entity var23 : var20.getIndirectPassengers()) {
                  if (var23.getUUID().equals(var21)) {
                     param1.startRiding(var23, true);
                     break;
                  }
               }
            }

            if (!param1.isPassenger()) {
               LOGGER.warn("Couldn't reattach entity to player");
               var20.discard();

               for (Entity var23 : var20.getIndirectPassengers()) {
                  var23.discard();
               }
            }
         }
      }

      param1.initInventoryMenu();
   }

   protected void updateEntireScoreboard(ServerScoreboard param0, ServerPlayer param1) {
      Set<Objective> var3 = Sets.newHashSet();

      for (PlayerTeam var5 : param0.getPlayerTeams()) {
         param1.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(var5, true));
      }

      for (int var4 = 0; var4 < 19; var4++) {
         Objective var5 = param0.getDisplayObjective(var4);
         if (var5 != null && !var3.contains(var5)) {
            for (Packet<?> var8 : param0.getStartTrackingPackets(var5)) {
               param1.connection.send(var8);
            }

            var3.add(var5);
         }
      }
   }

   public void addWorldborderListener(ServerLevel param0) {
      param0.getWorldBorder().addListener(new BorderChangeListener() {
         @Override
         public void onBorderSizeSet(WorldBorder l_param0, double l_param1) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderSizePacket(l_param0));
         }

         @Override
         public void onBorderSizeLerping(WorldBorder l_param0, double l_param1, double l_param3, long l_param5) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderLerpSizePacket(l_param0));
         }

         @Override
         public void onBorderCenterSet(WorldBorder l_param0, double l_param1, double l_param3) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderCenterPacket(l_param0));
         }

         @Override
         public void onBorderSetWarningTime(WorldBorder l_param0, int l_param1) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDelayPacket(l_param0));
         }

         @Override
         public void onBorderSetWarningBlocks(WorldBorder l_param0, int l_param1) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDistancePacket(l_param0));
         }

         @Override
         public void onBorderSetDamagePerBlock(WorldBorder l_param0, double l_param1) {
         }

         @Override
         public void onBorderSetDamageSafeZOne(WorldBorder l_param0, double l_param1) {
         }
      });
   }

   @Nullable
   public CompoundTag load(ServerPlayer param0) {
      CompoundTag var2 = this.server.getWorldData().getLoadedPlayerTag();
      CompoundTag var3;
      if (this.server.isSingleplayerOwner(param0.getGameProfile()) && var2 != null) {
         var3 = var2;
         param0.load(var3);
         LOGGER.debug("loading single player");
      } else {
         var3 = this.playerIo.load(param0);
      }

      return var3;
   }

   protected void save(ServerPlayer param0) {
      this.playerIo.save(param0);
      ServerStatsCounter var2 = this.stats.get(param0.getUUID());
      if (var2 != null) {
         var2.save();
      }

      PlayerAdvancements var3 = this.advancements.get(param0.getUUID());
      if (var3 != null) {
         var3.save();
      }
   }

   public void remove(ServerPlayer param0) {
      ServerLevel var2 = param0.getLevel();
      param0.awardStat(Stats.LEAVE_GAME);
      this.save(param0);
      if (param0.isPassenger()) {
         Entity var3 = param0.getRootVehicle();
         if (var3.hasExactlyOnePlayerPassenger()) {
            LOGGER.debug("Removing player mount");
            param0.stopRiding();
            var3.getPassengersAndSelf().forEach(l_param0 -> l_param0.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER));
         }
      }

      param0.unRide();
      var2.removePlayerImmediately(param0, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
      param0.getAdvancements().stopListening();
      this.players.remove(param0);
      this.server.getCustomBossEvents().onPlayerDisconnect(param0);
      UUID var3 = param0.getUUID();
      ServerPlayer var4 = this.playersByUUID.get(var3);
      if (var4 == param0) {
         this.playersByUUID.remove(var3);
         this.stats.remove(var3);
         this.advancements.remove(var3);
      }

      this.broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(param0.getUUID())));
   }

   @Nullable
   public Component canPlayerLogin(SocketAddress param0, GameProfile param1) {
      if (this.bans.isBanned(param1)) {
         UserBanListEntry var3 = this.bans.get(param1);
         MutableComponent var4 = Component.translatable("multiplayer.disconnect.banned.reason", var3.getReason());
         if (var3.getExpires() != null) {
            var4.append(Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(var3.getExpires())));
         }

         return var4;
      } else {
         if (!this.isWhiteListed(param1)) {
            return Component.translatable("multiplayer.disconnect.not_whitelisted");
         }

         if (this.ipBans.isBanned(param0)) {
            IpBanListEntry var3 = this.ipBans.get(param0);
            MutableComponent var4 = Component.translatable("multiplayer.disconnect.banned_ip.reason", var3.getReason());
            if (var3.getExpires() != null) {
               var4.append(Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(var3.getExpires())));
            }

            return var4;
         } else {
            return this.players.size() >= this.maxPlayers && !this.canBypassPlayerLimit(param1)
               ? Component.translatable("multiplayer.disconnect.server_full")
               : null;
         }
      }
   }

   public ServerPlayer getPlayerForLogin(GameProfile param0) {
      UUID var2 = UUIDUtil.getOrCreatePlayerUUID(param0);
      List<ServerPlayer> var3 = Lists.newArrayList();

      for (int var4 = 0; var4 < this.players.size(); var4++) {
         ServerPlayer var5 = this.players.get(var4);
         if (var5.getUUID().equals(var2)) {
            var3.add(var5);
         }
      }

      ServerPlayer var4 = this.playersByUUID.get(param0.getId());
      if (var4 != null && !var3.contains(var4)) {
         var3.add(var4);
      }

      for (ServerPlayer var6 : var3) {
         var6.connection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
      }

      return new ServerPlayer(this.server, this.server.overworld(), param0);
   }

   public ServerPlayer respawn(ServerPlayer param0, boolean param1) {
      this.players.remove(param0);
      param0.getLevel().removePlayerImmediately(param0, Entity.RemovalReason.DISCARDED);
      BlockPos var3 = param0.getRespawnPosition();
      float var4 = param0.getRespawnAngle();
      boolean var5 = param0.isRespawnForced();
      ServerLevel var6 = this.server.getLevel(param0.getRespawnDimension());
      Optional<Vec3> var7;
      if (var6 != null && var3 != null) {
         var7 = Player.findRespawnPositionAndUseSpawnBlock(var6, var3, var4, var5, param1);
      } else {
         var7 = Optional.empty();
      }

      ServerLevel var8 = var6 != null && var7.isPresent() ? var6 : this.server.overworld();
      ServerPlayer var9 = new ServerPlayer(this.server, var8, param0.getGameProfile());
      var9.connection = param0.connection;
      var9.restoreFrom(param0, param1);
      var9.setId(param0.getId());
      var9.setMainArm(param0.getMainArm());

      for (String var11 : param0.getTags()) {
         var9.addTag(var11);
      }

      boolean var10 = false;
      if (var7.isPresent()) {
         BlockState var11 = var8.getBlockState(var3);
         boolean var12 = var11.is(Blocks.RESPAWN_ANCHOR);
         Vec3 var13 = var7.get();
         float var14;
         if (!var11.is(BlockTags.BEDS) && !var12) {
            var14 = var4;
         } else {
            Vec3 var15 = Vec3.atBottomCenterOf(var3).subtract(var13).normalize();
            var14 = (float)Mth.wrapDegrees(Mth.atan2(var15.z, var15.x) * 180.0F / (float)Math.PI - 90.0);
         }

         var9.moveTo(var13.x, var13.y, var13.z, var14, 0.0F);
         var9.setRespawnPosition(var8.dimension(), var3, var4, var5, false);
         var10 = !param1 && var12;
      } else if (var3 != null) {
         var9.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
      }

      while (!var8.noCollision(var9) && var9.getY() < var8.getMaxBuildHeight()) {
         var9.setPos(var9.getX(), var9.getY() + 1.0, var9.getZ());
      }

      byte var11 = (byte)(param1 ? 1 : 0);
      LevelData var12 = var9.level.getLevelData();
      var9.connection
         .send(
            new ClientboundRespawnPacket(
               var9.level.dimensionTypeId(),
               var9.level.dimension(),
               BiomeManager.obfuscateSeed(var9.getLevel().getSeed()),
               var9.gameMode.getGameModeForPlayer(),
               var9.gameMode.getPreviousGameModeForPlayer(),
               var9.getLevel().isDebug(),
               var9.getLevel().isFlat(),
               var11,
               var9.getLastDeathLocation()
            )
         );
      var9.connection.teleport(var9.getX(), var9.getY(), var9.getZ(), var9.getYRot(), var9.getXRot());
      var9.connection.send(new ClientboundSetDefaultSpawnPositionPacket(var8.getSharedSpawnPos(), var8.getSharedSpawnAngle()));
      var9.connection.send(new ClientboundChangeDifficultyPacket(var12.getDifficulty(), var12.isDifficultyLocked()));
      var9.connection.send(new ClientboundSetExperiencePacket(var9.experienceProgress, var9.totalExperience, var9.experienceLevel));
      this.sendLevelInfo(var9, var8);
      this.sendPlayerPermissionLevel(var9);
      var8.addRespawnedPlayer(var9);
      this.players.add(var9);
      this.playersByUUID.put(var9.getUUID(), var9);
      var9.initInventoryMenu();
      var9.setHealth(var9.getHealth());
      if (var10) {
         var9.connection
            .send(
               new ClientboundSoundPacket(
                  SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, var3.getX(), var3.getY(), var3.getZ(), 1.0F, 1.0F, var8.getRandom().nextLong()
               )
            );
      }

      return var9;
   }

   public void sendPlayerPermissionLevel(ServerPlayer param0) {
      GameProfile var2 = param0.getGameProfile();
      int var3 = this.server.getProfilePermissions(var2);
      this.sendPlayerPermissionLevel(param0, var3);
   }

   public void tick() {
      if (++this.sendAllPlayerInfoIn > 600) {
         this.broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), this.players));
         this.sendAllPlayerInfoIn = 0;
      }
   }

   public void broadcastAll(Packet<?> param0) {
      for (ServerPlayer var3 : this.players) {
         var3.connection.send(param0);
      }
   }

   public void broadcastAll(Packet<?> param0, ResourceKey<Level> param1) {
      for (ServerPlayer var4 : this.players) {
         if (var4.level.dimension() == param1) {
            var4.connection.send(param0);
         }
      }
   }

   public void broadcastSystemToTeam(Player param0, Component param1) {
      Team var3 = param0.getTeam();
      if (var3 != null) {
         for (String var6 : var3.getPlayers()) {
            ServerPlayer var7 = this.getPlayerByName(var6);
            if (var7 != null && var7 != param0) {
               var7.sendSystemMessage(param1);
            }
         }
      }
   }

   public void broadcastSystemToAllExceptTeam(Player param0, Component param1) {
      Team var3 = param0.getTeam();
      if (var3 == null) {
         this.broadcastSystemMessage(param1, false);
      } else {
         for (int var4 = 0; var4 < this.players.size(); var4++) {
            ServerPlayer var5 = this.players.get(var4);
            if (var5.getTeam() != var3) {
               var5.sendSystemMessage(param1);
            }
         }
      }
   }

   public String[] getPlayerNamesArray() {
      String[] var1 = new String[this.players.size()];

      for (int var2 = 0; var2 < this.players.size(); var2++) {
         var1[var2] = this.players.get(var2).getGameProfile().getName();
      }

      return var1;
   }

   public UserBanList getBans() {
      return this.bans;
   }

   public IpBanList getIpBans() {
      return this.ipBans;
   }

   public void op(GameProfile param0) {
      this.ops.add(new ServerOpListEntry(param0, this.server.getOperatorUserPermissionLevel(), this.ops.canBypassPlayerLimit(param0)));
      ServerPlayer var2 = this.getPlayer(param0.getId());
      if (var2 != null) {
         this.sendPlayerPermissionLevel(var2);
      }
   }

   public void deop(GameProfile param0) {
      this.ops.remove(param0);
      ServerPlayer var2 = this.getPlayer(param0.getId());
      if (var2 != null) {
         this.sendPlayerPermissionLevel(var2);
      }
   }

   private void sendPlayerPermissionLevel(ServerPlayer param0, int param1) {
      if (param0.connection != null) {
         byte var3;
         if (param1 <= 0) {
            var3 = 24;
         } else if (param1 >= 4) {
            var3 = 28;
         } else {
            var3 = (byte)(24 + param1);
         }

         param0.connection.send(new ClientboundEntityEventPacket(param0, var3));
      }

      this.server.getCommands().sendCommands(param0);
   }

   public boolean isWhiteListed(GameProfile param0) {
      return !this.doWhiteList || this.ops.contains(param0) || this.whitelist.contains(param0);
   }

   public boolean isOp(GameProfile param0) {
      return this.ops.contains(param0)
         || this.server.isSingleplayerOwner(param0) && this.server.getWorldData().getAllowCommands()
         || this.allowCheatsForAllPlayers;
   }

   @Nullable
   public ServerPlayer getPlayerByName(String param0) {
      for (ServerPlayer var3 : this.players) {
         if (var3.getGameProfile().getName().equalsIgnoreCase(param0)) {
            return var3;
         }
      }

      return null;
   }

   public void broadcast(@Nullable Player param0, double param1, double param3, double param5, double param7, ResourceKey<Level> param9, Packet<?> param10) {
      for (int var12 = 0; var12 < this.players.size(); var12++) {
         ServerPlayer var13 = this.players.get(var12);
         if (var13 != param0 && var13.level.dimension() == param9) {
            double var14 = param1 - var13.getX();
            double var16 = param3 - var13.getY();
            double var18 = param5 - var13.getZ();
            if (var14 * var14 + var16 * var16 + var18 * var18 < param7 * param7) {
               var13.connection.send(param10);
            }
         }
      }
   }

   public void saveAll() {
      for (int var1 = 0; var1 < this.players.size(); var1++) {
         this.save(this.players.get(var1));
      }
   }

   public UserWhiteList getWhiteList() {
      return this.whitelist;
   }

   public String[] getWhiteListNames() {
      return this.whitelist.getUserList();
   }

   public ServerOpList getOps() {
      return this.ops;
   }

   public String[] getOpNames() {
      return this.ops.getUserList();
   }

   public void reloadWhiteList() {
   }

   public void sendLevelInfo(ServerPlayer param0, ServerLevel param1) {
      WorldBorder var3 = this.server.overworld().getWorldBorder();
      param0.connection.send(new ClientboundInitializeBorderPacket(var3));
      param0.connection
         .send(new ClientboundSetTimePacket(param1.getGameTime(), param1.getDayTime(), param1.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
      param0.connection.send(new ClientboundSetDefaultSpawnPositionPacket(param1.getSharedSpawnPos(), param1.getSharedSpawnAngle()));
      if (param1.isRaining()) {
         param0.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
         param0.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, param1.getRainLevel(1.0F)));
         param0.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, param1.getThunderLevel(1.0F)));
      }
   }

   public void sendAllPlayerInfo(ServerPlayer param0) {
      param0.inventoryMenu.sendAllDataToRemote();
      param0.resetSentInfo();
      param0.connection.send(new ClientboundSetCarriedItemPacket(param0.getInventory().selected));
   }

   public int getPlayerCount() {
      return this.players.size();
   }

   public int getMaxPlayers() {
      return this.maxPlayers;
   }

   public boolean isUsingWhitelist() {
      return this.doWhiteList;
   }

   public void setUsingWhiteList(boolean param0) {
      this.doWhiteList = param0;
   }

   public List<ServerPlayer> getPlayersWithAddress(String param0) {
      List<ServerPlayer> var2 = Lists.newArrayList();

      for (ServerPlayer var4 : this.players) {
         if (var4.getIpAddress().equals(param0)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public int getViewDistance() {
      return this.viewDistance;
   }

   public int getSimulationDistance() {
      return this.simulationDistance;
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   @Nullable
   public CompoundTag getSingleplayerData() {
      return null;
   }

   public void setAllowCheatsForAllPlayers(boolean param0) {
      this.allowCheatsForAllPlayers = param0;
   }

   public void removeAll() {
      for (int var1 = 0; var1 < this.players.size(); var1++) {
         this.players.get(var1).connection.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
      }
   }

   public void broadcastSystemMessage(Component param0, boolean param1) {
      this.broadcastSystemMessage(param0, l_param1 -> param0, param1);
   }

   public void broadcastSystemMessage(Component param0, Function<ServerPlayer, Component> param1, boolean param2) {
      this.server.sendSystemMessage(param0);

      for (ServerPlayer var5 : this.players) {
         Component var6 = param1.apply(var5);
         if (var6 != null) {
            var5.sendSystemMessage(var6, param2);
         }
      }
   }

   public void broadcastChatMessage(PlayerChatMessage param0, CommandSourceStack param1, ChatType.Bound param2) {
      this.broadcastChatMessage(param0, param1::shouldFilterMessageTo, param1.getPlayer(), param2);
   }

   public void broadcastChatMessage(PlayerChatMessage param0, ServerPlayer param1, ChatType.Bound param2) {
      this.broadcastChatMessage(param0, param1::shouldFilterMessageTo, param1, param2);
   }

   private void broadcastChatMessage(PlayerChatMessage param0, Predicate<ServerPlayer> param1, @Nullable ServerPlayer param2, ChatType.Bound param3) {
      boolean var5 = this.verifyChatTrusted(param0);
      this.server.logChatMessage(param0.decoratedContent(), param3, var5 ? null : "Not Secure");
      OutgoingChatMessage var6 = OutgoingChatMessage.create(param0);
      boolean var7 = false;

      for (ServerPlayer var9 : this.players) {
         boolean var10 = param1.test(var9);
         var9.sendChatMessage(var6, var10, param3);
         var7 |= var10 && param0.isFullyFiltered();
      }

      if (var7 && param2 != null) {
         param2.sendSystemMessage(CHAT_FILTERED_FULL);
      }
   }

   private boolean verifyChatTrusted(PlayerChatMessage param0) {
      return param0.hasSignature() && !param0.hasExpiredServer(Instant.now());
   }

   public ServerStatsCounter getPlayerStats(Player param0) {
      UUID var2 = param0.getUUID();
      ServerStatsCounter var3 = this.stats.get(var2);
      if (var3 == null) {
         File var4 = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
         File var5 = new File(var4, var2 + ".json");
         if (!var5.exists()) {
            File var6 = new File(var4, param0.getName().getString() + ".json");
            Path var7 = java.nio.file.Paths.get(var6.getPath());
            if (FileUtil.isPathNormalized(var7) && FileUtil.isPathPortable(var7) && var7.startsWith(var4.getPath()) && var6.isFile()) {
               var6.renameTo(var5);
            }
         }

         var3 = new ServerStatsCounter(this.server, var5);
         this.stats.put(var2, var3);
      }

      return var3;
   }

   public PlayerAdvancements getPlayerAdvancements(ServerPlayer param0) {
      UUID var2 = param0.getUUID();
      PlayerAdvancements var3 = this.advancements.get(var2);
      if (var3 == null) {
         Path var4 = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(var2 + ".json");
         var3 = new PlayerAdvancements(this.server.getFixerUpper(), this, this.server.getAdvancements(), var4, param0);
         this.advancements.put(var2, var3);
      }

      var3.setPlayer(param0);
      return var3;
   }

   public void setViewDistance(int param0) {
      this.viewDistance = param0;
      this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(param0));

      for (ServerLevel var3 : this.server.getAllLevels()) {
         if (var3 != null) {
            var3.getChunkSource().setViewDistance(param0);
         }
      }
   }

   public void setSimulationDistance(int param0) {
      this.simulationDistance = param0;
      this.broadcastAll(new ClientboundSetSimulationDistancePacket(param0));

      for (ServerLevel var3 : this.server.getAllLevels()) {
         if (var3 != null) {
            var3.getChunkSource().setSimulationDistance(param0);
         }
      }
   }

   public List<ServerPlayer> getPlayers() {
      return this.players;
   }

   @Nullable
   public ServerPlayer getPlayer(UUID param0) {
      return this.playersByUUID.get(param0);
   }

   public boolean canBypassPlayerLimit(GameProfile param0) {
      return false;
   }

   public void reloadResources() {
      for (PlayerAdvancements var2 : this.advancements.values()) {
         var2.reload(this.server.getAdvancements());
      }

      this.broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
      ClientboundUpdateRecipesPacket var1 = new ClientboundUpdateRecipesPacket(this.server.getRecipeManager().getRecipes());

      for (ServerPlayer var3 : this.players) {
         var3.connection.send(var1);
         var3.getRecipeBook().sendInitialRecipeBook(var3);
      }
   }

   public boolean isAllowCheatsForAllPlayers() {
      return this.allowCheatsForAllPlayers;
   }
}
