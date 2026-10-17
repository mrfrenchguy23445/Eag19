package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.slf4j.Logger;

public class SectionStorage<R> implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String SECTIONS_TAG = "Sections";
   private final IOWorker worker;
   private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectOpenHashMap<>();
   private final LongLinkedOpenHashSet dirty = new LongLinkedOpenHashSet();
   private final Function<Runnable, Codec<R>> codec;
   private final Function<Runnable, R> factory;
   private final DataFixer fixerUpper;
   private final DataFixTypes type;
   private final RegistryAccess registryAccess;
   protected final LevelHeightAccessor levelHeightAccessor;

   public SectionStorage(
      Path param0,
      Function<Runnable, Codec<R>> param1,
      Function<Runnable, R> param2,
      DataFixer param3,
      DataFixTypes param4,
      boolean param5,
      RegistryAccess param6,
      LevelHeightAccessor param7
   ) {
      this.codec = param1;
      this.factory = param2;
      this.fixerUpper = param3;
      this.type = param4;
      this.registryAccess = param6;
      this.levelHeightAccessor = param7;
      this.worker = new IOWorker(param0, param5, param0.getFileName().toString());
   }

   protected void tick(BooleanSupplier param0) {
      while (this.hasWork() && param0.getAsBoolean()) {
         ChunkPos var2 = SectionPos.of(this.dirty.firstLong()).chunk();
         this.writeColumn(var2);
      }
   }

   public boolean hasWork() {
      return !this.dirty.isEmpty();
   }

   @Nullable
   protected Optional<R> get(long param0) {
      return this.storage.get(param0);
   }

   protected Optional<R> getOrLoad(long param0) {
      if (this.outsideStoredRange(param0)) {
         return Optional.empty();
      } else {
         Optional<R> var3 = this.get(param0);
         if (var3 != null) {
            return var3;
         } else {
            this.readColumn(SectionPos.of(param0).chunk());
            var3 = this.get(param0);
            if (var3 == null) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
            } else {
               return var3;
            }
         }
      }
   }

   protected boolean outsideStoredRange(long param0) {
      int var3 = SectionPos.sectionToBlockCoord(SectionPos.y(param0));
      return this.levelHeightAccessor.isOutsideBuildHeight(var3);
   }

   protected R getOrCreate(long param0) {
      if (this.outsideStoredRange(param0)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("sectionPos out of bounds"));
      }

      Optional<R> var3 = this.getOrLoad(param0);
      if (var3.isPresent()) {
         return var3.get();
      }

      R var4 = this.factory.apply(() -> this.setDirty(param0));
      this.storage.put(param0, Optional.of(var4));
      return var4;
   }

   private void readColumn(ChunkPos param0) {
      Optional<CompoundTag> var2 = this.tryRead(param0).join();
      RegistryOps<Tag> var3 = RegistryOps.create(NbtOps.INSTANCE, this.registryAccess);
      this.readColumn(param0, var3, var2.orElse(null));
   }

   private CompletableFuture<Optional<CompoundTag>> tryRead(ChunkPos param0) {
      return this.worker.loadAsync(param0).exceptionally(l_param1 -> {
         if (l_param1 instanceof IOException l_var2) {
            LOGGER.error("Error reading chunk {} data from disk", param0, l_var2);
            return Optional.empty();
         } else {
            throw new CompletionException(l_param1);
         }
      });
   }

   private <T> void readColumn(ChunkPos param0, DynamicOps<T> param1, @Nullable T param2) {
      if (param2 == null) {
         for (int var4 = this.levelHeightAccessor.getMinSection(); var4 < this.levelHeightAccessor.getMaxSection(); var4++) {
            this.storage.put(getKey(param0, var4), Optional.empty());
         }
      } else {
         Dynamic<T> var4 = new Dynamic<>(param1, param2);
         int var5 = getVersion(var4);
         int var6 = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
         boolean var7 = var5 != var6;
         Dynamic<T> var8 = this.type.update(this.fixerUpper, var4, var5, var6);
         OptionalDynamic<T> var9 = var8.get("Sections");

         for (int var10 = this.levelHeightAccessor.getMinSection(); var10 < this.levelHeightAccessor.getMaxSection(); var10++) {
            long var11 = getKey(param0, var10);
            Optional<R> var13 = var9.get(Integer.toString(var10))
               .result()
               .flatMap(l_param2 -> this.codec.apply(() -> this.setDirty(var11)).parse((Dynamic<T>)l_param2).resultOrPartial(LOGGER::error));
            this.storage.put(var11, var13);
            var13.ifPresent(l_param3 -> {
               this.onSectionLoad(var11);
               if (var7) {
                  this.setDirty(var11);
               }
            });
         }
      }
   }

   private void writeColumn(ChunkPos param0) {
      RegistryOps<Tag> var2 = RegistryOps.create(NbtOps.INSTANCE, this.registryAccess);
      Dynamic<Tag> var3 = this.writeColumn(param0, var2);
      Tag var4 = var3.getValue();
      if (var4 instanceof CompoundTag) {
         this.worker.store(param0, (CompoundTag)var4);
      } else {
         LOGGER.error("Expected compound tag, got {}", var4);
      }
   }

   private <T> Dynamic<T> writeColumn(ChunkPos param0, DynamicOps<T> param1) {
      Map<T, T> var3 = Maps.newHashMap();

      for (int var4 = this.levelHeightAccessor.getMinSection(); var4 < this.levelHeightAccessor.getMaxSection(); var4++) {
         long var5 = getKey(param0, var4);
         this.dirty.remove(var5);
         Optional<R> var7 = this.storage.get(var5);
         if (var7 != null && var7.isPresent()) {
            DataResult<T> var8 = this.codec.apply(() -> this.setDirty(var5)).encodeStart(param1, var7.get());
            String var9 = Integer.toString(var4);
            var8.resultOrPartial(LOGGER::error).ifPresent(l_param3 -> var3.put(param1.createString(var9), (T)l_param3));
         }
      }

      return new Dynamic<>(
         param1,
         param1.createMap(
            ImmutableMap.of(
               param1.createString("Sections"),
               param1.createMap(var3),
               param1.createString("DataVersion"),
               param1.createInt(SharedConstants.getCurrentVersion().getDataVersion().getVersion())
            )
         )
      );
   }

   private static long getKey(ChunkPos param0, int param1) {
      return SectionPos.asLong(param0.x, param1, param0.z);
   }

   protected void onSectionLoad(long param0) {
   }

   protected void setDirty(long param0) {
      Optional<R> var3 = this.storage.get(param0);
      if (var3 != null && var3.isPresent()) {
         this.dirty.add(param0);
      } else {
         LOGGER.warn("No data for position: {}", SectionPos.of(param0));
      }
   }

   private static int getVersion(Dynamic<?> param0) {
      return param0.get("DataVersion").asInt(1945);
   }

   public void flush(ChunkPos param0) {
      if (this.hasWork()) {
         for (int var2 = this.levelHeightAccessor.getMinSection(); var2 < this.levelHeightAccessor.getMaxSection(); var2++) {
            long var3 = getKey(param0, var2);
            if (this.dirty.contains(var3)) {
               this.writeColumn(param0);
               return;
            }
         }
      }
   }

   @Override
   public void close() throws IOException {
      this.worker.close();
   }
}
