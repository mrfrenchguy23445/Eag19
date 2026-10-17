package net.minecraft.network.protocol.game;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundUpdateAdvancementsPacket implements Packet<ClientGamePacketListener> {
   private final boolean reset;
   private final Map<ResourceLocation, Advancement.Builder> added;
   private final Set<ResourceLocation> removed;
   private final Map<ResourceLocation, AdvancementProgress> progress;

   public ClientboundUpdateAdvancementsPacket(
      boolean param0, Collection<Advancement> param1, Set<ResourceLocation> param2, Map<ResourceLocation, AdvancementProgress> param3
   ) {
      this.reset = param0;
      Builder<ResourceLocation, Advancement.Builder> var5 = ImmutableMap.builder();

      for (Advancement var7 : param1) {
         var5.put(var7.getId(), var7.deconstruct());
      }

      this.added = var5.build();
      this.removed = ImmutableSet.copyOf(param2);
      this.progress = ImmutableMap.copyOf(param3);
   }

   public ClientboundUpdateAdvancementsPacket(FriendlyByteBuf param0) {
      this.reset = param0.readBoolean();
      this.added = param0.readMap(FriendlyByteBuf::readResourceLocation, Advancement.Builder::fromNetwork);
      this.removed = param0.readCollection(Sets::newLinkedHashSetWithExpectedSize, FriendlyByteBuf::readResourceLocation);
      this.progress = param0.readMap(FriendlyByteBuf::readResourceLocation, AdvancementProgress::fromNetwork);
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeBoolean(this.reset);
      param0.writeMap(this.added, FriendlyByteBuf::writeResourceLocation, (l_param0, l_param1) -> l_param1.serializeToNetwork(l_param0));
      param0.writeCollection(this.removed, FriendlyByteBuf::writeResourceLocation);
      param0.writeMap(this.progress, FriendlyByteBuf::writeResourceLocation, (l_param0, l_param1) -> l_param1.serializeToNetwork(l_param0));
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleUpdateAdvancementsPacket(this);
   }

   public Map<ResourceLocation, Advancement.Builder> getAdded() {
      return this.added;
   }

   public Set<ResourceLocation> getRemoved() {
      return this.removed;
   }

   public Map<ResourceLocation, AdvancementProgress> getProgress() {
      return this.progress;
   }

   public boolean shouldReset() {
      return this.reset;
   }
}
