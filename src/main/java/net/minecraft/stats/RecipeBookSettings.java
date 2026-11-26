package net.minecraft.stats;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.RecipeBookType;

public final class RecipeBookSettings {
   private static final Map<RecipeBookType, Pair<String, String>> TAG_FIELDS = ImmutableMap.of(
      RecipeBookType.CRAFTING,
      Pair.of("isGuiOpen", "isFilteringCraftable"),
      RecipeBookType.FURNACE,
      Pair.of("isFurnaceGuiOpen", "isFurnaceFilteringCraftable"),
      RecipeBookType.BLAST_FURNACE,
      Pair.of("isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable"),
      RecipeBookType.SMOKER,
      Pair.of("isSmokerGuiOpen", "isSmokerFilteringCraftable")
   );
   private final Map<RecipeBookType, RecipeBookSettings.TypeSettings> states;

   private RecipeBookSettings(Map<RecipeBookType, RecipeBookSettings.TypeSettings> param0) {
      this.states = param0;
   }

   public RecipeBookSettings() {
      this(Util.make(Maps.newEnumMap(RecipeBookType.class), l_param0 -> {
         for (RecipeBookType l_var4 : RecipeBookType.values()) {
            l_param0.put(l_var4, new RecipeBookSettings.TypeSettings(false, false));
         }
      }));
   }

   public boolean isOpen(RecipeBookType param0) {
      return this.states.get(param0).open;
   }

   public void setOpen(RecipeBookType param0, boolean param1) {
      this.states.get(param0).open = param1;
   }

   public boolean isFiltering(RecipeBookType param0) {
      return this.states.get(param0).filtering;
   }

   public void setFiltering(RecipeBookType param0, boolean param1) {
      this.states.get(param0).filtering = param1;
   }

   public static RecipeBookSettings read(FriendlyByteBuf param0) {
      Map<RecipeBookType, RecipeBookSettings.TypeSettings> var1 = Maps.newEnumMap(RecipeBookType.class);

      for (RecipeBookType var5 : RecipeBookType.values()) {
         boolean var6 = param0.readBoolean();
         boolean var7 = param0.readBoolean();
         var1.put(var5, new RecipeBookSettings.TypeSettings(var6, var7));
      }

      return new RecipeBookSettings(var1);
   }

   public void write(FriendlyByteBuf param0) {
      for (RecipeBookType var5 : RecipeBookType.values()) {
         RecipeBookSettings.TypeSettings var6 = this.states.get(var5);
         if (var6 == null) {
            param0.writeBoolean(false);
            param0.writeBoolean(false);
         } else {
            param0.writeBoolean(var6.open);
            param0.writeBoolean(var6.filtering);
         }
      }
   }

   public static RecipeBookSettings read(CompoundTag param0) {
      Map<RecipeBookType, RecipeBookSettings.TypeSettings> var1 = Maps.newEnumMap(RecipeBookType.class);
      TAG_FIELDS.forEach((l_param2, l_param3) -> {
         boolean l_var4 = param0.getBoolean(l_param3.getFirst());
         boolean l_var5 = param0.getBoolean(l_param3.getSecond());
         var1.put(l_param2, new RecipeBookSettings.TypeSettings(l_var4, l_var5));
      });
      return new RecipeBookSettings(var1);
   }

   public void write(CompoundTag param0) {
      TAG_FIELDS.forEach((l_param1, l_param2) -> {
         RecipeBookSettings.TypeSettings l_var4 = this.states.get(l_param1);
         param0.putBoolean(l_param2.getFirst(), l_var4.open);
         param0.putBoolean(l_param2.getSecond(), l_var4.filtering);
      });
   }

   public RecipeBookSettings copy() {
      Map<RecipeBookType, RecipeBookSettings.TypeSettings> var1 = Maps.newEnumMap(RecipeBookType.class);

      for (RecipeBookType var5 : RecipeBookType.values()) {
         RecipeBookSettings.TypeSettings var6 = this.states.get(var5);
         var1.put(var5, var6.copy());
      }

      return new RecipeBookSettings(var1);
   }

   public void replaceFrom(RecipeBookSettings param0) {
      this.states.clear();

      for (RecipeBookType var5 : RecipeBookType.values()) {
         RecipeBookSettings.TypeSettings var6 = param0.states.get(var5);
         this.states.put(var5, var6.copy());
      }
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0 || param0 instanceof RecipeBookSettings && this.states.equals(((RecipeBookSettings)param0).states);
   }

   @Override
   public int hashCode() {
      return this.states.hashCode();
   }

   static final class TypeSettings {
      boolean open;
      boolean filtering;

      public TypeSettings(boolean param0, boolean param1) {
         this.open = param0;
         this.filtering = param1;
      }

      public RecipeBookSettings.TypeSettings copy() {
         return new RecipeBookSettings.TypeSettings(this.open, this.filtering);
      }

      @Override
      public boolean equals(Object param0) {
         if (this == param0) {
            return true;
         } else {
            return !(param0 instanceof RecipeBookSettings.TypeSettings var2) ? false : this.open == var2.open && this.filtering == var2.filtering;
         }
      }

      @Override
      public int hashCode() {
         int var1 = this.open ? 1 : 0;
         return 31 * var1 + (this.filtering ? 1 : 0);
      }

      @Override
      public String toString() {
         return "[open=" + this.open + ", filtering=" + this.filtering + "]";
      }
   }
}
