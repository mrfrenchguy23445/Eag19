package net.minecraft.client.telemetry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class TelemetryPropertyMap {
   final Map<TelemetryProperty<?>, Object> entries;

   TelemetryPropertyMap(Map<TelemetryProperty<?>, Object> param0) {
      this.entries = param0;
   }

   public static TelemetryPropertyMap.Builder builder() {
      return new TelemetryPropertyMap.Builder();
   }

   public static Codec<TelemetryPropertyMap> createCodec(final List<TelemetryProperty<?>> param0) {
      return (new MapCodec<TelemetryPropertyMap>() {
            public <T> RecordBuilder<T> encode(TelemetryPropertyMap l_param0, DynamicOps<T> l_param1, RecordBuilder<T> l_param2) {
               RecordBuilder<T> l_var4 = l_param2;

               for (TelemetryProperty<?> l_var6 : param0) {
                  l_var4 = this.encodeProperty(l_param0, l_var4, l_var6);
               }

               return l_var4;
            }

            private <T, V> RecordBuilder<T> encodeProperty(TelemetryPropertyMap l_param0, RecordBuilder<T> l_param1, TelemetryProperty<V> l_param2) {
               V l_var4 = l_param0.get(l_param2);
               return l_var4 != null ? l_param1.add(l_param2.id(), l_var4, l_param2.codec()) : l_param1;
            }

            @Override
            public <T> DataResult<TelemetryPropertyMap> decode(DynamicOps<T> l_param0, MapLike<T> l_param1) {
               DataResult<TelemetryPropertyMap.Builder> l_var3 = DataResult.success(new TelemetryPropertyMap.Builder());

               for (TelemetryProperty<?> l_var5 : param0) {
                  l_var3 = this.decodeProperty(l_var3, l_param0, l_param1, l_var5);
               }

               return l_var3.map(TelemetryPropertyMap.Builder::build);
            }

            private <T, V> DataResult<TelemetryPropertyMap.Builder> decodeProperty(
               DataResult<TelemetryPropertyMap.Builder> l_param0, DynamicOps<T> l_param1, MapLike<T> l_param2, TelemetryProperty<V> l_param3
            ) {
               T l_var5 = l_param2.get(l_param3.id());
               if (l_var5 != null) {
                  DataResult<V> l_var6 = l_param3.codec().parse(l_param1, l_var5);
                  return l_param0.apply2stable((l2_param1, l2_param2) -> l2_param1.put(l_param3, (V)l2_param2), l_var6);
               } else {
                  return l_param0;
               }
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> l_param0) {
               return param0.stream().map(TelemetryProperty::id).map(l_param0::createString);
            }
         })
         .codec();
   }

   @Nullable
   public <T> T get(TelemetryProperty<T> param0) {
      return (T)this.entries.get(param0);
   }

   @Override
   public String toString() {
      return this.entries.toString();
   }

   public Set<TelemetryProperty<?>> propertySet() {
      return this.entries.keySet();
   }

   public static class Builder {
      private final Map<TelemetryProperty<?>, Object> entries = new Reference2ObjectOpenHashMap<>();

      Builder() {
      }

      public <T> TelemetryPropertyMap.Builder put(TelemetryProperty<T> param0, T param1) {
         this.entries.put(param0, param1);
         return this;
      }

      public TelemetryPropertyMap.Builder putAll(TelemetryPropertyMap param0) {
         this.entries.putAll(param0.entries);
         return this;
      }

      public TelemetryPropertyMap build() {
         return new TelemetryPropertyMap(this.entries);
      }
   }
}
