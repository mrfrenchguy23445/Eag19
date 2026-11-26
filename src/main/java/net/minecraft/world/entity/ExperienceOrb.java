package net.minecraft.world.entity;

import java.util.List;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ExperienceOrb extends Entity {
   private static final int LIFETIME = 6000;
   private static final int ENTITY_SCAN_PERIOD = 20;
   private static final int MAX_FOLLOW_DIST = 8;
   private static final int ORB_GROUPS_PER_AREA = 40;
   private static final double ORB_MERGE_DISTANCE = 0.5;
   private int age;
   private int health = 5;
   private int value;
   private int count = 1;
   private Player followingPlayer;

   public ExperienceOrb(Level param0, double param1, double param3, double param5, int param7) {
      this(EntityType.EXPERIENCE_ORB, param0);
      this.setPos(param1, param3, param5);
      this.setYRot((float)(this.random.nextDouble() * 360.0));
      this.setDeltaMovement(
         (this.random.nextDouble() * 0.2F - 0.1F) * 2.0, this.random.nextDouble() * 0.2 * 2.0, (this.random.nextDouble() * 0.2F - 0.1F) * 2.0
      );
      this.value = param7;
   }

   public ExperienceOrb(EntityType<? extends ExperienceOrb> param0, Level param1) {
      super(param0, param1);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   @Override
   protected void defineSynchedData() {
   }

   @Override
   public void tick() {
      super.tick();
      this.xo = this.getX();
      this.yo = this.getY();
      this.zo = this.getZ();
      if (this.isEyeInFluid(FluidTags.WATER)) {
         this.setUnderwaterMovement();
      } else if (!this.isNoGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.03, 0.0));
      }

      if (this.level.getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
         this.setDeltaMovement((this.random.nextFloat() - this.random.nextFloat()) * 0.2F, 0.2F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
      }

      if (!this.level.noCollision(this.getBoundingBox())) {
         this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
      }

      if (this.tickCount % 20 == 1) {
         this.scanForEntities();
      }

      if (this.followingPlayer != null && (this.followingPlayer.isSpectator() || this.followingPlayer.isDeadOrDying())) {
         this.followingPlayer = null;
      }

      if (this.followingPlayer != null) {
         Vec3 var1 = new Vec3(
            this.followingPlayer.getX() - this.getX(),
            this.followingPlayer.getY() + this.followingPlayer.getEyeHeight() / 2.0 - this.getY(),
            this.followingPlayer.getZ() - this.getZ()
         );
         double var2 = var1.lengthSqr();
         if (var2 < 64.0) {
            double var4 = 1.0 - Math.sqrt(var2) / 8.0;
            this.setDeltaMovement(this.getDeltaMovement().add(var1.normalize().scale(var4 * var4 * 0.1)));
         }
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      float var1 = 0.98F;
      if (this.onGround) {
         var1 = this.level.getBlockState(BlockPos.containing(this.getX(), this.getY() - 1.0, this.getZ())).getBlock().getFriction() * 0.98F;
      }

      this.setDeltaMovement(this.getDeltaMovement().multiply(var1, 0.98, var1));
      if (this.onGround) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, -0.9, 1.0));
      }

      this.age++;
      if (this.age >= 6000) {
         this.discard();
      }
   }

   private void scanForEntities() {
      if (this.followingPlayer == null || this.followingPlayer.distanceToSqr(this) > 64.0) {
         this.followingPlayer = this.level.getNearestPlayer(this, 8.0);
      }

      if (this.level instanceof ServerLevel) {
         for (ExperienceOrb var3 : this.level.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), this.getBoundingBox().inflate(0.5), this::canMerge)) {
            this.merge(var3);
         }
      }
   }

   public static void award(ServerLevel param0, Vec3 param1, int param2) {
      while (param2 > 0) {
         int var3 = getExperienceValue(param2);
         param2 -= var3;
         if (!tryMergeToExisting(param0, param1, var3)) {
            param0.addFreshEntity(new ExperienceOrb(param0, param1.x(), param1.y(), param1.z(), var3));
         }
      }
   }

   private static boolean tryMergeToExisting(ServerLevel param0, Vec3 param1, int param2) {
      AABB var3 = AABB.ofSize(param1, 1.0, 1.0, 1.0);
      int var4 = param0.getRandom().nextInt(40);
      List<ExperienceOrb> var5 = param0.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), var3, l_param2 -> canMerge(l_param2, var4, param2));
      if (!var5.isEmpty()) {
         ExperienceOrb var6 = var5.get(0);
         var6.count++;
         var6.age = 0;
         return true;
      } else {
         return false;
      }
   }

   private boolean canMerge(ExperienceOrb param0) {
      return param0 != this && canMerge(param0, this.getId(), this.value);
   }

   private static boolean canMerge(ExperienceOrb param0, int param1, int param2) {
      return !param0.isRemoved() && (param0.getId() - param1) % 40 == 0 && param0.value == param2;
   }

   private void merge(ExperienceOrb param0) {
      this.count = this.count + param0.count;
      this.age = Math.min(this.age, param0.age);
      param0.discard();
   }

   private void setUnderwaterMovement() {
      Vec3 var1 = this.getDeltaMovement();
      this.setDeltaMovement(var1.x * 0.99F, Math.min(var1.y + 5.0E-4F, 0.06F), var1.z * 0.99F);
   }

   @Override
   protected void doWaterSplashEffect() {
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      if (this.level.isClientSide) {
         return true;
      }

      this.markHurt();
      this.health = (int)(this.health - param1);
      if (this.health <= 0) {
         this.discard();
      }

      return true;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      param0.putShort("Health", (short)this.health);
      param0.putShort("Age", (short)this.age);
      param0.putShort("Value", (short)this.value);
      param0.putInt("Count", this.count);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      this.health = param0.getShort("Health");
      this.age = param0.getShort("Age");
      this.value = param0.getShort("Value");
      this.count = Math.max(param0.getInt("Count"), 1);
   }

   @Override
   public void playerTouch(Player param0) {
      if (!this.level.isClientSide) {
         if (param0.takeXpDelay == 0) {
            param0.takeXpDelay = 2;
            param0.take(this, 1);
            int var2 = this.repairPlayerItems(param0, this.value);
            if (var2 > 0) {
               param0.giveExperiencePoints(var2);
            }

            this.count--;
            if (this.count == 0) {
               this.discard();
            }
         }
      }
   }

   private int repairPlayerItems(Player param0, int param1) {
      Entry<EquipmentSlot, ItemStack> var3 = EnchantmentHelper.getRandomItemWith(Enchantments.MENDING, param0, ItemStack::isDamaged);
      if (var3 != null) {
         ItemStack var4 = var3.getValue();
         int var5 = Math.min(this.xpToDurability(this.value), var4.getDamageValue());
         var4.setDamageValue(var4.getDamageValue() - var5);
         int var6 = param1 - this.durabilityToXp(var5);
         return var6 > 0 ? this.repairPlayerItems(param0, var6) : 0;
      } else {
         return param1;
      }
   }

   private int durabilityToXp(int param0) {
      return param0 / 2;
   }

   private int xpToDurability(int param0) {
      return param0 * 2;
   }

   public int getValue() {
      return this.value;
   }

   public int getIcon() {
      if (this.value >= 2477) {
         return 10;
      } else if (this.value >= 1237) {
         return 9;
      } else if (this.value >= 617) {
         return 8;
      } else if (this.value >= 307) {
         return 7;
      } else if (this.value >= 149) {
         return 6;
      } else if (this.value >= 73) {
         return 5;
      } else if (this.value >= 37) {
         return 4;
      } else if (this.value >= 17) {
         return 3;
      } else if (this.value >= 7) {
         return 2;
      } else {
         return this.value >= 3 ? 1 : 0;
      }
   }

   public static int getExperienceValue(int param0) {
      if (param0 >= 2477) {
         return 2477;
      } else if (param0 >= 1237) {
         return 1237;
      } else if (param0 >= 617) {
         return 617;
      } else if (param0 >= 307) {
         return 307;
      } else if (param0 >= 149) {
         return 149;
      } else if (param0 >= 73) {
         return 73;
      } else if (param0 >= 37) {
         return 37;
      } else if (param0 >= 17) {
         return 17;
      } else if (param0 >= 7) {
         return 7;
      } else {
         return param0 >= 3 ? 3 : 1;
      }
   }

   @Override
   public boolean isAttackable() {
      return false;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddExperienceOrbPacket(this);
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.AMBIENT;
   }
}
