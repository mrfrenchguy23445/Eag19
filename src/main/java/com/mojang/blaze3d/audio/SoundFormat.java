package com.mojang.blaze3d.audio;

/**
 * The port's replacement for {@code javax.sound.sampled.AudioFormat}.
 *
 * WHY NOT THE JDK TYPE
 * ====================
 *
 * {@code javax.sound.sampled.AudioFormat} cannot be supplied to the WASM-GC TeaVM target
 * at all. The classlib's {@code META-INF/teavm.properties} excludes exactly one javax
 * package -- {@code javax.xml} -- so the obvious workaround, shipping a class of that name
 * the way the java.* shims do, does not apply: there is no rule that would map a stub onto
 * the real name, and a class packaged as {@code javax.sound.sampled.AudioFormat} is never
 * read. This was tried, as a file in jdk-shims/src/javax/sound/sampled/, and it was
 * reported missing on every link. It has been removed rather than left in place, because a
 * stub that provably does not resolve is worse than no stub: it reads as though the type is
 * handled.
 *
 * The general reason is the one documented at length in jdk-shims/build.gradle.kts: a
 * {@code javax.*} or {@code sun.*} package is answered by the bootstrap loader out of the
 * module image before the classpath is consulted, so the jar entry is simply never read.
 *
 * WHAT THIS IS
 * ============
 *
 * The five things this port ever did with an AudioFormat, and nothing else:
 *
 *   new AudioFormat(float, int, int, boolean, boolean)  -- OggAudioStream, once, for a
 *       fixed 16-bit stereo 44.1 kHz descriptor
 *   getSampleRate()          -- Channel.calculateBufferSize
 *   getSampleSizeInBits()    -- Channel.calculateBufferSize, OpenAlUtil.audioFormatToOpenAl
 *   getChannels()            -- Channel.calculateBufferSize, OpenAlUtil.audioFormatToOpenAl
 *   getEncoding()            -- OpenAlUtil.audioFormatToOpenAl, which distinguishes
 *                               PCM_SIGNED from PCM_UNSIGNED
 *
 * The five-argument constructor keeps AudioFormat's own parameter order and meaning, so
 * the call site reads the same as it did before the swap: rate, bits, channels, signed,
 * big-endian. That is deliberate -- it is the one place a reader could otherwise be misled
 * into thinking a different format was substituted.
 *
 * Only one format is ever constructed in this port. OggAudioStream reports the silent
 * 44.1 kHz / 16-bit / stereo format it has always reported, and LoopingAudioStream passes
 * whatever its delegate reports. So the interesting behaviour is not in this class but in
 * the two places that consume it, both of which kept their arithmetic unchanged.
 *
 * NOT a general-purpose replacement. It is not a superset of AudioFormat and does not try
 * to be: it holds no frame size, no frame rate, no encoding object, and supports no frame
 * length. Adding those speculatively would be the same mistake as stubbing a wider JDK
 * class -- it would type-check against nothing and mislead the next reader about what this
 * port actually uses.
 */
public class SoundFormat {

   /**
    * The subset of {@code AudioFormat.Encoding} that OpenAlUtil distinguishes. ULAW and
    * ALAW are carried because they are part of the real enum and because the JVM's
    * five-argument constructor can be asked for them, even though nothing here does.
    */
   public enum Encoding {
      PCM_SIGNED,
      PCM_UNSIGNED,
      ULAW,
      ALAW
   }

   private final Encoding encoding;
   private final float sampleRate;
   private final int sampleSizeInBits;
   private final int channels;
   private final boolean bigEndian;

   /**
    * Same order and same meaning as {@code AudioFormat(float, int, int, boolean, boolean)}:
    * frames per second, bits per sample, channel count, signedness, byte order.
    *
    * <p>The signedness flag becomes the encoding, which is how the JDK derives it too: a
    * signed format is PCM_SIGNED and an unsigned one PCM_UNSIGNED. Storing it as an enum
    * rather than re-deriving it on every getEncoding() call means audioFormatToOpenAl's
    * two equals() comparisons are exact, as they are upstream.
    */
   public SoundFormat(float param0, int param1, int param2, boolean param3, boolean param4) {
      this.sampleRate = param0;
      this.sampleSizeInBits = param1;
      this.channels = param2;
      this.encoding = param3 ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED;
      this.bigEndian = param4;
   }

   public Encoding getEncoding() {
      return this.encoding;
   }

   public float getSampleRate() {
      return this.sampleRate;
   }

   public int getSampleSizeInBits() {
      return this.sampleSizeInBits;
   }

   public int getChannels() {
      return this.channels;
   }

   public boolean isBigEndian() {
      return this.bigEndian;
   }
}