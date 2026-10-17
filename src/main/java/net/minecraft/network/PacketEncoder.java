package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.IOException;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class PacketEncoder extends MessageToByteEncoder<Packet<?>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PacketFlow flow;

   public PacketEncoder(PacketFlow param0) {
      this.flow = param0;
   }

   protected void encode(ChannelHandlerContext param0, Packet<?> param1, ByteBuf param2) throws Exception {
      ConnectionProtocol var4 = param0.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get();
      if (var4 == null) {
         throw new RuntimeException("ConnectionProtocol unknown: " + param1);
      }

      int var5 = var4.getPacketId(this.flow, param1);
      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug(
            Connection.PACKET_SENT_MARKER, "OUT: [{}:{}] {}", param0.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get(), var5, param1.getClass().getName()
         );
      }

      if (var5 == -1) {
         throw new IOException("Can't serialize unregistered packet");
      }

      FriendlyByteBuf var6 = new FriendlyByteBuf(param2);
      var6.writeVarInt(var5);

      try {
         int var7 = var6.writerIndex();
         param1.write(var6);
         int var8 = var6.writerIndex() - var7;
         if (var8 > 8388608) {
            throw new IllegalArgumentException("Packet too big (is " + var8 + ", should be less than 8388608): " + param1);
         }

         int var9 = param0.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get().getId();
         JvmProfiler.INSTANCE.onPacketSent(var9, var5, param0.channel().remoteAddress(), var8);
      } catch (Throwable var7) {
         LOGGER.error("Error receiving packet {}", var5, var7);
         if (param1.isSkippable()) {
            throw new SkipPacketException(var7);
         } else {
            throw var7;
         }
      }
   }
}
