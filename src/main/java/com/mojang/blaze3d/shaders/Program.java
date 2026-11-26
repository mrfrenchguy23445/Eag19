package com.mojang.blaze3d.shaders;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class Program {
   private static final int MAX_LOG_LENGTH = 32768;
   private final Program.Type type;
   private final String name;
   private int id;

   protected Program(Program.Type param0, int param1, String param2) {
      this.type = param0;
      this.id = param1;
      this.name = param2;
   }

   public void attachToShader(Shader param0) {
      RenderSystem.assertOnRenderThread();
      GlStateManager.glAttachShader(param0.getId(), this.getId());
   }

   public void close() {
      if (this.id != -1) {
         RenderSystem.assertOnRenderThread();
         GlStateManager.glDeleteShader(this.id);
         this.id = -1;
         this.type.getPrograms().remove(this.name);
      }
   }

   public String getName() {
      return this.name;
   }

   public static Program compileShader(Program.Type param0, String param1, InputStream param2, String param3, GlslPreprocessor param4) throws IOException {
      RenderSystem.assertOnRenderThread();
      int var5 = compileShaderInternal(param0, param1, param2, param3, param4);
      Program var6 = new Program(param0, var5, param1);
      param0.getPrograms().put(param1, var6);
      return var6;
   }

   protected static int compileShaderInternal(Program.Type param0, String param1, InputStream param2, String param3, GlslPreprocessor param4) throws IOException {
      String var5 = readFullyAsString(param2);
      if (var5 == null) {
         throw new IOException("Could not load program " + param0.getName());
      } else {
         int var6 = GlStateManager.glCreateShader(param0.getGlType());
         GlStateManager.glShaderSource(var6, param4.process(var5));
         GlStateManager.glCompileShader(var6);
         if (GlStateManager.glGetShaderi(var6, 35713) == 0) {
            String var7 = StringUtils.trim(GlStateManager.glGetShaderInfoLog(var6, 32768));
            throw new IOException("Couldn't compile " + param0.getName() + " program (" + param3 + ", " + param1 + ") : " + var7);
         } else {
            return var6;
         }
      }
   }

   /**
    * Port note: this replaces org.apache.commons.io.IOUtils.toString(InputStream, Charset),
    * the last commons-io call on the boot path and one that could not link.
    *
    * IOUtils.toString routes through IOUtils.copy, whose first act is to touch
    * org.apache.commons.io.Charsets, and Charsets' static initializer builds an immutable
    * map with Collections.unmodifiableSortedMap. The classlib's Collections has no such
    * method:
    *
    *   Method java.util.Collections.unmodifiableSortedMap(Ljava/util/SortedMap;)Ljava/util/SortedMap; was not found
    *       at org.apache.commons.io.Charsets.<clinit>(Charsets.java:73)
    *       at org.apache.commons.io.IOUtils.copy(IOUtils.java:1158)
    *       at com.mojang.blaze3d.shaders.Program.compileShaderInternal(Program.java:53)
    *       at net.minecraft.client.renderer.GameRenderer.preloadUiShader(GameRenderer.java:419)
    *       at net.minecraft.client.Minecraft.<init>(Minecraft.java:564)
    *
    * The other game files reaching IOUtils were routed around it earlier; this one runs
    * before the first frame of the very first boot and could not be. Semantics are
    * unchanged: read the stream whole, decode it as UTF-8, return a String. readAllBytes()
    * is a Java 9 method the classlib does not model either, hence the hand-rolled loop.
    */
   private static String readFullyAsString(InputStream in) throws IOException {
      java.io.ByteArrayOutputStream var1 = new java.io.ByteArrayOutputStream();
      byte[] var2 = new byte[8192];
      int var3;
      while ((var3 = in.read(var2)) != -1) {
         var1.write(var2, 0, var3);
      }
      return new String(var1.toByteArray(), StandardCharsets.UTF_8);
   }

   protected int getId() {
      return this.id;
   }

   public enum Type {
      VERTEX("vertex", ".vsh", 35633),
      FRAGMENT("fragment", ".fsh", 35632);

      private final String name;
      private final String extension;
      private final int glType;
      private final Map<String, Program> programs = Maps.newHashMap();

      Type(String param2, String param3, int param4) {
         this.name = param2;
         this.extension = param3;
         this.glType = param4;
      }

      public String getName() {
         return this.name;
      }

      public String getExtension() {
         return this.extension;
      }

      int getGlType() {
         return this.glType;
      }

      public Map<String, Program> getPrograms() {
         return this.programs;
      }
   }
}
