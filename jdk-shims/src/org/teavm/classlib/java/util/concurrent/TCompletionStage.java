package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Shim for {@code java.util.concurrent.CompletionStage}.
 *
 * <p>Absent from the TeaVM classlib, and required because {@link CompletableFuture}
 * implements it in the real JDK. It is also genuinely referenced by name across the
 * codebase -- Minecraft's own APIs use {@code CompletionStage} in return positions, e.g.
 * {@code ChunkFuture} and the {@code CompletableFuture}-returning methods on
 * {@code ClientPacketListener} -- so an implementation of the interface has to exist rather
 * than the shimmed {@code CompletableFuture} merely dropping the supertype.
 *
 * <p><b>Why nearly every method is abstract here.</b> In the JDK almost all of these have
 * {@code default} bodies that implement one variant in terms of another; the only truly
 * abstract members are {@link #toCompletableFuture()} and the handful of primitives. A shim
 * that reproduced those defaults would be a second implementation of the whole
 * continuation algebra to maintain and verify. Instead the shim declares the methods and
 * the single concrete {@link CompletableFuture} supplies every body, which is also the only
 * implementation this target can have: there is one execution model, not two.
 *
 * <p><b>Subset.</b> The combinations that depend on a second independent stage to make
 * progress ({@code thenCombine}, {@code applyToEither} and friends) are declared but
 * implemented as straight-left-biased equivalents -- with inline completion, "wait for the
 * other stage" has no meaning, so the left operand wins. Every such case is documented on
 * the implementation.
 */
public interface TCompletionStage<T> {
    <U> TCompletionStage<U> thenApply(Function<? super T, ? extends U> fn);

    <U> TCompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn);

    <U> TCompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor);

    TCompletionStage<Void> thenAccept(Consumer<? super T> action);

    TCompletionStage<Void> thenAcceptAsync(Consumer<? super T> action);

    TCompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor);

    TCompletionStage<Void> thenRun(Runnable action);

    TCompletionStage<Void> thenRunAsync(Runnable action);

    TCompletionStage<Void> thenRunAsync(Runnable action, Executor executor);

    <U> TCompletionStage<U> thenCompose(Function<? super T, ? extends TCompletionStage<U>> fn);

    <U> TCompletionStage<U> thenComposeAsync(Function<? super T, ? extends TCompletionStage<U>> fn);

    <U> TCompletionStage<U> thenComposeAsync(Function<? super T, ? extends TCompletionStage<U>> fn, Executor executor);

    TCompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

    TCompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action);

    TCompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor);

    TCompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn);

    TCompletionStage<T> exceptionallyAsync(Function<Throwable, ? extends T> fn);

    TCompletionStage<T> exceptionallyAsync(Function<Throwable, ? extends T> fn, Executor executor);

    <U, V> TCompletionStage<V> handle(BiFunction<? super T, Throwable, ? extends V> fn);

    <U, V> TCompletionStage<V> handleAsync(BiFunction<? super T, Throwable, ? extends V> fn);

    <U, V> TCompletionStage<V> handleAsync(BiFunction<? super T, Throwable, ? extends V> fn, Executor executor);

    /**
     * Left-biased: applies {@code fn} to this stage's value and ignores {@code other}
     * entirely. With inline completion there is no second stage to wait for, so waiting
     * would mean waiting forever.
     */
    <U, V> TCompletionStage<V> thenCombine(TCompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);

    TCompletionStage<Void> thenAcceptBoth(TCompletionStage<?> other, BiConsumer<? super T, ?> action);

    TCompletionStage<Void> runAfterBoth(TCompletionStage<?> other, Runnable action);

    TCompletionStage<T> copy();

    TCompletableFuture<T> toCompletableFuture();
}