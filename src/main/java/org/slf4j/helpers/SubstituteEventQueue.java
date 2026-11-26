/*
 * Copyright (c) 2004-2017 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.slf4j.helpers;

import java.util.ArrayDeque;
import java.util.Collection;
import org.slf4j.event.SubstituteLoggingEvent;

/**
 * The buffer that holds log events recorded before SLF4J has bound to a real provider.
 *
 * <h2>Port note: replaces {@code LinkedBlockingQueue}</h2>
 *
 * <p>Upstream types this field as {@code LinkedBlockingQueue<SubstituteLoggingEvent>}, and
 * {@code LoggerFactory.replayEvents} then calls {@code drainTo(Collection, int)} on it.
 * {@code java.util.concurrent.LinkedBlockingQueue} is not in this target's TeaVM classlib --
 * {@code java.util.concurrent} ships ArrayBlockingQueue, BlockingQueue, Callable,
 * CancellationException, ConcurrentHashMap/Map, CopyOnWriteArrayList, ExecutionException,
 * Executor, Flow, LinkedBlockingDeque, TimeUnit, ThreadLocalRandom and the atomic family, and no
 * LinkedBlockingQueue (see {@code .decompile_work/classlib-model.txt}). That was 4 of the build's
 * "Class java.util.concurrent.LinkedBlockingQueue was not found" errors, all reached from
 * {@code LoggerFactory}'s own static initializer.
 *
 * <p>Neither of the two present substitutes is a good fit:
 * {@code ArrayBlockingQueue} does have {@code drainTo}, but needs a capacity up front and
 * {@code add} <i>throws</i> once that capacity is reached -- and this buffer is exactly the thing
 * that is unbounded by design, since it accumulates every log call made during class
 * initialization. Silently capping it would drop log events; letting it throw would turn
 * "logger not bound yet" into a crash in unrelated code.
 *
 * <p>So this extends {@link ArrayDeque}, which is fully modelled, and adds the one method
 * {@code LoggerFactory} needs that {@code ArrayDeque} lacks. Extending {@code ArrayDeque} rather
 * than holding one is deliberate: {@code SubstituteLogger} and {@code EventRecordingLogger} both
 * accept the field as a {@code java.util.Queue}, so they are untouched, and the public
 * {@code SubstituteLoggerFactory.getEventQueue()} return type changes from
 * {@code LinkedBlockingQueue} to this class in exactly one place upstream
 * ({@code LoggerFactory.replayEvents}) plus its own declaration.
 *
 * <p><b>On thread safety.</b> {@code ArrayDeque} is not synchronized, and neither is
 * {@code LinkedBlockingQueue}'s {@code drainTo} atomicity contract reproduced here in any
 * meaningful sense -- but on this target there is only one thread, so the question does not
 * arise. If this code is ever shared with a threaded backend, the fix is to wrap the field in a
 * synchronized queue, not to make this class lock internally.
 */
public class SubstituteEventQueue extends ArrayDeque<SubstituteLoggingEvent> {

    private static final long serialVersionUID = 1L;

    public SubstituteEventQueue() {
        super();
    }

    /**
     * Moves at most {@code maxElements} events from this queue into {@code c}, in the order they
     * were recorded, and returns how many were moved.
     *
     * <p>Faithful to {@code BlockingQueue.drainTo(Collection, int)}: returns 0 rather than
     * blocking when nothing is available, which is what {@code LoggerFactory.replayEvents} loops
     * on to terminate.
     */
    public int drainTo(Collection<? super SubstituteLoggingEvent> c, int maxElements) {
        if (maxElements <= 0) {
            return 0;
        }
        int transferred = 0;
        while (transferred < maxElements) {
            SubstituteLoggingEvent event = poll();
            if (event == null) {
                break;
            }
            c.add(event);
            transferred++;
        }
        return transferred;
    }
}
