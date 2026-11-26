package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class MultiPart implements UnbakedModel {
   private final StateDefinition<Block, BlockState> definition;
   private final List<Selector> selectors;

   public MultiPart(StateDefinition<Block, BlockState> param0, List<Selector> param1) {
      this.definition = param0;
      this.selectors = param1;
   }

   public List<Selector> getSelectors() {
      return this.selectors;
   }

   public Set<MultiVariant> getMultiVariants() {
      Set<MultiVariant> var1 = Sets.newHashSet();

      for (Selector var3 : this.selectors) {
         var1.add(var3.getVariant());
      }

      return var1;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return !(param0 instanceof MultiPart var2)
            ? false
            : Objects.equals(this.definition, var2.definition) && Objects.equals(this.selectors, var2.selectors);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.definition, this.selectors);
   }

   @Override
   public Collection<ResourceLocation> getDependencies() {
      return this.getSelectors().stream().flatMap(l_param0 -> l_param0.getVariant().getDependencies().stream()).collect(Collectors.toSet());
   }

   @Override
   public void resolveParents(Function<ResourceLocation, UnbakedModel> param0) {
      this.getSelectors().forEach(l_param1 -> l_param1.getVariant().resolveParents(param0));
   }

   @Nullable
   @Override
   public BakedModel bake(ModelBaker param0, Function<Material, TextureAtlasSprite> param1, ModelState param2, ResourceLocation param3) {
      MultiPartBakedModel.Builder var5 = new MultiPartBakedModel.Builder();

      for (Selector var7 : this.getSelectors()) {
         BakedModel var8 = var7.getVariant().bake(param0, param1, param2, param3);
         if (var8 != null) {
            var5.add(var7.getPredicate(this.definition), var8);
         }
      }

      return var5.build();
   }

   public static class Deserializer implements JsonDeserializer<MultiPart> {
      private final BlockModelDefinition.Context context;

      public Deserializer(BlockModelDefinition.Context param0) {
         this.context = param0;
      }

      public MultiPart deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         return new MultiPart(this.context.getDefinition(), this.getSelectors(param2, param0.getAsJsonArray()));
      }

      private List<Selector> getSelectors(JsonDeserializationContext param0, JsonArray param1) {
         List<Selector> var3 = Lists.newArrayList();

         for (JsonElement var5 : param1) {
            var3.add(param0.deserialize(var5, Selector.class));
         }

         return var3;
      }
   }
}
