package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ClientboundUpdateAttributesPacket implements Packet<ClientGamePacketListener> {
   private final int entityId;
   private final List<ClientboundUpdateAttributesPacket.AttributeSnapshot> attributes;

   public ClientboundUpdateAttributesPacket(int param0, Collection<AttributeInstance> param1) {
      this.entityId = param0;
      this.attributes = Lists.newArrayList();

      for (AttributeInstance var4 : param1) {
         this.attributes.add(new ClientboundUpdateAttributesPacket.AttributeSnapshot(var4.getAttribute(), var4.getBaseValue(), var4.getModifiers()));
      }
   }

   public ClientboundUpdateAttributesPacket(FriendlyByteBuf param0) {
      this.entityId = param0.readVarInt();
      this.attributes = param0.readList(
         l_param0 -> {
            ResourceLocation l_var1 = l_param0.readResourceLocation();
            Attribute l_var2 = BuiltInRegistries.ATTRIBUTE.get(l_var1);
            double l_var3 = l_param0.readDouble();
            List<AttributeModifier> l_var5 = l_param0.readList(
               l2_param0 -> new AttributeModifier(
                  l2_param0.readUUID(),
                  "Unknown synced attribute modifier",
                  l2_param0.readDouble(),
                  AttributeModifier.Operation.fromValue(l2_param0.readByte())
               )
            );
            return new ClientboundUpdateAttributesPacket.AttributeSnapshot(l_var2, l_var3, l_var5);
         }
      );
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeVarInt(this.entityId);
      param0.writeCollection(this.attributes, (l_param0, l_param1) -> {
         l_param0.writeResourceLocation(BuiltInRegistries.ATTRIBUTE.getKey(l_param1.getAttribute()));
         l_param0.writeDouble(l_param1.getBase());
         l_param0.writeCollection(l_param1.getModifiers(), (l2_param0, l2_param1) -> {
            l2_param0.writeUUID(l2_param1.getId());
            l2_param0.writeDouble(l2_param1.getAmount());
            l2_param0.writeByte(l2_param1.getOperation().toValue());
         });
      });
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleUpdateAttributes(this);
   }

   public int getEntityId() {
      return this.entityId;
   }

   public List<ClientboundUpdateAttributesPacket.AttributeSnapshot> getValues() {
      return this.attributes;
   }

   public static class AttributeSnapshot {
      private final Attribute attribute;
      private final double base;
      private final Collection<AttributeModifier> modifiers;

      public AttributeSnapshot(Attribute param0, double param1, Collection<AttributeModifier> param3) {
         this.attribute = param0;
         this.base = param1;
         this.modifiers = param3;
      }

      public Attribute getAttribute() {
         return this.attribute;
      }

      public double getBase() {
         return this.base;
      }

      public Collection<AttributeModifier> getModifiers() {
         return this.modifiers;
      }
   }
}
