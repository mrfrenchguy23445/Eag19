package net.minecraft.world.level.block.state;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

   // Cooperative-port (FerriteCore-style): vanilla stores block-state properties in a
   // Guava ImmutableMap and the neighbour lookup in an ArrayTable; every
   // getValue/hasProperty call hashes and every setValue/trySetValue call does TWO
   // linear indexOf scans through the table's row/column key lists. These are the
   // hottest block-state paths in worldgen (feature placement, updateShapeAtEdge)
   // and model building, and they are pure overhead in wasm-gc. Replaced with flat
   // arrays aligned to this state's property list, neighbour rows indexed via
   // Property.valueIndex. Built once in populateNeighbours at registry construction
   // (StateDefinition ctor, before any setValue can occur). getValues()/codec keep
   // the ImmutableMap, so serialization, toString and saves are untouched.
   @Nullable
   private Property<?>[] fastProperties;
   @Nullable
   private Comparable<?>[] fastValues;
   @Nullable
   private S[][] fastNeighbours;
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
      Property<?>[] var2 = this.fastProperties;
      if (var2 != null) {
         for (int var3 = 0; var3 < var2.length; var3++) {
            if (var2[var3] == param0) {
               return true;
            }
         }

         return false;
      }

      return this.values.containsKey(param0);
   }

   public <T extends Comparable<T>> T getValue(Property<T> param0) {
      Property<?>[] var2 = this.fastProperties;
      if (var2 != null) {
         for (int var3 = 0; var3 < var2.length; var3++) {
            if (var2[var3] == param0) {
               return param0.getValueClass().cast(this.fastValues[var3]);
            }
         }

         throw new IllegalArgumentException("Cannot get property " + param0 + " as it does not exist in " + this.owner);
      }

      Comparable<?> var4 = this.values.get(param0);
      if (var4 == null) {
         throw new IllegalArgumentException("Cannot get property " + param0 + " as it does not exist in " + this.owner);
      } else {
         return param0.getValueClass().cast(var4);
      }
   }

   public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> param0) {
      Property<?>[] var2 = this.fastProperties;
      if (var2 != null) {
         for (int var3 = 0; var3 < var2.length; var3++) {
            if (var2[var3] == param0) {
               return Optional.of(param0.getValueClass().cast(this.fastValues[var3]));
            }
         }

         return Optional.empty();
      }

      Comparable<?> var4 = this.values.get(param0);
      return var4 == null ? Optional.empty() : Optional.of(param0.getValueClass().cast(var4));
   }

   public <T extends Comparable<T>, V extends T> S setValue(Property<T> param0, V param1) {
      // Cooperative-port: populateNeighbours is guaranteed to have run before any game
      // code (StateDefinition ctor invokes it for every state it creates), so the
      // flat tables below are always ready here.
      Property<?>[] var3 = this.fastProperties;
      for (int var4 = 0; var4 < var3.length; var4++) {
         if (var3[var4] == param0) {
            if (this.fastValues[var4] == param1) {
               return (S)this;
            }

            if (this.fastNeighbours[var4] == null) {
               throw new IllegalArgumentException("Cannot set property " + param0 + " to " + param1 + " on " + this.owner + ", it is not an allowed value");
            }

            int var5 = param0.valueIndex(param1);
            if (var5 >= 0) {
               S var6 = this.fastNeighbours[var4][var5];
               if (var6 != null) {
                  return var6;
               }
            }

            throw new IllegalArgumentException("Cannot set property " + param0 + " to " + param1 + " on " + this.owner + ", it is not an allowed value");
         }
      }

      throw new IllegalArgumentException("Cannot set property " + param0 + " as it does not exist in " + this.owner);
   }

   public <T extends Comparable<T>, V extends T> S trySetValue(Property<T> param0, V param1) {
      Property<?>[] var3 = this.fastProperties;
      for (int var4 = 0; var4 < var3.length; var4++) {
         if (var3[var4] == param0) {
            if (this.fastValues[var4] == param1) {
               return (S)this;
            }

            if (this.fastNeighbours[var4] == null) {
               throw new IllegalArgumentException("Cannot set property " + param0 + " to " + param1 + " on " + this.owner + ", it is not an allowed value");
            }

            int var5 = param0.valueIndex(param1);
            if (var5 >= 0) {
               S var6 = this.fastNeighbours[var4][var5];
               if (var6 != null) {
                  return var6;
               }
            }

            throw new IllegalArgumentException("Cannot set property " + param0 + " to " + param1 + " on " + this.owner + ", it is not an allowed value");
         }
      }

      return (S)this;
   }

   public void populateNeighbours(Map<Map<Property<?>, Comparable<?>>, S> param0) {
      if (this.fastProperties != null) {
         throw new IllegalStateException();
      }

      // Cooperative-port (FerriteCore-style): builds the flat lookup tables in place of
      // the old HashBasedTable + ArrayTable pair. Rows follow the state's property
      // order; each row is indexed by that property's value index. Boot-time only.
      List<Entry<Property<?>, Comparable<?>>> var2 = new ArrayList<>(this.values.entrySet());
      int var3 = var2.size();
      Property<?>[] var4 = new Property<?>[var3];
      Comparable<?>[] var5 = new Comparable<?>[var3];
      S[][] var6 = (S[][])new StateHolder[var3][];

      for (int var7 = 0; var7 < var3; var7++) {
         Entry<Property<?>, Comparable<?>> var8 = var2.get(var7);
         Property<?> var9 = var8.getKey();
         var4[var7] = var9;
         var5[var7] = var8.getValue();

         Collection<? extends Comparable<?>> var10 = var9.getPossibleValues();
         int var11 = var10.size();
         S[] var12 = var11 == 0 ? null : (S[])new StateHolder[var11];
         var6[var7] = var12;

         for (Comparable<?> var13 : var10) {
            if (var13 != var8.getValue()) {
               int var14 = var9.valueIndex(var13);
               if (var14 >= 0 && var12 != null) {
                  var12[var14] = param0.get(this.makeNeighbourValues(var9, var13));
               }
            }
         }
      }

      this.fastProperties = var4;
      this.fastValues = var5;
      this.fastNeighbours = var6;
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
