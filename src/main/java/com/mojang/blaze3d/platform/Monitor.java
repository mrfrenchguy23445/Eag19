package com.mojang.blaze3d.platform;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.lax1dude.eaglercraft.v1_8.Display;

/**
 * A single Eaglercraft display, standing in for a GLFW monitor.
 *
 * <p>Upstream wraps a {@code GLFWmonitor*} and enumerates its video modes with
 * {@code glfwGetVideoModes}, filtering to those with at least 8 bits per channel, and reads its
 * position with {@code glfwGetMonitorPos}. Eaglercraft has neither: the canvas is the display, its
 * geometry belongs to the browser, and the page never offers a list of modes to choose from.
 *
 * <p>So {@link #refreshVideoModes()} publishes exactly one mode -- the live canvas size at 8 bits
 * per channel -- and reports the display origin as (0, 0). Consequences, all of them intended:
 *
 * <ul>
 *   <li>{@code VideoSettingsScreen}'s fullscreen-resolution option still works: it offers
 *       "current", because {@link #getVideoModeIndex} only matches the current size.</li>
 *   <li>{@link #getPreferredVidMode} therefore always returns {@link #getCurrentMode()}.</li>
 *   <li>Applying a different fullscreen resolution is not possible, which matches Eaglercraft 1.8 --
 *       fullscreen there is {@code Display.toggleFullscreen()}, a request to the platform, not a
 *       mode switch. The size is re-read from {@link Display} every frame by
 *       {@link Window#updateDisplay()} regardless.</li>
 * </ul>
 *
 * <p>The colour depth is reported as 8/8/8 because that is what {@code VideoMode.read} derives from
 * a saved {@code WIDTHxHEIGHT@R:BITDEPTH} string, and the refresh rate defaults to 60 for the same
 * reason -- the real rate is not observable from here (see {@code GLX._getRefreshRate}).
 */
public final class Monitor {
   private final long monitor;
   private final List<VideoMode> videoModes;
   private VideoMode currentMode;
   private int x;
   private int y;

   public Monitor(long param0) {
      this.monitor = param0;
      this.videoModes = Lists.newArrayList();
      this.refreshVideoModes();
   }

   public void refreshVideoModes() {
      RenderSystem.assertInInitPhase();
      this.videoModes.clear();
      this.x = 0;
      this.y = 0;
      this.currentMode = new VideoMode(Display.getWidth(), Display.getHeight(), 8, 8, 8, 60);
      this.videoModes.add(this.currentMode);
   }

   public VideoMode getPreferredVidMode(Optional<VideoMode> param0) {
      RenderSystem.assertInInitPhase();
      if (param0.isPresent()) {
         VideoMode var2 = param0.get();

         for (VideoMode var4 : this.videoModes) {
            if (var4.equals(var2)) {
               return var4;
            }
         }
      }

      return this.getCurrentMode();
   }

   public int getVideoModeIndex(VideoMode param0) {
      RenderSystem.assertInInitPhase();
      return this.videoModes.indexOf(param0);
   }

   public VideoMode getCurrentMode() {
      return this.currentMode;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public VideoMode getMode(int param0) {
      return this.videoModes.get(param0);
   }

   public int getModeCount() {
      return this.videoModes.size();
   }

   public long getMonitor() {
      return this.monitor;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "Monitor[%s %sx%s %s]", this.monitor, this.x, this.y, this.currentMode);
   }
}
