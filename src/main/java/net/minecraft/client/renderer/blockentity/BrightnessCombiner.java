package net.minecraft.client.renderer.blockentity;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BrightnessCombiner<S extends BlockEntity> implements DoubleBlockCombiner.Combiner<S, Int2IntFunction> {
   public Int2IntFunction acceptDouble(S param0, S param1) {
      return l_param2 -> {
         int l_var3 = LevelRenderer.getLightColor(param0.getLevel(), param0.getBlockPos());
         int l_var4 = LevelRenderer.getLightColor(param1.getLevel(), param1.getBlockPos());
         int l_var5 = LightTexture.block(l_var3);
         int l_var6 = LightTexture.block(l_var4);
         int l_var7 = LightTexture.sky(l_var3);
         int l_var8 = LightTexture.sky(l_var4);
         return LightTexture.pack(Math.max(l_var5, l_var6), Math.max(l_var7, l_var8));
      };
   }

   public Int2IntFunction acceptSingle(S param0) {
      return l_param0 -> l_param0;
   }

   public Int2IntFunction acceptNone() {
      return l_param0 -> l_param0;
   }
}
