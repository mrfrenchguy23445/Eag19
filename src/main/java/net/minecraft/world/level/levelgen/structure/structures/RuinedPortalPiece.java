package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlackstoneReplaceProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockAgeProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.LavaSubmergedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public class RuinedPortalPiece extends TemplateStructurePiece {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final float PROBABILITY_OF_GOLD_GONE = 0.3F;
   private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_NETHERRACK = 0.07F;
   private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_LAVA = 0.2F;
   private final RuinedPortalPiece.VerticalPlacement verticalPlacement;
   private final RuinedPortalPiece.Properties properties;

   public RuinedPortalPiece(
      StructureTemplateManager param0,
      BlockPos param1,
      RuinedPortalPiece.VerticalPlacement param2,
      RuinedPortalPiece.Properties param3,
      ResourceLocation param4,
      StructureTemplate param5,
      Rotation param6,
      Mirror param7,
      BlockPos param8
   ) {
      super(StructurePieceType.RUINED_PORTAL, 0, param0, param4, param4.toString(), makeSettings(param7, param6, param2, param8, param3), param1);
      this.verticalPlacement = param2;
      this.properties = param3;
   }

   public RuinedPortalPiece(StructureTemplateManager param0, CompoundTag param1) {
      super(StructurePieceType.RUINED_PORTAL, param1, param0, l_param2 -> makeSettings(param0, param1, l_param2));
      this.verticalPlacement = RuinedPortalPiece.VerticalPlacement.byName(param1.getString("VerticalPlacement"));
      this.properties = RuinedPortalPiece.Properties.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, param1.get("Properties"))).getOrThrow(true, LOGGER::error);
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
      super.addAdditionalSaveData(param0, param1);
      param1.putString("Rotation", this.placeSettings.getRotation().name());
      param1.putString("Mirror", this.placeSettings.getMirror().name());
      param1.putString("VerticalPlacement", this.verticalPlacement.getName());
      RuinedPortalPiece.Properties.CODEC
         .encodeStart(NbtOps.INSTANCE, this.properties)
         .resultOrPartial(LOGGER::error)
         .ifPresent(l_param1 -> param1.put("Properties", l_param1));
   }

   private static StructurePlaceSettings makeSettings(StructureTemplateManager param0, CompoundTag param1, ResourceLocation param2) {
      StructureTemplate var3 = param0.getOrCreate(param2);
      BlockPos var4 = new BlockPos(var3.getSize().getX() / 2, 0, var3.getSize().getZ() / 2);
      return makeSettings(
         Mirror.valueOf(param1.getString("Mirror")),
         Rotation.valueOf(param1.getString("Rotation")),
         RuinedPortalPiece.VerticalPlacement.byName(param1.getString("VerticalPlacement")),
         var4,
         RuinedPortalPiece.Properties.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, param1.get("Properties"))).getOrThrow(true, LOGGER::error)
      );
   }

   private static StructurePlaceSettings makeSettings(
      Mirror param0, Rotation param1, RuinedPortalPiece.VerticalPlacement param2, BlockPos param3, RuinedPortalPiece.Properties param4
   ) {
      BlockIgnoreProcessor var5 = param4.airPocket ? BlockIgnoreProcessor.STRUCTURE_BLOCK : BlockIgnoreProcessor.STRUCTURE_AND_AIR;
      List<ProcessorRule> var6 = Lists.newArrayList();
      var6.add(getBlockReplaceRule(Blocks.GOLD_BLOCK, 0.3F, Blocks.AIR));
      var6.add(getLavaProcessorRule(param2, param4));
      if (!param4.cold) {
         var6.add(getBlockReplaceRule(Blocks.NETHERRACK, 0.07F, Blocks.MAGMA_BLOCK));
      }

      StructurePlaceSettings var7 = new StructurePlaceSettings()
         .setRotation(param1)
         .setMirror(param0)
         .setRotationPivot(param3)
         .addProcessor(var5)
         .addProcessor(new RuleProcessor(var6))
         .addProcessor(new BlockAgeProcessor(param4.mossiness))
         .addProcessor(new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE))
         .addProcessor(new LavaSubmergedBlockProcessor());
      if (param4.replaceWithBlackstone) {
         var7.addProcessor(BlackstoneReplaceProcessor.INSTANCE);
      }

      return var7;
   }

   private static ProcessorRule getLavaProcessorRule(RuinedPortalPiece.VerticalPlacement param0, RuinedPortalPiece.Properties param1) {
      if (param0 == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR) {
         return getBlockReplaceRule(Blocks.LAVA, Blocks.MAGMA_BLOCK);
      } else {
         return param1.cold ? getBlockReplaceRule(Blocks.LAVA, Blocks.NETHERRACK) : getBlockReplaceRule(Blocks.LAVA, 0.2F, Blocks.MAGMA_BLOCK);
      }
   }

   @Override
   public void postProcess(
      WorldGenLevel param0, StructureManager param1, ChunkGenerator param2, RandomSource param3, BoundingBox param4, ChunkPos param5, BlockPos param6
   ) {
      BoundingBox var8 = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
      if (param4.isInside(var8.getCenter())) {
         param4.encapsulate(var8);
         super.postProcess(param0, param1, param2, param3, param4, param5, param6);
         this.spreadNetherrack(param3, param0);
         this.addNetherrackDripColumnsBelowPortal(param3, param0);
         if (this.properties.vines || this.properties.overgrown) {
            BlockPos.betweenClosedStream(this.getBoundingBox()).forEach(l_param2 -> {
               if (this.properties.vines) {
                  this.maybeAddVines(param3, param0, l_param2);
               }

               if (this.properties.overgrown) {
                  this.maybeAddLeavesAbove(param3, param0, l_param2);
               }
            });
         }
      }
   }

   @Override
   protected void handleDataMarker(String param0, BlockPos param1, ServerLevelAccessor param2, RandomSource param3, BoundingBox param4) {
   }

   private void maybeAddVines(RandomSource param0, LevelAccessor param1, BlockPos param2) {
      BlockState var4 = param1.getBlockState(param2);
      if (!var4.isAir() && !var4.is(Blocks.VINE)) {
         Direction var5 = getRandomHorizontalDirection(param0);
         BlockPos var6 = param2.relative(var5);
         BlockState var7 = param1.getBlockState(var6);
         if (var7.isAir()) {
            if (Block.isFaceFull(var4.getCollisionShape(param1, param2), var5)) {
               BooleanProperty var8 = VineBlock.getPropertyForFace(var5.getOpposite());
               param1.setBlock(var6, Blocks.VINE.defaultBlockState().setValue(var8, true), 3);
            }
         }
      }
   }

   private void maybeAddLeavesAbove(RandomSource param0, LevelAccessor param1, BlockPos param2) {
      if (param0.nextFloat() < 0.5F && param1.getBlockState(param2).is(Blocks.NETHERRACK) && param1.getBlockState(param2.above()).isAir()) {
         param1.setBlock(param2.above(), Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 3);
      }
   }

   private void addNetherrackDripColumnsBelowPortal(RandomSource param0, LevelAccessor param1) {
      for (int var3 = this.boundingBox.minX() + 1; var3 < this.boundingBox.maxX(); var3++) {
         for (int var4 = this.boundingBox.minZ() + 1; var4 < this.boundingBox.maxZ(); var4++) {
            BlockPos var5 = new BlockPos(var3, this.boundingBox.minY(), var4);
            if (param1.getBlockState(var5).is(Blocks.NETHERRACK)) {
               this.addNetherrackDripColumn(param0, param1, var5.below());
            }
         }
      }
   }

   private void addNetherrackDripColumn(RandomSource param0, LevelAccessor param1, BlockPos param2) {
      BlockPos.MutableBlockPos var4 = param2.mutable();
      this.placeNetherrackOrMagma(param0, param1, var4);
      int var5 = 8;

      while (var5 > 0 && param0.nextFloat() < 0.5F) {
         var4.move(Direction.DOWN);
         var5--;
         this.placeNetherrackOrMagma(param0, param1, var4);
      }
   }

   private void spreadNetherrack(RandomSource param0, LevelAccessor param1) {
      boolean var3 = this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_LAND_SURFACE
         || this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR;
      BlockPos var4 = this.boundingBox.getCenter();
      int var5 = var4.getX();
      int var6 = var4.getZ();
      float[] var7 = new float[]{1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.9F, 0.9F, 0.8F, 0.7F, 0.6F, 0.4F, 0.2F};
      int var8 = var7.length;
      int var9 = (this.boundingBox.getXSpan() + this.boundingBox.getZSpan()) / 2;
      int var10 = param0.nextInt(Math.max(1, 8 - var9 / 2));
      int var11 = 3;
      BlockPos.MutableBlockPos var12 = BlockPos.ZERO.mutable();

      for (int var13 = var5 - var8; var13 <= var5 + var8; var13++) {
         for (int var14 = var6 - var8; var14 <= var6 + var8; var14++) {
            int var15 = Math.abs(var13 - var5) + Math.abs(var14 - var6);
            int var16 = Math.max(0, var15 + var10);
            if (var16 < var8) {
               float var17 = var7[var16];
               if (param0.nextDouble() < var17) {
                  int var18 = getSurfaceY(param1, var13, var14, this.verticalPlacement);
                  int var19 = var3 ? var18 : Math.min(this.boundingBox.minY(), var18);
                  var12.set(var13, var19, var14);
                  if (Math.abs(var19 - this.boundingBox.minY()) <= 3 && this.canBlockBeReplacedByNetherrackOrMagma(param1, var12)) {
                     this.placeNetherrackOrMagma(param0, param1, var12);
                     if (this.properties.overgrown) {
                        this.maybeAddLeavesAbove(param0, param1, var12);
                     }

                     this.addNetherrackDripColumn(param0, param1, var12.below());
                  }
               }
            }
         }
      }
   }

   private boolean canBlockBeReplacedByNetherrackOrMagma(LevelAccessor param0, BlockPos param1) {
      BlockState var3 = param0.getBlockState(param1);
      return !var3.is(Blocks.AIR)
         && !var3.is(Blocks.OBSIDIAN)
         && !var3.is(BlockTags.FEATURES_CANNOT_REPLACE)
         && (this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_NETHER || !var3.is(Blocks.LAVA));
   }

   private void placeNetherrackOrMagma(RandomSource param0, LevelAccessor param1, BlockPos param2) {
      if (!this.properties.cold && param0.nextFloat() < 0.07F) {
         param1.setBlock(param2, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
      } else {
         param1.setBlock(param2, Blocks.NETHERRACK.defaultBlockState(), 3);
      }
   }

   private static int getSurfaceY(LevelAccessor param0, int param1, int param2, RuinedPortalPiece.VerticalPlacement param3) {
      return param0.getHeight(getHeightMapType(param3), param1, param2) - 1;
   }

   public static Heightmap.Types getHeightMapType(RuinedPortalPiece.VerticalPlacement param0) {
      return param0 == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
   }

   private static ProcessorRule getBlockReplaceRule(Block param0, float param1, Block param2) {
      return new ProcessorRule(new RandomBlockMatchTest(param0, param1), AlwaysTrueTest.INSTANCE, param2.defaultBlockState());
   }

   private static ProcessorRule getBlockReplaceRule(Block param0, Block param1) {
      return new ProcessorRule(new BlockMatchTest(param0), AlwaysTrueTest.INSTANCE, param1.defaultBlockState());
   }

   public static class Properties {
      public static final Codec<RuinedPortalPiece.Properties> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               Codec.BOOL.fieldOf("cold").forGetter(l_param0 -> l_param0.cold),
               Codec.FLOAT.fieldOf("mossiness").forGetter(l_param0 -> l_param0.mossiness),
               Codec.BOOL.fieldOf("air_pocket").forGetter(l_param0 -> l_param0.airPocket),
               Codec.BOOL.fieldOf("overgrown").forGetter(l_param0 -> l_param0.overgrown),
               Codec.BOOL.fieldOf("vines").forGetter(l_param0 -> l_param0.vines),
               Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(l_param0 -> l_param0.replaceWithBlackstone)
            )
            .apply(param0, RuinedPortalPiece.Properties::new)
      );
      public boolean cold;
      public float mossiness;
      public boolean airPocket;
      public boolean overgrown;
      public boolean vines;
      public boolean replaceWithBlackstone;

      public Properties() {
      }

      public Properties(boolean param0, float param1, boolean param2, boolean param3, boolean param4, boolean param5) {
         this.cold = param0;
         this.mossiness = param1;
         this.airPocket = param2;
         this.overgrown = param3;
         this.vines = param4;
         this.replaceWithBlackstone = param5;
      }
   }

   public enum VerticalPlacement implements StringRepresentable {
      ON_LAND_SURFACE("on_land_surface"),
      PARTLY_BURIED("partly_buried"),
      ON_OCEAN_FLOOR("on_ocean_floor"),
      IN_MOUNTAIN("in_mountain"),
      UNDERGROUND("underground"),
      IN_NETHER("in_nether");

      public static final StringRepresentable.EnumCodec<RuinedPortalPiece.VerticalPlacement> CODEC = StringRepresentable.fromEnum(
         RuinedPortalPiece.VerticalPlacement::values
      );
      private final String name;

      VerticalPlacement(String param2) {
         this.name = param2;
      }

      public String getName() {
         return this.name;
      }

      public static RuinedPortalPiece.VerticalPlacement byName(String param0) {
         return CODEC.byName(param0);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
