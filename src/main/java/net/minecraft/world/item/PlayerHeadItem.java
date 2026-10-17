package net.minecraft.world.item;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.apache.commons.lang3.StringUtils;

public class PlayerHeadItem extends StandingAndWallBlockItem {
   public static final String TAG_SKULL_OWNER = "SkullOwner";

   public PlayerHeadItem(Block param0, Block param1, Item.Properties param2) {
      super(param0, param1, param2, Direction.DOWN);
   }

   @Override
   public Component getName(ItemStack param0) {
      if (param0.is(Items.PLAYER_HEAD) && param0.hasTag()) {
         String var2 = null;
         CompoundTag var3 = param0.getTag();
         if (var3.contains("SkullOwner", 8)) {
            var2 = var3.getString("SkullOwner");
         } else if (var3.contains("SkullOwner", 10)) {
            CompoundTag var4 = var3.getCompound("SkullOwner");
            if (var4.contains("Name", 8)) {
               var2 = var4.getString("Name");
            }
         }

         if (var2 != null) {
            return Component.translatable(this.getDescriptionId() + ".named", var2);
         }
      }

      return super.getName(param0);
   }

   @Override
   public void verifyTagAfterLoad(CompoundTag param0) {
      super.verifyTagAfterLoad(param0);
      if (param0.contains("SkullOwner", 8) && !StringUtils.isBlank(param0.getString("SkullOwner"))) {
         GameProfile var2 = new GameProfile(null, param0.getString("SkullOwner"));
         SkullBlockEntity.updateGameprofile(var2, l_param1 -> param0.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), l_param1)));
      }
   }
}
