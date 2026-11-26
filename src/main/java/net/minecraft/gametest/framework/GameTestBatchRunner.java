package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class GameTestBatchRunner {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final BlockPos firstTestNorthWestCorner;
   final ServerLevel level;
   private final GameTestTicker testTicker;
   private final int testsPerRow;
   private final List<GameTestInfo> allTestInfos;
   private final List<Pair<GameTestBatch, Collection<GameTestInfo>>> batches;
   private final BlockPos.MutableBlockPos nextTestNorthWestCorner;

   public GameTestBatchRunner(Collection<GameTestBatch> param0, BlockPos param1, Rotation param2, ServerLevel param3, GameTestTicker param4, int param5) {
      this.nextTestNorthWestCorner = param1.mutable();
      this.firstTestNorthWestCorner = param1;
      this.level = param3;
      this.testTicker = param4;
      this.testsPerRow = param5;
      this.batches = param0.stream()
         .map(
            l_param2 -> {
               Collection<GameTestInfo> l_var3 = l_param2.getTestFunctions()
                  .stream()
                  .map(l2_param2 -> new GameTestInfo(l2_param2, param2, param3))
                  .collect(ImmutableList.toImmutableList());
               return Pair.of(l_param2, l_var3);
            }
         )
         .collect(ImmutableList.toImmutableList());
      this.allTestInfos = this.batches.stream().flatMap(l_param0 -> l_param0.getSecond().stream()).collect(ImmutableList.toImmutableList());
   }

   public List<GameTestInfo> getTestInfos() {
      return this.allTestInfos;
   }

   public void start() {
      this.runBatch(0);
   }

   void runBatch(final int param0) {
      if (param0 < this.batches.size()) {
         Pair<GameTestBatch, Collection<GameTestInfo>> var2 = this.batches.get(param0);
         final GameTestBatch var3 = var2.getFirst();
         Collection<GameTestInfo> var4 = var2.getSecond();
         Map<GameTestInfo, BlockPos> var5 = this.createStructuresForBatch(var4);
         String var6 = var3.getName();
         LOGGER.info("Running test batch '{}' ({} tests)...", var6, var4.size());
         var3.runBeforeBatchFunction(this.level);
         final MultipleTestTracker var7 = new MultipleTestTracker();
         var4.forEach(var7::addTestToTrack);
         var7.addListener(new GameTestListener() {
            private void testCompleted() {
               if (var7.isDone()) {
                  var3.runAfterBatchFunction(GameTestBatchRunner.this.level);
                  GameTestBatchRunner.this.runBatch(param0 + 1);
               }
            }

            @Override
            public void testStructureLoaded(GameTestInfo l_param0) {
            }

            @Override
            public void testPassed(GameTestInfo l_param0) {
               this.testCompleted();
            }

            @Override
            public void testFailed(GameTestInfo l_param0) {
               this.testCompleted();
            }
         });
         var4.forEach(l_param1 -> {
            BlockPos l_var3 = var5.get(l_param1);
            GameTestRunner.runTest(l_param1, l_var3, this.testTicker);
         });
      }
   }

   private Map<GameTestInfo, BlockPos> createStructuresForBatch(Collection<GameTestInfo> param0) {
      Map<GameTestInfo, BlockPos> var2 = Maps.newHashMap();
      int var3 = 0;
      AABB var4 = new AABB(this.nextTestNorthWestCorner);

      for (GameTestInfo var6 : param0) {
         BlockPos var7 = new BlockPos(this.nextTestNorthWestCorner);
         StructureBlockEntity var8 = StructureUtils.spawnStructure(var6.getStructureName(), var7, var6.getRotation(), 2, this.level, true);
         AABB var9 = StructureUtils.getStructureBounds(var8);
         var6.setStructureBlockPos(var8.getBlockPos());
         var2.put(var6, new BlockPos(this.nextTestNorthWestCorner));
         var4 = var4.minmax(var9);
         this.nextTestNorthWestCorner.move((int)var9.getXsize() + 5, 0, 0);
         if (var3++ % this.testsPerRow == this.testsPerRow - 1) {
            this.nextTestNorthWestCorner.move(0, 0, (int)var4.getZsize() + 6);
            this.nextTestNorthWestCorner.setX(this.firstTestNorthWestCorner.getX());
            var4 = new AABB(this.nextTestNorthWestCorner);
         }
      }

      return var2;
   }
}
