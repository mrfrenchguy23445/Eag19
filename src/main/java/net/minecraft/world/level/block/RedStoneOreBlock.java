package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RedStoneOreBlock extends Block {
   public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

   public RedStoneOreBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
   }

   @Override
   public void attack(BlockState param0, Level param1, BlockPos param2, Player param3) {
      interact(param0, param1, param2);
      super.attack(param0, param1, param2, param3);
   }

   @Override
   public void stepOn(Level param0, BlockPos param1, BlockState param2, Entity param3) {
      if (!param3.isSteppingCarefully()) {
         interact(param2, param0, param1);
      }

      super.stepOn(param0, param1, param2, param3);
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param1.isClientSide) {
         spawnParticles(param1, param2);
      } else {
         interact(param0, param1, param2);
      }

      ItemStack var7 = param3.getItemInHand(param4);
      return var7.getItem() instanceof BlockItem && new BlockPlaceContext(param3, param4, var7, param5).canPlace()
         ? InteractionResult.PASS
         : InteractionResult.SUCCESS;
   }

   private static void interact(BlockState param0, Level param1, BlockPos param2) {
      spawnParticles(param1, param2);
      if (!param0.getValue(LIT)) {
         param1.setBlock(param2, param0.setValue(LIT, true), 3);
      }
   }

   @Override
   public boolean isRandomlyTicking(BlockState param0) {
      return param0.getValue(LIT);
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(LIT)) {
         param1.setBlock(param2, param0.setValue(LIT, false), 3);
      }
   }

   @Override
   public void spawnAfterBreak(BlockState param0, ServerLevel param1, BlockPos param2, ItemStack param3, boolean param4) {
      super.spawnAfterBreak(param0, param1, param2, param3, param4);
      if (param4 && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, param3) == 0) {
         int var6 = 1 + param1.random.nextInt(5);
         this.popExperience(param1, param2, var6);
      }
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(LIT)) {
         spawnParticles(param1, param2);
      }
   }

   private static void spawnParticles(Level param0, BlockPos param1) {
      double var2 = 0.5625;
      RandomSource var4 = param0.random;

      for (Direction var8 : Direction.values()) {
         BlockPos var9 = param1.relative(var8);
         if (!param0.getBlockState(var9).isSolidRender(param0, var9)) {
            Direction.Axis var10 = var8.getAxis();
            double var11 = var10 == Direction.Axis.X ? 0.5 + 0.5625 * var8.getStepX() : var4.nextFloat();
            double var13 = var10 == Direction.Axis.Y ? 0.5 + 0.5625 * var8.getStepY() : var4.nextFloat();
            double var15 = var10 == Direction.Axis.Z ? 0.5 + 0.5625 * var8.getStepZ() : var4.nextFloat();
            param0.addParticle(DustParticleOptions.REDSTONE, param1.getX() + var11, param1.getY() + var13, param1.getZ() + var15, 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(LIT);
   }
}
