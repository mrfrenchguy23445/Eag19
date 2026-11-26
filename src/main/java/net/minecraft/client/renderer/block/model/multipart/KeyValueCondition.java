package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class KeyValueCondition implements Condition {
   private static final Splitter PIPE_SPLITTER = Splitter.on('|').omitEmptyStrings();
   private final String key;
   private final String value;

   public KeyValueCondition(String param0, String param1) {
      this.key = param0;
      this.value = param1;
   }

   @Override
   public Predicate<BlockState> getPredicate(StateDefinition<Block, BlockState> param0) {
      Property<?> var2 = param0.getProperty(this.key);
      if (var2 == null) {
         throw new RuntimeException(String.format(Locale.ROOT, "Unknown property '%s' on '%s'", this.key, param0.getOwner()));
      }

      String var3 = this.value;
      boolean var4 = !var3.isEmpty() && var3.charAt(0) == '!';
      if (var4) {
         var3 = var3.substring(1);
      }

      List<String> var5 = PIPE_SPLITTER.splitToList(var3);
      if (var5.isEmpty()) {
         throw new RuntimeException(String.format(Locale.ROOT, "Empty value '%s' for property '%s' on '%s'", this.value, this.key, param0.getOwner()));
      }

      Predicate<BlockState> var6;
      if (var5.size() == 1) {
         var6 = this.getBlockStatePredicate(param0, var2, var3);
      } else {
         List<Predicate<BlockState>> var7 = var5.stream().map(l_param2 -> this.getBlockStatePredicate(param0, var2, l_param2)).collect(Collectors.toList());
         var6 = l_param1 -> var7.stream().anyMatch(l2_param1 -> l2_param1.test(l_param1));
      }

      return var4 ? var6.negate() : var6;
   }

   private Predicate<BlockState> getBlockStatePredicate(StateDefinition<Block, BlockState> param0, Property<?> param1, String param2) {
      Optional<?> var4 = param1.getValue(param2);
      if (!var4.isPresent()) {
         throw new RuntimeException(
            String.format(Locale.ROOT, "Unknown value '%s' for property '%s' on '%s' in '%s'", param2, this.key, param0.getOwner(), this.value)
         );
      } else {
         return l_param2 -> l_param2.getValue(param1).equals(var4.get());
      }
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).add("key", this.key).add("value", this.value).toString();
   }
}
