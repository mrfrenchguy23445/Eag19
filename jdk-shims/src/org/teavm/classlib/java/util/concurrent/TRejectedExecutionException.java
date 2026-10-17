package org.teavm.classlib.java.util.concurrent;

/**
 * Shim for {@code java.util.concurrent.RejectedExecutionException}.
 *
 * <p>Absent from the TeaVM classlib -- {@code java.util.concurrent} there has the atomics, the
 * concurrent collections and {@code TCancellationException}/{@code TExecutionException}, but no
 * rejected-execution exception of any kind. Nothing here needs it; netty does.
 *
 * <p>Netty's {@code SingleThreadEventExecutor.reject} throws it, and netty reaches that from a
 * path this port is actively using: {@code HttpUtil.DOWNLOAD_EXECUTOR} is a
 * {@code MoreExecutors.listeningDecorator} over a direct executor, and
 * {@code TelemetryLogManager.open} submits to it through
 * {@code CompletableFuture.supplyAsync}. So the name was being asked for on a live path and
 * came back not found.
 *
 * <p>The real class extends {@link RuntimeException}, which is unchecked -- that is the whole
 * point of it, since {@link java.util.concurrent.ExecutorService#execute} does not declare it.
 * Nothing is lost by reproducing that exactly: this shim exists so the name resolves and so the
 * exception can still be thrown and caught, not so the rejection logic itself can be modelled.
 *
 * <p>Where it is thrown from is now a port question rather than a JDK one. Every executor on this
 * target is a direct, inline executor -- see {@code TExecutors}, which documents that decision --
 * so {@code execute} runs the task inline and only "rejects" if it has already shut down. Which is
 * the one case worth keeping faithful: a task submitted to a shut-down executor has to fail
 * rather than silently run, and that is exactly what this exception means.
 */
public class TRejectedExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TRejectedExecutionException() {
    }

    public TRejectedExecutionException(String message) {
        super(message);
    }

    public TRejectedExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TRejectedExecutionException(Throwable cause) {
        super(cause);
    }
}
