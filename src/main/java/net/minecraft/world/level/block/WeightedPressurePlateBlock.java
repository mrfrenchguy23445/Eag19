package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class WeightedPressurePlateBlock extends BasePressurePlateBlock {
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   private final int maxWeight;

   protected WeightedPressurePlateBlock(int param0, BlockBehaviour.Properties param1, BlockSetType param2) {
      super(param1, param2);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
      this.maxWeight = param0;
   }

   @Override
   protected int getSignalStrength(Level param0, BlockPos param1) {
      int var3 = Math.min(param0.getEntitiesOfClass(Entity.class, TOUCH_AABB.move(param1)).size(), this.maxWeight);
      if (var3 > 0) {
         float var4 = (float)Math.min(this.maxWeight, var3) / this.maxWeight;
         return Mth.ceil(var4 * 15.0F);
      } else {
         return 0;
      }
   }

   @Override
   protected int getSignalForState(BlockState param0) {
      return param0.getValue(POWER);
   }

   @Override
   protected BlockState setSignalForState(BlockState param0, int param1) {
      return param0.setValue(POWER, param1);
   }

   @Override
   protected int getPressedTime() {
      return 10;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(POWER);
   }
}
