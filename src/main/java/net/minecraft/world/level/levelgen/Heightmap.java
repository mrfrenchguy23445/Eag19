package net.minecraft.world.level.levelgen;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;

public class Heightmap {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final Predicate<BlockState> NOT_AIR = param0 -> !param0.isAir();
   static final Predicate<BlockState> MATERIAL_MOTION_BLOCKING = param0 -> param0.getMaterial().blocksMotion();
   private final BitStorage data;
   private final Predicate<BlockState> isOpaque;
   private final ChunkAccess chunk;

   public Heightmap(ChunkAccess param0, Heightmap.Types param1) {
      this.isOpaque = param1.isOpaque();
      this.chunk = param0;
      int var3 = Mth.ceillog2(param0.getHeight() + 1);
      this.data = new SimpleBitStorage(var3, 256);
   }

   public static void primeHeightmaps(ChunkAccess param0, Set<Heightmap.Types> param1) {
      int var2 = param1.size();
      ObjectList<Heightmap> var3 = new ObjectArrayList<>(var2);
      ObjectListIterator<Heightmap> var4 = var3.iterator();
      int var5 = param0.getHighestSectionPosition() + 16;
      BlockPos.MutableBlockPos var6 = new BlockPos.MutableBlockPos();

      for (int var7 = 0; var7 < 16; var7++) {
         for (int var8 = 0; var8 < 16; var8++) {
            for (Heightmap.Types var10 : param1) {
               var3.add(param0.getOrCreateHeightmapUnprimed(var10));
            }

            for (int var9 = var5 - 1; var9 >= param0.getMinBuildHeight(); var9--) {
               var6.set(var7, var9, var8);
               BlockState var10 = param0.getBlockState(var6);
               if (!var10.is(Blocks.AIR)) {
                  while (var4.hasNext()) {
                     Heightmap var11 = var4.next();
                     if (var11.isOpaque.test(var10)) {
                        var11.setHeight(var7, var8, var9 + 1);
                        var4.remove();
                     }
                  }

                  if (var3.isEmpty()) {
                     break;
                  }

                  var4.back(var2);
               }
            }
         }
      }
   }

   public boolean update(int param0, int param1, int param2, BlockState param3) {
      int var5 = this.getFirstAvailable(param0, param2);
      if (param1 <= var5 - 2) {
         return false;
      }

      if (this.isOpaque.test(param3)) {
         if (param1 >= var5) {
            this.setHeight(param0, param2, param1 + 1);
            return true;
         }
      } else if (var5 - 1 == param1) {
         BlockPos.MutableBlockPos var6 = new BlockPos.MutableBlockPos();

         for (int var7 = param1 - 1; var7 >= this.chunk.getMinBuildHeight(); var7--) {
            var6.set(param0, var7, param2);
            if (this.isOpaque.test(this.chunk.getBlockState(var6))) {
               this.setHeight(param0, param2, var7 + 1);
               return true;
            }
         }

         this.setHeight(param0, param2, this.chunk.getMinBuildHeight());
         return true;
      }

      return false;
   }

   public int getFirstAvailable(int param0, int param1) {
      return this.getFirstAvailable(getIndex(param0, param1));
   }

   public int getHighestTaken(int param0, int param1) {
      return this.getFirstAvailable(getIndex(param0, param1)) - 1;
   }

   private int getFirstAvailable(int param0) {
      return this.data.get(param0) + this.chunk.getMinBuildHeight();
   }

   private void setHeight(int param0, int param1, int param2) {
      this.data.set(getIndex(param0, param1), param2 - this.chunk.getMinBuildHeight());
   }

   public void setRawData(ChunkAccess param0, Heightmap.Types param1, long[] param2) {
      long[] var4 = this.data.getRaw();
      if (var4.length == param2.length) {
         System.arraycopy(param2, 0, var4, 0, param2.length);
      } else {
         LOGGER.warn("Ignoring heightmap data for chunk " + param0.getPos() + ", size does not match; expected: " + var4.length + ", got: " + param2.length);
         primeHeightmaps(param0, EnumSet.of(param1));
      }
   }

   public long[] getRawData() {
      return this.data.getRaw();
   }

   private static int getIndex(int param0, int param1) {
      return param0 + param1 * 16;
   }

   public enum Types implements StringRepresentable {
      WORLD_SURFACE_WG("WORLD_SURFACE_WG", Heightmap.Usage.WORLDGEN, Heightmap.NOT_AIR),
      WORLD_SURFACE("WORLD_SURFACE", Heightmap.Usage.CLIENT, Heightmap.NOT_AIR),
      OCEAN_FLOOR_WG("OCEAN_FLOOR_WG", Heightmap.Usage.WORLDGEN, Heightmap.MATERIAL_MOTION_BLOCKING),
      OCEAN_FLOOR("OCEAN_FLOOR", Heightmap.Usage.LIVE_WORLD, Heightmap.MATERIAL_MOTION_BLOCKING),
      MOTION_BLOCKING("MOTION_BLOCKING", Heightmap.Usage.CLIENT, param0 -> param0.getMaterial().blocksMotion() || !param0.getFluidState().isEmpty()),
      MOTION_BLOCKING_NO_LEAVES(
         "MOTION_BLOCKING_NO_LEAVES",
         Heightmap.Usage.LIVE_WORLD,
         param0 -> (param0.getMaterial().blocksMotion() || !param0.getFluidState().isEmpty()) && !(param0.getBlock() instanceof LeavesBlock)
      );

      public static final Codec<Heightmap.Types> CODEC = StringRepresentable.fromEnum(Heightmap.Types::values);
      private final String serializationKey;
      private final Heightmap.Usage usage;
      private final Predicate<BlockState> isOpaque;

      Types(String param2, Heightmap.Usage param3, Predicate<BlockState> param4) {
         this.serializationKey = param2;
         this.usage = param3;
         this.isOpaque = param4;
      }

      public String getSerializationKey() {
         return this.serializationKey;
      }

      public boolean sendToClient() {
         return this.usage == Heightmap.Usage.CLIENT;
      }

      public boolean keepAfterWorldgen() {
         return this.usage != Heightmap.Usage.WORLDGEN;
      }

      public Predicate<BlockState> isOpaque() {
         return this.isOpaque;
      }

      @Override
      public String getSerializedName() {
         return this.serializationKey;
      }
   }

   public enum Usage {
      WORLDGEN,
      LIVE_WORLD,
      CLIENT;
   }
}
