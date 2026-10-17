package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;

public class BiomeManager {
   public static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
   private static final int ZOOM_BITS = 2;
   private static final int ZOOM = 4;
   private static final int ZOOM_MASK = 3;
   private final BiomeManager.NoiseBiomeSource noiseBiomeSource;
   private final long biomeZoomSeed;

   public BiomeManager(BiomeManager.NoiseBiomeSource param0, long param1) {
      this.noiseBiomeSource = param0;
      this.biomeZoomSeed = param1;
   }

   public static long obfuscateSeed(long param0) {
      return Hashing.sha256().hashLong(param0).asLong();
   }

   public BiomeManager withDifferentSource(BiomeManager.NoiseBiomeSource param0) {
      return new BiomeManager(param0, this.biomeZoomSeed);
   }

   public Holder<Biome> getBiome(BlockPos param0) {
      int var2 = param0.getX() - 2;
      int var3 = param0.getY() - 2;
      int var4 = param0.getZ() - 2;
      int var5 = var2 >> 2;
      int var6 = var3 >> 2;
      int var7 = var4 >> 2;
      double var8 = (var2 & 3) / 4.0;
      double var10 = (var3 & 3) / 4.0;
      double var12 = (var4 & 3) / 4.0;
      int var14 = 0;
      double var15 = Double.POSITIVE_INFINITY;

      for (int var17 = 0; var17 < 8; var17++) {
         boolean var18 = (var17 & 4) == 0;
         boolean var19 = (var17 & 2) == 0;
         boolean var20 = (var17 & 1) == 0;
         int var21 = var18 ? var5 : var5 + 1;
         int var22 = var19 ? var6 : var6 + 1;
         int var23 = var20 ? var7 : var7 + 1;
         double var24 = var18 ? var8 : var8 - 1.0;
         double var26 = var19 ? var10 : var10 - 1.0;
         double var28 = var20 ? var12 : var12 - 1.0;
         double var30 = getFiddledDistance(this.biomeZoomSeed, var21, var22, var23, var24, var26, var28);
         if (var15 > var30) {
            var14 = var17;
            var15 = var30;
         }
      }

      int var17 = (var14 & 4) == 0 ? var5 : var5 + 1;
      int var18 = (var14 & 2) == 0 ? var6 : var6 + 1;
      int var19 = (var14 & 1) == 0 ? var7 : var7 + 1;
      return this.noiseBiomeSource.getNoiseBiome(var17, var18, var19);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(double param0, double param2, double param4) {
      int var7 = QuartPos.fromBlock(Mth.floor(param0));
      int var8 = QuartPos.fromBlock(Mth.floor(param2));
      int var9 = QuartPos.fromBlock(Mth.floor(param4));
      return this.getNoiseBiomeAtQuart(var7, var8, var9);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(BlockPos param0) {
      int var2 = QuartPos.fromBlock(param0.getX());
      int var3 = QuartPos.fromBlock(param0.getY());
      int var4 = QuartPos.fromBlock(param0.getZ());
      return this.getNoiseBiomeAtQuart(var2, var3, var4);
   }

   public Holder<Biome> getNoiseBiomeAtQuart(int param0, int param1, int param2) {
      return this.noiseBiomeSource.getNoiseBiome(param0, param1, param2);
   }

   private static double getFiddledDistance(long param0, int param2, int param3, int param4, double param5, double param7, double param9) {
      long var11 = param0;
      var11 = LinearCongruentialGenerator.next(var11, param2);
      var11 = LinearCongruentialGenerator.next(var11, param3);
      var11 = LinearCongruentialGenerator.next(var11, param4);
      var11 = LinearCongruentialGenerator.next(var11, param2);
      var11 = LinearCongruentialGenerator.next(var11, param3);
      var11 = LinearCongruentialGenerator.next(var11, param4);
      double var13 = getFiddle(var11);
      var11 = LinearCongruentialGenerator.next(var11, param0);
      double var15 = getFiddle(var11);
      var11 = LinearCongruentialGenerator.next(var11, param0);
      double var17 = getFiddle(var11);
      return Mth.square(param9 + var17) + Mth.square(param7 + var15) + Mth.square(param5 + var13);
   }

   private static double getFiddle(long param0) {
      double var2 = Math.floorMod(param0 >> 24, 1024) / 1024.0;
      return (var2 - 0.5) * 0.9;
   }

   public interface NoiseBiomeSource {
      Holder<Biome> getNoiseBiome(int var1, int var2, int var3);
   }
}
