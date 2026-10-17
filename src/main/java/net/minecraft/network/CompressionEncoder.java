package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.zip.Deflater;

public class CompressionEncoder extends MessageToByteEncoder<ByteBuf> {
   private final byte[] encodeBuf = new byte[8192];
   private final Deflater deflater;
   private int threshold;

   public CompressionEncoder(int param0) {
      this.threshold = param0;
      this.deflater = new Deflater();
   }

   protected void encode(ChannelHandlerContext param0, ByteBuf param1, ByteBuf param2) {
      int var4 = param1.readableBytes();
      FriendlyByteBuf var5 = new FriendlyByteBuf(param2);
      if (var4 < this.threshold) {
         var5.writeVarInt(0);
         var5.writeBytes(param1);
      } else {
         byte[] var6 = new byte[var4];
         param1.readBytes(var6);
         var5.writeVarInt(var6.length);
         this.deflater.setInput(var6, 0, var4);
         this.deflater.finish();

         while (!this.deflater.finished()) {
            int var7 = this.deflater.deflate(this.encodeBuf);
            var5.writeBytes(this.encodeBuf, 0, var7);
         }

         this.deflater.reset();
      }
   }

   public int getThreshold() {
      return this.threshold;
   }

   public void setThreshold(int param0) {
      this.threshold = param0;
   }
}
