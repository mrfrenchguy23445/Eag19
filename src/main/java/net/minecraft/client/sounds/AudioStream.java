package net.minecraft.client.sounds;

import com.mojang.blaze3d.audio.SoundFormat;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface AudioStream extends Closeable {
   SoundFormat getFormat();

   ByteBuffer read(int var1) throws IOException;
}
