package net.minecraft.client.searchtree;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

public class SuffixArray<T> {
   private static final boolean DEBUG_COMPARISONS = Boolean.parseBoolean(System.getProperty("SuffixArray.printComparisons", "false"));
   private static final boolean DEBUG_ARRAY = Boolean.parseBoolean(System.getProperty("SuffixArray.printArray", "false"));
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int END_OF_TEXT_MARKER = -1;
   private static final int END_OF_DATA = -2;
   protected final List<T> list = Lists.newArrayList();
   private final IntList chars = new IntArrayList();
   private final IntList wordStarts = new IntArrayList();
   private IntList suffixToT = new IntArrayList();
   private IntList offsets = new IntArrayList();
   private int maxStringLength;

   public void add(T param0, String param1) {
      this.maxStringLength = Math.max(this.maxStringLength, param1.length());
      int var3 = this.list.size();
      this.list.add(param0);
      this.wordStarts.add(this.chars.size());

      for (int var4 = 0; var4 < param1.length(); var4++) {
         this.suffixToT.add(var3);
         this.offsets.add(var4);
         this.chars.add(param1.charAt(var4));
      }

      this.suffixToT.add(var3);
      this.offsets.add(param1.length());
      this.chars.add(-1);
   }

   public void generate() {
      int var1 = this.chars.size();
      int[] var2 = new int[var1];
      int[] var3 = new int[var1];
      int[] var4 = new int[var1];
      int[] var5 = new int[var1];
      IntComparator var6 = (l_param2, l_param3) -> var3[l_param2] == var3[l_param3]
         ? Integer.compare(var4[l_param2], var4[l_param3])
         : Integer.compare(var3[l_param2], var3[l_param3]);
      Swapper var7 = (l_param3, l_param4) -> {
         if (l_param3 != l_param4) {
            int l_var5 = var3[l_param3];
            var3[l_param3] = var3[l_param4];
            var3[l_param4] = l_var5;
            l_var5 = var4[l_param3];
            var4[l_param3] = var4[l_param4];
            var4[l_param4] = l_var5;
            l_var5 = var5[l_param3];
            var5[l_param3] = var5[l_param4];
            var5[l_param4] = l_var5;
         }
      };

      for (int var8 = 0; var8 < var1; var8++) {
         var2[var8] = this.chars.getInt(var8);
      }

      int var8 = 1;

      for (int var9 = Math.min(var1, this.maxStringLength); var8 * 2 < var9; var8 *= 2) {
         for (int var10 = 0; var10 < var1; var5[var10] = var10++) {
            var3[var10] = var2[var10];
            var4[var10] = var10 + var8 < var1 ? var2[var10 + var8] : -2;
         }

         Arrays.quickSort(0, var1, var6, var7);

         for (int var10 = 0; var10 < var1; var10++) {
            if (var10 > 0 && var3[var10] == var3[var10 - 1] && var4[var10] == var4[var10 - 1]) {
               var2[var5[var10]] = var2[var5[var10 - 1]];
            } else {
               var2[var5[var10]] = var10;
            }
         }
      }

      IntList var10 = this.suffixToT;
      IntList var11 = this.offsets;
      this.suffixToT = new IntArrayList(var10.size());
      this.offsets = new IntArrayList(var11.size());

      for (int var12 = 0; var12 < var1; var12++) {
         int var13 = var5[var12];
         this.suffixToT.add(var10.getInt(var13));
         this.offsets.add(var11.getInt(var13));
      }

      if (DEBUG_ARRAY) {
         this.print();
      }
   }

   private void print() {
      for (int var1 = 0; var1 < this.suffixToT.size(); var1++) {
         LOGGER.debug("{} {}", var1, this.getString(var1));
      }

      LOGGER.debug("");
   }

   private String getString(int param0) {
      int var2 = this.offsets.getInt(param0);
      int var3 = this.wordStarts.getInt(this.suffixToT.getInt(param0));
      StringBuilder var4 = new StringBuilder();

      for (int var5 = 0; var3 + var5 < this.chars.size(); var5++) {
         if (var5 == var2) {
            var4.append('^');
         }

         int var6 = this.chars.getInt(var3 + var5);
         if (var6 == -1) {
            break;
         }

         var4.append((char)var6);
      }

      return var4.toString();
   }

   private int compare(String param0, int param1) {
      int var3 = this.wordStarts.getInt(this.suffixToT.getInt(param1));
      int var4 = this.offsets.getInt(param1);

      for (int var5 = 0; var5 < param0.length(); var5++) {
         int var6 = this.chars.getInt(var3 + var4 + var5);
         if (var6 == -1) {
            return 1;
         }

         char var7 = param0.charAt(var5);
         char var8 = (char)var6;
         if (var7 < var8) {
            return -1;
         }

         if (var7 > var8) {
            return 1;
         }
      }

      return 0;
   }

   public List<T> search(String param0) {
      int var2 = this.suffixToT.size();
      int var3 = 0;
      int var4 = var2;

      while (var3 < var4) {
         int var5 = var3 + (var4 - var3) / 2;
         int var6 = this.compare(param0, var5);
         if (DEBUG_COMPARISONS) {
            LOGGER.debug("comparing lower \"{}\" with {} \"{}\": {}", param0, var5, this.getString(var5), var6);
         }

         if (var6 > 0) {
            var3 = var5 + 1;
         } else {
            var4 = var5;
         }
      }

      if (var3 >= 0 && var3 < var2) {
         int var5 = var3;
         var4 = var2;

         while (var3 < var4) {
            int var6 = var3 + (var4 - var3) / 2;
            int var7 = this.compare(param0, var6);
            if (DEBUG_COMPARISONS) {
               LOGGER.debug("comparing upper \"{}\" with {} \"{}\": {}", param0, var6, this.getString(var6), var7);
            }

            if (var7 >= 0) {
               var3 = var6 + 1;
            } else {
               var4 = var6;
            }
         }

         int var6 = var3;
         IntSet var7 = new IntOpenHashSet();

         for (int var8 = var5; var8 < var6; var8++) {
            var7.add(this.suffixToT.getInt(var8));
         }

         int[] var8 = var7.toIntArray();
         java.util.Arrays.sort(var8);
         Set<T> var9 = Sets.newLinkedHashSet();

         for (int var13 : var8) {
            var9.add(this.list.get(var13));
         }

         return Lists.newArrayList(var9);
      } else {
         return Collections.emptyList();
      }
   }
}
