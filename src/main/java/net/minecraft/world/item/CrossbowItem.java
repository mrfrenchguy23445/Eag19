package net.minecraft.world.item;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CrossbowItem extends ProjectileWeaponItem implements Vanishable {
   private static final String TAG_CHARGED = "Charged";
   private static final String TAG_CHARGED_PROJECTILES = "ChargedProjectiles";
   private static final int MAX_CHARGE_DURATION = 25;
   public static final int DEFAULT_RANGE = 8;
   private boolean startSoundPlayed = false;
   private boolean midLoadSoundPlayed = false;
   private static final float START_SOUND_PERCENT = 0.2F;
   private static final float MID_SOUND_PERCENT = 0.5F;
   private static final float ARROW_POWER = 3.15F;
   private static final float FIREWORK_POWER = 1.6F;

   public CrossbowItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public Predicate<ItemStack> getSupportedHeldProjectiles() {
      return ARROW_OR_FIREWORK;
   }

   @Override
   public Predicate<ItemStack> getAllSupportedProjectiles() {
      return ARROW_ONLY;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      if (isCharged(var4)) {
         performShooting(param0, param1, param2, var4, getShootingPower(var4), 1.0F);
         setCharged(var4, false);
         return InteractionResultHolder.consume(var4);
      }

      if (!param1.getProjectile(var4).isEmpty()) {
         if (!isCharged(var4)) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
            param1.startUsingItem(param2);
         }

         return InteractionResultHolder.consume(var4);
      } else {
         return InteractionResultHolder.fail(var4);
      }
   }

   private static float getShootingPower(ItemStack param0) {
      return containsChargedProjectile(param0, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
   }

   @Override
   public void releaseUsing(ItemStack param0, Level param1, LivingEntity param2, int param3) {
      int var5 = this.getUseDuration(param0) - param3;
      float var6 = getPowerForTime(var5, param0);
      if (var6 >= 1.0F && !isCharged(param0) && tryLoadProjectiles(param2, param0)) {
         setCharged(param0, true);
         SoundSource var7 = param2 instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
         param1.playSound(
            null,
            param2.getX(),
            param2.getY(),
            param2.getZ(),
            SoundEvents.CROSSBOW_LOADING_END,
            var7,
            1.0F,
            1.0F / (param1.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F
         );
      }
   }

   private static boolean tryLoadProjectiles(LivingEntity param0, ItemStack param1) {
      int var2 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, param1);
      int var3 = var2 == 0 ? 1 : 3;
      boolean var4 = param0 instanceof Player && ((Player)param0).getAbilities().instabuild;
      ItemStack var5 = param0.getProjectile(param1);
      ItemStack var6 = var5.copy();

      for (int var7 = 0; var7 < var3; var7++) {
         if (var7 > 0) {
            var5 = var6.copy();
         }

         if (var5.isEmpty() && var4) {
            var5 = new ItemStack(Items.ARROW);
            var6 = var5.copy();
         }

         if (!loadProjectile(param0, param1, var5, var7 > 0, var4)) {
            return false;
         }
      }

      return true;
   }

   private static boolean loadProjectile(LivingEntity param0, ItemStack param1, ItemStack param2, boolean param3, boolean param4) {
      if (param2.isEmpty()) {
         return false;
      }

      boolean var5 = param4 && param2.getItem() instanceof ArrowItem;
      ItemStack var6;
      if (!var5 && !param4 && !param3) {
         var6 = param2.split(1);
         if (param2.isEmpty() && param0 instanceof Player) {
            ((Player)param0).getInventory().removeItem(param2);
         }
      } else {
         var6 = param2.copy();
      }

      addChargedProjectile(param1, var6);
      return true;
   }

   public static boolean isCharged(ItemStack param0) {
      CompoundTag var1 = param0.getTag();
      return var1 != null && var1.getBoolean("Charged");
   }

   public static void setCharged(ItemStack param0, boolean param1) {
      CompoundTag var2 = param0.getOrCreateTag();
      var2.putBoolean("Charged", param1);
   }

   private static void addChargedProjectile(ItemStack param0, ItemStack param1) {
      CompoundTag var2 = param0.getOrCreateTag();
      ListTag var3;
      if (var2.contains("ChargedProjectiles", 9)) {
         var3 = var2.getList("ChargedProjectiles", 10);
      } else {
         var3 = new ListTag();
      }

      CompoundTag var4 = new CompoundTag();
      param1.save(var4);
      var3.add(var4);
      var2.put("ChargedProjectiles", var3);
   }

   private static List<ItemStack> getChargedProjectiles(ItemStack param0) {
      List<ItemStack> var1 = Lists.newArrayList();
      CompoundTag var2 = param0.getTag();
      if (var2 != null && var2.contains("ChargedProjectiles", 9)) {
         ListTag var3 = var2.getList("ChargedProjectiles", 10);
         if (var3 != null) {
            for (int var4 = 0; var4 < var3.size(); var4++) {
               CompoundTag var5 = var3.getCompound(var4);
               var1.add(ItemStack.of(var5));
            }
         }
      }

      return var1;
   }

   private static void clearChargedProjectiles(ItemStack param0) {
      CompoundTag var1 = param0.getTag();
      if (var1 != null) {
         ListTag var2 = var1.getList("ChargedProjectiles", 9);
         var2.clear();
         var1.put("ChargedProjectiles", var2);
      }
   }

   public static boolean containsChargedProjectile(ItemStack param0, Item param1) {
      return getChargedProjectiles(param0).stream().anyMatch(l_param1 -> l_param1.is(param1));
   }

   private static void shootProjectile(
      Level param0,
      LivingEntity param1,
      InteractionHand param2,
      ItemStack param3,
      ItemStack param4,
      float param5,
      boolean param6,
      float param7,
      float param8,
      float param9
   ) {
      if (!param0.isClientSide) {
         boolean var10 = param4.is(Items.FIREWORK_ROCKET);
         Projectile var11;
         if (var10) {
            var11 = new FireworkRocketEntity(param0, param4, param1, param1.getX(), param1.getEyeY() - 0.15F, param1.getZ(), true);
         } else {
            var11 = getArrow(param0, param1, param3, param4);
            if (param6 || param9 != 0.0F) {
               ((AbstractArrow)var11).pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
         }

         if (param1 instanceof CrossbowAttackMob var12) {
            var12.shootCrossbowProjectile(var12.getTarget(), param3, var11, param9);
         } else {
            Vec3 var12 = param1.getUpVector(1.0F);
            Quaternionf var13 = new Quaternionf().setAngleAxis(param9 * (float) (Math.PI / 180.0), var12.x, var12.y, var12.z);
            Vec3 var14 = param1.getViewVector(1.0F);
            Vector3f var15 = var14.toVector3f().rotate(var13);
            var11.shoot(var15.x(), var15.y(), var15.z(), param7, param8);
         }

         param3.hurtAndBreak(var10 ? 3 : 1, param1, l_param1 -> l_param1.broadcastBreakEvent(param2));
         param0.addFreshEntity(var11);
         param0.playSound(null, param1.getX(), param1.getY(), param1.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, param5);
      }
   }

   private static AbstractArrow getArrow(Level param0, LivingEntity param1, ItemStack param2, ItemStack param3) {
      ArrowItem var4 = (ArrowItem)(param3.getItem() instanceof ArrowItem ? param3.getItem() : Items.ARROW);
      AbstractArrow var5 = var4.createArrow(param0, param3, param1);
      if (param1 instanceof Player) {
         var5.setCritArrow(true);
      }

      var5.setSoundEvent(SoundEvents.CROSSBOW_HIT);
      var5.setShotFromCrossbow(true);
      int var6 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, param2);
      if (var6 > 0) {
         var5.setPierceLevel((byte)var6);
      }

      return var5;
   }

   public static void performShooting(Level param0, LivingEntity param1, InteractionHand param2, ItemStack param3, float param4, float param5) {
      List<ItemStack> var6 = getChargedProjectiles(param3);
      float[] var7 = getShotPitches(param1.getRandom());

      for (int var8 = 0; var8 < var6.size(); var8++) {
         ItemStack var9 = var6.get(var8);
         boolean var10 = param1 instanceof Player && ((Player)param1).getAbilities().instabuild;
         if (!var9.isEmpty()) {
            if (var8 == 0) {
               shootProjectile(param0, param1, param2, param3, var9, var7[var8], var10, param4, param5, 0.0F);
            } else if (var8 == 1) {
               shootProjectile(param0, param1, param2, param3, var9, var7[var8], var10, param4, param5, -10.0F);
            } else if (var8 == 2) {
               shootProjectile(param0, param1, param2, param3, var9, var7[var8], var10, param4, param5, 10.0F);
            }
         }
      }

      onCrossbowShot(param0, param1, param3);
   }

   private static float[] getShotPitches(RandomSource param0) {
      boolean var1 = param0.nextBoolean();
      return new float[]{1.0F, getRandomShotPitch(var1, param0), getRandomShotPitch(!var1, param0)};
   }

   private static float getRandomShotPitch(boolean param0, RandomSource param1) {
      float var2 = param0 ? 0.63F : 0.43F;
      return 1.0F / (param1.nextFloat() * 0.5F + 1.8F) + var2;
   }

   private static void onCrossbowShot(Level param0, LivingEntity param1, ItemStack param2) {
      if (param1 instanceof ServerPlayer var3) {
         if (!param0.isClientSide) {
            CriteriaTriggers.SHOT_CROSSBOW.trigger(var3, param2);
         }

         var3.awardStat(Stats.ITEM_USED.get(param2.getItem()));
      }

      clearChargedProjectiles(param2);
   }

   @Override
   public void onUseTick(Level param0, LivingEntity param1, ItemStack param2, int param3) {
      if (!param0.isClientSide) {
         int var5 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, param2);
         SoundEvent var6 = this.getStartSound(var5);
         SoundEvent var7 = var5 == 0 ? SoundEvents.CROSSBOW_LOADING_MIDDLE : null;
         float var8 = (float)(param2.getUseDuration() - param3) / getChargeDuration(param2);
         if (var8 < 0.2F) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
         }

         if (var8 >= 0.2F && !this.startSoundPlayed) {
            this.startSoundPlayed = true;
            param0.playSound(null, param1.getX(), param1.getY(), param1.getZ(), var6, SoundSource.PLAYERS, 0.5F, 1.0F);
         }

         if (var8 >= 0.5F && var7 != null && !this.midLoadSoundPlayed) {
            this.midLoadSoundPlayed = true;
            param0.playSound(null, param1.getX(), param1.getY(), param1.getZ(), var7, SoundSource.PLAYERS, 0.5F, 1.0F);
         }
      }
   }

   @Override
   public int getUseDuration(ItemStack param0) {
      return getChargeDuration(param0) + 3;
   }

   public static int getChargeDuration(ItemStack param0) {
      int var1 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, param0);
      return var1 == 0 ? 25 : 25 - 5 * var1;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack param0) {
      return UseAnim.CROSSBOW;
   }

   private SoundEvent getStartSound(int param0) {
      switch (param0) {
         case 1:
            return SoundEvents.CROSSBOW_QUICK_CHARGE_1;
         case 2:
            return SoundEvents.CROSSBOW_QUICK_CHARGE_2;
         case 3:
            return SoundEvents.CROSSBOW_QUICK_CHARGE_3;
         default:
            return SoundEvents.CROSSBOW_LOADING_START;
      }
   }

   private static float getPowerForTime(int param0, ItemStack param1) {
      float var2 = (float)param0 / getChargeDuration(param1);
      if (var2 > 1.0F) {
         var2 = 1.0F;
      }

      return var2;
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      List<ItemStack> var5 = getChargedProjectiles(param0);
      if (isCharged(param0) && !var5.isEmpty()) {
         ItemStack var6 = var5.get(0);
         param2.add(Component.translatable("item.minecraft.crossbow.projectile").append(CommonComponents.SPACE).append(var6.getDisplayName()));
         if (param3.isAdvanced() && var6.is(Items.FIREWORK_ROCKET)) {
            List<Component> var7 = Lists.newArrayList();
            Items.FIREWORK_ROCKET.appendHoverText(var6, param1, var7, param3);
            if (!var7.isEmpty()) {
               for (int var8 = 0; var8 < var7.size(); var8++) {
                  var7.set(var8, Component.literal("  ").append(var7.get(var8)).withStyle(ChatFormatting.GRAY));
               }

               param2.addAll(var7);
            }
         }
      }
   }

   @Override
   public boolean useOnRelease(ItemStack param0) {
      return param0.is(this);
   }

   @Override
   public int getDefaultProjectileRange() {
      return 8;
   }
}
