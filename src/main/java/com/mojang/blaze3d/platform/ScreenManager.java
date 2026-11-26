package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.lax1dude.eaglercraft.v1_8.Display;
import org.slf4j.Logger;

/**
 * Monitor selection, reduced to Eaglercraft's single implicit display.
 *
 * <p>Upstream enumerates GLFW monitors ({@code glfwGetMonitors}, {@code glfwSetMonitorCallback}) and
 * picks whichever one has the largest overlap with the window, preferring the primary monitor on
 * ties. Eaglercraft has no monitor concept at all -- there is one surface, the page canvas, and the
 * browser decides where and how large it is -- so this class now always reports the one synthetic
 * display instead of searching a set.
 *
 * <p>{@link #getMonitor(long)} returns that same display for any handle, including
 * {@code 0L}. Upstream returns {@code null} for an unknown handle, and {@code Window}'s constructor
 * treats {@code null} as "headless, skip monitor-relative positioning", so answering
 * "always present" is the behaviour that keeps fullscreen working rather than silently disabling it.
 *
 * <p>{@link Monitor} supplies a single video mode -- the live canvas size -- so
 * {@code VideoSettingsScreen}'s fullscreen-resolution option still renders ("current" rather than a
 * mode name) instead of showing "unavailable".
 */
public class ScreenManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Monitor display;

   public ScreenManager(MonitorCreator param0) {
      RenderSystem.assertInInitPhase();
      this.display = param0.createMonitor(0L);
   }

   @javax.annotation.Nullable
   public Monitor getMonitor(long param0) {
      RenderSystem.assertInInitPhase();
      return this.display;
   }

   @javax.annotation.Nullable
   public Monitor findBestMonitor(Window param0) {
      // Upstream scores every monitor by overlap area with the window and breaks ties towards the
      // primary monitor. With a single display there is nothing to score, so it is returned
      // directly; the debug line is kept so the log shape stays recognisable.
      LOGGER.debug("Selecting monitor - only display {}x{}", Display.getWidth(), Display.getHeight());
      return this.display;
   }

   public static int clamp(int param0, int param1, int param2) {
      if (param0 < param1) {
         return param1;
      } else {
         return param0 > param2 ? param2 : param0;
      }
   }

   public void shutdown() {
      RenderSystem.assertOnRenderThread();
      // No-op: upstream clears the GLFW monitor callback here. There is no callback to clear, and
      // the display is not owned by this class.
   }
}
