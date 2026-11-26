package net.minecraft.world.entity.decoration;

import com.mojang.logging.LogUtils;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public abstract class HangingEntity extends Entity {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected static final Predicate<Entity> HANGING_ENTITY = param0 -> param0 instanceof HangingEntity;
   private int checkInterval;
   protected BlockPos pos;
   protected Direction direction = Direction.SOUTH;

   protected HangingEntity(EntityType<? extends HangingEntity> param0, Level param1) {
      super(param0, param1);
   }

   protected HangingEntity(EntityType<? extends HangingEntity> param0, Level param1, BlockPos param2) {
      this(param0, param1);
      this.pos = param2;
   }

   @Override
   protected void defineSynchedData() {
   }

   protected void setDirection(Direction param0) {
      Validate.notNull(param0);
      Validate.isTrue(param0.getAxis().isHorizontal());
      this.direction = param0;
      this.setYRot(this.direction.get2DDataValue() * 90);
      this.yRotO = this.getYRot();
      this.recalculateBoundingBox();
   }

   protected void recalculateBoundingBox() {
      if (this.direction != null) {
         double var1 = this.pos.getX() + 0.5;
         double var3 = this.pos.getY() + 0.5;
         double var5 = this.pos.getZ() + 0.5;
         double var7 = 0.46875;
         double var9 = this.offs(this.getWidth());
         double var11 = this.offs(this.getHeight());
         var1 -= this.direction.getStepX() * 0.46875;
         var5 -= this.direction.getStepZ() * 0.46875;
         var3 += var11;
         Direction var13 = this.direction.getCounterClockWise();
         var1 += var9 * var13.getStepX();
         var5 += var9 * var13.getStepZ();
         this.setPosRaw(var1, var3, var5);
         double var14 = this.getWidth();
         double var16 = this.getHeight();
         double var18 = this.getWidth();
         if (this.direction.getAxis() == Direction.Axis.Z) {
            var18 = 1.0;
         } else {
            var14 = 1.0;
         }

         var14 /= 32.0;
         var16 /= 32.0;
         var18 /= 32.0;
         this.setBoundingBox(new AABB(var1 - var14, var3 - var16, var5 - var18, var1 + var14, var3 + var16, var5 + var18));
      }
   }

   private double offs(int param0) {
      return param0 % 32 == 0 ? 0.5 : 0.0;
   }

   @Override
   public void tick() {
      if (!this.level.isClientSide) {
         this.checkOutOfWorld();
         if (this.checkInterval++ == 100) {
            this.checkInterval = 0;
            if (!this.isRemoved() && !this.survives()) {
               this.discard();
               this.dropItem(null);
            }
         }
      }
   }

   public boolean survives() {
      if (!this.level.noCollision(this)) {
         return false;
      }

      int var1 = Math.max(1, this.getWidth() / 16);
      int var2 = Math.max(1, this.getHeight() / 16);
      BlockPos var3 = this.pos.relative(this.direction.getOpposite());
      Direction var4 = this.direction.getCounterClockWise();
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();

      for (int var6 = 0; var6 < var1; var6++) {
         for (int var7 = 0; var7 < var2; var7++) {
            int var8 = (var1 - 1) / -2;
            int var9 = (var2 - 1) / -2;
            var5.set(var3).move(var4, var6 + var8).move(Direction.UP, var7 + var9);
            BlockState var10 = this.level.getBlockState(var5);
            if (!var10.getMaterial().isSolid() && !DiodeBlock.isDiode(var10)) {
               return false;
            }
         }
      }

      return this.level.getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty();
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public boolean skipAttackInteraction(Entity param0) {
      if (param0 instanceof Player var2) {
         return !this.level.mayInteract(var2, this.pos) ? true : this.hurt(this.damageSources().playerAttack(var2), 0.0F);
      } else {
         return false;
      }
   }

   @Override
   public Direction getDirection() {
      return this.direction;
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      if (!this.isRemoved() && !this.level.isClientSide) {
         this.kill();
         this.markHurt();
         this.dropItem(param0.getEntity());
      }

      return true;
   }

   @Override
   public void move(MoverType param0, Vec3 param1) {
      if (!this.level.isClientSide && !this.isRemoved() && param1.lengthSqr() > 0.0) {
         this.kill();
         this.dropItem(null);
      }
   }

   @Override
   public void push(double param0, double param2, double param4) {
      if (!this.level.isClientSide && !this.isRemoved() && param0 * param0 + param2 * param2 + param4 * param4 > 0.0) {
         this.kill();
         this.dropItem(null);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      BlockPos var2 = this.getPos();
      param0.putInt("TileX", var2.getX());
      param0.putInt("TileY", var2.getY());
      param0.putInt("TileZ", var2.getZ());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      BlockPos var2 = new BlockPos(param0.getInt("TileX"), param0.getInt("TileY"), param0.getInt("TileZ"));
      if (!var2.closerThan(this.blockPosition(), 16.0)) {
         LOGGER.error("Hanging entity at invalid position: {}", var2);
      } else {
         this.pos = var2;
      }
   }

   public abstract int getWidth();

   public abstract int getHeight();

   public abstract void dropItem(@Nullable Entity var1);

   public abstract void playPlacementSound();

   @Override
   public ItemEntity spawnAtLocation(ItemStack param0, float param1) {
      ItemEntity var3 = new ItemEntity(
         this.level, this.getX() + this.direction.getStepX() * 0.15F, this.getY() + param1, this.getZ() + this.direction.getStepZ() * 0.15F, param0
      );
      var3.setDefaultPickUpDelay();
      this.level.addFreshEntity(var3);
      return var3;
   }

   @Override
   protected boolean repositionEntityAfterLoad() {
      return false;
   }

   @Override
   public void setPos(double param0, double param2, double param4) {
      this.pos = BlockPos.containing(param0, param2, param4);
      this.recalculateBoundingBox();
      this.hasImpulse = true;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   @Override
   public float rotate(Rotation param0) {
      if (this.direction.getAxis() != Direction.Axis.Y) {
         switch (param0) {
            case CLOCKWISE_180:
               this.direction = this.direction.getOpposite();
               break;
            case COUNTERCLOCKWISE_90:
               this.direction = this.direction.getCounterClockWise();
               break;
            case CLOCKWISE_90:
               this.direction = this.direction.getClockWise();
         }
      }

      float var2 = Mth.wrapDegrees(this.getYRot());
      switch (param0) {
         case CLOCKWISE_180:
            return var2 + 180.0F;
         case COUNTERCLOCKWISE_90:
            return var2 + 90.0F;
         case CLOCKWISE_90:
            return var2 + 270.0F;
         default:
            return var2;
      }
   }

   @Override
   public float mirror(Mirror param0) {
      return this.rotate(param0.getRotation(this.direction));
   }

   @Override
   public void thunderHit(ServerLevel param0, LightningBolt param1) {
   }

   @Override
   public void refreshDimensions() {
   }
}
