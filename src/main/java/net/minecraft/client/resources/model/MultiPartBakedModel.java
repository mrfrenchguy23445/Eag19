package net.minecraft.client.resources.model;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

public class MultiPartBakedModel implements BakedModel {
   private final List<Pair<Predicate<BlockState>, BakedModel>> selectors;
   protected final boolean hasAmbientOcclusion;
   protected final boolean isGui3d;
   protected final boolean usesBlockLight;
   protected final TextureAtlasSprite particleIcon;
   protected final ItemTransforms transforms;
   protected final ItemOverrides overrides;
   private final Map<BlockState, BitSet> selectorCache = new Object2ObjectOpenCustomHashMap<>(Util.identityStrategy());

   public MultiPartBakedModel(List<Pair<Predicate<BlockState>, BakedModel>> param0) {
      this.selectors = param0;
      BakedModel var2 = param0.iterator().next().getRight();
      this.hasAmbientOcclusion = var2.useAmbientOcclusion();
      this.isGui3d = var2.isGui3d();
      this.usesBlockLight = var2.usesBlockLight();
      this.particleIcon = var2.getParticleIcon();
      this.transforms = var2.getTransforms();
      this.overrides = var2.getOverrides();
   }

   @Override
   public List<BakedQuad> getQuads(@Nullable BlockState param0, @Nullable Direction param1, RandomSource param2) {
      if (param0 == null) {
         return Collections.emptyList();
      }

      BitSet var4 = this.selectorCache.get(param0);
      if (var4 == null) {
         var4 = new BitSet();

         for (int var5 = 0; var5 < this.selectors.size(); var5++) {
            Pair<Predicate<BlockState>, BakedModel> var6 = this.selectors.get(var5);
            if (var6.getLeft().test(param0)) {
               var4.set(var5);
            }
         }

         this.selectorCache.put(param0, var4);
      }

      List<BakedQuad> var5 = Lists.newArrayList();
      long var6 = param2.nextLong();

      for (int var8 = 0; var8 < var4.length(); var8++) {
         if (var4.get(var8)) {
            var5.addAll(this.selectors.get(var8).getRight().getQuads(param0, param1, RandomSource.create(var6)));
         }
      }

      return var5;
   }

   @Override
   public boolean useAmbientOcclusion() {
      return this.hasAmbientOcclusion;
   }

   @Override
   public boolean isGui3d() {
      return this.isGui3d;
   }

   @Override
   public boolean usesBlockLight() {
      return this.usesBlockLight;
   }

   @Override
   public boolean isCustomRenderer() {
      return false;
   }

   @Override
   public TextureAtlasSprite getParticleIcon() {
      return this.particleIcon;
   }

   @Override
   public ItemTransforms getTransforms() {
      return this.transforms;
   }

   @Override
   public ItemOverrides getOverrides() {
      return this.overrides;
   }

   public static class Builder {
      private final List<Pair<Predicate<BlockState>, BakedModel>> selectors = Lists.newArrayList();

      public void add(Predicate<BlockState> param0, BakedModel param1) {
         this.selectors.add(Pair.of(param0, param1));
      }

      public BakedModel build() {
         return new MultiPartBakedModel(this.selectors);
      }
   }
}
