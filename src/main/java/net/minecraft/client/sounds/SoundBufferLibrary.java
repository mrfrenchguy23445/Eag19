package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

public class SoundBufferLibrary {
   private final ResourceProvider resourceManager;
   private final Map<ResourceLocation, CompletableFuture<SoundBuffer>> cache = Maps.newHashMap();

   public SoundBufferLibrary(ResourceProvider param0) {
      this.resourceManager = param0;
   }

   public CompletableFuture<SoundBuffer> getCompleteBuffer(ResourceLocation param0) {
      return this.cache.computeIfAbsent(param0, l_param0 -> CompletableFuture.supplyAsync(() -> {
         try (
            InputStream l2_var2 = this.resourceManager.open(l_param0);
            OggAudioStream l2_var3 = new OggAudioStream(l2_var2);
         ) {
            ByteBuffer l2_var4 = l2_var3.readAll();
            return new SoundBuffer(l2_var4, l2_var3.getFormat());
         } catch (IOException l2_var2) {
            throw new CompletionException(l2_var2);
         }
      }, Util.backgroundExecutor()));
   }

   public CompletableFuture<AudioStream> getStream(ResourceLocation param0, boolean param1) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            InputStream l_var3 = this.resourceManager.open(param0);
            return param1 ? new LoopingAudioStream(OggAudioStream::new, l_var3) : new OggAudioStream(l_var3);
         } catch (IOException l_var3) {
            throw new CompletionException(l_var3);
         }
      }, Util.backgroundExecutor());
   }

   public void clear() {
      this.cache.values().forEach(l_param0 -> l_param0.thenAccept(SoundBuffer::discardAlBuffer));
      this.cache.clear();
   }

   public CompletableFuture<?> preload(Collection<Sound> param0) {
      return CompletableFuture.allOf(param0.stream().map(l_param0 -> this.getCompleteBuffer(l_param0.getPath())).toArray(CompletableFuture[]::new));
   }
}
