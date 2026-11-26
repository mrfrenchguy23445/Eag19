package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ComposterBlock extends Block implements WorldlyContainerHolder {
   public static final int READY = 8;
   public static final int MIN_LEVEL = 0;
   public static final int MAX_LEVEL = 7;
   public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_COMPOSTER;
   public static final Object2FloatMap<ItemLike> COMPOSTABLES = new Object2FloatOpenHashMap<>();
   private static final int AABB_SIDE_THICKNESS = 2;
   private static final VoxelShape OUTER_SHAPE = Shapes.block();
   private static final VoxelShape[] SHAPES = Util.make(new VoxelShape[9], param0 -> {
      for (int var1 = 0; var1 < 8; var1++) {
         param0[var1] = Shapes.join(OUTER_SHAPE, Block.box(2.0, Math.max(2, 1 + var1 * 2), 2.0, 14.0, 16.0, 14.0), BooleanOp.ONLY_FIRST);
      }

      param0[8] = param0[7];
   });

   public static void bootStrap() {
      COMPOSTABLES.defaultReturnValue(-1.0F);
      float var0 = 0.3F;
      float var1 = 0.5F;
      float var2 = 0.65F;
      float var3 = 0.85F;
      float var4 = 1.0F;
      add(0.3F, Items.JUNGLE_LEAVES);
      add(0.3F, Items.OAK_LEAVES);
      add(0.3F, Items.SPRUCE_LEAVES);
      add(0.3F, Items.DARK_OAK_LEAVES);
      add(0.3F, Items.ACACIA_LEAVES);
      add(0.3F, Items.CHERRY_LEAVES);
      add(0.3F, Items.BIRCH_LEAVES);
      add(0.3F, Items.AZALEA_LEAVES);
      add(0.3F, Items.MANGROVE_LEAVES);
      add(0.3F, Items.OAK_SAPLING);
      add(0.3F, Items.SPRUCE_SAPLING);
      add(0.3F, Items.BIRCH_SAPLING);
      add(0.3F, Items.JUNGLE_SAPLING);
      add(0.3F, Items.ACACIA_SAPLING);
      add(0.3F, Items.CHERRY_SAPLING);
      add(0.3F, Items.DARK_OAK_SAPLING);
      add(0.3F, Items.MANGROVE_PROPAGULE);
      add(0.3F, Items.BEETROOT_SEEDS);
      add(0.3F, Items.DRIED_KELP);
      add(0.3F, Items.GRASS);
      add(0.3F, Items.KELP);
      add(0.3F, Items.MELON_SEEDS);
      add(0.3F, Items.PUMPKIN_SEEDS);
      add(0.3F, Items.SEAGRASS);
      add(0.3F, Items.SWEET_BERRIES);
      add(0.3F, Items.GLOW_BERRIES);
      add(0.3F, Items.WHEAT_SEEDS);
      add(0.3F, Items.MOSS_CARPET);
      add(0.3F, Items.PINK_PETALS);
      add(0.3F, Items.SMALL_DRIPLEAF);
      add(0.3F, Items.HANGING_ROOTS);
      add(0.3F, Items.MANGROVE_ROOTS);
      add(0.3F, Items.TORCHFLOWER_SEEDS);
      add(0.5F, Items.DRIED_KELP_BLOCK);
      add(0.5F, Items.TALL_GRASS);
      add(0.5F, Items.FLOWERING_AZALEA_LEAVES);
      add(0.5F, Items.CACTUS);
      add(0.5F, Items.SUGAR_CANE);
      add(0.5F, Items.VINE);
      add(0.5F, Items.NETHER_SPROUTS);
      add(0.5F, Items.WEEPING_VINES);
      add(0.5F, Items.TWISTING_VINES);
      add(0.5F, Items.MELON_SLICE);
      add(0.5F, Items.GLOW_LICHEN);
      add(0.65F, Items.SEA_PICKLE);
      add(0.65F, Items.LILY_PAD);
      add(0.65F, Items.PUMPKIN);
      add(0.65F, Items.CARVED_PUMPKIN);
      add(0.65F, Items.MELON);
      add(0.65F, Items.APPLE);
      add(0.65F, Items.BEETROOT);
      add(0.65F, Items.CARROT);
      add(0.65F, Items.COCOA_BEANS);
      add(0.65F, Items.POTATO);
      add(0.65F, Items.WHEAT);
      add(0.65F, Items.BROWN_MUSHROOM);
      add(0.65F, Items.RED_MUSHROOM);
      add(0.65F, Items.MUSHROOM_STEM);
      add(0.65F, Items.CRIMSON_FUNGUS);
      add(0.65F, Items.WARPED_FUNGUS);
      add(0.65F, Items.NETHER_WART);
      add(0.65F, Items.CRIMSON_ROOTS);
      add(0.65F, Items.WARPED_ROOTS);
      add(0.65F, Items.SHROOMLIGHT);
      add(0.65F, Items.DANDELION);
      add(0.65F, Items.POPPY);
      add(0.65F, Items.BLUE_ORCHID);
      add(0.65F, Items.ALLIUM);
      add(0.65F, Items.AZURE_BLUET);
      add(0.65F, Items.RED_TULIP);
      add(0.65F, Items.ORANGE_TULIP);
      add(0.65F, Items.WHITE_TULIP);
      add(0.65F, Items.PINK_TULIP);
      add(0.65F, Items.OXEYE_DAISY);
      add(0.65F, Items.CORNFLOWER);
      add(0.65F, Items.LILY_OF_THE_VALLEY);
      add(0.65F, Items.WITHER_ROSE);
      add(0.65F, Items.FERN);
      add(0.65F, Items.SUNFLOWER);
      add(0.65F, Items.LILAC);
      add(0.65F, Items.ROSE_BUSH);
      add(0.65F, Items.PEONY);
      add(0.65F, Items.LARGE_FERN);
      add(0.65F, Items.SPORE_BLOSSOM);
      add(0.65F, Items.AZALEA);
      add(0.65F, Items.MOSS_BLOCK);
      add(0.65F, Items.BIG_DRIPLEAF);
      add(0.85F, Items.HAY_BLOCK);
      add(0.85F, Items.BROWN_MUSHROOM_BLOCK);
      add(0.85F, Items.RED_MUSHROOM_BLOCK);
      add(0.85F, Items.NETHER_WART_BLOCK);
      add(0.85F, Items.WARPED_WART_BLOCK);
      add(0.85F, Items.FLOWERING_AZALEA);
      add(0.85F, Items.BREAD);
      add(0.85F, Items.BAKED_POTATO);
      add(0.85F, Items.COOKIE);
      add(0.85F, Items.TORCHFLOWER);
      add(1.0F, Items.CAKE);
      add(1.0F, Items.PUMPKIN_PIE);
   }

   private static void add(float param0, ItemLike param1) {
      COMPOSTABLES.put(param1.asItem(), param0);
   }

   public ComposterBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
   }

   public static void handleFill(Level param0, BlockPos param1, boolean param2) {
      BlockState var3 = param0.getBlockState(param1);
      param0.playLocalSound(param1, param2 ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
      double var4 = var3.getShape(param0, param1).max(Direction.Axis.Y, 0.5, 0.5) + 0.03125;
      double var6 = 0.13125F;
      double var8 = 0.7375F;
      RandomSource var10 = param0.getRandom();

      for (int var11 = 0; var11 < 10; var11++) {
         double var12 = var10.nextGaussian() * 0.02;
         double var14 = var10.nextGaussian() * 0.02;
         double var16 = var10.nextGaussian() * 0.02;
         param0.addParticle(
            ParticleTypes.COMPOSTER,
            param1.getX() + 0.13125F + 0.7375F * var10.nextFloat(),
            param1.getY() + var4 + var10.nextFloat() * (1.0 - var4),
            param1.getZ() + 0.13125F + 0.7375F * var10.nextFloat(),
            var12,
            var14,
            var16
         );
      }
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPES[param0.getValue(LEVEL)];
   }

   @Override
   public VoxelShape getInteractionShape(BlockState param0, BlockGetter param1, BlockPos param2) {
      return OUTER_SHAPE;
   }

   @Override
   public VoxelShape getCollisionShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPES[0];
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (param0.getValue(LEVEL) == 7) {
         param1.scheduleTick(param2, param0.getBlock(), 20);
      }
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      int var7 = param0.getValue(LEVEL);
      ItemStack var8 = param3.getItemInHand(param4);
      if (var7 < 8 && COMPOSTABLES.containsKey(var8.getItem())) {
         if (var7 < 7 && !param1.isClientSide) {
            BlockState var9 = addItem(param3, param0, param1, param2, var8);
            param1.levelEvent(1500, param2, param0 != var9 ? 1 : 0);
            param3.awardStat(Stats.ITEM_USED.get(var8.getItem()));
            if (!param3.getAbilities().instabuild) {
               var8.shrink(1);
            }
         }

         return InteractionResult.sidedSuccess(param1.isClientSide);
      } else if (var7 == 8) {
         extractProduce(param3, param0, param1, param2);
         return InteractionResult.sidedSuccess(param1.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public static BlockState insertItem(Entity param0, BlockState param1, ServerLevel param2, ItemStack param3, BlockPos param4) {
      int var5 = param1.getValue(LEVEL);
      if (var5 < 7 && COMPOSTABLES.containsKey(param3.getItem())) {
         BlockState var6 = addItem(param0, param1, param2, param4, param3);
         param3.shrink(1);
         return var6;
      } else {
         return param1;
      }
   }

   public static BlockState extractProduce(Entity param0, BlockState param1, Level param2, BlockPos param3) {
      if (!param2.isClientSide) {
         Vec3 var4 = Vec3.atLowerCornerWithOffset(param3, 0.5, 1.01, 0.5).offsetRandom(param2.random, 0.7F);
         ItemEntity var5 = new ItemEntity(param2, var4.x(), var4.y(), var4.z(), new ItemStack(Items.BONE_MEAL));
         var5.setDefaultPickUpDelay();
         param2.addFreshEntity(var5);
      }

      BlockState var4 = empty(param0, param1, param2, param3);
      param2.playSound((Player)null, param3, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
      return var4;
   }

   static BlockState empty(@Nullable Entity param0, BlockState param1, LevelAccessor param2, BlockPos param3) {
      BlockState var4 = param1.setValue(LEVEL, 0);
      param2.setBlock(param3, var4, 3);
      param2.gameEvent(GameEvent.BLOCK_CHANGE, param3, GameEvent.Context.of(param0, var4));
      return var4;
   }

   static BlockState addItem(@Nullable Entity param0, BlockState param1, LevelAccessor param2, BlockPos param3, ItemStack param4) {
      int var5 = param1.getValue(LEVEL);
      float var6 = COMPOSTABLES.getFloat(param4.getItem());
      if ((var5 != 0 || !(var6 > 0.0F)) && !(param2.getRandom().nextDouble() < var6)) {
         return param1;
      }

      int var7 = var5 + 1;
      BlockState var8 = param1.setValue(LEVEL, var7);
      param2.setBlock(param3, var8, 3);
      param2.gameEvent(GameEvent.BLOCK_CHANGE, param3, GameEvent.Context.of(param0, var8));
      if (var7 == 7) {
         param2.scheduleTick(param3, param1.getBlock(), 20);
      }

      return var8;
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(LEVEL) == 7) {
         param1.setBlock(param2, param0.cycle(LEVEL), 3);
         param1.playSound((Player)null, param2, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState param0) {
      return true;
   }

   @Override
   public int getAnalogOutputSignal(BlockState param0, Level param1, BlockPos param2) {
      return param0.getValue(LEVEL);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(LEVEL);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   @Override
   public WorldlyContainer getContainer(BlockState param0, LevelAccessor param1, BlockPos param2) {
      int var4 = param0.getValue(LEVEL);
      if (var4 == 8) {
         return new ComposterBlock.OutputContainer(param0, param1, param2, new ItemStack(Items.BONE_MEAL));
      } else {
         return var4 < 7 ? new ComposterBlock.InputContainer(param0, param1, param2) : new ComposterBlock.EmptyContainer();
      }
   }

   static class EmptyContainer extends SimpleContainer implements WorldlyContainer {
      public EmptyContainer() {
         super(0);
      }

      @Override
      public int[] getSlotsForFace(Direction param0) {
         return new int[0];
      }

      @Override
      public boolean canPlaceItemThroughFace(int param0, ItemStack param1, @Nullable Direction param2) {
         return false;
      }

      @Override
      public boolean canTakeItemThroughFace(int param0, ItemStack param1, Direction param2) {
         return false;
      }
   }

   static class InputContainer extends SimpleContainer implements WorldlyContainer {
      private final BlockState state;
      private final LevelAccessor level;
      private final BlockPos pos;
      private boolean changed;

      public InputContainer(BlockState param0, LevelAccessor param1, BlockPos param2) {
         super(1);
         this.state = param0;
         this.level = param1;
         this.pos = param2;
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public int[] getSlotsForFace(Direction param0) {
         return param0 == Direction.UP ? new int[]{0} : new int[0];
      }

      @Override
      public boolean canPlaceItemThroughFace(int param0, ItemStack param1, @Nullable Direction param2) {
         return !this.changed && param2 == Direction.UP && ComposterBlock.COMPOSTABLES.containsKey(param1.getItem());
      }

      @Override
      public boolean canTakeItemThroughFace(int param0, ItemStack param1, Direction param2) {
         return false;
      }

      @Override
      public void setChanged() {
         ItemStack var1 = this.getItem(0);
         if (!var1.isEmpty()) {
            this.changed = true;
            BlockState var2 = ComposterBlock.addItem(null, this.state, this.level, this.pos, var1);
            this.level.levelEvent(1500, this.pos, var2 != this.state ? 1 : 0);
            this.removeItemNoUpdate(0);
         }
      }
   }

   static class OutputContainer extends SimpleContainer implements WorldlyContainer {
      private final BlockState state;
      private final LevelAccessor level;
      private final BlockPos pos;
      private boolean changed;

      public OutputContainer(BlockState param0, LevelAccessor param1, BlockPos param2, ItemStack param3) {
         super(param3);
         this.state = param0;
         this.level = param1;
         this.pos = param2;
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public int[] getSlotsForFace(Direction param0) {
         return param0 == Direction.DOWN ? new int[]{0} : new int[0];
      }

      @Override
      public boolean canPlaceItemThroughFace(int param0, ItemStack param1, @Nullable Direction param2) {
         return false;
      }

      @Override
      public boolean canTakeItemThroughFace(int param0, ItemStack param1, Direction param2) {
         return !this.changed && param2 == Direction.DOWN && param1.is(Items.BONE_MEAL);
      }

      @Override
      public void setChanged() {
         ComposterBlock.empty(null, this.state, this.level, this.pos);
         this.changed = true;
      }
   }
}
