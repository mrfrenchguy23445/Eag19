package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;

abstract class CombiningPredicate implements BlockPredicate {
   protected final List<BlockPredicate> predicates;

   protected CombiningPredicate(List<BlockPredicate> param0) {
      this.predicates = param0;
   }

   public static <T extends CombiningPredicate> Codec<T> codec(Function<List<BlockPredicate>, T> param0) {
      return RecordCodecBuilder.create(
         l_param1 -> l_param1.group(BlockPredicate.CODEC.listOf().fieldOf("predicates").forGetter(l2_param0 -> l2_param0.predicates)).apply(l_param1, param0)
      );
   }
}
