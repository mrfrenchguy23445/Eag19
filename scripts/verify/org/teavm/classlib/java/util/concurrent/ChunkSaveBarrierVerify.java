package org.teavm.classlib.java.util.concurrent;

/** A chunk must not become saveable until its generation dependencies complete. */
public final class ChunkSaveBarrierVerify {
   private static void check(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
   public static void main(String[] args) {
      for (boolean rightFirst : new boolean[] {false, true}) {
         TCompletableFuture<String> chunk = new TCompletableFuture<>();
         TCompletableFuture<String> generation = new TCompletableFuture<>();
         int[] calls = {0};
         TCompletableFuture<String> saved = chunk.thenCombine(generation, (left, right) -> {
            calls[0]++;
            return left + right;
         });
         if (rightFirst) generation.complete(" ready"); else chunk.complete("chunk");
         check(!saved.isDone(), "save barrier completed before both dependencies");
         if (rightFirst) chunk.complete("chunk"); else generation.complete(" ready");
         check(saved.join().equals("chunk ready") && calls[0] == 1, "combined result exactly once");
      }
      TCompletableFuture<String> chunk = TCompletableFuture.completedFuture("chunk");
      TCompletableFuture<Void> generation = new TCompletableFuture<>();
      TCompletableFuture<String> barrier = chunk.thenCombine(generation, (left, right) -> left);
      check(!barrier.isDone(), "completed chunk must still wait for pending generation");
      generation.completeExceptionally(new IllegalStateException("generation failed"));
      check(barrier.isCompletedExceptionally(), "generation failure must fail the save barrier");
      TCompletableFuture<Void> first = new TCompletableFuture<>(), second = new TCompletableFuture<>();
      int[] both = {0};
      TCompletableFuture<Void> done = first.runAfterBoth(second, () -> both[0]++);
      first.complete(null);
      check(both[0] == 0 && !done.isDone(), "runAfterBoth must wait");
      second.complete(null);
      check(done.isDone() && both[0] == 1, "runAfterBoth completion");
      System.out.println("Chunk save barrier verification passed");
   }
}
