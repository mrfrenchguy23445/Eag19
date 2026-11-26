package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class VertexFormat {
   private final ImmutableList<VertexFormatElement> elements;
   private final ImmutableMap<String, VertexFormatElement> elementMapping;
   private final IntList offsets = new IntArrayList();
   private final int vertexSize;
   @Nullable
   private VertexBuffer immediateDrawVertexBuffer;

   public VertexFormat(ImmutableMap<String, VertexFormatElement> param0) {
      this.elementMapping = param0;
      this.elements = param0.values().asList();
      int var2 = 0;

      for (VertexFormatElement var4 : param0.values()) {
         this.offsets.add(var2);
         var2 += var4.getByteSize();
      }

      this.vertexSize = var2;
   }

   @Override
   public String toString() {
      return "format: "
         + this.elementMapping.size()
         + " elements: "
         + this.elementMapping.entrySet().stream().map(Object::toString).collect(Collectors.joining(" "));
   }

   public int getIntegerSize() {
      return this.getVertexSize() / 4;
   }

   public int getVertexSize() {
      return this.vertexSize;
   }

   public ImmutableList<VertexFormatElement> getElements() {
      return this.elements;
   }

   public ImmutableList<String> getElementAttributeNames() {
      return this.elementMapping.keySet().asList();
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (param0 != null && this.getClass() == param0.getClass()) {
         VertexFormat var2 = (VertexFormat)param0;
         return this.vertexSize != var2.vertexSize ? false : this.elementMapping.equals(var2.elementMapping);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.elementMapping.hashCode();
   }

   public void setupBufferState() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(this::_setupBufferState);
      } else {
         this._setupBufferState();
      }
   }

   private void _setupBufferState() {
      int var1 = this.getVertexSize();
      List<VertexFormatElement> var2 = this.getElements();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         var2.get(var3).setupBufferState(var3, this.offsets.getInt(var3), var1);
      }
   }

   public void clearBufferState() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(this::_clearBufferState);
      } else {
         this._clearBufferState();
      }
   }

   private void _clearBufferState() {
      ImmutableList<VertexFormatElement> var1 = this.getElements();

      for (int var2 = 0; var2 < var1.size(); var2++) {
         VertexFormatElement var3 = var1.get(var2);
         var3.clearBufferState(var2);
      }
   }

   public VertexBuffer getImmediateDrawVertexBuffer() {
      VertexBuffer var1 = this.immediateDrawVertexBuffer;
      if (var1 == null) {
         this.immediateDrawVertexBuffer = var1 = new VertexBuffer();
      }

      return var1;
   }

   public enum IndexType {
      BYTE(5121, 1),
      SHORT(5123, 2),
      INT(5125, 4);

      public final int asGLType;
      public final int bytes;

      IndexType(int param2, int param3) {
         this.asGLType = param2;
         this.bytes = param3;
      }

      public static VertexFormat.IndexType least(int param0) {
         if ((param0 & -65536) != 0) {
            return INT;
         } else {
            return (param0 & 0xFF00) != 0 ? SHORT : BYTE;
         }
      }
   }

   public enum Mode {
      LINES(4, 2, 2, false),
      LINE_STRIP(5, 2, 1, true),
      DEBUG_LINES(1, 2, 2, false),
      DEBUG_LINE_STRIP(3, 2, 1, true),
      TRIANGLES(4, 3, 3, false),
      TRIANGLE_STRIP(5, 3, 1, true),
      TRIANGLE_FAN(6, 3, 1, true),
      QUADS(4, 4, 4, false);

      public final int asGLMode;
      public final int primitiveLength;
      public final int primitiveStride;
      public final boolean connectedPrimitives;

      Mode(int param2, int param3, int param4, boolean param5) {
         this.asGLMode = param2;
         this.primitiveLength = param3;
         this.primitiveStride = param4;
         this.connectedPrimitives = param5;
      }

      public int indexCount(int param0) {
         return switch (this) {
            case LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> param0;
            case LINES, QUADS -> param0 / 4 * 6;
            default -> 0;
         };
      }
   }
}
