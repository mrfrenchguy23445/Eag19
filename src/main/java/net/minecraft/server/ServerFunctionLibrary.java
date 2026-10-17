package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ServerFunctionLibrary implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter LISTER = new FileToIdConverter("functions", ".mcfunction");
   private volatile Map<ResourceLocation, CommandFunction> functions = ImmutableMap.of();
   private final TagLoader<CommandFunction> tagsLoader = new TagLoader<>(this::getFunction, "tags/functions");
   private volatile Map<ResourceLocation, Collection<CommandFunction>> tags = Map.of();
   private final int functionCompilationLevel;
   private final CommandDispatcher<CommandSourceStack> dispatcher;

   public Optional<CommandFunction> getFunction(ResourceLocation param0) {
      return Optional.ofNullable(this.functions.get(param0));
   }

   public Map<ResourceLocation, CommandFunction> getFunctions() {
      return this.functions;
   }

   public Collection<CommandFunction> getTag(ResourceLocation param0) {
      return this.tags.getOrDefault(param0, List.of());
   }

   public Iterable<ResourceLocation> getAvailableTags() {
      return this.tags.keySet();
   }

   public ServerFunctionLibrary(int param0, CommandDispatcher<CommandSourceStack> param1) {
      this.functionCompilationLevel = param0;
      this.dispatcher = param1;
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier param0,
      ResourceManager param1,
      ProfilerFiller param2,
      ProfilerFiller param3,
      Executor param4,
      Executor param5
   ) {
      CompletableFuture<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> var7 = CompletableFuture.supplyAsync(() -> this.tagsLoader.load(param1), param4);
      CompletableFuture<Map<ResourceLocation, CompletableFuture<CommandFunction>>> var8 = CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(
            () -> LISTER.listMatchingResources(param1), param4
         )
         .thenCompose(
            l_param1 -> {
               Map<ResourceLocation, CompletableFuture<CommandFunction>> l_var3 = Maps.newHashMap();
               CommandSourceStack l_var4 = new CommandSourceStack(
                  CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, this.functionCompilationLevel, "", CommonComponents.EMPTY, null, null
               );

               for (Entry<ResourceLocation, Resource> l_var6 : l_param1.entrySet()) {
                  ResourceLocation l_var7 = l_var6.getKey();
                  ResourceLocation l_var8 = LISTER.fileToId(l_var7);
                  l_var3.put(l_var8, CompletableFuture.supplyAsync(() -> {
                     List<String> l2_var4 = readLines(l_var6.getValue());
                     return CommandFunction.fromLines(l_var8, this.dispatcher, l_var4, l2_var4);
                  }, param4));
               }

               CompletableFuture<?>[] l_var5 = l_var3.values().toArray(new CompletableFuture[0]);
               return CompletableFuture.allOf(l_var5).handle((l2_param1, l2_param2) -> l_var3);
            }
         );
      return var7.thenCombine(var8, Pair::of).thenCompose(param0::wait).thenAcceptAsync(l_param0 -> {
         Map<ResourceLocation, CompletableFuture<CommandFunction>> l_var2 = (Map<ResourceLocation, CompletableFuture<CommandFunction>>)l_param0.getSecond();
         Builder<ResourceLocation, CommandFunction> l_var3 = ImmutableMap.builder();
         l_var2.forEach((l2_param1, l2_param2) -> l2_param2.handle((l3_param2, l3_param3) -> {
            if (l3_param3 != null) {
               LOGGER.error("Failed to load function {}", l2_param1, l3_param3);
            } else {
               l_var3.put(l2_param1, l3_param2);
            }

            return null;
         }).join());
         this.functions = l_var3.build();
         this.tags = this.tagsLoader.build((Map<ResourceLocation, List<TagLoader.EntryWithSource>>)l_param0.getFirst());
      }, param5);
   }

   private static List<String> readLines(Resource param0) {
      try (BufferedReader var1 = param0.openAsReader()) {
         return var1.lines().toList();
      } catch (IOException var1) {
         throw new CompletionException(var1);
      }
   }
}
