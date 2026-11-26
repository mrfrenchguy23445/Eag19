package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class RandomizedIntStateProvider extends BlockStateProvider {
   public static final Codec<RandomizedIntStateProvider> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            BlockStateProvider.CODEC.fieldOf("source").forGetter(l_param0 -> l_param0.source),
            Codec.STRING.fieldOf("property").forGetter(l_param0 -> l_param0.propertyName),
            IntProvider.CODEC.fieldOf("values").forGetter(l_param0 -> l_param0.values)
         )
         .apply(param0, RandomizedIntStateProvider::new)
   );
   private final BlockStateProvider source;
   private final String propertyName;
   @Nullable
   private IntegerProperty property;
   private final IntProvider values;

   public RandomizedIntStateProvider(BlockStateProvider param0, IntegerProperty param1, IntProvider param2) {
      this.source = param0;
      this.property = param1;
      this.propertyName = param1.getName();
      this.values = param2;
      Collection<Integer> var4 = param1.getPossibleValues();

      for (int var5 = param2.getMinValue(); var5 <= param2.getMaxValue(); var5++) {
         if (!var4.contains(var5)) {
            throw new IllegalArgumentException("Property value out of range: " + param1.getName() + ": " + var5);
         }
      }
   }

   public RandomizedIntStateProvider(BlockStateProvider param0, String param1, IntProvider param2) {
      this.source = param0;
      this.propertyName = param1;
      this.values = param2;
   }

   @Override
   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.RANDOMIZED_INT_STATE_PROVIDER;
   }

   @Override
   public BlockState getState(RandomSource param0, BlockPos param1) {
      BlockState var3 = this.source.getState(param0, param1);
      if (this.property == null || !var3.hasProperty(this.property)) {
         this.property = findProperty(var3, this.propertyName);
      }

      return var3.setValue(this.property, this.values.sample(param0));
   }

   private static IntegerProperty findProperty(BlockState param0, String param1) {
      Collection<Property<?>> var2 = param0.getProperties();
      Optional<IntegerProperty> var3 = var2.stream()
         .filter(l_param1 -> l_param1.getName().equals(param1))
         .filter(l_param0 -> l_param0 instanceof IntegerProperty)
         .map(l_param0 -> (IntegerProperty)l_param0)
         .findAny();
      return var3.orElseThrow(() -> new IllegalArgumentException("Illegal property: " + param1));
   }
}
