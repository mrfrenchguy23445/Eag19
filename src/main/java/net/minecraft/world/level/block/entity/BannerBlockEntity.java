package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BannerBlockEntity extends BlockEntity implements Nameable {
   public static final int MAX_PATTERNS = 6;
   public static final String TAG_PATTERNS = "Patterns";
   public static final String TAG_PATTERN = "Pattern";
   public static final String TAG_COLOR = "Color";
   @Nullable
   private Component name;
   private DyeColor baseColor;
   @Nullable
   private ListTag itemPatterns;
   @Nullable
   private List<Pair<Holder<BannerPattern>, DyeColor>> patterns;

   public BannerBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.BANNER, param0, param1);
      this.baseColor = ((AbstractBannerBlock)param1.getBlock()).getColor();
   }

   public BannerBlockEntity(BlockPos param0, BlockState param1, DyeColor param2) {
      this(param0, param1);
      this.baseColor = param2;
   }

   @Nullable
   public static ListTag getItemPatterns(ItemStack param0) {
      ListTag var1 = null;
      CompoundTag var2 = BlockItem.getBlockEntityData(param0);
      if (var2 != null && var2.contains("Patterns", 9)) {
         var1 = var2.getList("Patterns", 10).copy();
      }

      return var1;
   }

   public void fromItem(ItemStack param0, DyeColor param1) {
      this.baseColor = param1;
      this.fromItem(param0);
   }

   public void fromItem(ItemStack param0) {
      this.itemPatterns = getItemPatterns(param0);
      this.patterns = null;
      this.name = param0.hasCustomHoverName() ? param0.getHoverName() : null;
   }

   @Override
   public Component getName() {
      return this.name != null ? this.name : Component.translatable("block.minecraft.banner");
   }

   @Nullable
   @Override
   public Component getCustomName() {
      return this.name;
   }

   public void setCustomName(Component param0) {
      this.name = param0;
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      if (this.itemPatterns != null) {
         param0.put("Patterns", this.itemPatterns);
      }

      if (this.name != null) {
         param0.putString("CustomName", Component.Serializer.toJson(this.name));
      }
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      if (param0.contains("CustomName", 8)) {
         this.name = Component.Serializer.fromJson(param0.getString("CustomName"));
      }

      this.itemPatterns = param0.getList("Patterns", 10);
      this.patterns = null;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public static int getPatternCount(ItemStack param0) {
      CompoundTag var1 = BlockItem.getBlockEntityData(param0);
      return var1 != null && var1.contains("Patterns") ? var1.getList("Patterns", 10).size() : 0;
   }

   public List<Pair<Holder<BannerPattern>, DyeColor>> getPatterns() {
      if (this.patterns == null) {
         this.patterns = createPatterns(this.baseColor, this.itemPatterns);
      }

      return this.patterns;
   }

   public static List<Pair<Holder<BannerPattern>, DyeColor>> createPatterns(DyeColor param0, @Nullable ListTag param1) {
      List<Pair<Holder<BannerPattern>, DyeColor>> var2 = Lists.newArrayList();
      var2.add(Pair.of(BuiltInRegistries.BANNER_PATTERN.getHolderOrThrow(BannerPatterns.BASE), param0));
      if (param1 != null) {
         for (int var3 = 0; var3 < param1.size(); var3++) {
            CompoundTag var4 = param1.getCompound(var3);
            Holder<BannerPattern> var5 = BannerPattern.byHash(var4.getString("Pattern"));
            if (var5 != null) {
               int var6 = var4.getInt("Color");
               var2.add(Pair.of(var5, DyeColor.byId(var6)));
            }
         }
      }

      return var2;
   }

   public static void removeLastPattern(ItemStack param0) {
      CompoundTag var1 = BlockItem.getBlockEntityData(param0);
      if (var1 != null && var1.contains("Patterns", 9)) {
         ListTag var2 = var1.getList("Patterns", 10);
         if (!var2.isEmpty()) {
            var2.remove(var2.size() - 1);
            if (var2.isEmpty()) {
               var1.remove("Patterns");
            }

            BlockItem.setBlockEntityData(param0, BlockEntityType.BANNER, var1);
         }
      }
   }

   public ItemStack getItem() {
      ItemStack var1 = new ItemStack(BannerBlock.byColor(this.baseColor));
      if (this.itemPatterns != null && !this.itemPatterns.isEmpty()) {
         CompoundTag var2 = new CompoundTag();
         var2.put("Patterns", this.itemPatterns.copy());
         BlockItem.setBlockEntityData(var1, this.getType(), var2);
      }

      if (this.name != null) {
         var1.setHoverName(this.name);
      }

      return var1;
   }

   public DyeColor getBaseColor() {
      return this.baseColor;
   }
}
