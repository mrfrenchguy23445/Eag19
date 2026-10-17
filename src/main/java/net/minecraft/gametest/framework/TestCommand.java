package net.minecraft.gametest.framework;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.io.IOUtils;

public class TestCommand {
   private static final int DEFAULT_CLEAR_RADIUS = 200;
   private static final int MAX_CLEAR_RADIUS = 1024;
   private static final int STRUCTURE_BLOCK_NEARBY_SEARCH_RADIUS = 15;
   private static final int STRUCTURE_BLOCK_FULL_SEARCH_RADIUS = 200;
   private static final int TEST_POS_Z_OFFSET_FROM_PLAYER = 3;
   private static final int SHOW_POS_DURATION_MS = 10000;
   private static final int DEFAULT_X_SIZE = 5;
   private static final int DEFAULT_Y_SIZE = 5;
   private static final int DEFAULT_Z_SIZE = 5;

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("test")
            .then(Commands.literal("runthis").executes(l_param0 -> runNearbyTest(l_param0.getSource())))
            .then(Commands.literal("runthese").executes(l_param0 -> runAllNearbyTests(l_param0.getSource())))
            .then(
               Commands.literal("runfailed")
                  .executes(l_param0 -> runLastFailedTests(l_param0.getSource(), false, 0, 8))
                  .then(
                     Commands.argument("onlyRequiredTests", BoolArgumentType.bool())
                        .executes(l_param0 -> runLastFailedTests(l_param0.getSource(), BoolArgumentType.getBool(l_param0, "onlyRequiredTests"), 0, 8))
                        .then(
                           Commands.argument("rotationSteps", IntegerArgumentType.integer())
                              .executes(
                                 l_param0 -> runLastFailedTests(
                                    l_param0.getSource(),
                                    BoolArgumentType.getBool(l_param0, "onlyRequiredTests"),
                                    IntegerArgumentType.getInteger(l_param0, "rotationSteps"),
                                    8
                                 )
                              )
                              .then(
                                 Commands.argument("testsPerRow", IntegerArgumentType.integer())
                                    .executes(
                                       l_param0 -> runLastFailedTests(
                                          l_param0.getSource(),
                                          BoolArgumentType.getBool(l_param0, "onlyRequiredTests"),
                                          IntegerArgumentType.getInteger(l_param0, "rotationSteps"),
                                          IntegerArgumentType.getInteger(l_param0, "testsPerRow")
                                       )
                                    )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("run")
                  .then(
                     Commands.argument("testName", TestFunctionArgument.testFunctionArgument())
                        .executes(l_param0 -> runTest(l_param0.getSource(), TestFunctionArgument.getTestFunction(l_param0, "testName"), 0))
                        .then(
                           Commands.argument("rotationSteps", IntegerArgumentType.integer())
                              .executes(
                                 l_param0 -> runTest(
                                    l_param0.getSource(),
                                    TestFunctionArgument.getTestFunction(l_param0, "testName"),
                                    IntegerArgumentType.getInteger(l_param0, "rotationSteps")
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("runall")
                  .executes(l_param0 -> runAllTests(l_param0.getSource(), 0, 8))
                  .then(
                     Commands.argument("testClassName", TestClassNameArgument.testClassName())
                        .executes(l_param0 -> runAllTestsInClass(l_param0.getSource(), TestClassNameArgument.getTestClassName(l_param0, "testClassName"), 0, 8))
                        .then(
                           Commands.argument("rotationSteps", IntegerArgumentType.integer())
                              .executes(
                                 l_param0 -> runAllTestsInClass(
                                    l_param0.getSource(),
                                    TestClassNameArgument.getTestClassName(l_param0, "testClassName"),
                                    IntegerArgumentType.getInteger(l_param0, "rotationSteps"),
                                    8
                                 )
                              )
                              .then(
                                 Commands.argument("testsPerRow", IntegerArgumentType.integer())
                                    .executes(
                                       l_param0 -> runAllTestsInClass(
                                          l_param0.getSource(),
                                          TestClassNameArgument.getTestClassName(l_param0, "testClassName"),
                                          IntegerArgumentType.getInteger(l_param0, "rotationSteps"),
                                          IntegerArgumentType.getInteger(l_param0, "testsPerRow")
                                       )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.argument("rotationSteps", IntegerArgumentType.integer())
                        .executes(l_param0 -> runAllTests(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "rotationSteps"), 8))
                        .then(
                           Commands.argument("testsPerRow", IntegerArgumentType.integer())
                              .executes(
                                 l_param0 -> runAllTests(
                                    l_param0.getSource(),
                                    IntegerArgumentType.getInteger(l_param0, "rotationSteps"),
                                    IntegerArgumentType.getInteger(l_param0, "testsPerRow")
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("export")
                  .then(
                     Commands.argument("testName", StringArgumentType.word())
                        .executes(l_param0 -> exportTestStructure(l_param0.getSource(), StringArgumentType.getString(l_param0, "testName")))
                  )
            )
            .then(Commands.literal("exportthis").executes(l_param0 -> exportNearestTestStructure(l_param0.getSource())))
            .then(
               Commands.literal("import")
                  .then(
                     Commands.argument("testName", StringArgumentType.word())
                        .executes(l_param0 -> importTestStructure(l_param0.getSource(), StringArgumentType.getString(l_param0, "testName")))
                  )
            )
            .then(
               Commands.literal("pos")
                  .executes(l_param0 -> showPos(l_param0.getSource(), "pos"))
                  .then(
                     Commands.argument("var", StringArgumentType.word())
                        .executes(l_param0 -> showPos(l_param0.getSource(), StringArgumentType.getString(l_param0, "var")))
                  )
            )
            .then(
               Commands.literal("create")
                  .then(
                     Commands.argument("testName", StringArgumentType.word())
                        .executes(l_param0 -> createNewStructure(l_param0.getSource(), StringArgumentType.getString(l_param0, "testName"), 5, 5, 5))
                        .then(
                           Commands.argument("width", IntegerArgumentType.integer())
                              .executes(
                                 l_param0 -> createNewStructure(
                                    l_param0.getSource(),
                                    StringArgumentType.getString(l_param0, "testName"),
                                    IntegerArgumentType.getInteger(l_param0, "width"),
                                    IntegerArgumentType.getInteger(l_param0, "width"),
                                    IntegerArgumentType.getInteger(l_param0, "width")
                                 )
                              )
                              .then(
                                 Commands.argument("height", IntegerArgumentType.integer())
                                    .then(
                                       Commands.argument("depth", IntegerArgumentType.integer())
                                          .executes(
                                             l_param0 -> createNewStructure(
                                                l_param0.getSource(),
                                                StringArgumentType.getString(l_param0, "testName"),
                                                IntegerArgumentType.getInteger(l_param0, "width"),
                                                IntegerArgumentType.getInteger(l_param0, "height"),
                                                IntegerArgumentType.getInteger(l_param0, "depth")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("clearall")
                  .executes(l_param0 -> clearAllTests(l_param0.getSource(), 200))
                  .then(
                     Commands.argument("radius", IntegerArgumentType.integer())
                        .executes(l_param0 -> clearAllTests(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "radius")))
                  )
            )
      );
   }

   private static int createNewStructure(CommandSourceStack param0, String param1, int param2, int param3, int param4) {
      if (param2 <= 48 && param3 <= 48 && param4 <= 48) {
         ServerLevel var5 = param0.getLevel();
         BlockPos var6 = BlockPos.containing(param0.getPosition());
         BlockPos var7 = new BlockPos(var6.getX(), param0.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, var6).getY(), var6.getZ() + 3);
         StructureUtils.createNewEmptyStructureBlock(param1.toLowerCase(), var7, new Vec3i(param2, param3, param4), Rotation.NONE, var5);

         for (int var8 = 0; var8 < param2; var8++) {
            for (int var9 = 0; var9 < param4; var9++) {
               BlockPos var10 = new BlockPos(var7.getX() + var8, var7.getY() + 1, var7.getZ() + var9);
               Block var11 = Blocks.POLISHED_ANDESITE;
               BlockInput var12 = new BlockInput(var11.defaultBlockState(), Collections.emptySet(), null);
               var12.place(var5, var10, 2);
            }
         }

         StructureUtils.addCommandBlockAndButtonToStartTest(var7, new BlockPos(1, 0, -1), Rotation.NONE, var5);
         return 0;
      } else {
         throw new IllegalArgumentException("The structure must be less than 48 blocks big in each axis");
      }
   }

   private static int showPos(CommandSourceStack param0, String param1) throws CommandSyntaxException {
      BlockHitResult var2 = (BlockHitResult)param0.getPlayerOrException().pick(10.0, 1.0F, false);
      BlockPos var3 = var2.getBlockPos();
      ServerLevel var4 = param0.getLevel();
      Optional<BlockPos> var5 = StructureUtils.findStructureBlockContainingPos(var3, 15, var4);
      if (!var5.isPresent()) {
         var5 = StructureUtils.findStructureBlockContainingPos(var3, 200, var4);
      }

      if (!var5.isPresent()) {
         param0.sendFailure(Component.literal("Can't find a structure block that contains the targeted pos " + var3));
         return 0;
      } else {
         StructureBlockEntity var6 = (StructureBlockEntity)var4.getBlockEntity(var5.get());
         BlockPos var7 = var3.subtract(var5.get());
         String var8 = var7.getX() + ", " + var7.getY() + ", " + var7.getZ();
         String var9 = var6.getStructurePath();
         Component var10 = Component.literal(var8)
            .setStyle(
               Style.EMPTY
                  .withBold(true)
                  .withColor(ChatFormatting.GREEN)
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy to clipboard")))
                  .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "final BlockPos " + param1 + " = new BlockPos(" + var8 + ");"))
            );
         param0.sendSuccess(Component.literal("Position relative to " + var9 + ": ").append(var10), false);
         DebugPackets.sendGameTestAddMarker(var4, new BlockPos(var3), var8, -2147418368, 10000);
         return 1;
      }
   }

   private static int runNearbyTest(CommandSourceStack param0) {
      BlockPos var1 = BlockPos.containing(param0.getPosition());
      ServerLevel var2 = param0.getLevel();
      BlockPos var3 = StructureUtils.findNearestStructureBlock(var1, 15, var2);
      if (var3 == null) {
         say(var2, "Couldn't find any structure block within 15 radius", ChatFormatting.RED);
         return 0;
      } else {
         GameTestRunner.clearMarkers(var2);
         runTest(var2, var3, null);
         return 1;
      }
   }

   private static int runAllNearbyTests(CommandSourceStack param0) {
      BlockPos var1 = BlockPos.containing(param0.getPosition());
      ServerLevel var2 = param0.getLevel();
      Collection<BlockPos> var3 = StructureUtils.findStructureBlocks(var1, 200, var2);
      if (var3.isEmpty()) {
         say(var2, "Couldn't find any structure blocks within 200 block radius", ChatFormatting.RED);
         return 1;
      } else {
         GameTestRunner.clearMarkers(var2);
         say(param0, "Running " + var3.size() + " tests...");
         MultipleTestTracker var4 = new MultipleTestTracker();
         var3.forEach(l_param2 -> runTest(var2, l_param2, var4));
         return 1;
      }
   }

   private static void runTest(ServerLevel param0, BlockPos param1, @Nullable MultipleTestTracker param2) {
      StructureBlockEntity var3 = (StructureBlockEntity)param0.getBlockEntity(param1);
      String var4 = var3.getStructurePath();
      TestFunction var5 = GameTestRegistry.getTestFunction(var4);
      GameTestInfo var6 = new GameTestInfo(var5, var3.getRotation(), param0);
      if (param2 != null) {
         param2.addTestToTrack(var6);
         var6.addListener(new TestCommand.TestSummaryDisplayer(param0, param2));
      }

      runTestPreparation(var5, param0);
      AABB var7 = StructureUtils.getStructureBounds(var3);
      BlockPos var8 = BlockPos.containing(var7.minX, var7.minY, var7.minZ);
      GameTestRunner.runTest(var6, var8, GameTestTicker.SINGLETON);
   }

   static void showTestSummaryIfAllDone(ServerLevel param0, MultipleTestTracker param1) {
      if (param1.isDone()) {
         say(param0, "GameTest done! " + param1.getTotalCount() + " tests were run", ChatFormatting.WHITE);
         if (param1.hasFailedRequired()) {
            say(param0, param1.getFailedRequiredCount() + " required tests failed :(", ChatFormatting.RED);
         } else {
            say(param0, "All required tests passed :)", ChatFormatting.GREEN);
         }

         if (param1.hasFailedOptional()) {
            say(param0, param1.getFailedOptionalCount() + " optional tests failed", ChatFormatting.GRAY);
         }
      }
   }

   private static int clearAllTests(CommandSourceStack param0, int param1) {
      ServerLevel var2 = param0.getLevel();
      GameTestRunner.clearMarkers(var2);
      BlockPos var3 = BlockPos.containing(
         param0.getPosition().x,
         param0.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, BlockPos.containing(param0.getPosition())).getY(),
         param0.getPosition().z
      );
      GameTestRunner.clearAllTests(var2, var3, GameTestTicker.SINGLETON, Mth.clamp(param1, 0, 1024));
      return 1;
   }

   private static int runTest(CommandSourceStack param0, TestFunction param1, int param2) {
      ServerLevel var3 = param0.getLevel();
      BlockPos var4 = BlockPos.containing(param0.getPosition());
      int var5 = param0.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, var4).getY();
      BlockPos var6 = new BlockPos(var4.getX(), var5, var4.getZ() + 3);
      GameTestRunner.clearMarkers(var3);
      runTestPreparation(param1, var3);
      Rotation var7 = StructureUtils.getRotationForRotationSteps(param2);
      GameTestInfo var8 = new GameTestInfo(param1, var7, var3);
      GameTestRunner.runTest(var8, var6, GameTestTicker.SINGLETON);
      return 1;
   }

   private static void runTestPreparation(TestFunction param0, ServerLevel param1) {
      Consumer<ServerLevel> var2 = GameTestRegistry.getBeforeBatchFunction(param0.getBatchName());
      if (var2 != null) {
         var2.accept(param1);
      }
   }

   private static int runAllTests(CommandSourceStack param0, int param1, int param2) {
      GameTestRunner.clearMarkers(param0.getLevel());
      Collection<TestFunction> var3 = GameTestRegistry.getAllTestFunctions();
      say(param0, "Running all " + var3.size() + " tests...");
      GameTestRegistry.forgetFailedTests();
      runTests(param0, var3, param1, param2);
      return 1;
   }

   private static int runAllTestsInClass(CommandSourceStack param0, String param1, int param2, int param3) {
      Collection<TestFunction> var4 = GameTestRegistry.getTestFunctionsForClassName(param1);
      GameTestRunner.clearMarkers(param0.getLevel());
      say(param0, "Running " + var4.size() + " tests from " + param1 + "...");
      GameTestRegistry.forgetFailedTests();
      runTests(param0, var4, param2, param3);
      return 1;
   }

   private static int runLastFailedTests(CommandSourceStack param0, boolean param1, int param2, int param3) {
      Collection<TestFunction> var4;
      if (param1) {
         var4 = GameTestRegistry.getLastFailedTests().stream().filter(TestFunction::isRequired).collect(Collectors.toList());
      } else {
         var4 = GameTestRegistry.getLastFailedTests();
      }

      if (var4.isEmpty()) {
         say(param0, "No failed tests to rerun");
         return 0;
      } else {
         GameTestRunner.clearMarkers(param0.getLevel());
         say(param0, "Rerunning " + var4.size() + " failed tests (" + (param1 ? "only required tests" : "including optional tests") + ")");
         runTests(param0, var4, param2, param3);
         return 1;
      }
   }

   private static void runTests(CommandSourceStack param0, Collection<TestFunction> param1, int param2, int param3) {
      BlockPos var4 = BlockPos.containing(param0.getPosition());
      BlockPos var5 = new BlockPos(var4.getX(), param0.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, var4).getY(), var4.getZ() + 3);
      ServerLevel var6 = param0.getLevel();
      Rotation var7 = StructureUtils.getRotationForRotationSteps(param2);
      Collection<GameTestInfo> var8 = GameTestRunner.runTests(param1, var5, var7, var6, GameTestTicker.SINGLETON, param3);
      MultipleTestTracker var9 = new MultipleTestTracker(var8);
      var9.addListener(new TestCommand.TestSummaryDisplayer(var6, var9));
      var9.addFailureListener(l_param0 -> GameTestRegistry.rememberFailedTest(l_param0.getTestFunction()));
   }

   private static void say(CommandSourceStack param0, String param1) {
      param0.sendSuccess(Component.literal(param1), false);
   }

   private static int exportNearestTestStructure(CommandSourceStack param0) {
      BlockPos var1 = BlockPos.containing(param0.getPosition());
      ServerLevel var2 = param0.getLevel();
      BlockPos var3 = StructureUtils.findNearestStructureBlock(var1, 15, var2);
      if (var3 == null) {
         say(var2, "Couldn't find any structure block within 15 radius", ChatFormatting.RED);
         return 0;
      } else {
         StructureBlockEntity var4 = (StructureBlockEntity)var2.getBlockEntity(var3);
         String var5 = var4.getStructurePath();
         return exportTestStructure(param0, var5);
      }
   }

   private static int exportTestStructure(CommandSourceStack param0, String param1) {
      Path var2 = Paths.get(StructureUtils.testStructuresDir);
      ResourceLocation var3 = new ResourceLocation("minecraft", param1);
      Path var4 = param0.getLevel().getStructureManager().getPathToGeneratedStructure(var3, ".nbt");
      Path var5 = NbtToSnbt.convertStructure(CachedOutput.NO_CACHE, var4, param1, var2);
      if (var5 == null) {
         say(param0, "Failed to export " + var4);
         return 1;
      }

      try {
         Files.createDirectories(var5.getParent());
      } catch (IOException var6) {
         say(param0, "Could not create folder " + var5.getParent());
         var6.printStackTrace();
         return 1;
      }

      say(param0, "Exported " + param1 + " to " + var5.toAbsolutePath());
      return 0;
   }

   private static int importTestStructure(CommandSourceStack param0, String param1) {
      Path var2 = Paths.get(StructureUtils.testStructuresDir, param1 + ".snbt");
      ResourceLocation var3 = new ResourceLocation("minecraft", param1);
      Path var4 = param0.getLevel().getStructureManager().getPathToGeneratedStructure(var3, ".nbt");

      try {
         BufferedReader var5 = Files.newBufferedReader(var2);
         String var6 = IOUtils.toString(var5);
         Files.createDirectories(var4.getParent());

         try (OutputStream var7 = Files.newOutputStream(var4)) {
            NbtIo.writeCompressed(NbtUtils.snbtToStructure(var6), var7);
         }

         say(param0, "Imported to " + var4.toAbsolutePath());
         return 0;
      } catch (IOException | CommandSyntaxException var5) {
         System.err.println("Failed to load structure " + param1);
         var5.printStackTrace();
         return 1;
      }
   }

   private static void say(ServerLevel param0, String param1, ChatFormatting param2) {
      param0.getPlayers(l_param0 -> true).forEach(l_param2 -> l_param2.sendSystemMessage(Component.literal(param2 + param1)));
   }

   static class TestSummaryDisplayer implements GameTestListener {
      private final ServerLevel level;
      private final MultipleTestTracker tracker;

      public TestSummaryDisplayer(ServerLevel param0, MultipleTestTracker param1) {
         this.level = param0;
         this.tracker = param1;
      }

      @Override
      public void testStructureLoaded(GameTestInfo param0) {
      }

      @Override
      public void testPassed(GameTestInfo param0) {
         TestCommand.showTestSummaryIfAllDone(this.level, this.tracker);
      }

      @Override
      public void testFailed(GameTestInfo param0) {
         TestCommand.showTestSummaryIfAllDone(this.level, this.tracker);
      }
   }
}
