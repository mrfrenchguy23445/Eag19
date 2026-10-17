package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public class ClientboundSetPassengersPacket implements Packet<ClientGamePacketListener> {
   private final int vehicle;
   private final int[] passengers;

   public ClientboundSetPassengersPacket(Entity param0) {
      this.vehicle = param0.getId();
      List<Entity> var2 = param0.getPassengers();
      this.passengers = new int[var2.size()];

      for (int var3 = 0; var3 < var2.size(); var3++) {
         this.passengers[var3] = var2.get(var3).getId();
      }
   }

   public ClientboundSetPassengersPacket(FriendlyByteBuf param0) {
      this.vehicle = param0.readVarInt();
      this.passengers = param0.readVarIntArray();
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeVarInt(this.vehicle);
      param0.writeVarIntArray(this.passengers);
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleSetEntityPassengersPacket(this);
   }

   public int[] getPassengers() {
      return this.passengers;
   }

   public int getVehicle() {
      return this.vehicle;
   }
}
