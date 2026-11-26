package net.minecraft.world.entity.player;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public class StackedContents {
   private static final int EMPTY = 0;
   public final Int2IntMap contents = new Int2IntOpenHashMap();

   public void accountSimpleStack(ItemStack param0) {
      if (!param0.isDamaged() && !param0.isEnchanted() && !param0.hasCustomHoverName()) {
         this.accountStack(param0);
      }
   }

   public void accountStack(ItemStack param0) {
      this.accountStack(param0, 64);
   }

   public void accountStack(ItemStack param0, int param1) {
      if (!param0.isEmpty()) {
         int var3 = getStackingIndex(param0);
         int var4 = Math.min(param1, param0.getCount());
         this.put(var3, var4);
      }
   }

   public static int getStackingIndex(ItemStack param0) {
      return BuiltInRegistries.ITEM.getId(param0.getItem());
   }

   boolean has(int param0) {
      return this.contents.get(param0) > 0;
   }

   int take(int param0, int param1) {
      int var3 = this.contents.get(param0);
      if (var3 >= param1) {
         this.contents.put(param0, var3 - param1);
         return param0;
      } else {
         return 0;
      }
   }

   void put(int param0, int param1) {
      this.contents.put(param0, this.contents.get(param0) + param1);
   }

   public boolean canCraft(Recipe<?> param0, @Nullable IntList param1) {
      return this.canCraft(param0, param1, 1);
   }

   public boolean canCraft(Recipe<?> param0, @Nullable IntList param1, int param2) {
      return new StackedContents.RecipePicker(param0).tryPick(param2, param1);
   }

   public int getBiggestCraftableStack(Recipe<?> param0, @Nullable IntList param1) {
      return this.getBiggestCraftableStack(param0, Integer.MAX_VALUE, param1);
   }

   public int getBiggestCraftableStack(Recipe<?> param0, int param1, @Nullable IntList param2) {
      return new StackedContents.RecipePicker(param0).tryPickAll(param1, param2);
   }

   public static ItemStack fromStackingIndex(int param0) {
      return param0 == 0 ? ItemStack.EMPTY : new ItemStack(Item.byId(param0));
   }

   public void clear() {
      this.contents.clear();
   }

   class RecipePicker {
      private final Recipe<?> recipe;
      private final List<Ingredient> ingredients = Lists.newArrayList();
      private final int ingredientCount;
      private final int[] items;
      private final int itemCount;
      private final BitSet data;
      private final IntList path = new IntArrayList();

      public RecipePicker(Recipe<?> param1) {
         this.recipe = param1;
         this.ingredients.addAll(param1.getIngredients());
         this.ingredients.removeIf(Ingredient::isEmpty);
         this.ingredientCount = this.ingredients.size();
         this.items = this.getUniqueAvailableIngredientItems();
         this.itemCount = this.items.length;
         this.data = new BitSet(this.ingredientCount + this.itemCount + this.ingredientCount + this.ingredientCount * this.itemCount);

         for (int var3 = 0; var3 < this.ingredients.size(); var3++) {
            IntList var4 = this.ingredients.get(var3).getStackingIds();

            for (int var5 = 0; var5 < this.itemCount; var5++) {
               if (var4.contains(this.items[var5])) {
                  this.data.set(this.getIndex(true, var5, var3));
               }
            }
         }
      }

      public boolean tryPick(int param0, @Nullable IntList param1) {
         if (param0 <= 0) {
            return true;
         }

         int var3;
         for (var3 = 0; this.dfs(param0); var3++) {
            StackedContents.this.take(this.items[this.path.getInt(0)], param0);
            int var4 = this.path.size() - 1;
            this.setSatisfied(this.path.getInt(var4));

            for (int var5 = 0; var5 < var4; var5++) {
               this.toggleResidual((var5 & 1) == 0, this.path.get(var5), this.path.get(var5 + 1));
            }

            this.path.clear();
            this.data.clear(0, this.ingredientCount + this.itemCount);
         }

         boolean var4 = var3 == this.ingredientCount;
         boolean var5 = var4 && param1 != null;
         if (var5) {
            param1.clear();
         }

         this.data.clear(0, this.ingredientCount + this.itemCount + this.ingredientCount);
         int var6 = 0;
         List<Ingredient> var7 = this.recipe.getIngredients();

         for (int var8 = 0; var8 < var7.size(); var8++) {
            if (var5 && var7.get(var8).isEmpty()) {
               param1.add(0);
            } else {
               for (int var9 = 0; var9 < this.itemCount; var9++) {
                  if (this.hasResidual(false, var6, var9)) {
                     this.toggleResidual(true, var9, var6);
                     StackedContents.this.put(this.items[var9], param0);
                     if (var5) {
                        param1.add(this.items[var9]);
                     }
                  }
               }

               var6++;
            }
         }

         return var4;
      }

      private int[] getUniqueAvailableIngredientItems() {
         IntCollection var1 = new IntAVLTreeSet();

         for (Ingredient var3 : this.ingredients) {
            var1.addAll(var3.getStackingIds());
         }

         IntIterator var2 = var1.iterator();

         while (var2.hasNext()) {
            if (!StackedContents.this.has(var2.nextInt())) {
               var2.remove();
            }
         }

         return var1.toIntArray();
      }

      private boolean dfs(int param0) {
         int var2 = this.itemCount;

         for (int var3 = 0; var3 < var2; var3++) {
            if (StackedContents.this.contents.get(this.items[var3]) >= param0) {
               this.visit(false, var3);

               while (!this.path.isEmpty()) {
                  int var4 = this.path.size();
                  boolean var5 = (var4 & 1) == 1;
                  int var6 = this.path.getInt(var4 - 1);
                  if (!var5 && !this.isSatisfied(var6)) {
                     break;
                  }

                  int var7 = var5 ? this.ingredientCount : var2;
                  int var8 = 0;

                  while (true) {
                     if (var8 < var7) {
                        if (this.hasVisited(var5, var8) || !this.hasConnection(var5, var6, var8) || !this.hasResidual(var5, var6, var8)) {
                           var8++;
                           continue;
                        }

                        this.visit(var5, var8);
                     }

                     var8 = this.path.size();
                     if (var8 == var4) {
                        this.path.removeInt(var8 - 1);
                     }
                     break;
                  }
               }

               if (!this.path.isEmpty()) {
                  return true;
               }
            }
         }

         return false;
      }

      private boolean isSatisfied(int param0) {
         return this.data.get(this.getSatisfiedIndex(param0));
      }

      private void setSatisfied(int param0) {
         this.data.set(this.getSatisfiedIndex(param0));
      }

      private int getSatisfiedIndex(int param0) {
         return this.ingredientCount + this.itemCount + param0;
      }

      private boolean hasConnection(boolean param0, int param1, int param2) {
         return this.data.get(this.getIndex(param0, param1, param2));
      }

      private boolean hasResidual(boolean param0, int param1, int param2) {
         return param0 != this.data.get(1 + this.getIndex(param0, param1, param2));
      }

      private void toggleResidual(boolean param0, int param1, int param2) {
         this.data.flip(1 + this.getIndex(param0, param1, param2));
      }

      private int getIndex(boolean param0, int param1, int param2) {
         int var4 = param0 ? param1 * this.ingredientCount + param2 : param2 * this.ingredientCount + param1;
         return this.ingredientCount + this.itemCount + this.ingredientCount + 2 * var4;
      }

      private void visit(boolean param0, int param1) {
         this.data.set(this.getVisitedIndex(param0, param1));
         this.path.add(param1);
      }

      private boolean hasVisited(boolean param0, int param1) {
         return this.data.get(this.getVisitedIndex(param0, param1));
      }

      private int getVisitedIndex(boolean param0, int param1) {
         return (param0 ? 0 : this.ingredientCount) + param1;
      }

      public int tryPickAll(int param0, @Nullable IntList param1) {
         int var3 = 0;
         int var4 = Math.min(param0, this.getMinIngredientCount()) + 1;

         while (true) {
            int var5 = (var3 + var4) / 2;
            if (this.tryPick(var5, null)) {
               if (var4 - var3 <= 1) {
                  if (var5 > 0) {
                     this.tryPick(var5, param1);
                  }

                  return var5;
               }

               var3 = var5;
            } else {
               var4 = var5;
            }
         }
      }

      private int getMinIngredientCount() {
         int var1 = Integer.MAX_VALUE;

         for (Ingredient var3 : this.ingredients) {
            int var4 = 0;

            for (int var6 : var3.getStackingIds()) {
               var4 = Math.max(var4, StackedContents.this.contents.get(var6));
            }

            if (var1 > 0) {
               var1 = Math.min(var1, var4);
            }
         }

         return var1;
      }
   }
}
