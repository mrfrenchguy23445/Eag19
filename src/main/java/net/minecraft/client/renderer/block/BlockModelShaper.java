package net.minecraft.client.renderer.block;

import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockModelShaper {
   private Map<BlockState, BakedModel> modelByStateCache = Map.of();
   private final ModelManager modelManager;

   public BlockModelShaper(ModelManager param0) {
      this.modelManager = param0;
   }

   public TextureAtlasSprite getParticleIcon(BlockState param0) {
      return this.getBlockModel(param0).getParticleIcon();
   }

   public BakedModel getBlockModel(BlockState param0) {
      BakedModel var2 = this.modelByStateCache.get(param0);
      if (var2 == null) {
         var2 = this.modelManager.getMissingModel();
      }

      return var2;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public void replaceCache(Map<BlockState, BakedModel> param0) {
      this.modelByStateCache = param0;
   }

   public static ModelResourceLocation stateToModelLocation(BlockState param0) {
      return stateToModelLocation(BuiltInRegistries.BLOCK.getKey(param0.getBlock()), param0);
   }

   public static ModelResourceLocation stateToModelLocation(ResourceLocation param0, BlockState param1) {
      return new ModelResourceLocation(param0, statePropertiesToString(param1.getValues()));
   }

   public static String statePropertiesToString(Map<Property<?>, Comparable<?>> param0) {
      StringBuilder var1 = new StringBuilder();

      for (Entry<Property<?>, Comparable<?>> var3 : param0.entrySet()) {
         if (var1.length() != 0) {
            var1.append(',');
         }

         Property<?> var4 = var3.getKey();
         var1.append(var4.getName());
         var1.append('=');
         var1.append(getValue(var4, var3.getValue()));
      }

      return var1.toString();
   }

   private static <T extends Comparable<T>> String getValue(Property<T> param0, Comparable<?> param1) {
      return param0.getName((T)param1);
   }
}
