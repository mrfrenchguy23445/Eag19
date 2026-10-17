package net.minecraft.world.entity.ai.goal;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class MoveThroughVillageGoal extends Goal {
   protected final PathfinderMob mob;
   private final double speedModifier;
   @Nullable
   private Path path;
   private BlockPos poiPos;
   private final boolean onlyAtNight;
   private final List<BlockPos> visited = Lists.newArrayList();
   private final int distanceToPoi;
   private final BooleanSupplier canDealWithDoors;

   public MoveThroughVillageGoal(PathfinderMob param0, double param1, boolean param3, int param4, BooleanSupplier param5) {
      this.mob = param0;
      this.speedModifier = param1;
      this.onlyAtNight = param3;
      this.distanceToPoi = param4;
      this.canDealWithDoors = param5;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      if (!GoalUtils.hasGroundPathNavigation(param0)) {
         throw new IllegalArgumentException("Unsupported mob for MoveThroughVillageGoal");
      }
   }

   @Override
   public boolean canUse() {
      if (!GoalUtils.hasGroundPathNavigation(this.mob)) {
         return false;
      }

      this.updateVisited();
      if (this.onlyAtNight && this.mob.level.isDay()) {
         return false;
      }

      ServerLevel var1 = (ServerLevel)this.mob.level;
      BlockPos var2 = this.mob.blockPosition();
      if (!var1.isCloseToVillage(var2, 6)) {
         return false;
      }

      Vec3 var3 = LandRandomPos.getPos(
         this.mob,
         15,
         7,
         l_param2 -> {
            if (!var1.isVillage(l_param2)) {
               return Double.NEGATIVE_INFINITY;
            }

            Optional<BlockPos> l_var4 = var1.getPoiManager()
               .find(l2_param0 -> l2_param0.is(PoiTypeTags.VILLAGE), this::hasNotVisited, l_param2, 10, PoiManager.Occupancy.IS_OCCUPIED);
            return l_var4.<Double>map(l2_param1 -> -l2_param1.distSqr(var2)).orElse(Double.NEGATIVE_INFINITY);
         }
      );
      if (var3 == null) {
         return false;
      }

      Optional<BlockPos> var4 = var1.getPoiManager()
         .find(l_param0 -> l_param0.is(PoiTypeTags.VILLAGE), this::hasNotVisited, BlockPos.containing(var3), 10, PoiManager.Occupancy.IS_OCCUPIED);
      if (var4.isEmpty()) {
         return false;
      }

      this.poiPos = var4.get().immutable();
      GroundPathNavigation var5 = (GroundPathNavigation)this.mob.getNavigation();
      boolean var6 = var5.canOpenDoors();
      var5.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
      this.path = var5.createPath(this.poiPos, 0);
      var5.setCanOpenDoors(var6);
      if (this.path == null) {
         Vec3 var7 = DefaultRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf(this.poiPos), (float) (Math.PI / 2));
         if (var7 == null) {
            return false;
         }

         var5.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
         this.path = this.mob.getNavigation().createPath(var7.x, var7.y, var7.z, 0);
         var5.setCanOpenDoors(var6);
         if (this.path == null) {
            return false;
         }
      }

      for (int var7 = 0; var7 < this.path.getNodeCount(); var7++) {
         Node var8 = this.path.getNode(var7);
         BlockPos var9 = new BlockPos(var8.x, var8.y + 1, var8.z);
         if (DoorBlock.isWoodenDoor(this.mob.level, var9)) {
            this.path = this.mob.getNavigation().createPath(var8.x, var8.y, var8.z, 0);
            break;
         }
      }

      return this.path != null;
   }

   @Override
   public boolean canContinueToUse() {
      return this.mob.getNavigation().isDone() ? false : !this.poiPos.closerToCenterThan(this.mob.position(), this.mob.getBbWidth() + this.distanceToPoi);
   }

   @Override
   public void start() {
      this.mob.getNavigation().moveTo(this.path, this.speedModifier);
   }

   @Override
   public void stop() {
      if (this.mob.getNavigation().isDone() || this.poiPos.closerToCenterThan(this.mob.position(), this.distanceToPoi)) {
         this.visited.add(this.poiPos);
      }
   }

   private boolean hasNotVisited(BlockPos param0) {
      for (BlockPos var3 : this.visited) {
         if (Objects.equals(param0, var3)) {
            return false;
         }
      }

      return true;
   }

   private void updateVisited() {
      if (this.visited.size() > 15) {
         this.visited.remove(0);
      }
   }
}
