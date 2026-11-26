package net.minecraft.server.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public class LegacyQueryHandler extends ChannelInboundHandlerAdapter {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int FAKE_PROTOCOL_VERSION = 127;
   private final ServerConnectionListener serverConnectionListener;

   public LegacyQueryHandler(ServerConnectionListener param0) {
      this.serverConnectionListener = param0;
   }

   @Override
   public void channelRead(ChannelHandlerContext param0, Object param1) {
      ByteBuf var3 = (ByteBuf)param1;
      var3.markReaderIndex();
      boolean var4 = true;

      try {
         try {
            if (var3.readUnsignedByte() != 254) {
               return;
            }

            InetSocketAddress var5 = (InetSocketAddress)param0.channel().remoteAddress();
            MinecraftServer var6 = this.serverConnectionListener.getServer();
            int var7 = var3.readableBytes();
            switch (var7) {
               case 0: {
                  LOGGER.debug("Ping: (<1.3.x) from {}:{}", var5.getAddress(), var5.getPort());
                  String var8 = String.format(Locale.ROOT, "%s§%d§%d", var6.getMotd(), var6.getPlayerCount(), var6.getMaxPlayers());
                  this.sendFlushAndClose(param0, this.createReply(var8));
                  break;
               }
               case 1: {
                  if (var3.readUnsignedByte() != 1) {
                     return;
                  }

                  LOGGER.debug("Ping: (1.4-1.5.x) from {}:{}", var5.getAddress(), var5.getPort());
                  String var8 = String.format(
                     Locale.ROOT,
                     "§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
                     127,
                     var6.getServerVersion(),
                     var6.getMotd(),
                     var6.getPlayerCount(),
                     var6.getMaxPlayers()
                  );
                  this.sendFlushAndClose(param0, this.createReply(var8));
                  break;
               }
               default: {
                  boolean var8 = var3.readUnsignedByte() == 1;
                  var8 &= var3.readUnsignedByte() == 250;
                  var8 &= "MC|PingHost".equals(new String(var3.readBytes(var3.readShort() * 2).array(), StandardCharsets.UTF_16BE));
                  int var9 = var3.readUnsignedShort();
                  var8 &= var3.readUnsignedByte() >= 73;
                  var8 &= 3 + var3.readBytes(var3.readShort() * 2).array().length + 4 == var9;
                  var8 &= var3.readInt() <= 65535;
                  var8 &= var3.readableBytes() == 0;
                  if (!var8) {
                     return;
                  }

                  LOGGER.debug("Ping: (1.6) from {}:{}", var5.getAddress(), var5.getPort());
                  String var10 = String.format(
                     Locale.ROOT,
                     "§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
                     127,
                     var6.getServerVersion(),
                     var6.getMotd(),
                     var6.getPlayerCount(),
                     var6.getMaxPlayers()
                  );
                  ByteBuf var11 = this.createReply(var10);

                  try {
                     this.sendFlushAndClose(param0, var11);
                  } finally {
                     var11.release();
                  }
               }
            }

            var3.release();
            var4 = false;
         } catch (RuntimeException var21) {
         }
      } finally {
         if (var4) {
            var3.resetReaderIndex();
            param0.channel().pipeline().remove("legacy_query");
            param0.fireChannelRead(param1);
         }
      }
   }

   private void sendFlushAndClose(ChannelHandlerContext param0, ByteBuf param1) {
      param0.pipeline().firstContext().writeAndFlush(param1).addListener(ChannelFutureListener.CLOSE);
   }

   private ByteBuf createReply(String param0) {
      ByteBuf var2 = Unpooled.buffer();
      var2.writeByte(255);
      char[] var3 = param0.toCharArray();
      var2.writeShort(var3.length);

      for (char var7 : var3) {
         var2.writeChar(var7);
      }

      return var2;
   }
}
