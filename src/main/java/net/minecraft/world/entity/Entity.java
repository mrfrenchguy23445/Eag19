package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public abstract class Entity implements Nameable, EntityAccess, CommandSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String ID_TAG = "id";
   public static final String PASSENGERS_TAG = "Passengers";
   private static final AtomicInteger ENTITY_COUNTER = new AtomicInteger();
   private static final List<ItemStack> EMPTY_LIST = Collections.emptyList();
   public static final int BOARDING_COOLDOWN = 60;
   public static final int TOTAL_AIR_SUPPLY = 300;
   public static final int MAX_ENTITY_TAG_COUNT = 1024;
   public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW = 0.5000001;
   public static final float BREATHING_DISTANCE_BELOW_EYES = 0.11111111F;
   public static final int BASE_TICKS_REQUIRED_TO_FREEZE = 140;
   public static final int FREEZE_HURT_FREQUENCY = 40;
   private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   private static final double WATER_FLOW_SCALE = 0.014;
   private static final double LAVA_FAST_FLOW_SCALE = 0.007;
   private static final double LAVA_SLOW_FLOW_SCALE = 0.0023333333333333335;
   public static final String UUID_TAG = "UUID";
   private static double viewScale = 1.0;
   private final EntityType<?> type;
   private int id = ENTITY_COUNTER.incrementAndGet();
   public boolean blocksBuilding;
   private ImmutableList<Entity> passengers = ImmutableList.of();
   protected int boardingCooldown;
   @Nullable
   private Entity vehicle;
   public Level level;
   public double xo;
   public double yo;
   public double zo;
   private Vec3 position;
   private BlockPos blockPosition;
   private ChunkPos chunkPosition;
   private Vec3 deltaMovement = Vec3.ZERO;
   private float yRot;
   private float xRot;
   public float yRotO;
   public float xRotO;
   private AABB bb = INITIAL_AABB;
   protected boolean onGround;
   public boolean horizontalCollision;
   public boolean verticalCollision;
   public boolean verticalCollisionBelow;
   public boolean minorHorizontalCollision;
   public boolean hurtMarked;
   protected Vec3 stuckSpeedMultiplier = Vec3.ZERO;
   @Nullable
   private Entity.RemovalReason removalReason;
   public static final float DEFAULT_BB_WIDTH = 0.6F;
   public static final float DEFAULT_BB_HEIGHT = 1.8F;
   public float walkDistO;
   public float walkDist;
   public float moveDist;
   public float flyDist;
   public float fallDistance;
   private float nextStep = 1.0F;
   public double xOld;
   public double yOld;
   public double zOld;
   private float maxUpStep;
   public boolean noPhysics;
   protected final RandomSource random = RandomSource.create();
   public int tickCount;
   private int remainingFireTicks = -this.getFireImmuneTicks();
   protected boolean wasTouchingWater;
   protected Object2DoubleMap<TagKey<Fluid>> fluidHeight = new Object2DoubleArrayMap<>(2);
   protected boolean wasEyeInWater;
   private final Set<TagKey<Fluid>> fluidOnEyes = new HashSet<>();
   public int invulnerableTime;
   protected boolean firstTick = true;
   protected final SynchedEntityData entityData;
   protected static final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BYTE);
   protected static final int FLAG_ONFIRE = 0;
   private static final int FLAG_SHIFT_KEY_DOWN = 1;
   private static final int FLAG_SPRINTING = 3;
   private static final int FLAG_SWIMMING = 4;
   private static final int FLAG_INVISIBLE = 5;
   protected static final int FLAG_GLOWING = 6;
   protected static final int FLAG_FALL_FLYING = 7;
   private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Optional<Component>> DATA_CUSTOM_NAME = SynchedEntityData.defineId(
      Entity.class, EntityDataSerializers.OPTIONAL_COMPONENT
   );
   private static final EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_SILENT = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_NO_GRAVITY = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
   protected static final EntityDataAccessor<Pose> DATA_POSE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.POSE);
   private static final EntityDataAccessor<Integer> DATA_TICKS_FROZEN = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
   private EntityInLevelCallback levelCallback = EntityInLevelCallback.NULL;
   private final VecDeltaCodec packetPositionCodec = new VecDeltaCodec();
   public boolean noCulling;
   public boolean hasImpulse;
   private int portalCooldown;
   protected boolean isInsidePortal;
   protected int portalTime;
   protected BlockPos portalEntrancePos;
   private boolean invulnerable;
   protected UUID uuid = Mth.createInsecureUUID(this.random);
   protected String stringUUID = this.uuid.toString();
   private boolean hasGlowingTag;
   private final Set<String> tags = Sets.newHashSet();
   private final double[] pistonDeltas = new double[]{0.0, 0.0, 0.0};
   private long pistonDeltasGameTime;
   private EntityDimensions dimensions;
   private float eyeHeight;
   public boolean isInPowderSnow;
   public boolean wasInPowderSnow;
   public boolean wasOnFire;
   private float crystalSoundIntensity;
   private int lastCrystalSoundPlayTick;
   private boolean hasVisualFire;
   @Nullable
   private BlockState feetBlockState = null;

   public Entity(EntityType<?> param0, Level param1) {
      this.type = param0;
      this.level = param1;
      this.dimensions = param0.getDimensions();
      this.position = Vec3.ZERO;
      this.blockPosition = BlockPos.ZERO;
      this.chunkPosition = ChunkPos.ZERO;
      this.entityData = new SynchedEntityData(this);
      this.entityData.define(DATA_SHARED_FLAGS_ID, (byte)0);
      this.entityData.define(DATA_AIR_SUPPLY_ID, this.getMaxAirSupply());
      this.entityData.define(DATA_CUSTOM_NAME_VISIBLE, false);
      this.entityData.define(DATA_CUSTOM_NAME, Optional.empty());
      this.entityData.define(DATA_SILENT, false);
      this.entityData.define(DATA_NO_GRAVITY, false);
      this.entityData.define(DATA_POSE, Pose.STANDING);
      this.entityData.define(DATA_TICKS_FROZEN, 0);
      this.defineSynchedData();
      this.setPos(0.0, 0.0, 0.0);
      this.eyeHeight = this.getEyeHeight(Pose.STANDING, this.dimensions);
   }

   public boolean isColliding(BlockPos param0, BlockState param1) {
      VoxelShape var3 = param1.getCollisionShape(this.level, param0, CollisionContext.of(this));
      VoxelShape var4 = var3.move(param0.getX(), param0.getY(), param0.getZ());
      return Shapes.joinIsNotEmpty(var4, Shapes.create(this.getBoundingBox()), BooleanOp.AND);
   }

   public int getTeamColor() {
      Team var1 = this.getTeam();
      return var1 != null && var1.getColor().getColor() != null ? var1.getColor().getColor() : 16777215;
   }

   public boolean isSpectator() {
      return false;
   }

   public final void unRide() {
      if (this.isVehicle()) {
         this.ejectPassengers();
      }

      if (this.isPassenger()) {
         this.stopRiding();
      }
   }

   public void syncPacketPositionCodec(double param0, double param2, double param4) {
      this.packetPositionCodec.setBase(new Vec3(param0, param2, param4));
   }

   public VecDeltaCodec getPositionCodec() {
      return this.packetPositionCodec;
   }

   public EntityType<?> getType() {
      return this.type;
   }

   @Override
   public int getId() {
      return this.id;
   }

   public void setId(int param0) {
      this.id = param0;
   }

   public Set<String> getTags() {
      return this.tags;
   }

   public boolean addTag(String param0) {
      return this.tags.size() >= 1024 ? false : this.tags.add(param0);
   }

   public boolean removeTag(String param0) {
      return this.tags.remove(param0);
   }

   public void kill() {
      this.remove(Entity.RemovalReason.KILLED);
      this.gameEvent(GameEvent.ENTITY_DIE);
   }

   public final void discard() {
      this.remove(Entity.RemovalReason.DISCARDED);
   }

   protected abstract void defineSynchedData();

   public SynchedEntityData getEntityData() {
      return this.entityData;
   }

   @Override
   public boolean equals(Object param0) {
      return param0 instanceof Entity ? ((Entity)param0).id == this.id : false;
   }

   @Override
   public int hashCode() {
      return this.id;
   }

   public void remove(Entity.RemovalReason param0) {
      this.setRemoved(param0);
   }

   public void onClientRemoval() {
   }

   public void setPose(Pose param0) {
      this.entityData.set(DATA_POSE, param0);
   }

   public Pose getPose() {
      return this.entityData.get(DATA_POSE);
   }

   public boolean hasPose(Pose param0) {
      return this.getPose() == param0;
   }

   public boolean closerThan(Entity param0, double param1) {
      return this.position().closerThan(param0.position(), param1);
   }

   public boolean closerThan(Entity param0, double param1, double param3) {
      double var6 = param0.getX() - this.getX();
      double var8 = param0.getY() - this.getY();
      double var10 = param0.getZ() - this.getZ();
      return Mth.lengthSquared(var6, var10) < Mth.square(param1) && Mth.square(var8) < Mth.square(param3);
   }

   protected void setRot(float param0, float param1) {
      this.setYRot(param0 % 360.0F);
      this.setXRot(param1 % 360.0F);
   }

   public final void setPos(Vec3 param0) {
      this.setPos(param0.x(), param0.y(), param0.z());
   }

   public void setPos(double param0, double param2, double param4) {
      this.setPosRaw(param0, param2, param4);
      this.setBoundingBox(this.makeBoundingBox());
   }

   protected AABB makeBoundingBox() {
      return this.dimensions.makeBoundingBox(this.position);
   }

   protected void reapplyPosition() {
      this.setPos(this.position.x, this.position.y, this.position.z);
   }

   public void turn(double param0, double param2) {
      float var5 = (float)param2 * 0.15F;
      float var6 = (float)param0 * 0.15F;
      this.setXRot(this.getXRot() + var5);
      this.setYRot(this.getYRot() + var6);
      this.setXRot(Mth.clamp(this.getXRot(), -90.0F, 90.0F));
      this.xRotO += var5;
      this.yRotO += var6;
      this.xRotO = Mth.clamp(this.xRotO, -90.0F, 90.0F);
      if (this.vehicle != null) {
         this.vehicle.onPassengerTurned(this);
      }
   }

   public void tick() {
      this.baseTick();
   }

   public void baseTick() {
      this.level.getProfiler().push("entityBaseTick");
      this.feetBlockState = null;
      if (this.isPassenger() && this.getVehicle().isRemoved()) {
         this.stopRiding();
      }

      if (this.boardingCooldown > 0) {
         this.boardingCooldown--;
      }

      this.walkDistO = this.walkDist;
      this.xRotO = this.getXRot();
      this.yRotO = this.getYRot();
      this.handleNetherPortal();
      if (this.canSpawnSprintParticle()) {
         this.spawnSprintParticle();
      }

      this.wasInPowderSnow = this.isInPowderSnow;
      this.isInPowderSnow = false;
      this.updateInWaterStateAndDoFluidPushing();
      this.updateFluidOnEyes();
      this.updateSwimming();
      if (this.level.isClientSide) {
         this.clearFire();
      } else if (this.remainingFireTicks > 0) {
         if (this.fireImmune()) {
            this.setRemainingFireTicks(this.remainingFireTicks - 4);
            if (this.remainingFireTicks < 0) {
               this.clearFire();
            }
         } else {
            if (this.remainingFireTicks % 20 == 0 && !this.isInLava()) {
               this.hurt(this.damageSources().onFire(), 1.0F);
            }

            this.setRemainingFireTicks(this.remainingFireTicks - 1);
         }

         if (this.getTicksFrozen() > 0) {
            this.setTicksFrozen(0);
            this.level.levelEvent(null, 1009, this.blockPosition, 1);
         }
      }

      if (this.isInLava()) {
         this.lavaHurt();
         this.fallDistance *= 0.5F;
      }

      this.checkOutOfWorld();
      if (!this.level.isClientSide) {
         this.setSharedFlagOnFire(this.remainingFireTicks > 0);
      }

      this.firstTick = false;
      this.level.getProfiler().pop();
   }

   public void setSharedFlagOnFire(boolean param0) {
      this.setSharedFlag(0, param0 || this.hasVisualFire);
   }

   public void checkOutOfWorld() {
      if (this.getY() < this.level.getMinBuildHeight() - 64) {
         this.outOfWorld();
      }
   }

   public void setPortalCooldown() {
      this.portalCooldown = this.getDimensionChangingDelay();
   }

   public boolean isOnPortalCooldown() {
      return this.portalCooldown > 0;
   }

   protected void processPortalCooldown() {
      if (this.isOnPortalCooldown()) {
         this.portalCooldown--;
      }
   }

   public int getPortalWaitTime() {
      return 0;
   }

   public void lavaHurt() {
      if (!this.fireImmune()) {
         this.setSecondsOnFire(15);
         if (this.hurt(this.damageSources().lava(), 4.0F)) {
            this.playSound(SoundEvents.GENERIC_BURN, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
         }
      }
   }

   public void setSecondsOnFire(int param0) {
      int var2 = param0 * 20;
      if (this instanceof LivingEntity) {
         var2 = ProtectionEnchantment.getFireAfterDampener((LivingEntity)this, var2);
      }

      if (this.remainingFireTicks < var2) {
         this.setRemainingFireTicks(var2);
      }
   }

   public void setRemainingFireTicks(int param0) {
      this.remainingFireTicks = param0;
   }

   public int getRemainingFireTicks() {
      return this.remainingFireTicks;
   }

   public void clearFire() {
      this.setRemainingFireTicks(0);
   }

   protected void outOfWorld() {
      this.discard();
   }

   public boolean isFree(double param0, double param2, double param4) {
      return this.isFree(this.getBoundingBox().move(param0, param2, param4));
   }

   private boolean isFree(AABB param0) {
      return this.level.noCollision(this, param0) && !this.level.containsAnyLiquid(param0);
   }

   public void setOnGround(boolean param0) {
      this.onGround = param0;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public void move(MoverType param0, Vec3 param1) {
      if (this.noPhysics) {
         this.setPos(this.getX() + param1.x, this.getY() + param1.y, this.getZ() + param1.z);
      } else {
         this.wasOnFire = this.isOnFire();
         if (param0 == MoverType.PISTON) {
            param1 = this.limitPistonMovement(param1);
            if (param1.equals(Vec3.ZERO)) {
               return;
            }
         }

         this.level.getProfiler().push("move");
         if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7) {
            param1 = param1.multiply(this.stuckSpeedMultiplier);
            this.stuckSpeedMultiplier = Vec3.ZERO;
            this.setDeltaMovement(Vec3.ZERO);
         }

         param1 = this.maybeBackOffFromEdge(param1, param0);
         Vec3 var3 = this.collide(param1);
         double var4 = var3.lengthSqr();
         if (var4 > 1.0E-7) {
            if (this.fallDistance != 0.0F && var4 >= 1.0) {
               BlockHitResult var6 = this.level
                  .clip(new ClipContext(this.position(), this.position().add(var3), ClipContext.Block.FALLDAMAGE_RESETTING, ClipContext.Fluid.WATER, this));
               if (var6.getType() != HitResult.Type.MISS) {
                  this.resetFallDistance();
               }
            }

            this.setPos(this.getX() + var3.x, this.getY() + var3.y, this.getZ() + var3.z);
         }

         this.level.getProfiler().pop();
         this.level.getProfiler().push("rest");
         boolean var6 = !Mth.equal(param1.x, var3.x);
         boolean var7 = !Mth.equal(param1.z, var3.z);
         this.horizontalCollision = var6 || var7;
         this.verticalCollision = param1.y != var3.y;
         this.verticalCollisionBelow = this.verticalCollision && param1.y < 0.0;
         if (this.horizontalCollision) {
            this.minorHorizontalCollision = this.isHorizontalCollisionMinor(var3);
         } else {
            this.minorHorizontalCollision = false;
         }

         this.onGround = this.verticalCollision && param1.y < 0.0;
         BlockPos var8 = this.getOnPosLegacy();
         BlockState var9 = this.level.getBlockState(var8);
         this.checkFallDamage(var3.y, this.onGround, var9, var8);
         if (this.isRemoved()) {
            this.level.getProfiler().pop();
         } else {
            if (this.horizontalCollision) {
               Vec3 var10 = this.getDeltaMovement();
               this.setDeltaMovement(var6 ? 0.0 : var10.x, var10.y, var7 ? 0.0 : var10.z);
            }

            Block var10 = var9.getBlock();
            if (param1.y != var3.y) {
               var10.updateEntityAfterFallOn(this.level, this);
            }

            if (this.onGround) {
               var10.stepOn(this.level, var8, var9, this);
            }

            Entity.MovementEmission var11 = this.getMovementEmission();
            if (var11.emitsAnything() && !this.isPassenger()) {
               double var12 = var3.x;
               double var14 = var3.y;
               double var16 = var3.z;
               this.flyDist = this.flyDist + (float)(var3.length() * 0.6);
               boolean var18 = var9.is(BlockTags.CLIMBABLE) || var9.is(Blocks.POWDER_SNOW);
               if (!var18) {
                  var14 = 0.0;
               }

               this.walkDist = this.walkDist + (float)var3.horizontalDistance() * 0.6F;
               this.moveDist = this.moveDist + (float)Math.sqrt(var12 * var12 + var14 * var14 + var16 * var16) * 0.6F;
               if (this.moveDist > this.nextStep && !var9.isAir()) {
                  this.nextStep = this.nextStep();
                  if (this.isInWater()) {
                     if (var11.emitsSounds()) {
                        Entity var19 = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
                        float var20 = var19 == this ? 0.35F : 0.4F;
                        Vec3 var21 = var19.getDeltaMovement();
                        float var22 = Math.min(1.0F, (float)Math.sqrt(var21.x * var21.x * 0.2F + var21.y * var21.y + var21.z * var21.z * 0.2F) * var20);
                        this.playSwimSound(var22);
                     }

                     if (var11.emitsEvents()) {
                        this.gameEvent(GameEvent.SWIM);
                     }
                  } else {
                     if (var11.emitsSounds()) {
                        this.playAmethystStepSound(var9);
                        this.playStepSound(var8, var9);
                     }

                     if (var11.emitsEvents() && (this.onGround || param1.y == 0.0 || this.isInPowderSnow || var18)) {
                        this.level.gameEvent(GameEvent.STEP, this.position, GameEvent.Context.of(this, this.getBlockStateOn()));
                     }
                  }
               } else if (var9.isAir()) {
                  this.processFlappingMovement();
               }
            }

            this.tryCheckInsideBlocks();
            float var12 = this.getBlockSpeedFactor();
            this.setDeltaMovement(this.getDeltaMovement().multiply(var12, 1.0, var12));
            if (this.level
               .getBlockStatesIfLoaded(this.getBoundingBox().deflate(1.0E-6))
               .noneMatch(l_param0 -> l_param0.is(BlockTags.FIRE) || l_param0.is(Blocks.LAVA))) {
               if (this.remainingFireTicks <= 0) {
                  this.setRemainingFireTicks(-this.getFireImmuneTicks());
               }

               if (this.wasOnFire && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
                  this.playEntityOnFireExtinguishedSound();
               }
            }

            if (this.isOnFire() && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
               this.setRemainingFireTicks(-this.getFireImmuneTicks());
            }

            this.level.getProfiler().pop();
         }
      }
   }

   protected boolean isHorizontalCollisionMinor(Vec3 param0) {
      return false;
   }

   protected void tryCheckInsideBlocks() {
      try {
         this.checkInsideBlocks();
      } catch (Throwable var1) {
         CrashReport var2 = CrashReport.forThrowable(var1, "Checking entity block collision");
         CrashReportCategory var3 = var2.addCategory("Entity being checked for collision");
         this.fillCrashReportCategory(var3);
         throw new ReportedException(var2);
      }
   }

   protected void playEntityOnFireExtinguishedSound() {
      this.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
   }

   public void extinguishFire() {
      if (!this.level.isClientSide && this.wasOnFire) {
         this.playEntityOnFireExtinguishedSound();
      }

      this.clearFire();
   }

   protected void processFlappingMovement() {
      if (this.isFlapping()) {
         this.onFlap();
         if (this.getMovementEmission().emitsEvents()) {
            this.gameEvent(GameEvent.FLAP);
         }
      }
   }

   @Deprecated
   public BlockPos getOnPosLegacy() {
      return this.getOnPos(0.2F);
   }

   public BlockPos getOnPos() {
      return this.getOnPos(1.0E-5F);
   }

   private BlockPos getOnPos(float param0) {
      int var2 = Mth.floor(this.position.x);
      int var3 = Mth.floor(this.position.y - param0);
      int var4 = Mth.floor(this.position.z);
      BlockPos var5 = new BlockPos(var2, var3, var4);
      if (this.level.getBlockState(var5).isAir()) {
         BlockPos var6 = var5.below();
         BlockState var7 = this.level.getBlockState(var6);
         if (var7.is(BlockTags.FENCES) || var7.is(BlockTags.WALLS) || var7.getBlock() instanceof FenceGateBlock) {
            return var6;
         }
      }

      return var5;
   }

   protected float getBlockJumpFactor() {
      float var1 = this.level.getBlockState(this.blockPosition()).getBlock().getJumpFactor();
      float var2 = this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
      return var1 == 1.0 ? var2 : var1;
   }

   protected float getBlockSpeedFactor() {
      BlockState var1 = this.level.getBlockState(this.blockPosition());
      float var2 = var1.getBlock().getSpeedFactor();
      if (!var1.is(Blocks.WATER) && !var1.is(Blocks.BUBBLE_COLUMN)) {
         return var2 == 1.0 ? this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getSpeedFactor() : var2;
      } else {
         return var2;
      }
   }

   protected BlockPos getBlockPosBelowThatAffectsMyMovement() {
      return BlockPos.containing(this.position.x, this.getBoundingBox().minY - 0.5000001, this.position.z);
   }

   protected Vec3 maybeBackOffFromEdge(Vec3 param0, MoverType param1) {
      return param0;
   }

   protected Vec3 limitPistonMovement(Vec3 param0) {
      if (param0.lengthSqr() <= 1.0E-7) {
         return param0;
      }

      long var2 = this.level.getGameTime();
      if (var2 != this.pistonDeltasGameTime) {
         Arrays.fill(this.pistonDeltas, 0.0);
         this.pistonDeltasGameTime = var2;
      }

      if (param0.x != 0.0) {
         double var4 = this.applyPistonMovementRestriction(Direction.Axis.X, param0.x);
         return Math.abs(var4) <= 1.0E-5F ? Vec3.ZERO : new Vec3(var4, 0.0, 0.0);
      } else if (param0.y != 0.0) {
         double var4 = this.applyPistonMovementRestriction(Direction.Axis.Y, param0.y);
         return Math.abs(var4) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, var4, 0.0);
      } else if (param0.z != 0.0) {
         double var4 = this.applyPistonMovementRestriction(Direction.Axis.Z, param0.z);
         return Math.abs(var4) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, 0.0, var4);
      } else {
         return Vec3.ZERO;
      }
   }

   private double applyPistonMovementRestriction(Direction.Axis param0, double param1) {
      int var4 = param0.ordinal();
      double var5 = Mth.clamp(param1 + this.pistonDeltas[var4], -0.51, 0.51);
      param1 = var5 - this.pistonDeltas[var4];
      this.pistonDeltas[var4] = var5;
      return param1;
   }

   private Vec3 collide(Vec3 param0) {
      AABB var2 = this.getBoundingBox();
      List<VoxelShape> var3 = this.level.getEntityCollisions(this, var2.expandTowards(param0));
      Vec3 var4 = param0.lengthSqr() == 0.0 ? param0 : collideBoundingBox(this, param0, var2, this.level, var3);
      boolean var5 = param0.x != var4.x;
      boolean var6 = param0.y != var4.y;
      boolean var7 = param0.z != var4.z;
      boolean var8 = this.onGround || var6 && param0.y < 0.0;
      if (this.maxUpStep() > 0.0F && var8 && (var5 || var7)) {
         Vec3 var9 = collideBoundingBox(this, new Vec3(param0.x, this.maxUpStep(), param0.z), var2, this.level, var3);
         Vec3 var10 = collideBoundingBox(this, new Vec3(0.0, this.maxUpStep(), 0.0), var2.expandTowards(param0.x, 0.0, param0.z), this.level, var3);
         if (var10.y < this.maxUpStep()) {
            Vec3 var11 = collideBoundingBox(this, new Vec3(param0.x, 0.0, param0.z), var2.move(var10), this.level, var3).add(var10);
            if (var11.horizontalDistanceSqr() > var9.horizontalDistanceSqr()) {
               var9 = var11;
            }
         }

         if (var9.horizontalDistanceSqr() > var4.horizontalDistanceSqr()) {
            return var9.add(collideBoundingBox(this, new Vec3(0.0, -var9.y + param0.y, 0.0), var2.move(var9), this.level, var3));
         }
      }

      return var4;
   }

   public static Vec3 collideBoundingBox(@Nullable Entity param0, Vec3 param1, AABB param2, Level param3, List<VoxelShape> param4) {
      Builder<VoxelShape> var5 = ImmutableList.builderWithExpectedSize(param4.size() + 1);
      if (!param4.isEmpty()) {
         var5.addAll(param4);
      }

      WorldBorder var6 = param3.getWorldBorder();
      boolean var7 = param0 != null && var6.isInsideCloseToBorder(param0, param2.expandTowards(param1));
      if (var7) {
         var5.add(var6.getCollisionShape());
      }

      var5.addAll(param3.getBlockCollisions(param0, param2.expandTowards(param1)));
      return collideWithShapes(param1, param2, var5.build());
   }

   private static Vec3 collideWithShapes(Vec3 param0, AABB param1, List<VoxelShape> param2) {
      if (param2.isEmpty()) {
         return param0;
      }

      double var3 = param0.x;
      double var5 = param0.y;
      double var7 = param0.z;
      if (var5 != 0.0) {
         var5 = Shapes.collide(Direction.Axis.Y, param1, param2, var5);
         if (var5 != 0.0) {
            param1 = param1.move(0.0, var5, 0.0);
         }
      }

      boolean var9 = Math.abs(var3) < Math.abs(var7);
      if (var9 && var7 != 0.0) {
         var7 = Shapes.collide(Direction.Axis.Z, param1, param2, var7);
         if (var7 != 0.0) {
            param1 = param1.move(0.0, 0.0, var7);
         }
      }

      if (var3 != 0.0) {
         var3 = Shapes.collide(Direction.Axis.X, param1, param2, var3);
         if (!var9 && var3 != 0.0) {
            param1 = param1.move(var3, 0.0, 0.0);
         }
      }

      if (!var9 && var7 != 0.0) {
         var7 = Shapes.collide(Direction.Axis.Z, param1, param2, var7);
      }

      return new Vec3(var3, var5, var7);
   }

   protected float nextStep() {
      return (int)this.moveDist + 1;
   }

   protected SoundEvent getSwimSound() {
      return SoundEvents.GENERIC_SWIM;
   }

   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.GENERIC_SPLASH;
   }

   protected SoundEvent getSwimHighSpeedSplashSound() {
      return SoundEvents.GENERIC_SPLASH;
   }

   protected void checkInsideBlocks() {
      AABB var1 = this.getBoundingBox();
      BlockPos var2 = BlockPos.containing(var1.minX + 1.0E-7, var1.minY + 1.0E-7, var1.minZ + 1.0E-7);
      BlockPos var3 = BlockPos.containing(var1.maxX - 1.0E-7, var1.maxY - 1.0E-7, var1.maxZ - 1.0E-7);
      if (this.level.hasChunksAt(var2, var3)) {
         BlockPos.MutableBlockPos var4 = new BlockPos.MutableBlockPos();

         for (int var5 = var2.getX(); var5 <= var3.getX(); var5++) {
            for (int var6 = var2.getY(); var6 <= var3.getY(); var6++) {
               for (int var7 = var2.getZ(); var7 <= var3.getZ(); var7++) {
                  var4.set(var5, var6, var7);
                  BlockState var8 = this.level.getBlockState(var4);

                  try {
                     var8.entityInside(this.level, var4, this);
                     this.onInsideBlock(var8);
                  } catch (Throwable var9) {
                     CrashReport var10 = CrashReport.forThrowable(var9, "Colliding entity with block");
                     CrashReportCategory var11 = var10.addCategory("Block being collided with");
                     CrashReportCategory.populateBlockDetails(var11, this.level, var4, var8);
                     throw new ReportedException(var10);
                  }
               }
            }
         }
      }
   }

   protected void onInsideBlock(BlockState param0) {
   }

   public void gameEvent(GameEvent param0, @Nullable Entity param1) {
      this.level.gameEvent(param1, param0, this.position);
   }

   public void gameEvent(GameEvent param0) {
      this.gameEvent(param0, this);
   }

   protected void playStepSound(BlockPos param0, BlockState param1) {
      BlockState var3 = this.level.getBlockState(param0.above());
      boolean var4 = var3.is(BlockTags.INSIDE_STEP_SOUND_BLOCKS);
      if (var4 || !param1.getMaterial().isLiquid()) {
         SoundType var5 = var4 ? var3.getSoundType() : param1.getSoundType();
         this.playSound(var5.getStepSound(), var5.getVolume() * 0.15F, var5.getPitch());
      }
   }

   private void playAmethystStepSound(BlockState param0) {
      if (param0.is(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.tickCount >= this.lastCrystalSoundPlayTick + 20) {
         this.crystalSoundIntensity = this.crystalSoundIntensity * (float)Math.pow(0.997, this.tickCount - this.lastCrystalSoundPlayTick);
         this.crystalSoundIntensity = Math.min(1.0F, this.crystalSoundIntensity + 0.07F);
         float var2 = 0.5F + this.crystalSoundIntensity * this.random.nextFloat() * 1.2F;
         float var3 = 0.1F + this.crystalSoundIntensity * 1.2F;
         this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, var3, var2);
         this.lastCrystalSoundPlayTick = this.tickCount;
      }
   }

   protected void playSwimSound(float param0) {
      this.playSound(this.getSwimSound(), param0, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
   }

   protected void onFlap() {
   }

   protected boolean isFlapping() {
      return false;
   }

   public void playSound(SoundEvent param0, float param1, float param2) {
      if (!this.isSilent()) {
         this.level.playSound(null, this.getX(), this.getY(), this.getZ(), param0, this.getSoundSource(), param1, param2);
      }
   }

   public void playSound(SoundEvent param0) {
      if (!this.isSilent()) {
         this.playSound(param0, 1.0F, 1.0F);
      }
   }

   public boolean isSilent() {
      return this.entityData.get(DATA_SILENT);
   }

   public void setSilent(boolean param0) {
      this.entityData.set(DATA_SILENT, param0);
   }

   public boolean isNoGravity() {
      return this.entityData.get(DATA_NO_GRAVITY);
   }

   public void setNoGravity(boolean param0) {
      this.entityData.set(DATA_NO_GRAVITY, param0);
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.ALL;
   }

   public boolean dampensVibrations() {
      return false;
   }

   protected void checkFallDamage(double param0, boolean param2, BlockState param3, BlockPos param4) {
      if (param2) {
         if (this.fallDistance > 0.0F) {
            param3.getBlock().fallOn(this.level, param3, param4, this, this.fallDistance);
            this.level.gameEvent(GameEvent.HIT_GROUND, this.position, GameEvent.Context.of(this, this.getBlockStateOn()));
         }

         this.resetFallDistance();
      } else if (param0 < 0.0) {
         this.fallDistance -= (float)param0;
      }
   }

   public boolean fireImmune() {
      return this.getType().fireImmune();
   }

   public boolean causeFallDamage(float param0, float param1, DamageSource param2) {
      if (this.type.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
         return false;
      }

      if (this.isVehicle()) {
         for (Entity var5 : this.getPassengers()) {
            var5.causeFallDamage(param0, param1, param2);
         }
      }

      return false;
   }

   public boolean isInWater() {
      return this.wasTouchingWater;
   }

   private boolean isInRain() {
      BlockPos var1 = this.blockPosition();
      return this.level.isRainingAt(var1) || this.level.isRainingAt(BlockPos.containing(var1.getX(), this.getBoundingBox().maxY, var1.getZ()));
   }

   private boolean isInBubbleColumn() {
      return this.level.getBlockState(this.blockPosition()).is(Blocks.BUBBLE_COLUMN);
   }

   public boolean isInWaterOrRain() {
      return this.isInWater() || this.isInRain();
   }

   public boolean isInWaterRainOrBubble() {
      return this.isInWater() || this.isInRain() || this.isInBubbleColumn();
   }

   public boolean isInWaterOrBubble() {
      return this.isInWater() || this.isInBubbleColumn();
   }

   public boolean isUnderWater() {
      return this.wasEyeInWater && this.isInWater();
   }

   public void updateSwimming() {
      if (this.isSwimming()) {
         this.setSwimming(this.isSprinting() && this.isInWater() && !this.isPassenger());
      } else {
         this.setSwimming(this.isSprinting() && this.isUnderWater() && !this.isPassenger() && this.level.getFluidState(this.blockPosition).is(FluidTags.WATER));
      }
   }

   protected boolean updateInWaterStateAndDoFluidPushing() {
      this.fluidHeight.clear();
      this.updateInWaterStateAndDoWaterCurrentPushing();
      double var1 = this.level.dimensionType().ultraWarm() ? 0.007 : 0.0023333333333333335;
      boolean var3 = this.updateFluidHeightAndDoFluidPushing(FluidTags.LAVA, var1);
      return this.isInWater() || var3;
   }

   void updateInWaterStateAndDoWaterCurrentPushing() {
      if (this.getVehicle() instanceof Boat var1 && !var1.isUnderWater()) {
         this.wasTouchingWater = false;
      } else if (this.updateFluidHeightAndDoFluidPushing(FluidTags.WATER, 0.014)) {
         if (!this.wasTouchingWater && !this.firstTick) {
            this.doWaterSplashEffect();
         }

         this.resetFallDistance();
         this.wasTouchingWater = true;
         this.clearFire();
      } else {
         this.wasTouchingWater = false;
      }
   }

   private void updateFluidOnEyes() {
      this.wasEyeInWater = this.isEyeInFluid(FluidTags.WATER);
      this.fluidOnEyes.clear();
      double var1 = this.getEyeY() - 0.11111111F;
      if (!(this.getVehicle() instanceof Boat var4 && !var4.isUnderWater() && var4.getBoundingBox().maxY >= var1 && var4.getBoundingBox().minY <= var1)) {
         BlockPos var4 = BlockPos.containing(this.getX(), var1, this.getZ());
         FluidState var5 = this.level.getFluidState(var4);
         double var6 = var4.getY() + var5.getHeight(this.level, var4);
         if (var6 > var1) {
            var5.getTags().forEach(this.fluidOnEyes::add);
         }
      }
   }

   protected void doWaterSplashEffect() {
      Entity var1 = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
      float var2 = var1 == this ? 0.2F : 0.9F;
      Vec3 var3 = var1.getDeltaMovement();
      float var4 = Math.min(1.0F, (float)Math.sqrt(var3.x * var3.x * 0.2F + var3.y * var3.y + var3.z * var3.z * 0.2F) * var2);
      if (var4 < 0.25F) {
         this.playSound(this.getSwimSplashSound(), var4, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
      } else {
         this.playSound(this.getSwimHighSpeedSplashSound(), var4, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
      }

      float var5 = Mth.floor(this.getY());

      for (int var6 = 0; var6 < 1.0F + this.dimensions.width * 20.0F; var6++) {
         double var7 = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width;
         double var9 = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width;
         this.level
            .addParticle(ParticleTypes.BUBBLE, this.getX() + var7, var5 + 1.0F, this.getZ() + var9, var3.x, var3.y - this.random.nextDouble() * 0.2F, var3.z);
      }

      for (int var6 = 0; var6 < 1.0F + this.dimensions.width * 20.0F; var6++) {
         double var7 = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width;
         double var9 = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width;
         this.level.addParticle(ParticleTypes.SPLASH, this.getX() + var7, var5 + 1.0F, this.getZ() + var9, var3.x, var3.y, var3.z);
      }

      this.gameEvent(GameEvent.SPLASH);
   }

   @Deprecated
   protected BlockState getBlockStateOnLegacy() {
      return this.level.getBlockState(this.getOnPosLegacy());
   }

   public BlockState getBlockStateOn() {
      return this.level.getBlockState(this.getOnPos());
   }

   public boolean canSpawnSprintParticle() {
      return this.isSprinting() && !this.isInWater() && !this.isSpectator() && !this.isCrouching() && !this.isInLava() && this.isAlive();
   }

   protected void spawnSprintParticle() {
      int var1 = Mth.floor(this.getX());
      int var2 = Mth.floor(this.getY() - 0.2F);
      int var3 = Mth.floor(this.getZ());
      BlockPos var4 = new BlockPos(var1, var2, var3);
      BlockState var5 = this.level.getBlockState(var4);
      if (var5.getRenderShape() != RenderShape.INVISIBLE) {
         Vec3 var6 = this.getDeltaMovement();
         this.level
            .addParticle(
               new BlockParticleOption(ParticleTypes.BLOCK, var5),
               this.getX() + (this.random.nextDouble() - 0.5) * this.dimensions.width,
               this.getY() + 0.1,
               this.getZ() + (this.random.nextDouble() - 0.5) * this.dimensions.width,
               var6.x * -4.0,
               1.5,
               var6.z * -4.0
            );
      }
   }

   public boolean isEyeInFluid(TagKey<Fluid> param0) {
      return this.fluidOnEyes.contains(param0);
   }

   public boolean isInLava() {
      return !this.firstTick && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
   }

   public void moveRelative(float param0, Vec3 param1) {
      Vec3 var3 = getInputVector(param1, param0, this.getYRot());
      this.setDeltaMovement(this.getDeltaMovement().add(var3));
   }

   private static Vec3 getInputVector(Vec3 param0, float param1, float param2) {
      double var3 = param0.lengthSqr();
      if (var3 < 1.0E-7) {
         return Vec3.ZERO;
      }

      Vec3 var5 = (var3 > 1.0 ? param0.normalize() : param0).scale(param1);
      float var6 = Mth.sin(param2 * (float) (Math.PI / 180.0));
      float var7 = Mth.cos(param2 * (float) (Math.PI / 180.0));
      return new Vec3(var5.x * var7 - var5.z * var6, var5.y, var5.z * var7 + var5.x * var6);
   }

   @Deprecated
   public float getLightLevelDependentMagicValue() {
      return this.level.hasChunkAt(this.getBlockX(), this.getBlockZ())
         ? this.level.getLightLevelDependentMagicValue(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ()))
         : 0.0F;
   }

   public void absMoveTo(double param0, double param2, double param4, float param6, float param7) {
      this.absMoveTo(param0, param2, param4);
      this.setYRot(param6 % 360.0F);
      this.setXRot(Mth.clamp(param7, -90.0F, 90.0F) % 360.0F);
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   public void absMoveTo(double param0, double param2, double param4) {
      double var7 = Mth.clamp(param0, -3.0E7, 3.0E7);
      double var9 = Mth.clamp(param4, -3.0E7, 3.0E7);
      this.xo = var7;
      this.yo = param2;
      this.zo = var9;
      this.setPos(var7, param2, var9);
   }

   public void moveTo(Vec3 param0) {
      this.moveTo(param0.x, param0.y, param0.z);
   }

   public void moveTo(double param0, double param2, double param4) {
      this.moveTo(param0, param2, param4, this.getYRot(), this.getXRot());
   }

   public void moveTo(BlockPos param0, float param1, float param2) {
      this.moveTo(param0.getX() + 0.5, param0.getY(), param0.getZ() + 0.5, param1, param2);
   }

   public void moveTo(double param0, double param2, double param4, float param6, float param7) {
      this.setPosRaw(param0, param2, param4);
      this.setYRot(param6);
      this.setXRot(param7);
      this.setOldPosAndRot();
      this.reapplyPosition();
   }

   public final void setOldPosAndRot() {
      double var1 = this.getX();
      double var3 = this.getY();
      double var5 = this.getZ();
      this.xo = var1;
      this.yo = var3;
      this.zo = var5;
      this.xOld = var1;
      this.yOld = var3;
      this.zOld = var5;
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   public float distanceTo(Entity param0) {
      float var2 = (float)(this.getX() - param0.getX());
      float var3 = (float)(this.getY() - param0.getY());
      float var4 = (float)(this.getZ() - param0.getZ());
      return Mth.sqrt(var2 * var2 + var3 * var3 + var4 * var4);
   }

   public double distanceToSqr(double param0, double param2, double param4) {
      double var7 = this.getX() - param0;
      double var9 = this.getY() - param2;
      double var11 = this.getZ() - param4;
      return var7 * var7 + var9 * var9 + var11 * var11;
   }

   public double distanceToSqr(Entity param0) {
      return this.distanceToSqr(param0.position());
   }

   public double distanceToSqr(Vec3 param0) {
      double var2 = this.getX() - param0.x;
      double var4 = this.getY() - param0.y;
      double var6 = this.getZ() - param0.z;
      return var2 * var2 + var4 * var4 + var6 * var6;
   }

   public void playerTouch(Player param0) {
   }

   public void push(Entity param0) {
      if (!this.isPassengerOfSameVehicle(param0)) {
         if (!param0.noPhysics && !this.noPhysics) {
            double var2 = param0.getX() - this.getX();
            double var4 = param0.getZ() - this.getZ();
            double var6 = Mth.absMax(var2, var4);
            if (var6 >= 0.01F) {
               var6 = Math.sqrt(var6);
               var2 /= var6;
               var4 /= var6;
               double var8 = 1.0 / var6;
               if (var8 > 1.0) {
                  var8 = 1.0;
               }

               var2 *= var8;
               var4 *= var8;
               var2 *= 0.05F;
               var4 *= 0.05F;
               if (!this.isVehicle() && this.isPushable()) {
                  this.push(-var2, 0.0, -var4);
               }

               if (!param0.isVehicle() && param0.isPushable()) {
                  param0.push(var2, 0.0, var4);
               }
            }
         }
      }
   }

   public void push(double param0, double param2, double param4) {
      this.setDeltaMovement(this.getDeltaMovement().add(param0, param2, param4));
      this.hasImpulse = true;
   }

   protected void markHurt() {
      this.hurtMarked = true;
   }

   public boolean hurt(DamageSource param0, float param1) {
      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      this.markHurt();
      return false;
   }

   public final Vec3 getViewVector(float param0) {
      return this.calculateViewVector(this.getViewXRot(param0), this.getViewYRot(param0));
   }

   public float getViewXRot(float param0) {
      return param0 == 1.0F ? this.getXRot() : Mth.lerp(param0, this.xRotO, this.getXRot());
   }

   public float getViewYRot(float param0) {
      return param0 == 1.0F ? this.getYRot() : Mth.lerp(param0, this.yRotO, this.getYRot());
   }

   protected final Vec3 calculateViewVector(float param0, float param1) {
      float var3 = param0 * (float) (Math.PI / 180.0);
      float var4 = -param1 * (float) (Math.PI / 180.0);
      float var5 = Mth.cos(var4);
      float var6 = Mth.sin(var4);
      float var7 = Mth.cos(var3);
      float var8 = Mth.sin(var3);
      return new Vec3(var6 * var7, -var8, var5 * var7);
   }

   public final Vec3 getUpVector(float param0) {
      return this.calculateUpVector(this.getViewXRot(param0), this.getViewYRot(param0));
   }

   protected final Vec3 calculateUpVector(float param0, float param1) {
      return this.calculateViewVector(param0 - 90.0F, param1);
   }

   public final Vec3 getEyePosition() {
      return new Vec3(this.getX(), this.getEyeY(), this.getZ());
   }

   public final Vec3 getEyePosition(float param0) {
      double var2 = Mth.lerp(param0, this.xo, this.getX());
      double var4 = Mth.lerp(param0, this.yo, this.getY()) + this.getEyeHeight();
      double var6 = Mth.lerp(param0, this.zo, this.getZ());
      return new Vec3(var2, var4, var6);
   }

   public Vec3 getLightProbePosition(float param0) {
      return this.getEyePosition(param0);
   }

   public final Vec3 getPosition(float param0) {
      double var2 = Mth.lerp(param0, this.xo, this.getX());
      double var4 = Mth.lerp(param0, this.yo, this.getY());
      double var6 = Mth.lerp(param0, this.zo, this.getZ());
      return new Vec3(var2, var4, var6);
   }

   public HitResult pick(double param0, float param2, boolean param3) {
      Vec3 var5 = this.getEyePosition(param2);
      Vec3 var6 = this.getViewVector(param2);
      Vec3 var7 = var5.add(var6.x * param0, var6.y * param0, var6.z * param0);
      return this.level.clip(new ClipContext(var5, var7, ClipContext.Block.OUTLINE, param3 ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
   }

   public boolean canBeHitByProjectile() {
      return this.isAlive() && this.isPickable();
   }

   public boolean isPickable() {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   public void awardKillScore(Entity param0, int param1, DamageSource param2) {
      if (param0 instanceof ServerPlayer) {
         CriteriaTriggers.ENTITY_KILLED_PLAYER.trigger((ServerPlayer)param0, this, param2);
      }
   }

   public boolean shouldRender(double param0, double param2, double param4) {
      double var7 = this.getX() - param0;
      double var9 = this.getY() - param2;
      double var11 = this.getZ() - param4;
      double var13 = var7 * var7 + var9 * var9 + var11 * var11;
      return this.shouldRenderAtSqrDistance(var13);
   }

   public boolean shouldRenderAtSqrDistance(double param0) {
      double var3 = this.getBoundingBox().getSize();
      if (Double.isNaN(var3)) {
         var3 = 1.0;
      }

      var3 *= 64.0 * viewScale;
      return param0 < var3 * var3;
   }

   public boolean saveAsPassenger(CompoundTag param0) {
      if (this.removalReason != null && !this.removalReason.shouldSave()) {
         return false;
      }

      String var2 = this.getEncodeId();
      if (var2 == null) {
         return false;
      }

      param0.putString("id", var2);
      this.saveWithoutId(param0);
      return true;
   }

   public boolean save(CompoundTag param0) {
      return this.isPassenger() ? false : this.saveAsPassenger(param0);
   }

   public CompoundTag saveWithoutId(CompoundTag param0) {
      try {
         if (this.vehicle != null) {
            param0.put("Pos", this.newDoubleList(this.vehicle.getX(), this.getY(), this.vehicle.getZ()));
         } else {
            param0.put("Pos", this.newDoubleList(this.getX(), this.getY(), this.getZ()));
         }

         Vec3 var2 = this.getDeltaMovement();
         param0.put("Motion", this.newDoubleList(var2.x, var2.y, var2.z));
         param0.put("Rotation", this.newFloatList(this.getYRot(), this.getXRot()));
         param0.putFloat("FallDistance", this.fallDistance);
         param0.putShort("Fire", (short)this.remainingFireTicks);
         param0.putShort("Air", (short)this.getAirSupply());
         param0.putBoolean("OnGround", this.onGround);
         param0.putBoolean("Invulnerable", this.invulnerable);
         param0.putInt("PortalCooldown", this.portalCooldown);
         param0.putUUID("UUID", this.getUUID());
         Component var3 = this.getCustomName();
         if (var3 != null) {
            param0.putString("CustomName", Component.Serializer.toJson(var3));
         }

         if (this.isCustomNameVisible()) {
            param0.putBoolean("CustomNameVisible", this.isCustomNameVisible());
         }

         if (this.isSilent()) {
            param0.putBoolean("Silent", this.isSilent());
         }

         if (this.isNoGravity()) {
            param0.putBoolean("NoGravity", this.isNoGravity());
         }

         if (this.hasGlowingTag) {
            param0.putBoolean("Glowing", true);
         }

         int var4 = this.getTicksFrozen();
         if (var4 > 0) {
            param0.putInt("TicksFrozen", this.getTicksFrozen());
         }

         if (this.hasVisualFire) {
            param0.putBoolean("HasVisualFire", this.hasVisualFire);
         }

         if (!this.tags.isEmpty()) {
            ListTag var5 = new ListTag();

            for (String var7 : this.tags) {
               var5.add(StringTag.valueOf(var7));
            }

            param0.put("Tags", var5);
         }

         this.addAdditionalSaveData(param0);
         if (this.isVehicle()) {
            ListTag var5 = new ListTag();

            for (Entity var7 : this.getPassengers()) {
               CompoundTag var8 = new CompoundTag();
               if (var7.saveAsPassenger(var8)) {
                  var5.add(var8);
               }
            }

            if (!var5.isEmpty()) {
               param0.put("Passengers", var5);
            }
         }

         return param0;
      } catch (Throwable var2) {
         CrashReport var3 = CrashReport.forThrowable(var2, "Saving entity NBT");
         CrashReportCategory var4 = var3.addCategory("Entity being saved");
         this.fillCrashReportCategory(var4);
         throw new ReportedException(var3);
      }
   }

   public void load(CompoundTag param0) {
      try {
         ListTag var2 = param0.getList("Pos", 6);
         ListTag var3 = param0.getList("Motion", 6);
         ListTag var4 = param0.getList("Rotation", 5);
         double var5 = var3.getDouble(0);
         double var7 = var3.getDouble(1);
         double var9 = var3.getDouble(2);
         this.setDeltaMovement(Math.abs(var5) > 10.0 ? 0.0 : var5, Math.abs(var7) > 10.0 ? 0.0 : var7, Math.abs(var9) > 10.0 ? 0.0 : var9);
         double var11 = 3.0000512E7;
         this.setPosRaw(
            Mth.clamp(var2.getDouble(0), -3.0000512E7, 3.0000512E7),
            Mth.clamp(var2.getDouble(1), -2.0E7, 2.0E7),
            Mth.clamp(var2.getDouble(2), -3.0000512E7, 3.0000512E7)
         );
         this.setYRot(var4.getFloat(0));
         this.setXRot(var4.getFloat(1));
         this.setOldPosAndRot();
         this.setYHeadRot(this.getYRot());
         this.setYBodyRot(this.getYRot());
         this.fallDistance = param0.getFloat("FallDistance");
         this.remainingFireTicks = param0.getShort("Fire");
         if (param0.contains("Air")) {
            this.setAirSupply(param0.getShort("Air"));
         }

         this.onGround = param0.getBoolean("OnGround");
         this.invulnerable = param0.getBoolean("Invulnerable");
         this.portalCooldown = param0.getInt("PortalCooldown");
         if (param0.hasUUID("UUID")) {
            this.uuid = param0.getUUID("UUID");
            this.stringUUID = this.uuid.toString();
         }

         if (!Double.isFinite(this.getX()) || !Double.isFinite(this.getY()) || !Double.isFinite(this.getZ())) {
            throw new IllegalStateException("Entity has invalid position");
         }

         if (Double.isFinite(this.getYRot()) && Double.isFinite(this.getXRot())) {
            this.reapplyPosition();
            this.setRot(this.getYRot(), this.getXRot());
            if (param0.contains("CustomName", 8)) {
               String var13 = param0.getString("CustomName");

               try {
                  this.setCustomName(Component.Serializer.fromJson(var13));
               } catch (Exception var14) {
                  LOGGER.warn("Failed to parse entity custom name {}", var13, var14);
               }
            }

            this.setCustomNameVisible(param0.getBoolean("CustomNameVisible"));
            this.setSilent(param0.getBoolean("Silent"));
            this.setNoGravity(param0.getBoolean("NoGravity"));
            this.setGlowingTag(param0.getBoolean("Glowing"));
            this.setTicksFrozen(param0.getInt("TicksFrozen"));
            this.hasVisualFire = param0.getBoolean("HasVisualFire");
            if (param0.contains("Tags", 9)) {
               this.tags.clear();
               ListTag var13 = param0.getList("Tags", 8);
               int var14 = Math.min(var13.size(), 1024);

               for (int var15 = 0; var15 < var14; var15++) {
                  this.tags.add(var13.getString(var15));
               }
            }

            this.readAdditionalSaveData(param0);
            if (this.repositionEntityAfterLoad()) {
               this.reapplyPosition();
            }
         } else {
            throw new IllegalStateException("Entity has invalid rotation");
         }
      } catch (Throwable var2) {
         CrashReport var3 = CrashReport.forThrowable(var2, "Loading entity NBT");
         CrashReportCategory var4 = var3.addCategory("Entity being loaded");
         this.fillCrashReportCategory(var4);
         throw new ReportedException(var3);
      }
   }

   protected boolean repositionEntityAfterLoad() {
      return true;
   }

   @Nullable
   protected final String getEncodeId() {
      EntityType<?> var1 = this.getType();
      ResourceLocation var2 = EntityType.getKey(var1);
      return var1.canSerialize() && var2 != null ? var2.toString() : null;
   }

   protected abstract void readAdditionalSaveData(CompoundTag var1);

   protected abstract void addAdditionalSaveData(CompoundTag var1);

   protected ListTag newDoubleList(double... param0) {
      ListTag var2 = new ListTag();

      for (double var6 : param0) {
         var2.add(DoubleTag.valueOf(var6));
      }

      return var2;
   }

   protected ListTag newFloatList(float... param0) {
      ListTag var2 = new ListTag();

      for (float var6 : param0) {
         var2.add(FloatTag.valueOf(var6));
      }

      return var2;
   }

   @Nullable
   public ItemEntity spawnAtLocation(ItemLike param0) {
      return this.spawnAtLocation(param0, 0);
   }

   @Nullable
   public ItemEntity spawnAtLocation(ItemLike param0, int param1) {
      return this.spawnAtLocation(new ItemStack(param0), param1);
   }

   @Nullable
   public ItemEntity spawnAtLocation(ItemStack param0) {
      return this.spawnAtLocation(param0, 0.0F);
   }

   @Nullable
   public ItemEntity spawnAtLocation(ItemStack param0, float param1) {
      if (param0.isEmpty()) {
         return null;
      }

      if (this.level.isClientSide) {
         return null;
      }

      ItemEntity var3 = new ItemEntity(this.level, this.getX(), this.getY() + param1, this.getZ(), param0);
      var3.setDefaultPickUpDelay();
      this.level.addFreshEntity(var3);
      return var3;
   }

   public boolean isAlive() {
      return !this.isRemoved();
   }

   public boolean isInWall() {
      if (this.noPhysics) {
         return false;
      }

      float var1 = this.dimensions.width * 0.8F;
      AABB var2 = AABB.ofSize(this.getEyePosition(), var1, 1.0E-6, var1);
      return BlockPos.betweenClosedStream(var2)
         .anyMatch(
            l_param1 -> {
               BlockState l_var3 = this.level.getBlockState(l_param1);
               return !l_var3.isAir()
                  && l_var3.isSuffocating(this.level, l_param1)
                  && Shapes.joinIsNotEmpty(
                     l_var3.getCollisionShape(this.level, l_param1).move(l_param1.getX(), l_param1.getY(), l_param1.getZ()), Shapes.create(var2), BooleanOp.AND
                  );
            }
         );
   }

   public InteractionResult interact(Player param0, InteractionHand param1) {
      return InteractionResult.PASS;
   }

   public boolean canCollideWith(Entity param0) {
      return param0.canBeCollidedWith() && !this.isPassengerOfSameVehicle(param0);
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public void rideTick() {
      this.setDeltaMovement(Vec3.ZERO);
      this.tick();
      if (this.isPassenger()) {
         this.getVehicle().positionRider(this);
      }
   }

   public void positionRider(Entity param0) {
      this.positionRider(param0, Entity::setPos);
   }

   private void positionRider(Entity param0, Entity.MoveFunction param1) {
      if (this.hasPassenger(param0)) {
         double var3 = this.getY() + this.getPassengersRidingOffset() + param0.getMyRidingOffset();
         param1.accept(param0, this.getX(), var3, this.getZ());
      }
   }

   public void onPassengerTurned(Entity param0) {
   }

   public double getMyRidingOffset() {
      return 0.0;
   }

   public double getPassengersRidingOffset() {
      return this.dimensions.height * 0.75;
   }

   public boolean startRiding(Entity param0) {
      return this.startRiding(param0, false);
   }

   public boolean showVehicleHealth() {
      return this instanceof LivingEntity;
   }

   public boolean startRiding(Entity param0, boolean param1) {
      if (param0 == this.vehicle) {
         return false;
      }

      if (!param0.couldAcceptPassenger()) {
         return false;
      }

      for (Entity var3 = param0; var3.vehicle != null; var3 = var3.vehicle) {
         if (var3.vehicle == this) {
            return false;
         }
      }

      if (param1 || this.canRide(param0) && param0.canAddPassenger(this)) {
         if (this.isPassenger()) {
            this.stopRiding();
         }

         this.setPose(Pose.STANDING);
         this.vehicle = param0;
         this.vehicle.addPassenger(this);
         param0.getIndirectPassengersStream()
            .filter(l_param0 -> l_param0 instanceof ServerPlayer)
            .forEach(l_param0 -> CriteriaTriggers.START_RIDING_TRIGGER.trigger((ServerPlayer)l_param0));
         return true;
      } else {
         return false;
      }
   }

   protected boolean canRide(Entity param0) {
      return !this.isShiftKeyDown() && this.boardingCooldown <= 0;
   }

   protected boolean canEnterPose(Pose param0) {
      return this.level.noCollision(this, this.getBoundingBoxForPose(param0).deflate(1.0E-7));
   }

   public void ejectPassengers() {
      for (int var1 = this.passengers.size() - 1; var1 >= 0; var1--) {
         this.passengers.get(var1).stopRiding();
      }
   }

   public void removeVehicle() {
      if (this.vehicle != null) {
         Entity var1 = this.vehicle;
         this.vehicle = null;
         var1.removePassenger(this);
      }
   }

   public void stopRiding() {
      this.removeVehicle();
   }

   protected void addPassenger(Entity param0) {
      if (param0.getVehicle() != this) {
         throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
      }

      if (this.passengers.isEmpty()) {
         this.passengers = ImmutableList.of(param0);
      } else {
         List<Entity> var2 = Lists.newArrayList(this.passengers);
         if (!this.level.isClientSide && param0 instanceof Player && !(this.getFirstPassenger() instanceof Player)) {
            var2.add(0, param0);
         } else {
            var2.add(param0);
         }

         this.passengers = ImmutableList.copyOf(var2);
      }

      this.gameEvent(GameEvent.ENTITY_MOUNT, param0);
   }

   protected void removePassenger(Entity param0) {
      if (param0.getVehicle() == this) {
         throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
      }

      if (this.passengers.size() == 1 && this.passengers.get(0) == param0) {
         this.passengers = ImmutableList.of();
      } else {
         this.passengers = this.passengers.stream().filter(l_param1 -> l_param1 != param0).collect(ImmutableList.toImmutableList());
      }

      param0.boardingCooldown = 60;
      this.gameEvent(GameEvent.ENTITY_DISMOUNT, param0);
   }

   protected boolean canAddPassenger(Entity param0) {
      return this.passengers.isEmpty();
   }

   protected boolean couldAcceptPassenger() {
      return true;
   }

   public void lerpTo(double param0, double param2, double param4, float param6, float param7, int param8, boolean param9) {
      this.setPos(param0, param2, param4);
      this.setRot(param6, param7);
   }

   public void lerpHeadTo(float param0, int param1) {
      this.setYHeadRot(param0);
   }

   public float getPickRadius() {
      return 0.0F;
   }

   public Vec3 getLookAngle() {
      return this.calculateViewVector(this.getXRot(), this.getYRot());
   }

   public Vec3 getHandHoldingItemAngle(Item param0) {
      if (!(this instanceof Player var2)) {
         return Vec3.ZERO;
      } else {
         boolean var3 = var2.getOffhandItem().is(param0) && !var2.getMainHandItem().is(param0);
         HumanoidArm var4 = var3 ? var2.getMainArm().getOpposite() : var2.getMainArm();
         return this.calculateViewVector(0.0F, this.getYRot() + (var4 == HumanoidArm.RIGHT ? 80 : -80)).scale(0.5);
      }
   }

   public Vec2 getRotationVector() {
      return new Vec2(this.getXRot(), this.getYRot());
   }

   public Vec3 getForward() {
      return Vec3.directionFromRotation(this.getRotationVector());
   }

   public void handleInsidePortal(BlockPos param0) {
      if (this.isOnPortalCooldown()) {
         this.setPortalCooldown();
      } else {
         if (!this.level.isClientSide && !param0.equals(this.portalEntrancePos)) {
            this.portalEntrancePos = param0.immutable();
         }

         this.isInsidePortal = true;
      }
   }

   protected void handleNetherPortal() {
      if (this.level instanceof ServerLevel) {
         int var1 = this.getPortalWaitTime();
         ServerLevel var2 = (ServerLevel)this.level;
         if (this.isInsidePortal) {
            MinecraftServer var3 = var2.getServer();
            ResourceKey<Level> var4 = this.level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
            ServerLevel var5 = var3.getLevel(var4);
            if (var5 != null && var3.isNetherEnabled() && !this.isPassenger() && this.portalTime++ >= var1) {
               this.level.getProfiler().push("portal");
               this.portalTime = var1;
               this.setPortalCooldown();
               this.changeDimension(var5);
               this.level.getProfiler().pop();
            }

            this.isInsidePortal = false;
         } else {
            if (this.portalTime > 0) {
               this.portalTime -= 4;
            }

            if (this.portalTime < 0) {
               this.portalTime = 0;
            }
         }

         this.processPortalCooldown();
      }
   }

   public int getDimensionChangingDelay() {
      return 300;
   }

   public void lerpMotion(double param0, double param2, double param4) {
      this.setDeltaMovement(param0, param2, param4);
   }

   public void handleDamageEvent(DamageSource param0) {
   }

   public void handleEntityEvent(byte param0) {
      switch (param0) {
         case 53:
            HoneyBlock.showSlideParticles(this);
      }
   }

   public void animateHurt(float param0) {
   }

   public Iterable<ItemStack> getHandSlots() {
      return EMPTY_LIST;
   }

   public Iterable<ItemStack> getArmorSlots() {
      return EMPTY_LIST;
   }

   public Iterable<ItemStack> getAllSlots() {
      return Iterables.concat(this.getHandSlots(), this.getArmorSlots());
   }

   public void setItemSlot(EquipmentSlot param0, ItemStack param1) {
   }

   public boolean isOnFire() {
      boolean var1 = this.level != null && this.level.isClientSide;
      return !this.fireImmune() && (this.remainingFireTicks > 0 || var1 && this.getSharedFlag(0));
   }

   public boolean isPassenger() {
      return this.getVehicle() != null;
   }

   public boolean isVehicle() {
      return !this.passengers.isEmpty();
   }

   public boolean dismountsUnderwater() {
      return this.getType().is(EntityTypeTags.DISMOUNTS_UNDERWATER);
   }

   public void setShiftKeyDown(boolean param0) {
      this.setSharedFlag(1, param0);
   }

   public boolean isShiftKeyDown() {
      return this.getSharedFlag(1);
   }

   public boolean isSteppingCarefully() {
      return this.isShiftKeyDown();
   }

   public boolean isSuppressingBounce() {
      return this.isShiftKeyDown();
   }

   public boolean isDiscrete() {
      return this.isShiftKeyDown();
   }

   public boolean isDescending() {
      return this.isShiftKeyDown();
   }

   public boolean isCrouching() {
      return this.hasPose(Pose.CROUCHING);
   }

   public boolean isSprinting() {
      return this.getSharedFlag(3);
   }

   public void setSprinting(boolean param0) {
      this.setSharedFlag(3, param0);
   }

   public boolean isSwimming() {
      return this.getSharedFlag(4);
   }

   public boolean isVisuallySwimming() {
      return this.hasPose(Pose.SWIMMING);
   }

   public boolean isVisuallyCrawling() {
      return this.isVisuallySwimming() && !this.isInWater();
   }

   public void setSwimming(boolean param0) {
      this.setSharedFlag(4, param0);
   }

   public final boolean hasGlowingTag() {
      return this.hasGlowingTag;
   }

   public final void setGlowingTag(boolean param0) {
      this.hasGlowingTag = param0;
      this.setSharedFlag(6, this.isCurrentlyGlowing());
   }

   public boolean isCurrentlyGlowing() {
      return this.level.isClientSide() ? this.getSharedFlag(6) : this.hasGlowingTag;
   }

   public boolean isInvisible() {
      return this.getSharedFlag(5);
   }

   public boolean isInvisibleTo(Player param0) {
      if (param0.isSpectator()) {
         return false;
      }

      Team var2 = this.getTeam();
      return var2 != null && param0 != null && param0.getTeam() == var2 && var2.canSeeFriendlyInvisibles() ? false : this.isInvisible();
   }

   public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> param0) {
   }

   @Nullable
   public Team getTeam() {
      return this.level.getScoreboard().getPlayersTeam(this.getScoreboardName());
   }

   public boolean isAlliedTo(Entity param0) {
      return this.isAlliedTo(param0.getTeam());
   }

   public boolean isAlliedTo(Team param0) {
      return this.getTeam() != null ? this.getTeam().isAlliedTo(param0) : false;
   }

   public void setInvisible(boolean param0) {
      this.setSharedFlag(5, param0);
   }

   protected boolean getSharedFlag(int param0) {
      return (this.entityData.get(DATA_SHARED_FLAGS_ID) & 1 << param0) != 0;
   }

   protected void setSharedFlag(int param0, boolean param1) {
      byte var3 = this.entityData.get(DATA_SHARED_FLAGS_ID);
      if (param1) {
         this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(var3 | 1 << param0));
      } else {
         this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(var3 & ~(1 << param0)));
      }
   }

   public int getMaxAirSupply() {
      return 300;
   }

   public int getAirSupply() {
      return this.entityData.get(DATA_AIR_SUPPLY_ID);
   }

   public void setAirSupply(int param0) {
      this.entityData.set(DATA_AIR_SUPPLY_ID, param0);
   }

   public int getTicksFrozen() {
      return this.entityData.get(DATA_TICKS_FROZEN);
   }

   public void setTicksFrozen(int param0) {
      this.entityData.set(DATA_TICKS_FROZEN, param0);
   }

   public float getPercentFrozen() {
      int var1 = this.getTicksRequiredToFreeze();
      return (float)Math.min(this.getTicksFrozen(), var1) / var1;
   }

   public boolean isFullyFrozen() {
      return this.getTicksFrozen() >= this.getTicksRequiredToFreeze();
   }

   public int getTicksRequiredToFreeze() {
      return 140;
   }

   public void thunderHit(ServerLevel param0, LightningBolt param1) {
      this.setRemainingFireTicks(this.remainingFireTicks + 1);
      if (this.remainingFireTicks == 0) {
         this.setSecondsOnFire(8);
      }

      this.hurt(this.damageSources().lightningBolt(), 5.0F);
   }

   public void onAboveBubbleCol(boolean param0) {
      Vec3 var2 = this.getDeltaMovement();
      double var3;
      if (param0) {
         var3 = Math.max(-0.9, var2.y - 0.03);
      } else {
         var3 = Math.min(1.8, var2.y + 0.1);
      }

      this.setDeltaMovement(var2.x, var3, var2.z);
   }

   public void onInsideBubbleColumn(boolean param0) {
      Vec3 var2 = this.getDeltaMovement();
      double var3;
      if (param0) {
         var3 = Math.max(-0.3, var2.y - 0.03);
      } else {
         var3 = Math.min(0.7, var2.y + 0.06);
      }

      this.setDeltaMovement(var2.x, var3, var2.z);
      this.resetFallDistance();
   }

   public boolean wasKilled(ServerLevel param0, LivingEntity param1) {
      return true;
   }

   public void checkSlowFallDistance() {
      if (this.getDeltaMovement().y() > -0.5 && this.fallDistance > 1.0F) {
         this.fallDistance = 1.0F;
      }
   }

   public void resetFallDistance() {
      this.fallDistance = 0.0F;
   }

   protected void moveTowardsClosestSpace(double param0, double param2, double param4) {
      BlockPos var7 = BlockPos.containing(param0, param2, param4);
      Vec3 var8 = new Vec3(param0 - var7.getX(), param2 - var7.getY(), param4 - var7.getZ());
      BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();
      Direction var10 = Direction.UP;
      double var11 = Double.MAX_VALUE;

      for (Direction var16 : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
         var9.setWithOffset(var7, var16);
         if (!this.level.getBlockState(var9).isCollisionShapeFullBlock(this.level, var9)) {
            double var17 = var8.get(var16.getAxis());
            double var19 = var16.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - var17 : var17;
            if (var19 < var11) {
               var11 = var19;
               var10 = var16;
            }
         }
      }

      float var13 = this.random.nextFloat() * 0.2F + 0.1F;
      float var14 = var10.getAxisDirection().getStep();
      Vec3 var15 = this.getDeltaMovement().scale(0.75);
      if (var10.getAxis() == Direction.Axis.X) {
         this.setDeltaMovement(var14 * var13, var15.y, var15.z);
      } else if (var10.getAxis() == Direction.Axis.Y) {
         this.setDeltaMovement(var15.x, var14 * var13, var15.z);
      } else if (var10.getAxis() == Direction.Axis.Z) {
         this.setDeltaMovement(var15.x, var15.y, var14 * var13);
      }
   }

   public void makeStuckInBlock(BlockState param0, Vec3 param1) {
      this.resetFallDistance();
      this.stuckSpeedMultiplier = param1;
   }

   private static Component removeAction(Component param0) {
      MutableComponent var1 = param0.plainCopy().setStyle(param0.getStyle().withClickEvent(null));

      for (Component var3 : param0.getSiblings()) {
         var1.append(removeAction(var3));
      }

      return var1;
   }

   @Override
   public Component getName() {
      Component var1 = this.getCustomName();
      return var1 != null ? removeAction(var1) : this.getTypeName();
   }

   protected Component getTypeName() {
      return this.type.getDescription();
   }

   public boolean is(Entity param0) {
      return this == param0;
   }

   public float getYHeadRot() {
      return 0.0F;
   }

   public void setYHeadRot(float param0) {
   }

   public void setYBodyRot(float param0) {
   }

   public boolean isAttackable() {
      return true;
   }

   public boolean skipAttackInteraction(Entity param0) {
      return false;
   }

   @Override
   public String toString() {
      String var1 = this.level == null ? "~NULL~" : this.level.toString();
      return this.removalReason != null
         ? String.format(
            Locale.ROOT,
            "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f, removed=%s]",
            this.getClass().getSimpleName(),
            this.getName().getString(),
            this.id,
            var1,
            this.getX(),
            this.getY(),
            this.getZ(),
            this.removalReason
         )
         : String.format(
            Locale.ROOT,
            "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]",
            this.getClass().getSimpleName(),
            this.getName().getString(),
            this.id,
            var1,
            this.getX(),
            this.getY(),
            this.getZ()
         );
   }

   public boolean isInvulnerableTo(DamageSource param0) {
      return this.isRemoved()
         || this.invulnerable && !param0.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !param0.isCreativePlayer()
         || param0.is(DamageTypeTags.IS_FIRE) && this.fireImmune()
         || param0.is(DamageTypeTags.IS_FALL) && this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE);
   }

   public boolean isInvulnerable() {
      return this.invulnerable;
   }

   public void setInvulnerable(boolean param0) {
      this.invulnerable = param0;
   }

   public void copyPosition(Entity param0) {
      this.moveTo(param0.getX(), param0.getY(), param0.getZ(), param0.getYRot(), param0.getXRot());
   }

   public void restoreFrom(Entity param0) {
      CompoundTag var2 = param0.saveWithoutId(new CompoundTag());
      var2.remove("Dimension");
      this.load(var2);
      this.portalCooldown = param0.portalCooldown;
      this.portalEntrancePos = param0.portalEntrancePos;
   }

   @Nullable
   public Entity changeDimension(ServerLevel param0) {
      if (this.level instanceof ServerLevel && !this.isRemoved()) {
         this.level.getProfiler().push("changeDimension");
         this.unRide();
         this.level.getProfiler().push("reposition");
         PortalInfo var2 = this.findDimensionEntryPoint(param0);
         if (var2 == null) {
            return null;
         }

         this.level.getProfiler().popPush("reloading");
         Entity var3 = this.getType().create(param0);
         if (var3 != null) {
            var3.restoreFrom(this);
            var3.moveTo(var2.pos.x, var2.pos.y, var2.pos.z, var2.yRot, var3.getXRot());
            var3.setDeltaMovement(var2.speed);
            param0.addDuringTeleport(var3);
            if (param0.dimension() == Level.END) {
               ServerLevel.makeObsidianPlatform(param0);
            }
         }

         this.removeAfterChangingDimensions();
         this.level.getProfiler().pop();
         ((ServerLevel)this.level).resetEmptyTime();
         param0.resetEmptyTime();
         this.level.getProfiler().pop();
         return var3;
      } else {
         return null;
      }
   }

   protected void removeAfterChangingDimensions() {
      this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
   }

   @Nullable
   protected PortalInfo findDimensionEntryPoint(ServerLevel param0) {
      boolean var2 = this.level.dimension() == Level.END && param0.dimension() == Level.OVERWORLD;
      boolean var3 = param0.dimension() == Level.END;
      if (!var2 && !var3) {
         boolean var4 = param0.dimension() == Level.NETHER;
         if (this.level.dimension() != Level.NETHER && !var4) {
            return null;
         }

         WorldBorder var5 = param0.getWorldBorder();
         double var6 = DimensionType.getTeleportationScale(this.level.dimensionType(), param0.dimensionType());
         BlockPos var8 = var5.clampToBounds(this.getX() * var6, this.getY(), this.getZ() * var6);
         return this.getExitPortal(param0, var8, var4, var5)
            .map(
               l_param1 -> {
                  BlockState l_var5 = this.level.getBlockState(this.portalEntrancePos);
                  Direction.Axis l_var3;
                  Vec3 l_var4;
                  if (l_var5.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                     l_var3 = l_var5.getValue(BlockStateProperties.HORIZONTAL_AXIS);
                     BlockUtil.FoundRectangle l_var6 = BlockUtil.getLargestRectangleAround(
                        this.portalEntrancePos, l_var3, 21, Direction.Axis.Y, 21, l2_param1 -> this.level.getBlockState(l2_param1) == l_var5
                     );
                     l_var4 = this.getRelativePortalPosition(l_var3, l_var6);
                  } else {
                     l_var3 = Direction.Axis.X;
                     l_var4 = new Vec3(0.5, 0.0, 0.0);
                  }

                  return PortalShape.createPortalInfo(param0, l_param1, l_var3, l_var4, this, this.getDeltaMovement(), this.getYRot(), this.getXRot());
               }
            )
            .orElse(null);
      } else {
         BlockPos var4;
         if (var3) {
            var4 = ServerLevel.END_SPAWN_POINT;
         } else {
            var4 = param0.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, param0.getSharedSpawnPos());
         }

         return new PortalInfo(new Vec3(var4.getX() + 0.5, var4.getY(), var4.getZ() + 0.5), this.getDeltaMovement(), this.getYRot(), this.getXRot());
      }
   }

   protected Vec3 getRelativePortalPosition(Direction.Axis param0, BlockUtil.FoundRectangle param1) {
      return PortalShape.getRelativePosition(param1, param0, this.position(), this.getDimensions(this.getPose()));
   }

   protected Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel param0, BlockPos param1, boolean param2, WorldBorder param3) {
      return param0.getPortalForcer().findPortalAround(param1, param2, param3);
   }

   public boolean canChangeDimensions() {
      return !this.isPassenger() && !this.isVehicle();
   }

   public float getBlockExplosionResistance(Explosion param0, BlockGetter param1, BlockPos param2, BlockState param3, FluidState param4, float param5) {
      return param5;
   }

   public boolean shouldBlockExplode(Explosion param0, BlockGetter param1, BlockPos param2, BlockState param3, float param4) {
      return true;
   }

   public int getMaxFallDistance() {
      return 3;
   }

   public boolean isIgnoringBlockTriggers() {
      return false;
   }

   public void fillCrashReportCategory(CrashReportCategory param0) {
      param0.setDetail("Entity Type", () -> EntityType.getKey(this.getType()) + " (" + this.getClass().getCanonicalName() + ")");
      param0.setDetail("Entity ID", this.id);
      param0.setDetail("Entity Name", () -> this.getName().getString());
      param0.setDetail("Entity's Exact location", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
      param0.setDetail(
         "Entity's Block location", CrashReportCategory.formatLocation(this.level, Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()))
      );
      Vec3 var2 = this.getDeltaMovement();
      param0.setDetail("Entity's Momentum", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", var2.x, var2.y, var2.z));
      param0.setDetail("Entity's Passengers", () -> this.getPassengers().toString());
      param0.setDetail("Entity's Vehicle", () -> String.valueOf(this.getVehicle()));
   }

   public boolean displayFireAnimation() {
      return this.isOnFire() && !this.isSpectator();
   }

   public void setUUID(UUID param0) {
      this.uuid = param0;
      this.stringUUID = this.uuid.toString();
   }

   @Override
   public UUID getUUID() {
      return this.uuid;
   }

   public String getStringUUID() {
      return this.stringUUID;
   }

   public String getScoreboardName() {
      return this.stringUUID;
   }

   public boolean isPushedByFluid() {
      return true;
   }

   public static double getViewScale() {
      return viewScale;
   }

   public static void setViewScale(double param0) {
      viewScale = param0;
   }

   @Override
   public Component getDisplayName() {
      return PlayerTeam.formatNameForTeam(this.getTeam(), this.getName())
         .withStyle(l_param0 -> l_param0.withHoverEvent(this.createHoverEvent()).withInsertion(this.getStringUUID()));
   }

   public void setCustomName(@Nullable Component param0) {
      this.entityData.set(DATA_CUSTOM_NAME, Optional.ofNullable(param0));
   }

   @Nullable
   @Override
   public Component getCustomName() {
      return this.entityData.get(DATA_CUSTOM_NAME).orElse(null);
   }

   @Override
   public boolean hasCustomName() {
      return this.entityData.get(DATA_CUSTOM_NAME).isPresent();
   }

   public void setCustomNameVisible(boolean param0) {
      this.entityData.set(DATA_CUSTOM_NAME_VISIBLE, param0);
   }

   public boolean isCustomNameVisible() {
      return this.entityData.get(DATA_CUSTOM_NAME_VISIBLE);
   }

   public final void teleportToWithTicket(double param0, double param2, double param4) {
      if (this.level instanceof ServerLevel) {
         ChunkPos var7 = new ChunkPos(BlockPos.containing(param0, param2, param4));
         ((ServerLevel)this.level).getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, var7, 0, this.getId());
         this.level.getChunk(var7.x, var7.z);
         this.teleportTo(param0, param2, param4);
      }
   }

   public boolean teleportTo(ServerLevel param0, double param1, double param3, double param5, Set<RelativeMovement> param7, float param8, float param9) {
      float var11 = Mth.clamp(param9, -90.0F, 90.0F);
      if (param0 == this.level) {
         this.moveTo(param1, param3, param5, param8, var11);
         this.setYHeadRot(param8);
      } else {
         this.unRide();
         Entity var12 = this.getType().create(param0);
         if (var12 == null) {
            return false;
         }

         var12.restoreFrom(this);
         var12.moveTo(param1, param3, param5, param8, var11);
         var12.setYHeadRot(param8);
         this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
         param0.addDuringTeleport(var12);
      }

      return true;
   }

   public void dismountTo(double param0, double param2, double param4) {
      this.teleportTo(param0, param2, param4);
   }

   public void teleportTo(double param0, double param2, double param4) {
      if (this.level instanceof ServerLevel) {
         this.moveTo(param0, param2, param4, this.getYRot(), this.getXRot());
         this.getSelfAndPassengers().forEach(l_param0 -> {
            for (Entity l_var2 : l_param0.passengers) {
               l_param0.positionRider(l_var2, Entity::moveTo);
            }
         });
      }
   }

   public void teleportRelative(double param0, double param2, double param4) {
      this.teleportTo(this.getX() + param0, this.getY() + param2, this.getZ() + param4);
   }

   public boolean shouldShowName() {
      return this.isCustomNameVisible();
   }

   public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> param0) {
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> param0) {
      if (DATA_POSE.equals(param0)) {
         this.refreshDimensions();
      }
   }

   @Deprecated
   protected void fixupDimensions() {
      Pose var1 = this.getPose();
      EntityDimensions var2 = this.getDimensions(var1);
      this.dimensions = var2;
      this.eyeHeight = this.getEyeHeight(var1, var2);
   }

   public void refreshDimensions() {
      EntityDimensions var1 = this.dimensions;
      Pose var2 = this.getPose();
      EntityDimensions var3 = this.getDimensions(var2);
      this.dimensions = var3;
      this.eyeHeight = this.getEyeHeight(var2, var3);
      this.reapplyPosition();
      boolean var4 = var3.width <= 4.0 && var3.height <= 4.0;
      if (!this.level.isClientSide
         && !this.firstTick
         && !this.noPhysics
         && var4
         && (var3.width > var1.width || var3.height > var1.height)
         && !(this instanceof Player)) {
         Vec3 var5 = this.position().add(0.0, var1.height / 2.0, 0.0);
         double var6 = Math.max(0.0F, var3.width - var1.width) + 1.0E-6;
         double var8 = Math.max(0.0F, var3.height - var1.height) + 1.0E-6;
         VoxelShape var10 = Shapes.create(AABB.ofSize(var5, var6, var8, var6));
         this.level
            .findFreePosition(this, var10, var5, var3.width, var3.height, var3.width)
            .ifPresent(l_param1 -> this.setPos(l_param1.add(0.0, -var3.height / 2.0, 0.0)));
      }
   }

   public Direction getDirection() {
      return Direction.fromYRot(this.getYRot());
   }

   public Direction getMotionDirection() {
      return this.getDirection();
   }

   protected HoverEvent createHoverEvent() {
      return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityTooltipInfo(this.getType(), this.getUUID(), this.getName()));
   }

   public boolean broadcastToPlayer(ServerPlayer param0) {
      return true;
   }

   @Override
   public final AABB getBoundingBox() {
      return this.bb;
   }

   public AABB getBoundingBoxForCulling() {
      return this.getBoundingBox();
   }

   protected AABB getBoundingBoxForPose(Pose param0) {
      EntityDimensions var2 = this.getDimensions(param0);
      float var3 = var2.width / 2.0F;
      Vec3 var4 = new Vec3(this.getX() - var3, this.getY(), this.getZ() - var3);
      Vec3 var5 = new Vec3(this.getX() + var3, this.getY() + var2.height, this.getZ() + var3);
      return new AABB(var4, var5);
   }

   public final void setBoundingBox(AABB param0) {
      this.bb = param0;
   }

   protected float getEyeHeight(Pose param0, EntityDimensions param1) {
      return param1.height * 0.85F;
   }

   public float getEyeHeight(Pose param0) {
      return this.getEyeHeight(param0, this.getDimensions(param0));
   }

   public final float getEyeHeight() {
      return this.eyeHeight;
   }

   public Vec3 getLeashOffset(float param0) {
      return this.getLeashOffset();
   }

   protected Vec3 getLeashOffset() {
      return new Vec3(0.0, this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   public SlotAccess getSlot(int param0) {
      return SlotAccess.NULL;
   }

   @Override
   public void sendSystemMessage(Component param0) {
   }

   public Level getCommandSenderWorld() {
      return this.level;
   }

   @Nullable
   public MinecraftServer getServer() {
      return this.level.getServer();
   }

   public InteractionResult interactAt(Player param0, Vec3 param1, InteractionHand param2) {
      return InteractionResult.PASS;
   }

   public boolean ignoreExplosion() {
      return false;
   }

   public void doEnchantDamageEffects(LivingEntity param0, Entity param1) {
      if (param1 instanceof LivingEntity) {
         EnchantmentHelper.doPostHurtEffects((LivingEntity)param1, param0);
      }

      EnchantmentHelper.doPostDamageEffects(param0, param1);
   }

   public void startSeenByPlayer(ServerPlayer param0) {
   }

   public void stopSeenByPlayer(ServerPlayer param0) {
   }

   public float rotate(Rotation param0) {
      float var2 = Mth.wrapDegrees(this.getYRot());
      switch (param0) {
         case CLOCKWISE_180:
            return var2 + 180.0F;
         case COUNTERCLOCKWISE_90:
            return var2 + 270.0F;
         case CLOCKWISE_90:
            return var2 + 90.0F;
         default:
            return var2;
      }
   }

   public float mirror(Mirror param0) {
      float var2 = Mth.wrapDegrees(this.getYRot());
      switch (param0) {
         case FRONT_BACK:
            return -var2;
         case LEFT_RIGHT:
            return 180.0F - var2;
         default:
            return var2;
      }
   }

   public boolean onlyOpCanSetNbt() {
      return false;
   }

   @Nullable
   public LivingEntity getControllingPassenger() {
      return null;
   }

   public final boolean hasControllingPassenger() {
      return this.getControllingPassenger() != null;
   }

   public final List<Entity> getPassengers() {
      return this.passengers;
   }

   @Nullable
   public Entity getFirstPassenger() {
      return this.passengers.isEmpty() ? null : this.passengers.get(0);
   }

   public boolean hasPassenger(Entity param0) {
      return this.passengers.contains(param0);
   }

   public boolean hasPassenger(Predicate<Entity> param0) {
      for (Entity var3 : this.passengers) {
         if (param0.test(var3)) {
            return true;
         }
      }

      return false;
   }

   private Stream<Entity> getIndirectPassengersStream() {
      return this.passengers.stream().flatMap(Entity::getSelfAndPassengers);
   }

   @Override
   public Stream<Entity> getSelfAndPassengers() {
      return Stream.concat(Stream.of(this), this.getIndirectPassengersStream());
   }

   @Override
   public Stream<Entity> getPassengersAndSelf() {
      return Stream.concat(this.passengers.stream().flatMap(Entity::getPassengersAndSelf), Stream.of(this));
   }

   public Iterable<Entity> getIndirectPassengers() {
      return () -> this.getIndirectPassengersStream().iterator();
   }

   public boolean hasExactlyOnePlayerPassenger() {
      return this.getIndirectPassengersStream().filter(l_param0 -> l_param0 instanceof Player).count() == 1L;
   }

   public Entity getRootVehicle() {
      Entity var1 = this;

      while (var1.isPassenger()) {
         var1 = var1.getVehicle();
      }

      return var1;
   }

   public boolean isPassengerOfSameVehicle(Entity param0) {
      return this.getRootVehicle() == param0.getRootVehicle();
   }

   public boolean hasIndirectPassenger(Entity param0) {
      if (!param0.isPassenger()) {
         return false;
      }

      Entity var2 = param0.getVehicle();
      return var2 == this ? true : this.hasIndirectPassenger(var2);
   }

   public boolean isControlledByLocalInstance() {
      return this.getControllingPassenger() instanceof Player var1 ? var1.isLocalPlayer() : this.isEffectiveAi();
   }

   public boolean isEffectiveAi() {
      return !this.level.isClientSide;
   }

   protected static Vec3 getCollisionHorizontalEscapeVector(double param0, double param2, float param4) {
      double var5 = (param0 + param2 + 1.0E-5F) / 2.0;
      float var7 = -Mth.sin(param4 * (float) (Math.PI / 180.0));
      float var8 = Mth.cos(param4 * (float) (Math.PI / 180.0));
      float var9 = Math.max(Math.abs(var7), Math.abs(var8));
      return new Vec3(var7 * var5 / var9, 0.0, var8 * var5 / var9);
   }

   public Vec3 getDismountLocationForPassenger(LivingEntity param0) {
      return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
   }

   @Nullable
   public Entity getVehicle() {
      return this.vehicle;
   }

   @Nullable
   public Entity getControlledVehicle() {
      return this.vehicle != null && this.vehicle.getControllingPassenger() == this ? this.vehicle : null;
   }

   public PushReaction getPistonPushReaction() {
      return PushReaction.NORMAL;
   }

   public SoundSource getSoundSource() {
      return SoundSource.NEUTRAL;
   }

   protected int getFireImmuneTicks() {
      return 1;
   }

   public CommandSourceStack createCommandSourceStack() {
      return new CommandSourceStack(
         this,
         this.position(),
         this.getRotationVector(),
         this.level instanceof ServerLevel ? (ServerLevel)this.level : null,
         this.getPermissionLevel(),
         this.getName().getString(),
         this.getDisplayName(),
         this.level.getServer(),
         this
      );
   }

   protected int getPermissionLevel() {
      return 0;
   }

   public boolean hasPermissions(int param0) {
      return this.getPermissionLevel() >= param0;
   }

   @Override
   public boolean acceptsSuccess() {
      return this.level.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK);
   }

   @Override
   public boolean acceptsFailure() {
      return true;
   }

   @Override
   public boolean shouldInformAdmins() {
      return true;
   }

   public void lookAt(EntityAnchorArgument.Anchor param0, Vec3 param1) {
      Vec3 var3 = param0.apply(this);
      double var4 = param1.x - var3.x;
      double var6 = param1.y - var3.y;
      double var8 = param1.z - var3.z;
      double var10 = Math.sqrt(var4 * var4 + var8 * var8);
      this.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(var6, var10) * 180.0F / (float)Math.PI))));
      this.setYRot(Mth.wrapDegrees((float)(Mth.atan2(var8, var4) * 180.0F / (float)Math.PI) - 90.0F));
      this.setYHeadRot(this.getYRot());
      this.xRotO = this.getXRot();
      this.yRotO = this.getYRot();
   }

   public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> param0, double param1) {
      if (this.touchingUnloadedChunk()) {
         return false;
      }

      AABB var4 = this.getBoundingBox().deflate(0.001);
      int var5 = Mth.floor(var4.minX);
      int var6 = Mth.ceil(var4.maxX);
      int var7 = Mth.floor(var4.minY);
      int var8 = Mth.ceil(var4.maxY);
      int var9 = Mth.floor(var4.minZ);
      int var10 = Mth.ceil(var4.maxZ);
      double var11 = 0.0;
      boolean var13 = this.isPushedByFluid();
      boolean var14 = false;
      Vec3 var15 = Vec3.ZERO;
      int var16 = 0;
      BlockPos.MutableBlockPos var17 = new BlockPos.MutableBlockPos();

      for (int var18 = var5; var18 < var6; var18++) {
         for (int var19 = var7; var19 < var8; var19++) {
            for (int var20 = var9; var20 < var10; var20++) {
               var17.set(var18, var19, var20);
               FluidState var21 = this.level.getFluidState(var17);
               if (var21.is(param0)) {
                  double var22 = var19 + var21.getHeight(this.level, var17);
                  if (var22 >= var4.minY) {
                     var14 = true;
                     var11 = Math.max(var22 - var4.minY, var11);
                     if (var13) {
                        Vec3 var24 = var21.getFlow(this.level, var17);
                        if (var11 < 0.4) {
                           var24 = var24.scale(var11);
                        }

                        var15 = var15.add(var24);
                        var16++;
                     }
                  }
               }
            }
         }
      }

      if (var15.length() > 0.0) {
         if (var16 > 0) {
            var15 = var15.scale(1.0 / var16);
         }

         if (!(this instanceof Player)) {
            var15 = var15.normalize();
         }

         Vec3 var18 = this.getDeltaMovement();
         var15 = var15.scale(param1 * 1.0);
         double var19 = 0.003;
         if (Math.abs(var18.x) < 0.003 && Math.abs(var18.z) < 0.003 && var15.length() < 0.0045000000000000005) {
            var15 = var15.normalize().scale(0.0045000000000000005);
         }

         this.setDeltaMovement(this.getDeltaMovement().add(var15));
      }

      this.fluidHeight.put(param0, var11);
      return var14;
   }

   public boolean touchingUnloadedChunk() {
      AABB var1 = this.getBoundingBox().inflate(1.0);
      int var2 = Mth.floor(var1.minX);
      int var3 = Mth.ceil(var1.maxX);
      int var4 = Mth.floor(var1.minZ);
      int var5 = Mth.ceil(var1.maxZ);
      return !this.level.hasChunksAt(var2, var4, var3, var5);
   }

   public double getFluidHeight(TagKey<Fluid> param0) {
      return this.fluidHeight.getDouble(param0);
   }

   public double getFluidJumpThreshold() {
      return this.getEyeHeight() < 0.4 ? 0.0 : 0.4;
   }

   public final float getBbWidth() {
      return this.dimensions.width;
   }

   public final float getBbHeight() {
      return this.dimensions.height;
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this);
   }

   public EntityDimensions getDimensions(Pose param0) {
      return this.type.getDimensions();
   }

   public Vec3 position() {
      return this.position;
   }

   public Vec3 trackingPosition() {
      return this.position();
   }

   @Override
   public BlockPos blockPosition() {
      return this.blockPosition;
   }

   public BlockState getFeetBlockState() {
      if (this.feetBlockState == null) {
         this.feetBlockState = this.level.getBlockState(this.blockPosition());
      }

      return this.feetBlockState;
   }

   public ChunkPos chunkPosition() {
      return this.chunkPosition;
   }

   public Vec3 getDeltaMovement() {
      return this.deltaMovement;
   }

   public void setDeltaMovement(Vec3 param0) {
      this.deltaMovement = param0;
   }

   public void addDeltaMovement(Vec3 param0) {
      this.setDeltaMovement(this.getDeltaMovement().add(param0));
   }

   public void setDeltaMovement(double param0, double param2, double param4) {
      this.setDeltaMovement(new Vec3(param0, param2, param4));
   }

   public final int getBlockX() {
      return this.blockPosition.getX();
   }

   public final double getX() {
      return this.position.x;
   }

   public double getX(double param0) {
      return this.position.x + this.getBbWidth() * param0;
   }

   public double getRandomX(double param0) {
      return this.getX((2.0 * this.random.nextDouble() - 1.0) * param0);
   }

   public final int getBlockY() {
      return this.blockPosition.getY();
   }

   public final double getY() {
      return this.position.y;
   }

   public double getY(double param0) {
      return this.position.y + this.getBbHeight() * param0;
   }

   public double getRandomY() {
      return this.getY(this.random.nextDouble());
   }

   public double getEyeY() {
      return this.position.y + this.eyeHeight;
   }

   public final int getBlockZ() {
      return this.blockPosition.getZ();
   }

   public final double getZ() {
      return this.position.z;
   }

   public double getZ(double param0) {
      return this.position.z + this.getBbWidth() * param0;
   }

   public double getRandomZ(double param0) {
      return this.getZ((2.0 * this.random.nextDouble() - 1.0) * param0);
   }

   public final void setPosRaw(double param0, double param2, double param4) {
      if (this.position.x != param0 || this.position.y != param2 || this.position.z != param4) {
         this.position = new Vec3(param0, param2, param4);
         int var7 = Mth.floor(param0);
         int var8 = Mth.floor(param2);
         int var9 = Mth.floor(param4);
         if (var7 != this.blockPosition.getX() || var8 != this.blockPosition.getY() || var9 != this.blockPosition.getZ()) {
            this.blockPosition = new BlockPos(var7, var8, var9);
            this.feetBlockState = null;
            if (SectionPos.blockToSectionCoord(var7) != this.chunkPosition.x || SectionPos.blockToSectionCoord(var9) != this.chunkPosition.z) {
               this.chunkPosition = new ChunkPos(this.blockPosition);
            }
         }

         this.levelCallback.onMove();
      }
   }

   public void checkDespawn() {
   }

   public Vec3 getRopeHoldPosition(float param0) {
      return this.getPosition(param0).add(0.0, this.eyeHeight * 0.7, 0.0);
   }

   public void recreateFromPacket(ClientboundAddEntityPacket param0) {
      int var2 = param0.getId();
      double var3 = param0.getX();
      double var5 = param0.getY();
      double var7 = param0.getZ();
      this.syncPacketPositionCodec(var3, var5, var7);
      this.moveTo(var3, var5, var7);
      this.setXRot(param0.getXRot());
      this.setYRot(param0.getYRot());
      this.setId(var2);
      this.setUUID(param0.getUUID());
   }

   @Nullable
   public ItemStack getPickResult() {
      return null;
   }

   public void setIsInPowderSnow(boolean param0) {
      this.isInPowderSnow = param0;
   }

   public boolean canFreeze() {
      return !this.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
   }

   public boolean isFreezing() {
      return (this.isInPowderSnow || this.wasInPowderSnow) && this.canFreeze();
   }

   public float getYRot() {
      return this.yRot;
   }

   public float getVisualRotationYInDegrees() {
      return this.getYRot();
   }

   public void setYRot(float param0) {
      if (!Float.isFinite(param0)) {
         Util.logAndPauseIfInIde("Invalid entity rotation: " + param0 + ", discarding.");
      } else {
         this.yRot = param0;
      }
   }

   public float getXRot() {
      return this.xRot;
   }

   public void setXRot(float param0) {
      if (!Float.isFinite(param0)) {
         Util.logAndPauseIfInIde("Invalid entity rotation: " + param0 + ", discarding.");
      } else {
         this.xRot = param0;
      }
   }

   public boolean canSprint() {
      return false;
   }

   public float maxUpStep() {
      return this.maxUpStep;
   }

   public void setMaxUpStep(float param0) {
      this.maxUpStep = param0;
   }

   public final boolean isRemoved() {
      return this.removalReason != null;
   }

   @Nullable
   public Entity.RemovalReason getRemovalReason() {
      return this.removalReason;
   }

   @Override
   public final void setRemoved(Entity.RemovalReason param0) {
      if (this.removalReason == null) {
         this.removalReason = param0;
      }

      if (this.removalReason.shouldDestroy()) {
         this.stopRiding();
      }

      this.getPassengers().forEach(Entity::stopRiding);
      this.levelCallback.onRemove(param0);
   }

   protected void unsetRemoved() {
      this.removalReason = null;
   }

   @Override
   public void setLevelCallback(EntityInLevelCallback param0) {
      this.levelCallback = param0;
   }

   @Override
   public boolean shouldBeSaved() {
      if (this.removalReason != null && !this.removalReason.shouldSave()) {
         return false;
      } else {
         return this.isPassenger() ? false : !this.isVehicle() || !this.hasExactlyOnePlayerPassenger();
      }
   }

   @Override
   public boolean isAlwaysTicking() {
      return false;
   }

   public boolean mayInteract(Level param0, BlockPos param1) {
      return true;
   }

   public Level getLevel() {
      return this.level;
   }

   public DamageSources damageSources() {
      return this.level.damageSources();
   }

   @FunctionalInterface
   public interface MoveFunction {
      void accept(Entity var1, double var2, double var4, double var6);
   }

   public enum MovementEmission {
      NONE(false, false),
      SOUNDS(true, false),
      EVENTS(false, true),
      ALL(true, true);

      final boolean sounds;
      final boolean events;

      MovementEmission(boolean param2, boolean param3) {
         this.sounds = param2;
         this.events = param3;
      }

      public boolean emitsAnything() {
         return this.events || this.sounds;
      }

      public boolean emitsEvents() {
         return this.events;
      }

      public boolean emitsSounds() {
         return this.sounds;
      }
   }

   public enum RemovalReason {
      KILLED(true, false),
      DISCARDED(true, false),
      UNLOADED_TO_CHUNK(false, true),
      UNLOADED_WITH_PLAYER(false, false),
      CHANGED_DIMENSION(false, false);

      private final boolean destroy;
      private final boolean save;

      RemovalReason(boolean param2, boolean param3) {
         this.destroy = param2;
         this.save = param3;
      }

      public boolean shouldDestroy() {
         return this.destroy;
      }

      public boolean shouldSave() {
         return this.save;
      }
   }
}
