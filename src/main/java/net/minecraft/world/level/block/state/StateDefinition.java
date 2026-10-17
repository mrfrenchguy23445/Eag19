package net.minecraft.world.level.block.state;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

public class StateDefinition<O, S extends StateHolder<O, S>> {
   static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");
   private final O owner;
   private final ImmutableSortedMap<String, Property<?>> propertiesByName;
   private final ImmutableList<S> states;

   protected StateDefinition(Function<O, S> param0, O param1, StateDefinition.Factory<O, S> param2, Map<String, Property<?>> param3) {
      this.owner = param1;
      this.propertiesByName = ImmutableSortedMap.copyOf(param3);
      Supplier<S> var5 = () -> param0.apply(param1);
      MapCodec<S> var6 = MapCodec.of(Encoder.empty(), Decoder.unit(var5));

      for (Entry<String, Property<?>> var8 : this.propertiesByName.entrySet()) {
         var6 = appendPropertyCodec(var6, var5, var8.getKey(), var8.getValue());
      }

      MapCodec<S> var7 = var6;
      Map<Map<Property<?>, Comparable<?>>, S> var8 = Maps.newLinkedHashMap();
      List<S> var9 = Lists.newArrayList();
      Stream<List<Pair<Property<?>, Comparable<?>>>> var10 = Stream.of(Collections.emptyList());

      for (Property<?> var12 : this.propertiesByName.values()) {
         var10 = var10.flatMap(l_param1 -> var12.getPossibleValues().stream().map(l2_param2 -> {
            List<Pair<Property<?>, Comparable<?>>> l2_var3 = Lists.newArrayList(l_param1);
            l2_var3.add(Pair.of(var12, l2_param2));
            return l2_var3;
         }));
      }

      var10.forEach(l_param5 -> {
         ImmutableMap<Property<?>, Comparable<?>> l_var6 = l_param5.stream().collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
         S l_var7 = param2.create(param1, l_var6, var7);
         var8.put(l_var6, l_var7);
         var9.add(l_var7);
      });

      for (S var12 : var9) {
         var12.populateNeighbours(var8);
      }

      this.states = ImmutableList.copyOf(var9);
   }

   private static <S extends StateHolder<?, S>, T extends Comparable<T>> MapCodec<S> appendPropertyCodec(
      MapCodec<S> param0, Supplier<S> param1, String param2, Property<T> param3
   ) {
      return Codec.mapPair(param0, param3.valueCodec().fieldOf(param2).orElseGet(l_param0 -> {}, () -> param3.value(param1.get())))
         .xmap(l_param1 -> l_param1.getFirst().setValue(param3, l_param1.getSecond().value()), l_param1 -> Pair.of((S)l_param1, param3.value(l_param1)));
   }

   public ImmutableList<S> getPossibleStates() {
      return this.states;
   }

   public S any() {
      return this.states.get(0);
   }

   public O getOwner() {
      return this.owner;
   }

   public Collection<Property<?>> getProperties() {
      return this.propertiesByName.values();
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
         .add("block", this.owner)
         .add("properties", this.propertiesByName.values().stream().map(Property::getName).collect(Collectors.toList()))
         .toString();
   }

   @Nullable
   public Property<?> getProperty(String param0) {
      return this.propertiesByName.get(param0);
   }

   public static class Builder<O, S extends StateHolder<O, S>> {
      private final O owner;
      private final Map<String, Property<?>> properties = Maps.newHashMap();

      public Builder(O param0) {
         this.owner = param0;
      }

      public StateDefinition.Builder<O, S> add(Property<?>... param0) {
         for (Property<?> var5 : param0) {
            this.validateProperty(var5);
            this.properties.put(var5.getName(), var5);
         }

         return this;
      }

      private <T extends Comparable<T>> void validateProperty(Property<T> param0) {
         String var2 = param0.getName();
         if (!StateDefinition.NAME_PATTERN.matcher(var2).matches()) {
            throw new IllegalArgumentException(this.owner + " has invalidly named property: " + var2);
         }

         Collection<T> var3 = param0.getPossibleValues();
         if (var3.size() <= 1) {
            throw new IllegalArgumentException(this.owner + " attempted use property " + var2 + " with <= 1 possible values");
         }

         for (T var5 : var3) {
            String var6 = param0.getName(var5);
            if (!StateDefinition.NAME_PATTERN.matcher(var6).matches()) {
               throw new IllegalArgumentException(this.owner + " has property: " + var2 + " with invalidly named value: " + var6);
            }
         }

         if (this.properties.containsKey(var2)) {
            throw new IllegalArgumentException(this.owner + " has duplicate property: " + var2);
         }
      }

      public StateDefinition<O, S> create(Function<O, S> param0, StateDefinition.Factory<O, S> param1) {
         return new StateDefinition<>(param0, this.owner, param1, this.properties);
      }
   }

   public interface Factory<O, S> {
      S create(O var1, ImmutableMap<Property<?>, Comparable<?>> var2, MapCodec<S> var3);
   }
}
