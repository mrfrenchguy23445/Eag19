package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class LongArgumentInfo implements ArgumentTypeInfo<LongArgumentType, LongArgumentInfo.Template> {
   public void serializeToNetwork(LongArgumentInfo.Template param0, FriendlyByteBuf param1) {
      boolean var3 = param0.min != Long.MIN_VALUE;
      boolean var4 = param0.max != Long.MAX_VALUE;
      param1.writeByte(ArgumentUtils.createNumberFlags(var3, var4));
      if (var3) {
         param1.writeLong(param0.min);
      }

      if (var4) {
         param1.writeLong(param0.max);
      }
   }

   public LongArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf param0) {
      byte var2 = param0.readByte();
      long var3 = ArgumentUtils.numberHasMin(var2) ? param0.readLong() : Long.MIN_VALUE;
      long var5 = ArgumentUtils.numberHasMax(var2) ? param0.readLong() : Long.MAX_VALUE;
      return new LongArgumentInfo.Template(var3, var5);
   }

   public void serializeToJson(LongArgumentInfo.Template param0, JsonObject param1) {
      if (param0.min != Long.MIN_VALUE) {
         param1.addProperty("min", param0.min);
      }

      if (param0.max != Long.MAX_VALUE) {
         param1.addProperty("max", param0.max);
      }
   }

   public LongArgumentInfo.Template unpack(LongArgumentType param0) {
      return new LongArgumentInfo.Template(param0.getMinimum(), param0.getMaximum());
   }

   public final class Template implements ArgumentTypeInfo.Template<LongArgumentType> {
      final long min;
      final long max;

      Template(long param1, long param3) {
         this.min = param1;
         this.max = param3;
      }

      public LongArgumentType instantiate(CommandBuildContext param0) {
         return LongArgumentType.longArg(this.min, this.max);
      }

      @Override
      public ArgumentTypeInfo<LongArgumentType, ?> type() {
         return LongArgumentInfo.this;
      }
   }
}
