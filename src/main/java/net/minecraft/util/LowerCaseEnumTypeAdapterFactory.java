package net.minecraft.util;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

public class LowerCaseEnumTypeAdapterFactory implements TypeAdapterFactory {
   @Nullable
   @Override
   public <T> TypeAdapter<T> create(Gson param0, TypeToken<T> param1) {
      Class<T> var3 = (Class<T>)param1.getRawType();
      if (!var3.isEnum()) {
         return null;
      }

      final Map<String, T> var4 = Maps.newHashMap();

      for (T var8 : var3.getEnumConstants()) {
         var4.put(this.toLowercase(var8), var8);
      }

      return new TypeAdapter<T>() {
         @Override
         public void write(JsonWriter l_param0, T l_param1) throws IOException {
            if (l_param1 == null) {
               l_param0.nullValue();
            } else {
               l_param0.value(LowerCaseEnumTypeAdapterFactory.this.toLowercase(l_param1));
            }
         }

         @Nullable
         @Override
         public T read(JsonReader l_param0) throws IOException {
            if (l_param0.peek() == JsonToken.NULL) {
               l_param0.nextNull();
               return null;
            } else {
               return var4.get(l_param0.nextString());
            }
         }
      };
   }

   String toLowercase(Object param0) {
      return param0 instanceof Enum ? ((Enum)param0).name().toLowerCase(Locale.ROOT) : param0.toString().toLowerCase(Locale.ROOT);
   }
}
