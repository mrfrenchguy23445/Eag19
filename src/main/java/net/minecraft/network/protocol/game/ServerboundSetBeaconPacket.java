package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffect;

public class ServerboundSetBeaconPacket implements Packet<ServerGamePacketListener> {
   private final Optional<MobEffect> primary;
   private final Optional<MobEffect> secondary;

   public ServerboundSetBeaconPacket(Optional<MobEffect> param0, Optional<MobEffect> param1) {
      this.primary = param0;
      this.secondary = param1;
   }

   public ServerboundSetBeaconPacket(FriendlyByteBuf param0) {
      this.primary = param0.readOptional(l_param0 -> l_param0.readById(BuiltInRegistries.MOB_EFFECT));
      this.secondary = param0.readOptional(l_param0 -> l_param0.readById(BuiltInRegistries.MOB_EFFECT));
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeOptional(this.primary, (l_param0, l_param1) -> l_param0.writeId(BuiltInRegistries.MOB_EFFECT, l_param1));
      param0.writeOptional(this.secondary, (l_param0, l_param1) -> l_param0.writeId(BuiltInRegistries.MOB_EFFECT, l_param1));
   }

   public void handle(ServerGamePacketListener param0) {
      param0.handleSetBeaconPacket(this);
   }

   public Optional<MobEffect> getPrimary() {
      return this.primary;
   }

   public Optional<MobEffect> getSecondary() {
      return this.secondary;
   }
}
