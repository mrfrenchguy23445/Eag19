package net.minecraft.network.protocol;

import io.netty.util.AttributeKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.PacketListener;

public interface BundlerInfo {
   AttributeKey<BundlerInfo.Provider> BUNDLER_PROVIDER = AttributeKey.valueOf("bundler");
   int BUNDLE_SIZE_LIMIT = 4096;
   BundlerInfo EMPTY = new BundlerInfo() {
      @Override
      public void unbundlePacket(Packet<?> param0, Consumer<Packet<?>> param1) {
         param1.accept(param0);
      }

      @Nullable
      @Override
      public BundlerInfo.Bundler startPacketBundling(Packet<?> param0) {
         return null;
      }
   };

   static <T extends PacketListener, P extends BundlePacket<T>> BundlerInfo createForPacket(
      final Class<P> param0, final Function<Iterable<Packet<T>>, P> param1, final BundleDelimiterPacket<T> param2
   ) {
      return new BundlerInfo() {
         @Override
         public void unbundlePacket(Packet<?> l_param0, Consumer<Packet<?>> l_param1) {
            if (l_param0.getClass() == param0) {
               P l_var3 = (P)l_param0;
               l_param1.accept(param2);
               l_var3.subPackets().forEach(l_param1);
               l_param1.accept(param2);
            } else {
               l_param1.accept(l_param0);
            }
         }

         @Nullable
         @Override
         public BundlerInfo.Bundler startPacketBundling(Packet<?> l_param0) {
            return l_param0 == param2 ? new BundlerInfo.Bundler() {
               private final List<Packet<T>> bundlePackets = new ArrayList<>();

               @Nullable
               @Override
               public Packet<?> addPacket(Packet<?> l2_param0) {
                  if (l2_param0 == param2) {
                     return param1.apply(this.bundlePackets);
                  }

                  Packet<T> l2_var2 = (Packet<T>)l2_param0;
                  if (this.bundlePackets.size() >= 4096) {
                     throw new IllegalStateException("Too many packets in a bundle");
                  }

                  this.bundlePackets.add(l2_var2);
                  return null;
               }
            } : null;
         }
      };
   }

   void unbundlePacket(Packet<?> var1, Consumer<Packet<?>> var2);

   @Nullable
   BundlerInfo.Bundler startPacketBundling(Packet<?> var1);

   interface Bundler {
      @Nullable
      Packet<?> addPacket(Packet<?> var1);
   }

   interface Provider {
      BundlerInfo getBundlerInfo(PacketFlow var1);
   }
}
