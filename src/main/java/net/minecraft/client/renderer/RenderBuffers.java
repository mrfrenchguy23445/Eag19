package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.SortedMap;
import net.minecraft.Util;
import net.minecraft.client.resources.model.ModelBakery;

public class RenderBuffers {
   private final ChunkBufferBuilderPack fixedBufferPack = new ChunkBufferBuilderPack();
   private final SortedMap<RenderType, BufferBuilder> fixedBuffers = Util.make(new Object2ObjectLinkedOpenHashMap<>(), l_param0 -> {
      l_param0.put(Sheets.solidBlockSheet(), this.fixedBufferPack.builder(RenderType.solid()));
      l_param0.put(Sheets.cutoutBlockSheet(), this.fixedBufferPack.builder(RenderType.cutout()));
      l_param0.put(Sheets.bannerSheet(), this.fixedBufferPack.builder(RenderType.cutoutMipped()));
      l_param0.put(Sheets.translucentCullBlockSheet(), this.fixedBufferPack.builder(RenderType.translucent()));
      put(l_param0, Sheets.shieldSheet());
      put(l_param0, Sheets.bedSheet());
      put(l_param0, Sheets.shulkerBoxSheet());
      put(l_param0, Sheets.signSheet());
      put(l_param0, Sheets.hangingSignSheet());
      put(l_param0, Sheets.chestSheet());
      put(l_param0, RenderType.translucentNoCrumbling());
      put(l_param0, RenderType.armorGlint());
      put(l_param0, RenderType.armorEntityGlint());
      put(l_param0, RenderType.glint());
      put(l_param0, RenderType.glintDirect());
      put(l_param0, RenderType.glintTranslucent());
      put(l_param0, RenderType.entityGlint());
      put(l_param0, RenderType.entityGlintDirect());
      put(l_param0, RenderType.waterMask());
      ModelBakery.DESTROY_TYPES.forEach(l2_param1 -> put(l_param0, l2_param1));
   });
   private final MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediateWithBuffers(this.fixedBuffers, new BufferBuilder(256));
   private final MultiBufferSource.BufferSource crumblingBufferSource = MultiBufferSource.immediate(new BufferBuilder(256));
   private final OutlineBufferSource outlineBufferSource = new OutlineBufferSource(this.bufferSource);

   private static void put(Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> param0, RenderType param1) {
      param0.put(param1, new BufferBuilder(param1.bufferSize()));
   }

   public ChunkBufferBuilderPack fixedBufferPack() {
      return this.fixedBufferPack;
   }

   public MultiBufferSource.BufferSource bufferSource() {
      return this.bufferSource;
   }

   public MultiBufferSource.BufferSource crumblingBufferSource() {
      return this.crumblingBufferSource;
   }

   public OutlineBufferSource outlineBufferSource() {
      return this.outlineBufferSource;
   }
}
