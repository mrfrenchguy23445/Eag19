package net.minecraft.core;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.util.StringRepresentable;

public enum FrontAndTop implements StringRepresentable {
   DOWN_EAST("down_east", Direction.DOWN, Direction.EAST),
   DOWN_NORTH("down_north", Direction.DOWN, Direction.NORTH),
   DOWN_SOUTH("down_south", Direction.DOWN, Direction.SOUTH),
   DOWN_WEST("down_west", Direction.DOWN, Direction.WEST),
   UP_EAST("up_east", Direction.UP, Direction.EAST),
   UP_NORTH("up_north", Direction.UP, Direction.NORTH),
   UP_SOUTH("up_south", Direction.UP, Direction.SOUTH),
   UP_WEST("up_west", Direction.UP, Direction.WEST),
   WEST_UP("west_up", Direction.WEST, Direction.UP),
   EAST_UP("east_up", Direction.EAST, Direction.UP),
   NORTH_UP("north_up", Direction.NORTH, Direction.UP),
   SOUTH_UP("south_up", Direction.SOUTH, Direction.UP);

   private static final Int2ObjectMap<FrontAndTop> LOOKUP_TOP_FRONT = Util.make(new Int2ObjectOpenHashMap<>(values().length), param0 -> {
      for (FrontAndTop var4 : values()) {
         param0.put(lookupKey(var4.front, var4.top), var4);
      }
   });
   private final String name;
   private final Direction top;
   private final Direction front;

   private static int lookupKey(Direction param0, Direction param1) {
      return param1.ordinal() << 3 | param0.ordinal();
   }

   FrontAndTop(String param2, Direction param3, Direction param4) {
      this.name = param2;
      this.front = param3;
      this.top = param4;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public static FrontAndTop fromFrontAndTop(Direction param0, Direction param1) {
      int var2 = lookupKey(param0, param1);
      return LOOKUP_TOP_FRONT.get(var2);
   }

   public Direction front() {
      return this.front;
   }

   public Direction top() {
      return this.top;
   }
}
