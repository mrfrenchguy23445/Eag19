package net.minecraft.network.protocol.game;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundLevelParticlesPacket implements Packet<ClientGamePacketListener> {
   private final double x;
   private final double y;
   private final double z;
   private final float xDist;
   private final float yDist;
   private final float zDist;
   private final float maxSpeed;
   private final int count;
   private final boolean overrideLimiter;
   private final ParticleOptions particle;

   public <T extends ParticleOptions> ClientboundLevelParticlesPacket(
      T param0, boolean param1, double param2, double param4, double param6, float param8, float param9, float param10, float param11, int param12
   ) {
      this.particle = param0;
      this.overrideLimiter = param1;
      this.x = param2;
      this.y = param4;
      this.z = param6;
      this.xDist = param8;
      this.yDist = param9;
      this.zDist = param10;
      this.maxSpeed = param11;
      this.count = param12;
   }

   public ClientboundLevelParticlesPacket(FriendlyByteBuf param0) {
      ParticleType<?> var2 = param0.readById(BuiltInRegistries.PARTICLE_TYPE);
      this.overrideLimiter = param0.readBoolean();
      this.x = param0.readDouble();
      this.y = param0.readDouble();
      this.z = param0.readDouble();
      this.xDist = param0.readFloat();
      this.yDist = param0.readFloat();
      this.zDist = param0.readFloat();
      this.maxSpeed = param0.readFloat();
      this.count = param0.readInt();
      this.particle = this.readParticle(param0, (ParticleType<ParticleOptions>)var2);
   }

   private <T extends ParticleOptions> T readParticle(FriendlyByteBuf param0, ParticleType<T> param1) {
      return param1.getDeserializer().fromNetwork(param1, param0);
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeId(BuiltInRegistries.PARTICLE_TYPE, this.particle.getType());
      param0.writeBoolean(this.overrideLimiter);
      param0.writeDouble(this.x);
      param0.writeDouble(this.y);
      param0.writeDouble(this.z);
      param0.writeFloat(this.xDist);
      param0.writeFloat(this.yDist);
      param0.writeFloat(this.zDist);
      param0.writeFloat(this.maxSpeed);
      param0.writeInt(this.count);
      this.particle.writeToNetwork(param0);
   }

   public boolean isOverrideLimiter() {
      return this.overrideLimiter;
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

   public float getXDist() {
      return this.xDist;
   }

   public float getYDist() {
      return this.yDist;
   }

   public float getZDist() {
      return this.zDist;
   }

   public float getMaxSpeed() {
      return this.maxSpeed;
   }

   public int getCount() {
      return this.count;
   }

   public ParticleOptions getParticle() {
      return this.particle;
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleParticleEvent(this);
   }
}
