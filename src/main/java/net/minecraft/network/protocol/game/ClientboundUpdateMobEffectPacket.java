package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class ClientboundUpdateMobEffectPacket implements Packet<ClientGamePacketListener> {
   private static final int FLAG_AMBIENT = 1;
   private static final int FLAG_VISIBLE = 2;
   private static final int FLAG_SHOW_ICON = 4;
   private final int entityId;
   private final MobEffect effect;
   private final byte effectAmplifier;
   private final int effectDurationTicks;
   private final byte flags;
   @Nullable
   private final MobEffectInstance.FactorData factorData;

   public ClientboundUpdateMobEffectPacket(int param0, MobEffectInstance param1) {
      this.entityId = param0;
      this.effect = param1.getEffect();
      this.effectAmplifier = (byte)(param1.getAmplifier() & 0xFF);
      this.effectDurationTicks = param1.getDuration();
      byte var3 = 0;
      if (param1.isAmbient()) {
         var3 = (byte)(var3 | 1);
      }

      if (param1.isVisible()) {
         var3 = (byte)(var3 | 2);
      }

      if (param1.showIcon()) {
         var3 = (byte)(var3 | 4);
      }

      this.flags = var3;
      this.factorData = param1.getFactorData().orElse(null);
   }

   public ClientboundUpdateMobEffectPacket(FriendlyByteBuf param0) {
      this.entityId = param0.readVarInt();
      this.effect = param0.readById(BuiltInRegistries.MOB_EFFECT);
      this.effectAmplifier = param0.readByte();
      this.effectDurationTicks = param0.readVarInt();
      this.flags = param0.readByte();
      this.factorData = param0.readNullable(l_param0 -> l_param0.readWithCodec(NbtOps.INSTANCE, MobEffectInstance.FactorData.CODEC));
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeVarInt(this.entityId);
      param0.writeId(BuiltInRegistries.MOB_EFFECT, this.effect);
      param0.writeByte(this.effectAmplifier);
      param0.writeVarInt(this.effectDurationTicks);
      param0.writeByte(this.flags);
      param0.writeNullable(this.factorData, (l_param0, l_param1) -> l_param0.writeWithCodec(NbtOps.INSTANCE, MobEffectInstance.FactorData.CODEC, l_param1));
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleUpdateMobEffect(this);
   }

   public int getEntityId() {
      return this.entityId;
   }

   public MobEffect getEffect() {
      return this.effect;
   }

   public byte getEffectAmplifier() {
      return this.effectAmplifier;
   }

   public int getEffectDurationTicks() {
      return this.effectDurationTicks;
   }

   public boolean isEffectVisible() {
      return (this.flags & 2) == 2;
   }

   public boolean isEffectAmbient() {
      return (this.flags & 1) == 1;
   }

   public boolean effectShowsIcon() {
      return (this.flags & 4) == 4;
   }

   @Nullable
   public MobEffectInstance.FactorData getFactorData() {
      return this.factorData;
   }
}
