package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class CloneCommands {
   private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(Component.translatable("commands.clone.overlap"));
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.clone.toobig", param0, param1)
   );
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
   public static final Predicate<BlockInWorld> FILTER_AIR = param0 -> !param0.getState().isAir();

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("clone")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(beginEndDestinationAndModeSuffix(param1, l_param0 -> l_param0.getSource().getLevel()))
            .then(
               Commands.literal("from")
                  .then(
                     Commands.argument("sourceDimension", DimensionArgument.dimension())
                        .then(beginEndDestinationAndModeSuffix(param1, l_param0 -> DimensionArgument.getDimension(l_param0, "sourceDimension")))
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(
      CommandBuildContext param0, CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> param1
   ) {
      return Commands.argument("begin", BlockPosArgument.blockPos())
         .then(
            Commands.argument("end", BlockPosArgument.blockPos())
               .then(destinationAndModeSuffix(param0, param1, l_param0 -> l_param0.getSource().getLevel()))
               .then(
                  Commands.literal("to")
                     .then(
                        Commands.argument("targetDimension", DimensionArgument.dimension())
                           .then(destinationAndModeSuffix(param0, param1, l_param0 -> DimensionArgument.getDimension(l_param0, "targetDimension")))
                     )
               )
         );
   }

   private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(CommandContext<CommandSourceStack> param0, ServerLevel param1, String param2) throws CommandSyntaxException {
      BlockPos var3 = BlockPosArgument.getLoadedBlockPos(param0, param1, param2);
      return new CloneCommands.DimensionAndPosition(param1, var3);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> destinationAndModeSuffix(
      CommandBuildContext param0,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> param1,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> param2
   ) {
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var3 = l_param1 -> getLoadedDimensionAndPosition(
         l_param1, param1.apply(l_param1), "begin"
      );
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var4 = l_param1 -> getLoadedDimensionAndPosition(
         l_param1, param1.apply(l_param1), "end"
      );
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var5 = l_param1 -> getLoadedDimensionAndPosition(
         l_param1, param2.apply(l_param1), "destination"
      );
      return Commands.argument("destination", BlockPosArgument.blockPos())
         .executes(
            l_param3 -> clone(
               l_param3.getSource(), var3.apply(l_param3), var4.apply(l_param3), var5.apply(l_param3), l2_param0 -> true, CloneCommands.Mode.NORMAL
            )
         )
         .then(
            wrapWithCloneMode(
               var3,
               var4,
               var5,
               l_param0 -> l2_param0 -> true,
               Commands.literal("replace")
                  .executes(
                     l_param3 -> clone(
                        l_param3.getSource(), var3.apply(l_param3), var4.apply(l_param3), var5.apply(l_param3), l2_param0 -> true, CloneCommands.Mode.NORMAL
                     )
                  )
            )
         )
         .then(
            wrapWithCloneMode(
               var3,
               var4,
               var5,
               l_param0 -> FILTER_AIR,
               Commands.literal("masked")
                  .executes(
                     l_param3 -> clone(
                        l_param3.getSource(), var3.apply(l_param3), var4.apply(l_param3), var5.apply(l_param3), FILTER_AIR, CloneCommands.Mode.NORMAL
                     )
                  )
            )
         )
         .then(
            Commands.literal("filtered")
               .then(
                  wrapWithCloneMode(
                     var3,
                     var4,
                     var5,
                     l_param0 -> BlockPredicateArgument.getBlockPredicate(l_param0, "filter"),
                     Commands.argument("filter", BlockPredicateArgument.blockPredicate(param0))
                        .executes(
                           l_param3 -> clone(
                              l_param3.getSource(),
                              var3.apply(l_param3),
                              var4.apply(l_param3),
                              var5.apply(l_param3),
                              BlockPredicateArgument.getBlockPredicate(l_param3, "filter"),
                              CloneCommands.Mode.NORMAL
                           )
                        )
                  )
               )
         );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> param0,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> param1,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> param2,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> param3,
      ArgumentBuilder<CommandSourceStack, ?> param4
   ) {
      return param4.then(
            Commands.literal("force")
               .executes(
                  l_param4 -> clone(
                     l_param4.getSource(),
                     param0.apply(l_param4),
                     param1.apply(l_param4),
                     param2.apply(l_param4),
                     param3.apply(l_param4),
                     CloneCommands.Mode.FORCE
                  )
               )
         )
         .then(
            Commands.literal("move")
               .executes(
                  l_param4 -> clone(
                     l_param4.getSource(),
                     param0.apply(l_param4),
                     param1.apply(l_param4),
                     param2.apply(l_param4),
                     param3.apply(l_param4),
                     CloneCommands.Mode.MOVE
                  )
               )
         )
         .then(
            Commands.literal("normal")
               .executes(
                  l_param4 -> clone(
                     l_param4.getSource(),
                     param0.apply(l_param4),
                     param1.apply(l_param4),
                     param2.apply(l_param4),
                     param3.apply(l_param4),
                     CloneCommands.Mode.NORMAL
                  )
               )
         );
   }

   private static int clone(
      CommandSourceStack param0,
      CloneCommands.DimensionAndPosition param1,
      CloneCommands.DimensionAndPosition param2,
      CloneCommands.DimensionAndPosition param3,
      Predicate<BlockInWorld> param4,
      CloneCommands.Mode param5
   ) throws CommandSyntaxException {
      BlockPos var6 = param1.position();
      BlockPos var7 = param2.position();
      BoundingBox var8 = BoundingBox.fromCorners(var6, var7);
      BlockPos var9 = param3.position();
      BlockPos var10 = var9.offset(var8.getLength());
      BoundingBox var11 = BoundingBox.fromCorners(var9, var10);
      ServerLevel var12 = param1.dimension();
      ServerLevel var13 = param3.dimension();
      if (!param5.canOverlap() && var12 == var13 && var11.intersects(var8)) {
         throw ERROR_OVERLAP.create();
      }

      int var14 = var8.getXSpan() * var8.getYSpan() * var8.getZSpan();
      int var15 = param0.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
      if (var14 > var15) {
         throw ERROR_AREA_TOO_LARGE.create(var15, var14);
      }

      if (var12.hasChunksAt(var6, var7) && var13.hasChunksAt(var9, var10)) {
         List<CloneCommands.CloneBlockInfo> var16 = Lists.newArrayList();
         List<CloneCommands.CloneBlockInfo> var17 = Lists.newArrayList();
         List<CloneCommands.CloneBlockInfo> var18 = Lists.newArrayList();
         Deque<BlockPos> var19 = Lists.newLinkedList();
         BlockPos var20 = new BlockPos(var11.minX() - var8.minX(), var11.minY() - var8.minY(), var11.minZ() - var8.minZ());

         for (int var21 = var8.minZ(); var21 <= var8.maxZ(); var21++) {
            for (int var22 = var8.minY(); var22 <= var8.maxY(); var22++) {
               for (int var23 = var8.minX(); var23 <= var8.maxX(); var23++) {
                  BlockPos var24 = new BlockPos(var23, var22, var21);
                  BlockPos var25 = var24.offset(var20);
                  BlockInWorld var26 = new BlockInWorld(var12, var24, false);
                  BlockState var27 = var26.getState();
                  if (param4.test(var26)) {
                     BlockEntity var28 = var12.getBlockEntity(var24);
                     if (var28 != null) {
                        CompoundTag var29 = var28.saveWithoutMetadata();
                        var17.add(new CloneCommands.CloneBlockInfo(var25, var27, var29));
                        var19.addLast(var24);
                     } else if (!var27.isSolidRender(var12, var24) && !var27.isCollisionShapeFullBlock(var12, var24)) {
                        var18.add(new CloneCommands.CloneBlockInfo(var25, var27, null));
                        var19.addFirst(var24);
                     } else {
                        var16.add(new CloneCommands.CloneBlockInfo(var25, var27, null));
                        var19.addLast(var24);
                     }
                  }
               }
            }
         }

         if (param5 == CloneCommands.Mode.MOVE) {
            for (BlockPos var22 : var19) {
               BlockEntity var23 = var12.getBlockEntity(var22);
               Clearable.tryClear(var23);
               var12.setBlock(var22, Blocks.BARRIER.defaultBlockState(), 2);
            }

            for (BlockPos var22 : var19) {
               var12.setBlock(var22, Blocks.AIR.defaultBlockState(), 3);
            }
         }

         List<CloneCommands.CloneBlockInfo> var21 = Lists.newArrayList();
         var21.addAll(var16);
         var21.addAll(var17);
         var21.addAll(var18);
         List<CloneCommands.CloneBlockInfo> var22 = Lists.reverse(var21);

         for (CloneCommands.CloneBlockInfo var24 : var22) {
            BlockEntity var25 = var13.getBlockEntity(var24.pos);
            Clearable.tryClear(var25);
            var13.setBlock(var24.pos, Blocks.BARRIER.defaultBlockState(), 2);
         }

         int var23 = 0;

         for (CloneCommands.CloneBlockInfo var25 : var21) {
            if (var13.setBlock(var25.pos, var25.state, 2)) {
               var23++;
            }
         }

         for (CloneCommands.CloneBlockInfo var25 : var17) {
            BlockEntity var26 = var13.getBlockEntity(var25.pos);
            if (var25.tag != null && var26 != null) {
               var26.load(var25.tag);
               var26.setChanged();
            }

            var13.setBlock(var25.pos, var25.state, 2);
         }

         for (CloneCommands.CloneBlockInfo var25 : var22) {
            var13.blockUpdated(var25.pos, var25.state.getBlock());
         }

         var13.getBlockTicks().copyAreaFrom(var12.getBlockTicks(), var8, var20);
         if (var23 == 0) {
            throw ERROR_FAILED.create();
         }

         param0.sendSuccess(Component.translatable("commands.clone.success", var23), true);
         return var23;
      } else {
         throw BlockPosArgument.ERROR_NOT_LOADED.create();
      }
   }

   static class CloneBlockInfo {
      public final BlockPos pos;
      public final BlockState state;
      @Nullable
      public final CompoundTag tag;

      public CloneBlockInfo(BlockPos param0, BlockState param1, @Nullable CompoundTag param2) {
         this.pos = param0;
         this.state = param1;
         this.tag = param2;
      }
   }

   @FunctionalInterface
   interface CommandFunction<T, R> {
      R apply(T var1) throws CommandSyntaxException;
   }

   record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
   }

   enum Mode {
      FORCE(true),
      MOVE(true),
      NORMAL(false);

      private final boolean canOverlap;

      Mode(boolean param2) {
         this.canOverlap = param2;
      }

      public boolean canOverlap() {
         return this.canOverlap;
      }
   }
}
