package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt implements DataProvider {
   @Nullable
   private static final Path DUMP_SNBT_TO = null;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput output;
   private final Iterable<Path> inputFolders;
   private final List<SnbtToNbt.Filter> filters = Lists.newArrayList();

   public SnbtToNbt(PackOutput param0, Iterable<Path> param1) {
      this.output = param0;
      this.inputFolders = param1;
   }

   public SnbtToNbt addFilter(SnbtToNbt.Filter param0) {
      this.filters.add(param0);
      return this;
   }

   private CompoundTag applyFilters(String param0, CompoundTag param1) {
      CompoundTag var3 = param1;

      for (SnbtToNbt.Filter var5 : this.filters) {
         var3 = var5.apply(param0, var3);
      }

      return var3;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput param0) {
      Path var2 = this.output.getOutputFolder();
      List<CompletableFuture<?>> var3 = Lists.newArrayList();

      for (Path var5 : this.inputFolders) {
         var3.add(
            CompletableFuture.<CompletableFuture<Void>>supplyAsync(
                  () -> {
                     try (Stream<Path> l_var4 = Files.walk(var5)) {
                        return CompletableFuture.allOf(
                           l_var4.filter(l2_param0 -> l2_param0.toString().endsWith(".snbt")).map(l2_param3 -> CompletableFuture.runAsync(() -> {
                              SnbtToNbt.TaskResult l3_var5 = this.readStructure(l2_param3, this.getName(var5, l2_param3));
                              this.storeStructureIfChanged(param0, l3_var5, var2);
                           }, Util.backgroundExecutor())).toArray(CompletableFuture[]::new)
                        );
                     } catch (Exception l_var4) {
                        throw new RuntimeException("Failed to read structure input directory, aborting", l_var4);
                     }
                  },
                  Util.backgroundExecutor()
               )
               .thenCompose(l_param0 -> (CompletionStage<Void>)l_param0)
         );
      }

      return Util.sequenceFailFast(var3);
   }

   @Override
   public final String getName() {
      return "SNBT -> NBT";
   }

   private String getName(Path param0, Path param1) {
      String var3 = param0.relativize(param1).toString().replaceAll("\\\\", "/");
      return var3.substring(0, var3.length() - ".snbt".length());
   }

   private SnbtToNbt.TaskResult readStructure(Path param0, String param1) {
      try (BufferedReader var3 = Files.newBufferedReader(param0)) {
         String var4 = IOUtils.toString(var3);
         CompoundTag var5 = this.applyFilters(param1, NbtUtils.snbtToStructure(var4));
         ByteArrayOutputStream var6 = new ByteArrayOutputStream();
         HashingOutputStream var7 = new HashingOutputStream(Hashing.sha1(), var6);
         NbtIo.writeCompressed(var5, var7);
         byte[] var8 = var6.toByteArray();
         HashCode var9 = var7.hash();
         String var10;
         if (DUMP_SNBT_TO != null) {
            var10 = NbtUtils.structureToSnbt(var5);
         } else {
            var10 = null;
         }

         return new SnbtToNbt.TaskResult(param1, var8, var10, var9);
      } catch (Throwable var3) {
         throw new SnbtToNbt.StructureConversionException(param0, var3);
      }
   }

   private void storeStructureIfChanged(CachedOutput param0, SnbtToNbt.TaskResult param1, Path param2) {
      if (param1.snbtPayload != null) {
         Path var4 = DUMP_SNBT_TO.resolve(param1.name + ".snbt");

         try {
            NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, var4, param1.snbtPayload);
         } catch (IOException var5) {
            LOGGER.error("Couldn't write structure SNBT {} at {}", param1.name, var4, var5);
         }
      }

      Path var4 = param2.resolve(param1.name + ".nbt");

      try {
         param0.writeIfNeeded(var4, param1.payload, param1.hash);
      } catch (IOException var5) {
         LOGGER.error("Couldn't write structure {} at {}", param1.name, var4, var5);
      }
   }

   @FunctionalInterface
   public interface Filter {
      CompoundTag apply(String var1, CompoundTag var2);
   }

   static class StructureConversionException extends RuntimeException {
      public StructureConversionException(Path param0, Throwable param1) {
         super(param0.toAbsolutePath().toString(), param1);
      }
   }

   record TaskResult(String name, byte[] payload, @Nullable String snbtPayload, HashCode hash) {
   }
}
