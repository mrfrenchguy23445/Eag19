package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;

public class FireworkStarItem extends Item {
   public FireworkStarItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      CompoundTag var5 = param0.getTagElement("Explosion");
      if (var5 != null) {
         appendHoverText(var5, param2);
      }
   }

   public static void appendHoverText(CompoundTag param0, List<Component> param1) {
      FireworkRocketItem.Shape var2 = FireworkRocketItem.Shape.byId(param0.getByte("Type"));
      param1.add(Component.translatable("item.minecraft.firework_star.shape." + var2.getName()).withStyle(ChatFormatting.GRAY));
      int[] var3 = param0.getIntArray("Colors");
      if (var3.length > 0) {
         param1.add(appendColors(Component.empty().withStyle(ChatFormatting.GRAY), var3));
      }

      int[] var4 = param0.getIntArray("FadeColors");
      if (var4.length > 0) {
         param1.add(
            appendColors(Component.translatable("item.minecraft.firework_star.fade_to").append(CommonComponents.SPACE).withStyle(ChatFormatting.GRAY), var4)
         );
      }

      if (param0.getBoolean("Trail")) {
         param1.add(Component.translatable("item.minecraft.firework_star.trail").withStyle(ChatFormatting.GRAY));
      }

      if (param0.getBoolean("Flicker")) {
         param1.add(Component.translatable("item.minecraft.firework_star.flicker").withStyle(ChatFormatting.GRAY));
      }
   }

   private static Component appendColors(MutableComponent param0, int[] param1) {
      for (int var2 = 0; var2 < param1.length; var2++) {
         if (var2 > 0) {
            param0.append(", ");
         }

         param0.append(getColorName(param1[var2]));
      }

      return param0;
   }

   private static Component getColorName(int param0) {
      DyeColor var1 = DyeColor.byFireworkColor(param0);
      return var1 == null
         ? Component.translatable("item.minecraft.firework_star.custom_color")
         : Component.translatable("item.minecraft.firework_star." + var1.getName());
   }
}
