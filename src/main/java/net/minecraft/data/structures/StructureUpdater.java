package net.minecraft.data.structures;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

public class StructureUpdater implements SnbtToNbt.Filter {
   private static final Logger LOGGER = LogUtils.getLogger();

   @Override
   public CompoundTag apply(String param0, CompoundTag param1) {
      return param0.startsWith("data/minecraft/structures/") ? update(param0, param1) : param1;
   }

   public static CompoundTag update(String param0, CompoundTag param1) {
      StructureTemplate var2 = new StructureTemplate();
      int var3 = NbtUtils.getDataVersion(param1, 500);
      int var4 = 3318;
      if (var3 < 3318) {
         LOGGER.warn("SNBT Too old, do not forget to update: {} < {}: {}", var3, 3318, param0);
      }

      CompoundTag var5 = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), param1, var3);
      var2.load(BuiltInRegistries.BLOCK.asLookup(), var5);
      return var2.save(new CompoundTag());
   }
}
