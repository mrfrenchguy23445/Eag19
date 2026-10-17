/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.lang;

import org.teavm.classlib.PlatformDetector;
import org.teavm.platform.Platform;
import org.teavm.runtime.EventQueue;
import org.teavm.runtime.Fiber;

public class XThread extends TObject implements TRunnable {
    private static XThread mainThread = new XThread("main");
    private static XThread currentThread = mainThread;
    private static int nextId = 1;
    private static int activeCount = 1;
    private static UncaughtExceptionHandler defaultUncaughtExceptionHandler =
            // Same reconciliation as XClassLoader.getSystemClassLoader: the classlib's
            // TDefaultUncaughtExceptionHandler implements TThread.UncaughtExceptionHandler,
            // which this file renamed to XThread's nested interface. The X and the T are
            // stripped at link time and both land on java.lang.Thread.UncaughtExceptionHandler.
            (UncaughtExceptionHandler) (Object) new TDefaultUncaughtExceptionHandler();
    private UncaughtExceptionHandler uncaughtExceptionHandler;
    private long id;
    private int priority;
    private boolean daemon;
    private long timeSliceStart;
    private int yieldCount;
    // Port note (wasm15): finishedLock is gone. It existed only to hand join() an event to
    // wait for, and join() stopped waiting two iterations ago (see the note in join), so the
    // notifyAll below was signalling nobody. It was not free, though: it was the one thing
    // keeping Object.notifyAll reachable, and that call is broken on this target, in a
    // different way from wait --
    //
    //   Method is not annotated with org.teavm.interop.Import
    //       at org.teavm.platform.PlatformQueue.unwrap
    //       at org.teavm.platform.PlatformQueue.remove$static(PlatformQueue.java:43)
    //       at java.lang.Object.notifyAll(TObject.java:427)
    //       at java.lang.Thread.runThread(XThread.java:89)
    //       ...
    //       at net.minecraft.client.gui.screens.ConnectScreen.connect(ConnectScreen.java:124)
    //
    // TObject.notifyAll routes through PlatformQueue, which is implements JSObject and whose
    // unwrap/wrap bodies are supplied by PlatformQueueGenerator, a
    // org.teavm.backend.javascript.spi.Injector: a JavaScript backend hook. The WASM-GC
    // backend has no body to give it and unwrap is not marked @Import, so the call cannot be
    // generated at all. wait is unusable for its own, already-documented reason (Fiber), and
    // the two between them cover every monitor primitive java.lang.Object offers, so no
    // waiter or signaler survives this target.
    private boolean interruptedFlag;
    public TThreadInterruptHandler interruptHandler;
    public Object key;

    private String name;
    private boolean alive = true;
    TRunnable target;

    public XThread() {
        this(null, null);
    }

    public XThread(String name) {
        this(null, name);
    }

    public XThread(TRunnable target) {
        this(target, null);
    }

    public XThread(TRunnable target, String name) {
        this.name = name;
        this.target = target;
        id = nextId++;
    }

    public void start() {
        if (PlatformDetector.isLowLevel()) {
            boolean daemon = this.daemon;
            if (!daemon) {
                Fiber.userThreadCount++;
            }
            EventQueue.offer(() -> Fiber.start(this::runThread, daemon));
        } else {
            Platform.startThread(this::runThread);
        }
    }

    private void runThread() {
        try {
            activeCount++;
            setCurrentThread(XThread.this);
            key = new Object();
            XThread.this.run();
        } catch (Throwable t) {
            getUncaughtExceptionHandler().uncaughtException(this, t);
        } finally {
            // see the note above finishedLock: nothing waits on us any more
            alive = false;
            activeCount--;
            key = null;
            setCurrentThread(mainThread);
        }
    }

    static void setCurrentThread(XThread thread) {
        if (currentThread != thread) {
            currentThread = thread;
        }
        currentThread.timeSliceStart = System.currentTimeMillis();
    }

    static XThread getMainThread() {
        return mainThread;
    }

    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    public static XThread currentThread() {
        return currentThread;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public final boolean isDaemon() {
        return daemon;
    }

    public final void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public final void join(long millis, int nanos) throws InterruptedException {
        /*
         * Port note: body removed for wasm_gc, and it is not a stub papering over
         * a missing feature -- it is the exact truth of this target. The old body
         * was synchronized(finishedLock) { finishedLock.wait(...) }, and Object.wait
         * is @Async in the classlib, which on wasm_gc means Fiber:
         *
         *   Class org.teavm.runtime.Fiber is not supported on current target
         *       at java.lang.Object.waitImpl
         *       at java.lang.Object.wait(TObject.java:453)
         *       at java.lang.Thread.join(XThread.java:144)
         *       at net.minecraft.client.sounds.SoundEngineExecutor.flush...
         *
         * There is no Fiber to suspend into, so wait can never return here. What
         * makes the empty body correct rather than merely linkable is start():
         * on this target the "new thread" runs inline inside start() (see
         * XPlatform.startThread), so by the time start() returns the thread is
         * already finished and alive == false. Every joiner therefore joins a
         * thread that is already dead, and waiting for it would be waiting for
         * an event that happened before the call. A joiner racing an inline
         * start is impossible: same thread, sequential execution.
         */
    }

    public final void join(long millis) throws InterruptedException {
        join(millis, 0);
    }

    public final void join() throws InterruptedException {
        join(0);
    }

    public static void yield() {
        /*
         * Port note: body removed for wasm_gc. It used to count time slices and
         * call switchContext() every 30th yield once 100ms had passed, and
         * switchContext is @Async:
         *
         *   Class org.teavm.runtime.Fiber is not supported on current target
         *       at java.lang.Thread.switchContext
         *       at java.lang.Thread.yield(XThread.java:163)
         *       at io.netty.util.internal.ReferenceCountUpdater.retryRelease0...
         *
         * A context switch needs a second context to switch to, and this target
         * has one thread. yield() is documented as a hint that may be ignored;
         * on a single-threaded host there is nobody to yield TO, so ignoring it
         * is the implementation, not a degradation of one. The counter, the
         * time-slice bookkeeping and switchContext itself went with it -- the
         * fields stay because start()/setCurrentThread still maintain them for
         * whatever wants to read a time slice later.
         */
    }

    public void interrupt() {
        interruptedFlag = true;
        if (interruptHandler != null) {
            interruptHandler.interrupted();
            interruptHandler = null;
        }
    }

    public static boolean interrupted() {
        XThread thread = currentThread();
        boolean result = thread.interruptedFlag;
        thread.interruptedFlag = false;
        return result;
    }

    public boolean isInterrupted() {
        return interruptedFlag;
    }

    public boolean isAlive() {
        return alive;
    }

    public static int activeCount() {
        return activeCount;
    }

    public long getId() {
        return id;
    }

    public static boolean holdsLock(TObject obj) {
        return TObject.holdsLock(obj);
    }

    public static void sleep(long millis) throws TInterruptedException {
        /*
         * Port note: rewritten for wasm_gc. The old form was an @Async native
         * declaration plus an implementation that scheduled the continuation
         * through Platform.schedule or EventQueue:
         *
         *   Class org.teavm.runtime.Fiber is not supported on current target
         *       at java.lang.Thread.sleep
         *       at net.minecraft.world.level.storage.LevelStorageSource
         *          $LevelStorageAccess.deleteLevel(LevelStorageSource.java:491)
         *
         * @Async is the problem, not the scheduler behind it: a synchronous
         * call into an @Async method on wasm_gc suspends through Fiber, and
         * Fiber is the one thing this target cannot build. The rewrite is a
         * plain method that spins until the deadline, which is the only way to
         * hold a single browser thread for N milliseconds -- the alternative,
         * handing the continuation to setTimeout, would make sleep return to
         * the wrong place, because the caller is not a fiber that can be
         * resumed. Every statically reachable sleep on this target is a short
         * retry back-off (deleteLevel's 500ms after a failed file delete, at
         * most five times), so the freeze this can cause is bounded and rare;
         * a long sleep from code that is reached later will freeze the page
         * for its full duration, which is exactly what the caller asked for.
         *
         * Interrupt semantics are reduced to what a spin can offer: the flag
         * is observed on entry and throws, as the spec requires for an
         * interrupt that arrived before the sleep began. An interrupt raised
         * DURING the spin cannot be observed until the deadline passes -- the
         * spin would otherwise have to poll Thread.interrupted(), which it now
         * does not, and a busy loop that aborts early would break callers that
         * sleep as a debounce. SleepHandler (the old schedule/interrupt
         * handshake) went away with the old implementation.
         */
        XThread current = currentThread();
        if (current.interruptedFlag) {
            current.interruptedFlag = false;
            throw new TInterruptedException();
        }
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            // spin: see header
        }
    }

    public final void setPriority(int newPriority) {
        this.priority = newPriority;
    }

    public final int getPriority() {
        return this.priority;
    }

    public TStackTraceElement[] getStackTrace() {
        return new TStackTraceElement[0];
    }

    public TClassLoader getContextClassLoader() {
        return contextClassLoader != null ? contextClassLoader : TClassLoader.getSystemClassLoader();
    }
    
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        if (this.uncaughtExceptionHandler != null) {
            return this.uncaughtExceptionHandler;
        }
        return defaultUncaughtExceptionHandler;
    }
    
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }
    
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }
    
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        defaultUncaughtExceptionHandler = handler;
    }
    
    public interface UncaughtExceptionHandler {
        void uncaughtException(XThread t, Throwable e);
    }


    private TThreadGroup group;
    private TClassLoader contextClassLoader;

    /*
     * Port note: the three-argument constructor and setContextClassLoader are the only members
     * added. Both are reached from one path the integrated server cannot avoid --
     * ServerConnectionListener's LocalEventLoopGroup reaches MultithreadEventExecutorGroup,
     * whose constructor walks its children, and DefaultThreadFactory then builds a
     * FastThreadLocalThread with all three arguments:
     *
     *   Method java.lang.Thread.<init>(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;)V
     *       at io.netty.util.concurrent.FastThreadLocalThread.<init>(FastThreadLocalThread.java:65)
     *   Method java.lang.Thread.setContextClassLoader(Ljava/lang/ClassLoader;)V
     *       at io.netty.util.concurrent.GlobalEventExecutor$2.run(GlobalEventExecutor.java:231)
     *
     * The group is accepted and stored rather than ignored so that a future getThreadGroup can
     * answer it; nothing on this target reads it back yet. GlobalEventExecutor sets the context
     * class loader to null on purpose -- it is shedding a reference to a class loader that must
     * not outlive the thread -- so storing the value verbatim is exactly right.
     */
    public XThread(TThreadGroup group, TRunnable target, String name) {
        this.group = group;
        this.target = target;
        this.name = name;
        id = nextId++;
    }

    public void setContextClassLoader(TClassLoader cl) {
        this.contextClassLoader = cl;
    }

}
