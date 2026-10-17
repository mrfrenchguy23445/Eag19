package net.minecraft.world.level.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

public class PressurePlateBlock extends BasePressurePlateBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private final PressurePlateBlock.Sensitivity sensitivity;

   protected PressurePlateBlock(PressurePlateBlock.Sensitivity param0, BlockBehaviour.Properties param1, BlockSetType param2) {
      super(param1, param2);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
      this.sensitivity = param0;
   }

   @Override
   protected int getSignalForState(BlockState param0) {
      return param0.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected BlockState setSignalForState(BlockState param0, int param1) {
      return param0.setValue(POWERED, param1 > 0);
   }

   @Override
   protected int getSignalStrength(Level param0, BlockPos param1) {
      AABB var3 = TOUCH_AABB.move(param1);
      List<? extends Entity> var4;
      switch (this.sensitivity) {
         case EVERYTHING:
            var4 = param0.getEntities(null, var3);
            break;
         case MOBS:
            var4 = param0.getEntitiesOfClass(LivingEntity.class, var3);
            break;
         default:
            return 0;
      }

      if (!var4.isEmpty()) {
         for (Entity var6 : var4) {
            if (!var6.isIgnoringBlockTriggers()) {
               return 15;
            }
         }
      }

      return 0;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(POWERED);
   }

   public enum Sensitivity {
      EVERYTHING,
      MOBS;
   }
}
