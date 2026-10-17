package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;

public class EffectProgram extends Program {
   private static final GlslPreprocessor PREPROCESSOR = new GlslPreprocessor() {
      @Override
      public String applyImport(boolean param0, String param1) {
         return "#error Import statement not supported";
      }
   };
   private int references;

   private EffectProgram(Program.Type param0, int param1, String param2) {
      super(param0, param1, param2);
   }

   public void attachToEffect(Effect param0) {
      RenderSystem.assertOnRenderThread();
      this.references++;
      this.attachToShader(param0);
   }

   @Override
   public void close() {
      RenderSystem.assertOnRenderThread();
      this.references--;
      if (this.references <= 0) {
         super.close();
      }
   }

   public static EffectProgram compileShader(Program.Type param0, String param1, InputStream param2, String param3) throws IOException {
      RenderSystem.assertOnRenderThread();
      int var4 = compileShaderInternal(param0, param1, param2, param3, PREPROCESSOR);
      EffectProgram var5 = new EffectProgram(param0, var4, param1);
      param0.getPrograms().put(param1, var5);
      return var5;
   }
}
