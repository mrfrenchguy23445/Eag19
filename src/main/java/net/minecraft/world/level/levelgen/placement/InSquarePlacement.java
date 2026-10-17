package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public class InSquarePlacement extends PlacementModifier {
   private static final InSquarePlacement INSTANCE = new InSquarePlacement();
   public static final Codec<InSquarePlacement> CODEC = Codec.unit(() -> INSTANCE);

   public static InSquarePlacement spread() {
      return INSTANCE;
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext param0, RandomSource param1, BlockPos param2) {
      int var4 = param1.nextInt(16) + param2.getX();
      int var5 = param1.nextInt(16) + param2.getZ();
      return Stream.of(new BlockPos(var4, param2.getY(), var5));
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.IN_SQUARE;
   }
}
