package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.slf4j.Logger;

public class ClientLevel extends Level {
   private static final Logger LOGGER = LogUtils.getLogger();
   // Cooperative-port: throttle counter for ambient particle tick failure logs.
   private int ambientTickFailCount = 0;
   private static final double FLUID_PARTICLE_SPAWN_OFFSET = 0.05;
   private static final int NORMAL_LIGHT_UPDATES_PER_FRAME = 10;
   private static final int LIGHT_UPDATE_QUEUE_SIZE_THRESHOLD = 1000;
   final EntityTickList tickingEntities = new EntityTickList();
   private final TransientEntitySectionManager<Entity> entityStorage = new TransientEntitySectionManager<>(Entity.class, new ClientLevel.EntityCallbacks());
   private final ClientPacketListener connection;
   private final LevelRenderer levelRenderer;
   private final ClientLevel.ClientLevelData clientLevelData;
   private final DimensionSpecialEffects effects;
   private final Minecraft minecraft = Minecraft.getInstance();
   final List<AbstractClientPlayer> players = Lists.newArrayList();
   private Scoreboard scoreboard = new Scoreboard();
   private final Map<String, MapItemSavedData> mapData = Maps.newHashMap();
   private static final long CLOUD_COLOR = 16777215L;
   private int skyFlashTime;
   private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = Util.make(new Object2ObjectArrayMap<>(3), l_param0 -> {
      l_param0.put(BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache(l2_param0 -> this.calculateBlockTint(l2_param0, BiomeColors.GRASS_COLOR_RESOLVER)));
      l_param0.put(BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache(l2_param0 -> this.calculateBlockTint(l2_param0, BiomeColors.FOLIAGE_COLOR_RESOLVER)));
      l_param0.put(BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache(l2_param0 -> this.calculateBlockTint(l2_param0, BiomeColors.WATER_COLOR_RESOLVER)));
   });
   private final ClientChunkCache chunkSource;
   private final Deque<Runnable> lightUpdateQueue = Queues.newArrayDeque();
   private int serverSimulationDistance;
   private final BlockStatePredictionHandler blockStatePredictionHandler = new BlockStatePredictionHandler();
   private static final Set<Item> MARKER_PARTICLE_ITEMS = Set.of(Items.BARRIER, Items.LIGHT);

   public void handleBlockChangedAck(int param0) {
      this.blockStatePredictionHandler.endPredictionsUpTo(param0, this);
   }

   public void setServerVerifiedBlockState(BlockPos param0, BlockState param1, int param2) {
      if (!this.blockStatePredictionHandler.updateKnownServerState(param0, param1)) {
         super.setBlock(param0, param1, param2, 512);
      }
   }

   public void syncBlockState(BlockPos param0, BlockState param1, Vec3 param2) {
      BlockState var4 = this.getBlockState(param0);
      if (var4 != param1) {
         this.setBlock(param0, param1, 19);
         Player var5 = this.minecraft.player;
         if (this == var5.level && var5.isColliding(param0, param1)) {
            var5.absMoveTo(param2.x, param2.y, param2.z);
         }
      }
   }

   BlockStatePredictionHandler getBlockStatePredictionHandler() {
      return this.blockStatePredictionHandler;
   }

   @Override
   public boolean setBlock(BlockPos param0, BlockState param1, int param2, int param3) {
      if (this.blockStatePredictionHandler.isPredicting()) {
         BlockState var5 = this.getBlockState(param0);
         boolean var6 = super.setBlock(param0, param1, param2, param3);
         if (var6) {
            this.blockStatePredictionHandler.retainKnownServerState(param0, var5, this.minecraft.player);
         }

         return var6;
      } else {
         return super.setBlock(param0, param1, param2, param3);
      }
   }

   public ClientLevel(
      ClientPacketListener param0,
      ClientLevel.ClientLevelData param1,
      ResourceKey<Level> param2,
      Holder<DimensionType> param3,
      int param4,
      int param5,
      Supplier<ProfilerFiller> param6,
      LevelRenderer param7,
      boolean param8,
      long param9
   ) {
      super(param1, param2, param0.registryAccess(), param3, param6, true, param8, param9, 1000000);
      this.connection = param0;
      this.chunkSource = new ClientChunkCache(this, param4);
      this.clientLevelData = param1;
      this.levelRenderer = param7;
      this.effects = DimensionSpecialEffects.forType(param3.value());
      this.setDefaultSpawnPos(new BlockPos(8, 64, 8), 0.0F);
      this.serverSimulationDistance = param5;
      this.updateSkyBrightness();
      this.prepareWeather();
   }

   public void queueLightUpdate(Runnable param0) {
      this.lightUpdateQueue.add(param0);
   }

   public void pollLightUpdates() {
      int var1 = this.lightUpdateQueue.size();
      int var2 = var1 < 1000 ? Math.max(10, var1 / 10) : var1;

      for (int var3 = 0; var3 < var2; var3++) {
         Runnable var4 = this.lightUpdateQueue.poll();
         if (var4 == null) {
            break;
         }

         var4.run();
      }
   }

   public boolean isLightUpdateQueueEmpty() {
      return this.lightUpdateQueue.isEmpty();
   }

   public DimensionSpecialEffects effects() {
      return this.effects;
   }

   public void tick(BooleanSupplier param0) {
      this.getWorldBorder().tick();
      this.tickTime();
      this.getProfiler().push("blocks");
      this.chunkSource.tick(param0, true);
      this.getProfiler().pop();
   }

   private void tickTime() {
      this.setGameTime(this.levelData.getGameTime() + 1L);
      if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
         this.setDayTime(this.levelData.getDayTime() + 1L);
      }
   }

   public void setGameTime(long param0) {
      this.clientLevelData.setGameTime(param0);
   }

   public void setDayTime(long param0) {
      if (param0 < 0L) {
         param0 = -param0;
         this.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, null);
      } else {
         this.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, null);
      }

      this.clientLevelData.setDayTime(param0);
   }

   public Iterable<Entity> entitiesForRendering() {
      return this.getEntities().getAll();
   }

   public void tickEntities() {
      ProfilerFiller var1 = this.getProfiler();
      var1.push("entities");
      this.tickingEntities.forEach(l_param0 -> {
         if (!l_param0.isRemoved() && !l_param0.isPassenger()) {
            this.guardEntityTick(this::tickNonPassenger, l_param0);
         }
      });
      var1.pop();
      this.tickBlockEntities();
   }

   @Override
   public boolean shouldTickDeath(Entity param0) {
      return param0.chunkPosition().getChessboardDistance(this.minecraft.player.chunkPosition()) <= this.serverSimulationDistance;
   }

   public void tickNonPassenger(Entity param0) {
      param0.setOldPosAndRot();
      param0.tickCount++;
      this.getProfiler().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(param0.getType()).toString());
      param0.tick();
      this.getProfiler().pop();

      for (Entity var3 : param0.getPassengers()) {
         this.tickPassenger(param0, var3);
      }
   }

   private void tickPassenger(Entity param0, Entity param1) {
      if (param1.isRemoved() || param1.getVehicle() != param0) {
         param1.stopRiding();
      } else if (param1 instanceof Player || this.tickingEntities.contains(param1)) {
         param1.setOldPosAndRot();
         param1.tickCount++;
         param1.rideTick();

         for (Entity var4 : param1.getPassengers()) {
            this.tickPassenger(param1, var4);
         }
      }
   }

   public void unload(LevelChunk param0) {
      param0.clearAllBlockEntities();
      this.chunkSource.getLightEngine().enableLightSources(param0.getPos(), false);
      this.entityStorage.stopTicking(param0.getPos());
   }

   public void onChunkLoaded(ChunkPos param0) {
      this.tintCaches.forEach((l_param1, l_param2) -> l_param2.invalidateForChunk(param0.x, param0.z));
      this.entityStorage.startTicking(param0);
   }

   public void clearTintCaches() {
      this.tintCaches.forEach((l_param0, l_param1) -> l_param1.invalidateAll());
   }

   @Override
   public boolean hasChunk(int param0, int param1) {
      return true;
   }

   public int getEntityCount() {
      return this.entityStorage.count();
   }

   public void addPlayer(int param0, AbstractClientPlayer param1) {
      this.addEntity(param0, param1);
   }

   public void putNonPlayerEntity(int param0, Entity param1) {
      this.addEntity(param0, param1);
   }

   private void addEntity(int param0, Entity param1) {
      this.removeEntity(param0, Entity.RemovalReason.DISCARDED);
      this.entityStorage.addEntity(param1);
   }

   public void removeEntity(int param0, Entity.RemovalReason param1) {
      Entity var3 = this.getEntities().get(param0);
      if (var3 != null) {
         var3.setRemoved(param1);
         var3.onClientRemoval();
      }
   }

   @Nullable
   @Override
   public Entity getEntity(int param0) {
      return this.getEntities().get(param0);
   }

   @Override
   public void disconnect() {
      this.connection.getConnection().disconnect(Component.translatable("multiplayer.status.quitting"));
   }

   public void animateTick(int param0, int param1, int param2) {
      int var4 = 32;
      RandomSource var5 = RandomSource.create();
      Block var6 = this.getMarkerParticleTarget();
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();

      for (int var8 = 0; var8 < 667; var8++) {
         // Cooperative-port hardening: ambient particles are purely visual. In this port an
         // NPE inside the spawn chain (a leftover particle provider against torn-down or
         // still-loading chunk state) must not take the whole client down; log with
         // position so the culprit block can be identified instead. Throttled so a
         // deterministic per-frame failure can't spam thousands of console errors.
         try {
            this.doAnimateTick(param0, param1, param2, 16, var5, var6, var7);
            this.doAnimateTick(param0, param1, param2, 32, var5, var6, var7);
         } catch (Throwable l_var2) {
            if (++ambientTickFailCount == 1 || ambientTickFailCount % 500 == 0) {
               LOGGER.error(
                  "Ambient particle tick failed near block ({}, {}, {}) [failure #{}]",
                  var7.getX(), var7.getY(), var7.getZ(), ambientTickFailCount, l_var2
               );
            }
         }
      }
   }

   @Nullable
   private Block getMarkerParticleTarget() {
      if (this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
         ItemStack var1 = this.minecraft.player.getMainHandItem();
         Item var2 = var1.getItem();
         if (MARKER_PARTICLE_ITEMS.contains(var2) && var2 instanceof BlockItem var3) {
            return var3.getBlock();
         }
      }

      return null;
   }

   public void doAnimateTick(int param0, int param1, int param2, int param3, RandomSource param4, @Nullable Block param5, BlockPos.MutableBlockPos param6) {
      int var8 = param0 + this.random.nextInt(param3) - this.random.nextInt(param3);
      int var9 = param1 + this.random.nextInt(param3) - this.random.nextInt(param3);
      int var10 = param2 + this.random.nextInt(param3) - this.random.nextInt(param3);
      param6.set(var8, var9, var10);
      BlockState var11 = this.getBlockState(param6);
      var11.getBlock().animateTick(var11, this, param6, param4);
      FluidState var12 = this.getFluidState(param6);
      if (!var12.isEmpty()) {
         var12.animateTick(this, param6, param4);
         ParticleOptions var13 = var12.getDripParticle();
         if (var13 != null && this.random.nextInt(10) == 0) {
            boolean var14 = var11.isFaceSturdy(this, param6, Direction.DOWN);
            BlockPos var15 = param6.below();
            this.trySpawnDripParticles(var15, this.getBlockState(var15), var13, var14);
         }
      }

      if (param5 == var11.getBlock()) {
         this.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, var11), var8 + 0.5, var9 + 0.5, var10 + 0.5, 0.0, 0.0, 0.0);
      }

      if (!var11.isCollisionShapeFullBlock(this, param6)) {
         this.getBiome(param6)
            .value()
            .getAmbientParticle()
            .ifPresent(
               l_param1 -> {
                  if (l_param1.canSpawn(this.random)) {
                     this.addParticle(
                        l_param1.getOptions(),
                        param6.getX() + this.random.nextDouble(),
                        param6.getY() + this.random.nextDouble(),
                        param6.getZ() + this.random.nextDouble(),
                        0.0,
                        0.0,
                        0.0
                     );
                  }
               }
            );
      }
   }

   private void trySpawnDripParticles(BlockPos param0, BlockState param1, ParticleOptions param2, boolean param3) {
      if (param1.getFluidState().isEmpty()) {
         VoxelShape var5 = param1.getCollisionShape(this, param0);
         double var6 = var5.max(Direction.Axis.Y);
         if (var6 < 1.0) {
            if (param3) {
               this.spawnFluidParticle(param0.getX(), param0.getX() + 1, param0.getZ(), param0.getZ() + 1, param0.getY() + 1 - 0.05, param2);
            }
         } else if (!param1.is(BlockTags.IMPERMEABLE)) {
            double var8 = var5.min(Direction.Axis.Y);
            if (var8 > 0.0) {
               this.spawnParticle(param0, param2, var5, param0.getY() + var8 - 0.05);
            } else {
               BlockPos var10 = param0.below();
               BlockState var11 = this.getBlockState(var10);
               VoxelShape var12 = var11.getCollisionShape(this, var10);
               double var13 = var12.max(Direction.Axis.Y);
               if (var13 < 1.0 && var11.getFluidState().isEmpty()) {
                  this.spawnParticle(param0, param2, var5, param0.getY() - 0.05);
               }
            }
         }
      }
   }

   private void spawnParticle(BlockPos param0, ParticleOptions param1, VoxelShape param2, double param3) {
      this.spawnFluidParticle(
         param0.getX() + param2.min(Direction.Axis.X),
         param0.getX() + param2.max(Direction.Axis.X),
         param0.getZ() + param2.min(Direction.Axis.Z),
         param0.getZ() + param2.max(Direction.Axis.Z),
         param3,
         param1
      );
   }

   private void spawnFluidParticle(double param0, double param2, double param4, double param6, double param8, ParticleOptions param10) {
      this.addParticle(param10, Mth.lerp(this.random.nextDouble(), param0, param2), param8, Mth.lerp(this.random.nextDouble(), param4, param6), 0.0, 0.0, 0.0);
   }

   @Override
   public CrashReportCategory fillReportDetails(CrashReport param0) {
      CrashReportCategory var2 = super.fillReportDetails(param0);
      var2.setDetail("Server brand", () -> this.minecraft.player.getServerBrand());
      var2.setDetail(
         "Server type", () -> this.minecraft.getSingleplayerServer() == null ? "Non-integrated multiplayer server" : "Integrated singleplayer server"
      );
      return var2;
   }

   @Override
   public void playSeededSound(
      @Nullable Player param0,
      double param1,
      double param3,
      double param5,
      Holder<SoundEvent> param7,
      SoundSource param8,
      float param9,
      float param10,
      long param11
   ) {
      if (param0 == this.minecraft.player) {
         this.playSound(param1, param3, param5, param7.value(), param8, param9, param10, false, param11);
      }
   }

   @Override
   public void playSeededSound(@Nullable Player param0, Entity param1, Holder<SoundEvent> param2, SoundSource param3, float param4, float param5, long param6) {
      if (param0 == this.minecraft.player) {
         this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(param2.value(), param3, param4, param5, param1, param6));
      }
   }

   @Override
   public void playLocalSound(double param0, double param2, double param4, SoundEvent param6, SoundSource param7, float param8, float param9, boolean param10) {
      this.playSound(param0, param2, param4, param6, param7, param8, param9, param10, this.random.nextLong());
   }

   private void playSound(
      double param0, double param2, double param4, SoundEvent param6, SoundSource param7, float param8, float param9, boolean param10, long param11
   ) {
      double var14 = this.minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(param0, param2, param4);
      SimpleSoundInstance var16 = new SimpleSoundInstance(param6, param7, param8, param9, RandomSource.create(param11), param0, param2, param4);
      if (param10 && var14 > 100.0) {
         double var17 = Math.sqrt(var14) / 40.0;
         this.minecraft.getSoundManager().playDelayed(var16, (int)(var17 * 20.0));
      } else {
         this.minecraft.getSoundManager().play(var16);
      }
   }

   @Override
   public void createFireworks(double param0, double param2, double param4, double param6, double param8, double param10, @Nullable CompoundTag param12) {
      this.minecraft
         .particleEngine
         .add(new FireworkParticles.Starter(this, param0, param2, param4, param6, param8, param10, this.minecraft.particleEngine, param12));
   }

   @Override
   public void sendPacketToServer(Packet<?> param0) {
      this.connection.send(param0);
   }

   @Override
   public RecipeManager getRecipeManager() {
      return this.connection.getRecipeManager();
   }

   public void setScoreboard(Scoreboard param0) {
      this.scoreboard = param0;
   }

   @Override
   public LevelTickAccess<Block> getBlockTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   @Override
   public LevelTickAccess<Fluid> getFluidTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   public ClientChunkCache getChunkSource() {
      return this.chunkSource;
   }

   @Nullable
   @Override
   public MapItemSavedData getMapData(String param0) {
      return this.mapData.get(param0);
   }

   public void overrideMapData(String param0, MapItemSavedData param1) {
      this.mapData.put(param0, param1);
   }

   @Override
   public void setMapData(String param0, MapItemSavedData param1) {
   }

   @Override
   public int getFreeMapId() {
      return 0;
   }

   @Override
   public Scoreboard getScoreboard() {
      return this.scoreboard;
   }

   @Override
   public void sendBlockUpdated(BlockPos param0, BlockState param1, BlockState param2, int param3) {
      this.levelRenderer.blockChanged(this, param0, param1, param2, param3);
   }

   @Override
   public void setBlocksDirty(BlockPos param0, BlockState param1, BlockState param2) {
      this.levelRenderer.setBlockDirty(param0, param1, param2);
   }

   public void setSectionDirtyWithNeighbors(int param0, int param1, int param2) {
      this.levelRenderer.setSectionDirtyWithNeighbors(param0, param1, param2);
   }

   public void setLightReady(int param0, int param1) {
      LevelChunk var3 = this.chunkSource.getChunk(param0, param1, false);
      if (var3 != null) {
         var3.setClientLightReady(true);
      }
   }

   @Override
   public void destroyBlockProgress(int param0, BlockPos param1, int param2) {
      this.levelRenderer.destroyBlockProgress(param0, param1, param2);
   }

   @Override
   public void globalLevelEvent(int param0, BlockPos param1, int param2) {
      this.levelRenderer.globalLevelEvent(param0, param1, param2);
   }

   @Override
   public void levelEvent(@Nullable Player param0, int param1, BlockPos param2, int param3) {
      try {
         this.levelRenderer.levelEvent(param1, param2, param3);
      } catch (Throwable var5) {
         CrashReport var6 = CrashReport.forThrowable(var5, "Playing level event");
         CrashReportCategory var7 = var6.addCategory("Level event being played");
         var7.setDetail("Block coordinates", CrashReportCategory.formatLocation(this, param2));
         var7.setDetail("Event source", param0);
         var7.setDetail("Event type", param1);
         var7.setDetail("Event data", param3);
         throw new ReportedException(var6);
      }
   }

   @Override
   public void addParticle(ParticleOptions param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      this.levelRenderer.addParticle(param0, param0.getType().getOverrideLimiter(), param1, param3, param5, param7, param9, param11);
   }

   @Override
   public void addParticle(ParticleOptions param0, boolean param1, double param2, double param4, double param6, double param8, double param10, double param12) {
      this.levelRenderer.addParticle(param0, param0.getType().getOverrideLimiter() || param1, param2, param4, param6, param8, param10, param12);
   }

   @Override
   public void addAlwaysVisibleParticle(ParticleOptions param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      this.levelRenderer.addParticle(param0, false, true, param1, param3, param5, param7, param9, param11);
   }

   @Override
   public void addAlwaysVisibleParticle(
      ParticleOptions param0, boolean param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      this.levelRenderer.addParticle(param0, param0.getType().getOverrideLimiter() || param1, true, param2, param4, param6, param8, param10, param12);
   }

   @Override
   public List<AbstractClientPlayer> players() {
      return this.players;
   }

   @Override
   public Holder<Biome> getUncachedNoiseBiome(int param0, int param1, int param2) {
      return this.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
   }

   public float getSkyDarken(float param0) {
      float var2 = this.getTimeOfDay(param0);
      float var3 = 1.0F - (Mth.cos(var2 * (float) (Math.PI * 2)) * 2.0F + 0.2F);
      var3 = Mth.clamp(var3, 0.0F, 1.0F);
      var3 = 1.0F - var3;
      var3 *= 1.0F - this.getRainLevel(param0) * 5.0F / 16.0F;
      var3 *= 1.0F - this.getThunderLevel(param0) * 5.0F / 16.0F;
      return var3 * 0.8F + 0.2F;
   }

   public Vec3 getSkyColor(Vec3 param0, float param1) {
      float var3 = this.getTimeOfDay(param1);
      Vec3 var4 = param0.subtract(2.0, 2.0, 2.0).scale(0.25);
      BiomeManager var5 = this.getBiomeManager();
      Vec3 var6 = CubicSampler.gaussianSampleVec3(
         var4, (l_param1, l_param2, l_param3) -> Vec3.fromRGB24(var5.getNoiseBiomeAtQuart(l_param1, l_param2, l_param3).value().getSkyColor())
      );
      float var7 = Mth.cos(var3 * (float) (Math.PI * 2)) * 2.0F + 0.5F;
      var7 = Mth.clamp(var7, 0.0F, 1.0F);
      float var8 = (float)var6.x * var7;
      float var9 = (float)var6.y * var7;
      float var10 = (float)var6.z * var7;
      float var11 = this.getRainLevel(param1);
      if (var11 > 0.0F) {
         float var12 = (var8 * 0.3F + var9 * 0.59F + var10 * 0.11F) * 0.6F;
         float var13 = 1.0F - var11 * 0.75F;
         var8 = var8 * var13 + var12 * (1.0F - var13);
         var9 = var9 * var13 + var12 * (1.0F - var13);
         var10 = var10 * var13 + var12 * (1.0F - var13);
      }

      float var12 = this.getThunderLevel(param1);
      if (var12 > 0.0F) {
         float var13 = (var8 * 0.3F + var9 * 0.59F + var10 * 0.11F) * 0.2F;
         float var14 = 1.0F - var12 * 0.75F;
         var8 = var8 * var14 + var13 * (1.0F - var14);
         var9 = var9 * var14 + var13 * (1.0F - var14);
         var10 = var10 * var14 + var13 * (1.0F - var14);
      }

      if (!this.minecraft.options.hideLightningFlash().get() && this.skyFlashTime > 0) {
         float var13 = this.skyFlashTime - param1;
         if (var13 > 1.0F) {
            var13 = 1.0F;
         }

         var13 *= 0.45F;
         var8 = var8 * (1.0F - var13) + 0.8F * var13;
         var9 = var9 * (1.0F - var13) + 0.8F * var13;
         var10 = var10 * (1.0F - var13) + 1.0F * var13;
      }

      return new Vec3(var8, var9, var10);
   }

   public Vec3 getCloudColor(float param0) {
      float var2 = this.getTimeOfDay(param0);
      float var3 = Mth.cos(var2 * (float) (Math.PI * 2)) * 2.0F + 0.5F;
      var3 = Mth.clamp(var3, 0.0F, 1.0F);
      float var4 = 1.0F;
      float var5 = 1.0F;
      float var6 = 1.0F;
      float var7 = this.getRainLevel(param0);
      if (var7 > 0.0F) {
         float var8 = (var4 * 0.3F + var5 * 0.59F + var6 * 0.11F) * 0.6F;
         float var9 = 1.0F - var7 * 0.95F;
         var4 = var4 * var9 + var8 * (1.0F - var9);
         var5 = var5 * var9 + var8 * (1.0F - var9);
         var6 = var6 * var9 + var8 * (1.0F - var9);
      }

      var4 *= var3 * 0.9F + 0.1F;
      var5 *= var3 * 0.9F + 0.1F;
      var6 *= var3 * 0.85F + 0.15F;
      float var8 = this.getThunderLevel(param0);
      if (var8 > 0.0F) {
         float var9 = (var4 * 0.3F + var5 * 0.59F + var6 * 0.11F) * 0.2F;
         float var10 = 1.0F - var8 * 0.95F;
         var4 = var4 * var10 + var9 * (1.0F - var10);
         var5 = var5 * var10 + var9 * (1.0F - var10);
         var6 = var6 * var10 + var9 * (1.0F - var10);
      }

      return new Vec3(var4, var5, var6);
   }

   public float getStarBrightness(float param0) {
      float var2 = this.getTimeOfDay(param0);
      float var3 = 1.0F - (Mth.cos(var2 * (float) (Math.PI * 2)) * 2.0F + 0.25F);
      var3 = Mth.clamp(var3, 0.0F, 1.0F);
      return var3 * var3 * 0.5F;
   }

   public int getSkyFlashTime() {
      return this.skyFlashTime;
   }

   @Override
   public void setSkyFlashTime(int param0) {
      this.skyFlashTime = param0;
   }

   @Override
   public float getShade(Direction param0, boolean param1) {
      boolean var3 = this.effects().constantAmbientLight();
      if (!param1) {
         return var3 ? 0.9F : 1.0F;
      }

      switch (param0) {
         case DOWN:
            return var3 ? 0.9F : 0.5F;
         case UP:
            return var3 ? 0.9F : 1.0F;
         case NORTH:
         case SOUTH:
            return 0.8F;
         case WEST:
         case EAST:
            return 0.6F;
         default:
            return 1.0F;
      }
   }

   @Override
   public int getBlockTint(BlockPos param0, ColorResolver param1) {
      BlockTintCache var3 = this.tintCaches.get(param1);
      return var3.getColor(param0);
   }

   public int calculateBlockTint(BlockPos param0, ColorResolver param1) {
      int var3 = Minecraft.getInstance().options.biomeBlendRadius().get();
      if (var3 == 0) {
         return param1.getColor(this.getBiome(param0).value(), param0.getX(), param0.getZ());
      }

      int var4 = (var3 * 2 + 1) * (var3 * 2 + 1);
      int var5 = 0;
      int var6 = 0;
      int var7 = 0;
      Cursor3D var8 = new Cursor3D(param0.getX() - var3, param0.getY(), param0.getZ() - var3, param0.getX() + var3, param0.getY(), param0.getZ() + var3);
      BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();

      while (var8.advance()) {
         var9.set(var8.nextX(), var8.nextY(), var8.nextZ());
         int var10 = param1.getColor(this.getBiome(var9).value(), var9.getX(), var9.getZ());
         var5 += (var10 & 0xFF0000) >> 16;
         var6 += (var10 & 0xFF00) >> 8;
         var7 += var10 & 0xFF;
      }

      return (var5 / var4 & 0xFF) << 16 | (var6 / var4 & 0xFF) << 8 | var7 / var4 & 0xFF;
   }

   public void setDefaultSpawnPos(BlockPos param0, float param1) {
      this.levelData.setSpawn(param0, param1);
   }

   @Override
   public String toString() {
      return "ClientLevel";
   }

   public ClientLevel.ClientLevelData getLevelData() {
      return this.clientLevelData;
   }

   @Override
   public void gameEvent(GameEvent param0, Vec3 param1, GameEvent.Context param2) {
   }

   protected Map<String, MapItemSavedData> getAllMapData() {
      return ImmutableMap.copyOf(this.mapData);
   }

   protected void addMapData(Map<String, MapItemSavedData> param0) {
      this.mapData.putAll(param0);
   }

   @Override
   protected LevelEntityGetter<Entity> getEntities() {
      return this.entityStorage.getEntityGetter();
   }

   @Override
   public String gatherChunkSourceStats() {
      return "Chunks[C] W: " + this.chunkSource.gatherStats() + " E: " + this.entityStorage.gatherStats();
   }

   @Override
   public void addDestroyBlockEffect(BlockPos param0, BlockState param1) {
      this.minecraft.particleEngine.destroy(param0, param1);
   }

   public void setServerSimulationDistance(int param0) {
      this.serverSimulationDistance = param0;
   }

   public int getServerSimulationDistance() {
      return this.serverSimulationDistance;
   }

   @Override
   public FeatureFlagSet enabledFeatures() {
      return this.connection.enabledFeatures();
   }

   public static class ClientLevelData implements WritableLevelData {
      private final boolean hardcore;
      private final GameRules gameRules;
      private final boolean isFlat;
      private int xSpawn;
      private int ySpawn;
      private int zSpawn;
      private float spawnAngle;
      private long gameTime;
      private long dayTime;
      private boolean raining;
      private Difficulty difficulty;
      private boolean difficultyLocked;

      public ClientLevelData(Difficulty param0, boolean param1, boolean param2) {
         this.difficulty = param0;
         this.hardcore = param1;
         this.isFlat = param2;
         this.gameRules = new GameRules();
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

      public void setGameTime(long param0) {
         this.gameTime = param0;
      }

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
      public boolean isThundering() {
         return false;
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
      public boolean isHardcore() {
         return this.hardcore;
      }

      @Override
      public GameRules getGameRules() {
         return this.gameRules;
      }

      @Override
      public Difficulty getDifficulty() {
         return this.difficulty;
      }

      @Override
      public boolean isDifficultyLocked() {
         return this.difficultyLocked;
      }

      @Override
      public void fillCrashReportCategory(CrashReportCategory param0, LevelHeightAccessor param1) {
         WritableLevelData.super.fillCrashReportCategory(param0, param1);
      }

      public void setDifficulty(Difficulty param0) {
         this.difficulty = param0;
      }

      public void setDifficultyLocked(boolean param0) {
         this.difficultyLocked = param0;
      }

      public double getHorizonHeight(LevelHeightAccessor param0) {
         return this.isFlat ? param0.getMinBuildHeight() : 63.0;
      }

      public float getClearColorScale() {
         return this.isFlat ? 1.0F : 0.03125F;
      }
   }

   final class EntityCallbacks implements LevelCallback<Entity> {
      public void onCreated(Entity param0) {
      }

      public void onDestroyed(Entity param0) {
      }

      public void onTickingStart(Entity param0) {
         ClientLevel.this.tickingEntities.add(param0);
      }

      public void onTickingEnd(Entity param0) {
         ClientLevel.this.tickingEntities.remove(param0);
      }

      public void onTrackingStart(Entity param0) {
         if (param0 instanceof AbstractClientPlayer) {
            ClientLevel.this.players.add((AbstractClientPlayer)param0);
         }
      }

      public void onTrackingEnd(Entity param0) {
         param0.unRide();
         ClientLevel.this.players.remove(param0);
      }

      public void onSectionChange(Entity param0) {
      }
   }
}
