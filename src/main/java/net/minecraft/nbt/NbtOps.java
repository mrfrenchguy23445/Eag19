package net.minecraft.nbt;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class NbtOps implements DynamicOps<Tag> {
   public static final NbtOps INSTANCE = new NbtOps();
   private static final String WRAPPER_MARKER = "";

   protected NbtOps() {
   }

   public Tag empty() {
      return EndTag.INSTANCE;
   }

   public <U> U convertTo(DynamicOps<U> param0, Tag param1) {
      switch (param1.getId()) {
         case 0:
            return param0.empty();
         case 1:
            return param0.createByte(((NumericTag)param1).getAsByte());
         case 2:
            return param0.createShort(((NumericTag)param1).getAsShort());
         case 3:
            return param0.createInt(((NumericTag)param1).getAsInt());
         case 4:
            return param0.createLong(((NumericTag)param1).getAsLong());
         case 5:
            return param0.createFloat(((NumericTag)param1).getAsFloat());
         case 6:
            return param0.createDouble(((NumericTag)param1).getAsDouble());
         case 7:
            return param0.createByteList(ByteBuffer.wrap(((ByteArrayTag)param1).getAsByteArray()));
         case 8:
            return param0.createString(param1.getAsString());
         case 9:
            return this.convertList(param0, param1);
         case 10:
            return this.convertMap(param0, param1);
         case 11:
            return param0.createIntList(Arrays.stream(((IntArrayTag)param1).getAsIntArray()));
         case 12:
            return param0.createLongList(Arrays.stream(((LongArrayTag)param1).getAsLongArray()));
         default:
            throw new IllegalStateException("Unknown tag type: " + param1);
      }
   }

   public DataResult<Number> getNumberValue(Tag param0) {
      return param0 instanceof NumericTag var2 ? DataResult.success(var2.getAsNumber()) : DataResult.error(() -> "Not a number");
   }

   public Tag createNumeric(Number param0) {
      return DoubleTag.valueOf(param0.doubleValue());
   }

   public Tag createByte(byte param0) {
      return ByteTag.valueOf(param0);
   }

   public Tag createShort(short param0) {
      return ShortTag.valueOf(param0);
   }

   public Tag createInt(int param0) {
      return IntTag.valueOf(param0);
   }

   public Tag createLong(long param0) {
      return LongTag.valueOf(param0);
   }

   public Tag createFloat(float param0) {
      return FloatTag.valueOf(param0);
   }

   public Tag createDouble(double param0) {
      return DoubleTag.valueOf(param0);
   }

   public Tag createBoolean(boolean param0) {
      return ByteTag.valueOf(param0);
   }

   public DataResult<String> getStringValue(Tag param0) {
      return param0 instanceof StringTag var2 ? DataResult.success(var2.getAsString()) : DataResult.error(() -> "Not a string");
   }

   public Tag createString(String param0) {
      return StringTag.valueOf(param0);
   }

   public DataResult<Tag> mergeToList(Tag param0, Tag param1) {
      return createCollector(param0)
         .map(l_param1 -> DataResult.success(l_param1.accept(param1).result()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + param0, param0));
   }

   public DataResult<Tag> mergeToList(Tag param0, List<Tag> param1) {
      return createCollector(param0)
         .map(l_param1 -> DataResult.success(l_param1.acceptAll(param1).result()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + param0, param0));
   }

   public DataResult<Tag> mergeToMap(Tag param0, Tag param1, Tag param2) {
      if (!(param0 instanceof CompoundTag) && !(param0 instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + param0, param0);
      }

      if (!(param1 instanceof StringTag)) {
         return DataResult.error(() -> "key is not a string: " + param1, param0);
      }

      CompoundTag var4 = new CompoundTag();
      if (param0 instanceof CompoundTag var5) {
         var5.getAllKeys().forEach(l_param2 -> var4.put(l_param2, var5.get(l_param2)));
      }

      var4.put(param1.getAsString(), param2);
      return DataResult.success(var4);
   }

   public DataResult<Tag> mergeToMap(Tag param0, MapLike<Tag> param1) {
      if (!(param0 instanceof CompoundTag) && !(param0 instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + param0, param0);
      }

      CompoundTag var3 = new CompoundTag();
      if (param0 instanceof CompoundTag var4) {
         var4.getAllKeys().forEach(l_param2 -> var3.put(l_param2, var4.get(l_param2)));
      }

      List<Tag> var4 = Lists.newArrayList();
      param1.entries().forEach(l_param2 -> {
         Tag l_var3 = l_param2.getFirst();
         if (!(l_var3 instanceof StringTag)) {
            var4.add(l_var3);
         } else {
            var3.put(l_var3.getAsString(), l_param2.getSecond());
         }
      });
      return !var4.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + var4, var3) : DataResult.success(var3);
   }

   public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag param0) {
      return param0 instanceof CompoundTag var2
         ? DataResult.success(var2.getAllKeys().stream().map(l_param1 -> Pair.of(this.createString(l_param1), var2.get(l_param1))))
         : DataResult.error(() -> "Not a map: " + param0);
   }

   public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag param0) {
      return param0 instanceof CompoundTag var2
         ? DataResult.success(l_param1 -> var2.getAllKeys().forEach(l2_param2 -> l_param1.accept(this.createString(l2_param2), var2.get(l2_param2))))
         : DataResult.error(() -> "Not a map: " + param0);
   }

   public DataResult<MapLike<Tag>> getMap(Tag param0) {
      return param0 instanceof CompoundTag var2 ? DataResult.success(new MapLike<Tag>() {
         @Nullable
         public Tag get(Tag l_param0) {
            return var2.get(l_param0.getAsString());
         }

         @Nullable
         public Tag get(String l_param0) {
            return var2.get(l_param0);
         }

         @Override
         public Stream<Pair<Tag, Tag>> entries() {
            return var2.getAllKeys().stream().map(l2_param1 -> Pair.of(NbtOps.this.createString(l2_param1), var2.get(l2_param1)));
         }

         @Override
         public String toString() {
            return "MapLike[" + var2 + "]";
         }
      }) : DataResult.error(() -> "Not a map: " + param0);
   }

   public Tag createMap(Stream<Pair<Tag, Tag>> param0) {
      CompoundTag var2 = new CompoundTag();
      param0.forEach(l_param1 -> var2.put(l_param1.getFirst().getAsString(), l_param1.getSecond()));
      return var2;
   }

   private static Tag tryUnwrap(CompoundTag param0) {
      if (param0.size() == 1) {
         Tag var1 = param0.get("");
         if (var1 != null) {
            return var1;
         }
      }

      return param0;
   }

   public DataResult<Stream<Tag>> getStream(Tag param0) {
      if (param0 instanceof ListTag var2) {
         return var2.getElementType() == 10
            ? DataResult.success(var2.stream().map(l_param0 -> tryUnwrap((CompoundTag)l_param0)))
            : DataResult.success(var2.stream());
      } else {
         return param0 instanceof CollectionTag<?> var2 ? DataResult.success(var2.stream().map(l_param0 -> l_param0)) : DataResult.error(() -> "Not a list");
      }
   }

   public DataResult<Consumer<Consumer<Tag>>> getList(Tag param0) {
      if (param0 instanceof ListTag var2) {
         return var2.getElementType() == 10
            ? DataResult.success(l_param1 -> var2.forEach(l2_param1 -> l_param1.accept(tryUnwrap((CompoundTag)l2_param1))))
            : DataResult.success(var2::forEach);
      } else {
         return param0 instanceof CollectionTag<?> var2 ? DataResult.success(var2::forEach) : DataResult.error(() -> "Not a list: " + param0);
      }
   }

   public DataResult<ByteBuffer> getByteBuffer(Tag param0) {
      return param0 instanceof ByteArrayTag var2 ? DataResult.success(ByteBuffer.wrap(var2.getAsByteArray())) : DynamicOps.super.getByteBuffer(param0);
   }

   public Tag createByteList(ByteBuffer param0) {
      return new ByteArrayTag(DataFixUtils.toArray(param0));
   }

   public DataResult<IntStream> getIntStream(Tag param0) {
      return param0 instanceof IntArrayTag var2 ? DataResult.success(Arrays.stream(var2.getAsIntArray())) : DynamicOps.super.getIntStream(param0);
   }

   public Tag createIntList(IntStream param0) {
      return new IntArrayTag(param0.toArray());
   }

   public DataResult<LongStream> getLongStream(Tag param0) {
      return param0 instanceof LongArrayTag var2 ? DataResult.success(Arrays.stream(var2.getAsLongArray())) : DynamicOps.super.getLongStream(param0);
   }

   public Tag createLongList(LongStream param0) {
      return new LongArrayTag(param0.toArray());
   }

   public Tag createList(Stream<Tag> param0) {
      return NbtOps.InitialListCollector.INSTANCE.acceptAll(param0).result();
   }

   public Tag remove(Tag param0, String param1) {
      if (param0 instanceof CompoundTag var3) {
         CompoundTag var4 = new CompoundTag();
         var3.getAllKeys().stream().filter(l_param1 -> !Objects.equals(l_param1, param1)).forEach(l_param2 -> var4.put(l_param2, var3.get(l_param2)));
         return var4;
      } else {
         return param0;
      }
   }

   @Override
   public String toString() {
      return "NBT";
   }

   @Override
   public RecordBuilder<Tag> mapBuilder() {
      return new NbtOps.NbtRecordBuilder();
   }

   private static Optional<NbtOps.ListCollector> createCollector(Tag param0) {
      if (param0 instanceof EndTag) {
         return Optional.of(NbtOps.InitialListCollector.INSTANCE);
      }

      if (param0 instanceof CollectionTag<?> var1) {
         if (var1.isEmpty()) {
            return Optional.of(NbtOps.InitialListCollector.INSTANCE);
         }

         if (var1 instanceof ListTag var2) {
            return switch (var2.getElementType()) {
               case 0 -> Optional.of(NbtOps.InitialListCollector.INSTANCE);
               case 10 -> Optional.of(new NbtOps.HeterogenousListCollector(var2));
               default -> Optional.of(new NbtOps.HomogenousListCollector(var2));
            };
         }

         if (var1 instanceof ByteArrayTag var2) {
            return Optional.of(new NbtOps.ByteListCollector(var2.getAsByteArray()));
         }

         if (var1 instanceof IntArrayTag var2) {
            return Optional.of(new NbtOps.IntListCollector(var2.getAsIntArray()));
         }

         if (var1 instanceof LongArrayTag var2) {
            return Optional.of(new NbtOps.LongListCollector(var2.getAsLongArray()));
         }
      }

      return Optional.empty();
   }

   static class ByteListCollector implements NbtOps.ListCollector {
      private final ByteArrayList values = new ByteArrayList();

      public ByteListCollector(byte param0) {
         this.values.add(param0);
      }

      public ByteListCollector(byte[] param0) {
         this.values.addElements(0, param0);
      }

      @Override
      public NbtOps.ListCollector accept(Tag param0) {
         if (param0 instanceof ByteTag var2) {
            this.values.add(var2.getAsByte());
            return this;
         } else {
            return new NbtOps.HeterogenousListCollector(this.values).accept(param0);
         }
      }

      @Override
      public Tag result() {
         return new ByteArrayTag(this.values.toByteArray());
      }
   }

   static class HeterogenousListCollector implements NbtOps.ListCollector {
      private final ListTag result = new ListTag();

      public HeterogenousListCollector() {
      }

      public HeterogenousListCollector(Collection<Tag> param0) {
         this.result.addAll(param0);
      }

      public HeterogenousListCollector(IntArrayList param0) {
         param0.forEach(l_param0 -> this.result.add(wrapElement(IntTag.valueOf(l_param0))));
      }

      public HeterogenousListCollector(ByteArrayList param0) {
         param0.forEach(l_param0 -> this.result.add(wrapElement(ByteTag.valueOf(l_param0))));
      }

      public HeterogenousListCollector(LongArrayList param0) {
         param0.forEach(l_param0 -> this.result.add(wrapElement(LongTag.valueOf(l_param0))));
      }

      private static boolean isWrapper(CompoundTag param0) {
         return param0.size() == 1 && param0.contains("");
      }

      private static Tag wrapIfNeeded(Tag param0) {
         return param0 instanceof CompoundTag var1 && !isWrapper(var1) ? var1 : wrapElement(param0);
      }

      private static CompoundTag wrapElement(Tag param0) {
         CompoundTag var1 = new CompoundTag();
         var1.put("", param0);
         return var1;
      }

      @Override
      public NbtOps.ListCollector accept(Tag param0) {
         this.result.add(wrapIfNeeded(param0));
         return this;
      }

      @Override
      public Tag result() {
         return this.result;
      }
   }

   static class HomogenousListCollector implements NbtOps.ListCollector {
      private final ListTag result = new ListTag();

      HomogenousListCollector(Tag param0) {
         this.result.add(param0);
      }

      HomogenousListCollector(ListTag param0) {
         this.result.addAll(param0);
      }

      @Override
      public NbtOps.ListCollector accept(Tag param0) {
         if (param0.getId() != this.result.getElementType()) {
            return new NbtOps.HeterogenousListCollector().acceptAll(this.result).accept(param0);
         }

         this.result.add(param0);
         return this;
      }

      @Override
      public Tag result() {
         return this.result;
      }
   }

   static class InitialListCollector implements NbtOps.ListCollector {
      public static final NbtOps.InitialListCollector INSTANCE = new NbtOps.InitialListCollector();

      private InitialListCollector() {
      }

      @Override
      public NbtOps.ListCollector accept(Tag param0) {
         if (param0 instanceof CompoundTag var2) {
            return new NbtOps.HeterogenousListCollector().accept(var2);
         } else if (param0 instanceof ByteTag var2) {
            return new NbtOps.ByteListCollector(var2.getAsByte());
         } else if (param0 instanceof IntTag var2) {
            return new NbtOps.IntListCollector(var2.getAsInt());
         } else {
            return param0 instanceof LongTag var2 ? new NbtOps.LongListCollector(var2.getAsLong()) : new NbtOps.HomogenousListCollector(param0);
         }
      }

      @Override
      public Tag result() {
         return new ListTag();
      }
   }

   static class IntListCollector implements NbtOps.ListCollector {
      private final IntArrayList values = new IntArrayList();

      public IntListCollector(int param0) {
         this.values.add(param0);
      }

      public IntListCollector(int[] param0) {
         this.values.addElements(0, param0);
      }

      @Override
      public NbtOps.ListCollector accept(Tag param0) {
         if (param0 instanceof IntTag var2) {
            this.values.add(var2.getAsInt());
            return this;
         } else {
            return new NbtOps.HeterogenousListCollector(this.values).accept(param0);
         }
      }

      @Override
      public Tag result() {
         return new IntArrayTag(this.values.toIntArray());
      }
   }

   interface ListCollector {
      NbtOps.ListCollector accept(Tag var1);

      default NbtOps.ListCollector acceptAll(Iterable<Tag> param0) {
         NbtOps.ListCollector var2 = this;

         for (Tag var4 : param0) {
            var2 = var2.accept(var4);
         }

         return var2;
      }

      default NbtOps.ListCollector acceptAll(Stream<Tag> param0) {
         return this.acceptAll(param0::iterator);
      }

      Tag result();
   }

   static class LongListCollector implements NbtOps.ListCollector {
      private final LongArrayList values = new LongArrayList();

      public LongListCollector(long param0) {
         this.values.add(param0);
      }

      public LongListCollector(long[] param0) {
         this.values.addElements(0, param0);
      }

      @Override
      public NbtOps.ListCollector accept(Tag param0) {
         if (param0 instanceof LongTag var2) {
            this.values.add(var2.getAsLong());
            return this;
         } else {
            return new NbtOps.HeterogenousListCollector(this.values).accept(param0);
         }
      }

      @Override
      public Tag result() {
         return new LongArrayTag(this.values.toLongArray());
      }
   }

   class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
      protected NbtRecordBuilder() {
         super(NbtOps.this);
      }

      protected CompoundTag initBuilder() {
         return new CompoundTag();
      }

      protected CompoundTag append(String param0, Tag param1, CompoundTag param2) {
         param2.put(param0, param1);
         return param2;
      }

      protected DataResult<Tag> build(CompoundTag param0, Tag param1) {
         if (param1 == null || param1 == EndTag.INSTANCE) {
            return DataResult.success(param0);
         } else if (!(param1 instanceof CompoundTag var3)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + param1, param1);
         } else {
            CompoundTag var4 = new CompoundTag(Maps.newHashMap(var3.entries()));

            for (Entry<String, Tag> var6 : param0.entries().entrySet()) {
               var4.put(var6.getKey(), var6.getValue());
            }

            return DataResult.success(var4);
         }
      }
   }
}
