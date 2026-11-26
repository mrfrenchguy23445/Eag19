package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public enum Direction implements StringRepresentable {
   DOWN(0, 1, -1, "down", Direction.AxisDirection.NEGATIVE, Direction.Axis.Y, new Vec3i(0, -1, 0)),
   UP(1, 0, -1, "up", Direction.AxisDirection.POSITIVE, Direction.Axis.Y, new Vec3i(0, 1, 0)),
   NORTH(2, 3, 2, "north", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, new Vec3i(0, 0, -1)),
   SOUTH(3, 2, 0, "south", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, new Vec3i(0, 0, 1)),
   WEST(4, 5, 1, "west", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, new Vec3i(-1, 0, 0)),
   EAST(5, 4, 3, "east", Direction.AxisDirection.POSITIVE, Direction.Axis.X, new Vec3i(1, 0, 0));

   public static final StringRepresentable.EnumCodec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);
   public static final Codec<Direction> VERTICAL_CODEC = ExtraCodecs.validate(CODEC, Direction::verifyVertical);
   private final int data3d;
   private final int oppositeIndex;
   private final int data2d;
   private final String name;
   private final Direction.Axis axis;
   private final Direction.AxisDirection axisDirection;
   private final Vec3i normal;
   private static final Direction[] VALUES = values();
   private static final Direction[] BY_3D_DATA = Arrays.stream(VALUES).sorted(Comparator.comparingInt(param0 -> param0.data3d)).toArray(Direction[]::new);
   private static final Direction[] BY_2D_DATA = Arrays.stream(VALUES)
      .filter(param0 -> param0.getAxis().isHorizontal())
      .sorted(Comparator.comparingInt(param0 -> param0.data2d))
      .toArray(Direction[]::new);
   private static final Long2ObjectMap<Direction> BY_NORMAL = Arrays.stream(VALUES)
      .collect(Collectors.toMap(param0 -> new BlockPos(param0.getNormal()).asLong(), param0 -> (Direction)param0, (param0, param1) -> {
         throw new IllegalArgumentException("Duplicate keys");
      }, Long2ObjectOpenHashMap::new));

   Direction(int param2, int param3, int param4, String param5, Direction.AxisDirection param6, Direction.Axis param7, Vec3i param8) {
      this.data3d = param2;
      this.data2d = param4;
      this.oppositeIndex = param3;
      this.name = param5;
      this.axis = param7;
      this.axisDirection = param6;
      this.normal = param8;
   }

   public static Direction[] orderedByNearest(Entity param0) {
      float var1 = param0.getViewXRot(1.0F) * (float) (Math.PI / 180.0);
      float var2 = -param0.getViewYRot(1.0F) * (float) (Math.PI / 180.0);
      float var3 = Mth.sin(var1);
      float var4 = Mth.cos(var1);
      float var5 = Mth.sin(var2);
      float var6 = Mth.cos(var2);
      boolean var7 = var5 > 0.0F;
      boolean var8 = var3 < 0.0F;
      boolean var9 = var6 > 0.0F;
      float var10 = var7 ? var5 : -var5;
      float var11 = var8 ? -var3 : var3;
      float var12 = var9 ? var6 : -var6;
      float var13 = var10 * var4;
      float var14 = var12 * var4;
      Direction var15 = var7 ? EAST : WEST;
      Direction var16 = var8 ? UP : DOWN;
      Direction var17 = var9 ? SOUTH : NORTH;
      if (var10 > var12) {
         if (var11 > var13) {
            return makeDirectionArray(var16, var15, var17);
         } else {
            return var14 > var11 ? makeDirectionArray(var15, var17, var16) : makeDirectionArray(var15, var16, var17);
         }
      } else if (var11 > var14) {
         return makeDirectionArray(var16, var17, var15);
      } else {
         return var13 > var11 ? makeDirectionArray(var17, var15, var16) : makeDirectionArray(var17, var16, var15);
      }
   }

   private static Direction[] makeDirectionArray(Direction param0, Direction param1, Direction param2) {
      return new Direction[]{param0, param1, param2, param2.getOpposite(), param1.getOpposite(), param0.getOpposite()};
   }

   public static Direction rotate(Matrix4f param0, Direction param1) {
      Vec3i var2 = param1.getNormal();
      Vector4f var3 = param0.transform(new Vector4f(var2.getX(), var2.getY(), var2.getZ(), 0.0F));
      return getNearest(var3.x(), var3.y(), var3.z());
   }

   public static Collection<Direction> allShuffled(RandomSource param0) {
      return Util.shuffledCopy(values(), param0);
   }

   public static Stream<Direction> stream() {
      return Stream.of(VALUES);
   }

   public Quaternionf getRotation() {
      return switch (this) {
         case DOWN -> new Quaternionf().rotationX((float) Math.PI);
         case UP -> new Quaternionf();
         case NORTH -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) Math.PI);
         case SOUTH -> new Quaternionf().rotationX((float) (Math.PI / 2));
         case WEST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (Math.PI / 2));
         case EAST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (-Math.PI / 2));
      };
   }

   public int get3DDataValue() {
      return this.data3d;
   }

   public int get2DDataValue() {
      return this.data2d;
   }

   public Direction.AxisDirection getAxisDirection() {
      return this.axisDirection;
   }

   public static Direction getFacingAxis(Entity param0, Direction.Axis param1) {
      return switch (param1) {
         case X -> EAST.isFacingAngle(param0.getViewYRot(1.0F)) ? EAST : WEST;
         case Z -> SOUTH.isFacingAngle(param0.getViewYRot(1.0F)) ? SOUTH : NORTH;
         case Y -> param0.getViewXRot(1.0F) < 0.0F ? UP : DOWN;
      };
   }

   public Direction getOpposite() {
      return from3DDataValue(this.oppositeIndex);
   }

   public Direction getClockWise(Direction.Axis param0) {
      return switch (param0) {
         case X -> this != WEST && this != EAST ? this.getClockWiseX() : this;
         case Z -> this != NORTH && this != SOUTH ? this.getClockWiseZ() : this;
         case Y -> this != UP && this != DOWN ? this.getClockWise() : this;
      };
   }

   public Direction getCounterClockWise(Direction.Axis param0) {
      return switch (param0) {
         case X -> this != WEST && this != EAST ? this.getCounterClockWiseX() : this;
         case Z -> this != NORTH && this != SOUTH ? this.getCounterClockWiseZ() : this;
         case Y -> this != UP && this != DOWN ? this.getCounterClockWise() : this;
      };
   }

   public Direction getClockWise() {
      return switch (this) {
         case NORTH -> EAST;
         case SOUTH -> WEST;
         case WEST -> NORTH;
         case EAST -> SOUTH;
         default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
      };
   }

   private Direction getClockWiseX() {
      return switch (this) {
         case DOWN -> SOUTH;
         case UP -> NORTH;
         case NORTH -> DOWN;
         case SOUTH -> UP;
         default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
      };
   }

   private Direction getCounterClockWiseX() {
      return switch (this) {
         case DOWN -> NORTH;
         case UP -> SOUTH;
         case NORTH -> UP;
         case SOUTH -> DOWN;
         default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
      };
   }

   private Direction getClockWiseZ() {
      return switch (this) {
         case DOWN -> WEST;
         case UP -> EAST;
         default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
         case WEST -> UP;
         case EAST -> DOWN;
      };
   }

   private Direction getCounterClockWiseZ() {
      return switch (this) {
         case DOWN -> EAST;
         case UP -> WEST;
         default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
         case WEST -> DOWN;
         case EAST -> UP;
      };
   }

   public Direction getCounterClockWise() {
      return switch (this) {
         case NORTH -> WEST;
         case SOUTH -> EAST;
         case WEST -> SOUTH;
         case EAST -> NORTH;
         default -> throw new IllegalStateException("Unable to get CCW facing of " + this);
      };
   }

   public int getStepX() {
      return this.normal.getX();
   }

   public int getStepY() {
      return this.normal.getY();
   }

   public int getStepZ() {
      return this.normal.getZ();
   }

   public Vector3f step() {
      return new Vector3f(this.getStepX(), this.getStepY(), this.getStepZ());
   }

   public String getName() {
      return this.name;
   }

   public Direction.Axis getAxis() {
      return this.axis;
   }

   @Nullable
   public static Direction byName(@Nullable String param0) {
      return CODEC.byName(param0);
   }

   public static Direction from3DDataValue(int param0) {
      return BY_3D_DATA[Mth.abs(param0 % BY_3D_DATA.length)];
   }

   public static Direction from2DDataValue(int param0) {
      return BY_2D_DATA[Mth.abs(param0 % BY_2D_DATA.length)];
   }

   @Nullable
   public static Direction fromNormal(BlockPos param0) {
      return BY_NORMAL.get(param0.asLong());
   }

   @Nullable
   public static Direction fromNormal(int param0, int param1, int param2) {
      return BY_NORMAL.get(BlockPos.asLong(param0, param1, param2));
   }

   public static Direction fromYRot(double param0) {
      return from2DDataValue(Mth.floor(param0 / 90.0 + 0.5) & 3);
   }

   public static Direction fromAxisAndDirection(Direction.Axis param0, Direction.AxisDirection param1) {
      return switch (param0) {
         case X -> param1 == Direction.AxisDirection.POSITIVE ? EAST : WEST;
         case Z -> param1 == Direction.AxisDirection.POSITIVE ? SOUTH : NORTH;
         case Y -> param1 == Direction.AxisDirection.POSITIVE ? UP : DOWN;
      };
   }

   public float toYRot() {
      return (this.data2d & 3) * 90;
   }

   public static Direction getRandom(RandomSource param0) {
      return Util.getRandom(VALUES, param0);
   }

   public static Direction getNearest(double param0, double param2, double param4) {
      return getNearest((float)param0, (float)param2, (float)param4);
   }

   public static Direction getNearest(float param0, float param1, float param2) {
      Direction var3 = NORTH;
      float var4 = Float.MIN_VALUE;

      for (Direction var8 : VALUES) {
         float var9 = param0 * var8.normal.getX() + param1 * var8.normal.getY() + param2 * var8.normal.getZ();
         if (var9 > var4) {
            var4 = var9;
            var3 = var8;
         }
      }

      return var3;
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   private static DataResult<Direction> verifyVertical(Direction param0) {
      return param0.getAxis().isVertical() ? DataResult.success(param0) : DataResult.error(() -> "Expected a vertical direction");
   }

   public static Direction get(Direction.AxisDirection param0, Direction.Axis param1) {
      for (Direction var5 : VALUES) {
         if (var5.getAxisDirection() == param0 && var5.getAxis() == param1) {
            return var5;
         }
      }

      throw new IllegalArgumentException("No such direction: " + param0 + " " + param1);
   }

   public Vec3i getNormal() {
      return this.normal;
   }

   public boolean isFacingAngle(float param0) {
      float var2 = param0 * (float) (Math.PI / 180.0);
      float var3 = -Mth.sin(var2);
      float var4 = Mth.cos(var2);
      return this.normal.getX() * var3 + this.normal.getZ() * var4 > 0.0F;
   }

   public enum Axis implements StringRepresentable, Predicate<Direction> {
      X("x") {
         @Override
         public int choose(int param0, int param1, int param2) {
            return param0;
         }

         @Override
         public double choose(double param0, double param2, double param4) {
            return param0;
         }
      },
      Y("y") {
         @Override
         public int choose(int param0, int param1, int param2) {
            return param1;
         }

         @Override
         public double choose(double param0, double param2, double param4) {
            return param2;
         }
      },
      Z("z") {
         @Override
         public int choose(int param0, int param1, int param2) {
            return param2;
         }

         @Override
         public double choose(double param0, double param2, double param4) {
            return param4;
         }
      };

      public static final Direction.Axis[] VALUES = values();
      public static final StringRepresentable.EnumCodec<Direction.Axis> CODEC = StringRepresentable.fromEnum(Direction.Axis::values);
      private final String name;

      Axis(String param2) {
         this.name = param2;
      }

      @Nullable
      public static Direction.Axis byName(String param0) {
         return CODEC.byName(param0);
      }

      public String getName() {
         return this.name;
      }

      public boolean isVertical() {
         return this == Y;
      }

      public boolean isHorizontal() {
         return this == X || this == Z;
      }

      @Override
      public String toString() {
         return this.name;
      }

      public static Direction.Axis getRandom(RandomSource param0) {
         return Util.getRandom(VALUES, param0);
      }

      public boolean test(@Nullable Direction param0) {
         return param0 != null && param0.getAxis() == this;
      }

      public Direction.Plane getPlane() {
         return switch (this) {
            case X, Z -> Direction.Plane.HORIZONTAL;
            case Y -> Direction.Plane.VERTICAL;
         };
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public abstract int choose(int var1, int var2, int var3);

      public abstract double choose(double var1, double var3, double var5);
   }

   public enum AxisDirection {
      POSITIVE(1, "Towards positive"),
      NEGATIVE(-1, "Towards negative");

      private final int step;
      private final String name;

      AxisDirection(int param2, String param3) {
         this.step = param2;
         this.name = param3;
      }

      public int getStep() {
         return this.step;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String toString() {
         return this.name;
      }

      public Direction.AxisDirection opposite() {
         return this == POSITIVE ? NEGATIVE : POSITIVE;
      }
   }

   public enum Plane implements Iterable<Direction>, Predicate<Direction> {
      HORIZONTAL(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}, new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}),
      VERTICAL(new Direction[]{Direction.UP, Direction.DOWN}, new Direction.Axis[]{Direction.Axis.Y});

      private final Direction[] faces;
      private final Direction.Axis[] axis;

      Plane(Direction[] param2, Direction.Axis[] param3) {
         this.faces = param2;
         this.axis = param3;
      }

      public Direction getRandomDirection(RandomSource param0) {
         return Util.getRandom(this.faces, param0);
      }

      public Direction.Axis getRandomAxis(RandomSource param0) {
         return Util.getRandom(this.axis, param0);
      }

      public boolean test(@Nullable Direction param0) {
         return param0 != null && param0.getAxis().getPlane() == this;
      }

      @Override
      public Iterator<Direction> iterator() {
         return Iterators.forArray(this.faces);
      }

      public Stream<Direction> stream() {
         return Arrays.stream(this.faces);
      }

      public List<Direction> shuffledCopy(RandomSource param0) {
         return Util.shuffledCopy(this.faces, param0);
      }
   }
}
