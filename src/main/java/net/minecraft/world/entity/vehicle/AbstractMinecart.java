package net.minecraft.world.entity.vehicle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractMinecart extends Entity {
   private static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ID_HURTDIR = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_BLOCK = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_OFFSET = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_ID_CUSTOM_DISPLAY = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.BOOLEAN);
   private static final ImmutableMap<Pose, ImmutableList<Integer>> POSE_DISMOUNT_HEIGHTS = ImmutableMap.of(
      Pose.STANDING, ImmutableList.of(0, 1, -1), Pose.CROUCHING, ImmutableList.of(0, 1, -1), Pose.SWIMMING, ImmutableList.of(0, 1)
   );
   protected static final float WATER_SLOWDOWN_FACTOR = 0.95F;
   private boolean flipped;
   private static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Util.make(Maps.newEnumMap(RailShape.class), param0 -> {
      Vec3i var1 = Direction.WEST.getNormal();
      Vec3i var2 = Direction.EAST.getNormal();
      Vec3i var3 = Direction.NORTH.getNormal();
      Vec3i var4 = Direction.SOUTH.getNormal();
      Vec3i var5 = var1.below();
      Vec3i var6 = var2.below();
      Vec3i var7 = var3.below();
      Vec3i var8 = var4.below();
      param0.put(RailShape.NORTH_SOUTH, Pair.of(var3, var4));
      param0.put(RailShape.EAST_WEST, Pair.of(var1, var2));
      param0.put(RailShape.ASCENDING_EAST, Pair.of(var5, var2));
      param0.put(RailShape.ASCENDING_WEST, Pair.of(var1, var6));
      param0.put(RailShape.ASCENDING_NORTH, Pair.of(var3, var8));
      param0.put(RailShape.ASCENDING_SOUTH, Pair.of(var7, var4));
      param0.put(RailShape.SOUTH_EAST, Pair.of(var4, var2));
      param0.put(RailShape.SOUTH_WEST, Pair.of(var4, var1));
      param0.put(RailShape.NORTH_WEST, Pair.of(var3, var1));
      param0.put(RailShape.NORTH_EAST, Pair.of(var3, var2));
   });
   private int lSteps;
   private double lx;
   private double ly;
   private double lz;
   private double lyr;
   private double lxr;
   private double lxd;
   private double lyd;
   private double lzd;

   protected AbstractMinecart(EntityType<?> param0, Level param1) {
      super(param0, param1);
      this.blocksBuilding = true;
   }

   protected AbstractMinecart(EntityType<?> param0, Level param1, double param2, double param4, double param6) {
      this(param0, param1);
      this.setPos(param2, param4, param6);
      this.xo = param2;
      this.yo = param4;
      this.zo = param6;
   }

   public static AbstractMinecart createMinecart(Level param0, double param1, double param3, double param5, AbstractMinecart.Type param7) {
      if (param7 == AbstractMinecart.Type.CHEST) {
         return new MinecartChest(param0, param1, param3, param5);
      } else if (param7 == AbstractMinecart.Type.FURNACE) {
         return new MinecartFurnace(param0, param1, param3, param5);
      } else if (param7 == AbstractMinecart.Type.TNT) {
         return new MinecartTNT(param0, param1, param3, param5);
      } else if (param7 == AbstractMinecart.Type.SPAWNER) {
         return new MinecartSpawner(param0, param1, param3, param5);
      } else if (param7 == AbstractMinecart.Type.HOPPER) {
         return new MinecartHopper(param0, param1, param3, param5);
      } else {
         return param7 == AbstractMinecart.Type.COMMAND_BLOCK
            ? new MinecartCommandBlock(param0, param1, param3, param5)
            : new Minecart(param0, param1, param3, param5);
      }
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
      this.entityData.define(DATA_ID_DISPLAY_BLOCK, Block.getId(Blocks.AIR.defaultBlockState()));
      this.entityData.define(DATA_ID_DISPLAY_OFFSET, 6);
      this.entityData.define(DATA_ID_CUSTOM_DISPLAY, false);
   }

   @Override
   public boolean canCollideWith(Entity param0) {
      return Boat.canVehicleCollide(this, param0);
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
      return 0.0;
   }

   @Override
   public Vec3 getDismountLocationForPassenger(LivingEntity param0) {
      Direction var2 = this.getMotionDirection();
      if (var2.getAxis() == Direction.Axis.Y) {
         return super.getDismountLocationForPassenger(param0);
      }

      int[][] var3 = DismountHelper.offsetsForDirection(var2);
      BlockPos var4 = this.blockPosition();
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();
      ImmutableList<Pose> var6 = param0.getDismountPoses();

      for (Pose var8 : var6) {
         EntityDimensions var9 = param0.getDimensions(var8);
         float var10 = Math.min(var9.width, 1.0F) / 2.0F;

         for (int var12 : POSE_DISMOUNT_HEIGHTS.get(var8)) {
            for (int[] var16 : var3) {
               var5.set(var4.getX() + var16[0], var4.getY() + var12, var4.getZ() + var16[1]);
               double var17 = this.level
                  .getBlockFloorHeight(DismountHelper.nonClimbableShape(this.level, var5), () -> DismountHelper.nonClimbableShape(this.level, var5.below()));
               if (DismountHelper.isBlockFloorValid(var17)) {
                  AABB var19 = new AABB(-var10, 0.0, -var10, var10, var9.height, var10);
                  Vec3 var20 = Vec3.upFromBottomCenterOf(var5, var17);
                  if (DismountHelper.canDismountTo(this.level, param0, var19.move(var20))) {
                     param0.setPose(var8);
                     return var20;
                  }
               }
            }
         }
      }

      double var7 = this.getBoundingBox().maxY;
      var5.set(var4.getX(), var7, var4.getZ());

      for (Pose var10 : var6) {
         double var11 = param0.getDimensions(var10).height;
         int var13 = Mth.ceil(var7 - var5.getY() + var11);
         double var14 = DismountHelper.findCeilingFrom(var5, var13, l_param0 -> this.level.getBlockState(l_param0).getCollisionShape(this.level, l_param0));
         if (var7 + var11 <= var14) {
            param0.setPose(var10);
            break;
         }
      }

      return super.getDismountLocationForPassenger(param0);
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.level.isClientSide || this.isRemoved()) {
         return true;
      }

      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.markHurt();
      this.setDamage(this.getDamage() + param1 * 10.0F);
      this.gameEvent(GameEvent.ENTITY_DAMAGE, param0.getEntity());
      boolean var3 = param0.getEntity() instanceof Player && ((Player)param0.getEntity()).getAbilities().instabuild;
      if (var3 || this.getDamage() > 40.0F) {
         this.ejectPassengers();
         if (var3 && !this.hasCustomName()) {
            this.discard();
         } else {
            this.destroy(param0);
         }
      }

      return true;
   }

   @Override
   protected float getBlockSpeedFactor() {
      BlockState var1 = this.level.getBlockState(this.blockPosition());
      return var1.is(BlockTags.RAILS) ? 1.0F : super.getBlockSpeedFactor();
   }

   public void destroy(DamageSource param0) {
      this.kill();
      if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
         ItemStack var2 = new ItemStack(this.getDropItem());
         if (this.hasCustomName()) {
            var2.setHoverName(this.getCustomName());
         }

         this.spawnAtLocation(var2);
      }
   }

   abstract Item getDropItem();

   @Override
   public void animateHurt(float param0) {
      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   private static Pair<Vec3i, Vec3i> exits(RailShape param0) {
      return EXITS.get(param0);
   }

   @Override
   public Direction getMotionDirection() {
      return this.flipped ? this.getDirection().getOpposite().getClockWise() : this.getDirection().getClockWise();
   }

   @Override
   public void tick() {
      if (this.getHurtTime() > 0) {
         this.setHurtTime(this.getHurtTime() - 1);
      }

      if (this.getDamage() > 0.0F) {
         this.setDamage(this.getDamage() - 1.0F);
      }

      this.checkOutOfWorld();
      this.handleNetherPortal();
      if (this.level.isClientSide) {
         if (this.lSteps > 0) {
            double var1 = this.getX() + (this.lx - this.getX()) / this.lSteps;
            double var3 = this.getY() + (this.ly - this.getY()) / this.lSteps;
            double var5 = this.getZ() + (this.lz - this.getZ()) / this.lSteps;
            double var7 = Mth.wrapDegrees(this.lyr - this.getYRot());
            this.setYRot(this.getYRot() + (float)var7 / this.lSteps);
            this.setXRot(this.getXRot() + (float)(this.lxr - this.getXRot()) / this.lSteps);
            this.lSteps--;
            this.setPos(var1, var3, var5);
            this.setRot(this.getYRot(), this.getXRot());
         } else {
            this.reapplyPosition();
            this.setRot(this.getYRot(), this.getXRot());
         }
      } else {
         if (!this.isNoGravity()) {
            double var1 = this.isInWater() ? -0.005 : -0.04;
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, var1, 0.0));
         }

         int var1 = Mth.floor(this.getX());
         int var2 = Mth.floor(this.getY());
         int var3 = Mth.floor(this.getZ());
         if (this.level.getBlockState(new BlockPos(var1, var2 - 1, var3)).is(BlockTags.RAILS)) {
            var2--;
         }

         BlockPos var4 = new BlockPos(var1, var2, var3);
         BlockState var5 = this.level.getBlockState(var4);
         if (BaseRailBlock.isRail(var5)) {
            this.moveAlongTrack(var4, var5);
            if (var5.is(Blocks.ACTIVATOR_RAIL)) {
               this.activateMinecart(var1, var2, var3, var5.getValue(PoweredRailBlock.POWERED));
            }
         } else {
            this.comeOffTrack();
         }

         this.checkInsideBlocks();
         this.setXRot(0.0F);
         double var6 = this.xo - this.getX();
         double var8 = this.zo - this.getZ();
         if (var6 * var6 + var8 * var8 > 0.001) {
            this.setYRot((float)(Mth.atan2(var8, var6) * 180.0 / Math.PI));
            if (this.flipped) {
               this.setYRot(this.getYRot() + 180.0F);
            }
         }

         double var10 = Mth.wrapDegrees(this.getYRot() - this.yRotO);
         if (var10 < -170.0 || var10 >= 170.0) {
            this.setYRot(this.getYRot() + 180.0F);
            this.flipped = !this.flipped;
         }

         this.setRot(this.getYRot(), this.getXRot());
         if (this.getMinecartType() == AbstractMinecart.Type.RIDEABLE && this.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            List<Entity> var12 = this.level.getEntities(this, this.getBoundingBox().inflate(0.2F, 0.0, 0.2F), EntitySelector.pushableBy(this));
            if (!var12.isEmpty()) {
               for (int var13 = 0; var13 < var12.size(); var13++) {
                  Entity var14 = var12.get(var13);
                  if (!(var14 instanceof Player)
                     && !(var14 instanceof IronGolem)
                     && !(var14 instanceof AbstractMinecart)
                     && !this.isVehicle()
                     && !var14.isPassenger()) {
                     var14.startRiding(this);
                  } else {
                     var14.push(this);
                  }
               }
            }
         } else {
            for (Entity var13 : this.level.getEntities(this, this.getBoundingBox().inflate(0.2F, 0.0, 0.2F))) {
               if (!this.hasPassenger(var13) && var13.isPushable() && var13 instanceof AbstractMinecart) {
                  var13.push(this);
               }
            }
         }

         this.updateInWaterStateAndDoFluidPushing();
         if (this.isInLava()) {
            this.lavaHurt();
            this.fallDistance *= 0.5F;
         }

         this.firstTick = false;
      }
   }

   protected double getMaxSpeed() {
      return (this.isInWater() ? 4.0 : 8.0) / 20.0;
   }

   public void activateMinecart(int param0, int param1, int param2, boolean param3) {
   }

   protected void comeOffTrack() {
      double var1 = this.getMaxSpeed();
      Vec3 var3 = this.getDeltaMovement();
      this.setDeltaMovement(Mth.clamp(var3.x, -var1, var1), var3.y, Mth.clamp(var3.z, -var1, var1));
      if (this.onGround) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      if (!this.onGround) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
      }
   }

   protected void moveAlongTrack(BlockPos param0, BlockState param1) {
      this.resetFallDistance();
      double var3 = this.getX();
      double var5 = this.getY();
      double var7 = this.getZ();
      Vec3 var9 = this.getPos(var3, var5, var7);
      var5 = param0.getY();
      boolean var10 = false;
      boolean var11 = false;
      if (param1.is(Blocks.POWERED_RAIL)) {
         var10 = param1.getValue(PoweredRailBlock.POWERED);
         var11 = !var10;
      }

      double var12 = 0.0078125;
      if (this.isInWater()) {
         var12 *= 0.2;
      }

      Vec3 var14 = this.getDeltaMovement();
      RailShape var15 = param1.getValue(((BaseRailBlock)param1.getBlock()).getShapeProperty());
      switch (var15) {
         case ASCENDING_EAST:
            this.setDeltaMovement(var14.add(-var12, 0.0, 0.0));
            var5++;
            break;
         case ASCENDING_WEST:
            this.setDeltaMovement(var14.add(var12, 0.0, 0.0));
            var5++;
            break;
         case ASCENDING_NORTH:
            this.setDeltaMovement(var14.add(0.0, 0.0, var12));
            var5++;
            break;
         case ASCENDING_SOUTH:
            this.setDeltaMovement(var14.add(0.0, 0.0, -var12));
            var5++;
      }

      var14 = this.getDeltaMovement();
      Pair<Vec3i, Vec3i> var16 = exits(var15);
      Vec3i var17 = var16.getFirst();
      Vec3i var18 = var16.getSecond();
      double var19 = var18.getX() - var17.getX();
      double var21 = var18.getZ() - var17.getZ();
      double var23 = Math.sqrt(var19 * var19 + var21 * var21);
      double var25 = var14.x * var19 + var14.z * var21;
      if (var25 < 0.0) {
         var19 = -var19;
         var21 = -var21;
      }

      double var27 = Math.min(2.0, var14.horizontalDistance());
      var14 = new Vec3(var27 * var19 / var23, var14.y, var27 * var21 / var23);
      this.setDeltaMovement(var14);
      Entity var29 = this.getFirstPassenger();
      if (var29 instanceof Player) {
         Vec3 var30 = var29.getDeltaMovement();
         double var31 = var30.horizontalDistanceSqr();
         double var33 = this.getDeltaMovement().horizontalDistanceSqr();
         if (var31 > 1.0E-4 && var33 < 0.01) {
            this.setDeltaMovement(this.getDeltaMovement().add(var30.x * 0.1, 0.0, var30.z * 0.1));
            var11 = false;
         }
      }

      if (var11) {
         double var30 = this.getDeltaMovement().horizontalDistance();
         if (var30 < 0.03) {
            this.setDeltaMovement(Vec3.ZERO);
         } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.0, 0.5));
         }
      }

      double var30 = param0.getX() + 0.5 + var17.getX() * 0.5;
      double var32 = param0.getZ() + 0.5 + var17.getZ() * 0.5;
      double var34 = param0.getX() + 0.5 + var18.getX() * 0.5;
      double var36 = param0.getZ() + 0.5 + var18.getZ() * 0.5;
      var19 = var34 - var30;
      var21 = var36 - var32;
      double var38;
      if (var19 == 0.0) {
         var38 = var7 - param0.getZ();
      } else if (var21 == 0.0) {
         var38 = var3 - param0.getX();
      } else {
         double var40 = var3 - var30;
         double var42 = var7 - var32;
         var38 = (var40 * var19 + var42 * var21) * 2.0;
      }

      var3 = var30 + var19 * var38;
      var7 = var32 + var21 * var38;
      this.setPos(var3, var5, var7);
      double var40 = this.isVehicle() ? 0.75 : 1.0;
      double var42 = this.getMaxSpeed();
      var14 = this.getDeltaMovement();
      this.move(MoverType.SELF, new Vec3(Mth.clamp(var40 * var14.x, -var42, var42), 0.0, Mth.clamp(var40 * var14.z, -var42, var42)));
      if (var17.getY() != 0 && Mth.floor(this.getX()) - param0.getX() == var17.getX() && Mth.floor(this.getZ()) - param0.getZ() == var17.getZ()) {
         this.setPos(this.getX(), this.getY() + var17.getY(), this.getZ());
      } else if (var18.getY() != 0 && Mth.floor(this.getX()) - param0.getX() == var18.getX() && Mth.floor(this.getZ()) - param0.getZ() == var18.getZ()) {
         this.setPos(this.getX(), this.getY() + var18.getY(), this.getZ());
      }

      this.applyNaturalSlowdown();
      Vec3 var44 = this.getPos(this.getX(), this.getY(), this.getZ());
      if (var44 != null && var9 != null) {
         double var45 = (var9.y - var44.y) * 0.05;
         Vec3 var47 = this.getDeltaMovement();
         double var48 = var47.horizontalDistance();
         if (var48 > 0.0) {
            this.setDeltaMovement(var47.multiply((var48 + var45) / var48, 1.0, (var48 + var45) / var48));
         }

         this.setPos(this.getX(), var44.y, this.getZ());
      }

      int var45 = Mth.floor(this.getX());
      int var46 = Mth.floor(this.getZ());
      if (var45 != param0.getX() || var46 != param0.getZ()) {
         Vec3 var47 = this.getDeltaMovement();
         double var48 = var47.horizontalDistance();
         this.setDeltaMovement(var48 * (var45 - param0.getX()), var47.y, var48 * (var46 - param0.getZ()));
      }

      if (var10) {
         Vec3 var47 = this.getDeltaMovement();
         double var48 = var47.horizontalDistance();
         if (var48 > 0.01) {
            double var50 = 0.06;
            this.setDeltaMovement(var47.add(var47.x / var48 * 0.06, 0.0, var47.z / var48 * 0.06));
         } else {
            Vec3 var50 = this.getDeltaMovement();
            double var51 = var50.x;
            double var53 = var50.z;
            if (var15 == RailShape.EAST_WEST) {
               if (this.isRedstoneConductor(param0.west())) {
                  var51 = 0.02;
               } else if (this.isRedstoneConductor(param0.east())) {
                  var51 = -0.02;
               }
            } else {
               if (var15 != RailShape.NORTH_SOUTH) {
                  return;
               }

               if (this.isRedstoneConductor(param0.north())) {
                  var53 = 0.02;
               } else if (this.isRedstoneConductor(param0.south())) {
                  var53 = -0.02;
               }
            }

            this.setDeltaMovement(var51, var50.y, var53);
         }
      }
   }

   private boolean isRedstoneConductor(BlockPos param0) {
      return this.level.getBlockState(param0).isRedstoneConductor(this.level, param0);
   }

   protected void applyNaturalSlowdown() {
      double var1 = this.isVehicle() ? 0.997 : 0.96;
      Vec3 var3 = this.getDeltaMovement();
      var3 = var3.multiply(var1, 0.0, var1);
      if (this.isInWater()) {
         var3 = var3.scale(0.95F);
      }

      this.setDeltaMovement(var3);
   }

   @Nullable
   public Vec3 getPosOffs(double param0, double param2, double param4, double param6) {
      int var9 = Mth.floor(param0);
      int var10 = Mth.floor(param2);
      int var11 = Mth.floor(param4);
      if (this.level.getBlockState(new BlockPos(var9, var10 - 1, var11)).is(BlockTags.RAILS)) {
         var10--;
      }

      BlockState var12 = this.level.getBlockState(new BlockPos(var9, var10, var11));
      if (BaseRailBlock.isRail(var12)) {
         RailShape var13 = var12.getValue(((BaseRailBlock)var12.getBlock()).getShapeProperty());
         param2 = var10;
         if (var13.isAscending()) {
            param2 = var10 + 1;
         }

         Pair<Vec3i, Vec3i> var14 = exits(var13);
         Vec3i var15 = var14.getFirst();
         Vec3i var16 = var14.getSecond();
         double var17 = var16.getX() - var15.getX();
         double var19 = var16.getZ() - var15.getZ();
         double var21 = Math.sqrt(var17 * var17 + var19 * var19);
         var17 /= var21;
         var19 /= var21;
         param0 += var17 * param6;
         param4 += var19 * param6;
         if (var15.getY() != 0 && Mth.floor(param0) - var9 == var15.getX() && Mth.floor(param4) - var11 == var15.getZ()) {
            param2 += var15.getY();
         } else if (var16.getY() != 0 && Mth.floor(param0) - var9 == var16.getX() && Mth.floor(param4) - var11 == var16.getZ()) {
            param2 += var16.getY();
         }

         return this.getPos(param0, param2, param4);
      } else {
         return null;
      }
   }

   @Nullable
   public Vec3 getPos(double param0, double param2, double param4) {
      int var7 = Mth.floor(param0);
      int var8 = Mth.floor(param2);
      int var9 = Mth.floor(param4);
      if (this.level.getBlockState(new BlockPos(var7, var8 - 1, var9)).is(BlockTags.RAILS)) {
         var8--;
      }

      BlockState var10 = this.level.getBlockState(new BlockPos(var7, var8, var9));
      if (BaseRailBlock.isRail(var10)) {
         RailShape var11 = var10.getValue(((BaseRailBlock)var10.getBlock()).getShapeProperty());
         Pair<Vec3i, Vec3i> var12 = exits(var11);
         Vec3i var13 = var12.getFirst();
         Vec3i var14 = var12.getSecond();
         double var15 = var7 + 0.5 + var13.getX() * 0.5;
         double var17 = var8 + 0.0625 + var13.getY() * 0.5;
         double var19 = var9 + 0.5 + var13.getZ() * 0.5;
         double var21 = var7 + 0.5 + var14.getX() * 0.5;
         double var23 = var8 + 0.0625 + var14.getY() * 0.5;
         double var25 = var9 + 0.5 + var14.getZ() * 0.5;
         double var27 = var21 - var15;
         double var29 = (var23 - var17) * 2.0;
         double var31 = var25 - var19;
         double var33;
         if (var27 == 0.0) {
            var33 = param4 - var9;
         } else if (var31 == 0.0) {
            var33 = param0 - var7;
         } else {
            double var35 = param0 - var15;
            double var37 = param4 - var19;
            var33 = (var35 * var27 + var37 * var31) * 2.0;
         }

         param0 = var15 + var27 * var33;
         param2 = var17 + var29 * var33;
         param4 = var19 + var31 * var33;
         if (var29 < 0.0) {
            param2++;
         } else if (var29 > 0.0) {
            param2 += 0.5;
         }

         return new Vec3(param0, param2, param4);
      } else {
         return null;
      }
   }

   @Override
   public AABB getBoundingBoxForCulling() {
      AABB var1 = this.getBoundingBox();
      return this.hasCustomDisplay() ? var1.inflate(Math.abs(this.getDisplayOffset()) / 16.0) : var1;
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag param0) {
      if (param0.getBoolean("CustomDisplayTile")) {
         this.setDisplayBlockState(NbtUtils.readBlockState(this.level.holderLookup(Registries.BLOCK), param0.getCompound("DisplayState")));
         this.setDisplayOffset(param0.getInt("DisplayOffset"));
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag param0) {
      if (this.hasCustomDisplay()) {
         param0.putBoolean("CustomDisplayTile", true);
         param0.put("DisplayState", NbtUtils.writeBlockState(this.getDisplayBlockState()));
         param0.putInt("DisplayOffset", this.getDisplayOffset());
      }
   }

   @Override
   public void push(Entity param0) {
      if (!this.level.isClientSide) {
         if (!param0.noPhysics && !this.noPhysics) {
            if (!this.hasPassenger(param0)) {
               double var2 = param0.getX() - this.getX();
               double var4 = param0.getZ() - this.getZ();
               double var6 = var2 * var2 + var4 * var4;
               if (var6 >= 1.0E-4F) {
                  var6 = Math.sqrt(var6);
                  var2 /= var6;
                  var4 /= var6;
                  double var8 = 1.0 / var6;
                  if (var8 > 1.0) {
                     var8 = 1.0;
                  }

                  var2 *= var8;
                  var4 *= var8;
                  var2 *= 0.1F;
                  var4 *= 0.1F;
                  var2 *= 0.5;
                  var4 *= 0.5;
                  if (param0 instanceof AbstractMinecart) {
                     double var10 = param0.getX() - this.getX();
                     double var12 = param0.getZ() - this.getZ();
                     Vec3 var14 = new Vec3(var10, 0.0, var12).normalize();
                     Vec3 var15 = new Vec3(Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)), 0.0, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)))
                        .normalize();
                     double var16 = Math.abs(var14.dot(var15));
                     if (var16 < 0.8F) {
                        return;
                     }

                     Vec3 var18 = this.getDeltaMovement();
                     Vec3 var19 = param0.getDeltaMovement();
                     if (((AbstractMinecart)param0).getMinecartType() == AbstractMinecart.Type.FURNACE
                        && this.getMinecartType() != AbstractMinecart.Type.FURNACE) {
                        this.setDeltaMovement(var18.multiply(0.2, 1.0, 0.2));
                        this.push(var19.x - var2, 0.0, var19.z - var4);
                        param0.setDeltaMovement(var19.multiply(0.95, 1.0, 0.95));
                     } else if (((AbstractMinecart)param0).getMinecartType() != AbstractMinecart.Type.FURNACE
                        && this.getMinecartType() == AbstractMinecart.Type.FURNACE) {
                        param0.setDeltaMovement(var19.multiply(0.2, 1.0, 0.2));
                        param0.push(var18.x + var2, 0.0, var18.z + var4);
                        this.setDeltaMovement(var18.multiply(0.95, 1.0, 0.95));
                     } else {
                        double var20 = (var19.x + var18.x) / 2.0;
                        double var22 = (var19.z + var18.z) / 2.0;
                        this.setDeltaMovement(var18.multiply(0.2, 1.0, 0.2));
                        this.push(var20 - var2, 0.0, var22 - var4);
                        param0.setDeltaMovement(var19.multiply(0.2, 1.0, 0.2));
                        param0.push(var20 + var2, 0.0, var22 + var4);
                     }
                  } else {
                     this.push(-var2, 0.0, -var4);
                     param0.push(var2 / 4.0, 0.0, var4 / 4.0);
                  }
               }
            }
         }
      }
   }

   @Override
   public void lerpTo(double param0, double param2, double param4, float param6, float param7, int param8, boolean param9) {
      this.lx = param0;
      this.ly = param2;
      this.lz = param4;
      this.lyr = param6;
      this.lxr = param7;
      this.lSteps = param8 + 2;
      this.setDeltaMovement(this.lxd, this.lyd, this.lzd);
   }

   @Override
   public void lerpMotion(double param0, double param2, double param4) {
      this.lxd = param0;
      this.lyd = param2;
      this.lzd = param4;
      this.setDeltaMovement(this.lxd, this.lyd, this.lzd);
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

   public void setHurtDir(int param0) {
      this.entityData.set(DATA_ID_HURTDIR, param0);
   }

   public int getHurtDir() {
      return this.entityData.get(DATA_ID_HURTDIR);
   }

   public abstract AbstractMinecart.Type getMinecartType();

   public BlockState getDisplayBlockState() {
      return !this.hasCustomDisplay() ? this.getDefaultDisplayBlockState() : Block.stateById(this.getEntityData().get(DATA_ID_DISPLAY_BLOCK));
   }

   public BlockState getDefaultDisplayBlockState() {
      return Blocks.AIR.defaultBlockState();
   }

   public int getDisplayOffset() {
      return !this.hasCustomDisplay() ? this.getDefaultDisplayOffset() : this.getEntityData().get(DATA_ID_DISPLAY_OFFSET);
   }

   public int getDefaultDisplayOffset() {
      return 6;
   }

   public void setDisplayBlockState(BlockState param0) {
      this.getEntityData().set(DATA_ID_DISPLAY_BLOCK, Block.getId(param0));
      this.setCustomDisplay(true);
   }

   public void setDisplayOffset(int param0) {
      this.getEntityData().set(DATA_ID_DISPLAY_OFFSET, param0);
      this.setCustomDisplay(true);
   }

   public boolean hasCustomDisplay() {
      return this.getEntityData().get(DATA_ID_CUSTOM_DISPLAY);
   }

   public void setCustomDisplay(boolean param0) {
      this.getEntityData().set(DATA_ID_CUSTOM_DISPLAY, param0);
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(switch (this.getMinecartType()) {
         case FURNACE -> Items.FURNACE_MINECART;
         case CHEST -> Items.CHEST_MINECART;
         case TNT -> Items.TNT_MINECART;
         case HOPPER -> Items.HOPPER_MINECART;
         case COMMAND_BLOCK -> Items.COMMAND_BLOCK_MINECART;
         default -> Items.MINECART;
      });
   }

   public enum Type {
      RIDEABLE,
      CHEST,
      FURNACE,
      TNT,
      SPAWNER,
      HOPPER,
      COMMAND_BLOCK;
   }
}
