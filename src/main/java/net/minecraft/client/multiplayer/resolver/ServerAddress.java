package net.minecraft.client.multiplayer.resolver;

import com.google.common.net.HostAndPort;
import com.mojang.logging.LogUtils;
import java.net.IDN;
import org.slf4j.Logger;

public final class ServerAddress {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final HostAndPort hostAndPort;
   private static final ServerAddress INVALID = new ServerAddress(HostAndPort.fromParts("server.invalid", 25565));

   public ServerAddress(String param0, int param1) {
      this(HostAndPort.fromParts(param0, param1));
   }

   private ServerAddress(HostAndPort param0) {
      this.hostAndPort = param0;
   }

   public String getHost() {
      try {
         return IDN.toASCII(this.hostAndPort.getHost());
      } catch (IllegalArgumentException var1) {
         return "";
      }
   }

   public int getPort() {
      return this.hostAndPort.getPort();
   }

   public static ServerAddress parseString(String param0) {
      if (param0 == null) {
         return INVALID;
      }

      try {
         HostAndPort var1 = HostAndPort.fromString(param0).withDefaultPort(25565);
         return var1.getHost().isEmpty() ? INVALID : new ServerAddress(var1);
      } catch (IllegalArgumentException var1) {
         LOGGER.info("Failed to parse URL {}", param0, var1);
         return INVALID;
      }
   }

   public static boolean isValidAddress(String param0) {
      try {
         HostAndPort var1 = HostAndPort.fromString(param0);
         String var2 = var1.getHost();
         if (!var2.isEmpty()) {
            IDN.toASCII(var2);
            return true;
         }
      } catch (IllegalArgumentException var3) {
      }

      return false;
   }

   static int parsePort(String param0) {
      try {
         return Integer.parseInt(param0.trim());
      } catch (Exception var2) {
         return 25565;
      }
   }

   @Override
   public String toString() {
      return this.hostAndPort.toString();
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return param0 instanceof ServerAddress ? this.hostAndPort.equals(((ServerAddress)param0).hostAndPort) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.hostAndPort.hashCode();
   }
}
