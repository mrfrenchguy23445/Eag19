package net.minecraft.client.multiplayer.resolver;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;

public class ServerNameResolver {
   public static final ServerNameResolver DEFAULT = new ServerNameResolver(
      ServerAddressResolver.SYSTEM, ServerRedirectHandler.createDnsSrvRedirectHandler(), AddressCheck.createFromService()
   );
   private final ServerAddressResolver resolver;
   private final ServerRedirectHandler redirectHandler;
   private final AddressCheck addressCheck;

   @VisibleForTesting
   ServerNameResolver(ServerAddressResolver param0, ServerRedirectHandler param1, AddressCheck param2) {
      this.resolver = param0;
      this.redirectHandler = param1;
      this.addressCheck = param2;
   }

   public Optional<ResolvedServerAddress> resolveAddress(ServerAddress param0) {
      Optional<ResolvedServerAddress> var2 = this.resolver.resolve(param0);
      if ((!var2.isPresent() || this.addressCheck.isAllowed(var2.get())) && this.addressCheck.isAllowed(param0)) {
         Optional<ServerAddress> var3 = this.redirectHandler.lookupRedirect(param0);
         if (var3.isPresent()) {
            var2 = this.resolver.resolve(var3.get()).filter(this.addressCheck::isAllowed);
         }

         return var2;
      } else {
         return Optional.empty();
      }
   }
}
