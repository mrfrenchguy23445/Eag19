package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public class PacketBundlePacker extends MessageToMessageDecoder<Packet<?>> {
   @Nullable
   private BundlerInfo.Bundler currentBundler;
   @Nullable
   private BundlerInfo infoForCurrentBundler;
   private final PacketFlow flow;

   public PacketBundlePacker(PacketFlow param0) {
      this.flow = param0;
   }

   protected void decode(ChannelHandlerContext param0, Packet<?> param1, List<Object> param2) throws Exception {
      BundlerInfo.Provider var4 = param0.channel().attr(BundlerInfo.BUNDLER_PROVIDER).get();
      if (var4 == null) {
         throw new DecoderException("Bundler not configured: " + param1);
      }

      BundlerInfo var5 = var4.getBundlerInfo(this.flow);
      if (this.currentBundler != null) {
         if (this.infoForCurrentBundler != var5) {
            throw new DecoderException("Bundler handler changed during bundling");
         }

         Packet<?> var6 = this.currentBundler.addPacket(param1);
         if (var6 != null) {
            this.infoForCurrentBundler = null;
            this.currentBundler = null;
            param2.add(var6);
         }
      } else {
         BundlerInfo.Bundler var6 = var5.startPacketBundling(param1);
         if (var6 != null) {
            this.currentBundler = var6;
            this.infoForCurrentBundler = var5;
         } else {
            param2.add(param1);
         }
      }
   }
}
