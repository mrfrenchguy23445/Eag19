package net.minecraft.world.entity.boss.enderdragon;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class EnderDragon extends Mob implements Enemy {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(EnderDragon.class, EntityDataSerializers.INT);
   private static final TargetingConditions CRYSTAL_DESTROY_TARGETING = TargetingConditions.forCombat().range(64.0);
   private static final int GROWL_INTERVAL_MIN = 200;
   private static final int GROWL_INTERVAL_MAX = 400;
   private static final float SITTING_ALLOWED_DAMAGE_PERCENTAGE = 0.25F;
   private static final String DRAGON_DEATH_TIME_KEY = "DragonDeathTime";
   private static final String DRAGON_PHASE_KEY = "DragonPhase";
   public final double[][] positions = new double[64][3];
   public int posPointer = -1;
   private final EnderDragonPart[] subEntities;
   public final EnderDragonPart head;
   private final EnderDragonPart neck;
   private final EnderDragonPart body;
   private final EnderDragonPart tail1;
   private final EnderDragonPart tail2;
   private final EnderDragonPart tail3;
   private final EnderDragonPart wing1;
   private final EnderDragonPart wing2;
   public float oFlapTime;
   public float flapTime;
   public boolean inWall;
   public int dragonDeathTime;
   public float yRotA;
   @Nullable
   public EndCrystal nearestCrystal;
   @Nullable
   private final EndDragonFight dragonFight;
   private final EnderDragonPhaseManager phaseManager;
   private int growlTime = 100;
   private float sittingDamageReceived;
   private final Node[] nodes = new Node[24];
   private final int[] nodeAdjacency = new int[24];
   private final BinaryHeap openSet = new BinaryHeap();

   public EnderDragon(EntityType<? extends EnderDragon> param0, Level param1) {
      super(EntityType.ENDER_DRAGON, param1);
      this.head = new EnderDragonPart(this, "head", 1.0F, 1.0F);
      this.neck = new EnderDragonPart(this, "neck", 3.0F, 3.0F);
      this.body = new EnderDragonPart(this, "body", 5.0F, 3.0F);
      this.tail1 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
      this.tail2 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
      this.tail3 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
      this.wing1 = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
      this.wing2 = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
      this.subEntities = new EnderDragonPart[]{this.head, this.neck, this.body, this.tail1, this.tail2, this.tail3, this.wing1, this.wing2};
      this.setHealth(this.getMaxHealth());
      this.noPhysics = true;
      this.noCulling = true;
      if (param1 instanceof ServerLevel) {
         this.dragonFight = ((ServerLevel)param1).dragonFight();
      } else {
         this.dragonFight = null;
      }

      this.phaseManager = new EnderDragonPhaseManager(this);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 200.0);
   }

   @Override
   public boolean isFlapping() {
      float var1 = Mth.cos(this.flapTime * (float) (Math.PI * 2));
      float var2 = Mth.cos(this.oFlapTime * (float) (Math.PI * 2));
      return var2 <= -0.3F && var1 >= -0.3F;
   }

   @Override
   public void onFlap() {
      if (this.level.isClientSide && !this.isSilent()) {
         this.level
            .playLocalSound(
               this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, this.getSoundSource(), 5.0F, 0.8F + this.random.nextFloat() * 0.3F, false
            );
      }
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.getEntityData().define(DATA_PHASE, EnderDragonPhase.HOVERING.getId());
   }

   public double[] getLatencyPos(int param0, float param1) {
      if (this.isDeadOrDying()) {
         param1 = 0.0F;
      }

      param1 = 1.0F - param1;
      int var3 = this.posPointer - param0 & 63;
      int var4 = this.posPointer - param0 - 1 & 63;
      double[] var5 = new double[3];
      double var6 = this.positions[var3][0];
      double var8 = Mth.wrapDegrees(this.positions[var4][0] - var6);
      var5[0] = var6 + var8 * param1;
      var6 = this.positions[var3][1];
      var8 = this.positions[var4][1] - var6;
      var5[1] = var6 + var8 * param1;
      var5[2] = Mth.lerp(param1, this.positions[var3][2], this.positions[var4][2]);
      return var5;
   }

   @Override
   public void aiStep() {
      this.processFlappingMovement();
      if (this.level.isClientSide) {
         this.setHealth(this.getHealth());
         if (!this.isSilent() && !this.phaseManager.getCurrentPhase().isSitting() && --this.growlTime < 0) {
            this.level
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.ENDER_DRAGON_GROWL,
                  this.getSoundSource(),
                  2.5F,
                  0.8F + this.random.nextFloat() * 0.3F,
                  false
               );
            this.growlTime = 200 + this.random.nextInt(200);
         }
      }

      this.oFlapTime = this.flapTime;
      if (this.isDeadOrDying()) {
         float var1 = (this.random.nextFloat() - 0.5F) * 8.0F;
         float var2 = (this.random.nextFloat() - 0.5F) * 4.0F;
         float var3 = (this.random.nextFloat() - 0.5F) * 8.0F;
         this.level.addParticle(ParticleTypes.EXPLOSION, this.getX() + var1, this.getY() + 2.0 + var2, this.getZ() + var3, 0.0, 0.0, 0.0);
      } else {
         this.checkCrystals();
         Vec3 var1 = this.getDeltaMovement();
         float var2 = 0.2F / ((float)var1.horizontalDistance() * 10.0F + 1.0F);
         var2 *= (float)Math.pow(2.0, var1.y);
         if (this.phaseManager.getCurrentPhase().isSitting()) {
            this.flapTime += 0.1F;
         } else if (this.inWall) {
            this.flapTime += var2 * 0.5F;
         } else {
            this.flapTime += var2;
         }

         this.setYRot(Mth.wrapDegrees(this.getYRot()));
         if (this.isNoAi()) {
            this.flapTime = 0.5F;
         } else {
            if (this.posPointer < 0) {
               for (int var3 = 0; var3 < this.positions.length; var3++) {
                  this.positions[var3][0] = this.getYRot();
                  this.positions[var3][1] = this.getY();
               }
            }

            if (++this.posPointer == this.positions.length) {
               this.posPointer = 0;
            }

            this.positions[this.posPointer][0] = this.getYRot();
            this.positions[this.posPointer][1] = this.getY();
            if (this.level.isClientSide) {
               if (this.lerpSteps > 0) {
                  double var3 = this.getX() + (this.lerpX - this.getX()) / this.lerpSteps;
                  double var5 = this.getY() + (this.lerpY - this.getY()) / this.lerpSteps;
                  double var7 = this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps;
                  double var9 = Mth.wrapDegrees(this.lerpYRot - this.getYRot());
                  this.setYRot(this.getYRot() + (float)var9 / this.lerpSteps);
                  this.setXRot(this.getXRot() + (float)(this.lerpXRot - this.getXRot()) / this.lerpSteps);
                  this.lerpSteps--;
                  this.setPos(var3, var5, var7);
                  this.setRot(this.getYRot(), this.getXRot());
               }

               this.phaseManager.getCurrentPhase().doClientTick();
            } else {
               DragonPhaseInstance var3 = this.phaseManager.getCurrentPhase();
               var3.doServerTick();
               if (this.phaseManager.getCurrentPhase() != var3) {
                  var3 = this.phaseManager.getCurrentPhase();
                  var3.doServerTick();
               }

               Vec3 var4 = var3.getFlyTargetLocation();
               if (var4 != null) {
                  double var5 = var4.x - this.getX();
                  double var7 = var4.y - this.getY();
                  double var9 = var4.z - this.getZ();
                  double var11 = var5 * var5 + var7 * var7 + var9 * var9;
                  float var13 = var3.getFlySpeed();
                  double var14 = Math.sqrt(var5 * var5 + var9 * var9);
                  if (var14 > 0.0) {
                     var7 = Mth.clamp(var7 / var14, -var13, var13);
                  }

                  this.setDeltaMovement(this.getDeltaMovement().add(0.0, var7 * 0.01, 0.0));
                  this.setYRot(Mth.wrapDegrees(this.getYRot()));
                  Vec3 var16 = var4.subtract(this.getX(), this.getY(), this.getZ()).normalize();
                  Vec3 var17 = new Vec3(
                        Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), this.getDeltaMovement().y, -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0))
                     )
                     .normalize();
                  float var18 = Math.max(((float)var17.dot(var16) + 0.5F) / 1.5F, 0.0F);
                  if (Math.abs(var5) > 1.0E-5F || Math.abs(var9) > 1.0E-5F) {
                     float var19 = Mth.clamp(Mth.wrapDegrees(180.0F - (float)Mth.atan2(var5, var9) * (180.0F / (float)Math.PI) - this.getYRot()), -50.0F, 50.0F);
                     this.yRotA *= 0.8F;
                     this.yRotA = this.yRotA + var19 * var3.getTurnSpeed();
                     this.setYRot(this.getYRot() + this.yRotA * 0.1F);
                  }

                  float var19 = (float)(2.0 / (var11 + 1.0));
                  float var20 = 0.06F;
                  this.moveRelative(0.06F * (var18 * var19 + (1.0F - var19)), new Vec3(0.0, 0.0, -1.0));
                  if (this.inWall) {
                     this.move(MoverType.SELF, this.getDeltaMovement().scale(0.8F));
                  } else {
                     this.move(MoverType.SELF, this.getDeltaMovement());
                  }

                  Vec3 var21 = this.getDeltaMovement().normalize();
                  double var22 = 0.8 + 0.15 * (var21.dot(var17) + 1.0) / 2.0;
                  this.setDeltaMovement(this.getDeltaMovement().multiply(var22, 0.91F, var22));
               }
            }

            this.yBodyRot = this.getYRot();
            Vec3[] var3 = new Vec3[this.subEntities.length];

            for (int var4 = 0; var4 < this.subEntities.length; var4++) {
               var3[var4] = new Vec3(this.subEntities[var4].getX(), this.subEntities[var4].getY(), this.subEntities[var4].getZ());
            }

            float var4 = (float)(this.getLatencyPos(5, 1.0F)[1] - this.getLatencyPos(10, 1.0F)[1]) * 10.0F * (float) (Math.PI / 180.0);
            float var5 = Mth.cos(var4);
            float var6 = Mth.sin(var4);
            float var7 = this.getYRot() * (float) (Math.PI / 180.0);
            float var8 = Mth.sin(var7);
            float var9 = Mth.cos(var7);
            this.tickPart(this.body, var8 * 0.5F, 0.0, -var9 * 0.5F);
            this.tickPart(this.wing1, var9 * 4.5F, 2.0, var8 * 4.5F);
            this.tickPart(this.wing2, var9 * -4.5F, 2.0, var8 * -4.5F);
            if (!this.level.isClientSide && this.hurtTime == 0) {
               this.knockBack(
                  this.level
                     .getEntities(this, this.wing1.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR)
               );
               this.knockBack(
                  this.level
                     .getEntities(this, this.wing2.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR)
               );
               this.hurt(this.level.getEntities(this, this.head.getBoundingBox().inflate(1.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
               this.hurt(this.level.getEntities(this, this.neck.getBoundingBox().inflate(1.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
            }

            float var10 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0) - this.yRotA * 0.01F);
            float var11 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0) - this.yRotA * 0.01F);
            float var12 = this.getHeadYOffset();
            this.tickPart(this.head, var10 * 6.5F * var5, var12 + var6 * 6.5F, -var11 * 6.5F * var5);
            this.tickPart(this.neck, var10 * 5.5F * var5, var12 + var6 * 5.5F, -var11 * 5.5F * var5);
            double[] var13 = this.getLatencyPos(5, 1.0F);

            for (int var14 = 0; var14 < 3; var14++) {
               EnderDragonPart var15 = null;
               if (var14 == 0) {
                  var15 = this.tail1;
               }

               if (var14 == 1) {
                  var15 = this.tail2;
               }

               if (var14 == 2) {
                  var15 = this.tail3;
               }

               double[] var16 = this.getLatencyPos(12 + var14 * 2, 1.0F);
               float var17 = this.getYRot() * (float) (Math.PI / 180.0) + this.rotWrap(var16[0] - var13[0]) * (float) (Math.PI / 180.0);
               float var18 = Mth.sin(var17);
               float var19 = Mth.cos(var17);
               float var20 = 1.5F;
               float var21 = (var14 + 1) * 2.0F;
               this.tickPart(
                  var15, -(var8 * 1.5F + var18 * var21) * var5, var16[1] - var13[1] - (var21 + 1.5F) * var6 + 1.5, (var9 * 1.5F + var19 * var21) * var5
               );
            }

            if (!this.level.isClientSide) {
               this.inWall = this.checkWalls(this.head.getBoundingBox())
                  | this.checkWalls(this.neck.getBoundingBox())
                  | this.checkWalls(this.body.getBoundingBox());
               if (this.dragonFight != null) {
                  this.dragonFight.updateDragon(this);
               }
            }

            for (int var14 = 0; var14 < this.subEntities.length; var14++) {
               this.subEntities[var14].xo = var3[var14].x;
               this.subEntities[var14].yo = var3[var14].y;
               this.subEntities[var14].zo = var3[var14].z;
               this.subEntities[var14].xOld = var3[var14].x;
               this.subEntities[var14].yOld = var3[var14].y;
               this.subEntities[var14].zOld = var3[var14].z;
            }
         }
      }
   }

   private void tickPart(EnderDragonPart param0, double param1, double param3, double param5) {
      param0.setPos(this.getX() + param1, this.getY() + param3, this.getZ() + param5);
   }

   private float getHeadYOffset() {
      if (this.phaseManager.getCurrentPhase().isSitting()) {
         return -1.0F;
      }

      double[] var1 = this.getLatencyPos(5, 1.0F);
      double[] var2 = this.getLatencyPos(0, 1.0F);
      return (float)(var1[1] - var2[1]);
   }

   private void checkCrystals() {
      if (this.nearestCrystal != null) {
         if (this.nearestCrystal.isRemoved()) {
            this.nearestCrystal = null;
         } else if (this.tickCount % 10 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getHealth() + 1.0F);
         }
      }

      if (this.random.nextInt(10) == 0) {
         List<EndCrystal> var1 = this.level.getEntitiesOfClass(EndCrystal.class, this.getBoundingBox().inflate(32.0));
         EndCrystal var2 = null;
         double var3 = Double.MAX_VALUE;

         for (EndCrystal var6 : var1) {
            double var7 = var6.distanceToSqr(this);
            if (var7 < var3) {
               var3 = var7;
               var2 = var6;
            }
         }

         this.nearestCrystal = var2;
      }
   }

   private void knockBack(List<Entity> param0) {
      double var2 = (this.body.getBoundingBox().minX + this.body.getBoundingBox().maxX) / 2.0;
      double var4 = (this.body.getBoundingBox().minZ + this.body.getBoundingBox().maxZ) / 2.0;

      for (Entity var7 : param0) {
         if (var7 instanceof LivingEntity) {
            double var8 = var7.getX() - var2;
            double var10 = var7.getZ() - var4;
            double var12 = Math.max(var8 * var8 + var10 * var10, 0.1);
            var7.push(var8 / var12 * 4.0, 0.2F, var10 / var12 * 4.0);
            if (!this.phaseManager.getCurrentPhase().isSitting() && ((LivingEntity)var7).getLastHurtByMobTimestamp() < var7.tickCount - 2) {
               var7.hurt(this.damageSources().mobAttack(this), 5.0F);
               this.doEnchantDamageEffects(this, var7);
            }
         }
      }
   }

   private void hurt(List<Entity> param0) {
      for (Entity var3 : param0) {
         if (var3 instanceof LivingEntity) {
            var3.hurt(this.damageSources().mobAttack(this), 10.0F);
            this.doEnchantDamageEffects(this, var3);
         }
      }
   }

   private float rotWrap(double param0) {
      return (float)Mth.wrapDegrees(param0);
   }

   private boolean checkWalls(AABB param0) {
      int var2 = Mth.floor(param0.minX);
      int var3 = Mth.floor(param0.minY);
      int var4 = Mth.floor(param0.minZ);
      int var5 = Mth.floor(param0.maxX);
      int var6 = Mth.floor(param0.maxY);
      int var7 = Mth.floor(param0.maxZ);
      boolean var8 = false;
      boolean var9 = false;

      for (int var10 = var2; var10 <= var5; var10++) {
         for (int var11 = var3; var11 <= var6; var11++) {
            for (int var12 = var4; var12 <= var7; var12++) {
               BlockPos var13 = new BlockPos(var10, var11, var12);
               BlockState var14 = this.level.getBlockState(var13);
               if (!var14.isAir() && !var14.is(BlockTags.DRAGON_TRANSPARENT)) {
                  if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && !var14.is(BlockTags.DRAGON_IMMUNE)) {
                     var9 = this.level.removeBlock(var13, false) || var9;
                  } else {
                     var8 = true;
                  }
               }
            }
         }
      }

      if (var9) {
         BlockPos var10 = new BlockPos(
            var2 + this.random.nextInt(var5 - var2 + 1), var3 + this.random.nextInt(var6 - var3 + 1), var4 + this.random.nextInt(var7 - var4 + 1)
         );
         this.level.levelEvent(2008, var10, 0);
      }

      return var8;
   }

   public boolean hurt(EnderDragonPart param0, DamageSource param1, float param2) {
      if (this.phaseManager.getCurrentPhase().getPhase() == EnderDragonPhase.DYING) {
         return false;
      }

      param2 = this.phaseManager.getCurrentPhase().onHurt(param1, param2);
      if (param0 != this.head) {
         param2 = param2 / 4.0F + Math.min(param2, 1.0F);
      }

      if (param2 < 0.01F) {
         return false;
      }

      if (param1.getEntity() instanceof Player || param1.is(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS)) {
         float var4 = this.getHealth();
         this.reallyHurt(param1, param2);
         if (this.isDeadOrDying() && !this.phaseManager.getCurrentPhase().isSitting()) {
            this.setHealth(1.0F);
            this.phaseManager.setPhase(EnderDragonPhase.DYING);
         }

         if (this.phaseManager.getCurrentPhase().isSitting()) {
            this.sittingDamageReceived = this.sittingDamageReceived + var4 - this.getHealth();
            if (this.sittingDamageReceived > 0.25F * this.getMaxHealth()) {
               this.sittingDamageReceived = 0.0F;
               this.phaseManager.setPhase(EnderDragonPhase.TAKEOFF);
            }
         }
      }

      return true;
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      return !this.level.isClientSide ? this.hurt(this.body, param0, param1) : false;
   }

   protected boolean reallyHurt(DamageSource param0, float param1) {
      return super.hurt(param0, param1);
   }

   @Override
   public void kill() {
      this.remove(Entity.RemovalReason.KILLED);
      this.gameEvent(GameEvent.ENTITY_DIE);
      if (this.dragonFight != null) {
         this.dragonFight.updateDragon(this);
         this.dragonFight.setDragonKilled(this);
      }
   }

   @Override
   protected void tickDeath() {
      if (this.dragonFight != null) {
         this.dragonFight.updateDragon(this);
      }

      this.dragonDeathTime++;
      if (this.dragonDeathTime >= 180 && this.dragonDeathTime <= 200) {
         float var1 = (this.random.nextFloat() - 0.5F) * 8.0F;
         float var2 = (this.random.nextFloat() - 0.5F) * 4.0F;
         float var3 = (this.random.nextFloat() - 0.5F) * 8.0F;
         this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX() + var1, this.getY() + 2.0 + var2, this.getZ() + var3, 0.0, 0.0, 0.0);
      }

      boolean var1 = this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
      int var2 = 500;
      if (this.dragonFight != null && !this.dragonFight.hasPreviouslyKilledDragon()) {
         var2 = 12000;
      }

      if (this.level instanceof ServerLevel) {
         if (this.dragonDeathTime > 150 && this.dragonDeathTime % 5 == 0 && var1) {
            ExperienceOrb.award((ServerLevel)this.level, this.position(), Mth.floor(var2 * 0.08F));
         }

         if (this.dragonDeathTime == 1 && !this.isSilent()) {
            this.level.globalLevelEvent(1028, this.blockPosition(), 0);
         }
      }

      this.move(MoverType.SELF, new Vec3(0.0, 0.1F, 0.0));
      if (this.dragonDeathTime == 200 && this.level instanceof ServerLevel) {
         if (var1) {
            ExperienceOrb.award((ServerLevel)this.level, this.position(), Mth.floor(var2 * 0.2F));
         }

         if (this.dragonFight != null) {
            this.dragonFight.setDragonKilled(this);
         }

         this.remove(Entity.RemovalReason.KILLED);
         this.gameEvent(GameEvent.ENTITY_DIE);
      }
   }

   public int findClosestNode() {
      if (this.nodes[0] == null) {
         for (int var1 = 0; var1 < 24; var1++) {
            int var2 = 5;
            int var3 = var1;
            int var4;
            int var5;
            if (var1 < 12) {
               var4 = Mth.floor(60.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * var3)));
               var5 = Mth.floor(60.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * var3)));
            } else if (var1 < 20) {
               var3 -= 12;
               var4 = Mth.floor(40.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * var3)));
               var5 = Mth.floor(40.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * var3)));
               var2 += 10;
            } else {
               var3 -= 20;
               var4 = Mth.floor(20.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * var3)));
               var5 = Mth.floor(20.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * var3)));
            }

            int var6 = Math.max(
               this.level.getSeaLevel() + 10, this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(var4, 0, var5)).getY() + var2
            );
            this.nodes[var1] = new Node(var4, var6, var5);
         }

         this.nodeAdjacency[0] = 6146;
         this.nodeAdjacency[1] = 8197;
         this.nodeAdjacency[2] = 8202;
         this.nodeAdjacency[3] = 16404;
         this.nodeAdjacency[4] = 32808;
         this.nodeAdjacency[5] = 32848;
         this.nodeAdjacency[6] = 65696;
         this.nodeAdjacency[7] = 131392;
         this.nodeAdjacency[8] = 131712;
         this.nodeAdjacency[9] = 263424;
         this.nodeAdjacency[10] = 526848;
         this.nodeAdjacency[11] = 525313;
         this.nodeAdjacency[12] = 1581057;
         this.nodeAdjacency[13] = 3166214;
         this.nodeAdjacency[14] = 2138120;
         this.nodeAdjacency[15] = 6373424;
         this.nodeAdjacency[16] = 4358208;
         this.nodeAdjacency[17] = 12910976;
         this.nodeAdjacency[18] = 9044480;
         this.nodeAdjacency[19] = 9706496;
         this.nodeAdjacency[20] = 15216640;
         this.nodeAdjacency[21] = 13688832;
         this.nodeAdjacency[22] = 11763712;
         this.nodeAdjacency[23] = 8257536;
      }

      return this.findClosestNode(this.getX(), this.getY(), this.getZ());
   }

   public int findClosestNode(double param0, double param2, double param4) {
      float var7 = 10000.0F;
      int var8 = 0;
      Node var9 = new Node(Mth.floor(param0), Mth.floor(param2), Mth.floor(param4));
      int var10 = 0;
      if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
         var10 = 12;
      }

      for (int var11 = var10; var11 < 24; var11++) {
         if (this.nodes[var11] != null) {
            float var12 = this.nodes[var11].distanceToSqr(var9);
            if (var12 < var7) {
               var7 = var12;
               var8 = var11;
            }
         }
      }

      return var8;
   }

   @Nullable
   public Path findPath(int param0, int param1, @Nullable Node param2) {
      for (int var4 = 0; var4 < 24; var4++) {
         Node var5 = this.nodes[var4];
         var5.closed = false;
         var5.f = 0.0F;
         var5.g = 0.0F;
         var5.h = 0.0F;
         var5.cameFrom = null;
         var5.heapIdx = -1;
      }

      Node var4 = this.nodes[param0];
      Node var5 = this.nodes[param1];
      var4.g = 0.0F;
      var4.h = var4.distanceTo(var5);
      var4.f = var4.h;
      this.openSet.clear();
      this.openSet.insert(var4);
      Node var6 = var4;
      int var7 = 0;
      if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
         var7 = 12;
      }

      while (!this.openSet.isEmpty()) {
         Node var8 = this.openSet.pop();
         if (var8.equals(var5)) {
            if (param2 != null) {
               param2.cameFrom = var5;
               var5 = param2;
            }

            return this.reconstructPath(var4, var5);
         }

         if (var8.distanceTo(var5) < var6.distanceTo(var5)) {
            var6 = var8;
         }

         var8.closed = true;
         int var9 = 0;

         for (int var10 = 0; var10 < 24; var10++) {
            if (this.nodes[var10] == var8) {
               var9 = var10;
               break;
            }
         }

         for (int var10 = var7; var10 < 24; var10++) {
            if ((this.nodeAdjacency[var9] & 1 << var10) > 0) {
               Node var11 = this.nodes[var10];
               if (!var11.closed) {
                  float var12 = var8.g + var8.distanceTo(var11);
                  if (!var11.inOpenSet() || var12 < var11.g) {
                     var11.cameFrom = var8;
                     var11.g = var12;
                     var11.h = var11.distanceTo(var5);
                     if (var11.inOpenSet()) {
                        this.openSet.changeCost(var11, var11.g + var11.h);
                     } else {
                        var11.f = var11.g + var11.h;
                        this.openSet.insert(var11);
                     }
                  }
               }
            }
         }
      }

      if (var6 == var4) {
         return null;
      }

      LOGGER.debug("Failed to find path from {} to {}", param0, param1);
      if (param2 != null) {
         param2.cameFrom = var6;
         var6 = param2;
      }

      return this.reconstructPath(var4, var6);
   }

   private Path reconstructPath(Node param0, Node param1) {
      List<Node> var3 = Lists.newArrayList();
      Node var4 = param1;
      var3.add(0, var4);

      while (var4.cameFrom != null) {
         var4 = var4.cameFrom;
         var3.add(0, var4);
      }

      return new Path(var3, new BlockPos(param1.x, param1.y, param1.z), true);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      param0.putInt("DragonPhase", this.phaseManager.getCurrentPhase().getPhase().getId());
      param0.putInt("DragonDeathTime", this.dragonDeathTime);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      if (param0.contains("DragonPhase")) {
         this.phaseManager.setPhase(EnderDragonPhase.getById(param0.getInt("DragonPhase")));
      }

      if (param0.contains("DragonDeathTime")) {
         this.dragonDeathTime = param0.getInt("DragonDeathTime");
      }
   }

   @Override
   public void checkDespawn() {
   }

   public EnderDragonPart[] getSubEntities() {
      return this.subEntities;
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.ENDER_DRAGON_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource param0) {
      return SoundEvents.ENDER_DRAGON_HURT;
   }

   @Override
   protected float getSoundVolume() {
      return 5.0F;
   }

   public float getHeadPartYOffset(int param0, double[] param1, double[] param2) {
      DragonPhaseInstance var4 = this.phaseManager.getCurrentPhase();
      EnderDragonPhase<? extends DragonPhaseInstance> var5 = var4.getPhase();
      double var6;
      if (var5 == EnderDragonPhase.LANDING || var5 == EnderDragonPhase.TAKEOFF) {
         BlockPos var8 = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.END_PODIUM_LOCATION);
         double var9 = Math.max(Math.sqrt(var8.distToCenterSqr(this.position())) / 4.0, 1.0);
         var6 = param0 / var9;
      } else if (var4.isSitting()) {
         var6 = param0;
      } else if (param0 == 6) {
         var6 = 0.0;
      } else {
         var6 = param2[1] - param1[1];
      }

      return (float)var6;
   }

   public Vec3 getHeadLookVector(float param0) {
      DragonPhaseInstance var2 = this.phaseManager.getCurrentPhase();
      EnderDragonPhase<? extends DragonPhaseInstance> var3 = var2.getPhase();
      Vec3 var4;
      if (var3 == EnderDragonPhase.LANDING || var3 == EnderDragonPhase.TAKEOFF) {
         BlockPos var5 = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.END_PODIUM_LOCATION);
         float var6 = Math.max((float)Math.sqrt(var5.distToCenterSqr(this.position())) / 4.0F, 1.0F);
         float var7 = 6.0F / var6;
         float var8 = this.getXRot();
         float var9 = 1.5F;
         this.setXRot(-var7 * 1.5F * 5.0F);
         var4 = this.getViewVector(param0);
         this.setXRot(var8);
      } else if (var2.isSitting()) {
         float var5 = this.getXRot();
         float var6 = 1.5F;
         this.setXRot(-45.0F);
         var4 = this.getViewVector(param0);
         this.setXRot(var5);
      } else {
         var4 = this.getViewVector(param0);
      }

      return var4;
   }

   public void onCrystalDestroyed(EndCrystal param0, BlockPos param1, DamageSource param2) {
      Player var4;
      if (param2.getEntity() instanceof Player) {
         var4 = (Player)param2.getEntity();
      } else {
         var4 = this.level.getNearestPlayer(CRYSTAL_DESTROY_TARGETING, param1.getX(), param1.getY(), param1.getZ());
      }

      if (param0 == this.nearestCrystal) {
         this.hurt(this.head, this.damageSources().explosion(param0, var4), 10.0F);
      }

      this.phaseManager.getCurrentPhase().onCrystalDestroyed(param0, param1, param2, var4);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> param0) {
      if (DATA_PHASE.equals(param0) && this.level.isClientSide) {
         this.phaseManager.setPhase(EnderDragonPhase.getById(this.getEntityData().get(DATA_PHASE)));
      }

      super.onSyncedDataUpdated(param0);
   }

   public EnderDragonPhaseManager getPhaseManager() {
      return this.phaseManager;
   }

   @Nullable
   public EndDragonFight getDragonFight() {
      return this.dragonFight;
   }

   @Override
   public boolean addEffect(MobEffectInstance param0, @Nullable Entity param1) {
      return false;
   }

   @Override
   protected boolean canRide(Entity param0) {
      return false;
   }

   @Override
   public boolean canChangeDimensions() {
      return false;
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket param0) {
      super.recreateFromPacket(param0);
      EnderDragonPart[] var2 = this.getSubEntities();

      for (int var3 = 0; var3 < var2.length; var3++) {
         var2[var3].setId(var3 + param0.getId());
      }
   }

   @Override
   public boolean canAttack(LivingEntity param0) {
      return param0.canBeSeenAsEnemy();
   }

   @Override
   public double getPassengersRidingOffset() {
      return this.body.getBbHeight();
   }
}
