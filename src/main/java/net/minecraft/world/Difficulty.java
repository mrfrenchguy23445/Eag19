package net.minecraft.world;

import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum Difficulty implements StringRepresentable {
   PEACEFUL(0, "peaceful"),
   EASY(1, "easy"),
   NORMAL(2, "normal"),
   HARD(3, "hard");

   public static final StringRepresentable.EnumCodec<Difficulty> CODEC = StringRepresentable.fromEnum(Difficulty::values);
   private static final IntFunction<Difficulty> BY_ID = ByIdMap.continuous(Difficulty::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   private final int id;
   private final String key;

   Difficulty(int param2, String param3) {
      this.id = param2;
      this.key = param3;
   }

   public int getId() {
      return this.id;
   }

   public Component getDisplayName() {
      return Component.translatable("options.difficulty." + this.key);
   }

   public Component getInfo() {
      return Component.translatable("options.difficulty." + this.key + ".info");
   }

   public static Difficulty byId(int param0) {
      return BY_ID.apply(param0);
   }

   @Nullable
   public static Difficulty byName(String param0) {
      return CODEC.byName(param0);
   }

   public String getKey() {
      return this.key;
   }

   @Override
   public String getSerializedName() {
      return this.key;
   }
}
