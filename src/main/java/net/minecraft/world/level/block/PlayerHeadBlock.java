package net.minecraft.world.level.block;

import com.mojang.authlib.GameProfile;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;

public class PlayerHeadBlock extends SkullBlock {
   protected PlayerHeadBlock(BlockBehaviour.Properties param0) {
      super(SkullBlock.Types.PLAYER, param0);
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, @Nullable LivingEntity param3, ItemStack param4) {
      super.setPlacedBy(param0, param1, param2, param3, param4);
      if (param0.getBlockEntity(param1) instanceof SkullBlockEntity var7) {
         GameProfile var8 = null;
         if (param4.hasTag()) {
            CompoundTag var9 = param4.getTag();
            if (var9.contains("SkullOwner", 10)) {
               var8 = NbtUtils.readGameProfile(var9.getCompound("SkullOwner"));
            } else if (var9.contains("SkullOwner", 8) && !StringUtils.isBlank(var9.getString("SkullOwner"))) {
               var8 = new GameProfile(null, var9.getString("SkullOwner"));
            }
         }

         var7.setOwner(var8);
      }
   }
}
