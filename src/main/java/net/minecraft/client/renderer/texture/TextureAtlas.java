package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;
import org.slf4j.Logger;

public class TextureAtlas extends AbstractTexture implements Dumpable, Tickable {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Deprecated
   public static final ResourceLocation LOCATION_BLOCKS = InventoryMenu.BLOCK_ATLAS;
   @Deprecated
   public static final ResourceLocation LOCATION_PARTICLES = new ResourceLocation("textures/atlas/particles.png");
   private List<SpriteContents> sprites = List.of();
   private List<TextureAtlasSprite.Ticker> animatedTextures = List.of();
   private Map<ResourceLocation, TextureAtlasSprite> texturesByName = Map.of();
   private final ResourceLocation location;
   private final int maxSupportedTextureSize;
   private int width;
   private int height;
   private int mipLevel;

   public TextureAtlas(ResourceLocation param0) {
      this.location = param0;
      this.maxSupportedTextureSize = RenderSystem.maxSupportedTextureSize();
   }

   @Override
   public void load(ResourceManager param0) {
   }

   public void upload(SpriteLoader.Preparations param0) {
      LOGGER.info("Created: {}x{}x{} {}-atlas", param0.width(), param0.height(), param0.mipLevel(), this.location);
      TextureUtil.prepareImage(this.getId(), param0.mipLevel(), param0.width(), param0.height());
      this.width = param0.width();
      this.height = param0.height();
      this.mipLevel = param0.mipLevel();
      this.clearTextureData();
      this.texturesByName = Map.copyOf(param0.regions());
      List<SpriteContents> var2 = new ArrayList<>();
      List<TextureAtlasSprite.Ticker> var3 = new ArrayList<>();

      for (TextureAtlasSprite var5 : param0.regions().values()) {
         var2.add(var5.contents());

         try {
            var5.uploadFirstFrame();
         } catch (Throwable var6) {
            CrashReport var7 = CrashReport.forThrowable(var6, "Stitching texture atlas");
            CrashReportCategory var8 = var7.addCategory("Texture being stitched together");
            var8.setDetail("Atlas path", this.location);
            var8.setDetail("Sprite", var5);
            throw new ReportedException(var7);
         }

         TextureAtlasSprite.Ticker var6 = var5.createTicker();
         if (var6 != null) {
            var3.add(var6);
         }
      }

      this.sprites = List.copyOf(var2);
      this.animatedTextures = List.copyOf(var3);
   }

   @Override
   public void dumpContents(ResourceLocation param0, Path param1) throws IOException {
      String var3 = param0.toDebugFileName();
      TextureUtil.writeAsPNG(param1, var3, this.getId(), this.mipLevel, this.width, this.height);
      dumpSpriteNames(param1, var3, this.texturesByName);
   }

   private static void dumpSpriteNames(Path param0, String param1, Map<ResourceLocation, TextureAtlasSprite> param2) {
      Path var3 = param0.resolve(param1 + ".txt");

      try (Writer var4 = Files.newBufferedWriter(var3)) {
         for (Entry<ResourceLocation, TextureAtlasSprite> var6 : param2.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
            TextureAtlasSprite var7 = var6.getValue();
            var4.write(
               String.format(
                  Locale.ROOT, "%s\tx=%d\ty=%d\tw=%d\th=%d%n", var6.getKey(), var7.getX(), var7.getY(), var7.contents().width(), var7.contents().height()
               )
            );
         }
      } catch (IOException var4) {
         LOGGER.warn("Failed to write file {}", var3, var4);
      }
   }

   public void cycleAnimationFrames() {
      this.bind();

      for (TextureAtlasSprite.Ticker var2 : this.animatedTextures) {
         var2.tickAndUpload();
      }
   }

   @Override
   public void tick() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(this::cycleAnimationFrames);
      } else {
         this.cycleAnimationFrames();
      }
   }

   public TextureAtlasSprite getSprite(ResourceLocation param0) {
      TextureAtlasSprite var2 = this.texturesByName.get(param0);
      return var2 == null ? this.texturesByName.get(MissingTextureAtlasSprite.getLocation()) : var2;
   }

   public void clearTextureData() {
      this.sprites.forEach(SpriteContents::close);
      this.animatedTextures.forEach(TextureAtlasSprite.Ticker::close);
      this.sprites = List.of();
      this.animatedTextures = List.of();
      this.texturesByName = Map.of();
   }

   public ResourceLocation location() {
      return this.location;
   }

   public int maxSupportedTextureSize() {
      return this.maxSupportedTextureSize;
   }

   int getWidth() {
      return this.width;
   }

   int getHeight() {
      return this.height;
   }

   public void updateFilter(SpriteLoader.Preparations param0) {
      this.setFilter(false, param0.mipLevel() > 0);
   }
}
