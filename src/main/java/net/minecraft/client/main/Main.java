package net.minecraft.client.main;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import java.io.File;
import java.util.Optional;
import java.util.OptionalInt;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformInput;
import net.lax1dude.eaglercraft.v1_8.profile.EaglerProfile;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.NativeModuleLister;


public class Main {

   public static void main(String[] param0) {
      String username = "Player" + Util.getMillis() % 1000L;
      launch(new GameConfig(
         new GameConfig.UserData(new User(username, UUIDUtil.createOfflinePlayerUUID(username).toString(), "0", Optional.empty(), Optional.empty(), User.Type.LEGACY)),
         new DisplayData(854, 480, OptionalInt.empty(), OptionalInt.empty(), false),
         new GameConfig.FolderData(new File("."), new File("resourcepacks/"), new File("assets/"), null),
         new GameConfig.GameData(false, "1.19.4", "release", false, false),
         new GameConfig.ServerData(null, 25565)
      ));
   }

   /**
    * Eaglercraft entry point.
    *
    * <p>The 1.8 tree launched the game from here rather than through Mojang's
    * {@code main(String[])}, because Eaglercraft needs the player's stored
    * profile, its own display defaults and its own storage layout before the
    * game object exists. This is that seam: {@code src/teavm} and
    * {@code src/lwjgl} call it, and it is the only place a {@link GameConfig} is
    * built from Eaglercraft state.</p>
    *
    * <p>Call after platform bring-up ({@code EagRuntime.create()}, window and
    * input setup). It does not return until the game exits.</p>
    */
   public static void appMain() {
      EaglerProfile.loadFromStorage();

      User user = new User(
         EaglerProfile.getName(),
         EaglerProfile.getPlayerUUIDString(),
         "0",
         Optional.empty(),
         Optional.empty(),
         User.Type.LEGACY
      );

      launch(new GameConfig(
         new GameConfig.UserData(user),
         buildDisplayData(),
         new GameConfig.FolderData(new File("."), new File("resourcepacks/"), new File("assets/"), null),
         new GameConfig.GameData(false, "1.19.4", "release", false, false),
         new GameConfig.ServerData(null, 25565)
      ));
   }

   /**
    * Display settings, taken from the platform's configured window where one is
    * available so the game opens at the size the player chose, and falling back
    * to a sane default otherwise.
    */
   private static DisplayData buildDisplayData() {
      int width = PlatformInput.getWindowWidth();
      int height = PlatformInput.getWindowHeight();
      if (width <= 0 || height <= 0) {
         width = 854;
         height = 480;
      }
      return new DisplayData(width, height, OptionalInt.empty(), OptionalInt.empty(), PlatformInput.isFullscreen());
   }

   /**
    * Shared startup: version setup, bootstrap, window creation, run, teardown.
    * Extracted so {@code main} and {@code appMain} cannot drift apart.
    */
   private static void launch(GameConfig config) {
      SharedConstants.setVersion(DetectedVersion.BUILT_IN);
      SharedConstants.enableDataFixerOptimizations();

      CrashReport.preload();
      Bootstrap.bootStrap();
      Bootstrap.validate();

      Minecraft minecraft;
      try {
         RenderSystem.initRenderThread();
         RenderSystem.beginInitialization();
         minecraft = new Minecraft(config);
         RenderSystem.finishInitialization();
      } catch (SilentInitException e) {
         System.err.println("Failed to create window: " + e);
         return;
      } catch (Throwable t) {
         CrashReport report = CrashReport.forThrowable(t, "Initializing game");
         CrashReportCategory category = report.addCategory("Initialization");
         NativeModuleLister.addCrashSection(category);
         Minecraft.fillReport(null, null, config.game.launchVersion, null, report);
         Minecraft.crash(report);
         return;
      }

      try {
         RenderSystem.initGameThread(false);
         minecraft.run();
      } catch (Throwable t) {
         System.err.println("Unhandled game exception: " + t);
      }

      BufferUploader.reset();

      try {
         minecraft.stop();
      } finally {
         minecraft.destroy();
      }
   }
}