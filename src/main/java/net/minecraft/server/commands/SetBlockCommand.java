package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class SetBlockCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.setblock.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("setblock")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("pos", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("block", BlockStateArgument.block(param1))
                        .executes(
                           l_param0 -> setBlock(
                              l_param0.getSource(),
                              BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                              BlockStateArgument.getBlock(l_param0, "block"),
                              SetBlockCommand.Mode.REPLACE,
                              null
                           )
                        )
                        .then(
                           Commands.literal("destroy")
                              .executes(
                                 l_param0 -> setBlock(
                                    l_param0.getSource(),
                                    BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                    BlockStateArgument.getBlock(l_param0, "block"),
                                    SetBlockCommand.Mode.DESTROY,
                                    null
                                 )
                              )
                        )
                        .then(
                           Commands.literal("keep")
                              .executes(
                                 l_param0 -> setBlock(
                                    l_param0.getSource(),
                                    BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                    BlockStateArgument.getBlock(l_param0, "block"),
                                    SetBlockCommand.Mode.REPLACE,
                                    l2_param0 -> l2_param0.getLevel().isEmptyBlock(l2_param0.getPos())
                                 )
                              )
                        )
                        .then(
                           Commands.literal("replace")
                              .executes(
                                 l_param0 -> setBlock(
                                    l_param0.getSource(),
                                    BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                    BlockStateArgument.getBlock(l_param0, "block"),
                                    SetBlockCommand.Mode.REPLACE,
                                    null
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int setBlock(
      CommandSourceStack param0, BlockPos param1, BlockInput param2, SetBlockCommand.Mode param3, @Nullable Predicate<BlockInWorld> param4
   ) throws CommandSyntaxException {
      ServerLevel var5 = param0.getLevel();
      if (param4 != null && !param4.test(new BlockInWorld(var5, param1, true))) {
         throw ERROR_FAILED.create();
      }

      boolean var6;
      if (param3 == SetBlockCommand.Mode.DESTROY) {
         var5.destroyBlock(param1, true);
         var6 = !param2.getState().isAir() || !var5.getBlockState(param1).isAir();
      } else {
         BlockEntity var7 = var5.getBlockEntity(param1);
         Clearable.tryClear(var7);
         var6 = true;
      }

      if (var6 && !param2.place(var5, param1, 2)) {
         throw ERROR_FAILED.create();
      }

      var5.blockUpdated(param1, param2.getState().getBlock());
      param0.sendSuccess(Component.translatable("commands.setblock.success", param1.getX(), param1.getY(), param1.getZ()), true);
      return 1;
   }

   public interface Filter {
      @Nullable
      BlockInput filter(BoundingBox var1, BlockPos var2, BlockInput var3, ServerLevel var4);
   }

   public enum Mode {
      REPLACE,
      DESTROY;
   }
}
