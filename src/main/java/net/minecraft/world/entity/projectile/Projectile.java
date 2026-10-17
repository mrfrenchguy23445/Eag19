package net.minecraft.world.entity.projectile;

import com.google.common.base.MoreObjects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class Projectile extends Entity implements TraceableEntity {
   @Nullable
   private UUID ownerUUID;
   @Nullable
   private Entity cachedOwner;
   private boolean leftOwner;
   private boolean hasBeenShot;

   Projectile(EntityType<? extends Projectile> param0, Level param1) {
      super(param0, param1);
   }

   public void setOwner(@Nullable Entity param0) {
      if (param0 != null) {
         this.ownerUUID = param0.getUUID();
         this.cachedOwner = param0;
      }
   }

   @Nullable
   @Override
   public Entity getOwner() {
      if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
         return this.cachedOwner;
      } else if (this.ownerUUID != null && this.level instanceof ServerLevel) {
         this.cachedOwner = ((ServerLevel)this.level).getEntity(this.ownerUUID);
         return this.cachedOwner;
      } else {
         return null;
      }
   }

   public Entity getEffectSource() {
      return MoreObjects.firstNonNull(this.getOwner(), this);
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag param0) {
      if (this.ownerUUID != null) {
         param0.putUUID("Owner", this.ownerUUID);
      }

      if (this.leftOwner) {
         param0.putBoolean("LeftOwner", true);
      }

      param0.putBoolean("HasBeenShot", this.hasBeenShot);
   }

   protected boolean ownedBy(Entity param0) {
      return param0.getUUID().equals(this.ownerUUID);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag param0) {
      if (param0.hasUUID("Owner")) {
         this.ownerUUID = param0.getUUID("Owner");
      }

      this.leftOwner = param0.getBoolean("LeftOwner");
      this.hasBeenShot = param0.getBoolean("HasBeenShot");
   }

   @Override
   public void tick() {
      if (!this.hasBeenShot) {
         this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner());
         this.hasBeenShot = true;
      }

      if (!this.leftOwner) {
         this.leftOwner = this.checkLeftOwner();
      }

      super.tick();
   }

   private boolean checkLeftOwner() {
      Entity var1 = this.getOwner();
      if (var1 != null) {
         for (Entity var3 : this.level
            .getEntities(
               this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), l_param0 -> !l_param0.isSpectator() && l_param0.isPickable()
            )) {
            if (var3.getRootVehicle() == var1.getRootVehicle()) {
               return false;
            }
         }
      }

      return true;
   }

   public void shoot(double param0, double param2, double param4, float param6, float param7) {
      Vec3 var9 = new Vec3(param0, param2, param4)
         .normalize()
         .add(this.random.triangle(0.0, 0.0172275 * param7), this.random.triangle(0.0, 0.0172275 * param7), this.random.triangle(0.0, 0.0172275 * param7))
         .scale(param6);
      this.setDeltaMovement(var9);
      double var10 = var9.horizontalDistance();
      this.setYRot((float)(Mth.atan2(var9.x, var9.z) * 180.0F / (float)Math.PI));
      this.setXRot((float)(Mth.atan2(var9.y, var10) * 180.0F / (float)Math.PI));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   public void shootFromRotation(Entity param0, float param1, float param2, float param3, float param4, float param5) {
      float var7 = -Mth.sin(param2 * (float) (Math.PI / 180.0)) * Mth.cos(param1 * (float) (Math.PI / 180.0));
      float var8 = -Mth.sin((param1 + param3) * (float) (Math.PI / 180.0));
      float var9 = Mth.cos(param2 * (float) (Math.PI / 180.0)) * Mth.cos(param1 * (float) (Math.PI / 180.0));
      this.shoot(var7, var8, var9, param4, param5);
      Vec3 var10 = param0.getDeltaMovement();
      this.setDeltaMovement(this.getDeltaMovement().add(var10.x, param0.isOnGround() ? 0.0 : var10.y, var10.z));
   }

   protected void onHit(HitResult param0) {
      HitResult.Type var2 = param0.getType();
      if (var2 == HitResult.Type.ENTITY) {
         this.onHitEntity((EntityHitResult)param0);
         this.level.gameEvent(GameEvent.PROJECTILE_LAND, param0.getLocation(), GameEvent.Context.of(this, null));
      } else if (var2 == HitResult.Type.BLOCK) {
         BlockHitResult var3 = (BlockHitResult)param0;
         this.onHitBlock(var3);
         BlockPos var4 = var3.getBlockPos();
         this.level.gameEvent(GameEvent.PROJECTILE_LAND, var4, GameEvent.Context.of(this, this.level.getBlockState(var4)));
      }
   }

   protected void onHitEntity(EntityHitResult param0) {
   }

   protected void onHitBlock(BlockHitResult param0) {
      BlockState var2 = this.level.getBlockState(param0.getBlockPos());
      var2.onProjectileHit(this.level, var2, param0, this);
   }

   @Override
   public void lerpMotion(double param0, double param2, double param4) {
      this.setDeltaMovement(param0, param2, param4);
      if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
         double var7 = Math.sqrt(param0 * param0 + param4 * param4);
         this.setXRot((float)(Mth.atan2(param2, var7) * 180.0F / (float)Math.PI));
         this.setYRot((float)(Mth.atan2(param0, param4) * 180.0F / (float)Math.PI));
         this.xRotO = this.getXRot();
         this.yRotO = this.getYRot();
         this.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
      }
   }

   protected boolean canHitEntity(Entity param0) {
      if (!param0.canBeHitByProjectile()) {
         return false;
      }

      Entity var2 = this.getOwner();
      return var2 == null || this.leftOwner || !var2.isPassengerOfSameVehicle(param0);
   }

   protected void updateRotation() {
      Vec3 var1 = this.getDeltaMovement();
      double var2 = var1.horizontalDistance();
      this.setXRot(lerpRotation(this.xRotO, (float)(Mth.atan2(var1.y, var2) * 180.0F / (float)Math.PI)));
      this.setYRot(lerpRotation(this.yRotO, (float)(Mth.atan2(var1.x, var1.z) * 180.0F / (float)Math.PI)));
   }

   protected static float lerpRotation(float param0, float param1) {
      while (param1 - param0 < -180.0F) {
         param0 -= 360.0F;
      }

      while (param1 - param0 >= 180.0F) {
         param0 += 360.0F;
      }

      return Mth.lerp(0.2F, param0, param1);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      Entity var1 = this.getOwner();
      return new ClientboundAddEntityPacket(this, var1 == null ? 0 : var1.getId());
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket param0) {
      super.recreateFromPacket(param0);
      Entity var2 = this.level.getEntity(param0.getData());
      if (var2 != null) {
         this.setOwner(var2);
      }
   }

   @Override
   public boolean mayInteract(Level param0, BlockPos param1) {
      Entity var3 = this.getOwner();
      return var3 instanceof Player ? var3.mayInteract(param0, param1) : var3 == null || param0.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
   }
}
