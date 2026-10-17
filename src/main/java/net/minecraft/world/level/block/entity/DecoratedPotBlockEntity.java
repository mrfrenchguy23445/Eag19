package net.minecraft.world.level.block.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class DecoratedPotBlockEntity extends BlockEntity {
   private static final String TAG_SHARDS = "shards";
   private static final int SHARDS_IN_POT = 4;
   private boolean isBroken = false;
   private final List<Item> shards = Util.make(new ArrayList<>(4), l_param0 -> {
      l_param0.add(Items.BRICK);
      l_param0.add(Items.BRICK);
      l_param0.add(Items.BRICK);
      l_param0.add(Items.BRICK);
   });

   public DecoratedPotBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.DECORATED_POT, param0, param1);
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      saveShards(this.shards, param0);
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      if (param0.contains("shards", 9)) {
         ListTag var2 = param0.getList("shards", 8);
         this.shards.clear();
         int var3 = Math.min(4, var2.size());

         for (int var4 = 0; var4 < var3; var4++) {
            if (var2.get(var4) instanceof StringTag var5) {
               this.shards.add(BuiltInRegistries.ITEM.get(new ResourceLocation(var5.getAsString())));
            } else {
               this.shards.add(Items.BRICK);
            }
         }

         int var4 = 4 - var3;

         for (int var5 = 0; var5 < var4; var5++) {
            this.shards.add(Items.BRICK);
         }
      }
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public static void saveShards(List<Item> param0, CompoundTag param1) {
      ListTag var2 = new ListTag();

      for (Item var4 : param0) {
         var2.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(var4).toString()));
      }

      param1.put("shards", var2);
   }

   public ItemStack getItem() {
      ItemStack var1 = new ItemStack(Blocks.DECORATED_POT);
      CompoundTag var2 = new CompoundTag();
      saveShards(this.shards, var2);
      BlockItem.setBlockEntityData(var1, BlockEntityType.DECORATED_POT, var2);
      return var1;
   }

   public List<Item> getShards() {
      return this.shards;
   }

   public void playerDestroy(Level param0, BlockPos param1, ItemStack param2, Player param3) {
      if (param3.isCreative()) {
         this.isBroken = true;
      } else {
         if (param2.is(ItemTags.BREAKS_DECORATED_POTS) && !EnchantmentHelper.hasSilkTouch(param2)) {
            List<Item> var5 = this.getShards();
            NonNullList<ItemStack> var6 = NonNullList.createWithCapacity(var5.size());
            var6.addAll(0, var5.stream().map(Item::getDefaultInstance).toList());
            Containers.dropContents(param0, param1, var6);
            this.isBroken = true;
            param0.playSound((Player)null, param1, SoundEvents.DECORATED_POT_SHATTER, SoundSource.PLAYERS, 1.0F, 1.0F);
         }
      }
   }

   public boolean isBroken() {
      return this.isBroken;
   }

   public Direction getDirection() {
      return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
   }

   public void setFromItem(ItemStack param0) {
      CompoundTag var2 = BlockItem.getBlockEntityData(param0);
      if (var2 != null) {
         this.load(var2);
      }
   }
}
