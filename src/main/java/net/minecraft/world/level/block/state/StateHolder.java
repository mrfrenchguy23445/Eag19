package net.minecraft.world.level.block.state;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class StateHolder<O, S> {
   public static final String NAME_TAG = "Name";
   public static final String PROPERTIES_TAG = "Properties";
   private static final Function<Entry<Property<?>, Comparable<?>>, String> PROPERTY_ENTRY_TO_STRING_FUNCTION = new Function<Entry<Property<?>, Comparable<?>>, String>() {
      public String apply(@Nullable Entry<Property<?>, Comparable<?>> param0) {
         if (param0 == null) {
            return "<NULL>";
         }

         Property<?> var2 = param0.getKey();
         return var2.getName() + "=" + this.getName(var2, param0.getValue());
      }

      private <T extends Comparable<T>> String getName(Property<T> param0, Comparable<?> param1) {
         return param0.getName((T)param1);
      }
   };
   protected final O owner;
   private final ImmutableMap<Property<?>, Comparable<?>> values;
   private Table<Property<?>, Comparable<?>, S> neighbours;
   protected final MapCodec<S> propertiesCodec;

   protected StateHolder(O param0, ImmutableMap<Property<?>, Comparable<?>> param1, MapCodec<S> param2) {
      this.owner = param0;
      this.values = param1;
      this.propertiesCodec = param2;
   }

   public <T extends Comparable<T>> S cycle(Property<T> param0) {
      return this.setValue(param0, findNextInCollection(param0.getPossibleValues(), this.getValue(param0)));
   }

   protected static <T> T findNextInCollection(Collection<T> param0, T param1) {
      Iterator<T> var2 = param0.iterator();

      while (var2.hasNext()) {
         if (var2.next().equals(param1)) {
            if (var2.hasNext()) {
               return var2.next();
            }

            return param0.iterator().next();
         }
      }

      return var2.next();
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append(this.owner);
      if (!this.getValues().isEmpty()) {
         var1.append('[');
         var1.append(this.getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
         var1.append(']');
      }

      return var1.toString();
   }

   public Collection<Property<?>> getProperties() {
      return Collections.unmodifiableCollection(this.values.keySet());
   }

   public <T extends Comparable<T>> boolean hasProperty(Property<T> param0) {
      return this.values.containsKey(param0);
   }

   public <T extends Comparable<T>> T getValue(Property<T> param0) {
      Comparable<?> var2 = this.values.get(param0);
      if (var2 == null) {
         throw new IllegalArgumentException("Cannot get property " + param0 + " as it does not exist in " + this.owner);
      } else {
         return param0.getValueClass().cast(var2);
      }
   }

   public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> param0) {
      Comparable<?> var2 = this.values.get(param0);
      return var2 == null ? Optional.empty() : Optional.of(param0.getValueClass().cast(var2));
   }

   public <T extends Comparable<T>, V extends T> S setValue(Property<T> param0, V param1) {
      Comparable<?> var3 = this.values.get(param0);
      if (var3 == null) {
         throw new IllegalArgumentException("Cannot set property " + param0 + " as it does not exist in " + this.owner);
      } else if (var3 == param1) {
         return (S)this;
      } else {
         S var4 = this.neighbours.get(param0, param1);
         if (var4 == null) {
            throw new IllegalArgumentException("Cannot set property " + param0 + " to " + param1 + " on " + this.owner + ", it is not an allowed value");
         } else {
            return var4;
         }
      }
   }

   public <T extends Comparable<T>, V extends T> S trySetValue(Property<T> param0, V param1) {
      Comparable<?> var3 = this.values.get(param0);
      if (var3 != null && var3 != param1) {
         S var4 = this.neighbours.get(param0, param1);
         if (var4 == null) {
            throw new IllegalArgumentException("Cannot set property " + param0 + " to " + param1 + " on " + this.owner + ", it is not an allowed value");
         } else {
            return var4;
         }
      } else {
         return (S)this;
      }
   }

   public void populateNeighbours(Map<Map<Property<?>, Comparable<?>>, S> param0) {
      if (this.neighbours != null) {
         throw new IllegalStateException();
      }

      Table<Property<?>, Comparable<?>, S> var2 = HashBasedTable.create();

      for (Entry<Property<?>, Comparable<?>> var4 : this.values.entrySet()) {
         Property<?> var5 = var4.getKey();

         for (Comparable<?> var7 : var5.getPossibleValues()) {
            if (var7 != var4.getValue()) {
               var2.put(var5, var7, param0.get(this.makeNeighbourValues(var5, var7)));
            }
         }
      }

      this.neighbours = var2.isEmpty() ? var2 : ArrayTable.create(var2);
   }

   private Map<Property<?>, Comparable<?>> makeNeighbourValues(Property<?> param0, Comparable<?> param1) {
      Map<Property<?>, Comparable<?>> var3 = Maps.newHashMap(this.values);
      var3.put(param0, param1);
      return var3;
   }

   public ImmutableMap<Property<?>, Comparable<?>> getValues() {
      return this.values;
   }

   protected static <O, S extends StateHolder<O, S>> Codec<S> codec(Codec<O> param0, Function<O, S> param1) {
      return param0.dispatch(
         "Name",
         l_param0 -> l_param0.owner,
         l_param1 -> {
            S l_var2 = param1.apply((O)l_param1);
            return l_var2.getValues().isEmpty()
               ? Codec.unit(l_var2)
               : l_var2.propertiesCodec.codec().optionalFieldOf("Properties").xmap(l2_param1 -> l2_param1.orElse(l_var2), Optional::of).codec();
         }
      );
   }
}
