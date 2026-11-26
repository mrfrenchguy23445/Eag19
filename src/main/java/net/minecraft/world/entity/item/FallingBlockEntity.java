package net.minecraft.world.entity.item;

import com.mojang.logging.LogUtils;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class FallingBlockEntity extends Entity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private BlockState blockState = Blocks.SAND.defaultBlockState();
   public int time;
   public boolean dropItem = true;
   private boolean cancelDrop;
   private boolean hurtEntities;
   private int fallDamageMax = 40;
   private float fallDamagePerDistance;
   @Nullable
   public CompoundTag blockData;
   protected static final EntityDataAccessor<BlockPos> DATA_START_POS = SynchedEntityData.defineId(FallingBlockEntity.class, EntityDataSerializers.BLOCK_POS);

   public FallingBlockEntity(EntityType<? extends FallingBlockEntity> param0, Level param1) {
      super(param0, param1);
   }

   private FallingBlockEntity(Level param0, double param1, double param3, double param5, BlockState param7) {
      this(EntityType.FALLING_BLOCK, param0);
      this.blockState = param7;
      this.blocksBuilding = true;
      this.setPos(param1, param3, param5);
      this.setDeltaMovement(Vec3.ZERO);
      this.xo = param1;
      this.yo = param3;
      this.zo = param5;
      this.setStartPos(this.blockPosition());
   }

   public static FallingBlockEntity fall(Level param0, BlockPos param1, BlockState param2) {
      FallingBlockEntity var3 = new FallingBlockEntity(
         param0,
         param1.getX() + 0.5,
         param1.getY(),
         param1.getZ() + 0.5,
         param2.hasProperty(BlockStateProperties.WATERLOGGED) ? param2.setValue(BlockStateProperties.WATERLOGGED, false) : param2
      );
      param0.setBlock(param1, param2.getFluidState().createLegacyBlock(), 3);
      param0.addFreshEntity(var3);
      return var3;
   }

   @Override
   public boolean isAttackable() {
      return false;
   }

   public void setStartPos(BlockPos param0) {
      this.entityData.set(DATA_START_POS, param0);
   }

   public BlockPos getStartPos() {
      return this.entityData.get(DATA_START_POS);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   @Override
   protected void defineSynchedData() {
      this.entityData.define(DATA_START_POS, BlockPos.ZERO);
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   @Override
   public void tick() {
      if (this.blockState.isAir()) {
         this.discard();
      } else {
         Block var1 = this.blockState.getBlock();
         this.time++;
         if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
         }

         this.move(MoverType.SELF, this.getDeltaMovement());
         if (!this.level.isClientSide) {
            BlockPos var2 = this.blockPosition();
            boolean var3 = this.blockState.getBlock() instanceof ConcretePowderBlock;
            boolean var4 = var3 && this.level.getFluidState(var2).is(FluidTags.WATER);
            double var5 = this.getDeltaMovement().lengthSqr();
            if (var3 && var5 > 1.0) {
               BlockHitResult var7 = this.level
                  .clip(new ClipContext(new Vec3(this.xo, this.yo, this.zo), this.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, this));
               if (var7.getType() != HitResult.Type.MISS && this.level.getFluidState(var7.getBlockPos()).is(FluidTags.WATER)) {
                  var2 = var7.getBlockPos();
                  var4 = true;
               }
            }

            if (this.onGround || var4) {
               BlockState var7 = this.level.getBlockState(var2);
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
               if (!var7.is(Blocks.MOVING_PISTON)) {
                  if (!this.cancelDrop) {
                     boolean var8 = var7.canBeReplaced(new DirectionalPlaceContext(this.level, var2, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
                     boolean var9 = FallingBlock.isFree(this.level.getBlockState(var2.below())) && (!var3 || !var4);
                     boolean var10 = this.blockState.canSurvive(this.level, var2) && !var9;
                     if (var8 && var10) {
                        if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED) && this.level.getFluidState(var2).getType() == Fluids.WATER) {
                           this.blockState = this.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
                        }

                        if (this.level.setBlock(var2, this.blockState, 3)) {
                           ((ServerLevel)this.level)
                              .getChunkSource()
                              .chunkMap
                              .broadcast(this, new ClientboundBlockUpdatePacket(var2, this.level.getBlockState(var2)));
                           this.discard();
                           if (var1 instanceof Fallable) {
                              ((Fallable)var1).onLand(this.level, var2, this.blockState, var7, this);
                           }

                           if (this.blockData != null && this.blockState.hasBlockEntity()) {
                              BlockEntity var11 = this.level.getBlockEntity(var2);
                              if (var11 != null) {
                                 CompoundTag var12 = var11.saveWithoutMetadata();

                                 for (String var14 : this.blockData.getAllKeys()) {
                                    var12.put(var14, this.blockData.get(var14).copy());
                                 }

                                 try {
                                    var11.load(var12);
                                 } catch (Exception var13) {
                                    LOGGER.error("Failed to load block entity from falling block", var13);
                                 }

                                 var11.setChanged();
                              }
                           }
                        } else if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                           this.discard();
                           this.callOnBrokenAfterFall(var1, var2);
                           this.spawnAtLocation(var1);
                        }
                     } else {
                        this.discard();
                        if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                           this.callOnBrokenAfterFall(var1, var2);
                           this.spawnAtLocation(var1);
                        }
                     }
                  } else {
                     this.discard();
                     this.callOnBrokenAfterFall(var1, var2);
                  }
               }
            } else if (!this.level.isClientSide
               && (this.time > 100 && (var2.getY() <= this.level.getMinBuildHeight() || var2.getY() > this.level.getMaxBuildHeight()) || this.time > 600)) {
               if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                  this.spawnAtLocation(var1);
               }

               this.discard();
            }
         }

         this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
      }
   }

   public void callOnBrokenAfterFall(Block param0, BlockPos param1) {
      if (param0 instanceof Fallable) {
         ((Fallable)param0).onBrokenAfterFall(this.level, param1, this);
      }
   }

   @Override
   public boolean causeFallDamage(float param0, float param1, DamageSource param2) {
      if (!this.hurtEntities) {
         return false;
      }

      int var4 = Mth.ceil(param0 - 1.0F);
      if (var4 < 0) {
         return false;
      }

      Predicate<Entity> var5;
      DamageSource var6;
      if (this.blockState.getBlock() instanceof Fallable) {
         Fallable var7 = (Fallable)this.blockState.getBlock();
         var5 = var7.getHurtsEntitySelector();
         var6 = var7.getFallDamageSource(this);
      } else {
         var5 = EntitySelector.NO_SPECTATORS;
         var6 = this.damageSources().fallingBlock(this);
      }

      float var7 = Math.min(Mth.floor(var4 * this.fallDamagePerDistance), this.fallDamageMax);
      this.level.getEntities(this, this.getBoundingBox(), var5).forEach(l_param2 -> l_param2.hurt(var6, var7));
      boolean var8 = this.blockState.is(BlockTags.ANVIL);
      if (var8 && var7 > 0.0F && this.random.nextFloat() < 0.05F + var4 * 0.05F) {
         BlockState var9 = AnvilBlock.damage(this.blockState);
         if (var9 == null) {
            this.cancelDrop = true;
         } else {
            this.blockState = var9;
         }
      }

      return false;
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag param0) {
      param0.put("BlockState", NbtUtils.writeBlockState(this.blockState));
      param0.putInt("Time", this.time);
      param0.putBoolean("DropItem", this.dropItem);
      param0.putBoolean("HurtEntities", this.hurtEntities);
      param0.putFloat("FallHurtAmount", this.fallDamagePerDistance);
      param0.putInt("FallHurtMax", this.fallDamageMax);
      if (this.blockData != null) {
         param0.put("TileEntityData", this.blockData);
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag param0) {
      this.blockState = NbtUtils.readBlockState(this.level.holderLookup(Registries.BLOCK), param0.getCompound("BlockState"));
      this.time = param0.getInt("Time");
      if (param0.contains("HurtEntities", 99)) {
         this.hurtEntities = param0.getBoolean("HurtEntities");
         this.fallDamagePerDistance = param0.getFloat("FallHurtAmount");
         this.fallDamageMax = param0.getInt("FallHurtMax");
      } else if (this.blockState.is(BlockTags.ANVIL)) {
         this.hurtEntities = true;
      }

      if (param0.contains("DropItem", 99)) {
         this.dropItem = param0.getBoolean("DropItem");
      }

      if (param0.contains("TileEntityData", 10)) {
         this.blockData = param0.getCompound("TileEntityData");
      }

      if (this.blockState.isAir()) {
         this.blockState = Blocks.SAND.defaultBlockState();
      }
   }

   public void setHurtsEntities(float param0, int param1) {
      this.hurtEntities = true;
      this.fallDamagePerDistance = param0;
      this.fallDamageMax = param1;
   }

   public void disableDrop() {
      this.cancelDrop = true;
   }

   @Override
   public boolean displayFireAnimation() {
      return false;
   }

   @Override
   public void fillCrashReportCategory(CrashReportCategory param0) {
      super.fillCrashReportCategory(param0);
      param0.setDetail("Immitating BlockState", this.blockState.toString());
   }

   public BlockState getBlockState() {
      return this.blockState;
   }

   @Override
   protected Component getTypeName() {
      return Component.translatable("entity.minecraft.falling_block_type", this.blockState.getBlock().getName());
   }

   @Override
   public boolean onlyOpCanSetNbt() {
      return true;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this, Block.getId(this.getBlockState()));
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket param0) {
      super.recreateFromPacket(param0);
      this.blockState = Block.stateById(param0.getData());
      this.blocksBuilding = true;
      double var2 = param0.getX();
      double var4 = param0.getY();
      double var6 = param0.getZ();
      this.setPos(var2, var4, var6);
      this.setStartPos(this.blockPosition());
   }
}
