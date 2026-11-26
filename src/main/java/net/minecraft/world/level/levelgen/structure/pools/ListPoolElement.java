package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class ListPoolElement extends StructurePoolElement {
   public static final Codec<ListPoolElement> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(StructurePoolElement.CODEC.listOf().fieldOf("elements").forGetter(l_param0 -> l_param0.elements), projectionCodec())
         .apply(param0, ListPoolElement::new)
   );
   private final List<StructurePoolElement> elements;

   public ListPoolElement(List<StructurePoolElement> param0, StructureTemplatePool.Projection param1) {
      super(param1);
      if (param0.isEmpty()) {
         throw new IllegalArgumentException("Elements are empty");
      }

      this.elements = param0;
      this.setProjectionOnEachElement(param1);
   }

   @Override
   public Vec3i getSize(StructureTemplateManager param0, Rotation param1) {
      int var3 = 0;
      int var4 = 0;
      int var5 = 0;

      for (StructurePoolElement var7 : this.elements) {
         Vec3i var8 = var7.getSize(param0, param1);
         var3 = Math.max(var3, var8.getX());
         var4 = Math.max(var4, var8.getY());
         var5 = Math.max(var5, var8.getZ());
      }

      return new Vec3i(var3, var4, var5);
   }

   @Override
   public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager param0, BlockPos param1, Rotation param2, RandomSource param3
   ) {
      return this.elements.get(0).getShuffledJigsawBlocks(param0, param1, param2, param3);
   }

   @Override
   public BoundingBox getBoundingBox(StructureTemplateManager param0, BlockPos param1, Rotation param2) {
      Stream<BoundingBox> var4 = this.elements
         .stream()
         .filter(l_param0 -> l_param0 != EmptyPoolElement.INSTANCE)
         .map(l_param3 -> l_param3.getBoundingBox(param0, param1, param2));
      return BoundingBox.encapsulatingBoxes(var4::iterator).orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox for ListPoolElement"));
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
      for (StructurePoolElement var12 : this.elements) {
         if (!var12.place(param0, param1, param2, param3, param4, param5, param6, param7, param8, param9)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.LIST;
   }

   @Override
   public StructurePoolElement setProjection(StructureTemplatePool.Projection param0) {
      super.setProjection(param0);
      this.setProjectionOnEachElement(param0);
      return this;
   }

   @Override
   public String toString() {
      return "List[" + this.elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
   }

   private void setProjectionOnEachElement(StructureTemplatePool.Projection param0) {
      this.elements.forEach(l_param1 -> l_param1.setProjection(param0));
   }
}
