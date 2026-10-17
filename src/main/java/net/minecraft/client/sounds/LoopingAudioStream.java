package net.minecraft.client.sounds;

import com.mojang.blaze3d.audio.SoundFormat;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class LoopingAudioStream implements AudioStream {
   private final LoopingAudioStream.AudioStreamProvider provider;
   private AudioStream stream;
   private final BufferedInputStream bufferedInputStream;

   public LoopingAudioStream(LoopingAudioStream.AudioStreamProvider param0, InputStream param1) throws IOException {
      this.provider = param0;
      this.bufferedInputStream = new BufferedInputStream(param1);
      this.bufferedInputStream.mark(Integer.MAX_VALUE);
      this.stream = param0.create(new LoopingAudioStream.NoCloseBuffer(this.bufferedInputStream));
   }

   @Override
   public SoundFormat getFormat() {
      return this.stream.getFormat();
   }

   @Override
   public ByteBuffer read(int param0) throws IOException {
      ByteBuffer var2 = this.stream.read(param0);
      if (!var2.hasRemaining()) {
         this.stream.close();
         this.bufferedInputStream.reset();
         this.stream = this.provider.create(new LoopingAudioStream.NoCloseBuffer(this.bufferedInputStream));
         var2 = this.stream.read(param0);
      }

      return var2;
   }

   @Override
   public void close() throws IOException {
      this.stream.close();
      this.bufferedInputStream.close();
   }

   @FunctionalInterface
   public interface AudioStreamProvider {
      AudioStream create(InputStream var1) throws IOException;
   }

   static class NoCloseBuffer extends FilterInputStream {
      NoCloseBuffer(InputStream param0) {
         super(param0);
      }

      @Override
      public void close() {
      }
   }
}
