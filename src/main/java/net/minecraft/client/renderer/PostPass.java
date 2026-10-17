package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;

public class PostPass implements AutoCloseable {
   private final EffectInstance effect;
   public final RenderTarget inTarget;
   public final RenderTarget outTarget;
   private final List<IntSupplier> auxAssets = Lists.newArrayList();
   private final List<String> auxNames = Lists.newArrayList();
   private final List<Integer> auxWidths = Lists.newArrayList();
   private final List<Integer> auxHeights = Lists.newArrayList();
   private Matrix4f shaderOrthoMatrix;

   public PostPass(ResourceManager param0, String param1, RenderTarget param2, RenderTarget param3) throws IOException {
      this.effect = new EffectInstance(param0, param1);
      this.inTarget = param2;
      this.outTarget = param3;
   }

   @Override
   public void close() {
      this.effect.close();
   }

   public final String getName() {
      return this.effect.getName();
   }

   public void addAuxAsset(String param0, IntSupplier param1, int param2, int param3) {
      this.auxNames.add(this.auxNames.size(), param0);
      this.auxAssets.add(this.auxAssets.size(), param1);
      this.auxWidths.add(this.auxWidths.size(), param2);
      this.auxHeights.add(this.auxHeights.size(), param3);
   }

   public void setOrthoMatrix(Matrix4f param0) {
      this.shaderOrthoMatrix = param0;
   }

   public void process(float param0) {
      this.inTarget.unbindWrite();
      float var2 = this.outTarget.width;
      float var3 = this.outTarget.height;
      RenderSystem.viewport(0, 0, (int)var2, (int)var3);
      this.effect.setSampler("DiffuseSampler", this.inTarget::getColorTextureId);

      for (int var4 = 0; var4 < this.auxAssets.size(); var4++) {
         this.effect.setSampler(this.auxNames.get(var4), this.auxAssets.get(var4));
         this.effect.safeGetUniform("AuxSize" + var4).set((float)this.auxWidths.get(var4).intValue(), (float)this.auxHeights.get(var4).intValue());
      }

      this.effect.safeGetUniform("ProjMat").set(this.shaderOrthoMatrix);
      this.effect.safeGetUniform("InSize").set((float)this.inTarget.width, (float)this.inTarget.height);
      this.effect.safeGetUniform("OutSize").set(var2, var3);
      this.effect.safeGetUniform("Time").set(param0);
      Minecraft var4 = Minecraft.getInstance();
      this.effect.safeGetUniform("ScreenSize").set((float)var4.getWindow().getWidth(), (float)var4.getWindow().getHeight());
      this.effect.apply();
      this.outTarget.clear(Minecraft.ON_OSX);
      this.outTarget.bindWrite(false);
      RenderSystem.depthFunc(519);
      BufferBuilder var5 = Tesselator.getInstance().getBuilder();
      var5.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
      var5.vertex(0.0, 0.0, 500.0).endVertex();
      var5.vertex(var2, 0.0, 500.0).endVertex();
      var5.vertex(var2, var3, 500.0).endVertex();
      var5.vertex(0.0, var3, 500.0).endVertex();
      BufferUploader.draw(var5.end());
      RenderSystem.depthFunc(515);
      this.effect.clear();
      this.outTarget.unbindWrite();
      this.inTarget.unbindRead();

      for (Object var7 : this.auxAssets) {
         if (var7 instanceof RenderTarget) {
            ((RenderTarget)var7).unbindRead();
         }
      }
   }

   public EffectInstance getEffect() {
      return this.effect;
   }
}
