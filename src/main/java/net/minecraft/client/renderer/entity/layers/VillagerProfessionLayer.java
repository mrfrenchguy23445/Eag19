package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerHeadModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;

public class VillagerProfessionLayer<T extends LivingEntity & VillagerDataHolder, M extends EntityModel<T> & VillagerHeadModel> extends RenderLayer<T, M> {
   private static final Int2ObjectMap<ResourceLocation> LEVEL_LOCATIONS = Util.make(new Int2ObjectOpenHashMap<>(), param0 -> {
      param0.put(1, new ResourceLocation("stone"));
      param0.put(2, new ResourceLocation("iron"));
      param0.put(3, new ResourceLocation("gold"));
      param0.put(4, new ResourceLocation("emerald"));
      param0.put(5, new ResourceLocation("diamond"));
   });
   private final Object2ObjectMap<VillagerType, VillagerMetaDataSection.Hat> typeHatCache = new Object2ObjectOpenHashMap<>();
   private final Object2ObjectMap<VillagerProfession, VillagerMetaDataSection.Hat> professionHatCache = new Object2ObjectOpenHashMap<>();
   private final ResourceManager resourceManager;
   private final String path;

   public VillagerProfessionLayer(RenderLayerParent<T, M> param0, ResourceManager param1, String param2) {
      super(param0);
      this.resourceManager = param1;
      this.path = param2;
   }

   public void render(
      PoseStack param0, MultiBufferSource param1, int param2, T param3, float param4, float param5, float param6, float param7, float param8, float param9
   ) {
      if (!param3.isInvisible()) {
         VillagerData var11 = param3.getVillagerData();
         VillagerType var12 = var11.getType();
         VillagerProfession var13 = var11.getProfession();
         VillagerMetaDataSection.Hat var14 = this.getHatData(this.typeHatCache, "type", BuiltInRegistries.VILLAGER_TYPE, var12);
         VillagerMetaDataSection.Hat var15 = this.getHatData(this.professionHatCache, "profession", BuiltInRegistries.VILLAGER_PROFESSION, var13);
         M var16 = this.getParentModel();
         var16.hatVisible(
            var15 == VillagerMetaDataSection.Hat.NONE || var15 == VillagerMetaDataSection.Hat.PARTIAL && var14 != VillagerMetaDataSection.Hat.FULL
         );
         ResourceLocation var17 = this.getResourceLocation("type", BuiltInRegistries.VILLAGER_TYPE.getKey(var12));
         renderColoredCutoutModel(var16, var17, param0, param1, param2, param3, 1.0F, 1.0F, 1.0F);
         var16.hatVisible(true);
         if (var13 != VillagerProfession.NONE && !param3.isBaby()) {
            ResourceLocation var18 = this.getResourceLocation("profession", BuiltInRegistries.VILLAGER_PROFESSION.getKey(var13));
            renderColoredCutoutModel(var16, var18, param0, param1, param2, param3, 1.0F, 1.0F, 1.0F);
            if (var13 != VillagerProfession.NITWIT) {
               ResourceLocation var19 = this.getResourceLocation(
                  "profession_level", LEVEL_LOCATIONS.get(Mth.clamp(var11.getLevel(), 1, LEVEL_LOCATIONS.size()))
               );
               renderColoredCutoutModel(var16, var19, param0, param1, param2, param3, 1.0F, 1.0F, 1.0F);
            }
         }
      }
   }

   private ResourceLocation getResourceLocation(String param0, ResourceLocation param1) {
      return param1.withPath(l_param1 -> "textures/entity/" + this.path + "/" + param0 + "/" + l_param1 + ".png");
   }

   public <K> VillagerMetaDataSection.Hat getHatData(
      Object2ObjectMap<K, VillagerMetaDataSection.Hat> param0, String param1, DefaultedRegistry<K> param2, K param3
   ) {
      return param0.computeIfAbsent(
         param3, l_param3 -> this.resourceManager.getResource(this.getResourceLocation(param1, param2.getKey(param3))).flatMap(l2_param0 -> {
            try {
               return l2_param0.metadata().getSection(VillagerMetaDataSection.SERIALIZER).map(VillagerMetaDataSection::getHat);
            } catch (IOException l2_var1) {
               return Optional.empty();
            }
         }).orElse(VillagerMetaDataSection.Hat.NONE)
      );
   }
}
