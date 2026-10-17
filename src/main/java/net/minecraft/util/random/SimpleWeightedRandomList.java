package net.minecraft.util.random;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public class SimpleWeightedRandomList<E> extends WeightedRandomList<WeightedEntry.Wrapper<E>> {
   public static <E> Codec<SimpleWeightedRandomList<E>> wrappedCodecAllowingEmpty(Codec<E> param0) {
      return WeightedEntry.Wrapper.codec(param0).listOf().xmap(SimpleWeightedRandomList::new, WeightedRandomList::unwrap);
   }

   public static <E> Codec<SimpleWeightedRandomList<E>> wrappedCodec(Codec<E> param0) {
      return ExtraCodecs.nonEmptyList(WeightedEntry.Wrapper.codec(param0).listOf()).xmap(SimpleWeightedRandomList::new, WeightedRandomList::unwrap);
   }

   SimpleWeightedRandomList(List<? extends WeightedEntry.Wrapper<E>> param0) {
      super(param0);
   }

   public static <E> SimpleWeightedRandomList.Builder<E> builder() {
      return new SimpleWeightedRandomList.Builder<>();
   }

   public static <E> SimpleWeightedRandomList<E> empty() {
      return new SimpleWeightedRandomList<>(List.of());
   }

   public static <E> SimpleWeightedRandomList<E> single(E param0) {
      return new SimpleWeightedRandomList<>(List.of(WeightedEntry.wrap(param0, 1)));
   }

   public Optional<E> getRandomValue(RandomSource param0) {
      return this.getRandom(param0).map(WeightedEntry.Wrapper::getData);
   }

   public static class Builder<E> {
      private final ImmutableList.Builder<WeightedEntry.Wrapper<E>> result = ImmutableList.builder();

      public SimpleWeightedRandomList.Builder<E> add(E param0, int param1) {
         this.result.add(WeightedEntry.wrap(param0, param1));
         return this;
      }

      public SimpleWeightedRandomList<E> build() {
         return new SimpleWeightedRandomList<>(this.result.build());
      }
   }
}
