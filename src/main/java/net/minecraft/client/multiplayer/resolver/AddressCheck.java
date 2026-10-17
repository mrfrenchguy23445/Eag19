package net.minecraft.client.multiplayer.resolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mojang.blocklist.BlockListSupplier;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Predicate;

public interface AddressCheck {
   boolean isAllowed(ResolvedServerAddress var1);

   boolean isAllowed(ServerAddress var1);

   static AddressCheck createFromService() {
      final ImmutableList<Predicate<String>> var0 = Streams.stream(ServiceLoader.load(BlockListSupplier.class))
         .map(BlockListSupplier::createBlockList)
         .filter(Objects::nonNull)
         .collect(ImmutableList.toImmutableList());
      return new AddressCheck() {
         @Override
         public boolean isAllowed(ResolvedServerAddress l_param0) {
            String l_var2 = l_param0.getHostName();
            String l_var3 = l_param0.getHostIp();
            return var0.stream().noneMatch(l2_param2 -> l2_param2.test(l_var2) || l2_param2.test(l_var3));
         }

         @Override
         public boolean isAllowed(ServerAddress l_param0) {
            String l_var2 = l_param0.getHost();
            return var0.stream().noneMatch(l2_param1 -> l2_param1.test(l_var2));
         }
      };
   }
}
