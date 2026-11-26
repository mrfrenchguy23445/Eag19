package net.minecraft.client.renderer.texture;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class TextureManager implements PreparableReloadListener, Tickable, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ResourceLocation INTENTIONAL_MISSING_TEXTURE = new ResourceLocation("");
   private final Map<ResourceLocation, AbstractTexture> byPath = Maps.newHashMap();
   private final Set<Tickable> tickableTextures = Sets.newHashSet();
   private final Map<String, Integer> prefixRegister = Maps.newHashMap();
   private final ResourceManager resourceManager;

   public TextureManager(ResourceManager param0) {
      this.resourceManager = param0;
   }

   public void bindForSetup(ResourceLocation param0) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this._bind(param0));
      } else {
         this._bind(param0);
      }
   }

   private void _bind(ResourceLocation param0) {
      AbstractTexture var2 = this.byPath.get(param0);
      if (var2 == null) {
         var2 = new SimpleTexture(param0);
         this.register(param0, var2);
      }

      var2.bind();
   }

   public void register(ResourceLocation param0, AbstractTexture param1) {
      param1 = this.loadTexture(param0, param1);
      AbstractTexture var3 = this.byPath.put(param0, param1);
      if (var3 != param1) {
         if (var3 != null && var3 != MissingTextureAtlasSprite.getTexture()) {
            this.tickableTextures.remove(var3);
            this.safeClose(param0, var3);
         }

         if (param1 instanceof Tickable) {
            this.tickableTextures.add((Tickable)param1);
         }
      }
   }

   private void safeClose(ResourceLocation param0, AbstractTexture param1) {
      if (param1 != MissingTextureAtlasSprite.getTexture()) {
         try {
            param1.close();
         } catch (Exception var3) {
            LOGGER.warn("Failed to close texture {}", param0, var3);
         }
      }

      param1.releaseId();
   }

   private AbstractTexture loadTexture(ResourceLocation param0, AbstractTexture param1) {
      try {
         param1.load(this.resourceManager);
         return param1;
      } catch (IOException var3) {
         if (param0 != INTENTIONAL_MISSING_TEXTURE) {
            LOGGER.warn("Failed to load texture: {}", param0, var3);
         }

         return MissingTextureAtlasSprite.getTexture();
      } catch (Throwable var3) {
         CrashReport var4 = CrashReport.forThrowable(var3, "Registering texture");
         CrashReportCategory var5 = var4.addCategory("Resource location being registered");
         var5.setDetail("Resource location", param0);
         var5.setDetail("Texture object class", () -> param1.getClass().getName());
         throw new ReportedException(var4);
      }
   }

   public AbstractTexture getTexture(ResourceLocation param0) {
      AbstractTexture var2 = this.byPath.get(param0);
      if (var2 == null) {
         var2 = new SimpleTexture(param0);
         this.register(param0, var2);
      }

      return var2;
   }

   public AbstractTexture getTexture(ResourceLocation param0, AbstractTexture param1) {
      return this.byPath.getOrDefault(param0, param1);
   }

   public ResourceLocation register(String param0, DynamicTexture param1) {
      Integer var3 = this.prefixRegister.get(param0);
      if (var3 == null) {
         var3 = 1;
      } else {
         var3 = var3 + 1;
      }

      this.prefixRegister.put(param0, var3);
      ResourceLocation var4 = new ResourceLocation(String.format(Locale.ROOT, "dynamic/%s_%d", param0, var3));
      this.register(var4, param1);
      return var4;
   }

   public CompletableFuture<Void> preload(ResourceLocation param0, Executor param1) {
      if (!this.byPath.containsKey(param0)) {
         PreloadedTexture var3 = new PreloadedTexture(this.resourceManager, param0, param1);
         this.byPath.put(param0, var3);
         return var3.getFuture().thenRunAsync(() -> this.register(param0, var3), TextureManager::execute);
      } else {
         return CompletableFuture.completedFuture(null);
      }
   }

   private static void execute(Runnable param0) {
      Minecraft.getInstance().execute(() -> RenderSystem.recordRenderCall(param0::run));
   }

   @Override
   public void tick() {
      for (Tickable var2 : this.tickableTextures) {
         var2.tick();
      }
   }

   public void release(ResourceLocation param0) {
      AbstractTexture var2 = this.getTexture(param0, MissingTextureAtlasSprite.getTexture());
      if (var2 != MissingTextureAtlasSprite.getTexture()) {
         TextureUtil.releaseTextureId(var2.getId());
      }
   }

   @Override
   public void close() {
      this.byPath.forEach(this::safeClose);
      this.byPath.clear();
      this.tickableTextures.clear();
      this.prefixRegister.clear();
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier param0,
      ResourceManager param1,
      ProfilerFiller param2,
      ProfilerFiller param3,
      Executor param4,
      Executor param5
   ) {
      CompletableFuture<Void> var7 = new CompletableFuture<>();
      CompletableFuture.allOf(TitleScreen.preloadResources(this, param4), this.preload(AbstractWidget.WIDGETS_LOCATION, param4))
         .thenCompose(param0::wait)
         .thenAcceptAsync(l_param3 -> {
            MissingTextureAtlasSprite.getTexture();
            RealmsMainScreen.updateTeaserImages(this.resourceManager);
            Iterator<Entry<ResourceLocation, AbstractTexture>> l_var5 = this.byPath.entrySet().iterator();

            while (l_var5.hasNext()) {
               Entry<ResourceLocation, AbstractTexture> l_var6 = l_var5.next();
               ResourceLocation l_var7 = l_var6.getKey();
               AbstractTexture l_var8 = l_var6.getValue();
               if (l_var8 == MissingTextureAtlasSprite.getTexture() && !l_var7.equals(MissingTextureAtlasSprite.getLocation())) {
                  l_var5.remove();
               } else {
                  l_var8.reset(this, param1, l_var7, param5);
               }
            }

            Minecraft.getInstance().tell(() -> var7.complete(null));
         }, l_param0 -> RenderSystem.recordRenderCall(l_param0::run));
      return var7;
   }

   public void dumpAllSheets(Path param0) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this._dumpAllSheets(param0));
      } else {
         this._dumpAllSheets(param0);
      }
   }

   private void _dumpAllSheets(Path param0) {
      try {
         Files.createDirectories(param0);
      } catch (IOException var2) {
         LOGGER.error("Failed to create directory {}", param0, var2);
         return;
      }

      this.byPath.forEach((l_param1, l_param2) -> {
         if (l_param2 instanceof Dumpable l_var3) {
            try {
               l_var3.dumpContents(l_param1, param0);
            } catch (IOException l_var4) {
               LOGGER.error("Failed to dump texture {}", l_param1, l_var4);
            }
         }
      });
   }
}
