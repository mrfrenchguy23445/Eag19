package net.minecraft.client.renderer.block.model.multipart;

import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

@FunctionalInterface
public interface Condition {
   Condition TRUE = param0 -> l_param0 -> true;
   Condition FALSE = param0 -> l_param0 -> false;

   Predicate<BlockState> getPredicate(StateDefinition<Block, BlockState> var1);
}
