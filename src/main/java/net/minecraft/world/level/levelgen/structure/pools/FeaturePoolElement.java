package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class FeaturePoolElement extends StructurePoolElement {
   public static final Codec<FeaturePoolElement> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(PlacedFeature.CODEC.fieldOf("feature").forGetter(l_param0 -> l_param0.feature), projectionCodec())
         .apply(param0, FeaturePoolElement::new)
   );
   private final Holder<PlacedFeature> feature;
   private final CompoundTag defaultJigsawNBT;

   protected FeaturePoolElement(Holder<PlacedFeature> param0, StructureTemplatePool.Projection param1) {
      super(param1);
      this.feature = param0;
      this.defaultJigsawNBT = this.fillDefaultJigsawNBT();
   }

   private CompoundTag fillDefaultJigsawNBT() {
      CompoundTag var1 = new CompoundTag();
      var1.putString("name", "minecraft:bottom");
      var1.putString("final_state", "minecraft:air");
      var1.putString("pool", "minecraft:empty");
      var1.putString("target", "minecraft:empty");
      var1.putString("joint", JigsawBlockEntity.JointType.ROLLABLE.getSerializedName());
      return var1;
   }

   @Override
   public Vec3i getSize(StructureTemplateManager param0, Rotation param1) {
      return Vec3i.ZERO;
   }

   @Override
   public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager param0, BlockPos param1, Rotation param2, RandomSource param3
   ) {
      List<StructureTemplate.StructureBlockInfo> var5 = Lists.newArrayList();
      var5.add(
         new StructureTemplate.StructureBlockInfo(
            param1,
            Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, FrontAndTop.fromFrontAndTop(Direction.DOWN, Direction.SOUTH)),
            this.defaultJigsawNBT
         )
      );
      return var5;
   }

   @Override
   public BoundingBox getBoundingBox(StructureTemplateManager param0, BlockPos param1, Rotation param2) {
      Vec3i var4 = this.getSize(param0, param2);
      return new BoundingBox(param1.getX(), param1.getY(), param1.getZ(), param1.getX() + var4.getX(), param1.getY() + var4.getY(), param1.getZ() + var4.getZ());
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
      return this.feature.value().place(param1, param3, param8, param4);
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.FEATURE;
   }

   @Override
   public String toString() {
      return "Feature[" + this.feature + "]";
   }
}
