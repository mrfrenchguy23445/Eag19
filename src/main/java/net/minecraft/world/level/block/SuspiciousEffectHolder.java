package net.minecraft.world.level.block;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ItemLike;

public interface SuspiciousEffectHolder {
   MobEffect getSuspiciousEffect();

   int getEffectDuration();

   static List<SuspiciousEffectHolder> getAllEffectHolders() {
      return BuiltInRegistries.ITEM.stream().map(SuspiciousEffectHolder::tryGet).filter(Objects::nonNull).collect(Collectors.toList());
   }

   @Nullable
   static SuspiciousEffectHolder tryGet(ItemLike param0) {
      if (param0.asItem() instanceof BlockItem var1 && var1.getBlock() instanceof SuspiciousEffectHolder var2) {
         return var2;
      } else {
         return param0.asItem() instanceof SuspiciousEffectHolder var1 ? var1 : null;
      }
   }
}
