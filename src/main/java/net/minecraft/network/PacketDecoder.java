package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class PacketDecoder extends ByteToMessageDecoder {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PacketFlow flow;

   public PacketDecoder(PacketFlow param0) {
      this.flow = param0;
   }

   @Override
   protected void decode(ChannelHandlerContext param0, ByteBuf param1, List<Object> param2) throws Exception {
      int var4 = param1.readableBytes();
      if (var4 != 0) {
         FriendlyByteBuf var5 = new FriendlyByteBuf(param1);
         int var6 = var5.readVarInt();
         Packet<?> var7 = param0.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get().createPacket(this.flow, var6, var5);
         if (var7 == null) {
            throw new IOException("Bad packet id " + var6);
         }

         int var8 = param0.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get().getId();
         JvmProfiler.INSTANCE.onPacketReceived(var8, var6, param0.channel().remoteAddress(), var4);
         if (var5.readableBytes() > 0) {
            throw new IOException(
               "Packet "
                  + param0.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get().getId()
                  + "/"
                  + var6
                  + " ("
                  + var7.getClass().getSimpleName()
                  + ") was larger than I expected, found "
                  + var5.readableBytes()
                  + " bytes extra whilst reading packet "
                  + var6
            );
         }

         param2.add(var7);
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
               Connection.PACKET_RECEIVED_MARKER,
               " IN: [{}:{}] {}",
               param0.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get(),
               var6,
               var7.getClass().getName()
            );
         }
      }
   }
}
