package net.minecraft.world.level;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Explosion {
   private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
   private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
   private final boolean fire;
   private final Explosion.BlockInteraction blockInteraction;
   private final RandomSource random = RandomSource.create();
   private final Level level;
   private final double x;
   private final double y;
   private final double z;
   @Nullable
   private final Entity source;
   private final float radius;
   private final DamageSource damageSource;
   private final ExplosionDamageCalculator damageCalculator;
   private final ObjectArrayList<BlockPos> toBlow = new ObjectArrayList<>();
   private final Map<Player, Vec3> hitPlayers = Maps.newHashMap();

   public Explosion(Level param0, @Nullable Entity param1, double param2, double param4, double param6, float param8, List<BlockPos> param9) {
      this(param0, param1, param2, param4, param6, param8, false, Explosion.BlockInteraction.DESTROY_WITH_DECAY, param9);
   }

   public Explosion(
      Level param0,
      @Nullable Entity param1,
      double param2,
      double param4,
      double param6,
      float param8,
      boolean param9,
      Explosion.BlockInteraction param10,
      List<BlockPos> param11
   ) {
      this(param0, param1, param2, param4, param6, param8, param9, param10);
      this.toBlow.addAll(param11);
   }

   public Explosion(
      Level param0, @Nullable Entity param1, double param2, double param4, double param6, float param8, boolean param9, Explosion.BlockInteraction param10
   ) {
      this(param0, param1, null, null, param2, param4, param6, param8, param9, param10);
   }

   public Explosion(
      Level param0,
      @Nullable Entity param1,
      @Nullable DamageSource param2,
      @Nullable ExplosionDamageCalculator param3,
      double param4,
      double param6,
      double param8,
      float param10,
      boolean param11,
      Explosion.BlockInteraction param12
   ) {
      this.level = param0;
      this.source = param1;
      this.radius = param10;
      this.x = param4;
      this.y = param6;
      this.z = param8;
      this.fire = param11;
      this.blockInteraction = param12;
      this.damageSource = param2 == null ? param0.damageSources().explosion(this) : param2;
      this.damageCalculator = param3 == null ? this.makeDamageCalculator(param1) : param3;
   }

   private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity param0) {
      return param0 == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(param0);
   }

   public static float getSeenPercent(Vec3 param0, Entity param1) {
      AABB var2 = param1.getBoundingBox();
      double var3 = 1.0 / ((var2.maxX - var2.minX) * 2.0 + 1.0);
      double var5 = 1.0 / ((var2.maxY - var2.minY) * 2.0 + 1.0);
      double var7 = 1.0 / ((var2.maxZ - var2.minZ) * 2.0 + 1.0);
      double var9 = (1.0 - Math.floor(1.0 / var3) * var3) / 2.0;
      double var11 = (1.0 - Math.floor(1.0 / var7) * var7) / 2.0;
      if (!(var3 < 0.0) && !(var5 < 0.0) && !(var7 < 0.0)) {
         int var13 = 0;
         int var14 = 0;

         for (double var15 = 0.0; var15 <= 1.0; var15 += var3) {
            for (double var17 = 0.0; var17 <= 1.0; var17 += var5) {
               for (double var19 = 0.0; var19 <= 1.0; var19 += var7) {
                  double var21 = Mth.lerp(var15, var2.minX, var2.maxX);
                  double var23 = Mth.lerp(var17, var2.minY, var2.maxY);
                  double var25 = Mth.lerp(var19, var2.minZ, var2.maxZ);
                  Vec3 var27 = new Vec3(var21 + var9, var23, var25 + var11);
                  if (param1.level.clip(new ClipContext(var27, param0, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, param1)).getType()
                     == HitResult.Type.MISS) {
                     var13++;
                  }

                  var14++;
               }
            }
         }

         return (float)var13 / var14;
      } else {
         return 0.0F;
      }
   }

   public void explode() {
      this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
      Set<BlockPos> var1 = Sets.newHashSet();
      int var2 = 16;

      for (int var3 = 0; var3 < 16; var3++) {
         for (int var4 = 0; var4 < 16; var4++) {
            for (int var5 = 0; var5 < 16; var5++) {
               if (var3 == 0 || var3 == 15 || var4 == 0 || var4 == 15 || var5 == 0 || var5 == 15) {
                  double var6 = var3 / 15.0F * 2.0F - 1.0F;
                  double var8 = var4 / 15.0F * 2.0F - 1.0F;
                  double var10 = var5 / 15.0F * 2.0F - 1.0F;
                  double var12 = Math.sqrt(var6 * var6 + var8 * var8 + var10 * var10);
                  var6 /= var12;
                  var8 /= var12;
                  var10 /= var12;
                  float var14 = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                  double var15 = this.x;
                  double var17 = this.y;
                  double var19 = this.z;
                  float var21 = 0.3F;

                  while (var14 > 0.0F) {
                     BlockPos var22 = BlockPos.containing(var15, var17, var19);
                     BlockState var23 = this.level.getBlockState(var22);
                     FluidState var24 = this.level.getFluidState(var22);
                     if (!this.level.isInWorldBounds(var22)) {
                        break;
                     }

                     Optional<Float> var25 = this.damageCalculator.getBlockExplosionResistance(this, this.level, var22, var23, var24);
                     if (var25.isPresent()) {
                        var14 -= (var25.get() + 0.3F) * 0.3F;
                     }

                     if (var14 > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, var22, var23, var14)) {
                        var1.add(var22);
                     }

                     var15 += var6 * 0.3F;
                     var17 += var8 * 0.3F;
                     var19 += var10 * 0.3F;
                     var14 -= 0.22500001F;
                  }
               }
            }
         }
      }

      this.toBlow.addAll(var1);
      float var3 = this.radius * 2.0F;
      int var4 = Mth.floor(this.x - var3 - 1.0);
      int var5 = Mth.floor(this.x + var3 + 1.0);
      int var6 = Mth.floor(this.y - var3 - 1.0);
      int var7 = Mth.floor(this.y + var3 + 1.0);
      int var8 = Mth.floor(this.z - var3 - 1.0);
      int var9 = Mth.floor(this.z + var3 + 1.0);
      List<Entity> var10 = this.level.getEntities(this.source, new AABB(var4, var6, var8, var5, var7, var9));
      Vec3 var11 = new Vec3(this.x, this.y, this.z);

      for (int var12 = 0; var12 < var10.size(); var12++) {
         Entity var13 = var10.get(var12);
         if (!var13.ignoreExplosion()) {
            double var14 = Math.sqrt(var13.distanceToSqr(var11)) / var3;
            if (var14 <= 1.0) {
               double var16 = var13.getX() - this.x;
               double var18 = (var13 instanceof PrimedTnt ? var13.getY() : var13.getEyeY()) - this.y;
               double var20 = var13.getZ() - this.z;
               double var22 = Math.sqrt(var16 * var16 + var18 * var18 + var20 * var20);
               if (var22 != 0.0) {
                  var16 /= var22;
                  var18 /= var22;
                  var20 /= var22;
                  double var24 = getSeenPercent(var11, var13);
                  double var26 = (1.0 - var14) * var24;
                  var13.hurt(this.getDamageSource(), (int)((var26 * var26 + var26) / 2.0 * 7.0 * var3 + 1.0));
                  double var28;
                  if (var13 instanceof LivingEntity var30) {
                     var28 = ProtectionEnchantment.getExplosionKnockbackAfterDampener(var30, var26);
                  } else {
                     var28 = var26;
                  }

                  var16 *= var28;
                  var18 *= var28;
                  var20 *= var28;
                  Vec3 var30 = new Vec3(var16, var18, var20);
                  var13.setDeltaMovement(var13.getDeltaMovement().add(var30));
                  if (var13 instanceof Player var31 && !var31.isSpectator() && (!var31.isCreative() || !var31.getAbilities().flying)) {
                     this.hitPlayers.put(var31, var30);
                  }
               }
            }
         }
      }
   }

   public void finalizeExplosion(boolean param0) {
      if (this.level.isClientSide) {
         this.level
            .playLocalSound(
               this.x,
               this.y,
               this.z,
               SoundEvents.GENERIC_EXPLODE,
               SoundSource.BLOCKS,
               4.0F,
               (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
               false
            );
      }

      boolean var2 = this.interactsWithBlocks();
      if (param0) {
         if (!(this.radius < 2.0F) && var2) {
            this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0, 0.0, 0.0);
         } else {
            this.level.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0, 0.0, 0.0);
         }
      }

      if (var2) {
         ObjectArrayList<Pair<ItemStack, BlockPos>> var3 = new ObjectArrayList<>();
         boolean var4 = this.getIndirectSourceEntity() instanceof Player;
         Util.shuffle(this.toBlow, this.level.random);

         for (BlockPos var6 : this.toBlow) {
            BlockState var7 = this.level.getBlockState(var6);
            Block var8 = var7.getBlock();
            if (!var7.isAir()) {
               BlockPos var9 = var6.immutable();
               this.level.getProfiler().push("explosion_blocks");
               if (var8.dropFromExplosion(this) && this.level instanceof ServerLevel var10) {
                  BlockEntity var11 = var7.hasBlockEntity() ? this.level.getBlockEntity(var6) : null;
                  LootContext.Builder var12 = new LootContext.Builder(var10)
                     .withRandom(this.level.random)
                     .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(var6))
                     .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                     .withOptionalParameter(LootContextParams.BLOCK_ENTITY, var11)
                     .withOptionalParameter(LootContextParams.THIS_ENTITY, this.source);
                  if (this.blockInteraction == Explosion.BlockInteraction.DESTROY_WITH_DECAY) {
                     var12.withParameter(LootContextParams.EXPLOSION_RADIUS, this.radius);
                  }

                  var7.spawnAfterBreak(var10, var6, ItemStack.EMPTY, var4);
                  var7.getDrops(var12).forEach(l_param2 -> addBlockDrops(var3, l_param2, var9));
               }

               this.level.setBlock(var6, Blocks.AIR.defaultBlockState(), 3);
               var8.wasExploded(this.level, var6, this);
               this.level.getProfiler().pop();
            }
         }

         for (Pair<ItemStack, BlockPos> var6 : var3) {
            Block.popResource(this.level, var6.getSecond(), var6.getFirst());
         }
      }

      if (this.fire) {
         for (BlockPos var4 : this.toBlow) {
            if (this.random.nextInt(3) == 0
               && this.level.getBlockState(var4).isAir()
               && this.level.getBlockState(var4.below()).isSolidRender(this.level, var4.below())) {
               this.level.setBlockAndUpdate(var4, BaseFireBlock.getState(this.level, var4));
            }
         }
      }
   }

   public boolean interactsWithBlocks() {
      return this.blockInteraction != Explosion.BlockInteraction.KEEP;
   }

   private static void addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> param0, ItemStack param1, BlockPos param2) {
      int var3 = param0.size();

      for (int var4 = 0; var4 < var3; var4++) {
         Pair<ItemStack, BlockPos> var5 = param0.get(var4);
         ItemStack var6 = var5.getFirst();
         if (ItemEntity.areMergable(var6, param1)) {
            ItemStack var7 = ItemEntity.merge(var6, param1, 16);
            param0.set(var4, Pair.of(var7, var5.getSecond()));
            if (param1.isEmpty()) {
               return;
            }
         }
      }

      param0.add(Pair.of(param1, param2));
   }

   public DamageSource getDamageSource() {
      return this.damageSource;
   }

   public Map<Player, Vec3> getHitPlayers() {
      return this.hitPlayers;
   }

   @Nullable
   public LivingEntity getIndirectSourceEntity() {
      if (this.source == null) {
         return null;
      } else if (this.source instanceof PrimedTnt var1) {
         return var1.getOwner();
      } else if (this.source instanceof LivingEntity var1) {
         return var1;
      } else {
         return this.source instanceof Projectile var1 && var1.getOwner() instanceof LivingEntity var3 ? var3 : null;
      }
   }

   @Nullable
   public Entity getDirectSourceEntity() {
      return this.source;
   }

   public void clearToBlow() {
      this.toBlow.clear();
   }

   public List<BlockPos> getToBlow() {
      return this.toBlow;
   }

   public enum BlockInteraction {
      KEEP,
      DESTROY,
      DESTROY_WITH_DECAY;
   }
}
