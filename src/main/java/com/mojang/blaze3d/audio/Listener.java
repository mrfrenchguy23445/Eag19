package com.mojang.blaze3d.audio;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Port note (LWJGL removal): the OpenAL listener calls are gone; the state they carried is
 * not.
 *
 * <p>Vanilla pushed the listener position/orientation/gain into OpenAL with
 * {@code alListener3f}/{@code alListenerfv}/{@code alListenerf}. Those were pure side
 * effects -- every one of them also wrote a Java field that the rest of Minecraft reads.
 * The fields survive, the pushes do not.
 *
 * <p>Keeping the state matters: {@code SoundEngine} uses {@link #getListenerPosition()} for
 * its distance cull and {@link #getGain()} to skip sounds entirely when master volume is
 * zero (line 361). If these returned defaults, the cull would reject every positional
 * sound as infinitely far away, so this is not inert bookkeeping.
 *
 * <p>{@link #setListenerOrientation} has no counterpart to keep and becomes a no-op; no
 * caller reads the orientation back.
 */
public class Listener {
   private float gain = 1.0F;
   private Vec3 position = Vec3.ZERO;

   public void setListenerPosition(Vec3 param0) {
      this.position = param0;
   }

   public Vec3 getListenerPosition() {
      return this.position;
   }

   /** No-op: the orientation was only ever read by OpenAL. */
   public void setListenerOrientation(Vector3f param0, Vector3f param1) {
   }

   public void setGain(float param0) {
      this.gain = param0;
   }

   public float getGain() {
      return this.gain;
   }

   public void reset() {
      this.setListenerPosition(Vec3.ZERO);
      this.setListenerOrientation(new Vector3f(0.0F, 0.0F, -1.0F), new Vector3f(0.0F, 1.0F, 0.0F));
   }
}