package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;

public record SavedTick<T>(T type, BlockPos pos, int delay, TickPriority priority) {
   private static final String TAG_ID = "i";
   private static final String TAG_X = "x";
   private static final String TAG_Y = "y";
   private static final String TAG_Z = "z";
   private static final String TAG_DELAY = "t";
   private static final String TAG_PRIORITY = "p";
   public static final Strategy<SavedTick<?>> UNIQUE_TICK_HASH = new Strategy<SavedTick<?>>() {
      public int hashCode(SavedTick<?> param0) {
         return 31 * param0.pos().hashCode() + param0.type().hashCode();
      }

      public boolean equals(@Nullable SavedTick<?> param0, @Nullable SavedTick<?> param1) {
         if (param0 == param1) {
            return true;
         } else {
            return param0 != null && param1 != null ? param0.type() == param1.type() && param0.pos().equals(param1.pos()) : false;
         }
      }
   };

   public static <T> void loadTickList(ListTag param0, Function<String, Optional<T>> param1, ChunkPos param2, Consumer<SavedTick<T>> param3) {
      long var4 = param2.toLong();

      for (int var6 = 0; var6 < param0.size(); var6++) {
         CompoundTag var7 = param0.getCompound(var6);
         loadTick(var7, param1).ifPresent(l_param3 -> {
            if (ChunkPos.asLong(l_param3.pos()) == var4) {
               param3.accept((SavedTick<T>)l_param3);
            }
         });
      }
   }

   public static <T> Optional<SavedTick<T>> loadTick(CompoundTag param0, Function<String, Optional<T>> param1) {
      return param1.apply(param0.getString("i")).map(l_param1 -> {
         BlockPos l_var2 = new BlockPos(param0.getInt("x"), param0.getInt("y"), param0.getInt("z"));
         return new SavedTick<>((T)l_param1, l_var2, param0.getInt("t"), TickPriority.byValue(param0.getInt("p")));
      });
   }

   private static CompoundTag saveTick(String param0, BlockPos param1, int param2, TickPriority param3) {
      CompoundTag var4 = new CompoundTag();
      var4.putString("i", param0);
      var4.putInt("x", param1.getX());
      var4.putInt("y", param1.getY());
      var4.putInt("z", param1.getZ());
      var4.putInt("t", param2);
      var4.putInt("p", param3.getValue());
      return var4;
   }

   public static <T> CompoundTag saveTick(ScheduledTick<T> param0, Function<T, String> param1, long param2) {
      return saveTick(param1.apply(param0.type()), param0.pos(), (int)(param0.triggerTick() - param2), param0.priority());
   }

   public CompoundTag save(Function<T, String> param0) {
      return saveTick(param0.apply(this.type), this.pos, this.delay, this.priority);
   }

   public ScheduledTick<T> unpack(long param0, long param2) {
      return new ScheduledTick<>(this.type, this.pos, param0 + this.delay, this.priority, param2);
   }

   public static <T> SavedTick<T> probe(T param0, BlockPos param1) {
      return new SavedTick<>(param0, param1, 0, TickPriority.NORMAL);
   }
}
