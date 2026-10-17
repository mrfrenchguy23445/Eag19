package net.minecraft.gametest.framework;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import org.slf4j.Logger;

public class TeamcityTestReporter implements TestReporter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Escaper ESCAPER = Escapers.builder()
      .addEscape('\'', "|'")
      .addEscape('\n', "|n")
      .addEscape('\r', "|r")
      .addEscape('|', "||")
      .addEscape('[', "|[")
      .addEscape(']', "|]")
      .build();

   @Override
   public void onTestFailed(GameTestInfo param0) {
      String var2 = ESCAPER.escape(param0.getTestName());
      String var3 = ESCAPER.escape(param0.getError().getMessage());
      String var4 = ESCAPER.escape(Util.describeError(param0.getError()));
      LOGGER.info("##teamcity[testStarted name='{}']", var2);
      if (param0.isRequired()) {
         LOGGER.info("##teamcity[testFailed name='{}' message='{}' details='{}']", var2, var3, var4);
      } else {
         LOGGER.info("##teamcity[testIgnored name='{}' message='{}' details='{}']", var2, var3, var4);
      }

      LOGGER.info("##teamcity[testFinished name='{}' duration='{}']", var2, param0.getRunTime());
   }

   @Override
   public void onTestSuccess(GameTestInfo param0) {
      String var2 = ESCAPER.escape(param0.getTestName());
      LOGGER.info("##teamcity[testStarted name='{}']", var2);
      LOGGER.info("##teamcity[testFinished name='{}' duration='{}']", var2, param0.getRunTime());
   }
}
