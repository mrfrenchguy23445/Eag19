package net.minecraft.world.entity.ai.village;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class VillageSiege implements CustomSpawner {
   private static final Logger LOGGER = LogUtils.getLogger();
   private boolean hasSetupSiege;
   private VillageSiege.State siegeState = VillageSiege.State.SIEGE_DONE;
   private int zombiesToSpawn;
   private int nextSpawnTime;
   private int spawnX;
   private int spawnY;
   private int spawnZ;

   @Override
   public int tick(ServerLevel param0, boolean param1, boolean param2) {
      if (!param0.isDay() && param1) {
         float var4 = param0.getTimeOfDay(0.0F);
         if (var4 == 0.5) {
            this.siegeState = param0.random.nextInt(10) == 0 ? VillageSiege.State.SIEGE_TONIGHT : VillageSiege.State.SIEGE_DONE;
         }

         if (this.siegeState == VillageSiege.State.SIEGE_DONE) {
            return 0;
         }

         if (!this.hasSetupSiege) {
            if (!this.tryToSetupSiege(param0)) {
               return 0;
            }

            this.hasSetupSiege = true;
         }

         if (this.nextSpawnTime > 0) {
            this.nextSpawnTime--;
            return 0;
         }

         this.nextSpawnTime = 2;
         if (this.zombiesToSpawn > 0) {
            this.trySpawn(param0);
            this.zombiesToSpawn--;
         } else {
            this.siegeState = VillageSiege.State.SIEGE_DONE;
         }

         return 1;
      } else {
         this.siegeState = VillageSiege.State.SIEGE_DONE;
         this.hasSetupSiege = false;
         return 0;
      }
   }

   private boolean tryToSetupSiege(ServerLevel param0) {
      for (Player var3 : param0.players()) {
         if (!var3.isSpectator()) {
            BlockPos var4 = var3.blockPosition();
            if (param0.isVillage(var4) && !param0.getBiome(var4).is(BiomeTags.WITHOUT_ZOMBIE_SIEGES)) {
               for (int var5 = 0; var5 < 10; var5++) {
                  float var6 = param0.random.nextFloat() * (float) (Math.PI * 2);
                  this.spawnX = var4.getX() + Mth.floor(Mth.cos(var6) * 32.0F);
                  this.spawnY = var4.getY();
                  this.spawnZ = var4.getZ() + Mth.floor(Mth.sin(var6) * 32.0F);
                  if (this.findRandomSpawnPos(param0, new BlockPos(this.spawnX, this.spawnY, this.spawnZ)) != null) {
                     this.nextSpawnTime = 0;
                     this.zombiesToSpawn = 20;
                     break;
                  }
               }

               return true;
            }
         }
      }

      return false;
   }

   private void trySpawn(ServerLevel param0) {
      Vec3 var2 = this.findRandomSpawnPos(param0, new BlockPos(this.spawnX, this.spawnY, this.spawnZ));
      if (var2 != null) {
         Zombie var3;
         try {
            var3 = new Zombie(param0);
            var3.finalizeSpawn(param0, param0.getCurrentDifficultyAt(var3.blockPosition()), MobSpawnType.EVENT, null, null);
         } catch (Exception var4) {
            LOGGER.warn("Failed to create zombie for village siege at {}", var2, var4);
            return;
         }

         var3.moveTo(var2.x, var2.y, var2.z, param0.random.nextFloat() * 360.0F, 0.0F);
         param0.addFreshEntityWithPassengers(var3);
      }
   }

   @Nullable
   private Vec3 findRandomSpawnPos(ServerLevel param0, BlockPos param1) {
      for (int var3 = 0; var3 < 10; var3++) {
         int var4 = param1.getX() + param0.random.nextInt(16) - 8;
         int var5 = param1.getZ() + param0.random.nextInt(16) - 8;
         int var6 = param0.getHeight(Heightmap.Types.WORLD_SURFACE, var4, var5);
         BlockPos var7 = new BlockPos(var4, var6, var5);
         if (param0.isVillage(var7) && Monster.checkMonsterSpawnRules(EntityType.ZOMBIE, param0, MobSpawnType.EVENT, var7, param0.random)) {
            return Vec3.atBottomCenterOf(var7);
         }
      }

      return null;
   }

   enum State {
      SIEGE_CAN_ACTIVATE,
      SIEGE_TONIGHT,
      SIEGE_DONE;
   }
}
