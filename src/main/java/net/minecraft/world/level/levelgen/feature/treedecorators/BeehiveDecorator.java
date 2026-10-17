package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BeehiveDecorator extends TreeDecorator {
   public static final Codec<BeehiveDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(BeehiveDecorator::new, param0 -> param0.probability)
      .codec();
   private static final Direction WORLDGEN_FACING = Direction.SOUTH;
   private static final Direction[] SPAWN_DIRECTIONS = Direction.Plane.HORIZONTAL
      .stream()
      .filter(param0 -> param0 != WORLDGEN_FACING.getOpposite())
      .toArray(Direction[]::new);
   private final float probability;

   public BeehiveDecorator(float param0) {
      this.probability = param0;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.BEEHIVE;
   }

   @Override
   public void place(TreeDecorator.Context param0) {
      RandomSource var2 = param0.random();
      if (!(var2.nextFloat() >= this.probability)) {
         List<BlockPos> var3 = param0.leaves();
         List<BlockPos> var4 = param0.logs();
         int var5 = !var3.isEmpty()
            ? Math.max(var3.get(0).getY() - 1, var4.get(0).getY() + 1)
            : Math.min(var4.get(0).getY() + 1 + var2.nextInt(3), var4.get(var4.size() - 1).getY());
         List<BlockPos> var6 = var4.stream()
            .filter(l_param1 -> l_param1.getY() == var5)
            .flatMap(l_param0 -> Stream.of(SPAWN_DIRECTIONS).map(l_param0::relative))
            .collect(Collectors.toList());
         if (!var6.isEmpty()) {
            Collections.shuffle(var6);
            Optional<BlockPos> var7 = var6.stream().filter(l_param1 -> param0.isAir(l_param1) && param0.isAir(l_param1.relative(WORLDGEN_FACING))).findFirst();
            if (!var7.isEmpty()) {
               param0.setBlock(var7.get(), Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, WORLDGEN_FACING));
               param0.level().getBlockEntity(var7.get(), BlockEntityType.BEEHIVE).ifPresent(l_param1 -> {
                  int l_var2 = 2 + var2.nextInt(2);

                  for (int l_var3 = 0; l_var3 < l_var2; l_var3++) {
                     CompoundTag l_var4 = new CompoundTag();
                     l_var4.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BEE).toString());
                     l_param1.storeBee(l_var4, var2.nextInt(599), false);
                  }
               });
            }
         }
      }
   }
}
