package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class DamageSource {
   private final Holder<DamageType> type;
   @Nullable
   private final Entity causingEntity;
   @Nullable
   private final Entity directEntity;
   @Nullable
   private final Vec3 damageSourcePosition;

   @Override
   public String toString() {
      return "DamageSource (" + this.type().msgId() + ")";
   }

   public float getFoodExhaustion() {
      return this.type().exhaustion();
   }

   public boolean isIndirect() {
      return this.causingEntity != this.directEntity;
   }

   private DamageSource(Holder<DamageType> param0, @Nullable Entity param1, @Nullable Entity param2, @Nullable Vec3 param3) {
      this.type = param0;
      this.causingEntity = param2;
      this.directEntity = param1;
      this.damageSourcePosition = param3;
   }

   public DamageSource(Holder<DamageType> param0, @Nullable Entity param1, @Nullable Entity param2) {
      this(param0, param1, param2, null);
   }

   public DamageSource(Holder<DamageType> param0, Vec3 param1) {
      this(param0, null, null, param1);
   }

   public DamageSource(Holder<DamageType> param0, @Nullable Entity param1) {
      this(param0, param1, param1);
   }

   public DamageSource(Holder<DamageType> param0) {
      this(param0, null, null, null);
   }

   @Nullable
   public Entity getDirectEntity() {
      return this.directEntity;
   }

   @Nullable
   public Entity getEntity() {
      return this.causingEntity;
   }

   public Component getLocalizedDeathMessage(LivingEntity param0) {
      String var2 = "death.attack." + this.type().msgId();
      if (this.causingEntity == null && this.directEntity == null) {
         LivingEntity var3 = param0.getKillCredit();
         String var4 = var2 + ".player";
         return var3 != null
            ? Component.translatable(var4, param0.getDisplayName(), var3.getDisplayName())
            : Component.translatable(var2, param0.getDisplayName());
      } else {
         Component var3 = this.causingEntity == null ? this.directEntity.getDisplayName() : this.causingEntity.getDisplayName();
         ItemStack var4 = this.causingEntity instanceof LivingEntity var5 ? var5.getMainHandItem() : ItemStack.EMPTY;
         return !var4.isEmpty() && var4.hasCustomHoverName()
            ? Component.translatable(var2 + ".item", param0.getDisplayName(), var3, var4.getDisplayName())
            : Component.translatable(var2, param0.getDisplayName(), var3);
      }
   }

   public String getMsgId() {
      return this.type().msgId();
   }

   public boolean scalesWithDifficulty() {
      return switch (this.type().scaling()) {
         case NEVER -> false;
         case WHEN_CAUSED_BY_LIVING_NON_PLAYER -> this.causingEntity instanceof LivingEntity && !(this.causingEntity instanceof Player);
         case ALWAYS -> true;
      };
   }

   public boolean isCreativePlayer() {
      return this.getEntity() instanceof Player var1 && var1.getAbilities().instabuild;
   }

   @Nullable
   public Vec3 getSourcePosition() {
      if (this.damageSourcePosition != null) {
         return this.damageSourcePosition;
      } else {
         return this.causingEntity != null ? this.causingEntity.position() : null;
      }
   }

   @Nullable
   public Vec3 sourcePositionRaw() {
      return this.damageSourcePosition;
   }

   public boolean is(TagKey<DamageType> param0) {
      return this.type.is(param0);
   }

   public boolean is(ResourceKey<DamageType> param0) {
      return this.type.is(param0);
   }

   public DamageType type() {
      return this.type.value();
   }

   public Holder<DamageType> typeHolder() {
      return this.type;
   }
}
