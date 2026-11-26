package com.mojang.blaze3d.systems;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * GPU timer queries, reduced to a no-op for the Eaglercraft targets.
 *
 * <p>Upstream backs this with LWJGL's {@code GL32C}/{@code ARBTimerQuery} and gates construction on
 * {@code GL.getCapabilities().GL_ARB_timer_query}. All three of those are unavailable here: the LWJGL
 * GL bindings are not on the WASM-GC TeaVM classpath, and Eaglercraft's own GL layer has no timer
 * query support at all -- {@code EaglercraftGPU} keeps a {@code mapQueriesGL} slot but exposes no
 * create/destroy/query methods, because the Eaglercraft 1.8 client it was written against had no
 * GPU timing either.
 *
 * <p>So {@link #getInstance()} now reports "unsupported" by returning {@link Optional#empty()}, which
 * is exactly the branch vanilla takes on hardware without {@code ARB_timer_query}. The
 * {@code Optional} shape is preserved deliberately: {@code Minecraft} gates on it to decide whether
 * to profile, so with an empty Optional it simply never profiles and
 * {@code Minecraft.gpuUtilization} stays at its 0.0 default, matching Eaglercraft 1.8.
 *
 * <p>The instance methods are kept as no-ops rather than deleted because {@code Minecraft} refers to
 * {@link #beginProfile} and {@link #endProfile} through method references, which keeps them reachable
 * for TeaVM regardless of what the Optional contains. {@link FrameProfile} is likewise retained: its
 * methods are all reachable through the {@code currentFrameProfile} field even though nothing ever
 * assigns a non-null value to it.
 */
public class TimerQuery {
   private int nextQueryName;

   public static Optional<TimerQuery> getInstance() {
      return TimerQuery.TimerQueryLazyLoader.INSTANCE;
   }

   public void beginProfile() {
      RenderSystem.assertOnRenderThread();
      if (this.nextQueryName != 0) {
         throw new IllegalStateException("Current profile not ended");
      }
   }

   public TimerQuery.FrameProfile endProfile() {
      RenderSystem.assertOnRenderThread();
      if (this.nextQueryName == 0) {
         throw new IllegalStateException("endProfile called before beginProfile");
      }

      this.nextQueryName = 0;
      return new TimerQuery.FrameProfile(this.nextQueryName);
   }

   public static class FrameProfile {
      private static final long NO_RESULT = 0L;
      private static final long CANCELLED_RESULT = -1L;
      private final int queryName;
      private long result = CANCELLED_RESULT;

      FrameProfile(int param0) {
         this.queryName = param0;
      }

      public void cancel() {
         RenderSystem.assertOnRenderThread();
         this.result = CANCELLED_RESULT;
      }

      public boolean isDone() {
         RenderSystem.assertOnRenderThread();
         return this.result != NO_RESULT;
      }

      public long get() {
         RenderSystem.assertOnRenderThread();
         return this.result;
      }
   }

   static class TimerQueryLazyLoader {
      static final Optional<TimerQuery> INSTANCE = Optional.ofNullable(instantiate());

      private TimerQueryLazyLoader() {
      }

      @Nullable
      private static TimerQuery instantiate() {
         // Upstream: !GL.getCapabilities().GL_ARB_timer_query ? null : new TimerQuery().
         // Eaglercraft's GL layer has no timer query support, so the answer is always "no".
         return null;
      }
   }
}
