package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class OrCondition implements Condition {
   public static final String TOKEN = "OR";
   private final Iterable<? extends Condition> conditions;

   public OrCondition(Iterable<? extends Condition> param0) {
      this.conditions = param0;
   }

   @Override
   public Predicate<BlockState> getPredicate(StateDefinition<Block, BlockState> param0) {
      List<Predicate<BlockState>> var2 = Streams.stream(this.conditions).map(l_param1 -> l_param1.getPredicate(param0)).collect(Collectors.toList());
      return l_param1 -> var2.stream().anyMatch(l2_param1 -> l2_param1.test(l_param1));
   }
}
