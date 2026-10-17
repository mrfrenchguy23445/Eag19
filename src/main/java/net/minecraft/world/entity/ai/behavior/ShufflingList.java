package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.RandomSource;

public class ShufflingList<U> implements Iterable<U> {
   protected final List<ShufflingList.WeightedEntry<U>> entries;
   private final RandomSource random = RandomSource.create();

   public ShufflingList() {
      this.entries = Lists.newArrayList();
   }

   private ShufflingList(List<ShufflingList.WeightedEntry<U>> param0) {
      this.entries = Lists.newArrayList(param0);
   }

   public static <U> Codec<ShufflingList<U>> codec(Codec<U> param0) {
      return ShufflingList.WeightedEntry.codec(param0).listOf().xmap(ShufflingList::new, l_param0 -> l_param0.entries);
   }

   public ShufflingList<U> add(U param0, int param1) {
      this.entries.add(new ShufflingList.WeightedEntry<>(param0, param1));
      return this;
   }

   public ShufflingList<U> shuffle() {
      this.entries.forEach(l_param0 -> l_param0.setRandom(this.random.nextFloat()));
      this.entries.sort(Comparator.comparingDouble(ShufflingList.WeightedEntry::getRandWeight));
      return this;
   }

   public Stream<U> stream() {
      return this.entries.stream().map(ShufflingList.WeightedEntry::getData);
   }

   @Override
   public Iterator<U> iterator() {
      return Iterators.transform(this.entries.iterator(), ShufflingList.WeightedEntry::getData);
   }

   @Override
   public String toString() {
      return "ShufflingList[" + this.entries + "]";
   }

   public static class WeightedEntry<T> {
      final T data;
      final int weight;
      private double randWeight;

      WeightedEntry(T param0, int param1) {
         this.weight = param1;
         this.data = param0;
      }

      private double getRandWeight() {
         return this.randWeight;
      }

      void setRandom(float param0) {
         this.randWeight = -Math.pow(param0, 1.0F / this.weight);
      }

      public T getData() {
         return this.data;
      }

      public int getWeight() {
         return this.weight;
      }

      @Override
      public String toString() {
         return this.weight + ":" + this.data;
      }

      public static <E> Codec<ShufflingList.WeightedEntry<E>> codec(final Codec<E> param0) {
         return new Codec<ShufflingList.WeightedEntry<E>>() {
            @Override
            public <T> DataResult<Pair<ShufflingList.WeightedEntry<E>, T>> decode(DynamicOps<T> l_param0, T l_param1) {
               Dynamic<T> l_var3 = new Dynamic<>(l_param0, l_param1);
               return l_var3.get("data")
                  .flatMap(param0::parse)
                  .map(l2_param1 -> new ShufflingList.WeightedEntry<>(l2_param1, l_var3.get("weight").asInt(1)))
                  .map(l2_param1 -> Pair.of((ShufflingList.WeightedEntry<E>)l2_param1, l_param0.empty()));
            }

            public <T> DataResult<T> encode(ShufflingList.WeightedEntry<E> l_param0, DynamicOps<T> l_param1, T l_param2) {
               return l_param1.mapBuilder()
                  .add("weight", l_param1.createInt(l_param0.weight))
                  .add("data", param0.encodeStart(l_param1, l_param0.data))
                  .build(l_param2);
            }
         };
      }
   }
}
