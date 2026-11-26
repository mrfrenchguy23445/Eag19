package net.minecraft.world.entity.decoration;

import com.mojang.logging.LogUtils;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class ItemFrame extends HangingEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.ITEM_STACK);
   private static final EntityDataAccessor<Integer> DATA_ROTATION = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.INT);
   public static final int NUM_ROTATIONS = 8;
   private float dropChance = 1.0F;
   private boolean fixed;

   public ItemFrame(EntityType<? extends ItemFrame> param0, Level param1) {
      super(param0, param1);
   }

   public ItemFrame(Level param0, BlockPos param1, Direction param2) {
      this(EntityType.ITEM_FRAME, param0, param1, param2);
   }

   public ItemFrame(EntityType<? extends ItemFrame> param0, Level param1, BlockPos param2, Direction param3) {
      super(param0, param1, param2);
      this.setDirection(param3);
   }

   @Override
   protected float getEyeHeight(Pose param0, EntityDimensions param1) {
      return 0.0F;
   }

   @Override
   protected void defineSynchedData() {
      this.getEntityData().define(DATA_ITEM, ItemStack.EMPTY);
      this.getEntityData().define(DATA_ROTATION, 0);
   }

   @Override
   protected void setDirection(Direction param0) {
      Validate.notNull(param0);
      this.direction = param0;
      if (param0.getAxis().isHorizontal()) {
         this.setXRot(0.0F);
         this.setYRot(this.direction.get2DDataValue() * 90);
      } else {
         this.setXRot(-90 * param0.getAxisDirection().getStep());
         this.setYRot(0.0F);
      }

      this.xRotO = this.getXRot();
      this.yRotO = this.getYRot();
      this.recalculateBoundingBox();
   }

   @Override
   protected void recalculateBoundingBox() {
      if (this.direction != null) {
         double var1 = 0.46875;
         double var3 = this.pos.getX() + 0.5 - this.direction.getStepX() * 0.46875;
         double var5 = this.pos.getY() + 0.5 - this.direction.getStepY() * 0.46875;
         double var7 = this.pos.getZ() + 0.5 - this.direction.getStepZ() * 0.46875;
         this.setPosRaw(var3, var5, var7);
         double var9 = this.getWidth();
         double var11 = this.getHeight();
         double var13 = this.getWidth();
         Direction.Axis var15 = this.direction.getAxis();
         switch (var15) {
            case X:
               var9 = 1.0;
               break;
            case Y:
               var11 = 1.0;
               break;
            case Z:
               var13 = 1.0;
         }

         var9 /= 32.0;
         var11 /= 32.0;
         var13 /= 32.0;
         this.setBoundingBox(new AABB(var3 - var9, var5 - var11, var7 - var13, var3 + var9, var5 + var11, var7 + var13));
      }
   }

   @Override
   public boolean survives() {
      if (this.fixed) {
         return true;
      }

      if (!this.level.noCollision(this)) {
         return false;
      }

      BlockState var1 = this.level.getBlockState(this.pos.relative(this.direction.getOpposite()));
      return var1.getMaterial().isSolid() || this.direction.getAxis().isHorizontal() && DiodeBlock.isDiode(var1)
         ? this.level.getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty()
         : false;
   }

   @Override
   public void move(MoverType param0, Vec3 param1) {
      if (!this.fixed) {
         super.move(param0, param1);
      }
   }

   @Override
   public void push(double param0, double param2, double param4) {
      if (!this.fixed) {
         super.push(param0, param2, param4);
      }
   }

   @Override
   public float getPickRadius() {
      return 0.0F;
   }

   @Override
   public void kill() {
      this.removeFramedMap(this.getItem());
      super.kill();
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.fixed) {
         return !param0.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !param0.isCreativePlayer() ? false : super.hurt(param0, param1);
      }

      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      if (!param0.is(DamageTypeTags.IS_EXPLOSION) && !this.getItem().isEmpty()) {
         if (!this.level.isClientSide) {
            this.dropItem(param0.getEntity(), false);
            this.gameEvent(GameEvent.BLOCK_CHANGE, param0.getEntity());
            this.playSound(this.getRemoveItemSound(), 1.0F, 1.0F);
         }

         return true;
      } else {
         return super.hurt(param0, param1);
      }
   }

   public SoundEvent getRemoveItemSound() {
      return SoundEvents.ITEM_FRAME_REMOVE_ITEM;
   }

   @Override
   public int getWidth() {
      return 12;
   }

   @Override
   public int getHeight() {
      return 12;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double param0) {
      double var3 = 16.0;
      var3 *= 64.0 * getViewScale();
      return param0 < var3 * var3;
   }

   @Override
   public void dropItem(@Nullable Entity param0) {
      this.playSound(this.getBreakSound(), 1.0F, 1.0F);
      this.dropItem(param0, true);
      this.gameEvent(GameEvent.BLOCK_CHANGE, param0);
   }

   public SoundEvent getBreakSound() {
      return SoundEvents.ITEM_FRAME_BREAK;
   }

   @Override
   public void playPlacementSound() {
      this.playSound(this.getPlaceSound(), 1.0F, 1.0F);
   }

   public SoundEvent getPlaceSound() {
      return SoundEvents.ITEM_FRAME_PLACE;
   }

   private void dropItem(@Nullable Entity param0, boolean param1) {
      if (!this.fixed) {
         ItemStack var3 = this.getItem();
         this.setItem(ItemStack.EMPTY);
         if (!this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            if (param0 == null) {
               this.removeFramedMap(var3);
            }
         } else if (param0 instanceof Player var4 && var4.getAbilities().instabuild) {
            this.removeFramedMap(var3);
         } else {
            if (param1) {
               this.spawnAtLocation(this.getFrameItemStack());
            }

            if (!var3.isEmpty()) {
               var3 = var3.copy();
               this.removeFramedMap(var3);
               if (this.random.nextFloat() < this.dropChance) {
                  this.spawnAtLocation(var3);
               }
            }
         }
      }
   }

   private void removeFramedMap(ItemStack param0) {
      this.getFramedMapId().ifPresent(l_param0 -> {
         MapItemSavedData l_var2 = MapItem.getSavedData(l_param0, this.level);
         if (l_var2 != null) {
            l_var2.removedFromFrame(this.pos, this.getId());
            l_var2.setDirty(true);
         }
      });
      param0.setEntityRepresentation(null);
   }

   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM);
   }

   public OptionalInt getFramedMapId() {
      ItemStack var1 = this.getItem();
      if (var1.is(Items.FILLED_MAP)) {
         Integer var2 = MapItem.getMapId(var1);
         if (var2 != null) {
            return OptionalInt.of(var2);
         }
      }

      return OptionalInt.empty();
   }

   public boolean hasFramedMap() {
      return this.getFramedMapId().isPresent();
   }

   public void setItem(ItemStack param0) {
      this.setItem(param0, true);
   }

   public void setItem(ItemStack param0, boolean param1) {
      if (!param0.isEmpty()) {
         param0 = param0.copy();
         param0.setCount(1);
      }

      this.onItemChanged(param0);
      this.getEntityData().set(DATA_ITEM, param0);
      if (!param0.isEmpty()) {
         this.playSound(this.getAddItemSound(), 1.0F, 1.0F);
      }

      if (param1 && this.pos != null) {
         this.level.updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
      }
   }

   public SoundEvent getAddItemSound() {
      return SoundEvents.ITEM_FRAME_ADD_ITEM;
   }

   @Override
   public SlotAccess getSlot(int param0) {
      return param0 == 0 ? new SlotAccess() {
         @Override
         public ItemStack get() {
            return ItemFrame.this.getItem();
         }

         @Override
         public boolean set(ItemStack l_param0) {
            ItemFrame.this.setItem(l_param0);
            return true;
         }
      } : super.getSlot(param0);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> param0) {
      if (param0.equals(DATA_ITEM)) {
         this.onItemChanged(this.getItem());
      }
   }

   private void onItemChanged(ItemStack param0) {
      if (!param0.isEmpty() && param0.getFrame() != this) {
         param0.setEntityRepresentation(this);
      }

      this.recalculateBoundingBox();
   }

   public int getRotation() {
      return this.getEntityData().get(DATA_ROTATION);
   }

   public void setRotation(int param0) {
      this.setRotation(param0, true);
   }

   private void setRotation(int param0, boolean param1) {
      this.getEntityData().set(DATA_ROTATION, param0 % 8);
      if (param1 && this.pos != null) {
         this.level.updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      if (!this.getItem().isEmpty()) {
         param0.put("Item", this.getItem().save(new CompoundTag()));
         param0.putByte("ItemRotation", (byte)this.getRotation());
         param0.putFloat("ItemDropChance", this.dropChance);
      }

      param0.putByte("Facing", (byte)this.direction.get3DDataValue());
      param0.putBoolean("Invisible", this.isInvisible());
      param0.putBoolean("Fixed", this.fixed);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      CompoundTag var2 = param0.getCompound("Item");
      if (var2 != null && !var2.isEmpty()) {
         ItemStack var3 = ItemStack.of(var2);
         if (var3.isEmpty()) {
            LOGGER.warn("Unable to load item from: {}", var2);
         }

         ItemStack var4 = this.getItem();
         if (!var4.isEmpty() && !ItemStack.matches(var3, var4)) {
            this.removeFramedMap(var4);
         }

         this.setItem(var3, false);
         this.setRotation(param0.getByte("ItemRotation"), false);
         if (param0.contains("ItemDropChance", 99)) {
            this.dropChance = param0.getFloat("ItemDropChance");
         }
      }

      this.setDirection(Direction.from3DDataValue(param0.getByte("Facing")));
      this.setInvisible(param0.getBoolean("Invisible"));
      this.fixed = param0.getBoolean("Fixed");
   }

   @Override
   public InteractionResult interact(Player param0, InteractionHand param1) {
      ItemStack var3 = param0.getItemInHand(param1);
      boolean var4 = !this.getItem().isEmpty();
      boolean var5 = !var3.isEmpty();
      if (this.fixed) {
         return InteractionResult.PASS;
      }

      if (!this.level.isClientSide) {
         if (!var4) {
            if (var5 && !this.isRemoved()) {
               if (var3.is(Items.FILLED_MAP)) {
                  MapItemSavedData var6 = MapItem.getSavedData(var3, this.level);
                  if (var6 != null && var6.isTrackedCountOverLimit(256)) {
                     return InteractionResult.FAIL;
                  }
               }

               this.setItem(var3);
               this.gameEvent(GameEvent.BLOCK_CHANGE, param0);
               if (!param0.getAbilities().instabuild) {
                  var3.shrink(1);
               }
            }
         } else {
            this.playSound(this.getRotateItemSound(), 1.0F, 1.0F);
            this.setRotation(this.getRotation() + 1);
            this.gameEvent(GameEvent.BLOCK_CHANGE, param0);
         }

         return InteractionResult.CONSUME;
      } else {
         return !var4 && !var5 ? InteractionResult.PASS : InteractionResult.SUCCESS;
      }
   }

   public SoundEvent getRotateItemSound() {
      return SoundEvents.ITEM_FRAME_ROTATE_ITEM;
   }

   public int getAnalogOutput() {
      return this.getItem().isEmpty() ? 0 : this.getRotation() % 8 + 1;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this, this.direction.get3DDataValue(), this.getPos());
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket param0) {
      super.recreateFromPacket(param0);
      this.setDirection(Direction.from3DDataValue(param0.getData()));
   }

   @Override
   public ItemStack getPickResult() {
      ItemStack var1 = this.getItem();
      return var1.isEmpty() ? this.getFrameItemStack() : var1.copy();
   }

   protected ItemStack getFrameItemStack() {
      return new ItemStack(Items.ITEM_FRAME);
   }

   @Override
   public float getVisualRotationYInDegrees() {
      Direction var1 = this.getDirection();
      int var2 = var1.getAxis().isVertical() ? 90 * var1.getAxisDirection().getStep() : 0;
      return Mth.wrapDegrees(180 + var1.get2DDataValue() * 90 + this.getRotation() * 45 + var2);
   }
}
