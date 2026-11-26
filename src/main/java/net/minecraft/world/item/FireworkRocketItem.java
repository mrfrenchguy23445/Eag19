package net.minecraft.world.item;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FireworkRocketItem extends Item {
   public static final byte[] CRAFTABLE_DURATIONS = new byte[]{1, 2, 3};
   public static final String TAG_FIREWORKS = "Fireworks";
   public static final String TAG_EXPLOSION = "Explosion";
   public static final String TAG_EXPLOSIONS = "Explosions";
   public static final String TAG_FLIGHT = "Flight";
   public static final String TAG_EXPLOSION_TYPE = "Type";
   public static final String TAG_EXPLOSION_TRAIL = "Trail";
   public static final String TAG_EXPLOSION_FLICKER = "Flicker";
   public static final String TAG_EXPLOSION_COLORS = "Colors";
   public static final String TAG_EXPLOSION_FADECOLORS = "FadeColors";
   public static final double ROCKET_PLACEMENT_OFFSET = 0.15;

   public FireworkRocketItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      if (!var2.isClientSide) {
         ItemStack var3 = param0.getItemInHand();
         Vec3 var4 = param0.getClickLocation();
         Direction var5 = param0.getClickedFace();
         FireworkRocketEntity var6 = new FireworkRocketEntity(
            var2, param0.getPlayer(), var4.x + var5.getStepX() * 0.15, var4.y + var5.getStepY() * 0.15, var4.z + var5.getStepZ() * 0.15, var3
         );
         var2.addFreshEntity(var6);
         var3.shrink(1);
      }

      return InteractionResult.sidedSuccess(var2.isClientSide);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      if (param1.isFallFlying()) {
         ItemStack var4 = param1.getItemInHand(param2);
         if (!param0.isClientSide) {
            FireworkRocketEntity var5 = new FireworkRocketEntity(param0, var4, param1);
            param0.addFreshEntity(var5);
            if (!param1.getAbilities().instabuild) {
               var4.shrink(1);
            }

            param1.awardStat(Stats.ITEM_USED.get(this));
         }

         return InteractionResultHolder.sidedSuccess(param1.getItemInHand(param2), param0.isClientSide());
      } else {
         return InteractionResultHolder.pass(param1.getItemInHand(param2));
      }
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      CompoundTag var5 = param0.getTagElement("Fireworks");
      if (var5 != null) {
         if (var5.contains("Flight", 99)) {
            param2.add(
               Component.translatable("item.minecraft.firework_rocket.flight")
                  .append(CommonComponents.SPACE)
                  .append(String.valueOf(var5.getByte("Flight")))
                  .withStyle(ChatFormatting.GRAY)
            );
         }

         ListTag var6 = var5.getList("Explosions", 10);
         if (!var6.isEmpty()) {
            for (int var7 = 0; var7 < var6.size(); var7++) {
               CompoundTag var8 = var6.getCompound(var7);
               List<Component> var9 = Lists.newArrayList();
               FireworkStarItem.appendHoverText(var8, var9);
               if (!var9.isEmpty()) {
                  for (int var10 = 1; var10 < var9.size(); var10++) {
                     var9.set(var10, Component.literal("  ").append(var9.get(var10)).withStyle(ChatFormatting.GRAY));
                  }

                  param2.addAll(var9);
               }
            }
         }
      }
   }

   public static void setDuration(ItemStack param0, byte param1) {
      param0.getOrCreateTagElement("Fireworks").putByte("Flight", param1);
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack var1 = new ItemStack(this);
      setDuration(var1, (byte)1);
      return var1;
   }

   public enum Shape {
      SMALL_BALL(0, "small_ball"),
      LARGE_BALL(1, "large_ball"),
      STAR(2, "star"),
      CREEPER(3, "creeper"),
      BURST(4, "burst");

      private static final IntFunction<FireworkRocketItem.Shape> BY_ID = ByIdMap.continuous(
         FireworkRocketItem.Shape::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      private final int id;
      private final String name;

      Shape(int param2, String param3) {
         this.id = param2;
         this.name = param3;
      }

      public int getId() {
         return this.id;
      }

      public String getName() {
         return this.name;
      }

      public static FireworkRocketItem.Shape byId(int param0) {
         return BY_ID.apply(param0);
      }
   }
}
