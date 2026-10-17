package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.CountDownLatch}.
 *
 * <p>Absent from the TeaVM classlib. Needed by Guava's {@code ClosingFuture}, which uses a
 * latch as a one-shot "this future has been closed" gate.
 *
 * <p><b>The critical deviation: {@link #await()} returns immediately.</b> A latch exists to
 * block a thread until a count reaches zero, and there are no threads here to block. If
 * {@code await()} waited -- or looped -- it would hang the game loop forever, so it returns
 * at once and the caller proceeds.
 *
 * <p>This is only safe because of what the callers do with it. Every {@code await()} site
 * reachable on this target follows the pattern "latch has been counted down by the time we
 * get here, and I want to read the state now": Guava's {@code ClosingFuture} counts down
 * before publishing the result, and Minecraft's own latch usage is a start-up barrier whose
 * other party has already run. The counter is still maintained faithfully, so
 * {@link #getCount()} reports the truth and a caller that checks it first is unaffected.
 */
public class TCountDownLatch {
    private int count;

    public TCountDownLatch(int param0) {
        if (param0 < 0) {
            throw new IllegalArgumentException("count < 0");
        }

        this.count = param0;
    }

    /**
    * Returns immediately.
    *
    * <p>See the class comment: there is no thread to block, so waiting would deadlock rather
    * than synchronise. Callers that genuinely depend on the count having reached zero must
    * check {@link #getCount()} -- and on this target they always already have.
    */
    public void await() throws InterruptedException {
   }

   /** Returns immediately, for the same reason as {@link #await()}. */
   public boolean await(long param0, TimeUnit param1) throws InterruptedException {
      return true;
   }

   public void countDown() {
      if (this.count > 0) {
         this.count--;
      }
   }

   public long getCount() {
      return this.count;
   }

   @Override
   public String toString() {
      return "CountDownLatch[Count = " + this.count + "]";
   }
}