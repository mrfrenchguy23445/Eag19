package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public class FossilFeatureConfiguration implements FeatureConfiguration {
   public static final Codec<FossilFeatureConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            ResourceLocation.CODEC.listOf().fieldOf("fossil_structures").forGetter(l_param0 -> l_param0.fossilStructures),
            ResourceLocation.CODEC.listOf().fieldOf("overlay_structures").forGetter(l_param0 -> l_param0.overlayStructures),
            StructureProcessorType.LIST_CODEC.fieldOf("fossil_processors").forGetter(l_param0 -> l_param0.fossilProcessors),
            StructureProcessorType.LIST_CODEC.fieldOf("overlay_processors").forGetter(l_param0 -> l_param0.overlayProcessors),
            Codec.intRange(0, 7).fieldOf("max_empty_corners_allowed").forGetter(l_param0 -> l_param0.maxEmptyCornersAllowed)
         )
         .apply(param0, FossilFeatureConfiguration::new)
   );
   public final List<ResourceLocation> fossilStructures;
   public final List<ResourceLocation> overlayStructures;
   public final Holder<StructureProcessorList> fossilProcessors;
   public final Holder<StructureProcessorList> overlayProcessors;
   public final int maxEmptyCornersAllowed;

   public FossilFeatureConfiguration(
      List<ResourceLocation> param0, List<ResourceLocation> param1, Holder<StructureProcessorList> param2, Holder<StructureProcessorList> param3, int param4
   ) {
      if (param0.isEmpty()) {
         throw new IllegalArgumentException("Fossil structure lists need at least one entry");
      }

      if (param0.size() != param1.size()) {
         throw new IllegalArgumentException("Fossil structure lists must be equal lengths");
      }

      this.fossilStructures = param0;
      this.overlayStructures = param1;
      this.fossilProcessors = param2;
      this.overlayProcessors = param3;
      this.maxEmptyCornersAllowed = param4;
   }
}
