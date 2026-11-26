package net.minecraft.server.packs.resources;

import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;

public interface ResourceMetadata {
   ResourceMetadata EMPTY = new ResourceMetadata() {
      @Override
      public <T> Optional<T> getSection(MetadataSectionSerializer<T> param0) {
         return Optional.empty();
      }
   };
   IoSupplier<ResourceMetadata> EMPTY_SUPPLIER = () -> EMPTY;

   static ResourceMetadata fromJsonStream(InputStream param0) throws IOException {
      try (BufferedReader var1 = new BufferedReader(new InputStreamReader(param0, StandardCharsets.UTF_8))) {
         final JsonObject var2 = GsonHelper.parse(var1);
         return new ResourceMetadata() {
            @Override
            public <T> Optional<T> getSection(MetadataSectionSerializer<T> l_param0) {
               String l_var2 = l_param0.getMetadataSectionName();
               return var2.has(l_var2) ? Optional.of(l_param0.fromJson(GsonHelper.getAsJsonObject(var2, l_var2))) : Optional.empty();
            }
         };
      }
   }

   <T> Optional<T> getSection(MetadataSectionSerializer<T> var1);
}
