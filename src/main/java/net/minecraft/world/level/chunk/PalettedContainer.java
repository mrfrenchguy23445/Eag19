package net.minecraft.world.level.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;

public class PalettedContainer<T> implements PaletteResize<T>, PalettedContainerRO<T> {
   private static final int MIN_PALETTE_BITS = 0;
   private final PaletteResize<T> dummyPaletteResize = (l_param0, l_param1) -> 0;
   private final IdMap<T> registry;
   private volatile PalettedContainer.Data<T> data;
   private final PalettedContainer.Strategy strategy;
   private final ThreadingDetector threadingDetector = new ThreadingDetector("PalettedContainer");

   public void acquire() {
      this.threadingDetector.checkAndLock();
   }

   public void release() {
      this.threadingDetector.checkAndUnlock();
   }

   public static <T> Codec<PalettedContainer<T>> codecRW(IdMap<T> param0, Codec<T> param1, PalettedContainer.Strategy param2, T param3) {
      PalettedContainerRO.Unpacker<T, PalettedContainer<T>> var4 = PalettedContainer::unpack;
      return codec(param0, param1, param2, param3, var4);
   }

   public static <T> Codec<PalettedContainerRO<T>> codecRO(IdMap<T> param0, Codec<T> param1, PalettedContainer.Strategy param2, T param3) {
      PalettedContainerRO.Unpacker<T, PalettedContainerRO<T>> var4 = (l_param0, l_param1, l_param2) -> unpack(l_param0, l_param1, l_param2)
         .map(l2_param0 -> (PalettedContainerRO<T>)l2_param0);
      return codec(param0, param1, param2, param3, var4);
   }

   private static <T, C extends PalettedContainerRO<T>> Codec<C> codec(
      IdMap<T> param0, Codec<T> param1, PalettedContainer.Strategy param2, T param3, PalettedContainerRO.Unpacker<T, C> param4
   ) {
      return RecordCodecBuilder.<PalettedContainerRO.PackedData>create(
            l_param2 -> l_param2.group(
                  param1.mapResult(ExtraCodecs.orElsePartial(param3)).listOf().fieldOf("palette").forGetter(PalettedContainerRO.PackedData::paletteEntries),
                  Codec.LONG_STREAM.optionalFieldOf("data").forGetter(PalettedContainerRO.PackedData::storage)
               )
               .apply(l_param2, PalettedContainerRO.PackedData::new)
         )
         .comapFlatMap(l_param3 -> param4.read(param0, param2, (PalettedContainerRO.PackedData<T>)l_param3), l_param2 -> l_param2.pack(param0, param2));
   }

   public PalettedContainer(IdMap<T> param0, PalettedContainer.Strategy param1, PalettedContainer.Configuration<T> param2, BitStorage param3, List<T> param4) {
      this.registry = param0;
      this.strategy = param1;
      this.data = new PalettedContainer.Data<>(param2, param3, param2.factory().create(param2.bits(), param0, this, param4));
   }

   private PalettedContainer(IdMap<T> param0, PalettedContainer.Strategy param1, PalettedContainer.Data<T> param2) {
      this.registry = param0;
      this.strategy = param1;
      this.data = param2;
   }

   public PalettedContainer(IdMap<T> param0, T param1, PalettedContainer.Strategy param2) {
      this.strategy = param2;
      this.registry = param0;
      this.data = this.createOrReuseData(null, 0);
      this.data.palette.idFor(param1);
   }

   private PalettedContainer.Data<T> createOrReuseData(@Nullable PalettedContainer.Data<T> param0, int param1) {
      PalettedContainer.Configuration<T> var3 = this.strategy.getConfiguration(this.registry, param1);
      return param0 != null && var3.equals(param0.configuration()) ? param0 : var3.createData(this.registry, this, this.strategy.size());
   }

   @Override
   public int onResize(int param0, T param1) {
      PalettedContainer.Data<T> var3 = this.data;
      PalettedContainer.Data<T> var4 = this.createOrReuseData(var3, param0);
      var4.copyFrom(var3.palette, var3.storage);
      this.data = var4;
      return var4.palette.idFor(param1);
   }

   public T getAndSet(int param0, int param1, int param2, T param3) {
      this.acquire();

      try {
         return this.getAndSet(this.strategy.getIndex(param0, param1, param2), param3);
      } finally {
         this.release();
      }
   }

   public T getAndSetUnchecked(int param0, int param1, int param2, T param3) {
      return this.getAndSet(this.strategy.getIndex(param0, param1, param2), param3);
   }

   private T getAndSet(int param0, T param1) {
      int var3 = this.data.palette.idFor(param1);
      int var4 = this.data.storage.getAndSet(param0, var3);
      return this.data.palette.valueFor(var4);
   }

   public void set(int param0, int param1, int param2, T param3) {
      this.acquire();

      try {
         this.set(this.strategy.getIndex(param0, param1, param2), param3);
      } finally {
         this.release();
      }
   }

   private void set(int param0, T param1) {
      int var3 = this.data.palette.idFor(param1);
      this.data.storage.set(param0, var3);
   }

   @Override
   public T get(int param0, int param1, int param2) {
      return this.get(this.strategy.getIndex(param0, param1, param2));
   }

   protected T get(int param0) {
      PalettedContainer.Data<T> var2 = this.data;
      return var2.palette.valueFor(var2.storage.get(param0));
   }

   @Override
   public void getAll(Consumer<T> param0) {
      Palette<T> var2 = this.data.palette();
      IntSet var3 = new IntArraySet();
      this.data.storage.getAll(var3::add);
      var3.forEach(l_param2 -> param0.accept(var2.valueFor(l_param2)));
   }

   public void read(FriendlyByteBuf param0) {
      this.acquire();

      try {
         int var2 = param0.readByte();
         PalettedContainer.Data<T> var3 = this.createOrReuseData(this.data, var2);
         var3.palette.read(param0);
         param0.readLongArray(var3.storage.getRaw());
         this.data = var3;
      } finally {
         this.release();
      }
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      this.acquire();

      try {
         this.data.write(param0);
      } finally {
         this.release();
      }
   }

   private static <T> DataResult<PalettedContainer<T>> unpack(IdMap<T> param0, PalettedContainer.Strategy param1, PalettedContainerRO.PackedData<T> param2) {
      List<T> var3 = param2.paletteEntries();
      int var4 = param1.size();
      int var5 = param1.calculateBitsForSerialization(param0, var3.size());
      PalettedContainer.Configuration<T> var6 = param1.getConfiguration(param0, var5);
      BitStorage var7;
      if (var5 == 0) {
         var7 = new ZeroBitStorage(var4);
      } else {
         Optional<LongStream> var8 = param2.storage();
         if (var8.isEmpty()) {
            return DataResult.error(() -> "Missing values for non-zero storage");
         }

         long[] var9 = var8.get().toArray();

         try {
            if (var6.factory() == PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY) {
               Palette<T> var10 = new HashMapPalette<>(param0, var5, (l_param0, l_param1) -> 0, var3);
               SimpleBitStorage var11 = new SimpleBitStorage(var5, var4, var9);
               int[] var12 = new int[var4];
               var11.unpack(var12);
               swapPalette(var12, l_param2 -> param0.getId(var10.valueFor(l_param2)));
               var7 = new SimpleBitStorage(var6.bits(), var4, var12);
            } else {
               var7 = new SimpleBitStorage(var6.bits(), var4, var9);
            }
         } catch (SimpleBitStorage.InitializationException var10) {
            return DataResult.error(() -> "Failed to read PalettedContainer: " + var10.getMessage());
         }
      }

      return DataResult.success(new PalettedContainer<>(param0, param1, var6, var7, var3));
   }

   @Override
   public PalettedContainerRO.PackedData<T> pack(IdMap<T> param0, PalettedContainer.Strategy param1) {
      this.acquire();

      try {
         HashMapPalette<T> var3 = new HashMapPalette<>(param0, this.data.storage.getBits(), this.dummyPaletteResize);
         int var4 = param1.size();
         int[] var5 = new int[var4];
         this.data.storage.unpack(var5);
         swapPalette(var5, l_param1 -> var3.idFor(this.data.palette.valueFor(l_param1)));
         int var6 = param1.calculateBitsForSerialization(param0, var3.getSize());
         Optional<LongStream> var7;
         if (var6 != 0) {
            SimpleBitStorage var8 = new SimpleBitStorage(var6, var4, var5);
            var7 = Optional.of(Arrays.stream(var8.getRaw()));
         } else {
            var7 = Optional.empty();
         }

         return new PalettedContainerRO.PackedData<>(var3.getEntries(), var7);
      } finally {
         this.release();
      }
   }

   private static <T> void swapPalette(int[] param0, IntUnaryOperator param1) {
      int var2 = -1;
      int var3 = -1;

      for (int var4 = 0; var4 < param0.length; var4++) {
         int var5 = param0[var4];
         if (var5 != var2) {
            var2 = var5;
            var3 = param1.applyAsInt(var5);
         }

         param0[var4] = var3;
      }
   }

   @Override
   public int getSerializedSize() {
      return this.data.getSerializedSize();
   }

   @Override
   public boolean maybeHas(Predicate<T> param0) {
      return this.data.palette.maybeHas(param0);
   }

   public PalettedContainer<T> copy() {
      return new PalettedContainer<>(this.registry, this.strategy, this.data.copy());
   }

   @Override
   public PalettedContainer<T> recreate() {
      return new PalettedContainer<>(this.registry, this.data.palette.valueFor(0), this.strategy);
   }

   @Override
   public void count(PalettedContainer.CountConsumer<T> param0) {
      if (this.data.palette.getSize() == 1) {
         param0.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
      } else {
         Int2IntOpenHashMap var2 = new Int2IntOpenHashMap();
         this.data.storage.getAll(l_param1 -> var2.addTo(l_param1, 1));
         var2.int2IntEntrySet().forEach(l_param1 -> param0.accept(this.data.palette.valueFor(l_param1.getIntKey()), l_param1.getIntValue()));
      }
   }

   record Configuration<T>(Palette.Factory factory, int bits) {
      public PalettedContainer.Data<T> createData(IdMap<T> param0, PaletteResize<T> param1, int param2) {
         BitStorage var4 = this.bits == 0 ? new ZeroBitStorage(param2) : new SimpleBitStorage(this.bits, param2);
         Palette<T> var5 = this.factory.create(this.bits, param0, param1, List.of());
         return new PalettedContainer.Data<>(this, var4, var5);
      }
   }

   @FunctionalInterface
   public interface CountConsumer<T> {
      void accept(T var1, int var2);
   }

   record Data<T>(PalettedContainer.Configuration<T> configuration, BitStorage storage, Palette<T> palette) {

      public void copyFrom(Palette<T> param0, BitStorage param1) {
         for (int var3 = 0; var3 < param1.getSize(); var3++) {
            T var4 = param0.valueFor(param1.get(var3));
            this.storage.set(var3, this.palette.idFor(var4));
         }
      }

      public int getSerializedSize() {
         return 1 + this.palette.getSerializedSize() + FriendlyByteBuf.getVarIntSize(this.storage.getSize()) + this.storage.getRaw().length * 8;
      }

      public void write(FriendlyByteBuf param0) {
         param0.writeByte(this.storage.getBits());
         this.palette.write(param0);
         param0.writeLongArray(this.storage.getRaw());
      }

      public PalettedContainer.Data<T> copy() {
         return new PalettedContainer.Data<>(this.configuration, this.storage.copy(), this.palette.copy());
      }
   }

   public abstract static class Strategy {
      public static final Palette.Factory SINGLE_VALUE_PALETTE_FACTORY = SingleValuePalette::create;
      public static final Palette.Factory LINEAR_PALETTE_FACTORY = LinearPalette::create;
      public static final Palette.Factory HASHMAP_PALETTE_FACTORY = HashMapPalette::create;
      static final Palette.Factory GLOBAL_PALETTE_FACTORY = GlobalPalette::create;
      public static final PalettedContainer.Strategy SECTION_STATES = new PalettedContainer.Strategy(4) {
         @Override
         public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> param0, int param1) {
            return switch (param1) {
               case 0 -> new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, param1);
               case 1, 2, 3, 4 -> new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, 4);
               case 5, 6, 7, 8 -> new PalettedContainer.Configuration(HASHMAP_PALETTE_FACTORY, param1);
               default -> new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(param0.size()));
            };
         }
      };
      public static final PalettedContainer.Strategy SECTION_BIOMES = new PalettedContainer.Strategy(2) {
         @Override
         public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> param0, int param1) {
            return switch (param1) {
               case 0 -> new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, param1);
               case 1, 2, 3 -> new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, param1);
               default -> new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(param0.size()));
            };
         }
      };
      private final int sizeBits;

      Strategy(int param0) {
         this.sizeBits = param0;
      }

      public int size() {
         return 1 << this.sizeBits * 3;
      }

      public int getIndex(int param0, int param1, int param2) {
         return (param1 << this.sizeBits | param2) << this.sizeBits | param0;
      }

      public abstract <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> var1, int var2);

      <A> int calculateBitsForSerialization(IdMap<A> param0, int param1) {
         int var3 = Mth.ceillog2(param1);
         PalettedContainer.Configuration<A> var4 = this.getConfiguration(param0, var3);
         return var4.factory() == GLOBAL_PALETTE_FACTORY ? var3 : var4.bits();
      }
   }
}
