package net.minecraft.world.level.block.state.properties;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.StateHolder;

public abstract class Property<T extends Comparable<T>> {
   private final Class<T> clazz;
   private final String name;
   @Nullable
   private Integer hashCode;
   private final Codec<T> codec = Codec.STRING
      .comapFlatMap(
         l_param0 -> this.getValue(l_param0)
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unable to read property: " + this + " with value: " + l_param0)),
         this::getName
      );
   private final Codec<Property.Value<T>> valueCodec = this.codec.xmap(this::value, Property.Value::value);

   protected Property(String param0, Class<T> param1) {
      this.clazz = param1;
      this.name = param0;
   }

   public Property.Value<T> value(T param0) {
      return new Property.Value<>(this, param0);
   }

   public Property.Value<T> value(StateHolder<?, ?> param0) {
      return new Property.Value<>(this, param0.getValue(this));
   }

   public Stream<Property.Value<T>> getAllValues() {
      return this.getPossibleValues().stream().map(this::value);
   }

   public Codec<T> codec() {
      return this.codec;
   }

   public Codec<Property.Value<T>> valueCodec() {
      return this.valueCodec;
   }

   public String getName() {
      return this.name;
   }

   public Class<T> getValueClass() {
      return this.clazz;
   }

   public abstract Collection<T> getPossibleValues();

   @Nullable
   private java.util.Map<Comparable<?>, Integer> valueIndexMap;

   /** Cooperative-port (FerriteCore-style): value -> position cache backing the flat
    * state tables in StateHolder. Equals-keyed so autoboxed values (e.g. an Integer
    * for DISTANCE passed through hot placement paths) resolve exactly. Built once
    * per Property at first use; Property instances are shared singletons per block
    * definition, so the cache is tiny and stable. */
   public int valueIndex(Comparable<?> param0) {
      if (this.valueIndexMap == null) {
         java.util.Map<Comparable<?>, Integer> var1 = new java.util.HashMap<>();
         for (Comparable<?> var3 : this.getPossibleValues()) {
            var1.put(var3, var1.size());
         }

         this.valueIndexMap = var1;
      }

      Integer var4 = this.valueIndexMap.get(param0);
      return var4 == null ? -1 : var4;
   }

   public abstract String getName(T var1);

   public abstract Optional<T> getValue(String var1);

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).add("name", this.name).add("clazz", this.clazz).add("values", this.getPossibleValues()).toString();
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return !(param0 instanceof Property<?> var2) ? false : this.clazz.equals(var2.clazz) && this.name.equals(var2.name);
      }
   }

   @Override
   public final int hashCode() {
      if (this.hashCode == null) {
         this.hashCode = this.generateHashCode();
      }

      return this.hashCode;
   }

   public int generateHashCode() {
      return 31 * this.clazz.hashCode() + this.name.hashCode();
   }

   public <U, S extends StateHolder<?, S>> DataResult<S> parseValue(DynamicOps<U> param0, S param1, U param2) {
      DataResult<T> var4 = this.codec.parse(param0, param2);
      return var4.<S>map(l_param1 -> param1.setValue(this, l_param1)).setPartial(param1);
   }

   public record Value<T extends Comparable<T>>(Property<T> property, T value) {
      public Value {
         if (!property.getPossibleValues().contains(value)) {
            throw new IllegalArgumentException("Value " + value + " does not belong to property " + property);
         }
      }

      @Override
      public String toString() {
         return this.property.getName() + "=" + this.property.getName(this.value);
      }
   }
}
