package net.minecraft.world.level.saveddata.maps;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class MapIndex extends SavedData {
   public static final String FILE_NAME = "idcounts";
   private final Object2IntMap<String> usedAuxIds = new Object2IntOpenHashMap<>();

   public MapIndex() {
      this.usedAuxIds.defaultReturnValue(-1);
   }

   public static MapIndex load(CompoundTag param0) {
      MapIndex var1 = new MapIndex();

      for (String var3 : param0.getAllKeys()) {
         if (param0.contains(var3, 99)) {
            var1.usedAuxIds.put(var3, param0.getInt(var3));
         }
      }

      return var1;
   }

   @Override
   public CompoundTag save(CompoundTag param0) {
      for (Entry<String> var3 : this.usedAuxIds.object2IntEntrySet()) {
         param0.putInt(var3.getKey(), var3.getIntValue());
      }

      return param0;
   }

   public int getFreeAuxValueForMap() {
      int var1 = this.usedAuxIds.getInt("map") + 1;
      this.usedAuxIds.put("map", var1);
      this.setDirty();
      return var1;
   }
}
