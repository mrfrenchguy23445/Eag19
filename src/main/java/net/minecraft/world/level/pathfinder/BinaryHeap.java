package net.minecraft.world.level.pathfinder;

public class BinaryHeap {
   private Node[] heap = new Node[128];
   private int size;

   public Node insert(Node param0) {
      if (param0.heapIdx >= 0) {
         throw new IllegalStateException("OW KNOWS!");
      }

      if (this.size == this.heap.length) {
         Node[] var2 = new Node[this.size << 1];
         System.arraycopy(this.heap, 0, var2, 0, this.size);
         this.heap = var2;
      }

      this.heap[this.size] = param0;
      param0.heapIdx = this.size;
      this.upHeap(this.size++);
      return param0;
   }

   public void clear() {
      this.size = 0;
   }

   public Node peek() {
      return this.heap[0];
   }

   public Node pop() {
      Node var1 = this.heap[0];
      this.heap[0] = this.heap[--this.size];
      this.heap[this.size] = null;
      if (this.size > 0) {
         this.downHeap(0);
      }

      var1.heapIdx = -1;
      return var1;
   }

   public void remove(Node param0) {
      this.heap[param0.heapIdx] = this.heap[--this.size];
      this.heap[this.size] = null;
      if (this.size > param0.heapIdx) {
         if (this.heap[param0.heapIdx].f < param0.f) {
            this.upHeap(param0.heapIdx);
         } else {
            this.downHeap(param0.heapIdx);
         }
      }

      param0.heapIdx = -1;
   }

   public void changeCost(Node param0, float param1) {
      float var3 = param0.f;
      param0.f = param1;
      if (param1 < var3) {
         this.upHeap(param0.heapIdx);
      } else {
         this.downHeap(param0.heapIdx);
      }
   }

   public int size() {
      return this.size;
   }

   private void upHeap(int param0) {
      Node var2 = this.heap[param0];
      float var3 = var2.f;

      while (param0 > 0) {
         int var4 = param0 - 1 >> 1;
         Node var5 = this.heap[var4];
         if (!(var3 < var5.f)) {
            break;
         }

         this.heap[param0] = var5;
         var5.heapIdx = param0;
         param0 = var4;
      }

      this.heap[param0] = var2;
      var2.heapIdx = param0;
   }

   private void downHeap(int param0) {
      Node var2 = this.heap[param0];
      float var3 = var2.f;

      while (true) {
         int var4 = 1 + (param0 << 1);
         int var5 = var4 + 1;
         if (var4 >= this.size) {
            break;
         }

         Node var6 = this.heap[var4];
         float var7 = var6.f;
         Node var8;
         float var9;
         if (var5 >= this.size) {
            var8 = null;
            var9 = Float.POSITIVE_INFINITY;
         } else {
            var8 = this.heap[var5];
            var9 = var8.f;
         }

         if (var7 < var9) {
            if (!(var7 < var3)) {
               break;
            }

            this.heap[param0] = var6;
            var6.heapIdx = param0;
            param0 = var4;
         } else {
            if (!(var9 < var3)) {
               break;
            }

            this.heap[param0] = var8;
            var8.heapIdx = param0;
            param0 = var5;
         }
      }

      this.heap[param0] = var2;
      var2.heapIdx = param0;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public Node[] getHeap() {
      Node[] var1 = new Node[this.size()];
      System.arraycopy(this.heap, 0, var1, 0, this.size());
      return var1;
   }
}
