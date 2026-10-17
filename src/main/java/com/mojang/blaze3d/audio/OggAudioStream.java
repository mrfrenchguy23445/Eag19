package com.mojang.blaze3d.audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import net.minecraft.client.sounds.AudioStream;

/**
 * Port note (LWJGL removal): the Ogg Vorbis decoder is refused; this is a silent stream.
 *
 * <p>Vanilla decoded Vorbis through {@code org.lwjgl.stb.STBVorbis}: push the whole file
 * through {@code stb_vorbis_push_memory}, pull frames with
 * {@code stb_vorbis_get_samples_float_interleaved}, and convert float to 16-bit PCM in
 * {@link OutputConcat}. That is a Vorbis decoder. There is no {@code stb_vorbis} to bind to
 * on this target and no Vorbis implementation in the TeaVM classlib, and writing one is a
 * project in itself (codebooks, floors, residue decoding, packet-loss handling).
 *
 * <p><b>Why this returns silence instead of throwing.</b> The title screen's music is a
 * {@code SimpleSoundInstance} whose {@code shouldStream()} is true, so
 * {@code SoundBufferLibrary.getStream} constructs one of these <em>on the boot path</em>.
 * Throwing would take the game down before the main menu. A stream that reports
 * end-of-data immediately gets treated as a sound that finished instantly -- see the class
 * comment on {@link Channel} for how that unwinds cleanly through {@code ChannelAccess} and
 * {@code SoundEngine}.
 *
 * <p><b>Why {@link #read(int)} returns an empty buffer rather than {@code null}.</b>
 * {@code LoopingAudioStream.read} does:
 *
 * <pre>{@code
 * ByteBuffer var2 = this.stream.read(param0);
 * if (!var2.hasRemaining()) { ...recreate and re-read... }
 * }</pre>
 *
 * A {@code null} return would NPE there. An empty buffer sends it down its "wrap round
 * again" path once, which terminates, and {@code Channel.pumpBuffers}' {@code var3 != null}
 * check stays satisfied. Empty is the only value both callers already handle.
 *
 * <p>{@link #readAll()} likewise returns empty, which {@code SoundBufferLibrary} hands
 * straight to a {@link SoundBuffer} -- whose {@code getAlBuffer()} is always empty anyway.
 *
 * <p>The 16-bit stereo 44.1 kHz format is reported so that {@link Channel}'s buffer-size
 * arithmetic runs on plausible values rather than on a stub zero.
 */
public class OggAudioStream implements AudioStream {
   private static final SoundFormat SILENT_FORMAT = new SoundFormat(44100.0F, 16, 2, true, false);
   private final InputStream input;

   public OggAudioStream(InputStream param0) throws IOException {
      this.input = param0;
   }

   /**
    * Closes the underlying resource-manager stream.
    *
    * <p>Vanilla closed only the stb_vorbis handle, trusting the caller's try-with-resources
    * for the stream. That still holds here, but closing it is harmless and closes the gap
    * for the {@code LoopingAudioStream} path, which wraps the stream in a
    * {@code NoCloseBuffer} specifically so the decoder cannot close it early -- and
    * therefore relies on {@code AudioStream.close()} to release it.
    */
   @Override
   public void close() throws IOException {
      this.input.close();
   }

   @Override
   public SoundFormat getFormat() {
      return SILENT_FORMAT;
   }

   /** Always an empty buffer -- never {@code null}, see the class comment. */
   @Override
   public ByteBuffer read(int param0) throws IOException {
      return ByteBuffer.allocate(0);
   }

   /** Always empty; consumed by {@code SoundBufferLibrary.getCompleteBuffer}. */
   public ByteBuffer readAll() throws IOException {
      return ByteBuffer.allocate(0);
   }
}