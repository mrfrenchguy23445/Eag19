package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.MutableInt;

public class FillBiomeCommand {
   public static final SimpleCommandExceptionType ERROR_NOT_LOADED = new SimpleCommandExceptionType(Component.translatable("argument.pos.unloaded"));
   private static final Dynamic2CommandExceptionType ERROR_VOLUME_TOO_LARGE = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.fillbiome.toobig", param0, param1)
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("fillbiome")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("to", BlockPosArgument.blockPos())
                        .then(
                           Commands.argument("biome", ResourceArgument.resource(param1, Registries.BIOME))
                              .executes(
                                 l_param0 -> fill(
                                    l_param0.getSource(),
                                    BlockPosArgument.getLoadedBlockPos(l_param0, "from"),
                                    BlockPosArgument.getLoadedBlockPos(l_param0, "to"),
                                    ResourceArgument.getResource(l_param0, "biome", Registries.BIOME),
                                    l2_param0 -> true
                                 )
                              )
                              .then(
                                 Commands.literal("replace")
                                    .then(
                                       Commands.argument("filter", ResourceOrTagArgument.resourceOrTag(param1, Registries.BIOME))
                                          .executes(
                                             l_param0 -> fill(
                                                l_param0.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(l_param0, "from"),
                                                BlockPosArgument.getLoadedBlockPos(l_param0, "to"),
                                                ResourceArgument.getResource(l_param0, "biome", Registries.BIOME),
                                                ResourceOrTagArgument.getResourceOrTag(l_param0, "filter", Registries.BIOME)::test
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int quantize(int param0) {
      return QuartPos.toBlock(QuartPos.fromBlock(param0));
   }

   private static BlockPos quantize(BlockPos param0) {
      return new BlockPos(quantize(param0.getX()), quantize(param0.getY()), quantize(param0.getZ()));
   }

   private static BiomeResolver makeResolver(MutableInt param0, ChunkAccess param1, BoundingBox param2, Holder<Biome> param3, Predicate<Holder<Biome>> param4) {
      return (l_param5, l_param6, l_param7, l_param8) -> {
         int l_var9 = QuartPos.toBlock(l_param5);
         int l_var10 = QuartPos.toBlock(l_param6);
         int l_var11 = QuartPos.toBlock(l_param7);
         Holder<Biome> l_var12 = param1.getNoiseBiome(l_param5, l_param6, l_param7);
         if (param2.isInside(l_var9, l_var10, l_var11) && param4.test(l_var12)) {
            param0.increment();
            return param3;
         } else {
            return l_var12;
         }
      };
   }

   private static int fill(CommandSourceStack param0, BlockPos param1, BlockPos param2, Holder.Reference<Biome> param3, Predicate<Holder<Biome>> param4) throws CommandSyntaxException {
      BlockPos var5 = quantize(param1);
      BlockPos var6 = quantize(param2);
      BoundingBox var7 = BoundingBox.fromCorners(var5, var6);
      int var8 = var7.getXSpan() * var7.getYSpan() * var7.getZSpan();
      int var9 = param0.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
      if (var8 > var9) {
         throw ERROR_VOLUME_TOO_LARGE.create(var9, var8);
      }

      ServerLevel var10 = param0.getLevel();
      List<ChunkAccess> var11 = new ArrayList<>();

      for (int var12 = SectionPos.blockToSectionCoord(var7.minZ()); var12 <= SectionPos.blockToSectionCoord(var7.maxZ()); var12++) {
         for (int var13 = SectionPos.blockToSectionCoord(var7.minX()); var13 <= SectionPos.blockToSectionCoord(var7.maxX()); var13++) {
            ChunkAccess var14 = var10.getChunk(var13, var12, ChunkStatus.FULL, false);
            if (var14 == null) {
               throw ERROR_NOT_LOADED.create();
            }

            var11.add(var14);
         }
      }

      MutableInt var12 = new MutableInt(0);

      for (ChunkAccess var14 : var11) {
         var14.fillBiomesFromNoise(makeResolver(var12, var14, var7, param3, param4), var10.getChunkSource().randomState().sampler());
         var14.setUnsaved(true);
      }

      var10.getChunkSource().chunkMap.resendBiomesForChunks(var11);
      param0.sendSuccess(
         Component.translatable(
            "commands.fillbiome.success.count", var12.getValue(), var7.minX(), var7.minY(), var7.minZ(), var7.maxX(), var7.maxY(), var7.maxZ()
         ),
         true
      );
      return var12.getValue();
   }
}
