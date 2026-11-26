package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.StringUtils;

public class FilterMask {
   public static final Codec<FilterMask> CODEC = StringRepresentable.fromEnum(FilterMask.Type::values).dispatch(FilterMask::type, FilterMask.Type::codec);
   public static final FilterMask FULLY_FILTERED = new FilterMask(new BitSet(0), FilterMask.Type.FULLY_FILTERED);
   public static final FilterMask PASS_THROUGH = new FilterMask(new BitSet(0), FilterMask.Type.PASS_THROUGH);
   public static final Style FILTERED_STYLE = Style.EMPTY
      .withColor(ChatFormatting.DARK_GRAY)
      .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.filtered")));
   static final Codec<FilterMask> PASS_THROUGH_CODEC = Codec.unit(PASS_THROUGH);
   static final Codec<FilterMask> FULLY_FILTERED_CODEC = Codec.unit(FULLY_FILTERED);
   static final Codec<FilterMask> PARTIALLY_FILTERED_CODEC = ExtraCodecs.BIT_SET.xmap(FilterMask::new, FilterMask::mask);
   private static final char HASH = '#';
   private final BitSet mask;
   private final FilterMask.Type type;

   private FilterMask(BitSet param0, FilterMask.Type param1) {
      this.mask = param0;
      this.type = param1;
   }

   private FilterMask(BitSet param0) {
      this.mask = param0;
      this.type = FilterMask.Type.PARTIALLY_FILTERED;
   }

   public FilterMask(int param0) {
      this(new BitSet(param0), FilterMask.Type.PARTIALLY_FILTERED);
   }

   private FilterMask.Type type() {
      return this.type;
   }

   private BitSet mask() {
      return this.mask;
   }

   public static FilterMask read(FriendlyByteBuf param0) {
      FilterMask.Type var1 = param0.readEnum(FilterMask.Type.class);

      return switch (var1) {
         case PASS_THROUGH -> PASS_THROUGH;
         case FULLY_FILTERED -> FULLY_FILTERED;
         case PARTIALLY_FILTERED -> new FilterMask(param0.readBitSet(), FilterMask.Type.PARTIALLY_FILTERED);
      };
   }

   public static void write(FriendlyByteBuf param0, FilterMask param1) {
      param0.writeEnum(param1.type);
      if (param1.type == FilterMask.Type.PARTIALLY_FILTERED) {
         param0.writeBitSet(param1.mask);
      }
   }

   public void setFiltered(int param0) {
      this.mask.set(param0);
   }

   @Nullable
   public String apply(String param0) {
      return switch (this.type) {
         case PASS_THROUGH -> param0;
         case FULLY_FILTERED -> null;
         case PARTIALLY_FILTERED -> {
            char[] var2 = param0.toCharArray();

            for (int var3 = 0; var3 < var2.length && var3 < this.mask.length(); var3++) {
               if (this.mask.get(var3)) {
                  var2[var3] = '#';
               }
            }

            yield new String(var2);
         }
      };
   }

   @Nullable
   public Component applyWithFormatting(String param0) {
      return switch (this.type) {
         case PASS_THROUGH -> Component.literal(param0);
         case FULLY_FILTERED -> null;
         case PARTIALLY_FILTERED -> {
            MutableComponent var2 = Component.empty();
            int var3 = 0;
            boolean var4 = this.mask.get(0);

            while (true) {
               int var5 = var4 ? this.mask.nextClearBit(var3) : this.mask.nextSetBit(var3);
               var5 = var5 < 0 ? param0.length() : var5;
               if (var5 == var3) {
                  yield var2;
               }

               if (var4) {
                  var2.append(Component.literal(StringUtils.repeat('#', var5 - var3)).withStyle(FILTERED_STYLE));
               } else {
                  var2.append(param0.substring(var3, var5));
               }

               var4 = !var4;
               var3 = var5;
            }
         }
      };
   }

   public boolean isEmpty() {
      return this.type == FilterMask.Type.PASS_THROUGH;
   }

   public boolean isFullyFiltered() {
      return this.type == FilterMask.Type.FULLY_FILTERED;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (param0 != null && this.getClass() == param0.getClass()) {
         FilterMask var2 = (FilterMask)param0;
         return this.mask.equals(var2.mask) && this.type == var2.type;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int var1 = this.mask.hashCode();
      return 31 * var1 + this.type.hashCode();
   }

   enum Type implements StringRepresentable {
      PASS_THROUGH("pass_through", () -> FilterMask.PASS_THROUGH_CODEC),
      FULLY_FILTERED("fully_filtered", () -> FilterMask.FULLY_FILTERED_CODEC),
      PARTIALLY_FILTERED("partially_filtered", () -> FilterMask.PARTIALLY_FILTERED_CODEC);

      private final String serializedName;
      private final Supplier<Codec<FilterMask>> codec;

      Type(String param2, Supplier<Codec<FilterMask>> param3) {
         this.serializedName = param2;
         this.codec = param3;
      }

      @Override
      public String getSerializedName() {
         return this.serializedName;
      }

      private Codec<FilterMask> codec() {
         return this.codec.get();
      }
   }
}
