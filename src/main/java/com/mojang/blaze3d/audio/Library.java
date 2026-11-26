package com.mojang.blaze3d.audio;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;

/**
 * Port note (LWJGL removal): the channel pools are real, the audio device is not.
 *
 * <p>Vanilla's {@code Library} owned an OpenAL device and context, queried the driver for
 * its channel count, and handed out {@link Channel}s from two {@link CountingChannelPool}s
 * sized to that. All of the device work is gone: {@code alcOpenDevice},
 * {@code alcCreateContext}, {@code AL.createCapabilities}, HRTF reset, disconnect
 * detection, device enumeration. There is no device to open and no capability set to
 * require, so the capability checks that used to throw
 * ({@code "OpenAL 1.1 not supported"}, {@code "Failed to open OpenAL device"}) have
 * nothing to fail against and were removed rather than made to pass vacuously.
 *
 * <p><b>What deliberately survives is the pooling.</b> The pools are still built in
 * {@link #init} and channels are still handed out and taken back, because the entire sound
 * system downstream is built on that contract: {@code ChannelAccess.createHandle} submits
 * to a pool and completes a future with whatever it gets, {@code scheduleTick} returns
 * channels that report themselves stopped, and {@code releaseChannel} throws on an unknown
 * channel. Standing the pools up for real (rather than leaving the {@link #EMPTY} sentinel
 * in place) keeps that machinery exercised and keeps handle counts bounded -- see the
 * class comment on {@link Channel} for why the bounds matter.
 *
 * <p>The pools are sized {@link #DEFAULT_CHANNEL_COUNT} each, which is vanilla's fallback
 * when the driver would not report a count.
 */
public class Library {
   static final Logger LOGGER = LogUtils.getLogger();

   /**
    * Vanilla's fallback when the OpenAL driver would not report a channel count. Both
    * pools are sized to this.
    */
   private static final int DEFAULT_CHANNEL_COUNT = 30;

   /**
    * The name reported for the (absent) device. Chosen so that it cannot collide with a
    * real device name in {@code Options}: {@code SoundEngine}'s device-change check at line
    * 196 only reloads when the current name differs from the configured one <em>and</em>
    * appears in {@link #getAvailableSoundDevices()}, which is empty here -- so this value
    * can never trigger a reload loop.
    */
   private static final String SILENT_DEVICE_NAME = "Eaglercraft (silent)";

   private ChannelPool staticChannels = EMPTY;
   private ChannelPool streamingChannels = EMPTY;
   private final Listener listener = new Listener();
   private boolean initialized;

   public Listener getListener() {
      return this.listener;
   }

   /**
    * Stands in for the device/context setup.
    *
    * <p>Still installs both channel pools, which is the part with real consequences. Logs
    * once per call at debug, matching vanilla's "OpenAL initialized on device {}" line
    * minus the device.
    */
   public void init(@Nullable String param0, boolean param1) {
      if (!this.initialized) {
         this.initialized = true;
         this.staticChannels = new CountingChannelPool(DEFAULT_CHANNEL_COUNT);
         this.streamingChannels = new CountingChannelPool(DEFAULT_CHANNEL_COUNT);
         LOGGER.debug("Audio initialised in silent mode: {} channels per pool, no output device", DEFAULT_CHANNEL_COUNT);
      }
   }

   @Nullable
   public Channel acquireChannel(Library.Pool param0) {
      return (param0 == Library.Pool.STREAMING ? this.streamingChannels : this.staticChannels).acquire();
   }

   public void releaseChannel(Channel param0) {
      if (!this.staticChannels.release(param0) && !this.streamingChannels.release(param0)) {
         throw new IllegalStateException("Tried to release unknown channel");
      }
   }

   /**
    * Destroys both pools.
    *
    * <p>Vanilla destroyed the AL context and closed the device here. Both are gone; what
    * remains matters more for this port, because every silent {@link Channel} may hold an
    * unclosed {@code AudioStream}, and {@code Channel.destroy()} is what closes it. Without
    * this, a device reload would leak a decompressor per in-flight stream.
    */
   public void cleanup() {
      this.staticChannels.cleanup();
      this.streamingChannels.cleanup();
      this.staticChannels = EMPTY;
      this.streamingChannels = EMPTY;
      this.initialized = false;
   }

   public String getDebugString() {
      return String.format(
         Locale.ROOT,
         "Sounds: %d/%d + %d/%d (silent)",
         this.staticChannels.getUsedCount(),
         this.staticChannels.getMaxCount(),
         this.streamingChannels.getUsedCount(),
         this.streamingChannels.getMaxCount()
      );
   }

   /**
    * Always empty.
    *
    * <p>Vanilla enumerated OpenAL playback devices. There are none, and this being empty is
    * what stops {@code Options}' sound-device dropdown and {@code SoundEngine}'s
    * device-change check from doing anything.
    */
   public List<String> getAvailableSoundDevices() {
      return Collections.emptyList();
   }

   /** Always {@code false}: no device exists to be unplugged. */
   public boolean isCurrentDeviceDisconnected() {
      return false;
   }

   /** Always {@code false}: see {@link #getAvailableSoundDevices()} -- nothing to switch to. */
   public boolean hasDefaultDeviceChanged() {
      return false;
   }

   public static String getDefaultDeviceName() {
      return SILENT_DEVICE_NAME;
   }

   public String getCurrentDeviceName() {
      return SILENT_DEVICE_NAME;
   }

   /** The pool held before {@link #init}, and again after {@link #cleanup}. */
   private static final ChannelPool EMPTY = new ChannelPool() {
      @Nullable
      public Channel acquire() {
         return null;
      }

      public boolean release(Channel var1) {
         return false;
      }

      public void cleanup() {
      }

      public int getMaxCount() {
         return 0;
      }

      public int getUsedCount() {
         return 0;
      }
   };

   interface ChannelPool {
      @Nullable
      Channel acquire();

      boolean release(Channel var1);

      void cleanup();

      int getMaxCount();

      int getUsedCount();
   }

   /**
    * Unchanged from vanilla apart from the type of thing it hands out.
    *
    * <p>The bound is what keeps {@link ChannelAccess} from growing without limit: a channel
    * is only returned to the pool once it reports itself stopped, and the silent
    * {@link Channel} does so immediately.
    */
   static class CountingChannelPool implements ChannelPool {
      private final int limit;
      private final Set<Channel> activeChannels = Sets.newIdentityHashSet();

      public CountingChannelPool(int param0) {
         this.limit = param0;
      }

      @Nullable
      public Channel acquire() {
         Channel var1 = Channel.create();
         if (var1 != null && this.activeChannels.size() < this.limit) {
            this.activeChannels.add(var1);
            return var1;
         }

         return null;
      }

      public boolean release(Channel param0) {
         return this.activeChannels.remove(param0);
      }

      public void cleanup() {
         for (Channel var1 : this.activeChannels) {
            var1.destroy();
         }

         this.activeChannels.clear();
      }

      public int getMaxCount() {
         return this.limit;
      }

      public int getUsedCount() {
         return this.activeChannels.size();
      }
   }

   public enum Pool {
      STATIC,
      STREAMING;
   }
}