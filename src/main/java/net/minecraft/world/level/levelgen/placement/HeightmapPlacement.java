package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public class HeightmapPlacement extends PlacementModifier {
   public static final Codec<HeightmapPlacement> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(Heightmap.Types.CODEC.fieldOf("heightmap").forGetter(l_param0 -> l_param0.heightmap)).apply(param0, HeightmapPlacement::new)
   );
   private final Heightmap.Types heightmap;

   private HeightmapPlacement(Heightmap.Types param0) {
      this.heightmap = param0;
   }

   public static HeightmapPlacement onHeightmap(Heightmap.Types param0) {
      return new HeightmapPlacement(param0);
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext param0, RandomSource param1, BlockPos param2) {
      int var4 = param2.getX();
      int var5 = param2.getZ();
      int var6 = param0.getHeight(this.heightmap, var4, var5);
      return var6 > param0.getMinBuildHeight() ? Stream.of(new BlockPos(var4, var6, var5)) : Stream.of();
   }

   @Override
   public void appendPositions(PlacementContext param0, RandomSource param1, BlockPos param2, List<BlockPos> param3) {
      // Cooperative-port (TeaVM wasm-gc): stream-free; identical semantics.
      int var4 = param2.getX();
      int var5 = param2.getZ();
      int var6 = param0.getHeight(this.heightmap, var4, var5);
      if (var6 > param0.getMinBuildHeight()) {
         param3.add(new BlockPos(var4, var6, var5));
      }
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.HEIGHTMAP;
   }
}
