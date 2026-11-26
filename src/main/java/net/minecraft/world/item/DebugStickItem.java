package net.minecraft.world.item;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class DebugStickItem extends Item {
   public DebugStickItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public boolean isFoil(ItemStack param0) {
      return true;
   }

   @Override
   public boolean canAttackBlock(BlockState param0, Level param1, BlockPos param2, Player param3) {
      if (!param1.isClientSide) {
         this.handleInteraction(param3, param0, param1, param2, false, param3.getItemInHand(InteractionHand.MAIN_HAND));
      }

      return false;
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Player var2 = param0.getPlayer();
      Level var3 = param0.getLevel();
      if (!var3.isClientSide && var2 != null) {
         BlockPos var4 = param0.getClickedPos();
         if (!this.handleInteraction(var2, var3.getBlockState(var4), var3, var4, true, param0.getItemInHand())) {
            return InteractionResult.FAIL;
         }
      }

      return InteractionResult.sidedSuccess(var3.isClientSide);
   }

   private boolean handleInteraction(Player param0, BlockState param1, LevelAccessor param2, BlockPos param3, boolean param4, ItemStack param5) {
      if (!param0.canUseGameMasterBlocks()) {
         return false;
      }

      Block var7 = param1.getBlock();
      StateDefinition<Block, BlockState> var8 = var7.getStateDefinition();
      Collection<Property<?>> var9 = var8.getProperties();
      String var10 = BuiltInRegistries.BLOCK.getKey(var7).toString();
      if (var9.isEmpty()) {
         message(param0, Component.translatable(this.getDescriptionId() + ".empty", var10));
         return false;
      }

      CompoundTag var11 = param5.getOrCreateTagElement("DebugProperty");
      String var12 = var11.getString(var10);
      Property<?> var13 = var8.getProperty(var12);
      if (param4) {
         if (var13 == null) {
            var13 = var9.iterator().next();
         }

         BlockState var14 = cycleState(param1, var13, param0.isSecondaryUseActive());
         param2.setBlock(param3, var14, 18);
         message(param0, Component.translatable(this.getDescriptionId() + ".update", var13.getName(), getNameHelper(var14, var13)));
      } else {
         var13 = getRelative(var9, var13, param0.isSecondaryUseActive());
         String var14 = var13.getName();
         var11.putString(var10, var14);
         message(param0, Component.translatable(this.getDescriptionId() + ".select", var14, getNameHelper(param1, var13)));
      }

      return true;
   }

   private static <T extends Comparable<T>> BlockState cycleState(BlockState param0, Property<T> param1, boolean param2) {
      return param0.setValue(param1, getRelative(param1.getPossibleValues(), param0.getValue(param1), param2));
   }

   private static <T> T getRelative(Iterable<T> param0, @Nullable T param1, boolean param2) {
      return param2 ? Util.findPreviousInIterable(param0, param1) : Util.findNextInIterable(param0, param1);
   }

   private static void message(Player param0, Component param1) {
      ((ServerPlayer)param0).sendSystemMessage(param1, true);
   }

   private static <T extends Comparable<T>> String getNameHelper(BlockState param0, Property<T> param1) {
      return param1.getName(param0.getValue(param1));
   }
}
