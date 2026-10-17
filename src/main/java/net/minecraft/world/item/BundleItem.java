package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.Level;

public class BundleItem extends Item {
   private static final String TAG_ITEMS = "Items";
   public static final int MAX_WEIGHT = 64;
   private static final int BUNDLE_IN_BUNDLE_WEIGHT = 4;
   private static final int BAR_COLOR = Mth.color(0.4F, 0.4F, 1.0F);

   public BundleItem(Item.Properties param0) {
      super(param0);
   }

   public static float getFullnessDisplay(ItemStack param0) {
      return getContentWeight(param0) / 64.0F;
   }

   @Override
   public boolean overrideStackedOnOther(ItemStack param0, Slot param1, ClickAction param2, Player param3) {
      if (param2 != ClickAction.SECONDARY) {
         return false;
      }

      ItemStack var5 = param1.getItem();
      if (var5.isEmpty()) {
         this.playRemoveOneSound(param3);
         removeOne(param0).ifPresent(l_param2 -> add(param0, param1.safeInsert(l_param2)));
      } else if (var5.getItem().canFitInsideContainerItems()) {
         int var6 = (64 - getContentWeight(param0)) / getWeight(var5);
         int var7 = add(param0, param1.safeTake(var5.getCount(), var6, param3));
         if (var7 > 0) {
            this.playInsertSound(param3);
         }
      }

      return true;
   }

   @Override
   public boolean overrideOtherStackedOnMe(ItemStack param0, ItemStack param1, Slot param2, ClickAction param3, Player param4, SlotAccess param5) {
      if (param3 == ClickAction.SECONDARY && param2.allowModification(param4)) {
         if (param1.isEmpty()) {
            removeOne(param0).ifPresent(l_param2 -> {
               this.playRemoveOneSound(param4);
               param5.set(l_param2);
            });
         } else {
            int var7 = add(param0, param1);
            if (var7 > 0) {
               this.playInsertSound(param4);
               param1.shrink(var7);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      if (dropContents(var4, param1)) {
         this.playDropContentsSound(param1);
         param1.awardStat(Stats.ITEM_USED.get(this));
         return InteractionResultHolder.sidedSuccess(var4, param0.isClientSide());
      } else {
         return InteractionResultHolder.fail(var4);
      }
   }

   @Override
   public boolean isBarVisible(ItemStack param0) {
      return getContentWeight(param0) > 0;
   }

   @Override
   public int getBarWidth(ItemStack param0) {
      return Math.min(1 + 12 * getContentWeight(param0) / 64, 13);
   }

   @Override
   public int getBarColor(ItemStack param0) {
      return BAR_COLOR;
   }

   private static int add(ItemStack param0, ItemStack param1) {
      if (!param1.isEmpty() && param1.getItem().canFitInsideContainerItems()) {
         CompoundTag var2 = param0.getOrCreateTag();
         if (!var2.contains("Items")) {
            var2.put("Items", new ListTag());
         }

         int var3 = getContentWeight(param0);
         int var4 = getWeight(param1);
         int var5 = Math.min(param1.getCount(), (64 - var3) / var4);
         if (var5 == 0) {
            return 0;
         }

         ListTag var6 = var2.getList("Items", 10);
         Optional<CompoundTag> var7 = getMatchingItem(param1, var6);
         if (var7.isPresent()) {
            CompoundTag var8 = var7.get();
            ItemStack var9 = ItemStack.of(var8);
            var9.grow(var5);
            var9.save(var8);
            var6.remove(var8);
            var6.add(0, var8);
         } else {
            ItemStack var8 = param1.copy();
            var8.setCount(var5);
            CompoundTag var9 = new CompoundTag();
            var8.save(var9);
            var6.add(0, var9);
         }

         return var5;
      } else {
         return 0;
      }
   }

   private static Optional<CompoundTag> getMatchingItem(ItemStack param0, ListTag param1) {
      return param0.is(Items.BUNDLE)
         ? Optional.empty()
         : param1.stream()
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .filter(l_param1 -> ItemStack.isSameItemSameTags(ItemStack.of(l_param1), param0))
            .findFirst();
   }

   private static int getWeight(ItemStack param0) {
      if (param0.is(Items.BUNDLE)) {
         return 4 + getContentWeight(param0);
      }

      if ((param0.is(Items.BEEHIVE) || param0.is(Items.BEE_NEST)) && param0.hasTag()) {
         CompoundTag var1 = BlockItem.getBlockEntityData(param0);
         if (var1 != null && !var1.getList("Bees", 10).isEmpty()) {
            return 64;
         }
      }

      return 64 / param0.getMaxStackSize();
   }

   private static int getContentWeight(ItemStack param0) {
      return getContents(param0).mapToInt(l_param0 -> getWeight(l_param0) * l_param0.getCount()).sum();
   }

   private static Optional<ItemStack> removeOne(ItemStack param0) {
      CompoundTag var1 = param0.getOrCreateTag();
      if (!var1.contains("Items")) {
         return Optional.empty();
      }

      ListTag var2 = var1.getList("Items", 10);
      if (var2.isEmpty()) {
         return Optional.empty();
      }

      int var3 = 0;
      CompoundTag var4 = var2.getCompound(0);
      ItemStack var5 = ItemStack.of(var4);
      var2.remove(0);
      if (var2.isEmpty()) {
         param0.removeTagKey("Items");
      }

      return Optional.of(var5);
   }

   private static boolean dropContents(ItemStack param0, Player param1) {
      CompoundTag var2 = param0.getOrCreateTag();
      if (!var2.contains("Items")) {
         return false;
      }

      if (param1 instanceof ServerPlayer) {
         ListTag var3 = var2.getList("Items", 10);

         for (int var4 = 0; var4 < var3.size(); var4++) {
            CompoundTag var5 = var3.getCompound(var4);
            ItemStack var6 = ItemStack.of(var5);
            param1.drop(var6, true);
         }
      }

      param0.removeTagKey("Items");
      return true;
   }

   private static Stream<ItemStack> getContents(ItemStack param0) {
      CompoundTag var1 = param0.getTag();
      if (var1 == null) {
         return Stream.empty();
      }

      ListTag var2 = var1.getList("Items", 10);
      return var2.stream().map(CompoundTag.class::cast).map(ItemStack::of);
   }

   @Override
   public Optional<TooltipComponent> getTooltipImage(ItemStack param0) {
      NonNullList<ItemStack> var2 = NonNullList.create();
      getContents(param0).forEach(var2::add);
      return Optional.of(new BundleTooltip(var2, getContentWeight(param0)));
   }

   @Override
   public void appendHoverText(ItemStack param0, Level param1, List<Component> param2, TooltipFlag param3) {
      param2.add(Component.translatable("item.minecraft.bundle.fullness", getContentWeight(param0), 64).withStyle(ChatFormatting.GRAY));
   }

   @Override
   public void onDestroyed(ItemEntity param0) {
      ItemUtils.onContainerDestroyed(param0, getContents(param0.getItem()));
   }

   private void playRemoveOneSound(Entity param0) {
      param0.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + param0.getLevel().getRandom().nextFloat() * 0.4F);
   }

   private void playInsertSound(Entity param0) {
      param0.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + param0.getLevel().getRandom().nextFloat() * 0.4F);
   }

   private void playDropContentsSound(Entity param0) {
      param0.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + param0.getLevel().getRandom().nextFloat() * 0.4F);
   }
}
