package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Optional;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PlaceCommand {
   private static final SimpleCommandExceptionType ERROR_FEATURE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.place.feature.failed")
   );
   private static final SimpleCommandExceptionType ERROR_JIGSAW_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.jigsaw.failed"));
   private static final SimpleCommandExceptionType ERROR_STRUCTURE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.place.structure.failed")
   );
   private static final DynamicCommandExceptionType ERROR_TEMPLATE_INVALID = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.place.template.invalid", param0)
   );
   private static final SimpleCommandExceptionType ERROR_TEMPLATE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.place.template.failed")
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES = (param0, param1) -> {
      StructureTemplateManager var2 = param0.getSource().getLevel().getStructureManager();
      return SharedSuggestionProvider.suggestResource(var2.listTemplates(), param1);
   };

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("place")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("feature")
                  .then(
                     Commands.argument("feature", ResourceKeyArgument.key(Registries.CONFIGURED_FEATURE))
                        .executes(
                           l_param0 -> placeFeature(
                              l_param0.getSource(),
                              ResourceKeyArgument.getConfiguredFeature(l_param0, "feature"),
                              BlockPos.containing(l_param0.getSource().getPosition())
                           )
                        )
                        .then(
                           Commands.argument("pos", BlockPosArgument.blockPos())
                              .executes(
                                 l_param0 -> placeFeature(
                                    l_param0.getSource(),
                                    ResourceKeyArgument.getConfiguredFeature(l_param0, "feature"),
                                    BlockPosArgument.getLoadedBlockPos(l_param0, "pos")
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("jigsaw")
                  .then(
                     Commands.argument("pool", ResourceKeyArgument.key(Registries.TEMPLATE_POOL))
                        .then(
                           Commands.argument("target", ResourceLocationArgument.id())
                              .then(
                                 Commands.argument("max_depth", IntegerArgumentType.integer(1, 7))
                                    .executes(
                                       l_param0 -> placeJigsaw(
                                          l_param0.getSource(),
                                          ResourceKeyArgument.getStructureTemplatePool(l_param0, "pool"),
                                          ResourceLocationArgument.getId(l_param0, "target"),
                                          IntegerArgumentType.getInteger(l_param0, "max_depth"),
                                          BlockPos.containing(l_param0.getSource().getPosition())
                                       )
                                    )
                                    .then(
                                       Commands.argument("position", BlockPosArgument.blockPos())
                                          .executes(
                                             l_param0 -> placeJigsaw(
                                                l_param0.getSource(),
                                                ResourceKeyArgument.getStructureTemplatePool(l_param0, "pool"),
                                                ResourceLocationArgument.getId(l_param0, "target"),
                                                IntegerArgumentType.getInteger(l_param0, "max_depth"),
                                                BlockPosArgument.getLoadedBlockPos(l_param0, "position")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("structure")
                  .then(
                     Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                        .executes(
                           l_param0 -> placeStructure(
                              l_param0.getSource(),
                              ResourceKeyArgument.getStructure(l_param0, "structure"),
                              BlockPos.containing(l_param0.getSource().getPosition())
                           )
                        )
                        .then(
                           Commands.argument("pos", BlockPosArgument.blockPos())
                              .executes(
                                 l_param0 -> placeStructure(
                                    l_param0.getSource(),
                                    ResourceKeyArgument.getStructure(l_param0, "structure"),
                                    BlockPosArgument.getLoadedBlockPos(l_param0, "pos")
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("template")
                  .then(
                     Commands.argument("template", ResourceLocationArgument.id())
                        .suggests(SUGGEST_TEMPLATES)
                        .executes(
                           l_param0 -> placeTemplate(
                              l_param0.getSource(),
                              ResourceLocationArgument.getId(l_param0, "template"),
                              BlockPos.containing(l_param0.getSource().getPosition()),
                              Rotation.NONE,
                              Mirror.NONE,
                              1.0F,
                              0
                           )
                        )
                        .then(
                           Commands.argument("pos", BlockPosArgument.blockPos())
                              .executes(
                                 l_param0 -> placeTemplate(
                                    l_param0.getSource(),
                                    ResourceLocationArgument.getId(l_param0, "template"),
                                    BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                    Rotation.NONE,
                                    Mirror.NONE,
                                    1.0F,
                                    0
                                 )
                              )
                              .then(
                                 Commands.argument("rotation", TemplateRotationArgument.templateRotation())
                                    .executes(
                                       l_param0 -> placeTemplate(
                                          l_param0.getSource(),
                                          ResourceLocationArgument.getId(l_param0, "template"),
                                          BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                          TemplateRotationArgument.getRotation(l_param0, "rotation"),
                                          Mirror.NONE,
                                          1.0F,
                                          0
                                       )
                                    )
                                    .then(
                                       Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
                                          .executes(
                                             l_param0 -> placeTemplate(
                                                l_param0.getSource(),
                                                ResourceLocationArgument.getId(l_param0, "template"),
                                                BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                TemplateRotationArgument.getRotation(l_param0, "rotation"),
                                                TemplateMirrorArgument.getMirror(l_param0, "mirror"),
                                                1.0F,
                                                0
                                             )
                                          )
                                          .then(
                                             Commands.argument("integrity", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                .executes(
                                                   l_param0 -> placeTemplate(
                                                      l_param0.getSource(),
                                                      ResourceLocationArgument.getId(l_param0, "template"),
                                                      BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                      TemplateRotationArgument.getRotation(l_param0, "rotation"),
                                                      TemplateMirrorArgument.getMirror(l_param0, "mirror"),
                                                      FloatArgumentType.getFloat(l_param0, "integrity"),
                                                      0
                                                   )
                                                )
                                                .then(
                                                   Commands.argument("seed", IntegerArgumentType.integer())
                                                      .executes(
                                                         l_param0 -> placeTemplate(
                                                            l_param0.getSource(),
                                                            ResourceLocationArgument.getId(l_param0, "template"),
                                                            BlockPosArgument.getLoadedBlockPos(l_param0, "pos"),
                                                            TemplateRotationArgument.getRotation(l_param0, "rotation"),
                                                            TemplateMirrorArgument.getMirror(l_param0, "mirror"),
                                                            FloatArgumentType.getFloat(l_param0, "integrity"),
                                                            IntegerArgumentType.getInteger(l_param0, "seed")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   public static int placeFeature(CommandSourceStack param0, Holder.Reference<ConfiguredFeature<?, ?>> param1, BlockPos param2) throws CommandSyntaxException {
      ServerLevel var3 = param0.getLevel();
      ConfiguredFeature<?, ?> var4 = param1.value();
      ChunkPos var5 = new ChunkPos(param2);
      checkLoaded(var3, new ChunkPos(var5.x - 1, var5.z - 1), new ChunkPos(var5.x + 1, var5.z + 1));
      if (!var4.place(var3, var3.getChunkSource().getGenerator(), var3.getRandom(), param2)) {
         throw ERROR_FEATURE_FAILED.create();
      }

      String var6 = param1.key().location().toString();
      param0.sendSuccess(Component.translatable("commands.place.feature.success", var6, param2.getX(), param2.getY(), param2.getZ()), true);
      return 1;
   }

   public static int placeJigsaw(CommandSourceStack param0, Holder<StructureTemplatePool> param1, ResourceLocation param2, int param3, BlockPos param4) throws CommandSyntaxException {
      ServerLevel var5 = param0.getLevel();
      if (!JigsawPlacement.generateJigsaw(var5, param1, param2, param3, param4, false)) {
         throw ERROR_JIGSAW_FAILED.create();
      }

      param0.sendSuccess(Component.translatable("commands.place.jigsaw.success", param4.getX(), param4.getY(), param4.getZ()), true);
      return 1;
   }

   public static int placeStructure(CommandSourceStack param0, Holder.Reference<Structure> param1, BlockPos param2) throws CommandSyntaxException {
      ServerLevel var3 = param0.getLevel();
      Structure var4 = param1.value();
      ChunkGenerator var5 = var3.getChunkSource().getGenerator();
      StructureStart var6 = var4.generate(
         param0.registryAccess(),
         var5,
         var5.getBiomeSource(),
         var3.getChunkSource().randomState(),
         var3.getStructureManager(),
         var3.getSeed(),
         new ChunkPos(param2),
         0,
         var3,
         l_param0 -> true
      );
      if (!var6.isValid()) {
         throw ERROR_STRUCTURE_FAILED.create();
      }

      BoundingBox var7 = var6.getBoundingBox();
      ChunkPos var8 = new ChunkPos(SectionPos.blockToSectionCoord(var7.minX()), SectionPos.blockToSectionCoord(var7.minZ()));
      ChunkPos var9 = new ChunkPos(SectionPos.blockToSectionCoord(var7.maxX()), SectionPos.blockToSectionCoord(var7.maxZ()));
      checkLoaded(var3, var8, var9);
      ChunkPos.rangeClosed(var8, var9)
         .forEach(
            l_param3 -> var6.placeInChunk(
               var3,
               var3.structureManager(),
               var5,
               var3.getRandom(),
               new BoundingBox(
                  l_param3.getMinBlockX(),
                  var3.getMinBuildHeight(),
                  l_param3.getMinBlockZ(),
                  l_param3.getMaxBlockX(),
                  var3.getMaxBuildHeight(),
                  l_param3.getMaxBlockZ()
               ),
               l_param3
            )
         );
      String var10 = param1.key().location().toString();
      param0.sendSuccess(Component.translatable("commands.place.structure.success", var10, param2.getX(), param2.getY(), param2.getZ()), true);
      return 1;
   }

   public static int placeTemplate(
      CommandSourceStack param0, ResourceLocation param1, BlockPos param2, Rotation param3, Mirror param4, float param5, int param6
   ) throws CommandSyntaxException {
      ServerLevel var7 = param0.getLevel();
      StructureTemplateManager var8 = var7.getStructureManager();

      Optional<StructureTemplate> var9;
      try {
         var9 = var8.get(param1);
      } catch (ResourceLocationException var10) {
         throw ERROR_TEMPLATE_INVALID.create(param1);
      }

      if (var9.isEmpty()) {
         throw ERROR_TEMPLATE_INVALID.create(param1);
      }

      StructureTemplate var10 = var9.get();
      checkLoaded(var7, new ChunkPos(param2), new ChunkPos(param2.offset(var10.getSize())));
      StructurePlaceSettings var11 = new StructurePlaceSettings().setMirror(param4).setRotation(param3);
      if (param5 < 1.0F) {
         var11.clearProcessors().addProcessor(new BlockRotProcessor(param5)).setRandom(StructureBlockEntity.createRandom(param6));
      }

      boolean var12 = var10.placeInWorld(var7, param2, param2, var11, StructureBlockEntity.createRandom(param6), 2);
      if (!var12) {
         throw ERROR_TEMPLATE_FAILED.create();
      }

      param0.sendSuccess(Component.translatable("commands.place.template.success", param1, param2.getX(), param2.getY(), param2.getZ()), true);
      return 1;
   }

   private static void checkLoaded(ServerLevel param0, ChunkPos param1, ChunkPos param2) throws CommandSyntaxException {
      if (ChunkPos.rangeClosed(param1, param2).filter(l_param1 -> !param0.isLoaded(l_param1.getWorldPosition())).findAny().isPresent()) {
         throw BlockPosArgument.ERROR_NOT_LOADED.create();
      }
   }
}
