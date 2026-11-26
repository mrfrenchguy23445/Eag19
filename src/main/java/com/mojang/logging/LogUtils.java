package com.mojang.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMarkerFactory;

/**
 * Vendored, WASM-GC-safe replacement for {@code com.mojang:logging}'s {@code LogUtils}.
 *
 * <p>Upstream is {@code com.mojang:logging:1.1.1}, which is a plain jar and therefore cannot be
 * patched. It is a three-line class that pulls in three things the WASM-GC TeaVM classlib does not
 * have:
 *
 * <ul>
 *   <li>{@link java.lang.StackWalker} for the caller-sensitive {@link #getLogger()}, plus the
 *       {@code StackWalker.Option.RETAIN_CLASS_REFERENCE} constant its static initialiser reads.</li>
 *   <li>{@code org.apache.logging.log4j.core.LoggerContext} / {@code LifeCycle} for
 *       {@link #isLoggerActive()} -- log4j-core drags in a large amount of reflection that the
 *       in-game build has no use for.</li>
 *   <li>{@link org.slf4j.MarkerFactory}, whose static initialiser falls back to
 *       {@code org.slf4j.impl.StaticMarkerBinder} when no SLF4J provider is installed, and that
 *       class is not on the TeaVM classpath.</li>
 * </ul>
 *
 * <p>Each is replaced below without changing observable behaviour:
 *
 * <ul>
 *   <li>{@link #getLogger()} recovers the caller from {@link Throwable#getStackTrace()}, which the
 *       WASM-GC classlib implements for real off the native WASM stack (see
 *       {@code TThrowable.ensureStackTrace}), rather than from {@code StackWalker}. Caller-sensitive
 *       semantics are preserved: the first frame outside this class wins, exactly as
 *       {@code getCallerClass()} would return. See the "DO NOT WRAP" contract below.</li>
 *   <li>{@link #isLoggerActive()} returns {@code true}, which is the same "sensible default" answer
 *       the upstream code already falls back to when the context is not a {@code LifeCycle}.</li>
 *   <li>{@link #FATAL_MARKER} is built through {@link BasicMarkerFactory}, a concrete marker
 *       implementation that performs no provider lookup.</li>
 * </ul>
 *
 * <p>{@link #configureRootLoggingLevel} is not ported: it mutates the log4j-core root
 * {@code LoggerConfig}, which is inherently host-only, and nothing in this repository calls it.
 */
public class LogUtils {
   public static final String FATAL_MARKER_ID = "FATAL";

   public static final Marker FATAL_MARKER = new BasicMarkerFactory().getMarker(FATAL_MARKER_ID);

   /**
    * Callers are memoised because {@link #getLogger()} runs from hundreds of static initialisers and
    * each call captures a fresh native stack trace.
    */
   private static final Map<String, Logger> LOGGERS = new ConcurrentHashMap<>();

   private static final Logger ROOT_LOGGER = LoggerFactory.getLogger("ROOT");

   public static boolean isLoggerActive() {
      return true;
   }

   public static Object defer(final Supplier<Object> result) {
      class ToString {
         @Override
         public String toString() {
            return result.get().toString();
         }
      }

      return new ToString();
   }

   /**
    * Caller sensitive, DO NOT WRAP
    */
   public static Logger getLogger() {
      StackTraceElement[] var1 = new Throwable().getStackTrace();
      for (int var2 = 1; var2 < var1.length; var2++) {
         String var3 = var1[var2].getClassName();
         if (!var3.equals(LogUtils.class.getName())) {
            Logger var4 = LOGGERS.get(var3);
            if (var4 != null) {
               return var4;
            }

            var4 = LoggerFactory.getLogger(var3);
            LOGGERS.put(var3, var4);
            return var4;
         }
      }

      return ROOT_LOGGER;
   }
}
