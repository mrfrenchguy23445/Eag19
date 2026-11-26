package net.minecraft.world.item.enchantment;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;

public class FrostWalkerEnchantment extends Enchantment {
   public FrostWalkerEnchantment(Enchantment.Rarity param0, EquipmentSlot... param1) {
      super(param0, EnchantmentCategory.ARMOR_FEET, param1);
   }

   @Override
   public int getMinCost(int param0) {
      return param0 * 10;
   }

   @Override
   public int getMaxCost(int param0) {
      return this.getMinCost(param0) + 15;
   }

   @Override
   public boolean isTreasureOnly() {
      return true;
   }

   @Override
   public int getMaxLevel() {
      return 2;
   }

   public static void onEntityMoved(LivingEntity param0, Level param1, BlockPos param2, int param3) {
      if (param0.isOnGround()) {
         BlockState var4 = Blocks.FROSTED_ICE.defaultBlockState();
         int var5 = Math.min(16, 2 + param3);
         BlockPos.MutableBlockPos var6 = new BlockPos.MutableBlockPos();

         for (BlockPos var8 : BlockPos.betweenClosed(param2.offset(-var5, -1, -var5), param2.offset(var5, -1, var5))) {
            if (var8.closerToCenterThan(param0.position(), var5)) {
               var6.set(var8.getX(), var8.getY() + 1, var8.getZ());
               BlockState var9 = param1.getBlockState(var6);
               if (var9.isAir()) {
                  BlockState var10 = param1.getBlockState(var8);
                  if (var10.getMaterial() == Material.WATER
                     && var10.getValue(LiquidBlock.LEVEL) == 0
                     && var4.canSurvive(param1, var8)
                     && param1.isUnobstructed(var4, var8, CollisionContext.empty())) {
                     param1.setBlockAndUpdate(var8, var4);
                     param1.scheduleTick(var8, Blocks.FROSTED_ICE, Mth.nextInt(param0.getRandom(), 60, 120));
                  }
               }
            }
         }
      }
   }

   @Override
   public boolean checkCompatibility(Enchantment param0) {
      return super.checkCompatibility(param0) && param0 != Enchantments.DEPTH_STRIDER;
   }
}
