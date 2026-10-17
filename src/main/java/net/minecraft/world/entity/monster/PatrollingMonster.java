package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public abstract class PatrollingMonster extends Monster {
   @Nullable
   private BlockPos patrolTarget;
   private boolean patrolLeader;
   private boolean patrolling;

   protected PatrollingMonster(EntityType<? extends PatrollingMonster> param0, Level param1) {
      super(param0, param1);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(4, new PatrollingMonster.LongDistancePatrolGoal<>(this, 0.7, 0.595));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      if (this.patrolTarget != null) {
         param0.put("PatrolTarget", NbtUtils.writeBlockPos(this.patrolTarget));
      }

      param0.putBoolean("PatrolLeader", this.patrolLeader);
      param0.putBoolean("Patrolling", this.patrolling);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      if (param0.contains("PatrolTarget")) {
         this.patrolTarget = NbtUtils.readBlockPos(param0.getCompound("PatrolTarget"));
      }

      this.patrolLeader = param0.getBoolean("PatrolLeader");
      this.patrolling = param0.getBoolean("Patrolling");
   }

   @Override
   public double getMyRidingOffset() {
      return -0.45;
   }

   public boolean canBeLeader() {
      return true;
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor param0, DifficultyInstance param1, MobSpawnType param2, @Nullable SpawnGroupData param3, @Nullable CompoundTag param4
   ) {
      if (param2 != MobSpawnType.PATROL
         && param2 != MobSpawnType.EVENT
         && param2 != MobSpawnType.STRUCTURE
         && param0.getRandom().nextFloat() < 0.06F
         && this.canBeLeader()) {
         this.patrolLeader = true;
      }

      if (this.isPatrolLeader()) {
         this.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
         this.setDropChance(EquipmentSlot.HEAD, 2.0F);
      }

      if (param2 == MobSpawnType.PATROL) {
         this.patrolling = true;
      }

      return super.finalizeSpawn(param0, param1, param2, param3, param4);
   }

   public static boolean checkPatrollingMonsterSpawnRules(
      EntityType<? extends PatrollingMonster> param0, LevelAccessor param1, MobSpawnType param2, BlockPos param3, RandomSource param4
   ) {
      return param1.getBrightness(LightLayer.BLOCK, param3) > 8 ? false : checkAnyLightMonsterSpawnRules(param0, param1, param2, param3, param4);
   }

   @Override
   public boolean removeWhenFarAway(double param0) {
      return !this.patrolling || param0 > 16384.0;
   }

   public void setPatrolTarget(BlockPos param0) {
      this.patrolTarget = param0;
      this.patrolling = true;
   }

   public BlockPos getPatrolTarget() {
      return this.patrolTarget;
   }

   public boolean hasPatrolTarget() {
      return this.patrolTarget != null;
   }

   public void setPatrolLeader(boolean param0) {
      this.patrolLeader = param0;
      this.patrolling = true;
   }

   public boolean isPatrolLeader() {
      return this.patrolLeader;
   }

   public boolean canJoinPatrol() {
      return true;
   }

   public void findPatrolTarget() {
      this.patrolTarget = this.blockPosition().offset(-500 + this.random.nextInt(1000), 0, -500 + this.random.nextInt(1000));
      this.patrolling = true;
   }

   protected boolean isPatrolling() {
      return this.patrolling;
   }

   protected void setPatrolling(boolean param0) {
      this.patrolling = param0;
   }

   public static class LongDistancePatrolGoal<T extends PatrollingMonster> extends Goal {
      private static final int NAVIGATION_FAILED_COOLDOWN = 200;
      private final T mob;
      private final double speedModifier;
      private final double leaderSpeedModifier;
      private long cooldownUntil;

      public LongDistancePatrolGoal(T param0, double param1, double param3) {
         this.mob = param0;
         this.speedModifier = param1;
         this.leaderSpeedModifier = param3;
         this.cooldownUntil = -1L;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         boolean var1 = this.mob.level.getGameTime() < this.cooldownUntil;
         return this.mob.isPatrolling() && this.mob.getTarget() == null && !this.mob.isVehicle() && this.mob.hasPatrolTarget() && !var1;
      }

      @Override
      public void start() {
      }

      @Override
      public void stop() {
      }

      @Override
      public void tick() {
         boolean var1 = this.mob.isPatrolLeader();
         PathNavigation var2 = this.mob.getNavigation();
         if (var2.isDone()) {
            List<PatrollingMonster> var3 = this.findPatrolCompanions();
            if (this.mob.isPatrolling() && var3.isEmpty()) {
               this.mob.setPatrolling(false);
            } else if (var1 && this.mob.getPatrolTarget().closerToCenterThan(this.mob.position(), 10.0)) {
               this.mob.findPatrolTarget();
            } else {
               Vec3 var4 = Vec3.atBottomCenterOf(this.mob.getPatrolTarget());
               Vec3 var5 = this.mob.position();
               Vec3 var6 = var5.subtract(var4);
               var4 = var6.yRot(90.0F).scale(0.4).add(var4);
               Vec3 var7 = var4.subtract(var5).normalize().scale(10.0).add(var5);
               BlockPos var8 = BlockPos.containing(var7);
               var8 = this.mob.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, var8);
               if (!var2.moveTo(var8.getX(), var8.getY(), var8.getZ(), var1 ? this.leaderSpeedModifier : this.speedModifier)) {
                  this.moveRandomly();
                  this.cooldownUntil = this.mob.level.getGameTime() + 200L;
               } else if (var1) {
                  for (PatrollingMonster var10 : var3) {
                     var10.setPatrolTarget(var8);
                  }
               }
            }
         }
      }

      private List<PatrollingMonster> findPatrolCompanions() {
         return this.mob
            .level
            .getEntitiesOfClass(
               PatrollingMonster.class, this.mob.getBoundingBox().inflate(16.0), l_param0 -> l_param0.canJoinPatrol() && !l_param0.is(this.mob)
            );
      }

      private boolean moveRandomly() {
         RandomSource var1 = this.mob.getRandom();
         BlockPos var2 = this.mob
            .level
            .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.mob.blockPosition().offset(-8 + var1.nextInt(16), 0, -8 + var1.nextInt(16)));
         return this.mob.getNavigation().moveTo(var2.getX(), var2.getY(), var2.getZ(), this.speedModifier);
      }
   }
}
