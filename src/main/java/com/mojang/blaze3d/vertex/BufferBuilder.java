package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Floats;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class BufferBuilder extends DefaultedVertexConsumer implements BufferVertexConsumer {
   private static final int GROWTH_SIZE = 2097152;
   private static final Logger LOGGER = LogUtils.getLogger();
   private ByteBuffer buffer;
   private int renderedBufferCount;
   private int renderedBufferPointer;
   private int nextElementByte;
   private int vertices;
   @Nullable
   private VertexFormatElement currentElement;
   private int elementIndex;
   private VertexFormat format;
   private VertexFormat.Mode mode;
   private boolean fastFormat;
   private boolean fullFormat;
   private boolean building;
   @Nullable
   private Vector3f[] sortingPoints;
   private float sortX = Float.NaN;
   private float sortY = Float.NaN;
   private float sortZ = Float.NaN;
   private boolean indexOnly;

   public BufferBuilder(int param0) {
      this.buffer = MemoryTracker.create(param0 * 6);
   }

   private void ensureVertexCapacity() {
      this.ensureCapacity(this.format.getVertexSize());
   }

   private void ensureCapacity(int param0) {
      if (this.nextElementByte + param0 > this.buffer.capacity()) {
         int var2 = this.buffer.capacity();
         int var3 = var2 + roundUp(param0);
         LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", var2, var3);
         ByteBuffer var4 = MemoryTracker.resize(this.buffer, var3);
         var4.rewind();
         this.buffer = var4;
      }
   }

   private static int roundUp(int param0) {
      int var1 = 2097152;
      if (param0 == 0) {
         return var1;
      }

      if (param0 < 0) {
         var1 *= -1;
      }

      int var2 = param0 % var1;
      return var2 == 0 ? param0 : param0 + var1 - var2;
   }

   public void setQuadSortOrigin(float param0, float param1, float param2) {
      if (this.mode == VertexFormat.Mode.QUADS) {
         if (this.sortX != param0 || this.sortY != param1 || this.sortZ != param2) {
            this.sortX = param0;
            this.sortY = param1;
            this.sortZ = param2;
            if (this.sortingPoints == null) {
               this.sortingPoints = this.makeQuadSortingPoints();
            }
         }
      }
   }

   public BufferBuilder.SortState getSortState() {
      return new BufferBuilder.SortState(this.mode, this.vertices, this.sortingPoints, this.sortX, this.sortY, this.sortZ);
   }

   public void restoreSortState(BufferBuilder.SortState param0) {
      this.buffer.rewind();
      this.mode = param0.mode;
      this.vertices = param0.vertices;
      this.nextElementByte = this.renderedBufferPointer;
      this.sortingPoints = param0.sortingPoints;
      this.sortX = param0.sortX;
      this.sortY = param0.sortY;
      this.sortZ = param0.sortZ;
      this.indexOnly = true;
   }

   public void begin(VertexFormat.Mode param0, VertexFormat param1) {
      if (this.building) {
         throw new IllegalStateException("Already building!");
      }

      this.building = true;
      this.mode = param0;
      this.switchFormat(param1);
      this.currentElement = param1.getElements().get(0);
      this.elementIndex = 0;
      this.buffer.rewind();
   }

   private void switchFormat(VertexFormat param0) {
      if (this.format != param0) {
         this.format = param0;
         boolean var2 = param0 == DefaultVertexFormat.NEW_ENTITY;
         boolean var3 = param0 == DefaultVertexFormat.BLOCK;
         this.fastFormat = var2 || var3;
         this.fullFormat = var2;
      }
   }

   private IntConsumer intConsumer(int param0, VertexFormat.IndexType param1) {
      MutableInt var3 = new MutableInt(param0);

      return switch (param1) {
         case BYTE -> l_param1 -> this.buffer.put(var3.getAndIncrement(), (byte)l_param1);
         case SHORT -> l_param1 -> this.buffer.putShort(var3.getAndAdd(2), (short)l_param1);
         case INT -> l_param1 -> this.buffer.putInt(var3.getAndAdd(4), l_param1);
      };
   }

   private Vector3f[] makeQuadSortingPoints() {
      FloatBuffer var1 = this.buffer.asFloatBuffer();
      int var2 = this.renderedBufferPointer / 4;
      int var3 = this.format.getIntegerSize();
      int var4 = var3 * this.mode.primitiveStride;
      int var5 = this.vertices / this.mode.primitiveStride;
      Vector3f[] var6 = new Vector3f[var5];

      for (int var7 = 0; var7 < var5; var7++) {
         float var8 = var1.get(var2 + var7 * var4 + 0);
         float var9 = var1.get(var2 + var7 * var4 + 1);
         float var10 = var1.get(var2 + var7 * var4 + 2);
         float var11 = var1.get(var2 + var7 * var4 + var3 * 2 + 0);
         float var12 = var1.get(var2 + var7 * var4 + var3 * 2 + 1);
         float var13 = var1.get(var2 + var7 * var4 + var3 * 2 + 2);
         float var14 = (var8 + var11) / 2.0F;
         float var15 = (var9 + var12) / 2.0F;
         float var16 = (var10 + var13) / 2.0F;
         var6[var7] = new Vector3f(var14, var15, var16);
      }

      return var6;
   }

   private void putSortedQuadIndices(VertexFormat.IndexType param0) {
      float[] var2 = new float[this.sortingPoints.length];
      int[] var3 = new int[this.sortingPoints.length];

      for (int var4 = 0; var4 < this.sortingPoints.length; var3[var4] = var4++) {
         float var5 = this.sortingPoints[var4].x() - this.sortX;
         float var6 = this.sortingPoints[var4].y() - this.sortY;
         float var7 = this.sortingPoints[var4].z() - this.sortZ;
         var2[var4] = var5 * var5 + var6 * var6 + var7 * var7;
      }

      IntArrays.mergeSort(var3, (l_param1, l_param2) -> Floats.compare(var2[l_param2], var2[l_param1]));
      IntConsumer var4 = this.intConsumer(this.nextElementByte, param0);

      for (int var8 : var3) {
         var4.accept(var8 * this.mode.primitiveStride + 0);
         var4.accept(var8 * this.mode.primitiveStride + 1);
         var4.accept(var8 * this.mode.primitiveStride + 2);
         var4.accept(var8 * this.mode.primitiveStride + 2);
         var4.accept(var8 * this.mode.primitiveStride + 3);
         var4.accept(var8 * this.mode.primitiveStride + 0);
      }
   }

   public boolean isCurrentBatchEmpty() {
      return this.vertices == 0;
   }

   @Nullable
   public BufferBuilder.RenderedBuffer endOrDiscardIfEmpty() {
      this.ensureDrawing();
      if (this.isCurrentBatchEmpty()) {
         this.reset();
         return null;
      } else {
         BufferBuilder.RenderedBuffer var1 = this.storeRenderedBuffer();
         this.reset();
         return var1;
      }
   }

   public BufferBuilder.RenderedBuffer end() {
      this.ensureDrawing();
      BufferBuilder.RenderedBuffer var1 = this.storeRenderedBuffer();
      this.reset();
      return var1;
   }

   private void ensureDrawing() {
      if (!this.building) {
         throw new IllegalStateException("Not building!");
      }
   }

   private BufferBuilder.RenderedBuffer storeRenderedBuffer() {
      int var1 = this.mode.indexCount(this.vertices);
      int var2 = !this.indexOnly ? this.vertices * this.format.getVertexSize() : 0;
      VertexFormat.IndexType var3 = VertexFormat.IndexType.least(var1);
      boolean var4;
      int var5;
      if (this.sortingPoints != null) {
         int var6 = Mth.roundToward(var1 * var3.bytes, 4);
         this.ensureCapacity(var6);
         this.putSortedQuadIndices(var3);
         var4 = false;
         this.nextElementByte += var6;
         var5 = var2 + var6;
      } else {
         var4 = true;
         var5 = var2;
      }

      int var6 = this.renderedBufferPointer;
      this.renderedBufferPointer += var5;
      this.renderedBufferCount++;
      BufferBuilder.DrawState var7 = new BufferBuilder.DrawState(this.format, this.vertices, var1, this.mode, var3, this.indexOnly, var4);
      return new BufferBuilder.RenderedBuffer(var6, var7);
   }

   private void reset() {
      this.building = false;
      this.vertices = 0;
      this.currentElement = null;
      this.elementIndex = 0;
      this.sortingPoints = null;
      this.sortX = Float.NaN;
      this.sortY = Float.NaN;
      this.sortZ = Float.NaN;
      this.indexOnly = false;
   }

   @Override
   public void putByte(int param0, byte param1) {
      this.buffer.put(this.nextElementByte + param0, param1);
   }

   @Override
   public void putShort(int param0, short param1) {
      this.buffer.putShort(this.nextElementByte + param0, param1);
   }

   @Override
   public void putFloat(int param0, float param1) {
      this.buffer.putFloat(this.nextElementByte + param0, param1);
   }

   @Override
   public void endVertex() {
      if (this.elementIndex != 0) {
         throw new IllegalStateException("Not filled all elements of the vertex");
      }

      this.vertices++;
      this.ensureVertexCapacity();
      if (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP) {
         int var1 = this.format.getVertexSize();
         this.buffer.put(this.nextElementByte, this.buffer, this.nextElementByte - var1, var1);
         this.nextElementByte += var1;
         this.vertices++;
         this.ensureVertexCapacity();
      }
   }

   @Override
   public void nextElement() {
      ImmutableList<VertexFormatElement> var1 = this.format.getElements();
      this.elementIndex = (this.elementIndex + 1) % var1.size();
      this.nextElementByte = this.nextElementByte + this.currentElement.getByteSize();
      VertexFormatElement var2 = var1.get(this.elementIndex);
      this.currentElement = var2;
      if (var2.getUsage() == VertexFormatElement.Usage.PADDING) {
         this.nextElement();
      }

      if (this.defaultColorSet && this.currentElement.getUsage() == VertexFormatElement.Usage.COLOR) {
         BufferVertexConsumer.super.color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
      }
   }

   @Override
   public VertexConsumer color(int param0, int param1, int param2, int param3) {
      if (this.defaultColorSet) {
         throw new IllegalStateException();
      } else {
         return BufferVertexConsumer.super.color(param0, param1, param2, param3);
      }
   }

   @Override
   public void vertex(
      float param0,
      float param1,
      float param2,
      float param3,
      float param4,
      float param5,
      float param6,
      float param7,
      float param8,
      int param9,
      int param10,
      float param11,
      float param12,
      float param13
   ) {
      if (this.defaultColorSet) {
         throw new IllegalStateException();
      }

      if (this.fastFormat) {
         this.putFloat(0, param0);
         this.putFloat(4, param1);
         this.putFloat(8, param2);
         this.putByte(12, (byte)(param3 * 255.0F));
         this.putByte(13, (byte)(param4 * 255.0F));
         this.putByte(14, (byte)(param5 * 255.0F));
         this.putByte(15, (byte)(param6 * 255.0F));
         this.putFloat(16, param7);
         this.putFloat(20, param8);
         int var15;
         if (this.fullFormat) {
            this.putShort(24, (short)(param9 & 65535));
            this.putShort(26, (short)(param9 >> 16 & 65535));
            var15 = 28;
         } else {
            var15 = 24;
         }

         this.putShort(var15 + 0, (short)(param10 & 65535));
         this.putShort(var15 + 2, (short)(param10 >> 16 & 65535));
         this.putByte(var15 + 4, BufferVertexConsumer.normalIntValue(param11));
         this.putByte(var15 + 5, BufferVertexConsumer.normalIntValue(param12));
         this.putByte(var15 + 6, BufferVertexConsumer.normalIntValue(param13));
         this.nextElementByte += var15 + 8;
         this.endVertex();
      } else {
         super.vertex(param0, param1, param2, param3, param4, param5, param6, param7, param8, param9, param10, param11, param12, param13);
      }
   }

   void releaseRenderedBuffer() {
      if (this.renderedBufferCount > 0 && --this.renderedBufferCount == 0) {
         this.clear();
      }
   }

   public void clear() {
      if (this.renderedBufferCount > 0) {
         LOGGER.warn("Clearing BufferBuilder with unused batches");
      }

      this.discard();
   }

   public void discard() {
      this.renderedBufferCount = 0;
      this.renderedBufferPointer = 0;
      this.nextElementByte = 0;
   }

   @Override
   public VertexFormatElement currentElement() {
      if (this.currentElement == null) {
         throw new IllegalStateException("BufferBuilder not started");
      } else {
         return this.currentElement;
      }
   }

   public boolean building() {
      return this.building;
   }

   ByteBuffer bufferSlice(int param0, int param1) {
      // LWJGL's MemoryUtil.memSlice(this.buffer, offset, length) is replaced with the plain
      // java.nio slice dance. LWJGL memSlice is an absolute-address range of a direct buffer and
      // its result is rewind()-ed by the caller; duplicating and re-positioning reproduces exactly
      // that, without needing org.lwjgl.system.MemoryUtil (absent from the WASM-GC classlib).
      return this.buffer.duplicate().position(param0).limit(param1);
   }

   public record DrawState(
      VertexFormat format,
      int vertexCount,
      int indexCount,
      VertexFormat.Mode mode,
      VertexFormat.IndexType indexType,
      boolean indexOnly,
      boolean sequentialIndex
   ) {

      public int vertexBufferSize() {
         return this.vertexCount * this.format.getVertexSize();
      }

      public int vertexBufferStart() {
         return 0;
      }

      public int vertexBufferEnd() {
         return this.vertexBufferSize();
      }

      public int indexBufferStart() {
         return this.indexOnly ? 0 : this.vertexBufferEnd();
      }

      public int indexBufferEnd() {
         return this.indexBufferStart() + this.indexBufferSize();
      }

      private int indexBufferSize() {
         return this.sequentialIndex ? 0 : this.indexCount * this.indexType.bytes;
      }

      public int bufferSize() {
         return this.indexBufferEnd();
      }
   }

   public class RenderedBuffer {
      private final int pointer;
      private final BufferBuilder.DrawState drawState;
      private boolean released;

      RenderedBuffer(int param1, BufferBuilder.DrawState param2) {
         this.pointer = param1;
         this.drawState = param2;
      }

      public ByteBuffer vertexBuffer() {
         int var1 = this.pointer + this.drawState.vertexBufferStart();
         int var2 = this.pointer + this.drawState.vertexBufferEnd();
         return BufferBuilder.this.bufferSlice(var1, var2);
      }

      public ByteBuffer indexBuffer() {
         int var1 = this.pointer + this.drawState.indexBufferStart();
         int var2 = this.pointer + this.drawState.indexBufferEnd();
         return BufferBuilder.this.bufferSlice(var1, var2);
      }

      public BufferBuilder.DrawState drawState() {
         return this.drawState;
      }

      public boolean isEmpty() {
         return this.drawState.vertexCount == 0;
      }

      public void release() {
         if (this.released) {
            throw new IllegalStateException("Buffer has already been released!");
         }

         BufferBuilder.this.releaseRenderedBuffer();
         this.released = true;
      }
   }

   public static class SortState {
      final VertexFormat.Mode mode;
      final int vertices;
      @Nullable
      final Vector3f[] sortingPoints;
      final float sortX;
      final float sortY;
      final float sortZ;

      SortState(VertexFormat.Mode param0, int param1, @Nullable Vector3f[] param2, float param3, float param4, float param5) {
         this.mode = param0;
         this.vertices = param1;
         this.sortingPoints = param2;
         this.sortX = param3;
         this.sortY = param4;
         this.sortZ = param5;
      }
   }
}
