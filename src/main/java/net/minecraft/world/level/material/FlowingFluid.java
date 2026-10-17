package net.minecraft.world.level.material;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class FlowingFluid extends Fluid {
   public static final BooleanProperty FALLING = BlockStateProperties.FALLING;
   public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING;
   private static final int CACHE_SIZE = 200;
   private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
      Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> var0 = new Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>(200) {
         @Override
         protected void rehash(int l_param0) {
         }
      };
      var0.defaultReturnValue((byte)127);
      return var0;
   });
   private final Map<FluidState, VoxelShape> shapes = Maps.newIdentityHashMap();

   @Override
   protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> param0) {
      param0.add(FALLING);
   }

   @Override
   public Vec3 getFlow(BlockGetter param0, BlockPos param1, FluidState param2) {
      double var4 = 0.0;
      double var6 = 0.0;
      BlockPos.MutableBlockPos var8 = new BlockPos.MutableBlockPos();

      for (Direction var10 : Direction.Plane.HORIZONTAL) {
         var8.setWithOffset(param1, var10);
         FluidState var11 = param0.getFluidState(var8);
         if (this.affectsFlow(var11)) {
            float var12 = var11.getOwnHeight();
            float var13 = 0.0F;
            if (var12 == 0.0F) {
               if (!param0.getBlockState(var8).getMaterial().blocksMotion()) {
                  BlockPos var14 = var8.below();
                  FluidState var15 = param0.getFluidState(var14);
                  if (this.affectsFlow(var15)) {
                     var12 = var15.getOwnHeight();
                     if (var12 > 0.0F) {
                        var13 = param2.getOwnHeight() - (var12 - 0.8888889F);
                     }
                  }
               }
            } else if (var12 > 0.0F) {
               var13 = param2.getOwnHeight() - var12;
            }

            if (var13 != 0.0F) {
               var4 += var10.getStepX() * var13;
               var6 += var10.getStepZ() * var13;
            }
         }
      }

      Vec3 var9 = new Vec3(var4, 0.0, var6);
      if (param2.getValue(FALLING)) {
         for (Direction var11 : Direction.Plane.HORIZONTAL) {
            var8.setWithOffset(param1, var11);
            if (this.isSolidFace(param0, var8, var11) || this.isSolidFace(param0, var8.above(), var11)) {
               var9 = var9.normalize().add(0.0, -6.0, 0.0);
               break;
            }
         }
      }

      return var9.normalize();
   }

   private boolean affectsFlow(FluidState param0) {
      return param0.isEmpty() || param0.getType().isSame(this);
   }

   protected boolean isSolidFace(BlockGetter param0, BlockPos param1, Direction param2) {
      BlockState var4 = param0.getBlockState(param1);
      FluidState var5 = param0.getFluidState(param1);
      if (var5.getType().isSame(this)) {
         return false;
      } else if (param2 == Direction.UP) {
         return true;
      } else {
         return var4.getMaterial() == Material.ICE ? false : var4.isFaceSturdy(param0, param1, param2);
      }
   }

   protected void spread(Level param0, BlockPos param1, FluidState param2) {
      if (!param2.isEmpty()) {
         BlockState var4 = param0.getBlockState(param1);
         BlockPos var5 = param1.below();
         BlockState var6 = param0.getBlockState(var5);
         FluidState var7 = this.getNewLiquid(param0, var5, var6);
         if (this.canSpreadTo(param0, param1, var4, Direction.DOWN, var5, var6, param0.getFluidState(var5), var7.getType())) {
            this.spreadTo(param0, var5, var6, Direction.DOWN, var7);
            if (this.sourceNeighborCount(param0, param1) >= 3) {
               this.spreadToSides(param0, param1, param2, var4);
            }
         } else if (param2.isSource() || !this.isWaterHole(param0, var7.getType(), param1, var4, var5, var6)) {
            this.spreadToSides(param0, param1, param2, var4);
         }
      }
   }

   private void spreadToSides(Level param0, BlockPos param1, FluidState param2, BlockState param3) {
      int var5 = param2.getAmount() - this.getDropOff(param0);
      if (param2.getValue(FALLING)) {
         var5 = 7;
      }

      if (var5 > 0) {
         Map<Direction, FluidState> var6 = this.getSpread(param0, param1, param3);

         for (Entry<Direction, FluidState> var8 : var6.entrySet()) {
            Direction var9 = var8.getKey();
            FluidState var10 = var8.getValue();
            BlockPos var11 = param1.relative(var9);
            BlockState var12 = param0.getBlockState(var11);
            if (this.canSpreadTo(param0, param1, param3, var9, var11, var12, param0.getFluidState(var11), var10.getType())) {
               this.spreadTo(param0, var11, var12, var9, var10);
            }
         }
      }
   }

   protected FluidState getNewLiquid(Level param0, BlockPos param1, BlockState param2) {
      int var4 = 0;
      int var5 = 0;

      for (Direction var7 : Direction.Plane.HORIZONTAL) {
         BlockPos var8 = param1.relative(var7);
         BlockState var9 = param0.getBlockState(var8);
         FluidState var10 = var9.getFluidState();
         if (var10.getType().isSame(this) && this.canPassThroughWall(var7, param0, param1, param2, var8, var9)) {
            if (var10.isSource()) {
               var5++;
            }

            var4 = Math.max(var4, var10.getAmount());
         }
      }

      if (this.canConvertToSource(param0) && var5 >= 2) {
         BlockState var6 = param0.getBlockState(param1.below());
         FluidState var7 = var6.getFluidState();
         if (var6.getMaterial().isSolid() || this.isSourceBlockOfThisType(var7)) {
            return this.getSource(false);
         }
      }

      BlockPos var6 = param1.above();
      BlockState var7 = param0.getBlockState(var6);
      FluidState var8 = var7.getFluidState();
      if (!var8.isEmpty() && var8.getType().isSame(this) && this.canPassThroughWall(Direction.UP, param0, param1, param2, var6, var7)) {
         return this.getFlowing(8, true);
      }

      int var9 = var4 - this.getDropOff(param0);
      return var9 <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(var9, false);
   }

   private boolean canPassThroughWall(Direction param0, BlockGetter param1, BlockPos param2, BlockState param3, BlockPos param4, BlockState param5) {
      Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> var7;
      if (!param3.getBlock().hasDynamicShape() && !param5.getBlock().hasDynamicShape()) {
         var7 = OCCLUSION_CACHE.get();
      } else {
         var7 = null;
      }

      Block.BlockStatePairKey var8;
      if (var7 != null) {
         var8 = new Block.BlockStatePairKey(param3, param5, param0);
         byte var9 = var7.getAndMoveToFirst(var8);
         if (var9 != 127) {
            return var9 != 0;
         }
      } else {
         var8 = null;
      }

      VoxelShape var9 = param3.getCollisionShape(param1, param2);
      VoxelShape var10 = param5.getCollisionShape(param1, param4);
      boolean var11 = !Shapes.mergedFaceOccludes(var9, var10, param0);
      if (var7 != null) {
         if (var7.size() == 200) {
            var7.removeLastByte();
         }

         var7.putAndMoveToFirst(var8, (byte)(var11 ? 1 : 0));
      }

      return var11;
   }

   public abstract Fluid getFlowing();

   public FluidState getFlowing(int param0, boolean param1) {
      return this.getFlowing().defaultFluidState().setValue(LEVEL, param0).setValue(FALLING, param1);
   }

   public abstract Fluid getSource();

   public FluidState getSource(boolean param0) {
      return this.getSource().defaultFluidState().setValue(FALLING, param0);
   }

   protected abstract boolean canConvertToSource(Level var1);

   protected void spreadTo(LevelAccessor param0, BlockPos param1, BlockState param2, Direction param3, FluidState param4) {
      if (param2.getBlock() instanceof LiquidBlockContainer) {
         ((LiquidBlockContainer)param2.getBlock()).placeLiquid(param0, param1, param2, param4);
      } else {
         if (!param2.isAir()) {
            this.beforeDestroyingBlock(param0, param1, param2);
         }

         param0.setBlock(param1, param4.createLegacyBlock(), 3);
      }
   }

   protected abstract void beforeDestroyingBlock(LevelAccessor var1, BlockPos var2, BlockState var3);

   private static short getCacheKey(BlockPos param0, BlockPos param1) {
      int var2 = param1.getX() - param0.getX();
      int var3 = param1.getZ() - param0.getZ();
      return (short)((var2 + 128 & 0xFF) << 8 | var3 + 128 & 0xFF);
   }

   protected int getSlopeDistance(
      LevelReader param0,
      BlockPos param1,
      int param2,
      Direction param3,
      BlockState param4,
      BlockPos param5,
      Short2ObjectMap<Pair<BlockState, FluidState>> param6,
      Short2BooleanMap param7
   ) {
      int var9 = 1000;

      for (Direction var11 : Direction.Plane.HORIZONTAL) {
         if (var11 != param3) {
            BlockPos var12 = param1.relative(var11);
            short var13 = getCacheKey(param5, var12);
            Pair<BlockState, FluidState> var14 = param6.computeIfAbsent(var13, l_param2 -> {
               BlockState l_var3 = param0.getBlockState(var12);
               return Pair.of(l_var3, l_var3.getFluidState());
            });
            BlockState var15 = var14.getFirst();
            FluidState var16 = var14.getSecond();
            if (this.canPassThrough(param0, this.getFlowing(), param1, param4, var11, var12, var15, var16)) {
               boolean var17 = param7.computeIfAbsent(var13, l_param3 -> {
                  BlockPos l_var5 = var12.below();
                  BlockState l_var6 = param0.getBlockState(l_var5);
                  return this.isWaterHole(param0, this.getFlowing(), var12, var15, l_var5, l_var6);
               });
               if (var17) {
                  return param2;
               }

               if (param2 < this.getSlopeFindDistance(param0)) {
                  int var18 = this.getSlopeDistance(param0, var12, param2 + 1, var11.getOpposite(), var15, param5, param6, param7);
                  if (var18 < var9) {
                     var9 = var18;
                  }
               }
            }
         }
      }

      return var9;
   }

   private boolean isWaterHole(BlockGetter param0, Fluid param1, BlockPos param2, BlockState param3, BlockPos param4, BlockState param5) {
      if (!this.canPassThroughWall(Direction.DOWN, param0, param2, param3, param4, param5)) {
         return false;
      } else {
         return param5.getFluidState().getType().isSame(this) ? true : this.canHoldFluid(param0, param4, param5, param1);
      }
   }

   private boolean canPassThrough(
      BlockGetter param0, Fluid param1, BlockPos param2, BlockState param3, Direction param4, BlockPos param5, BlockState param6, FluidState param7
   ) {
      return !this.isSourceBlockOfThisType(param7)
         && this.canPassThroughWall(param4, param0, param2, param3, param5, param6)
         && this.canHoldFluid(param0, param5, param6, param1);
   }

   private boolean isSourceBlockOfThisType(FluidState param0) {
      return param0.getType().isSame(this) && param0.isSource();
   }

   protected abstract int getSlopeFindDistance(LevelReader var1);

   private int sourceNeighborCount(LevelReader param0, BlockPos param1) {
      int var3 = 0;

      for (Direction var5 : Direction.Plane.HORIZONTAL) {
         BlockPos var6 = param1.relative(var5);
         FluidState var7 = param0.getFluidState(var6);
         if (this.isSourceBlockOfThisType(var7)) {
            var3++;
         }
      }

      return var3;
   }

   protected Map<Direction, FluidState> getSpread(Level param0, BlockPos param1, BlockState param2) {
      int var4 = 1000;
      Map<Direction, FluidState> var5 = Maps.newEnumMap(Direction.class);
      Short2ObjectMap<Pair<BlockState, FluidState>> var6 = new Short2ObjectOpenHashMap<>();
      Short2BooleanMap var7 = new Short2BooleanOpenHashMap();

      for (Direction var9 : Direction.Plane.HORIZONTAL) {
         BlockPos var10 = param1.relative(var9);
         short var11 = getCacheKey(param1, var10);
         Pair<BlockState, FluidState> var12 = var6.computeIfAbsent(var11, l_param2 -> {
            BlockState l_var3 = param0.getBlockState(var10);
            return Pair.of(l_var3, l_var3.getFluidState());
         });
         BlockState var13 = var12.getFirst();
         FluidState var14 = var12.getSecond();
         FluidState var15 = this.getNewLiquid(param0, var10, var13);
         if (this.canPassThrough(param0, var15.getType(), param1, param2, var9, var10, var13, var14)) {
            BlockPos var17 = var10.below();
            boolean var18 = var7.computeIfAbsent(var11, l_param4 -> {
               BlockState l_var6 = param0.getBlockState(var17);
               return this.isWaterHole(param0, this.getFlowing(), var10, var13, var17, l_var6);
            });
            int var16;
            if (var18) {
               var16 = 0;
            } else {
               var16 = this.getSlopeDistance(param0, var10, 1, var9.getOpposite(), var13, param1, var6, var7);
            }

            if (var16 < var4) {
               var5.clear();
            }

            if (var16 <= var4) {
               var5.put(var9, var15);
               var4 = var16;
            }
         }
      }

      return var5;
   }

   private boolean canHoldFluid(BlockGetter param0, BlockPos param1, BlockState param2, Fluid param3) {
      Block var5 = param2.getBlock();
      if (var5 instanceof LiquidBlockContainer) {
         return ((LiquidBlockContainer)var5).canPlaceLiquid(param0, param1, param2, param3);
      } else if (!(var5 instanceof DoorBlock)
         && !param2.is(BlockTags.SIGNS)
         && !param2.is(Blocks.LADDER)
         && !param2.is(Blocks.SUGAR_CANE)
         && !param2.is(Blocks.BUBBLE_COLUMN)) {
         Material var6 = param2.getMaterial();
         return var6 != Material.PORTAL && var6 != Material.STRUCTURAL_AIR && var6 != Material.WATER_PLANT && var6 != Material.REPLACEABLE_WATER_PLANT
            ? !var6.blocksMotion()
            : false;
      } else {
         return false;
      }
   }

   protected boolean canSpreadTo(
      BlockGetter param0, BlockPos param1, BlockState param2, Direction param3, BlockPos param4, BlockState param5, FluidState param6, Fluid param7
   ) {
      return param6.canBeReplacedWith(param0, param4, param7, param3)
         && this.canPassThroughWall(param3, param0, param1, param2, param4, param5)
         && this.canHoldFluid(param0, param4, param5, param7);
   }

   protected abstract int getDropOff(LevelReader var1);

   protected int getSpreadDelay(Level param0, BlockPos param1, FluidState param2, FluidState param3) {
      return this.getTickDelay(param0);
   }

   @Override
   public void tick(Level param0, BlockPos param1, FluidState param2) {
      if (!param2.isSource()) {
         FluidState var4 = this.getNewLiquid(param0, param1, param0.getBlockState(param1));
         int var5 = this.getSpreadDelay(param0, param1, param2, var4);
         if (var4.isEmpty()) {
            param2 = var4;
            param0.setBlock(param1, Blocks.AIR.defaultBlockState(), 3);
         } else if (!var4.equals(param2)) {
            param2 = var4;
            BlockState var6 = param2.createLegacyBlock();
            param0.setBlock(param1, var6, 2);
            param0.scheduleTick(param1, param2.getType(), var5);
            param0.updateNeighborsAt(param1, var6.getBlock());
         }
      }

      this.spread(param0, param1, param2);
   }

   protected static int getLegacyLevel(FluidState param0) {
      return param0.isSource() ? 0 : 8 - Math.min(param0.getAmount(), 8) + (param0.getValue(FALLING) ? 8 : 0);
   }

   private static boolean hasSameAbove(FluidState param0, BlockGetter param1, BlockPos param2) {
      return param0.getType().isSame(param1.getFluidState(param2.above()).getType());
   }

   @Override
   public float getHeight(FluidState param0, BlockGetter param1, BlockPos param2) {
      return hasSameAbove(param0, param1, param2) ? 1.0F : param0.getOwnHeight();
   }

   @Override
   public float getOwnHeight(FluidState param0) {
      return param0.getAmount() / 9.0F;
   }

   @Override
   public abstract int getAmount(FluidState var1);

   @Override
   public VoxelShape getShape(FluidState param0, BlockGetter param1, BlockPos param2) {
      return param0.getAmount() == 9 && hasSameAbove(param0, param1, param2)
         ? Shapes.block()
         : this.shapes.computeIfAbsent(param0, l_param2 -> Shapes.box(0.0, 0.0, 0.0, 1.0, l_param2.getHeight(param1, param2), 1.0));
   }
}
