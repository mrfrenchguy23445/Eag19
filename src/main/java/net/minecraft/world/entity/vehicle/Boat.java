package net.minecraft.world.entity.vehicle;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Boat extends Entity implements VariantHolder<Boat.Type> {
   private static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ID_HURTDIR = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_ID_TYPE = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_LEFT = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_RIGHT = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_ID_BUBBLE_TIME = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
   public static final int PADDLE_LEFT = 0;
   public static final int PADDLE_RIGHT = 1;
   private static final int TIME_TO_EJECT = 60;
   private static final float PADDLE_SPEED = (float) (Math.PI / 8);
   public static final double PADDLE_SOUND_TIME = (float) (Math.PI / 4);
   public static final int BUBBLE_TIME = 60;
   private final float[] paddlePositions = new float[2];
   private float invFriction;
   private float outOfControlTicks;
   private float deltaRotation;
   private int lerpSteps;
   private double lerpX;
   private double lerpY;
   private double lerpZ;
   private double lerpYRot;
   private double lerpXRot;
   private boolean inputLeft;
   private boolean inputRight;
   private boolean inputUp;
   private boolean inputDown;
   private double waterLevel;
   private float landFriction;
   private Boat.Status status;
   private Boat.Status oldStatus;
   private double lastYd;
   private boolean isAboveBubbleColumn;
   private boolean bubbleColumnDirectionIsDown;
   private float bubbleMultiplier;
   private float bubbleAngle;
   private float bubbleAngleO;

   public Boat(EntityType<? extends Boat> param0, Level param1) {
      super(param0, param1);
      this.blocksBuilding = true;
   }

   public Boat(Level param0, double param1, double param3, double param5) {
      this(EntityType.BOAT, param0);
      this.setPos(param1, param3, param5);
      this.xo = param1;
      this.yo = param3;
      this.zo = param5;
   }

   @Override
   protected float getEyeHeight(Pose param0, EntityDimensions param1) {
      return param1.height;
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   protected void defineSynchedData() {
      this.entityData.define(DATA_ID_HURT, 0);
      this.entityData.define(DATA_ID_HURTDIR, 1);
      this.entityData.define(DATA_ID_DAMAGE, 0.0F);
      this.entityData.define(DATA_ID_TYPE, Boat.Type.OAK.ordinal());
      this.entityData.define(DATA_ID_PADDLE_LEFT, false);
      this.entityData.define(DATA_ID_PADDLE_RIGHT, false);
      this.entityData.define(DATA_ID_BUBBLE_TIME, 0);
   }

   @Override
   public boolean canCollideWith(Entity param0) {
      return canVehicleCollide(this, param0);
   }

   public static boolean canVehicleCollide(Entity param0, Entity param1) {
      return (param1.canBeCollidedWith() || param1.isPushable()) && !param0.isPassengerOfSameVehicle(param1);
   }

   @Override
   public boolean canBeCollidedWith() {
      return true;
   }

   @Override
   public boolean isPushable() {
      return true;
   }

   @Override
   protected Vec3 getRelativePortalPosition(Direction.Axis param0, BlockUtil.FoundRectangle param1) {
      return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(param0, param1));
   }

   @Override
   public double getPassengersRidingOffset() {
      return this.getVariant() == Boat.Type.BAMBOO ? 0.3 : -0.1;
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      if (!this.level.isClientSide && !this.isRemoved()) {
         this.setHurtDir(-this.getHurtDir());
         this.setHurtTime(10);
         this.setDamage(this.getDamage() + param1 * 10.0F);
         this.markHurt();
         this.gameEvent(GameEvent.ENTITY_DAMAGE, param0.getEntity());
         boolean var3 = param0.getEntity() instanceof Player && ((Player)param0.getEntity()).getAbilities().instabuild;
         if (var3 || this.getDamage() > 40.0F) {
            if (!var3 && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
               this.destroy(param0);
            }

            this.discard();
         }

         return true;
      } else {
         return true;
      }
   }

   protected void destroy(DamageSource param0) {
      this.spawnAtLocation(this.getDropItem());
   }

   @Override
   public void onAboveBubbleCol(boolean param0) {
      if (!this.level.isClientSide) {
         this.isAboveBubbleColumn = true;
         this.bubbleColumnDirectionIsDown = param0;
         if (this.getBubbleTime() == 0) {
            this.setBubbleTime(60);
         }
      }

      this.level
         .addParticle(ParticleTypes.SPLASH, this.getX() + this.random.nextFloat(), this.getY() + 0.7, this.getZ() + this.random.nextFloat(), 0.0, 0.0, 0.0);
      if (this.random.nextInt(20) == 0) {
         this.level
            .playLocalSound(
               this.getX(), this.getY(), this.getZ(), this.getSwimSplashSound(), this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat(), false
            );
         this.gameEvent(GameEvent.SPLASH, this.getControllingPassenger());
      }
   }

   @Override
   public void push(Entity param0) {
      if (param0 instanceof Boat) {
         if (param0.getBoundingBox().minY < this.getBoundingBox().maxY) {
            super.push(param0);
         }
      } else if (param0.getBoundingBox().minY <= this.getBoundingBox().minY) {
         super.push(param0);
      }
   }

   public Item getDropItem() {
      return switch (this.getVariant()) {
         case SPRUCE -> Items.SPRUCE_BOAT;
         case BIRCH -> Items.BIRCH_BOAT;
         case JUNGLE -> Items.JUNGLE_BOAT;
         case ACACIA -> Items.ACACIA_BOAT;
         case CHERRY -> Items.CHERRY_BOAT;
         case DARK_OAK -> Items.DARK_OAK_BOAT;
         case MANGROVE -> Items.MANGROVE_BOAT;
         case BAMBOO -> Items.BAMBOO_RAFT;
         default -> Items.OAK_BOAT;
      };
   }

   @Override
   public void animateHurt(float param0) {
      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.setDamage(this.getDamage() * 11.0F);
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   @Override
   public void lerpTo(double param0, double param2, double param4, float param6, float param7, int param8, boolean param9) {
      this.lerpX = param0;
      this.lerpY = param2;
      this.lerpZ = param4;
      this.lerpYRot = param6;
      this.lerpXRot = param7;
      this.lerpSteps = 10;
   }

   @Override
   public Direction getMotionDirection() {
      return this.getDirection().getClockWise();
   }

   @Override
   public void tick() {
      this.oldStatus = this.status;
      this.status = this.getStatus();
      if (this.status != Boat.Status.UNDER_WATER && this.status != Boat.Status.UNDER_FLOWING_WATER) {
         this.outOfControlTicks = 0.0F;
      } else {
         this.outOfControlTicks++;
      }

      if (!this.level.isClientSide && this.outOfControlTicks >= 60.0F) {
         this.ejectPassengers();
      }

      if (this.getHurtTime() > 0) {
         this.setHurtTime(this.getHurtTime() - 1);
      }

      if (this.getDamage() > 0.0F) {
         this.setDamage(this.getDamage() - 1.0F);
      }

      super.tick();
      this.tickLerp();
      if (this.isControlledByLocalInstance()) {
         if (!(this.getFirstPassenger() instanceof Player)) {
            this.setPaddleState(false, false);
         }

         this.floatBoat();
         if (this.level.isClientSide) {
            this.controlBoat();
            this.level.sendPacketToServer(new ServerboundPaddleBoatPacket(this.getPaddleState(0), this.getPaddleState(1)));
         }

         this.move(MoverType.SELF, this.getDeltaMovement());
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }

      this.tickBubbleColumn();

      for (int var1 = 0; var1 <= 1; var1++) {
         if (this.getPaddleState(var1)) {
            if (!this.isSilent()
               && this.paddlePositions[var1] % (float) (Math.PI * 2) <= (float) (Math.PI / 4)
               && (this.paddlePositions[var1] + (float) (Math.PI / 8)) % (float) (Math.PI * 2) >= (float) (Math.PI / 4)) {
               SoundEvent var2 = this.getPaddleSound();
               if (var2 != null) {
                  Vec3 var3 = this.getViewVector(1.0F);
                  double var4 = var1 == 1 ? -var3.z : var3.z;
                  double var6 = var1 == 1 ? var3.x : -var3.x;
                  this.level
                     .playSound(
                        null, this.getX() + var4, this.getY(), this.getZ() + var6, var2, this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat()
                     );
               }
            }

            this.paddlePositions[var1] = this.paddlePositions[var1] + (float) (Math.PI / 8);
         } else {
            this.paddlePositions[var1] = 0.0F;
         }
      }

      this.checkInsideBlocks();
      List<Entity> var1 = this.level.getEntities(this, this.getBoundingBox().inflate(0.2F, -0.01F, 0.2F), EntitySelector.pushableBy(this));
      if (!var1.isEmpty()) {
         boolean var2 = !this.level.isClientSide && !(this.getControllingPassenger() instanceof Player);

         for (int var3 = 0; var3 < var1.size(); var3++) {
            Entity var4 = var1.get(var3);
            if (!var4.hasPassenger(this)) {
               if (var2
                  && this.getPassengers().size() < this.getMaxPassengers()
                  && !var4.isPassenger()
                  && this.hasEnoughSpaceFor(var4)
                  && var4 instanceof LivingEntity
                  && !(var4 instanceof WaterAnimal)
                  && !(var4 instanceof Player)) {
                  var4.startRiding(this);
               } else {
                  this.push(var4);
               }
            }
         }
      }
   }

   private void tickBubbleColumn() {
      if (this.level.isClientSide) {
         int var1 = this.getBubbleTime();
         if (var1 > 0) {
            this.bubbleMultiplier += 0.05F;
         } else {
            this.bubbleMultiplier -= 0.1F;
         }

         this.bubbleMultiplier = Mth.clamp(this.bubbleMultiplier, 0.0F, 1.0F);
         this.bubbleAngleO = this.bubbleAngle;
         this.bubbleAngle = 10.0F * (float)Math.sin(0.5F * (float)this.level.getGameTime()) * this.bubbleMultiplier;
      } else {
         if (!this.isAboveBubbleColumn) {
            this.setBubbleTime(0);
         }

         int var1 = this.getBubbleTime();
         if (var1 > 0) {
            this.setBubbleTime(--var1);
            int var2 = 60 - var1 - 1;
            if (var2 > 0 && var1 == 0) {
               this.setBubbleTime(0);
               Vec3 var3 = this.getDeltaMovement();
               if (this.bubbleColumnDirectionIsDown) {
                  this.setDeltaMovement(var3.add(0.0, -0.7, 0.0));
                  this.ejectPassengers();
               } else {
                  this.setDeltaMovement(var3.x, this.hasPassenger(l_param0 -> l_param0 instanceof Player) ? 2.7 : 0.6, var3.z);
               }
            }

            this.isAboveBubbleColumn = false;
         }
      }
   }

   @Nullable
   protected SoundEvent getPaddleSound() {
      switch (this.getStatus()) {
         case IN_WATER:
         case UNDER_WATER:
         case UNDER_FLOWING_WATER:
            return SoundEvents.BOAT_PADDLE_WATER;
         case ON_LAND:
            return SoundEvents.BOAT_PADDLE_LAND;
         case IN_AIR:
         default:
            return null;
      }
   }

   private void tickLerp() {
      if (this.isControlledByLocalInstance()) {
         this.lerpSteps = 0;
         this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
      }

      if (this.lerpSteps > 0) {
         double var1 = this.getX() + (this.lerpX - this.getX()) / this.lerpSteps;
         double var3 = this.getY() + (this.lerpY - this.getY()) / this.lerpSteps;
         double var5 = this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps;
         double var7 = Mth.wrapDegrees(this.lerpYRot - this.getYRot());
         this.setYRot(this.getYRot() + (float)var7 / this.lerpSteps);
         this.setXRot(this.getXRot() + (float)(this.lerpXRot - this.getXRot()) / this.lerpSteps);
         this.lerpSteps--;
         this.setPos(var1, var3, var5);
         this.setRot(this.getYRot(), this.getXRot());
      }
   }

   public void setPaddleState(boolean param0, boolean param1) {
      this.entityData.set(DATA_ID_PADDLE_LEFT, param0);
      this.entityData.set(DATA_ID_PADDLE_RIGHT, param1);
   }

   public float getRowingTime(int param0, float param1) {
      return this.getPaddleState(param0) ? Mth.clampedLerp(this.paddlePositions[param0] - (float) (Math.PI / 8), this.paddlePositions[param0], param1) : 0.0F;
   }

   private Boat.Status getStatus() {
      Boat.Status var1 = this.isUnderwater();
      if (var1 != null) {
         this.waterLevel = this.getBoundingBox().maxY;
         return var1;
      } else if (this.checkInWater()) {
         return Boat.Status.IN_WATER;
      } else {
         float var2 = this.getGroundFriction();
         if (var2 > 0.0F) {
            this.landFriction = var2;
            return Boat.Status.ON_LAND;
         } else {
            return Boat.Status.IN_AIR;
         }
      }
   }

   public float getWaterLevelAbove() {
      AABB var1 = this.getBoundingBox();
      int var2 = Mth.floor(var1.minX);
      int var3 = Mth.ceil(var1.maxX);
      int var4 = Mth.floor(var1.maxY);
      int var5 = Mth.ceil(var1.maxY - this.lastYd);
      int var6 = Mth.floor(var1.minZ);
      int var7 = Mth.ceil(var1.maxZ);
      BlockPos.MutableBlockPos var8 = new BlockPos.MutableBlockPos();

      label39:
      for (int var9 = var4; var9 < var5; var9++) {
         float var10 = 0.0F;

         for (int var11 = var2; var11 < var3; var11++) {
            for (int var12 = var6; var12 < var7; var12++) {
               var8.set(var11, var9, var12);
               FluidState var13 = this.level.getFluidState(var8);
               if (var13.is(FluidTags.WATER)) {
                  var10 = Math.max(var10, var13.getHeight(this.level, var8));
               }

               if (var10 >= 1.0F) {
                  continue label39;
               }
            }
         }

         if (var10 < 1.0F) {
            return var8.getY() + var10;
         }
      }

      return var5 + 1;
   }

   public float getGroundFriction() {
      AABB var1 = this.getBoundingBox();
      AABB var2 = new AABB(var1.minX, var1.minY - 0.001, var1.minZ, var1.maxX, var1.minY, var1.maxZ);
      int var3 = Mth.floor(var2.minX) - 1;
      int var4 = Mth.ceil(var2.maxX) + 1;
      int var5 = Mth.floor(var2.minY) - 1;
      int var6 = Mth.ceil(var2.maxY) + 1;
      int var7 = Mth.floor(var2.minZ) - 1;
      int var8 = Mth.ceil(var2.maxZ) + 1;
      VoxelShape var9 = Shapes.create(var2);
      float var10 = 0.0F;
      int var11 = 0;
      BlockPos.MutableBlockPos var12 = new BlockPos.MutableBlockPos();

      for (int var13 = var3; var13 < var4; var13++) {
         for (int var14 = var7; var14 < var8; var14++) {
            int var15 = (var13 != var3 && var13 != var4 - 1 ? 0 : 1) + (var14 != var7 && var14 != var8 - 1 ? 0 : 1);
            if (var15 != 2) {
               for (int var16 = var5; var16 < var6; var16++) {
                  if (var15 <= 0 || var16 != var5 && var16 != var6 - 1) {
                     var12.set(var13, var16, var14);
                     BlockState var17 = this.level.getBlockState(var12);
                     if (!(var17.getBlock() instanceof WaterlilyBlock)
                        && Shapes.joinIsNotEmpty(var17.getCollisionShape(this.level, var12).move(var13, var16, var14), var9, BooleanOp.AND)) {
                        var10 += var17.getBlock().getFriction();
                        var11++;
                     }
                  }
               }
            }
         }
      }

      return var10 / var11;
   }

   private boolean checkInWater() {
      AABB var1 = this.getBoundingBox();
      int var2 = Mth.floor(var1.minX);
      int var3 = Mth.ceil(var1.maxX);
      int var4 = Mth.floor(var1.minY);
      int var5 = Mth.ceil(var1.minY + 0.001);
      int var6 = Mth.floor(var1.minZ);
      int var7 = Mth.ceil(var1.maxZ);
      boolean var8 = false;
      this.waterLevel = -Double.MAX_VALUE;
      BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();

      for (int var10 = var2; var10 < var3; var10++) {
         for (int var11 = var4; var11 < var5; var11++) {
            for (int var12 = var6; var12 < var7; var12++) {
               var9.set(var10, var11, var12);
               FluidState var13 = this.level.getFluidState(var9);
               if (var13.is(FluidTags.WATER)) {
                  float var14 = var11 + var13.getHeight(this.level, var9);
                  this.waterLevel = Math.max(var14, this.waterLevel);
                  var8 |= var1.minY < var14;
               }
            }
         }
      }

      return var8;
   }

   @Nullable
   private Boat.Status isUnderwater() {
      AABB var1 = this.getBoundingBox();
      double var2 = var1.maxY + 0.001;
      int var4 = Mth.floor(var1.minX);
      int var5 = Mth.ceil(var1.maxX);
      int var6 = Mth.floor(var1.maxY);
      int var7 = Mth.ceil(var2);
      int var8 = Mth.floor(var1.minZ);
      int var9 = Mth.ceil(var1.maxZ);
      boolean var10 = false;
      BlockPos.MutableBlockPos var11 = new BlockPos.MutableBlockPos();

      for (int var12 = var4; var12 < var5; var12++) {
         for (int var13 = var6; var13 < var7; var13++) {
            for (int var14 = var8; var14 < var9; var14++) {
               var11.set(var12, var13, var14);
               FluidState var15 = this.level.getFluidState(var11);
               if (var15.is(FluidTags.WATER) && var2 < var11.getY() + var15.getHeight(this.level, var11)) {
                  if (!var15.isSource()) {
                     return Boat.Status.UNDER_FLOWING_WATER;
                  }

                  var10 = true;
               }
            }
         }
      }

      return var10 ? Boat.Status.UNDER_WATER : null;
   }

   private void floatBoat() {
      double var1 = -0.04F;
      double var3 = this.isNoGravity() ? 0.0 : -0.04F;
      double var5 = 0.0;
      this.invFriction = 0.05F;
      if (this.oldStatus == Boat.Status.IN_AIR && this.status != Boat.Status.IN_AIR && this.status != Boat.Status.ON_LAND) {
         this.waterLevel = this.getY(1.0);
         this.setPos(this.getX(), this.getWaterLevelAbove() - this.getBbHeight() + 0.101, this.getZ());
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.0, 1.0));
         this.lastYd = 0.0;
         this.status = Boat.Status.IN_WATER;
      } else {
         if (this.status == Boat.Status.IN_WATER) {
            var5 = (this.waterLevel - this.getY()) / this.getBbHeight();
            this.invFriction = 0.9F;
         } else if (this.status == Boat.Status.UNDER_FLOWING_WATER) {
            var3 = -7.0E-4;
            this.invFriction = 0.9F;
         } else if (this.status == Boat.Status.UNDER_WATER) {
            var5 = 0.01F;
            this.invFriction = 0.45F;
         } else if (this.status == Boat.Status.IN_AIR) {
            this.invFriction = 0.9F;
         } else if (this.status == Boat.Status.ON_LAND) {
            this.invFriction = this.landFriction;
            if (this.getControllingPassenger() instanceof Player) {
               this.landFriction /= 2.0F;
            }
         }

         Vec3 var7 = this.getDeltaMovement();
         this.setDeltaMovement(var7.x * this.invFriction, var7.y + var3, var7.z * this.invFriction);
         this.deltaRotation = this.deltaRotation * this.invFriction;
         if (var5 > 0.0) {
            Vec3 var8 = this.getDeltaMovement();
            this.setDeltaMovement(var8.x, (var8.y + var5 * 0.06153846016296973) * 0.75, var8.z);
         }
      }
   }

   private void controlBoat() {
      if (this.isVehicle()) {
         float var1 = 0.0F;
         if (this.inputLeft) {
            this.deltaRotation--;
         }

         if (this.inputRight) {
            this.deltaRotation++;
         }

         if (this.inputRight != this.inputLeft && !this.inputUp && !this.inputDown) {
            var1 += 0.005F;
         }

         this.setYRot(this.getYRot() + this.deltaRotation);
         if (this.inputUp) {
            var1 += 0.04F;
         }

         if (this.inputDown) {
            var1 -= 0.005F;
         }

         this.setDeltaMovement(
            this.getDeltaMovement()
               .add(Mth.sin(-this.getYRot() * (float) (Math.PI / 180.0)) * var1, 0.0, Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * var1)
         );
         this.setPaddleState(this.inputRight && !this.inputLeft || this.inputUp, this.inputLeft && !this.inputRight || this.inputUp);
      }
   }

   protected float getSinglePassengerXOffset() {
      return 0.0F;
   }

   public boolean hasEnoughSpaceFor(Entity param0) {
      return param0.getBbWidth() < this.getBbWidth();
   }

   @Override
   public void positionRider(Entity param0) {
      if (this.hasPassenger(param0)) {
         float var2 = this.getSinglePassengerXOffset();
         float var3 = (float)((this.isRemoved() ? 0.01F : this.getPassengersRidingOffset()) + param0.getMyRidingOffset());
         if (this.getPassengers().size() > 1) {
            int var4 = this.getPassengers().indexOf(param0);
            if (var4 == 0) {
               var2 = 0.2F;
            } else {
               var2 = -0.6F;
            }

            if (param0 instanceof Animal) {
               var2 += 0.2F;
            }
         }

         Vec3 var4 = new Vec3(var2, 0.0, 0.0).yRot(-this.getYRot() * (float) (Math.PI / 180.0) - (float) (Math.PI / 2));
         param0.setPos(this.getX() + var4.x, this.getY() + var3, this.getZ() + var4.z);
         param0.setYRot(param0.getYRot() + this.deltaRotation);
         param0.setYHeadRot(param0.getYHeadRot() + this.deltaRotation);
         this.clampRotation(param0);
         if (param0 instanceof Animal && this.getPassengers().size() == this.getMaxPassengers()) {
            int var5 = param0.getId() % 2 == 0 ? 90 : 270;
            param0.setYBodyRot(((Animal)param0).yBodyRot + var5);
            param0.setYHeadRot(param0.getYHeadRot() + var5);
         }
      }
   }

   @Override
   public Vec3 getDismountLocationForPassenger(LivingEntity param0) {
      Vec3 var2 = getCollisionHorizontalEscapeVector(this.getBbWidth() * Mth.SQRT_OF_TWO, param0.getBbWidth(), param0.getYRot());
      double var3 = this.getX() + var2.x;
      double var5 = this.getZ() + var2.z;
      BlockPos var7 = BlockPos.containing(var3, this.getBoundingBox().maxY, var5);
      BlockPos var8 = var7.below();
      if (!this.level.isWaterAt(var8)) {
         List<Vec3> var9 = Lists.newArrayList();
         double var10 = this.level.getBlockFloorHeight(var7);
         if (DismountHelper.isBlockFloorValid(var10)) {
            var9.add(new Vec3(var3, var7.getY() + var10, var5));
         }

         double var12 = this.level.getBlockFloorHeight(var8);
         if (DismountHelper.isBlockFloorValid(var12)) {
            var9.add(new Vec3(var3, var8.getY() + var12, var5));
         }

         for (Pose var15 : param0.getDismountPoses()) {
            for (Vec3 var17 : var9) {
               if (DismountHelper.canDismountTo(this.level, var17, param0, var15)) {
                  param0.setPose(var15);
                  return var17;
               }
            }
         }
      }

      return super.getDismountLocationForPassenger(param0);
   }

   protected void clampRotation(Entity param0) {
      param0.setYBodyRot(this.getYRot());
      float var2 = Mth.wrapDegrees(param0.getYRot() - this.getYRot());
      float var3 = Mth.clamp(var2, -105.0F, 105.0F);
      param0.yRotO += var3 - var2;
      param0.setYRot(param0.getYRot() + var3 - var2);
      param0.setYHeadRot(param0.getYRot());
   }

   @Override
   public void onPassengerTurned(Entity param0) {
      this.clampRotation(param0);
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag param0) {
      param0.putString("Type", this.getVariant().getSerializedName());
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag param0) {
      if (param0.contains("Type", 8)) {
         this.setVariant(Boat.Type.byName(param0.getString("Type")));
      }
   }

   @Override
   public InteractionResult interact(Player param0, InteractionHand param1) {
      if (param0.isSecondaryUseActive()) {
         return InteractionResult.PASS;
      }

      if (this.outOfControlTicks < 60.0F) {
         if (!this.level.isClientSide) {
            return param0.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected void checkFallDamage(double param0, boolean param2, BlockState param3, BlockPos param4) {
      this.lastYd = this.getDeltaMovement().y;
      if (!this.isPassenger()) {
         if (param2) {
            if (this.fallDistance > 3.0F) {
               if (this.status != Boat.Status.ON_LAND) {
                  this.resetFallDistance();
                  return;
               }

               this.causeFallDamage(this.fallDistance, 1.0F, this.damageSources().fall());
               if (!this.level.isClientSide && !this.isRemoved()) {
                  this.kill();
                  if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                     for (int var6 = 0; var6 < 3; var6++) {
                        this.spawnAtLocation(this.getVariant().getPlanks());
                     }

                     for (int var6 = 0; var6 < 2; var6++) {
                        this.spawnAtLocation(Items.STICK);
                     }
                  }
               }
            }

            this.resetFallDistance();
         } else if (!this.level.getFluidState(this.blockPosition().below()).is(FluidTags.WATER) && param0 < 0.0) {
            this.fallDistance -= (float)param0;
         }
      }
   }

   public boolean getPaddleState(int param0) {
      return this.entityData.get(param0 == 0 ? DATA_ID_PADDLE_LEFT : DATA_ID_PADDLE_RIGHT) && this.getControllingPassenger() != null;
   }

   public void setDamage(float param0) {
      this.entityData.set(DATA_ID_DAMAGE, param0);
   }

   public float getDamage() {
      return this.entityData.get(DATA_ID_DAMAGE);
   }

   public void setHurtTime(int param0) {
      this.entityData.set(DATA_ID_HURT, param0);
   }

   public int getHurtTime() {
      return this.entityData.get(DATA_ID_HURT);
   }

   private void setBubbleTime(int param0) {
      this.entityData.set(DATA_ID_BUBBLE_TIME, param0);
   }

   private int getBubbleTime() {
      return this.entityData.get(DATA_ID_BUBBLE_TIME);
   }

   public float getBubbleAngle(float param0) {
      return Mth.lerp(param0, this.bubbleAngleO, this.bubbleAngle);
   }

   public void setHurtDir(int param0) {
      this.entityData.set(DATA_ID_HURTDIR, param0);
   }

   public int getHurtDir() {
      return this.entityData.get(DATA_ID_HURTDIR);
   }

   public void setVariant(Boat.Type param0) {
      this.entityData.set(DATA_ID_TYPE, param0.ordinal());
   }

   public Boat.Type getVariant() {
      return Boat.Type.byId(this.entityData.get(DATA_ID_TYPE));
   }

   @Override
   protected boolean canAddPassenger(Entity param0) {
      return this.getPassengers().size() < this.getMaxPassengers() && !this.isEyeInFluid(FluidTags.WATER);
   }

   protected int getMaxPassengers() {
      return 2;
   }

   @Nullable
   @Override
   public LivingEntity getControllingPassenger() {
      return this.getFirstPassenger() instanceof LivingEntity var1 ? var1 : null;
   }

   public void setInput(boolean param0, boolean param1, boolean param2, boolean param3) {
      this.inputLeft = param0;
      this.inputRight = param1;
      this.inputUp = param2;
      this.inputDown = param3;
   }

   @Override
   public boolean isUnderWater() {
      return this.status == Boat.Status.UNDER_WATER || this.status == Boat.Status.UNDER_FLOWING_WATER;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(this.getDropItem());
   }

   public enum Status {
      IN_WATER,
      UNDER_WATER,
      UNDER_FLOWING_WATER,
      ON_LAND,
      IN_AIR;
   }

   public enum Type implements StringRepresentable {
      OAK(Blocks.OAK_PLANKS, "oak"),
      SPRUCE(Blocks.SPRUCE_PLANKS, "spruce"),
      BIRCH(Blocks.BIRCH_PLANKS, "birch"),
      JUNGLE(Blocks.JUNGLE_PLANKS, "jungle"),
      ACACIA(Blocks.ACACIA_PLANKS, "acacia"),
      CHERRY(Blocks.CHERRY_PLANKS, "cherry"),
      DARK_OAK(Blocks.DARK_OAK_PLANKS, "dark_oak"),
      MANGROVE(Blocks.MANGROVE_PLANKS, "mangrove"),
      BAMBOO(Blocks.BAMBOO_PLANKS, "bamboo");

      private final String name;
      private final Block planks;
      public static final StringRepresentable.EnumCodec<Boat.Type> CODEC = StringRepresentable.fromEnum(Boat.Type::values);
      private static final IntFunction<Boat.Type> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

      Type(Block param2, String param3) {
         this.name = param3;
         this.planks = param2;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public String getName() {
         return this.name;
      }

      public Block getPlanks() {
         return this.planks;
      }

      @Override
      public String toString() {
         return this.name;
      }

      public static Boat.Type byId(int param0) {
         return BY_ID.apply(param0);
      }

      public static Boat.Type byName(String param0) {
         return CODEC.byName(param0, OAK);
      }
   }
}
