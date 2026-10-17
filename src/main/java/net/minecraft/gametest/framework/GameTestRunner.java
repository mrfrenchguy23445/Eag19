package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.MutableInt;

public class GameTestRunner {
   private static final int MAX_TESTS_PER_BATCH = 100;
   public static final int PADDING_AROUND_EACH_STRUCTURE = 2;
   public static final int SPACE_BETWEEN_COLUMNS = 5;
   public static final int SPACE_BETWEEN_ROWS = 6;
   public static final int DEFAULT_TESTS_PER_ROW = 8;

   public static void runTest(GameTestInfo param0, BlockPos param1, GameTestTicker param2) {
      param0.startExecution();
      param2.add(param0);
      param0.addListener(new ReportGameListener(param0, param2, param1));
      param0.spawnStructure(param1, 2);
   }

   public static Collection<GameTestInfo> runTestBatches(
      Collection<GameTestBatch> param0, BlockPos param1, Rotation param2, ServerLevel param3, GameTestTicker param4, int param5
   ) {
      GameTestBatchRunner var6 = new GameTestBatchRunner(param0, param1, param2, param3, param4, param5);
      var6.start();
      return var6.getTestInfos();
   }

   public static Collection<GameTestInfo> runTests(
      Collection<TestFunction> param0, BlockPos param1, Rotation param2, ServerLevel param3, GameTestTicker param4, int param5
   ) {
      return runTestBatches(groupTestsIntoBatches(param0), param1, param2, param3, param4, param5);
   }

   public static Collection<GameTestBatch> groupTestsIntoBatches(Collection<TestFunction> param0) {
      Map<String, List<TestFunction>> var1 = param0.stream().collect(Collectors.groupingBy(TestFunction::getBatchName));
      return var1.entrySet()
         .stream()
         .flatMap(
            l_param0 -> {
               String l_var1 = l_param0.getKey();
               Consumer<ServerLevel> l_var2 = GameTestRegistry.getBeforeBatchFunction(l_var1);
               Consumer<ServerLevel> l_var3 = GameTestRegistry.getAfterBatchFunction(l_var1);
               MutableInt l_var4 = new MutableInt();
               Collection<TestFunction> l_var5 = l_param0.getValue();
               return Streams.stream(Iterables.partition(l_var5, 100))
                  .map(
                     l2_param4 -> new GameTestBatch(
                        l_var1 + ":" + l_var4.incrementAndGet(), ImmutableList.copyOf((Collection<? extends TestFunction>)l2_param4), l_var2, l_var3
                     )
                  );
            }
         )
         .collect(ImmutableList.toImmutableList());
   }

   public static void clearAllTests(ServerLevel param0, BlockPos param1, GameTestTicker param2, int param3) {
      param2.clear();
      BlockPos var4 = param1.offset(-param3, 0, -param3);
      BlockPos var5 = param1.offset(param3, 0, param3);
      BlockPos.betweenClosedStream(var4, var5).filter(l_param1 -> param0.getBlockState(l_param1).is(Blocks.STRUCTURE_BLOCK)).forEach(l_param1 -> {
         StructureBlockEntity l_var2 = (StructureBlockEntity)param0.getBlockEntity(l_param1);
         BlockPos l_var3 = l_var2.getBlockPos();
         BoundingBox l_var4 = StructureUtils.getStructureBoundingBox(l_var2);
         StructureUtils.clearSpaceForStructure(l_var4, l_var3.getY(), param0);
      });
   }

   public static void clearMarkers(ServerLevel param0) {
      DebugPackets.sendGameTestClearPacket(param0);
   }
}
