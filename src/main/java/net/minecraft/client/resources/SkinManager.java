package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

public class SkinManager {
   public static final String PROPERTY_TEXTURES = "textures";
   private final TextureManager textureManager;
   private final File skinsDirectory;
   private final MinecraftSessionService sessionService;
   private final LoadingCache<String, Map<Type, MinecraftProfileTexture>> insecureSkinCache;

   public SkinManager(TextureManager param0, File param1, final MinecraftSessionService param2) {
      this.textureManager = param0;
      this.skinsDirectory = param1;
      this.sessionService = param2;
      this.insecureSkinCache = CacheBuilder.newBuilder()
         .expireAfterAccess(15L, TimeUnit.SECONDS)
         .build(new CacheLoader<String, Map<Type, MinecraftProfileTexture>>() {
            public Map<Type, MinecraftProfileTexture> load(String l_param0) {
               GameProfile l_var2 = new GameProfile(null, "dummy_mcdummyface");
               l_var2.getProperties().put("textures", new Property("textures", l_param0, ""));

               try {
                  return param2.getTextures(l_var2, false);
               } catch (Throwable l_var3) {
                  return ImmutableMap.of();
               }
            }
         });
   }

   public ResourceLocation registerTexture(MinecraftProfileTexture param0, Type param1) {
      return this.registerTexture(param0, param1, null);
   }

   private ResourceLocation registerTexture(MinecraftProfileTexture param0, Type param1, @Nullable SkinManager.SkinTextureCallback param2) {
      String var4 = Hashing.sha1().hashUnencodedChars(param0.getHash()).toString();
      ResourceLocation var5 = getTextureLocation(param1, var4);
      AbstractTexture var6 = this.textureManager.getTexture(var5, MissingTextureAtlasSprite.getTexture());
      if (var6 == MissingTextureAtlasSprite.getTexture()) {
         File var7 = new File(this.skinsDirectory, var4.length() > 2 ? var4.substring(0, 2) : "xx");
         File var8 = new File(var7, var4);
         HttpTexture var9 = new HttpTexture(var8, param0.getUrl(), DefaultPlayerSkin.getDefaultSkin(), param1 == Type.SKIN, () -> {
            if (param2 != null) {
               param2.onSkinTextureAvailable(param1, var5, param0);
            }
         });
         this.textureManager.register(var5, var9);
      } else if (param2 != null) {
         param2.onSkinTextureAvailable(param1, var5, param0);
      }

      return var5;
   }

   private static ResourceLocation getTextureLocation(Type param0, String param1) {
      String var2 = switch (param0) {
         case SKIN -> "skins";
         case CAPE -> "capes";
         case ELYTRA -> "elytra";
      };
      return new ResourceLocation(var2 + "/" + param1);
   }

   public void registerSkins(GameProfile param0, SkinManager.SkinTextureCallback param1, boolean param2) {
      Runnable var4 = () -> {
         Map<Type, MinecraftProfileTexture> l_var4 = Maps.newHashMap();

         try {
            l_var4.putAll(this.sessionService.getTextures(param0, param2));
         } catch (InsecureTextureException var7) {
         }

         if (l_var4.isEmpty()) {
            param0.getProperties().clear();
            if (param0.getId().equals(Minecraft.getInstance().getUser().getGameProfile().getId())) {
               param0.getProperties().putAll(Minecraft.getInstance().getProfileProperties());
               l_var4.putAll(this.sessionService.getTextures(param0, false));
            } else {
               this.sessionService.fillProfileProperties(param0, param2);

               try {
                  l_var4.putAll(this.sessionService.getTextures(param0, param2));
               } catch (InsecureTextureException var6) {
               }
            }
         }

         Minecraft.getInstance().execute(() -> RenderSystem.recordRenderCall(() -> ImmutableList.of(Type.SKIN, Type.CAPE).forEach(l4_param2 -> {
            if (l_var4.containsKey(l4_param2)) {
               this.registerTexture(l_var4.get(l4_param2), l4_param2, param1);
            }
         })));
      };
      Util.backgroundExecutor().execute(var4);
   }

   public Map<Type, MinecraftProfileTexture> getInsecureSkinInformation(GameProfile param0) {
      Property var2 = Iterables.getFirst(param0.getProperties().get("textures"), null);
      return var2 == null ? ImmutableMap.of() : this.insecureSkinCache.getUnchecked(var2.getValue());
   }

   public ResourceLocation getInsecureSkinLocation(GameProfile param0) {
      MinecraftProfileTexture var2 = this.getInsecureSkinInformation(param0).get(Type.SKIN);
      return var2 != null ? this.registerTexture(var2, Type.SKIN) : DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(param0));
   }

   public interface SkinTextureCallback {
      void onSkinTextureAvailable(Type var1, ResourceLocation var2, MinecraftProfileTexture var3);
   }
}
