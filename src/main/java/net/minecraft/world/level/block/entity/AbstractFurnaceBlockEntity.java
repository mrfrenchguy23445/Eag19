package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeHolder, StackedContentsCompatible {
   protected static final int SLOT_INPUT = 0;
   protected static final int SLOT_FUEL = 1;
   protected static final int SLOT_RESULT = 2;
   public static final int DATA_LIT_TIME = 0;
   private static final int[] SLOTS_FOR_UP = new int[]{0};
   private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
   private static final int[] SLOTS_FOR_SIDES = new int[]{1};
   public static final int DATA_LIT_DURATION = 1;
   public static final int DATA_COOKING_PROGRESS = 2;
   public static final int DATA_COOKING_TOTAL_TIME = 3;
   public static final int NUM_DATA_VALUES = 4;
   public static final int BURN_TIME_STANDARD = 200;
   public static final int BURN_COOL_SPEED = 2;
   protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
   int litTime;
   int litDuration;
   int cookingProgress;
   int cookingTotalTime;
   protected final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(int param0) {
         switch (param0) {
            case 0:
               return AbstractFurnaceBlockEntity.this.litTime;
            case 1:
               return AbstractFurnaceBlockEntity.this.litDuration;
            case 2:
               return AbstractFurnaceBlockEntity.this.cookingProgress;
            case 3:
               return AbstractFurnaceBlockEntity.this.cookingTotalTime;
            default:
               return 0;
         }
      }

      @Override
      public void set(int param0, int param1) {
         switch (param0) {
            case 0:
               AbstractFurnaceBlockEntity.this.litTime = param1;
               break;
            case 1:
               AbstractFurnaceBlockEntity.this.litDuration = param1;
               break;
            case 2:
               AbstractFurnaceBlockEntity.this.cookingProgress = param1;
               break;
            case 3:
               AbstractFurnaceBlockEntity.this.cookingTotalTime = param1;
         }
      }

      @Override
      public int getCount() {
         return 4;
      }
   };
   private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
   private final RecipeManager.CachedCheck<Container, ? extends AbstractCookingRecipe> quickCheck;

   protected AbstractFurnaceBlockEntity(BlockEntityType<?> param0, BlockPos param1, BlockState param2, RecipeType<? extends AbstractCookingRecipe> param3) {
      super(param0, param1, param2);
      this.quickCheck = RecipeManager.createCheck(param3);
   }

   public static Map<Item, Integer> getFuel() {
      Map<Item, Integer> var0 = Maps.newLinkedHashMap();
      add(var0, Items.LAVA_BUCKET, 20000);
      add(var0, Blocks.COAL_BLOCK, 16000);
      add(var0, Items.BLAZE_ROD, 2400);
      add(var0, Items.COAL, 1600);
      add(var0, Items.CHARCOAL, 1600);
      add(var0, ItemTags.LOGS, 300);
      add(var0, ItemTags.BAMBOO_BLOCKS, 300);
      add(var0, ItemTags.PLANKS, 300);
      add(var0, Blocks.BAMBOO_MOSAIC, 300);
      add(var0, ItemTags.WOODEN_STAIRS, 300);
      add(var0, Blocks.BAMBOO_MOSAIC_STAIRS, 300);
      add(var0, ItemTags.WOODEN_SLABS, 150);
      add(var0, Blocks.BAMBOO_MOSAIC_SLAB, 150);
      add(var0, ItemTags.WOODEN_TRAPDOORS, 300);
      add(var0, ItemTags.WOODEN_PRESSURE_PLATES, 300);
      add(var0, ItemTags.WOODEN_FENCES, 300);
      add(var0, ItemTags.FENCE_GATES, 300);
      add(var0, Blocks.NOTE_BLOCK, 300);
      add(var0, Blocks.BOOKSHELF, 300);
      add(var0, Blocks.CHISELED_BOOKSHELF, 300);
      add(var0, Blocks.LECTERN, 300);
      add(var0, Blocks.JUKEBOX, 300);
      add(var0, Blocks.CHEST, 300);
      add(var0, Blocks.TRAPPED_CHEST, 300);
      add(var0, Blocks.CRAFTING_TABLE, 300);
      add(var0, Blocks.DAYLIGHT_DETECTOR, 300);
      add(var0, ItemTags.BANNERS, 300);
      add(var0, Items.BOW, 300);
      add(var0, Items.FISHING_ROD, 300);
      add(var0, Blocks.LADDER, 300);
      add(var0, ItemTags.SIGNS, 200);
      add(var0, ItemTags.HANGING_SIGNS, 800);
      add(var0, Items.WOODEN_SHOVEL, 200);
      add(var0, Items.WOODEN_SWORD, 200);
      add(var0, Items.WOODEN_HOE, 200);
      add(var0, Items.WOODEN_AXE, 200);
      add(var0, Items.WOODEN_PICKAXE, 200);
      add(var0, ItemTags.WOODEN_DOORS, 200);
      add(var0, ItemTags.BOATS, 1200);
      add(var0, ItemTags.WOOL, 100);
      add(var0, ItemTags.WOODEN_BUTTONS, 100);
      add(var0, Items.STICK, 100);
      add(var0, ItemTags.SAPLINGS, 100);
      add(var0, Items.BOWL, 100);
      add(var0, ItemTags.WOOL_CARPETS, 67);
      add(var0, Blocks.DRIED_KELP_BLOCK, 4001);
      add(var0, Items.CROSSBOW, 300);
      add(var0, Blocks.BAMBOO, 50);
      add(var0, Blocks.DEAD_BUSH, 100);
      add(var0, Blocks.SCAFFOLDING, 50);
      add(var0, Blocks.LOOM, 300);
      add(var0, Blocks.BARREL, 300);
      add(var0, Blocks.CARTOGRAPHY_TABLE, 300);
      add(var0, Blocks.FLETCHING_TABLE, 300);
      add(var0, Blocks.SMITHING_TABLE, 300);
      add(var0, Blocks.COMPOSTER, 300);
      add(var0, Blocks.AZALEA, 100);
      add(var0, Blocks.FLOWERING_AZALEA, 100);
      add(var0, Blocks.MANGROVE_ROOTS, 300);
      return var0;
   }

   private static boolean isNeverAFurnaceFuel(Item param0) {
      return param0.builtInRegistryHolder().is(ItemTags.NON_FLAMMABLE_WOOD);
   }

   private static void add(Map<Item, Integer> param0, TagKey<Item> param1, int param2) {
      for (Holder<Item> var4 : BuiltInRegistries.ITEM.getTagOrEmpty(param1)) {
         if (!isNeverAFurnaceFuel(var4.value())) {
            param0.put(var4.value(), param2);
         }
      }
   }

   private static void add(Map<Item, Integer> param0, ItemLike param1, int param2) {
      Item var3 = param1.asItem();
      if (isNeverAFurnaceFuel(var3)) {
         if (SharedConstants.IS_RUNNING_IN_IDE) {
            throw (IllegalStateException)Util.pauseInIde(
               new IllegalStateException(
                  "A developer tried to explicitly make fire resistant item " + var3.getName(null).getString() + " a furnace fuel. That will not work!"
               )
            );
         }
      } else {
         param0.put(var3, param2);
      }
   }

   private boolean isLit() {
      return this.litTime > 0;
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(param0, this.items);
      this.litTime = param0.getShort("BurnTime");
      this.cookingProgress = param0.getShort("CookTime");
      this.cookingTotalTime = param0.getShort("CookTimeTotal");
      this.litDuration = this.getBurnDuration(this.items.get(1));
      CompoundTag var2 = param0.getCompound("RecipesUsed");

      for (String var4 : var2.getAllKeys()) {
         this.recipesUsed.put(new ResourceLocation(var4), var2.getInt(var4));
      }
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      param0.putShort("BurnTime", (short)this.litTime);
      param0.putShort("CookTime", (short)this.cookingProgress);
      param0.putShort("CookTimeTotal", (short)this.cookingTotalTime);
      ContainerHelper.saveAllItems(param0, this.items);
      CompoundTag var2 = new CompoundTag();
      this.recipesUsed.forEach((l_param1, l_param2) -> var2.putInt(l_param1.toString(), l_param2));
      param0.put("RecipesUsed", var2);
   }

   public static void serverTick(Level param0, BlockPos param1, BlockState param2, AbstractFurnaceBlockEntity param3) {
      boolean var4 = param3.isLit();
      boolean var5 = false;
      if (param3.isLit()) {
         param3.litTime--;
      }

      ItemStack var6 = param3.items.get(1);
      boolean var7 = !param3.items.get(0).isEmpty();
      boolean var8 = !var6.isEmpty();
      if (param3.isLit() || var8 && var7) {
         Recipe<?> var9;
         if (var7) {
            var9 = (Recipe<?>)param3.quickCheck.getRecipeFor(param3, param0).orElse(null);
         } else {
            var9 = null;
         }

         int var10 = param3.getMaxStackSize();
         if (!param3.isLit() && canBurn(param0.registryAccess(), var9, param3.items, var10)) {
            param3.litTime = param3.getBurnDuration(var6);
            param3.litDuration = param3.litTime;
            if (param3.isLit()) {
               var5 = true;
               if (var8) {
                  Item var11 = var6.getItem();
                  var6.shrink(1);
                  if (var6.isEmpty()) {
                     Item var12 = var11.getCraftingRemainingItem();
                     param3.items.set(1, var12 == null ? ItemStack.EMPTY : new ItemStack(var12));
                  }
               }
            }
         }

         if (param3.isLit() && canBurn(param0.registryAccess(), var9, param3.items, var10)) {
            param3.cookingProgress++;
            if (param3.cookingProgress == param3.cookingTotalTime) {
               param3.cookingProgress = 0;
               param3.cookingTotalTime = getTotalCookTime(param0, param3);
               if (burn(param0.registryAccess(), var9, param3.items, var10)) {
                  param3.setRecipeUsed(var9);
               }

               var5 = true;
            }
         } else {
            param3.cookingProgress = 0;
         }
      } else if (!param3.isLit() && param3.cookingProgress > 0) {
         param3.cookingProgress = Mth.clamp(param3.cookingProgress - 2, 0, param3.cookingTotalTime);
      }

      if (var4 != param3.isLit()) {
         var5 = true;
         param2 = param2.setValue(AbstractFurnaceBlock.LIT, param3.isLit());
         param0.setBlock(param1, param2, 3);
      }

      if (var5) {
         setChanged(param0, param1, param2);
      }
   }

   private static boolean canBurn(RegistryAccess param0, @Nullable Recipe<?> param1, NonNullList<ItemStack> param2, int param3) {
      if (!param2.get(0).isEmpty() && param1 != null) {
         ItemStack var4 = param1.getResultItem(param0);
         if (var4.isEmpty()) {
            return false;
         } else {
            ItemStack var5 = param2.get(2);
            if (var5.isEmpty()) {
               return true;
            } else if (!var5.sameItem(var4)) {
               return false;
            } else {
               return var5.getCount() < param3 && var5.getCount() < var5.getMaxStackSize() ? true : var5.getCount() < var4.getMaxStackSize();
            }
         }
      } else {
         return false;
      }
   }

   private static boolean burn(RegistryAccess param0, @Nullable Recipe<?> param1, NonNullList<ItemStack> param2, int param3) {
      if (param1 != null && canBurn(param0, param1, param2, param3)) {
         ItemStack var4 = param2.get(0);
         ItemStack var5 = param1.getResultItem(param0);
         ItemStack var6 = param2.get(2);
         if (var6.isEmpty()) {
            param2.set(2, var5.copy());
         } else if (var6.is(var5.getItem())) {
            var6.grow(1);
         }

         if (var4.is(Blocks.WET_SPONGE.asItem()) && !param2.get(1).isEmpty() && param2.get(1).is(Items.BUCKET)) {
            param2.set(1, new ItemStack(Items.WATER_BUCKET));
         }

         var4.shrink(1);
         return true;
      } else {
         return false;
      }
   }

   protected int getBurnDuration(ItemStack param0) {
      if (param0.isEmpty()) {
         return 0;
      }

      Item var2 = param0.getItem();
      return getFuel().getOrDefault(var2, 0);
   }

   private static int getTotalCookTime(Level param0, AbstractFurnaceBlockEntity param1) {
      return param1.quickCheck.getRecipeFor(param1, param0).map(AbstractCookingRecipe::getCookingTime).orElse(200);
   }

   public static boolean isFuel(ItemStack param0) {
      return getFuel().containsKey(param0.getItem());
   }

   @Override
   public int[] getSlotsForFace(Direction param0) {
      if (param0 == Direction.DOWN) {
         return SLOTS_FOR_DOWN;
      } else {
         return param0 == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
      }
   }

   @Override
   public boolean canPlaceItemThroughFace(int param0, ItemStack param1, @Nullable Direction param2) {
      return this.canPlaceItem(param0, param1);
   }

   @Override
   public boolean canTakeItemThroughFace(int param0, ItemStack param1, Direction param2) {
      return param2 == Direction.DOWN && param0 == 1 ? param1.is(Items.WATER_BUCKET) || param1.is(Items.BUCKET) : true;
   }

   @Override
   public int getContainerSize() {
      return this.items.size();
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack var2 : this.items) {
         if (!var2.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(int param0) {
      return this.items.get(param0);
   }

   @Override
   public ItemStack removeItem(int param0, int param1) {
      return ContainerHelper.removeItem(this.items, param0, param1);
   }

   @Override
   public ItemStack removeItemNoUpdate(int param0) {
      return ContainerHelper.takeItem(this.items, param0);
   }

   @Override
   public void setItem(int param0, ItemStack param1) {
      ItemStack var3 = this.items.get(param0);
      boolean var4 = !param1.isEmpty() && param1.sameItem(var3) && ItemStack.tagMatches(param1, var3);
      this.items.set(param0, param1);
      if (param1.getCount() > this.getMaxStackSize()) {
         param1.setCount(this.getMaxStackSize());
      }

      if (param0 == 0 && !var4) {
         this.cookingTotalTime = getTotalCookTime(this.level, this);
         this.cookingProgress = 0;
         this.setChanged();
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return Container.stillValidBlockEntity(this, param0);
   }

   @Override
   public boolean canPlaceItem(int param0, ItemStack param1) {
      if (param0 == 2) {
         return false;
      }

      if (param0 != 1) {
         return true;
      }

      ItemStack var3 = this.items.get(1);
      return isFuel(param1) || param1.is(Items.BUCKET) && !var3.is(Items.BUCKET);
   }

   @Override
   public void clearContent() {
      this.items.clear();
   }

   @Override
   public void setRecipeUsed(@Nullable Recipe<?> param0) {
      if (param0 != null) {
         ResourceLocation var2 = param0.getId();
         this.recipesUsed.addTo(var2, 1);
      }
   }

   @Nullable
   @Override
   public Recipe<?> getRecipeUsed() {
      return null;
   }

   @Override
   public void awardUsedRecipes(Player param0) {
   }

   public void awardUsedRecipesAndPopExperience(ServerPlayer param0) {
      List<Recipe<?>> var2 = this.getRecipesToAwardAndPopExperience(param0.getLevel(), param0.position());
      param0.awardRecipes(var2);
      this.recipesUsed.clear();
   }

   public List<Recipe<?>> getRecipesToAwardAndPopExperience(ServerLevel param0, Vec3 param1) {
      List<Recipe<?>> var3 = Lists.newArrayList();

      for (Entry<ResourceLocation> var5 : this.recipesUsed.object2IntEntrySet()) {
         param0.getRecipeManager().byKey(var5.getKey()).ifPresent(l_param4 -> {
            var3.add((Recipe<?>)l_param4);
            createExperience(param0, param1, var5.getIntValue(), ((AbstractCookingRecipe)l_param4).getExperience());
         });
      }

      return var3;
   }

   private static void createExperience(ServerLevel param0, Vec3 param1, int param2, float param3) {
      int var4 = Mth.floor(param2 * param3);
      float var5 = Mth.frac(param2 * param3);
      if (var5 != 0.0F && Math.random() < var5) {
         var4++;
      }

      ExperienceOrb.award(param0, param1, var4);
   }

   @Override
   public void fillStackedContents(StackedContents param0) {
      for (ItemStack var3 : this.items) {
         param0.accountStack(var3);
      }
   }
}
