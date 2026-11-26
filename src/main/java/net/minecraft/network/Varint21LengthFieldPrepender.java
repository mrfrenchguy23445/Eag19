package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class Varint21LengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {
   private static final int MAX_BYTES = 3;

   protected void encode(ChannelHandlerContext param0, ByteBuf param1, ByteBuf param2) {
      int var4 = param1.readableBytes();
      int var5 = FriendlyByteBuf.getVarIntSize(var4);
      if (var5 > 3) {
         throw new IllegalArgumentException("unable to fit " + var4 + " into 3");
      }

      FriendlyByteBuf var6 = new FriendlyByteBuf(param2);
      var6.ensureWritable(var5 + var4);
      var6.writeVarInt(var4);
      var6.writeBytes(param1, param1.readerIndex(), var4);
   }
}
