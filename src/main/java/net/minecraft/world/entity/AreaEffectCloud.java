package net.minecraft.world.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;

public class AreaEffectCloud extends Entity implements TraceableEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int TIME_BETWEEN_APPLICATIONS = 5;
   private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<ParticleOptions> DATA_PARTICLE = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.PARTICLE);
   private static final float MAX_RADIUS = 32.0F;
   private static final float MINIMAL_RADIUS = 0.5F;
   private static final float DEFAULT_RADIUS = 3.0F;
   public static final float DEFAULT_WIDTH = 6.0F;
   public static final float HEIGHT = 0.5F;
   private Potion potion = Potions.EMPTY;
   private final List<MobEffectInstance> effects = Lists.newArrayList();
   private final Map<Entity, Integer> victims = Maps.newHashMap();
   private int duration = 600;
   private int waitTime = 20;
   private int reapplicationDelay = 20;
   private boolean fixedColor;
   private int durationOnUse;
   private float radiusOnUse;
   private float radiusPerTick;
   @Nullable
   private LivingEntity owner;
   @Nullable
   private UUID ownerUUID;

   public AreaEffectCloud(EntityType<? extends AreaEffectCloud> param0, Level param1) {
      super(param0, param1);
      this.noPhysics = true;
   }

   public AreaEffectCloud(Level param0, double param1, double param3, double param5) {
      this(EntityType.AREA_EFFECT_CLOUD, param0);
      this.setPos(param1, param3, param5);
   }

   @Override
   protected void defineSynchedData() {
      this.getEntityData().define(DATA_COLOR, 0);
      this.getEntityData().define(DATA_RADIUS, 3.0F);
      this.getEntityData().define(DATA_WAITING, false);
      this.getEntityData().define(DATA_PARTICLE, ParticleTypes.ENTITY_EFFECT);
   }

   public void setRadius(float param0) {
      if (!this.level.isClientSide) {
         this.getEntityData().set(DATA_RADIUS, Mth.clamp(param0, 0.0F, 32.0F));
      }
   }

   @Override
   public void refreshDimensions() {
      double var1 = this.getX();
      double var3 = this.getY();
      double var5 = this.getZ();
      super.refreshDimensions();
      this.setPos(var1, var3, var5);
   }

   public float getRadius() {
      return this.getEntityData().get(DATA_RADIUS);
   }

   public void setPotion(Potion param0) {
      this.potion = param0;
      if (!this.fixedColor) {
         this.updateColor();
      }
   }

   private void updateColor() {
      if (this.potion == Potions.EMPTY && this.effects.isEmpty()) {
         this.getEntityData().set(DATA_COLOR, 0);
      } else {
         this.getEntityData().set(DATA_COLOR, PotionUtils.getColor(PotionUtils.getAllEffects(this.potion, this.effects)));
      }
   }

   public void addEffect(MobEffectInstance param0) {
      this.effects.add(param0);
      if (!this.fixedColor) {
         this.updateColor();
      }
   }

   public int getColor() {
      return this.getEntityData().get(DATA_COLOR);
   }

   public void setFixedColor(int param0) {
      this.fixedColor = true;
      this.getEntityData().set(DATA_COLOR, param0);
   }

   public ParticleOptions getParticle() {
      return this.getEntityData().get(DATA_PARTICLE);
   }

   public void setParticle(ParticleOptions param0) {
      this.getEntityData().set(DATA_PARTICLE, param0);
   }

   protected void setWaiting(boolean param0) {
      this.getEntityData().set(DATA_WAITING, param0);
   }

   public boolean isWaiting() {
      return this.getEntityData().get(DATA_WAITING);
   }

   public int getDuration() {
      return this.duration;
   }

   public void setDuration(int param0) {
      this.duration = param0;
   }

   @Override
   public void tick() {
      super.tick();
      boolean var1 = this.isWaiting();
      float var2 = this.getRadius();
      if (this.level.isClientSide) {
         if (var1 && this.random.nextBoolean()) {
            return;
         }

         ParticleOptions var3 = this.getParticle();
         int var4;
         float var5;
         if (var1) {
            var4 = 2;
            var5 = 0.2F;
         } else {
            var4 = Mth.ceil((float) Math.PI * var2 * var2);
            var5 = var2;
         }

         for (int var6 = 0; var6 < var4; var6++) {
            float var7 = this.random.nextFloat() * (float) (Math.PI * 2);
            float var8 = Mth.sqrt(this.random.nextFloat()) * var5;
            double var9 = this.getX() + Mth.cos(var7) * var8;
            double var11 = this.getY();
            double var13 = this.getZ() + Mth.sin(var7) * var8;
            double var15;
            double var17;
            double var19;
            if (var3.getType() == ParticleTypes.ENTITY_EFFECT) {
               int var21 = var1 && this.random.nextBoolean() ? 16777215 : this.getColor();
               var15 = (var21 >> 16 & 0xFF) / 255.0F;
               var17 = (var21 >> 8 & 0xFF) / 255.0F;
               var19 = (var21 & 0xFF) / 255.0F;
            } else if (var1) {
               var15 = 0.0;
               var17 = 0.0;
               var19 = 0.0;
            } else {
               var15 = (0.5 - this.random.nextDouble()) * 0.15;
               var17 = 0.01F;
               var19 = (0.5 - this.random.nextDouble()) * 0.15;
            }

            this.level.addAlwaysVisibleParticle(var3, var9, var11, var13, var15, var17, var19);
         }
      } else {
         if (this.tickCount >= this.waitTime + this.duration) {
            this.discard();
            return;
         }

         boolean var3 = this.tickCount < this.waitTime;
         if (var1 != var3) {
            this.setWaiting(var3);
         }

         if (var3) {
            return;
         }

         if (this.radiusPerTick != 0.0F) {
            var2 += this.radiusPerTick;
            if (var2 < 0.5F) {
               this.discard();
               return;
            }

            this.setRadius(var2);
         }

         if (this.tickCount % 5 == 0) {
            this.victims.entrySet().removeIf(l_param0 -> this.tickCount >= l_param0.getValue());
            List<MobEffectInstance> var4 = Lists.newArrayList();

            for (MobEffectInstance var6 : this.potion.getEffects()) {
               var4.add(
                  new MobEffectInstance(var6.getEffect(), var6.mapDuration(l_param0 -> l_param0 / 4), var6.getAmplifier(), var6.isAmbient(), var6.isVisible())
               );
            }

            var4.addAll(this.effects);
            if (var4.isEmpty()) {
               this.victims.clear();
            } else {
               List<LivingEntity> var5 = this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
               if (!var5.isEmpty()) {
                  for (LivingEntity var7 : var5) {
                     if (!this.victims.containsKey(var7) && var7.isAffectedByPotions()) {
                        double var8 = var7.getX() - this.getX();
                        double var10 = var7.getZ() - this.getZ();
                        double var12 = var8 * var8 + var10 * var10;
                        if (var12 <= var2 * var2) {
                           this.victims.put(var7, this.tickCount + this.reapplicationDelay);

                           for (MobEffectInstance var15 : var4) {
                              if (var15.getEffect().isInstantenous()) {
                                 var15.getEffect().applyInstantenousEffect(this, this.getOwner(), var7, var15.getAmplifier(), 0.5);
                              } else {
                                 var7.addEffect(new MobEffectInstance(var15), this);
                              }
                           }

                           if (this.radiusOnUse != 0.0F) {
                              var2 += this.radiusOnUse;
                              if (var2 < 0.5F) {
                                 this.discard();
                                 return;
                              }

                              this.setRadius(var2);
                           }

                           if (this.durationOnUse != 0) {
                              this.duration = this.duration + this.durationOnUse;
                              if (this.duration <= 0) {
                                 this.discard();
                                 return;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public float getRadiusOnUse() {
      return this.radiusOnUse;
   }

   public void setRadiusOnUse(float param0) {
      this.radiusOnUse = param0;
   }

   public float getRadiusPerTick() {
      return this.radiusPerTick;
   }

   public void setRadiusPerTick(float param0) {
      this.radiusPerTick = param0;
   }

   public int getDurationOnUse() {
      return this.durationOnUse;
   }

   public void setDurationOnUse(int param0) {
      this.durationOnUse = param0;
   }

   public int getWaitTime() {
      return this.waitTime;
   }

   public void setWaitTime(int param0) {
      this.waitTime = param0;
   }

   public void setOwner(@Nullable LivingEntity param0) {
      this.owner = param0;
      this.ownerUUID = param0 == null ? null : param0.getUUID();
   }

   @Nullable
   public LivingEntity getOwner() {
      if (this.owner == null && this.ownerUUID != null && this.level instanceof ServerLevel) {
         Entity var1 = ((ServerLevel)this.level).getEntity(this.ownerUUID);
         if (var1 instanceof LivingEntity) {
            this.owner = (LivingEntity)var1;
         }
      }

      return this.owner;
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag param0) {
      this.tickCount = param0.getInt("Age");
      this.duration = param0.getInt("Duration");
      this.waitTime = param0.getInt("WaitTime");
      this.reapplicationDelay = param0.getInt("ReapplicationDelay");
      this.durationOnUse = param0.getInt("DurationOnUse");
      this.radiusOnUse = param0.getFloat("RadiusOnUse");
      this.radiusPerTick = param0.getFloat("RadiusPerTick");
      this.setRadius(param0.getFloat("Radius"));
      if (param0.hasUUID("Owner")) {
         this.ownerUUID = param0.getUUID("Owner");
      }

      if (param0.contains("Particle", 8)) {
         try {
            this.setParticle(ParticleArgument.readParticle(new StringReader(param0.getString("Particle")), BuiltInRegistries.PARTICLE_TYPE.asLookup()));
         } catch (CommandSyntaxException var2) {
            LOGGER.warn("Couldn't load custom particle {}", param0.getString("Particle"), var2);
         }
      }

      if (param0.contains("Color", 99)) {
         this.setFixedColor(param0.getInt("Color"));
      }

      if (param0.contains("Potion", 8)) {
         this.setPotion(PotionUtils.getPotion(param0));
      }

      if (param0.contains("Effects", 9)) {
         ListTag var2 = param0.getList("Effects", 10);
         this.effects.clear();

         for (int var3 = 0; var3 < var2.size(); var3++) {
            MobEffectInstance var4 = MobEffectInstance.load(var2.getCompound(var3));
            if (var4 != null) {
               this.addEffect(var4);
            }
         }
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag param0) {
      param0.putInt("Age", this.tickCount);
      param0.putInt("Duration", this.duration);
      param0.putInt("WaitTime", this.waitTime);
      param0.putInt("ReapplicationDelay", this.reapplicationDelay);
      param0.putInt("DurationOnUse", this.durationOnUse);
      param0.putFloat("RadiusOnUse", this.radiusOnUse);
      param0.putFloat("RadiusPerTick", this.radiusPerTick);
      param0.putFloat("Radius", this.getRadius());
      param0.putString("Particle", this.getParticle().writeToString());
      if (this.ownerUUID != null) {
         param0.putUUID("Owner", this.ownerUUID);
      }

      if (this.fixedColor) {
         param0.putInt("Color", this.getColor());
      }

      if (this.potion != Potions.EMPTY) {
         param0.putString("Potion", BuiltInRegistries.POTION.getKey(this.potion).toString());
      }

      if (!this.effects.isEmpty()) {
         ListTag var2 = new ListTag();

         for (MobEffectInstance var4 : this.effects) {
            var2.add(var4.save(new CompoundTag()));
         }

         param0.put("Effects", var2);
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> param0) {
      if (DATA_RADIUS.equals(param0)) {
         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(param0);
   }

   public Potion getPotion() {
      return this.potion;
   }

   @Override
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @Override
   public EntityDimensions getDimensions(Pose param0) {
      return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
   }
}
