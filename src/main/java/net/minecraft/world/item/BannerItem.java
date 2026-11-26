package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.apache.commons.lang3.Validate;

public class BannerItem extends StandingAndWallBlockItem {
   private static final String PATTERN_PREFIX = "block.minecraft.banner.";

   public BannerItem(Block param0, Block param1, Item.Properties param2) {
      super(param0, param1, param2, Direction.DOWN);
      Validate.isInstanceOf(AbstractBannerBlock.class, param0);
      Validate.isInstanceOf(AbstractBannerBlock.class, param1);
   }

   public static void appendHoverTextFromBannerBlockEntityTag(ItemStack param0, List<Component> param1) {
      CompoundTag var2 = BlockItem.getBlockEntityData(param0);
      if (var2 != null && var2.contains("Patterns")) {
         ListTag var3 = var2.getList("Patterns", 10);

         for (int var4 = 0; var4 < var3.size() && var4 < 6; var4++) {
            CompoundTag var5 = var3.getCompound(var4);
            DyeColor var6 = DyeColor.byId(var5.getInt("Color"));
            Holder<BannerPattern> var7 = BannerPattern.byHash(var5.getString("Pattern"));
            if (var7 != null) {
               var7.unwrapKey()
                  .map(l_param0 -> l_param0.location().toShortLanguageKey())
                  .ifPresent(
                     l_param2 -> param1.add(Component.translatable("block.minecraft.banner." + l_param2 + "." + var6.getName()).withStyle(ChatFormatting.GRAY))
                  );
            }
         }
      }
   }

   public DyeColor getColor() {
      return ((AbstractBannerBlock)this.getBlock()).getColor();
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      appendHoverTextFromBannerBlockEntityTag(param0, param2);
   }
}
