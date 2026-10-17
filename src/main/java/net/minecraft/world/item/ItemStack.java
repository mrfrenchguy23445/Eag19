package net.minecraft.world.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.slf4j.Logger;

public final class ItemStack {
   public static final Codec<ItemStack> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(l_param0 -> l_param0.item),
            Codec.INT.fieldOf("Count").forGetter(l_param0 -> l_param0.count),
            CompoundTag.CODEC.optionalFieldOf("tag").forGetter(l_param0 -> Optional.ofNullable(l_param0.tag))
         )
         .apply(param0, ItemStack::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ItemStack EMPTY = new ItemStack((Item)null);
   public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = Util.make(
      new DecimalFormat("#.##"), param0 -> param0.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT))
   );
   public static final String TAG_ENCH = "Enchantments";
   public static final String TAG_DISPLAY = "display";
   public static final String TAG_DISPLAY_NAME = "Name";
   public static final String TAG_LORE = "Lore";
   public static final String TAG_DAMAGE = "Damage";
   public static final String TAG_COLOR = "color";
   private static final String TAG_UNBREAKABLE = "Unbreakable";
   private static final String TAG_REPAIR_COST = "RepairCost";
   private static final String TAG_CAN_DESTROY_BLOCK_LIST = "CanDestroy";
   private static final String TAG_CAN_PLACE_ON_BLOCK_LIST = "CanPlaceOn";
   private static final String TAG_HIDE_FLAGS = "HideFlags";
   private static final Component DISABLED_ITEM_TOOLTIP = Component.translatable("item.disabled").withStyle(ChatFormatting.RED);
   private static final int DONT_HIDE_TOOLTIP = 0;
   private static final Style LORE_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true);
   private int count;
   private int popTime;
   @Deprecated
   private final Item item;
   @Nullable
   private CompoundTag tag;
   private boolean emptyCacheFlag;
   @Nullable
   private Entity entityRepresentation;
   @Nullable
   private AdventureModeCheck adventureBreakCheck;
   @Nullable
   private AdventureModeCheck adventurePlaceCheck;

   public Optional<TooltipComponent> getTooltipImage() {
      return this.getItem().getTooltipImage(this);
   }

   public ItemStack(ItemLike param0) {
      this(param0, 1);
   }

   public ItemStack(Holder<Item> param0) {
      this(param0.value(), 1);
   }

   private ItemStack(ItemLike param0, int param1, Optional<CompoundTag> param2) {
      this(param0, param1);
      param2.ifPresent(this::setTag);
   }

   public ItemStack(Holder<Item> param0, int param1) {
      this(param0.value(), param1);
   }

   public ItemStack(ItemLike param0, int param1) {
      this.item = param0 == null ? null : param0.asItem();
      this.count = param1;
      if (this.item != null && this.item.canBeDepleted()) {
         this.setDamageValue(this.getDamageValue());
      }

      this.updateEmptyCacheFlag();
   }

   private void updateEmptyCacheFlag() {
      this.emptyCacheFlag = false;
      this.emptyCacheFlag = this.isEmpty();
   }

   private ItemStack(CompoundTag param0) {
      this.item = BuiltInRegistries.ITEM.get(new ResourceLocation(param0.getString("id")));
      this.count = param0.getByte("Count");
      if (param0.contains("tag", 10)) {
         this.tag = param0.getCompound("tag");
         this.getItem().verifyTagAfterLoad(this.tag);
      }

      if (this.getItem().canBeDepleted()) {
         this.setDamageValue(this.getDamageValue());
      }

      this.updateEmptyCacheFlag();
   }

   public static ItemStack of(CompoundTag param0) {
      try {
         return new ItemStack(param0);
      } catch (RuntimeException var1) {
         LOGGER.debug("Tried to load invalid item: {}", param0, var1);
         return EMPTY;
      }
   }

   public boolean isEmpty() {
      if (this == EMPTY) {
         return true;
      } else {
         return this.getItem() == null || this.is(Items.AIR) ? true : this.count <= 0;
      }
   }

   public boolean isItemEnabled(FeatureFlagSet param0) {
      return this.isEmpty() || this.getItem().isEnabled(param0);
   }

   public ItemStack split(int param0) {
      int var2 = Math.min(param0, this.count);
      ItemStack var3 = this.copy();
      var3.setCount(var2);
      this.shrink(var2);
      return var3;
   }

   public Item getItem() {
      return this.emptyCacheFlag ? Items.AIR : this.item;
   }

   public Holder<Item> getItemHolder() {
      return this.getItem().builtInRegistryHolder();
   }

   public boolean is(TagKey<Item> param0) {
      return this.getItem().builtInRegistryHolder().is(param0);
   }

   public boolean is(Item param0) {
      return this.getItem() == param0;
   }

   public boolean is(Predicate<Holder<Item>> param0) {
      return param0.test(this.getItem().builtInRegistryHolder());
   }

   public boolean is(Holder<Item> param0) {
      return this.getItem().builtInRegistryHolder() == param0;
   }

   public Stream<TagKey<Item>> getTags() {
      return this.getItem().builtInRegistryHolder().tags();
   }

   public InteractionResult useOn(UseOnContext param0) {
      Player var2 = param0.getPlayer();
      BlockPos var3 = param0.getClickedPos();
      BlockInWorld var4 = new BlockInWorld(param0.getLevel(), var3, false);
      if (var2 != null
         && !var2.getAbilities().mayBuild
         && !this.hasAdventureModePlaceTagForBlock(param0.getLevel().registryAccess().registryOrThrow(Registries.BLOCK), var4)) {
         return InteractionResult.PASS;
      }

      Item var5 = this.getItem();
      InteractionResult var6 = var5.useOn(param0);
      if (var2 != null && var6.shouldAwardStats()) {
         var2.awardStat(Stats.ITEM_USED.get(var5));
      }

      return var6;
   }

   public float getDestroySpeed(BlockState param0) {
      return this.getItem().getDestroySpeed(this, param0);
   }

   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      return this.getItem().use(param0, param1, param2);
   }

   public ItemStack finishUsingItem(Level param0, LivingEntity param1) {
      return this.getItem().finishUsingItem(this, param0, param1);
   }

   public CompoundTag save(CompoundTag param0) {
      ResourceLocation var2 = BuiltInRegistries.ITEM.getKey(this.getItem());
      param0.putString("id", var2 == null ? "minecraft:air" : var2.toString());
      param0.putByte("Count", (byte)this.count);
      if (this.tag != null) {
         param0.put("tag", this.tag.copy());
      }

      return param0;
   }

   public int getMaxStackSize() {
      return this.getItem().getMaxStackSize();
   }

   public boolean isStackable() {
      return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
   }

   public boolean isDamageableItem() {
      if (!this.emptyCacheFlag && this.getItem().getMaxDamage() > 0) {
         CompoundTag var1 = this.getTag();
         return var1 == null || !var1.getBoolean("Unbreakable");
      } else {
         return false;
      }
   }

   public boolean isDamaged() {
      return this.isDamageableItem() && this.getDamageValue() > 0;
   }

   public int getDamageValue() {
      return this.tag == null ? 0 : this.tag.getInt("Damage");
   }

   public void setDamageValue(int param0) {
      this.getOrCreateTag().putInt("Damage", Math.max(0, param0));
   }

   public int getMaxDamage() {
      return this.getItem().getMaxDamage();
   }

   public boolean hurt(int param0, RandomSource param1, @Nullable ServerPlayer param2) {
      if (!this.isDamageableItem()) {
         return false;
      }

      if (param0 > 0) {
         int var4 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, this);
         int var5 = 0;

         for (int var6 = 0; var4 > 0 && var6 < param0; var6++) {
            if (DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(this, var4, param1)) {
               var5++;
            }
         }

         param0 -= var5;
         if (param0 <= 0) {
            return false;
         }
      }

      if (param2 != null && param0 != 0) {
         CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(param2, this, this.getDamageValue() + param0);
      }

      int var4 = this.getDamageValue() + param0;
      this.setDamageValue(var4);
      return var4 >= this.getMaxDamage();
   }

   public <T extends LivingEntity> void hurtAndBreak(int param0, T param1, Consumer<T> param2) {
      if (!param1.level.isClientSide && (!(param1 instanceof Player) || !((Player)param1).getAbilities().instabuild)) {
         if (this.isDamageableItem()) {
            if (this.hurt(param0, param1.getRandom(), param1 instanceof ServerPlayer ? (ServerPlayer)param1 : null)) {
               param2.accept(param1);
               Item var4 = this.getItem();
               this.shrink(1);
               if (param1 instanceof Player) {
                  ((Player)param1).awardStat(Stats.ITEM_BROKEN.get(var4));
               }

               this.setDamageValue(0);
            }
         }
      }
   }

   public boolean isBarVisible() {
      return this.item.isBarVisible(this);
   }

   public int getBarWidth() {
      return this.item.getBarWidth(this);
   }

   public int getBarColor() {
      return this.item.getBarColor(this);
   }

   public boolean overrideStackedOnOther(Slot param0, ClickAction param1, Player param2) {
      return this.getItem().overrideStackedOnOther(this, param0, param1, param2);
   }

   public boolean overrideOtherStackedOnMe(ItemStack param0, Slot param1, ClickAction param2, Player param3, SlotAccess param4) {
      return this.getItem().overrideOtherStackedOnMe(this, param0, param1, param2, param3, param4);
   }

   public void hurtEnemy(LivingEntity param0, Player param1) {
      Item var3 = this.getItem();
      if (var3.hurtEnemy(this, param0, param1)) {
         param1.awardStat(Stats.ITEM_USED.get(var3));
      }
   }

   public void mineBlock(Level param0, BlockState param1, BlockPos param2, Player param3) {
      Item var5 = this.getItem();
      if (var5.mineBlock(this, param0, param1, param2, param3)) {
         param3.awardStat(Stats.ITEM_USED.get(var5));
      }
   }

   public boolean isCorrectToolForDrops(BlockState param0) {
      return this.getItem().isCorrectToolForDrops(param0);
   }

   public InteractionResult interactLivingEntity(Player param0, LivingEntity param1, InteractionHand param2) {
      return this.getItem().interactLivingEntity(this, param0, param1, param2);
   }

   public ItemStack copy() {
      if (this.isEmpty()) {
         return EMPTY;
      }

      ItemStack var1 = new ItemStack(this.getItem(), this.count);
      var1.setPopTime(this.getPopTime());
      if (this.tag != null) {
         var1.tag = this.tag.copy();
      }

      return var1;
   }

   public ItemStack copyWithCount(int param0) {
      ItemStack var2 = this.copy();
      var2.setCount(param0);
      return var2;
   }

   public static boolean tagMatches(ItemStack param0, ItemStack param1) {
      if (param0.isEmpty() && param1.isEmpty()) {
         return true;
      } else if (param0.isEmpty() || param1.isEmpty()) {
         return false;
      } else {
         return param0.tag == null && param1.tag != null ? false : param0.tag == null || param0.tag.equals(param1.tag);
      }
   }

   public static boolean matches(ItemStack param0, ItemStack param1) {
      if (param0.isEmpty() && param1.isEmpty()) {
         return true;
      } else {
         return !param0.isEmpty() && !param1.isEmpty() ? param0.matches(param1) : false;
      }
   }

   private boolean matches(ItemStack param0) {
      if (this.count != param0.count) {
         return false;
      } else if (!this.is(param0.getItem())) {
         return false;
      } else {
         return this.tag == null && param0.tag != null ? false : this.tag == null || this.tag.equals(param0.tag);
      }
   }

   public static boolean isSame(ItemStack param0, ItemStack param1) {
      if (param0 == param1) {
         return true;
      } else {
         return !param0.isEmpty() && !param1.isEmpty() ? param0.sameItem(param1) : false;
      }
   }

   public boolean sameItem(ItemStack param0) {
      return !param0.isEmpty() && this.is(param0.getItem());
   }

   public static boolean isSameItemSameTags(ItemStack param0, ItemStack param1) {
      return param0.is(param1.getItem()) && tagMatches(param0, param1);
   }

   public String getDescriptionId() {
      return this.getItem().getDescriptionId(this);
   }

   @Override
   public String toString() {
      return this.count + " " + this.getItem();
   }

   public void inventoryTick(Level param0, Entity param1, int param2, boolean param3) {
      if (this.popTime > 0) {
         this.popTime--;
      }

      if (this.getItem() != null) {
         this.getItem().inventoryTick(this, param0, param1, param2, param3);
      }
   }

   public void onCraftedBy(Level param0, Player param1, int param2) {
      param1.awardStat(Stats.ITEM_CRAFTED.get(this.getItem()), param2);
      this.getItem().onCraftedBy(this, param0, param1);
   }

   public int getUseDuration() {
      return this.getItem().getUseDuration(this);
   }

   public UseAnim getUseAnimation() {
      return this.getItem().getUseAnimation(this);
   }

   public void releaseUsing(Level param0, LivingEntity param1, int param2) {
      this.getItem().releaseUsing(this, param0, param1, param2);
   }

   public boolean useOnRelease() {
      return this.getItem().useOnRelease(this);
   }

   public boolean hasTag() {
      return !this.emptyCacheFlag && this.tag != null && !this.tag.isEmpty();
   }

   @Nullable
   public CompoundTag getTag() {
      return this.tag;
   }

   public CompoundTag getOrCreateTag() {
      if (this.tag == null) {
         this.setTag(new CompoundTag());
      }

      return this.tag;
   }

   public CompoundTag getOrCreateTagElement(String param0) {
      if (this.tag != null && this.tag.contains(param0, 10)) {
         return this.tag.getCompound(param0);
      }

      CompoundTag var2 = new CompoundTag();
      this.addTagElement(param0, var2);
      return var2;
   }

   @Nullable
   public CompoundTag getTagElement(String param0) {
      return this.tag != null && this.tag.contains(param0, 10) ? this.tag.getCompound(param0) : null;
   }

   public void removeTagKey(String param0) {
      if (this.tag != null && this.tag.contains(param0)) {
         this.tag.remove(param0);
         if (this.tag.isEmpty()) {
            this.tag = null;
         }
      }
   }

   public ListTag getEnchantmentTags() {
      return this.tag != null ? this.tag.getList("Enchantments", 10) : new ListTag();
   }

   public void setTag(@Nullable CompoundTag param0) {
      this.tag = param0;
      if (this.getItem().canBeDepleted()) {
         this.setDamageValue(this.getDamageValue());
      }

      if (param0 != null) {
         this.getItem().verifyTagAfterLoad(param0);
      }
   }

   public Component getHoverName() {
      CompoundTag var1 = this.getTagElement("display");
      if (var1 != null && var1.contains("Name", 8)) {
         try {
            Component var2 = Component.Serializer.fromJson(var1.getString("Name"));
            if (var2 != null) {
               return var2;
            }

            var1.remove("Name");
         } catch (Exception var2) {
            var1.remove("Name");
         }
      }

      return this.getItem().getName(this);
   }

   public ItemStack setHoverName(@Nullable Component param0) {
      CompoundTag var2 = this.getOrCreateTagElement("display");
      if (param0 != null) {
         var2.putString("Name", Component.Serializer.toJson(param0));
      } else {
         var2.remove("Name");
      }

      return this;
   }

   public void resetHoverName() {
      CompoundTag var1 = this.getTagElement("display");
      if (var1 != null) {
         var1.remove("Name");
         if (var1.isEmpty()) {
            this.removeTagKey("display");
         }
      }

      if (this.tag != null && this.tag.isEmpty()) {
         this.tag = null;
      }
   }

   public boolean hasCustomHoverName() {
      CompoundTag var1 = this.getTagElement("display");
      return var1 != null && var1.contains("Name", 8);
   }

   public List<Component> getTooltipLines(@Nullable Player param0, TooltipFlag param1) {
      List<Component> var3 = Lists.newArrayList();
      MutableComponent var4 = Component.empty().append(this.getHoverName()).withStyle(this.getRarity().color);
      if (this.hasCustomHoverName()) {
         var4.withStyle(ChatFormatting.ITALIC);
      }

      var3.add(var4);
      if (!param1.isAdvanced() && !this.hasCustomHoverName() && this.is(Items.FILLED_MAP)) {
         Integer var5 = MapItem.getMapId(this);
         if (var5 != null) {
            var3.add(Component.literal("#" + var5).withStyle(ChatFormatting.GRAY));
         }
      }

      int var5 = this.getHideFlags();
      if (shouldShowInTooltip(var5, ItemStack.TooltipPart.ADDITIONAL)) {
         this.getItem().appendHoverText(this, param0 == null ? null : param0.level, var3, param1);
      }

      if (this.hasTag()) {
         if (shouldShowInTooltip(var5, ItemStack.TooltipPart.UPGRADES) && param0 != null) {
            ArmorTrim.appendUpgradeHoverText(this, param0.level.registryAccess(), var3);
         }

         if (shouldShowInTooltip(var5, ItemStack.TooltipPart.ENCHANTMENTS)) {
            appendEnchantmentNames(var3, this.getEnchantmentTags());
         }

         if (this.tag.contains("display", 10)) {
            CompoundTag var6 = this.tag.getCompound("display");
            if (shouldShowInTooltip(var5, ItemStack.TooltipPart.DYE) && var6.contains("color", 99)) {
               if (param1.isAdvanced()) {
                  var3.add(Component.translatable("item.color", String.format(Locale.ROOT, "#%06X", var6.getInt("color"))).withStyle(ChatFormatting.GRAY));
               } else {
                  var3.add(Component.translatable("item.dyed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
               }
            }

            if (var6.getTagType("Lore") == 9) {
               ListTag var7 = var6.getList("Lore", 8);

               for (int var8 = 0; var8 < var7.size(); var8++) {
                  String var9 = var7.getString(var8);

                  try {
                     MutableComponent var10 = Component.Serializer.fromJson(var9);
                     if (var10 != null) {
                        var3.add(ComponentUtils.mergeStyles(var10, LORE_STYLE));
                     }
                  } catch (Exception var10) {
                     var6.remove("Lore");
                  }
               }
            }
         }
      }

      if (shouldShowInTooltip(var5, ItemStack.TooltipPart.MODIFIERS)) {
         for (EquipmentSlot var9 : EquipmentSlot.values()) {
            Multimap<Attribute, AttributeModifier> var10 = this.getAttributeModifiers(var9);
            if (!var10.isEmpty()) {
               var3.add(CommonComponents.EMPTY);
               var3.add(Component.translatable("item.modifiers." + var9.getName()).withStyle(ChatFormatting.GRAY));

               for (Entry<Attribute, AttributeModifier> var12 : var10.entries()) {
                  AttributeModifier var13 = var12.getValue();
                  double var14 = var13.getAmount();
                  boolean var18 = false;
                  if (param0 != null) {
                     if (var13.getId() == Item.BASE_ATTACK_DAMAGE_UUID) {
                        var14 += param0.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                        var14 += EnchantmentHelper.getDamageBonus(this, MobType.UNDEFINED);
                        var18 = true;
                     } else if (var13.getId() == Item.BASE_ATTACK_SPEED_UUID) {
                        var14 += param0.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                        var18 = true;
                     }
                  }

                  double var16;
                  if (var13.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE || var13.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                     var16 = var14 * 100.0;
                  } else if (var12.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                     var16 = var14 * 10.0;
                  } else {
                     var16 = var14;
                  }

                  if (var18) {
                     var3.add(
                        CommonComponents.space()
                           .append(
                              Component.translatable(
                                 "attribute.modifier.equals." + var13.getOperation().toValue(),
                                 ATTRIBUTE_MODIFIER_FORMAT.format(var16),
                                 Component.translatable(var12.getKey().getDescriptionId())
                              )
                           )
                           .withStyle(ChatFormatting.DARK_GREEN)
                     );
                  } else if (var14 > 0.0) {
                     var3.add(
                        Component.translatable(
                              "attribute.modifier.plus." + var13.getOperation().toValue(),
                              ATTRIBUTE_MODIFIER_FORMAT.format(var16),
                              Component.translatable(var12.getKey().getDescriptionId())
                           )
                           .withStyle(ChatFormatting.BLUE)
                     );
                  } else if (var14 < 0.0) {
                     var16 *= -1.0;
                     var3.add(
                        Component.translatable(
                              "attribute.modifier.take." + var13.getOperation().toValue(),
                              ATTRIBUTE_MODIFIER_FORMAT.format(var16),
                              Component.translatable(var12.getKey().getDescriptionId())
                           )
                           .withStyle(ChatFormatting.RED)
                     );
                  }
               }
            }
         }
      }

      if (this.hasTag()) {
         if (shouldShowInTooltip(var5, ItemStack.TooltipPart.UNBREAKABLE) && this.tag.getBoolean("Unbreakable")) {
            var3.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
         }

         if (shouldShowInTooltip(var5, ItemStack.TooltipPart.CAN_DESTROY) && this.tag.contains("CanDestroy", 9)) {
            ListTag var6 = this.tag.getList("CanDestroy", 8);
            if (!var6.isEmpty()) {
               var3.add(CommonComponents.EMPTY);
               var3.add(Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY));

               for (int var7 = 0; var7 < var6.size(); var7++) {
                  var3.addAll(expandBlockState(var6.getString(var7)));
               }
            }
         }

         if (shouldShowInTooltip(var5, ItemStack.TooltipPart.CAN_PLACE) && this.tag.contains("CanPlaceOn", 9)) {
            ListTag var6 = this.tag.getList("CanPlaceOn", 8);
            if (!var6.isEmpty()) {
               var3.add(CommonComponents.EMPTY);
               var3.add(Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY));

               for (int var7 = 0; var7 < var6.size(); var7++) {
                  var3.addAll(expandBlockState(var6.getString(var7)));
               }
            }
         }
      }

      if (param1.isAdvanced()) {
         if (this.isDamaged()) {
            var3.add(Component.translatable("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()));
         }

         var3.add(Component.literal(BuiltInRegistries.ITEM.getKey(this.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
         if (this.hasTag()) {
            var3.add(Component.translatable("item.nbt_tags", this.tag.getAllKeys().size()).withStyle(ChatFormatting.DARK_GRAY));
         }
      }

      if (param0 != null && !this.getItem().isEnabled(param0.getLevel().enabledFeatures())) {
         var3.add(DISABLED_ITEM_TOOLTIP);
      }

      return var3;
   }

   private static boolean shouldShowInTooltip(int param0, ItemStack.TooltipPart param1) {
      return (param0 & param1.getMask()) == 0;
   }

   private int getHideFlags() {
      return this.hasTag() && this.tag.contains("HideFlags", 99) ? this.tag.getInt("HideFlags") : 0;
   }

   public void hideTooltipPart(ItemStack.TooltipPart param0) {
      CompoundTag var2 = this.getOrCreateTag();
      var2.putInt("HideFlags", var2.getInt("HideFlags") | param0.getMask());
   }

   public static void appendEnchantmentNames(List<Component> param0, ListTag param1) {
      for (int var2 = 0; var2 < param1.size(); var2++) {
         CompoundTag var3 = param1.getCompound(var2);
         BuiltInRegistries.ENCHANTMENT
            .getOptional(EnchantmentHelper.getEnchantmentId(var3))
            .ifPresent(l_param2 -> param0.add(l_param2.getFullname(EnchantmentHelper.getEnchantmentLevel(var3))));
      }
   }

   private static Collection<Component> expandBlockState(String param0) {
      try {
         return BlockStateParser.parseForTesting(BuiltInRegistries.BLOCK.asLookup(), param0, true)
            .map(
               l_param0 -> Lists.newArrayList(l_param0.blockState().getBlock().getName().withStyle(ChatFormatting.DARK_GRAY)),
               l_param0 -> l_param0.tag()
                  .stream()
                  .map(l2_param0 -> l2_param0.value().getName().withStyle(ChatFormatting.DARK_GRAY))
                  .collect(Collectors.toList())
            );
      } catch (CommandSyntaxException var2) {
         return Lists.newArrayList(Component.literal("missingno").withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   public boolean hasFoil() {
      return this.getItem().isFoil(this);
   }

   public Rarity getRarity() {
      return this.getItem().getRarity(this);
   }

   public boolean isEnchantable() {
      return !this.getItem().isEnchantable(this) ? false : !this.isEnchanted();
   }

   public void enchant(Enchantment param0, int param1) {
      this.getOrCreateTag();
      if (!this.tag.contains("Enchantments", 9)) {
         this.tag.put("Enchantments", new ListTag());
      }

      ListTag var3 = this.tag.getList("Enchantments", 10);
      var3.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(param0), (byte)param1));
   }

   public boolean isEnchanted() {
      return this.tag != null && this.tag.contains("Enchantments", 9) ? !this.tag.getList("Enchantments", 10).isEmpty() : false;
   }

   public void addTagElement(String param0, Tag param1) {
      this.getOrCreateTag().put(param0, param1);
   }

   public boolean isFramed() {
      return this.entityRepresentation instanceof ItemFrame;
   }

   public void setEntityRepresentation(@Nullable Entity param0) {
      this.entityRepresentation = param0;
   }

   @Nullable
   public ItemFrame getFrame() {
      return this.entityRepresentation instanceof ItemFrame ? (ItemFrame)this.getEntityRepresentation() : null;
   }

   @Nullable
   public Entity getEntityRepresentation() {
      return !this.emptyCacheFlag ? this.entityRepresentation : null;
   }

   public int getBaseRepairCost() {
      return this.hasTag() && this.tag.contains("RepairCost", 3) ? this.tag.getInt("RepairCost") : 0;
   }

   public void setRepairCost(int param0) {
      this.getOrCreateTag().putInt("RepairCost", param0);
   }

   public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot param0) {
      Multimap<Attribute, AttributeModifier> var2;
      if (this.hasTag() && this.tag.contains("AttributeModifiers", 9)) {
         var2 = HashMultimap.create();
         ListTag var3 = this.tag.getList("AttributeModifiers", 10);

         for (int var4 = 0; var4 < var3.size(); var4++) {
            CompoundTag var5 = var3.getCompound(var4);
            if (!var5.contains("Slot", 8) || var5.getString("Slot").equals(param0.getName())) {
               Optional<Attribute> var6 = BuiltInRegistries.ATTRIBUTE.getOptional(ResourceLocation.tryParse(var5.getString("AttributeName")));
               if (var6.isPresent()) {
                  AttributeModifier var7 = AttributeModifier.load(var5);
                  if (var7 != null && var7.getId().getLeastSignificantBits() != 0L && var7.getId().getMostSignificantBits() != 0L) {
                     var2.put(var6.get(), var7);
                  }
               }
            }
         }
      } else {
         var2 = this.getItem().getDefaultAttributeModifiers(param0);
      }

      return var2;
   }

   public void addAttributeModifier(Attribute param0, AttributeModifier param1, @Nullable EquipmentSlot param2) {
      this.getOrCreateTag();
      if (!this.tag.contains("AttributeModifiers", 9)) {
         this.tag.put("AttributeModifiers", new ListTag());
      }

      ListTag var4 = this.tag.getList("AttributeModifiers", 10);
      CompoundTag var5 = param1.save();
      var5.putString("AttributeName", BuiltInRegistries.ATTRIBUTE.getKey(param0).toString());
      if (param2 != null) {
         var5.putString("Slot", param2.getName());
      }

      var4.add(var5);
   }

   public Component getDisplayName() {
      MutableComponent var1 = Component.empty().append(this.getHoverName());
      if (this.hasCustomHoverName()) {
         var1.withStyle(ChatFormatting.ITALIC);
      }

      MutableComponent var2 = ComponentUtils.wrapInSquareBrackets(var1);
      if (!this.emptyCacheFlag) {
         var2.withStyle(this.getRarity().color)
            .withStyle(l_param0 -> l_param0.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(this))));
      }

      return var2;
   }

   public boolean hasAdventureModePlaceTagForBlock(Registry<Block> param0, BlockInWorld param1) {
      if (this.adventurePlaceCheck == null) {
         this.adventurePlaceCheck = new AdventureModeCheck("CanPlaceOn");
      }

      return this.adventurePlaceCheck.test(this, param0, param1);
   }

   public boolean hasAdventureModeBreakTagForBlock(Registry<Block> param0, BlockInWorld param1) {
      if (this.adventureBreakCheck == null) {
         this.adventureBreakCheck = new AdventureModeCheck("CanDestroy");
      }

      return this.adventureBreakCheck.test(this, param0, param1);
   }

   public int getPopTime() {
      return this.popTime;
   }

   public void setPopTime(int param0) {
      this.popTime = param0;
   }

   public int getCount() {
      return this.emptyCacheFlag ? 0 : this.count;
   }

   public void setCount(int param0) {
      this.count = param0;
      this.updateEmptyCacheFlag();
   }

   public void grow(int param0) {
      this.setCount(this.count + param0);
   }

   public void shrink(int param0) {
      this.grow(-param0);
   }

   public void onUseTick(Level param0, LivingEntity param1, int param2) {
      this.getItem().onUseTick(param0, param1, this, param2);
   }

   public void onDestroyed(ItemEntity param0) {
      this.getItem().onDestroyed(param0);
   }

   public boolean isEdible() {
      return this.getItem().isEdible();
   }

   public SoundEvent getDrinkingSound() {
      return this.getItem().getDrinkingSound();
   }

   public SoundEvent getEatingSound() {
      return this.getItem().getEatingSound();
   }

   public enum TooltipPart {
      ENCHANTMENTS,
      MODIFIERS,
      UNBREAKABLE,
      CAN_DESTROY,
      CAN_PLACE,
      ADDITIONAL,
      DYE,
      UPGRADES;

      private final int mask = 1 << this.ordinal();

      public int getMask() {
         return this.mask;
      }
   }
}
