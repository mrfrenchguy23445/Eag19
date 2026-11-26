package net.minecraft.client.sounds;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.Listener;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMarkerFactory;

public class SoundEngine {
   // BasicMarkerFactory rather than MarkerFactory -- see Connection.MARKERS for why: referencing
   // org.slf4j.MarkerFactory pulls in org.slf4j.impl.StaticMarkerBinder, which the WASM-GC TeaVM
   // classlib does not have.
   private static final Marker MARKER = new BasicMarkerFactory().getMarker("SOUNDS");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final float PITCH_MIN = 0.5F;
   private static final float PITCH_MAX = 2.0F;
   private static final float VOLUME_MIN = 0.0F;
   private static final float VOLUME_MAX = 1.0F;
   private static final int MIN_SOURCE_LIFETIME = 20;
   private static final Set<ResourceLocation> ONLY_WARN_ONCE = Sets.newHashSet();
   private static final long DEFAULT_DEVICE_CHECK_INTERVAL_MS = 1000L;
   public static final String MISSING_SOUND = "FOR THE DEBUG!";
   public static final String OPEN_AL_SOFT_PREFIX = "OpenAL Soft on ";
   public static final int OPEN_AL_SOFT_PREFIX_LENGTH = "OpenAL Soft on ".length();
   private final SoundManager soundManager;
   private final Options options;
   private boolean loaded;
   private final Library library = new Library();
   private final Listener listener = this.library.getListener();
   private final SoundBufferLibrary soundBuffers;
   private final SoundEngineExecutor executor = new SoundEngineExecutor();
   private final ChannelAccess channelAccess = new ChannelAccess(this.library, this.executor);
   private int tickCount;
   private long lastDeviceCheckTime;
   private final AtomicReference<SoundEngine.DeviceCheckState> devicePoolState = new AtomicReference<>(SoundEngine.DeviceCheckState.NO_CHANGE);
   private final Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel = Maps.newHashMap();
   private final Multimap<SoundSource, SoundInstance> instanceBySource = HashMultimap.create();
   private final List<TickableSoundInstance> tickingSounds = Lists.newArrayList();
   private final Map<SoundInstance, Integer> queuedSounds = Maps.newHashMap();
   private final Map<SoundInstance, Integer> soundDeleteTime = Maps.newHashMap();
   private final List<SoundEventListener> listeners = Lists.newArrayList();
   private final List<TickableSoundInstance> queuedTickableSounds = Lists.newArrayList();
   private final List<Sound> preloadQueue = Lists.newArrayList();

   public SoundEngine(SoundManager param0, Options param1, ResourceProvider param2) {
      this.soundManager = param0;
      this.options = param1;
      this.soundBuffers = new SoundBufferLibrary(param2);
   }

   public void reload() {
      ONLY_WARN_ONCE.clear();

      for (SoundEvent var2 : BuiltInRegistries.SOUND_EVENT) {
         if (var2 != SoundEvents.EMPTY) {
            ResourceLocation var3 = var2.getLocation();
            if (this.soundManager.getSoundEvent(var3) == null) {
               LOGGER.warn("Missing sound for event: {}", BuiltInRegistries.SOUND_EVENT.getKey(var2));
               ONLY_WARN_ONCE.add(var3);
            }
         }
      }

      this.destroy();
      this.loadLibrary();
   }

   private synchronized void loadLibrary() {
      if (!this.loaded) {
         try {
            String var1 = this.options.soundDevice().get();
            this.library.init("".equals(var1) ? null : var1, this.options.directionalAudio().get());
            this.listener.reset();
            this.listener.setGain(this.options.getSoundSourceVolume(SoundSource.MASTER));
            this.soundBuffers.preload(this.preloadQueue).thenRun(this.preloadQueue::clear);
            this.loaded = true;
            LOGGER.info(MARKER, "Sound engine started");
         } catch (RuntimeException var1) {
            LOGGER.error(MARKER, "Error starting SoundSystem. Turning off sounds & music", var1);
         }
      }
   }

   private float getVolume(@Nullable SoundSource param0) {
      return param0 != null && param0 != SoundSource.MASTER ? this.options.getSoundSourceVolume(param0) : 1.0F;
   }

   public void updateCategoryVolume(SoundSource param0, float param1) {
      if (this.loaded) {
         if (param0 == SoundSource.MASTER) {
            this.listener.setGain(param1);
         } else {
            this.instanceToChannel.forEach((l_param0, l_param1) -> {
               float l_var3 = this.calculateVolume(l_param0);
               l_param1.execute(l2_param1 -> {
                  if (l_var3 <= 0.0F) {
                     l2_param1.stop();
                  } else {
                     l2_param1.setVolume(l_var3);
                  }
               });
            });
         }
      }
   }

   public void destroy() {
      if (this.loaded) {
         this.stopAll();
         this.soundBuffers.clear();
         this.library.cleanup();
         this.loaded = false;
      }
   }

   public void stop(SoundInstance param0) {
      if (this.loaded) {
         ChannelAccess.ChannelHandle var2 = this.instanceToChannel.get(param0);
         if (var2 != null) {
            var2.execute(Channel::stop);
         }
      }
   }

   public void stopAll() {
      if (this.loaded) {
         this.executor.flush();
         this.instanceToChannel.values().forEach(l_param0 -> l_param0.execute(Channel::stop));
         this.instanceToChannel.clear();
         this.channelAccess.clear();
         this.queuedSounds.clear();
         this.tickingSounds.clear();
         this.instanceBySource.clear();
         this.soundDeleteTime.clear();
         this.queuedTickableSounds.clear();
      }
   }

   public void addEventListener(SoundEventListener param0) {
      this.listeners.add(param0);
   }

   public void removeEventListener(SoundEventListener param0) {
      this.listeners.remove(param0);
   }

   private boolean shouldChangeDevice() {
      if (this.library.isCurrentDeviceDisconnected()) {
         LOGGER.info("Audio device was lost!");
         return true;
      }

      long var1 = Util.getMillis();
      boolean var3 = var1 - this.lastDeviceCheckTime >= 1000L;
      if (var3) {
         this.lastDeviceCheckTime = var1;
         if (this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.NO_CHANGE, SoundEngine.DeviceCheckState.ONGOING)) {
            String var4 = this.options.soundDevice().get();
            Util.ioPool().execute(() -> {
               if ("".equals(var4)) {
                  if (this.library.hasDefaultDeviceChanged()) {
                     LOGGER.info("System default audio device has changed!");
                     this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.ONGOING, SoundEngine.DeviceCheckState.CHANGE_DETECTED);
                  }
               } else if (!this.library.getCurrentDeviceName().equals(var4) && this.library.getAvailableSoundDevices().contains(var4)) {
                  LOGGER.info("Preferred audio device has become available!");
                  this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.ONGOING, SoundEngine.DeviceCheckState.CHANGE_DETECTED);
               }

               this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.ONGOING, SoundEngine.DeviceCheckState.NO_CHANGE);
            });
         }
      }

      return this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.CHANGE_DETECTED, SoundEngine.DeviceCheckState.NO_CHANGE);
   }

   public void tick(boolean param0) {
      if (this.shouldChangeDevice()) {
         this.reload();
      }

      if (!param0) {
         this.tickNonPaused();
      }

      this.channelAccess.scheduleTick();
   }

   private void tickNonPaused() {
      this.tickCount++;
      this.queuedTickableSounds.stream().filter(SoundInstance::canPlaySound).forEach(this::play);
      this.queuedTickableSounds.clear();

      for (TickableSoundInstance var2 : this.tickingSounds) {
         if (!var2.canPlaySound()) {
            this.stop(var2);
         }

         var2.tick();
         if (var2.isStopped()) {
            this.stop(var2);
         } else {
            float var3 = this.calculateVolume(var2);
            float var4 = this.calculatePitch(var2);
            Vec3 var5 = new Vec3(var2.getX(), var2.getY(), var2.getZ());
            ChannelAccess.ChannelHandle var6 = this.instanceToChannel.get(var2);
            if (var6 != null) {
               var6.execute(l_param3 -> {
                  l_param3.setVolume(var3);
                  l_param3.setPitch(var4);
                  l_param3.setSelfPosition(var5);
               });
            }
         }
      }

      Iterator<Entry<SoundInstance, ChannelAccess.ChannelHandle>> var1 = this.instanceToChannel.entrySet().iterator();

      while (var1.hasNext()) {
         Entry<SoundInstance, ChannelAccess.ChannelHandle> var2 = var1.next();
         ChannelAccess.ChannelHandle var3 = var2.getValue();
         SoundInstance var4 = var2.getKey();
         float var5 = this.options.getSoundSourceVolume(var4.getSource());
         if (var5 <= 0.0F) {
            var3.execute(Channel::stop);
            var1.remove();
         } else if (var3.isStopped()) {
            int var6 = this.soundDeleteTime.get(var4);
            if (var6 <= this.tickCount) {
               if (shouldLoopManually(var4)) {
                  this.queuedSounds.put(var4, this.tickCount + var4.getDelay());
               }

               var1.remove();
               LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", var3);
               this.soundDeleteTime.remove(var4);

               try {
                  this.instanceBySource.remove(var4.getSource(), var4);
               } catch (RuntimeException var8) {
               }

               if (var4 instanceof TickableSoundInstance) {
                  this.tickingSounds.remove(var4);
               }
            }
         }
      }

      Iterator<Entry<SoundInstance, Integer>> var2 = this.queuedSounds.entrySet().iterator();

      while (var2.hasNext()) {
         Entry<SoundInstance, Integer> var3 = var2.next();
         if (this.tickCount >= var3.getValue()) {
            SoundInstance var4 = var3.getKey();
            if (var4 instanceof TickableSoundInstance) {
               ((TickableSoundInstance)var4).tick();
            }

            this.play(var4);
            var2.remove();
         }
      }
   }

   private static boolean requiresManualLooping(SoundInstance param0) {
      return param0.getDelay() > 0;
   }

   private static boolean shouldLoopManually(SoundInstance param0) {
      return param0.isLooping() && requiresManualLooping(param0);
   }

   private static boolean shouldLoopAutomatically(SoundInstance param0) {
      return param0.isLooping() && !requiresManualLooping(param0);
   }

   public boolean isActive(SoundInstance param0) {
      if (!this.loaded) {
         return false;
      } else {
         return this.soundDeleteTime.containsKey(param0) && this.soundDeleteTime.get(param0) <= this.tickCount
            ? true
            : this.instanceToChannel.containsKey(param0);
      }
   }

   public void play(SoundInstance param0) {
      if (this.loaded) {
         if (param0.canPlaySound()) {
            WeighedSoundEvents var2 = param0.resolve(this.soundManager);
            ResourceLocation var3 = param0.getLocation();
            if (var2 == null) {
               if (ONLY_WARN_ONCE.add(var3)) {
                  LOGGER.warn(MARKER, "Unable to play unknown soundEvent: {}", var3);
               }
            } else {
               Sound var4 = param0.getSound();
               if (var4 != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
                  if (var4 == SoundManager.EMPTY_SOUND) {
                     if (ONLY_WARN_ONCE.add(var3)) {
                        LOGGER.warn(MARKER, "Unable to play empty soundEvent: {}", var3);
                     }
                  } else {
                     float var5 = param0.getVolume();
                     float var6 = Math.max(var5, 1.0F) * var4.getAttenuationDistance();
                     SoundSource var7 = param0.getSource();
                     float var8 = this.calculateVolume(var5, var7);
                     float var9 = this.calculatePitch(param0);
                     SoundInstance.Attenuation var10 = param0.getAttenuation();
                     boolean var11 = param0.isRelative();
                     if (var8 == 0.0F && !param0.canStartSilent()) {
                        LOGGER.debug(MARKER, "Skipped playing sound {}, volume was zero.", var4.getLocation());
                     } else {
                        Vec3 var12 = new Vec3(param0.getX(), param0.getY(), param0.getZ());
                        if (!this.listeners.isEmpty()) {
                           boolean var13 = var11
                              || var10 == SoundInstance.Attenuation.NONE
                              || this.listener.getListenerPosition().distanceToSqr(var12) < var6 * var6;
                           if (var13) {
                              for (SoundEventListener var15 : this.listeners) {
                                 var15.onPlaySound(param0, var2);
                              }
                           } else {
                              LOGGER.debug(MARKER, "Did not notify listeners of soundEvent: {}, it is too far away to hear", var3);
                           }
                        }

                        if (this.listener.getGain() <= 0.0F) {
                           LOGGER.debug(MARKER, "Skipped playing soundEvent: {}, master volume was zero", var3);
                        } else {
                           boolean var13 = shouldLoopAutomatically(param0);
                           boolean var14 = var4.shouldStream();
                           CompletableFuture<ChannelAccess.ChannelHandle> var15 = this.channelAccess
                              .createHandle(var4.shouldStream() ? Library.Pool.STREAMING : Library.Pool.STATIC);
                           ChannelAccess.ChannelHandle var16 = var15.join();
                           if (var16 == null) {
                              if (SharedConstants.IS_RUNNING_IN_IDE) {
                                 LOGGER.warn("Failed to create new sound handle");
                              }
                           } else {
                              LOGGER.debug(MARKER, "Playing sound {} for event {}", var4.getLocation(), var3);
                              this.soundDeleteTime.put(param0, this.tickCount + 20);
                              this.instanceToChannel.put(param0, var16);
                              this.instanceBySource.put(var7, param0);
                              var16.execute(l_param8 -> {
                                 l_param8.setPitch(var9);
                                 l_param8.setVolume(var8);
                                 if (var10 == SoundInstance.Attenuation.LINEAR) {
                                    l_param8.linearAttenuation(var6);
                                 } else {
                                    l_param8.disableAttenuation();
                                 }

                                 l_param8.setLooping(var13 && !var14);
                                 l_param8.setSelfPosition(var12);
                                 l_param8.setRelative(var11);
                              });
                              if (!var14) {
                                 this.soundBuffers.getCompleteBuffer(var4.getPath()).thenAccept(l_param1 -> var16.execute(l2_param1 -> {
                                    l2_param1.attachStaticBuffer(l_param1);
                                    l2_param1.play();
                                 }));
                              } else {
                                 this.soundBuffers.getStream(var4.getPath(), var13).thenAccept(l_param1 -> var16.execute(l2_param1 -> {
                                    l2_param1.attachBufferStream(l_param1);
                                    l2_param1.play();
                                 }));
                              }

                              if (param0 instanceof TickableSoundInstance) {
                                 this.tickingSounds.add((TickableSoundInstance)param0);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void queueTickingSound(TickableSoundInstance param0) {
      this.queuedTickableSounds.add(param0);
   }

   public void requestPreload(Sound param0) {
      this.preloadQueue.add(param0);
   }

   private float calculatePitch(SoundInstance param0) {
      return Mth.clamp(param0.getPitch(), 0.5F, 2.0F);
   }

   private float calculateVolume(SoundInstance param0) {
      return this.calculateVolume(param0.getVolume(), param0.getSource());
   }

   private float calculateVolume(float param0, SoundSource param1) {
      return Mth.clamp(param0 * this.getVolume(param1), 0.0F, 1.0F);
   }

   public void pause() {
      if (this.loaded) {
         this.channelAccess.executeOnChannels(l_param0 -> l_param0.forEach(Channel::pause));
      }
   }

   public void resume() {
      if (this.loaded) {
         this.channelAccess.executeOnChannels(l_param0 -> l_param0.forEach(Channel::unpause));
      }
   }

   public void playDelayed(SoundInstance param0, int param1) {
      this.queuedSounds.put(param0, this.tickCount + param1);
   }

   public void updateSource(Camera param0) {
      if (this.loaded && param0.isInitialized()) {
         Vec3 var2 = param0.getPosition();
         Vector3f var3 = param0.getLookVector();
         Vector3f var4 = param0.getUpVector();
         this.executor.execute(() -> {
            this.listener.setListenerPosition(var2);
            this.listener.setListenerOrientation(var3, var4);
         });
      }
   }

   public void stop(@Nullable ResourceLocation param0, @Nullable SoundSource param1) {
      if (param1 != null) {
         for (SoundInstance var4 : this.instanceBySource.get(param1)) {
            if (param0 == null || var4.getLocation().equals(param0)) {
               this.stop(var4);
            }
         }
      } else if (param0 == null) {
         this.stopAll();
      } else {
         for (SoundInstance var4 : this.instanceToChannel.keySet()) {
            if (var4.getLocation().equals(param0)) {
               this.stop(var4);
            }
         }
      }
   }

   public String getDebugString() {
      return this.library.getDebugString();
   }

   public List<String> getAvailableSoundDevices() {
      return this.library.getAvailableSoundDevices();
   }

   enum DeviceCheckState {
      ONGOING,
      CHANGE_DETECTED,
      NO_CHANGE;
   }
}
