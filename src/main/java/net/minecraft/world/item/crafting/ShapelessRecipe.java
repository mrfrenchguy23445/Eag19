package net.minecraft.world.item.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShapelessRecipe implements CraftingRecipe {
   private final ResourceLocation id;
   final String group;
   final CraftingBookCategory category;
   final ItemStack result;
   final NonNullList<Ingredient> ingredients;

   public ShapelessRecipe(ResourceLocation param0, String param1, CraftingBookCategory param2, ItemStack param3, NonNullList<Ingredient> param4) {
      this.id = param0;
      this.group = param1;
      this.category = param2;
      this.result = param3;
      this.ingredients = param4;
   }

   @Override
   public ResourceLocation getId() {
      return this.id;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SHAPELESS_RECIPE;
   }

   @Override
   public String getGroup() {
      return this.group;
   }

   @Override
   public CraftingBookCategory category() {
      return this.category;
   }

   @Override
   public ItemStack getResultItem(RegistryAccess param0) {
      return this.result;
   }

   @Override
   public NonNullList<Ingredient> getIngredients() {
      return this.ingredients;
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      StackedContents var3 = new StackedContents();
      int var4 = 0;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            var4++;
            var3.accountStack(var6, 1);
         }
      }

      return var4 == this.ingredients.size() && var3.canCraft(this, null);
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      return this.result.copy();
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 * param1 >= this.ingredients.size();
   }

   public static class Serializer implements RecipeSerializer<ShapelessRecipe> {
      public ShapelessRecipe fromJson(ResourceLocation param0, JsonObject param1) {
         String var3 = GsonHelper.getAsString(param1, "group", "");
         CraftingBookCategory var4 = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(param1, "category", null), CraftingBookCategory.MISC);
         NonNullList<Ingredient> var5 = itemsFromJson(GsonHelper.getAsJsonArray(param1, "ingredients"));
         if (var5.isEmpty()) {
            throw new JsonParseException("No ingredients for shapeless recipe");
         }

         if (var5.size() > 9) {
            throw new JsonParseException("Too many ingredients for shapeless recipe");
         }

         ItemStack var6 = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(param1, "result"));
         return new ShapelessRecipe(param0, var3, var4, var6, var5);
      }

      private static NonNullList<Ingredient> itemsFromJson(JsonArray param0) {
         NonNullList<Ingredient> var1 = NonNullList.create();

         for (int var2 = 0; var2 < param0.size(); var2++) {
            Ingredient var3 = Ingredient.fromJson(param0.get(var2));
            if (!var3.isEmpty()) {
               var1.add(var3);
            }
         }

         return var1;
      }

      public ShapelessRecipe fromNetwork(ResourceLocation param0, FriendlyByteBuf param1) {
         String var3 = param1.readUtf();
         CraftingBookCategory var4 = param1.readEnum(CraftingBookCategory.class);
         int var5 = param1.readVarInt();
         NonNullList<Ingredient> var6 = NonNullList.withSize(var5, Ingredient.EMPTY);

         for (int var7 = 0; var7 < var6.size(); var7++) {
            var6.set(var7, Ingredient.fromNetwork(param1));
         }

         ItemStack var7 = param1.readItem();
         return new ShapelessRecipe(param0, var3, var4, var7, var6);
      }

      public void toNetwork(FriendlyByteBuf param0, ShapelessRecipe param1) {
         param0.writeUtf(param1.group);
         param0.writeEnum(param1.category);
         param0.writeVarInt(param1.ingredients.size());

         for (Ingredient var4 : param1.ingredients) {
            var4.toNetwork(param0);
         }

         param0.writeItem(param1.result);
      }
   }
}
