package net.minecraft.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.function.ToIntFunction;
import net.minecraft.Util;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public interface DataProvider {
   ToIntFunction<String> FIXED_ORDER_FIELDS = Util.make(new Object2IntOpenHashMap<>(), param0 -> {
      param0.put("type", 0);
      param0.put("parent", 1);
      param0.defaultReturnValue(2);
   });
   Comparator<String> KEY_COMPARATOR = Comparator.comparingInt(FIXED_ORDER_FIELDS).thenComparing(param0 -> (String)param0);
   Logger LOGGER = LogUtils.getLogger();

   CompletableFuture<?> run(CachedOutput var1);

   String getName();

   static CompletableFuture<?> saveStable(CachedOutput param0, JsonElement param1, Path param2) {
      return CompletableFuture.runAsync(() -> {
         try {
            ByteArrayOutputStream l_var3 = new ByteArrayOutputStream();
            HashingOutputStream l_var4 = new HashingOutputStream(Hashing.sha1(), l_var3);

            try (JsonWriter l_var5 = new JsonWriter(new OutputStreamWriter(l_var4, StandardCharsets.UTF_8))) {
               l_var5.setSerializeNulls(false);
               l_var5.setIndent("  ");
               GsonHelper.writeValue(l_var5, param1, KEY_COMPARATOR);
            }

            param0.writeIfNeeded(param2, l_var3.toByteArray(), l_var4.hash());
         } catch (IOException l_var3) {
            LOGGER.error("Failed to save file to {}", param2, l_var3);
         }
      }, Util.backgroundExecutor());
   }

   @FunctionalInterface
   interface Factory<T extends DataProvider> {
      T create(PackOutput var1);
   }
}
