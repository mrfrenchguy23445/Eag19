package net.minecraft.data.models.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelLocationUtils {
   @Deprecated
   public static ResourceLocation decorateBlockModelLocation(String param0) {
      return new ResourceLocation("minecraft", "block/" + param0);
   }

   public static ResourceLocation decorateItemModelLocation(String param0) {
      return new ResourceLocation("minecraft", "item/" + param0);
   }

   public static ResourceLocation getModelLocation(Block param0, String param1) {
      ResourceLocation var2 = BuiltInRegistries.BLOCK.getKey(param0);
      return var2.withPath(l_param1 -> "block/" + l_param1 + param1);
   }

   public static ResourceLocation getModelLocation(Block param0) {
      ResourceLocation var1 = BuiltInRegistries.BLOCK.getKey(param0);
      return var1.withPrefix("block/");
   }

   public static ResourceLocation getModelLocation(Item param0) {
      ResourceLocation var1 = BuiltInRegistries.ITEM.getKey(param0);
      return var1.withPrefix("item/");
   }

   public static ResourceLocation getModelLocation(Item param0, String param1) {
      ResourceLocation var2 = BuiltInRegistries.ITEM.getKey(param0);
      return var2.withPath(l_param1 -> "item/" + l_param1 + param1);
   }
}
