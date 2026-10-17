package net.minecraft.data.info;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockListReport implements DataProvider {
   private final PackOutput output;

   public BlockListReport(PackOutput param0) {
      this.output = param0;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput param0) {
      JsonObject var2 = new JsonObject();

      for (Block var4 : BuiltInRegistries.BLOCK) {
         ResourceLocation var5 = BuiltInRegistries.BLOCK.getKey(var4);
         JsonObject var6 = new JsonObject();
         StateDefinition<Block, BlockState> var7 = var4.getStateDefinition();
         if (!var7.getProperties().isEmpty()) {
            JsonObject var8 = new JsonObject();

            for (Property<?> var10 : var7.getProperties()) {
               JsonArray var11 = new JsonArray();

               for (Comparable<?> var13 : var10.getPossibleValues()) {
                  var11.add(Util.getPropertyName(var10, var13));
               }

               var8.add(var10.getName(), var11);
            }

            var6.add("properties", var8);
         }

         JsonArray var8 = new JsonArray();

         for (BlockState var10 : var7.getPossibleStates()) {
            JsonObject var11 = new JsonObject();
            JsonObject var12 = new JsonObject();

            for (Property<?> var14 : var7.getProperties()) {
               var12.addProperty(var14.getName(), Util.getPropertyName(var14, var10.getValue(var14)));
            }

            if (var12.size() > 0) {
               var11.add("properties", var12);
            }

            var11.addProperty("id", Block.getId(var10));
            if (var10 == var4.defaultBlockState()) {
               var11.addProperty("default", true);
            }

            var8.add(var11);
         }

         var6.add("states", var8);
         var2.add(var5.toString(), var6);
      }

      Path var3 = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("blocks.json");
      return DataProvider.saveStable(param0, var2, var3);
   }

   @Override
   public final String getName() {
      return "Block List";
   }
}
