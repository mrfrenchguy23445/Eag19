package net.minecraft.world.level;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class NaturalSpawner {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MIN_SPAWN_DISTANCE = 24;
   public static final int SPAWN_DISTANCE_CHUNK = 8;
   public static final int SPAWN_DISTANCE_BLOCK = 128;
   static final int MAGIC_NUMBER = (int)Math.pow(17.0, 2.0);
   private static final MobCategory[] SPAWNING_CATEGORIES = Stream.of(MobCategory.values())
      .filter(param0 -> param0 != MobCategory.MISC)
      .toArray(MobCategory[]::new);

   private NaturalSpawner() {
   }

   public static NaturalSpawner.SpawnState createState(int param0, Iterable<Entity> param1, NaturalSpawner.ChunkGetter param2, LocalMobCapCalculator param3) {
      PotentialCalculator var4 = new PotentialCalculator();
      Object2IntOpenHashMap<MobCategory> var5 = new Object2IntOpenHashMap<>();

      for (Entity var7 : param1) {
         if (!(var7 instanceof Mob var8 && (var8.isPersistenceRequired() || var8.requiresCustomPersistence()))) {
            MobCategory var8 = var7.getType().getCategory();
            if (var8 != MobCategory.MISC) {
               BlockPos var9 = var7.blockPosition();
               param2.query(ChunkPos.asLong(var9), l_param6 -> {
                  MobSpawnSettings.MobSpawnCost l_var7 = getRoughBiome(var9, l_param6).getMobSettings().getMobSpawnCost(var7.getType());
                  if (l_var7 != null) {
                     var4.addCharge(var7.blockPosition(), l_var7.charge());
                  }

                  if (var7 instanceof Mob) {
                     param3.addMob(l_param6.getPos(), var8);
                  }

                  var5.addTo(var8, 1);
               });
            }
         }
      }

      return new NaturalSpawner.SpawnState(param0, var5, var4, param3);
   }

   static Biome getRoughBiome(BlockPos param0, ChunkAccess param1) {
      return param1.getNoiseBiome(QuartPos.fromBlock(param0.getX()), QuartPos.fromBlock(param0.getY()), QuartPos.fromBlock(param0.getZ())).value();
   }

   public static void spawnForChunk(ServerLevel param0, LevelChunk param1, NaturalSpawner.SpawnState param2, boolean param3, boolean param4, boolean param5) {
      param0.getProfiler().push("spawner");

      for (MobCategory var9 : SPAWNING_CATEGORIES) {
         if ((param3 || !var9.isFriendly())
            && (param4 || var9.isFriendly())
            && (param5 || !var9.isPersistent())
            && param2.canSpawnForCategory(var9, param1.getPos())) {
            spawnCategoryForChunk(var9, param0, param1, param2::canSpawn, param2::afterSpawn);
         }
      }

      param0.getProfiler().pop();
   }

   public static void spawnCategoryForChunk(
      MobCategory param0, ServerLevel param1, LevelChunk param2, NaturalSpawner.SpawnPredicate param3, NaturalSpawner.AfterSpawnCallback param4
   ) {
      BlockPos var5 = getRandomPosWithin(param1, param2);
      if (var5.getY() >= param1.getMinBuildHeight() + 1) {
         spawnCategoryForPosition(param0, param1, param2, var5, param3, param4);
      }
   }

   @VisibleForDebug
   public static void spawnCategoryForPosition(MobCategory param0, ServerLevel param1, BlockPos param2) {
      spawnCategoryForPosition(param0, param1, param1.getChunk(param2), param2, (l_param0, l_param1, l_param2) -> true, (l_param0, l_param1) -> {});
   }

   public static void spawnCategoryForPosition(
      MobCategory param0,
      ServerLevel param1,
      ChunkAccess param2,
      BlockPos param3,
      NaturalSpawner.SpawnPredicate param4,
      NaturalSpawner.AfterSpawnCallback param5
   ) {
      StructureManager var6 = param1.structureManager();
      ChunkGenerator var7 = param1.getChunkSource().getGenerator();
      int var8 = param3.getY();
      BlockState var9 = param2.getBlockState(param3);
      if (!var9.isRedstoneConductor(param2, param3)) {
         BlockPos.MutableBlockPos var10 = new BlockPos.MutableBlockPos();
         int var11 = 0;

         for (int var12 = 0; var12 < 3; var12++) {
            int var13 = param3.getX();
            int var14 = param3.getZ();
            int var15 = 6;
            MobSpawnSettings.SpawnerData var16 = null;
            SpawnGroupData var17 = null;
            int var18 = Mth.ceil(param1.random.nextFloat() * 4.0F);
            int var19 = 0;

            for (int var20 = 0; var20 < var18; var20++) {
               var13 += param1.random.nextInt(6) - param1.random.nextInt(6);
               var14 += param1.random.nextInt(6) - param1.random.nextInt(6);
               var10.set(var13, var8, var14);
               double var21 = var13 + 0.5;
               double var23 = var14 + 0.5;
               Player var25 = param1.getNearestPlayer(var21, var8, var23, -1.0, false);
               if (var25 != null) {
                  double var26 = var25.distanceToSqr(var21, var8, var23);
                  if (isRightDistanceToPlayerAndSpawnPoint(param1, param2, var10, var26)) {
                     if (var16 == null) {
                        Optional<MobSpawnSettings.SpawnerData> var28 = getRandomSpawnMobAt(param1, var6, var7, param0, param1.random, var10);
                        if (var28.isEmpty()) {
                           break;
                        }

                        var16 = var28.get();
                        var18 = var16.minCount + param1.random.nextInt(1 + var16.maxCount - var16.minCount);
                     }

                     if (isValidSpawnPostitionForType(param1, param0, var6, var7, var16, var10, var26) && param4.test(var16.type, var10, param2)) {
                        Mob var28 = getMobForSpawn(param1, var16.type);
                        if (var28 == null) {
                           return;
                        }

                        var28.moveTo(var21, var8, var23, param1.random.nextFloat() * 360.0F, 0.0F);
                        if (isValidPositionForMob(param1, var28, var26)) {
                           var17 = var28.finalizeSpawn(param1, param1.getCurrentDifficultyAt(var28.blockPosition()), MobSpawnType.NATURAL, var17, null);
                           var11++;
                           var19++;
                           param1.addFreshEntityWithPassengers(var28);
                           param5.run(var28, param2);
                           if (var11 >= var28.getMaxSpawnClusterSize()) {
                              return;
                           }

                           if (var28.isMaxGroupSizeReached(var19)) {
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isRightDistanceToPlayerAndSpawnPoint(ServerLevel param0, ChunkAccess param1, BlockPos.MutableBlockPos param2, double param3) {
      if (param3 <= 576.0) {
         return false;
      } else {
         return param0.getSharedSpawnPos().closerToCenterThan(new Vec3(param2.getX() + 0.5, param2.getY(), param2.getZ() + 0.5), 24.0)
            ? false
            : Objects.equals(new ChunkPos(param2), param1.getPos()) || param0.isNaturalSpawningAllowed(param2);
      }
   }

   private static boolean isValidSpawnPostitionForType(
      ServerLevel param0,
      MobCategory param1,
      StructureManager param2,
      ChunkGenerator param3,
      MobSpawnSettings.SpawnerData param4,
      BlockPos.MutableBlockPos param5,
      double param6
   ) {
      EntityType<?> var8 = param4.type;
      if (var8.getCategory() == MobCategory.MISC) {
         return false;
      }

      if (!var8.canSpawnFarFromPlayer() && param6 > var8.getCategory().getDespawnDistance() * var8.getCategory().getDespawnDistance()) {
         return false;
      }

      if (var8.canSummon() && canSpawnMobAt(param0, param2, param3, param1, param4, param5)) {
         SpawnPlacements.Type var9 = SpawnPlacements.getPlacementType(var8);
         if (!isSpawnPositionOk(var9, param0, param5, var8)) {
            return false;
         } else {
            return !SpawnPlacements.checkSpawnRules(var8, param0, MobSpawnType.NATURAL, param5, param0.random)
               ? false
               : param0.noCollision(var8.getAABB(param5.getX() + 0.5, param5.getY(), param5.getZ() + 0.5));
         }
      } else {
         return false;
      }
   }

   @Nullable
   private static Mob getMobForSpawn(ServerLevel param0, EntityType<?> param1) {
      try {
         if (param1.create(param0) instanceof Mob var2) {
            return var2;
         }

         LOGGER.warn("Can't spawn entity of type: {}", BuiltInRegistries.ENTITY_TYPE.getKey(param1));
      } catch (Exception var2) {
         LOGGER.warn("Failed to create mob", var2);
      }

      return null;
   }

   private static boolean isValidPositionForMob(ServerLevel param0, Mob param1, double param2) {
      return param2 > param1.getType().getCategory().getDespawnDistance() * param1.getType().getCategory().getDespawnDistance()
            && param1.removeWhenFarAway(param2)
         ? false
         : param1.checkSpawnRules(param0, MobSpawnType.NATURAL) && param1.checkSpawnObstruction(param0);
   }

   private static Optional<MobSpawnSettings.SpawnerData> getRandomSpawnMobAt(
      ServerLevel param0, StructureManager param1, ChunkGenerator param2, MobCategory param3, RandomSource param4, BlockPos param5
   ) {
      Holder<Biome> var6 = param0.getBiome(param5);
      return param3 == MobCategory.WATER_AMBIENT && var6.is(BiomeTags.REDUCED_WATER_AMBIENT_SPAWNS) && param4.nextFloat() < 0.98F
         ? Optional.empty()
         : mobsAt(param0, param1, param2, param3, param5, var6).getRandom(param4);
   }

   private static boolean canSpawnMobAt(
      ServerLevel param0, StructureManager param1, ChunkGenerator param2, MobCategory param3, MobSpawnSettings.SpawnerData param4, BlockPos param5
   ) {
      return mobsAt(param0, param1, param2, param3, param5, null).unwrap().contains(param4);
   }

   private static WeightedRandomList<MobSpawnSettings.SpawnerData> mobsAt(
      ServerLevel param0, StructureManager param1, ChunkGenerator param2, MobCategory param3, BlockPos param4, @Nullable Holder<Biome> param5
   ) {
      return isInNetherFortressBounds(param4, param0, param3, param1)
         ? NetherFortressStructure.FORTRESS_ENEMIES
         : param2.getMobsAt(param5 != null ? param5 : param0.getBiome(param4), param1, param3, param4);
   }

   public static boolean isInNetherFortressBounds(BlockPos param0, ServerLevel param1, MobCategory param2, StructureManager param3) {
      if (param2 == MobCategory.MONSTER && param1.getBlockState(param0.below()).is(Blocks.NETHER_BRICKS)) {
         Structure var4 = param3.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BuiltinStructures.FORTRESS);
         return var4 == null ? false : param3.getStructureAt(param0, var4).isValid();
      } else {
         return false;
      }
   }

   private static BlockPos getRandomPosWithin(Level param0, LevelChunk param1) {
      ChunkPos var2 = param1.getPos();
      int var3 = var2.getMinBlockX() + param0.random.nextInt(16);
      int var4 = var2.getMinBlockZ() + param0.random.nextInt(16);
      int var5 = param1.getHeight(Heightmap.Types.WORLD_SURFACE, var3, var4) + 1;
      int var6 = Mth.randomBetweenInclusive(param0.random, param0.getMinBuildHeight(), var5);
      return new BlockPos(var3, var6, var4);
   }

   public static boolean isValidEmptySpawnBlock(BlockGetter param0, BlockPos param1, BlockState param2, FluidState param3, EntityType<?> param4) {
      if (param2.isCollisionShapeFullBlock(param0, param1)) {
         return false;
      } else if (param2.isSignalSource()) {
         return false;
      } else if (!param3.isEmpty()) {
         return false;
      } else {
         return param2.is(BlockTags.PREVENT_MOB_SPAWNING_INSIDE) ? false : !param4.isBlockDangerous(param2);
      }
   }

   public static boolean isSpawnPositionOk(SpawnPlacements.Type param0, LevelReader param1, BlockPos param2, @Nullable EntityType<?> param3) {
      if (param0 == SpawnPlacements.Type.NO_RESTRICTIONS) {
         return true;
      }

      if (param3 != null && param1.getWorldBorder().isWithinBounds(param2)) {
         BlockState var4 = param1.getBlockState(param2);
         FluidState var5 = param1.getFluidState(param2);
         BlockPos var6 = param2.above();
         BlockPos var7 = param2.below();
         switch (param0) {
            case IN_WATER:
               return var5.is(FluidTags.WATER) && !param1.getBlockState(var6).isRedstoneConductor(param1, var6);
            case IN_LAVA:
               return var5.is(FluidTags.LAVA);
            case ON_GROUND:
            default:
               BlockState var8 = param1.getBlockState(var7);
               return !var8.isValidSpawn(param1, var7, param3)
                  ? false
                  : isValidEmptySpawnBlock(param1, param2, var4, var5, param3)
                     && isValidEmptySpawnBlock(param1, var6, param1.getBlockState(var6), param1.getFluidState(var6), param3);
         }
      } else {
         return false;
      }
   }

   public static void spawnMobsForChunkGeneration(ServerLevelAccessor param0, Holder<Biome> param1, ChunkPos param2, RandomSource param3) {
      MobSpawnSettings var4 = param1.value().getMobSettings();
      WeightedRandomList<MobSpawnSettings.SpawnerData> var5 = var4.getMobs(MobCategory.CREATURE);
      if (!var5.isEmpty()) {
         int var6 = param2.getMinBlockX();
         int var7 = param2.getMinBlockZ();

         while (param3.nextFloat() < var4.getCreatureProbability()) {
            Optional<MobSpawnSettings.SpawnerData> var8 = var5.getRandom(param3);
            if (var8.isPresent()) {
               MobSpawnSettings.SpawnerData var9 = var8.get();
               int var10 = var9.minCount + param3.nextInt(1 + var9.maxCount - var9.minCount);
               SpawnGroupData var11 = null;
               int var12 = var6 + param3.nextInt(16);
               int var13 = var7 + param3.nextInt(16);
               int var14 = var12;
               int var15 = var13;

               for (int var16 = 0; var16 < var10; var16++) {
                  boolean var17 = false;

                  for (int var18 = 0; !var17 && var18 < 4; var18++) {
                     BlockPos var19 = getTopNonCollidingPos(param0, var9.type, var12, var13);
                     if (var9.type.canSummon() && isSpawnPositionOk(SpawnPlacements.getPlacementType(var9.type), param0, var19, var9.type)) {
                        float var20 = var9.type.getWidth();
                        double var21 = Mth.clamp(var12, (double)var6 + var20, var6 + 16.0 - var20);
                        double var23 = Mth.clamp(var13, (double)var7 + var20, var7 + 16.0 - var20);
                        if (!param0.noCollision(var9.type.getAABB(var21, var19.getY(), var23))
                           || !SpawnPlacements.checkSpawnRules(
                              var9.type, param0, MobSpawnType.CHUNK_GENERATION, BlockPos.containing(var21, var19.getY(), var23), param0.getRandom()
                           )) {
                           continue;
                        }

                        Entity var25;
                        try {
                           var25 = var9.type.create(param0.getLevel());
                        } catch (Exception var26) {
                           LOGGER.warn("Failed to create mob", var26);
                           continue;
                        }

                        if (var25 == null) {
                           continue;
                        }

                        var25.moveTo(var21, var19.getY(), var23, param3.nextFloat() * 360.0F, 0.0F);
                        if (var25 instanceof Mob var26 && var26.checkSpawnRules(param0, MobSpawnType.CHUNK_GENERATION) && var26.checkSpawnObstruction(param0)) {
                           var11 = var26.finalizeSpawn(param0, param0.getCurrentDifficultyAt(var26.blockPosition()), MobSpawnType.CHUNK_GENERATION, var11, null);
                           param0.addFreshEntityWithPassengers(var26);
                           var17 = true;
                        }
                     }

                     var12 += param3.nextInt(5) - param3.nextInt(5);

                     for (var13 += param3.nextInt(5) - param3.nextInt(5);
                        var12 < var6 || var12 >= var6 + 16 || var13 < var7 || var13 >= var7 + 16;
                        var13 = var15 + param3.nextInt(5) - param3.nextInt(5)
                     ) {
                        var12 = var14 + param3.nextInt(5) - param3.nextInt(5);
                     }
                  }
               }
            }
         }
      }
   }

   private static BlockPos getTopNonCollidingPos(LevelReader param0, EntityType<?> param1, int param2, int param3) {
      int var4 = param0.getHeight(SpawnPlacements.getHeightmapType(param1), param2, param3);
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos(param2, var4, param3);
      if (param0.dimensionType().hasCeiling()) {
         do {
            var5.move(Direction.DOWN);
         } while (!param0.getBlockState(var5).isAir());

         do {
            var5.move(Direction.DOWN);
         } while (param0.getBlockState(var5).isAir() && var5.getY() > param0.getMinBuildHeight());
      }

      if (SpawnPlacements.getPlacementType(param1) == SpawnPlacements.Type.ON_GROUND) {
         BlockPos var6 = var5.below();
         if (param0.getBlockState(var6).isPathfindable(param0, var6, PathComputationType.LAND)) {
            return var6;
         }
      }

      return var5.immutable();
   }

   @FunctionalInterface
   public interface AfterSpawnCallback {
      void run(Mob var1, ChunkAccess var2);
   }

   @FunctionalInterface
   public interface ChunkGetter {
      void query(long var1, Consumer<LevelChunk> var3);
   }

   @FunctionalInterface
   public interface SpawnPredicate {
      boolean test(EntityType<?> var1, BlockPos var2, ChunkAccess var3);
   }

   public static class SpawnState {
      private final int spawnableChunkCount;
      private final Object2IntOpenHashMap<MobCategory> mobCategoryCounts;
      private final PotentialCalculator spawnPotential;
      private final Object2IntMap<MobCategory> unmodifiableMobCategoryCounts;
      private final LocalMobCapCalculator localMobCapCalculator;
      @Nullable
      private BlockPos lastCheckedPos;
      @Nullable
      private EntityType<?> lastCheckedType;
      private double lastCharge;

      SpawnState(int param0, Object2IntOpenHashMap<MobCategory> param1, PotentialCalculator param2, LocalMobCapCalculator param3) {
         this.spawnableChunkCount = param0;
         this.mobCategoryCounts = param1;
         this.spawnPotential = param2;
         this.localMobCapCalculator = param3;
         this.unmodifiableMobCategoryCounts = Object2IntMaps.unmodifiable(param1);
      }

      private boolean canSpawn(EntityType<?> param0, BlockPos param1, ChunkAccess param2) {
         this.lastCheckedPos = param1;
         this.lastCheckedType = param0;
         MobSpawnSettings.MobSpawnCost var4 = NaturalSpawner.getRoughBiome(param1, param2).getMobSettings().getMobSpawnCost(param0);
         if (var4 == null) {
            this.lastCharge = 0.0;
            return true;
         } else {
            double var5 = var4.charge();
            this.lastCharge = var5;
            double var7 = this.spawnPotential.getPotentialEnergyChange(param1, var5);
            return var7 <= var4.energyBudget();
         }
      }

      private void afterSpawn(Mob param0, ChunkAccess param1) {
         EntityType<?> var3 = param0.getType();
         BlockPos var6 = param0.blockPosition();
         double var4;
         if (var6.equals(this.lastCheckedPos) && var3 == this.lastCheckedType) {
            var4 = this.lastCharge;
         } else {
            MobSpawnSettings.MobSpawnCost var7 = NaturalSpawner.getRoughBiome(var6, param1).getMobSettings().getMobSpawnCost(var3);
            if (var7 != null) {
               var4 = var7.charge();
            } else {
               var4 = 0.0;
            }
         }

         this.spawnPotential.addCharge(var6, var4);
         MobCategory var7 = var3.getCategory();
         this.mobCategoryCounts.addTo(var7, 1);
         this.localMobCapCalculator.addMob(new ChunkPos(var6), var7);
      }

      public int getSpawnableChunkCount() {
         return this.spawnableChunkCount;
      }

      public Object2IntMap<MobCategory> getMobCategoryCounts() {
         return this.unmodifiableMobCategoryCounts;
      }

      boolean canSpawnForCategory(MobCategory param0, ChunkPos param1) {
         int var3 = param0.getMaxInstancesPerChunk() * this.spawnableChunkCount / NaturalSpawner.MAGIC_NUMBER;
         return this.mobCategoryCounts.getInt(param0) >= var3 ? false : this.localMobCapCalculator.canSpawn(param0, param1);
      }
   }
}
