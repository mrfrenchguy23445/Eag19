package net.minecraft.world.entity.npc;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.phys.AABB;

public class CatSpawner implements CustomSpawner {
   private static final int TICK_DELAY = 1200;
   private int nextTick;

   @Override
   public int tick(ServerLevel param0, boolean param1, boolean param2) {
      if (param2 && param0.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
         this.nextTick--;
         if (this.nextTick > 0) {
            return 0;
         }

         this.nextTick = 1200;
         Player var4 = param0.getRandomPlayer();
         if (var4 == null) {
            return 0;
         }

         RandomSource var5 = param0.random;
         int var6 = (8 + var5.nextInt(24)) * (var5.nextBoolean() ? -1 : 1);
         int var7 = (8 + var5.nextInt(24)) * (var5.nextBoolean() ? -1 : 1);
         BlockPos var8 = var4.blockPosition().offset(var6, 0, var7);
         int var9 = 10;
         if (!param0.hasChunksAt(var8.getX() - 10, var8.getZ() - 10, var8.getX() + 10, var8.getZ() + 10)) {
            return 0;
         }

         if (NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, param0, var8, EntityType.CAT)) {
            if (param0.isCloseToVillage(var8, 2)) {
               return this.spawnInVillage(param0, var8);
            }

            if (param0.structureManager().getStructureWithPieceAt(var8, StructureTags.CATS_SPAWN_IN).isValid()) {
               return this.spawnInHut(param0, var8);
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private int spawnInVillage(ServerLevel param0, BlockPos param1) {
      int var3 = 48;
      if (param0.getPoiManager().getCountInRange(l_param0 -> l_param0.is(PoiTypes.HOME), param1, 48, PoiManager.Occupancy.IS_OCCUPIED) > 4L) {
         List<Cat> var4 = param0.getEntitiesOfClass(Cat.class, new AABB(param1).inflate(48.0, 8.0, 48.0));
         if (var4.size() < 5) {
            return this.spawnCat(param1, param0);
         }
      }

      return 0;
   }

   private int spawnInHut(ServerLevel param0, BlockPos param1) {
      int var3 = 16;
      List<Cat> var4 = param0.getEntitiesOfClass(Cat.class, new AABB(param1).inflate(16.0, 8.0, 16.0));
      return var4.size() < 1 ? this.spawnCat(param1, param0) : 0;
   }

   private int spawnCat(BlockPos param0, ServerLevel param1) {
      Cat var3 = EntityType.CAT.create(param1);
      if (var3 == null) {
         return 0;
      }

      var3.finalizeSpawn(param1, param1.getCurrentDifficultyAt(param0), MobSpawnType.NATURAL, null, null);
      var3.moveTo(param0, 0.0F, 0.0F);
      param1.addFreshEntityWithPassengers(var3);
      return 1;
   }
}
