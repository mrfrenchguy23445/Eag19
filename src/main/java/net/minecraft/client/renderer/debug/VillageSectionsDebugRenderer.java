package net.minecraft.client.renderer.debug;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public class VillageSectionsDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST_FOR_VILLAGE_SECTIONS = 60;
   private final Set<SectionPos> villageSections = Sets.newHashSet();

   VillageSectionsDebugRenderer() {
   }

   @Override
   public void clear() {
      this.villageSections.clear();
   }

   public void setVillageSection(SectionPos param0) {
      this.villageSections.add(param0);
   }

   public void setNotVillageSection(SectionPos param0) {
      this.villageSections.remove(param0);
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      BlockPos var9 = BlockPos.containing(param2, param4, param6);
      this.villageSections.forEach(l_param3 -> {
         if (var9.closerThan(l_param3.center(), 60.0)) {
            highlightVillageSection(param0, param1, l_param3);
         }
      });
   }

   private static void highlightVillageSection(PoseStack param0, MultiBufferSource param1, SectionPos param2) {
      int var3 = 1;
      BlockPos var4 = param2.center();
      BlockPos var5 = var4.offset(-1, -1, -1);
      BlockPos var6 = var4.offset(1, 1, 1);
      DebugRenderer.renderFilledBox(param0, param1, var5, var6, 0.2F, 1.0F, 0.2F, 0.15F);
   }
}
