package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public class PartDefinition {
   private final List<CubeDefinition> cubes;
   private final PartPose partPose;
   private final Map<String, PartDefinition> children = Maps.newHashMap();

   PartDefinition(List<CubeDefinition> param0, PartPose param1) {
      this.cubes = param0;
      this.partPose = param1;
   }

   public PartDefinition addOrReplaceChild(String param0, CubeListBuilder param1, PartPose param2) {
      PartDefinition var4 = new PartDefinition(param1.getCubes(), param2);
      PartDefinition var5 = this.children.put(param0, var4);
      if (var5 != null) {
         var4.children.putAll(var5.children);
      }

      return var4;
   }

   public ModelPart bake(int param0, int param1) {
      Object2ObjectArrayMap<String, ModelPart> var3 = this.children
         .entrySet()
         .stream()
         .collect(
            Collectors.toMap(
               Entry::getKey,
               l_param2 -> ((PartDefinition)l_param2.getValue()).bake(param0, param1),
               (l_param0, l_param1) -> l_param0,
               Object2ObjectArrayMap::new
            )
         );
      List<ModelPart.Cube> var4 = this.cubes.stream().map(l_param2 -> l_param2.bake(param0, param1)).collect(ImmutableList.toImmutableList());
      ModelPart var5 = new ModelPart(var4, var3);
      var5.setInitialPose(this.partPose);
      var5.loadPose(this.partPose);
      return var5;
   }

   public PartDefinition getChild(String param0) {
      return this.children.get(param0);
   }
}
