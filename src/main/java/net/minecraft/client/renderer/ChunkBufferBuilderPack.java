package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.Map;
import java.util.stream.Collectors;

public class ChunkBufferBuilderPack {
   private final Map<RenderType, BufferBuilder> builders = RenderType.chunkBufferLayers()
      .stream()
      .collect(Collectors.toMap(l_param0 -> (RenderType)l_param0, l_param0 -> new BufferBuilder(l_param0.bufferSize())));

   public BufferBuilder builder(RenderType param0) {
      return this.builders.get(param0);
   }

   public void clearAll() {
      this.builders.values().forEach(BufferBuilder::clear);
   }

   public void discardAll() {
      this.builders.values().forEach(BufferBuilder::discard);
   }
}
