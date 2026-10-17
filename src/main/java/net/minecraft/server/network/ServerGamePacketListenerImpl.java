package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.net.SocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.LastSeenMessagesValidator;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQuery;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQuery;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FutureChain;
import net.minecraft.util.Mth;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class ServerGamePacketListenerImpl implements ServerPlayerConnection, TickablePacketListener, ServerGamePacketListener {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int LATENCY_CHECK_INTERVAL = 15000;
   public static final double MAX_INTERACTION_DISTANCE = Mth.square(6.0);
   private static final int NO_BLOCK_UPDATES_TO_ACK = -1;
   private static final int TRACKED_MESSAGE_DISCONNECT_THRESHOLD = 4096;
   private static final Component CHAT_VALIDATION_FAILED = Component.translatable("multiplayer.disconnect.chat_validation_failed");
   private final Connection connection;
   private final MinecraftServer server;
   public ServerPlayer player;
   private int tickCount;
   private int ackBlockChangesUpTo = -1;
   private long keepAliveTime;
   private boolean keepAlivePending;
   private long keepAliveChallenge;
   private int chatSpamTickCount;
   private int dropSpamTickCount;
   private double firstGoodX;
   private double firstGoodY;
   private double firstGoodZ;
   private double lastGoodX;
   private double lastGoodY;
   private double lastGoodZ;
   @Nullable
   private Entity lastVehicle;
   private double vehicleFirstGoodX;
   private double vehicleFirstGoodY;
   private double vehicleFirstGoodZ;
   private double vehicleLastGoodX;
   private double vehicleLastGoodY;
   private double vehicleLastGoodZ;
   @Nullable
   private Vec3 awaitingPositionFromClient;
   private int awaitingTeleport;
   private int awaitingTeleportTime;
   private boolean clientIsFloating;
   private int aboveGroundTickCount;
   private boolean clientVehicleIsFloating;
   private int aboveGroundVehicleTickCount;
   private int receivedMovePacketCount;
   private int knownMovePacketCount;
   private final AtomicReference<Instant> lastChatTimeStamp = new AtomicReference<>(Instant.EPOCH);
   @Nullable
   private RemoteChatSession chatSession;
   private SignedMessageChain.Decoder signedMessageDecoder;
   private final LastSeenMessagesValidator lastSeenMessages = new LastSeenMessagesValidator(20);
   private final MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();
   private final FutureChain chatMessageChain;

   public ServerGamePacketListenerImpl(MinecraftServer param0, Connection param1, ServerPlayer param2) {
      this.server = param0;
      this.connection = param1;
      param1.setListener(this);
      this.player = param2;
      param2.connection = this;
      this.keepAliveTime = Util.getMillis();
      param2.getTextFilter().join();
      this.signedMessageDecoder = param0.enforceSecureProfile() ? SignedMessageChain.Decoder.REJECT_ALL : SignedMessageChain.Decoder.unsigned(param2.getUUID());
      this.chatMessageChain = new FutureChain(param0);
   }

   @Override
   public void tick() {
      if (this.ackBlockChangesUpTo > -1) {
         this.send(new ClientboundBlockChangedAckPacket(this.ackBlockChangesUpTo));
         this.ackBlockChangesUpTo = -1;
      }

      this.resetPosition();
      this.player.xo = this.player.getX();
      this.player.yo = this.player.getY();
      this.player.zo = this.player.getZ();
      this.player.doTick();
      this.player.absMoveTo(this.firstGoodX, this.firstGoodY, this.firstGoodZ, this.player.getYRot(), this.player.getXRot());
      this.tickCount++;
      this.knownMovePacketCount = this.receivedMovePacketCount;
      if (this.clientIsFloating && !this.player.isSleeping() && !this.player.isPassenger() && !this.player.isDeadOrDying()) {
         if (++this.aboveGroundTickCount > 80) {
            LOGGER.warn("{} was kicked for floating too long!", this.player.getName().getString());
            this.disconnect(Component.translatable("multiplayer.disconnect.flying"));
            return;
         }
      } else {
         this.clientIsFloating = false;
         this.aboveGroundTickCount = 0;
      }

      this.lastVehicle = this.player.getRootVehicle();
      if (this.lastVehicle != this.player && this.lastVehicle.getControllingPassenger() == this.player) {
         this.vehicleFirstGoodX = this.lastVehicle.getX();
         this.vehicleFirstGoodY = this.lastVehicle.getY();
         this.vehicleFirstGoodZ = this.lastVehicle.getZ();
         this.vehicleLastGoodX = this.lastVehicle.getX();
         this.vehicleLastGoodY = this.lastVehicle.getY();
         this.vehicleLastGoodZ = this.lastVehicle.getZ();
         if (this.clientVehicleIsFloating && this.player.getRootVehicle().getControllingPassenger() == this.player) {
            if (++this.aboveGroundVehicleTickCount > 80) {
               LOGGER.warn("{} was kicked for floating a vehicle too long!", this.player.getName().getString());
               this.disconnect(Component.translatable("multiplayer.disconnect.flying"));
               return;
            }
         } else {
            this.clientVehicleIsFloating = false;
            this.aboveGroundVehicleTickCount = 0;
         }
      } else {
         this.lastVehicle = null;
         this.clientVehicleIsFloating = false;
         this.aboveGroundVehicleTickCount = 0;
      }

      this.server.getProfiler().push("keepAlive");
      long var1 = Util.getMillis();
      if (var1 - this.keepAliveTime >= 15000L) {
         if (this.keepAlivePending) {
            this.disconnect(Component.translatable("disconnect.timeout"));
         } else {
            this.keepAlivePending = true;
            this.keepAliveTime = var1;
            this.keepAliveChallenge = var1;
            this.send(new ClientboundKeepAlivePacket(this.keepAliveChallenge));
         }
      }

      this.server.getProfiler().pop();
      if (this.chatSpamTickCount > 0) {
         this.chatSpamTickCount--;
      }

      if (this.dropSpamTickCount > 0) {
         this.dropSpamTickCount--;
      }

      if (this.player.getLastActionTime() > 0L
         && this.server.getPlayerIdleTimeout() > 0
         && Util.getMillis() - this.player.getLastActionTime() > this.server.getPlayerIdleTimeout() * 1000 * 60) {
         this.disconnect(Component.translatable("multiplayer.disconnect.idling"));
      }
   }

   public void resetPosition() {
      this.firstGoodX = this.player.getX();
      this.firstGoodY = this.player.getY();
      this.firstGoodZ = this.player.getZ();
      this.lastGoodX = this.player.getX();
      this.lastGoodY = this.player.getY();
      this.lastGoodZ = this.player.getZ();
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   private boolean isSingleplayerOwner() {
      return this.server.isSingleplayerOwner(this.player.getGameProfile());
   }

   public void disconnect(Component param0) {
      this.connection.send(new ClientboundDisconnectPacket(param0), PacketSendListener.thenRun(() -> this.connection.disconnect(param0)));
      this.connection.setReadOnly();
      this.server.executeBlocking(this.connection::handleDisconnection);
   }

   private <T, R> CompletableFuture<R> filterTextPacket(T param0, BiFunction<TextFilter, T, CompletableFuture<R>> param1) {
      return param1.apply(this.player.getTextFilter(), param0).thenApply(l_param0 -> {
         if (!this.isAcceptingMessages()) {
            LOGGER.debug("Ignoring packet due to disconnection");
            throw new CancellationException("disconnected");
         } else {
            return (R)l_param0;
         }
      });
   }

   private CompletableFuture<FilteredText> filterTextPacket(String param0) {
      return this.filterTextPacket(param0, TextFilter::processStreamMessage);
   }

   private CompletableFuture<List<FilteredText>> filterTextPacket(List<String> param0) {
      return this.filterTextPacket(param0, TextFilter::processMessageBundle);
   }

   @Override
   public void handlePlayerInput(ServerboundPlayerInputPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.setPlayerInput(param0.getXxa(), param0.getZza(), param0.isJumping(), param0.isShiftKeyDown());
   }

   private static boolean containsInvalidValues(double param0, double param2, double param4, float param6, float param7) {
      return Double.isNaN(param0) || Double.isNaN(param2) || Double.isNaN(param4) || !Floats.isFinite(param7) || !Floats.isFinite(param6);
   }

   private static double clampHorizontal(double param0) {
      return Mth.clamp(param0, -3.0E7, 3.0E7);
   }

   private static double clampVertical(double param0) {
      return Mth.clamp(param0, -2.0E7, 2.0E7);
   }

   @Override
   public void handleMoveVehicle(ServerboundMoveVehiclePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (containsInvalidValues(param0.getX(), param0.getY(), param0.getZ(), param0.getYRot(), param0.getXRot())) {
         this.disconnect(Component.translatable("multiplayer.disconnect.invalid_vehicle_movement"));
      } else {
         Entity var2 = this.player.getRootVehicle();
         if (var2 != this.player && var2.getControllingPassenger() == this.player && var2 == this.lastVehicle) {
            ServerLevel var3 = this.player.getLevel();
            double var4 = var2.getX();
            double var6 = var2.getY();
            double var8 = var2.getZ();
            double var10 = clampHorizontal(param0.getX());
            double var12 = clampVertical(param0.getY());
            double var14 = clampHorizontal(param0.getZ());
            float var16 = Mth.wrapDegrees(param0.getYRot());
            float var17 = Mth.wrapDegrees(param0.getXRot());
            double var18 = var10 - this.vehicleFirstGoodX;
            double var20 = var12 - this.vehicleFirstGoodY;
            double var22 = var14 - this.vehicleFirstGoodZ;
            double var24 = var2.getDeltaMovement().lengthSqr();
            double var26 = var18 * var18 + var20 * var20 + var22 * var22;
            if (var26 - var24 > 100.0 && !this.isSingleplayerOwner()) {
               LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", var2.getName().getString(), this.player.getName().getString(), var18, var20, var22);
               this.connection.send(new ClientboundMoveVehiclePacket(var2));
               return;
            }

            boolean var28 = var3.noCollision(var2, var2.getBoundingBox().deflate(0.0625));
            var18 = var10 - this.vehicleLastGoodX;
            var20 = var12 - this.vehicleLastGoodY - 1.0E-6;
            var22 = var14 - this.vehicleLastGoodZ;
            boolean var29 = var2.verticalCollisionBelow;
            var2.move(MoverType.PLAYER, new Vec3(var18, var20, var22));
            double var30 = var20;
            var18 = var10 - var2.getX();
            var20 = var12 - var2.getY();
            if (var20 > -0.5 || var20 < 0.5) {
               var20 = 0.0;
            }

            var22 = var14 - var2.getZ();
            var26 = var18 * var18 + var20 * var20 + var22 * var22;
            boolean var32 = false;
            if (var26 > 0.0625) {
               var32 = true;
               LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", var2.getName().getString(), this.player.getName().getString(), Math.sqrt(var26));
            }

            var2.absMoveTo(var10, var12, var14, var16, var17);
            boolean var33 = var3.noCollision(var2, var2.getBoundingBox().deflate(0.0625));
            if (var28 && (var32 || !var33)) {
               var2.absMoveTo(var4, var6, var8, var16, var17);
               this.connection.send(new ClientboundMoveVehiclePacket(var2));
               return;
            }

            this.player.getLevel().getChunkSource().move(this.player);
            this.player.checkMovementStatistics(this.player.getX() - var4, this.player.getY() - var6, this.player.getZ() - var8);
            this.clientVehicleIsFloating = var30 >= -0.03125 && !var29 && !this.server.isFlightAllowed() && !var2.isNoGravity() && this.noBlocksAround(var2);
            this.vehicleLastGoodX = var2.getX();
            this.vehicleLastGoodY = var2.getY();
            this.vehicleLastGoodZ = var2.getZ();
         }
      }
   }

   private boolean noBlocksAround(Entity param0) {
      return param0.level.getBlockStates(param0.getBoundingBox().inflate(0.0625).expandTowards(0.0, -0.55, 0.0)).allMatch(BlockBehaviour.BlockStateBase::isAir);
   }

   @Override
   public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (param0.getId() == this.awaitingTeleport) {
         if (this.awaitingPositionFromClient == null) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
            return;
         }

         this.player
            .absMoveTo(
               this.awaitingPositionFromClient.x,
               this.awaitingPositionFromClient.y,
               this.awaitingPositionFromClient.z,
               this.player.getYRot(),
               this.player.getXRot()
            );
         this.lastGoodX = this.awaitingPositionFromClient.x;
         this.lastGoodY = this.awaitingPositionFromClient.y;
         this.lastGoodZ = this.awaitingPositionFromClient.z;
         if (this.player.isChangingDimension()) {
            this.player.hasChangedDimension();
         }

         this.awaitingPositionFromClient = null;
      }
   }

   @Override
   public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.server.getRecipeManager().byKey(param0.getRecipe()).ifPresent(this.player.getRecipeBook()::removeHighlight);
   }

   @Override
   public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.getRecipeBook().setBookSetting(param0.getBookType(), param0.isOpen(), param0.isFiltering());
   }

   @Override
   public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (param0.getAction() == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
         ResourceLocation var2 = param0.getTab();
         Advancement var3 = this.server.getAdvancements().getAdvancement(var2);
         if (var3 != null) {
            this.player.getAdvancements().setSelectedTab(var3);
         }
      }
   }

   @Override
   public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      StringReader var2 = new StringReader(param0.getCommand());
      if (var2.canRead() && var2.peek() == '/') {
         var2.skip();
      }

      ParseResults<CommandSourceStack> var3 = this.server.getCommands().getDispatcher().parse(var2, this.player.createCommandSourceStack());
      this.server
         .getCommands()
         .getDispatcher()
         .getCompletionSuggestions(var3)
         .thenAccept(l_param1 -> this.connection.send(new ClientboundCommandSuggestionsPacket(param0.getId(), l_param1)));
   }

   @Override
   public void handleSetCommandBlock(ServerboundSetCommandBlockPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (!this.server.isCommandBlockEnabled()) {
         this.player.sendSystemMessage(Component.translatable("advMode.notEnabled"));
      } else if (!this.player.canUseGameMasterBlocks()) {
         this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
      } else {
         BaseCommandBlock var2 = null;
         CommandBlockEntity var3 = null;
         BlockPos var4 = param0.getPos();
         BlockEntity var5 = this.player.level.getBlockEntity(var4);
         if (var5 instanceof CommandBlockEntity) {
            var3 = (CommandBlockEntity)var5;
            var2 = var3.getCommandBlock();
         }

         String var6 = param0.getCommand();
         boolean var7 = param0.isTrackOutput();
         if (var2 != null) {
            CommandBlockEntity.Mode var8 = var3.getMode();
            BlockState var9 = this.player.level.getBlockState(var4);
            Direction var10 = var9.getValue(CommandBlock.FACING);

            BlockState var12 = (switch (param0.getMode()) {
               case SEQUENCE -> Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState();
               case AUTO -> Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState();
               default -> Blocks.COMMAND_BLOCK.defaultBlockState();
            }).setValue(CommandBlock.FACING, var10).setValue(CommandBlock.CONDITIONAL, param0.isConditional());
            if (var12 != var9) {
               this.player.level.setBlock(var4, var12, 2);
               var5.setBlockState(var12);
               this.player.level.getChunkAt(var4).setBlockEntity(var5);
            }

            var2.setCommand(var6);
            var2.setTrackOutput(var7);
            if (!var7) {
               var2.setLastOutput(null);
            }

            var3.setAutomatic(param0.isAutomatic());
            if (var8 != param0.getMode()) {
               var3.onModeSwitch();
            }

            var2.onUpdated();
            if (!StringUtil.isNullOrEmpty(var6)) {
               this.player.sendSystemMessage(Component.translatable("advMode.setCommand.success", var6));
            }
         }
      }
   }

   @Override
   public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (!this.server.isCommandBlockEnabled()) {
         this.player.sendSystemMessage(Component.translatable("advMode.notEnabled"));
      } else if (!this.player.canUseGameMasterBlocks()) {
         this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
      } else {
         BaseCommandBlock var2 = param0.getCommandBlock(this.player.level);
         if (var2 != null) {
            var2.setCommand(param0.getCommand());
            var2.setTrackOutput(param0.isTrackOutput());
            if (!param0.isTrackOutput()) {
               var2.setLastOutput(null);
            }

            var2.onUpdated();
            this.player.sendSystemMessage(Component.translatable("advMode.setCommand.success", param0.getCommand()));
         }
      }
   }

   @Override
   public void handlePickItem(ServerboundPickItemPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.getInventory().pickSlot(param0.getSlot());
      this.player
         .connection
         .send(
            new ClientboundContainerSetSlotPacket(
               -2, 0, this.player.getInventory().selected, this.player.getInventory().getItem(this.player.getInventory().selected)
            )
         );
      this.player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, param0.getSlot(), this.player.getInventory().getItem(param0.getSlot())));
      this.player.connection.send(new ClientboundSetCarriedItemPacket(this.player.getInventory().selected));
   }

   @Override
   public void handleRenameItem(ServerboundRenameItemPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.containerMenu instanceof AnvilMenu var2) {
         if (!var2.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, var2);
            return;
         }

         String var3 = SharedConstants.filterText(param0.getName());
         if (var3.length() <= 50) {
            var2.setItemName(var3);
         }
      }
   }

   @Override
   public void handleSetBeaconPacket(ServerboundSetBeaconPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.containerMenu instanceof BeaconMenu var2) {
         if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
            return;
         }

         var2.updateEffects(param0.getPrimary(), param0.getSecondary());
      }
   }

   @Override
   public void handleSetStructureBlock(ServerboundSetStructureBlockPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.canUseGameMasterBlocks()) {
         BlockPos var2 = param0.getPos();
         BlockState var3 = this.player.level.getBlockState(var2);
         if (this.player.level.getBlockEntity(var2) instanceof StructureBlockEntity var5) {
            var5.setMode(param0.getMode());
            var5.setStructureName(param0.getName());
            var5.setStructurePos(param0.getOffset());
            var5.setStructureSize(param0.getSize());
            var5.setMirror(param0.getMirror());
            var5.setRotation(param0.getRotation());
            var5.setMetaData(param0.getData());
            var5.setIgnoreEntities(param0.isIgnoreEntities());
            var5.setShowAir(param0.isShowAir());
            var5.setShowBoundingBox(param0.isShowBoundingBox());
            var5.setIntegrity(param0.getIntegrity());
            var5.setSeed(param0.getSeed());
            if (var5.hasStructureName()) {
               String var6 = var5.getStructureName();
               if (param0.getUpdateType() == StructureBlockEntity.UpdateType.SAVE_AREA) {
                  if (var5.saveStructure()) {
                     this.player.displayClientMessage(Component.translatable("structure_block.save_success", var6), false);
                  } else {
                     this.player.displayClientMessage(Component.translatable("structure_block.save_failure", var6), false);
                  }
               } else if (param0.getUpdateType() == StructureBlockEntity.UpdateType.LOAD_AREA) {
                  if (!var5.isStructureLoadable()) {
                     this.player.displayClientMessage(Component.translatable("structure_block.load_not_found", var6), false);
                  } else if (var5.loadStructure(this.player.getLevel())) {
                     this.player.displayClientMessage(Component.translatable("structure_block.load_success", var6), false);
                  } else {
                     this.player.displayClientMessage(Component.translatable("structure_block.load_prepare", var6), false);
                  }
               } else if (param0.getUpdateType() == StructureBlockEntity.UpdateType.SCAN_AREA) {
                  if (var5.detectSize()) {
                     this.player.displayClientMessage(Component.translatable("structure_block.size_success", var6), false);
                  } else {
                     this.player.displayClientMessage(Component.translatable("structure_block.size_failure"), false);
                  }
               }
            } else {
               this.player.displayClientMessage(Component.translatable("structure_block.invalid_structure_name", param0.getName()), false);
            }

            var5.setChanged();
            this.player.level.sendBlockUpdated(var2, var3, var3, 3);
         }
      }
   }

   @Override
   public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.canUseGameMasterBlocks()) {
         BlockPos var2 = param0.getPos();
         BlockState var3 = this.player.level.getBlockState(var2);
         if (this.player.level.getBlockEntity(var2) instanceof JigsawBlockEntity var5) {
            var5.setName(param0.getName());
            var5.setTarget(param0.getTarget());
            var5.setPool(ResourceKey.create(Registries.TEMPLATE_POOL, param0.getPool()));
            var5.setFinalState(param0.getFinalState());
            var5.setJoint(param0.getJoint());
            var5.setChanged();
            this.player.level.sendBlockUpdated(var2, var3, var3, 3);
         }
      }
   }

   @Override
   public void handleJigsawGenerate(ServerboundJigsawGeneratePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.canUseGameMasterBlocks()) {
         BlockPos var2 = param0.getPos();
         if (this.player.level.getBlockEntity(var2) instanceof JigsawBlockEntity var4) {
            var4.generate(this.player.getLevel(), param0.levels(), param0.keepJigsaws());
         }
      }
   }

   @Override
   public void handleSelectTrade(ServerboundSelectTradePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      int var2 = param0.getItem();
      if (this.player.containerMenu instanceof MerchantMenu var3) {
         if (!var3.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, var3);
            return;
         }

         var3.setSelectionHint(var2);
         var3.tryMoveItems(var2);
      }
   }

   @Override
   public void handleEditBook(ServerboundEditBookPacket param0) {
      int var2 = param0.getSlot();
      if (Inventory.isHotbarSlot(var2) || var2 == 40) {
         List<String> var3 = Lists.newArrayList();
         Optional<String> var4 = param0.getTitle();
         var4.ifPresent(var3::add);
         param0.getPages().stream().limit(100L).forEach(var3::add);
         Consumer<List<FilteredText>> var5 = var4.isPresent()
            ? l_param1 -> this.signBook(l_param1.get(0), l_param1.subList(1, l_param1.size()), var2)
            : l_param1 -> this.updateBookContents(l_param1, var2);
         this.filterTextPacket(var3).thenAcceptAsync(var5, this.server);
      }
   }

   private void updateBookContents(List<FilteredText> param0, int param1) {
      ItemStack var3 = this.player.getInventory().getItem(param1);
      if (var3.is(Items.WRITABLE_BOOK)) {
         this.updateBookPages(param0, UnaryOperator.identity(), var3);
      }
   }

   private void signBook(FilteredText param0, List<FilteredText> param1, int param2) {
      ItemStack var4 = this.player.getInventory().getItem(param2);
      if (var4.is(Items.WRITABLE_BOOK)) {
         ItemStack var5 = new ItemStack(Items.WRITTEN_BOOK);
         CompoundTag var6 = var4.getTag();
         if (var6 != null) {
            var5.setTag(var6.copy());
         }

         var5.addTagElement("author", StringTag.valueOf(this.player.getName().getString()));
         if (this.player.isTextFilteringEnabled()) {
            var5.addTagElement("title", StringTag.valueOf(param0.filteredOrEmpty()));
         } else {
            var5.addTagElement("filtered_title", StringTag.valueOf(param0.filteredOrEmpty()));
            var5.addTagElement("title", StringTag.valueOf(param0.raw()));
         }

         this.updateBookPages(param1, l_param0 -> Component.Serializer.toJson(Component.literal(l_param0)), var5);
         this.player.getInventory().setItem(param2, var5);
      }
   }

   private void updateBookPages(List<FilteredText> param0, UnaryOperator<String> param1, ItemStack param2) {
      ListTag var4 = new ListTag();
      if (this.player.isTextFilteringEnabled()) {
         param0.stream().map(l_param1 -> StringTag.valueOf(param1.apply(l_param1.filteredOrEmpty()))).forEach(var4::add);
      } else {
         CompoundTag var5 = new CompoundTag();
         int var6 = 0;

         for (int var7 = param0.size(); var6 < var7; var6++) {
            FilteredText var8 = param0.get(var6);
            String var9 = var8.raw();
            var4.add(StringTag.valueOf(param1.apply(var9)));
            if (var8.isFiltered()) {
               var5.putString(String.valueOf(var6), param1.apply(var8.filteredOrEmpty()));
            }
         }

         if (!var5.isEmpty()) {
            param2.addTagElement("filtered_pages", var5);
         }
      }

      param2.addTagElement("pages", var4);
   }

   @Override
   public void handleEntityTagQuery(ServerboundEntityTagQuery param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.hasPermissions(2)) {
         Entity var2 = this.player.getLevel().getEntity(param0.getEntityId());
         if (var2 != null) {
            CompoundTag var3 = var2.saveWithoutId(new CompoundTag());
            this.player.connection.send(new ClientboundTagQueryPacket(param0.getTransactionId(), var3));
         }
      }
   }

   @Override
   public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQuery param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.hasPermissions(2)) {
         BlockEntity var2 = this.player.getLevel().getBlockEntity(param0.getPos());
         CompoundTag var3 = var2 != null ? var2.saveWithoutMetadata() : null;
         this.player.connection.send(new ClientboundTagQueryPacket(param0.getTransactionId(), var3));
      }
   }

   @Override
   public void handleMovePlayer(ServerboundMovePlayerPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (containsInvalidValues(param0.getX(0.0), param0.getY(0.0), param0.getZ(0.0), param0.getYRot(0.0F), param0.getXRot(0.0F))) {
         this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
      } else {
         ServerLevel var2 = this.player.getLevel();
         if (!this.player.wonGame) {
            if (this.tickCount == 0) {
               this.resetPosition();
            }

            if (this.awaitingPositionFromClient != null) {
               if (this.tickCount - this.awaitingTeleportTime > 20) {
                  this.awaitingTeleportTime = this.tickCount;
                  this.teleport(
                     this.awaitingPositionFromClient.x,
                     this.awaitingPositionFromClient.y,
                     this.awaitingPositionFromClient.z,
                     this.player.getYRot(),
                     this.player.getXRot()
                  );
               }
            } else {
               this.awaitingTeleportTime = this.tickCount;
               double var3 = clampHorizontal(param0.getX(this.player.getX()));
               double var5 = clampVertical(param0.getY(this.player.getY()));
               double var7 = clampHorizontal(param0.getZ(this.player.getZ()));
               float var9 = Mth.wrapDegrees(param0.getYRot(this.player.getYRot()));
               float var10 = Mth.wrapDegrees(param0.getXRot(this.player.getXRot()));
               if (this.player.isPassenger()) {
                  this.player.absMoveTo(this.player.getX(), this.player.getY(), this.player.getZ(), var9, var10);
                  this.player.getLevel().getChunkSource().move(this.player);
               } else {
                  double var11 = this.player.getX();
                  double var13 = this.player.getY();
                  double var15 = this.player.getZ();
                  double var17 = this.player.getY();
                  double var19 = var3 - this.firstGoodX;
                  double var21 = var5 - this.firstGoodY;
                  double var23 = var7 - this.firstGoodZ;
                  double var25 = this.player.getDeltaMovement().lengthSqr();
                  double var27 = var19 * var19 + var21 * var21 + var23 * var23;
                  if (this.player.isSleeping()) {
                     if (var27 > 1.0) {
                        this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), var9, var10);
                     }
                  } else {
                     this.receivedMovePacketCount++;
                     int var29 = this.receivedMovePacketCount - this.knownMovePacketCount;
                     if (var29 > 5) {
                        LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getName().getString(), var29);
                        var29 = 1;
                     }

                     if (!this.player.isChangingDimension()
                        && (!this.player.getLevel().getGameRules().getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) || !this.player.isFallFlying())) {
                        float var30 = this.player.isFallFlying() ? 300.0F : 100.0F;
                        if (var27 - var25 > var30 * var29 && !this.isSingleplayerOwner()) {
                           LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getName().getString(), var19, var21, var23);
                           this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
                           return;
                        }
                     }

                     AABB var30 = this.player.getBoundingBox();
                     var19 = var3 - this.lastGoodX;
                     var21 = var5 - this.lastGoodY;
                     var23 = var7 - this.lastGoodZ;
                     boolean var31 = var21 > 0.0;
                     if (this.player.isOnGround() && !param0.isOnGround() && var31) {
                        this.player.jumpFromGround();
                     }

                     boolean var32 = this.player.verticalCollisionBelow;
                     this.player.move(MoverType.PLAYER, new Vec3(var19, var21, var23));
                     double var33 = var21;
                     var19 = var3 - this.player.getX();
                     var21 = var5 - this.player.getY();
                     if (var21 > -0.5 || var21 < 0.5) {
                        var21 = 0.0;
                     }

                     var23 = var7 - this.player.getZ();
                     var27 = var19 * var19 + var21 * var21 + var23 * var23;
                     boolean var35 = false;
                     if (!this.player.isChangingDimension()
                        && var27 > 0.0625
                        && !this.player.isSleeping()
                        && !this.player.gameMode.isCreative()
                        && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                        var35 = true;
                        LOGGER.warn("{} moved wrongly!", this.player.getName().getString());
                     }

                     this.player.absMoveTo(var3, var5, var7, var9, var10);
                     if (this.player.noPhysics
                        || this.player.isSleeping()
                        || (!var35 || !var2.noCollision(this.player, var30)) && !this.isPlayerCollidingWithAnythingNew(var2, var30)) {
                        this.clientIsFloating = var33 >= -0.03125
                           && !var32
                           && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR
                           && !this.server.isFlightAllowed()
                           && !this.player.getAbilities().mayfly
                           && !this.player.hasEffect(MobEffects.LEVITATION)
                           && !this.player.isFallFlying()
                           && !this.player.isAutoSpinAttack()
                           && this.noBlocksAround(this.player);
                        this.player.getLevel().getChunkSource().move(this.player);
                        this.player.doCheckFallDamage(this.player.getY() - var17, param0.isOnGround());
                        this.player.setOnGround(param0.isOnGround());
                        if (var31) {
                           this.player.resetFallDistance();
                        }

                        this.player.checkMovementStatistics(this.player.getX() - var11, this.player.getY() - var13, this.player.getZ() - var15);
                        this.lastGoodX = this.player.getX();
                        this.lastGoodY = this.player.getY();
                        this.lastGoodZ = this.player.getZ();
                     } else {
                        this.teleport(var11, var13, var15, var9, var10);
                        this.player.doCheckFallDamage(this.player.getY() - var17, param0.isOnGround());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isPlayerCollidingWithAnythingNew(LevelReader param0, AABB param1) {
      Iterable<VoxelShape> var3 = param0.getCollisions(this.player, this.player.getBoundingBox().deflate(1.0E-5F));
      VoxelShape var4 = Shapes.create(param1.deflate(1.0E-5F));

      for (VoxelShape var6 : var3) {
         if (!Shapes.joinIsNotEmpty(var6, var4, BooleanOp.AND)) {
            return true;
         }
      }

      return false;
   }

   public void teleport(double param0, double param2, double param4, float param6, float param7) {
      this.teleport(param0, param2, param4, param6, param7, Collections.emptySet());
   }

   public void teleport(double param0, double param2, double param4, float param6, float param7, Set<RelativeMovement> param8) {
      double var10 = param8.contains(RelativeMovement.X) ? this.player.getX() : 0.0;
      double var12 = param8.contains(RelativeMovement.Y) ? this.player.getY() : 0.0;
      double var14 = param8.contains(RelativeMovement.Z) ? this.player.getZ() : 0.0;
      float var16 = param8.contains(RelativeMovement.Y_ROT) ? this.player.getYRot() : 0.0F;
      float var17 = param8.contains(RelativeMovement.X_ROT) ? this.player.getXRot() : 0.0F;
      this.awaitingPositionFromClient = new Vec3(param0, param2, param4);
      if (++this.awaitingTeleport == Integer.MAX_VALUE) {
         this.awaitingTeleport = 0;
      }

      this.awaitingTeleportTime = this.tickCount;
      this.player.absMoveTo(param0, param2, param4, param6, param7);
      this.player
         .connection
         .send(
            new ClientboundPlayerPositionPacket(param0 - var10, param2 - var12, param4 - var14, param6 - var16, param7 - var17, param8, this.awaitingTeleport)
         );
   }

   @Override
   public void handlePlayerAction(ServerboundPlayerActionPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      BlockPos var2 = param0.getPos();
      this.player.resetLastActionTime();
      ServerboundPlayerActionPacket.Action var3 = param0.getAction();
      switch (var3) {
         case SWAP_ITEM_WITH_OFFHAND:
            if (!this.player.isSpectator()) {
               ItemStack var4 = this.player.getItemInHand(InteractionHand.OFF_HAND);
               this.player.setItemInHand(InteractionHand.OFF_HAND, this.player.getItemInHand(InteractionHand.MAIN_HAND));
               this.player.setItemInHand(InteractionHand.MAIN_HAND, var4);
               this.player.stopUsingItem();
            }

            return;
         case DROP_ITEM:
            if (!this.player.isSpectator()) {
               this.player.drop(false);
            }

            return;
         case DROP_ALL_ITEMS:
            if (!this.player.isSpectator()) {
               this.player.drop(true);
            }

            return;
         case RELEASE_USE_ITEM:
            this.player.releaseUsingItem();
            return;
         case START_DESTROY_BLOCK:
         case ABORT_DESTROY_BLOCK:
         case STOP_DESTROY_BLOCK:
            this.player.gameMode.handleBlockBreakAction(var2, var3, param0.getDirection(), this.player.level.getMaxBuildHeight(), param0.getSequence());
            this.player.connection.ackBlockChangesUpTo(param0.getSequence());
            return;
         default:
            throw new IllegalArgumentException("Invalid player action");
      }
   }

   private static boolean wasBlockPlacementAttempt(ServerPlayer param0, ItemStack param1) {
      if (param1.isEmpty()) {
         return false;
      }

      Item var2 = param1.getItem();
      return (var2 instanceof BlockItem || var2 instanceof BucketItem) && !param0.getCooldowns().isOnCooldown(var2);
   }

   @Override
   public void handleUseItemOn(ServerboundUseItemOnPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.connection.ackBlockChangesUpTo(param0.getSequence());
      ServerLevel var2 = this.player.getLevel();
      InteractionHand var3 = param0.getHand();
      ItemStack var4 = this.player.getItemInHand(var3);
      if (var4.isItemEnabled(var2.enabledFeatures())) {
         BlockHitResult var5 = param0.getHitResult();
         Vec3 var6 = var5.getLocation();
         BlockPos var7 = var5.getBlockPos();
         Vec3 var8 = Vec3.atCenterOf(var7);
         if (!(this.player.getEyePosition().distanceToSqr(var8) > MAX_INTERACTION_DISTANCE)) {
            Vec3 var9 = var6.subtract(var8);
            double var10 = 1.0000001;
            if (Math.abs(var9.x()) < 1.0000001 && Math.abs(var9.y()) < 1.0000001 && Math.abs(var9.z()) < 1.0000001) {
               Direction var12 = var5.getDirection();
               this.player.resetLastActionTime();
               int var13 = this.player.level.getMaxBuildHeight();
               if (var7.getY() < var13) {
                  if (this.awaitingPositionFromClient == null
                     && this.player.distanceToSqr(var7.getX() + 0.5, var7.getY() + 0.5, var7.getZ() + 0.5) < 64.0
                     && var2.mayInteract(this.player, var7)) {
                     InteractionResult var14 = this.player.gameMode.useItemOn(this.player, var2, var4, var3, var5);
                     if (var12 == Direction.UP && !var14.consumesAction() && var7.getY() >= var13 - 1 && wasBlockPlacementAttempt(this.player, var4)) {
                        Component var15 = Component.translatable("build.tooHigh", var13 - 1).withStyle(ChatFormatting.RED);
                        this.player.sendSystemMessage(var15, true);
                     } else if (var14.shouldSwing()) {
                        this.player.swing(var3, true);
                     }
                  }
               } else {
                  Component var14 = Component.translatable("build.tooHigh", var13 - 1).withStyle(ChatFormatting.RED);
                  this.player.sendSystemMessage(var14, true);
               }

               this.player.connection.send(new ClientboundBlockUpdatePacket(var2, var7));
               this.player.connection.send(new ClientboundBlockUpdatePacket(var2, var7.relative(var12)));
            } else {
               LOGGER.warn("Rejecting UseItemOnPacket from {}: Location {} too far away from hit block {}.", this.player.getGameProfile().getName(), var6, var7);
            }
         }
      }
   }

   @Override
   public void handleUseItem(ServerboundUseItemPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.ackBlockChangesUpTo(param0.getSequence());
      ServerLevel var2 = this.player.getLevel();
      InteractionHand var3 = param0.getHand();
      ItemStack var4 = this.player.getItemInHand(var3);
      this.player.resetLastActionTime();
      if (!var4.isEmpty() && var4.isItemEnabled(var2.enabledFeatures())) {
         InteractionResult var5 = this.player.gameMode.useItem(this.player, var2, var4, var3);
         if (var5.shouldSwing()) {
            this.player.swing(var3, true);
         }
      }
   }

   @Override
   public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.isSpectator()) {
         for (ServerLevel var3 : this.server.getAllLevels()) {
            Entity var4 = param0.getEntity(var3);
            if (var4 != null) {
               this.player.teleportTo(var3, var4.getX(), var4.getY(), var4.getZ(), var4.getYRot(), var4.getXRot());
               return;
            }
         }
      }
   }

   @Override
   public void handleResourcePackResponse(ServerboundResourcePackPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (param0.getAction() == ServerboundResourcePackPacket.Action.DECLINED && this.server.isResourcePackRequired()) {
         LOGGER.info("Disconnecting {} due to resource pack rejection", this.player.getName());
         this.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
      }
   }

   @Override
   public void handlePaddleBoat(ServerboundPaddleBoatPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.getControlledVehicle() instanceof Boat var3) {
         var3.setPaddleState(param0.getLeft(), param0.getRight());
      }
   }

   @Override
   public void handlePong(ServerboundPongPacket param0) {
   }

   @Override
   public void onDisconnect(Component param0) {
      this.chatMessageChain.close();
      LOGGER.info("{} lost connection: {}", this.player.getName().getString(), param0.getString());
      this.server.invalidateStatus();
      this.server
         .getPlayerList()
         .broadcastSystemMessage(Component.translatable("multiplayer.player.left", this.player.getDisplayName()).withStyle(ChatFormatting.YELLOW), false);
      this.player.disconnect();
      this.server.getPlayerList().remove(this.player);
      this.player.getTextFilter().leave();
      if (this.isSingleplayerOwner()) {
         LOGGER.info("Stopping singleplayer server as player logged out");
         this.server.halt(false);
      }
   }

   public void ackBlockChangesUpTo(int param0) {
      if (param0 < 0) {
         throw new IllegalArgumentException("Expected packet sequence nr >= 0");
      }

      this.ackBlockChangesUpTo = Math.max(param0, this.ackBlockChangesUpTo);
   }

   @Override
   public void send(Packet<?> param0) {
      this.send(param0, null);
   }

   public void send(Packet<?> param0, @Nullable PacketSendListener param1) {
      try {
         this.connection.send(param0, param1);
      } catch (Throwable var3) {
         CrashReport var4 = CrashReport.forThrowable(var3, "Sending packet");
         CrashReportCategory var5 = var4.addCategory("Packet being sent");
         var5.setDetail("Packet class", () -> param0.getClass().getCanonicalName());
         throw new ReportedException(var4);
      }
   }

   @Override
   public void handleSetCarriedItem(ServerboundSetCarriedItemPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (param0.getSlot() >= 0 && param0.getSlot() < Inventory.getSelectionSize()) {
         if (this.player.getInventory().selected != param0.getSlot() && this.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            this.player.stopUsingItem();
         }

         this.player.getInventory().selected = param0.getSlot();
         this.player.resetLastActionTime();
      } else {
         LOGGER.warn("{} tried to set an invalid carried item", this.player.getName().getString());
      }
   }

   @Override
   public void handleChat(ServerboundChatPacket param0) {
      if (isChatMessageIllegal(param0.message())) {
         this.disconnect(Component.translatable("multiplayer.disconnect.illegal_characters"));
      } else {
         Optional<LastSeenMessages> var2 = this.tryHandleChat(param0.message(), param0.timeStamp(), param0.lastSeenMessages());
         if (var2.isPresent()) {
            this.server.submit(() -> {
               PlayerChatMessage l_var3;
               try {
                  l_var3 = this.getSignedMessage(param0, var2.get());
               } catch (SignedMessageChain.DecodeException l_var4) {
                  this.handleMessageDecodeFailure(l_var4);
                  return;
               }

               CompletableFuture<FilteredText> l_var4 = this.filterTextPacket(l_var3.signedContent());
               CompletableFuture<Component> l_var5 = this.server.getChatDecorator().decorate(this.player, l_var3.decoratedContent());
               this.chatMessageChain.append(l2_param3 -> CompletableFuture.allOf(l_var4, l_var5).thenAcceptAsync(l3_param3 -> {
                  PlayerChatMessage l3_var5 = l_var3.withUnsignedContent(l_var5.join()).filter(l_var4.join().mask());
                  this.broadcastChatMessage(l3_var5);
               }, l2_param3));
            });
         }
      }
   }

   @Override
   public void handleChatCommand(ServerboundChatCommandPacket param0) {
      if (isChatMessageIllegal(param0.command())) {
         this.disconnect(Component.translatable("multiplayer.disconnect.illegal_characters"));
      } else {
         Optional<LastSeenMessages> var2 = this.tryHandleChat(param0.command(), param0.timeStamp(), param0.lastSeenMessages());
         if (var2.isPresent()) {
            this.server.submit(() -> {
               this.performChatCommand(param0, var2.get());
               this.detectRateSpam();
            });
         }
      }
   }

   private void performChatCommand(ServerboundChatCommandPacket param0, LastSeenMessages param1) {
      ParseResults<CommandSourceStack> var3 = this.parseCommand(param0.command());

      Map<String, PlayerChatMessage> var4;
      try {
         var4 = this.collectSignedArguments(param0, SignableCommand.of(var3), param1);
      } catch (SignedMessageChain.DecodeException var5) {
         this.handleMessageDecodeFailure(var5);
         return;
      }

      CommandSigningContext var5 = new CommandSigningContext.SignedArguments(var4);
      var3 = Commands.mapSource(var3, l_param1 -> l_param1.withSigningContext(var5));
      this.server.getCommands().performCommand(var3, param0.command());
   }

   private void handleMessageDecodeFailure(SignedMessageChain.DecodeException param0) {
      if (param0.shouldDisconnect()) {
         this.disconnect(param0.getComponent());
      } else {
         this.player.sendSystemMessage(param0.getComponent().copy().withStyle(ChatFormatting.RED));
      }
   }

   private Map<String, PlayerChatMessage> collectSignedArguments(ServerboundChatCommandPacket param0, SignableCommand<?> param1, LastSeenMessages param2) throws SignedMessageChain.DecodeException {
      Map<String, PlayerChatMessage> var4 = new Object2ObjectOpenHashMap<>();

      for (SignableCommand.Argument<?> var6 : param1.arguments()) {
         MessageSignature var7 = param0.argumentSignatures().get(var6.name());
         SignedMessageBody var8 = new SignedMessageBody(var6.value(), param0.timeStamp(), param0.salt(), param2);
         var4.put(var6.name(), this.signedMessageDecoder.unpack(var7, var8));
      }

      return var4;
   }

   private ParseResults<CommandSourceStack> parseCommand(String param0) {
      CommandDispatcher<CommandSourceStack> var2 = this.server.getCommands().getDispatcher();
      return var2.parse(param0, this.player.createCommandSourceStack());
   }

   private Optional<LastSeenMessages> tryHandleChat(String param0, Instant param1, LastSeenMessages.Update param2) {
      if (!this.updateChatOrder(param1)) {
         LOGGER.warn("{} sent out-of-order chat: '{}'", this.player.getName().getString(), param0);
         this.disconnect(Component.translatable("multiplayer.disconnect.out_of_order_chat"));
         return Optional.empty();
      } else {
         Optional<LastSeenMessages> var4 = this.unpackAndApplyLastSeen(param2);
         if (this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
            this.send(new ClientboundSystemChatPacket(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED), false));
            return Optional.empty();
         } else {
            this.player.resetLastActionTime();
            return var4;
         }
      }
   }

   private Optional<LastSeenMessages> unpackAndApplyLastSeen(LastSeenMessages.Update param0) {
      synchronized (this.lastSeenMessages) {
         Optional<LastSeenMessages> var3 = this.lastSeenMessages.applyUpdate(param0);
         if (var3.isEmpty()) {
            LOGGER.warn("Failed to validate message acknowledgements from {}", this.player.getName().getString());
            this.disconnect(CHAT_VALIDATION_FAILED);
         }

         return var3;
      }
   }

   private boolean updateChatOrder(Instant param0) {
      Instant var2;
      do {
         var2 = this.lastChatTimeStamp.get();
         if (param0.isBefore(var2)) {
            return false;
         }
      } while (!this.lastChatTimeStamp.compareAndSet(var2, param0));

      return true;
   }

   private static boolean isChatMessageIllegal(String param0) {
      for (int var1 = 0; var1 < param0.length(); var1++) {
         if (!SharedConstants.isAllowedChatCharacter(param0.charAt(var1))) {
            return true;
         }
      }

      return false;
   }

   private PlayerChatMessage getSignedMessage(ServerboundChatPacket param0, LastSeenMessages param1) throws SignedMessageChain.DecodeException {
      SignedMessageBody var3 = new SignedMessageBody(param0.message(), param0.timeStamp(), param0.salt(), param1);
      return this.signedMessageDecoder.unpack(param0.signature(), var3);
   }

   private void broadcastChatMessage(PlayerChatMessage param0) {
      this.server.getPlayerList().broadcastChatMessage(param0, this.player, ChatType.bind(ChatType.CHAT, this.player));
      this.detectRateSpam();
   }

   private void detectRateSpam() {
      this.chatSpamTickCount += 20;
      if (this.chatSpamTickCount > 200 && !this.server.getPlayerList().isOp(this.player.getGameProfile())) {
         this.disconnect(Component.translatable("disconnect.spam"));
      }
   }

   @Override
   public void handleChatAck(ServerboundChatAckPacket param0) {
      synchronized (this.lastSeenMessages) {
         if (!this.lastSeenMessages.applyOffset(param0.offset())) {
            LOGGER.warn("Failed to validate message acknowledgements from {}", this.player.getName().getString());
            this.disconnect(CHAT_VALIDATION_FAILED);
         }
      }
   }

   @Override
   public void handleAnimate(ServerboundSwingPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.resetLastActionTime();
      this.player.swing(param0.getHand());
   }

   @Override
   public void handlePlayerCommand(ServerboundPlayerCommandPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.resetLastActionTime();
      switch (param0.getAction()) {
         case PRESS_SHIFT_KEY:
            this.player.setShiftKeyDown(true);
            break;
         case RELEASE_SHIFT_KEY:
            this.player.setShiftKeyDown(false);
            break;
         case START_SPRINTING:
            this.player.setSprinting(true);
            break;
         case STOP_SPRINTING:
            this.player.setSprinting(false);
            break;
         case STOP_SLEEPING:
            if (this.player.isSleeping()) {
               this.player.stopSleepInBed(false, true);
               this.awaitingPositionFromClient = this.player.position();
            }
            break;
         case START_RIDING_JUMP:
            if (this.player.getControlledVehicle() instanceof PlayerRideableJumping var2) {
               int var3 = param0.getData();
               if (var2.canJump() && var3 > 0) {
                  var2.handleStartJump(var3);
               }
            }
            break;
         case STOP_RIDING_JUMP:
            if (this.player.getControlledVehicle() instanceof PlayerRideableJumping var2) {
               var2.handleStopJump();
            }
            break;
         case OPEN_INVENTORY:
            if (this.player.getVehicle() instanceof HasCustomInventoryScreen var2) {
               var2.openCustomInventoryScreen(this.player);
            }
            break;
         case START_FALL_FLYING:
            if (!this.player.tryToStartFallFlying()) {
               this.player.stopFallFlying();
            }
            break;
         default:
            throw new IllegalArgumentException("Invalid client command!");
      }
   }

   public void addPendingMessage(PlayerChatMessage param0) {
      MessageSignature var2 = param0.signature();
      if (var2 != null) {
         this.messageSignatureCache.push(param0);
         int var3;
         synchronized (this.lastSeenMessages) {
            this.lastSeenMessages.addPending(var2);
            var3 = this.lastSeenMessages.trackedMessagesCount();
         }

         if (var3 > 4096) {
            this.disconnect(Component.translatable("multiplayer.disconnect.too_many_pending_chats"));
         }
      }
   }

   public void sendPlayerChatMessage(PlayerChatMessage param0, ChatType.Bound param1) {
      this.send(
         new ClientboundPlayerChatPacket(
            param0.link().sender(),
            param0.link().index(),
            param0.signature(),
            param0.signedBody().pack(this.messageSignatureCache),
            param0.unsignedContent(),
            param0.filterMask(),
            param1.toNetwork(this.player.level.registryAccess())
         )
      );
      this.addPendingMessage(param0);
   }

   public void sendDisguisedChatMessage(Component param0, ChatType.Bound param1) {
      this.send(new ClientboundDisguisedChatPacket(param0, param1.toNetwork(this.player.level.registryAccess())));
   }

   public SocketAddress getRemoteAddress() {
      return this.connection.getRemoteAddress();
   }

   @Override
   public void handleInteract(ServerboundInteractPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      final ServerLevel var2 = this.player.getLevel();
      final Entity var3 = param0.getTarget(var2);
      this.player.resetLastActionTime();
      this.player.setShiftKeyDown(param0.isUsingSecondaryAction());
      if (var3 != null) {
         if (!var2.getWorldBorder().isWithinBounds(var3.blockPosition())) {
            return;
         }

         AABB var4 = var3.getBoundingBox();
         if (var4.distanceToSqr(this.player.getEyePosition()) < MAX_INTERACTION_DISTANCE) {
            param0.dispatch(
               new ServerboundInteractPacket.Handler() {
                  private void performInteraction(InteractionHand l_param0, ServerGamePacketListenerImpl.EntityInteraction l_param1) {
                     ItemStack l_var3 = ServerGamePacketListenerImpl.this.player.getItemInHand(l_param0);
                     if (l_var3.isItemEnabled(var2.enabledFeatures())) {
                        ItemStack l_var4 = l_var3.copy();
                        InteractionResult l_var5 = l_param1.run(ServerGamePacketListenerImpl.this.player, var3, l_param0);
                        if (l_var5.consumesAction()) {
                           CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(ServerGamePacketListenerImpl.this.player, l_var4, var3);
                           if (l_var5.shouldSwing()) {
                              ServerGamePacketListenerImpl.this.player.swing(l_param0, true);
                           }
                        }
                     }
                  }

                  @Override
                  public void onInteraction(InteractionHand l_param0) {
                     this.performInteraction(l_param0, Player::interactOn);
                  }

                  @Override
                  public void onInteraction(InteractionHand l_param0, Vec3 l_param1) {
                     this.performInteraction(l_param0, (l2_param1, l2_param2, l2_param3) -> l2_param2.interactAt(l2_param1, l_param1, l2_param3));
                  }

                  @Override
                  public void onAttack() {
                     if (!(var3 instanceof ItemEntity)
                        && !(var3 instanceof ExperienceOrb)
                        && !(var3 instanceof AbstractArrow)
                        && var3 != ServerGamePacketListenerImpl.this.player) {
                        ItemStack l_var1 = ServerGamePacketListenerImpl.this.player.getItemInHand(InteractionHand.MAIN_HAND);
                        if (l_var1.isItemEnabled(var2.enabledFeatures())) {
                           ServerGamePacketListenerImpl.this.player.attack(var3);
                        }
                     } else {
                        ServerGamePacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.invalid_entity_attacked"));
                        ServerGamePacketListenerImpl.LOGGER
                           .warn("Player {} tried to attack an invalid entity", ServerGamePacketListenerImpl.this.player.getName().getString());
                     }
                  }
               }
            );
         }
      }
   }

   @Override
   public void handleClientCommand(ServerboundClientCommandPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.resetLastActionTime();
      ServerboundClientCommandPacket.Action var2 = param0.getAction();
      switch (var2) {
         case PERFORM_RESPAWN:
            if (this.player.wonGame) {
               this.player.wonGame = false;
               this.player = this.server.getPlayerList().respawn(this.player, true);
               CriteriaTriggers.CHANGED_DIMENSION.trigger(this.player, Level.END, Level.OVERWORLD);
            } else {
               if (this.player.getHealth() > 0.0F) {
                  return;
               }

               this.player = this.server.getPlayerList().respawn(this.player, false);
               if (this.server.isHardcore()) {
                  this.player.setGameMode(GameType.SPECTATOR);
                  this.player.getLevel().getGameRules().getRule(GameRules.RULE_SPECTATORSGENERATECHUNKS).set(false, this.server);
               }
            }
            break;
         case REQUEST_STATS:
            this.player.getStats().sendStats(this.player);
      }
   }

   @Override
   public void handleContainerClose(ServerboundContainerClosePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.doCloseContainer();
   }

   @Override
   public void handleContainerClick(ServerboundContainerClickPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.resetLastActionTime();
      if (this.player.containerMenu.containerId == param0.getContainerId()) {
         if (this.player.isSpectator()) {
            this.player.containerMenu.sendAllDataToRemote();
         } else if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
         } else {
            int var2 = param0.getSlotNum();
            if (!this.player.containerMenu.isValidSlotIndex(var2)) {
               LOGGER.debug(
                  "Player {} clicked invalid slot index: {}, available slots: {}", this.player.getName(), var2, this.player.containerMenu.slots.size()
               );
            } else {
               boolean var3 = param0.getStateId() != this.player.containerMenu.getStateId();
               this.player.containerMenu.suppressRemoteUpdates();
               this.player.containerMenu.clicked(var2, param0.getButtonNum(), param0.getClickType(), this.player);

               for (Entry<ItemStack> var5 : Int2ObjectMaps.fastIterable(param0.getChangedSlots())) {
                  this.player.containerMenu.setRemoteSlotNoCopy(var5.getIntKey(), var5.getValue());
               }

               this.player.containerMenu.setRemoteCarried(param0.getCarriedItem());
               this.player.containerMenu.resumeRemoteUpdates();
               if (var3) {
                  this.player.containerMenu.broadcastFullState();
               } else {
                  this.player.containerMenu.broadcastChanges();
               }
            }
         }
      }
   }

   @Override
   public void handlePlaceRecipe(ServerboundPlaceRecipePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.resetLastActionTime();
      if (!this.player.isSpectator() && this.player.containerMenu.containerId == param0.getContainerId() && this.player.containerMenu instanceof RecipeBookMenu
         )
       {
         if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
         } else {
            this.server
               .getRecipeManager()
               .byKey(param0.getRecipe())
               .ifPresent(l_param1 -> ((RecipeBookMenu)this.player.containerMenu).handlePlacement(param0.isShiftDown(), (Recipe<?>)l_param1, this.player));
         }
      }
   }

   @Override
   public void handleContainerButtonClick(ServerboundContainerButtonClickPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.resetLastActionTime();
      if (this.player.containerMenu.containerId == param0.getContainerId() && !this.player.isSpectator()) {
         if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
         } else {
            boolean var2 = this.player.containerMenu.clickMenuButton(this.player, param0.getButtonId());
            if (var2) {
               this.player.containerMenu.broadcastChanges();
            }
         }
      }
   }

   @Override
   public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.gameMode.isCreative()) {
         boolean var2 = param0.getSlotNum() < 0;
         ItemStack var3 = param0.getItem();
         if (!var3.isItemEnabled(this.player.getLevel().enabledFeatures())) {
            return;
         }

         CompoundTag var4 = BlockItem.getBlockEntityData(var3);
         if (!var3.isEmpty() && var4 != null && var4.contains("x") && var4.contains("y") && var4.contains("z")) {
            BlockPos var5 = BlockEntity.getPosFromTag(var4);
            if (this.player.level.isLoaded(var5)) {
               BlockEntity var6 = this.player.level.getBlockEntity(var5);
               if (var6 != null) {
                  var6.saveToItem(var3);
               }
            }
         }

         boolean var5 = param0.getSlotNum() >= 1 && param0.getSlotNum() <= 45;
         boolean var6 = var3.isEmpty() || var3.getDamageValue() >= 0 && var3.getCount() <= 64 && !var3.isEmpty();
         if (var5 && var6) {
            this.player.inventoryMenu.getSlot(param0.getSlotNum()).setByPlayer(var3);
            this.player.inventoryMenu.broadcastChanges();
         } else if (var2 && var6 && this.dropSpamTickCount < 200) {
            this.dropSpamTickCount += 20;
            this.player.drop(var3, true);
         }
      }
   }

   @Override
   public void handleSignUpdate(ServerboundSignUpdatePacket param0) {
      List<String> var2 = Stream.of(param0.getLines()).map(ChatFormatting::stripFormatting).collect(Collectors.toList());
      this.filterTextPacket(var2).thenAcceptAsync(l_param1 -> this.updateSignText(param0, (List<FilteredText>)l_param1), this.server);
   }

   private void updateSignText(ServerboundSignUpdatePacket param0, List<FilteredText> param1) {
      this.player.resetLastActionTime();
      ServerLevel var3 = this.player.getLevel();
      BlockPos var4 = param0.getPos();
      if (var3.hasChunkAt(var4)) {
         BlockState var5 = var3.getBlockState(var4);
         if (!(var3.getBlockEntity(var4) instanceof SignBlockEntity var7)) {
            return;
         }

         if (!var7.isEditable() || !this.player.getUUID().equals(var7.getPlayerWhoMayEdit())) {
            LOGGER.warn("Player {} just tried to change non-editable sign", this.player.getName().getString());
            return;
         }

         for (int var8 = 0; var8 < param1.size(); var8++) {
            FilteredText var9 = param1.get(var8);
            if (this.player.isTextFilteringEnabled()) {
               var7.setMessage(var8, Component.literal(var9.filteredOrEmpty()));
            } else {
               var7.setMessage(var8, Component.literal(var9.raw()), Component.literal(var9.filteredOrEmpty()));
            }
         }

         var7.setChanged();
         var3.sendBlockUpdated(var4, var5, var5, 3);
      }
   }

   @Override
   public void handleKeepAlive(ServerboundKeepAlivePacket param0) {
      if (this.keepAlivePending && param0.getId() == this.keepAliveChallenge) {
         int var2 = (int)(Util.getMillis() - this.keepAliveTime);
         this.player.latency = (this.player.latency * 3 + var2) / 4;
         this.keepAlivePending = false;
      } else if (!this.isSingleplayerOwner()) {
         this.disconnect(Component.translatable("disconnect.timeout"));
      }
   }

   @Override
   public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.getAbilities().flying = param0.isFlying() && this.player.getAbilities().mayfly;
   }

   @Override
   public void handleClientInformation(ServerboundClientInformationPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      this.player.updateOptions(param0);
   }

   @Override
   public void handleCustomPayload(ServerboundCustomPayloadPacket param0) {
   }

   @Override
   public void handleChangeDifficulty(ServerboundChangeDifficultyPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.hasPermissions(2) || this.isSingleplayerOwner()) {
         this.server.setDifficulty(param0.getDifficulty(), false);
      }
   }

   @Override
   public void handleLockDifficulty(ServerboundLockDifficultyPacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      if (this.player.hasPermissions(2) || this.isSingleplayerOwner()) {
         this.server.setDifficultyLocked(param0.isLocked());
      }
   }

   @Override
   public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket param0) {
      PacketUtils.ensureRunningOnSameThread(param0, this, this.player.getLevel());
      RemoteChatSession.Data var2 = param0.chatSession();
      ProfilePublicKey.Data var3 = this.chatSession != null ? this.chatSession.profilePublicKey().data() : null;
      ProfilePublicKey.Data var4 = var2.profilePublicKey();
      if (!Objects.equals(var3, var4)) {
         if (var3 != null && var4.expiresAt().isBefore(var3.expiresAt())) {
            this.disconnect(ProfilePublicKey.EXPIRED_PROFILE_PUBLIC_KEY);
         } else {
            try {
               SignatureValidator var5 = this.server.getServiceSignatureValidator();
               this.resetPlayerChatState(var2.validate(this.player.getGameProfile(), var5, Duration.ZERO));
            } catch (ProfilePublicKey.ValidationException var5) {
               LOGGER.error("Failed to validate profile key: {}", var5.getMessage());
               this.disconnect(var5.getComponent());
            }
         }
      }
   }

   private void resetPlayerChatState(RemoteChatSession param0) {
      this.chatSession = param0;
      this.signedMessageDecoder = param0.createMessageDecoder(this.player.getUUID());
      this.chatMessageChain
         .append(
            l_param1 -> {
               this.player.setChatSession(param0);
               this.server
                  .getPlayerList()
                  .broadcastAll(
                     new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT), List.of(this.player))
                  );
               return CompletableFuture.completedFuture(null);
            }
         );
   }

   @Override
   public ServerPlayer getPlayer() {
      return this.player;
   }

   @FunctionalInterface
   interface EntityInteraction {
      InteractionResult run(ServerPlayer var1, Entity var2, InteractionHand var3);
   }
}
