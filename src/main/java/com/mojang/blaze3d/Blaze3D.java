package com.mojang.blaze3d;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime;

public class Blaze3D {
   public static void process(RenderPipeline param0, float param1) {
      ConcurrentLinkedQueue<RenderCall> var2 = param0.getRecordingQueue();
   }

   public static void render(RenderPipeline param0, float param1) {
      ConcurrentLinkedQueue<RenderCall> var2 = param0.getProcessedQueue();
   }

   /**
    * Terminates the client immediately, bypassing the graceful crash-report path.
    *
    * <p>Upstream writes a zero byte to address {@code 0} via LWJGL's {@code MemoryUtil.memSet}. That
    * is a deliberate segmentation fault: it exists precisely so that a manually triggered crash
    * (Ctrl+Shift+F3 held for ten seconds) cannot be caught by the {@code Throwable}-handling that
    * produces Minecraft's readable {@code CrashReport} screen. On a JVM there is no equivalent of
    * "fault without a catchable throwable", and the code immediately below this call
    * ({@code KeyboardHandler.tick}) catches {@code Throwable} to build exactly the screen upstream is
    * trying to avoid -- so throwing here would be a no-op.
    *
    * <p>{@link PlatformRuntime#exit()} is therefore the portable equivalent: it logs at fatal level
    * and tears the client down without unwinding through the tick's exception handling. If a
    * platform's implementation of {@code exit} ever returned, the caller's existing
    * {@code ReportedException} would then run, which is a graceful degradation rather than a
    * regression.
    */
   public static void youJustLostTheGame() {
      PlatformRuntime.exit();
   }

   /**
    * Seconds since an unspecified origin, matching GLFW's {@code glfwGetTime} contract.
    *
    * <p>Upstream reads GLFW's monotonic clock, seeded when the window is created.
    * {@link PlatformRuntime#nanoTime()} is the same kind of clock and is what Eaglercraft's own
    * {@code Display.sync()} frame limiter uses, so frame pacing and this stay on one timeline. This
    * also matches the replacement already made in {@code GLX._initGlfw()}, which hands the same
    * clock to {@code RenderSystem} as a nanosecond supplier.
    */
   public static double getTime() {
      return (double)PlatformRuntime.nanoTime() / 1.0E9;
   }
}
