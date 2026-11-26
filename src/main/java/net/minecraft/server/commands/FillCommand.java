package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class FillCommand {
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.fill.toobig", param0, param1)
   );
   static final BlockInput HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), null);
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.fill.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("fill")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("to", BlockPosArgument.blockPos())
                        .then(
                           Commands.argument("block", BlockStateArgument.block(param1))
                              .executes(
                                 l_param0 -> fillBlocks(
                                    l_param0.getSource(),
                                    BoundingBox.fromCorners(
                                       BlockPosArgument.getLoadedBlockPos(l_param0, "from"), BlockPosArgument.getLoadedBlockPos(l_param0, "to")
                                    ),
                                    BlockStateArgument.getBlock(l_param0, "block"),
                                    FillCommand.Mode.REPLACE,
                                    null
                                 )
                              )
                              .then(
                                 Commands.literal("replace")
                                    .executes(
                                       l_param0 -> fillBlocks(
                                          l_param0.getSource(),
                                          BoundingBox.fromCorners(
                                             BlockPosArgument.getLoadedBlockPos(l_param0, "from"), BlockPosArgument.getLoadedBlockPos(l_param0, "to")
                                          ),
                                          BlockStateArgument.getBlock(l_param0, "block"),
                                          FillCommand.Mode.REPLACE,
                                          null
                                       )
                                    )
                                    .then(
                                       Commands.argument("filter", BlockPredicateArgument.blockPredicate(param1))
                                          .executes(
                                             l_param0 -> fillBlocks(
                                                l_param0.getSource(),
                                                BoundingBox.fromCorners(
                                                   BlockPosArgument.getLoadedBlockPos(l_param0, "from"), BlockPosArgument.getLoadedBlockPos(l_param0, "to")
                                                ),
                                                BlockStateArgument.getBlock(l_param0, "block"),
                                                FillCommand.Mode.REPLACE,
                                                BlockPredicateArgument.getBlockPredicate(l_param0, "filter")
                                             )
                                          )
                                    )
                              )
                              .then(
                                 Commands.literal("keep")
                                    .executes(
                                       l_param0 -> fillBlocks(
                                          l_param0.getSource(),
                                          BoundingBox.fromCorners(
                                             BlockPosArgument.getLoadedBlockPos(l_param0, "from"), BlockPosArgument.getLoadedBlockPos(l_param0, "to")
                                          ),
                                          BlockStateArgument.getBlock(l_param0, "block"),
                                          FillCommand.Mode.REPLACE,
                                          l2_param0 -> l2_param0.getLevel().isEmptyBlock(l2_param0.getPos())
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("outline")
                                    .executes(
                                       l_param0 -> fillBlocks(
                                          l_param0.getSource(),
                                          BoundingBox.fromCorners(
                                             BlockPosArgument.getLoadedBlockPos(l_param0, "from"), BlockPosArgument.getLoadedBlockPos(l_param0, "to")
                                          ),
                                          BlockStateArgument.getBlock(l_param0, "block"),
                                          FillCommand.Mode.OUTLINE,
                                          null
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("hollow")
                                    .executes(
                                       l_param0 -> fillBlocks(
                                          l_param0.getSource(),
                                          BoundingBox.fromCorners(
                                             BlockPosArgument.getLoadedBlockPos(l_param0, "from"), BlockPosArgument.getLoadedBlockPos(l_param0, "to")
                                          ),
                                          BlockStateArgument.getBlock(l_param0, "block"),
                                          FillCommand.Mode.HOLLOW,
                                          null
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("destroy")
                                    .executes(
                                       l_param0 -> fillBlocks(
                                          l_param0.getSource(),
                                          BoundingBox.fromCorners(
                                             BlockPosArgument.getLoadedBlockPos(l_param0, "from"), BlockPosArgument.getLoadedBlockPos(l_param0, "to")
                                          ),
                                          BlockStateArgument.getBlock(l_param0, "block"),
                                          FillCommand.Mode.DESTROY,
                                          null
                                       )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int fillBlocks(
      CommandSourceStack param0, BoundingBox param1, BlockInput param2, FillCommand.Mode param3, @Nullable Predicate<BlockInWorld> param4
   ) throws CommandSyntaxException {
      int var5 = param1.getXSpan() * param1.getYSpan() * param1.getZSpan();
      int var6 = param0.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
      if (var5 > var6) {
         throw ERROR_AREA_TOO_LARGE.create(var6, var5);
      }

      List<BlockPos> var7 = Lists.newArrayList();
      ServerLevel var8 = param0.getLevel();
      int var9 = 0;

      for (BlockPos var11 : BlockPos.betweenClosed(param1.minX(), param1.minY(), param1.minZ(), param1.maxX(), param1.maxY(), param1.maxZ())) {
         if (param4 == null || param4.test(new BlockInWorld(var8, var11, true))) {
            BlockInput var12 = param3.filter.filter(param1, var11, param2, var8);
            if (var12 != null) {
               BlockEntity var13 = var8.getBlockEntity(var11);
               Clearable.tryClear(var13);
               if (var12.place(var8, var11, 2)) {
                  var7.add(var11.immutable());
                  var9++;
               }
            }
         }
      }

      for (BlockPos var11 : var7) {
         Block var12 = var8.getBlockState(var11).getBlock();
         var8.blockUpdated(var11, var12);
      }

      if (var9 == 0) {
         throw ERROR_FAILED.create();
      }

      param0.sendSuccess(Component.translatable("commands.fill.success", var9), true);
      return var9;
   }

   enum Mode {
      REPLACE((param0, param1, param2, param3) -> param2),
      OUTLINE(
         (param0, param1, param2, param3) -> param1.getX() != param0.minX()
               && param1.getX() != param0.maxX()
               && param1.getY() != param0.minY()
               && param1.getY() != param0.maxY()
               && param1.getZ() != param0.minZ()
               && param1.getZ() != param0.maxZ()
            ? null
            : param2
      ),
      HOLLOW(
         (param0, param1, param2, param3) -> param1.getX() != param0.minX()
               && param1.getX() != param0.maxX()
               && param1.getY() != param0.minY()
               && param1.getY() != param0.maxY()
               && param1.getZ() != param0.minZ()
               && param1.getZ() != param0.maxZ()
            ? FillCommand.HOLLOW_CORE
            : param2
      ),
      DESTROY((param0, param1, param2, param3) -> {
         param3.destroyBlock(param1, true);
         return param2;
      });

      public final SetBlockCommand.Filter filter;

      Mode(SetBlockCommand.Filter param2) {
         this.filter = param2;
      }
   }
}
