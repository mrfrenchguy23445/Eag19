package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetTimePacket implements Packet<ClientGamePacketListener> {
   private final long gameTime;
   private final long dayTime;

   public ClientboundSetTimePacket(long param0, long param2, boolean param4) {
      this.gameTime = param0;
      long var6 = param2;
      if (!param4) {
         var6 = -var6;
         if (var6 == 0L) {
            var6 = -1L;
         }
      }

      this.dayTime = var6;
   }

   public ClientboundSetTimePacket(FriendlyByteBuf param0) {
      this.gameTime = param0.readLong();
      this.dayTime = param0.readLong();
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeLong(this.gameTime);
      param0.writeLong(this.dayTime);
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleSetTime(this);
   }

   public long getGameTime() {
      return this.gameTime;
   }

   public long getDayTime() {
      return this.dayTime;
   }
}
