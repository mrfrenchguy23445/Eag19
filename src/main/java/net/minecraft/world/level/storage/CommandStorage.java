package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

public class CommandStorage {
   private static final String ID_PREFIX = "command_storage_";
   private final Map<String, CommandStorage.Container> namespaces = Maps.newHashMap();
   private final DimensionDataStorage storage;

   public CommandStorage(DimensionDataStorage param0) {
      this.storage = param0;
   }

   private CommandStorage.Container newStorage(String param0) {
      CommandStorage.Container var2 = new CommandStorage.Container();
      this.namespaces.put(param0, var2);
      return var2;
   }

   public CompoundTag get(ResourceLocation param0) {
      String var2 = param0.getNamespace();
      CommandStorage.Container var3 = this.storage.get(l_param1 -> this.newStorage(var2).load(l_param1), createId(var2));
      return var3 != null ? var3.get(param0.getPath()) : new CompoundTag();
   }

   public void set(ResourceLocation param0, CompoundTag param1) {
      String var3 = param0.getNamespace();
      this.storage.computeIfAbsent(l_param1 -> this.newStorage(var3).load(l_param1), () -> this.newStorage(var3), createId(var3)).put(param0.getPath(), param1);
   }

   public Stream<ResourceLocation> keys() {
      return this.namespaces.entrySet().stream().flatMap(l_param0 -> l_param0.getValue().getKeys(l_param0.getKey()));
   }

   private static String createId(String param0) {
      return "command_storage_" + param0;
   }

   static class Container extends SavedData {
      private static final String TAG_CONTENTS = "contents";
      private final Map<String, CompoundTag> storage = Maps.newHashMap();

      CommandStorage.Container load(CompoundTag param0) {
         CompoundTag var2 = param0.getCompound("contents");

         for (String var4 : var2.getAllKeys()) {
            this.storage.put(var4, var2.getCompound(var4));
         }

         return this;
      }

      @Override
      public CompoundTag save(CompoundTag param0) {
         CompoundTag var2 = new CompoundTag();
         this.storage.forEach((l_param1, l_param2) -> var2.put(l_param1, l_param2.copy()));
         param0.put("contents", var2);
         return param0;
      }

      public CompoundTag get(String param0) {
         CompoundTag var2 = this.storage.get(param0);
         return var2 != null ? var2 : new CompoundTag();
      }

      public void put(String param0, CompoundTag param1) {
         if (param1.isEmpty()) {
            this.storage.remove(param0);
         } else {
            this.storage.put(param0, param1);
         }

         this.setDirty();
      }

      public Stream<ResourceLocation> getKeys(String param0) {
         return this.storage.keySet().stream().map(l_param1 -> new ResourceLocation(param0, l_param1));
      }
   }
}
