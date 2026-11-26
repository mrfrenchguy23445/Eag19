package com.mojang.blaze3d.platform;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * A display mode.
 *
 * <p>The two GLFW constructors upstream ({@code VideoMode(GLFWVidMode.Buffer)} and
 * {@code VideoMode(GLFWVidMode)}) are removed along with the monitor enumeration that used them: the
 * Eaglercraft build has no GLFW and no way to enumerate modes, so the only way a mode is ever
 * created here is the explicit six-argument one, from a saved options string or from
 * {@link Monitor#refreshVideoModes()}.
 */
public final class VideoMode {
   private final int width;
   private final int height;
   private final int redBits;
   private final int greenBits;
   private final int blueBits;
   private final int refreshRate;
   private static final Pattern PATTERN = Pattern.compile("(\\d+)x(\\d+)(?:@(\\d+)(?::(\\d+))?)?");

   public VideoMode(int param0, int param1, int param2, int param3, int param4, int param5) {
      this.width = param0;
      this.height = param1;
      this.redBits = param2;
      this.greenBits = param3;
      this.blueBits = param4;
      this.refreshRate = param5;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public int getRedBits() {
      return this.redBits;
   }

   public int getGreenBits() {
      return this.greenBits;
   }

   public int getBlueBits() {
      return this.blueBits;
   }

   public int getRefreshRate() {
      return this.refreshRate;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (param0 != null && this.getClass() == param0.getClass()) {
         VideoMode var2 = (VideoMode)param0;
         return this.width == var2.width
            && this.height == var2.height
            && this.redBits == var2.redBits
            && this.greenBits == var2.greenBits
            && this.blueBits == var2.blueBits
            && this.refreshRate == var2.refreshRate;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.width, this.height, this.redBits, this.greenBits, this.blueBits, this.refreshRate);
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "%sx%s@%s (%sbit)", this.width, this.height, this.refreshRate, this.redBits + this.greenBits + this.blueBits);
   }

   public static Optional<VideoMode> read(@Nullable String param0) {
      if (param0 == null) {
         return Optional.empty();
      }

      try {
         Matcher var1 = PATTERN.matcher(param0);
         if (var1.matches()) {
            int var2 = Integer.parseInt(var1.group(1));
            int var3 = Integer.parseInt(var1.group(2));
            String var4 = var1.group(3);
            int var5;
            if (var4 == null) {
               var5 = 60;
            } else {
               var5 = Integer.parseInt(var4);
            }

            String var6 = var1.group(4);
            int var7;
            if (var6 == null) {
               var7 = 24;
            } else {
               var7 = Integer.parseInt(var6);
            }

            int var8 = var7 / 3;
            return Optional.of(new VideoMode(var2, var3, var8, var8, var8, var5));
         }
      } catch (Exception var9) {
      }

      return Optional.empty();
   }

   public String write() {
      return String.format(Locale.ROOT, "%sx%s@%s:%s", this.width, this.height, this.refreshRate, this.redBits + this.greenBits + this.blueBits);
   }
}
