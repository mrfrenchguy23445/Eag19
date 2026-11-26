package net.minecraft.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;

public class CrudeIncrementalIntIdentityHashBiMap<K> implements IdMap<K> {
   private static final int NOT_FOUND = -1;
   private static final Object EMPTY_SLOT = null;
   private static final float LOADFACTOR = 0.8F;
   private K[] keys;
   private int[] values;
   private K[] byId;
   private int nextId;
   private int size;

   private CrudeIncrementalIntIdentityHashBiMap(int param0) {
      this.keys = (K[])(new Object[param0]);
      this.values = new int[param0];
      this.byId = (K[])(new Object[param0]);
   }

   private CrudeIncrementalIntIdentityHashBiMap(K[] param0, int[] param1, K[] param2, int param3, int param4) {
      this.keys = param0;
      this.values = param1;
      this.byId = param2;
      this.nextId = param3;
      this.size = param4;
   }

   public static <A> CrudeIncrementalIntIdentityHashBiMap<A> create(int param0) {
      return new CrudeIncrementalIntIdentityHashBiMap((int)(param0 / 0.8F));
   }

   @Override
   public int getId(@Nullable K param0) {
      return this.getValue(this.indexOf(param0, this.hash(param0)));
   }

   @Nullable
   @Override
   public K byId(int param0) {
      return param0 >= 0 && param0 < this.byId.length ? this.byId[param0] : null;
   }

   private int getValue(int param0) {
      return param0 == -1 ? -1 : this.values[param0];
   }

   public boolean contains(K param0) {
      return this.getId(param0) != -1;
   }

   public boolean contains(int param0) {
      return this.byId(param0) != null;
   }

   public int add(K param0) {
      int var2 = this.nextId();
      this.addMapping(param0, var2);
      return var2;
   }

   private int nextId() {
      while (this.nextId < this.byId.length && this.byId[this.nextId] != null) {
         this.nextId++;
      }

      return this.nextId;
   }

   private void grow(int param0) {
      K[] var2 = this.keys;
      int[] var3 = this.values;
      CrudeIncrementalIntIdentityHashBiMap<K> var4 = new CrudeIncrementalIntIdentityHashBiMap<>(param0);

      for (int var5 = 0; var5 < var2.length; var5++) {
         if (var2[var5] != null) {
            var4.addMapping(var2[var5], var3[var5]);
         }
      }

      this.keys = var4.keys;
      this.values = var4.values;
      this.byId = var4.byId;
      this.nextId = var4.nextId;
      this.size = var4.size;
   }

   public void addMapping(K param0, int param1) {
      int var3 = Math.max(param1, this.size + 1);
      if (var3 >= this.keys.length * 0.8F) {
         int var4 = this.keys.length << 1;

         while (var4 < param1) {
            var4 <<= 1;
         }

         this.grow(var4);
      }

      int var4 = this.findEmpty(this.hash(param0));
      this.keys[var4] = param0;
      this.values[var4] = param1;
      this.byId[param1] = param0;
      this.size++;
      if (param1 == this.nextId) {
         this.nextId++;
      }
   }

   private int hash(@Nullable K param0) {
      return (Mth.murmurHash3Mixer(System.identityHashCode(param0)) & 2147483647) % this.keys.length;
   }

   private int indexOf(@Nullable K param0, int param1) {
      for (int var3 = param1; var3 < this.keys.length; var3++) {
         if (this.keys[var3] == param0) {
            return var3;
         }

         if (this.keys[var3] == EMPTY_SLOT) {
            return -1;
         }
      }

      for (int var3 = 0; var3 < param1; var3++) {
         if (this.keys[var3] == param0) {
            return var3;
         }

         if (this.keys[var3] == EMPTY_SLOT) {
            return -1;
         }
      }

      return -1;
   }

   private int findEmpty(int param0) {
      for (int var2 = param0; var2 < this.keys.length; var2++) {
         if (this.keys[var2] == EMPTY_SLOT) {
            return var2;
         }
      }

      for (int var2 = 0; var2 < param0; var2++) {
         if (this.keys[var2] == EMPTY_SLOT) {
            return var2;
         }
      }

      throw new RuntimeException("Overflowed :(");
   }

   @Override
   public Iterator<K> iterator() {
      return Iterators.filter(Iterators.forArray(this.byId), Predicates.notNull());
   }

   public void clear() {
      Arrays.fill(this.keys, null);
      Arrays.fill(this.byId, null);
      this.nextId = 0;
      this.size = 0;
   }

   @Override
   public int size() {
      return this.size;
   }

   public CrudeIncrementalIntIdentityHashBiMap<K> copy() {
      return new CrudeIncrementalIntIdentityHashBiMap<>(
         (K[])((Object[])this.keys.clone()), (int[])this.values.clone(), (K[])((Object[])this.byId.clone()), this.nextId, this.size
      );
   }
}
