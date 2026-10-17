package net.minecraft.server.packs.resources;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public abstract class SimpleJsonResourceReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Gson gson;
   private final String directory;

   public SimpleJsonResourceReloadListener(Gson param0, String param1) {
      this.gson = param0;
      this.directory = param1;
   }

   protected Map<ResourceLocation, JsonElement> prepare(ResourceManager param0, ProfilerFiller param1) {
      Map<ResourceLocation, JsonElement> var3 = Maps.newHashMap();
      FileToIdConverter var4 = FileToIdConverter.json(this.directory);

      for (Entry<ResourceLocation, Resource> var6 : var4.listMatchingResources(param0).entrySet()) {
         ResourceLocation var7 = var6.getKey();
         ResourceLocation var8 = var4.fileToId(var7);

         try (Reader var9 = var6.getValue().openAsReader()) {
            JsonElement var10 = GsonHelper.fromJson(this.gson, var9, JsonElement.class);
            JsonElement var11 = var3.put(var8, var10);
            if (var11 != null) {
               throw new IllegalStateException("Duplicate data file ignored with ID " + var8);
            }
         } catch (JsonParseException | IllegalArgumentException | IOException var9) {
            LOGGER.error("Couldn't parse data file {} from {}", var8, var7, var9);
         }
      }

      return var3;
   }
}
