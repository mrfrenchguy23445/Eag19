package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum Mirror implements StringRepresentable {
   NONE("none", OctahedralGroup.IDENTITY),
   LEFT_RIGHT("left_right", OctahedralGroup.INVERT_Z),
   FRONT_BACK("front_back", OctahedralGroup.INVERT_X);

   public static final Codec<Mirror> CODEC = StringRepresentable.fromEnum(Mirror::values);
   private final String id;
   private final Component symbol;
   private final OctahedralGroup rotation;

   Mirror(String param2, OctahedralGroup param3) {
      this.id = param2;
      this.symbol = Component.translatable("mirror." + param2);
      this.rotation = param3;
   }

   public int mirror(int param0, int param1) {
      int var3 = param1 / 2;
      int var4 = param0 > var3 ? param0 - param1 : param0;
      switch (this) {
         case FRONT_BACK:
            return (param1 - var4) % param1;
         case LEFT_RIGHT:
            return (var3 - var4 + param1) % param1;
         default:
            return param0;
      }
   }

   public Rotation getRotation(Direction param0) {
      Direction.Axis var2 = param0.getAxis();
      return (this != LEFT_RIGHT || var2 != Direction.Axis.Z) && (this != FRONT_BACK || var2 != Direction.Axis.X) ? Rotation.NONE : Rotation.CLOCKWISE_180;
   }

   public Direction mirror(Direction param0) {
      if (this == FRONT_BACK && param0.getAxis() == Direction.Axis.X) {
         return param0.getOpposite();
      } else {
         return this == LEFT_RIGHT && param0.getAxis() == Direction.Axis.Z ? param0.getOpposite() : param0;
      }
   }

   public OctahedralGroup rotation() {
      return this.rotation;
   }

   public Component symbol() {
      return this.symbol;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }
}
