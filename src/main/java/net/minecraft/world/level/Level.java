package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;

public abstract class Level implements LevelAccessor, AutoCloseable {
   public static final Codec<ResourceKey<Level>> RESOURCE_KEY_CODEC = ResourceKey.codec(Registries.DIMENSION);
   public static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("overworld"));
   public static final ResourceKey<Level> NETHER = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("the_nether"));
   public static final ResourceKey<Level> END = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("the_end"));
   public static final int MAX_LEVEL_SIZE = 30000000;
   public static final int LONG_PARTICLE_CLIP_RANGE = 512;
   public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
   private static final Direction[] DIRECTIONS = Direction.values();
   public static final int MAX_BRIGHTNESS = 15;
   public static final int TICKS_PER_DAY = 24000;
   public static final int MAX_ENTITY_SPAWN_Y = 20000000;
   public static final int MIN_ENTITY_SPAWN_Y = -20000000;
   protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
   protected final NeighborUpdater neighborUpdater;
   private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
   private boolean tickingBlockEntities;
   private final Thread thread;
   private final boolean isDebug;
   private int skyDarken;
   protected int randValue = RandomSource.create().nextInt();
   protected final int addend = 1013904223;
   protected float oRainLevel;
   protected float rainLevel;
   protected float oThunderLevel;
   protected float thunderLevel;
   public final RandomSource random = RandomSource.create();
   @Deprecated
   private final RandomSource threadSafeRandom = RandomSource.createThreadSafe();
   private final ResourceKey<DimensionType> dimensionTypeId;
   private final Holder<DimensionType> dimensionTypeRegistration;
   protected final WritableLevelData levelData;
   private final Supplier<ProfilerFiller> profiler;
   public final boolean isClientSide;
   private final WorldBorder worldBorder;
   private final BiomeManager biomeManager;
   private final ResourceKey<Level> dimension;
   private final RegistryAccess registryAccess;
   private final DamageSources damageSources;
   private long subTickCount;

   protected Level(
      WritableLevelData param0,
      ResourceKey<Level> param1,
      RegistryAccess param2,
      Holder<DimensionType> param3,
      Supplier<ProfilerFiller> param4,
      boolean param5,
      boolean param6,
      long param7,
      int param9
   ) {
      this.profiler = param4;
      this.levelData = param0;
      this.dimensionTypeRegistration = param3;
      this.dimensionTypeId = param3.unwrapKey().orElseThrow(() -> new IllegalArgumentException("Dimension must be registered, got " + param3));
      final DimensionType var11 = param3.value();
      this.dimension = param1;
      this.isClientSide = param5;
      if (var11.coordinateScale() != 1.0) {
         this.worldBorder = new WorldBorder() {
            @Override
            public double getCenterX() {
               return super.getCenterX() / var11.coordinateScale();
            }

            @Override
            public double getCenterZ() {
               return super.getCenterZ() / var11.coordinateScale();
            }
         };
      } else {
         this.worldBorder = new WorldBorder();
      }

      this.thread = Thread.currentThread();
      this.biomeManager = new BiomeManager(this, param7);
      this.isDebug = param6;
      this.neighborUpdater = new CollectingNeighborUpdater(this, param9);
      this.registryAccess = param2;
      this.damageSources = new DamageSources(param2);
   }

   @Override
   public boolean isClientSide() {
      return this.isClientSide;
   }

   @Nullable
   @Override
   public MinecraftServer getServer() {
      return null;
   }

   public boolean isInWorldBounds(BlockPos param0) {
      return !this.isOutsideBuildHeight(param0) && isInWorldBoundsHorizontal(param0);
   }

   public static boolean isInSpawnableBounds(BlockPos param0) {
      return !isOutsideSpawnableHeight(param0.getY()) && isInWorldBoundsHorizontal(param0);
   }

   private static boolean isInWorldBoundsHorizontal(BlockPos param0) {
      return param0.getX() >= -30000000 && param0.getZ() >= -30000000 && param0.getX() < 30000000 && param0.getZ() < 30000000;
   }

   private static boolean isOutsideSpawnableHeight(int param0) {
      return param0 < -20000000 || param0 >= 20000000;
   }

   public LevelChunk getChunkAt(BlockPos param0) {
      return this.getChunk(SectionPos.blockToSectionCoord(param0.getX()), SectionPos.blockToSectionCoord(param0.getZ()));
   }

   public LevelChunk getChunk(int param0, int param1) {
      return (LevelChunk)this.getChunk(param0, param1, ChunkStatus.FULL);
   }

   @Nullable
   @Override
   public ChunkAccess getChunk(int param0, int param1, ChunkStatus param2, boolean param3) {
      ChunkAccess var5 = this.getChunkSource().getChunk(param0, param1, param2, param3);
      if (var5 == null && param3) {
         throw new IllegalStateException("Should always be able to create a chunk!");
      } else {
         return var5;
      }
   }

   @Override
   public boolean setBlock(BlockPos param0, BlockState param1, int param2) {
      return this.setBlock(param0, param1, param2, 512);
   }

   @Override
   public boolean setBlock(BlockPos param0, BlockState param1, int param2, int param3) {
      if (this.isOutsideBuildHeight(param0)) {
         return false;
      }

      if (!this.isClientSide && this.isDebug()) {
         return false;
      }

      LevelChunk var5 = this.getChunkAt(param0);
      Block var6 = param1.getBlock();
      BlockState var7 = var5.setBlockState(param0, param1, (param2 & 64) != 0);
      if (var7 == null) {
         return false;
      }

      BlockState var8 = this.getBlockState(param0);
      if ((param2 & 128) == 0
         && var8 != var7
         && (
            var8.getLightBlock(this, param0) != var7.getLightBlock(this, param0)
               || var8.getLightEmission() != var7.getLightEmission()
               || var8.useShapeForLightOcclusion()
               || var7.useShapeForLightOcclusion()
         )) {
         this.getProfiler().push("queueCheckLight");
         this.getChunkSource().getLightEngine().checkBlock(param0);
         this.getProfiler().pop();
      }

      if (var8 == param1) {
         if (var7 != var8) {
            this.setBlocksDirty(param0, var7, var8);
         }

         if ((param2 & 2) != 0
            && (!this.isClientSide || (param2 & 4) == 0)
            && (this.isClientSide || var5.getFullStatus() != null && var5.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING))) {
            this.sendBlockUpdated(param0, var7, param1, param2);
         }

         if ((param2 & 1) != 0) {
            this.blockUpdated(param0, var7.getBlock());
            if (!this.isClientSide && param1.hasAnalogOutputSignal()) {
               this.updateNeighbourForOutputSignal(param0, var6);
            }
         }

         if ((param2 & 16) == 0 && param3 > 0) {
            int var9 = param2 & -34;
            var7.updateIndirectNeighbourShapes(this, param0, var9, param3 - 1);
            param1.updateNeighbourShapes(this, param0, var9, param3 - 1);
            param1.updateIndirectNeighbourShapes(this, param0, var9, param3 - 1);
         }

         this.onBlockStateChange(param0, var7, var8);
      }

      return true;
   }

   public void onBlockStateChange(BlockPos param0, BlockState param1, BlockState param2) {
   }

   @Override
   public boolean removeBlock(BlockPos param0, boolean param1) {
      FluidState var3 = this.getFluidState(param0);
      return this.setBlock(param0, var3.createLegacyBlock(), 3 | (param1 ? 64 : 0));
   }

   @Override
   public boolean destroyBlock(BlockPos param0, boolean param1, @Nullable Entity param2, int param3) {
      BlockState var5 = this.getBlockState(param0);
      if (var5.isAir()) {
         return false;
      }

      FluidState var6 = this.getFluidState(param0);
      if (!(var5.getBlock() instanceof BaseFireBlock)) {
         this.levelEvent(2001, param0, Block.getId(var5));
      }

      if (param1) {
         BlockEntity var7 = var5.hasBlockEntity() ? this.getBlockEntity(param0) : null;
         Block.dropResources(var5, this, param0, var7, param2, ItemStack.EMPTY);
      }

      boolean var7 = this.setBlock(param0, var6.createLegacyBlock(), 3, param3);
      if (var7) {
         this.gameEvent(GameEvent.BLOCK_DESTROY, param0, GameEvent.Context.of(param2, var5));
      }

      return var7;
   }

   public void addDestroyBlockEffect(BlockPos param0, BlockState param1) {
   }

   public boolean setBlockAndUpdate(BlockPos param0, BlockState param1) {
      return this.setBlock(param0, param1, 3);
   }

   public abstract void sendBlockUpdated(BlockPos var1, BlockState var2, BlockState var3, int var4);

   public void setBlocksDirty(BlockPos param0, BlockState param1, BlockState param2) {
   }

   public void updateNeighborsAt(BlockPos param0, Block param1) {
   }

   public void updateNeighborsAtExceptFromFacing(BlockPos param0, Block param1, Direction param2) {
   }

   public void neighborChanged(BlockPos param0, Block param1, BlockPos param2) {
   }

   public void neighborChanged(BlockState param0, BlockPos param1, Block param2, BlockPos param3, boolean param4) {
   }

   @Override
   public void neighborShapeChanged(Direction param0, BlockState param1, BlockPos param2, BlockPos param3, int param4, int param5) {
      this.neighborUpdater.shapeUpdate(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public int getHeight(Heightmap.Types param0, int param1, int param2) {
      int var4;
      if (param1 >= -30000000 && param2 >= -30000000 && param1 < 30000000 && param2 < 30000000) {
         if (this.hasChunk(SectionPos.blockToSectionCoord(param1), SectionPos.blockToSectionCoord(param2))) {
            var4 = this.getChunk(SectionPos.blockToSectionCoord(param1), SectionPos.blockToSectionCoord(param2)).getHeight(param0, param1 & 15, param2 & 15)
               + 1;
         } else {
            var4 = this.getMinBuildHeight();
         }
      } else {
         var4 = this.getSeaLevel() + 1;
      }

      return var4;
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.getChunkSource().getLightEngine();
   }

   @Override
   public BlockState getBlockState(BlockPos param0) {
      if (this.isOutsideBuildHeight(param0)) {
         return Blocks.VOID_AIR.defaultBlockState();
      }

      LevelChunk var2 = this.getChunk(SectionPos.blockToSectionCoord(param0.getX()), SectionPos.blockToSectionCoord(param0.getZ()));
      return var2.getBlockState(param0);
   }

   @Override
   public FluidState getFluidState(BlockPos param0) {
      if (this.isOutsideBuildHeight(param0)) {
         return Fluids.EMPTY.defaultFluidState();
      }

      LevelChunk var2 = this.getChunkAt(param0);
      return var2.getFluidState(param0);
   }

   public boolean isDay() {
      return !this.dimensionType().hasFixedTime() && this.skyDarken < 4;
   }

   public boolean isNight() {
      return !this.dimensionType().hasFixedTime() && !this.isDay();
   }

   public void playSound(@Nullable Entity param0, BlockPos param1, SoundEvent param2, SoundSource param3, float param4, float param5) {
      this.playSound(param0 instanceof Player var7 ? var7 : null, param1, param2, param3, param4, param5);
   }

   @Override
   public void playSound(@Nullable Player param0, BlockPos param1, SoundEvent param2, SoundSource param3, float param4, float param5) {
      this.playSound(param0, param1.getX() + 0.5, param1.getY() + 0.5, param1.getZ() + 0.5, param2, param3, param4, param5);
   }

   public abstract void playSeededSound(
      @Nullable Player var1, double var2, double var4, double var6, Holder<SoundEvent> var8, SoundSource var9, float var10, float var11, long var12
   );

   public void playSeededSound(
      @Nullable Player param0, double param1, double param3, double param5, SoundEvent param7, SoundSource param8, float param9, float param10, long param11
   ) {
      this.playSeededSound(param0, param1, param3, param5, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(param7), param8, param9, param10, param11);
   }

   public abstract void playSeededSound(@Nullable Player var1, Entity var2, Holder<SoundEvent> var3, SoundSource var4, float var5, float var6, long var7);

   public void playSound(
      @Nullable Player param0, double param1, double param3, double param5, SoundEvent param7, SoundSource param8, float param9, float param10
   ) {
      this.playSeededSound(param0, param1, param3, param5, param7, param8, param9, param10, this.threadSafeRandom.nextLong());
   }

   public void playSound(@Nullable Player param0, Entity param1, SoundEvent param2, SoundSource param3, float param4, float param5) {
      this.playSeededSound(param0, param1, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(param2), param3, param4, param5, this.threadSafeRandom.nextLong());
   }

   public void playLocalSound(BlockPos param0, SoundEvent param1, SoundSource param2, float param3, float param4, boolean param5) {
      this.playLocalSound(param0.getX() + 0.5, param0.getY() + 0.5, param0.getZ() + 0.5, param1, param2, param3, param4, param5);
   }

   public void playLocalSound(double param0, double param2, double param4, SoundEvent param6, SoundSource param7, float param8, float param9, boolean param10) {
   }

   @Override
   public void addParticle(ParticleOptions param0, double param1, double param3, double param5, double param7, double param9, double param11) {
   }

   public void addParticle(ParticleOptions param0, boolean param1, double param2, double param4, double param6, double param8, double param10, double param12) {
   }

   public void addAlwaysVisibleParticle(ParticleOptions param0, double param1, double param3, double param5, double param7, double param9, double param11) {
   }

   public void addAlwaysVisibleParticle(
      ParticleOptions param0, boolean param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
   }

   public float getSunAngle(float param0) {
      float var2 = this.getTimeOfDay(param0);
      return var2 * (float) (Math.PI * 2);
   }

   public void addBlockEntityTicker(TickingBlockEntity param0) {
      (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(param0);
   }

   protected void tickBlockEntities() {
      ProfilerFiller var1 = this.getProfiler();
      var1.push("blockEntities");
      this.tickingBlockEntities = true;
      if (!this.pendingBlockEntityTickers.isEmpty()) {
         this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
         this.pendingBlockEntityTickers.clear();
      }

      Iterator<TickingBlockEntity> var2 = this.blockEntityTickers.iterator();

      while (var2.hasNext()) {
         TickingBlockEntity var3 = var2.next();
         if (var3.isRemoved()) {
            var2.remove();
         } else if (this.shouldTickBlocksAt(var3.getPos())) {
            var3.tick();
         }
      }

      this.tickingBlockEntities = false;
      var1.pop();
   }

   public <T extends Entity> void guardEntityTick(Consumer<T> param0, T param1) {
      try {
         param0.accept(param1);
      } catch (Throwable var3) {
         CrashReport var4 = CrashReport.forThrowable(var3, "Ticking entity");
         CrashReportCategory var5 = var4.addCategory("Entity being ticked");
         param1.fillCrashReportCategory(var5);
         throw new ReportedException(var4);
      }
   }

   public boolean shouldTickDeath(Entity param0) {
      return true;
   }

   public boolean shouldTickBlocksAt(long param0) {
      return true;
   }

   public boolean shouldTickBlocksAt(BlockPos param0) {
      return this.shouldTickBlocksAt(ChunkPos.asLong(param0));
   }

   public Explosion explode(@Nullable Entity param0, double param1, double param3, double param5, float param7, Level.ExplosionInteraction param8) {
      return this.explode(param0, null, null, param1, param3, param5, param7, false, param8);
   }

   public Explosion explode(
      @Nullable Entity param0, double param1, double param3, double param5, float param7, boolean param8, Level.ExplosionInteraction param9
   ) {
      return this.explode(param0, null, null, param1, param3, param5, param7, param8, param9);
   }

   public Explosion explode(
      @Nullable Entity param0,
      @Nullable DamageSource param1,
      @Nullable ExplosionDamageCalculator param2,
      Vec3 param3,
      float param4,
      boolean param5,
      Level.ExplosionInteraction param6
   ) {
      return this.explode(param0, param1, param2, param3.x(), param3.y(), param3.z(), param4, param5, param6);
   }

   public Explosion explode(
      @Nullable Entity param0,
      @Nullable DamageSource param1,
      @Nullable ExplosionDamageCalculator param2,
      double param3,
      double param5,
      double param7,
      float param9,
      boolean param10,
      Level.ExplosionInteraction param11
   ) {
      return this.explode(param0, param1, param2, param3, param5, param7, param9, param10, param11, true);
   }

   public Explosion explode(
      @Nullable Entity param0,
      @Nullable DamageSource param1,
      @Nullable ExplosionDamageCalculator param2,
      double param3,
      double param5,
      double param7,
      float param9,
      boolean param10,
      Level.ExplosionInteraction param11,
      boolean param12
   ) {
      Explosion.BlockInteraction var14 = switch (param11) {
         case NONE -> Explosion.BlockInteraction.KEEP;
         case BLOCK -> this.getDestroyType(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY);
         case MOB -> this.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
            ? this.getDestroyType(GameRules.RULE_MOB_EXPLOSION_DROP_DECAY)
            : Explosion.BlockInteraction.KEEP;
         case TNT -> this.getDestroyType(GameRules.RULE_TNT_EXPLOSION_DROP_DECAY);
      };
      Explosion var15 = new Explosion(this, param0, param1, param2, param3, param5, param7, param9, param10, var14);
      var15.explode();
      var15.finalizeExplosion(param12);
      return var15;
   }

   private Explosion.BlockInteraction getDestroyType(GameRules.Key<GameRules.BooleanValue> param0) {
      return this.getGameRules().getBoolean(param0) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY;
   }

   public abstract String gatherChunkSourceStats();

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos param0) {
      if (this.isOutsideBuildHeight(param0)) {
         return null;
      } else {
         return !this.isClientSide && Thread.currentThread() != this.thread
            ? null
            : this.getChunkAt(param0).getBlockEntity(param0, LevelChunk.EntityCreationType.IMMEDIATE);
      }
   }

   public void setBlockEntity(BlockEntity param0) {
      BlockPos var2 = param0.getBlockPos();
      if (!this.isOutsideBuildHeight(var2)) {
         this.getChunkAt(var2).addAndRegisterBlockEntity(param0);
      }
   }

   public void removeBlockEntity(BlockPos param0) {
      if (!this.isOutsideBuildHeight(param0)) {
         this.getChunkAt(param0).removeBlockEntity(param0);
      }
   }

   public boolean isLoaded(BlockPos param0) {
      return this.isOutsideBuildHeight(param0)
         ? false
         : this.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(param0.getX()), SectionPos.blockToSectionCoord(param0.getZ()));
   }

   public boolean loadedAndEntityCanStandOnFace(BlockPos param0, Entity param1, Direction param2) {
      if (this.isOutsideBuildHeight(param0)) {
         return false;
      }

      ChunkAccess var4 = this.getChunk(SectionPos.blockToSectionCoord(param0.getX()), SectionPos.blockToSectionCoord(param0.getZ()), ChunkStatus.FULL, false);
      return var4 == null ? false : var4.getBlockState(param0).entityCanStandOnFace(this, param0, param1, param2);
   }

   public boolean loadedAndEntityCanStandOn(BlockPos param0, Entity param1) {
      return this.loadedAndEntityCanStandOnFace(param0, param1, Direction.UP);
   }

   public void updateSkyBrightness() {
      double var1 = 1.0 - this.getRainLevel(1.0F) * 5.0F / 16.0;
      double var3 = 1.0 - this.getThunderLevel(1.0F) * 5.0F / 16.0;
      double var5 = 0.5 + 2.0 * Mth.clamp(Mth.cos(this.getTimeOfDay(1.0F) * (float) (Math.PI * 2)), -0.25, 0.25);
      this.skyDarken = (int)((1.0 - var5 * var1 * var3) * 11.0);
   }

   public void setSpawnSettings(boolean param0, boolean param1) {
      this.getChunkSource().setSpawnSettings(param0, param1);
   }

   public BlockPos getSharedSpawnPos() {
      BlockPos var1 = new BlockPos(this.levelData.getXSpawn(), this.levelData.getYSpawn(), this.levelData.getZSpawn());
      if (!this.getWorldBorder().isWithinBounds(var1)) {
         var1 = this.getHeightmapPos(
            Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(this.getWorldBorder().getCenterX(), 0.0, this.getWorldBorder().getCenterZ())
         );
      }

      return var1;
   }

   public float getSharedSpawnAngle() {
      return this.levelData.getSpawnAngle();
   }

   protected void prepareWeather() {
      if (this.levelData.isRaining()) {
         this.rainLevel = 1.0F;
         if (this.levelData.isThundering()) {
            this.thunderLevel = 1.0F;
         }
      }
   }

   @Override
   public void close() throws IOException {
      this.getChunkSource().close();
   }

   @Nullable
   @Override
   public BlockGetter getChunkForCollisions(int param0, int param1) {
      return this.getChunk(param0, param1, ChunkStatus.FULL, false);
   }

   @Override
   public List<Entity> getEntities(@Nullable Entity param0, AABB param1, Predicate<? super Entity> param2) {
      this.getProfiler().incrementCounter("getEntities");
      List<Entity> var4 = Lists.newArrayList();
      this.getEntities().get(param1, l_param3 -> {
         if (l_param3 != param0 && param2.test(l_param3)) {
            var4.add(l_param3);
         }

         if (l_param3 instanceof EnderDragon) {
            for (EnderDragonPart l_var7 : ((EnderDragon)l_param3).getSubEntities()) {
               if (l_param3 != param0 && param2.test(l_var7)) {
                  var4.add(l_var7);
               }
            }
         }
      });
      return var4;
   }

   @Override
   public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> param0, AABB param1, Predicate<? super T> param2) {
      List<T> var4 = Lists.newArrayList();
      this.getEntities(param0, param1, param2, var4);
      return var4;
   }

   public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> param0, AABB param1, Predicate<? super T> param2, List<? super T> param3) {
      this.getEntities(param0, param1, param2, param3, Integer.MAX_VALUE);
   }

   public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> param0, AABB param1, Predicate<? super T> param2, List<? super T> param3, int param4) {
      this.getProfiler().incrementCounter("getEntities");
      this.getEntities().get(param0, param1, l_param4 -> {
         if (param2.test(l_param4)) {
            param3.add(l_param4);
            if (param3.size() >= param4) {
               return AbortableIterationConsumer.Continuation.ABORT;
            }
         }

         if (l_param4 instanceof EnderDragon l_var5) {
            for (EnderDragonPart l_var9 : l_var5.getSubEntities()) {
               T l_var10 = param0.tryCast(l_var9);
               if (l_var10 != null && param2.test(l_var10)) {
                  param3.add(l_var10);
                  if (param3.size() >= param4) {
                     return AbortableIterationConsumer.Continuation.ABORT;
                  }
               }
            }
         }

         return AbortableIterationConsumer.Continuation.CONTINUE;
      });
   }

   @Nullable
   public abstract Entity getEntity(int var1);

   public void blockEntityChanged(BlockPos param0) {
      if (this.hasChunkAt(param0)) {
         this.getChunkAt(param0).setUnsaved(true);
      }
   }

   @Override
   public int getSeaLevel() {
      return 63;
   }

   public int getDirectSignalTo(BlockPos param0) {
      int var2 = 0;
      var2 = Math.max(var2, this.getDirectSignal(param0.below(), Direction.DOWN));
      if (var2 >= 15) {
         return var2;
      }

      var2 = Math.max(var2, this.getDirectSignal(param0.above(), Direction.UP));
      if (var2 >= 15) {
         return var2;
      }

      var2 = Math.max(var2, this.getDirectSignal(param0.north(), Direction.NORTH));
      if (var2 >= 15) {
         return var2;
      }

      var2 = Math.max(var2, this.getDirectSignal(param0.south(), Direction.SOUTH));
      if (var2 >= 15) {
         return var2;
      }

      var2 = Math.max(var2, this.getDirectSignal(param0.west(), Direction.WEST));
      if (var2 >= 15) {
         return var2;
      }

      var2 = Math.max(var2, this.getDirectSignal(param0.east(), Direction.EAST));
      return var2 >= 15 ? var2 : var2;
   }

   public boolean hasSignal(BlockPos param0, Direction param1) {
      return this.getSignal(param0, param1) > 0;
   }

   public int getSignal(BlockPos param0, Direction param1) {
      BlockState var3 = this.getBlockState(param0);
      int var4 = var3.getSignal(this, param0, param1);
      return var3.isRedstoneConductor(this, param0) ? Math.max(var4, this.getDirectSignalTo(param0)) : var4;
   }

   public boolean hasNeighborSignal(BlockPos param0) {
      if (this.getSignal(param0.below(), Direction.DOWN) > 0) {
         return true;
      } else if (this.getSignal(param0.above(), Direction.UP) > 0) {
         return true;
      } else if (this.getSignal(param0.north(), Direction.NORTH) > 0) {
         return true;
      } else if (this.getSignal(param0.south(), Direction.SOUTH) > 0) {
         return true;
      } else {
         return this.getSignal(param0.west(), Direction.WEST) > 0 ? true : this.getSignal(param0.east(), Direction.EAST) > 0;
      }
   }

   public int getBestNeighborSignal(BlockPos param0) {
      int var2 = 0;

      for (Direction var6 : DIRECTIONS) {
         int var7 = this.getSignal(param0.relative(var6), var6);
         if (var7 >= 15) {
            return 15;
         }

         if (var7 > var2) {
            var2 = var7;
         }
      }

      return var2;
   }

   public void disconnect() {
   }

   public long getGameTime() {
      return this.levelData.getGameTime();
   }

   public long getDayTime() {
      return this.levelData.getDayTime();
   }

   public boolean mayInteract(Player param0, BlockPos param1) {
      return true;
   }

   public void broadcastEntityEvent(Entity param0, byte param1) {
   }

   public void broadcastDamageEvent(Entity param0, DamageSource param1) {
   }

   public void blockEvent(BlockPos param0, Block param1, int param2, int param3) {
      this.getBlockState(param0).triggerEvent(this, param0, param2, param3);
   }

   @Override
   public LevelData getLevelData() {
      return this.levelData;
   }

   public GameRules getGameRules() {
      return this.levelData.getGameRules();
   }

   public float getThunderLevel(float param0) {
      return Mth.lerp(param0, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(param0);
   }

   public void setThunderLevel(float param0) {
      float var2 = Mth.clamp(param0, 0.0F, 1.0F);
      this.oThunderLevel = var2;
      this.thunderLevel = var2;
   }

   public float getRainLevel(float param0) {
      return Mth.lerp(param0, this.oRainLevel, this.rainLevel);
   }

   public void setRainLevel(float param0) {
      float var2 = Mth.clamp(param0, 0.0F, 1.0F);
      this.oRainLevel = var2;
      this.rainLevel = var2;
   }

   public boolean isThundering() {
      return this.dimensionType().hasSkyLight() && !this.dimensionType().hasCeiling() ? this.getThunderLevel(1.0F) > 0.9 : false;
   }

   public boolean isRaining() {
      return this.getRainLevel(1.0F) > 0.2;
   }

   public boolean isRainingAt(BlockPos param0) {
      if (!this.isRaining()) {
         return false;
      }

      if (!this.canSeeSky(param0)) {
         return false;
      }

      if (this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, param0).getY() > param0.getY()) {
         return false;
      }

      Biome var2 = this.getBiome(param0).value();
      return var2.getPrecipitationAt(param0) == Biome.Precipitation.RAIN;
   }

   @Nullable
   public abstract MapItemSavedData getMapData(String var1);

   public abstract void setMapData(String var1, MapItemSavedData var2);

   public abstract int getFreeMapId();

   public void globalLevelEvent(int param0, BlockPos param1, int param2) {
   }

   public CrashReportCategory fillReportDetails(CrashReport param0) {
      CrashReportCategory var2 = param0.addCategory("Affected level", 1);
      var2.setDetail("All players", () -> this.players().size() + " total; " + this.players());
      var2.setDetail("Chunk stats", this.getChunkSource()::gatherStats);
      var2.setDetail("Level dimension", () -> this.dimension().location().toString());

      try {
         this.levelData.fillCrashReportCategory(var2, this);
      } catch (Throwable var3) {
         var2.setDetailError("Level Data Unobtainable", var3);
      }

      return var2;
   }

   public abstract void destroyBlockProgress(int var1, BlockPos var2, int var3);

   public void createFireworks(double param0, double param2, double param4, double param6, double param8, double param10, @Nullable CompoundTag param12) {
   }

   public abstract Scoreboard getScoreboard();

   public void updateNeighbourForOutputSignal(BlockPos param0, Block param1) {
      for (Direction var4 : Direction.Plane.HORIZONTAL) {
         BlockPos var5 = param0.relative(var4);
         if (this.hasChunkAt(var5)) {
            BlockState var6 = this.getBlockState(var5);
            if (var6.is(Blocks.COMPARATOR)) {
               this.neighborChanged(var6, var5, param1, param0, false);
            } else if (var6.isRedstoneConductor(this, var5)) {
               var5 = var5.relative(var4);
               var6 = this.getBlockState(var5);
               if (var6.is(Blocks.COMPARATOR)) {
                  this.neighborChanged(var6, var5, param1, param0, false);
               }
            }
         }
      }
   }

   @Override
   public DifficultyInstance getCurrentDifficultyAt(BlockPos param0) {
      long var2 = 0L;
      float var4 = 0.0F;
      if (this.hasChunkAt(param0)) {
         var4 = this.getMoonBrightness();
         var2 = this.getChunkAt(param0).getInhabitedTime();
      }

      return new DifficultyInstance(this.getDifficulty(), this.getDayTime(), var2, var4);
   }

   @Override
   public int getSkyDarken() {
      return this.skyDarken;
   }

   public void setSkyFlashTime(int param0) {
   }

   @Override
   public WorldBorder getWorldBorder() {
      return this.worldBorder;
   }

   public void sendPacketToServer(Packet<?> param0) {
      throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
   }

   @Override
   public DimensionType dimensionType() {
      return this.dimensionTypeRegistration.value();
   }

   public ResourceKey<DimensionType> dimensionTypeId() {
      return this.dimensionTypeId;
   }

   public Holder<DimensionType> dimensionTypeRegistration() {
      return this.dimensionTypeRegistration;
   }

   public ResourceKey<Level> dimension() {
      return this.dimension;
   }

   @Override
   public RandomSource getRandom() {
      return this.random;
   }

   @Override
   public boolean isStateAtPosition(BlockPos param0, Predicate<BlockState> param1) {
      return param1.test(this.getBlockState(param0));
   }

   @Override
   public boolean isFluidAtPosition(BlockPos param0, Predicate<FluidState> param1) {
      return param1.test(this.getFluidState(param0));
   }

   public abstract RecipeManager getRecipeManager();

   public BlockPos getBlockRandomPos(int param0, int param1, int param2, int param3) {
      this.randValue = this.randValue * 3 + 1013904223;
      int var5 = this.randValue >> 2;
      return new BlockPos(param0 + (var5 & 15), param1 + (var5 >> 16 & param3), param2 + (var5 >> 8 & 15));
   }

   public boolean noSave() {
      return false;
   }

   public ProfilerFiller getProfiler() {
      return this.profiler.get();
   }

   public Supplier<ProfilerFiller> getProfilerSupplier() {
      return this.profiler;
   }

   @Override
   public BiomeManager getBiomeManager() {
      return this.biomeManager;
   }

   public final boolean isDebug() {
      return this.isDebug;
   }

   protected abstract LevelEntityGetter<Entity> getEntities();

   @Override
   public long nextSubTickCount() {
      return this.subTickCount++;
   }

   @Override
   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public DamageSources damageSources() {
      return this.damageSources;
   }

   public enum ExplosionInteraction {
      NONE,
      BLOCK,
      MOB,
      TNT;
   }
}
