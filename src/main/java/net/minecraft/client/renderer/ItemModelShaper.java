package net.minecraft.client.renderer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemModelShaper {
   public final Int2ObjectMap<ModelResourceLocation> shapes = new Int2ObjectOpenHashMap<>(256);
   private final Int2ObjectMap<BakedModel> shapesCache = new Int2ObjectOpenHashMap<>(256);
   private final ModelManager modelManager;

   public ItemModelShaper(ModelManager param0) {
      this.modelManager = param0;
   }

   public BakedModel getItemModel(ItemStack param0) {
      BakedModel var2 = this.getItemModel(param0.getItem());
      return var2 == null ? this.modelManager.getMissingModel() : var2;
   }

   @Nullable
   public BakedModel getItemModel(Item param0) {
      return this.shapesCache.get(getIndex(param0));
   }

   private static int getIndex(Item param0) {
      return Item.getId(param0);
   }

   public void register(Item param0, ModelResourceLocation param1) {
      this.shapes.put(getIndex(param0), param1);
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public void rebuildCache() {
      this.shapesCache.clear();

      for (Entry<Integer, ModelResourceLocation> var2 : this.shapes.entrySet()) {
         this.shapesCache.put(var2.getKey(), this.modelManager.getModel(var2.getValue()));
      }
   }
}
