package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddEntityPacket implements Packet<ClientGamePacketListener> {
   private static final double MAGICAL_QUANTIZATION = 8000.0;
   private static final double LIMIT = 3.9;
   private final int id;
   private final UUID uuid;
   private final EntityType<?> type;
   private final double x;
   private final double y;
   private final double z;
   private final int xa;
   private final int ya;
   private final int za;
   private final byte xRot;
   private final byte yRot;
   private final byte yHeadRot;
   private final int data;

   public ClientboundAddEntityPacket(Entity param0) {
      this(param0, 0);
   }

   public ClientboundAddEntityPacket(Entity param0, int param1) {
      this(
         param0.getId(),
         param0.getUUID(),
         param0.getX(),
         param0.getY(),
         param0.getZ(),
         param0.getXRot(),
         param0.getYRot(),
         param0.getType(),
         param1,
         param0.getDeltaMovement(),
         param0.getYHeadRot()
      );
   }

   public ClientboundAddEntityPacket(Entity param0, int param1, BlockPos param2) {
      this(
         param0.getId(),
         param0.getUUID(),
         param2.getX(),
         param2.getY(),
         param2.getZ(),
         param0.getXRot(),
         param0.getYRot(),
         param0.getType(),
         param1,
         param0.getDeltaMovement(),
         param0.getYHeadRot()
      );
   }

   public ClientboundAddEntityPacket(
      int param0,
      UUID param1,
      double param2,
      double param4,
      double param6,
      float param8,
      float param9,
      EntityType<?> param10,
      int param11,
      Vec3 param12,
      double param13
   ) {
      this.id = param0;
      this.uuid = param1;
      this.x = param2;
      this.y = param4;
      this.z = param6;
      this.xRot = (byte)Mth.floor(param8 * 256.0F / 360.0F);
      this.yRot = (byte)Mth.floor(param9 * 256.0F / 360.0F);
      this.yHeadRot = (byte)Mth.floor(param13 * 256.0 / 360.0);
      this.type = param10;
      this.data = param11;
      this.xa = (int)(Mth.clamp(param12.x, -3.9, 3.9) * 8000.0);
      this.ya = (int)(Mth.clamp(param12.y, -3.9, 3.9) * 8000.0);
      this.za = (int)(Mth.clamp(param12.z, -3.9, 3.9) * 8000.0);
   }

   public ClientboundAddEntityPacket(FriendlyByteBuf param0) {
      this.id = param0.readVarInt();
      this.uuid = param0.readUUID();
      this.type = param0.readById(BuiltInRegistries.ENTITY_TYPE);
      this.x = param0.readDouble();
      this.y = param0.readDouble();
      this.z = param0.readDouble();
      this.xRot = param0.readByte();
      this.yRot = param0.readByte();
      this.yHeadRot = param0.readByte();
      this.data = param0.readVarInt();
      this.xa = param0.readShort();
      this.ya = param0.readShort();
      this.za = param0.readShort();
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeVarInt(this.id);
      param0.writeUUID(this.uuid);
      param0.writeId(BuiltInRegistries.ENTITY_TYPE, this.type);
      param0.writeDouble(this.x);
      param0.writeDouble(this.y);
      param0.writeDouble(this.z);
      param0.writeByte(this.xRot);
      param0.writeByte(this.yRot);
      param0.writeByte(this.yHeadRot);
      param0.writeVarInt(this.data);
      param0.writeShort(this.xa);
      param0.writeShort(this.ya);
      param0.writeShort(this.za);
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleAddEntity(this);
   }

   public int getId() {
      return this.id;
   }

   public UUID getUUID() {
      return this.uuid;
   }

   public EntityType<?> getType() {
      return this.type;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public double getXa() {
      return this.xa / 8000.0;
   }

   public double getYa() {
      return this.ya / 8000.0;
   }

   public double getZa() {
      return this.za / 8000.0;
   }

   public float getXRot() {
      return this.xRot * 360 / 256.0F;
   }

   public float getYRot() {
      return this.yRot * 360 / 256.0F;
   }

   public float getYHeadRot() {
      return this.yHeadRot * 360 / 256.0F;
   }

   public int getData() {
      return this.data;
   }
}
