package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ClientboundExplodePacket implements Packet<ClientGamePacketListener> {
   private final double x;
   private final double y;
   private final double z;
   private final float power;
   private final List<BlockPos> toBlow;
   private final float knockbackX;
   private final float knockbackY;
   private final float knockbackZ;

   public ClientboundExplodePacket(double param0, double param2, double param4, float param6, List<BlockPos> param7, @Nullable Vec3 param8) {
      this.x = param0;
      this.y = param2;
      this.z = param4;
      this.power = param6;
      this.toBlow = Lists.newArrayList(param7);
      if (param8 != null) {
         this.knockbackX = (float)param8.x;
         this.knockbackY = (float)param8.y;
         this.knockbackZ = (float)param8.z;
      } else {
         this.knockbackX = 0.0F;
         this.knockbackY = 0.0F;
         this.knockbackZ = 0.0F;
      }
   }

   public ClientboundExplodePacket(FriendlyByteBuf param0) {
      this.x = param0.readDouble();
      this.y = param0.readDouble();
      this.z = param0.readDouble();
      this.power = param0.readFloat();
      int var2 = Mth.floor(this.x);
      int var3 = Mth.floor(this.y);
      int var4 = Mth.floor(this.z);
      this.toBlow = param0.readList(l_param3 -> {
         int l_var4 = l_param3.readByte() + var2;
         int l_var5 = l_param3.readByte() + var3;
         int l_var6 = l_param3.readByte() + var4;
         return new BlockPos(l_var4, l_var5, l_var6);
      });
      this.knockbackX = param0.readFloat();
      this.knockbackY = param0.readFloat();
      this.knockbackZ = param0.readFloat();
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeDouble(this.x);
      param0.writeDouble(this.y);
      param0.writeDouble(this.z);
      param0.writeFloat(this.power);
      int var2 = Mth.floor(this.x);
      int var3 = Mth.floor(this.y);
      int var4 = Mth.floor(this.z);
      param0.writeCollection(this.toBlow, (l_param3, l_param4) -> {
         int l_var5 = l_param4.getX() - var2;
         int l_var6 = l_param4.getY() - var3;
         int l_var7 = l_param4.getZ() - var4;
         l_param3.writeByte(l_var5);
         l_param3.writeByte(l_var6);
         l_param3.writeByte(l_var7);
      });
      param0.writeFloat(this.knockbackX);
      param0.writeFloat(this.knockbackY);
      param0.writeFloat(this.knockbackZ);
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleExplosion(this);
   }

   public float getKnockbackX() {
      return this.knockbackX;
   }

   public float getKnockbackY() {
      return this.knockbackY;
   }

   public float getKnockbackZ() {
      return this.knockbackZ;
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

   public float getPower() {
      return this.power;
   }

   public List<BlockPos> getToBlow() {
      return this.toBlow;
   }
}
