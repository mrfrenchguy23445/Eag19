package net.minecraft.world.entity.decoration;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LeashFenceKnotEntity extends HangingEntity {
   public static final double OFFSET_Y = 0.375;

   public LeashFenceKnotEntity(EntityType<? extends LeashFenceKnotEntity> param0, Level param1) {
      super(param0, param1);
   }

   public LeashFenceKnotEntity(Level param0, BlockPos param1) {
      super(EntityType.LEASH_KNOT, param0, param1);
      this.setPos(param1.getX(), param1.getY(), param1.getZ());
   }

   @Override
   protected void recalculateBoundingBox() {
      this.setPosRaw(this.pos.getX() + 0.5, this.pos.getY() + 0.375, this.pos.getZ() + 0.5);
      double var1 = this.getType().getWidth() / 2.0;
      double var3 = this.getType().getHeight();
      this.setBoundingBox(new AABB(this.getX() - var1, this.getY(), this.getZ() - var1, this.getX() + var1, this.getY() + var3, this.getZ() + var1));
   }

   @Override
   public void setDirection(Direction param0) {
   }

   @Override
   public int getWidth() {
      return 9;
   }

   @Override
   public int getHeight() {
      return 9;
   }

   @Override
   protected float getEyeHeight(Pose param0, EntityDimensions param1) {
      return 0.0625F;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double param0) {
      return param0 < 1024.0;
   }

   @Override
   public void dropItem(@Nullable Entity param0) {
      this.playSound(SoundEvents.LEASH_KNOT_BREAK, 1.0F, 1.0F);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
   }

   @Override
   public InteractionResult interact(Player param0, InteractionHand param1) {
      if (this.level.isClientSide) {
         return InteractionResult.SUCCESS;
      }

      boolean var3 = false;
      double var4 = 7.0;
      List<Mob> var6 = this.level
         .getEntitiesOfClass(
            Mob.class, new AABB(this.getX() - 7.0, this.getY() - 7.0, this.getZ() - 7.0, this.getX() + 7.0, this.getY() + 7.0, this.getZ() + 7.0)
         );

      for (Mob var8 : var6) {
         if (var8.getLeashHolder() == param0) {
            var8.setLeashedTo(this, true);
            var3 = true;
         }
      }

      boolean var7 = false;
      if (!var3) {
         this.discard();
         if (param0.getAbilities().instabuild) {
            for (Mob var9 : var6) {
               if (var9.isLeashed() && var9.getLeashHolder() == this) {
                  var9.dropLeash(true, false);
                  var7 = true;
               }
            }
         }
      }

      if (var3 || var7) {
         this.gameEvent(GameEvent.BLOCK_ATTACH, param0);
      }

      return InteractionResult.CONSUME;
   }

   @Override
   public boolean survives() {
      return this.level.getBlockState(this.pos).is(BlockTags.FENCES);
   }

   public static LeashFenceKnotEntity getOrCreateKnot(Level param0, BlockPos param1) {
      int var2 = param1.getX();
      int var3 = param1.getY();
      int var4 = param1.getZ();

      for (LeashFenceKnotEntity var7 : param0.getEntitiesOfClass(
         LeashFenceKnotEntity.class, new AABB(var2 - 1.0, var3 - 1.0, var4 - 1.0, var2 + 1.0, var3 + 1.0, var4 + 1.0)
      )) {
         if (var7.getPos().equals(param1)) {
            return var7;
         }
      }

      LeashFenceKnotEntity var6 = new LeashFenceKnotEntity(param0, param1);
      param0.addFreshEntity(var6);
      return var6;
   }

   @Override
   public void playPlacementSound() {
      this.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.0F);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this, 0, this.getPos());
   }

   @Override
   public Vec3 getRopeHoldPosition(float param0) {
      return this.getPosition(param0).add(0.0, 0.2, 0.0);
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.LEAD);
   }
}
