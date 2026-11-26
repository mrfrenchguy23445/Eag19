package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class SinglePoolElement extends StructurePoolElement {
   private static final Codec<Either<ResourceLocation, StructureTemplate>> TEMPLATE_CODEC = Codec.of(
      SinglePoolElement::encodeTemplate, ResourceLocation.CODEC.map(Either::left)
   );
   public static final Codec<SinglePoolElement> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(templateCodec(), processorsCodec(), projectionCodec()).apply(param0, SinglePoolElement::new)
   );
   protected final Either<ResourceLocation, StructureTemplate> template;
   protected final Holder<StructureProcessorList> processors;

   private static <T> DataResult<T> encodeTemplate(Either<ResourceLocation, StructureTemplate> param0, DynamicOps<T> param1, T param2) {
      Optional<ResourceLocation> var3 = param0.left();
      return !var3.isPresent() ? DataResult.error(() -> "Can not serialize a runtime pool element") : ResourceLocation.CODEC.encode(var3.get(), param1, param2);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Holder<StructureProcessorList>> processorsCodec() {
      return StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(l_param0 -> l_param0.processors);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Either<ResourceLocation, StructureTemplate>> templateCodec() {
      return TEMPLATE_CODEC.fieldOf("location").forGetter(l_param0 -> l_param0.template);
   }

   protected SinglePoolElement(
      Either<ResourceLocation, StructureTemplate> param0, Holder<StructureProcessorList> param1, StructureTemplatePool.Projection param2
   ) {
      super(param2);
      this.template = param0;
      this.processors = param1;
   }

   @Override
   public Vec3i getSize(StructureTemplateManager param0, Rotation param1) {
      StructureTemplate var3 = this.getTemplate(param0);
      return var3.getSize(param1);
   }

   private StructureTemplate getTemplate(StructureTemplateManager param0) {
      return this.template.map(param0::getOrCreate, Function.identity());
   }

   public List<StructureTemplate.StructureBlockInfo> getDataMarkers(StructureTemplateManager param0, BlockPos param1, Rotation param2, boolean param3) {
      StructureTemplate var5 = this.getTemplate(param0);
      List<StructureTemplate.StructureBlockInfo> var6 = var5.filterBlocks(
         param1, new StructurePlaceSettings().setRotation(param2), Blocks.STRUCTURE_BLOCK, param3
      );
      List<StructureTemplate.StructureBlockInfo> var7 = Lists.newArrayList();

      for (StructureTemplate.StructureBlockInfo var9 : var6) {
         if (var9.nbt != null) {
            StructureMode var10 = StructureMode.valueOf(var9.nbt.getString("mode"));
            if (var10 == StructureMode.DATA) {
               var7.add(var9);
            }
         }
      }

      return var7;
   }

   @Override
   public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager param0, BlockPos param1, Rotation param2, RandomSource param3
   ) {
      StructureTemplate var5 = this.getTemplate(param0);
      ObjectArrayList<StructureTemplate.StructureBlockInfo> var6 = var5.filterBlocks(
         param1, new StructurePlaceSettings().setRotation(param2), Blocks.JIGSAW, true
      );
      Util.shuffle(var6, param3);
      return var6;
   }

   @Override
   public BoundingBox getBoundingBox(StructureTemplateManager param0, BlockPos param1, Rotation param2) {
      StructureTemplate var4 = this.getTemplate(param0);
      return var4.getBoundingBox(new StructurePlaceSettings().setRotation(param2), param1);
   }

   @Override
   public boolean place(
      StructureTemplateManager param0,
      WorldGenLevel param1,
      StructureManager param2,
      ChunkGenerator param3,
      BlockPos param4,
      BlockPos param5,
      Rotation param6,
      BoundingBox param7,
      RandomSource param8,
      boolean param9
   ) {
      StructureTemplate var11 = this.getTemplate(param0);
      StructurePlaceSettings var12 = this.getSettings(param6, param7, param9);
      if (!var11.placeInWorld(param1, param4, param5, var12, param8, 18)) {
         return false;
      }

      for (StructureTemplate.StructureBlockInfo var15 : StructureTemplate.processBlockInfos(
         param1, param4, param5, var12, this.getDataMarkers(param0, param4, param6, false)
      )) {
         this.handleDataMarker(param1, var15, param4, param6, param8, param7);
      }

      return true;
   }

   protected StructurePlaceSettings getSettings(Rotation param0, BoundingBox param1, boolean param2) {
      StructurePlaceSettings var4 = new StructurePlaceSettings();
      var4.setBoundingBox(param1);
      var4.setRotation(param0);
      var4.setKnownShape(true);
      var4.setIgnoreEntities(false);
      var4.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      var4.setFinalizeEntities(true);
      if (!param2) {
         var4.addProcessor(JigsawReplacementProcessor.INSTANCE);
      }

      this.processors.value().list().forEach(var4::addProcessor);
      this.getProjection().getProcessors().forEach(var4::addProcessor);
      return var4;
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.SINGLE;
   }

   @Override
   public String toString() {
      return "Single[" + this.template + "]";
   }
}
