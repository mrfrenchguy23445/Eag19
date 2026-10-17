package com.mojang.blaze3d.audio;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Port note (LWJGL removal): a silent null object standing in for an OpenAL source.
 *
 * <p>Vanilla's {@code Channel} was a thin wrapper over one {@code ALuint} source:
 * {@code alGenSources}, {@code alSourcePlay}, {@code alSourcefv}, buffer queueing, and
 * streaming via {@code alSourceQueueBuffers}. None of that exists here -- there is no
 * OpenAL, and the target's audio backend is the browser's Web Audio, reached through
 * Eaglercraft's {@code PlatformAudio} seam, which takes whole tracks and absolute
 * positions rather than per-source state. Every setter below was a write into OpenAL's
 * source struct with no Java-side counterpart, so they became no-ops; the method signatures
 * stay because {@code SoundEngine} and {@code ChannelAccess} are driven entirely through
 * this interface and are worth keeping compilable and unmodified.
 *
 * <p><b>The one design decision that matters: this reports itself as stopped.</b>
 * {@link #stopped()} returning {@code true} is what makes the null object behave like a
 * sound that finished playing instantly, and the rest of the sound system is already built
 * to handle exactly that case:
 *
 * <ul>
 *   <li>{@code ChannelAccess.scheduleTick()} sees {@code stopped()}, calls
 *       {@code ChannelHandle.release()} and drops it from the active set.</li>
 *   <li>{@code SoundEngine.tick} then takes the {@code else if (var3.isStopped())} branch
 *       (line 259) and removes the handle once {@code soundDeleteTime} elapses, re-queueing
 *       it first if {@code shouldLoopManually}.</li>
 * </ul>
 *
 * <p>The alternative -- reporting {@link #playing()} as {@code true} forever -- would pin
 * every ambient and looping sound to a channel for the whole session and exhaust the pool,
 * after which {@code acquireChannel} returns null and every later sound is dropped. So
 * "finished immediately" is both the honest report and the one that keeps the pool bounded.
 *
 * <p>Streaming still tracks its {@link AudioStream} purely so {@link #destroy()} can close
 * it: a Vorbis stream opened and never closed leaks both a decompressor and the underlying
 * resource-manager handle.
 */
public class Channel {
   private static final Logger LOGGER = LogUtils.getLogger();

   /** OpenAL's AL_STOPPED, kept so {@link #getState()} keeps reporting AL's own numbering. */
   private static final int AL_STOPPED = 4116;
   private static final int AL_PLAYING = 4114;
   private static final int AL_PAUSED = 4115;

   public static final int BUFFER_DURATION_SECONDS = 1;

   private final AtomicBoolean initialized = new AtomicBoolean(true);

   /**
    * Vanilla recomputed this from the stream's format on every attach and used it to size
    * each queued PCM block. With no queue there is nothing to size, but it is still
    * computed so the {@link SoundFormat} shim is exercised on every streamed sound rather
    * than left entirely unreferenced.
    */
   private int streamingBufferSize = 16384;

   @Nullable
   private AudioStream stream;

   /**
    * Always succeeds.
    *
    * <p>Vanilla returned {@code null} here when {@code alGenSources} failed, a path
    * {@code ChannelAccess.createHandle} turns into a future completed with null and
    * {@code SoundEngine.play} handles explicitly at line 369. Handing back a real (silent)
    * channel is the better of the two: the sound stays represented in
    * {@code instanceToChannel} for its full lifetime and is then reaped through the normal
    * "finished playing" path, instead of being dropped from the bookkeeping immediately.
    */
   static Channel create() {
      return new Channel();
   }

   private Channel() {
   }

   public void destroy() {
      if (this.initialized.compareAndSet(true, false) && this.stream != null) {
         try {
            this.stream.close();
         } catch (IOException var1) {
            LOGGER.error("Failed to close audio stream", var1);
         }

         this.stream = null;
      }
   }

   /** No-op: there is no source to start. */
   public void play() {
   }

   private int getState() {
      return AL_STOPPED;
   }

   /** No-op: {@link #getState()} is never AL_PLAYING. */
   public void pause() {
   }

   /** No-op: {@link #getState()} is never AL_PAUSED. */
   public void unpause() {
   }

   /** No-op: a stopped channel is already stopped. */
   public void stop() {
   }

   /** Always {@code false} -- see the class comment for why this must not report playing. */
   public boolean playing() {
      return this.getState() == AL_PLAYING;
   }

   /** Always {@code true}, which is what drives the channel's reaping. */
   public boolean stopped() {
      return this.getState() == AL_STOPPED;
   }

   /** No-op: the position was only ever pushed into the OpenAL source. */
   public void setSelfPosition(Vec3 param0) {
   }

   /** No-op: the pitch was only ever pushed into the OpenAL source. */
   public void setPitch(float param0) {
   }

   /** No-op: the loop flag was only ever pushed into the OpenAL source. */
   public void setLooping(boolean param0) {
   }

   /** No-op: the volume was only ever pushed into the OpenAL source. */
   public void setVolume(float param0) {
   }

   /** No-op: OpenAL computed attenuation from the source model. */
   public void disableAttenuation() {
   }

   /** No-op: OpenAL computed attenuation from the source model. */
   public void linearAttenuation(float param0) {
   }

   /** No-op: the relative flag was only ever pushed into the OpenAL source. */
   public void setRelative(boolean param0) {
   }

   /** No-op: {@link SoundBuffer#getAlBuffer()} is always empty, so there is never an id. */
   public void attachStaticBuffer(SoundBuffer param0) {
   }

   /**
    * Records the stream so {@link #destroy()} can close it, and still computes the buffer
    * size.
    *
    * <p>Vanilla then called {@code pumpBuffers(4)}; that is skipped because pumping only
    * existed to queue decoded PCM into the source. The size calculation is kept because it
    * is the one piece of arithmetic here driven by real format data rather than by OpenAL,
    * so it exercises the {@link SoundFormat} shim on every streamed sound.
    */
   public void attachBufferStream(AudioStream param0) {
      this.stream = param0;
      SoundFormat var1 = param0.getFormat();
      this.streamingBufferSize = calculateBufferSize(var1, 1);
   }

   private static int calculateBufferSize(SoundFormat param0, int param1) {
      return (int)(param1 * param0.getSampleSizeInBits() / 8.0F * param0.getChannels() * param0.getSampleRate());
   }

   /** No-op: no buffers are ever queued, so there is nothing to re-queue. */
   public void updateStream() {
   }
}