package net.minecraft.world.level.pathfinder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public class PathFinder {
   private static final float FUDGING = 1.5F;
   private final Node[] neighbors = new Node[32];
   private final int maxVisitedNodes;
   private final NodeEvaluator nodeEvaluator;
   private static final boolean DEBUG = false;
   private final BinaryHeap openSet = new BinaryHeap();

   public PathFinder(NodeEvaluator param0, int param1) {
      this.nodeEvaluator = param0;
      this.maxVisitedNodes = param1;
   }

   @Nullable
   public Path findPath(PathNavigationRegion param0, Mob param1, Set<BlockPos> param2, float param3, int param4, float param5) {
      this.openSet.clear();
      this.nodeEvaluator.prepare(param0, param1);
      Node var7 = this.nodeEvaluator.getStart();
      if (var7 == null) {
         return null;
      }

      Map<Target, BlockPos> var8 = param2.stream()
         .collect(Collectors.toMap(l_param0 -> this.nodeEvaluator.getGoal(l_param0.getX(), l_param0.getY(), l_param0.getZ()), Function.identity()));
      Path var9 = this.findPath(param0.getProfiler(), var7, var8, param3, param4, param5);
      this.nodeEvaluator.done();
      return var9;
   }

   @Nullable
   private Path findPath(ProfilerFiller param0, Node param1, Map<Target, BlockPos> param2, float param3, int param4, float param5) {
      param0.push("find_path");
      param0.markForCharting(MetricCategory.PATH_FINDING);
      Set<Target> var7 = param2.keySet();
      param1.g = 0.0F;
      param1.h = this.getBestH(param1, var7);
      param1.f = param1.h;
      this.openSet.clear();
      this.openSet.insert(param1);
      Set<Node> var8 = ImmutableSet.of();
      int var9 = 0;
      Set<Target> var10 = Sets.newHashSetWithExpectedSize(var7.size());
      int var11 = (int)(this.maxVisitedNodes * param5);

      while (!this.openSet.isEmpty()) {
         if (++var9 >= var11) {
            break;
         }

         Node var12 = this.openSet.pop();
         var12.closed = true;

         for (Target var14 : var7) {
            if (var12.distanceManhattan(var14) <= param4) {
               var14.setReached();
               var10.add(var14);
            }
         }

         if (!var10.isEmpty()) {
            break;
         }

         if (!(var12.distanceTo(param1) >= param3)) {
            int var13 = this.nodeEvaluator.getNeighbors(this.neighbors, var12);

            for (int var14 = 0; var14 < var13; var14++) {
               Node var15 = this.neighbors[var14];
               float var16 = this.distance(var12, var15);
               var15.walkedDistance = var12.walkedDistance + var16;
               float var17 = var12.g + var16 + var15.costMalus;
               if (var15.walkedDistance < param3 && (!var15.inOpenSet() || var17 < var15.g)) {
                  var15.cameFrom = var12;
                  var15.g = var17;
                  var15.h = this.getBestH(var15, var7) * 1.5F;
                  if (var15.inOpenSet()) {
                     this.openSet.changeCost(var15, var15.g + var15.h);
                  } else {
                     var15.f = var15.g + var15.h;
                     this.openSet.insert(var15);
                  }
               }
            }
         }
      }

      Optional<Path> var12 = !var10.isEmpty()
         ? var10.stream()
            .map(l_param1 -> this.reconstructPath(l_param1.getBestNode(), param2.get(l_param1), true))
            .min(Comparator.comparingInt(Path::getNodeCount))
         : var7.stream()
            .map(l_param1 -> this.reconstructPath(l_param1.getBestNode(), param2.get(l_param1), false))
            .min(Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount));
      param0.pop();
      return !var12.isPresent() ? null : var12.get();
   }

   protected float distance(Node param0, Node param1) {
      return param0.distanceTo(param1);
   }

   private float getBestH(Node param0, Set<Target> param1) {
      float var3 = Float.MAX_VALUE;

      for (Target var5 : param1) {
         float var6 = param0.distanceTo(var5);
         var5.updateBest(var6, param0);
         var3 = Math.min(var6, var3);
      }

      return var3;
   }

   private Path reconstructPath(Node param0, BlockPos param1, boolean param2) {
      List<Node> var4 = Lists.newArrayList();
      Node var5 = param0;
      var4.add(0, var5);

      while (var5.cameFrom != null) {
         var5 = var5.cameFrom;
         var4.add(0, var5);
      }

      return new Path(var4, param1, param2);
   }
}
