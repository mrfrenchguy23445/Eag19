package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ServerItemCooldowns;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class ServerPlayer extends Player {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_XZ = 32;
   private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_Y = 10;
   public ServerGamePacketListenerImpl connection;
   public final MinecraftServer server;
   public final ServerPlayerGameMode gameMode;
   private final PlayerAdvancements advancements;
   private final ServerStatsCounter stats;
   private float lastRecordedHealthAndAbsorption = Float.MIN_VALUE;
   private int lastRecordedFoodLevel = Integer.MIN_VALUE;
   private int lastRecordedAirLevel = Integer.MIN_VALUE;
   private int lastRecordedArmor = Integer.MIN_VALUE;
   private int lastRecordedLevel = Integer.MIN_VALUE;
   private int lastRecordedExperience = Integer.MIN_VALUE;
   private float lastSentHealth = -1.0E8F;
   private int lastSentFood = -99999999;
   private boolean lastFoodSaturationZero = true;
   private int lastSentExp = -99999999;
   private int spawnInvulnerableTime = 60;
   private ChatVisiblity chatVisibility = ChatVisiblity.FULL;
   private boolean canChatColor = true;
   private long lastActionTime = Util.getMillis();
   @Nullable
   private Entity camera;
   private boolean isChangingDimension;
   private boolean seenCredits;
   private final ServerRecipeBook recipeBook = new ServerRecipeBook();
   @Nullable
   private Vec3 levitationStartPos;
   private int levitationStartTime;
   private boolean disconnected;
   @Nullable
   private Vec3 startingToFallPosition;
   @Nullable
   private Vec3 enteredNetherPosition;
   @Nullable
   private Vec3 enteredLavaOnVehiclePosition;
   private SectionPos lastSectionPos = SectionPos.of(0, 0, 0);
   private ResourceKey<Level> respawnDimension = Level.OVERWORLD;
   @Nullable
   private BlockPos respawnPosition;
   private boolean respawnForced;
   private float respawnAngle;
   private final TextFilter textFilter;
   private boolean textFilteringEnabled;
   private boolean allowsListing = true;
   private WardenSpawnTracker wardenSpawnTracker = new WardenSpawnTracker(0, 0, 0);
   private final ContainerSynchronizer containerSynchronizer = new ContainerSynchronizer() {
      @Override
      public void sendInitialData(AbstractContainerMenu param0, NonNullList<ItemStack> param1, ItemStack param2, int[] param3) {
         ServerPlayer.this.connection.send(new ClientboundContainerSetContentPacket(param0.containerId, param0.incrementStateId(), param1, param2));

         for (int var5 = 0; var5 < param3.length; var5++) {
            this.broadcastDataValue(param0, var5, param3[var5]);
         }
      }

      @Override
      public void sendSlotChange(AbstractContainerMenu param0, int param1, ItemStack param2) {
         ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(param0.containerId, param0.incrementStateId(), param1, param2));
      }

      @Override
      public void sendCarriedChange(AbstractContainerMenu param0, ItemStack param1) {
         ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(-1, param0.incrementStateId(), -1, param1));
      }

      @Override
      public void sendDataChange(AbstractContainerMenu param0, int param1, int param2) {
         this.broadcastDataValue(param0, param1, param2);
      }

      private void broadcastDataValue(AbstractContainerMenu param0, int param1, int param2) {
         ServerPlayer.this.connection.send(new ClientboundContainerSetDataPacket(param0.containerId, param1, param2));
      }
   };
   private final ContainerListener containerListener = new ContainerListener() {
      @Override
      public void slotChanged(AbstractContainerMenu param0, int param1, ItemStack param2) {
         Slot var4 = param0.getSlot(param1);
         if (!(var4 instanceof ResultSlot)) {
            if (var4.container == ServerPlayer.this.getInventory()) {
               CriteriaTriggers.INVENTORY_CHANGED.trigger(ServerPlayer.this, ServerPlayer.this.getInventory(), param2);
            }
         }
      }

      @Override
      public void dataChanged(AbstractContainerMenu param0, int param1, int param2) {
      }
   };
   @Nullable
   private RemoteChatSession chatSession;
   private int containerCounter;
   public int latency;
   public boolean wonGame;

   public ServerPlayer(MinecraftServer param0, ServerLevel param1, GameProfile param2) {
      super(param1, param1.getSharedSpawnPos(), param1.getSharedSpawnAngle(), param2);
      this.textFilter = param0.createTextFilterForPlayer(this);
      this.gameMode = param0.createGameModeForPlayer(this);
      this.server = param0;
      this.stats = param0.getPlayerList().getPlayerStats(this);
      this.advancements = param0.getPlayerList().getPlayerAdvancements(this);
      this.setMaxUpStep(1.0F);
      this.fudgeSpawnLocation(param1);
   }

   private void fudgeSpawnLocation(ServerLevel param0) {
      BlockPos var2 = param0.getSharedSpawnPos();
      if (param0.dimensionType().hasSkyLight() && param0.getServer().getWorldData().getGameType() != GameType.ADVENTURE) {
         int var3 = Math.max(0, this.server.getSpawnRadius(param0));
         int var4 = Mth.floor(param0.getWorldBorder().getDistanceToBorder(var2.getX(), var2.getZ()));
         if (var4 < var3) {
            var3 = var4;
         }

         if (var4 <= 1) {
            var3 = 1;
         }

         long var5 = var3 * 2 + 1;
         long var7 = var5 * var5;
         int var9 = var7 > 2147483647L ? Integer.MAX_VALUE : (int)var7;
         int var10 = this.getCoprime(var9);
         int var11 = RandomSource.create().nextInt(var9);

         for (int var12 = 0; var12 < var9; var12++) {
            int var13 = (var11 + var10 * var12) % var9;
            int var14 = var13 % (var3 * 2 + 1);
            int var15 = var13 / (var3 * 2 + 1);
            BlockPos var16 = PlayerRespawnLogic.getOverworldRespawnPos(param0, var2.getX() + var14 - var3, var2.getZ() + var15 - var3);
            if (var16 != null) {
               this.moveTo(var16, 0.0F, 0.0F);
               if (param0.noCollision(this)) {
                  break;
               }
            }
         }
      } else {
         this.moveTo(var2, 0.0F, 0.0F);

         while (!param0.noCollision(this) && this.getY() < param0.getMaxBuildHeight() - 1) {
            this.setPos(this.getX(), this.getY() + 1.0, this.getZ());
         }
      }
   }

   private int getCoprime(int param0) {
      return param0 <= 16 ? param0 - 1 : 17;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      if (param0.contains("warden_spawn_tracker", 10)) {
         WardenSpawnTracker.CODEC
            .parse(new Dynamic<>(NbtOps.INSTANCE, param0.get("warden_spawn_tracker")))
            .resultOrPartial(LOGGER::error)
            .ifPresent(l_param0 -> this.wardenSpawnTracker = l_param0);
      }

      if (param0.contains("enteredNetherPosition", 10)) {
         CompoundTag var2 = param0.getCompound("enteredNetherPosition");
         this.enteredNetherPosition = new Vec3(var2.getDouble("x"), var2.getDouble("y"), var2.getDouble("z"));
      }

      this.seenCredits = param0.getBoolean("seenCredits");
      if (param0.contains("recipeBook", 10)) {
         this.recipeBook.fromNbt(param0.getCompound("recipeBook"), this.server.getRecipeManager());
      }

      if (this.isSleeping()) {
         this.stopSleeping();
      }

      if (param0.contains("SpawnX", 99) && param0.contains("SpawnY", 99) && param0.contains("SpawnZ", 99)) {
         this.respawnPosition = new BlockPos(param0.getInt("SpawnX"), param0.getInt("SpawnY"), param0.getInt("SpawnZ"));
         this.respawnForced = param0.getBoolean("SpawnForced");
         this.respawnAngle = param0.getFloat("SpawnAngle");
         if (param0.contains("SpawnDimension")) {
            this.respawnDimension = Level.RESOURCE_KEY_CODEC
               .parse(NbtOps.INSTANCE, param0.get("SpawnDimension"))
               .resultOrPartial(LOGGER::error)
               .orElse(Level.OVERWORLD);
         }
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      WardenSpawnTracker.CODEC
         .encodeStart(NbtOps.INSTANCE, this.wardenSpawnTracker)
         .resultOrPartial(LOGGER::error)
         .ifPresent(l_param1 -> param0.put("warden_spawn_tracker", l_param1));
      this.storeGameTypes(param0);
      param0.putBoolean("seenCredits", this.seenCredits);
      if (this.enteredNetherPosition != null) {
         CompoundTag var2 = new CompoundTag();
         var2.putDouble("x", this.enteredNetherPosition.x);
         var2.putDouble("y", this.enteredNetherPosition.y);
         var2.putDouble("z", this.enteredNetherPosition.z);
         param0.put("enteredNetherPosition", var2);
      }

      Entity var2 = this.getRootVehicle();
      Entity var3 = this.getVehicle();
      if (var3 != null && var2 != this && var2.hasExactlyOnePlayerPassenger()) {
         CompoundTag var4 = new CompoundTag();
         CompoundTag var5 = new CompoundTag();
         var2.save(var5);
         var4.putUUID("Attach", var3.getUUID());
         var4.put("Entity", var5);
         param0.put("RootVehicle", var4);
      }

      param0.put("recipeBook", this.recipeBook.toNbt());
      param0.putString("Dimension", this.level.dimension().location().toString());
      if (this.respawnPosition != null) {
         param0.putInt("SpawnX", this.respawnPosition.getX());
         param0.putInt("SpawnY", this.respawnPosition.getY());
         param0.putInt("SpawnZ", this.respawnPosition.getZ());
         param0.putBoolean("SpawnForced", this.respawnForced);
         param0.putFloat("SpawnAngle", this.respawnAngle);
         ResourceLocation.CODEC
            .encodeStart(NbtOps.INSTANCE, this.respawnDimension.location())
            .resultOrPartial(LOGGER::error)
            .ifPresent(l_param1 -> param0.put("SpawnDimension", l_param1));
      }
   }

   public void setExperiencePoints(int param0) {
      float var2 = this.getXpNeededForNextLevel();
      float var3 = (var2 - 1.0F) / var2;
      this.experienceProgress = Mth.clamp(param0 / var2, 0.0F, var3);
      this.lastSentExp = -1;
   }

   public void setExperienceLevels(int param0) {
      this.experienceLevel = param0;
      this.lastSentExp = -1;
   }

   @Override
   public void giveExperienceLevels(int param0) {
      super.giveExperienceLevels(param0);
      this.lastSentExp = -1;
   }

   @Override
   public void onEnchantmentPerformed(ItemStack param0, int param1) {
      super.onEnchantmentPerformed(param0, param1);
      this.lastSentExp = -1;
   }

   private void initMenu(AbstractContainerMenu param0) {
      param0.addSlotListener(this.containerListener);
      param0.setSynchronizer(this.containerSynchronizer);
   }

   public void initInventoryMenu() {
      this.initMenu(this.inventoryMenu);
   }

   @Override
   public void onEnterCombat() {
      super.onEnterCombat();
      this.connection.send(new ClientboundPlayerCombatEnterPacket());
   }

   @Override
   public void onLeaveCombat() {
      super.onLeaveCombat();
      this.connection.send(new ClientboundPlayerCombatEndPacket(this.getCombatTracker()));
   }

   @Override
   protected void onInsideBlock(BlockState param0) {
      CriteriaTriggers.ENTER_BLOCK.trigger(this, param0);
   }

   @Override
   protected ItemCooldowns createItemCooldowns() {
      return new ServerItemCooldowns(this);
   }

   @Override
   public void tick() {
      this.gameMode.tick();
      this.wardenSpawnTracker.tick();
      this.spawnInvulnerableTime--;
      if (this.invulnerableTime > 0) {
         this.invulnerableTime--;
      }

      this.containerMenu.broadcastChanges();
      if (!this.level.isClientSide && !this.containerMenu.stillValid(this)) {
         this.closeContainer();
         this.containerMenu = this.inventoryMenu;
      }

      Entity var1 = this.getCamera();
      if (var1 != this) {
         if (var1.isAlive()) {
            this.absMoveTo(var1.getX(), var1.getY(), var1.getZ(), var1.getYRot(), var1.getXRot());
            this.getLevel().getChunkSource().move(this);
            if (this.wantsToStopRiding()) {
               this.setCamera(this);
            }
         } else {
            this.setCamera(this);
         }
      }

      CriteriaTriggers.TICK.trigger(this);
      if (this.levitationStartPos != null) {
         CriteriaTriggers.LEVITATION.trigger(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
      }

      this.trackStartFallingPosition();
      this.trackEnteredOrExitedLavaOnVehicle();
      this.advancements.flushDirty(this);
   }

   public void doTick() {
      try {
         if (!this.isSpectator() || !this.touchingUnloadedChunk()) {
            super.tick();
         }

         for (int var1 = 0; var1 < this.getInventory().getContainerSize(); var1++) {
            ItemStack var2 = this.getInventory().getItem(var1);
            if (var2.getItem().isComplex()) {
               Packet<?> var3 = ((ComplexItem)var2.getItem()).getUpdatePacket(var2, this.level, this);
               if (var3 != null) {
                  this.connection.send(var3);
               }
            }
         }

         if (this.getHealth() != this.lastSentHealth
            || this.lastSentFood != this.foodData.getFoodLevel()
            || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
            this.connection.send(new ClientboundSetHealthPacket(this.getHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel()));
            this.lastSentHealth = this.getHealth();
            this.lastSentFood = this.foodData.getFoodLevel();
            this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
         }

         if (this.getHealth() + this.getAbsorptionAmount() != this.lastRecordedHealthAndAbsorption) {
            this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionAmount();
            this.updateScoreForCriteria(ObjectiveCriteria.HEALTH, Mth.ceil(this.lastRecordedHealthAndAbsorption));
         }

         if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
            this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
            this.updateScoreForCriteria(ObjectiveCriteria.FOOD, Mth.ceil(this.lastRecordedFoodLevel));
         }

         if (this.getAirSupply() != this.lastRecordedAirLevel) {
            this.lastRecordedAirLevel = this.getAirSupply();
            this.updateScoreForCriteria(ObjectiveCriteria.AIR, Mth.ceil(this.lastRecordedAirLevel));
         }

         if (this.getArmorValue() != this.lastRecordedArmor) {
            this.lastRecordedArmor = this.getArmorValue();
            this.updateScoreForCriteria(ObjectiveCriteria.ARMOR, Mth.ceil(this.lastRecordedArmor));
         }

         if (this.totalExperience != this.lastRecordedExperience) {
            this.lastRecordedExperience = this.totalExperience;
            this.updateScoreForCriteria(ObjectiveCriteria.EXPERIENCE, Mth.ceil(this.lastRecordedExperience));
         }

         if (this.experienceLevel != this.lastRecordedLevel) {
            this.lastRecordedLevel = this.experienceLevel;
            this.updateScoreForCriteria(ObjectiveCriteria.LEVEL, Mth.ceil(this.lastRecordedLevel));
         }

         if (this.totalExperience != this.lastSentExp) {
            this.lastSentExp = this.totalExperience;
            this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
         }

         if (this.tickCount % 20 == 0) {
            CriteriaTriggers.LOCATION.trigger(this);
         }
      } catch (Throwable var1) {
         CrashReport var2 = CrashReport.forThrowable(var1, "Ticking player");
         CrashReportCategory var3 = var2.addCategory("Player being ticked");
         this.fillCrashReportCategory(var3);
         throw new ReportedException(var2);
      }
   }

   @Override
   public void resetFallDistance() {
      if (this.getHealth() > 0.0F && this.startingToFallPosition != null) {
         CriteriaTriggers.FALL_FROM_HEIGHT.trigger(this, this.startingToFallPosition);
      }

      this.startingToFallPosition = null;
      super.resetFallDistance();
   }

   public void trackStartFallingPosition() {
      if (this.fallDistance > 0.0F && this.startingToFallPosition == null) {
         this.startingToFallPosition = this.position();
      }
   }

   public void trackEnteredOrExitedLavaOnVehicle() {
      if (this.getVehicle() != null && this.getVehicle().isInLava()) {
         if (this.enteredLavaOnVehiclePosition == null) {
            this.enteredLavaOnVehiclePosition = this.position();
         } else {
            CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.trigger(this, this.enteredLavaOnVehiclePosition);
         }
      }

      if (this.enteredLavaOnVehiclePosition != null && (this.getVehicle() == null || !this.getVehicle().isInLava())) {
         this.enteredLavaOnVehiclePosition = null;
      }
   }

   private void updateScoreForCriteria(ObjectiveCriteria param0, int param1) {
      this.getScoreboard().forAllObjectives(param0, this.getScoreboardName(), l_param1 -> l_param1.setScore(param1));
   }

   @Override
   public void die(DamageSource param0) {
      this.gameEvent(GameEvent.ENTITY_DIE);
      boolean var2 = this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
      if (var2) {
         Component var3 = this.getCombatTracker().getDeathMessage();
         this.connection
            .send(
               new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), var3),
               PacketSendListener.exceptionallySend(
                  () -> {
                     int l_var2 = 256;
                     String l_var3 = var3.getString(256);
                     Component l_var4 = Component.translatable("death.attack.message_too_long", Component.literal(l_var3).withStyle(ChatFormatting.YELLOW));
                     Component l_var5 = Component.translatable("death.attack.even_more_magic", this.getDisplayName())
                        .withStyle(l2_param1 -> l2_param1.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, l_var4)));
                     return new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), l_var5);
                  }
               )
            );
         Team var4 = this.getTeam();
         if (var4 == null || var4.getDeathMessageVisibility() == Team.Visibility.ALWAYS) {
            this.server.getPlayerList().broadcastSystemMessage(var3, false);
         } else if (var4.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
            this.server.getPlayerList().broadcastSystemToTeam(this, var3);
         } else if (var4.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
            this.server.getPlayerList().broadcastSystemToAllExceptTeam(this, var3);
         }
      } else {
         this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), CommonComponents.EMPTY));
      }

      this.removeEntitiesOnShoulder();
      if (this.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
         this.tellNeutralMobsThatIDied();
      }

      if (!this.isSpectator()) {
         this.dropAllDeathLoot(param0);
      }

      this.getScoreboard().forAllObjectives(ObjectiveCriteria.DEATH_COUNT, this.getScoreboardName(), Score::increment);
      LivingEntity var3 = this.getKillCredit();
      if (var3 != null) {
         this.awardStat(Stats.ENTITY_KILLED_BY.get(var3.getType()));
         var3.awardKillScore(this, this.deathScore, param0);
         this.createWitherRose(var3);
      }

      this.level.broadcastEntityEvent(this, (byte)3);
      this.awardStat(Stats.DEATHS);
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
      this.clearFire();
      this.setTicksFrozen(0);
      this.setSharedFlagOnFire(false);
      this.getCombatTracker().recheckStatus();
      this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level.dimension(), this.blockPosition())));
   }

   private void tellNeutralMobsThatIDied() {
      AABB var1 = new AABB(this.blockPosition()).inflate(32.0, 10.0, 32.0);
      this.level
         .getEntitiesOfClass(Mob.class, var1, EntitySelector.NO_SPECTATORS)
         .stream()
         .filter(l_param0 -> l_param0 instanceof NeutralMob)
         .forEach(l_param0 -> ((NeutralMob)l_param0).playerDied(this));
   }

   @Override
   public void awardKillScore(Entity param0, int param1, DamageSource param2) {
      if (param0 != this) {
         super.awardKillScore(param0, param1, param2);
         this.increaseScore(param1);
         String var4 = this.getScoreboardName();
         String var5 = param0.getScoreboardName();
         this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_ALL, var4, Score::increment);
         if (param0 instanceof Player) {
            this.awardStat(Stats.PLAYER_KILLS);
            this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_PLAYERS, var4, Score::increment);
         } else {
            this.awardStat(Stats.MOB_KILLS);
         }

         this.handleTeamKill(var4, var5, ObjectiveCriteria.TEAM_KILL);
         this.handleTeamKill(var5, var4, ObjectiveCriteria.KILLED_BY_TEAM);
         CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(this, param0, param2);
      }
   }

   private void handleTeamKill(String param0, String param1, ObjectiveCriteria[] param2) {
      PlayerTeam var4 = this.getScoreboard().getPlayersTeam(param1);
      if (var4 != null) {
         int var5 = var4.getColor().getId();
         if (var5 >= 0 && var5 < param2.length) {
            this.getScoreboard().forAllObjectives(param2[var5], param0, Score::increment);
         }
      }
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.isInvulnerableTo(param0)) {
         return false;
      } else {
         boolean var3 = this.server.isDedicatedServer() && this.isPvpAllowed() && param0.is(DamageTypeTags.IS_FALL);
         if (!var3 && this.spawnInvulnerableTime > 0 && !param0.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
         } else {
            Entity var4 = param0.getEntity();
            if (var4 instanceof Player var5 && !this.canHarmPlayer(var5)) {
               return false;
            } else {
               return var4 instanceof AbstractArrow var5 && var5.getOwner() instanceof Player var7 && !this.canHarmPlayer(var7)
                  ? false
                  : super.hurt(param0, param1);
            }
         }
      }
   }

   @Override
   public boolean canHarmPlayer(Player param0) {
      return !this.isPvpAllowed() ? false : super.canHarmPlayer(param0);
   }

   private boolean isPvpAllowed() {
      return this.server.isPvpAllowed();
   }

   @Nullable
   @Override
   protected PortalInfo findDimensionEntryPoint(ServerLevel param0) {
      PortalInfo var2 = super.findDimensionEntryPoint(param0);
      if (var2 != null && this.level.dimension() == Level.OVERWORLD && param0.dimension() == Level.END) {
         Vec3 var3 = var2.pos.add(0.0, -1.0, 0.0);
         return new PortalInfo(var3, Vec3.ZERO, 90.0F, 0.0F);
      } else {
         return var2;
      }
   }

   @Nullable
   @Override
   public Entity changeDimension(ServerLevel param0) {
      this.isChangingDimension = true;
      ServerLevel var2 = this.getLevel();
      ResourceKey<Level> var3 = var2.dimension();
      if (var3 == Level.END && param0.dimension() == Level.OVERWORLD) {
         this.unRide();
         this.getLevel().removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
         if (!this.wonGame) {
            this.wonGame = true;
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, this.seenCredits ? 0.0F : 1.0F));
            this.seenCredits = true;
         }

         return this;
      } else {
         LevelData var4 = param0.getLevelData();
         this.connection
            .send(
               new ClientboundRespawnPacket(
                  param0.dimensionTypeId(),
                  param0.dimension(),
                  BiomeManager.obfuscateSeed(param0.getSeed()),
                  this.gameMode.getGameModeForPlayer(),
                  this.gameMode.getPreviousGameModeForPlayer(),
                  param0.isDebug(),
                  param0.isFlat(),
                  (byte)3,
                  this.getLastDeathLocation()
               )
            );
         this.connection.send(new ClientboundChangeDifficultyPacket(var4.getDifficulty(), var4.isDifficultyLocked()));
         PlayerList var5 = this.server.getPlayerList();
         var5.sendPlayerPermissionLevel(this);
         var2.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
         this.unsetRemoved();
         PortalInfo var6 = this.findDimensionEntryPoint(param0);
         if (var6 != null) {
            var2.getProfiler().push("moving");
            if (var3 == Level.OVERWORLD && param0.dimension() == Level.NETHER) {
               this.enteredNetherPosition = this.position();
            } else if (param0.dimension() == Level.END) {
               this.createEndPlatform(param0, BlockPos.containing(var6.pos));
            }

            var2.getProfiler().pop();
            var2.getProfiler().push("placing");
            this.setLevel(param0);
            this.connection.teleport(var6.pos.x, var6.pos.y, var6.pos.z, var6.yRot, var6.xRot);
            this.connection.resetPosition();
            param0.addDuringPortalTeleport(this);
            var2.getProfiler().pop();
            this.triggerDimensionChangeTriggers(var2);
            this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
            var5.sendLevelInfo(this, param0);
            var5.sendAllPlayerInfo(this);

            for (MobEffectInstance var8 : this.getActiveEffects()) {
               this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), var8));
            }

            this.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
            this.lastSentExp = -1;
            this.lastSentHealth = -1.0F;
            this.lastSentFood = -1;
         }

         return this;
      }
   }

   private void createEndPlatform(ServerLevel param0, BlockPos param1) {
      BlockPos.MutableBlockPos var3 = param1.mutable();

      for (int var4 = -2; var4 <= 2; var4++) {
         for (int var5 = -2; var5 <= 2; var5++) {
            for (int var6 = -1; var6 < 3; var6++) {
               BlockState var7 = var6 == -1 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
               param0.setBlockAndUpdate(var3.set(param1).move(var5, var6, var4), var7);
            }
         }
      }
   }

   @Override
   protected Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel param0, BlockPos param1, boolean param2, WorldBorder param3) {
      Optional<BlockUtil.FoundRectangle> var5 = super.getExitPortal(param0, param1, param2, param3);
      if (var5.isPresent()) {
         return var5;
      }

      Direction.Axis var6 = this.level.getBlockState(this.portalEntrancePos).getOptionalValue(NetherPortalBlock.AXIS).orElse(Direction.Axis.X);
      Optional<BlockUtil.FoundRectangle> var7 = param0.getPortalForcer().createPortal(param1, var6);
      if (!var7.isPresent()) {
         LOGGER.error("Unable to create a portal, likely target out of worldborder");
      }

      return var7;
   }

   private void triggerDimensionChangeTriggers(ServerLevel param0) {
      ResourceKey<Level> var2 = param0.dimension();
      ResourceKey<Level> var3 = this.level.dimension();
      CriteriaTriggers.CHANGED_DIMENSION.trigger(this, var2, var3);
      if (var2 == Level.NETHER && var3 == Level.OVERWORLD && this.enteredNetherPosition != null) {
         CriteriaTriggers.NETHER_TRAVEL.trigger(this, this.enteredNetherPosition);
      }

      if (var3 != Level.NETHER) {
         this.enteredNetherPosition = null;
      }
   }

   @Override
   public boolean broadcastToPlayer(ServerPlayer param0) {
      if (param0.isSpectator()) {
         return this.getCamera() == this;
      } else {
         return this.isSpectator() ? false : super.broadcastToPlayer(param0);
      }
   }

   @Override
   public void take(Entity param0, int param1) {
      super.take(param0, param1);
      this.containerMenu.broadcastChanges();
   }

   @Override
   public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos param0) {
      Direction var2 = this.level.getBlockState(param0).getValue(HorizontalDirectionalBlock.FACING);
      if (this.isSleeping() || !this.isAlive()) {
         return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
      }

      if (!this.level.dimensionType().natural()) {
         return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
      }

      if (!this.bedInRange(param0, var2)) {
         return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
      }

      if (this.bedBlocked(param0, var2)) {
         return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
      }

      this.setRespawnPosition(this.level.dimension(), param0, this.getYRot(), false, true);
      if (this.level.isDay()) {
         return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
      }

      if (!this.isCreative()) {
         double var3 = 8.0;
         double var5 = 5.0;
         Vec3 var7 = Vec3.atBottomCenterOf(param0);
         List<Monster> var8 = this.level
            .getEntitiesOfClass(
               Monster.class,
               new AABB(var7.x() - 8.0, var7.y() - 5.0, var7.z() - 8.0, var7.x() + 8.0, var7.y() + 5.0, var7.z() + 8.0),
               l_param0 -> l_param0.isPreventingPlayerRest(this)
            );
         if (!var8.isEmpty()) {
            return Either.left(Player.BedSleepingProblem.NOT_SAFE);
         }
      }

      Either<Player.BedSleepingProblem, Unit> var3 = super.startSleepInBed(param0).ifRight(l_param0 -> {
         this.awardStat(Stats.SLEEP_IN_BED);
         CriteriaTriggers.SLEPT_IN_BED.trigger(this);
      });
      if (!this.getLevel().canSleepThroughNights()) {
         this.displayClientMessage(Component.translatable("sleep.not_possible"), true);
      }

      ((ServerLevel)this.level).updateSleepingPlayerList();
      return var3;
   }

   @Override
   public void startSleeping(BlockPos param0) {
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
      super.startSleeping(param0);
   }

   private boolean bedInRange(BlockPos param0, Direction param1) {
      return this.isReachableBedBlock(param0) || this.isReachableBedBlock(param0.relative(param1.getOpposite()));
   }

   private boolean isReachableBedBlock(BlockPos param0) {
      Vec3 var2 = Vec3.atBottomCenterOf(param0);
      return Math.abs(this.getX() - var2.x()) <= 3.0 && Math.abs(this.getY() - var2.y()) <= 2.0 && Math.abs(this.getZ() - var2.z()) <= 3.0;
   }

   private boolean bedBlocked(BlockPos param0, Direction param1) {
      BlockPos var3 = param0.above();
      return !this.freeAt(var3) || !this.freeAt(var3.relative(param1.getOpposite()));
   }

   @Override
   public void stopSleepInBed(boolean param0, boolean param1) {
      if (this.isSleeping()) {
         this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(this, 2));
      }

      super.stopSleepInBed(param0, param1);
      if (this.connection != null) {
         this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
      }
   }

   @Override
   public void dismountTo(double param0, double param2, double param4) {
      this.removeVehicle();
      this.setPos(param0, param2, param4);
   }

   @Override
   public boolean isInvulnerableTo(DamageSource param0) {
      return super.isInvulnerableTo(param0) || this.isChangingDimension();
   }

   @Override
   protected void checkFallDamage(double param0, boolean param2, BlockState param3, BlockPos param4) {
   }

   @Override
   protected void onChangedBlock(BlockPos param0) {
      if (!this.isSpectator()) {
         super.onChangedBlock(param0);
      }
   }

   public void doCheckFallDamage(double param0, boolean param2) {
      if (!this.touchingUnloadedChunk()) {
         BlockPos var4 = this.getOnPosLegacy();
         super.checkFallDamage(param0, param2, this.level.getBlockState(var4), var4);
      }
   }

   @Override
   public void openTextEdit(SignBlockEntity param0) {
      param0.setAllowedPlayerEditor(this.getUUID());
      this.connection.send(new ClientboundBlockUpdatePacket(this.level, param0.getBlockPos()));
      this.connection.send(new ClientboundOpenSignEditorPacket(param0.getBlockPos()));
   }

   private void nextContainerCounter() {
      this.containerCounter = this.containerCounter % 100 + 1;
   }

   @Override
   public OptionalInt openMenu(@Nullable MenuProvider param0) {
      if (param0 == null) {
         return OptionalInt.empty();
      }

      if (this.containerMenu != this.inventoryMenu) {
         this.closeContainer();
      }

      this.nextContainerCounter();
      AbstractContainerMenu var2 = param0.createMenu(this.containerCounter, this.getInventory(), this);
      if (var2 == null) {
         if (this.isSpectator()) {
            this.displayClientMessage(Component.translatable("container.spectatorCantOpen").withStyle(ChatFormatting.RED), true);
         }

         return OptionalInt.empty();
      } else {
         this.connection.send(new ClientboundOpenScreenPacket(var2.containerId, var2.getType(), param0.getDisplayName()));
         this.initMenu(var2);
         this.containerMenu = var2;
         return OptionalInt.of(this.containerCounter);
      }
   }

   @Override
   public void sendMerchantOffers(int param0, MerchantOffers param1, int param2, int param3, boolean param4, boolean param5) {
      this.connection.send(new ClientboundMerchantOffersPacket(param0, param1, param2, param3, param4, param5));
   }

   @Override
   public void openHorseInventory(AbstractHorse param0, Container param1) {
      if (this.containerMenu != this.inventoryMenu) {
         this.closeContainer();
      }

      this.nextContainerCounter();
      this.connection.send(new ClientboundHorseScreenOpenPacket(this.containerCounter, param1.getContainerSize(), param0.getId()));
      this.containerMenu = new HorseInventoryMenu(this.containerCounter, this.getInventory(), param1, param0);
      this.initMenu(this.containerMenu);
   }

   @Override
   public void openItemGui(ItemStack param0, InteractionHand param1) {
      if (param0.is(Items.WRITTEN_BOOK)) {
         if (WrittenBookItem.resolveBookComponents(param0, this.createCommandSourceStack(), this)) {
            this.containerMenu.broadcastChanges();
         }

         this.connection.send(new ClientboundOpenBookPacket(param1));
      }
   }

   @Override
   public void openCommandBlock(CommandBlockEntity param0) {
      this.connection.send(ClientboundBlockEntityDataPacket.create(param0, BlockEntity::saveWithoutMetadata));
   }

   @Override
   public void closeContainer() {
      this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
      this.doCloseContainer();
   }

   @Override
   public void doCloseContainer() {
      this.containerMenu.removed(this);
      this.inventoryMenu.transferState(this.containerMenu);
      this.containerMenu = this.inventoryMenu;
   }

   public void setPlayerInput(float param0, float param1, boolean param2, boolean param3) {
      if (this.isPassenger()) {
         if (param0 >= -1.0F && param0 <= 1.0F) {
            this.xxa = param0;
         }

         if (param1 >= -1.0F && param1 <= 1.0F) {
            this.zza = param1;
         }

         this.jumping = param2;
         this.setShiftKeyDown(param3);
      }
   }

   @Override
   public void awardStat(Stat<?> param0, int param1) {
      this.stats.increment(this, param0, param1);
      this.getScoreboard().forAllObjectives(param0, this.getScoreboardName(), l_param1 -> l_param1.add(param1));
   }

   @Override
   public void resetStat(Stat<?> param0) {
      this.stats.setValue(this, param0, 0);
      this.getScoreboard().forAllObjectives(param0, this.getScoreboardName(), Score::reset);
   }

   @Override
   public int awardRecipes(Collection<Recipe<?>> param0) {
      return this.recipeBook.addRecipes(param0, this);
   }

   @Override
   public void awardRecipesByKey(ResourceLocation[] param0) {
      List<Recipe<?>> var2 = Lists.newArrayList();

      for (ResourceLocation var6 : param0) {
         this.server.getRecipeManager().byKey(var6).ifPresent(var2::add);
      }

      this.awardRecipes(var2);
   }

   @Override
   public int resetRecipes(Collection<Recipe<?>> param0) {
      return this.recipeBook.removeRecipes(param0, this);
   }

   @Override
   public void giveExperiencePoints(int param0) {
      super.giveExperiencePoints(param0);
      this.lastSentExp = -1;
   }

   public void disconnect() {
      this.disconnected = true;
      this.ejectPassengers();
      if (this.isSleeping()) {
         this.stopSleepInBed(true, false);
      }
   }

   public boolean hasDisconnected() {
      return this.disconnected;
   }

   public void resetSentInfo() {
      this.lastSentHealth = -1.0E8F;
   }

   @Override
   public void displayClientMessage(Component param0, boolean param1) {
      this.sendSystemMessage(param0, param1);
   }

   @Override
   protected void completeUsingItem() {
      if (!this.useItem.isEmpty() && this.isUsingItem()) {
         this.connection.send(new ClientboundEntityEventPacket(this, (byte)9));
         super.completeUsingItem();
      }
   }

   @Override
   public void lookAt(EntityAnchorArgument.Anchor param0, Vec3 param1) {
      super.lookAt(param0, param1);
      this.connection.send(new ClientboundPlayerLookAtPacket(param0, param1.x, param1.y, param1.z));
   }

   public void lookAt(EntityAnchorArgument.Anchor param0, Entity param1, EntityAnchorArgument.Anchor param2) {
      Vec3 var4 = param2.apply(param1);
      super.lookAt(param0, var4);
      this.connection.send(new ClientboundPlayerLookAtPacket(param0, param1, param2));
   }

   public void restoreFrom(ServerPlayer param0, boolean param1) {
      this.wardenSpawnTracker = param0.wardenSpawnTracker;
      this.textFilteringEnabled = param0.textFilteringEnabled;
      this.chatSession = param0.chatSession;
      this.gameMode.setGameModeForPlayer(param0.gameMode.getGameModeForPlayer(), param0.gameMode.getPreviousGameModeForPlayer());
      this.onUpdateAbilities();
      if (param1) {
         this.getInventory().replaceWith(param0.getInventory());
         this.setHealth(param0.getHealth());
         this.foodData = param0.foodData;
         this.experienceLevel = param0.experienceLevel;
         this.totalExperience = param0.totalExperience;
         this.experienceProgress = param0.experienceProgress;
         this.setScore(param0.getScore());
         this.portalEntrancePos = param0.portalEntrancePos;
      } else if (this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || param0.isSpectator()) {
         this.getInventory().replaceWith(param0.getInventory());
         this.experienceLevel = param0.experienceLevel;
         this.totalExperience = param0.totalExperience;
         this.experienceProgress = param0.experienceProgress;
         this.setScore(param0.getScore());
      }

      this.enchantmentSeed = param0.enchantmentSeed;
      this.enderChestInventory = param0.enderChestInventory;
      this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, param0.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
      this.lastSentExp = -1;
      this.lastSentHealth = -1.0F;
      this.lastSentFood = -1;
      this.recipeBook.copyOverData(param0.recipeBook);
      this.seenCredits = param0.seenCredits;
      this.enteredNetherPosition = param0.enteredNetherPosition;
      this.setShoulderEntityLeft(param0.getShoulderEntityLeft());
      this.setShoulderEntityRight(param0.getShoulderEntityRight());
      this.setLastDeathLocation(param0.getLastDeathLocation());
   }

   @Override
   protected void onEffectAdded(MobEffectInstance param0, @Nullable Entity param1) {
      super.onEffectAdded(param0, param1);
      this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), param0));
      if (param0.getEffect() == MobEffects.LEVITATION) {
         this.levitationStartTime = this.tickCount;
         this.levitationStartPos = this.position();
      }

      CriteriaTriggers.EFFECTS_CHANGED.trigger(this, param1);
   }

   @Override
   protected void onEffectUpdated(MobEffectInstance param0, boolean param1, @Nullable Entity param2) {
      super.onEffectUpdated(param0, param1, param2);
      this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), param0));
      CriteriaTriggers.EFFECTS_CHANGED.trigger(this, param2);
   }

   @Override
   protected void onEffectRemoved(MobEffectInstance param0) {
      super.onEffectRemoved(param0);
      this.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), param0.getEffect()));
      if (param0.getEffect() == MobEffects.LEVITATION) {
         this.levitationStartPos = null;
      }

      CriteriaTriggers.EFFECTS_CHANGED.trigger(this, null);
   }

   @Override
   public void teleportTo(double param0, double param2, double param4) {
      this.connection.teleport(param0, param2, param4, this.getYRot(), this.getXRot(), RelativeMovement.ROTATION);
   }

   @Override
   public void teleportRelative(double param0, double param2, double param4) {
      this.connection.teleport(this.getX() + param0, this.getY() + param2, this.getZ() + param4, this.getYRot(), this.getXRot(), RelativeMovement.ALL);
   }

   @Override
   public boolean teleportTo(ServerLevel param0, double param1, double param3, double param5, Set<RelativeMovement> param7, float param8, float param9) {
      ChunkPos var11 = new ChunkPos(BlockPos.containing(param1, param3, param5));
      param0.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, var11, 1, this.getId());
      this.stopRiding();
      if (this.isSleeping()) {
         this.stopSleepInBed(true, true);
      }

      if (param0 == this.level) {
         this.connection.teleport(param1, param3, param5, param8, param9, param7);
      } else {
         this.teleportTo(param0, param1, param3, param5, param8, param9);
      }

      this.setYHeadRot(param8);
      return true;
   }

   @Override
   public void moveTo(double param0, double param2, double param4) {
      this.teleportTo(param0, param2, param4);
      this.connection.resetPosition();
   }

   @Override
   public void crit(Entity param0) {
      this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(param0, 4));
   }

   @Override
   public void magicCrit(Entity param0) {
      this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(param0, 5));
   }

   @Override
   public void onUpdateAbilities() {
      if (this.connection != null) {
         this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
         this.updateInvisibilityStatus();
      }
   }

   public ServerLevel getLevel() {
      return (ServerLevel)this.level;
   }

   public boolean setGameMode(GameType param0) {
      if (!this.gameMode.changeGameModeForPlayer(param0)) {
         return false;
      }

      this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, param0.getId()));
      if (param0 == GameType.SPECTATOR) {
         this.removeEntitiesOnShoulder();
         this.stopRiding();
      } else {
         this.setCamera(this);
      }

      this.onUpdateAbilities();
      this.updateEffectVisibility();
      return true;
   }

   @Override
   public boolean isSpectator() {
      return this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
   }

   @Override
   public boolean isCreative() {
      return this.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
   }

   @Override
   public void sendSystemMessage(Component param0) {
      this.sendSystemMessage(param0, false);
   }

   public void sendSystemMessage(Component param0, boolean param1) {
      if (this.acceptsSystemMessages(param1)) {
         this.connection.send(new ClientboundSystemChatPacket(param0, param1), PacketSendListener.exceptionallySend(() -> {
            if (this.acceptsSystemMessages(false)) {
               int l_var2 = 256;
               String l_var3 = param0.getString(256);
               Component l_var4 = Component.literal(l_var3).withStyle(ChatFormatting.YELLOW);
               return new ClientboundSystemChatPacket(Component.translatable("multiplayer.message_not_delivered", l_var4).withStyle(ChatFormatting.RED), false);
            } else {
               return null;
            }
         }));
      }
   }

   public void sendChatMessage(OutgoingChatMessage param0, boolean param1, ChatType.Bound param2) {
      if (this.acceptsChatMessages()) {
         param0.sendToPlayer(this, param1, param2);
      }
   }

   public String getIpAddress() {
      return this.connection.getRemoteAddress() instanceof InetSocketAddress var2 ? InetAddresses.toAddrString(var2.getAddress()) : "<unknown>";
   }

   public void updateOptions(ServerboundClientInformationPacket param0) {
      this.chatVisibility = param0.chatVisibility();
      this.canChatColor = param0.chatColors();
      this.textFilteringEnabled = param0.textFilteringEnabled();
      this.allowsListing = param0.allowsListing();
      this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte)param0.modelCustomisation());
      this.getEntityData().set(DATA_PLAYER_MAIN_HAND, (byte)(param0.mainHand() == HumanoidArm.LEFT ? 0 : 1));
   }

   public boolean canChatInColor() {
      return this.canChatColor;
   }

   public ChatVisiblity getChatVisibility() {
      return this.chatVisibility;
   }

   private boolean acceptsSystemMessages(boolean param0) {
      return this.chatVisibility == ChatVisiblity.HIDDEN ? param0 : true;
   }

   private boolean acceptsChatMessages() {
      return this.chatVisibility == ChatVisiblity.FULL;
   }

   public void sendTexturePack(String param0, String param1, boolean param2, @Nullable Component param3) {
      this.connection.send(new ClientboundResourcePackPacket(param0, param1, param2, param3));
   }

   public void sendServerStatus(ServerStatus param0) {
      this.connection
         .send(new ClientboundServerDataPacket(param0.description(), param0.favicon().map(ServerStatus.Favicon::iconBytes), param0.enforcesSecureChat()));
   }

   @Override
   protected int getPermissionLevel() {
      return this.server.getProfilePermissions(this.getGameProfile());
   }

   public void resetLastActionTime() {
      this.lastActionTime = Util.getMillis();
   }

   public ServerStatsCounter getStats() {
      return this.stats;
   }

   public ServerRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   @Override
   protected void updateInvisibilityStatus() {
      if (this.isSpectator()) {
         this.removeEffectParticles();
         this.setInvisible(true);
      } else {
         super.updateInvisibilityStatus();
      }
   }

   public Entity getCamera() {
      return this.camera == null ? this : this.camera;
   }

   public void setCamera(@Nullable Entity param0) {
      Entity var2 = this.getCamera();
      this.camera = param0 == null ? this : param0;
      if (var2 != this.camera) {
         if (this.camera.getLevel() instanceof ServerLevel var3) {
            this.teleportTo(var3, this.camera.getX(), this.camera.getY(), this.camera.getZ(), Set.of(), this.getYRot(), this.getXRot());
         }

         if (param0 != null) {
            this.getLevel().getChunkSource().move(this);
         }

         this.connection.send(new ClientboundSetCameraPacket(this.camera));
         this.connection.resetPosition();
      }
   }

   @Override
   protected void processPortalCooldown() {
      if (!this.isChangingDimension) {
         super.processPortalCooldown();
      }
   }

   @Override
   public void attack(Entity param0) {
      if (this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
         this.setCamera(param0);
      } else {
         super.attack(param0);
      }
   }

   public long getLastActionTime() {
      return this.lastActionTime;
   }

   @Nullable
   public Component getTabListDisplayName() {
      return null;
   }

   @Override
   public void swing(InteractionHand param0) {
      super.swing(param0);
      this.resetAttackStrengthTicker();
   }

   public boolean isChangingDimension() {
      return this.isChangingDimension;
   }

   public void hasChangedDimension() {
      this.isChangingDimension = false;
   }

   public PlayerAdvancements getAdvancements() {
      return this.advancements;
   }

   public void teleportTo(ServerLevel param0, double param1, double param3, double param5, float param7, float param8) {
      this.setCamera(this);
      this.stopRiding();
      if (param0 == this.level) {
         this.connection.teleport(param1, param3, param5, param7, param8);
      } else {
         ServerLevel var10 = this.getLevel();
         LevelData var11 = param0.getLevelData();
         this.connection
            .send(
               new ClientboundRespawnPacket(
                  param0.dimensionTypeId(),
                  param0.dimension(),
                  BiomeManager.obfuscateSeed(param0.getSeed()),
                  this.gameMode.getGameModeForPlayer(),
                  this.gameMode.getPreviousGameModeForPlayer(),
                  param0.isDebug(),
                  param0.isFlat(),
                  (byte)3,
                  this.getLastDeathLocation()
               )
            );
         this.connection.send(new ClientboundChangeDifficultyPacket(var11.getDifficulty(), var11.isDifficultyLocked()));
         this.server.getPlayerList().sendPlayerPermissionLevel(this);
         var10.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
         this.unsetRemoved();
         this.moveTo(param1, param3, param5, param7, param8);
         this.setLevel(param0);
         param0.addDuringCommandTeleport(this);
         this.triggerDimensionChangeTriggers(var10);
         this.connection.teleport(param1, param3, param5, param7, param8);
         this.server.getPlayerList().sendLevelInfo(this, param0);
         this.server.getPlayerList().sendAllPlayerInfo(this);
      }
   }

   @Nullable
   public BlockPos getRespawnPosition() {
      return this.respawnPosition;
   }

   public float getRespawnAngle() {
      return this.respawnAngle;
   }

   public ResourceKey<Level> getRespawnDimension() {
      return this.respawnDimension;
   }

   public boolean isRespawnForced() {
      return this.respawnForced;
   }

   public void setRespawnPosition(ResourceKey<Level> param0, @Nullable BlockPos param1, float param2, boolean param3, boolean param4) {
      if (param1 != null) {
         boolean var6 = param1.equals(this.respawnPosition) && param0.equals(this.respawnDimension);
         if (param4 && !var6) {
            this.sendSystemMessage(Component.translatable("block.minecraft.set_spawn"));
         }

         this.respawnPosition = param1;
         this.respawnDimension = param0;
         this.respawnAngle = param2;
         this.respawnForced = param3;
      } else {
         this.respawnPosition = null;
         this.respawnDimension = Level.OVERWORLD;
         this.respawnAngle = 0.0F;
         this.respawnForced = false;
      }
   }

   public void trackChunk(ChunkPos param0, Packet<?> param1) {
      this.connection.send(param1);
   }

   public void untrackChunk(ChunkPos param0) {
      if (this.isAlive()) {
         this.connection.send(new ClientboundForgetLevelChunkPacket(param0.x, param0.z));
      }
   }

   public SectionPos getLastSectionPos() {
      return this.lastSectionPos;
   }

   public void setLastSectionPos(SectionPos param0) {
      this.lastSectionPos = param0;
   }

   @Override
   public void playNotifySound(SoundEvent param0, SoundSource param1, float param2, float param3) {
      this.connection
         .send(
            new ClientboundSoundPacket(
               BuiltInRegistries.SOUND_EVENT.wrapAsHolder(param0), param1, this.getX(), this.getY(), this.getZ(), param2, param3, this.random.nextLong()
            )
         );
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddPlayerPacket(this);
   }

   @Override
   public ItemEntity drop(ItemStack param0, boolean param1, boolean param2) {
      ItemEntity var4 = super.drop(param0, param1, param2);
      if (var4 == null) {
         return null;
      }

      this.level.addFreshEntity(var4);
      ItemStack var5 = var4.getItem();
      if (param2) {
         if (!var5.isEmpty()) {
            this.awardStat(Stats.ITEM_DROPPED.get(var5.getItem()), param0.getCount());
         }

         this.awardStat(Stats.DROP);
      }

      return var4;
   }

   public TextFilter getTextFilter() {
      return this.textFilter;
   }

   public void setLevel(ServerLevel param0) {
      this.level = param0;
      this.gameMode.setLevel(param0);
   }

   @Nullable
   private static GameType readPlayerMode(@Nullable CompoundTag param0, String param1) {
      return param0 != null && param0.contains(param1, 99) ? GameType.byId(param0.getInt(param1)) : null;
   }

   private GameType calculateGameModeForNewPlayer(@Nullable GameType param0) {
      GameType var2 = this.server.getForcedGameType();
      if (var2 != null) {
         return var2;
      } else {
         return param0 != null ? param0 : this.server.getDefaultGameType();
      }
   }

   public void loadGameTypes(@Nullable CompoundTag param0) {
      this.gameMode
         .setGameModeForPlayer(this.calculateGameModeForNewPlayer(readPlayerMode(param0, "playerGameType")), readPlayerMode(param0, "previousPlayerGameType"));
   }

   private void storeGameTypes(CompoundTag param0) {
      param0.putInt("playerGameType", this.gameMode.getGameModeForPlayer().getId());
      GameType var2 = this.gameMode.getPreviousGameModeForPlayer();
      if (var2 != null) {
         param0.putInt("previousPlayerGameType", var2.getId());
      }
   }

   @Override
   public boolean isTextFilteringEnabled() {
      return this.textFilteringEnabled;
   }

   public boolean shouldFilterMessageTo(ServerPlayer param0) {
      return param0 == this ? false : this.textFilteringEnabled || param0.textFilteringEnabled;
   }

   @Override
   public boolean mayInteract(Level param0, BlockPos param1) {
      return super.mayInteract(param0, param1) && param0.mayInteract(this, param1);
   }

   @Override
   protected void updateUsingItem(ItemStack param0) {
      CriteriaTriggers.USING_ITEM.trigger(this, param0);
      super.updateUsingItem(param0);
   }

   public boolean drop(boolean param0) {
      Inventory var2 = this.getInventory();
      ItemStack var3 = var2.removeFromSelected(param0);
      this.containerMenu.findSlot(var2, var2.selected).ifPresent(l_param1 -> this.containerMenu.setRemoteSlot(l_param1, var2.getSelected()));
      return this.drop(var3, false, true) != null;
   }

   public boolean allowsListing() {
      return this.allowsListing;
   }

   @Override
   public Optional<WardenSpawnTracker> getWardenSpawnTracker() {
      return Optional.of(this.wardenSpawnTracker);
   }

   @Override
   public void onItemPickup(ItemEntity param0) {
      super.onItemPickup(param0);
      Entity var2 = param0.getOwner();
      if (var2 != null) {
         CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.trigger(this, param0.getItem(), var2);
      }
   }

   public void setChatSession(RemoteChatSession param0) {
      this.chatSession = param0;
   }

   @Nullable
   public RemoteChatSession getChatSession() {
      return this.chatSession;
   }

   @Override
   public void indicateDamage(double param0, double param2) {
      this.hurtDir = (float)(Mth.atan2(param2, param0) * 180.0F / (float)Math.PI - this.getYRot());
      this.connection.send(new ClientboundHurtAnimationPacket(this));
   }
}
