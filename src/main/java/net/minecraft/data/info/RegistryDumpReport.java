package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public class RegistryDumpReport implements DataProvider {
   private final PackOutput output;

   public RegistryDumpReport(PackOutput param0) {
      this.output = param0;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput param0) {
      JsonObject var2 = new JsonObject();
      BuiltInRegistries.REGISTRY.holders().forEach(l_param1 -> var2.add(l_param1.key().location().toString(), dumpRegistry((Registry<?>)l_param1.value())));
      Path var3 = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("registries.json");
      return DataProvider.saveStable(param0, var2, var3);
   }

   private static <T> JsonElement dumpRegistry(Registry<T> param0) {
      JsonObject var1 = new JsonObject();
      if (param0 instanceof DefaultedRegistry) {
         ResourceLocation var2 = ((DefaultedRegistry)param0).getDefaultKey();
         var1.addProperty("default", var2.toString());
      }

      int var2 = ((net.minecraft.core.Registry)BuiltInRegistries.REGISTRY).getId(param0);
      var1.addProperty("protocol_id", var2);
      JsonObject var3 = new JsonObject();
      param0.holders().forEach(l_param2 -> {
         T l_var3 = l_param2.value();
         int l_var4 = param0.getId(l_var3);
         JsonObject l_var5 = new JsonObject();
         l_var5.addProperty("protocol_id", l_var4);
         var3.add(l_param2.key().location().toString(), l_var5);
      });
      var1.add("entries", var3);
      return var1;
   }

   @Override
   public final String getName() {
      return "Registry Dump";
   }
}
