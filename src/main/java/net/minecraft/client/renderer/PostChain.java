package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.joml.Matrix4f;

public class PostChain implements AutoCloseable {
   private static final String MAIN_RENDER_TARGET = "minecraft:main";
   private final RenderTarget screenTarget;
   private final ResourceManager resourceManager;
   private final String name;
   private final List<PostPass> passes = Lists.newArrayList();
   private final Map<String, RenderTarget> customRenderTargets = Maps.newHashMap();
   private final List<RenderTarget> fullSizedTargets = Lists.newArrayList();
   private Matrix4f shaderOrthoMatrix;
   private int screenWidth;
   private int screenHeight;
   private float time;
   private float lastStamp;

   public PostChain(TextureManager param0, ResourceManager param1, RenderTarget param2, ResourceLocation param3) throws IOException, JsonSyntaxException {
      this.resourceManager = param1;
      this.screenTarget = param2;
      this.time = 0.0F;
      this.lastStamp = 0.0F;
      this.screenWidth = param2.viewWidth;
      this.screenHeight = param2.viewHeight;
      this.name = param3.toString();
      this.updateOrthoMatrix();
      this.load(param0, param3);
   }

   private void load(TextureManager param0, ResourceLocation param1) throws IOException, JsonSyntaxException {
      Resource var3 = this.resourceManager.getResourceOrThrow(param1);

      try (Reader var4 = var3.openAsReader()) {
         JsonObject var5 = GsonHelper.parse(var4);
         if (GsonHelper.isArrayNode(var5, "targets")) {
            JsonArray var6 = var5.getAsJsonArray("targets");
            int var7 = 0;

            for (JsonElement var9 : var6) {
               try {
                  this.parseTargetNode(var9);
               } catch (Exception var10) {
                  ChainedJsonException var11 = ChainedJsonException.forException(var10);
                  var11.prependJsonKey("targets[" + var7 + "]");
                  throw var11;
               }

               var7++;
            }
         }

         if (GsonHelper.isArrayNode(var5, "passes")) {
            JsonArray var6 = var5.getAsJsonArray("passes");
            int var7 = 0;

            for (JsonElement var9 : var6) {
               try {
                  this.parsePassNode(param0, var9);
               } catch (Exception var10) {
                  ChainedJsonException var11 = ChainedJsonException.forException(var10);
                  var11.prependJsonKey("passes[" + var7 + "]");
                  throw var11;
               }

               var7++;
            }
         }
      } catch (Exception var4) {
         ChainedJsonException var5 = ChainedJsonException.forException(var4);
         var5.setFilenameAndFlush(param1.getPath() + " (" + var3.sourcePackId() + ")");
         throw var5;
      }
   }

   private void parseTargetNode(JsonElement param0) throws ChainedJsonException {
      if (GsonHelper.isStringValue(param0)) {
         this.addTempTarget(param0.getAsString(), this.screenWidth, this.screenHeight);
      } else {
         JsonObject var2 = GsonHelper.convertToJsonObject(param0, "target");
         String var3 = GsonHelper.getAsString(var2, "name");
         int var4 = GsonHelper.getAsInt(var2, "width", this.screenWidth);
         int var5 = GsonHelper.getAsInt(var2, "height", this.screenHeight);
         if (this.customRenderTargets.containsKey(var3)) {
            throw new ChainedJsonException(var3 + " is already defined");
         }

         this.addTempTarget(var3, var4, var5);
      }
   }

   private void parsePassNode(TextureManager param0, JsonElement param1) throws IOException {
      JsonObject var3 = GsonHelper.convertToJsonObject(param1, "pass");
      String var4 = GsonHelper.getAsString(var3, "name");
      String var5 = GsonHelper.getAsString(var3, "intarget");
      String var6 = GsonHelper.getAsString(var3, "outtarget");
      RenderTarget var7 = this.getRenderTarget(var5);
      RenderTarget var8 = this.getRenderTarget(var6);
      if (var7 == null) {
         throw new ChainedJsonException("Input target '" + var5 + "' does not exist");
      }

      if (var8 == null) {
         throw new ChainedJsonException("Output target '" + var6 + "' does not exist");
      }

      PostPass var9 = this.addPass(var4, var7, var8);
      JsonArray var10 = GsonHelper.getAsJsonArray(var3, "auxtargets", null);
      if (var10 != null) {
         int var11 = 0;

         for (JsonElement var13 : var10) {
            try {
               JsonObject var14 = GsonHelper.convertToJsonObject(var13, "auxtarget");
               String var15 = GsonHelper.getAsString(var14, "name");
               String var16 = GsonHelper.getAsString(var14, "id");
               boolean var17;
               String var18;
               if (var16.endsWith(":depth")) {
                  var17 = true;
                  var18 = var16.substring(0, var16.lastIndexOf(58));
               } else {
                  var17 = false;
                  var18 = var16;
               }

               RenderTarget var19 = this.getRenderTarget(var18);
               if (var19 == null) {
                  if (var17) {
                     throw new ChainedJsonException("Render target '" + var18 + "' can't be used as depth buffer");
                  }

                  ResourceLocation var20 = new ResourceLocation("textures/effect/" + var18 + ".png");
                  this.resourceManager
                     .getResource(var20)
                     .orElseThrow(() -> new ChainedJsonException("Render target or texture '" + var18 + "' does not exist"));
                  RenderSystem.setShaderTexture(0, var20);
                  param0.bindForSetup(var20);
                  AbstractTexture var21 = param0.getTexture(var20);
                  int var22 = GsonHelper.getAsInt(var14, "width");
                  int var23 = GsonHelper.getAsInt(var14, "height");
                  boolean var24 = GsonHelper.getAsBoolean(var14, "bilinear");
                  if (var24) {
                     RenderSystem.texParameter(3553, 10241, 9729);
                     RenderSystem.texParameter(3553, 10240, 9729);
                  } else {
                     RenderSystem.texParameter(3553, 10241, 9728);
                     RenderSystem.texParameter(3553, 10240, 9728);
                  }

                  var9.addAuxAsset(var15, var21::getId, var22, var23);
               } else if (var17) {
                  var9.addAuxAsset(var15, var19::getDepthTextureId, var19.width, var19.height);
               } else {
                  var9.addAuxAsset(var15, var19::getColorTextureId, var19.width, var19.height);
               }
            } catch (Exception var14) {
               ChainedJsonException var15 = ChainedJsonException.forException(var14);
               var15.prependJsonKey("auxtargets[" + var11 + "]");
               throw var15;
            }

            var11++;
         }
      }

      JsonArray var11 = GsonHelper.getAsJsonArray(var3, "uniforms", null);
      if (var11 != null) {
         int var12 = 0;

         for (JsonElement var14 : var11) {
            try {
               this.parseUniformNode(var14);
            } catch (Exception var15) {
               ChainedJsonException var16 = ChainedJsonException.forException(var15);
               var16.prependJsonKey("uniforms[" + var12 + "]");
               throw var16;
            }

            var12++;
         }
      }
   }

   private void parseUniformNode(JsonElement param0) throws ChainedJsonException {
      JsonObject var2 = GsonHelper.convertToJsonObject(param0, "uniform");
      String var3 = GsonHelper.getAsString(var2, "name");
      Uniform var4 = this.passes.get(this.passes.size() - 1).getEffect().getUniform(var3);
      if (var4 == null) {
         throw new ChainedJsonException("Uniform '" + var3 + "' does not exist");
      }

      float[] var5 = new float[4];
      int var6 = 0;

      for (JsonElement var9 : GsonHelper.getAsJsonArray(var2, "values")) {
         try {
            var5[var6] = GsonHelper.convertToFloat(var9, "value");
         } catch (Exception var10) {
            ChainedJsonException var11 = ChainedJsonException.forException(var10);
            var11.prependJsonKey("values[" + var6 + "]");
            throw var11;
         }

         var6++;
      }

      switch (var6) {
         case 0:
         default:
            break;
         case 1:
            var4.set(var5[0]);
            break;
         case 2:
            var4.set(var5[0], var5[1]);
            break;
         case 3:
            var4.set(var5[0], var5[1], var5[2]);
            break;
         case 4:
            var4.set(var5[0], var5[1], var5[2], var5[3]);
      }
   }

   public RenderTarget getTempTarget(String param0) {
      return this.customRenderTargets.get(param0);
   }

   public void addTempTarget(String param0, int param1, int param2) {
      RenderTarget var4 = new TextureTarget(param1, param2, true, Minecraft.ON_OSX);
      var4.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.customRenderTargets.put(param0, var4);
      if (param1 == this.screenWidth && param2 == this.screenHeight) {
         this.fullSizedTargets.add(var4);
      }
   }

   @Override
   public void close() {
      for (RenderTarget var2 : this.customRenderTargets.values()) {
         var2.destroyBuffers();
      }

      for (PostPass var2 : this.passes) {
         var2.close();
      }

      this.passes.clear();
   }

   public PostPass addPass(String param0, RenderTarget param1, RenderTarget param2) throws IOException {
      PostPass var4 = new PostPass(this.resourceManager, param0, param1, param2);
      this.passes.add(this.passes.size(), var4);
      return var4;
   }

   private void updateOrthoMatrix() {
      this.shaderOrthoMatrix = new Matrix4f().setOrtho(0.0F, this.screenTarget.width, 0.0F, this.screenTarget.height, 0.1F, 1000.0F);
   }

   public void resize(int param0, int param1) {
      this.screenWidth = this.screenTarget.width;
      this.screenHeight = this.screenTarget.height;
      this.updateOrthoMatrix();

      for (PostPass var4 : this.passes) {
         var4.setOrthoMatrix(this.shaderOrthoMatrix);
      }

      for (RenderTarget var4 : this.fullSizedTargets) {
         var4.resize(param0, param1, Minecraft.ON_OSX);
      }
   }

   public void process(float param0) {
      if (param0 < this.lastStamp) {
         this.time = this.time + (1.0F - this.lastStamp);
         this.time += param0;
      } else {
         this.time = this.time + (param0 - this.lastStamp);
      }

      this.lastStamp = param0;

      while (this.time > 20.0F) {
         this.time -= 20.0F;
      }

      for (PostPass var3 : this.passes) {
         var3.process(this.time / 20.0F);
      }
   }

   public final String getName() {
      return this.name;
   }

   @Nullable
   private RenderTarget getRenderTarget(@Nullable String param0) {
      if (param0 == null) {
         return null;
      } else {
         return param0.equals("minecraft:main") ? this.screenTarget : this.customRenderTargets.get(param0);
      }
   }
}
