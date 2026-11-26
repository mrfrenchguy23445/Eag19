package net.minecraft.util;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ParticleUtils {
   public static void spawnParticlesOnBlockFaces(Level param0, BlockPos param1, ParticleOptions param2, IntProvider param3) {
      for (Direction var7 : Direction.values()) {
         spawnParticlesOnBlockFace(param0, param1, param2, param3, var7, () -> getRandomSpeedRanges(param0.random), 0.55);
      }
   }

   public static void spawnParticlesOnBlockFace(
      Level param0, BlockPos param1, ParticleOptions param2, IntProvider param3, Direction param4, Supplier<Vec3> param5, double param6
   ) {
      int var8 = param3.sample(param0.random);

      for (int var9 = 0; var9 < var8; var9++) {
         spawnParticleOnFace(param0, param1, param4, param2, param5.get(), param6);
      }
   }

   private static Vec3 getRandomSpeedRanges(RandomSource param0) {
      return new Vec3(Mth.nextDouble(param0, -0.5, 0.5), Mth.nextDouble(param0, -0.5, 0.5), Mth.nextDouble(param0, -0.5, 0.5));
   }

   public static void spawnParticlesAlongAxis(Direction.Axis param0, Level param1, BlockPos param2, double param3, ParticleOptions param5, UniformInt param6) {
      Vec3 var7 = Vec3.atCenterOf(param2);
      boolean var8 = param0 == Direction.Axis.X;
      boolean var9 = param0 == Direction.Axis.Y;
      boolean var10 = param0 == Direction.Axis.Z;
      int var11 = param6.sample(param1.random);

      for (int var12 = 0; var12 < var11; var12++) {
         double var13 = var7.x + Mth.nextDouble(param1.random, -1.0, 1.0) * (var8 ? 0.5 : param3);
         double var15 = var7.y + Mth.nextDouble(param1.random, -1.0, 1.0) * (var9 ? 0.5 : param3);
         double var17 = var7.z + Mth.nextDouble(param1.random, -1.0, 1.0) * (var10 ? 0.5 : param3);
         double var19 = var8 ? Mth.nextDouble(param1.random, -1.0, 1.0) : 0.0;
         double var21 = var9 ? Mth.nextDouble(param1.random, -1.0, 1.0) : 0.0;
         double var23 = var10 ? Mth.nextDouble(param1.random, -1.0, 1.0) : 0.0;
         param1.addParticle(param5, var13, var15, var17, var19, var21, var23);
      }
   }

   public static void spawnParticleOnFace(Level param0, BlockPos param1, Direction param2, ParticleOptions param3, Vec3 param4, double param5) {
      Vec3 var7 = Vec3.atCenterOf(param1);
      int var8 = param2.getStepX();
      int var9 = param2.getStepY();
      int var10 = param2.getStepZ();
      double var11 = var7.x + (var8 == 0 ? Mth.nextDouble(param0.random, -0.5, 0.5) : var8 * param5);
      double var13 = var7.y + (var9 == 0 ? Mth.nextDouble(param0.random, -0.5, 0.5) : var9 * param5);
      double var15 = var7.z + (var10 == 0 ? Mth.nextDouble(param0.random, -0.5, 0.5) : var10 * param5);
      double var17 = var8 == 0 ? param4.x() : 0.0;
      double var19 = var9 == 0 ? param4.y() : 0.0;
      double var21 = var10 == 0 ? param4.z() : 0.0;
      param0.addParticle(param3, var11, var13, var15, var17, var19, var21);
   }

   public static void spawnParticleBelow(Level param0, BlockPos param1, RandomSource param2, ParticleOptions param3) {
      double var4 = param1.getX() + param2.nextDouble();
      double var6 = param1.getY() - 0.05;
      double var8 = param1.getZ() + param2.nextDouble();
      param0.addParticle(param3, var4, var6, var8, 0.0, 0.0, 0.0);
   }
}
