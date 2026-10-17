package net.minecraft.client.renderer.texture.atlas.sources;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import org.slf4j.Logger;

public class PalettedPermutations implements SpriteSource {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<PalettedPermutations> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.list(ResourceLocation.CODEC).fieldOf("textures").forGetter(l_param0 -> l_param0.textures),
            ResourceLocation.CODEC.fieldOf("palette_key").forGetter(l_param0 -> l_param0.paletteKey),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).fieldOf("permutations").forGetter(l_param0 -> l_param0.permutations)
         )
         .apply(param0, PalettedPermutations::new)
   );
   private final List<ResourceLocation> textures;
   private final Map<String, ResourceLocation> permutations;
   private final ResourceLocation paletteKey;

   private PalettedPermutations(List<ResourceLocation> param0, ResourceLocation param1, Map<String, ResourceLocation> param2) {
      this.textures = param0;
      this.permutations = param2;
      this.paletteKey = param1;
   }

   @Override
   public void run(ResourceManager param0, SpriteSource.Output param1) {
      Supplier<int[]> var3 = Suppliers.<int[]>memoize(() -> loadPaletteEntryFromImage(param0, this.paletteKey));
      Map<String, Supplier<IntUnaryOperator>> var4 = new HashMap<>();
      this.permutations
         .forEach(
            (l_param3, l_param4) -> var4.put(
               l_param3, Suppliers.<IntUnaryOperator>memoize(() -> createPaletteMapping(var3.get(), loadPaletteEntryFromImage(param0, l_param4)))
            )
         );

      for (ResourceLocation var6 : this.textures) {
         ResourceLocation var7 = TEXTURE_ID_CONVERTER.idToFile(var6);
         Optional<Resource> var8 = param0.getResource(var7);
         if (var8.isEmpty()) {
            LOGGER.warn("Unable to find texture {}", var7);
         } else {
            LazyLoadedImage var9 = new LazyLoadedImage(var7, var8.get(), var4.size());

            for (Entry<String, Supplier<IntUnaryOperator>> var11 : var4.entrySet()) {
               ResourceLocation var12 = var6.withSuffix("_" + var11.getKey());
               param1.add(var12, new PalettedPermutations.PalettedSpriteSupplier(var9, var11.getValue(), var12));
            }
         }
      }
   }

   private static IntUnaryOperator createPaletteMapping(int[] param0, int[] param1) {
      if (param1.length != param0.length) {
         LOGGER.warn("Palette mapping has different sizes: {} and {}", param0.length, param1.length);
         throw new IllegalArgumentException();
      }

      Int2IntMap var2 = new Int2IntOpenHashMap(param1.length);

      for (int var3 = 0; var3 < param0.length; var3++) {
         int var4 = param0[var3];
         if (FastColor.ABGR32.alpha(var4) != 0) {
            var2.put(FastColor.ABGR32.transparent(var4), param1[var3]);
         }
      }

      return l_param1 -> {
         int l_var2 = FastColor.ABGR32.alpha(l_param1);
         if (l_var2 == 0) {
            return l_param1;
         }

         int l_var3 = FastColor.ABGR32.transparent(l_param1);
         int l_var4 = var2.getOrDefault(l_var3, FastColor.ABGR32.opaque(l_var3));
         int l_var5 = FastColor.ABGR32.alpha(l_var4);
         return FastColor.ABGR32.color(l_var2 * l_var5 / 255, l_var4);
      };
   }

   public static int[] loadPaletteEntryFromImage(ResourceManager param0, ResourceLocation param1) {
      Optional<Resource> var2 = param0.getResource(TEXTURE_ID_CONVERTER.idToFile(param1));
      if (var2.isEmpty()) {
         LOGGER.error("Failed to load palette image {}", param1);
         throw new IllegalArgumentException();
      }

      try (
         InputStream var3 = var2.get().open();
         NativeImage var4 = NativeImage.read(var3);
      ) {
         return var4.getPixelsRGBA();
      } catch (Exception var3) {
         LOGGER.error("Couldn't load texture {}", param1, var3);
         throw new IllegalArgumentException();
      }
   }

   @Override
   public SpriteSourceType type() {
      return SpriteSources.PALETTED_PERMUTATIONS;
   }

   record PalettedSpriteSupplier(LazyLoadedImage baseImage, Supplier<IntUnaryOperator> palette, ResourceLocation permutationLocation)
      implements SpriteSource.SpriteSupplier {
      @Nullable
      public SpriteContents get() {
         try {
            NativeImage var1 = this.baseImage.get().mappedCopy(this.palette.get());
            return new SpriteContents(this.permutationLocation, new FrameSize(var1.getWidth(), var1.getHeight()), var1, AnimationMetadataSection.EMPTY);
         } catch (IOException | IllegalArgumentException var1) {
            PalettedPermutations.LOGGER.error("unable to apply palette to {}", this.permutationLocation, var1);
            return null;
         } finally {
            this.baseImage.release();
         }
      }

      @Override
      public void discard() {
         this.baseImage.release();
      }
   }
}
