package net.minecraft.data.structures;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.slf4j.Logger;

public class NbtToSnbt implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Iterable<Path> inputFolders;
   private final PackOutput output;

   public NbtToSnbt(PackOutput param0, Collection<Path> param1) {
      this.inputFolders = param1;
      this.output = param0;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput param0) {
      Path var2 = this.output.getOutputFolder();
      List<CompletableFuture<?>> var3 = new ArrayList<>();

      for (Path var5 : this.inputFolders) {
         var3.add(
            CompletableFuture.<CompletableFuture<Void>>supplyAsync(
                  () -> {
                     try (Stream<Path> l_var3 = Files.walk(var5)) {
                        return CompletableFuture.allOf(
                           l_var3.filter(l2_param0 -> l2_param0.toString().endsWith(".nbt"))
                              .map(
                                 l2_param3 -> CompletableFuture.runAsync(
                                    () -> convertStructure(param0, l2_param3, getName(var5, l2_param3), var2), Util.ioPool()
                                 )
                              )
                              .toArray(CompletableFuture[]::new)
                        );
                     } catch (IOException l_var3) {
                        LOGGER.error("Failed to read structure input directory", l_var3);
                        return CompletableFuture.completedFuture(null);
                     }
                  },
                  Util.backgroundExecutor()
               )
               .thenCompose(l_param0 -> (CompletionStage<Void>)l_param0)
         );
      }

      return CompletableFuture.allOf(var3.toArray(CompletableFuture[]::new));
   }

   @Override
   public final String getName() {
      return "NBT -> SNBT";
   }

   private static String getName(Path param0, Path param1) {
      String var2 = param0.relativize(param1).toString().replaceAll("\\\\", "/");
      return var2.substring(0, var2.length() - ".nbt".length());
   }

   @Nullable
   public static Path convertStructure(CachedOutput param0, Path param1, String param2, Path param3) {
      try (InputStream var4 = Files.newInputStream(param1)) {
         Path var5 = param3.resolve(param2 + ".snbt");
         writeSnbt(param0, var5, NbtUtils.structureToSnbt(NbtIo.readCompressed(var4)));
         LOGGER.info("Converted {} from NBT to SNBT", param2);
         return var5;
      } catch (IOException var4) {
         LOGGER.error("Couldn't convert {} from NBT to SNBT at {}", param2, param1, var4);
         return null;
      }
   }

   public static void writeSnbt(CachedOutput param0, Path param1, String param2) throws IOException {
      ByteArrayOutputStream var3 = new ByteArrayOutputStream();
      HashingOutputStream var4 = new HashingOutputStream(Hashing.sha1(), var3);
      var4.write(param2.getBytes(StandardCharsets.UTF_8));
      var4.write(10);
      param0.writeIfNeeded(param1, var3.toByteArray(), var4.hash());
   }
}
