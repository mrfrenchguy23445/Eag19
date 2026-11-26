package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnerBlock extends BaseEntityBlock {
   protected SpawnerBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new SpawnerBlockEntity(param0, param1);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      return createTickerHelper(param2, BlockEntityType.MOB_SPAWNER, param0.isClientSide ? SpawnerBlockEntity::clientTick : SpawnerBlockEntity::serverTick);
   }

   @Override
   public void spawnAfterBreak(BlockState param0, ServerLevel param1, BlockPos param2, ItemStack param3, boolean param4) {
      super.spawnAfterBreak(param0, param1, param2, param3, param4);
      if (param4) {
         int var6 = 15 + param1.random.nextInt(15) + param1.random.nextInt(15);
         this.popExperience(param1, param2, var6);
      }
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable BlockGetter param1, List<Component> param2, TooltipFlag param3) {
      super.appendHoverText(param0, param1, param2, param3);
      Optional<Component> var5 = this.getSpawnEntityDisplayName(param0);
      if (var5.isPresent()) {
         param2.add(var5.get());
      } else {
         param2.add(CommonComponents.EMPTY);
         param2.add(Component.translatable("block.minecraft.spawner.desc1").withStyle(ChatFormatting.GRAY));
         param2.add(CommonComponents.space().append(Component.translatable("block.minecraft.spawner.desc2").withStyle(ChatFormatting.BLUE)));
      }
   }

   private Optional<Component> getSpawnEntityDisplayName(ItemStack param0) {
      CompoundTag var2 = BlockItem.getBlockEntityData(param0);
      if (var2 != null && var2.contains("SpawnData", 10)) {
         String var3 = var2.getCompound("SpawnData").getCompound("entity").getString("id");
         ResourceLocation var4 = ResourceLocation.tryParse(var3);
         if (var4 != null) {
            return BuiltInRegistries.ENTITY_TYPE
               .getOptional(var4)
               .map(l_param0 -> Component.translatable(l_param0.getDescriptionId()).withStyle(ChatFormatting.GRAY));
         }
      }

      return Optional.empty();
   }
}
