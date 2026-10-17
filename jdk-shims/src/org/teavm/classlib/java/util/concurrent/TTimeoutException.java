package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.TimeoutException}.
 *
 * <p>Absent from the TeaVM classlib, and reachable only through
 * {@link Future#get(long, TimeUnit)}. Provided so that the {@code Future} shim can declare
 * the same {@code throws} clause as the JDK's, rather than silently narrowing it -- a
 * narrower {@code throws} would make the shim <em>more</em> permissive than the real class,
 * which is the direction that hides problems instead of surfacing them.
 */
public class TTimeoutException extends Exception {
    private static final long serialVersionUID = 1L;

    public TTimeoutException() {
    }

    public TTimeoutException(String message) {
        super(message);
    }
}