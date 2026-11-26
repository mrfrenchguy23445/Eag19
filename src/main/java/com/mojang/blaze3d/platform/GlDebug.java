package com.mojang.blaze3d.platform;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Queue;
import javax.annotation.Nullable;

// PORT NOTE (Eag19 / TeaVM WASM-GC)
//
// This class was unmodified 1.19.4 code that hooked the desktop GL debug-output
// extension, through four LWJGL facilities the WASM target cannot have:
//
//   GL.getCapabilities()        a per-context capability query backed by LWJGL's
//                               native struct -- WebGL exposes no equivalent and
//                               there is no context object to ask
//   KHRDebug / ARBDebugOutput   the extension entry points themselves
//   GLX.make(...)               a native trampoline turning a Java method
//                               reference into a C function pointer
//   GLDebugMessageCallback      decoding a native message, whose .getMessage(..)
//                               reads client memory at a long address
//
// None of that can be built, and -- more to the point -- none of it would work:
// there is no way to route a WebGL debug message into a JVM listener at all.
//
// So enableDebugCallback() does nothing and the message buffer stays empty. That
// is the correct observable behaviour rather than a loss: debugEnabled could
// only ever be set inside enableDebugCallback, so isDebugEnabled() would have
// returned false on this target under any emulation. Minecraft reads both
// isDebugEnabled() and getLastOpenGlDebugMessages() when building a crash report
// (Minecraft.java:544 and :2295); both still work and simply have nothing to
// report.
//
// printDebugLog() is deleted: its only caller was the method reference inside
// enableDebugCallback. LogEntry and the token-to-string helpers stay because
// getLastOpenGlDebugMessages() and LogEntry.toString() still use them.
public class GlDebug {
   private static final int CIRCULAR_LOG_SIZE = 10;
   private static final Queue<GlDebug.LogEntry> MESSAGE_BUFFER = EvictingQueue.create(10);
   // Always null now that nothing can append to MESSAGE_BUFFER. Kept so that
   // getLastOpenGlDebugMessages() keeps its original shape and so the coalescing
   // logic can come back if a debug-message source is ever wired up.
   @Nullable
   private static volatile GlDebug.LogEntry lastEntry;
   private static boolean debugEnabled;

   private static String printUnknownToken(int param0) {
      return "Unknown (0x" + Integer.toHexString(param0).toUpperCase() + ")";
   }

   public static String sourceToString(int param0) {
      switch (param0) {
         case 33350:
            return "API";
         case 33351:
            return "WINDOW SYSTEM";
         case 33352:
            return "SHADER COMPILER";
         case 33353:
            return "THIRD PARTY";
         case 33354:
            return "APPLICATION";
         case 33355:
            return "OTHER";
         default:
            return printUnknownToken(param0);
      }
   }

   public static String typeToString(int param0) {
      switch (param0) {
         case 33356:
            return "ERROR";
         case 33357:
            return "DEPRECATED BEHAVIOR";
         case 33358:
            return "UNDEFINED BEHAVIOR";
         case 33359:
            return "PORTABILITY";
         case 33360:
            return "PERFORMANCE";
         case 33361:
            return "OTHER";
         case 33384:
            return "MARKER";
         default:
            return printUnknownToken(param0);
      }
   }

   public static String severityToString(int param0) {
      switch (param0) {
         case 33387:
            return "NOTIFICATION";
         case 37190:
            return "HIGH";
         case 37191:
            return "MEDIUM";
         case 37192:
            return "LOW";
         default:
            return printUnknownToken(param0);
      }
   }

   /**
    * No-op on this target. See the port note at the top of the file: there is no
    * WebGL equivalent of the debug-output extension, and no way to deliver a
    * message from the GL implementation into a JVM callback.
    *
    * <p>Upstream would discover that KHR_debug / ARB_debug_output is absent and
    * return without enabling anything, so doing nothing reproduces its observable
    * result exactly: debugEnabled stays false and getLastOpenGlDebugMessages()
    * stays empty. It must not throw: the sole caller, GLX._init, runs
    * unconditionally from RenderSystem.initRenderer during Minecraft.&lt;init&gt;
    * with the default glDebugVerbosity = 1, so throwing aborts startup.
    */
   public static void enableDebugCallback(int param0, boolean param1) {
      // Deliberately empty; debugEnabled is left false. See the port note above.
   }

   public static List<String> getLastOpenGlDebugMessages() {
      synchronized (MESSAGE_BUFFER) {
         List<String> var1 = Lists.newArrayListWithCapacity(MESSAGE_BUFFER.size());

         for (GlDebug.LogEntry var3 : MESSAGE_BUFFER) {
            var1.add(var3 + " x " + var3.count);
         }

         return var1;
      }
   }

   public static boolean isDebugEnabled() {
      return debugEnabled;
   }

   static class LogEntry {
      private final int id;
      private final int source;
      private final int type;
      private final int severity;
      private final String message;
      int count = 1;

      LogEntry(int param0, int param1, int param2, int param3, String param4) {
         this.id = param2;
         this.source = param0;
         this.type = param1;
         this.severity = param3;
         this.message = param4;
      }

      boolean isSame(int param0, int param1, int param2, int param3, String param4) {
         return param1 == this.type && param0 == this.source && param2 == this.id && param3 == this.severity && param4.equals(this.message);
      }

      @Override
      public String toString() {
         return "id="
            + this.id
            + ", source="
            + GlDebug.sourceToString(this.source)
            + ", type="
            + GlDebug.typeToString(this.type)
            + ", severity="
            + GlDebug.severityToString(this.severity)
            + ", message='"
            + this.message
            + "'";
      }
   }
}
