package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class ShaderInstance implements Shader, AutoCloseable {
   public static final String SHADER_PATH = "shaders";
   private static final String SHADER_CORE_PATH = "shaders/core/";
   private static final String SHADER_INCLUDE_PATH = "shaders/include/";
   static final Logger LOGGER = LogUtils.getLogger();
   private static final AbstractUniform DUMMY_UNIFORM = new AbstractUniform();
   private static final boolean ALWAYS_REAPPLY = true;
   private static ShaderInstance lastAppliedShader;
   private static int lastProgramId = -1;
   private final Map<String, Object> samplerMap = Maps.newHashMap();
   private final List<String> samplerNames = Lists.newArrayList();
   private final List<Integer> samplerLocations = Lists.newArrayList();
   private final List<Uniform> uniforms = Lists.newArrayList();
   private final List<Integer> uniformLocations = Lists.newArrayList();
   private final Map<String, Uniform> uniformMap = Maps.newHashMap();
   private final int programId;
   private final String name;
   private boolean dirty;
   private final BlendMode blend;
   private final List<Integer> attributes;
   private final List<String> attributeNames;
   private final Program vertexProgram;
   private final Program fragmentProgram;
   private final VertexFormat vertexFormat;
   @Nullable
   public final Uniform MODEL_VIEW_MATRIX;
   @Nullable
   public final Uniform PROJECTION_MATRIX;
   @Nullable
   public final Uniform INVERSE_VIEW_ROTATION_MATRIX;
   @Nullable
   public final Uniform TEXTURE_MATRIX;
   @Nullable
   public final Uniform SCREEN_SIZE;
   @Nullable
   public final Uniform COLOR_MODULATOR;
   @Nullable
   public final Uniform LIGHT0_DIRECTION;
   @Nullable
   public final Uniform LIGHT1_DIRECTION;
   @Nullable
   public final Uniform GLINT_ALPHA;
   @Nullable
   public final Uniform FOG_START;
   @Nullable
   public final Uniform FOG_END;
   @Nullable
   public final Uniform FOG_COLOR;
   @Nullable
   public final Uniform FOG_SHAPE;
   @Nullable
   public final Uniform LINE_WIDTH;
   @Nullable
   public final Uniform GAME_TIME;
   @Nullable
   public final Uniform CHUNK_OFFSET;

   public ShaderInstance(ResourceProvider param0, String param1, VertexFormat param2) throws IOException {
      this.name = param1;
      this.vertexFormat = param2;
      ResourceLocation var4 = new ResourceLocation("shaders/core/" + param1 + ".json");

      try (Reader var6 = param0.openAsReader(var4)) {
         JsonObject var5 = GsonHelper.parse(var6);
         String var7 = GsonHelper.getAsString(var5, "vertex");
         String var8 = GsonHelper.getAsString(var5, "fragment");
         JsonArray var9 = GsonHelper.getAsJsonArray(var5, "samplers", null);
         if (var9 != null) {
            int var10 = 0;

            for (JsonElement var12 : var9) {
               try {
                  this.parseSamplerNode(var12);
               } catch (Exception var13) {
                  ChainedJsonException var14 = ChainedJsonException.forException(var13);
                  var14.prependJsonKey("samplers[" + var10 + "]");
                  throw var14;
               }

               var10++;
            }
         }

         JsonArray var10 = GsonHelper.getAsJsonArray(var5, "attributes", null);
         if (var10 != null) {
            int var11 = 0;
            this.attributes = Lists.newArrayListWithCapacity(var10.size());
            this.attributeNames = Lists.newArrayListWithCapacity(var10.size());

            for (JsonElement var13 : var10) {
               try {
                  this.attributeNames.add(GsonHelper.convertToString(var13, "attribute"));
               } catch (Exception var14) {
                  ChainedJsonException var15 = ChainedJsonException.forException(var14);
                  var15.prependJsonKey("attributes[" + var11 + "]");
                  throw var15;
               }

               var11++;
            }
         } else {
            this.attributes = null;
            this.attributeNames = null;
         }

         JsonArray var11 = GsonHelper.getAsJsonArray(var5, "uniforms", null);
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

         this.blend = parseBlendNode(GsonHelper.getAsJsonObject(var5, "blend", null));
         this.vertexProgram = getOrCreate(param0, Program.Type.VERTEX, var7);
         this.fragmentProgram = getOrCreate(param0, Program.Type.FRAGMENT, var8);
         this.programId = ProgramManager.createProgram();
         if (this.attributeNames != null) {
            int var12 = 0;

            for (String var14 : param2.getElementAttributeNames()) {
               Uniform.glBindAttribLocation(this.programId, var12, var14);
               this.attributes.add(var12);
               var12++;
            }
         }

         ProgramManager.linkShader(this);
         this.updateLocations();
      } catch (Exception var6) {
         ChainedJsonException var7 = ChainedJsonException.forException(var6);
         var7.setFilenameAndFlush(var4.getPath());
         throw var7;
      }

      this.markDirty();
      this.MODEL_VIEW_MATRIX = this.getUniform("ModelViewMat");
      this.PROJECTION_MATRIX = this.getUniform("ProjMat");
      this.INVERSE_VIEW_ROTATION_MATRIX = this.getUniform("IViewRotMat");
      this.TEXTURE_MATRIX = this.getUniform("TextureMat");
      this.SCREEN_SIZE = this.getUniform("ScreenSize");
      this.COLOR_MODULATOR = this.getUniform("ColorModulator");
      this.LIGHT0_DIRECTION = this.getUniform("Light0_Direction");
      this.LIGHT1_DIRECTION = this.getUniform("Light1_Direction");
      this.GLINT_ALPHA = this.getUniform("GlintAlpha");
      this.FOG_START = this.getUniform("FogStart");
      this.FOG_END = this.getUniform("FogEnd");
      this.FOG_COLOR = this.getUniform("FogColor");
      this.FOG_SHAPE = this.getUniform("FogShape");
      this.LINE_WIDTH = this.getUniform("LineWidth");
      this.GAME_TIME = this.getUniform("GameTime");
      this.CHUNK_OFFSET = this.getUniform("ChunkOffset");
   }

   private static Program getOrCreate(final ResourceProvider param0, Program.Type param1, String param2) throws IOException {
      Program var4 = param1.getPrograms().get(param2);
      Program var3;
      if (var4 == null) {
         String var5 = "shaders/core/" + param2 + param1.getExtension();
         Resource var6 = param0.getResourceOrThrow(new ResourceLocation(var5));

         try (InputStream var7 = var6.open()) {
            final String var8 = FileUtil.getFullResourcePath(var5);
            var3 = Program.compileShader(param1, param2, var7, var6.sourcePackId(), new GlslPreprocessor() {
               private final Set<String> importedPaths = Sets.newHashSet();

               @Override
               public String applyImport(boolean l_param0, String l_param1) {
                  l_param1 = FileUtil.normalizeResourcePath((l_param0 ? var8 : "shaders/include/") + l_param1);
                  if (!this.importedPaths.add(l_param1)) {
                     return null;
                  }

                  ResourceLocation l_var3 = new ResourceLocation(l_param1);

                  try (Reader l_var4 = param0.openAsReader(l_var3)) {
                     return IOUtils.toString(l_var4);
                  } catch (IOException l_var4) {
                     ShaderInstance.LOGGER.error("Could not open GLSL import {}: {}", l_param1, l_var4.getMessage());
                     return "#error " + l_var4.getMessage();
                  }
               }
            });
         }
      } else {
         var3 = var4;
      }

      return var3;
   }

   public static BlendMode parseBlendNode(JsonObject param0) {
      if (param0 == null) {
         return new BlendMode();
      }

      int var1 = 32774;
      int var2 = 1;
      int var3 = 0;
      int var4 = 1;
      int var5 = 0;
      boolean var6 = true;
      boolean var7 = false;
      if (GsonHelper.isStringValue(param0, "func")) {
         var1 = BlendMode.stringToBlendFunc(param0.get("func").getAsString());
         if (var1 != 32774) {
            var6 = false;
         }
      }

      if (GsonHelper.isStringValue(param0, "srcrgb")) {
         var2 = BlendMode.stringToBlendFactor(param0.get("srcrgb").getAsString());
         if (var2 != 1) {
            var6 = false;
         }
      }

      if (GsonHelper.isStringValue(param0, "dstrgb")) {
         var3 = BlendMode.stringToBlendFactor(param0.get("dstrgb").getAsString());
         if (var3 != 0) {
            var6 = false;
         }
      }

      if (GsonHelper.isStringValue(param0, "srcalpha")) {
         var4 = BlendMode.stringToBlendFactor(param0.get("srcalpha").getAsString());
         if (var4 != 1) {
            var6 = false;
         }

         var7 = true;
      }

      if (GsonHelper.isStringValue(param0, "dstalpha")) {
         var5 = BlendMode.stringToBlendFactor(param0.get("dstalpha").getAsString());
         if (var5 != 0) {
            var6 = false;
         }

         var7 = true;
      }

      if (var6) {
         return new BlendMode();
      } else {
         return var7 ? new BlendMode(var2, var3, var4, var5, var1) : new BlendMode(var2, var3, var1);
      }
   }

   @Override
   public void close() {
      for (Uniform var2 : this.uniforms) {
         var2.close();
      }

      ProgramManager.releaseProgram(this);
   }

   public void clear() {
      RenderSystem.assertOnRenderThread();
      ProgramManager.glUseProgram(0);
      lastProgramId = -1;
      lastAppliedShader = null;
      int var1 = GlStateManager._getActiveTexture();

      for (int var2 = 0; var2 < this.samplerLocations.size(); var2++) {
         if (this.samplerMap.get(this.samplerNames.get(var2)) != null) {
            GlStateManager._activeTexture(33984 + var2);
            GlStateManager._bindTexture(0);
         }
      }

      GlStateManager._activeTexture(var1);
   }

   public void apply() {
      RenderSystem.assertOnRenderThread();
      this.dirty = false;
      lastAppliedShader = this;
      this.blend.apply();
      if (this.programId != lastProgramId) {
         ProgramManager.glUseProgram(this.programId);
         lastProgramId = this.programId;
      }

      int var1 = GlStateManager._getActiveTexture();

      for (int var2 = 0; var2 < this.samplerLocations.size(); var2++) {
         String var3 = this.samplerNames.get(var2);
         if (this.samplerMap.get(var3) != null) {
            int var4 = Uniform.glGetUniformLocation(this.programId, var3);
            Uniform.uploadInteger(var4, var2);
            RenderSystem.activeTexture(33984 + var2);
            Object var5 = this.samplerMap.get(var3);
            int var6 = -1;
            if (var5 instanceof RenderTarget) {
               var6 = ((RenderTarget)var5).getColorTextureId();
            } else if (var5 instanceof AbstractTexture) {
               var6 = ((AbstractTexture)var5).getId();
            } else if (var5 instanceof Integer) {
               var6 = (Integer)var5;
            }

            if (var6 != -1) {
               RenderSystem.bindTexture(var6);
            }
         }
      }

      GlStateManager._activeTexture(var1);

      for (Uniform var3 : this.uniforms) {
         var3.upload();
      }
   }

   @Override
   public void markDirty() {
      this.dirty = true;
   }

   @Nullable
   public Uniform getUniform(String param0) {
      RenderSystem.assertOnRenderThread();
      return this.uniformMap.get(param0);
   }

   public AbstractUniform safeGetUniform(String param0) {
      RenderSystem.assertOnGameThread();
      Uniform var2 = this.getUniform(param0);
      return var2 == null ? DUMMY_UNIFORM : var2;
   }

   private void updateLocations() {
      RenderSystem.assertOnRenderThread();
      IntList var1 = new IntArrayList();

      for (int var2 = 0; var2 < this.samplerNames.size(); var2++) {
         String var3 = this.samplerNames.get(var2);
         int var4 = Uniform.glGetUniformLocation(this.programId, var3);
         if (var4 == -1) {
            LOGGER.warn("Shader {} could not find sampler named {} in the specified shader program.", this.name, var3);
            this.samplerMap.remove(var3);
            var1.add(var2);
         } else {
            this.samplerLocations.add(var4);
         }
      }

      for (int var2 = var1.size() - 1; var2 >= 0; var2--) {
         int var3 = var1.getInt(var2);
         this.samplerNames.remove(var3);
      }

      for (Uniform var3 : this.uniforms) {
         String var4 = var3.getName();
         int var5 = Uniform.glGetUniformLocation(this.programId, var4);
         if (var5 == -1) {
            LOGGER.warn("Shader {} could not find uniform named {} in the specified shader program.", this.name, var4);
         } else {
            this.uniformLocations.add(var5);
            var3.setLocation(var5);
            this.uniformMap.put(var4, var3);
         }
      }
   }

   private void parseSamplerNode(JsonElement param0) {
      JsonObject var2 = GsonHelper.convertToJsonObject(param0, "sampler");
      String var3 = GsonHelper.getAsString(var2, "name");
      if (!GsonHelper.isStringValue(var2, "file")) {
         this.samplerMap.put(var3, null);
         this.samplerNames.add(var3);
      } else {
         this.samplerNames.add(var3);
      }
   }

   public void setSampler(String param0, Object param1) {
      this.samplerMap.put(param0, param1);
      this.markDirty();
   }

   private void parseUniformNode(JsonElement param0) throws ChainedJsonException {
      JsonObject var2 = GsonHelper.convertToJsonObject(param0, "uniform");
      String var3 = GsonHelper.getAsString(var2, "name");
      int var4 = Uniform.getTypeFromString(GsonHelper.getAsString(var2, "type"));
      int var5 = GsonHelper.getAsInt(var2, "count");
      float[] var6 = new float[Math.max(var5, 16)];
      JsonArray var7 = GsonHelper.getAsJsonArray(var2, "values");
      if (var7.size() != var5 && var7.size() > 1) {
         throw new ChainedJsonException("Invalid amount of values specified (expected " + var5 + ", found " + var7.size() + ")");
      }

      int var8 = 0;

      for (JsonElement var10 : var7) {
         try {
            var6[var8] = GsonHelper.convertToFloat(var10, "value");
         } catch (Exception var11) {
            ChainedJsonException var12 = ChainedJsonException.forException(var11);
            var12.prependJsonKey("values[" + var8 + "]");
            throw var12;
         }

         var8++;
      }

      if (var5 > 1 && var7.size() == 1) {
         while (var8 < var5) {
            var6[var8] = var6[0];
            var8++;
         }
      }

      int var9 = var5 > 1 && var5 <= 4 && var4 < 8 ? var5 - 1 : 0;
      Uniform var10 = new Uniform(var3, var4 + var9, var5, this);
      if (var4 <= 3) {
         var10.setSafe((int)var6[0], (int)var6[1], (int)var6[2], (int)var6[3]);
      } else if (var4 <= 7) {
         var10.setSafe(var6[0], var6[1], var6[2], var6[3]);
      } else {
         var10.set(Arrays.copyOfRange(var6, 0, var5));
      }

      this.uniforms.add(var10);
   }

   @Override
   public Program getVertexProgram() {
      return this.vertexProgram;
   }

   @Override
   public Program getFragmentProgram() {
      return this.fragmentProgram;
   }

   @Override
   public void attachToProgram() {
      this.fragmentProgram.attachToShader(this);
      this.vertexProgram.attachToShader(this);
   }

   public VertexFormat getVertexFormat() {
      return this.vertexFormat;
   }

   public String getName() {
      return this.name;
   }

   @Override
   public int getId() {
      return this.programId;
   }
}
