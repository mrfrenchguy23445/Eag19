package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ServerEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int TOLERANCE_LEVEL_ROTATION = 1;
   private final ServerLevel level;
   private final Entity entity;
   private final int updateInterval;
   private final boolean trackDelta;
   private final Consumer<Packet<?>> broadcast;
   private final VecDeltaCodec positionCodec = new VecDeltaCodec();
   private int yRotp;
   private int xRotp;
   private int yHeadRotp;
   private Vec3 ap = Vec3.ZERO;
   private int tickCount;
   private int teleportDelay;
   private List<Entity> lastPassengers = Collections.emptyList();
   private boolean wasRiding;
   private boolean wasOnGround;
   @Nullable
   private List<SynchedEntityData.DataValue<?>> trackedDataValues;

   public ServerEntity(ServerLevel param0, Entity param1, int param2, boolean param3, Consumer<Packet<?>> param4) {
      this.level = param0;
      this.broadcast = param4;
      this.entity = param1;
      this.updateInterval = param2;
      this.trackDelta = param3;
      this.positionCodec.setBase(param1.trackingPosition());
      this.yRotp = Mth.floor(param1.getYRot() * 256.0F / 360.0F);
      this.xRotp = Mth.floor(param1.getXRot() * 256.0F / 360.0F);
      this.yHeadRotp = Mth.floor(param1.getYHeadRot() * 256.0F / 360.0F);
      this.wasOnGround = param1.isOnGround();
      this.trackedDataValues = param1.getEntityData().getNonDefaultValues();
   }

   public void sendChanges() {
      List<Entity> var1 = this.entity.getPassengers();
      if (!var1.equals(this.lastPassengers)) {
         this.broadcast.accept(new ClientboundSetPassengersPacket(this.entity));
         this.changedPassengers(var1, this.lastPassengers).forEach(l_param1 -> {
            if (l_param1 instanceof ServerPlayer l_var2 && !var1.contains(l_var2)) {
               l_var2.connection.teleport(l_var2.getX(), l_var2.getY(), l_var2.getZ(), l_var2.getYRot(), l_var2.getXRot());
            }
         });
         this.lastPassengers = var1;
      }

      if (this.entity instanceof ItemFrame var2 && this.tickCount % 10 == 0) {
         ItemStack var3 = var2.getItem();
         if (var3.getItem() instanceof MapItem) {
            Integer var4 = MapItem.getMapId(var3);
            MapItemSavedData var5 = MapItem.getSavedData(var4, this.level);
            if (var5 != null) {
               for (ServerPlayer var7 : this.level.players()) {
                  var5.tickCarriedBy(var7, var3);
                  Packet<?> var8 = var5.getUpdatePacket(var4, var7);
                  if (var8 != null) {
                     var7.connection.send(var8);
                  }
               }
            }
         }

         this.sendDirtyEntityData();
      }

      if (this.tickCount % this.updateInterval == 0 || this.entity.hasImpulse || this.entity.getEntityData().isDirty()) {
         if (this.entity.isPassenger()) {
            int var2 = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
            int var3 = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
            boolean var4 = Math.abs(var2 - this.yRotp) >= 1 || Math.abs(var3 - this.xRotp) >= 1;
            if (var4) {
               this.broadcast.accept(new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte)var2, (byte)var3, this.entity.isOnGround()));
               this.yRotp = var2;
               this.xRotp = var3;
            }

            this.positionCodec.setBase(this.entity.trackingPosition());
            this.sendDirtyEntityData();
            this.wasRiding = true;
         } else {
            this.teleportDelay++;
            int var2 = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
            int var3 = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
            Vec3 var4 = this.entity.trackingPosition();
            boolean var5 = this.positionCodec.delta(var4).lengthSqr() >= 7.6293945E-6F;
            Packet<?> var6 = null;
            boolean var7 = var5 || this.tickCount % 60 == 0;
            boolean var8 = Math.abs(var2 - this.yRotp) >= 1 || Math.abs(var3 - this.xRotp) >= 1;
            boolean var9 = false;
            boolean var10 = false;
            if (this.tickCount > 0 || this.entity instanceof AbstractArrow) {
               long var11 = this.positionCodec.encodeX(var4);
               long var13 = this.positionCodec.encodeY(var4);
               long var15 = this.positionCodec.encodeZ(var4);
               boolean var17 = var11 < -32768L || var11 > 32767L || var13 < -32768L || var13 > 32767L || var15 < -32768L || var15 > 32767L;
               if (var17 || this.teleportDelay > 400 || this.wasRiding || this.wasOnGround != this.entity.isOnGround()) {
                  this.wasOnGround = this.entity.isOnGround();
                  this.teleportDelay = 0;
                  var6 = new ClientboundTeleportEntityPacket(this.entity);
                  var9 = true;
                  var10 = true;
               } else if ((!var7 || !var8) && !(this.entity instanceof AbstractArrow)) {
                  if (var7) {
                     var6 = new ClientboundMoveEntityPacket.Pos(this.entity.getId(), (short)var11, (short)var13, (short)var15, this.entity.isOnGround());
                     var9 = true;
                  } else if (var8) {
                     var6 = new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte)var2, (byte)var3, this.entity.isOnGround());
                     var10 = true;
                  }
               } else {
                  var6 = new ClientboundMoveEntityPacket.PosRot(
                     this.entity.getId(), (short)var11, (short)var13, (short)var15, (byte)var2, (byte)var3, this.entity.isOnGround()
                  );
                  var9 = true;
                  var10 = true;
               }
            }

            if ((this.trackDelta || this.entity.hasImpulse || this.entity instanceof LivingEntity && ((LivingEntity)this.entity).isFallFlying())
               && this.tickCount > 0) {
               Vec3 var11 = this.entity.getDeltaMovement();
               double var12 = var11.distanceToSqr(this.ap);
               if (var12 > 1.0E-7 || var12 > 0.0 && var11.lengthSqr() == 0.0) {
                  this.ap = var11;
                  this.broadcast.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.ap));
               }
            }

            if (var6 != null) {
               this.broadcast.accept(var6);
            }

            this.sendDirtyEntityData();
            if (var9) {
               this.positionCodec.setBase(var4);
            }

            if (var10) {
               this.yRotp = var2;
               this.xRotp = var3;
            }

            this.wasRiding = false;
         }

         int var2 = Mth.floor(this.entity.getYHeadRot() * 256.0F / 360.0F);
         if (Math.abs(var2 - this.yHeadRotp) >= 1) {
            this.broadcast.accept(new ClientboundRotateHeadPacket(this.entity, (byte)var2));
            this.yHeadRotp = var2;
         }

         this.entity.hasImpulse = false;
      }

      this.tickCount++;
      if (this.entity.hurtMarked) {
         this.broadcastAndSend(new ClientboundSetEntityMotionPacket(this.entity));
         this.entity.hurtMarked = false;
      }
   }

   private Stream<Entity> changedPassengers(List<Entity> param0, List<Entity> param1) {
      return Stream.concat(param1.stream().filter(l_param1 -> !param0.contains(l_param1)), param0.stream().filter(l_param1 -> !param1.contains(l_param1)));
   }

   public void removePairing(ServerPlayer param0) {
      this.entity.stopSeenByPlayer(param0);
      param0.connection.send(new ClientboundRemoveEntitiesPacket(this.entity.getId()));
   }

   public void addPairing(ServerPlayer param0) {
      List<Packet<ClientGamePacketListener>> var2 = new ArrayList<>();
      this.sendPairingData(var2::add);
      param0.connection.send(new ClientboundBundlePacket(var2));
      this.entity.startSeenByPlayer(param0);
   }

   public void sendPairingData(Consumer<Packet<ClientGamePacketListener>> param0) {
      if (this.entity.isRemoved()) {
         LOGGER.warn("Fetching packet for removed entity {}", this.entity);
      }

      Packet<ClientGamePacketListener> var2 = this.entity.getAddEntityPacket();
      this.yHeadRotp = Mth.floor(this.entity.getYHeadRot() * 256.0F / 360.0F);
      param0.accept(var2);
      if (this.trackedDataValues != null) {
         param0.accept(new ClientboundSetEntityDataPacket(this.entity.getId(), this.trackedDataValues));
      }

      boolean var3 = this.trackDelta;
      if (this.entity instanceof LivingEntity) {
         Collection<AttributeInstance> var4 = ((LivingEntity)this.entity).getAttributes().getSyncableAttributes();
         if (!var4.isEmpty()) {
            param0.accept(new ClientboundUpdateAttributesPacket(this.entity.getId(), var4));
         }

         if (((LivingEntity)this.entity).isFallFlying()) {
            var3 = true;
         }
      }

      this.ap = this.entity.getDeltaMovement();
      if (var3 && !(this.entity instanceof LivingEntity)) {
         param0.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.ap));
      }

      if (this.entity instanceof LivingEntity) {
         List<Pair<EquipmentSlot, ItemStack>> var4 = Lists.newArrayList();

         for (EquipmentSlot var8 : EquipmentSlot.values()) {
            ItemStack var9 = ((LivingEntity)this.entity).getItemBySlot(var8);
            if (!var9.isEmpty()) {
               var4.add(Pair.of(var8, var9.copy()));
            }
         }

         if (!var4.isEmpty()) {
            param0.accept(new ClientboundSetEquipmentPacket(this.entity.getId(), var4));
         }
      }

      if (this.entity instanceof LivingEntity var4) {
         for (MobEffectInstance var6 : var4.getActiveEffects()) {
            param0.accept(new ClientboundUpdateMobEffectPacket(this.entity.getId(), var6));
         }
      }

      if (!this.entity.getPassengers().isEmpty()) {
         param0.accept(new ClientboundSetPassengersPacket(this.entity));
      }

      if (this.entity.isPassenger()) {
         param0.accept(new ClientboundSetPassengersPacket(this.entity.getVehicle()));
      }

      if (this.entity instanceof Mob var4 && var4.isLeashed()) {
         param0.accept(new ClientboundSetEntityLinkPacket(var4, var4.getLeashHolder()));
      }
   }

   private void sendDirtyEntityData() {
      SynchedEntityData var1 = this.entity.getEntityData();
      List<SynchedEntityData.DataValue<?>> var2 = var1.packDirty();
      if (var2 != null) {
         this.trackedDataValues = var1.getNonDefaultValues();
         this.broadcastAndSend(new ClientboundSetEntityDataPacket(this.entity.getId(), var2));
      }

      if (this.entity instanceof LivingEntity) {
         Set<AttributeInstance> var3 = ((LivingEntity)this.entity).getAttributes().getDirtyAttributes();
         if (!var3.isEmpty()) {
            this.broadcastAndSend(new ClientboundUpdateAttributesPacket(this.entity.getId(), var3));
         }

         var3.clear();
      }
   }

   private void broadcastAndSend(Packet<?> param0) {
      this.broadcast.accept(param0);
      if (this.entity instanceof ServerPlayer) {
         ((ServerPlayer)this.entity).connection.send(param0);
      }
   }
}
