package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class Item implements FeatureElement, ItemLike {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Map<Block, Item> BY_BLOCK = Maps.newHashMap();
   protected static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
   protected static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
   public static final int MAX_STACK_SIZE = 64;
   public static final int EAT_DURATION = 32;
   public static final int MAX_BAR_WIDTH = 13;
   private final Holder.Reference<Item> builtInRegistryHolder = BuiltInRegistries.ITEM.createIntrusiveHolder(this);
   private final Rarity rarity;
   private final int maxStackSize;
   private final int maxDamage;
   private final boolean isFireResistant;
   @Nullable
   private final Item craftingRemainingItem;
   @Nullable
   private String descriptionId;
   @Nullable
   private final FoodProperties foodProperties;
   private final FeatureFlagSet requiredFeatures;

   public static int getId(Item param0) {
      return param0 == null ? 0 : BuiltInRegistries.ITEM.getId(param0);
   }

   public static Item byId(int param0) {
      return BuiltInRegistries.ITEM.byId(param0);
   }

   @Deprecated
   public static Item byBlock(Block param0) {
      return BY_BLOCK.getOrDefault(param0, Items.AIR);
   }

   public Item(Item.Properties param0) {
      this.rarity = param0.rarity;
      this.craftingRemainingItem = param0.craftingRemainingItem;
      this.maxDamage = param0.maxDamage;
      this.maxStackSize = param0.maxStackSize;
      this.foodProperties = param0.foodProperties;
      this.isFireResistant = param0.isFireResistant;
      this.requiredFeatures = param0.requiredFeatures;
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         String var2 = this.getClass().getSimpleName();
         if (!var2.endsWith("Item")) {
            LOGGER.error("Item classes should end with Item and {} doesn't.", var2);
         }
      }
   }

   @Deprecated
   public Holder.Reference<Item> builtInRegistryHolder() {
      return this.builtInRegistryHolder;
   }

   public void onUseTick(Level param0, LivingEntity param1, ItemStack param2, int param3) {
   }

   public void onDestroyed(ItemEntity param0) {
   }

   public void verifyTagAfterLoad(CompoundTag param0) {
   }

   public boolean canAttackBlock(BlockState param0, Level param1, BlockPos param2, Player param3) {
      return true;
   }

   @Override
   public Item asItem() {
      return this;
   }

   public InteractionResult useOn(UseOnContext param0) {
      return InteractionResult.PASS;
   }

   public float getDestroySpeed(ItemStack param0, BlockState param1) {
      return 1.0F;
   }

   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      if (this.isEdible()) {
         ItemStack var4 = param1.getItemInHand(param2);
         if (param1.canEat(this.getFoodProperties().canAlwaysEat())) {
            param1.startUsingItem(param2);
            return InteractionResultHolder.consume(var4);
         } else {
            return InteractionResultHolder.fail(var4);
         }
      } else {
         return InteractionResultHolder.pass(param1.getItemInHand(param2));
      }
   }

   public ItemStack finishUsingItem(ItemStack param0, Level param1, LivingEntity param2) {
      return this.isEdible() ? param2.eat(param1, param0) : param0;
   }

   public final int getMaxStackSize() {
      return this.maxStackSize;
   }

   public final int getMaxDamage() {
      return this.maxDamage;
   }

   public boolean canBeDepleted() {
      return this.maxDamage > 0;
   }

   public boolean isBarVisible(ItemStack param0) {
      return param0.isDamaged();
   }

   public int getBarWidth(ItemStack param0) {
      return Math.round(13.0F - param0.getDamageValue() * 13.0F / this.maxDamage);
   }

   public int getBarColor(ItemStack param0) {
      float var2 = Math.max(0.0F, ((float)this.maxDamage - param0.getDamageValue()) / this.maxDamage);
      return Mth.hsvToRgb(var2 / 3.0F, 1.0F, 1.0F);
   }

   public boolean overrideStackedOnOther(ItemStack param0, Slot param1, ClickAction param2, Player param3) {
      return false;
   }

   public boolean overrideOtherStackedOnMe(ItemStack param0, ItemStack param1, Slot param2, ClickAction param3, Player param4, SlotAccess param5) {
      return false;
   }

   public boolean hurtEnemy(ItemStack param0, LivingEntity param1, LivingEntity param2) {
      return false;
   }

   public boolean mineBlock(ItemStack param0, Level param1, BlockState param2, BlockPos param3, LivingEntity param4) {
      return false;
   }

   public boolean isCorrectToolForDrops(BlockState param0) {
      return false;
   }

   public InteractionResult interactLivingEntity(ItemStack param0, Player param1, LivingEntity param2, InteractionHand param3) {
      return InteractionResult.PASS;
   }

   public Component getDescription() {
      return Component.translatable(this.getDescriptionId());
   }

   @Override
   public String toString() {
      return BuiltInRegistries.ITEM.getKey(this).getPath();
   }

   protected String getOrCreateDescriptionId() {
      if (this.descriptionId == null) {
         this.descriptionId = Util.makeDescriptionId("item", BuiltInRegistries.ITEM.getKey(this));
      }

      return this.descriptionId;
   }

   public String getDescriptionId() {
      return this.getOrCreateDescriptionId();
   }

   public String getDescriptionId(ItemStack param0) {
      return this.getDescriptionId();
   }

   public boolean shouldOverrideMultiplayerNbt() {
      return true;
   }

   @Nullable
   public final Item getCraftingRemainingItem() {
      return this.craftingRemainingItem;
   }

   public boolean hasCraftingRemainingItem() {
      return this.craftingRemainingItem != null;
   }

   public void inventoryTick(ItemStack param0, Level param1, Entity param2, int param3, boolean param4) {
   }

   public void onCraftedBy(ItemStack param0, Level param1, Player param2) {
   }

   public boolean isComplex() {
      return false;
   }

   public UseAnim getUseAnimation(ItemStack param0) {
      return param0.getItem().isEdible() ? UseAnim.EAT : UseAnim.NONE;
   }

   public int getUseDuration(ItemStack param0) {
      if (param0.getItem().isEdible()) {
         return this.getFoodProperties().isFastFood() ? 16 : 32;
      } else {
         return 0;
      }
   }

   public void releaseUsing(ItemStack param0, Level param1, LivingEntity param2, int param3) {
   }

   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
   }

   public Optional<TooltipComponent> getTooltipImage(ItemStack param0) {
      return Optional.empty();
   }

   public Component getName(ItemStack param0) {
      return Component.translatable(this.getDescriptionId(param0));
   }

   public boolean isFoil(ItemStack param0) {
      return param0.isEnchanted();
   }

   public Rarity getRarity(ItemStack param0) {
      if (!param0.isEnchanted()) {
         return this.rarity;
      }

      switch (this.rarity) {
         case COMMON:
         case UNCOMMON:
            return Rarity.RARE;
         case RARE:
            return Rarity.EPIC;
         case EPIC:
         default:
            return this.rarity;
      }
   }

   public boolean isEnchantable(ItemStack param0) {
      return this.getMaxStackSize() == 1 && this.canBeDepleted();
   }

   protected static BlockHitResult getPlayerPOVHitResult(Level param0, Player param1, ClipContext.Fluid param2) {
      float var3 = param1.getXRot();
      float var4 = param1.getYRot();
      Vec3 var5 = param1.getEyePosition();
      float var6 = Mth.cos(-var4 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float var7 = Mth.sin(-var4 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float var8 = -Mth.cos(-var3 * (float) (Math.PI / 180.0));
      float var9 = Mth.sin(-var3 * (float) (Math.PI / 180.0));
      float var10 = var7 * var8;
      float var11 = var9;
      float var12 = var6 * var8;
      double var13 = 5.0;
      Vec3 var15 = var5.add(var10 * 5.0, var11 * 5.0, var12 * 5.0);
      return param0.clip(new ClipContext(var5, var15, ClipContext.Block.OUTLINE, param2, param1));
   }

   public int getEnchantmentValue() {
      return 0;
   }

   public boolean isValidRepairItem(ItemStack param0, ItemStack param1) {
      return false;
   }

   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot param0) {
      return ImmutableMultimap.of();
   }

   public boolean useOnRelease(ItemStack param0) {
      return false;
   }

   public ItemStack getDefaultInstance() {
      return new ItemStack(this);
   }

   public boolean isEdible() {
      return this.foodProperties != null;
   }

   @Nullable
   public FoodProperties getFoodProperties() {
      return this.foodProperties;
   }

   public SoundEvent getDrinkingSound() {
      return SoundEvents.GENERIC_DRINK;
   }

   public SoundEvent getEatingSound() {
      return SoundEvents.GENERIC_EAT;
   }

   public boolean isFireResistant() {
      return this.isFireResistant;
   }

   public boolean canBeHurtBy(DamageSource param0) {
      return !this.isFireResistant || !param0.is(DamageTypeTags.IS_FIRE);
   }

   public boolean canFitInsideContainerItems() {
      return true;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.requiredFeatures;
   }

   public static class Properties {
      int maxStackSize = 64;
      int maxDamage;
      @Nullable
      Item craftingRemainingItem;
      Rarity rarity = Rarity.COMMON;
      @Nullable
      FoodProperties foodProperties;
      boolean isFireResistant;
      FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;

      public Item.Properties food(FoodProperties param0) {
         this.foodProperties = param0;
         return this;
      }

      public Item.Properties stacksTo(int param0) {
         if (this.maxDamage > 0) {
            throw new RuntimeException("Unable to have damage AND stack.");
         }

         this.maxStackSize = param0;
         return this;
      }

      public Item.Properties defaultDurability(int param0) {
         return this.maxDamage == 0 ? this.durability(param0) : this;
      }

      public Item.Properties durability(int param0) {
         this.maxDamage = param0;
         this.maxStackSize = 1;
         return this;
      }

      public Item.Properties craftRemainder(Item param0) {
         this.craftingRemainingItem = param0;
         return this;
      }

      public Item.Properties rarity(Rarity param0) {
         this.rarity = param0;
         return this;
      }

      public Item.Properties fireResistant() {
         this.isFireResistant = true;
         return this;
      }

      public Item.Properties requiredFeatures(FeatureFlag... param0) {
         this.requiredFeatures = FeatureFlags.REGISTRY.subset(param0);
         return this;
      }
   }
}
