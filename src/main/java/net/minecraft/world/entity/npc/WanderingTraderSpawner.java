package net.minecraft.world.entity.npc;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;

public class WanderingTraderSpawner implements CustomSpawner {
   private static final int DEFAULT_TICK_DELAY = 1200;
   public static final int DEFAULT_SPAWN_DELAY = 24000;
   private static final int MIN_SPAWN_CHANCE = 25;
   private static final int MAX_SPAWN_CHANCE = 75;
   private static final int SPAWN_CHANCE_INCREASE = 25;
   private static final int SPAWN_ONE_IN_X_CHANCE = 10;
   private static final int NUMBER_OF_SPAWN_ATTEMPTS = 10;
   private final RandomSource random = RandomSource.create();
   private final ServerLevelData serverLevelData;
   private int tickDelay;
   private int spawnDelay;
   private int spawnChance;

   public WanderingTraderSpawner(ServerLevelData param0) {
      this.serverLevelData = param0;
      this.tickDelay = 1200;
      this.spawnDelay = param0.getWanderingTraderSpawnDelay();
      this.spawnChance = param0.getWanderingTraderSpawnChance();
      if (this.spawnDelay == 0 && this.spawnChance == 0) {
         this.spawnDelay = 24000;
         param0.setWanderingTraderSpawnDelay(this.spawnDelay);
         this.spawnChance = 25;
         param0.setWanderingTraderSpawnChance(this.spawnChance);
      }
   }

   @Override
   public int tick(ServerLevel param0, boolean param1, boolean param2) {
      if (!param0.getGameRules().getBoolean(GameRules.RULE_DO_TRADER_SPAWNING)) {
         return 0;
      } else if (--this.tickDelay > 0) {
         return 0;
      } else {
         this.tickDelay = 1200;
         this.spawnDelay -= 1200;
         this.serverLevelData.setWanderingTraderSpawnDelay(this.spawnDelay);
         if (this.spawnDelay > 0) {
            return 0;
         } else {
            this.spawnDelay = 24000;
            if (!param0.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
               return 0;
            } else {
               int var4 = this.spawnChance;
               this.spawnChance = Mth.clamp(this.spawnChance + 25, 25, 75);
               this.serverLevelData.setWanderingTraderSpawnChance(this.spawnChance);
               if (this.random.nextInt(100) > var4) {
                  return 0;
               } else if (this.spawn(param0)) {
                  this.spawnChance = 25;
                  return 1;
               } else {
                  return 0;
               }
            }
         }
      }
   }

   private boolean spawn(ServerLevel param0) {
      Player var2 = param0.getRandomPlayer();
      if (var2 == null) {
         return true;
      }

      if (this.random.nextInt(10) != 0) {
         return false;
      }

      BlockPos var3 = var2.blockPosition();
      int var4 = 48;
      PoiManager var5 = param0.getPoiManager();
      Optional<BlockPos> var6 = var5.find(l_param0 -> l_param0.is(PoiTypes.MEETING), l_param0 -> true, var3, 48, PoiManager.Occupancy.ANY);
      BlockPos var7 = var6.orElse(var3);
      BlockPos var8 = this.findSpawnPositionNear(param0, var7, 48);
      if (var8 != null && this.hasEnoughSpace(param0, var8)) {
         if (param0.getBiome(var8).is(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
            return false;
         }

         WanderingTrader var9 = EntityType.WANDERING_TRADER.spawn(param0, var8, MobSpawnType.EVENT);
         if (var9 != null) {
            for (int var10 = 0; var10 < 2; var10++) {
               this.tryToSpawnLlamaFor(param0, var9, 4);
            }

            this.serverLevelData.setWanderingTraderId(var9.getUUID());
            var9.setDespawnDelay(48000);
            var9.setWanderTarget(var7);
            var9.restrictTo(var7, 16);
            return true;
         }
      }

      return false;
   }

   private void tryToSpawnLlamaFor(ServerLevel param0, WanderingTrader param1, int param2) {
      BlockPos var4 = this.findSpawnPositionNear(param0, param1.blockPosition(), param2);
      if (var4 != null) {
         TraderLlama var5 = EntityType.TRADER_LLAMA.spawn(param0, var4, MobSpawnType.EVENT);
         if (var5 != null) {
            var5.setLeashedTo(param1, true);
         }
      }
   }

   @Nullable
   private BlockPos findSpawnPositionNear(LevelReader param0, BlockPos param1, int param2) {
      BlockPos var4 = null;

      for (int var5 = 0; var5 < 10; var5++) {
         int var6 = param1.getX() + this.random.nextInt(param2 * 2) - param2;
         int var7 = param1.getZ() + this.random.nextInt(param2 * 2) - param2;
         int var8 = param0.getHeight(Heightmap.Types.WORLD_SURFACE, var6, var7);
         BlockPos var9 = new BlockPos(var6, var8, var7);
         if (NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, param0, var9, EntityType.WANDERING_TRADER)) {
            var4 = var9;
            break;
         }
      }

      return var4;
   }

   private boolean hasEnoughSpace(BlockGetter param0, BlockPos param1) {
      for (BlockPos var4 : BlockPos.betweenClosed(param1, param1.offset(1, 2, 1))) {
         if (!param0.getBlockState(var4).getCollisionShape(param0, var4).isEmpty()) {
            return false;
         }
      }

      return true;
   }
}
