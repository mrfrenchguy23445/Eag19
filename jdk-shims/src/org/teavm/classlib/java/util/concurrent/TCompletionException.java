package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.CompletionException}.
 *
 * <p>Absent from the TeaVM classlib, and required by the shimmed
 * {@link CompletableFuture}: the JDK's {@code join()} wraps a stage's failure in exactly
 * this type, and callers branch on it -- {@code Util.sequenceFailFast} inspects the cause
 * chain, and several sites catch {@code CompletionException} specifically to distinguish a
 * failed stage from a thrown exception.
 */
public class TCompletionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TCompletionException() {
    }

    public TCompletionException(String message) {
        super(message);
    }

    public TCompletionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TCompletionException(Throwable cause) {
        super(cause);
    }
}