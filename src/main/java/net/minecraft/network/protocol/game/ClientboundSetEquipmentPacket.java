package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ClientboundSetEquipmentPacket implements Packet<ClientGamePacketListener> {
   private static final byte CONTINUE_MASK = -128;
   private final int entity;
   private final List<Pair<EquipmentSlot, ItemStack>> slots;

   public ClientboundSetEquipmentPacket(int param0, List<Pair<EquipmentSlot, ItemStack>> param1) {
      this.entity = param0;
      this.slots = param1;
   }

   public ClientboundSetEquipmentPacket(FriendlyByteBuf param0) {
      this.entity = param0.readVarInt();
      EquipmentSlot[] var2 = EquipmentSlot.values();
      this.slots = Lists.newArrayList();

      int var3;
      do {
         var3 = param0.readByte();
         EquipmentSlot var4 = var2[var3 & 127];
         ItemStack var5 = param0.readItem();
         this.slots.add(Pair.of(var4, var5));
      } while ((var3 & -128) != 0);
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeVarInt(this.entity);
      int var2 = this.slots.size();

      for (int var3 = 0; var3 < var2; var3++) {
         Pair<EquipmentSlot, ItemStack> var4 = this.slots.get(var3);
         EquipmentSlot var5 = var4.getFirst();
         boolean var6 = var3 != var2 - 1;
         int var7 = var5.ordinal();
         param0.writeByte(var6 ? var7 | -128 : var7);
         param0.writeItem(var4.getSecond());
      }
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleSetEquipment(this);
   }

   public int getEntity() {
      return this.entity;
   }

   public List<Pair<EquipmentSlot, ItemStack>> getSlots() {
      return this.slots;
   }
}
