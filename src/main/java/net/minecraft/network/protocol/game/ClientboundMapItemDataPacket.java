package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class ClientboundMapItemDataPacket implements Packet<ClientGamePacketListener> {
   private final int mapId;
   private final byte scale;
   private final boolean locked;
   @Nullable
   private final List<MapDecoration> decorations;
   @Nullable
   private final MapItemSavedData.MapPatch colorPatch;

   public ClientboundMapItemDataPacket(
      int param0, byte param1, boolean param2, @Nullable Collection<MapDecoration> param3, @Nullable MapItemSavedData.MapPatch param4
   ) {
      this.mapId = param0;
      this.scale = param1;
      this.locked = param2;
      this.decorations = param3 != null ? Lists.newArrayList(param3) : null;
      this.colorPatch = param4;
   }

   public ClientboundMapItemDataPacket(FriendlyByteBuf param0) {
      this.mapId = param0.readVarInt();
      this.scale = param0.readByte();
      this.locked = param0.readBoolean();
      this.decorations = param0.readNullable(l_param0 -> l_param0.readList(l2_param0 -> {
         MapDecoration.Type l2_var1 = l2_param0.readEnum(MapDecoration.Type.class);
         byte l2_var2 = l2_param0.readByte();
         byte l2_var3 = l2_param0.readByte();
         byte l2_var4 = (byte)(l2_param0.readByte() & 15);
         Component l2_var5 = l2_param0.readNullable(FriendlyByteBuf::readComponent);
         return new MapDecoration(l2_var1, l2_var2, l2_var3, l2_var4, l2_var5);
      }));
      int var2 = param0.readUnsignedByte();
      if (var2 > 0) {
         int var3 = param0.readUnsignedByte();
         int var4 = param0.readUnsignedByte();
         int var5 = param0.readUnsignedByte();
         byte[] var6 = param0.readByteArray();
         this.colorPatch = new MapItemSavedData.MapPatch(var4, var5, var2, var3, var6);
      } else {
         this.colorPatch = null;
      }
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeVarInt(this.mapId);
      param0.writeByte(this.scale);
      param0.writeBoolean(this.locked);
      param0.writeNullable(this.decorations, (l_param0, l_param1) -> l_param0.writeCollection(l_param1, (l2_param0, l2_param1) -> {
         l2_param0.writeEnum(l2_param1.getType());
         l2_param0.writeByte(l2_param1.getX());
         l2_param0.writeByte(l2_param1.getY());
         l2_param0.writeByte(l2_param1.getRot() & 15);
         l2_param0.writeNullable(l2_param1.getName(), FriendlyByteBuf::writeComponent);
      }));
      if (this.colorPatch != null) {
         param0.writeByte(this.colorPatch.width);
         param0.writeByte(this.colorPatch.height);
         param0.writeByte(this.colorPatch.startX);
         param0.writeByte(this.colorPatch.startY);
         param0.writeByteArray(this.colorPatch.mapColors);
      } else {
         param0.writeByte(0);
      }
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleMapItemData(this);
   }

   public int getMapId() {
      return this.mapId;
   }

   public void applyToMap(MapItemSavedData param0) {
      if (this.decorations != null) {
         param0.addClientSideDecorations(this.decorations);
      }

      if (this.colorPatch != null) {
         this.colorPatch.applyToMap(param0);
      }
   }

   public byte getScale() {
      return this.scale;
   }

   public boolean isLocked() {
      return this.locked;
   }
}
