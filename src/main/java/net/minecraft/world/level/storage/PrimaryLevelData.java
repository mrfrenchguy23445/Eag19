package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.timers.TimerCallbacks;
import net.minecraft.world.level.timers.TimerQueue;
import org.slf4j.Logger;

public class PrimaryLevelData implements ServerLevelData, WorldData {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected static final String PLAYER = "Player";
   protected static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
   private LevelSettings settings;
   private final WorldOptions worldOptions;
   private final PrimaryLevelData.SpecialWorldProperty specialWorldProperty;
   private final Lifecycle worldGenSettingsLifecycle;
   private int xSpawn;
   private int ySpawn;
   private int zSpawn;
   private float spawnAngle;
   private long gameTime;
   private long dayTime;
   @Nullable
   private final DataFixer fixerUpper;
   private final int playerDataVersion;
   private boolean upgradedPlayerTag;
   @Nullable
   private CompoundTag loadedPlayerTag;
   private final int version;
   private int clearWeatherTime;
   private boolean raining;
   private int rainTime;
   private boolean thundering;
   private int thunderTime;
   private boolean initialized;
   private boolean difficultyLocked;
   private WorldBorder.Settings worldBorder;
   private CompoundTag endDragonFightData;
   @Nullable
   private CompoundTag customBossEvents;
   private int wanderingTraderSpawnDelay;
   private int wanderingTraderSpawnChance;
   @Nullable
   private UUID wanderingTraderId;
   private final Set<String> knownServerBrands;
   private boolean wasModded;
   private final TimerQueue<MinecraftServer> scheduledEvents;

   private PrimaryLevelData(
      @Nullable DataFixer param0,
      int param1,
      @Nullable CompoundTag param2,
      boolean param3,
      int param4,
      int param5,
      int param6,
      float param7,
      long param8,
      long param10,
      int param12,
      int param13,
      int param14,
      boolean param15,
      int param16,
      boolean param17,
      boolean param18,
      boolean param19,
      WorldBorder.Settings param20,
      int param21,
      int param22,
      @Nullable UUID param23,
      Set<String> param24,
      TimerQueue<MinecraftServer> param25,
      @Nullable CompoundTag param26,
      CompoundTag param27,
      LevelSettings param28,
      WorldOptions param29,
      PrimaryLevelData.SpecialWorldProperty param30,
      Lifecycle param31
   ) {
      this.fixerUpper = param0;
      this.wasModded = param3;
      this.xSpawn = param4;
      this.ySpawn = param5;
      this.zSpawn = param6;
      this.spawnAngle = param7;
      this.gameTime = param8;
      this.dayTime = param10;
      this.version = param12;
      this.clearWeatherTime = param13;
      this.rainTime = param14;
      this.raining = param15;
      this.thunderTime = param16;
      this.thundering = param17;
      this.initialized = param18;
      this.difficultyLocked = param19;
      this.worldBorder = param20;
      this.wanderingTraderSpawnDelay = param21;
      this.wanderingTraderSpawnChance = param22;
      this.wanderingTraderId = param23;
      this.knownServerBrands = param24;
      this.loadedPlayerTag = param2;
      this.playerDataVersion = param1;
      this.scheduledEvents = param25;
      this.customBossEvents = param26;
      this.endDragonFightData = param27;
      this.settings = param28;
      this.worldOptions = param29;
      this.specialWorldProperty = param30;
      this.worldGenSettingsLifecycle = param31;
   }

   public PrimaryLevelData(LevelSettings param0, WorldOptions param1, PrimaryLevelData.SpecialWorldProperty param2, Lifecycle param3) {
      this(
         null,
         SharedConstants.getCurrentVersion().getDataVersion().getVersion(),
         null,
         false,
         0,
         0,
         0,
         0.0F,
         0L,
         0L,
         19133,
         0,
         0,
         false,
         0,
         false,
         false,
         false,
         WorldBorder.DEFAULT_SETTINGS,
         0,
         0,
         null,
         Sets.newLinkedHashSet(),
         new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS),
         null,
         new CompoundTag(),
         param0.copy(),
         param1,
         param2,
         param3
      );
   }

   public static <T> PrimaryLevelData parse(
      Dynamic<T> param0,
      DataFixer param1,
      int param2,
      @Nullable CompoundTag param3,
      LevelSettings param4,
      LevelVersion param5,
      PrimaryLevelData.SpecialWorldProperty param6,
      WorldOptions param7,
      Lifecycle param8
   ) {
      long var9 = param0.get("Time").asLong(0L);
      CompoundTag var11 = (CompoundTag)param0.get("DragonFight")
         .result()
         .orElseGet(() -> param0.get("DimensionData").get("1").get("DragonFight").orElseEmptyMap())
         .convert(NbtOps.INSTANCE)
         .getValue();
      return new PrimaryLevelData(
         param1,
         param2,
         param3,
         param0.get("WasModded").asBoolean(false),
         param0.get("SpawnX").asInt(0),
         param0.get("SpawnY").asInt(0),
         param0.get("SpawnZ").asInt(0),
         param0.get("SpawnAngle").asFloat(0.0F),
         var9,
         param0.get("DayTime").asLong(var9),
         param5.levelDataVersion(),
         param0.get("clearWeatherTime").asInt(0),
         param0.get("rainTime").asInt(0),
         param0.get("raining").asBoolean(false),
         param0.get("thunderTime").asInt(0),
         param0.get("thundering").asBoolean(false),
         param0.get("initialized").asBoolean(true),
         param0.get("DifficultyLocked").asBoolean(false),
         WorldBorder.Settings.read(param0, WorldBorder.DEFAULT_SETTINGS),
         param0.get("WanderingTraderSpawnDelay").asInt(0),
         param0.get("WanderingTraderSpawnChance").asInt(0),
         param0.get("WanderingTraderId").read(UUIDUtil.CODEC).result().orElse(null),
         param0.get("ServerBrands")
            .asStream()
            .flatMap(l_param0 -> l_param0.asString().result().stream())
            .collect(Collectors.toCollection(Sets::newLinkedHashSet)),
         new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS, param0.get("ScheduledEvents").asStream()),
         (CompoundTag)param0.get("CustomBossEvents").orElseEmptyMap().getValue(),
         var11,
         param4,
         param7,
         param6,
         param8
      );
   }

   @Override
   public CompoundTag createTag(RegistryAccess param0, @Nullable CompoundTag param1) {
      this.updatePlayerTag();
      if (param1 == null) {
         param1 = this.loadedPlayerTag;
      }

      CompoundTag var3 = new CompoundTag();
      this.setTagData(param0, var3, param1);
      return var3;
   }

   private void setTagData(RegistryAccess param0, CompoundTag param1, @Nullable CompoundTag param2) {
      ListTag var4 = new ListTag();
      this.knownServerBrands.stream().map(StringTag::valueOf).forEach(var4::add);
      param1.put("ServerBrands", var4);
      param1.putBoolean("WasModded", this.wasModded);
      CompoundTag var5 = new CompoundTag();
      var5.putString("Name", SharedConstants.getCurrentVersion().getName());
      var5.putInt("Id", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
      var5.putBoolean("Snapshot", !SharedConstants.getCurrentVersion().isStable());
      var5.putString("Series", SharedConstants.getCurrentVersion().getDataVersion().getSeries());
      param1.put("Version", var5);
      NbtUtils.addCurrentDataVersion(param1);
      DynamicOps<Tag> var6 = RegistryOps.create(NbtOps.INSTANCE, param0);
      WorldGenSettings.encode(var6, this.worldOptions, param0)
         .resultOrPartial(Util.prefix("WorldGenSettings: ", LOGGER::error))
         .ifPresent(l_param1 -> param1.put("WorldGenSettings", l_param1));
      param1.putInt("GameType", this.settings.gameType().getId());
      param1.putInt("SpawnX", this.xSpawn);
      param1.putInt("SpawnY", this.ySpawn);
      param1.putInt("SpawnZ", this.zSpawn);
      param1.putFloat("SpawnAngle", this.spawnAngle);
      param1.putLong("Time", this.gameTime);
      param1.putLong("DayTime", this.dayTime);
      param1.putLong("LastPlayed", Util.getEpochMillis());
      param1.putString("LevelName", this.settings.levelName());
      param1.putInt("version", 19133);
      param1.putInt("clearWeatherTime", this.clearWeatherTime);
      param1.putInt("rainTime", this.rainTime);
      param1.putBoolean("raining", this.raining);
      param1.putInt("thunderTime", this.thunderTime);
      param1.putBoolean("thundering", this.thundering);
      param1.putBoolean("hardcore", this.settings.hardcore());
      param1.putBoolean("allowCommands", this.settings.allowCommands());
      param1.putBoolean("initialized", this.initialized);
      this.worldBorder.write(param1);
      param1.putByte("Difficulty", (byte)this.settings.difficulty().getId());
      param1.putBoolean("DifficultyLocked", this.difficultyLocked);
      param1.put("GameRules", this.settings.gameRules().createTag());
      param1.put("DragonFight", this.endDragonFightData);
      if (param2 != null) {
         param1.put("Player", param2);
      }

      DataResult<Tag> var7 = WorldDataConfiguration.CODEC.encodeStart(NbtOps.INSTANCE, this.settings.getDataConfiguration());
      var7.get()
         .ifLeft(l_param1 -> param1.merge((CompoundTag)l_param1))
         .ifRight(l_param0 -> LOGGER.warn("Failed to encode configuration {}", l_param0.message()));
      if (this.customBossEvents != null) {
         param1.put("CustomBossEvents", this.customBossEvents);
      }

      param1.put("ScheduledEvents", this.scheduledEvents.store());
      param1.putInt("WanderingTraderSpawnDelay", this.wanderingTraderSpawnDelay);
      param1.putInt("WanderingTraderSpawnChance", this.wanderingTraderSpawnChance);
      if (this.wanderingTraderId != null) {
         param1.putUUID("WanderingTraderId", this.wanderingTraderId);
      }
   }

   @Override
   public int getXSpawn() {
      return this.xSpawn;
   }

   @Override
   public int getYSpawn() {
      return this.ySpawn;
   }

   @Override
   public int getZSpawn() {
      return this.zSpawn;
   }

   @Override
   public float getSpawnAngle() {
      return this.spawnAngle;
   }

   @Override
   public long getGameTime() {
      return this.gameTime;
   }

   @Override
   public long getDayTime() {
      return this.dayTime;
   }

   private void updatePlayerTag() {
      if (!this.upgradedPlayerTag && this.loadedPlayerTag != null) {
         if (this.playerDataVersion < SharedConstants.getCurrentVersion().getDataVersion().getVersion()) {
            if (this.fixerUpper == null) {
               throw (NullPointerException)Util.pauseInIde(
                  new NullPointerException("Fixer Upper not set inside LevelData, and the player tag is not upgraded.")
               );
            }

            this.loadedPlayerTag = DataFixTypes.PLAYER.updateToCurrentVersion(this.fixerUpper, this.loadedPlayerTag, this.playerDataVersion);
         }

         this.upgradedPlayerTag = true;
      }
   }

   @Override
   public CompoundTag getLoadedPlayerTag() {
      this.updatePlayerTag();
      return this.loadedPlayerTag;
   }

   @Override
   public void setXSpawn(int param0) {
      this.xSpawn = param0;
   }

   @Override
   public void setYSpawn(int param0) {
      this.ySpawn = param0;
   }

   @Override
   public void setZSpawn(int param0) {
      this.zSpawn = param0;
   }

   @Override
   public void setSpawnAngle(float param0) {
      this.spawnAngle = param0;
   }

   @Override
   public void setGameTime(long param0) {
      this.gameTime = param0;
   }

   @Override
   public void setDayTime(long param0) {
      this.dayTime = param0;
   }

   @Override
   public void setSpawn(BlockPos param0, float param1) {
      this.xSpawn = param0.getX();
      this.ySpawn = param0.getY();
      this.zSpawn = param0.getZ();
      this.spawnAngle = param1;
   }

   @Override
   public String getLevelName() {
      return this.settings.levelName();
   }

   @Override
   public int getVersion() {
      return this.version;
   }

   @Override
   public int getClearWeatherTime() {
      return this.clearWeatherTime;
   }

   @Override
   public void setClearWeatherTime(int param0) {
      this.clearWeatherTime = param0;
   }

   @Override
   public boolean isThundering() {
      return this.thundering;
   }

   @Override
   public void setThundering(boolean param0) {
      this.thundering = param0;
   }

   @Override
   public int getThunderTime() {
      return this.thunderTime;
   }

   @Override
   public void setThunderTime(int param0) {
      this.thunderTime = param0;
   }

   @Override
   public boolean isRaining() {
      return this.raining;
   }

   @Override
   public void setRaining(boolean param0) {
      this.raining = param0;
   }

   @Override
   public int getRainTime() {
      return this.rainTime;
   }

   @Override
   public void setRainTime(int param0) {
      this.rainTime = param0;
   }

   @Override
   public GameType getGameType() {
      return this.settings.gameType();
   }

   @Override
   public void setGameType(GameType param0) {
      this.settings = this.settings.withGameType(param0);
   }

   @Override
   public boolean isHardcore() {
      return this.settings.hardcore();
   }

   @Override
   public boolean getAllowCommands() {
      return this.settings.allowCommands();
   }

   @Override
   public boolean isInitialized() {
      return this.initialized;
   }

   @Override
   public void setInitialized(boolean param0) {
      this.initialized = param0;
   }

   @Override
   public GameRules getGameRules() {
      return this.settings.gameRules();
   }

   @Override
   public WorldBorder.Settings getWorldBorder() {
      return this.worldBorder;
   }

   @Override
   public void setWorldBorder(WorldBorder.Settings param0) {
      this.worldBorder = param0;
   }

   @Override
   public Difficulty getDifficulty() {
      return this.settings.difficulty();
   }

   @Override
   public void setDifficulty(Difficulty param0) {
      this.settings = this.settings.withDifficulty(param0);
   }

   @Override
   public boolean isDifficultyLocked() {
      return this.difficultyLocked;
   }

   @Override
   public void setDifficultyLocked(boolean param0) {
      this.difficultyLocked = param0;
   }

   @Override
   public TimerQueue<MinecraftServer> getScheduledEvents() {
      return this.scheduledEvents;
   }

   @Override
   public void fillCrashReportCategory(CrashReportCategory param0, LevelHeightAccessor param1) {
      ServerLevelData.super.fillCrashReportCategory(param0, param1);
      WorldData.super.fillCrashReportCategory(param0);
   }

   @Override
   public WorldOptions worldGenOptions() {
      return this.worldOptions;
   }

   @Override
   public boolean isFlatWorld() {
      return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.FLAT;
   }

   @Override
   public boolean isDebugWorld() {
      return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.DEBUG;
   }

   @Override
   public Lifecycle worldGenSettingsLifecycle() {
      return this.worldGenSettingsLifecycle;
   }

   @Override
   public CompoundTag endDragonFightData() {
      return this.endDragonFightData;
   }

   @Override
   public void setEndDragonFightData(CompoundTag param0) {
      this.endDragonFightData = param0;
   }

   @Override
   public WorldDataConfiguration getDataConfiguration() {
      return this.settings.getDataConfiguration();
   }

   @Override
   public void setDataConfiguration(WorldDataConfiguration param0) {
      this.settings = this.settings.withDataConfiguration(param0);
   }

   @Nullable
   @Override
   public CompoundTag getCustomBossEvents() {
      return this.customBossEvents;
   }

   @Override
   public void setCustomBossEvents(@Nullable CompoundTag param0) {
      this.customBossEvents = param0;
   }

   @Override
   public int getWanderingTraderSpawnDelay() {
      return this.wanderingTraderSpawnDelay;
   }

   @Override
   public void setWanderingTraderSpawnDelay(int param0) {
      this.wanderingTraderSpawnDelay = param0;
   }

   @Override
   public int getWanderingTraderSpawnChance() {
      return this.wanderingTraderSpawnChance;
   }

   @Override
   public void setWanderingTraderSpawnChance(int param0) {
      this.wanderingTraderSpawnChance = param0;
   }

   @Nullable
   @Override
   public UUID getWanderingTraderId() {
      return this.wanderingTraderId;
   }

   @Override
   public void setWanderingTraderId(UUID param0) {
      this.wanderingTraderId = param0;
   }

   @Override
   public void setModdedInfo(String param0, boolean param1) {
      this.knownServerBrands.add(param0);
      this.wasModded |= param1;
   }

   @Override
   public boolean wasModded() {
      return this.wasModded;
   }

   @Override
   public Set<String> getKnownServerBrands() {
      return ImmutableSet.copyOf(this.knownServerBrands);
   }

   @Override
   public ServerLevelData overworldData() {
      return this;
   }

   @Override
   public LevelSettings getLevelSettings() {
      return this.settings.copy();
   }

   @Deprecated
   public enum SpecialWorldProperty {
      NONE,
      FLAT,
      DEBUG;
   }
}
