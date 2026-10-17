package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public abstract class AbstractPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String name;
   private final boolean isBuiltin;

   protected AbstractPackResources(String param0, boolean param1) {
      this.name = param0;
      this.isBuiltin = param1;
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionSerializer<T> param0) throws IOException {
      IoSupplier<InputStream> var2 = this.getRootResource("pack.mcmeta");
      if (var2 == null) {
         return null;
      }

      try (InputStream var3 = var2.get()) {
         return getMetadataFromStream(param0, var3);
      }
   }

   @Nullable
   public static <T> T getMetadataFromStream(MetadataSectionSerializer<T> param0, InputStream param1) {
      JsonObject var2;
      try (BufferedReader var3 = new BufferedReader(new InputStreamReader(param1, StandardCharsets.UTF_8))) {
         var2 = GsonHelper.parse(var3);
      } catch (Exception var3) {
         LOGGER.error("Couldn't load {} metadata", param0.getMetadataSectionName(), var3);
         return null;
      }

      if (!var2.has(param0.getMetadataSectionName())) {
         return null;
      }

      try {
         return param0.fromJson(GsonHelper.getAsJsonObject(var2, param0.getMetadataSectionName()));
      } catch (Exception var3) {
         LOGGER.error("Couldn't load {} metadata", param0.getMetadataSectionName(), var3);
         return null;
      }
   }

   @Override
   public String packId() {
      return this.name;
   }

   @Override
   public boolean isBuiltin() {
      return this.isBuiltin;
   }
}
