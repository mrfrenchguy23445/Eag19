package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

public final class NbtUtils {
   private static final Comparator<ListTag> YXZ_LISTTAG_INT_COMPARATOR = Comparator.<ListTag>comparingInt(param0 -> param0.getInt(1))
      .thenComparingInt(param0 -> param0.getInt(0))
      .thenComparingInt(param0 -> param0.getInt(2));
   private static final Comparator<ListTag> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.<ListTag>comparingDouble(param0 -> param0.getDouble(1))
      .thenComparingDouble(param0 -> param0.getDouble(0))
      .thenComparingDouble(param0 -> param0.getDouble(2));
   public static final String SNBT_DATA_TAG = "data";
   private static final char PROPERTIES_START = '{';
   private static final char PROPERTIES_END = '}';
   private static final String ELEMENT_SEPARATOR = ",";
   private static final char KEY_VALUE_SEPARATOR = ':';
   private static final Splitter COMMA_SPLITTER = Splitter.on(",");
   private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int INDENT = 2;
   private static final int NOT_FOUND = -1;

   private NbtUtils() {
   }

   @Nullable
   public static GameProfile readGameProfile(CompoundTag param0) {
      String var1 = null;
      UUID var2 = null;
      if (param0.contains("Name", 8)) {
         var1 = param0.getString("Name");
      }

      if (param0.hasUUID("Id")) {
         var2 = param0.getUUID("Id");
      }

      try {
         GameProfile var3 = new GameProfile(var2, var1);
         if (param0.contains("Properties", 10)) {
            CompoundTag var4 = param0.getCompound("Properties");

            for (String var6 : var4.getAllKeys()) {
               ListTag var7 = var4.getList(var6, 10);

               for (int var8 = 0; var8 < var7.size(); var8++) {
                  CompoundTag var9 = var7.getCompound(var8);
                  String var10 = var9.getString("Value");
                  if (var9.contains("Signature", 8)) {
                     var3.getProperties().put(var6, new com.mojang.authlib.properties.Property(var6, var10, var9.getString("Signature")));
                  } else {
                     var3.getProperties().put(var6, new com.mojang.authlib.properties.Property(var6, var10));
                  }
               }
            }
         }

         return var3;
      } catch (Throwable var11) {
         return null;
      }
   }

   public static CompoundTag writeGameProfile(CompoundTag param0, GameProfile param1) {
      if (!StringUtil.isNullOrEmpty(param1.getName())) {
         param0.putString("Name", param1.getName());
      }

      if (param1.getId() != null) {
         param0.putUUID("Id", param1.getId());
      }

      if (!param1.getProperties().isEmpty()) {
         CompoundTag var2 = new CompoundTag();

         for (String var4 : param1.getProperties().keySet()) {
            ListTag var5 = new ListTag();

            for (com.mojang.authlib.properties.Property var7 : param1.getProperties().get(var4)) {
               CompoundTag var8 = new CompoundTag();
               var8.putString("Value", var7.getValue());
               if (var7.hasSignature()) {
                  var8.putString("Signature", var7.getSignature());
               }

               var5.add(var8);
            }

            var2.put(var4, var5);
         }

         param0.put("Properties", var2);
      }

      return param0;
   }

   @VisibleForTesting
   public static boolean compareNbt(@Nullable Tag param0, @Nullable Tag param1, boolean param2) {
      if (param0 == param1) {
         return true;
      }

      if (param0 == null) {
         return true;
      }

      if (param1 == null) {
         return false;
      }

      if (!param0.getClass().equals(param1.getClass())) {
         return false;
      }

      if (param0 instanceof CompoundTag var3) {
         CompoundTag var4 = (CompoundTag)param1;

         for (String var6 : var3.getAllKeys()) {
            Tag var7 = var3.get(var6);
            if (!compareNbt(var7, var4.get(var6), param2)) {
               return false;
            }
         }

         return true;
      } else if (param0 instanceof ListTag && param2) {
         ListTag var3 = (ListTag)param0;
         ListTag var4 = (ListTag)param1;
         if (var3.isEmpty()) {
            return var4.isEmpty();
         }

         for (int var5 = 0; var5 < var3.size(); var5++) {
            Tag var6 = var3.get(var5);
            boolean var7 = false;

            for (int var8 = 0; var8 < var4.size(); var8++) {
               if (compareNbt(var6, var4.get(var8), param2)) {
                  var7 = true;
                  break;
               }
            }

            if (!var7) {
               return false;
            }
         }

         return true;
      } else {
         return param0.equals(param1);
      }
   }

   public static IntArrayTag createUUID(UUID param0) {
      return new IntArrayTag(UUIDUtil.uuidToIntArray(param0));
   }

   public static UUID loadUUID(Tag param0) {
      if (param0.getType() != IntArrayTag.TYPE) {
         throw new IllegalArgumentException("Expected UUID-Tag to be of type " + IntArrayTag.TYPE.getName() + ", but found " + param0.getType().getName() + ".");
      } else {
         int[] var1 = ((IntArrayTag)param0).getAsIntArray();
         if (var1.length != 4) {
            throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + var1.length + ".");
         } else {
            return UUIDUtil.uuidFromIntArray(var1);
         }
      }
   }

   public static BlockPos readBlockPos(CompoundTag param0) {
      return new BlockPos(param0.getInt("X"), param0.getInt("Y"), param0.getInt("Z"));
   }

   public static CompoundTag writeBlockPos(BlockPos param0) {
      CompoundTag var1 = new CompoundTag();
      var1.putInt("X", param0.getX());
      var1.putInt("Y", param0.getY());
      var1.putInt("Z", param0.getZ());
      return var1;
   }

   public static BlockState readBlockState(HolderGetter<Block> param0, CompoundTag param1) {
      if (!param1.contains("Name", 8)) {
         return Blocks.AIR.defaultBlockState();
      }

      ResourceLocation var2 = new ResourceLocation(param1.getString("Name"));
      Optional<? extends Holder<Block>> var3 = param0.get(ResourceKey.create(Registries.BLOCK, var2));
      if (var3.isEmpty()) {
         return Blocks.AIR.defaultBlockState();
      }

      Block var4 = var3.get().value();
      BlockState var5 = var4.defaultBlockState();
      if (param1.contains("Properties", 10)) {
         CompoundTag var6 = param1.getCompound("Properties");
         StateDefinition<Block, BlockState> var7 = var4.getStateDefinition();

         for (String var9 : var6.getAllKeys()) {
            Property<?> var10 = var7.getProperty(var9);
            if (var10 != null) {
               var5 = setValueHelper(var5, var10, var9, var6, param1);
            }
         }
      }

      return var5;
   }

   private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(
      S param0, Property<T> param1, String param2, CompoundTag param3, CompoundTag param4
   ) {
      Optional<T> var5 = param1.getValue(param3.getString(param2));
      if (var5.isPresent()) {
         return param0.setValue(param1, var5.get());
      }

      LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", param2, param3.getString(param2), param4.toString());
      return param0;
   }

   public static CompoundTag writeBlockState(BlockState param0) {
      CompoundTag var1 = new CompoundTag();
      var1.putString("Name", BuiltInRegistries.BLOCK.getKey(param0.getBlock()).toString());
      ImmutableMap<Property<?>, Comparable<?>> var2 = param0.getValues();
      if (!var2.isEmpty()) {
         CompoundTag var3 = new CompoundTag();

         for (Entry<Property<?>, Comparable<?>> var5 : var2.entrySet()) {
            Property<?> var6 = var5.getKey();
            var3.putString(var6.getName(), getName(var6, var5.getValue()));
         }

         var1.put("Properties", var3);
      }

      return var1;
   }

   public static CompoundTag writeFluidState(FluidState param0) {
      CompoundTag var1 = new CompoundTag();
      var1.putString("Name", BuiltInRegistries.FLUID.getKey(param0.getType()).toString());
      ImmutableMap<Property<?>, Comparable<?>> var2 = param0.getValues();
      if (!var2.isEmpty()) {
         CompoundTag var3 = new CompoundTag();

         for (Entry<Property<?>, Comparable<?>> var5 : var2.entrySet()) {
            Property<?> var6 = var5.getKey();
            var3.putString(var6.getName(), getName(var6, var5.getValue()));
         }

         var1.put("Properties", var3);
      }

      return var1;
   }

   private static <T extends Comparable<T>> String getName(Property<T> param0, Comparable<?> param1) {
      return param0.getName((T)param1);
   }

   public static String prettyPrint(Tag param0) {
      return prettyPrint(param0, false);
   }

   public static String prettyPrint(Tag param0, boolean param1) {
      return prettyPrint(new StringBuilder(), param0, 0, param1).toString();
   }

   public static StringBuilder prettyPrint(StringBuilder param0, Tag param1, int param2, boolean param3) {
      switch (param1.getId()) {
         case 0:
            break;
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 8:
            param0.append(param1);
            break;
         case 7:
            ByteArrayTag var4 = (ByteArrayTag)param1;
            byte[] var5 = var4.getAsByteArray();
            int var6 = var5.length;
            indent(param2, param0).append("byte[").append(var6).append("] {\n");
            if (param3) {
               indent(param2 + 1, param0);

               for (int var7 = 0; var7 < var5.length; var7++) {
                  if (var7 != 0) {
                     param0.append(',');
                  }

                  if (var7 % 16 == 0 && var7 / 16 > 0) {
                     param0.append('\n');
                     if (var7 < var5.length) {
                        indent(param2 + 1, param0);
                     }
                  } else if (var7 != 0) {
                     param0.append(' ');
                  }

                  param0.append(String.format(Locale.ROOT, "0x%02X", var5[var7] & 255));
               }
            } else {
               indent(param2 + 1, param0).append(" // Skipped, supply withBinaryBlobs true");
            }

            param0.append('\n');
            indent(param2, param0).append('}');
            break;
         case 9:
            ListTag var4_4 = (ListTag)param1;
            int var5_4 = var4_4.size();
            int var6_4 = var4_4.getElementType();
            String var7 = var6_4 == 0 ? "undefined" : TagTypes.getType(var6_4).getPrettyName();
            indent(param2, param0).append("list<").append(var7).append(">[").append(var5_4).append("] [");
            if (var5_4 != 0) {
               param0.append('\n');
            }

            for (int var8 = 0; var8 < var5_4; var8++) {
               if (var8 != 0) {
                  param0.append(",\n");
               }

               indent(param2 + 1, param0);
               prettyPrint(param0, var4_4.get(var8), param2 + 1, param3);
            }

            if (var5_4 != 0) {
               param0.append('\n');
            }

            indent(param2, param0).append(']');
            break;
         case 10:
            CompoundTag var4_3 = (CompoundTag)param1;
            List<String> var5_3 = Lists.newArrayList(var4_3.getAllKeys());
            Collections.sort(var5_3);
            indent(param2, param0).append('{');
            if (param0.length() - param0.lastIndexOf("\n") > 2 * (param2 + 1)) {
               param0.append('\n');
               indent(param2 + 1, param0);
            }

            int var6_3 = var5_3.stream().mapToInt(String::length).max().orElse(0);
            String var7_2 = Strings.repeat(" ", var6_3);

            for (int var8 = 0; var8 < var5_3.size(); var8++) {
               if (var8 != 0) {
                  param0.append(",\n");
               }

               String var9 = var5_3.get(var8);
               indent(param2 + 1, param0).append('"').append(var9).append('"').append(var7_2, 0, var7_2.length() - var9.length()).append(": ");
               prettyPrint(param0, var4_3.get(var9), param2 + 1, param3);
            }

            if (!var5_3.isEmpty()) {
               param0.append('\n');
            }

            indent(param2, param0).append('}');
            break;
         case 11:
            IntArrayTag var4_2 = (IntArrayTag)param1;
            int[] var5_2 = var4_2.getAsIntArray();
            int var6_2 = 0;

            for (int var10 : var5_2) {
               var6_2 = Math.max(var6_2, String.format(Locale.ROOT, "%X", var10).length());
            }

            int var7_1 = var5_2.length;
            indent(param2, param0).append("int[").append(var7_1).append("] {\n");
            if (param3) {
               indent(param2 + 1, param0);

               for (int var8 = 0; var8 < var5_2.length; var8++) {
                  if (var8 != 0) {
                     param0.append(',');
                  }

                  if (var8 % 16 == 0 && var8 / 16 > 0) {
                     param0.append('\n');
                     if (var8 < var5_2.length) {
                        indent(param2 + 1, param0);
                     }
                  } else if (var8 != 0) {
                     param0.append(' ');
                  }

                  param0.append(String.format(Locale.ROOT, "0x%0" + var6_2 + "X", var5_2[var8]));
               }
            } else {
               indent(param2 + 1, param0).append(" // Skipped, supply withBinaryBlobs true");
            }

            param0.append('\n');
            indent(param2, param0).append('}');
            break;
         case 12:
            LongArrayTag var4_1 = (LongArrayTag)param1;
            long[] var5_1 = var4_1.getAsLongArray();
            long var6_1 = 0L;

            for (long var11 : var5_1) {
               var6_1 = Math.max(var6_1, String.format(Locale.ROOT, "%X", var11).length());
            }

            long var8 = var5_1.length;
            indent(param2, param0).append("long[").append(var8).append("] {\n");
            if (param3) {
               indent(param2 + 1, param0);

               for (int var10 = 0; var10 < var5_1.length; var10++) {
                  if (var10 != 0) {
                     param0.append(',');
                  }

                  if (var10 % 16 == 0 && var10 / 16 > 0) {
                     param0.append('\n');
                     if (var10 < var5_1.length) {
                        indent(param2 + 1, param0);
                     }
                  } else if (var10 != 0) {
                     param0.append(' ');
                  }

                  param0.append(String.format(Locale.ROOT, "0x%0" + var6_1 + "X", var5_1[var10]));
               }
            } else {
               indent(param2 + 1, param0).append(" // Skipped, supply withBinaryBlobs true");
            }

            param0.append('\n');
            indent(param2, param0).append('}');
            break;
         default:
            param0.append("<UNKNOWN :(>");
      }

      return param0;
   }

   private static StringBuilder indent(int param0, StringBuilder param1) {
      int var2 = param1.lastIndexOf("\n") + 1;
      int var3 = param1.length() - var2;

      for (int var4 = 0; var4 < 2 * param0 - var3; var4++) {
         param1.append(' ');
      }

      return param1;
   }

   public static Component toPrettyComponent(Tag param0) {
      return new TextComponentTagVisitor("", 0).visit(param0);
   }

   public static String structureToSnbt(CompoundTag param0) {
      return new SnbtPrinterTagVisitor().visit(packStructureTemplate(param0));
   }

   public static CompoundTag snbtToStructure(String param0) throws CommandSyntaxException {
      return unpackStructureTemplate(TagParser.parseTag(param0));
   }

   @VisibleForTesting
   static CompoundTag packStructureTemplate(CompoundTag param0) {
      boolean var2 = param0.contains("palettes", 9);
      ListTag var1;
      if (var2) {
         var1 = param0.getList("palettes", 9).getList(0);
      } else {
         var1 = param0.getList("palette", 10);
      }

      ListTag var3 = var1.stream()
         .map(CompoundTag.class::cast)
         .map(NbtUtils::packBlockState)
         .map(StringTag::valueOf)
         .collect(Collectors.toCollection(ListTag::new));
      param0.put("palette", var3);
      if (var2) {
         ListTag var4 = new ListTag();
         ListTag var5 = param0.getList("palettes", 9);
         var5.stream().map(ListTag.class::cast).forEach(l_param2 -> {
            CompoundTag l_var3 = new CompoundTag();

            for (int l_var4 = 0; l_var4 < l_param2.size(); l_var4++) {
               l_var3.putString(var3.getString(l_var4), packBlockState(l_param2.getCompound(l_var4)));
            }

            var4.add(l_var3);
         });
         param0.put("palettes", var4);
      }

      if (param0.contains("entities", 9)) {
         ListTag var4 = param0.getList("entities", 10);
         ListTag var5 = var4.stream()
            .map(CompoundTag.class::cast)
            .sorted(Comparator.comparing(l_param0 -> l_param0.getList("pos", 6), YXZ_LISTTAG_DOUBLE_COMPARATOR))
            .collect(Collectors.toCollection(ListTag::new));
         param0.put("entities", var5);
      }

      ListTag var4 = param0.getList("blocks", 10)
         .stream()
         .map(CompoundTag.class::cast)
         .sorted(Comparator.comparing(l_param0 -> l_param0.getList("pos", 3), YXZ_LISTTAG_INT_COMPARATOR))
         .peek(l_param1 -> l_param1.putString("state", var3.getString(l_param1.getInt("state"))))
         .collect(Collectors.toCollection(ListTag::new));
      param0.put("data", var4);
      param0.remove("blocks");
      return param0;
   }

   @VisibleForTesting
   static CompoundTag unpackStructureTemplate(CompoundTag param0) {
      ListTag var1 = param0.getList("palette", 8);
      Map<String, Tag> var2 = var1.stream()
         .map(StringTag.class::cast)
         .map(StringTag::getAsString)
         .collect(ImmutableMap.toImmutableMap(Function.identity(), NbtUtils::unpackBlockState));
      if (param0.contains("palettes", 9)) {
         param0.put(
            "palettes",
            param0.getList("palettes", 10)
               .stream()
               .map(CompoundTag.class::cast)
               .map(l_param1 -> var2.keySet().stream().map(l_param1::getString).map(NbtUtils::unpackBlockState).collect(Collectors.toCollection(ListTag::new)))
               .collect(Collectors.toCollection(ListTag::new))
         );
         param0.remove("palette");
      } else {
         param0.put("palette", var2.values().stream().collect(Collectors.toCollection(ListTag::new)));
      }

      if (param0.contains("data", 9)) {
         Object2IntMap<String> var3 = new Object2IntOpenHashMap<>();
         var3.defaultReturnValue(-1);

         for (int var4 = 0; var4 < var1.size(); var4++) {
            var3.put(var1.getString(var4), var4);
         }

         ListTag var4 = param0.getList("data", 10);

         for (int var5 = 0; var5 < var4.size(); var5++) {
            CompoundTag var6 = var4.getCompound(var5);
            String var7 = var6.getString("state");
            int var8 = var3.getInt(var7);
            if (var8 == -1) {
               throw new IllegalStateException("Entry " + var7 + " missing from palette");
            }

            var6.putInt("state", var8);
         }

         param0.put("blocks", var4);
         param0.remove("data");
      }

      return param0;
   }

   @VisibleForTesting
   static String packBlockState(CompoundTag param0) {
      StringBuilder var1 = new StringBuilder(param0.getString("Name"));
      if (param0.contains("Properties", 10)) {
         CompoundTag var2 = param0.getCompound("Properties");
         String var3 = var2.getAllKeys().stream().sorted().map(l_param1 -> l_param1 + ":" + var2.get(l_param1).getAsString()).collect(Collectors.joining(","));
         var1.append('{').append(var3).append('}');
      }

      return var1.toString();
   }

   @VisibleForTesting
   static CompoundTag unpackBlockState(String param0) {
      CompoundTag var1 = new CompoundTag();
      int var2 = param0.indexOf(123);
      String var3;
      if (var2 >= 0) {
         var3 = param0.substring(0, var2);
         CompoundTag var4 = new CompoundTag();
         if (var2 + 2 <= param0.length()) {
            String var5 = param0.substring(var2 + 1, param0.indexOf(125, var2));
            COMMA_SPLITTER.split(var5).forEach(l_param2 -> {
               List<String> l_var3 = COLON_SPLITTER.splitToList(l_param2);
               if (l_var3.size() == 2) {
                  var4.putString(l_var3.get(0), l_var3.get(1));
               } else {
                  LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", param0);
               }
            });
            var1.put("Properties", var4);
         }
      } else {
         var3 = param0;
      }

      var1.putString("Name", var3);
      return var1;
   }

   public static CompoundTag addCurrentDataVersion(CompoundTag param0) {
      int var1 = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
      return addDataVersion(param0, var1);
   }

   public static CompoundTag addDataVersion(CompoundTag param0, int param1) {
      param0.putInt("DataVersion", param1);
      return param0;
   }

   public static int getDataVersion(CompoundTag param0, int param1) {
      return param0.contains("DataVersion", 99) ? param0.getInt("DataVersion") : param1;
   }
}
