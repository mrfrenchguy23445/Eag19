package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
   private final GameTestInfo originalTestInfo;
   private final GameTestTicker testTicker;
   private final BlockPos structurePos;
   int attempts;
   int successes;

   public ReportGameListener(GameTestInfo param0, GameTestTicker param1, BlockPos param2) {
      this.originalTestInfo = param0;
      this.testTicker = param1;
      this.structurePos = param2;
      this.attempts = 0;
      this.successes = 0;
   }

   @Override
   public void testStructureLoaded(GameTestInfo param0) {
      spawnBeacon(this.originalTestInfo, Blocks.LIGHT_GRAY_STAINED_GLASS);
      this.attempts++;
   }

   @Override
   public void testPassed(GameTestInfo param0) {
      this.successes++;
      if (!param0.isFlaky()) {
         reportPassed(param0, param0.getTestName() + " passed! (" + param0.getRunTime() + "ms)");
      } else {
         if (this.successes >= param0.requiredSuccesses()) {
            reportPassed(param0, param0 + " passed " + this.successes + " times of " + this.attempts + " attempts.");
         } else {
            say(
               this.originalTestInfo.getLevel(),
               ChatFormatting.GREEN,
               "Flaky test " + this.originalTestInfo + " succeeded, attempt: " + this.attempts + " successes: " + this.successes
            );
            this.rerunTest();
         }
      }
   }

   @Override
   public void testFailed(GameTestInfo param0) {
      if (!param0.isFlaky()) {
         reportFailure(param0, param0.getError());
      } else {
         TestFunction var2 = this.originalTestInfo.getTestFunction();
         String var3 = "Flaky test " + this.originalTestInfo + " failed, attempt: " + this.attempts + "/" + var2.getMaxAttempts();
         if (var2.getRequiredSuccesses() > 1) {
            var3 = var3 + ", successes: " + this.successes + " (" + var2.getRequiredSuccesses() + " required)";
         }

         say(this.originalTestInfo.getLevel(), ChatFormatting.YELLOW, var3);
         if (param0.maxAttempts() - this.attempts + this.successes >= param0.requiredSuccesses()) {
            this.rerunTest();
         } else {
            reportFailure(param0, new ExhaustedAttemptsException(this.attempts, this.successes, param0));
         }
      }
   }

   public static void reportPassed(GameTestInfo param0, String param1) {
      spawnBeacon(param0, Blocks.LIME_STAINED_GLASS);
      visualizePassedTest(param0, param1);
   }

   private static void visualizePassedTest(GameTestInfo param0, String param1) {
      say(param0.getLevel(), ChatFormatting.GREEN, param1);
      GlobalTestReporter.onTestSuccess(param0);
   }

   protected static void reportFailure(GameTestInfo param0, Throwable param1) {
      spawnBeacon(param0, param0.isRequired() ? Blocks.RED_STAINED_GLASS : Blocks.ORANGE_STAINED_GLASS);
      spawnLectern(param0, Util.describeError(param1));
      visualizeFailedTest(param0, param1);
   }

   protected static void visualizeFailedTest(GameTestInfo param0, Throwable param1) {
      String var2 = param1.getMessage() + (param1.getCause() == null ? "" : " cause: " + Util.describeError(param1.getCause()));
      String var3 = (param0.isRequired() ? "" : "(optional) ") + param0.getTestName() + " failed! " + var2;
      say(param0.getLevel(), param0.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, var3);
      Throwable var4 = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(param1), param1);
      if (var4 instanceof GameTestAssertPosException var5) {
         showRedBox(param0.getLevel(), var5.getAbsolutePos(), var5.getMessageToShowAtBlock());
      }

      GlobalTestReporter.onTestFailed(param0);
   }

   private void rerunTest() {
      this.originalTestInfo.clearStructure();
      GameTestInfo var1 = new GameTestInfo(this.originalTestInfo.getTestFunction(), this.originalTestInfo.getRotation(), this.originalTestInfo.getLevel());
      var1.startExecution();
      this.testTicker.add(var1);
      var1.addListener(this);
      var1.spawnStructure(this.structurePos, 2);
   }

   protected static void spawnBeacon(GameTestInfo param0, Block param1) {
      ServerLevel var2 = param0.getLevel();
      BlockPos var3 = param0.getStructureBlockPos();
      BlockPos var4 = new BlockPos(-1, -1, -1);
      BlockPos var5 = StructureTemplate.transform(var3.offset(var4), Mirror.NONE, param0.getRotation(), var3);
      var2.setBlockAndUpdate(var5, Blocks.BEACON.defaultBlockState().rotate(param0.getRotation()));
      BlockPos var6 = var5.offset(0, 1, 0);
      var2.setBlockAndUpdate(var6, param1.defaultBlockState());

      for (int var7 = -1; var7 <= 1; var7++) {
         for (int var8 = -1; var8 <= 1; var8++) {
            BlockPos var9 = var5.offset(var7, -1, var8);
            var2.setBlockAndUpdate(var9, Blocks.IRON_BLOCK.defaultBlockState());
         }
      }
   }

   private static void spawnLectern(GameTestInfo param0, String param1) {
      ServerLevel var2 = param0.getLevel();
      BlockPos var3 = param0.getStructureBlockPos();
      BlockPos var4 = new BlockPos(-1, 1, -1);
      BlockPos var5 = StructureTemplate.transform(var3.offset(var4), Mirror.NONE, param0.getRotation(), var3);
      var2.setBlockAndUpdate(var5, Blocks.LECTERN.defaultBlockState().rotate(param0.getRotation()));
      BlockState var6 = var2.getBlockState(var5);
      ItemStack var7 = createBook(param0.getTestName(), param0.isRequired(), param1);
      LecternBlock.tryPlaceBook(null, var2, var5, var6, var7);
   }

   private static ItemStack createBook(String param0, boolean param1, String param2) {
      ItemStack var3 = new ItemStack(Items.WRITABLE_BOOK);
      ListTag var4 = new ListTag();
      StringBuffer var5 = new StringBuffer();
      Arrays.stream(param0.split("\\.")).forEach(l_param1 -> var5.append(l_param1).append('\n'));
      if (!param1) {
         var5.append("(optional)\n");
      }

      var5.append("-------------------\n");
      var4.add(StringTag.valueOf(var5 + param2));
      var3.addTagElement("pages", var4);
      return var3;
   }

   protected static void say(ServerLevel param0, ChatFormatting param1, String param2) {
      param0.getPlayers(l_param0 -> true).forEach(l_param2 -> l_param2.sendSystemMessage(Component.literal(param2).withStyle(param1)));
   }

   private static void showRedBox(ServerLevel param0, BlockPos param1, String param2) {
      DebugPackets.sendGameTestAddMarker(param0, param1, param2, -2130771968, Integer.MAX_VALUE);
   }
}
