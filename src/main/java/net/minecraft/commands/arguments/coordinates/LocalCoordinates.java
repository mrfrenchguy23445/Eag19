package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class LocalCoordinates implements Coordinates {
   public static final char PREFIX_LOCAL_COORDINATE = '^';
   private final double left;
   private final double up;
   private final double forwards;

   public LocalCoordinates(double param0, double param2, double param4) {
      this.left = param0;
      this.up = param2;
      this.forwards = param4;
   }

   @Override
   public Vec3 getPosition(CommandSourceStack param0) {
      Vec2 var2 = param0.getRotation();
      Vec3 var3 = param0.getAnchor().apply(param0);
      float var4 = Mth.cos((var2.y + 90.0F) * (float) (Math.PI / 180.0));
      float var5 = Mth.sin((var2.y + 90.0F) * (float) (Math.PI / 180.0));
      float var6 = Mth.cos(-var2.x * (float) (Math.PI / 180.0));
      float var7 = Mth.sin(-var2.x * (float) (Math.PI / 180.0));
      float var8 = Mth.cos((-var2.x + 90.0F) * (float) (Math.PI / 180.0));
      float var9 = Mth.sin((-var2.x + 90.0F) * (float) (Math.PI / 180.0));
      Vec3 var10 = new Vec3(var4 * var6, var7, var5 * var6);
      Vec3 var11 = new Vec3(var4 * var8, var9, var5 * var8);
      Vec3 var12 = var10.cross(var11).scale(-1.0);
      double var13 = var10.x * this.forwards + var11.x * this.up + var12.x * this.left;
      double var15 = var10.y * this.forwards + var11.y * this.up + var12.y * this.left;
      double var17 = var10.z * this.forwards + var11.z * this.up + var12.z * this.left;
      return new Vec3(var3.x + var13, var3.y + var15, var3.z + var17);
   }

   @Override
   public Vec2 getRotation(CommandSourceStack param0) {
      return Vec2.ZERO;
   }

   @Override
   public boolean isXRelative() {
      return true;
   }

   @Override
   public boolean isYRelative() {
      return true;
   }

   @Override
   public boolean isZRelative() {
      return true;
   }

   public static LocalCoordinates parse(StringReader param0) throws CommandSyntaxException {
      int var1 = param0.getCursor();
      double var2 = readDouble(param0, var1);
      if (param0.canRead() && param0.peek() == ' ') {
         param0.skip();
         double var4 = readDouble(param0, var1);
         if (param0.canRead() && param0.peek() == ' ') {
            param0.skip();
            double var6 = readDouble(param0, var1);
            return new LocalCoordinates(var2, var4, var6);
         } else {
            param0.setCursor(var1);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(param0);
         }
      } else {
         param0.setCursor(var1);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(param0);
      }
   }

   private static double readDouble(StringReader param0, int param1) throws CommandSyntaxException {
      if (!param0.canRead()) {
         throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(param0);
      } else if (param0.peek() != '^') {
         param0.setCursor(param1);
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(param0);
      } else {
         param0.skip();
         return param0.canRead() && param0.peek() != ' ' ? param0.readDouble() : 0.0;
      }
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return !(param0 instanceof LocalCoordinates var2) ? false : this.left == var2.left && this.up == var2.up && this.forwards == var2.forwards;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.left, this.up, this.forwards);
   }
}
