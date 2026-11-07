package com.mojang.logging;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;

/**
 * Vendored, WASM-GC-safe replacement for {@code com.mojang:logging}'s {@code LogQueues}.
 *
 * <p>Upstream is {@code com.mojang:logging:1.1.1}, which is a plain jar and therefore cannot be
 * patched (see {@link LogUtils}; excluded from the WASM-GC classpath for the same reason). Its
 * {@code LogQueues} is a two-method registry of named {@code BlockingQueue}s that its GUI log
 * appender fills in and the entity {@code MinecraftServerGui} drains from. The blocking-queue
 * plumbing it needs ({@code LinkedBlockingQueue}/{@code ReentrantReadWriteLock}) has no
 * counterpart in the TeaVM classlib this target links against, and nothing in the WASM-GC tree
 * ever produces queue entries anyway: the appender lives in log4j-core, which is excluded, so
 * the only caller just asks for the next event of a queue that is never populated and gets
 * {@code null} back immediately.
 *
 * <ul>
 *   <li>{@link #getOrCreateQueue} keeps the upstream name so any future caller links; it hands
 *       out a plain {@code LinkedList} standing in for the {@code LinkedBlockingQueue}.</li>
 *   <li>{@link #getNextLogEvent} polls the named queue without blocking and answers {@code null}
 *       both when the queue does not exist and when it is empty.</li>
 * </ul>
 */
public class LogQueues {
   private static final Map<String, Queue<String>> QUEUES = new ConcurrentHashMap<>();

   public static Queue<String> getOrCreateQueue(String var0) {
      return QUEUES.computeIfAbsent(var0, (var1) -> {
         return new LinkedList<>();
      });
   }

   public static String getNextLogEvent(String var0) {
      Queue<String> var1 = QUEUES.get(var0);
      return var1 == null ? null : var1.poll();
   }
}
