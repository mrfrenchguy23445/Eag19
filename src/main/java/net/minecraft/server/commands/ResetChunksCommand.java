package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.datafixers.util.Unit;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ResetChunksCommand {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("resetchunks")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .executes(l_param0 -> resetChunks(l_param0.getSource(), 0, true))
            .then(
               Commands.argument("range", IntegerArgumentType.integer(0, 5))
                  .executes(l_param0 -> resetChunks(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "range"), true))
                  .then(
                     Commands.argument("skipOldChunks", BoolArgumentType.bool())
                        .executes(
                           l_param0 -> resetChunks(
                              l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "range"), BoolArgumentType.getBool(l_param0, "skipOldChunks")
                           )
                        )
                  )
            )
      );
   }

   private static int resetChunks(CommandSourceStack param0, int param1, boolean param2) {
      ServerLevel var3 = param0.getLevel();
      ServerChunkCache var4 = var3.getChunkSource();
      var4.chunkMap.debugReloadGenerator();
      Vec3 var5 = param0.getPosition();
      ChunkPos var6 = new ChunkPos(BlockPos.containing(var5));
      int var7 = var6.z - param1;
      int var8 = var6.z + param1;
      int var9 = var6.x - param1;
      int var10 = var6.x + param1;

      for (int var11 = var7; var11 <= var8; var11++) {
         for (int var12 = var9; var12 <= var10; var12++) {
            ChunkPos var13 = new ChunkPos(var12, var11);
            LevelChunk var14 = var4.getChunk(var12, var11, false);
            if (var14 != null && (!param2 || !var14.isOldNoiseGeneration())) {
               for (BlockPos var16 : BlockPos.betweenClosed(
                  var13.getMinBlockX(),
                  var3.getMinBuildHeight(),
                  var13.getMinBlockZ(),
                  var13.getMaxBlockX(),
                  var3.getMaxBuildHeight() - 1,
                  var13.getMaxBlockZ()
               )) {
                  var3.setBlock(var16, Blocks.AIR.defaultBlockState(), 16);
               }
            }
         }
      }

      ProcessorMailbox<Runnable> var11 = ProcessorMailbox.create(Util.backgroundExecutor(), "worldgen-resetchunks");
      long var12 = System.currentTimeMillis();
      int var14 = (param1 * 2 + 1) * (param1 * 2 + 1);

      for (ChunkStatus var16 : ImmutableList.of(
         ChunkStatus.BIOMES, ChunkStatus.NOISE, ChunkStatus.SURFACE, ChunkStatus.CARVERS, ChunkStatus.LIQUID_CARVERS, ChunkStatus.FEATURES
      )) {
         long var17 = System.currentTimeMillis();
         CompletableFuture<Unit> var19 = CompletableFuture.supplyAsync(() -> Unit.INSTANCE, var11::tell);

         for (int var20 = var6.z - param1; var20 <= var6.z + param1; var20++) {
            for (int var21 = var6.x - param1; var21 <= var6.x + param1; var21++) {
               ChunkPos var22 = new ChunkPos(var21, var20);
               LevelChunk var23 = var4.getChunk(var21, var20, false);
               if (var23 != null && (!param2 || !var23.isOldNoiseGeneration())) {
                  List<ChunkAccess> var24 = Lists.newArrayList();
                  int var25 = Math.max(1, var16.getRange());

                  for (int var26 = var22.z - var25; var26 <= var22.z + var25; var26++) {
                     for (int var27 = var22.x - var25; var27 <= var22.x + var25; var27++) {
                        ChunkAccess var28 = var4.getChunk(var27, var26, var16.getParent(), true);
                        ChunkAccess var29;
                        if (var28 instanceof ImposterProtoChunk) {
                           var29 = new ImposterProtoChunk(((ImposterProtoChunk)var28).getWrapped(), true);
                        } else if (var28 instanceof LevelChunk) {
                           var29 = new ImposterProtoChunk((LevelChunk)var28, true);
                        } else {
                           var29 = var28;
                        }

                        var24.add(var29);
                     }
                  }

                  var19 = var19.thenComposeAsync(
                     l_param5 -> var16.generate(var11::tell, var3, var4.getGenerator(), var3.getStructureManager(), var4.getLightEngine(), l2_param0 -> {
                        throw new UnsupportedOperationException("Not creating full chunks here");
                     }, var24, true).thenApply(l2_param1 -> {
                        if (var16 == ChunkStatus.NOISE) {
                           l2_param1.left().ifPresent(l3_param0 -> Heightmap.primeHeightmaps(l3_param0, ChunkStatus.POST_FEATURES));
                        }

                        return Unit.INSTANCE;
                     }), var11::tell
                  );
               }
            }
         }

         param0.getServer().managedBlock(var19::isDone);
         LOGGER.debug(var16.getName() + " took " + (System.currentTimeMillis() - var17) + " ms");
      }

      long var15 = System.currentTimeMillis();

      for (int var17 = var6.z - param1; var17 <= var6.z + param1; var17++) {
         for (int var18 = var6.x - param1; var18 <= var6.x + param1; var18++) {
            ChunkPos var19 = new ChunkPos(var18, var17);
            LevelChunk var20 = var4.getChunk(var18, var17, false);
            if (var20 != null && (!param2 || !var20.isOldNoiseGeneration())) {
               for (BlockPos var22 : BlockPos.betweenClosed(
                  var19.getMinBlockX(),
                  var3.getMinBuildHeight(),
                  var19.getMinBlockZ(),
                  var19.getMaxBlockX(),
                  var3.getMaxBuildHeight() - 1,
                  var19.getMaxBlockZ()
               )) {
                  var4.blockChanged(var22);
               }
            }
         }
      }

      LOGGER.debug("blockChanged took " + (System.currentTimeMillis() - var15) + " ms");
      long var17 = System.currentTimeMillis() - var12;
      param0.sendSuccess(
         Component.literal(
            String.format(
               Locale.ROOT, "%d chunks have been reset. This took %d ms for %d chunks, or %02f ms per chunk", var14, var17, var14, (float)var17 / var14
            )
         ),
         true
      );
      return 1;
   }
}
