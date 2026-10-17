package net.minecraft.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

public class MultiVariantGenerator implements BlockStateGenerator {
   private final Block block;
   private final List<Variant> baseVariants;
   private final Set<Property<?>> seenProperties = Sets.newHashSet();
   private final List<PropertyDispatch> declaredPropertySets = Lists.newArrayList();

   private MultiVariantGenerator(Block param0, List<Variant> param1) {
      this.block = param0;
      this.baseVariants = param1;
   }

   public MultiVariantGenerator with(PropertyDispatch param0) {
      param0.getDefinedProperties().forEach(l_param0 -> {
         if (this.block.getStateDefinition().getProperty(l_param0.getName()) != l_param0) {
            throw new IllegalStateException("Property " + l_param0 + " is not defined for block " + this.block);
         }

         if (!this.seenProperties.add((Property<?>)l_param0)) {
            throw new IllegalStateException("Values of property " + l_param0 + " already defined for block " + this.block);
         }
      });
      this.declaredPropertySets.add(param0);
      return this;
   }

   public JsonElement get() {
      Stream<Pair<Selector, List<Variant>>> var1 = Stream.of(Pair.of(Selector.empty(), this.baseVariants));

      for (PropertyDispatch var3 : this.declaredPropertySets) {
         Map<Selector, List<Variant>> var4 = var3.getEntries();
         var1 = var1.flatMap(l_param1 -> var4.entrySet().stream().map(l2_param1 -> {
            Selector l2_var2 = ((Selector)l_param1.getFirst()).extend(l2_param1.getKey());
            List<Variant> l2_var3 = mergeVariants((List<Variant>)l_param1.getSecond(), l2_param1.getValue());
            return Pair.of(l2_var2, l2_var3);
         }));
      }

      Map<String, JsonElement> var2 = new TreeMap<>();
      var1.forEach(l_param1 -> var2.put(l_param1.getFirst().getKey(), Variant.convertList(l_param1.getSecond())));
      JsonObject var3 = new JsonObject();
      var3.add("variants", Util.make(new JsonObject(), l_param1 -> var2.forEach(l_param1::add)));
      return var3;
   }

   private static List<Variant> mergeVariants(List<Variant> param0, List<Variant> param1) {
      Builder<Variant> var2 = ImmutableList.builder();
      param0.forEach(l_param2 -> param1.forEach(l2_param2 -> var2.add(Variant.merge(l_param2, l2_param2))));
      return var2.build();
   }

   @Override
   public Block getBlock() {
      return this.block;
   }

   public static MultiVariantGenerator multiVariant(Block param0) {
      return new MultiVariantGenerator(param0, ImmutableList.of(Variant.variant()));
   }

   public static MultiVariantGenerator multiVariant(Block param0, Variant param1) {
      return new MultiVariantGenerator(param0, ImmutableList.of(param1));
   }

   public static MultiVariantGenerator multiVariant(Block param0, Variant... param1) {
      return new MultiVariantGenerator(param0, ImmutableList.copyOf(param1));
   }
}
