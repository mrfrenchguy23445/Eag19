package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public interface MultiBufferSource {
   static MultiBufferSource.BufferSource immediate(BufferBuilder param0) {
      return immediateWithBuffers(ImmutableMap.of(), param0);
   }

   static MultiBufferSource.BufferSource immediateWithBuffers(Map<RenderType, BufferBuilder> param0, BufferBuilder param1) {
      return new MultiBufferSource.BufferSource(param1, param0);
   }

   VertexConsumer getBuffer(RenderType var1);

   class BufferSource implements MultiBufferSource {
      protected final BufferBuilder builder;
      protected final Map<RenderType, BufferBuilder> fixedBuffers;
      protected Optional<RenderType> lastState = Optional.empty();
      protected final Set<BufferBuilder> startedBuffers = Sets.newHashSet();

      protected BufferSource(BufferBuilder param0, Map<RenderType, BufferBuilder> param1) {
         this.builder = param0;
         this.fixedBuffers = param1;
      }

      @Override
      public VertexConsumer getBuffer(RenderType param0) {
         Optional<RenderType> var2 = param0.asOptional();
         BufferBuilder var3 = this.getBuilderRaw(param0);
         if (!Objects.equals(this.lastState, var2) || !param0.canConsolidateConsecutiveGeometry()) {
            if (this.lastState.isPresent()) {
               RenderType var4 = this.lastState.get();
               if (!this.fixedBuffers.containsKey(var4)) {
                  this.endBatch(var4);
               }
            }

            if (this.startedBuffers.add(var3)) {
               var3.begin(param0.mode(), param0.format());
            }

            this.lastState = var2;
         }

         return var3;
      }

      private BufferBuilder getBuilderRaw(RenderType param0) {
         return this.fixedBuffers.getOrDefault(param0, this.builder);
      }

      public void endLastBatch() {
         if (this.lastState.isPresent()) {
            RenderType var1 = this.lastState.get();
            if (!this.fixedBuffers.containsKey(var1)) {
               this.endBatch(var1);
            }

            this.lastState = Optional.empty();
         }
      }

      public void endBatch() {
         this.lastState.ifPresent(l_param0 -> {
            VertexConsumer l_var2 = this.getBuffer(l_param0);
            if (l_var2 == this.builder) {
               this.endBatch(l_param0);
            }
         });

         for (RenderType var2 : this.fixedBuffers.keySet()) {
            this.endBatch(var2);
         }
      }

      public void endBatch(RenderType param0) {
         BufferBuilder var2 = this.getBuilderRaw(param0);
         boolean var3 = Objects.equals(this.lastState, param0.asOptional());
         if (var3 || var2 != this.builder) {
            if (this.startedBuffers.remove(var2)) {
               param0.end(var2, 0, 0, 0);
               if (var3) {
                  this.lastState = Optional.empty();
               }
            }
         }
      }
   }
}
