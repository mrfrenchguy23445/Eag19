package net.minecraft.network;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import net.lax1dude.eaglercraft.v1_8.internal.EnumEaglerConnectionState;
import net.lax1dude.eaglercraft.v1_8.internal.IWebSocketClient;
import net.lax1dude.eaglercraft.v1_8.internal.IWebSocketFrame;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformNetworking;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMarkerFactory;

public class Connection implements ChannelHandler {
   private static final float AVERAGE_PACKETS_SMOOTHING = 0.75F;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final BasicMarkerFactory MARKERS = new BasicMarkerFactory();
   public static final Marker ROOT_MARKER = MARKERS.getMarker("NETWORK");
   public static final Marker PACKET_MARKER = Util.make(MARKERS.getMarker("NETWORK_PACKETS"), param0 -> param0.add(ROOT_MARKER));
   public static final Marker PACKET_RECEIVED_MARKER = Util.make(MARKERS.getMarker("PACKET_RECEIVED"), param0 -> param0.add(PACKET_MARKER));
   public static final Marker PACKET_SENT_MARKER = Util.make(MARKERS.getMarker("PACKET_SENT"), param0 -> param0.add(PACKET_MARKER));
   public static final AttributeKey<ConnectionProtocol> ATTRIBUTE_PROTOCOL = AttributeKey.valueOf("protocol");
   private static final int MAX_PACKET_SIZE = 8388608;
   private final PacketFlow receiving;
   private final Queue<Connection.PacketHolder> queue = Queues.newConcurrentLinkedQueue();
   @Nullable
   private IWebSocketClient webSocket;
   @Nullable
   private SocketAddress address;
   @Nullable
   private PacketListener packetListener;
   @Nullable
   private Component disconnectedReason;
   @Nullable
   private ConnectionProtocol protocol;
   @Nullable
   private Cipher decryptCipher;
   @Nullable
   private Cipher encryptCipher;
   private boolean encrypted;
   private boolean disconnectionHandled;
   private boolean readOnly;
   private int compressionThreshold = -1;
   private int receivedPackets;
   private int sentPackets;
   private float averageReceivedPackets;
   private float averageSentPackets;
   private int tickCount;
   private boolean handlingFault;
   private Connection localPeer;
   private boolean localClosed;
   private final Queue<Packet<?>> localPackets = new java.util.ArrayDeque<>();

   /** Queued delivery avoids reentrant login and gameplay handlers. */
   public static Connection[] createLocalPair() {
      Connection client = new Connection(PacketFlow.CLIENTBOUND);
      Connection server = new Connection(PacketFlow.SERVERBOUND);
      client.address = server.address = new LocalSocketAddress();
      client.localPeer = server;
      server.localPeer = client;
      client.setProtocol(ConnectionProtocol.HANDSHAKING);
      server.setProtocol(ConnectionProtocol.HANDSHAKING);
      return new Connection[] { client, server };
   }

   public Connection(PacketFlow param0) {
      this.receiving = param0;
   }

   @Override
   public void handlerAdded(ChannelHandlerContext param0) throws Exception {
   }

   @Override
   public void handlerRemoved(ChannelHandlerContext param0) throws Exception {
   }

   @Override
   @Deprecated
   public void exceptionCaught(ChannelHandlerContext param0, Throwable param1) throws Exception {
   }

   public static Connection connectToServer(String webSocketURI, boolean useEpoll) {
      return connectToServer(openWebSocket(webSocketURI));
   }

   public static IWebSocketClient openWebSocket(String webSocketURI) {
      IWebSocketClient client = PlatformNetworking.openWebSocket(webSocketURI);
      if (client == null) {
         throw new IllegalStateException("Could not open WebSocket to \"" + webSocketURI + "\"");
      }

      return client;
   }

   public static Connection connectToServer(IWebSocketClient client) {
      if (client == null) {
         throw new IllegalStateException("Could not open WebSocket");
      }

      Connection connection = new Connection(PacketFlow.CLIENTBOUND);
      connection.webSocket = client;
      connection.setProtocol(ConnectionProtocol.HANDSHAKING);
      return connection;
   }

   public static String normalizeWebSocketURI(String rawAddress) {
      String trimmed = rawAddress.trim();
      if (trimmed.regionMatches(true, 0, "ws://", 0, 5) || trimmed.regionMatches(true, 0, "wss://", 0, 6)) {
         return trimmed;
      }

      return "wss://" + trimmed;
   }

   public static String getHostFromURI(String webSocketURI) {
      String var1 = authority(webSocketURI);
      int var2 = var1.lastIndexOf(58);
      if (var2 < 0) {
         return var1;
      }

      String var3 = var1.substring(var2 + 1);
      return isAllDigits(var3) ? var1.substring(0, var2) : var1;
   }

   public static int getPortFromURI(String webSocketURI) {
      String var1 = authority(webSocketURI);
      int var2 = var1.lastIndexOf(58);
      if (var2 >= 0) {
         String var3 = var1.substring(var2 + 1);
         if (isAllDigits(var3)) {
            try {
               return Integer.parseInt(var3);
            } catch (NumberFormatException var5) {
            }
         }
      }

      return webSocketURI.regionMatches(true, 0, "wss://", 0, 6) ? 443 : 80;
   }

   private static String authority(String param0) {
      int var1 = param0.indexOf("://");
      String var2 = var1 < 0 ? param0 : param0.substring(var1 + 3);
      int var3 = var2.length();

      for (int var4 = 0; var4 < var2.length(); var4++) {
         char var5 = var2.charAt(var4);
         if (var5 == '/' || var5 == '?' || var5 == '#') {
            var3 = var4;
            break;
         }
      }

      return var2.substring(0, var3);
   }

   private static boolean isAllDigits(String param0) {
      if (param0.isEmpty()) {
         return false;
      }

      for (int var1 = 0; var1 < param0.length(); var1++) {
         if (param0.charAt(var1) < '0' || param0.charAt(var1) > '9') {
            return false;
         }
      }

      return true;
   }

   public static Connection connectToServer(InetSocketAddress param0, boolean param1) {
      throw new UnsupportedOperationException("Not supported on this platform; use connectToServer(String, boolean)");
   }

   public static Connection connectToLocalServer(SocketAddress param0) {
      throw new UnsupportedOperationException("Not supported on this platform");
   }

   public static void configureSerialization(ChannelPipeline param0, PacketFlow param1) {
   }

   public void setProtocol(ConnectionProtocol param0) {
      this.protocol = param0;
   }

   @Nullable
   private ConnectionProtocol getCurrentProtocol() {
      return this.protocol;
   }

   public void setListener(PacketListener param0) {
      Validate.notNull(param0, "packetListener");
      this.packetListener = param0;
   }

   public void send(Packet<?> param0) {
      this.send(param0, null);
   }

   public void send(Packet<?> param0, @Nullable PacketSendListener param1) {
      if (this.canSend()) {
         this.flushQueue();
         this.doSendPacket(param0, param1);
      } else {
         this.queue.add(new Connection.PacketHolder(param0, param1));
      }
   }

   private void doSendPacket(Packet<?> param0, @Nullable PacketSendListener param1) {
      if (this.localPeer != null) {
         this.localPeer.localPackets.add(param0);
         this.sentPackets++;
         if (param1 != null) param1.onSuccess();
         return;
      }
      ConnectionProtocol var3 = ConnectionProtocol.getProtocolForPacket(param0);
      ConnectionProtocol var4 = this.getCurrentProtocol();
      this.sentPackets++;
      if (var4 != var3) {
         if (var3 == null) {
            throw new IllegalStateException("Encountered packet without set protocol: " + param0);
         }

         this.setProtocol(var3);
      }

      try {
         byte[] var5 = this.encode(param0, var3);
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(PACKET_SENT_MARKER, "OUT: [{}:{}] {}", var3, var3.getPacketId(this.getSending(), param0), param0.getClass().getName());
         }

         this.webSocket.send(var5);
         if (param1 != null) {
            param1.onSuccess();
         }
      } catch (Throwable var6) {
         LOGGER.error("Failed to send packet {}", param0, var6);
         if (param1 != null) {
            Packet<?> var7 = param1.onFailure();
            if (var7 != null) {
               try {
                  this.webSocket.send(this.encode(var7, ConnectionProtocol.getProtocolForPacket(var7)));
               } catch (Throwable var8) {
                  LOGGER.error("Failed to send fallback packet {}", var7, var8);
               }
            }
         }
      }
   }

   private void flushQueue() {
      if (this.canSend()) {
         synchronized (this.queue) {
            Connection.PacketHolder var2;
            while ((var2 = this.queue.poll()) != null) {
               this.doSendPacket(var2.packet, var2.listener);
            }
         }
      }
   }

   public void tick() {
      this.flushQueue();
      if (this.localPeer != null && !this.readOnly) {
         Packet<?> packet;
         while (this.isConnected() && (packet = this.localPackets.poll()) != null) {
            this.receivedPackets++;
            try {
               genericsFtw(packet, this.packetListener);
            } catch (RunningOnDifferentThreadException ignored) {
            }
         }
      }
      this.processReceivedFrames();
      if (this.packetListener instanceof TickablePacketListener var1) {
         var1.tick();
      }

      if (!this.isConnected() && !this.disconnectionHandled) {
         this.handleDisconnection();
      }

      if (this.tickCount++ % 20 == 0) {
         this.tickSecond();
      }
   }

   private void processReceivedFrames() {
      if (this.webSocket == null || this.readOnly) {
         return;
      }

      if (this.webSocket.availableStringFrames() > 0) {
         this.webSocket.clearStringFrames();
      }

      if (this.webSocket.availableBinaryFrames() > 0) {
         List<IWebSocketFrame> var1 = this.webSocket.getNextBinaryFrames();
         if (var1 != null) {
            for (int var2 = 0; var2 < var1.size(); var2++) {
               byte[] var3 = var1.get(var2).getByteArray();
               if (var3 != null) {
                  try {
                     this.decode(var3);
                  } catch (SkipPacketException var4) {
                     LOGGER.debug("Skipping packet due to errors", var4.getCause());
                  } catch (RunningOnDifferentThreadException var4x) {
                  } catch (RejectedExecutionException var4xx) {
                     this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
                  } catch (Throwable var4xxx) {
                     this.handleFault(var4xxx);
                  }
               }
            }
         }
      }
   }

   protected void tickSecond() {
      this.averageSentPackets = Mth.lerp(AVERAGE_PACKETS_SMOOTHING, this.sentPackets, this.averageSentPackets);
      this.averageReceivedPackets = Mth.lerp(AVERAGE_PACKETS_SMOOTHING, this.receivedPackets, this.averageReceivedPackets);
      this.sentPackets = 0;
      this.receivedPackets = 0;
   }

   public SocketAddress getRemoteAddress() {
      return this.address;
   }

   public void disconnect(Component param0) {
      if (this.localPeer != null && !this.localClosed) {
         this.localClosed = true;
         this.disconnectedReason = param0;
         this.localPeer.localClosed = true;
         this.localPeer.disconnectedReason = param0;
         this.queue.clear();
         this.localPackets.clear();
      }
      if (this.webSocket != null && !this.webSocket.isClosed()) {
         this.webSocket.close();
         this.disconnectedReason = param0;
      }
   }

   public boolean isMemoryConnection() {
      return this.localPeer != null;
   }

   public PacketFlow getReceiving() {
      return this.receiving;
   }

   public PacketFlow getSending() {
      return this.receiving.getOpposite();
   }

   public void setEncryptionKey(Cipher param0, Cipher param1) {
      this.decryptCipher = param0;
      this.encryptCipher = param1;
      this.encrypted = true;
   }

   public boolean isEncrypted() {
      return this.encrypted;
   }

   public boolean isConnected() {
      return this.localPeer != null ? !this.localClosed : this.webSocket != null && !this.webSocket.isClosed();
   }

   public boolean isConnecting() {
      return this.webSocket != null && this.webSocket.getState() == EnumEaglerConnectionState.CONNECTING;
   }

   public PacketListener getPacketListener() {
      return this.packetListener;
   }

   @Nullable
   public Component getDisconnectedReason() {
      return this.disconnectedReason;
   }

   public void setReadOnly() {
      this.readOnly = true;
   }

   public void setupCompression(int param0, boolean param1) {
      this.compressionThreshold = param0;
   }

   public void handleDisconnection() {
      if ((this.localPeer != null && this.localClosed) || (this.webSocket != null && this.webSocket.isClosed())) {
         if (this.disconnectionHandled) {
            LOGGER.warn("handleDisconnection() called twice");
         } else {
            this.disconnectionHandled = true;
            if (this.getDisconnectedReason() != null) {
               this.getPacketListener().onDisconnect(this.getDisconnectedReason());
            } else if (this.getPacketListener() != null) {
               this.getPacketListener().onDisconnect(Component.translatable("multiplayer.disconnect.generic"));
            }
         }
      }
   }

   public float getAverageReceivedPackets() {
      return this.averageReceivedPackets;
   }

   public float getAverageSentPackets() {
      return this.averageSentPackets;
   }

   private boolean canSend() {
      return this.localPeer != null ? !this.localClosed : this.webSocket != null && this.webSocket.isOpen();
   }

   private byte[] encode(Packet<?> param0, @Nullable ConnectionProtocol param1) {
      if (param1 == null) {
         throw new IllegalStateException("Encountered packet without set protocol: " + param0);
      }

      int var2 = param1.getPacketId(this.getSending(), param0);
      if (var2 == -1) {
         throw new IllegalStateException("Can't serialize unregistered packet: " + param0.getClass().getName());
      }

      FriendlyByteBuf var3 = new FriendlyByteBuf(Unpooled.buffer());
      var3.writeVarInt(var2);
      int var4 = var3.writerIndex();
      param0.write(var3);
      int var5 = var3.writerIndex() - var4;
      if (var5 > MAX_PACKET_SIZE) {
         throw new IllegalArgumentException("Packet too big (is " + var5 + ", should be less than " + MAX_PACKET_SIZE + "): " + param0);
      }

      byte[] var6 = new byte[var3.readableBytes()];
      var3.readBytes(var6);
      if (this.compressionThreshold >= 0) {
         var6 = compress(var6);
      }

      if (this.encryptCipher != null) {
         var6 = applyCipher(this.encryptCipher, var6);
      }

      return var6;
   }

   private void decode(byte[] param0) {
      if (this.decryptCipher != null) {
         param0 = applyCipher(this.decryptCipher, param0);
      }

      if (this.compressionThreshold >= 0) {
         param0 = decompress(param0);
      }

      FriendlyByteBuf var1 = new FriendlyByteBuf(Unpooled.wrappedBuffer(param0));
      ConnectionProtocol var2 = this.getCurrentProtocol();
      if (var2 == null) {
         throw new IllegalStateException("Received packet before a protocol was set");
      }

      int var3 = var1.readVarInt();
      Packet<?> var4 = var2.createPacket(this.receiving, var3, var1);
      if (var4 == null) {
         throw new IllegalStateException("Bad packet id " + var3 + " in state " + var2);
      }

      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug(PACKET_RECEIVED_MARKER, "IN: [{}:{}] {}", var2, var3, var4.getClass().getName());
      }

      genericsFtw(var4, this.packetListener);
      this.receivedPackets++;
   }

   private static <T extends PacketListener> void genericsFtw(Packet<T> param0, PacketListener param1) {
      param0.handle((T)param1);
   }

   private void handleFault(Throwable param0) {
      boolean var1 = !this.handlingFault;
      this.handlingFault = true;
      if (this.isConnected()) {
         Component var2 = Component.translatable("disconnect.genericReason", "Internal Exception: " + param0);
         if (var1) {
            StringBuilder var5 = new StringBuilder("Connection fault while handling inbound data, disconnecting: ");
            var5.append(param0);
            try {
               StackTraceElement[] var6 = param0.getStackTrace();
               for (int var7 = 0; var7 < var6.length && var7 < 32; ++var7) {
                  var5.append("\n  at ").append(var6[var7].toString());
               }
            } catch (Throwable var8) {
            }
            LOGGER.error(var5.toString(), param0);
            ConnectionProtocol var3 = this.getCurrentProtocol();
            Packet<?> var4 = var3 == ConnectionProtocol.LOGIN ? new ClientboundLoginDisconnectPacket(var2) : new ClientboundDisconnectPacket(var2);
            this.send(var4, PacketSendListener.thenRun(() -> this.disconnect(var2)));
            this.setReadOnly();
         } else {
            LOGGER.error("Double fault, disconnecting", param0);
            this.disconnect(var2);
         }
      }
   }

   private byte[] compress(byte[] param0) {
      if (param0.length < this.compressionThreshold) {
         FriendlyByteBuf var1 = new FriendlyByteBuf(Unpooled.buffer());
         var1.writeVarInt(0);
         var1.writeBytes(param0);
         return toByteArray(var1);
      }

      Deflater var6 = new Deflater();
      byte[] var7 = new byte[8192];
      FriendlyByteBuf var8 = new FriendlyByteBuf(Unpooled.buffer());
      var8.writeVarInt(param0.length);
      var6.setInput(param0, 0, param0.length);
      var6.finish();

      while (!var6.finished()) {
         int var9 = var6.deflate(var7);
         var8.writeBytes(var7, 0, var9);
      }

      var6.end();
      return toByteArray(var8);
   }

   private byte[] decompress(byte[] param0) {
      FriendlyByteBuf var1 = new FriendlyByteBuf(Unpooled.wrappedBuffer(param0));
      int var2 = var1.readVarInt();
      if (var2 == 0) {
         byte[] var9 = new byte[var1.readableBytes()];
         var1.readBytes(var9);
         return var9;
      }

      byte[] var3 = new byte[var1.readableBytes()];
      var1.readBytes(var3);
      byte[] var4 = new byte[var2];
      Inflater var5 = new Inflater();

      try {
         var5.setInput(var3);
         var5.inflate(var4);
      } catch (DataFormatException var8) {
         throw new IllegalStateException("Badly compressed packet", var8);
      } finally {
         var5.end();
      }

      return var4;
   }

   private static byte[] applyCipher(Cipher param0, byte[] param1) {
      try {
         byte[] var2 = new byte[param1.length];
         param0.update(param1, 0, param1.length, var2);
         return var2;
      } catch (Exception var3) {
         throw new IllegalStateException("Cipher error", var3);
      }
   }

   private static byte[] toByteArray(ByteBuf param0) {
      byte[] var1 = new byte[param0.readableBytes()];
      param0.readBytes(var1);
      return var1;
   }

   private static final class LocalSocketAddress extends SocketAddress {
      @Override public String toString() { return "local"; }
   }

   static class PacketHolder {
      final Packet<?> packet;
      @Nullable
      final PacketSendListener listener;

      public PacketHolder(Packet<?> param0, @Nullable PacketSendListener param1) {
         this.packet = param0;
         this.listener = param1;
      }
   }
}
