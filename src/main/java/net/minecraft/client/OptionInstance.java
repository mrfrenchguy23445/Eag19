package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.OptionEnum;
import org.slf4j.Logger;

public final class OptionInstance<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final OptionInstance.Enum<Boolean> BOOLEAN_VALUES = new OptionInstance.Enum<>(ImmutableList.of(Boolean.TRUE, Boolean.FALSE), Codec.BOOL);
   public static final OptionInstance.CaptionBasedToString<Boolean> BOOLEAN_TO_STRING = (param0, param1) -> param1
      ? CommonComponents.OPTION_ON
      : CommonComponents.OPTION_OFF;
   private final OptionInstance.TooltipSupplier<T> tooltip;
   final Function<T, Component> toString;
   private final OptionInstance.ValueSet<T> values;
   private final Codec<T> codec;
   private final T initialValue;
   private final Consumer<T> onValueUpdate;
   final Component caption;
   T value;

   public static OptionInstance<Boolean> createBoolean(String param0, boolean param1, Consumer<Boolean> param2) {
      return createBoolean(param0, noTooltip(), param1, param2);
   }

   public static OptionInstance<Boolean> createBoolean(String param0, boolean param1) {
      return createBoolean(param0, noTooltip(), param1, l_param0 -> {});
   }

   public static OptionInstance<Boolean> createBoolean(String param0, OptionInstance.TooltipSupplier<Boolean> param1, boolean param2) {
      return createBoolean(param0, param1, param2, l_param0 -> {});
   }

   public static OptionInstance<Boolean> createBoolean(String param0, OptionInstance.TooltipSupplier<Boolean> param1, boolean param2, Consumer<Boolean> param3) {
      return createBoolean(param0, param1, BOOLEAN_TO_STRING, param2, param3);
   }

   public static OptionInstance<Boolean> createBoolean(
      String param0,
      OptionInstance.TooltipSupplier<Boolean> param1,
      OptionInstance.CaptionBasedToString<Boolean> param2,
      boolean param3,
      Consumer<Boolean> param4
   ) {
      return new OptionInstance<>(param0, param1, param2, BOOLEAN_VALUES, param3, param4);
   }

   public OptionInstance(
      String param0,
      OptionInstance.TooltipSupplier<T> param1,
      OptionInstance.CaptionBasedToString<T> param2,
      OptionInstance.ValueSet<T> param3,
      T param4,
      Consumer<T> param5
   ) {
      this(param0, param1, param2, param3, param3.codec(), param4, param5);
   }

   public OptionInstance(
      String param0,
      OptionInstance.TooltipSupplier<T> param1,
      OptionInstance.CaptionBasedToString<T> param2,
      OptionInstance.ValueSet<T> param3,
      Codec<T> param4,
      T param5,
      Consumer<T> param6
   ) {
      this.caption = Component.translatable(param0);
      this.tooltip = param1;
      this.toString = l_param1 -> param2.toString(this.caption, l_param1);
      this.values = param3;
      this.codec = param4;
      this.initialValue = param5;
      this.onValueUpdate = param6;
      this.value = this.initialValue;
   }

   public static <T> OptionInstance.TooltipSupplier<T> noTooltip() {
      return l_param0 -> null;
   }

   public static <T> OptionInstance.TooltipSupplier<T> cachedConstantTooltip(Component param0) {
      return l_param1 -> Tooltip.create(param0);
   }

   public static <T extends OptionEnum> OptionInstance.CaptionBasedToString<T> forOptionEnum() {
      return (l_param0, l_param1) -> l_param1.getCaption();
   }

   public AbstractWidget createButton(Options param0, int param1, int param2, int param3) {
      return this.createButton(param0, param1, param2, param3, l_param0 -> {});
   }

   public AbstractWidget createButton(Options param0, int param1, int param2, int param3, Consumer<T> param4) {
      return this.values.createButton(this.tooltip, param0, param1, param2, param3, param4).apply(this);
   }

   public T get() {
      return this.value;
   }

   public Codec<T> codec() {
      return this.codec;
   }

   @Override
   public String toString() {
      return this.caption.getString();
   }

   public void set(T param0) {
      T var2 = this.values.validateValue(param0).orElseGet(() -> {
         LOGGER.error("Illegal option value " + param0 + " for " + this.caption);
         return this.initialValue;
      });
      if (!Minecraft.getInstance().isRunning()) {
         this.value = var2;
      } else {
         if (!Objects.equals(this.value, var2)) {
            this.value = var2;
            this.onValueUpdate.accept(this.value);
         }
      }
   }

   public OptionInstance.ValueSet<T> values() {
      return this.values;
   }

   public record AltEnum<T>(
      List<T> values, List<T> altValues, BooleanSupplier altCondition, OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter, Codec<T> codec
   ) implements OptionInstance.CycleableValueSet<T> {
      @Override
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.altCondition, this.values, this.altValues);
      }

      @Override
      public Optional<T> validateValue(T param0) {
         return (this.altCondition.getAsBoolean() ? this.altValues : this.values).contains(param0) ? Optional.of(param0) : Optional.empty();
      }
   }

   public interface CaptionBasedToString<T> {
      Component toString(Component var1, T var2);
   }

   public record ClampingLazyMaxIntRange(int minInclusive, IntSupplier maxSupplier, int encodableMaxInclusive)
      implements OptionInstance.IntRangeBase,
      OptionInstance.SliderableOrCyclableValueSet<Integer> {
      public Optional<Integer> validateValue(Integer param0) {
         return Optional.of(Mth.clamp(param0, this.minInclusive(), this.maxInclusive()));
      }

      @Override
      public int maxInclusive() {
         return this.maxSupplier.getAsInt();
      }

      @Override
      public Codec<Integer> codec() {
         return ExtraCodecs.validate(
            Codec.INT,
            l_param0 -> {
               int l_var2 = this.encodableMaxInclusive + 1;
               return l_param0.compareTo(this.minInclusive) >= 0 && l_param0.compareTo(l_var2) <= 0
                  ? DataResult.success(l_param0)
                  : DataResult.error(() -> "Value " + l_param0 + " outside of range [" + this.minInclusive + ":" + l_var2 + "]", l_param0);
            }
         );
      }

      @Override
      public boolean createCycleButton() {
         return true;
      }

      @Override
      public CycleButton.ValueListSupplier<Integer> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(IntStream.range(this.minInclusive, this.maxInclusive() + 1).boxed().toList());
      }
   }

   interface CycleableValueSet<T> extends OptionInstance.ValueSet<T> {
      CycleButton.ValueListSupplier<T> valueListSupplier();

      default OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter() {
         return OptionInstance::set;
      }

      @Override
      default Function<OptionInstance<T>, AbstractWidget> createButton(
         OptionInstance.TooltipSupplier<T> param0, Options param1, int param2, int param3, int param4, Consumer<T> param5
      ) {
         return l_param6 -> CycleButton.builder(l_param6.toString)
            .withValues(this.valueListSupplier())
            .withTooltip(param0)
            .withInitialValue(l_param6.value)
            .create(param2, param3, param4, 20, l_param6.caption, (l2_param3, l2_param4) -> {
               this.valueSetter().set(l_param6, l2_param4);
               param1.save();
               param5.accept(l2_param4);
            });
      }

      interface ValueSetter<T> {
         void set(OptionInstance<T> var1, T var2);
      }
   }

   public record Enum<T>(List<T> values, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
      @Override
      public Optional<T> validateValue(T param0) {
         return this.values.contains(param0) ? Optional.of(param0) : Optional.empty();
      }

      @Override
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.values);
      }
   }

   public record IntRange(int minInclusive, int maxInclusive) implements OptionInstance.IntRangeBase {
      public Optional<Integer> validateValue(Integer param0) {
         return param0.compareTo(this.minInclusive()) >= 0 && param0.compareTo(this.maxInclusive()) <= 0 ? Optional.of(param0) : Optional.empty();
      }

      @Override
      public Codec<Integer> codec() {
         return Codec.intRange(this.minInclusive, this.maxInclusive + 1);
      }
   }

   interface IntRangeBase extends OptionInstance.SliderableValueSet<Integer> {
      int minInclusive();

      int maxInclusive();

      default double toSliderValue(Integer param0) {
         return Mth.map(param0.intValue(), this.minInclusive(), this.maxInclusive(), 0.0F, 1.0F);
      }

      default Integer fromSliderValue(double param0) {
         return Mth.floor(Mth.map(param0, 0.0, 1.0, this.minInclusive(), this.maxInclusive()));
      }

      default <R> OptionInstance.SliderableValueSet<R> xmap(final IntFunction<? extends R> param0, final ToIntFunction<? super R> param1) {
         return new OptionInstance.SliderableValueSet<R>() {
            @Override
            public Optional<R> validateValue(R l_param0) {
               return IntRangeBase.this.validateValue(param1.applyAsInt(l_param0)).map(param0::apply);
            }

            @Override
            public double toSliderValue(R l_param0) {
               return IntRangeBase.this.toSliderValue(param1.applyAsInt(l_param0));
            }

            @Override
            public R fromSliderValue(double l_param0) {
               return (R)param0.apply(IntRangeBase.this.fromSliderValue(l_param0));
            }

            @Override
            public Codec<R> codec() {
               return IntRangeBase.this.codec().xmap(param0::apply, param1::applyAsInt);
            }
         };
      }
   }

   public record LazyEnum<T>(Supplier<List<T>> values, Function<T, Optional<T>> validateValue, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
      @Override
      public Optional<T> validateValue(T param0) {
         return this.validateValue.apply(param0);
      }

      @Override
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.values.get());
      }
   }

   static final class OptionInstanceSliderButton<N> extends AbstractOptionSliderButton {
      private final OptionInstance<N> instance;
      private final OptionInstance.SliderableValueSet<N> values;
      private final OptionInstance.TooltipSupplier<N> tooltipSupplier;
      private final Consumer<N> onValueChanged;

      OptionInstanceSliderButton(
         Options param0,
         int param1,
         int param2,
         int param3,
         int param4,
         OptionInstance<N> param5,
         OptionInstance.SliderableValueSet<N> param6,
         OptionInstance.TooltipSupplier<N> param7,
         Consumer<N> param8
      ) {
         super(param0, param1, param2, param3, param4, param6.toSliderValue(param5.get()));
         this.instance = param5;
         this.values = param6;
         this.tooltipSupplier = param7;
         this.onValueChanged = param8;
         this.updateMessage();
      }

      @Override
      protected void updateMessage() {
         this.setMessage(this.instance.toString.apply(this.instance.get()));
         this.setTooltip(this.tooltipSupplier.apply(this.values.fromSliderValue(this.value)));
      }

      @Override
      protected void applyValue() {
         this.instance.set(this.values.fromSliderValue(this.value));
         this.options.save();
         this.onValueChanged.accept(this.instance.get());
      }
   }

   interface SliderableOrCyclableValueSet<T> extends OptionInstance.CycleableValueSet<T>, OptionInstance.SliderableValueSet<T> {
      boolean createCycleButton();

      @Override
      default Function<OptionInstance<T>, AbstractWidget> createButton(
         OptionInstance.TooltipSupplier<T> param0, Options param1, int param2, int param3, int param4, Consumer<T> param5
      ) {
         return this.createCycleButton()
            ? OptionInstance.CycleableValueSet.super.createButton(param0, param1, param2, param3, param4, param5)
            : OptionInstance.SliderableValueSet.super.createButton(param0, param1, param2, param3, param4, param5);
      }
   }

   interface SliderableValueSet<T> extends OptionInstance.ValueSet<T> {
      double toSliderValue(T var1);

      T fromSliderValue(double var1);

      @Override
      default Function<OptionInstance<T>, AbstractWidget> createButton(
         OptionInstance.TooltipSupplier<T> param0, Options param1, int param2, int param3, int param4, Consumer<T> param5
      ) {
         return l_param6 -> new OptionInstance.OptionInstanceSliderButton<>(param1, param2, param3, param4, 20, l_param6, this, param0, param5);
      }
   }

   @FunctionalInterface
   public interface TooltipSupplier<T> {
      @Nullable
      Tooltip apply(T var1);
   }

   public enum UnitDouble implements OptionInstance.SliderableValueSet<Double> {
      INSTANCE;

      public Optional<Double> validateValue(Double param0) {
         return param0 >= 0.0 && param0 <= 1.0 ? Optional.of(param0) : Optional.empty();
      }

      public double toSliderValue(Double param0) {
         return param0;
      }

      public Double fromSliderValue(double param0) {
         return param0;
      }

      public <R> OptionInstance.SliderableValueSet<R> xmap(final DoubleFunction<? extends R> param0, final ToDoubleFunction<? super R> param1) {
         return new OptionInstance.SliderableValueSet<R>() {
            @Override
            public Optional<R> validateValue(R l_param0) {
               return UnitDouble.this.validateValue(param1.applyAsDouble(l_param0)).map(param0::apply);
            }

            @Override
            public double toSliderValue(R l_param0) {
               return UnitDouble.this.toSliderValue(param1.applyAsDouble(l_param0));
            }

            @Override
            public R fromSliderValue(double l_param0) {
               return (R)param0.apply(UnitDouble.this.fromSliderValue(l_param0));
            }

            @Override
            public Codec<R> codec() {
               return UnitDouble.this.codec().xmap(param0::apply, param1::applyAsDouble);
            }
         };
      }

      @Override
      public Codec<Double> codec() {
         return Codec.either(Codec.doubleRange(0.0, 1.0), Codec.BOOL)
            .xmap(l_param0 -> l_param0.map(l2_param0 -> (Double)l2_param0, l2_param0 -> l2_param0 ? 1.0 : 0.0), Either::left);
      }
   }

   interface ValueSet<T> {
      Function<OptionInstance<T>, AbstractWidget> createButton(
         OptionInstance.TooltipSupplier<T> var1, Options var2, int var3, int var4, int var5, Consumer<T> var6
      );

      Optional<T> validateValue(T var1);

      Codec<T> codec();
   }
}
