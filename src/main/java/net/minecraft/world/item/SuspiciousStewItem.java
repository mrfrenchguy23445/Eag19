package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;

public class SuspiciousStewItem extends Item {
   public static final String EFFECTS_TAG = "Effects";
   public static final String EFFECT_ID_TAG = "EffectId";
   public static final String EFFECT_DURATION_TAG = "EffectDuration";
   public static final int DEFAULT_DURATION = 160;

   public SuspiciousStewItem(Item.Properties param0) {
      super(param0);
   }

   public static void saveMobEffect(ItemStack param0, MobEffect param1, int param2) {
      CompoundTag var3 = param0.getOrCreateTag();
      ListTag var4 = var3.getList("Effects", 9);
      CompoundTag var5 = new CompoundTag();
      var5.putInt("EffectId", MobEffect.getId(param1));
      var5.putInt("EffectDuration", param2);
      var4.add(var5);
      var3.put("Effects", var4);
   }

   private static void listPotionEffects(ItemStack param0, Consumer<MobEffectInstance> param1) {
      CompoundTag var2 = param0.getTag();
      if (var2 != null && var2.contains("Effects", 9)) {
         ListTag var3 = var2.getList("Effects", 10);

         for (int var4 = 0; var4 < var3.size(); var4++) {
            CompoundTag var5 = var3.getCompound(var4);
            int var6;
            if (var5.contains("EffectDuration", 3)) {
               var6 = var5.getInt("EffectDuration");
            } else {
               var6 = 160;
            }

            MobEffect var7 = MobEffect.byId(var5.getInt("EffectId"));
            if (var7 != null) {
               param1.accept(new MobEffectInstance(var7, var6));
            }
         }
      }
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      super.appendHoverText(param0, param1, param2, param3);
      if (param3.isCreative()) {
         List<MobEffectInstance> var5 = new ArrayList<>();
         listPotionEffects(param0, var5::add);
         PotionUtils.addPotionTooltip(var5, param2, 1.0F);
      }
   }

   @Override
   public ItemStack finishUsingItem(ItemStack param0, Level param1, LivingEntity param2) {
      ItemStack var4 = super.finishUsingItem(param0, param1, param2);
      listPotionEffects(var4, param2::addEffect);
      return param2 instanceof Player && ((Player)param2).getAbilities().instabuild ? var4 : new ItemStack(Items.BOWL);
   }
}
