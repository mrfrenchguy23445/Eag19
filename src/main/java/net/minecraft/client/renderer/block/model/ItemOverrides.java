package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemOverrides {
   public static final ItemOverrides EMPTY = new ItemOverrides();
   public static final float NO_OVERRIDE = Float.NEGATIVE_INFINITY;
   private final ItemOverrides.BakedOverride[] overrides;
   private final ResourceLocation[] properties;

   private ItemOverrides() {
      this.overrides = new ItemOverrides.BakedOverride[0];
      this.properties = new ResourceLocation[0];
   }

   public ItemOverrides(ModelBaker param0, BlockModel param1, List<ItemOverride> param2) {
      this.properties = param2.stream()
         .flatMap(ItemOverride::getPredicates)
         .map(ItemOverride.Predicate::getProperty)
         .distinct()
         .toArray(ResourceLocation[]::new);
      Object2IntMap<ResourceLocation> var4 = new Object2IntOpenHashMap<>();

      for (int var5 = 0; var5 < this.properties.length; var5++) {
         var4.put(this.properties[var5], var5);
      }

      List<ItemOverrides.BakedOverride> var5 = Lists.newArrayList();

      for (int var6 = param2.size() - 1; var6 >= 0; var6--) {
         ItemOverride var7 = param2.get(var6);
         BakedModel var8 = this.bakeModel(param0, param1, var7);
         ItemOverrides.PropertyMatcher[] var9 = var7.getPredicates().map(l_param1 -> {
            int l_var2 = var4.getInt(l_param1.getProperty());
            return new ItemOverrides.PropertyMatcher(l_var2, l_param1.getValue());
         }).toArray(ItemOverrides.PropertyMatcher[]::new);
         var5.add(new ItemOverrides.BakedOverride(var9, var8));
      }

      this.overrides = var5.toArray(new ItemOverrides.BakedOverride[0]);
   }

   @Nullable
   private BakedModel bakeModel(ModelBaker param0, BlockModel param1, ItemOverride param2) {
      UnbakedModel var4 = param0.getModel(param2.getModel());
      return Objects.equals(var4, param1) ? null : param0.bake(param2.getModel(), BlockModelRotation.X0_Y0);
   }

   @Nullable
   public BakedModel resolve(BakedModel param0, ItemStack param1, @Nullable ClientLevel param2, @Nullable LivingEntity param3, int param4) {
      if (this.overrides.length != 0) {
         Item var6 = param1.getItem();
         int var7 = this.properties.length;
         float[] var8 = new float[var7];

         for (int var9 = 0; var9 < var7; var9++) {
            ResourceLocation var10 = this.properties[var9];
            ItemPropertyFunction var11 = ItemProperties.getProperty(var6, var10);
            if (var11 != null) {
               var8[var9] = var11.call(param1, param2, param3, param4);
            } else {
               var8[var9] = Float.NEGATIVE_INFINITY;
            }
         }

         for (ItemOverrides.BakedOverride var12 : this.overrides) {
            if (var12.test(var8)) {
               BakedModel var13 = var12.model;
               if (var13 == null) {
                  return param0;
               }

               return var13;
            }
         }
      }

      return param0;
   }

   static class BakedOverride {
      private final ItemOverrides.PropertyMatcher[] matchers;
      @Nullable
      final BakedModel model;

      BakedOverride(ItemOverrides.PropertyMatcher[] param0, @Nullable BakedModel param1) {
         this.matchers = param0;
         this.model = param1;
      }

      boolean test(float[] param0) {
         for (ItemOverrides.PropertyMatcher var5 : this.matchers) {
            float var6 = param0[var5.index];
            if (var6 < var5.value) {
               return false;
            }
         }

         return true;
      }
   }

   static class PropertyMatcher {
      public final int index;
      public final float value;

      PropertyMatcher(int param0, float param1) {
         this.index = param0;
         this.value = param1;
      }
   }
}
