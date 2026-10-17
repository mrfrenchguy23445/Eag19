package net.minecraft.world.level.dimension.end;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;

public enum DragonRespawnAnimation {
   START {
      @Override
      public void tick(ServerLevel param0, EndDragonFight param1, List<EndCrystal> param2, int param3, BlockPos param4) {
         BlockPos var6 = new BlockPos(0, 128, 0);

         for (EndCrystal var8 : param2) {
            var8.setBeamTarget(var6);
         }

         param1.setRespawnStage(PREPARING_TO_SUMMON_PILLARS);
      }
   },
   PREPARING_TO_SUMMON_PILLARS {
      @Override
      public void tick(ServerLevel param0, EndDragonFight param1, List<EndCrystal> param2, int param3, BlockPos param4) {
         if (param3 < 100) {
            if (param3 == 0 || param3 == 50 || param3 == 51 || param3 == 52 || param3 >= 95) {
               param0.levelEvent(3001, new BlockPos(0, 128, 0), 0);
            }
         } else {
            param1.setRespawnStage(SUMMONING_PILLARS);
         }
      }
   },
   SUMMONING_PILLARS {
      @Override
      public void tick(ServerLevel param0, EndDragonFight param1, List<EndCrystal> param2, int param3, BlockPos param4) {
         int var6 = 40;
         boolean var7 = param3 % 40 == 0;
         boolean var8 = param3 % 40 == 39;
         if (var7 || var8) {
            List<SpikeFeature.EndSpike> var9 = SpikeFeature.getSpikesForLevel(param0);
            int var10 = param3 / 40;
            if (var10 < var9.size()) {
               SpikeFeature.EndSpike var11 = var9.get(var10);
               if (var7) {
                  for (EndCrystal var13 : param2) {
                     var13.setBeamTarget(new BlockPos(var11.getCenterX(), var11.getHeight() + 1, var11.getCenterZ()));
                  }
               } else {
                  int var12 = 10;

                  for (BlockPos var14 : BlockPos.betweenClosed(
                     new BlockPos(var11.getCenterX() - 10, var11.getHeight() - 10, var11.getCenterZ() - 10),
                     new BlockPos(var11.getCenterX() + 10, var11.getHeight() + 10, var11.getCenterZ() + 10)
                  )) {
                     param0.removeBlock(var14, false);
                  }

                  param0.explode(null, var11.getCenterX() + 0.5F, var11.getHeight(), var11.getCenterZ() + 0.5F, 5.0F, Level.ExplosionInteraction.BLOCK);
                  SpikeConfiguration var13 = new SpikeConfiguration(true, ImmutableList.of(var11), new BlockPos(0, 128, 0));
                  Feature.END_SPIKE
                     .place(
                        var13, param0, param0.getChunkSource().getGenerator(), RandomSource.create(), new BlockPos(var11.getCenterX(), 45, var11.getCenterZ())
                     );
               }
            } else if (var7) {
               param1.setRespawnStage(SUMMONING_DRAGON);
            }
         }
      }
   },
   SUMMONING_DRAGON {
      @Override
      public void tick(ServerLevel param0, EndDragonFight param1, List<EndCrystal> param2, int param3, BlockPos param4) {
         if (param3 >= 100) {
            param1.setRespawnStage(END);
            param1.resetSpikeCrystals();

            for (EndCrystal var7 : param2) {
               var7.setBeamTarget(null);
               param0.explode(var7, var7.getX(), var7.getY(), var7.getZ(), 6.0F, Level.ExplosionInteraction.NONE);
               var7.discard();
            }
         } else if (param3 >= 80) {
            param0.levelEvent(3001, new BlockPos(0, 128, 0), 0);
         } else if (param3 == 0) {
            for (EndCrystal var7 : param2) {
               var7.setBeamTarget(new BlockPos(0, 128, 0));
            }
         } else if (param3 < 5) {
            param0.levelEvent(3001, new BlockPos(0, 128, 0), 0);
         }
      }
   },
   END {
      @Override
      public void tick(ServerLevel param0, EndDragonFight param1, List<EndCrystal> param2, int param3, BlockPos param4) {
      }
   };

   public abstract void tick(ServerLevel var1, EndDragonFight var2, List<EndCrystal> var3, int var4, BlockPos var5);
}
