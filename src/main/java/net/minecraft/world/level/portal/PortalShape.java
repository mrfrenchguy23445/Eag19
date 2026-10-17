package net.minecraft.world.level.portal;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PortalShape {
   private static final int MIN_WIDTH = 2;
   public static final int MAX_WIDTH = 21;
   private static final int MIN_HEIGHT = 3;
   public static final int MAX_HEIGHT = 21;
   private static final BlockBehaviour.StatePredicate FRAME = (param0, param1, param2) -> param0.is(Blocks.OBSIDIAN);
   private static final float SAFE_TRAVEL_MAX_ENTITY_XY = 4.0F;
   private static final double SAFE_TRAVEL_MAX_VERTICAL_DELTA = 1.0;
   private final LevelAccessor level;
   private final Direction.Axis axis;
   private final Direction rightDir;
   private int numPortalBlocks;
   @Nullable
   private BlockPos bottomLeft;
   private int height;
   private final int width;

   public static Optional<PortalShape> findEmptyPortalShape(LevelAccessor param0, BlockPos param1, Direction.Axis param2) {
      return findPortalShape(param0, param1, l_param0 -> l_param0.isValid() && l_param0.numPortalBlocks == 0, param2);
   }

   public static Optional<PortalShape> findPortalShape(LevelAccessor param0, BlockPos param1, Predicate<PortalShape> param2, Direction.Axis param3) {
      Optional<PortalShape> var4 = Optional.of(new PortalShape(param0, param1, param3)).filter(param2);
      if (var4.isPresent()) {
         return var4;
      }

      Direction.Axis var5 = param3 == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
      return Optional.of(new PortalShape(param0, param1, var5)).filter(param2);
   }

   public PortalShape(LevelAccessor param0, BlockPos param1, Direction.Axis param2) {
      this.level = param0;
      this.axis = param2;
      this.rightDir = param2 == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
      this.bottomLeft = this.calculateBottomLeft(param1);
      if (this.bottomLeft == null) {
         this.bottomLeft = param1;
         this.width = 1;
         this.height = 1;
      } else {
         this.width = this.calculateWidth();
         if (this.width > 0) {
            this.height = this.calculateHeight();
         }
      }
   }

   @Nullable
   private BlockPos calculateBottomLeft(BlockPos param0) {
      int var2 = Math.max(this.level.getMinBuildHeight(), param0.getY() - 21);

      while (param0.getY() > var2 && isEmpty(this.level.getBlockState(param0.below()))) {
         param0 = param0.below();
      }

      Direction var3 = this.rightDir.getOpposite();
      int var4 = this.getDistanceUntilEdgeAboveFrame(param0, var3) - 1;
      return var4 < 0 ? null : param0.relative(var3, var4);
   }

   private int calculateWidth() {
      int var1 = this.getDistanceUntilEdgeAboveFrame(this.bottomLeft, this.rightDir);
      return var1 >= 2 && var1 <= 21 ? var1 : 0;
   }

   private int getDistanceUntilEdgeAboveFrame(BlockPos param0, Direction param1) {
      BlockPos.MutableBlockPos var3 = new BlockPos.MutableBlockPos();

      for (int var4 = 0; var4 <= 21; var4++) {
         var3.set(param0).move(param1, var4);
         BlockState var5 = this.level.getBlockState(var3);
         if (!isEmpty(var5)) {
            if (FRAME.test(var5, this.level, var3)) {
               return var4;
            }
            break;
         }

         BlockState var6 = this.level.getBlockState(var3.move(Direction.DOWN));
         if (!FRAME.test(var6, this.level, var3)) {
            break;
         }
      }

      return 0;
   }

   private int calculateHeight() {
      BlockPos.MutableBlockPos var1 = new BlockPos.MutableBlockPos();
      int var2 = this.getDistanceUntilTop(var1);
      return var2 >= 3 && var2 <= 21 && this.hasTopFrame(var1, var2) ? var2 : 0;
   }

   private boolean hasTopFrame(BlockPos.MutableBlockPos param0, int param1) {
      for (int var3 = 0; var3 < this.width; var3++) {
         BlockPos.MutableBlockPos var4 = param0.set(this.bottomLeft).move(Direction.UP, param1).move(this.rightDir, var3);
         if (!FRAME.test(this.level.getBlockState(var4), this.level, var4)) {
            return false;
         }
      }

      return true;
   }

   private int getDistanceUntilTop(BlockPos.MutableBlockPos param0) {
      for (int var2 = 0; var2 < 21; var2++) {
         param0.set(this.bottomLeft).move(Direction.UP, var2).move(this.rightDir, -1);
         if (!FRAME.test(this.level.getBlockState(param0), this.level, param0)) {
            return var2;
         }

         param0.set(this.bottomLeft).move(Direction.UP, var2).move(this.rightDir, this.width);
         if (!FRAME.test(this.level.getBlockState(param0), this.level, param0)) {
            return var2;
         }

         for (int var3 = 0; var3 < this.width; var3++) {
            param0.set(this.bottomLeft).move(Direction.UP, var2).move(this.rightDir, var3);
            BlockState var4 = this.level.getBlockState(param0);
            if (!isEmpty(var4)) {
               return var2;
            }

            if (var4.is(Blocks.NETHER_PORTAL)) {
               this.numPortalBlocks++;
            }
         }
      }

      return 21;
   }

   private static boolean isEmpty(BlockState param0) {
      return param0.isAir() || param0.is(BlockTags.FIRE) || param0.is(Blocks.NETHER_PORTAL);
   }

   public boolean isValid() {
      return this.bottomLeft != null && this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
   }

   public void createPortalBlocks() {
      BlockState var1 = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, this.axis);
      BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
         .forEach(l_param1 -> this.level.setBlock(l_param1, var1, 18));
   }

   public boolean isComplete() {
      return this.isValid() && this.numPortalBlocks == this.width * this.height;
   }

   public static Vec3 getRelativePosition(BlockUtil.FoundRectangle param0, Direction.Axis param1, Vec3 param2, EntityDimensions param3) {
      double var4 = (double)param0.axis1Size - param3.width;
      double var6 = (double)param0.axis2Size - param3.height;
      BlockPos var8 = param0.minCorner;
      double var9;
      if (var4 > 0.0) {
         float var11 = var8.get(param1) + param3.width / 2.0F;
         var9 = Mth.clamp(Mth.inverseLerp(param2.get(param1) - var11, 0.0, var4), 0.0, 1.0);
      } else {
         var9 = 0.5;
      }

      double var11;
      if (var6 > 0.0) {
         Direction.Axis var13 = Direction.Axis.Y;
         var11 = Mth.clamp(Mth.inverseLerp(param2.get(var13) - var8.get(var13), 0.0, var6), 0.0, 1.0);
      } else {
         var11 = 0.0;
      }

      Direction.Axis var13 = param1 == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
      double var14 = param2.get(var13) - (var8.get(var13) + 0.5);
      return new Vec3(var9, var11, var14);
   }

   public static PortalInfo createPortalInfo(
      ServerLevel param0, BlockUtil.FoundRectangle param1, Direction.Axis param2, Vec3 param3, Entity param4, Vec3 param5, float param6, float param7
   ) {
      BlockPos var8 = param1.minCorner;
      BlockState var9 = param0.getBlockState(var8);
      Direction.Axis var10 = var9.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
      double var11 = param1.axis1Size;
      double var13 = param1.axis2Size;
      EntityDimensions var15 = param4.getDimensions(param4.getPose());
      int var16 = param2 == var10 ? 0 : 90;
      Vec3 var17 = param2 == var10 ? param5 : new Vec3(param5.z, param5.y, -param5.x);
      double var18 = var15.width / 2.0 + (var11 - var15.width) * param3.x();
      double var20 = (var13 - var15.height) * param3.y();
      double var22 = 0.5 + param3.z();
      boolean var24 = var10 == Direction.Axis.X;
      Vec3 var25 = new Vec3(var8.getX() + (var24 ? var18 : var22), var8.getY() + var20, var8.getZ() + (var24 ? var22 : var18));
      Vec3 var26 = findCollisionFreePosition(var25, param0, param4, var15);
      return new PortalInfo(var26, var17, param6 + var16, param7);
   }

   private static Vec3 findCollisionFreePosition(Vec3 param0, ServerLevel param1, Entity param2, EntityDimensions param3) {
      if (!(param3.width > 4.0F) && !(param3.height > 4.0F)) {
         double var4 = param3.height / 2.0;
         Vec3 var6 = param0.add(0.0, var4, 0.0);
         VoxelShape var7 = Shapes.create(AABB.ofSize(var6, param3.width, 0.0, param3.width).expandTowards(0.0, 1.0, 0.0).inflate(1.0E-6));
         Optional<Vec3> var8 = param1.findFreePosition(param2, var7, var6, param3.width, param3.height, param3.width);
         Optional<Vec3> var9 = var8.map(l_param2 -> l_param2.subtract(0.0, var4, 0.0));
         return var9.orElse(param0);
      } else {
         return param0;
      }
   }
}
