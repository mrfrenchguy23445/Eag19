package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RateKickingConnection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.LazyLoadedValue;
import org.slf4j.Logger;

public class ServerConnectionListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   // Kept behind a holder so a local game never initializes Netty's thread pools.
   private static final class TcpEventGroup {
      static final EventLoopGroup INSTANCE = new LocalEventLoopGroup(0,
         new ThreadFactoryBuilder().setNameFormat("Netty Server IO #%d").setDaemon(true).build());
   }
   final MinecraftServer server;
   public volatile boolean running;
   private final List<ChannelFuture> channels = Collections.synchronizedList(Lists.newArrayList());
   final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

   public ServerConnectionListener(MinecraftServer param0) {
      this.server = param0;
      this.running = true;
   }

   public void startTcpServerListener(@Nullable InetAddress param0, int param1) throws IOException {
      synchronized (this.channels) {
         Class<? extends ServerSocketChannel> var4 = NioServerSocketChannel.class;
         LOGGER.info("Using default channel type");

         this.channels
            .add(
               new ServerBootstrap()
                  .channel(var4)
                  .childHandler(
                     new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel l_param0) {
                           try {
                              l_param0.config().setOption(ChannelOption.TCP_NODELAY, true);
                           } catch (ChannelException var5x) {
                           }

                           ChannelPipeline l_var2 = l_param0.pipeline()
                              .addLast("timeout", new ReadTimeoutHandler(30))
                              .addLast("legacy_query", new LegacyQueryHandler(ServerConnectionListener.this));
                           Connection.configureSerialization(l_var2, PacketFlow.SERVERBOUND);
                           int l_var3 = ServerConnectionListener.this.server.getRateLimitPacketsPerSecond();
                           Connection l_var4 = l_var3 > 0 ? new RateKickingConnection(l_var3) : new Connection(PacketFlow.SERVERBOUND);
                           ServerConnectionListener.this.connections.add(l_var4);
                           l_var2.addLast("packet_handler", l_var4);
                           l_var4.setListener(new ServerHandshakePacketListenerImpl(ServerConnectionListener.this.server, l_var4));
                        }
                     }
                  )
                  .group(TcpEventGroup.INSTANCE)
                  .localAddress(param0, param1)
                  .bind()
                  .syncUninterruptibly()
            );
      }
   }

   public Connection connectLocalClient() {
      Connection[] pair = Connection.createLocalPair();
      pair[1].setListener(new MemoryServerHandshakePacketListenerImpl(this.server, pair[1]));
      this.connections.add(pair[1]);
      return pair[0];
   }

   public void stop() {
      this.running = false;
      for (Connection connection : this.connections) {
         connection.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
      }

      for (ChannelFuture channel : this.channels) {
         channel.channel().close();
      }
   }

   public void tick() {
      synchronized (this.connections) {
         Iterator<Connection> var2 = this.connections.iterator();

         while (var2.hasNext()) {
            Connection var3 = var2.next();
            if (!var3.isConnecting()) {
               if (var3.isConnected()) {
                  try {
                     var3.tick();
                  } catch (Exception var4) {
                     if (var3.isMemoryConnection()) {
                        throw new ReportedException(CrashReport.forThrowable(var4, "Ticking memory connection"));
                     }

                     LOGGER.warn("Failed to handle packet for {}", var3.getRemoteAddress(), var4);
                     Component var5 = Component.literal("Internal server error");
                     var3.send(new ClientboundDisconnectPacket(var5), PacketSendListener.thenRun(() -> var3.disconnect(var5)));
                     var3.setReadOnly();
                  }
               } else {
                  var2.remove();
                  var3.handleDisconnection();
               }
            }
         }
      }
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   public List<Connection> getConnections() {
      return this.connections;
   }

   static class LatencySimulator extends ChannelInboundHandlerAdapter {
      private static final Timer TIMER = new HashedWheelTimer();
      private final int delay;
      private final int jitter;
      private final List<ServerConnectionListener.LatencySimulator.DelayedMessage> queuedMessages = Lists.newArrayList();

      public LatencySimulator(int param0, int param1) {
         this.delay = param0;
         this.jitter = param1;
      }

      @Override
      public void channelRead(ChannelHandlerContext param0, Object param1) {
         this.delayDownstream(param0, param1);
      }

      private void delayDownstream(ChannelHandlerContext param0, Object param1) {
         int var3 = this.delay + (int)(Math.random() * this.jitter);
         this.queuedMessages.add(new ServerConnectionListener.LatencySimulator.DelayedMessage(param0, param1));
         TIMER.newTimeout(this::onTimeout, var3, TimeUnit.MILLISECONDS);
      }

      private void onTimeout(Timeout param0) {
         ServerConnectionListener.LatencySimulator.DelayedMessage var2 = this.queuedMessages.remove(0);
         var2.ctx.fireChannelRead(var2.msg);
      }

      static class DelayedMessage {
         public final ChannelHandlerContext ctx;
         public final Object msg;

         public DelayedMessage(ChannelHandlerContext param0, Object param1) {
            this.ctx = param0;
            this.msg = param1;
         }
      }
   }
}
