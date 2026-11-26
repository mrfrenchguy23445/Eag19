package com.mojang.blaze3d.vertex;

import net.minecraft.util.Mth;

public interface BufferVertexConsumer extends VertexConsumer {
   VertexFormatElement currentElement();

   void nextElement();

   void putByte(int var1, byte var2);

   void putShort(int var1, short var2);

   void putFloat(int var1, float var2);

   @Override
   default VertexConsumer vertex(double param0, double param2, double param4) {
      if (this.currentElement().getUsage() != VertexFormatElement.Usage.POSITION) {
         return this;
      } else if (this.currentElement().getType() == VertexFormatElement.Type.FLOAT && this.currentElement().getCount() == 3) {
         this.putFloat(0, (float)param0);
         this.putFloat(4, (float)param2);
         this.putFloat(8, (float)param4);
         this.nextElement();
         return this;
      } else {
         throw new IllegalStateException();
      }
   }

   @Override
   default VertexConsumer color(int param0, int param1, int param2, int param3) {
      VertexFormatElement var5 = this.currentElement();
      if (var5.getUsage() != VertexFormatElement.Usage.COLOR) {
         return this;
      } else if (var5.getType() == VertexFormatElement.Type.UBYTE && var5.getCount() == 4) {
         this.putByte(0, (byte)param0);
         this.putByte(1, (byte)param1);
         this.putByte(2, (byte)param2);
         this.putByte(3, (byte)param3);
         this.nextElement();
         return this;
      } else {
         throw new IllegalStateException();
      }
   }

   @Override
   default VertexConsumer uv(float param0, float param1) {
      VertexFormatElement var3 = this.currentElement();
      if (var3.getUsage() == VertexFormatElement.Usage.UV && var3.getIndex() == 0) {
         if (var3.getType() == VertexFormatElement.Type.FLOAT && var3.getCount() == 2) {
            this.putFloat(0, param0);
            this.putFloat(4, param1);
            this.nextElement();
            return this;
         } else {
            throw new IllegalStateException();
         }
      } else {
         return this;
      }
   }

   @Override
   default VertexConsumer overlayCoords(int param0, int param1) {
      return this.uvShort((short)param0, (short)param1, 1);
   }

   @Override
   default VertexConsumer uv2(int param0, int param1) {
      return this.uvShort((short)param0, (short)param1, 2);
   }

   default VertexConsumer uvShort(short param0, short param1, int param2) {
      VertexFormatElement var4 = this.currentElement();
      if (var4.getUsage() != VertexFormatElement.Usage.UV || var4.getIndex() != param2) {
         return this;
      } else if (var4.getType() == VertexFormatElement.Type.SHORT && var4.getCount() == 2) {
         this.putShort(0, param0);
         this.putShort(2, param1);
         this.nextElement();
         return this;
      } else {
         throw new IllegalStateException();
      }
   }

   @Override
   default VertexConsumer normal(float param0, float param1, float param2) {
      VertexFormatElement var4 = this.currentElement();
      if (var4.getUsage() != VertexFormatElement.Usage.NORMAL) {
         return this;
      } else if (var4.getType() == VertexFormatElement.Type.BYTE && var4.getCount() == 3) {
         this.putByte(0, normalIntValue(param0));
         this.putByte(1, normalIntValue(param1));
         this.putByte(2, normalIntValue(param2));
         this.nextElement();
         return this;
      } else {
         throw new IllegalStateException();
      }
   }

   static byte normalIntValue(float param0) {
      return (byte)((int)(Mth.clamp(param0, -1.0F, 1.0F) * 127.0F) & 0xFF);
   }
}
