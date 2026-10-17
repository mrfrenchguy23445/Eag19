package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public class WorldCoordinate {
   private static final char PREFIX_RELATIVE = '~';
   public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.double"));
   public static final SimpleCommandExceptionType ERROR_EXPECTED_INT = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.int"));
   private final boolean relative;
   private final double value;

   public WorldCoordinate(boolean param0, double param1) {
      this.relative = param0;
      this.value = param1;
   }

   public double get(double param0) {
      return this.relative ? this.value + param0 : this.value;
   }

   public static WorldCoordinate parseDouble(StringReader param0, boolean param1) throws CommandSyntaxException {
      if (param0.canRead() && param0.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(param0);
      }

      if (!param0.canRead()) {
         throw ERROR_EXPECTED_DOUBLE.createWithContext(param0);
      }

      boolean var2 = isRelative(param0);
      int var3 = param0.getCursor();
      double var4 = param0.canRead() && param0.peek() != ' ' ? param0.readDouble() : 0.0;
      String var6 = param0.getString().substring(var3, param0.getCursor());
      if (var2 && var6.isEmpty()) {
         return new WorldCoordinate(true, 0.0);
      }

      if (!var6.contains(".") && !var2 && param1) {
         var4 += 0.5;
      }

      return new WorldCoordinate(var2, var4);
   }

   public static WorldCoordinate parseInt(StringReader param0) throws CommandSyntaxException {
      if (param0.canRead() && param0.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(param0);
      }

      if (!param0.canRead()) {
         throw ERROR_EXPECTED_INT.createWithContext(param0);
      }

      boolean var1 = isRelative(param0);
      double var2;
      if (param0.canRead() && param0.peek() != ' ') {
         var2 = var1 ? param0.readDouble() : param0.readInt();
      } else {
         var2 = 0.0;
      }

      return new WorldCoordinate(var1, var2);
   }

   public static boolean isRelative(StringReader param0) {
      boolean var1;
      if (param0.peek() == '~') {
         var1 = true;
         param0.skip();
      } else {
         var1 = false;
      }

      return var1;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (!(param0 instanceof WorldCoordinate var2)) {
         return false;
      } else {
         return this.relative != var2.relative ? false : Double.compare(var2.value, this.value) == 0;
      }
   }

   @Override
   public int hashCode() {
      int var1 = this.relative ? 1 : 0;
      long var2 = Double.doubleToLongBits(this.value);
      return 31 * var1 + (int)(var2 ^ var2 >>> 32);
   }

   public boolean isRelative() {
      return this.relative;
   }
}
