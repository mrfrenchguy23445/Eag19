package net.minecraft.client.server;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.DefaultUncaughtExceptionHandler;
import org.slf4j.Logger;

public class LanServerDetection {
   static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
   static final Logger LOGGER = LogUtils.getLogger();

   /**
    * Port note (no multicast): the socket is gone, this never discovers anything.
    *
    * <p>Upstream listened on an IP multicast group. It joined {@code 224.0.2.60} on a
    * {@link MulticastSocket} bound to 4445, and read the 1024-byte datagrams that a
    * {@code LanServerPinger} on the same LAN advertises into a {@link DatagramPacket}, repeatedly,
    * until interrupted. The classlib has no {@code java.net} -- {@code MulticastSocket},
    * {@code DatagramPacket} and {@code InetAddress} are all absent -- and the absence is not the
    * problem, the network is: a browser cannot join a multicast group, cannot send or receive UDP,
    * and cannot see the local network at all.
    *
    * <p>The class stays because its result is not just the socket. {@code JoinMultiplayerScreen}
    * holds a {@link LanServerList} and renders a "Local LAN Servers" section from it, and holds
    * this object only to interrupt it on teardown. Both survive: the list stays empty and the
    * section renders empty, which is the same thing the screen shows today when the detector
    * fails to start -- {@code JoinMultiplayerScreen.init} already wraps construction and start in
    * a try/catch for exactly that.
    *
    * <p>The constructor no longer throws {@link IOException}, having nothing left that can. The
    * caller does not need to change, because it caught {@code Exception} and its
    * {@code lanServerDetector} field was already annotated {@code @Nullable}.
    *
    * <p>A local-network browser client cannot discover LAN worlds at all, and that is a platform
    * limit rather than a gap in this port. What would work instead is for the proxy to forward the
    * ping list, since the proxy is the part with a socket; nothing here is waiting on that, and
    * the socket code is not left behind pretending otherwise.
    */
   public static class LanServerDetector extends Thread {
      private final LanServerDetection.LanServerList serverList;

      public LanServerDetector(LanServerDetection.LanServerList param0) {
         super("LanServerDetector #" + LanServerDetection.UNIQUE_THREAD_ID.incrementAndGet());
         this.serverList = param0;
         this.setDaemon(true);
         this.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LanServerDetection.LOGGER));
      }

      @Override
      public void run() {
         // Nothing to listen on. See the port note above.
      }
   }

   public static class LanServerList {
      private final List<LanServer> servers = Lists.newArrayList();
      private boolean isDirty;

      @Nullable
      public synchronized List<LanServer> takeDirtyServers() {
         if (this.isDirty) {
            List<LanServer> var1 = List.copyOf(this.servers);
            this.isDirty = false;
            return var1;
         } else {
            return null;
         }
      }

      /**
       * Port note: the second parameter was an {@code InetAddress}.
       *
       * <p>The only thing it was read for was {@code getHostAddress()}, so it is that string now.
       * Nothing calls this any more -- {@link LanServerDetector} no longer receives a datagram to
       * call it with -- but keeping an {@code InetAddress} in a signature would keep
       * {@code java.net} in the linker's reach for a method that has no caller, and there is no
       * honest caller to give it: a browser has no source of a packet's source address.
       *
       * @param param0 the advertised MOTD and address, as sent by {@code LanServerPinger}
       * @param param1 the advertiser's IP address, as text
       */
      public synchronized void addServer(String param0, String param1) {
         String var3 = LanServerPinger.parseMotd(param0);
         String var4 = LanServerPinger.parseAddress(param0);
         if (var4 != null) {
            var4 = param1 + ":" + var4;
            boolean var5 = false;

            for (LanServer var7 : this.servers) {
               if (var7.getAddress().equals(var4)) {
                  var7.updatePingTime();
                  var5 = true;
                  break;
               }
            }

            if (!var5) {
               this.servers.add(new LanServer(var3, var4));
               this.isDirty = true;
            }
         }
      }
   }
}
