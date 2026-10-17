package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class PatrolSpawner implements CustomSpawner {
   private int nextTick;

   @Override
   public int tick(ServerLevel param0, boolean param1, boolean param2) {
      if (!param1) {
         return 0;
      }

      if (!param0.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
         return 0;
      }

      RandomSource var4 = param0.random;
      this.nextTick--;
      if (this.nextTick > 0) {
         return 0;
      }

      this.nextTick = this.nextTick + 12000 + var4.nextInt(1200);
      long var5 = param0.getDayTime() / 24000L;
      if (var5 < 5L || !param0.isDay()) {
         return 0;
      }

      if (var4.nextInt(5) != 0) {
         return 0;
      }

      int var7 = param0.players().size();
      if (var7 < 1) {
         return 0;
      }

      Player var8 = param0.players().get(var4.nextInt(var7));
      if (var8.isSpectator()) {
         return 0;
      }

      if (param0.isCloseToVillage(var8.blockPosition(), 2)) {
         return 0;
      }

      int var9 = (24 + var4.nextInt(24)) * (var4.nextBoolean() ? -1 : 1);
      int var10 = (24 + var4.nextInt(24)) * (var4.nextBoolean() ? -1 : 1);
      BlockPos.MutableBlockPos var11 = var8.blockPosition().mutable().move(var9, 0, var10);
      int var12 = 10;
      if (!param0.hasChunksAt(var11.getX() - 10, var11.getZ() - 10, var11.getX() + 10, var11.getZ() + 10)) {
         return 0;
      }

      Holder<Biome> var13 = param0.getBiome(var11);
      if (var13.is(BiomeTags.WITHOUT_PATROL_SPAWNS)) {
         return 0;
      }

      int var14 = 0;
      int var15 = (int)Math.ceil(param0.getCurrentDifficultyAt(var11).getEffectiveDifficulty()) + 1;

      for (int var16 = 0; var16 < var15; var16++) {
         var14++;
         var11.setY(param0.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, var11).getY());
         if (var16 == 0) {
            if (!this.spawnPatrolMember(param0, var11, var4, true)) {
               break;
            }
         } else {
            this.spawnPatrolMember(param0, var11, var4, false);
         }

         var11.setX(var11.getX() + var4.nextInt(5) - var4.nextInt(5));
         var11.setZ(var11.getZ() + var4.nextInt(5) - var4.nextInt(5));
      }

      return var14;
   }

   private boolean spawnPatrolMember(ServerLevel param0, BlockPos param1, RandomSource param2, boolean param3) {
      BlockState var5 = param0.getBlockState(param1);
      if (!NaturalSpawner.isValidEmptySpawnBlock(param0, param1, var5, var5.getFluidState(), EntityType.PILLAGER)) {
         return false;
      }

      if (!PatrollingMonster.checkPatrollingMonsterSpawnRules(EntityType.PILLAGER, param0, MobSpawnType.PATROL, param1, param2)) {
         return false;
      }

      PatrollingMonster var6 = EntityType.PILLAGER.create(param0);
      if (var6 != null) {
         if (param3) {
            var6.setPatrolLeader(true);
            var6.findPatrolTarget();
         }

         var6.setPos(param1.getX(), param1.getY(), param1.getZ());
         var6.finalizeSpawn(param0, param0.getCurrentDifficultyAt(param1), MobSpawnType.PATROL, null, null);
         param0.addFreshEntityWithPassengers(var6);
         return true;
      } else {
         return false;
      }
   }
}
