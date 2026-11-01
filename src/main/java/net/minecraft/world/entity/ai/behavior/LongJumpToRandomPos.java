package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class LongJumpToRandomPos<E extends Mob> extends Behavior<E> {
   protected static final int FIND_JUMP_TRIES = 20;
   private static final int PREPARE_JUMP_DURATION = 40;
   protected static final int MIN_PATHFIND_DISTANCE_TO_VALID_JUMP = 8;
   private static final int TIME_OUT_DURATION = 200;
   private static final List<Integer> ALLOWED_ANGLES = Lists.newArrayList(65, 70, 75, 80);
   private final UniformInt timeBetweenLongJumps;
   protected final int maxLongJumpHeight;
   protected final int maxLongJumpWidth;
   protected final float maxJumpVelocity;
   protected List<LongJumpToRandomPos.PossibleJump> jumpCandidates = Lists.newArrayList();
   protected Optional<Vec3> initialPosition = Optional.empty();
   @Nullable
   protected Vec3 chosenJump;
   protected int findJumpTries;
   protected long prepareJumpStart;
   private final Function<E, SoundEvent> getJumpSound;
   private final BiPredicate<E, BlockPos> acceptableLandingSpot;

   public LongJumpToRandomPos(UniformInt param0, int param1, int param2, float param3, Function<E, SoundEvent> param4) {
      this(param0, param1, param2, param3, param4, LongJumpToRandomPos::defaultAcceptableLandingSpot);
   }

   public static <E extends Mob> boolean defaultAcceptableLandingSpot(E param0, BlockPos param1) {
      Level var2 = param0.level;
      BlockPos var3 = param1.below();
      return var2.getBlockState(var3).isSolidRender(var2, var3)
         && param0.getPathfindingMalus(WalkNodeEvaluator.getBlockPathTypeStatic(var2, param1.mutable())) == 0.0F;
   }

   public LongJumpToRandomPos(UniformInt param0, int param1, int param2, float param3, Function<E, SoundEvent> param4, BiPredicate<E, BlockPos> param5) {
      super(
         ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.LONG_JUMP_MID_JUMP,
            MemoryStatus.VALUE_ABSENT
         ),
         200
      );
      this.timeBetweenLongJumps = param0;
      this.maxLongJumpHeight = param1;
      this.maxLongJumpWidth = param2;
      this.maxJumpVelocity = param3;
      this.getJumpSound = param4;
      this.acceptableLandingSpot = param5;
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Mob param1) {
      boolean var3 = param1.isOnGround() && !param1.isInWater() && !param1.isInLava() && !param0.getBlockState(param1.blockPosition()).is(Blocks.HONEY_BLOCK);
      if (!var3) {
         param1.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(param0.random) / 2);
      }

      return var3;
   }

   protected boolean canStillUse(ServerLevel param0, Mob param1, long param2) {
      boolean var5 = this.initialPosition.isPresent()
         && this.initialPosition.get().equals(param1.position())
         && this.findJumpTries > 0
         && !param1.isInWaterOrBubble()
         && (this.chosenJump != null || !this.jumpCandidates.isEmpty());
      if (!var5 && param1.getBrain().getMemory(MemoryModuleType.LONG_JUMP_MID_JUMP).isEmpty()) {
         param1.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(param0.random) / 2);
         param1.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      }

      return var5;
   }

   protected void start(ServerLevel param0, E param1, long param2) {
      this.chosenJump = null;
      this.findJumpTries = 20;
      this.initialPosition = Optional.of(param1.position());
      BlockPos var5 = param1.blockPosition();
      int var6 = var5.getX();
      int var7 = var5.getY();
      int var8 = var5.getZ();
      this.jumpCandidates = BlockPos.betweenClosedStream(
            var6 - this.maxLongJumpWidth,
            var7 - this.maxLongJumpHeight,
            var8 - this.maxLongJumpWidth,
            var6 + this.maxLongJumpWidth,
            var7 + this.maxLongJumpHeight,
            var8 + this.maxLongJumpWidth
         )
         .filter(l_param1 -> !l_param1.equals(var5))
         .map(l_param1 -> new LongJumpToRandomPos.PossibleJump(l_param1.immutable(), Mth.ceil(var5.distSqr(l_param1))))
         .collect(Collectors.toCollection(Lists::newArrayList));
   }

   protected void tick(ServerLevel param0, E param1, long param2) {
      if (this.chosenJump != null) {
         if (param2 - this.prepareJumpStart >= 40L) {
            param1.setYRot(param1.yBodyRot);
            param1.setDiscardFriction(true);
            double var5 = this.chosenJump.length();
            double var7 = var5 + param1.getJumpBoostPower();
            param1.setDeltaMovement(this.chosenJump.scale(var7 / var5));
            param1.getBrain().setMemory(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
            param0.playSound(null, param1, this.getJumpSound.apply(param1), SoundSource.NEUTRAL, 1.0F, 1.0F);
         }
      } else {
         this.findJumpTries--;
         this.pickCandidate(param0, param1, param2);
      }
   }

   protected void pickCandidate(ServerLevel param0, E param1, long param2) {
      while (!this.jumpCandidates.isEmpty()) {
         Optional<LongJumpToRandomPos.PossibleJump> var5 = this.getJumpCandidate(param0);
         if (!var5.isEmpty()) {
            LongJumpToRandomPos.PossibleJump var6 = var5.get();
            BlockPos var7 = var6.getJumpTarget();
            if (this.isAcceptableLandingPosition(param0, param1, var7)) {
               Vec3 var8 = Vec3.atCenterOf(var7);
               Vec3 var9 = this.calculateOptimalJumpVector(param1, var8);
               if (var9 != null) {
                  param1.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(var7));
                  PathNavigation var10 = param1.getNavigation();
                  Path var11 = var10.createPath(var7, 0, 8);
                  if (var11 == null || !var11.canReach()) {
                     this.chosenJump = var9;
                     this.prepareJumpStart = param2;
                     return;
                  }
               }
            }
         }
      }
   }

   protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(ServerLevel param0) {
      Optional<LongJumpToRandomPos.PossibleJump> var2 = WeightedRandom.getRandomItem(param0.random, this.jumpCandidates);
      var2.ifPresent(this.jumpCandidates::remove);
      return var2;
   }

   private boolean isAcceptableLandingPosition(ServerLevel param0, E param1, BlockPos param2) {
      BlockPos var4 = param1.blockPosition();
      int var5 = var4.getX();
      int var6 = var4.getZ();
      return var5 == param2.getX() && var6 == param2.getZ() ? false : this.acceptableLandingSpot.test(param1, param2);
   }

   @Nullable
   protected Vec3 calculateOptimalJumpVector(Mob param0, Vec3 param1) {
      List<Integer> var3 = Lists.newArrayList(ALLOWED_ANGLES);
      Util.shuffleList(var3, RandomSource.create());

      for (int var5 : var3) {
         Vec3 var6 = this.calculateJumpVectorForAngle(param0, param1, var5);
         if (var6 != null) {
            return var6;
         }
      }

      return null;
   }

   @Nullable
   private Vec3 calculateJumpVectorForAngle(Mob param0, Vec3 param1, int param2) {
      Vec3 var4 = param0.position();
      Vec3 var5 = new Vec3(param1.x - var4.x, 0.0, param1.z - var4.z).normalize().scale(0.5);
      param1 = param1.subtract(var5);
      Vec3 var6 = param1.subtract(var4);
      float var7 = param2 * (float) Math.PI / 180.0F;
      double var8 = Math.atan2(var6.z, var6.x);
      double var10 = var6.subtract(0.0, var6.y, 0.0).lengthSqr();
      double var12 = Math.sqrt(var10);
      double var14 = var6.y;
      double var16 = Math.sin(2.0F * var7);
      double var18 = 0.08;
      double var20 = Math.pow(Math.cos(var7), 2.0);
      double var22 = Math.sin(var7);
      double var24 = Math.cos(var7);
      double var26 = Math.sin(var8);
      double var28 = Math.cos(var8);
      double var30 = var10 * 0.08 / (var12 * var16 - 2.0 * var14 * var20);
      if (var30 < 0.0) {
         return null;
      }

      double var32 = Math.sqrt(var30);
      if (var32 > this.maxJumpVelocity) {
         return null;
      }

      double var34 = var32 * var24;
      double var36 = var32 * var22;
      int var38 = Mth.ceil(var12 / var34) * 2;
      double var39 = 0.0;
      Vec3 var41 = null;
      EntityDimensions var42 = param0.getDimensions(Pose.LONG_JUMPING);

      for (int var43 = 0; var43 < var38 - 1; var43++) {
         var39 += var12 / var38;
         double var44 = var22 / var24 * var39 - Math.pow(var39, 2.0) * 0.08 / (2.0 * var30 * Math.pow(var24, 2.0));
         double var46 = var39 * var28;
         double var48 = var39 * var26;
         Vec3 var50 = new Vec3(var4.x + var46, var4.y + var44, var4.z + var48);
         if (var41 != null && !this.isClearTransition(param0, var42, var41, var50)) {
            return null;
         }

         var41 = var50;
      }

      return new Vec3(var34 * var28, var36, var34 * var26).scale(0.95F);
   }

   private boolean isClearTransition(Mob param0, EntityDimensions param1, Vec3 param2, Vec3 param3) {
      Vec3 var5 = param3.subtract(param2);
      double var6 = Math.min(param1.width, param1.height);
      int var8 = Mth.ceil(var5.length() / var6);
      Vec3 var9 = var5.normalize();
      Vec3 var10 = param2;

      for (int var11 = 0; var11 < var8; var11++) {
         var10 = var11 == var8 - 1 ? param3 : var10.add(var9.scale(var6 * 0.9F));
         if (!param0.level.noCollision(param0, param1.makeBoundingBox(var10))) {
            return false;
         }
      }

      return true;
   }

   public static class PossibleJump extends WeightedEntry.IntrusiveBase {
      private final BlockPos jumpTarget;

      public PossibleJump(BlockPos param0, int param1) {
         super(param1);
         this.jumpTarget = param0;
      }

      public BlockPos getJumpTarget() {
         return this.jumpTarget;
      }
   }
}
