package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BoneMealItem extends Item {
   public static final int GRASS_SPREAD_WIDTH = 3;
   public static final int GRASS_SPREAD_HEIGHT = 1;
   public static final int GRASS_COUNT_MULTIPLIER = 3;

   public BoneMealItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockPos var4 = var3.relative(param0.getClickedFace());
      if (growCrop(param0.getItemInHand(), var2, var3)) {
         if (!var2.isClientSide) {
            var2.levelEvent(1505, var3, 0);
         }

         return InteractionResult.sidedSuccess(var2.isClientSide);
      } else {
         BlockState var5 = var2.getBlockState(var3);
         boolean var6 = var5.isFaceSturdy(var2, var3, param0.getClickedFace());
         if (var6 && growWaterPlant(param0.getItemInHand(), var2, var4, param0.getClickedFace())) {
            if (!var2.isClientSide) {
               var2.levelEvent(1505, var4, 0);
            }

            return InteractionResult.sidedSuccess(var2.isClientSide);
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public static boolean growCrop(ItemStack param0, Level param1, BlockPos param2) {
      BlockState var3 = param1.getBlockState(param2);
      if (var3.getBlock() instanceof BonemealableBlock) {
         BonemealableBlock var4 = (BonemealableBlock)var3.getBlock();
         if (var4.isValidBonemealTarget(param1, param2, var3, param1.isClientSide)) {
            if (param1 instanceof ServerLevel) {
               if (var4.isBonemealSuccess(param1, param1.random, param2, var3)) {
                  var4.performBonemeal((ServerLevel)param1, param1.random, param2, var3);
               }

               param0.shrink(1);
            }

            return true;
         }
      }

      return false;
   }

   public static boolean growWaterPlant(ItemStack param0, Level param1, BlockPos param2, @Nullable Direction param3) {
      if (param1.getBlockState(param2).is(Blocks.WATER) && param1.getFluidState(param2).getAmount() == 8) {
         if (!(param1 instanceof ServerLevel)) {
            return true;
         }

         RandomSource var4 = param1.getRandom();

         label78:
         for (int var5 = 0; var5 < 128; var5++) {
            BlockPos var6 = param2;
            BlockState var7 = Blocks.SEAGRASS.defaultBlockState();

            for (int var8 = 0; var8 < var5 / 16; var8++) {
               var6 = var6.offset(var4.nextInt(3) - 1, (var4.nextInt(3) - 1) * var4.nextInt(3) / 2, var4.nextInt(3) - 1);
               if (param1.getBlockState(var6).isCollisionShapeFullBlock(param1, var6)) {
                  continue label78;
               }
            }

            Holder<Biome> var8 = param1.getBiome(var6);
            if (var8.is(BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL)) {
               if (var5 == 0 && param3 != null && param3.getAxis().isHorizontal()) {
                  var7 = BuiltInRegistries.BLOCK
                     .getTag(BlockTags.WALL_CORALS)
                     .flatMap(l_param1 -> l_param1.getRandomElement(param1.random))
                     .map(l_param0 -> l_param0.value().defaultBlockState())
                     .orElse(var7);
                  if (var7.hasProperty(BaseCoralWallFanBlock.FACING)) {
                     var7 = var7.setValue(BaseCoralWallFanBlock.FACING, param3);
                  }
               } else if (var4.nextInt(4) == 0) {
                  var7 = BuiltInRegistries.BLOCK
                     .getTag(BlockTags.UNDERWATER_BONEMEALS)
                     .flatMap(l_param1 -> l_param1.getRandomElement(param1.random))
                     .map(l_param0 -> l_param0.value().defaultBlockState())
                     .orElse(var7);
               }
            }

            if (var7.is(BlockTags.WALL_CORALS, l_param0 -> l_param0.hasProperty(BaseCoralWallFanBlock.FACING))) {
               for (int var9 = 0; !var7.canSurvive(param1, var6) && var9 < 4; var9++) {
                  var7 = var7.setValue(BaseCoralWallFanBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(var4));
               }
            }

            if (var7.canSurvive(param1, var6)) {
               BlockState var9 = param1.getBlockState(var6);
               if (var9.is(Blocks.WATER) && param1.getFluidState(var6).getAmount() == 8) {
                  param1.setBlock(var6, var7, 3);
               } else if (var9.is(Blocks.SEAGRASS) && var4.nextInt(10) == 0) {
                  ((BonemealableBlock)Blocks.SEAGRASS).performBonemeal((ServerLevel)param1, var4, var6, var9);
               }
            }
         }

         param0.shrink(1);
         return true;
      } else {
         return false;
      }
   }

   public static void addGrowthParticles(LevelAccessor param0, BlockPos param1, int param2) {
      if (param2 == 0) {
         param2 = 15;
      }

      BlockState var3 = param0.getBlockState(param1);
      if (!var3.isAir()) {
         double var4 = 0.5;
         double var6;
         if (var3.is(Blocks.WATER)) {
            param2 *= 3;
            var6 = 1.0;
            var4 = 3.0;
         } else if (var3.isSolidRender(param0, param1)) {
            param1 = param1.above();
            param2 *= 3;
            var4 = 3.0;
            var6 = 1.0;
         } else {
            var6 = var3.getShape(param0, param1).max(Direction.Axis.Y);
         }

         param0.addParticle(ParticleTypes.HAPPY_VILLAGER, param1.getX() + 0.5, param1.getY() + 0.5, param1.getZ() + 0.5, 0.0, 0.0, 0.0);
         RandomSource var8 = param0.getRandom();

         for (int var9 = 0; var9 < param2; var9++) {
            double var10 = var8.nextGaussian() * 0.02;
            double var12 = var8.nextGaussian() * 0.02;
            double var14 = var8.nextGaussian() * 0.02;
            double var16 = 0.5 - var4;
            double var18 = param1.getX() + var16 + var8.nextDouble() * var4 * 2.0;
            double var20 = param1.getY() + var8.nextDouble() * var6;
            double var22 = param1.getZ() + var16 + var8.nextDouble() * var4 * 2.0;
            if (!param0.getBlockState(BlockPos.containing(var18, var20, var22).below()).isAir()) {
               param0.addParticle(ParticleTypes.HAPPY_VILLAGER, var18, var20, var22, var10, var12, var14);
            }
         }
      }
   }
}
