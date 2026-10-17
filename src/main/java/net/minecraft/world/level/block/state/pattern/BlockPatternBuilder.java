package net.minecraft.world.level.block.state.pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class BlockPatternBuilder {
   private static final Joiner COMMA_JOINED = Joiner.on(",");
   private final List<String[]> pattern = Lists.newArrayList();
   private final Map<Character, Predicate<BlockInWorld>> lookup = Maps.newHashMap();
   private int height;
   private int width;

   private BlockPatternBuilder() {
      this.lookup.put(' ', l_param0 -> true);
   }

   public BlockPatternBuilder aisle(String... param0) {
      if (!ArrayUtils.isEmpty(param0) && !StringUtils.isEmpty(param0[0])) {
         if (this.pattern.isEmpty()) {
            this.height = param0.length;
            this.width = param0[0].length();
         }

         if (param0.length != this.height) {
            throw new IllegalArgumentException("Expected aisle with height of " + this.height + ", but was given one with a height of " + param0.length + ")");
         }

         for (String var5 : param0) {
            if (var5.length() != this.width) {
               throw new IllegalArgumentException(
                  "Not all rows in the given aisle are the correct width (expected " + this.width + ", found one with " + var5.length() + ")"
               );
            }

            for (char var9 : var5.toCharArray()) {
               if (!this.lookup.containsKey(var9)) {
                  this.lookup.put(var9, null);
               }
            }
         }

         this.pattern.add(param0);
         return this;
      } else {
         throw new IllegalArgumentException("Empty pattern for aisle");
      }
   }

   public static BlockPatternBuilder start() {
      return new BlockPatternBuilder();
   }

   public BlockPatternBuilder where(char param0, Predicate<BlockInWorld> param1) {
      this.lookup.put(param0, param1);
      return this;
   }

   public BlockPattern build() {
      return new BlockPattern(this.createPattern());
   }

   private Predicate<BlockInWorld>[][][] createPattern() {
      this.ensureAllCharactersMatched();
      Predicate<BlockInWorld>[][][] var1 = (Predicate<BlockInWorld>[][][])Array.newInstance(Predicate.class, this.pattern.size(), this.height, this.width);

      for (int var2 = 0; var2 < this.pattern.size(); var2++) {
         for (int var3 = 0; var3 < this.height; var3++) {
            for (int var4 = 0; var4 < this.width; var4++) {
               var1[var2][var3][var4] = this.lookup.get(this.pattern.get(var2)[var3].charAt(var4));
            }
         }
      }

      return var1;
   }

   private void ensureAllCharactersMatched() {
      List<Character> var1 = Lists.newArrayList();

      for (Entry<Character, Predicate<BlockInWorld>> var3 : this.lookup.entrySet()) {
         if (var3.getValue() == null) {
            var1.add(var3.getKey());
         }
      }

      if (!var1.isEmpty()) {
         throw new IllegalStateException("Predicates for character(s) " + COMMA_JOINED.join(var1) + " are missing");
      }
   }
}
