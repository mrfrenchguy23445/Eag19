package net.minecraft.client.sounds;

import net.minecraft.util.thread.BlockableEventLoop;

/*
 * Port note: rewritten for wasm_gc. The class keeps its name, its queue and its
 * flush() contract; what it loses is the thread, which is the whole reason this
 * file exists in the original.
 *
 * What the original did: a field initializer ran createThread(), which started
 * a daemon "Sound engine" thread whose run() looped on managedBlock() draining
 * pendingRunnables until flush() set shutdown, interrupted the thread and
 * joined it. Two stacks from wasm12, both fatal to the link:
 *
 *   Class org.teavm.platform.Platform is not supported on current target
 *       at java.lang.Thread.start(XThread.java:78)
 *       at net.minecraft.client.sounds.SoundEngineExecutor.createThread(SoundEngineExecutor.java:18)
 *       at net.minecraft.client.sounds.SoundEngineExecutor.<init>(SoundEngineExecutor.java:7)
 *       at net.minecraft.client.sounds.SoundEngine.<init>(SoundEngine.java:62)
 *       at net.minecraft.client.sounds.SoundManager.<init>(SoundManager.java:63)
 *       at net.minecraft.client.Minecraft.<init>(Minecraft.java:481)
 *
 *   Class org.teavm.runtime.Fiber is not supported on current target
 *       at java.lang.Object.waitImpl
 *       at java.lang.Object.wait(TObject.java:453)
 *       at java.lang.Thread.join(XThread.java:144)
 *       at net.minecraft.client.sounds.SoundEngineExecutor.flush(SoundEngineExecutor.java:53)
 *       at net.minecraft.client.sounds.SoundEngine.stopAll...
 *       at net.minecraft.client.Minecraft.close...
 *
 * The threading is not fixable here -- Thread.start and Object.wait both bottom
 * out in machinery this target does not have -- and it is also unnecessary
 * here, which is why this is a rewrite rather than a stub. The sound engine's
 * hot path never used the queue through tell(): every call site in SoundEngine
 * and ChannelAccess goes through Executor.execute, and BlockableEventLoop.execute
 * runs the task inline whenever scheduleExecutables() is false, i.e. whenever
 * getRunningThread() == Thread.currentThread(). Returning the current thread
 * from getRunningThread() therefore makes execute(), submit() and
 * executeBlocking() all take their inline branches -- SoundEngine:457's
 * executor.execute(...) already did exactly that at runtime before this change,
 * its stack shows BlockableEventLoop.java:91, the param0.run() line. The thread
 * existed only to drain the rare tell(), and rare is not the same as needed:
 * tell() is overridden below to run inline as well, so the queue cannot strand
 * a task even if a future call site reaches for it.
 *
 * getRunningThread() returning currentThread() also makes isSameThread() true,
 * which is what executeBlocking() consults before it would submitAsync().join()
 * -- a CompletableFuture.join() is a blocking wait and would head straight for
 * Fiber the first time sound code blocked. With isSameThread() true the
 * blocking path runs the task and returns.
 *
 * flush() keeps its name and its dropAllTasks() half but loses the
 * interrupt/join/restart half: there is nothing to interrupt, nothing to join
 * (Thread.join's body is gone for the same Fiber reason, see XThread), and no
 * thread to recreate. shutdown survives as a field for shouldRun()'s sake but
 * is never set true -- no code outside this file ever set it; it was flush's
 * private handshake with the loop that no longer exists.
 *
 * run() and waitForTasks() are gone with the loop that called them.
 */
public class SoundEngineExecutor extends BlockableEventLoop<Runnable> {
   private volatile boolean shutdown;

   public SoundEngineExecutor() {
      super("Sound executor");
   }

   @Override
   protected Runnable wrapRunnable(Runnable param0) {
      return param0;
   }

   @Override
   protected boolean shouldRun(Runnable param0) {
      return !this.shutdown;
   }

   @Override
   protected Thread getRunningThread() {
      return Thread.currentThread();
   }

   @Override
   public void tell(Runnable param0) {
      if (this.shouldRun(param0)) {
         this.doRunTask(param0);
      }
   }

   public void flush() {
      this.dropAllTasks();
      this.shutdown = false;
   }
}
