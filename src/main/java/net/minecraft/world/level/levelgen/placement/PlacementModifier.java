package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;

public abstract class PlacementModifier {
   public static final Codec<PlacementModifier> CODEC = BuiltInRegistries.PLACEMENT_MODIFIER_TYPE
      .byNameCodec()
      .dispatch(PlacementModifier::type, PlacementModifierType::codec);

   public abstract Stream<BlockPos> getPositions(PlacementContext var1, RandomSource var2, BlockPos var3);

   /** Cooperative-port optimisation (TeaVM wasm-gc): list-append counterpart of
    * getPositions() used by PlacedFeature's position pipeline. The vanilla stream
    * flatMap chain allocates a pipeline node + spliterator + capturing lambda per
    * modifier per candidate position, which dominates the features stage here.
    * The default bridges to the stream API so any unconverted modifier still works;
    * vanilla placement modifiers override this to stay allocation-free. */
   public void appendPositions(PlacementContext var1, RandomSource var2, BlockPos var3, List<BlockPos> var4) {
      this.getPositions(var1, var2, var3).forEach(var4::add);
   }

   public abstract PlacementModifierType<?> type();
}
