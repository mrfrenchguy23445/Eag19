package net.minecraft.client.multiplayer.resolver;

import com.mojang.logging.LogUtils;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.slf4j.Logger;

@FunctionalInterface
public interface ServerAddressResolver {
   Logger LOGGER = LogUtils.getLogger();
   ServerAddressResolver SYSTEM = param0 -> {
      try {
         InetAddress var1 = InetAddress.getByName(param0.getHost());
         return Optional.of(ResolvedServerAddress.from(new InetSocketAddress(var1, param0.getPort())));
      } catch (UnknownHostException var1) {
         LOGGER.debug("Couldn't resolve server {} address", param0.getHost(), var1);
         return Optional.empty();
      }
   };

   Optional<ResolvedServerAddress> resolve(ServerAddress var1);
}
