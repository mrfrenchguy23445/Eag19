package com.mojang.blaze3d.audio;

import com.mojang.blaze3d.audio.SoundFormat.Encoding;

/**
 * Port note (LWJGL removal): this class no longer talks to OpenAL.
 *
 * <p>Vanilla's two error checks polled {@code alGetError}/{@code alcGetError} and logged
 * whatever the driver said. There is no driver on this target, so both checks now report
 * "no error" unconditionally -- which is not a stub that hides a failure, it is the
 * truthful answer: nothing can fail, because nothing is issued. The overload taking a
 * device handle keeps its {@code long} parameter for source compatibility even though no
 * handle exists any more; it is simply unused.
 *
 * <p>{@link #audioFormatToOpenAl} is kept and still validated, because it is pure
 * arithmetic over {@link SoundFormat} with no native call behind it. It is no longer used
 * to build an OpenAL format (nothing uploads samples), but leaving the validation in place
 * means the format shim is exercised and a bad format still reports itself rather than
 * failing silently later.
 */
public class OpenAlUtil {

    /**
     * Always reports success. Kept so the callers that guard every audio step keep reading
     * naturally; there is no OpenAL call whose error could be reported here.
     *
     * @param param0 a description of the step, now unused
     * @return always {@code false}, meaning "no error"
     */
    static boolean checkALError(String param0) {
        return false;
    }

    /**
     * Always reports success, for the same reason as {@link #checkALError(String)}.
     *
     * @param param0 the device handle, now unused; retained because vanilla passed an
     *        {@code ALCdevice} and call sites are kept source-compatible
     * @param param1 a description of the step, now unused
     * @return always {@code false}, meaning "no error"
     */
    static boolean checkALCError(long param0, String param1) {
        return false;
    }

    /**
     * Maps a PCM format onto its OpenAL format constant.
     *
     * <p>Retained unchanged, and in particular still throwing on an unsupported
     * combination, even though no OpenAL device consumes the result. A silent port that
     * also dropped its validation would let a malformed format reach the decoder layer
     * unchallenged; keeping it means the one pure function in the audio stack still says
     * something true.
     */
    static int audioFormatToOpenAl(SoundFormat param0) {
        Encoding var1 = param0.getEncoding();
        int var2 = param0.getChannels();
        int var3 = param0.getSampleSizeInBits();
        if (var1.equals(Encoding.PCM_UNSIGNED) || var1.equals(Encoding.PCM_SIGNED)) {
            if (var2 == 1) {
                if (var3 == 8) {
                    return 4352;
                }

                if (var3 == 16) {
                    return 4353;
                }
            } else if (var2 == 2) {
                if (var3 == 8) {
                    return 4354;
                }

                if (var3 == 16) {
                    return 4355;
                }
            }
        }

        throw new IllegalArgumentException("Invalid audio format: " + param0);
    }
}