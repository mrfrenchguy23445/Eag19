package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.StemGrownBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.SavedTick;
import org.slf4j.Logger;

public class UpgradeData {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final UpgradeData EMPTY = new UpgradeData(EmptyBlockGetter.INSTANCE);
   private static final String TAG_INDICES = "Indices";
   private static final Direction8[] DIRECTIONS = Direction8.values();
   private final EnumSet<Direction8> sides = EnumSet.noneOf(Direction8.class);
   private final List<SavedTick<Block>> neighborBlockTicks = Lists.newArrayList();
   private final List<SavedTick<Fluid>> neighborFluidTicks = Lists.newArrayList();
   private final int[][] index;
   static final Map<Block, UpgradeData.BlockFixer> MAP = new IdentityHashMap<>();
   static final Set<UpgradeData.BlockFixer> CHUNKY_FIXERS = Sets.newHashSet();

   private UpgradeData(LevelHeightAccessor param0) {
      this.index = new int[param0.getSectionsCount()][];
   }

   public UpgradeData(CompoundTag param0, LevelHeightAccessor param1) {
      this(param1);
      if (param0.contains("Indices", 10)) {
         CompoundTag var3 = param0.getCompound("Indices");

         for (int var4 = 0; var4 < this.index.length; var4++) {
            String var5 = String.valueOf(var4);
            if (var3.contains(var5, 11)) {
               this.index[var4] = var3.getIntArray(var5);
            }
         }
      }

      int var3 = param0.getInt("Sides");

      for (Direction8 var7 : Direction8.values()) {
         if ((var3 & 1 << var7.ordinal()) != 0) {
            this.sides.add(var7);
         }
      }

      loadTicks(
         param0,
         "neighbor_block_ticks",
         l_param0 -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(l_param0)).or(() -> Optional.of(Blocks.AIR)),
         this.neighborBlockTicks
      );
      loadTicks(
         param0,
         "neighbor_fluid_ticks",
         l_param0 -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(l_param0)).or(() -> Optional.of(Fluids.EMPTY)),
         this.neighborFluidTicks
      );
   }

   private static <T> void loadTicks(CompoundTag param0, String param1, Function<String, Optional<T>> param2, List<SavedTick<T>> param3) {
      if (param0.contains(param1, 9)) {
         for (Tag var6 : param0.getList(param1, 10)) {
            SavedTick.loadTick((CompoundTag)var6, param2).ifPresent(param3::add);
         }
      }
   }

   public void upgrade(LevelChunk param0) {
      this.upgradeInside(param0);

      for (Direction8 var5 : DIRECTIONS) {
         upgradeSides(param0, var5);
      }

      Level var2 = param0.getLevel();
      this.neighborBlockTicks.forEach(l_param1 -> {
         Block l_var2 = l_param1.type() == Blocks.AIR ? var2.getBlockState(l_param1.pos()).getBlock() : l_param1.type();
         var2.scheduleTick(l_param1.pos(), l_var2, l_param1.delay(), l_param1.priority());
      });
      this.neighborFluidTicks.forEach(l_param1 -> {
         Fluid l_var2 = l_param1.type() == Fluids.EMPTY ? var2.getFluidState(l_param1.pos()).getType() : l_param1.type();
         var2.scheduleTick(l_param1.pos(), l_var2, l_param1.delay(), l_param1.priority());
      });
      CHUNKY_FIXERS.forEach(l_param1 -> l_param1.processChunk(var2));
   }

   private static void upgradeSides(LevelChunk param0, Direction8 param1) {
      Level var2 = param0.getLevel();
      if (param0.getUpgradeData().sides.remove(param1)) {
         Set<Direction> var3 = param1.getDirections();
         int var4 = 0;
         int var5 = 15;
         boolean var6 = var3.contains(Direction.EAST);
         boolean var7 = var3.contains(Direction.WEST);
         boolean var8 = var3.contains(Direction.SOUTH);
         boolean var9 = var3.contains(Direction.NORTH);
         boolean var10 = var3.size() == 1;
         ChunkPos var11 = param0.getPos();
         int var12 = var11.getMinBlockX() + (!var10 || !var9 && !var8 ? (var7 ? 0 : 15) : 1);
         int var13 = var11.getMinBlockX() + (!var10 || !var9 && !var8 ? (var7 ? 0 : 15) : 14);
         int var14 = var11.getMinBlockZ() + (!var10 || !var6 && !var7 ? (var9 ? 0 : 15) : 1);
         int var15 = var11.getMinBlockZ() + (!var10 || !var6 && !var7 ? (var9 ? 0 : 15) : 14);
         Direction[] var16 = Direction.values();
         BlockPos.MutableBlockPos var17 = new BlockPos.MutableBlockPos();

         for (BlockPos var19 : BlockPos.betweenClosed(var12, var2.getMinBuildHeight(), var14, var13, var2.getMaxBuildHeight() - 1, var15)) {
            BlockState var20 = var2.getBlockState(var19);
            BlockState var21 = var20;

            for (Direction var25 : var16) {
               var17.setWithOffset(var19, var25);
               var21 = updateState(var21, var25, var2, var19, var17);
            }

            Block.updateOrDestroy(var20, var21, var2, var19, 18);
         }
      }
   }

   private static BlockState updateState(BlockState param0, Direction param1, LevelAccessor param2, BlockPos param3, BlockPos param4) {
      return MAP.getOrDefault(param0.getBlock(), UpgradeData.BlockFixers.DEFAULT)
         .updateShape(param0, param1, param2.getBlockState(param4), param2, param3, param4);
   }

   private void upgradeInside(LevelChunk param0) {
      BlockPos.MutableBlockPos var2 = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos var3 = new BlockPos.MutableBlockPos();
      ChunkPos var4 = param0.getPos();
      LevelAccessor var5 = param0.getLevel();

      for (int var6 = 0; var6 < this.index.length; var6++) {
         LevelChunkSection var7 = param0.getSection(var6);
         int[] var8 = this.index[var6];
         this.index[var6] = null;
         if (var8 != null && var8.length > 0) {
            Direction[] var9 = Direction.values();
            PalettedContainer<BlockState> var10 = var7.getStates();

            for (int var14 : var8) {
               int var15 = var14 & 15;
               int var16 = var14 >> 8 & 15;
               int var17 = var14 >> 4 & 15;
               var2.set(var4.getMinBlockX() + var15, var7.bottomBlockY() + var16, var4.getMinBlockZ() + var17);
               BlockState var18 = var10.get(var14);
               BlockState var19 = var18;

               for (Direction var23 : var9) {
                  var3.setWithOffset(var2, var23);
                  if (SectionPos.blockToSectionCoord(var2.getX()) == var4.x && SectionPos.blockToSectionCoord(var2.getZ()) == var4.z) {
                     var19 = updateState(var19, var23, var5, var2, var3);
                  }
               }

               Block.updateOrDestroy(var18, var19, var5, var2, 18);
            }
         }
      }

      for (int var6 = 0; var6 < this.index.length; var6++) {
         if (this.index[var6] != null) {
            LOGGER.warn("Discarding update data for section {} for chunk ({} {})", var5.getSectionYFromSectionIndex(var6), var4.x, var4.z);
         }

         this.index[var6] = null;
      }
   }

   public boolean isEmpty() {
      for (int[] var4 : this.index) {
         if (var4 != null) {
            return false;
         }
      }

      return this.sides.isEmpty();
   }

   public CompoundTag write() {
      CompoundTag var1 = new CompoundTag();
      CompoundTag var2 = new CompoundTag();

      for (int var3 = 0; var3 < this.index.length; var3++) {
         String var4 = String.valueOf(var3);
         if (this.index[var3] != null && this.index[var3].length != 0) {
            var2.putIntArray(var4, this.index[var3]);
         }
      }

      if (!var2.isEmpty()) {
         var1.put("Indices", var2);
      }

      int var3 = 0;

      for (Direction8 var5 : this.sides) {
         var3 |= 1 << var5.ordinal();
      }

      var1.putByte("Sides", (byte)var3);
      if (!this.neighborBlockTicks.isEmpty()) {
         ListTag var4 = new ListTag();
         this.neighborBlockTicks.forEach(l_param1 -> var4.add(l_param1.save(l2_param0 -> BuiltInRegistries.BLOCK.getKey(l2_param0).toString())));
         var1.put("neighbor_block_ticks", var4);
      }

      if (!this.neighborFluidTicks.isEmpty()) {
         ListTag var4 = new ListTag();
         this.neighborFluidTicks.forEach(l_param1 -> var4.add(l_param1.save(l2_param0 -> BuiltInRegistries.FLUID.getKey(l2_param0).toString())));
         var1.put("neighbor_fluid_ticks", var4);
      }

      return var1;
   }

   public interface BlockFixer {
      BlockState updateShape(BlockState var1, Direction var2, BlockState var3, LevelAccessor var4, BlockPos var5, BlockPos var6);

      default void processChunk(LevelAccessor param0) {
      }
   }

   enum BlockFixers implements UpgradeData.BlockFixer {
      BLACKLIST(
         Blocks.OBSERVER,
         Blocks.NETHER_PORTAL,
         Blocks.WHITE_CONCRETE_POWDER,
         Blocks.ORANGE_CONCRETE_POWDER,
         Blocks.MAGENTA_CONCRETE_POWDER,
         Blocks.LIGHT_BLUE_CONCRETE_POWDER,
         Blocks.YELLOW_CONCRETE_POWDER,
         Blocks.LIME_CONCRETE_POWDER,
         Blocks.PINK_CONCRETE_POWDER,
         Blocks.GRAY_CONCRETE_POWDER,
         Blocks.LIGHT_GRAY_CONCRETE_POWDER,
         Blocks.CYAN_CONCRETE_POWDER,
         Blocks.PURPLE_CONCRETE_POWDER,
         Blocks.BLUE_CONCRETE_POWDER,
         Blocks.BROWN_CONCRETE_POWDER,
         Blocks.GREEN_CONCRETE_POWDER,
         Blocks.RED_CONCRETE_POWDER,
         Blocks.BLACK_CONCRETE_POWDER,
         Blocks.ANVIL,
         Blocks.CHIPPED_ANVIL,
         Blocks.DAMAGED_ANVIL,
         Blocks.DRAGON_EGG,
         Blocks.GRAVEL,
         Blocks.SAND,
         Blocks.RED_SAND,
         Blocks.OAK_SIGN,
         Blocks.SPRUCE_SIGN,
         Blocks.BIRCH_SIGN,
         Blocks.ACACIA_SIGN,
         Blocks.CHERRY_SIGN,
         Blocks.JUNGLE_SIGN,
         Blocks.DARK_OAK_SIGN,
         Blocks.OAK_WALL_SIGN,
         Blocks.SPRUCE_WALL_SIGN,
         Blocks.BIRCH_WALL_SIGN,
         Blocks.ACACIA_WALL_SIGN,
         Blocks.JUNGLE_WALL_SIGN,
         Blocks.DARK_OAK_WALL_SIGN,
         Blocks.OAK_HANGING_SIGN,
         Blocks.SPRUCE_HANGING_SIGN,
         Blocks.BIRCH_HANGING_SIGN,
         Blocks.ACACIA_HANGING_SIGN,
         Blocks.JUNGLE_HANGING_SIGN,
         Blocks.DARK_OAK_HANGING_SIGN,
         Blocks.OAK_WALL_HANGING_SIGN,
         Blocks.SPRUCE_WALL_HANGING_SIGN,
         Blocks.BIRCH_WALL_HANGING_SIGN,
         Blocks.ACACIA_WALL_HANGING_SIGN,
         Blocks.JUNGLE_WALL_HANGING_SIGN,
         Blocks.DARK_OAK_WALL_HANGING_SIGN
      ) {
         @Override
         public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
            return param0;
         }
      },
      DEFAULT {
         @Override
         public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
            return param0.updateShape(param1, param3.getBlockState(param5), param3, param4, param5);
         }
      },
      CHEST(Blocks.CHEST, Blocks.TRAPPED_CHEST) {
         @Override
         public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
            if (param2.is(param0.getBlock())
               && param1.getAxis().isHorizontal()
               && param0.getValue(ChestBlock.TYPE) == ChestType.SINGLE
               && param2.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
               Direction var7 = param0.getValue(ChestBlock.FACING);
               if (param1.getAxis() != var7.getAxis() && var7 == param2.getValue(ChestBlock.FACING)) {
                  ChestType var8 = param1 == var7.getClockWise() ? ChestType.LEFT : ChestType.RIGHT;
                  param3.setBlock(param5, param2.setValue(ChestBlock.TYPE, var8.getOpposite()), 18);
                  if (var7 == Direction.NORTH || var7 == Direction.EAST) {
                     BlockEntity var9 = param3.getBlockEntity(param4);
                     BlockEntity var10 = param3.getBlockEntity(param5);
                     if (var9 instanceof ChestBlockEntity && var10 instanceof ChestBlockEntity) {
                        ChestBlockEntity.swapContents((ChestBlockEntity)var9, (ChestBlockEntity)var10);
                     }
                  }

                  return param0.setValue(ChestBlock.TYPE, var8);
               }
            }

            return param0;
         }
      },
      LEAVES(
         true,
         Blocks.ACACIA_LEAVES,
         Blocks.CHERRY_LEAVES,
         Blocks.BIRCH_LEAVES,
         Blocks.DARK_OAK_LEAVES,
         Blocks.JUNGLE_LEAVES,
         Blocks.OAK_LEAVES,
         Blocks.SPRUCE_LEAVES
      ) {
         private final ThreadLocal<List<ObjectSet<BlockPos>>> queue = ThreadLocal.withInitial(() -> Lists.newArrayListWithCapacity(7));

         @Override
         public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
            BlockState var7 = param0.updateShape(param1, param3.getBlockState(param5), param3, param4, param5);
            if (param0 != var7) {
               int var8 = var7.getValue(BlockStateProperties.DISTANCE);
               List<ObjectSet<BlockPos>> var9 = this.queue.get();
               if (var9.isEmpty()) {
                  for (int var10 = 0; var10 < 7; var10++) {
                     var9.add(new ObjectOpenHashSet<>());
                  }
               }

               var9.get(var8).add(param4.immutable());
            }

            return param0;
         }

         @Override
         public void processChunk(LevelAccessor param0) {
            BlockPos.MutableBlockPos var2 = new BlockPos.MutableBlockPos();
            List<ObjectSet<BlockPos>> var3 = this.queue.get();

            for (int var4 = 2; var4 < var3.size(); var4++) {
               int var5 = var4 - 1;
               ObjectSet<BlockPos> var6 = var3.get(var5);
               ObjectSet<BlockPos> var7 = var3.get(var4);

               for (BlockPos var9 : var6) {
                  BlockState var10 = param0.getBlockState(var9);
                  if (var10.getValue(BlockStateProperties.DISTANCE) >= var5) {
                     param0.setBlock(var9, var10.setValue(BlockStateProperties.DISTANCE, var5), 18);
                     if (var4 != 7) {
                        for (Direction var14 : DIRECTIONS) {
                           var2.setWithOffset(var9, var14);
                           BlockState var15 = param0.getBlockState(var2);
                           if (var15.hasProperty(BlockStateProperties.DISTANCE) && var10.getValue(BlockStateProperties.DISTANCE) > var4) {
                              var7.add(var2.immutable());
                           }
                        }
                     }
                  }
               }
            }

            var3.clear();
         }
      },
      STEM_BLOCK(Blocks.MELON_STEM, Blocks.PUMPKIN_STEM) {
         @Override
         public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
            if (param0.getValue(StemBlock.AGE) == 7) {
               StemGrownBlock var7 = ((StemBlock)param0.getBlock()).getFruit();
               if (param2.is(var7)) {
                  return var7.getAttachedStem().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, param1);
               }
            }

            return param0;
         }
      };

      public static final Direction[] DIRECTIONS = Direction.values();

      BlockFixers(Block... param2) {
         this(false, param2);
      }

      BlockFixers(boolean param2, Block... param3) {
         for (Block var8 : param3) {
            UpgradeData.MAP.put(var8, this);
         }

         if (param2) {
            UpgradeData.CHUNKY_FIXERS.add(this);
         }
      }
   }
}
