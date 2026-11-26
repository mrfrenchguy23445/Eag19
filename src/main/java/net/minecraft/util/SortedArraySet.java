package net.minecraft.util;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

public class SortedArraySet<T> extends AbstractSet<T> {
   private static final int DEFAULT_INITIAL_CAPACITY = 10;
   private final Comparator<T> comparator;
   T[] contents;
   int size;

   private SortedArraySet(int param0, Comparator<T> param1) {
      this.comparator = param1;
      if (param0 < 0) {
         throw new IllegalArgumentException("Initial capacity (" + param0 + ") is negative");
      }

      this.contents = (T[])castRawArray(new Object[param0]);
   }

   public static <T extends Comparable<T>> SortedArraySet<T> create() {
      return create(10);
   }

   public static <T extends Comparable<T>> SortedArraySet<T> create(int param0) {
      return new SortedArraySet<T>(param0, Comparator.<T>naturalOrder());
   }

   public static <T> SortedArraySet<T> create(Comparator<T> param0) {
      return create(param0, 10);
   }

   public static <T> SortedArraySet<T> create(Comparator<T> param0, int param1) {
      return new SortedArraySet<>(param1, param0);
   }

   private static <T> T[] castRawArray(Object[] param0) {
      return (T[])param0;
   }

   private int findIndex(T param0) {
      return Arrays.binarySearch(this.contents, 0, this.size, param0, this.comparator);
   }

   private static int getInsertionPosition(int param0) {
      return -param0 - 1;
   }

   @Override
   public boolean add(T param0) {
      int var2 = this.findIndex(param0);
      if (var2 >= 0) {
         return false;
      }

      int var3 = getInsertionPosition(var2);
      this.addInternal(param0, var3);
      return true;
   }

   private void grow(int param0) {
      if (param0 > this.contents.length) {
         if (this.contents != ObjectArrays.DEFAULT_EMPTY_ARRAY) {
            param0 = (int)Math.max(Math.min((long)this.contents.length + (this.contents.length >> 1), 2147483639L), param0);
         } else if (param0 < 10) {
            param0 = 10;
         }

         Object[] var2 = new Object[param0];
         System.arraycopy(this.contents, 0, var2, 0, this.size);
         this.contents = (T[])castRawArray(var2);
      }
   }

   private void addInternal(T param0, int param1) {
      this.grow(this.size + 1);
      if (param1 != this.size) {
         System.arraycopy(this.contents, param1, this.contents, param1 + 1, this.size - param1);
      }

      this.contents[param1] = param0;
      this.size++;
   }

   void removeInternal(int param0) {
      this.size--;
      if (param0 != this.size) {
         System.arraycopy(this.contents, param0 + 1, this.contents, param0, this.size - param0);
      }

      this.contents[this.size] = null;
   }

   private T getInternal(int param0) {
      return this.contents[param0];
   }

   public T addOrGet(T param0) {
      int var2 = this.findIndex(param0);
      if (var2 >= 0) {
         return this.getInternal(var2);
      }

      this.addInternal(param0, getInsertionPosition(var2));
      return param0;
   }

   @Override
   public boolean remove(Object param0) {
      int var2 = this.findIndex((T)param0);
      if (var2 >= 0) {
         this.removeInternal(var2);
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   public T get(T param0) {
      int var2 = this.findIndex(param0);
      return var2 >= 0 ? this.getInternal(var2) : null;
   }

   public T first() {
      return this.getInternal(0);
   }

   public T last() {
      return this.getInternal(this.size - 1);
   }

   @Override
   public boolean contains(Object param0) {
      int var2 = this.findIndex((T)param0);
      return var2 >= 0;
   }

   @Override
   public Iterator<T> iterator() {
      return new SortedArraySet.ArrayIterator();
   }

   @Override
   public int size() {
      return this.size;
   }

   @Override
   public Object[] toArray() {
      return Arrays.copyOf(this.contents, this.size, Object[].class);
   }

   @Override
   public <U> U[] toArray(U[] param0) {
      if (param0.length < this.size) {
         return (U[])Arrays.copyOf(this.contents, this.size, (Class<? extends T[]>)param0.getClass());
      }

      System.arraycopy(this.contents, 0, param0, 0, this.size);
      if (param0.length > this.size) {
         param0[this.size] = null;
      }

      return param0;
   }

   @Override
   public void clear() {
      Arrays.fill(this.contents, 0, this.size, null);
      this.size = 0;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return param0 instanceof SortedArraySet<?> var2 && this.comparator.equals(var2.comparator)
            ? this.size == var2.size && Arrays.equals(this.contents, var2.contents)
            : super.equals(param0);
      }
   }

   class ArrayIterator implements Iterator<T> {
      private int index;
      private int last = -1;

      @Override
      public boolean hasNext() {
         return this.index < SortedArraySet.this.size;
      }

      @Override
      public T next() {
         if (this.index >= SortedArraySet.this.size) {
            throw new NoSuchElementException();
         }

         this.last = this.index++;
         return SortedArraySet.this.contents[this.last];
      }

      @Override
      public void remove() {
         if (this.last == -1) {
            throw new IllegalStateException();
         }

         SortedArraySet.this.removeInternal(this.last);
         this.index--;
         this.last = -1;
      }
   }
}
