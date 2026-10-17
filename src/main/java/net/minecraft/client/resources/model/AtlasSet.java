package net.minecraft.client.resources.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class AtlasSet implements AutoCloseable {
   private final Map<ResourceLocation, AtlasSet.AtlasEntry> atlases;

   public AtlasSet(Map<ResourceLocation, ResourceLocation> param0, TextureManager param1) {
      this.atlases = param0.entrySet().stream().collect(Collectors.toMap(Entry::getKey, l_param1 -> {
         TextureAtlas l_var2 = new TextureAtlas(l_param1.getKey());
         param1.register(l_param1.getKey(), l_var2);
         return new AtlasSet.AtlasEntry(l_var2, l_param1.getValue());
      }));
   }

   public TextureAtlas getAtlas(ResourceLocation param0) {
      return this.atlases.get(param0).atlas();
   }

   @Override
   public void close() {
      this.atlases.values().forEach(AtlasSet.AtlasEntry::close);
      this.atlases.clear();
   }

   public Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> scheduleLoad(ResourceManager param0, int param1, Executor param2) {
      return this.atlases
         .entrySet()
         .stream()
         .collect(
            Collectors.toMap(
               Entry::getKey,
               l_param3 -> {
                  AtlasSet.AtlasEntry l_var4 = l_param3.getValue();
                  return SpriteLoader.create(l_var4.atlas)
                     .loadAndStitch(param0, l_var4.atlasInfoLocation, param1, param2)
                     .thenApply(l2_param1 -> new AtlasSet.StitchResult(l_var4.atlas, l2_param1));
               }
            )
         );
   }

   record AtlasEntry(TextureAtlas atlas, ResourceLocation atlasInfoLocation) implements AutoCloseable {

      @Override
      public void close() {
         this.atlas.clearTextureData();
      }
   }

   public static class StitchResult {
      private final TextureAtlas atlas;
      private final SpriteLoader.Preparations preparations;

      public StitchResult(TextureAtlas param0, SpriteLoader.Preparations param1) {
         this.atlas = param0;
         this.preparations = param1;
      }

      @Nullable
      public TextureAtlasSprite getSprite(ResourceLocation param0) {
         return this.preparations.regions().get(param0);
      }

      public TextureAtlasSprite missing() {
         return this.preparations.missing();
      }

      public CompletableFuture<Void> readyForUpload() {
         return this.preparations.readyForUpload();
      }

      public void upload() {
         this.atlas.upload(this.preparations);
      }
   }
}
