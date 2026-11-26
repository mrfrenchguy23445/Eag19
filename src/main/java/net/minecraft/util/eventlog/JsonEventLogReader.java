package net.minecraft.util.eventlog;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import javax.annotation.Nullable;
import net.minecraft.Util;

public interface JsonEventLogReader<T> extends Closeable {
   static <T> JsonEventLogReader<T> create(final Codec<T> param0, Reader param1) {
      final JsonReader var2 = new JsonReader(param1);
      var2.setLenient(true);
      return new JsonEventLogReader<T>() {
         @Nullable
         @Override
         public T next() throws IOException {
            try {
               if (!var2.hasNext()) {
                  return null;
               }

               JsonElement l_var1 = JsonParser.parseReader(var2);
               return Util.getOrThrow(param0.parse(JsonOps.INSTANCE, l_var1), IOException::new);
            } catch (JsonParseException l_var1) {
               throw new IOException(l_var1);
            } catch (EOFException l_var1) {
               return null;
            }
         }

         @Override
         public void close() throws IOException {
            var2.close();
         }
      };
   }

   @Nullable
   T next() throws IOException;
}
