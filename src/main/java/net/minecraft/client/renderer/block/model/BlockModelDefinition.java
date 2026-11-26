package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class BlockModelDefinition {
   private final Map<String, MultiVariant> variants = Maps.newLinkedHashMap();
   private MultiPart multiPart;

   public static BlockModelDefinition fromStream(BlockModelDefinition.Context param0, Reader param1) {
      return GsonHelper.fromJson(param0.gson, param1, BlockModelDefinition.class);
   }

   public static BlockModelDefinition fromJsonElement(BlockModelDefinition.Context param0, JsonElement param1) {
      return param0.gson.fromJson(param1, BlockModelDefinition.class);
   }

   public BlockModelDefinition(Map<String, MultiVariant> param0, MultiPart param1) {
      this.multiPart = param1;
      this.variants.putAll(param0);
   }

   public BlockModelDefinition(List<BlockModelDefinition> param0) {
      BlockModelDefinition var2 = null;

      for (BlockModelDefinition var4 : param0) {
         if (var4.isMultiPart()) {
            this.variants.clear();
            var2 = var4;
         }

         this.variants.putAll(var4.variants);
      }

      if (var2 != null) {
         this.multiPart = var2.multiPart;
      }
   }

   @VisibleForTesting
   public boolean hasVariant(String param0) {
      return this.variants.get(param0) != null;
   }

   @VisibleForTesting
   public MultiVariant getVariant(String param0) {
      MultiVariant var2 = this.variants.get(param0);
      if (var2 == null) {
         throw new BlockModelDefinition.MissingVariantException();
      } else {
         return var2;
      }
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (param0 instanceof BlockModelDefinition var2 && this.variants.equals(var2.variants)) {
         return this.isMultiPart() ? this.multiPart.equals(var2.multiPart) : !var2.isMultiPart();
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return 31 * this.variants.hashCode() + (this.isMultiPart() ? this.multiPart.hashCode() : 0);
   }

   public Map<String, MultiVariant> getVariants() {
      return this.variants;
   }

   @VisibleForTesting
   public Set<MultiVariant> getMultiVariants() {
      Set<MultiVariant> var1 = Sets.newHashSet(this.variants.values());
      if (this.isMultiPart()) {
         var1.addAll(this.multiPart.getMultiVariants());
      }

      return var1;
   }

   public boolean isMultiPart() {
      return this.multiPart != null;
   }

   public MultiPart getMultiPart() {
      return this.multiPart;
   }

   public static final class Context {
      protected final Gson gson = new GsonBuilder()
         .registerTypeAdapter(BlockModelDefinition.class, new BlockModelDefinition.Deserializer())
         .registerTypeAdapter(Variant.class, new Variant.Deserializer())
         .registerTypeAdapter(MultiVariant.class, new MultiVariant.Deserializer())
         .registerTypeAdapter(MultiPart.class, new MultiPart.Deserializer(this))
         .registerTypeAdapter(Selector.class, new Selector.Deserializer())
         .create();
      private StateDefinition<Block, BlockState> definition;

      public StateDefinition<Block, BlockState> getDefinition() {
         return this.definition;
      }

      public void setDefinition(StateDefinition<Block, BlockState> param0) {
         this.definition = param0;
      }
   }

   public static class Deserializer implements JsonDeserializer<BlockModelDefinition> {
      public BlockModelDefinition deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         JsonObject var4 = param0.getAsJsonObject();
         Map<String, MultiVariant> var5 = this.getVariants(param2, var4);
         MultiPart var6 = this.getMultiPart(param2, var4);
         if (!var5.isEmpty() || var6 != null && !var6.getMultiVariants().isEmpty()) {
            return new BlockModelDefinition(var5, var6);
         } else {
            throw new JsonParseException("Neither 'variants' nor 'multipart' found");
         }
      }

      protected Map<String, MultiVariant> getVariants(JsonDeserializationContext param0, JsonObject param1) {
         Map<String, MultiVariant> var3 = Maps.newHashMap();
         if (param1.has("variants")) {
            JsonObject var4 = GsonHelper.getAsJsonObject(param1, "variants");

            for (Entry<String, JsonElement> var6 : var4.entrySet()) {
               var3.put(var6.getKey(), param0.deserialize(var6.getValue(), MultiVariant.class));
            }
         }

         return var3;
      }

      @Nullable
      protected MultiPart getMultiPart(JsonDeserializationContext param0, JsonObject param1) {
         if (!param1.has("multipart")) {
            return null;
         }

         JsonArray var3 = GsonHelper.getAsJsonArray(param1, "multipart");
         return param0.deserialize(var3, MultiPart.class);
      }
   }

   protected class MissingVariantException extends RuntimeException {
   }
}
