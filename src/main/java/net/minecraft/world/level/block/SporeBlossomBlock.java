package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SporeBlossomBlock extends Block {
   private static final VoxelShape SHAPE = Block.box(2.0, 13.0, 2.0, 14.0, 16.0, 14.0);
   private static final int ADD_PARTICLE_ATTEMPTS = 14;
   private static final int PARTICLE_XZ_RADIUS = 10;
   private static final int PARTICLE_Y_MAX = 10;

   public SporeBlossomBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      return Block.canSupportCenter(param1, param2.above(), Direction.DOWN) && !param1.isWaterAt(param2);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      return param1 == Direction.UP && !this.canSurvive(param0, param3, param4)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      int var5 = param2.getX();
      int var6 = param2.getY();
      int var7 = param2.getZ();
      double var8 = var5 + param3.nextDouble();
      double var10 = var6 + 0.7;
      double var12 = var7 + param3.nextDouble();
      param1.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, var8, var10, var12, 0.0, 0.0, 0.0);
      BlockPos.MutableBlockPos var14 = new BlockPos.MutableBlockPos();

      for (int var15 = 0; var15 < 14; var15++) {
         var14.set(var5 + Mth.nextInt(param3, -10, 10), var6 - param3.nextInt(10), var7 + Mth.nextInt(param3, -10, 10));
         BlockState var16 = param1.getBlockState(var14);
         if (!var16.isCollisionShapeFullBlock(param1, var14)) {
            param1.addParticle(
               ParticleTypes.SPORE_BLOSSOM_AIR,
               var14.getX() + param3.nextDouble(),
               var14.getY() + param3.nextDouble(),
               var14.getZ() + param3.nextDouble(),
               0.0,
               0.0,
               0.0
            );
         }
      }
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }
}
