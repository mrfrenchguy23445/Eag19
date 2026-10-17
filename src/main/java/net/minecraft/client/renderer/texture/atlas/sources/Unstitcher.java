package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class Unstitcher implements SpriteSource {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<Unstitcher> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            ResourceLocation.CODEC.fieldOf("resource").forGetter(l_param0 -> l_param0.resource),
            ExtraCodecs.nonEmptyList(Unstitcher.Region.CODEC.listOf()).fieldOf("regions").forGetter(l_param0 -> l_param0.regions),
            Codec.DOUBLE.optionalFieldOf("divisor_x", 1.0).forGetter(l_param0 -> l_param0.xDivisor),
            Codec.DOUBLE.optionalFieldOf("divisor_y", 1.0).forGetter(l_param0 -> l_param0.yDivisor)
         )
         .apply(param0, Unstitcher::new)
   );
   private final ResourceLocation resource;
   private final List<Unstitcher.Region> regions;
   private final double xDivisor;
   private final double yDivisor;

   public Unstitcher(ResourceLocation param0, List<Unstitcher.Region> param1, double param2, double param4) {
      this.resource = param0;
      this.regions = param1;
      this.xDivisor = param2;
      this.yDivisor = param4;
   }

   @Override
   public void run(ResourceManager param0, SpriteSource.Output param1) {
      ResourceLocation var3 = TEXTURE_ID_CONVERTER.idToFile(this.resource);
      Optional<Resource> var4 = param0.getResource(var3);
      if (var4.isPresent()) {
         LazyLoadedImage var5 = new LazyLoadedImage(var3, var4.get(), this.regions.size());

         for (Unstitcher.Region var7 : this.regions) {
            param1.add(var7.sprite, new Unstitcher.RegionInstance(var5, var7, this.xDivisor, this.yDivisor));
         }
      } else {
         LOGGER.warn("Missing sprite: {}", var3);
      }
   }

   @Override
   public SpriteSourceType type() {
      return SpriteSources.UNSTITCHER;
   }

   record Region(ResourceLocation sprite, double x, double y, double width, double height) {
      public static final Codec<Unstitcher.Region> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               ResourceLocation.CODEC.fieldOf("sprite").forGetter(Unstitcher.Region::sprite),
               Codec.DOUBLE.fieldOf("x").forGetter(Unstitcher.Region::x),
               Codec.DOUBLE.fieldOf("y").forGetter(Unstitcher.Region::y),
               Codec.DOUBLE.fieldOf("width").forGetter(Unstitcher.Region::width),
               Codec.DOUBLE.fieldOf("height").forGetter(Unstitcher.Region::height)
            )
            .apply(param0, Unstitcher.Region::new)
      );
   }

   static class RegionInstance implements SpriteSource.SpriteSupplier {
      private final LazyLoadedImage image;
      private final Unstitcher.Region region;
      private final double xDivisor;
      private final double yDivisor;

      RegionInstance(LazyLoadedImage param0, Unstitcher.Region param1, double param2, double param4) {
         this.image = param0;
         this.region = param1;
         this.xDivisor = param2;
         this.yDivisor = param4;
      }

      public SpriteContents get() {
         try {
            NativeImage var1 = this.image.get();
            double var2 = var1.getWidth() / this.xDivisor;
            double var4 = var1.getHeight() / this.yDivisor;
            int var6 = Mth.floor(this.region.x * var2);
            int var7 = Mth.floor(this.region.y * var4);
            int var8 = Mth.floor(this.region.width * var2);
            int var9 = Mth.floor(this.region.height * var4);
            NativeImage var10 = new NativeImage(NativeImage.Format.RGBA, var8, var9, false);
            var1.copyRect(var10, var6, var7, 0, 0, var8, var9, false, false);
            return new SpriteContents(this.region.sprite, new FrameSize(var8, var9), var10, AnimationMetadataSection.EMPTY);
         } catch (Exception var1) {
            Unstitcher.LOGGER.error("Failed to unstitch region {}", this.region.sprite, var1);
         } finally {
            this.image.release();
         }

         return MissingTextureAtlasSprite.create();
      }

      @Override
      public void discard() {
         this.image.release();
      }
   }
}
