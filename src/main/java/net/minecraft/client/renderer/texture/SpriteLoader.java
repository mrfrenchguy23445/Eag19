package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class SpriteLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation location;
   private final int maxSupportedTextureSize;
   private final int minWidth;
   private final int minHeight;

   public SpriteLoader(ResourceLocation param0, int param1, int param2, int param3) {
      this.location = param0;
      this.maxSupportedTextureSize = param1;
      this.minWidth = param2;
      this.minHeight = param3;
   }

   public static SpriteLoader create(TextureAtlas param0) {
      return new SpriteLoader(param0.location(), param0.maxSupportedTextureSize(), param0.getWidth(), param0.getHeight());
   }

   public SpriteLoader.Preparations stitch(List<SpriteContents> param0, int param1, Executor param2) {
      int var4 = this.maxSupportedTextureSize;
      Stitcher<SpriteContents> var5 = new Stitcher<>(var4, var4, param1);
      int var6 = Integer.MAX_VALUE;
      int var7 = 1 << param1;

      for (SpriteContents var9 : param0) {
         var6 = Math.min(var6, Math.min(var9.width(), var9.height()));
         int var10 = Math.min(Integer.lowestOneBit(var9.width()), Integer.lowestOneBit(var9.height()));
         if (var10 < var7) {
            LOGGER.warn("Texture {} with size {}x{} limits mip level from {} to {}", var9.name(), var9.width(), var9.height(), Mth.log2(var7), Mth.log2(var10));
            var7 = var10;
         }

         var5.registerSprite(var9);
      }

      int var8 = Math.min(var6, var7);
      int var9 = Mth.log2(var8);
      int var10;
      if (var9 < param1) {
         LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", this.location, param1, var9, var8);
         var10 = var9;
      } else {
         var10 = param1;
      }

      try {
         var5.stitch();
      } catch (StitcherException var11) {
         CrashReport var12 = CrashReport.forThrowable(var11, "Stitching");
         CrashReportCategory var13 = var12.addCategory("Stitcher");
         var13.setDetail(
            "Sprites",
            var11.getAllSprites()
               .stream()
               .map(l_param0 -> String.format(Locale.ROOT, "%s[%dx%d]", l_param0.name(), l_param0.width(), l_param0.height()))
               .collect(Collectors.joining(","))
         );
         var13.setDetail("Max Texture Size", var4);
         throw new ReportedException(var12);
      }

      int var11 = Math.max(var5.getWidth(), this.minWidth);
      int var12 = Math.max(var5.getHeight(), this.minHeight);
      Map<ResourceLocation, TextureAtlasSprite> var13 = this.getStitchedSprites(var5, var11, var12);
      TextureAtlasSprite var14 = var13.get(MissingTextureAtlasSprite.getLocation());
      CompletableFuture<Void> var15;
      if (var10 > 0) {
         var15 = CompletableFuture.runAsync(() -> var13.values().forEach(l2_param1 -> l2_param1.contents().increaseMipLevel(var10)), param2);
      } else {
         var15 = CompletableFuture.completedFuture(null);
      }

      return new SpriteLoader.Preparations(var11, var12, var10, var14, var13, var15);
   }

   public static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(List<Supplier<SpriteContents>> param0, Executor param1) {
      List<CompletableFuture<SpriteContents>> var2 = param0.stream()
         .map(l_param1 -> CompletableFuture.supplyAsync((Supplier<SpriteContents>)l_param1, param1))
         .toList();
      return Util.sequence(var2).thenApply(l_param0 -> l_param0.stream().filter(Objects::nonNull).toList());
   }

   public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(ResourceManager param0, ResourceLocation param1, int param2, Executor param3) {
      return CompletableFuture.<List<Supplier<SpriteContents>>>supplyAsync(() -> SpriteResourceLoader.load(param0, param1).list(param0), param3)
         .thenCompose(l_param1 -> runSpriteSuppliers((List<Supplier<SpriteContents>>)l_param1, param3))
         .thenApply(l_param2 -> this.stitch((List<SpriteContents>)l_param2, param2, param3));
   }

   @Nullable
   public static SpriteContents loadSprite(ResourceLocation param0, Resource param1) {
      AnimationMetadataSection var2;
      try {
         var2 = param1.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElse(AnimationMetadataSection.EMPTY);
      } catch (Exception var3) {
         LOGGER.error("Unable to parse metadata from {}", param0, var3);
         return null;
      }

      NativeImage var3;
      try (InputStream var4 = param1.open()) {
         var3 = NativeImage.read(var4);
      } catch (IOException var4) {
         LOGGER.error("Using missing texture, unable to load {}", param0, var4);
         return null;
      }

      FrameSize var4 = var2.calculateFrameSize(var3.getWidth(), var3.getHeight());
      if (Mth.isMultipleOf(var3.getWidth(), var4.width()) && Mth.isMultipleOf(var3.getHeight(), var4.height())) {
         return new SpriteContents(param0, var4, var3, var2);
      }

      LOGGER.error("Image {} size {},{} is not multiple of frame size {},{}", param0, var3.getWidth(), var3.getHeight(), var4.width(), var4.height());
      var3.close();
      return null;
   }

   private Map<ResourceLocation, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> param0, int param1, int param2) {
      Map<ResourceLocation, TextureAtlasSprite> var4 = new HashMap<>();
      param0.gatherSprites(
         (l_param3, l_param4, l_param5) -> var4.put(l_param3.name(), new TextureAtlasSprite(this.location, l_param3, param1, param2, l_param4, l_param5))
      );
      return var4;
   }

   public record Preparations(
      int width,
      int height,
      int mipLevel,
      TextureAtlasSprite missing,
      Map<ResourceLocation, TextureAtlasSprite> regions,
      CompletableFuture<Void> readyForUpload
   ) {
      public CompletableFuture<SpriteLoader.Preparations> waitForUpload() {
         return this.readyForUpload.thenApply(l_param0 -> this);
      }
   }
}
