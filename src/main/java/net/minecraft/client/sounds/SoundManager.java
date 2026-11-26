package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.MultipliedFloats;
import org.slf4j.Logger;

public class SoundManager extends SimplePreparableReloadListener<SoundManager.Preparations> {
   public static final Sound EMPTY_SOUND = new Sound("minecraft:empty", ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16);
   public static final ResourceLocation INTENTIONALLY_EMPTY_SOUND_LOCATION = new ResourceLocation("minecraft", "intentionally_empty");
   public static final WeighedSoundEvents INTENTIONALLY_EMPTY_SOUND_EVENT = new WeighedSoundEvents(INTENTIONALLY_EMPTY_SOUND_LOCATION, null);
   public static final Sound INTENTIONALLY_EMPTY_SOUND = new Sound(
      INTENTIONALLY_EMPTY_SOUND_LOCATION.toString(), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16
   );
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String SOUNDS_PATH = "sounds.json";
   private static final Gson GSON = new GsonBuilder()
      .registerTypeHierarchyAdapter(Component.class, new Component.Serializer())
      .registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer())
      .create();
   // Port note: upstream writes new TypeToken<Map<String, SoundEventRegistration>>() {}, which
   // recovers its type argument from Class.getGenericSuperclass(). That method is absent from this
   // target's TeaVM classlib, so the anonymous form cannot work; the explicit factory carries the
   // same information. See com.google.gson.reflect.TypeToken.
   private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE = TypeToken.getParameterized(Map.class, String.class, SoundEventRegistration.class);
   private final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
   private final SoundEngine soundEngine;
   private final Map<ResourceLocation, Resource> soundCache = new HashMap<>();

   public SoundManager(Options param0) {
      this.soundEngine = new SoundEngine(this, param0, ResourceProvider.fromMap(this.soundCache));
   }

   protected SoundManager.Preparations prepare(ResourceManager param0, ProfilerFiller param1) {
      SoundManager.Preparations var3 = new SoundManager.Preparations();
      param1.startTick();
      param1.push("list");
      var3.listResources(param0);
      param1.pop();

      for (String var5 : param0.getNamespaces()) {
         param1.push(var5);

         try {
            for (Resource var8 : param0.getResourceStack(new ResourceLocation(var5, "sounds.json"))) {
               param1.push(var8.sourcePackId());

               try (Reader var9 = var8.openAsReader()) {
                  param1.push("parse");
                  Map<String, SoundEventRegistration> var10 = GsonHelper.fromJson(GSON, var9, SOUND_EVENT_REGISTRATION_TYPE);
                  param1.popPush("register");

                  for (Entry<String, SoundEventRegistration> var12 : var10.entrySet()) {
                     var3.handleRegistration(new ResourceLocation(var5, var12.getKey()), var12.getValue());
                  }

                  param1.pop();
               } catch (RuntimeException var9) {
                  LOGGER.warn("Invalid {} in resourcepack: '{}'", "sounds.json", var8.sourcePackId(), var9);
               }

               param1.pop();
            }
         } catch (IOException var16) {
         }

         param1.pop();
      }

      param1.endTick();
      return var3;
   }

   protected void apply(SoundManager.Preparations param0, ResourceManager param1, ProfilerFiller param2) {
      param0.apply(this.registry, this.soundCache, this.soundEngine);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         for (ResourceLocation var5 : this.registry.keySet()) {
            WeighedSoundEvents var6 = this.registry.get(var5);
            if (!ComponentUtils.isTranslationResolvable(var6.getSubtitle()) && BuiltInRegistries.SOUND_EVENT.containsKey(var5)) {
               LOGGER.error("Missing subtitle {} for sound event: {}", var6.getSubtitle(), var5);
            }
         }
      }

      if (LOGGER.isDebugEnabled()) {
         for (ResourceLocation var5 : this.registry.keySet()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(var5)) {
               LOGGER.debug("Not having sound event for: {}", var5);
            }
         }
      }

      this.soundEngine.reload();
   }

   public List<String> getAvailableSoundDevices() {
      return this.soundEngine.getAvailableSoundDevices();
   }

   static boolean validateSoundResource(Sound param0, ResourceLocation param1, ResourceProvider param2) {
      ResourceLocation var3 = param0.getPath();
      if (param2.getResource(var3).isEmpty()) {
         LOGGER.warn("File {} does not exist, cannot add it to event {}", var3, param1);
         return false;
      } else {
         return true;
      }
   }

   @Nullable
   public WeighedSoundEvents getSoundEvent(ResourceLocation param0) {
      return this.registry.get(param0);
   }

   public Collection<ResourceLocation> getAvailableSounds() {
      return this.registry.keySet();
   }

   public void queueTickingSound(TickableSoundInstance param0) {
      this.soundEngine.queueTickingSound(param0);
   }

   public void play(SoundInstance param0) {
      this.soundEngine.play(param0);
   }

   public void playDelayed(SoundInstance param0, int param1) {
      this.soundEngine.playDelayed(param0, param1);
   }

   public void updateSource(Camera param0) {
      this.soundEngine.updateSource(param0);
   }

   public void pause() {
      this.soundEngine.pause();
   }

   public void stop() {
      this.soundEngine.stopAll();
   }

   public void destroy() {
      this.soundEngine.destroy();
   }

   public void tick(boolean param0) {
      this.soundEngine.tick(param0);
   }

   public void resume() {
      this.soundEngine.resume();
   }

   public void updateSourceVolume(SoundSource param0, float param1) {
      if (param0 == SoundSource.MASTER && param1 <= 0.0F) {
         this.stop();
      }

      this.soundEngine.updateCategoryVolume(param0, param1);
   }

   public void stop(SoundInstance param0) {
      this.soundEngine.stop(param0);
   }

   public boolean isActive(SoundInstance param0) {
      return this.soundEngine.isActive(param0);
   }

   public void addListener(SoundEventListener param0) {
      this.soundEngine.addEventListener(param0);
   }

   public void removeListener(SoundEventListener param0) {
      this.soundEngine.removeEventListener(param0);
   }

   public void stop(@Nullable ResourceLocation param0, @Nullable SoundSource param1) {
      this.soundEngine.stop(param0, param1);
   }

   public String getDebugString() {
      return this.soundEngine.getDebugString();
   }

   public void reload() {
      this.soundEngine.reload();
   }

   protected static class Preparations {
      final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
      private Map<ResourceLocation, Resource> soundCache = Map.of();

      void listResources(ResourceManager param0) {
         this.soundCache = Sound.SOUND_LISTER.listMatchingResources(param0);
      }

      void handleRegistration(ResourceLocation param0, SoundEventRegistration param1) {
         WeighedSoundEvents var3 = this.registry.get(param0);
         boolean var4 = var3 == null;
         if (var4 || param1.isReplace()) {
            if (!var4) {
               SoundManager.LOGGER.debug("Replaced sound event location {}", param0);
            }

            var3 = new WeighedSoundEvents(param0, param1.getSubtitle());
            this.registry.put(param0, var3);
         }

         ResourceProvider var5 = ResourceProvider.fromMap(this.soundCache);

         for (final Sound var7 : param1.getSounds()) {
            final ResourceLocation var8 = var7.getLocation();
            Weighted<Sound> var9;
            switch (var7.getType()) {
               case FILE:
                  if (!SoundManager.validateSoundResource(var7, param0, var5)) {
                     continue;
                  }

                  var9 = var7;
                  break;
               case SOUND_EVENT:
                  var9 = new Weighted<Sound>() {
                     @Override
                     public int getWeight() {
                        WeighedSoundEvents l_var1 = Preparations.this.registry.get(var8);
                        return l_var1 == null ? 0 : l_var1.getWeight();
                     }

                     public Sound getSound(RandomSource l_param0) {
                        WeighedSoundEvents l_var2 = Preparations.this.registry.get(var8);
                        if (l_var2 == null) {
                           return SoundManager.EMPTY_SOUND;
                        }

                        Sound l_var3 = l_var2.getSound(l_param0);
                        return new Sound(
                           l_var3.getLocation().toString(),
                           new MultipliedFloats(l_var3.getVolume(), var7.getVolume()),
                           new MultipliedFloats(l_var3.getPitch(), var7.getPitch()),
                           var7.getWeight(),
                           Sound.Type.FILE,
                           l_var3.shouldStream() || var7.shouldStream(),
                           l_var3.shouldPreload(),
                           l_var3.getAttenuationDistance()
                        );
                     }

                     @Override
                     public void preloadIfRequired(SoundEngine l_param0) {
                        WeighedSoundEvents l_var2 = Preparations.this.registry.get(var8);
                        if (l_var2 != null) {
                           l_var2.preloadIfRequired(l_param0);
                        }
                     }
                  };
                  break;
               default:
                  throw new IllegalStateException("Unknown SoundEventRegistration type: " + var7.getType());
            }

            var3.addSound(var9);
         }
      }

      public void apply(Map<ResourceLocation, WeighedSoundEvents> param0, Map<ResourceLocation, Resource> param1, SoundEngine param2) {
         param0.clear();
         param1.clear();
         param1.putAll(this.soundCache);

         for (Entry<ResourceLocation, WeighedSoundEvents> var5 : this.registry.entrySet()) {
            param0.put(var5.getKey(), var5.getValue());
            var5.getValue().preloadIfRequired(param2);
         }
      }
   }
}
