package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ChiseledBookShelfBlock extends BaseEntityBlock {
   private static final int MAX_BOOKS_IN_STORAGE = 6;
   public static final int BOOKS_PER_ROW = 3;
   public static final List<BooleanProperty> SLOT_OCCUPIED_PROPERTIES = List.of(
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_0_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_1_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_2_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_3_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_4_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_5_OCCUPIED
   );

   public ChiseledBookShelfBlock(BlockBehaviour.Properties param0) {
      super(param0);
      BlockState var2 = this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);

      for (BooleanProperty var4 : SLOT_OCCUPIED_PROPERTIES) {
         var2 = var2.setValue(var4, false);
      }

      this.registerDefaultState(var2);
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param1.getBlockEntity(param2) instanceof ChiseledBookShelfBlockEntity var7) {
         Optional<Vec2> var8 = getRelativeHitCoordinatesForBlockFace(param5, param0.getValue(HorizontalDirectionalBlock.FACING));
         if (var8.isEmpty()) {
            return InteractionResult.PASS;
         } else {
            int var9 = getHitSlot(var8.get());
            if (param0.getValue(SLOT_OCCUPIED_PROPERTIES.get(var9))) {
               removeBook(param1, param2, param3, var7, var9);
               return InteractionResult.sidedSuccess(param1.isClientSide);
            } else {
               ItemStack var10 = param3.getItemInHand(param4);
               if (var10.is(ItemTags.BOOKSHELF_BOOKS)) {
                  addBook(param1, param2, param3, var7, var10, var9);
                  return InteractionResult.sidedSuccess(param1.isClientSide);
               } else {
                  return InteractionResult.CONSUME;
               }
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private static Optional<Vec2> getRelativeHitCoordinatesForBlockFace(BlockHitResult param0, Direction param1) {
      Direction var2 = param0.getDirection();
      if (param1 != var2) {
         return Optional.empty();
      }

      BlockPos var3 = param0.getBlockPos().relative(var2);
      Vec3 var4 = param0.getLocation().subtract(var3.getX(), var3.getY(), var3.getZ());
      double var5 = var4.x();
      double var7 = var4.y();
      double var9 = var4.z();

      return switch (var2) {
         case NORTH -> Optional.of(new Vec2((float)(1.0 - var5), (float)var7));
         case SOUTH -> Optional.of(new Vec2((float)var5, (float)var7));
         case WEST -> Optional.of(new Vec2((float)var9, (float)var7));
         case EAST -> Optional.of(new Vec2((float)(1.0 - var9), (float)var7));
         case DOWN, UP -> Optional.empty();
      };
   }

   private static int getHitSlot(Vec2 param0) {
      int var1 = param0.y >= 0.5F ? 0 : 1;
      int var2 = getSection(param0.x);
      return var2 + var1 * 3;
   }

   private static int getSection(float param0) {
      float var1 = 0.0625F;
      float var2 = 0.375F;
      if (param0 < 0.375F) {
         return 0;
      }

      float var3 = 0.6875F;
      return param0 < 0.6875F ? 1 : 2;
   }

   private static void addBook(Level param0, BlockPos param1, Player param2, ChiseledBookShelfBlockEntity param3, ItemStack param4, int param5) {
      if (!param0.isClientSide) {
         param2.awardStat(Stats.ITEM_USED.get(param4.getItem()));
         SoundEvent var6 = param4.is(Items.ENCHANTED_BOOK) ? SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED : SoundEvents.CHISELED_BOOKSHELF_INSERT;
         param3.setItem(param5, param4.split(1));
         param0.playSound((Player)null, param1, var6, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (param2.isCreative()) {
            param4.grow(1);
         }

         param0.gameEvent(param2, GameEvent.BLOCK_CHANGE, param1);
      }
   }

   private static void removeBook(Level param0, BlockPos param1, Player param2, ChiseledBookShelfBlockEntity param3, int param4) {
      if (!param0.isClientSide) {
         ItemStack var5 = param3.removeItem(param4, 1);
         SoundEvent var6 = var5.is(Items.ENCHANTED_BOOK) ? SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED : SoundEvents.CHISELED_BOOKSHELF_PICKUP;
         param0.playSound((Player)null, param1, var6, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (!param2.getInventory().add(var5)) {
            param2.drop(var5, false);
         }

         param0.gameEvent(param2, GameEvent.BLOCK_CHANGE, param1);
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new ChiseledBookShelfBlockEntity(param0, param1);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(HorizontalDirectionalBlock.FACING);
      SLOT_OCCUPIED_PROPERTIES.forEach(l_param1 -> param0.add(l_param1));
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param0.is(param3.getBlock())) {
         if (param1.getBlockEntity(param2) instanceof ChiseledBookShelfBlockEntity var7 && !var7.isEmpty()) {
            for (int var8 = 0; var8 < 6; var8++) {
               ItemStack var9 = var7.getItem(var8);
               if (!var9.isEmpty()) {
                  Containers.dropItemStack(param1, param2.getX(), param2.getY(), param2.getZ(), var9);
               }
            }

            var7.clearContent();
            param1.updateNeighbourForOutputSignal(param2, this);
         }

         super.onRemove(param0, param1, param2, param3, param4);
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, param0.getHorizontalDirection().getOpposite());
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState param0) {
      return true;
   }

   @Override
   public int getAnalogOutputSignal(BlockState param0, Level param1, BlockPos param2) {
      if (param1.isClientSide()) {
         return 0;
      } else {
         return param1.getBlockEntity(param2) instanceof ChiseledBookShelfBlockEntity var4 ? var4.getLastInteractedSlot() + 1 : 0;
      }
   }
}
