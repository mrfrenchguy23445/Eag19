package net.minecraft.world.item;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BlockItem extends Item {
   public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
   public static final String BLOCK_STATE_TAG = "BlockStateTag";
   @Deprecated
   private final Block block;

   public BlockItem(Block param0, Item.Properties param1) {
      super(param1);
      this.block = param0;
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      InteractionResult var2 = this.place(new BlockPlaceContext(param0));
      if (!var2.consumesAction() && this.isEdible()) {
         InteractionResult var3 = this.use(param0.getLevel(), param0.getPlayer(), param0.getHand()).getResult();
         return var3 == InteractionResult.CONSUME ? InteractionResult.CONSUME_PARTIAL : var3;
      } else {
         return var2;
      }
   }

   public InteractionResult place(BlockPlaceContext param0) {
      if (!this.getBlock().isEnabled(param0.getLevel().enabledFeatures())) {
         return InteractionResult.FAIL;
      }

      if (!param0.canPlace()) {
         return InteractionResult.FAIL;
      }

      BlockPlaceContext var2 = this.updatePlacementContext(param0);
      if (var2 == null) {
         return InteractionResult.FAIL;
      }

      BlockState var3 = this.getPlacementState(var2);
      if (var3 == null) {
         return InteractionResult.FAIL;
      }

      if (!this.placeBlock(var2, var3)) {
         return InteractionResult.FAIL;
      }

      BlockPos var4 = var2.getClickedPos();
      Level var5 = var2.getLevel();
      Player var6 = var2.getPlayer();
      ItemStack var7 = var2.getItemInHand();
      BlockState var8 = var5.getBlockState(var4);
      if (var8.is(var3.getBlock())) {
         var8 = this.updateBlockStateFromTag(var4, var5, var7, var8);
         this.updateCustomBlockEntityTag(var4, var5, var6, var7, var8);
         var8.getBlock().setPlacedBy(var5, var4, var8, var6, var7);
         if (var6 instanceof ServerPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)var6, var4, var7);
         }
      }

      SoundType var9 = var8.getSoundType();
      var5.playSound(var6, var4, this.getPlaceSound(var8), SoundSource.BLOCKS, (var9.getVolume() + 1.0F) / 2.0F, var9.getPitch() * 0.8F);
      var5.gameEvent(GameEvent.BLOCK_PLACE, var4, GameEvent.Context.of(var6, var8));
      if (var6 == null || !var6.getAbilities().instabuild) {
         var7.shrink(1);
      }

      return InteractionResult.sidedSuccess(var5.isClientSide);
   }

   protected SoundEvent getPlaceSound(BlockState param0) {
      return param0.getSoundType().getPlaceSound();
   }

   @Nullable
   public BlockPlaceContext updatePlacementContext(BlockPlaceContext param0) {
      return param0;
   }

   protected boolean updateCustomBlockEntityTag(BlockPos param0, Level param1, @Nullable Player param2, ItemStack param3, BlockState param4) {
      return updateCustomBlockEntityTag(param1, param2, param0, param3);
   }

   @Nullable
   protected BlockState getPlacementState(BlockPlaceContext param0) {
      BlockState var2 = this.getBlock().getStateForPlacement(param0);
      return var2 != null && this.canPlace(param0, var2) ? var2 : null;
   }

   private BlockState updateBlockStateFromTag(BlockPos param0, Level param1, ItemStack param2, BlockState param3) {
      BlockState var5 = param3;
      CompoundTag var6 = param2.getTag();
      if (var6 != null) {
         CompoundTag var7 = var6.getCompound("BlockStateTag");
         StateDefinition<Block, BlockState> var8 = var5.getBlock().getStateDefinition();

         for (String var10 : var7.getAllKeys()) {
            Property<?> var11 = var8.getProperty(var10);
            if (var11 != null) {
               String var12 = var7.get(var10).getAsString();
               var5 = updateState(var5, var11, var12);
            }
         }
      }

      if (var5 != param3) {
         param1.setBlock(param0, var5, 2);
      }

      return var5;
   }

   private static <T extends Comparable<T>> BlockState updateState(BlockState param0, Property<T> param1, String param2) {
      return param1.getValue(param2).map(l_param2 -> param0.setValue(param1, l_param2)).orElse(param0);
   }

   protected boolean canPlace(BlockPlaceContext param0, BlockState param1) {
      Player var3 = param0.getPlayer();
      CollisionContext var4 = var3 == null ? CollisionContext.empty() : CollisionContext.of(var3);
      return (!this.mustSurvive() || param1.canSurvive(param0.getLevel(), param0.getClickedPos()))
         && param0.getLevel().isUnobstructed(param1, param0.getClickedPos(), var4);
   }

   protected boolean mustSurvive() {
      return true;
   }

   protected boolean placeBlock(BlockPlaceContext param0, BlockState param1) {
      return param0.getLevel().setBlock(param0.getClickedPos(), param1, 11);
   }

   public static boolean updateCustomBlockEntityTag(Level param0, @Nullable Player param1, BlockPos param2, ItemStack param3) {
      MinecraftServer var4 = param0.getServer();
      if (var4 == null) {
         return false;
      }

      CompoundTag var5 = getBlockEntityData(param3);
      if (var5 != null) {
         BlockEntity var6 = param0.getBlockEntity(param2);
         if (var6 != null) {
            if (!param0.isClientSide && var6.onlyOpCanSetNbt() && (param1 == null || !param1.canUseGameMasterBlocks())) {
               return false;
            }

            CompoundTag var7 = var6.saveWithoutMetadata();
            CompoundTag var8 = var7.copy();
            var7.merge(var5);
            if (!var7.equals(var8)) {
               var6.load(var7);
               var6.setChanged();
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public String getDescriptionId() {
      return this.getBlock().getDescriptionId();
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      super.appendHoverText(param0, param1, param2, param3);
      this.getBlock().appendHoverText(param0, param1, param2, param3);
   }

   public Block getBlock() {
      return this.block;
   }

   public void registerBlocks(Map<Block, Item> param0, Item param1) {
      param0.put(this.getBlock(), param1);
   }

   @Override
   public boolean canFitInsideContainerItems() {
      return !(this.block instanceof ShulkerBoxBlock);
   }

   @Override
   public void onDestroyed(ItemEntity param0) {
      if (this.block instanceof ShulkerBoxBlock) {
         ItemStack var2 = param0.getItem();
         CompoundTag var3 = getBlockEntityData(var2);
         if (var3 != null && var3.contains("Items", 9)) {
            ListTag var4 = var3.getList("Items", 10);
            ItemUtils.onContainerDestroyed(param0, var4.stream().map(CompoundTag.class::cast).map(ItemStack::of));
         }
      }
   }

   @Nullable
   public static CompoundTag getBlockEntityData(ItemStack param0) {
      return param0.getTagElement("BlockEntityTag");
   }

   public static void setBlockEntityData(ItemStack param0, BlockEntityType<?> param1, CompoundTag param2) {
      if (param2.isEmpty()) {
         param0.removeTagKey("BlockEntityTag");
      } else {
         BlockEntity.addEntityType(param2, param1);
         param0.addTagElement("BlockEntityTag", param2);
      }
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.getBlock().requiredFeatures();
   }
}
