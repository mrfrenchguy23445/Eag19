package net.minecraft.stats;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.lax1dude.eaglercraft.v1_8.FileUtils;
import org.slf4j.Logger;

public class ServerStatsCounter extends StatsCounter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final MinecraftServer server;
   private final File file;
   private final Set<Stat<?>> dirty = Sets.newHashSet();

   public ServerStatsCounter(MinecraftServer param0, File param1) {
      this.server = param0;
      this.file = param1;
      if (param1.isFile()) {
         try {
            this.parseLocal(param0.getFixerUpper(), FileUtils.readFileToString(param1));
         } catch (IOException var3) {
            LOGGER.error("Couldn't read statistics file {}", param1, var3);
         } catch (JsonParseException var3) {
            LOGGER.error("Couldn't parse statistics file {}", param1, var3);
         }
      }
   }

   public void save() {
      try {
         FileUtils.writeStringToFile(this.file, this.toJson());
      } catch (IOException var1) {
         LOGGER.error("Couldn't save stats", var1);
      }
   }

   @Override
   public void setValue(Player param0, Stat<?> param1, int param2) {
      super.setValue(param0, param1, param2);
      this.dirty.add(param1);
   }

   private Set<Stat<?>> getDirty() {
      Set<Stat<?>> var1 = Sets.newHashSet(this.dirty);
      this.dirty.clear();
      return var1;
   }

   public void parseLocal(DataFixer param0, String param1) {
      try (JsonReader var3 = new JsonReader(new StringReader(param1))) {
         var3.setLenient(false);
         JsonElement var4 = Streams.parse(var3);
         if (!var4.isJsonNull()) {
            CompoundTag var5 = fromJson(var4.getAsJsonObject());
            var5 = DataFixTypes.STATS.updateToCurrentVersion(param0, var5, NbtUtils.getDataVersion(var5, 1343));
            if (var5.contains("stats", 10)) {
               CompoundTag var6 = var5.getCompound("stats");

               for (String var8 : var6.getAllKeys()) {
                  if (var6.contains(var8, 10)) {
                     Util.ifElse(
                        BuiltInRegistries.STAT_TYPE.getOptional(new ResourceLocation(var8)),
                        l_param2 -> {
                           CompoundTag l_var4 = var6.getCompound(var8);

                           for (String l_var6 : l_var4.getAllKeys()) {
                              if (l_var4.contains(l_var6, 99)) {
                                 Util.ifElse(
                                    this.getStat(l_param2, l_var6),
                                    l2_param2 -> this.stats.put(l2_param2, l_var4.getInt(l_var6)),
                                    () -> LOGGER.warn("Invalid statistic in {}: Don't know what {} is", this.file, l_var6)
                                 );
                              } else {
                                 LOGGER.warn("Invalid statistic value in {}: Don't know what {} is for key {}", this.file, l_var4.get(l_var6), l_var6);
                              }
                           }
                        },
                        () -> LOGGER.warn("Invalid statistic type in {}: Don't know what {} is", this.file, var8)
                     );
                  }
               }
            }
         } else {
            LOGGER.error("Unable to parse Stat data from {}", this.file);
         }
      } catch (JsonParseException | IOException var3) {
         LOGGER.error("Unable to parse Stat data from {}", this.file, var3);
      }
   }

   private <T> Optional<Stat<T>> getStat(StatType<T> param0, String param1) {
      return Optional.ofNullable(ResourceLocation.tryParse(param1)).flatMap(param0.getRegistry()::getOptional).map(param0::get);
   }

   private static CompoundTag fromJson(JsonObject param0) {
      CompoundTag var1 = new CompoundTag();

      for (Entry<String, JsonElement> var3 : param0.entrySet()) {
         JsonElement var4 = var3.getValue();
         if (var4.isJsonObject()) {
            var1.put(var3.getKey(), fromJson(var4.getAsJsonObject()));
         } else if (var4.isJsonPrimitive()) {
            JsonPrimitive var5 = var4.getAsJsonPrimitive();
            if (var5.isNumber()) {
               var1.putInt(var3.getKey(), var5.getAsInt());
            }
         }
      }

      return var1;
   }

   protected String toJson() {
      Map<StatType<?>, JsonObject> var1 = Maps.newHashMap();

      for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Stat<?>> var3 : this.stats.object2IntEntrySet()) {
         Stat<?> var4 = var3.getKey();
         var1.computeIfAbsent(var4.getType(), l_param0 -> new JsonObject()).addProperty(getKey(var4).toString(), var3.getIntValue());
      }

      JsonObject var2 = new JsonObject();

      for (Entry<StatType<?>, JsonObject> var4 : var1.entrySet()) {
         var2.add(BuiltInRegistries.STAT_TYPE.getKey(var4.getKey()).toString(), var4.getValue());
      }

      JsonObject var3 = new JsonObject();
      var3.add("stats", var2);
      var3.addProperty("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
      return var3.toString();
   }

   private static <T> ResourceLocation getKey(Stat<T> param0) {
      return param0.getType().getRegistry().getKey(param0.getValue());
   }

   public void markAllDirty() {
      this.dirty.addAll(this.stats.keySet());
   }

   public void sendStats(ServerPlayer param0) {
      Object2IntMap<Stat<?>> var2 = new Object2IntOpenHashMap<>();

      for (Stat<?> var4 : this.getDirty()) {
         var2.put(var4, this.getValue(var4));
      }

      param0.connection.send(new ClientboundAwardStatsPacket(var2));
   }
}
