package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class SwordItem extends TieredItem implements Vanishable {
   private final float attackDamage;
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;

   public SwordItem(Tier param0, int param1, float param2, Item.Properties param3) {
      super(param0, param3);
      this.attackDamage = param1 + param0.getAttackDamageBonus();
      Builder<Attribute, AttributeModifier> var5 = ImmutableMultimap.builder();
      var5.put(
         Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", this.attackDamage, AttributeModifier.Operation.ADDITION)
      );
      var5.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", param2, AttributeModifier.Operation.ADDITION));
      this.defaultModifiers = var5.build();
   }

   public float getDamage() {
      return this.attackDamage;
   }

   @Override
   public boolean canAttackBlock(BlockState param0, Level param1, BlockPos param2, Player param3) {
      return !param3.isCreative();
   }

   @Override
   public float getDestroySpeed(ItemStack param0, BlockState param1) {
      if (param1.is(Blocks.COBWEB)) {
         return 15.0F;
      }

      Material var3 = param1.getMaterial();
      return var3 != Material.PLANT && var3 != Material.REPLACEABLE_PLANT && !param1.is(BlockTags.LEAVES) && var3 != Material.VEGETABLE ? 1.0F : 1.5F;
   }

   @Override
   public boolean hurtEnemy(ItemStack param0, LivingEntity param1, LivingEntity param2) {
      param0.hurtAndBreak(1, param2, l_param0 -> l_param0.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      return true;
   }

   @Override
   public boolean mineBlock(ItemStack param0, Level param1, BlockState param2, BlockPos param3, LivingEntity param4) {
      if (param2.getDestroySpeed(param1, param3) != 0.0F) {
         param0.hurtAndBreak(2, param4, l_param0 -> l_param0.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      }

      return true;
   }

   @Override
   public boolean isCorrectToolForDrops(BlockState param0) {
      return param0.is(Blocks.COBWEB);
   }

   @Override
   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot param0) {
      return param0 == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(param0);
   }
}
