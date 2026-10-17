package net.minecraft.client.renderer.texture.atlas;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class SpriteResourceLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter ATLAS_INFO_CONVERTER = new FileToIdConverter("atlases", ".json");
   private final List<SpriteSource> sources;

   private SpriteResourceLoader(List<SpriteSource> param0) {
      this.sources = param0;
   }

   public List<Supplier<SpriteContents>> list(ResourceManager param0) {
      final Map<ResourceLocation, SpriteSource.SpriteSupplier> var2 = new HashMap<>();
      SpriteSource.Output var3 = new SpriteSource.Output() {
         @Override
         public void add(ResourceLocation l_param0, SpriteSource.SpriteSupplier l_param1) {
            SpriteSource.SpriteSupplier l_var3 = var2.put(l_param0, l_param1);
            if (l_var3 != null) {
               l_var3.discard();
            }
         }

         @Override
         public void removeAll(Predicate<ResourceLocation> l_param0) {
            Iterator<Entry<ResourceLocation, SpriteSource.SpriteSupplier>> l_var2 = var2.entrySet().iterator();

            while (l_var2.hasNext()) {
               Entry<ResourceLocation, SpriteSource.SpriteSupplier> l_var3 = l_var2.next();
               if (l_param0.test(l_var3.getKey())) {
                  l_var3.getValue().discard();
                  l_var2.remove();
               }
            }
         }
      };
      this.sources.forEach(l_param2 -> l_param2.run(param0, var3));
      Builder<Supplier<SpriteContents>> var4 = ImmutableList.builder();
      var4.add(MissingTextureAtlasSprite::create);
      var4.addAll(var2.values());
      return var4.build();
   }

   public static SpriteResourceLoader load(ResourceManager param0, ResourceLocation param1) {
      ResourceLocation var2 = ATLAS_INFO_CONVERTER.idToFile(param1);
      List<SpriteSource> var3 = new ArrayList<>();

      for (Resource var5 : param0.getResourceStack(var2)) {
         try (BufferedReader var6 = var5.openAsReader()) {
            Dynamic<JsonElement> var7 = new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseReader(var6));
            var3.addAll(SpriteSources.FILE_CODEC.parse(var7).getOrThrow(false, LOGGER::error));
         } catch (Exception var6) {
            LOGGER.warn("Failed to parse atlas definition {} in pack {}", var2, var5.sourcePackId(), var6);
         }
      }

      return new SpriteResourceLoader(var3);
   }
}
