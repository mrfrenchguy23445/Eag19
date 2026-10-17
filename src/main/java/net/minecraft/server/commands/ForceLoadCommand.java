package net.minecraft.server.commands;

import com.google.common.base.Joiner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ForceLoadCommand {
   private static final int MAX_CHUNK_LIMIT = 256;
   private static final Dynamic2CommandExceptionType ERROR_TOO_MANY_CHUNKS = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.forceload.toobig", param0, param1)
   );
   private static final Dynamic2CommandExceptionType ERROR_NOT_TICKING = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.forceload.query.failure", param0, param1)
   );
   private static final SimpleCommandExceptionType ERROR_ALL_ADDED = new SimpleCommandExceptionType(Component.translatable("commands.forceload.added.failure"));
   private static final SimpleCommandExceptionType ERROR_NONE_REMOVED = new SimpleCommandExceptionType(
      Component.translatable("commands.forceload.removed.failure")
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("forceload")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("add")
                  .then(
                     Commands.argument("from", ColumnPosArgument.columnPos())
                        .executes(
                           l_param0 -> changeForceLoad(
                              l_param0.getSource(), ColumnPosArgument.getColumnPos(l_param0, "from"), ColumnPosArgument.getColumnPos(l_param0, "from"), true
                           )
                        )
                        .then(
                           Commands.argument("to", ColumnPosArgument.columnPos())
                              .executes(
                                 l_param0 -> changeForceLoad(
                                    l_param0.getSource(),
                                    ColumnPosArgument.getColumnPos(l_param0, "from"),
                                    ColumnPosArgument.getColumnPos(l_param0, "to"),
                                    true
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("remove")
                  .then(
                     Commands.argument("from", ColumnPosArgument.columnPos())
                        .executes(
                           l_param0 -> changeForceLoad(
                              l_param0.getSource(), ColumnPosArgument.getColumnPos(l_param0, "from"), ColumnPosArgument.getColumnPos(l_param0, "from"), false
                           )
                        )
                        .then(
                           Commands.argument("to", ColumnPosArgument.columnPos())
                              .executes(
                                 l_param0 -> changeForceLoad(
                                    l_param0.getSource(),
                                    ColumnPosArgument.getColumnPos(l_param0, "from"),
                                    ColumnPosArgument.getColumnPos(l_param0, "to"),
                                    false
                                 )
                              )
                        )
                  )
                  .then(Commands.literal("all").executes(l_param0 -> removeAll(l_param0.getSource())))
            )
            .then(
               Commands.literal("query")
                  .executes(l_param0 -> listForceLoad(l_param0.getSource()))
                  .then(
                     Commands.argument("pos", ColumnPosArgument.columnPos())
                        .executes(l_param0 -> queryForceLoad(l_param0.getSource(), ColumnPosArgument.getColumnPos(l_param0, "pos")))
                  )
            )
      );
   }

   private static int queryForceLoad(CommandSourceStack param0, ColumnPos param1) throws CommandSyntaxException {
      ChunkPos var2 = param1.toChunkPos();
      ServerLevel var3 = param0.getLevel();
      ResourceKey<Level> var4 = var3.dimension();
      boolean var5 = var3.getForcedChunks().contains(var2.toLong());
      if (var5) {
         param0.sendSuccess(Component.translatable("commands.forceload.query.success", var2, var4.location()), false);
         return 1;
      } else {
         throw ERROR_NOT_TICKING.create(var2, var4.location());
      }
   }

   private static int listForceLoad(CommandSourceStack param0) {
      ServerLevel var1 = param0.getLevel();
      ResourceKey<Level> var2 = var1.dimension();
      LongSet var3 = var1.getForcedChunks();
      int var4 = var3.size();
      if (var4 > 0) {
         String var5 = Joiner.on(", ").join(var3.stream().sorted().map(ChunkPos::new).map(ChunkPos::toString).iterator());
         if (var4 == 1) {
            param0.sendSuccess(Component.translatable("commands.forceload.list.single", var2.location(), var5), false);
         } else {
            param0.sendSuccess(Component.translatable("commands.forceload.list.multiple", var4, var2.location(), var5), false);
         }
      } else {
         param0.sendFailure(Component.translatable("commands.forceload.added.none", var2.location()));
      }

      return var4;
   }

   private static int removeAll(CommandSourceStack param0) {
      ServerLevel var1 = param0.getLevel();
      ResourceKey<Level> var2 = var1.dimension();
      LongSet var3 = var1.getForcedChunks();
      var3.forEach(l_param1 -> var1.setChunkForced(ChunkPos.getX(l_param1), ChunkPos.getZ(l_param1), false));
      param0.sendSuccess(Component.translatable("commands.forceload.removed.all", var2.location()), true);
      return 0;
   }

   private static int changeForceLoad(CommandSourceStack param0, ColumnPos param1, ColumnPos param2, boolean param3) throws CommandSyntaxException {
      int var4 = Math.min(param1.x(), param2.x());
      int var5 = Math.min(param1.z(), param2.z());
      int var6 = Math.max(param1.x(), param2.x());
      int var7 = Math.max(param1.z(), param2.z());
      if (var4 >= -30000000 && var5 >= -30000000 && var6 < 30000000 && var7 < 30000000) {
         int var8 = SectionPos.blockToSectionCoord(var4);
         int var9 = SectionPos.blockToSectionCoord(var5);
         int var10 = SectionPos.blockToSectionCoord(var6);
         int var11 = SectionPos.blockToSectionCoord(var7);
         long var12 = (var10 - var8 + 1L) * (var11 - var9 + 1L);
         if (var12 > 256L) {
            throw ERROR_TOO_MANY_CHUNKS.create(256, var12);
         }

         ServerLevel var14 = param0.getLevel();
         ResourceKey<Level> var15 = var14.dimension();
         ChunkPos var16 = null;
         int var17 = 0;

         for (int var18 = var8; var18 <= var10; var18++) {
            for (int var19 = var9; var19 <= var11; var19++) {
               boolean var20 = var14.setChunkForced(var18, var19, param3);
               if (var20) {
                  var17++;
                  if (var16 == null) {
                     var16 = new ChunkPos(var18, var19);
                  }
               }
            }
         }

         if (var17 == 0) {
            throw (param3 ? ERROR_ALL_ADDED : ERROR_NONE_REMOVED).create();
         }

         if (var17 == 1) {
            param0.sendSuccess(Component.translatable("commands.forceload." + (param3 ? "added" : "removed") + ".single", var16, var15.location()), true);
         } else {
            ChunkPos var18 = new ChunkPos(var8, var9);
            ChunkPos var19 = new ChunkPos(var10, var11);
            param0.sendSuccess(
               Component.translatable("commands.forceload." + (param3 ? "added" : "removed") + ".multiple", var17, var15.location(), var18, var19), true
            );
         }

         return var17;
      } else {
         throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();
      }
   }
}
