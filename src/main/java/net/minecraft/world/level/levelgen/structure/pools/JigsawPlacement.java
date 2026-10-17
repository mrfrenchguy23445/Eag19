package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class JigsawPlacement {
   static final Logger LOGGER = LogUtils.getLogger();

   public static Optional<Structure.GenerationStub> addPieces(
      Structure.GenerationContext param0,
      Holder<StructureTemplatePool> param1,
      Optional<ResourceLocation> param2,
      int param3,
      BlockPos param4,
      boolean param5,
      Optional<Heightmap.Types> param6,
      int param7
   ) {
      RegistryAccess var8 = param0.registryAccess();
      ChunkGenerator var9 = param0.chunkGenerator();
      StructureTemplateManager var10 = param0.structureTemplateManager();
      LevelHeightAccessor var11 = param0.heightAccessor();
      WorldgenRandom var12 = param0.random();
      Registry<StructureTemplatePool> var13 = var8.registryOrThrow(Registries.TEMPLATE_POOL);
      Rotation var14 = Rotation.getRandom(var12);
      StructureTemplatePool var15 = param1.value();
      StructurePoolElement var16 = var15.getRandomTemplate(var12);
      if (var16 == EmptyPoolElement.INSTANCE) {
         return Optional.empty();
      }

      BlockPos var17;
      if (param2.isPresent()) {
         ResourceLocation var18 = param2.get();
         Optional<BlockPos> var19 = getRandomNamedJigsaw(var16, var18, param4, var14, var10, var12);
         if (var19.isEmpty()) {
            LOGGER.error(
               "No starting jigsaw {} found in start pool {}",
               var18,
               param1.unwrapKey().map(l_param0 -> l_param0.location().toString()).orElse("<unregistered>")
            );
            return Optional.empty();
         }

         var17 = var19.get();
      } else {
         var17 = param4;
      }

      Vec3i var18 = var17.subtract(param4);
      BlockPos var19 = param4.subtract(var18);
      PoolElementStructurePiece var20 = new PoolElementStructurePiece(
         var10, var16, var19, var16.getGroundLevelDelta(), var14, var16.getBoundingBox(var10, var19, var14)
      );
      BoundingBox var21 = var20.getBoundingBox();
      int var22 = (var21.maxX() + var21.minX()) / 2;
      int var23 = (var21.maxZ() + var21.minZ()) / 2;
      int var24;
      if (param6.isPresent()) {
         var24 = param4.getY() + var9.getFirstFreeHeight(var22, var23, param6.get(), var11, param0.randomState());
      } else {
         var24 = var19.getY();
      }

      int var25 = var21.minY() + var20.getGroundLevelDelta();
      var20.move(0, var24 - var25, 0);
      int var26 = var24 + var18.getY();
      return Optional.of(new Structure.GenerationStub(new BlockPos(var22, var26, var23), l_param14 -> {
         List<PoolElementStructurePiece> l_var15 = Lists.newArrayList();
         l_var15.add(var20);
         if (param3 > 0) {
            AABB l_var16 = new AABB(var22 - param7, var26 - param7, var23 - param7, var22 + param7 + 1, var26 + param7 + 1, var23 + param7 + 1);
            VoxelShape l_var17 = Shapes.join(Shapes.create(l_var16), Shapes.create(AABB.of(var21)), BooleanOp.ONLY_FIRST);
            addPieces(param0.randomState(), param3, param5, var9, var10, var11, var12, var13, var20, l_var15, l_var17);
            l_var15.forEach(l_param14::addPiece);
         }
      }));
   }

   private static Optional<BlockPos> getRandomNamedJigsaw(
      StructurePoolElement param0, ResourceLocation param1, BlockPos param2, Rotation param3, StructureTemplateManager param4, WorldgenRandom param5
   ) {
      List<StructureTemplate.StructureBlockInfo> var6 = param0.getShuffledJigsawBlocks(param4, param2, param3, param5);
      Optional<BlockPos> var7 = Optional.empty();

      for (StructureTemplate.StructureBlockInfo var9 : var6) {
         ResourceLocation var10 = ResourceLocation.tryParse(var9.nbt.getString("name"));
         if (param1.equals(var10)) {
            var7 = Optional.of(var9.pos);
            break;
         }
      }

      return var7;
   }

   private static void addPieces(
      RandomState param0,
      int param1,
      boolean param2,
      ChunkGenerator param3,
      StructureTemplateManager param4,
      LevelHeightAccessor param5,
      RandomSource param6,
      Registry<StructureTemplatePool> param7,
      PoolElementStructurePiece param8,
      List<PoolElementStructurePiece> param9,
      VoxelShape param10
   ) {
      JigsawPlacement.Placer var11 = new JigsawPlacement.Placer(param7, param1, param3, param4, param9, param6);
      var11.placing.addLast(new JigsawPlacement.PieceState(param8, new MutableObject<>(param10), 0));

      while (!var11.placing.isEmpty()) {
         JigsawPlacement.PieceState var12 = var11.placing.removeFirst();
         var11.tryPlacingChildren(var12.piece, var12.free, var12.depth, param2, param5, param0);
      }
   }

   public static boolean generateJigsaw(
      ServerLevel param0, Holder<StructureTemplatePool> param1, ResourceLocation param2, int param3, BlockPos param4, boolean param5
   ) {
      ChunkGenerator var6 = param0.getChunkSource().getGenerator();
      StructureTemplateManager var7 = param0.getStructureManager();
      StructureManager var8 = param0.structureManager();
      RandomSource var9 = param0.getRandom();
      Structure.GenerationContext var10 = new Structure.GenerationContext(
         param0.registryAccess(),
         var6,
         var6.getBiomeSource(),
         param0.getChunkSource().randomState(),
         var7,
         param0.getSeed(),
         new ChunkPos(param4),
         param0,
         l_param0 -> true
      );
      Optional<Structure.GenerationStub> var11 = addPieces(var10, param1, Optional.of(param2), param3, param4, false, Optional.empty(), 128);
      if (var11.isPresent()) {
         StructurePiecesBuilder var12 = var11.get().getPiecesBuilder();

         for (StructurePiece var14 : var12.build().pieces()) {
            if (var14 instanceof PoolElementStructurePiece var15) {
               var15.place(param0, var8, var6, var9, BoundingBox.infinite(), param4, param5);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   static final class PieceState {
      final PoolElementStructurePiece piece;
      final MutableObject<VoxelShape> free;
      final int depth;

      PieceState(PoolElementStructurePiece param0, MutableObject<VoxelShape> param1, int param2) {
         this.piece = param0;
         this.free = param1;
         this.depth = param2;
      }
   }

   static final class Placer {
      private final Registry<StructureTemplatePool> pools;
      private final int maxDepth;
      private final ChunkGenerator chunkGenerator;
      private final StructureTemplateManager structureTemplateManager;
      private final List<? super PoolElementStructurePiece> pieces;
      private final RandomSource random;
      final Deque<JigsawPlacement.PieceState> placing = Queues.newArrayDeque();

      Placer(
         Registry<StructureTemplatePool> param0,
         int param1,
         ChunkGenerator param2,
         StructureTemplateManager param3,
         List<? super PoolElementStructurePiece> param4,
         RandomSource param5
      ) {
         this.pools = param0;
         this.maxDepth = param1;
         this.chunkGenerator = param2;
         this.structureTemplateManager = param3;
         this.pieces = param4;
         this.random = param5;
      }

      void tryPlacingChildren(
         PoolElementStructurePiece param0, MutableObject<VoxelShape> param1, int param2, boolean param3, LevelHeightAccessor param4, RandomState param5
      ) {
         StructurePoolElement var7 = param0.getElement();
         BlockPos var8 = param0.getPosition();
         Rotation var9 = param0.getRotation();
         StructureTemplatePool.Projection var10 = var7.getProjection();
         boolean var11 = var10 == StructureTemplatePool.Projection.RIGID;
         MutableObject<VoxelShape> var12 = new MutableObject<>();
         BoundingBox var13 = param0.getBoundingBox();
         int var14 = var13.minY();

         label129:
         for (StructureTemplate.StructureBlockInfo var16 : var7.getShuffledJigsawBlocks(this.structureTemplateManager, var8, var9, this.random)) {
            Direction var17 = JigsawBlock.getFrontFacing(var16.state);
            BlockPos var18 = var16.pos;
            BlockPos var19 = var18.relative(var17);
            int var20 = var18.getY() - var14;
            int var21 = -1;
            ResourceKey<StructureTemplatePool> var22 = readPoolName(var16);
            Optional<? extends Holder<StructureTemplatePool>> var23 = this.pools.getHolder(var22);
            if (var23.isEmpty()) {
               JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", var22.location());
            } else {
               Holder<StructureTemplatePool> var24 = (Holder<StructureTemplatePool>)var23.get();
               if (var24.value().size() == 0 && !var24.is(Pools.EMPTY)) {
                  JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", var22.location());
               } else {
                  Holder<StructureTemplatePool> var25 = var24.value().getFallback();
                  if (var25.value().size() == 0 && !var25.is(Pools.EMPTY)) {
                     JigsawPlacement.LOGGER
                        .warn(
                           "Empty or non-existent fallback pool: {}",
                           var25.unwrapKey().map(l_param0 -> l_param0.location().toString()).orElse("<unregistered>")
                        );
                  } else {
                     boolean var27 = var13.isInside(var19);
                     MutableObject<VoxelShape> var26;
                     if (var27) {
                        var26 = var12;
                        if (var12.getValue() == null) {
                           var12.setValue(Shapes.create(AABB.of(var13)));
                        }
                     } else {
                        var26 = param1;
                     }

                     List<StructurePoolElement> var28 = Lists.newArrayList();
                     if (param2 != this.maxDepth) {
                        var28.addAll(var24.value().getShuffledTemplates(this.random));
                     }

                     var28.addAll(var25.value().getShuffledTemplates(this.random));

                     for (StructurePoolElement var30 : var28) {
                        if (var30 == EmptyPoolElement.INSTANCE) {
                           break;
                        }

                        for (Rotation var32 : Rotation.getShuffled(this.random)) {
                           List<StructureTemplate.StructureBlockInfo> var33 = var30.getShuffledJigsawBlocks(
                              this.structureTemplateManager, BlockPos.ZERO, var32, this.random
                           );
                           BoundingBox var34 = var30.getBoundingBox(this.structureTemplateManager, BlockPos.ZERO, var32);
                           int var35;
                           if (param3 && var34.getYSpan() <= 16) {
                              var35 = var33.stream().mapToInt(l_param1 -> {
                                 if (!var34.isInside(l_param1.pos.relative(JigsawBlock.getFrontFacing(l_param1.state)))) {
                                    return 0;
                                 }

                                 ResourceKey<StructureTemplatePool> l_var3 = readPoolName(l_param1);
                                 Optional<? extends Holder<StructureTemplatePool>> l_var4 = this.pools.getHolder(l_var3);
                                 Optional<Holder<StructureTemplatePool>> l_var5 = l_var4.map(l2_param0 -> l2_param0.value().getFallback());
                                 int l_var6 = l_var4.<Integer>map(l2_param0 -> l2_param0.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                 int l_var7 = l_var5.<Integer>map(l2_param0 -> l2_param0.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                 return Math.max(l_var6, l_var7);
                              }).max().orElse(0);
                           } else {
                              var35 = 0;
                           }

                           for (StructureTemplate.StructureBlockInfo var37 : var33) {
                              if (JigsawBlock.canAttach(var16, var37)) {
                                 BlockPos var38 = var37.pos;
                                 BlockPos var39 = var19.subtract(var38);
                                 BoundingBox var40 = var30.getBoundingBox(this.structureTemplateManager, var39, var32);
                                 int var41 = var40.minY();
                                 StructureTemplatePool.Projection var42 = var30.getProjection();
                                 boolean var43 = var42 == StructureTemplatePool.Projection.RIGID;
                                 int var44 = var38.getY();
                                 int var45 = var20 - var44 + JigsawBlock.getFrontFacing(var16.state).getStepY();
                                 int var46;
                                 if (var11 && var43) {
                                    var46 = var14 + var45;
                                 } else {
                                    if (var21 == -1) {
                                       var21 = this.chunkGenerator
                                          .getFirstFreeHeight(var18.getX(), var18.getZ(), Heightmap.Types.WORLD_SURFACE_WG, param4, param5);
                                    }

                                    var46 = var21 - var44;
                                 }

                                 int var47 = var46 - var41;
                                 BoundingBox var48 = var40.moved(0, var47, 0);
                                 BlockPos var49 = var39.offset(0, var47, 0);
                                 if (var35 > 0) {
                                    int var50 = Math.max(var35 + 1, var48.maxY() - var48.minY());
                                    var48.encapsulate(new BlockPos(var48.minX(), var48.minY() + var50, var48.minZ()));
                                 }

                                 if (!Shapes.joinIsNotEmpty(var26.getValue(), Shapes.create(AABB.of(var48).deflate(0.25)), BooleanOp.ONLY_SECOND)) {
                                    var26.setValue(Shapes.joinUnoptimized(var26.getValue(), Shapes.create(AABB.of(var48)), BooleanOp.ONLY_FIRST));
                                    int var50 = param0.getGroundLevelDelta();
                                    int var51;
                                    if (var43) {
                                       var51 = var50 - var45;
                                    } else {
                                       var51 = var30.getGroundLevelDelta();
                                    }

                                    PoolElementStructurePiece var52 = new PoolElementStructurePiece(
                                       this.structureTemplateManager, var30, var49, var51, var32, var48
                                    );
                                    int var53;
                                    if (var11) {
                                       var53 = var14 + var20;
                                    } else if (var43) {
                                       var53 = var46 + var44;
                                    } else {
                                       if (var21 == -1) {
                                          var21 = this.chunkGenerator
                                             .getFirstFreeHeight(var18.getX(), var18.getZ(), Heightmap.Types.WORLD_SURFACE_WG, param4, param5);
                                       }

                                       var53 = var21 + var45 / 2;
                                    }

                                    param0.addJunction(new JigsawJunction(var19.getX(), var53 - var20 + var50, var19.getZ(), var45, var42));
                                    var52.addJunction(new JigsawJunction(var18.getX(), var53 - var44 + var51, var18.getZ(), -var45, var10));
                                    this.pieces.add(var52);
                                    if (param2 + 1 <= this.maxDepth) {
                                       this.placing.addLast(new JigsawPlacement.PieceState(var52, var26, param2 + 1));
                                    }
                                    continue label129;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      private static ResourceKey<StructureTemplatePool> readPoolName(StructureTemplate.StructureBlockInfo param0) {
         return ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(param0.nbt.getString("pool")));
      }
   }
}
