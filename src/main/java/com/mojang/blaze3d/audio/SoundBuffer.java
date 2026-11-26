package com.mojang.blaze3d.audio;

import java.nio.ByteBuffer;
import java.util.OptionalInt;
import javax.annotation.Nullable;

/**
 * Port note (LWJGL removal): no longer allocates or owns an OpenAL buffer.
 *
 * <p>Vanilla lazily created an {@code ALuint} on the first {@link #getAlBuffer()} call,
 * uploaded the samples with {@code alBufferData}, and handed the id to
 * {@code Channel.attachStaticBuffer}. Without OpenAL there is no id to hand out, so
 * {@link #getAlBuffer()} always returns {@link OptionalInt#empty()}.
 *
 * <p>That empty return is load-bearing, not incidental. Both call sites already treat it
 * as "nothing to do":
 * <ul>
 *   <li>{@code Channel.attachStaticBuffer} is {@code getAlBuffer().ifPresent(...)} -- with
 *       nothing to attach, the channel is simply silent.</li>
 *   <li>{@code Channel.pumpBuffers} likewise does
 *       {@code releaseAlBuffer().ifPresent(...alSourceQueueBuffers...)}.</li>
 * </ul>
 * So the channel lifecycle, the streaming pump and {@code SoundEngine}'s "not playing
 * anymore" reaping all keep working untouched; the only thing missing is the sound.
 *
 * <p>The sample {@link ByteBuffer} is dropped in the constructor rather than retained.
 * Vanilla held it only until {@code alBufferData} consumed it, and with no upload there is
 * nothing to wait for -- holding megabytes of decoded PCM for a buffer that will never be
 * submitted would be strictly worse than discarding it.
 */
public class SoundBuffer {
   @Nullable
   private ByteBuffer data;
   private final SoundFormat format;
   private boolean hasAlBuffer;
   private int alBuffer;

   public SoundBuffer(ByteBuffer param0, SoundFormat param1) {
      this.data = param0;
      this.format = param1;
      // Nothing will ever read this back. Dropping it here is what keeps a decoded sound
      // from being retained for the lifetime of the buffer cache entry -- the cache holds
      // one SoundBuffer per sound for the whole session (SoundBufferLibrary.cache), so
      // retaining samples would mean the audio of every loaded sound staying resident.
      this.data = null;
   }

   /**
    * Always empty: there is no OpenAL buffer to hand to a source.
    *
    * <p>Kept as a method rather than deleted because the callers' control flow depends on
    * the {@link OptionalInt} -- see the class comment -- and because returning an empty
    * Optional is exactly what vanilla already did on its two failure paths
    * (buffer creation or upload error).
    */
   OptionalInt getAlBuffer() {
      return OptionalInt.empty();
   }

   /** No-op: nothing was ever allocated. */
   public void discardAlBuffer() {
      this.hasAlBuffer = false;
      this.alBuffer = 0;
   }

   /** Always empty, for the same reason as {@link #getAlBuffer()}. */
   public OptionalInt releaseAlBuffer() {
      this.hasAlBuffer = false;
      return OptionalInt.empty();
   }

   /** Unused; retained so the field stays documented as reachable state. */
   SoundFormat format() {
      return this.format;
   }
}