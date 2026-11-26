package net.minecraft.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public interface Condition extends Supplier<JsonElement> {
   void validate(StateDefinition<?, ?> var1);

   static Condition.TerminalCondition condition() {
      return new Condition.TerminalCondition();
   }

   static Condition and(Condition... param0) {
      return new Condition.CompositeCondition(Condition.Operation.AND, Arrays.asList(param0));
   }

   static Condition or(Condition... param0) {
      return new Condition.CompositeCondition(Condition.Operation.OR, Arrays.asList(param0));
   }

   class CompositeCondition implements Condition {
      private final Condition.Operation operation;
      private final List<Condition> subconditions;

      CompositeCondition(Condition.Operation param0, List<Condition> param1) {
         this.operation = param0;
         this.subconditions = param1;
      }

      @Override
      public void validate(StateDefinition<?, ?> param0) {
         this.subconditions.forEach(l_param1 -> l_param1.validate(param0));
      }

      public JsonElement get() {
         JsonArray var1 = new JsonArray();
         this.subconditions.stream().map(Supplier::get).forEach(var1::add);
         JsonObject var2 = new JsonObject();
         var2.add(this.operation.id, var1);
         return var2;
      }
   }

   enum Operation {
      AND("AND"),
      OR("OR");

      final String id;

      Operation(String param2) {
         this.id = param2;
      }
   }

   class TerminalCondition implements Condition {
      private final Map<Property<?>, String> terms = Maps.newHashMap();

      private static <T extends Comparable<T>> String joinValues(Property<T> param0, Stream<T> param1) {
         return param1.<CharSequence>map(param0::getName).collect(Collectors.joining("|"));
      }

      private static <T extends Comparable<T>> String getTerm(Property<T> param0, T param1, T[] param2) {
         return joinValues(param0, Stream.concat(Stream.of(param1), Stream.of(param2)));
      }

      private <T extends Comparable<T>> void putValue(Property<T> param0, String param1) {
         String var3 = this.terms.put(param0, param1);
         if (var3 != null) {
            throw new IllegalStateException("Tried to replace " + param0 + " value from " + var3 + " to " + param1);
         }
      }

      public final <T extends Comparable<T>> Condition.TerminalCondition term(Property<T> param0, T param1) {
         this.putValue(param0, param0.getName(param1));
         return this;
      }

      @SafeVarargs
      public final <T extends Comparable<T>> Condition.TerminalCondition term(Property<T> param0, T param1, T... param2) {
         this.putValue(param0, getTerm(param0, param1, param2));
         return this;
      }

      public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(Property<T> param0, T param1) {
         this.putValue(param0, "!" + param0.getName(param1));
         return this;
      }

      @SafeVarargs
      public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(Property<T> param0, T param1, T... param2) {
         this.putValue(param0, "!" + getTerm(param0, param1, param2));
         return this;
      }

      public JsonElement get() {
         JsonObject var1 = new JsonObject();
         this.terms.forEach((l_param1, l_param2) -> var1.addProperty(l_param1.getName(), l_param2));
         return var1;
      }

      @Override
      public void validate(StateDefinition<?, ?> param0) {
         List<Property<?>> var2 = this.terms
            .keySet()
            .stream()
            .filter(l_param1 -> param0.getProperty(l_param1.getName()) != l_param1)
            .collect(Collectors.toList());
         if (!var2.isEmpty()) {
            throw new IllegalStateException("Properties " + var2 + " are missing from " + param0);
         }
      }
   }
}
