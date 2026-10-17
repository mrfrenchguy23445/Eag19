package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.PredicateManager;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class ExecuteCommand {
   private static final int MAX_TEST_AREA = 32768;
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.execute.blocks.toobig", param0, param1)
   );
   private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.execute.conditional.fail")
   );
   private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.execute.conditional.fail_count", param0)
   );
   private static final BinaryOperator<ResultConsumer<CommandSourceStack>> CALLBACK_CHAINER = (param0, param1) -> (l_param2, l_param3, l_param4) -> {
      param0.onCommandComplete(l_param2, l_param3, l_param4);
      param1.onCommandComplete(l_param2, l_param3, l_param4);
   };
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_PREDICATE = (param0, param1) -> {
      PredicateManager var2 = param0.getSource().getServer().getPredicateManager();
      return SharedSuggestionProvider.suggestResource(var2.getKeys(), param1);
   };

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      LiteralCommandNode<CommandSourceStack> var2 = param0.register(Commands.literal("execute").requires(l_param0 -> l_param0.hasPermission(2)));
      param0.register(
         Commands.literal("execute")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(Commands.literal("run").redirect(param0.getRoot()))
            .then(addConditionals(var2, Commands.literal("if"), true, param1))
            .then(addConditionals(var2, Commands.literal("unless"), false, param1))
            .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(var2, l_param0 -> {
               List<CommandSourceStack> l_var1 = Lists.newArrayList();

               for (Entity l_var3 : EntityArgument.getOptionalEntities(l_param0, "targets")) {
                  l_var1.add(l_param0.getSource().withEntity(l_var3));
               }

               return l_var1;
            })))
            .then(Commands.literal("at").then(Commands.argument("targets", EntityArgument.entities()).fork(var2, l_param0 -> {
               List<CommandSourceStack> l_var1 = Lists.newArrayList();

               for (Entity l_var3 : EntityArgument.getOptionalEntities(l_param0, "targets")) {
                  l_var1.add(l_param0.getSource().withLevel((ServerLevel)l_var3.level).withPosition(l_var3.position()).withRotation(l_var3.getRotationVector()));
               }

               return l_var1;
            })))
            .then(Commands.literal("store").then(wrapStores(var2, Commands.literal("result"), true)).then(wrapStores(var2, Commands.literal("success"), false)))
            .then(
               Commands.literal("positioned")
                  .then(
                     Commands.argument("pos", Vec3Argument.vec3())
                        .redirect(
                           var2,
                           l_param0 -> l_param0.getSource().withPosition(Vec3Argument.getVec3(l_param0, "pos")).withAnchor(EntityAnchorArgument.Anchor.FEET)
                        )
                  )
                  .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(var2, l_param0 -> {
                     List<CommandSourceStack> l_var1 = Lists.newArrayList();

                     for (Entity l_var3 : EntityArgument.getOptionalEntities(l_param0, "targets")) {
                        l_var1.add(l_param0.getSource().withPosition(l_var3.position()));
                     }

                     return l_var1;
                  })))
                  .then(Commands.literal("over").then(Commands.argument("heightmap", HeightmapTypeArgument.heightmap()).redirect(var2, l_param0 -> {
                     Vec3 l_var1 = l_param0.getSource().getPosition();
                     ServerLevel l_var2 = l_param0.getSource().getLevel();
                     double l_var3 = l_var1.x();
                     double l_var5 = l_var1.z();
                     if (!l_var2.hasChunk(SectionPos.blockToSectionCoord(l_var3), SectionPos.blockToSectionCoord(l_var5))) {
                        throw BlockPosArgument.ERROR_NOT_LOADED.create();
                     }

                     int l_var7 = l_var2.getHeight(HeightmapTypeArgument.getHeightmap(l_param0, "heightmap"), Mth.floor(l_var3), Mth.floor(l_var5));
                     return l_param0.getSource().withPosition(new Vec3(l_var3, l_var7, l_var5));
                  })))
            )
            .then(
               Commands.literal("rotated")
                  .then(
                     Commands.argument("rot", RotationArgument.rotation())
                        .redirect(
                           var2, l_param0 -> l_param0.getSource().withRotation(RotationArgument.getRotation(l_param0, "rot").getRotation(l_param0.getSource()))
                        )
                  )
                  .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(var2, l_param0 -> {
                     List<CommandSourceStack> l_var1 = Lists.newArrayList();

                     for (Entity l_var3 : EntityArgument.getOptionalEntities(l_param0, "targets")) {
                        l_var1.add(l_param0.getSource().withRotation(l_var3.getRotationVector()));
                     }

                     return l_var1;
                  })))
            )
            .then(
               Commands.literal("facing")
                  .then(
                     Commands.literal("entity")
                        .then(
                           Commands.argument("targets", EntityArgument.entities())
                              .then(Commands.argument("anchor", EntityAnchorArgument.anchor()).fork(var2, l_param0 -> {
                                 List<CommandSourceStack> l_var1 = Lists.newArrayList();
                                 EntityAnchorArgument.Anchor l_var2 = EntityAnchorArgument.getAnchor(l_param0, "anchor");

                                 for (Entity l_var4 : EntityArgument.getOptionalEntities(l_param0, "targets")) {
                                    l_var1.add(l_param0.getSource().facing(l_var4, l_var2));
                                 }

                                 return l_var1;
                              }))
                        )
                  )
                  .then(
                     Commands.argument("pos", Vec3Argument.vec3())
                        .redirect(var2, l_param0 -> l_param0.getSource().facing(Vec3Argument.getVec3(l_param0, "pos")))
                  )
            )
            .then(
               Commands.literal("align")
                  .then(
                     Commands.argument("axes", SwizzleArgument.swizzle())
                        .redirect(
                           var2,
                           l_param0 -> l_param0.getSource()
                              .withPosition(l_param0.getSource().getPosition().align(SwizzleArgument.getSwizzle(l_param0, "axes")))
                        )
                  )
            )
            .then(
               Commands.literal("anchored")
                  .then(
                     Commands.argument("anchor", EntityAnchorArgument.anchor())
                        .redirect(var2, l_param0 -> l_param0.getSource().withAnchor(EntityAnchorArgument.getAnchor(l_param0, "anchor")))
                  )
            )
            .then(
               Commands.literal("in")
                  .then(
                     Commands.argument("dimension", DimensionArgument.dimension())
                        .redirect(var2, l_param0 -> l_param0.getSource().withLevel(DimensionArgument.getDimension(l_param0, "dimension")))
                  )
            )
            .then(
               Commands.literal("summon")
                  .then(
                     Commands.argument("entity", ResourceArgument.resource(param1, Registries.ENTITY_TYPE))
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .redirect(var2, l_param0 -> spawnEntityAndRedirect(l_param0.getSource(), ResourceArgument.getSummonableEntityType(l_param0, "entity")))
                  )
            )
            .then(createRelationOperations(var2, Commands.literal("on")))
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapStores(
      LiteralCommandNode<CommandSourceStack> param0, LiteralArgumentBuilder<CommandSourceStack> param1, boolean param2
   ) {
      param1.then(
         Commands.literal("score")
            .then(
               Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                  .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                  .then(
                     Commands.argument("objective", ObjectiveArgument.objective())
                        .redirect(
                           param0,
                           l_param1 -> storeValue(
                              l_param1.getSource(),
                              ScoreHolderArgument.getNamesWithDefaultWildcard(l_param1, "targets"),
                              ObjectiveArgument.getObjective(l_param1, "objective"),
                              param2
                           )
                        )
                  )
            )
      );
      param1.then(
         Commands.literal("bossbar")
            .then(
               Commands.argument("id", ResourceLocationArgument.id())
                  .suggests(BossBarCommands.SUGGEST_BOSS_BAR)
                  .then(
                     Commands.literal("value")
                        .redirect(param0, l_param1 -> storeValue(l_param1.getSource(), BossBarCommands.getBossBar(l_param1), true, param2))
                  )
                  .then(
                     Commands.literal("max")
                        .redirect(param0, l_param1 -> storeValue(l_param1.getSource(), BossBarCommands.getBossBar(l_param1), false, param2))
                  )
            )
      );

      for (DataCommands.DataProvider var4 : DataCommands.TARGET_PROVIDERS) {
         var4.wrap(
            param1,
            l_param3 -> l_param3.then(
               Commands.argument("path", NbtPathArgument.nbtPath())
                  .then(
                     Commands.literal("int")
                        .then(
                           Commands.argument("scale", DoubleArgumentType.doubleArg())
                              .redirect(
                                 param0,
                                 l2_param2 -> storeData(
                                    l2_param2.getSource(),
                                    var4.access(l2_param2),
                                    NbtPathArgument.getPath(l2_param2, "path"),
                                    l3_param1 -> IntTag.valueOf((int)(l3_param1 * DoubleArgumentType.getDouble(l2_param2, "scale"))),
                                    param2
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("float")
                        .then(
                           Commands.argument("scale", DoubleArgumentType.doubleArg())
                              .redirect(
                                 param0,
                                 l2_param2 -> storeData(
                                    l2_param2.getSource(),
                                    var4.access(l2_param2),
                                    NbtPathArgument.getPath(l2_param2, "path"),
                                    l3_param1 -> FloatTag.valueOf((float)(l3_param1 * DoubleArgumentType.getDouble(l2_param2, "scale"))),
                                    param2
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("short")
                        .then(
                           Commands.argument("scale", DoubleArgumentType.doubleArg())
                              .redirect(
                                 param0,
                                 l2_param2 -> storeData(
                                    l2_param2.getSource(),
                                    var4.access(l2_param2),
                                    NbtPathArgument.getPath(l2_param2, "path"),
                                    l3_param1 -> ShortTag.valueOf((short)(l3_param1 * DoubleArgumentType.getDouble(l2_param2, "scale"))),
                                    param2
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("long")
                        .then(
                           Commands.argument("scale", DoubleArgumentType.doubleArg())
                              .redirect(
                                 param0,
                                 l2_param2 -> storeData(
                                    l2_param2.getSource(),
                                    var4.access(l2_param2),
                                    NbtPathArgument.getPath(l2_param2, "path"),
                                    l3_param1 -> LongTag.valueOf((long)(l3_param1 * DoubleArgumentType.getDouble(l2_param2, "scale"))),
                                    param2
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("double")
                        .then(
                           Commands.argument("scale", DoubleArgumentType.doubleArg())
                              .redirect(
                                 param0,
                                 l2_param2 -> storeData(
                                    l2_param2.getSource(),
                                    var4.access(l2_param2),
                                    NbtPathArgument.getPath(l2_param2, "path"),
                                    l3_param1 -> DoubleTag.valueOf(l3_param1 * DoubleArgumentType.getDouble(l2_param2, "scale")),
                                    param2
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("byte")
                        .then(
                           Commands.argument("scale", DoubleArgumentType.doubleArg())
                              .redirect(
                                 param0,
                                 l2_param2 -> storeData(
                                    l2_param2.getSource(),
                                    var4.access(l2_param2),
                                    NbtPathArgument.getPath(l2_param2, "path"),
                                    l3_param1 -> ByteTag.valueOf((byte)(l3_param1 * DoubleArgumentType.getDouble(l2_param2, "scale"))),
                                    param2
                                 )
                              )
                        )
                  )
            )
         );
      }

      return param1;
   }

   private static CommandSourceStack storeValue(CommandSourceStack param0, Collection<String> param1, Objective param2, boolean param3) {
      Scoreboard var4 = param0.getServer().getScoreboard();
      return param0.withCallback((l_param4, l_param5, l_param6) -> {
         for (String l_var8 : param1) {
            Score l_var9 = var4.getOrCreatePlayerScore(l_var8, param2);
            int l_var10 = param3 ? l_param6 : (l_param5 ? 1 : 0);
            l_var9.setScore(l_var10);
         }
      }, CALLBACK_CHAINER);
   }

   private static CommandSourceStack storeValue(CommandSourceStack param0, CustomBossEvent param1, boolean param2, boolean param3) {
      return param0.withCallback((l_param3, l_param4, l_param5) -> {
         int l_var6 = param3 ? l_param5 : (l_param4 ? 1 : 0);
         if (param2) {
            param1.setValue(l_var6);
         } else {
            param1.setMax(l_var6);
         }
      }, CALLBACK_CHAINER);
   }

   private static CommandSourceStack storeData(
      CommandSourceStack param0, DataAccessor param1, NbtPathArgument.NbtPath param2, IntFunction<Tag> param3, boolean param4
   ) {
      return param0.withCallback((l_param4, l_param5, l_param6) -> {
         try {
            CompoundTag l_var7 = param1.getData();
            int l_var8 = param4 ? l_param6 : (l_param5 ? 1 : 0);
            param2.set(l_var7, param3.apply(l_var8));
            param1.setData(l_var7);
         } catch (CommandSyntaxException var9) {
         }
      }, CALLBACK_CHAINER);
   }

   private static boolean isChunkLoaded(ServerLevel param0, BlockPos param1) {
      int var2 = SectionPos.blockToSectionCoord(param1.getX());
      int var3 = SectionPos.blockToSectionCoord(param1.getZ());
      LevelChunk var4 = param0.getChunkSource().getChunkNow(var2, var3);
      return var4 != null ? var4.getFullStatus() == ChunkHolder.FullChunkStatus.ENTITY_TICKING : false;
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditionals(
      CommandNode<CommandSourceStack> param0, LiteralArgumentBuilder<CommandSourceStack> param1, boolean param2, CommandBuildContext param3
   ) {
      param1.then(
            Commands.literal("block")
               .then(
                  Commands.argument("pos", BlockPosArgument.blockPos())
                     .then(
                        addConditional(
                           param0,
                           Commands.argument("block", BlockPredicateArgument.blockPredicate(param3)),
                           param2,
                           l_param0 -> BlockPredicateArgument.getBlockPredicate(l_param0, "block")
                              .test(new BlockInWorld(l_param0.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(l_param0, "pos"), true))
                        )
                     )
               )
         )
         .then(
            Commands.literal("biome")
               .then(
                  Commands.argument("pos", BlockPosArgument.blockPos())
                     .then(
                        addConditional(
                           param0,
                           Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(param3, Registries.BIOME)),
                           param2,
                           l_param0 -> ResourceOrTagArgument.getResourceOrTag(l_param0, "biome", Registries.BIOME)
                              .test(l_param0.getSource().getLevel().getBiome(BlockPosArgument.getLoadedBlockPos(l_param0, "pos")))
                        )
                     )
               )
         )
         .then(
            Commands.literal("loaded")
               .then(
                  addConditional(
                     param0,
                     Commands.argument("pos", BlockPosArgument.blockPos()),
                     param2,
                     l_param0 -> isChunkLoaded(l_param0.getSource().getLevel(), BlockPosArgument.getBlockPos(l_param0, "pos"))
                  )
               )
         )
         .then(
            Commands.literal("dimension")
               .then(
                  addConditional(
                     param0,
                     Commands.argument("dimension", DimensionArgument.dimension()),
                     param2,
                     l_param0 -> DimensionArgument.getDimension(l_param0, "dimension") == l_param0.getSource().getLevel()
                  )
               )
         )
         .then(
            Commands.literal("score")
               .then(
                  Commands.argument("target", ScoreHolderArgument.scoreHolder())
                     .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                     .then(
                        Commands.argument("targetObjective", ObjectiveArgument.objective())
                           .then(
                              Commands.literal("=")
                                 .then(
                                    Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                       .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                       .then(
                                          addConditional(
                                             param0,
                                             Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                             param2,
                                             l_param0 -> checkScore(l_param0, Integer::equals)
                                          )
                                       )
                                 )
                           )
                           .then(
                              Commands.literal("<")
                                 .then(
                                    Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                       .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                       .then(
                                          addConditional(
                                             param0,
                                             Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                             param2,
                                             l_param0 -> checkScore(l_param0, (l2_param0, l2_param1) -> l2_param0 < l2_param1)
                                          )
                                       )
                                 )
                           )
                           .then(
                              Commands.literal("<=")
                                 .then(
                                    Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                       .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                       .then(
                                          addConditional(
                                             param0,
                                             Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                             param2,
                                             l_param0 -> checkScore(l_param0, (l2_param0, l2_param1) -> l2_param0 <= l2_param1)
                                          )
                                       )
                                 )
                           )
                           .then(
                              Commands.literal(">")
                                 .then(
                                    Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                       .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                       .then(
                                          addConditional(
                                             param0,
                                             Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                             param2,
                                             l_param0 -> checkScore(l_param0, (l2_param0, l2_param1) -> l2_param0 > l2_param1)
                                          )
                                       )
                                 )
                           )
                           .then(
                              Commands.literal(">=")
                                 .then(
                                    Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                       .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                       .then(
                                          addConditional(
                                             param0,
                                             Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                             param2,
                                             l_param0 -> checkScore(l_param0, (l2_param0, l2_param1) -> l2_param0 >= l2_param1)
                                          )
                                       )
                                 )
                           )
                           .then(
                              Commands.literal("matches")
                                 .then(
                                    addConditional(
                                       param0,
                                       Commands.argument("range", RangeArgument.intRange()),
                                       param2,
                                       l_param0 -> checkScore(l_param0, RangeArgument.Ints.getRange(l_param0, "range"))
                                    )
                                 )
                           )
                     )
               )
         )
         .then(
            Commands.literal("blocks")
               .then(
                  Commands.argument("start", BlockPosArgument.blockPos())
                     .then(
                        Commands.argument("end", BlockPosArgument.blockPos())
                           .then(
                              Commands.argument("destination", BlockPosArgument.blockPos())
                                 .then(addIfBlocksConditional(param0, Commands.literal("all"), param2, false))
                                 .then(addIfBlocksConditional(param0, Commands.literal("masked"), param2, true))
                           )
                     )
               )
         )
         .then(
            Commands.literal("entity")
               .then(
                  Commands.argument("entities", EntityArgument.entities())
                     .fork(param0, l_param1 -> expect(l_param1, param2, !EntityArgument.getOptionalEntities(l_param1, "entities").isEmpty()))
                     .executes(createNumericConditionalHandler(param2, l_param0 -> EntityArgument.getOptionalEntities(l_param0, "entities").size()))
               )
         )
         .then(
            Commands.literal("predicate")
               .then(
                  addConditional(
                     param0,
                     Commands.argument("predicate", ResourceLocationArgument.id()).suggests(SUGGEST_PREDICATE),
                     param2,
                     l_param0 -> checkCustomPredicate(l_param0.getSource(), ResourceLocationArgument.getPredicate(l_param0, "predicate"))
                  )
               )
         );

      for (DataCommands.DataProvider var5 : DataCommands.SOURCE_PROVIDERS) {
         param1.then(
            var5.wrap(
               Commands.literal("data"),
               l_param3 -> l_param3.then(
                  Commands.argument("path", NbtPathArgument.nbtPath())
                     .fork(
                        param0,
                        l2_param2 -> expect(l2_param2, param2, checkMatchingData(var5.access(l2_param2), NbtPathArgument.getPath(l2_param2, "path")) > 0)
                     )
                     .executes(
                        createNumericConditionalHandler(
                           param2, l2_param1 -> checkMatchingData(var5.access(l2_param1), NbtPathArgument.getPath(l2_param1, "path"))
                        )
                     )
               )
            )
         );
      }

      return param1;
   }

   private static Command<CommandSourceStack> createNumericConditionalHandler(boolean param0, ExecuteCommand.CommandNumericPredicate param1) {
      return param0 ? l_param1 -> {
         int l_var2 = param1.test(l_param1);
         if (l_var2 > 0) {
            l_param1.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass_count", l_var2), false);
            return l_var2;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      } : l_param1 -> {
         int l_var2 = param1.test(l_param1);
         if (l_var2 == 0) {
            l_param1.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create(l_var2);
         }
      };
   }

   private static int checkMatchingData(DataAccessor param0, NbtPathArgument.NbtPath param1) throws CommandSyntaxException {
      return param1.countMatching(param0.getData());
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> param0, BiPredicate<Integer, Integer> param1) throws CommandSyntaxException {
      String var2 = ScoreHolderArgument.getName(param0, "target");
      Objective var3 = ObjectiveArgument.getObjective(param0, "targetObjective");
      String var4 = ScoreHolderArgument.getName(param0, "source");
      Objective var5 = ObjectiveArgument.getObjective(param0, "sourceObjective");
      Scoreboard var6 = param0.getSource().getServer().getScoreboard();
      if (var6.hasPlayerScore(var2, var3) && var6.hasPlayerScore(var4, var5)) {
         Score var7 = var6.getOrCreatePlayerScore(var2, var3);
         Score var8 = var6.getOrCreatePlayerScore(var4, var5);
         return param1.test(var7.getScore(), var8.getScore());
      } else {
         return false;
      }
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> param0, MinMaxBounds.Ints param1) throws CommandSyntaxException {
      String var2 = ScoreHolderArgument.getName(param0, "target");
      Objective var3 = ObjectiveArgument.getObjective(param0, "targetObjective");
      Scoreboard var4 = param0.getSource().getServer().getScoreboard();
      return !var4.hasPlayerScore(var2, var3) ? false : param1.matches(var4.getOrCreatePlayerScore(var2, var3).getScore());
   }

   private static boolean checkCustomPredicate(CommandSourceStack param0, LootItemCondition param1) {
      ServerLevel var2 = param0.getLevel();
      LootContext.Builder var3 = new LootContext.Builder(var2)
         .withParameter(LootContextParams.ORIGIN, param0.getPosition())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, param0.getEntity());
      return param1.test(var3.create(LootContextParamSets.COMMAND));
   }

   private static Collection<CommandSourceStack> expect(CommandContext<CommandSourceStack> param0, boolean param1, boolean param2) {
      return param2 == param1 ? Collections.singleton(param0.getSource()) : Collections.emptyList();
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditional(
      CommandNode<CommandSourceStack> param0, ArgumentBuilder<CommandSourceStack, ?> param1, boolean param2, ExecuteCommand.CommandPredicate param3
   ) {
      return param1.fork(param0, l_param2 -> expect(l_param2, param2, param3.test(l_param2))).executes(l_param2 -> {
         if (param2 == param3.test(l_param2)) {
            l_param2.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      });
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addIfBlocksConditional(
      CommandNode<CommandSourceStack> param0, ArgumentBuilder<CommandSourceStack, ?> param1, boolean param2, boolean param3
   ) {
      return param1.fork(param0, l_param2 -> expect(l_param2, param2, checkRegions(l_param2, param3).isPresent()))
         .executes(param2 ? l_param1 -> checkIfRegions(l_param1, param3) : l_param1 -> checkUnlessRegions(l_param1, param3));
   }

   private static int checkIfRegions(CommandContext<CommandSourceStack> param0, boolean param1) throws CommandSyntaxException {
      OptionalInt var2 = checkRegions(param0, param1);
      if (var2.isPresent()) {
         param0.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass_count", var2.getAsInt()), false);
         return var2.getAsInt();
      } else {
         throw ERROR_CONDITIONAL_FAILED.create();
      }
   }

   private static int checkUnlessRegions(CommandContext<CommandSourceStack> param0, boolean param1) throws CommandSyntaxException {
      OptionalInt var2 = checkRegions(param0, param1);
      if (var2.isPresent()) {
         throw ERROR_CONDITIONAL_FAILED_COUNT.create(var2.getAsInt());
      }

      param0.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass"), false);
      return 1;
   }

   private static OptionalInt checkRegions(CommandContext<CommandSourceStack> param0, boolean param1) throws CommandSyntaxException {
      return checkRegions(
         param0.getSource().getLevel(),
         BlockPosArgument.getLoadedBlockPos(param0, "start"),
         BlockPosArgument.getLoadedBlockPos(param0, "end"),
         BlockPosArgument.getLoadedBlockPos(param0, "destination"),
         param1
      );
   }

   private static OptionalInt checkRegions(ServerLevel param0, BlockPos param1, BlockPos param2, BlockPos param3, boolean param4) throws CommandSyntaxException {
      BoundingBox var5 = BoundingBox.fromCorners(param1, param2);
      BoundingBox var6 = BoundingBox.fromCorners(param3, param3.offset(var5.getLength()));
      BlockPos var7 = new BlockPos(var6.minX() - var5.minX(), var6.minY() - var5.minY(), var6.minZ() - var5.minZ());
      int var8 = var5.getXSpan() * var5.getYSpan() * var5.getZSpan();
      if (var8 > 32768) {
         throw ERROR_AREA_TOO_LARGE.create(32768, var8);
      }

      int var9 = 0;

      for (int var10 = var5.minZ(); var10 <= var5.maxZ(); var10++) {
         for (int var11 = var5.minY(); var11 <= var5.maxY(); var11++) {
            for (int var12 = var5.minX(); var12 <= var5.maxX(); var12++) {
               BlockPos var13 = new BlockPos(var12, var11, var10);
               BlockPos var14 = var13.offset(var7);
               BlockState var15 = param0.getBlockState(var13);
               if (!param4 || !var15.is(Blocks.AIR)) {
                  if (var15 != param0.getBlockState(var14)) {
                     return OptionalInt.empty();
                  }

                  BlockEntity var16 = param0.getBlockEntity(var13);
                  BlockEntity var17 = param0.getBlockEntity(var14);
                  if (var16 != null) {
                     if (var17 == null) {
                        return OptionalInt.empty();
                     }

                     if (var17.getType() != var16.getType()) {
                        return OptionalInt.empty();
                     }

                     CompoundTag var18 = var16.saveWithoutMetadata();
                     CompoundTag var19 = var17.saveWithoutMetadata();
                     if (!var18.equals(var19)) {
                        return OptionalInt.empty();
                     }
                  }

                  var9++;
               }
            }
         }
      }

      return OptionalInt.of(var9);
   }

   private static RedirectModifier<CommandSourceStack> expandOneToOneEntityRelation(Function<Entity, Optional<Entity>> param0) {
      return l_param1 -> {
         CommandSourceStack l_var2 = l_param1.getSource();
         Entity l_var3 = l_var2.getEntity();
         return l_var3 == null
            ? List.of()
            : param0.apply(l_var3).filter(l2_param0 -> !l2_param0.isRemoved()).map(l2_param1 -> List.of(l_var2.withEntity(l2_param1))).orElse(List.of());
      };
   }

   private static RedirectModifier<CommandSourceStack> expandOneToManyEntityRelation(Function<Entity, Stream<Entity>> param0) {
      return l_param1 -> {
         CommandSourceStack l_var2 = l_param1.getSource();
         Entity l_var3 = l_var2.getEntity();
         return l_var3 == null ? List.of() : param0.apply(l_var3).filter(l2_param0 -> !l2_param0.isRemoved()).map(l_var2::withEntity).toList();
      };
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createRelationOperations(
      CommandNode<CommandSourceStack> param0, LiteralArgumentBuilder<CommandSourceStack> param1
   ) {
      return param1.then(
            Commands.literal("owner")
               .fork(
                  param0,
                  expandOneToOneEntityRelation(l_param0 -> l_param0 instanceof OwnableEntity l_var1 ? Optional.ofNullable(l_var1.getOwner()) : Optional.empty())
               )
         )
         .then(
            Commands.literal("leasher")
               .fork(
                  param0,
                  expandOneToOneEntityRelation(l_param0 -> l_param0 instanceof Mob l_var1 ? Optional.ofNullable(l_var1.getLeashHolder()) : Optional.empty())
               )
         )
         .then(
            Commands.literal("target")
               .fork(
                  param0,
                  expandOneToOneEntityRelation(l_param0 -> l_param0 instanceof Targeting l_var1 ? Optional.ofNullable(l_var1.getTarget()) : Optional.empty())
               )
         )
         .then(
            Commands.literal("attacker")
               .fork(
                  param0,
                  expandOneToOneEntityRelation(
                     l_param0 -> l_param0 instanceof Attackable l_var1 ? Optional.ofNullable(l_var1.getLastAttacker()) : Optional.empty()
                  )
               )
         )
         .then(Commands.literal("vehicle").fork(param0, expandOneToOneEntityRelation(l_param0 -> Optional.ofNullable(l_param0.getVehicle()))))
         .then(Commands.literal("controller").fork(param0, expandOneToOneEntityRelation(l_param0 -> Optional.ofNullable(l_param0.getControllingPassenger()))))
         .then(
            Commands.literal("origin")
               .fork(
                  param0,
                  expandOneToOneEntityRelation(
                     l_param0 -> l_param0 instanceof TraceableEntity l_var1 ? Optional.ofNullable(l_var1.getOwner()) : Optional.empty()
                  )
               )
         )
         .then(Commands.literal("passengers").fork(param0, expandOneToManyEntityRelation(l_param0 -> l_param0.getPassengers().stream())));
   }

   private static CommandSourceStack spawnEntityAndRedirect(CommandSourceStack param0, Holder.Reference<EntityType<?>> param1) throws CommandSyntaxException {
      Entity var2 = SummonCommand.createEntity(param0, param1, param0.getPosition(), new CompoundTag(), true);
      return param0.withEntity(var2);
   }

   @FunctionalInterface
   interface CommandNumericPredicate {
      int test(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface CommandPredicate {
      boolean test(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
   }
}
