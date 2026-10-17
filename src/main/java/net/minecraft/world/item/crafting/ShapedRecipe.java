package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class ShapedRecipe implements CraftingRecipe {
   final int width;
   final int height;
   final NonNullList<Ingredient> recipeItems;
   final ItemStack result;
   private final ResourceLocation id;
   final String group;
   final CraftingBookCategory category;
   final boolean showNotification;

   public ShapedRecipe(
      ResourceLocation param0,
      String param1,
      CraftingBookCategory param2,
      int param3,
      int param4,
      NonNullList<Ingredient> param5,
      ItemStack param6,
      boolean param7
   ) {
      this.id = param0;
      this.group = param1;
      this.category = param2;
      this.width = param3;
      this.height = param4;
      this.recipeItems = param5;
      this.result = param6;
      this.showNotification = param7;
   }

   public ShapedRecipe(
      ResourceLocation param0, String param1, CraftingBookCategory param2, int param3, int param4, NonNullList<Ingredient> param5, ItemStack param6
   ) {
      this(param0, param1, param2, param3, param4, param5, param6, true);
   }

   @Override
   public ResourceLocation getId() {
      return this.id;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SHAPED_RECIPE;
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
      return this.recipeItems;
   }

   @Override
   public boolean showNotification() {
      return this.showNotification;
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 >= this.width && param1 >= this.height;
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      for (int var3 = 0; var3 <= param0.getWidth() - this.width; var3++) {
         for (int var4 = 0; var4 <= param0.getHeight() - this.height; var4++) {
            if (this.matches(param0, var3, var4, true)) {
               return true;
            }

            if (this.matches(param0, var3, var4, false)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean matches(CraftingContainer param0, int param1, int param2, boolean param3) {
      for (int var5 = 0; var5 < param0.getWidth(); var5++) {
         for (int var6 = 0; var6 < param0.getHeight(); var6++) {
            int var7 = var5 - param1;
            int var8 = var6 - param2;
            Ingredient var9 = Ingredient.EMPTY;
            if (var7 >= 0 && var8 >= 0 && var7 < this.width && var8 < this.height) {
               if (param3) {
                  var9 = this.recipeItems.get(this.width - var7 - 1 + var8 * this.width);
               } else {
                  var9 = this.recipeItems.get(var7 + var8 * this.width);
               }
            }

            if (!var9.test(param0.getItem(var5 + var6 * param0.getWidth()))) {
               return false;
            }
         }
      }

      return true;
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      return this.getResultItem(param1).copy();
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   static NonNullList<Ingredient> dissolvePattern(String[] param0, Map<String, Ingredient> param1, int param2, int param3) {
      NonNullList<Ingredient> var4 = NonNullList.withSize(param2 * param3, Ingredient.EMPTY);
      Set<String> var5 = Sets.newHashSet(param1.keySet());
      var5.remove(" ");

      for (int var6 = 0; var6 < param0.length; var6++) {
         for (int var7 = 0; var7 < param0[var6].length(); var7++) {
            String var8 = param0[var6].substring(var7, var7 + 1);
            Ingredient var9 = param1.get(var8);
            if (var9 == null) {
               throw new JsonSyntaxException("Pattern references symbol '" + var8 + "' but it's not defined in the key");
            }

            var5.remove(var8);
            var4.set(var7 + param2 * var6, var9);
         }
      }

      if (!var5.isEmpty()) {
         throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + var5);
      } else {
         return var4;
      }
   }

   @VisibleForTesting
   static String[] shrink(String... param0) {
      int var1 = Integer.MAX_VALUE;
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;

      for (int var5 = 0; var5 < param0.length; var5++) {
         String var6 = param0[var5];
         var1 = Math.min(var1, firstNonSpace(var6));
         int var7 = lastNonSpace(var6);
         var2 = Math.max(var2, var7);
         if (var7 < 0) {
            if (var3 == var5) {
               var3++;
            }

            var4++;
         } else {
            var4 = 0;
         }
      }

      if (param0.length == var4) {
         return new String[0];
      }

      String[] var5 = new String[param0.length - var4 - var3];

      for (int var6 = 0; var6 < var5.length; var6++) {
         var5[var6] = param0[var6 + var3].substring(var1, var2 + 1);
      }

      return var5;
   }

   @Override
   public boolean isIncomplete() {
      NonNullList<Ingredient> var1 = this.getIngredients();
      return var1.isEmpty() || var1.stream().filter(l_param0 -> !l_param0.isEmpty()).anyMatch(l_param0 -> l_param0.getItems().length == 0);
   }

   private static int firstNonSpace(String param0) {
      int var1 = 0;

      while (var1 < param0.length() && param0.charAt(var1) == ' ') {
         var1++;
      }

      return var1;
   }

   private static int lastNonSpace(String param0) {
      int var1 = param0.length() - 1;

      while (var1 >= 0 && param0.charAt(var1) == ' ') {
         var1--;
      }

      return var1;
   }

   static String[] patternFromJson(JsonArray param0) {
      String[] var1 = new String[param0.size()];
      if (var1.length > 3) {
         throw new JsonSyntaxException("Invalid pattern: too many rows, 3 is maximum");
      }

      if (var1.length == 0) {
         throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
      }

      for (int var2 = 0; var2 < var1.length; var2++) {
         String var3 = GsonHelper.convertToString(param0.get(var2), "pattern[" + var2 + "]");
         if (var3.length() > 3) {
            throw new JsonSyntaxException("Invalid pattern: too many columns, 3 is maximum");
         }

         if (var2 > 0 && var1[0].length() != var3.length()) {
            throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
         }

         var1[var2] = var3;
      }

      return var1;
   }

   static Map<String, Ingredient> keyFromJson(JsonObject param0) {
      Map<String, Ingredient> var1 = Maps.newHashMap();

      for (Entry<String, JsonElement> var3 : param0.entrySet()) {
         if (var3.getKey().length() != 1) {
            throw new JsonSyntaxException("Invalid key entry: '" + var3.getKey() + "' is an invalid symbol (must be 1 character only).");
         }

         if (" ".equals(var3.getKey())) {
            throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
         }

         var1.put(var3.getKey(), Ingredient.fromJson(var3.getValue()));
      }

      var1.put(" ", Ingredient.EMPTY);
      return var1;
   }

   public static ItemStack itemStackFromJson(JsonObject param0) {
      Item var1 = itemFromJson(param0);
      if (param0.has("data")) {
         throw new JsonParseException("Disallowed data tag found");
      } else {
         int var2 = GsonHelper.getAsInt(param0, "count", 1);
         if (var2 < 1) {
            throw new JsonSyntaxException("Invalid output count: " + var2);
         } else {
            return new ItemStack(var1, var2);
         }
      }
   }

   public static Item itemFromJson(JsonObject param0) {
      String var1 = GsonHelper.getAsString(param0, "item");
      Item var2 = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(var1)).orElseThrow(() -> new JsonSyntaxException("Unknown item '" + var1 + "'"));
      if (var2 == Items.AIR) {
         throw new JsonSyntaxException("Invalid item: " + var1);
      } else {
         return var2;
      }
   }

   public static class Serializer implements RecipeSerializer<ShapedRecipe> {
      public ShapedRecipe fromJson(ResourceLocation param0, JsonObject param1) {
         String var3 = GsonHelper.getAsString(param1, "group", "");
         CraftingBookCategory var4 = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(param1, "category", null), CraftingBookCategory.MISC);
         Map<String, Ingredient> var5 = ShapedRecipe.keyFromJson(GsonHelper.getAsJsonObject(param1, "key"));
         String[] var6 = ShapedRecipe.shrink(ShapedRecipe.patternFromJson(GsonHelper.getAsJsonArray(param1, "pattern")));
         int var7 = var6[0].length();
         int var8 = var6.length;
         NonNullList<Ingredient> var9 = ShapedRecipe.dissolvePattern(var6, var5, var7, var8);
         ItemStack var10 = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(param1, "result"));
         boolean var11 = GsonHelper.getAsBoolean(param1, "show_notification", true);
         return new ShapedRecipe(param0, var3, var4, var7, var8, var9, var10, var11);
      }

      public ShapedRecipe fromNetwork(ResourceLocation param0, FriendlyByteBuf param1) {
         int var3 = param1.readVarInt();
         int var4 = param1.readVarInt();
         String var5 = param1.readUtf();
         CraftingBookCategory var6 = param1.readEnum(CraftingBookCategory.class);
         NonNullList<Ingredient> var7 = NonNullList.withSize(var3 * var4, Ingredient.EMPTY);

         for (int var8 = 0; var8 < var7.size(); var8++) {
            var7.set(var8, Ingredient.fromNetwork(param1));
         }

         ItemStack var8 = param1.readItem();
         boolean var9 = param1.readBoolean();
         return new ShapedRecipe(param0, var5, var6, var3, var4, var7, var8, var9);
      }

      public void toNetwork(FriendlyByteBuf param0, ShapedRecipe param1) {
         param0.writeVarInt(param1.width);
         param0.writeVarInt(param1.height);
         param0.writeUtf(param1.group);
         param0.writeEnum(param1.category);

         for (Ingredient var4 : param1.recipeItems) {
            var4.toNetwork(param0);
         }

         param0.writeItem(param1.result);
         param0.writeBoolean(param1.showNotification);
      }
   }
}
