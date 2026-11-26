package net.minecraft.network.protocol.game;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization;

public class ClientboundUpdateTagsPacket implements Packet<ClientGamePacketListener> {
   private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags;

   public ClientboundUpdateTagsPacket(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> param0) {
      this.tags = param0;
   }

   public ClientboundUpdateTagsPacket(FriendlyByteBuf param0) {
      this.tags = param0.readMap(l_param0 -> ResourceKey.createRegistryKey(l_param0.readResourceLocation()), TagNetworkSerialization.NetworkPayload::read);
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeMap(this.tags, (l_param0, l_param1) -> l_param0.writeResourceLocation(l_param1.location()), (l_param0, l_param1) -> l_param1.write(l_param0));
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleUpdateTags(this);
   }

   public Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> getTags() {
      return this.tags;
   }
}
