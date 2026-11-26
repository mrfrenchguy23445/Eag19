package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.shaders.Effect;
import com.mojang.blaze3d.shaders.EffectProgram;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public class EffectInstance implements Effect, AutoCloseable {
   private static final String EFFECT_SHADER_PATH = "shaders/program/";
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final AbstractUniform DUMMY_UNIFORM = new AbstractUniform();
   private static final boolean ALWAYS_REAPPLY = true;
   private static EffectInstance lastAppliedEffect;
   private static int lastProgramId = -1;
   private final Map<String, IntSupplier> samplerMap = Maps.newHashMap();
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
   private final EffectProgram vertexProgram;
   private final EffectProgram fragmentProgram;

   public EffectInstance(ResourceManager param0, String param1) throws IOException {
      ResourceLocation var3 = new ResourceLocation("shaders/program/" + param1 + ".json");
      this.name = param1;
      Resource var4 = param0.getResourceOrThrow(var3);

      try (Reader var5 = var4.openAsReader()) {
         JsonObject var6 = GsonHelper.parse(var5);
         String var7 = GsonHelper.getAsString(var6, "vertex");
         String var8 = GsonHelper.getAsString(var6, "fragment");
         JsonArray var9 = GsonHelper.getAsJsonArray(var6, "samplers", null);
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

         JsonArray var10 = GsonHelper.getAsJsonArray(var6, "attributes", null);
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

         JsonArray var11 = GsonHelper.getAsJsonArray(var6, "uniforms", null);
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

         this.blend = parseBlendNode(GsonHelper.getAsJsonObject(var6, "blend", null));
         this.vertexProgram = getOrCreate(param0, Program.Type.VERTEX, var7);
         this.fragmentProgram = getOrCreate(param0, Program.Type.FRAGMENT, var8);
         this.programId = ProgramManager.createProgram();
         ProgramManager.linkShader(this);
         this.updateLocations();
         if (this.attributeNames != null) {
            for (String var13 : this.attributeNames) {
               int var14 = Uniform.glGetAttribLocation(this.programId, var13);
               this.attributes.add(var14);
            }
         }
      } catch (Exception var5) {
         ChainedJsonException var6 = ChainedJsonException.forException(var5);
         var6.setFilenameAndFlush(var3.getPath() + " (" + var4.sourcePackId() + ")");
         throw var6;
      }

      this.markDirty();
   }

   public static EffectProgram getOrCreate(ResourceManager param0, Program.Type param1, String param2) throws IOException {
      Program var3 = param1.getPrograms().get(param2);
      if (var3 != null && !(var3 instanceof EffectProgram)) {
         throw new InvalidClassException("Program is not of type EffectProgram");
      }

      EffectProgram var4;
      if (var3 == null) {
         ResourceLocation var5 = new ResourceLocation("shaders/program/" + param2 + param1.getExtension());
         Resource var6 = param0.getResourceOrThrow(var5);

         try (InputStream var7 = var6.open()) {
            var4 = EffectProgram.compileShader(param1, param2, var7, var6.sourcePackId());
         }
      } else {
         var4 = (EffectProgram)var3;
      }

      return var4;
   }

   public static BlendMode parseBlendNode(@Nullable JsonObject param0) {
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
      lastAppliedEffect = null;

      for (int var1 = 0; var1 < this.samplerLocations.size(); var1++) {
         if (this.samplerMap.get(this.samplerNames.get(var1)) != null) {
            GlStateManager._activeTexture(33984 + var1);
            GlStateManager._bindTexture(0);
         }
      }
   }

   public void apply() {
      RenderSystem.assertOnGameThread();
      this.dirty = false;
      lastAppliedEffect = this;
      this.blend.apply();
      if (this.programId != lastProgramId) {
         ProgramManager.glUseProgram(this.programId);
         lastProgramId = this.programId;
      }

      for (int var1 = 0; var1 < this.samplerLocations.size(); var1++) {
         String var2 = this.samplerNames.get(var1);
         IntSupplier var3 = this.samplerMap.get(var2);
         if (var3 != null) {
            RenderSystem.activeTexture(33984 + var1);
            int var4 = var3.getAsInt();
            if (var4 != -1) {
               RenderSystem.bindTexture(var4);
               Uniform.uploadInteger(this.samplerLocations.get(var1), var1);
            }
         }
      }

      for (Uniform var2 : this.uniforms) {
         var2.upload();
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
         this.samplerNames.remove(var1.getInt(var2));
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

   public void setSampler(String param0, IntSupplier param1) {
      if (this.samplerMap.containsKey(param0)) {
         this.samplerMap.remove(param0);
      }

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
         var10.set(var6);
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
      this.fragmentProgram.attachToEffect(this);
      this.vertexProgram.attachToEffect(this);
   }

   public String getName() {
      return this.name;
   }

   @Override
   public int getId() {
      return this.programId;
   }
}
