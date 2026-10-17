package net.minecraft.client.multiplayer.resolver;

import com.mojang.logging.LogUtils;
import java.util.Hashtable;
import java.util.Optional;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.slf4j.Logger;

@FunctionalInterface
public interface ServerRedirectHandler {
   Logger LOGGER = LogUtils.getLogger();
   ServerRedirectHandler EMPTY = param0 -> Optional.empty();

   Optional<ServerAddress> lookupRedirect(ServerAddress var1);

   static ServerRedirectHandler createDnsSrvRedirectHandler() {
      DirContext var0;
      try {
         String var1 = "com.sun.jndi.dns.DnsContextFactory";
         Class.forName("com.sun.jndi.dns.DnsContextFactory");
         Hashtable<String, String> var2 = new Hashtable<>();
         var2.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
         var2.put("java.naming.provider.url", "dns:");
         var2.put("com.sun.jndi.dns.timeout.retries", "1");
         var0 = new InitialDirContext(var2);
      } catch (Throwable var1) {
         LOGGER.error("Failed to initialize SRV redirect resolved, some servers might not work", var1);
         return EMPTY;
      }

      return l_param1 -> {
         if (l_param1.getPort() == 25565) {
            try {
               Attributes l_var2 = var0.getAttributes("_minecraft._tcp." + l_param1.getHost(), new String[]{"SRV"});
               Attribute l_var3 = l_var2.get("srv");
               if (l_var3 != null) {
                  String[] l_var4 = l_var3.get().toString().split(" ", 4);
                  return Optional.of(new ServerAddress(l_var4[3], ServerAddress.parsePort(l_var4[2])));
               }
            } catch (Throwable var5) {
            }
         }

         return Optional.empty();
      };
   }
}
